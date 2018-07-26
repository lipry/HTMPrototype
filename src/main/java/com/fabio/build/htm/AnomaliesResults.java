package com.fabio.build.htm;

public class AnomaliesResults {
	
	private double anomalyLikelihood;
	private boolean anomalyDetected;
	
	public AnomaliesResults(double anomalyLikelihood, boolean anomalyDetected) {
		this.anomalyLikelihood = anomalyLikelihood;
		this.anomalyDetected = anomalyDetected;
	}

	public double getAnomalyLikelihood() {
		return anomalyLikelihood;
	}

	public void setAnomalyLikelihood(double anomalyLikelihood) {
		this.anomalyLikelihood = anomalyLikelihood;
	}

	public boolean isAnomalyDetected() {
		return anomalyDetected;
	}

	public void setAnomalyDetected(boolean anomalyDetected) {
		this.anomalyDetected = anomalyDetected;
	}
	
}
