package edu.uth.biocaddie.ner;


public class Config {

	public Config() {
		// TODO Auto-generated constructor stub
	}
	
	//for NER Task
	
	public static String HomeDir ="/Users/xchen2/Documents/BioCADDIE/NLP_process_supporting_data/";
	public static String NERTMPDir=HomeDir+"tmp/";
	public static String UMLSIndex = HomeDir+"umls_index_lucene4";
	public static String NERModelDir=HomeDir+"model/";
	public static String BpIDPath = HomeDir+"BP_terms_GOID.txt";
	public static String BPtermsPath = HomeDir+"all_BP_terms.txt";
	public static String GeneTermsPath = HomeDir+"HGNC_gene_alias_list.txt";
    public static String GeneCuiPath = HomeDir+"HGNC_gene_alias_cui.txt";
    
    //enable synonyms from terminology server
	public static Boolean SynonymsFlag = false;
	public static String TerminologyServer = "http://terminolgoy_server_ip/scigraph/graph/neighbors/";
	
	//enable Metamaplite cache
    public static Boolean MetaMapCache = true;
    public static String MetaMapCacheSize = "1024";
    
   // public static String NERTraininCorpusDir=HomeDir+"train/";
    public static String NERTraininCorpusDir;
	
    
	public static void setNERTmpDir(String nertmpdir)
	{	
		NERTMPDir =nertmpdir;
	}

    public static void setNERDir(String nerdir)
	{
		NERModelDir =nerdir;
		NERTMPDir =nerdir+"tmp/";
	}
	
}
