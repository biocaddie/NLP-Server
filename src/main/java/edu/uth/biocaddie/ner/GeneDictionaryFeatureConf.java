package edu.uth.biocaddie.ner;


import edu.uth.clamp.nlp.configurable.param.FileParam;
import edu.uth.clamp.nlp.configurable.NERFeatureConf;
import edu.uth.clamp.nlp.ner.NERFeatureExtractor;
import edu.uth.clamp.nlp.util.ClampConstants;

public class GeneDictionaryFeatureConf extends NERFeatureConf {
	public FileParam dictFile;
	
	static final String defaultDict = "gene_vanderbilt.txt";
	
	static final String name = "GENE Dictionary file";
	static final String value = defaultDict;
	static final String defaultValue = defaultDict;
	static final String description = "The dictionary which is used for feature extraction. \\nphrase <TAB> semantic\\n";
	
	public GeneDictionaryFeatureConf() {
		super( ClampConstants.NERFeatureExtractor, ClampConstants.NERDictionaryFeature );
		dictFile = new FileParam( name, defaultValue, defaultValue, description );
	}

	public NERFeatureExtractor create() {
		if( dictFile.isDefault() ) {
			return GeneDictionaryFeature.INSTANCE;
		}

		return new GeneDictionaryFeature( dictFile.value() 	);
	}
	

}
