package com.fabio.build.htm;

public class App 
{
    public static void main( String[] args )
    {
    	String inpath = "/home/fabio/Documents/htm.javaExperiments/data/CERN_DATA/read_mounts.csv";
    	String outpath = "/home/fabio/Documents/htm.javaExperiments/data/CERN_DATA/read_mounts_output.csv";
    	AnomalyDetector ad = new AnomalyDetector(inpath, outpath);
    	ad.startNetwork();
    }
}
