/* This file is licensed to you under the CLAMP Research and 
 * Evaluation Academic Use License Agreement (the "License"); 
 * you may not use this file except in compliance with the License.
 * See the NOTICE file distributed with this work for additional 
 * information regarding copyright ownership.
 * 
 * Software distributed under the License is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.  See the License for the specific 
 * language governing permissions and limitations under the License.
 */

package edu.uth.biocaddie.ner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import opennlp.tools.util.Span;

import org.apache.uima.jcas.JCas;

import edu.uth.clamp.nlp.ner.CRFUtil;
import edu.uth.clamp.nlp.ner.CharGramFeature;
import edu.uth.clamp.nlp.ner.ChemicalRelatedFeature;
import edu.uth.clamp.nlp.ner.DictionaryFeature;
import edu.uth.clamp.nlp.ner.DiscreteWordEmbeddingFeature;
import edu.uth.clamp.nlp.ner.DiseaseRelatedFeature;
import edu.uth.clamp.nlp.ner.NERFeatureExtractor;
import edu.uth.clamp.nlp.ner.NERInstance;
import edu.uth.clamp.nlp.ner.NERSentence;
import edu.uth.clamp.nlp.ner.NESpan;
import edu.uth.clamp.nlp.ner.NgramFeature;
import edu.uth.clamp.nlp.ner.PrefixSuffixFeature;
import edu.uth.clamp.nlp.ner.RegularExprFeature;
import edu.uth.clamp.nlp.ner.WordShapeFeature;
import edu.uth.clamp.nlp.structure.ClampNameEntity;
import edu.uth.clamp.nlp.structure.ClampSentence;
import edu.uth.clamp.nlp.structure.ClampToken;
import edu.uth.clamp.nlp.structure.Document;
import edu.uth.clamp.nlp.structure.XmiUtil;
import edu.uth.clamp.nlp.util.BigIntegerDictionary;

public class CelllineNERModelTrain {
    public static Vector<NERFeatureExtractor> feaExtractors = null;
    public static Map<String, int[] > evalRet = new HashMap<String, int[]>();

    
    public static void train(String traindir, File featureFile,File modelFile)
    {
    	 File Train_Dir = new File( traindir );
    	 List<File> trainFiles = new ArrayList<File>();
         for( File xmlFile : Train_Dir.listFiles() ) {
            if( xmlFile.getName().endsWith( ".xmi" ) ) 
            {
            	trainFiles.add(xmlFile);
            }   
         }
         
         train(trainFiles,  featureFile, modelFile) ;
    }
    
    public static void predict( String  testdir, File modelFile )
    {
   	 File TEST_Dir = new File( testdir );
   	 List<File> testFiles = new ArrayList<File>();
        for( File xmlFile : TEST_Dir.listFiles() ) {
           if( xmlFile.getName().endsWith( ".xmi" ) ) 
           {
           	testFiles.add(xmlFile);
           }   
        }
        
        predict(testFiles,   modelFile) ;
    }
    
