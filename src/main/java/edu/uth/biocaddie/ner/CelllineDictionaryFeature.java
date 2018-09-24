package edu.uth.biocaddie.ner;

import java.io.File;
import java.io.InputStream;
import edu.uth.clamp.nlp.ner.DictionaryFeature;
import edu.uth.clamp.nlp.ner.NERFeature;
import edu.uth.clamp.nlp.ner.NERFeatureExtractor;
import edu.uth.clamp.nlp.ner.NERInstance;
import edu.uth.clamp.nlp.ner.NERSentence;
import edu.uth.clamp.nlp.util.BigIntegerDictionary;
import edu.uth.clamp.nlp.util.StringUtil;
//import edu.uth.clamp.nlp.util.DFABasedDictionary;
import edu.uth.clamp.nlp.util.BigIntegerDictionary.Span;

public class CelllineDictionaryFeature implements NERFeatureExtractor {
	 
	public static final String defaultDict =  "cellline.txt";
	public static CelllineDictionaryFeature INSTANCE = new CelllineDictionaryFeature();
	
	BigIntegerDictionary dict =null;
	public CelllineDictionaryFeature() {
		InputStream dictStream = DictionaryFeature.class.getResourceAsStream( defaultDict );
	 	dict = new BigIntegerDictionary( dictStream );
	 	dict.setDoStem(true);
	}
	
	public CelllineDictionaryFeature(File dictFile ) {
	 	dict = new BigIntegerDictionary( dictFile );
	 	dict.setDoStem(true);
	}

	public CelllineDictionaryFeature( String dictname) {
		InputStream dictStream = DictionaryFeature.class.getResourceAsStream( dictname); 
		dict = new BigIntegerDictionary( dictStream );
		dict.setDoStem(true);
	}
	
	public synchronized int extract(NERSentence sent) {
		String[] tokens = new String[ sent.length() ];
		String[] sems = new String[ sent.length() + 4 ];
	  	
		sems[0] = "BOS";
		sems[1] = "BOS";
		sems[ sems.length - 2 ] = "EOS";
		sems[ sems.length - 1 ] = "EOS";
	 	
		
		for( int i = 0; i < sent.length(); i++ ) {
			tokens[i] = sent.getToken(i);
			String stem = StringUtil.stem( tokens[i] );
		 	sems[i+2] = "TK"; 
		}
		for( Span span : dict.lookup( tokens ) ) {
			sems[span.start() + 2] = NERInstance.LABELB + "CL";// span.sem();
			for( int i = span.start() + 1; i < span.end(); i++ ) {
				sems[i + 2] = NERInstance.LABELI + "CL";//span.sem();
			}
		}
		for( int i = 0; i < sent.length(); i++ ) {
			extract( sent, i, sems );
		}
		return 0;
	}
	
	public void extract( NERSentence sent, int index, String[] sems) {
		int newi = index + 2;
		 
		
		sent.addFeature( index, new NERFeature( "CLDictFeaUNI-2", sems[ newi - 2 ] ) );
		sent.addFeature( index, new NERFeature( "CLDictFeaUNI-1", sems[ newi - 1 ] ) );
		sent.addFeature( index, new NERFeature( "CLDictFeaUNI-0", sems[ newi - 0 ] ) );		
		sent.addFeature( index, new NERFeature( "CLDictFeaUNI+1", sems[ newi + 1 ] ) );
		sent.addFeature( index, new NERFeature( "CLDictFeaUNI+2", sems[ newi + 2 ] ) );
		sent.addFeature( index, new NERFeature( "CLDictFeaBI-2", sems[ newi - 2 ] + "+" + sems[ newi - 1 ] ) );
		sent.addFeature( index, new NERFeature( "CLDictFeaBI-1", sems[ newi - 1 ] + "+" + sems[ newi - 0 ] ) );
		sent.addFeature( index, new NERFeature( "CLDictFeaBI-0", sems[ newi - 0 ] + "+" + sems[ newi + 1 ] ) );
		sent.addFeature( index, new NERFeature( "CLDictFeaBI+1", sems[ newi + 1 ] + "+" + sems[ newi + 2 ] ) );
		sent.addFeature( index, new NERFeature( "CLDictFeaTRI-1", sems[ newi - 2 ] + "+" + sems[ newi - 1 ] + "+" + sems[ newi - 0 ] ) );
		sent.addFeature( index, new NERFeature( "CLDictFeaTRI-0", sems[ newi - 1 ] + "+" + sems[ newi - 0 ] + "+" + sems[ newi + 1 ] ) );
		sent.addFeature( index, new NERFeature( "CLDictFeaTRI+1", sems[ newi - 0 ] + "+" + sems[ newi + 1 ] + "+" + sems[ newi + 2 ] ) );		
	}
	

}
