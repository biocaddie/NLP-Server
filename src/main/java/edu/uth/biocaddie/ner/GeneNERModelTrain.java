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

/**
 *
 *
 * @author Jingqi Wang</br> Min Jiang</br>
 *
 */
public class GeneNERModelTrain {
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
    
    public static void predict( List<File> testFiles, File modelFile ) {
        File testFeatreFile = new File( modelFile.getParent() + File.separator + "test.fea" );
        for( File file : testFiles ) {
            System.out.println( "extracting test features, filen=[" + file.getName() + "]" );
            try {
                FileWriter writer = new FileWriter( testFeatreFile );
                Document doc = new Document( file );
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
            
//            
//                String ret = CRFUtil.predict( testFeatreFile.getAbsolutePath(), modelFile.getAbsolutePath() );
//              //  System.out.println(ret);
//                int i = 0;
//                for( String line : ret.split( "\\n\\n" ) ) {
//                    String[] lines = line.split( "\\n" );
//                    for( int j = 0; j < lines.length; j++ ) {
//                        sents.get(i).setPrediction(j, lines[j] );
//                    }
//                    i += 1;
//                }
                
        		String tagRslt = CRFUtil.predictWithProb(testFeatreFile.getAbsolutePath(),
        				modelFile.getAbsolutePath()).trim();

        		int isent = 0;
        		for (String line : tagRslt.split("\\n\\n")) {
        			String[] lines = line.split("\\n");

        			// get probability
        			if (lines.length > 0) {
        				String[] tmp = lines[0].split("\\t");
        				double seq_prob = (new Double(tmp[1])).doubleValue();
        				if (seq_prob > 0.1)
        					for (int j = 1; j < lines.length; j++) {
        						sents.get(isent).setPrediction(j - 1, lines[j]);
        					}
        			}
        			isent += 1;
        		}
                
                
                
                List<NESpan> prediction = new ArrayList<NESpan>();
                for(int i = 0; i < sents.size(); i++ ) {
                    for( NESpan span : sents.get(i).getPredictionSpan() ) {
                        prediction.add( new NESpan( sentSpans.get(i).getStart() + span.start()
                                , sentSpans.get(i).getStart() + span.end()
                                , span.sem() ) );
                    }
                }
                
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
        System.out.println("NER System for GENE Start....");
        long startTime = System.currentTimeMillis();
        
        feaExtractors = new Vector<NERFeatureExtractor>();
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
     //   feaExtractors.add(new BrownClusteringFeature(
	 //			"medline_2013.brownclutering.path"));

      //   train(Config.NERTraininCorpusDir+"GeneTag/genetag15000", new File( Config.NERTMPDir+"/gene_train_feature.txt" ), new File( Config.NERModelDir+"/Gene.model" ) );
       train(Config.NERTraininCorpusDir+"GeneTag/genetag200000", new File( Config.NERTMPDir+"/gene_train_feature_20000.txt" ), new File( Config.NERModelDir+"/Gene.model" ) );
      //  predict( Config.NERTraininCorpusDir+"GeneTag/genetag5000", new File( Config.NERModelDir+"/G.model" ) );
        
        
        long endTime = System.currentTimeMillis();
        System.out.println("Total execution time: " + (int)(endTime - startTime)/1000 + " seconds" );
        System.out.println("=========================================================");
        return;
    }
}
