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

import edu.uth.bioc.cdr.pubtator.PubTatorAnnotation;
import edu.uth.bioc.cdr.pubtator.PubTatorDoc;
import edu.uth.bioc.cdr.pubtator.PubtatorPublicAccessor;
import edu.uth.clamp.nlp.core.ClampSentDetector;
import edu.uth.clamp.nlp.core.ClampTokenizer;
import edu.uth.clamp.nlp.core.OpenNLPPosTagger;
import edu.uth.clamp.nlp.ner.*;
import edu.uth.clamp.nlp.structure.ClampSentence;
import edu.uth.clamp.nlp.structure.ClampToken;
import edu.uth.clamp.nlp.structure.DocProcessor;
import edu.uth.clamp.nlp.structure.Document;
import edu.uth.clamp.nlp.structure.XmiUtil;
import edu.uth.clamp.nlp.uima.PosTaggerUIMA;
import edu.uth.clamp.nlp.uima.SentDetectorUIMA;
import edu.uth.clamp.nlp.uima.TokenizerUIMA;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;

public class ChemDiseaseNERecognizer extends NameEntityRecognizer {
	static NameEntityRecognizer instance = null;

	static public NameEntityRecognizer getDefault() {
		if (instance == null) {
			instance = new ChemDiseaseNERecognizer(
					ClampSentDetector.getDefault(),
					ClampTokenizer.getDefault(), OpenNLPPosTagger.getDefault());
			instance.addFeaExtractor(WordShapeFeature.INSTANCE);
			instance.addFeaExtractor(NgramFeature.INSTANCE);
			instance.addFeaExtractor(CharGramFeature.INSTANCE);
			instance.addFeaExtractor(AbbrDictionaryFeature.INSTANCE);
			instance.addFeaExtractor(DiseaseRelatedFeature.INSTANCE);
			instance.addFeaExtractor(ChemicalRelatedFeature.INSTANCE);
			instance.addFeaExtractor(SectionFeature.INSTANCE);
			instance.addFeaExtractor(RegularExprFeature.INSTANCE);
			instance.addFeaExtractor(PrefixSuffixFeature.INSTANCE);
			instance.addFeaExtractor(DictionaryFeature.INSTANCE);
			instance.addFeaExtractor(DiscreteWordEmbeddingFeature.INSTANCE);
//			instance.addFeaExtractor(new BrownClusteringFeature(
//					"medline_2013.brownclutering.path"));
		}
		return instance;
	}

	Logger logger = Logger.getLogger(ChemDiseaseNERecognizer.class.getName());

	SentenceDetector sentDetector = null;
	Tokenizer tokenizer = null;
	POSTagger posTagger = null;
	Vector<NERFeatureExtractor> feaExtractors = null;

	File modelFile = null;

	public ChemDiseaseNERecognizer() {
		feaExtractors = new Vector<NERFeatureExtractor>();
	}

	public ChemDiseaseNERecognizer(SentenceDetector sentDetector,
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
					"ChemDiseaseNERecognizer write the feature file failed.");
			return null;
		}

		String tagRslt = CRFUtil.predictWithProb(featureFile.getAbsolutePath(),
				modelFile.getAbsolutePath()).trim();
		
		//System.out.println(tagRslt);
		
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

	// /used to train NER model from pubDator format annotaion corpus
	public String extractFeatureFromPubTatorDoc(PubTatorDoc pdoc) {
		String features = "";
		String text = pdoc.title + "\n" + pdoc.abs;
		Document doc = new Document(pdoc.PMID, text);
		DocProcessor sentProc = new SentDetectorUIMA(
				ClampSentDetector.getDefault());
		DocProcessor tokenProc = new TokenizerUIMA(ClampTokenizer.getDefault());
		DocProcessor posProc = new PosTaggerUIMA(OpenNLPPosTagger.getDefault());

		sentProc.process(doc);
		tokenProc.process(doc);
		posProc.process(doc);

		int sentIndex = 0;
		for (ClampSentence sent : doc.getSentences()) {

			NERSentence nerSent = new NERSentence(sent);
			if (sentIndex == 0)
				nerSent.setSection("_TLE_");
			else
				nerSent.setSection("_CON_");

			nerSent.setExtractors(this.feaExtractors);

			int tokenIndex = 0;
			for (ClampToken token : XmiUtil.selectToken(doc.getJCas(),
					sent.getBegin(), sent.getEnd())) {
				PubTatorAnnotation ann = PubtatorPublicAccessor.getAnn(token,
						pdoc);
				String BIO_label = PubtatorPublicAccessor.getBIOLabel(token,
						ann);
				nerSent.setLabel(tokenIndex, BIO_label);
				tokenIndex += 1;
			}

			nerSent.extract();
			features += nerSent.dump() + "\n";
			sentIndex += 1;
		}
		return features;
	}

	public static void main(String[] argv) {
		File modelFile = new File(Config.NERModelDir + "CD.model");

		ChemDiseaseNERecognizer recognizer = (ChemDiseaseNERecognizer) ChemDiseaseNERecognizer.getDefault();
		recognizer.setModel(modelFile);
		try {
			String text = FileUtils.file2String(new File(Config.NERTMPDir
					+ "10.txt"), "UTF-8");
			System.out.println(text);
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
		System.out.println("done");
	}

}
