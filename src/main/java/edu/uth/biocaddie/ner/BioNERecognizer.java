package edu.uth.biocaddie.ner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.uth.clamp.nlp.core.ClampSentDetector;
import edu.uth.clamp.nlp.core.ClampTokenizer;
import edu.uth.clamp.nlp.core.OpenNLPPosTagger;
import edu.uth.clamp.nlp.ner.*;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;



public class BioNERecognizer {

	static BioNERecognizer instance = null;
	public static final String LABELO = "O";
	public static final String LABELB = "B-";
	public static final String LABELI = "I-";

	Logger logger = Logger.getLogger(BioNERecognizer.class.getName());

	private static String DIR_NER_MODELS = "";

	SentenceDetector sentDetector = null;
	Tokenizer tokenizer = null;
	POSTagger posTagger = null;
	Vector<NERFeatureExtractor> feaExtractors = null;

	public enum NETYPEANDMODEL {
		CHEMICAL_DISEASE("CD.model", 0), CELLINE("CL.model", 1), GENE("G.model",
				2);

		private String mdl; // NER CRF model file name
		private int val;

		private NETYPEANDMODEL(String m, int v) {
			this.mdl = m;
			this.val = v;
		}

		public String getModelFile() {
			return this.mdl;
		}

		public int getValue() {
			return this.val;
		}
	}

	static public BioNERecognizer getDefault() {
		if (instance == null) {
			instance = new BioNERecognizer(ClampSentDetector.getDefault(),
					ClampTokenizer.getDefault(), OpenNLPPosTagger.getDefault());
			instance.addFeaExtractor(CharGramFeature.INSTANCE);
			instance.addFeaExtractor(CelllineDictionaryFeature.INSTANCE);
			instance.addFeaExtractor(ChemicalRelatedFeature.INSTANCE);
			instance.addFeaExtractor(ChunkFeature.INSTANCE);
			instance.addFeaExtractor(DiseaseRelatedFeature.INSTANCE);
			instance.addFeaExtractor(DictionaryFeature.INSTANCE);
			instance.addFeaExtractor(DiscreteWordEmbeddingFeature.INSTANCE);
			instance.addFeaExtractor(GeneDictionaryFeature.INSTANCE);
			instance.addFeaExtractor(NgramFeature.INSTANCE);
			instance.addFeaExtractor(RegularExprFeature.INSTANCE);
			instance.addFeaExtractor(SectionFeature.INSTANCE);
			instance.addFeaExtractor(PrefixSuffixFeature.INSTANCE);
			instance.addFeaExtractor(WordShapeFeature.INSTANCE);
			String modelPath = Config.HomeDir+"model";
			instance.setModelDir(modelPath);
		}
		return instance;
	}

	private BioNERecognizer(SentenceDetector sentDetector, Tokenizer tokenizer,
			POSTagger posTagger) {
		this.sentDetector = sentDetector;
		this.tokenizer = tokenizer;
		this.posTagger = posTagger;
		this.feaExtractors = new Vector<NERFeatureExtractor>();
	}

	public void setModelDir(String dir) {
		this.DIR_NER_MODELS = dir;
	}

