package edu.uth.biocaddie.ner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.uima.util.FileUtils;

import edu.uth.clamp.nlp.core.ClampSentDetector;
import edu.uth.clamp.nlp.core.ClampTokenizer;
import edu.uth.clamp.nlp.core.OpenNLPPosTagger;
import edu.uth.clamp.nlp.ner.*;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;

public class GeneNERecognizer extends NameEntityRecognizer {
	static NameEntityRecognizer instance = null;

	
	/**
	 *      feaExtractors = new Vector<NERFeatureExtractor>();
        feaExtractors.add( WordShapeFeature.INSTANCE );
        feaExtractors.add( CharGramFeature.INSTANCE);
        feaExtractors.add( ChemicalRelatedFeature.INSTANCE);
        feaExtractors.add( DiseaseRelatedFeature.INSTANCE);
        feaExtractors.add( NgramFeature.INSTANCE );
        feaExtractors.add( RegularExprFeature.INSTANCE );
        feaExtractors.add( PrefixSuffixFeature.INSTANCE );
        feaExtractors.add( GeneDictionaryFeature.INSTANCE );
        //feaExtractors.add( SectionFeature.getDefault() );
        feaExtractors.add( DictionaryFeature.INSTANCE );
        feaExtractors.add(DiscreteWordEmbeddingFeature.INSTANCE);
	 * @return
	 */
	
	static public NameEntityRecognizer getDefault() {
		if (instance == null) {
			instance = new GeneNERecognizer(
					ClampSentDetector.getDefault(),
					ClampTokenizer.getDefault(), OpenNLPPosTagger.getDefault());
			instance.addFeaExtractor(WordShapeFeature.INSTANCE);
			instance.addFeaExtractor(CharGramFeature.INSTANCE);
			instance.addFeaExtractor(DiseaseRelatedFeature.INSTANCE);
			instance.addFeaExtractor(NgramFeature.INSTANCE);
			instance.addFeaExtractor(RegularExprFeature.INSTANCE);
			instance.addFeaExtractor(PrefixSuffixFeature.INSTANCE);
			instance.addFeaExtractor(GeneDictionaryFeature.INSTANCE);
			instance.addFeaExtractor(DictionaryFeature.INSTANCE);
			instance.addFeaExtractor(DiscreteWordEmbeddingFeature.INSTANCE);

		}
		return instance;
	}

	Logger logger = Logger.getLogger(GeneNERecognizer.class.getName());

	SentenceDetector sentDetector = null;
	Tokenizer tokenizer = null;
	POSTagger posTagger = null;
	Vector<NERFeatureExtractor> feaExtractors = null;

	File modelFile = null;

	public GeneNERecognizer() {
		feaExtractors = new Vector<NERFeatureExtractor>();
	}

	public GeneNERecognizer(SentenceDetector sentDetector,
			Tokenizer tokenizer, POSTagger posTagger) {
		this.sentDetector = sentDetector;
		this.tokenizer = tokenizer;
		this.posTagger = posTagger;
		this.feaExtractors = new Vector<NERFeatureExtractor>();
	}

	@Override
	public void setModel(File modelFile) {
		this.modelFile = modelFile;
	}

