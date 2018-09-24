# NLP processing program

## Prerequisites

### For running

* Java 1.7 JRE

### For Development

* Java 1.7 JDK
* Maven 

Here are the two folders that are needed in the program. 

       edu.zip  :
     https://drive.google.com/file/d/0B_WIBY5A024WbHFIajdxcVBNdXM/view?usp=sharing

       NLP_process_supporting_data.zip:
     https://drive.google.com/open?id=0B_WIBY5A024WQjNQNUdOVTJyT0U
     
1. The NER code is in edu.uth.biocaddie.ner package in NLP_NER.java.

2. To run the program, you need to have NER supporting data and CLAMP resource files in the system.

3. Change the HomeDir in Config.java to the path where you store the supporting data.

4. Put the edu resource folder under src/main/resources/.

5. The function to process the NLP is "NLP_process_json_files" in NLP_NER.java.

6. Two things can be set in the Config.java. 
   1) To disable cache use in MetamapLite, set MetaMapCache=false but it will reduce the process speed. You can also change the maximum cache size for the MetamapLite.
   2) You can decide to use the terminology server in the process or not by setting SynonymsFlag.
       If SynonymsFlag=true, Config.TerminologyServer should point to the terminology server IP.
       IF SynonymsFlag=true, four more fields will be added to the results (MeshTerm synonyms, disease synonyms, chemical synonyms, gene synonyms).

7. An example of input and output are in the "input_example" and "output" folder.

8. You can use main function in NLP_NER.java to run a test case.