	public NESpan[]  recognize(String txtStr) {
	
		// pre-processing
		 Vector<NERSentence> sentences_CD = new Vector<NERSentence>();
         Vector<NERSentence> sentences_GN = new Vector<NERSentence>();
         Vector<NERSentence> sentences_CL = new Vector<NERSentence>();
         Span[] sentSpans = sentDetector.sentPosDetect(txtStr);
         for (Span span : sentDetector.sentPosDetect(txtStr)) {
                 String sentStr = txtStr.substring(span.getStart(), span.getEnd());  
                 Span[] tokens = tokenizer.tokenizePos(sentStr);
                 String[] tokenStrs = tokenizer.tokenize(sentStr);
                 String[] tags = posTagger.tag(tokenStrs);
                 sentences_CD.add(new NERSentence(sentStr, tokens, tags, null));
                 sentences_GN.add(new NERSentence(sentStr, tokens, tags, null));
                 sentences_CL.add(new NERSentence(sentStr, tokens, tags, null));
         }
		
		// feature extraction and save to file
		File featureFile = null;
		try {
			featureFile = File.createTempFile(
					"ClampFea" + System.currentTimeMillis(), ".temp");
			featureFile.deleteOnExit();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// recognize all bio NEs
		List<NESpan> retSpan = new ArrayList<NESpan>();
		if (extractFeature(sentences_CD, featureFile) < 0) {
			logger.log(Level.SEVERE,
					"The BioNERecognizer write the feature file failed.");
			return retSpan.toArray(new NESpan[retSpan.size()]);
		}
			Runnable r1 =  new NERThread (featureFile, DIR_NER_MODELS, sentences_CD, NETYPEANDMODEL.values()[0],sentSpans);
	        Thread t1 = new Thread(r1);
	        t1.start();

	        Runnable r2 =  new NERThread (featureFile, DIR_NER_MODELS, sentences_GN, NETYPEANDMODEL.values()[2],sentSpans);
	        Thread t2 = new Thread(r2);
	        t2.start();
	        
	        Runnable r3 =  new NERThread (featureFile, DIR_NER_MODELS, sentences_CL, NETYPEANDMODEL.values()[1],sentSpans);
	        Thread t3 =new Thread(r3);
	        t3.start();
	        
	        try {
				t1.join();
				t2.join();
				t3.join();
			} catch (InterruptedException e) {
				System.out.println("NER Main thread Interrupted");
			}
	        
	        List<NESpan> retSpan1 =((NERThread) r1).getRetSpan();
	        List<NESpan> retSpan2 =((NERThread) r2).getRetSpan();
	        List<NESpan> retSpan3 =((NERThread) r3).getRetSpan();
	        retSpan.addAll(retSpan1);
	        retSpan.addAll(retSpan2);
	        retSpan.addAll(retSpan3);
	        featureFile.delete();
		return retSpan.toArray(new NESpan[retSpan.size()]);
	}
	public NESpan[]  recognize_old(String txtStr) {
		long startTime = System.currentTimeMillis();
		// pre-processing
		Vector<NERSentence> sentences = new Vector<NERSentence>();
		Span[] sentSpans = sentDetector.sentPosDetect(txtStr);
		for (Span span : sentDetector.sentPosDetect(txtStr)) {
			String sentStr = txtStr.substring(span.getStart(), span.getEnd());
			Span[] tokens = tokenizer.tokenizePos(sentStr);
			String[] tokenStrs = tokenizer.tokenize(sentStr);
			String[] tags = posTagger.tag(tokenStrs);
			sentences.add(new NERSentence(sentStr, tokens, tags, null));
		}
		 
		long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("preprocess");
        System.out.println(elapsedTime);
        
        startTime = System.currentTimeMillis();
		// feature extraction and save to file
		File featureFile = null;
		try {
			featureFile = File.createTempFile(
					"ClampFea" + System.currentTimeMillis(), ".temp");
			featureFile.deleteOnExit();
		} catch (IOException e) {
			e.printStackTrace();
		}
		stopTime = System.currentTimeMillis();
        elapsedTime = stopTime - startTime;
        System.out.println("features");
        System.out.println(elapsedTime);
        
		// recognize all bio NEs
		List<NESpan> retSpan = new ArrayList<NESpan>();
		if (extractFeature(sentences, featureFile) < 0) {
			logger.log(Level.SEVERE,
					"The BioNERecognizer write the feature file failed.");
			return retSpan.toArray(new NESpan[retSpan.size()]);
		}

		    
	         for(NETYPEANDMODEL m: NETYPEANDMODEL.values())
			{  
	        	 
			startTime = System.currentTimeMillis();
			String ret = CRFUtil.predict(featureFile.getAbsolutePath(),	
					DIR_NER_MODELS+"/"+m.getModelFile());
			int i = 0;
			for (String line : ret.split("\\n\\n")) {
				String[] lines = line.split("\\n");
				for (int j = 0; j < lines.length; j++) 
				{
					sentences.get(i).setPrediction(j, "O");
					sentences.get(i).setPrediction(j, lines[j]);
				}
				i += 1;
			}
			
			for (int j = 0; j < sentences.size(); j++) {
				for (NESpan span : sentences.get(j).getPredictionSpan()) {
					retSpan.add(new NESpan(sentSpans[j].getStart() + span.start(),
							sentSpans[j].getStart() + span.end(), span.sem()));
				}
			}	
			stopTime = System.currentTimeMillis();
	        elapsedTime = stopTime - startTime;
	        System.out.println(m.toString());
	        System.out.println("model");
	        System.out.println(elapsedTime);
		}
	        stopTime = System.currentTimeMillis();
	        elapsedTime = stopTime - startTime;
	       
	        System.out.println("model");
	        System.out.println(elapsedTime);
		return retSpan.toArray(new NESpan[retSpan.size()]);
	}
	
	public int extractFeature(Vector<NERSentence> sentences, File featureFile) {
		try {
			FileWriter writer = new FileWriter(featureFile);
			for (NERSentence sent : sentences) {
				sent.setExtractors(feaExtractors);
				sent.extract();
				writer.write(sent.dump() + "\n");
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		return 0;
	}

	

	private void addFeaExtractor(NERFeatureExtractor extractor) {
		feaExtractors.add(extractor);
	}
	
	// /*
	// * only keep those entities with higher probability i.e. the probabilities
	// * of label sequences predicted by the model > min_prob
	// *
	// * @author jun xu
	// */
	// public NESpan[] recognizeNEWithProbFilter(String documentStr,
	// double min_prob) {
	// File featureFile = null;
	// try {
	// featureFile = File.createTempFile(
	// "ClampFea" + System.currentTimeMillis(), ".temp");
	// featureFile.deleteOnExit();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	//
	// List<NESpan> ret = new ArrayList<NESpan>();
	// Vector<NERSentence> sentences = new Vector<NERSentence>();
	// Span[] sentSpans = sentDetector.sentPosDetect(documentStr);
	// for (Span span : sentDetector.sentPosDetect(documentStr)) {
	// String sentStr = documentStr.substring(span.getStart(),
	// span.getEnd());
	// Span[] tokens = tokenizer.tokenizePos(sentStr);
	// String[] tokenStrs = tokenizer.tokenize(sentStr);
	// String[] tags = posTagger.tag(tokenStrs);
	// sentences.add(new NERSentence(sentStr, tokens, tags, null));
	// }
	//
	// if (extractFeature(sentences, featureFile) < 0) {
	// logger.log(Level.SEVERE,
	// "CelllineNERecognizer write the feature file failed.");
	// return null;
	// }
	//
	// String tagRslt = CRFUtil.predictWithProb(featureFile.getAbsolutePath(),
	// modelFile.getAbsolutePath()).trim();
	//
	// int isent = 0;
	// for (String line : tagRslt.split("\\n\\n")) {
	// String[] lines = line.split("\\n");
	//
	// // get probability
	// if (lines.length > 0) {
	// String[] tmp = lines[0].split("\\t");
	// double seq_prob = (new Double(tmp[1])).doubleValue();
	// if (seq_prob > min_prob)
	// for (int j = 1; j < lines.length; j++) {
	// sentences.get(isent).setPrediction(j - 1, lines[j]);
	// }
	// }
	// isent += 1;
	// }
	//
	// for (int i = 0; i < sentences.size(); i++) {
	// for (NESpan span : sentences.get(i).getPredictionSpan()) {
	// ret.add(new NESpan(sentSpans[i].getStart() + span.start(),
	// sentSpans[i].getStart() + span.end(), span.sem()));
	// }
	// }
	// return ret.toArray(new NESpan[ret.size()]);
	// }

	public static void main(String[] argv) {		

		BioNERecognizer recognizer = (BioNERecognizer) BioNERecognizer
				.getDefault();
		recognizer.setModelDir( Config.NERModelDir);
		
		//try {
			//String text = FileUtils.file2String(new File(
			//		Config.NERTraininCorpusDir
			//				+ "CellLine/CLL/PMID-2924315.txt"), "UTF-8");
            String text = "EGFR Cancer CRL-11422";
			/*NESpan[] ret = recognizer.recognize(text);
			// NESpan[] ret = recognizer.recognizeNEWithProbFilter(text,0.5);
			// NESpan[] ret = recognizer.recognize( text, feaFile );
			for (NESpan span : ret) {
				System.out.println("RET:" + span.start() + "\t" + span.end()
						+ "\t" + span.sem() + "\t\t"
						+ text.substring(span.start(), span.end()));
			}*/
			NESpan[] ret1 = recognizer.recognize(text);
			// NESpan[] ret = recognizer.recognizeNEWithProbFilter(text,0.5);
			// NESpan[] ret = recognizer.recognize( text, feaFile );
			for (NESpan span : ret1) {
				System.out.println("RET:" + span.start() + "\t" + span.end()
						+ "\t" + span.sem() + "\t\t"
						+ text.substring(span.start(), span.end()));
			}
			
		//} catch (IOException e) {
		//	e.printStackTrace();
		//}
	}

}
