package com.fabio.build.htm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;

import org.joda.time.DateTime;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.AnomalyLikelihood;
import static org.numenta.nupic.algorithms.Anomaly.KEY_WINDOW_SIZE;
import static org.numenta.nupic.algorithms.Anomaly.KEY_USE_MOVING_AVG;
import static org.numenta.nupic.algorithms.Anomaly.KEY_IS_WEIGHTED;
import static org.numenta.nupic.algorithms.Anomaly.KEY_LEARNING_PERIOD;
import static org.numenta.nupic.algorithms.Anomaly.KEY_ESTIMATION_SAMPLES;
import static org.numenta.nupic.algorithms.Anomaly.VALUE_NONE;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.network.Inference;
import org.numenta.nupic.network.ManualInput;
import org.numenta.nupic.network.Network;
import org.numenta.nupic.network.sensor.FileSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;

import rx.Subscriber;
import rx.functions.Func1;

import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.util.NamedTuple;

public class AnomalyDetector {
	
	private static String CSV_HEADER = "record_number, prediction, anomaly_score, anomaly_likelihood, anomaly_detected"; 
	private Network network;
	private PrintWriter pw;
	private File infile;
	private File outfile;
	private AnomalyLikelihood anomalyLikelihood = null;
	
	public AnomalyDetector(String inputPath, String outputPath){
		try {
			this.infile = new File(inputPath);
			this.outfile = new File(outputPath);
			this.pw = new PrintWriter(outfile);
			this.pw.println(CSV_HEADER);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		Map<String, Object> anomalyParams = ModelParameters.getAnomalyParams();
		this.initAnomalyLikelihood(anomalyParams);
		this.network = this.buildNetwork();
		this.network.observe().subscribe(this.getSubscriber());
	}
	
	private Network buildNetwork(){
    	//Parameters p = ModelParameters.getModelParameters();
		Parameters p = ModelParameters.getModelParameters();
    	
    	Func1<ManualInput, ManualInput> AnomalyLikelihood = this.anomalyLikelihood();
    	
    	Network network = Network.create("Demo", p)    
    	    .add(Network.createRegion("Region 1")                       
    	    	.add(Network.createLayer("Layer 2/3", p)                
    	    		.alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
    	    		.add(new TemporalMemory())                
    	    		.add(new SpatialPooler())
    	    		.add(Anomaly.create())
    	    		.add(AnomalyLikelihood)
    	    		.add(Sensor.create(FileSensor::create, SensorParams.create(
    	    				Keys::path, "", this.infile.getAbsolutePath())))));
    	
    	return network;
	}
	
	public void startNetwork() {
		this.network.start();
	}
	
	private Func1<ManualInput, ManualInput> anomalyLikelihood(){
		return I -> { 
    		double anomalyScore = I.getAnomalyScore();
    		Map<String, NamedTuple> inputs = I.getClassifierInput();
    		
            double inputValue = (double) inputs.get("value").get("inputValue");
            DateTime timestamp = (DateTime) inputs.get("timestamp").get("inputValue");
            double al = anomalyLikelihood.anomalyProbability(inputValue, anomalyScore, timestamp);
            
            AnomaliesResults results = new AnomaliesResults(al, (al > ModelParameters.ANOMALY_DETECTION_TRESHOLD));
            return I.customObject(results);
        };
	}
	
	private void initAnomalyLikelihood(Map<String, Object> anomalyParams) {
		boolean useMovingAvg = (boolean)anomalyParams.getOrDefault(KEY_USE_MOVING_AVG, false);
        int windowSize = (int)anomalyParams.getOrDefault(KEY_WINDOW_SIZE, -1);
        
        if(useMovingAvg && windowSize < 1) {
            throw new IllegalArgumentException("windowSize must be > 0, when using moving average.");
        }
		
        boolean isWeighted = (boolean)anomalyParams.getOrDefault(KEY_IS_WEIGHTED, false);
        int claLearningPeriod = (int)anomalyParams.getOrDefault(KEY_LEARNING_PERIOD, VALUE_NONE);
        int estimationSamples = (int)anomalyParams.getOrDefault(KEY_ESTIMATION_SAMPLES, VALUE_NONE);
        
		this.anomalyLikelihood = new AnomalyLikelihood(useMovingAvg, windowSize, isWeighted, 
				claLearningPeriod, estimationSamples);
	}
	
	private Subscriber<Inference> getSubscriber(){
		//TODO: generalize parameters
		return new Subscriber<Inference>(){
	
			@Override
			public void onCompleted() {
				System.out.println("\nfinished. output path: " + outfile.getAbsolutePath());
				pw.flush();
				pw.close();
			}
	
			@Override
			public void onError(Throwable e) {
				e.printStackTrace();
			}
	
			@Override
			public void onNext(Inference i) {
				double prediction;
				StringBuilder s = new StringBuilder();
				
				if(null != i.getClassification("value").getMostProbableValue(1)) {
					prediction = (Double)i.getClassification("value").getMostProbableValue(1);
				}else {
					prediction = 0.0;
				}
				
				AnomaliesResults results = (AnomaliesResults) i.getCustomObject();
				
				if(i.getRecordNum() % 100 == 0)
					System.out.println(i.getRecordNum() + "points elaborated");
				
				s.append(i.getRecordNum()).append(",")
					.append(prediction).append(",")
					.append(i.getAnomalyScore()).append(",")
					.append(results.getAnomalyLikelihood()).append(",")
					.append(results.isAnomalyDetected() ? 1 : 0);
				
				pw.println(s.toString());
				pw.flush();
				if(results.isAnomalyDetected()) {
					System.out.println("ANOMALY DETECTED: "+i.getClassifierInput().get("timestamp").get("inputValue"));
				}
				
			}
			
		};
	}
}
