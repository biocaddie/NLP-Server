package edu.uth.biocaddie.ner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import opennlp.tools.util.Span;
import edu.uth.biocaddie.ner.BioNERecognizer.NETYPEANDMODEL;
import edu.uth.clamp.nlp.ner.CRFUtil;
import edu.uth.clamp.nlp.ner.NERSentence;
import edu.uth.clamp.nlp.ner.NESpan;



class NERThread implements Runnable 
{  public File featureFile;
   public String DIR_NER_MODELS;
   public Vector<NERSentence> sentences;
   public NETYPEANDMODEL m;
   public List<NESpan> retSpan = new ArrayList<NESpan>();;
   public Span[] sentSpans;
  
   NERThread(File featureFile, String DIR_NER_MODELS,Vector<NERSentence> sentences, NETYPEANDMODEL m ,Span[] sentSpans){
	  this.featureFile = featureFile;
	  this.DIR_NER_MODELS = DIR_NER_MODELS;
	  this.sentences=sentences;
	  this.m = m;
	  this.sentSpans = sentSpans;
	 
	  
   }
   public void run ()
   {  
	   String ret = CRFUtil.predict(featureFile.getAbsolutePath(),	
				DIR_NER_MODELS+"/"+m.getModelFile());
		int i = 0;
		for (String line : ret.split("\\n\\n")) {
			String[] lines = line.split("\\n");
			for (int j = 0; j < lines.length; j++) 
			{
				
				 sentences.get(i).setPrediction(j, lines[j]);
			}
			i += 1;
		}

		for (int j = 0; j < sentences.size(); j++) {
			for (NESpan span : sentences.get(j).getPredictionSpan()) {
				this.retSpan.add(new NESpan(sentSpans[j].getStart() + span.start(),
						sentSpans[j].getStart() + span.end(), span.sem()));		
			}
		}	
		
		
   }
   public List<NESpan> getRetSpan(){
	   return this.retSpan;
   }
}

