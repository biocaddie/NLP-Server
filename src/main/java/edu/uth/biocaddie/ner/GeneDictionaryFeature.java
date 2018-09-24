package edu.uth.biocaddie.ner;

import java.io.File;
import java.io.InputStream;
import edu.uth.clamp.nlp.ner.DictionaryFeature;
import edu.uth.clamp.nlp.ner.NERFeature;
import edu.uth.clamp.nlp.ner.NERFeatureExtractor;
import edu.uth.clamp.nlp.ner.NERInstance;
import edu.uth.clamp.nlp.ner.NERSentence;
import edu.uth.clamp.nlp.util.BigIntegerDictionary;
//import edu.uth.clamp.nlp.util.DFABasedDictionary;
import edu.uth.clamp.nlp.util.BigIntegerDictionary.Span;

public class GeneDictionaryFeature implements NERFeatureExtractor {
	 
	public static final String defaultDict =  "gene_vanderbilt.txt";
	public static GeneDictionaryFeature INSTANCE = new GeneDictionaryFeature();
	
	BigIntegerDictionary dict =null;
	public GeneDictionaryFeature() {
		InputStream dictStream = DictionaryFeature.class.getResourceAsStream( defaultDict );
	//	dict = new DFABasedDictionary( dictStream );
		dict = new BigIntegerDictionary( dictStream );
		
	}
	
	public GeneDictionaryFeature(File dictFile ) {
	//	dict = new DFABasedDictionary( dictFile );
		dict = new BigIntegerDictionary( dictFile );

	}

	public GeneDictionaryFeature( String dictname) {
		InputStream dictStream = DictionaryFeature.class.getResourceAsStream( dictname);
	//	dict = new DFABasedDictionary( dictStream );
		dict = new BigIntegerDictionary( dictStream );
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
			sems[i+2] = "TK";
		}
		for( Span span : dict.lookup( tokens ) ) {
			sems[span.start() + 2] = NERInstance.LABELB + "GENE";// span.sem();
			for( int i = span.start() + 1; i < span.end(); i++ ) {
				sems[i + 2] = NERInstance.LABELI + "GENE";//span.sem();
			}
		}
		for( int i = 0; i < sent.length(); i++ ) {
			extract( sent, i, sems );
		}
		return 0;
	}
	
	public void extract( NERSentence sent, int index, String[] sems ) {
		int newi = index + 2;
		sent.addFeature( index, new NERFeature( "GENEDictFeaUNI-2", sems[ newi - 2 ] ) );
		sent.addFeature( index, new NERFeature( "GENEDictFeaUNI-1", sems[ newi - 1 ] ) );
		sent.addFeature( index, new NERFeature( "GENEDictFeaUNI-0", sems[ newi - 0 ] ) );
		sent.addFeature( index, new NERFeature( "GENEDictFeaUNI+1", sems[ newi + 1 ] ) );
		sent.addFeature( index, new NERFeature( "GENEDictFeaUNI+2", sems[ newi + 2 ] ) );
		sent.addFeature( index, new NERFeature( "GENEDictFeaBI-2", sems[ newi - 2 ] + "+" + sems[ newi - 1 ] ) );
		sent.addFeature( index, new NERFeature( "GENEDictFeaBI-1", sems[ newi - 1 ] + "+" + sems[ newi - 0 ] ) );
		sent.addFeature( index, new NERFeature( "GENEDictFeaBI-0", sems[ newi - 0 ] + "+" + sems[ newi + 1 ] ) );
		sent.addFeature( index, new NERFeature( "GENEDictFeaBI+1", sems[ newi + 1 ] + "+" + sems[ newi + 2 ] ) );
		sent.addFeature( index, new NERFeature( "GENEDictFeaTRI-1", sems[ newi - 2 ] + "+" + sems[ newi - 1 ] + "+" + sems[ newi - 0 ] ) );
		sent.addFeature( index, new NERFeature( "GENEDictFeaTRI-0", sems[ newi - 1 ] + "+" + sems[ newi - 0 ] + "+" + sems[ newi + 1 ] ) );
		sent.addFeature( index, new NERFeature( "GENEDictFeaTRI+1", sems[ newi - 0 ] + "+" + sems[ newi + 1 ] + "+" + sems[ newi + 2 ] ) );		
	}
	

}