    public static void train( List<File> trainFiles, File featureFile, File modelFile ) {
        try {
            FileWriter writer = new FileWriter( featureFile );
            for( File file : trainFiles ) {
                System.out.println( "Extracting features: filename=[" + file.getName() + "]" );
                Document doc = new Document( file );
                Vector<NERSentence> sents = getNERSentences( doc.getJCas() );
                for( NERSentence sent : sents ) {
                    sent.setExtractors( feaExtractors );
                    sent.extract();
                    writer.write( sent.dump() + "\n" );             
                }
            }
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        CRFUtil.train( featureFile.getAbsolutePath(), modelFile.getAbsolutePath(), "-a lbfgs -p c2=0.9" );
    }
    
    public static void predictWithDict( String  testdir )
    {
   	 File TEST_Dir = new File( testdir );
   	 List<File> testFiles = new ArrayList<File>();
        for( File xmlFile : TEST_Dir.listFiles() ) {
           if( xmlFile.getName().endsWith( ".xmi" ) ) 
           {
           	testFiles.add(xmlFile);
           }   
        }
        
        predictWithDict(testFiles) ;
    }
	public static void predictWithDict(List<File> testFiles)
    {
    	BigIntegerDictionary celldict= new BigIntegerDictionary(DictionaryFeature.class.getResourceAsStream( "cellline.txt"));
    	celldict.setCaseSensitive(false);
    	celldict.setDoStem(true);
    	
    	 for( File file : testFiles ) {
             System.out.println( "extracting test features, filen=[" + file.getName() + "]" );
     
             Document doc = new Document( file ); 
			 List<NESpan> prediction = new ArrayList<NESpan>();
			 for( ClampSentence sent : doc.getSentences() ) {
			    
			     int tokensize= sent.getTokens().size();
			     String []words = new String[tokensize];
			     int kk=0;
			     for(ClampToken t:sent.getTokens())
			     {
			    	 words[kk]=t.textStr();
			    	 kk=kk+1;
			     }
			     
			    for( edu.uth.clamp.nlp.util.BigIntegerDictionary.Span span : celldict.lookup( words ) ) 
				{
			    //	System.out.println(words[span.start()]);
			    //	System.out.println(words[span.end()]);
			    	int _beg = sent.getTokens().elementAt(span.start()).getBegin();
			     	int _end = sent.getTokens().elementAt(span.end()-1).getEnd();
			     	prediction.add( new NESpan( _beg,_end,span.sem() ) );	
			 } 
			 }
			 evaluate( doc, prediction.toArray( new NESpan[prediction.size()] ) );
    	 }
    	 
         for( String key : evalRet.keySet() ) {
             System.out.println( evalRet.get( key )[0] + "\t" + evalRet.get( key )[1] + "\t" + evalRet.get( key )[2] 
                     + "\t" + String.format( "%.3f", ((float) evalRet.get(key)[0])/evalRet.get(key)[1] )
                     + "\t" + String.format( "%.3f", ((float) evalRet.get(key)[0])/evalRet.get(key)[2] )
                     + "\t" + String.format( "%.3f", 2.0 * evalRet.get(key)[0] / ( evalRet.get(key)[2] + evalRet.get(key)[1] ) ) + "\t" + key  );
         }
    }

    public static void predict( List<File> testFiles, File modelFile ) {
        File testFeatreFile = new File( modelFile.getParent() + File.separator + "test.fea" );
        for( File file : testFiles ) {
            System.out.println( "extracting test features, filen=[" + file.getName() + "]" );
            boolean outputXMI= false;
            try {
                FileWriter writer = new FileWriter( testFeatreFile );
                Document doc = new Document( file );
                
                if(doc.getNameEntity().size()!=0) 
                	outputXMI =true;
                
                Vector<NERSentence> sents = getNERSentences( doc.getJCas() );
                List<Span> sentSpans = new ArrayList<Span>();
                for( ClampSentence sent : doc.getSentences() ) {
                    sentSpans.add( new Span( sent.getBegin(), sent.getEnd() ) );
                }
                for( NERSentence sent : sents ) {
                    sent.setExtractors( feaExtractors );
                    sent.extract();
                    writer.write( sent.dump() + "\n" );             
                }
                writer.close();
            
            
                String ret = CRFUtil.predict( testFeatreFile.getAbsolutePath(), modelFile.getAbsolutePath() );
                int i = 0;
                for( String line : ret.split( "\\n\\n" ) ) {
                    String[] lines = line.split( "\\n" );
                    for( int j = 0; j < lines.length; j++ ) {
                        sents.get(i).setPrediction(j, lines[j] );
                    }
                    i += 1;
                }
                
                List<NESpan> prediction = new ArrayList<NESpan>();
                for(int ii = 0; ii < sents.size(); ii++ ) {
                    for( NESpan span : sents.get(ii).getPredictionSpan() ) 
                    {
                    	int _beg = sentSpans.get(ii).getStart() + span.start();
                    	int _end = sentSpans.get(ii).getStart() + span.end();
                    	outputXMI = true;
                    	ClampNameEntity cne = new ClampNameEntity( doc.getJCas(), _beg, _end, "_PredictCL_");
                        prediction.add( new NESpan( _beg,_end,span.sem() ) );
                    }
                }
              
                if(outputXMI)
                	doc.save(Config.NERTraininCorpusDir+"/CellLine/tmpout/"+file.getName());
                evaluate( doc, prediction.toArray( new NESpan[prediction.size()] ) );
                                
            } catch( IOException e ) {
                e.printStackTrace();
            }
        }
        
        for( String key : evalRet.keySet() ) {
            System.out.println( evalRet.get( key )[0] + "\t" + evalRet.get( key )[1] + "\t" + evalRet.get( key )[2] 
                    + "\t" + String.format( "%.3f", ((float) evalRet.get(key)[0])/evalRet.get(key)[1] )
                    + "\t" + String.format( "%.3f", ((float) evalRet.get(key)[0])/evalRet.get(key)[2] )
                    + "\t" + String.format( "%.3f", 2.0 * evalRet.get(key)[0] / ( evalRet.get(key)[2] + evalRet.get(key)[1] ) ) + "\t" + key  );
        }
    }
    
    
    public static Map<String, int[] > evaluate( Document doc, NESpan[] prediction ) {
        for( NESpan span : prediction ) {
            String sem = span.sem();
            if( !evalRet.containsKey( sem ) ) {
                evalRet.put( sem, new int[3] );
                evalRet.get( sem )[0] = 0;
                evalRet.get( sem )[1] = 0;
                evalRet.get( sem )[2] = 0;
            }
            evalRet.get(sem)[1] += 1;
        }
        for( ClampNameEntity cne : doc.getNameEntity() ) {
            String sem = cne.getSemanticTag();
            if( !evalRet.containsKey( sem ) ) {
                evalRet.put( sem, new int[3] );
                evalRet.get( sem )[0] = 0;
                evalRet.get( sem )[1] = 0;
                evalRet.get( sem )[2] = 0;
            }
            evalRet.get(sem)[2] += 1;
            for( NESpan span : prediction ) {
                if( span.start() == cne.getBegin() 
                        && span.end() == cne.getEnd()
                        && span.sem().equals( cne.getSemanticTag() ) ) {
                    evalRet.get(sem)[0] += 1;
                }
            }
        }
        return evalRet;
    }
    
    public static Vector<NERSentence> getNERSentences( JCas aJCas ) {
        Vector<NERSentence> sentences = new Vector<NERSentence>();
        Map<Span, String> tokenBIOMap = new HashMap<Span, String>();
        for( ClampNameEntity cne : XmiUtil.selectNE(aJCas, 0, aJCas.getDocumentText().length() ) ) {
            int i = 0;
            for( ClampToken token : cne.getTokens() ) {
                if( i == 0 ) {
                    tokenBIOMap.put( new Span( token.getBegin(), token.getEnd() ), NERInstance.LABELB + cne.getSemanticTag() );
                } else {
                    tokenBIOMap.put( new Span( token.getBegin(), token.getEnd() ), NERInstance.LABELI + cne.getSemanticTag() );
                }
                i += 1;
            }
        }

        // for each sentences
        List<Span> sentSpans = new ArrayList<Span>();
        for( ClampSentence sent : XmiUtil.selectSentence( aJCas, 0, aJCas.getDocumentText().length() ) ) {
            String sentStr = sent.textStr();
            sentSpans.add( new Span( sent.getBegin(), sent.getEnd() ) );
            List<Span> tokens = new ArrayList<Span>();
            List<String> tokenStrs = new ArrayList<String>();
            List<String> tags = new ArrayList<String>();
            List<String> bios = new ArrayList<String>();
            for( ClampToken token : sent.getTokens() ) {
                tokens.add( new Span( token.getBegin() - sent.getBegin(), token.getEnd() - sent.getBegin() ) );
                tokenStrs.add( token.textStr() );
                tags.add( token.getPartOfSpeech() );
                Span key = new Span( token.getBegin(), token.getEnd() );
                if( tokenBIOMap.containsKey( key ) ) {
                    bios.add( tokenBIOMap.get( key ) );
                } else {
                    bios.add( NERInstance.LABELO );
                }
            }
            sentences.add( new NERSentence( sentStr, tokens.toArray( new Span[ tokens.size() ] ), 
                    tags.toArray( new String[ tags.size() ] ), bios.toArray( new String [ bios.size() ] ), sent.getSectionHeader() ) ); 
        }
        return sentences;
    }
    
    public static void main( String[] argv ) {
     
        System.out.println("=========================================================");
        System.out.println("NER System for Cell line Start....");
        long startTime = System.currentTimeMillis();
        
        feaExtractors = new Vector<NERFeatureExtractor>();
        feaExtractors.add( WordShapeFeature.INSTANCE );
        feaExtractors.add( CharGramFeature.INSTANCE); //xx
        feaExtractors.add( ChemicalRelatedFeature.INSTANCE);
        feaExtractors.add( DiseaseRelatedFeature.INSTANCE);//xx
        feaExtractors.add( NgramFeature.INSTANCE );
        feaExtractors.add( RegularExprFeature.INSTANCE );
        feaExtractors.add( PrefixSuffixFeature.INSTANCE );//x ????
        feaExtractors.add( CelllineDictionaryFeature.INSTANCE );
        feaExtractors.add(ChunkFeature.INSTANCE); 
        feaExtractors.add( DictionaryFeature.INSTANCE );
        feaExtractors.add(DiscreteWordEmbeddingFeature.INSTANCE);

     //  train(Config.NERTraininCorpusDir+"CellLine/Gellus", new File( Config.NERTMPDir+"/cellline_train_feature.txt" ), new File( Config.NERModelDir+"/Cellline.model" ) );
     //   predictWithDict(Config.NERTraininCorpusDir+"CellLine/CLL");
          
       predict( Config.NERTraininCorpusDir+"CellLine/CLL", new File( Config.NERModelDir+"/Cellline.model" ) );
       System.out.println("=========================================================");
       predict( Config.NERTraininCorpusDir+"CellLine/CellFinder-CL_corpus", new File( Config.NERModelDir+"/Cellline.model" ) ); 
        
        long endTime = System.currentTimeMillis();
        System.out.println("Total execution time: " + (int)(endTime - startTime)/1000 + " seconds" );
        System.out.println("=========================================================");
        return;
    }
}