	@Override
	public NESpan[] recognize(String documentStr) {
		File featureFile = null;
		try {
			featureFile = File.createTempFile(
					"ClampFea" + System.currentTimeMillis(), ".temp"); 
			featureFile.deleteOnExit();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return recognize(documentStr, featureFile);
	}

	@Override
	public NESpan[] recognize(String document, File featureFile) {
		List<NESpan> ret = new ArrayList<NESpan>();
		Vector<NERSentence> sentences = new Vector<NERSentence>();
		Span[] sentSpans = sentDetector.sentPosDetect(document);
		for (Span span : sentDetector.sentPosDetect(document)) {
			String sentStr = document.substring(span.getStart(), span.getEnd());
			Span[] tokens = tokenizer.tokenizePos(sentStr);
			String[] tokenStrs = tokenizer.tokenize(sentStr);
			String[] tags = posTagger.tag(tokenStrs);
			sentences.add(new NERSentence(sentStr, tokens, tags, null));
		}

		recognize(sentences, featureFile);

		for (int i = 0; i < sentences.size(); i++) {
			for (NESpan span : sentences.get(i).getPredictionSpan()) {
				ret.add(new NESpan(sentSpans[i].getStart() + span.start(),
						sentSpans[i].getStart() + span.end(), span.sem()));
			}
		}
		return ret.toArray(new NESpan[ret.size()]);
	}

	@Override
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

	@Override
	public int recognize(Vector<NERSentence> sentences, File featureFile) {
		// 1. write feature file;
		if (extractFeature(sentences, featureFile) < 0) {
			logger.log(Level.SEVERE,
					"ChemDiseaseNERecognizer write the feature file failed.");
			return -1;
		}

		 String ret = CRFUtil.predict(featureFile.getAbsolutePath(),
		 modelFile.getAbsolutePath()).trim(); 

		int i = 0;
		for (String line : ret.split("\\n\\n")) {
			String[] lines = line.split("\\n");
			for (int j = 0; j < lines.length; j++) {
				sentences.get(i).setPrediction(j, lines[j]);
			}
			i += 1;
		}

		return 0;
	}

	@Override
	public void addFeaExtractor(NERFeatureExtractor extractor) {
		feaExtractors.add(extractor);
	}

	@Override
	public void setFeaExtractors(Vector<NERFeatureExtractor> extractors) {
		feaExtractors = extractors;
	}

	/*
	 * only keep those entities with higher probability i.e. the probabilities
	 * of label sequences predicted by the model > min_prob
	 * 
	 * @author jun xu
	 */
	public NESpan[] recognizeNEWithProbFilter(String documentStr,
			double min_prob) {
		File featureFile = null;
		try {
			featureFile = File.createTempFile(
					"ClampFea" + System.currentTimeMillis(), ".temp"); 
			featureFile.deleteOnExit();
		} catch (IOException e) {
			e.printStackTrace();
		}

		List<NESpan> ret = new ArrayList<NESpan>();
		Vector<NERSentence> sentences = new Vector<NERSentence>();
		Span[] sentSpans = sentDetector.sentPosDetect(documentStr);
		for (Span span : sentDetector.sentPosDetect(documentStr)) {
			String sentStr = documentStr.substring(span.getStart(),
					span.getEnd());
			Span[] tokens = tokenizer.tokenizePos(sentStr);
			String[] tokenStrs = tokenizer.tokenize(sentStr);
			String[] tags = posTagger.tag(tokenStrs);
			sentences.add(new NERSentence(sentStr, tokens, tags, null));
		}

		if (extractFeature(sentences, featureFile) < 0) {
			logger.log(Level.SEVERE,
					"GeneNERecognizer write the feature file failed.");
			return null;
		}

		String tagRslt = CRFUtil.predictWithProb(featureFile.getAbsolutePath(),
				modelFile.getAbsolutePath()).trim();

		int isent = 0;
		for (String line : tagRslt.split("\\n\\n")) {
			String[] lines = line.split("\\n");

			// get probability
			if (lines.length > 0) {
				String[] tmp = lines[0].split("\\t");
				double seq_prob = (new Double(tmp[1])).doubleValue();
				if (seq_prob > min_prob)
					for (int j = 1; j < lines.length; j++) {
						sentences.get(isent).setPrediction(j - 1, lines[j]);
					}
			}
			isent += 1;
		}

		for (int i = 0; i < sentences.size(); i++) {
			for (NESpan span : sentences.get(i).getPredictionSpan()) {
				ret.add(new NESpan(sentSpans[i].getStart() + span.start(),
						sentSpans[i].getStart() + span.end(), span.sem()));
			}
		}
		return ret.toArray(new NESpan[ret.size()]);
	}

	public static void main(String[] argv) {
		File modelFile = new File(Config.NERModelDir + "G.model");

		GeneNERecognizer recognizer = (GeneNERecognizer) GeneNERecognizer.getDefault();
		recognizer.setModel(modelFile);
		try {
			String text = FileUtils.file2String(new File(Config.NERTMPDir
					+ "1.txt"), "UTF-8");
			
			//NESpan[] ret = recognizer.recognize(text);
			NESpan[] ret = recognizer.recognizeNEWithProbFilter(text,0.1);
			// NESpan[] ret = recognizer.recognize( text, feaFile );
			for (NESpan span : ret) {
				System.out.println("RET:" + span.start() + "\t" + span.end()
						+ "\t" + span.sem() + "\t\t"
						+ text.substring(span.start(), span.end()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
