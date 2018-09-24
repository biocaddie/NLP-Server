package edu.uth.biocaddie.ner;

public class StringUtil {
	
	public static boolean isSentenceEnd( char c ) {
		
		return false;
	}

	public static boolean isSpace( char c ) {		
		return Character.isWhitespace( c );
	}

	public static String stem( String word ) {
		word = word.toLowerCase();
		//add by xujun
		if(word.length()<=3)
			return word;
		
		
		Stemmer stemmer = new Stemmer();
		for( int i = 0; i < word.length(); i++ ) {
			stemmer.add( word.charAt( i ) );
		}
		stemmer.stem();
		String ret = stemmer.toString().toLowerCase();
	    if( ret.isEmpty() ) {
	    	ret = "__nil__";
	    }
	    return ret;
	}
}
