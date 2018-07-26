package com.fabio.build.htm;

import java.util.HashMap;
import java.util.Map;

import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Anomaly.Mode;
import static org.numenta.nupic.algorithms.Anomaly.KEY_MODE;
//import static org.numenta.nupic.algorithms.Anomaly.KEY_WINDOW_SIZE;
import static org.numenta.nupic.algorithms.Anomaly.KEY_LEARNING_PERIOD;
import static org.numenta.nupic.algorithms.Anomaly.KEY_ESTIMATION_SAMPLES;
import org.numenta.nupic.algorithms.Classifier;
import org.numenta.nupic.algorithms.SDRClassifier;
import org.numenta.nupic.util.Tuple;

public class ModelParameters {
	
	public static double ANOMALY_DETECTION_TRESHOLD = 0.9999;
	
	public static Parameters getModelParameters() {
		Map<String, Map<String, Object>> fieldEncodings = getEncodersMap();

        Parameters p = Parameters.getEncoderDefaultParameters();
        p.set(KEY.GLOBAL_INHIBITION, true);
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        p.set(KEY.INPUT_DIMENSIONS, new int[] { 2048 });
        p.set(KEY.CELLS_PER_COLUMN, 32);
        p.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 40.0);
        p.set(KEY.POTENTIAL_PCT, 0.85);
        p.set(KEY.SYN_PERM_CONNECTED,0.2);
        p.set(KEY.SYN_PERM_ACTIVE_INC, 0.003);
        p.set(KEY.SYN_PERM_INACTIVE_DEC, 0.0005);
        p.set(KEY.MAX_BOOST, 1.0);
        
        p.set(KEY.MAX_NEW_SYNAPSE_COUNT, 20);
        p.set(KEY.INITIAL_PERMANENCE, 0.21);
        p.set(KEY.PERMANENCE_INCREMENT, 0.04);
        p.set(KEY.PERMANENCE_DECREMENT, 0.008);
        p.set(KEY.MIN_THRESHOLD, 13);
        p.set(KEY.ACTIVATION_THRESHOLD, 20);
        
        p.set(KEY.CLIP_INPUT, true);
        p.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("value", SDRClassifier.class));
        return p;
	}
	
	public static Map<String, Map<String, Object>> getEncodersMap(){
		//TODO: Generalize parameters
        Map<String, Map<String, Object>> fieldEncodings = setupMap(
                null,
                0, // n
                0, // w
                0, 0, 0, 0, null, null, null,
                "timestamp", "datetime", "DateEncoder");
        fieldEncodings = setupMap(
                fieldEncodings, 
                50, 
                21, 
                0, 350, 0, 0.1, null, Boolean.TRUE, null, 
                "value", "float", "ScalarEncoder");
        
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_TOFD.getFieldName(), new Tuple(21,9.5)); // Time of day
        //fieldEncodings.get("timestamp").put(KEY.DATEFIELD_DOFW.getFieldName(), new Tuple(21,9.5)); //Date of the week
        //fieldEncodings.get("timestamp").put(KEY.DATEFIELD_WKEND.getFieldName(), new Tuple(21,9.5));// is Weekend?
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_PATTERN.getFieldName(), "YYYY-MM-dd'T'HH:mm:ssZ");
        
        return fieldEncodings;
	}
	
    public static Map<String, Map<String, Object>> setupMap(
            Map<String, Map<String, Object>> map,
            int n, int w, double min, double max, double radius, double resolution, Boolean periodic,
            Boolean clip, Boolean forced, String fieldName, String fieldType, String encoderType) {

        if(map == null) {
            map = new HashMap<String, Map<String, Object>>();
        }
        Map<String, Object> inner = null;
        if((inner = map.get(fieldName)) == null) {
            map.put(fieldName, inner = new HashMap<String, Object>());
        }

        inner.put("n", n);
        inner.put("w", w);
        inner.put("minVal", min);
        inner.put("maxVal", max);
        inner.put("radius", radius);
        inner.put("resolution", resolution);

        if(periodic != null) inner.put("periodic", periodic);
        if(clip != null) inner.put("clipInput", clip);
        if(forced != null) inner.put("forced", forced);
        if(fieldName != null) inner.put("fieldName", fieldName);
        if(fieldType != null) inner.put("fieldType", fieldType);
        if(encoderType != null) inner.put("encoderType", encoderType);

        return map;
    }
	
    public static Map<String, Class<? extends Classifier>> getInferredFieldsMap(
            String field, Class<? extends Classifier> classifier) {
        Map<String, Class<? extends Classifier>> inferredFieldsMap = new HashMap<>();
        inferredFieldsMap.put(field, classifier);
        
        return inferredFieldsMap;
    }
    
    public static Map<String, Object> getAnomalyParams(){
	    Map<String, Object> p = new HashMap<>();
	    p.put(KEY_MODE, Mode.LIKELIHOOD);       
	    //p.put(KEY_WINDOW_SIZE, 10);
	    p.put(KEY_LEARNING_PERIOD, 50);
	    p.put(KEY_ESTIMATION_SAMPLES, 50);
	    
	    return p;
    }
    
}
