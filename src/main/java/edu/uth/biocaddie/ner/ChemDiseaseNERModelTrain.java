package edu.uth.biocaddie.ner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.uima.util.FileUtils;
import edu.uth.bioc.cdr.pubtator.PubTatorDataSet;
import edu.uth.bioc.cdr.pubtator.PubTatorDoc;
import edu.uth.clamp.nlp.ner.CRFUtil;

public class ChemDiseaseNERModelTrain {
 
	
	public static void appendFile2PreFile(String pre, String other) throws IOException
	{
		BufferedWriter w= new  BufferedWriter( new  OutputStreamWriter(new  FileOutputStream(new File(pre),  true )));
		File rawF= new File(other);
		FileInputStream rawfis = new FileInputStream(rawF);
		byte[] data = new byte[(int) rawF.length()];
		rawfis.read(data);
		rawfis.close();
		String text = new String(data);
		w.append(text);
		w.close();
	}
	
	
	public static void main( String[] argv ) {
		
		String trainText = Config.NERTraininCorpusDir+"BioCreativeV-CDRTask/CDR_TrainingSet.PubTator.txt";
		String devText = Config.NERTraininCorpusDir+"BioCreativeV-CDRTask/CDR_DevelopmentSet.PubTator.txt";
		String testText  = Config.NERTraininCorpusDir+"BioCreativeV-CDRTask/CDR_TestSet.PubTator.txt";
		
		String midDir=Config.NERTMPDir ;
		
		
		if(argv.length>=3)
		{	
			trainText=argv[0];
			testText=argv[1];
			midDir=argv[2];
		}
 
		
		String tmpfile=midDir+"tmp.txt";
		try {
			BufferedWriter tmpo = new BufferedWriter(new FileWriter(tmpfile));
			tmpo.close();
			appendFile2PreFile(tmpfile,trainText);
			appendFile2PreFile(tmpfile,devText);
			appendFile2PreFile(tmpfile,testText);
			trainText = tmpfile;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String trainFeatureF = midDir+"CD_train.fea";
		String modelF        = midDir+"CD.model";	 
		
        System.out.println("=========================================================");
        System.out.println("NER System for "+"C&D Start....");
        long startTime = System.currentTimeMillis();
//		
		convInPubtator2FeaFile(trainText,trainFeatureF);
		
		String params ="-a lbfgs -p c2=0.8";			
		train(trainFeatureF, modelF,params );
		
		long endTime = System.currentTimeMillis();
        System.out.println("Total execution time: " + (int)(endTime - startTime)/1000 + " seconds" );
        System.out.println("=========================================================");
		
	}
 
	
	
	private static void convInPubtator2FeaFile(String inPubtator,String feaF)
	{
		
		ChemDiseaseNERecognizer cdner= (ChemDiseaseNERecognizer) ChemDiseaseNERecognizer.getDefault();
		try {			
			FileWriter featureFile = new FileWriter( new File( feaF ) );
			
			File finpubtator = new File(inPubtator);
			PubTatorDataSet dataset= new PubTatorDataSet(finpubtator); 
			 
			for(PubTatorDoc pdoc: dataset.docs)
			{
				featureFile.append(cdner.extractFeatureFromPubTatorDoc(pdoc));
			}
			featureFile.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static int train( String featureFileName, String modelFileName,String parms )
	{
		CRFUtil.train( featureFileName, modelFileName, parms );
		
		return 0;
	}
	
	static int train( String featureFileName, String modelFileName )
	{
		CRFUtil.train( featureFileName, modelFileName, "-a lbfgs -p c2=1.0" );
		
		return 0;
	}
	
	static int test( String featureFileName, String modelFileName, String predictFileName ) 
	{	
		
		String ret = CRFUtil.predict( featureFileName, modelFileName );

		try {
			FileWriter predict = new FileWriter( new File( predictFileName ) );
			predict.write( ret );
			predict.close();
		} catch( Exception e ) {
			e.printStackTrace();
		}
		
		return 0;
	}
	
	static int evaluate( String featureFile, String predictFile ) {
		try {
			Vector<String> feature = new Vector<String>();
			BufferedReader infile = new BufferedReader( new FileReader( featureFile ) );
			String line = "";
			while( ( line = infile.readLine() ) != null ) {
				if( line.indexOf( "\t" ) >= 0 ) {
					feature.add( line.substring(0, line.indexOf( "\t" ) ) );
				} else {
					feature.add( "" );
				}
			}
			
			String[] predict = FileUtils.file2String( new File( predictFile ), "UTF-8" ).split( "\\n" );
			infile.close();
			

			Map<String, String> goldNE = new HashMap<String, String>();
			Map<String, String> predNE = new HashMap<String, String>();
			Map<String, Integer[] > performance = new HashMap<String, Integer[]>();
			
			for( int i = 0; i < predict.length; i++ ) {
				if( predict[i].startsWith( "B-" ) ) {
					String sem = predict[i].substring( 2 );
					int start = i;
					int end = i;
					for( int j = i+1; j < predict.length; j++ ) {
						if( predict[j].equals( "I-" + sem ) ) {
							end = j;
						} else {
							break;
						}
					}
					predNE.put( start + ":" + end, sem );
					if( !performance.containsKey( sem ) ) {
						performance.put( sem, new Integer[]{0,0,0} );
					}
					performance.get( sem )[0] += 1;
				}

			}
			for( int i = 0; i < feature.size(); i++ ) {
				if( feature.get(i).startsWith( "B-" ) ) {
					String sem = feature.get(i).substring( 2 );
					int start = i;
					int end = i;
					for( int j = i+1; j < feature.size(); j++ ) {
						if( feature.get(j).equals( "I-" + sem ) ) {
							end = j;
						} else {
							break;
						}
					}
					goldNE.put( start + ":" + end, sem );
					if( !performance.containsKey( sem ) ) {
						performance.put( sem, new Integer[]{0,0,0} );
					}
					performance.get( sem )[1] += 1;
				}
			}
			for( String key : goldNE.keySet() ) {
				String sem = goldNE.get( key );
				if( predNE.containsKey( key ) && predNE.get(key).equals( goldNE.get(key) ) ) {
					performance.get( sem )[2] += 1;
				}
			}
			
			
		//	System.out.println( performance.toString() );
			for( String key : performance.keySet() ) {
				float p = performance.get(key)[2] / ((float) performance.get(key)[0] );
				float r = performance.get(key)[2] / ((float) performance.get(key)[1] );
				float f1 = 2 * p * r / ( p + r );
				System.out.println( performance.get( key )[0] 
						+ "\t" + performance.get(key)[1] + "\t" + performance.get(key)[2]
						+ "\t" + p + "\t" + r + "\t" + f1 + "\t" + key );				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return 0;
	}
}
