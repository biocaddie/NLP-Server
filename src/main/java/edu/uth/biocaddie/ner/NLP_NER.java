package edu.uth.biocaddie.ner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import edu.uth.clamp.nlp.ner.NESpan;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.util.DefaultPrettyPrinter;

import edu.uth.clamp.nlp.encoding.UmlsEncoder;

public class NLP_NER {
	/*
	 * extract the string to be processed from the Json file given field for
	 * example, given "dataItem", outString will concatenate content in
	 * "dataItem.title" and "dataItem.description" fields
	 */
	public static String extract_text(String filepath, String field) {
		JSONParser parser = new JSONParser();
		String outString = "";
		try {
			String text = read_file(filepath, StandardCharsets.UTF_8);
			Object obj = parser.parse(text);
			JSONObject jsonObject = (JSONObject) obj;
			JSONObject source = (JSONObject) jsonObject.get("_source");
			JSONObject dataItem = (JSONObject) source.get(field);// change to
																	// different
																	// repository
			String title = (String) dataItem.get("title");
			String description = (String) dataItem.get("description");
			outString = title + '\n' + description;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return outString;
	}

	public static String read_file(String path, Charset encoding)
			throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);

	}

	/*
	 * construct NLP field to a json object
	 */
	@SuppressWarnings("unchecked")
	public static void construct_json_fields(JSONObject jo, String fieldName,
			ArrayList<String> valueList) {
		JSONArray list = new JSONArray();
		for (String value : valueList) {
			list.add(value);
		}
		jo.put(fieldName, list);

	}

	/*
	 * add NLP fields to json file and write to outDir
	 */
	@SuppressWarnings("unchecked")
	public static void write_json_file(String oldJsonFile,
			JSONObject newFields, String outDir) throws FileNotFoundException,
			IOException, ParseException {
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(new FileReader(oldJsonFile));
		JSONObject jsonObject = (JSONObject) obj;
		JSONObject source = (JSONObject) jsonObject.get("_source");
		source.put("NLP_fields", newFields);
		try {
			String[] tempfile = oldJsonFile.split("/");
			String filename = tempfile[tempfile.length - 1];
			String outPath = outDir + filename;
			ObjectMapper mapper = new ObjectMapper();
			ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
			writer.writeValue(new File(outPath), jsonObject);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * get url content from terminology server
	 */
	public static String get_url(String urlString) throws IOException {
		URL oracle = new URL(urlString);
		URLConnection yc = oracle.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				yc.getInputStream()));
		try {
			String inputLine;
			StringBuffer sb = new StringBuffer();
			while ((inputLine = in.readLine()) != null)
				sb.append(inputLine);
			return sb.toString();
		} finally {
			in.close();
		}
		/*
		 * String output = new String(); while ((inputLine = in.readLine()) !=
		 * null) output += inputLine; in.close(); return output;
		 */
	}

	/*
	 * parse content from terminology server and return synonyms
	 */
	public static ArrayList<String> get_synonyms(String cuisID)
			throws IOException {
		String urlString = Config.TerminologyServer + "umls:_" + cuisID;
		String output = null;
		try {
			output = get_url(urlString);
		} catch (IOException e) {
			return null;
		}

		JSONParser parser = new JSONParser();
		Object obj = null;
		if (output != null) {
			try {
				obj = parser.parse(output);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			JSONObject jsonObject = (JSONObject) obj;
			JSONArray msg = (JSONArray) jsonObject.get("nodes");
			// System.out.println(jsonObject.get("nodes").getClass().getName());
			@SuppressWarnings("unchecked")
			Iterator<JSONObject> iterator = msg.iterator();
			while (iterator.hasNext()) {
				JSONObject node = iterator.next();
				String id = (String) node.get("id");
				if (id.equals("umls:_" + cuisID)) {
					@SuppressWarnings("unchecked")
					ArrayList<String> synonyms = (ArrayList<String>) ((JSONObject) node
							.get("meta")).get("synonym");
					return synonyms;
				}
			}
		}

		return null;

	}

	/*
	 * post process entities to increase precision
	 */
	public static boolean post_process_rules(String entity) {
		// remove more than 4 words
		int wordsNum1 = entity.split(" ").length;
		if (wordsNum1 > 4) {
			return false;
		}
		// remove more than 5 tokens
		int wordsNum = entity.split(" |-|\\*|\\.|\\+").length;
		if (wordsNum > 5) {
			return false;
		}
		// remove too long and less than 2 char
		if (entity.length() > 40 || entity.length() <= 2) {
			return false;
		}
		entity = entity.toLowerCase();
		// remove geo file
		if (entity.matches("gds\\d{2,}") || entity.matches("gsm\\d{2,}")
				|| entity.matches("gpl\\d{2,}")) {
			return false;
		}
		// remove in format "ab-ab-ed-de-ed"
		if (entity.matches("[a-z0-9]*-[a-z0-9]*-[a-z0-9]*-[a-z0-9]*-[a-z0-9]*")) {
			return false;
		}
		// remove more than 4 digits
		if (entity.matches(".*\\d{4,}.*")) {
			return false;
		}
		return true;
	}

	/*
	 * test if word is part of element in test_value
	 */
	public static boolean compare(String word, ArrayList<String> test_value) {
		String[] stems = word.split(" |-|\\+|/|\\(|\\)|\\]|\\[");
		if (test_value == null) {
			return false;
		}
		for (int i = 0; i < test_value.size(); i++) {
			String[] stem2 = test_value.get(i).split(
					" |-|\\+|/|\\(|\\)|\\]|\\[");
			for (int j = 0; j < stems.length; j++) {
				if (Arrays.asList(stem2).contains(stems[j])) {
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * dirPath: input folder which has json files outDir: output dir for new
	 * json files field: field name where title and description field is in
	 * ,e.g. dataset, dataItem, etc.
	 */
	public static void NLP_process_json_files(String dirPath, String outDir,
			String field) throws ClassNotFoundException,
			InstantiationException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException, Exception {
		File dir = new File(dirPath);
		File listDir[] = dir.listFiles();

		// Initialization
		BioNERecognizer recognizer = (BioNERecognizer) BioNERecognizer
				.getDefault();
		BigIntegerDictionary BPdict = (BigIntegerDictionary) BigIntegerDictionary
				.getDefault();
		GeneDictionarySearch Genedict = (GeneDictionarySearch) GeneDictionarySearch
				.getDefault();
		MetaMapLite MetaInstance = (MetaMapLite) MetaMapLite.getDefault();
		UmlsEncoder encoder = new UmlsEncoder(Config.UMLSIndex);

		// process for files in the dirPath
		for (int i = 0; i < listDir.length; i++) {
			//if (i % 10 == 0) {
				System.out.println(i);
			//}
			String filename = listDir[i].getName();

			if (filename.equals(".DS_Store")) {
				continue;
			}

			ArrayList<String> Genes = new ArrayList<String>();
			ArrayList<String> Disease = new ArrayList<String>();
			ArrayList<String> Chemical = new ArrayList<String>();
			ArrayList<String> CLs = new ArrayList<String>();
			ArrayList<String> Meshterms = new ArrayList<String>();

			ArrayList<String> GOIDs = new ArrayList<String>();
			ArrayList<String> DiseaseIDs = new ArrayList<String>();
			ArrayList<String> ChemicalIDs = new ArrayList<String>();
			ArrayList<String> GeneIDs = new ArrayList<String>();
			ArrayList<String> MeshIDs = new ArrayList<String>();

			ArrayList<String> MeshSyns = new ArrayList<String>();
			ArrayList<String> GeneSyns = new ArrayList<String>();
			ArrayList<String> DiseaseSyns = new ArrayList<String>();
			ArrayList<String> ChemicalSyns = new ArrayList<String>();
			
			String text = extract_text(listDir[i].toString(), field);
			
			// get BP, BP GOID
			ArrayList<String> BP = BigIntegerDictionary.unique_NER_BP(BPdict,
					text);
			for (String word : BP) {
				String GOID = BigIntegerDictionary.convert_to_GOID(word);
				if (GOID != null) {
					GOID.length();
					if (GOID.length() > 0) {
						GOIDs.add(GOID);
					}
				}
			}
			// get Mesh terms, meshID, mesh synonyms
			ArrayList<String> mesh = MetaMapLite.get_mesh(MetaInstance, text);
			for (String me : mesh) {
				String[] a = me.split("\\|");
				if (!Meshterms.contains(a[1])) {
					Meshterms.add(a[1].toLowerCase());
					MeshIDs.add(a[0]);
					if (Config.SynonymsFlag) {
						ArrayList<String> meshsyn = get_synonyms(a[0]);
						if (meshsyn != null) {
							MeshSyns.addAll(meshsyn);
						}
					}
				}

			}
			
			NESpan[] Ret = recognizer.recognize(text);
			// get Cell lines
			for (NESpan span : Ret) {
				if (span.sem().equals("CL")) {
					String newCL = text.substring(span.start(), span.end())
							.toLowerCase();
					if (!CLs.contains(newCL)) {
						CLs.add(newCL);

					}
				}
			}
			// get gene, geneID, gene synonyms
			ArrayList<String> oriGenes = new ArrayList<String>();
			for (NESpan span : Ret) {
				if (span.sem().equals("GENE")) {
					String newGene = text.substring(span.start(), span.end());
					oriGenes.add(newGene.toLowerCase());
					if (!Genes.contains(newGene.toLowerCase())) {
						if (!GeneDictionarySearch.Cui_dict.containsKey(newGene
								.toLowerCase())) {
							continue;
						}
						if (newGene.length() < 3) {
							continue;
						}
						if (newGene.length() == 3
								&& newGene.matches(".*[a-z].*")) {
							continue;
						}
						Genes.add(newGene.toLowerCase());
						String GeneID = GeneDictionarySearch.Cui_dict
								.get(newGene.toLowerCase());

						if (GeneID.length() > 0) {
							GeneIDs.add(GeneID);
							if (Config.SynonymsFlag) {
								ArrayList<String> Genesyn = get_synonyms(GeneID);
								if (Genesyn != null) {
									GeneSyns.addAll(Genesyn);
								}

							}
						}

					}
				}
			}

			for (NESpan span : Ret) {
				if (span.sem().equals("Disease")) { // get Disease, DiseaseIDs,
													// disease synonyms
					String newDisease = text
							.substring(span.start(), span.end()).toLowerCase();
					if (!Disease.contains(newDisease)) {
						if (!post_process_rules(newDisease)) {
							continue;
						}
						if (Genes.contains(newDisease)) {
							continue;
						}
						String diseaseID = encoder.encode(newDisease);
						if (diseaseID.length() < 1) {
							continue;
						}
						Disease.add(newDisease);
						DiseaseIDs.add(diseaseID);
						if (Config.SynonymsFlag) {
							ArrayList<String> diseasesSyn = get_synonyms(diseaseID);
							if (diseasesSyn != null) {
								DiseaseSyns.addAll(diseasesSyn);
							}
						}
					}

				}

				if (span.sem().equals("Chemical")) { // get Chemical,
														// ChemicalIDs, chemical
														// synonyms

					String newChemical = text.substring(span.start(),
							span.end());
					if (!Chemical.contains(newChemical.toLowerCase())) {
						if (!post_process_rules(newChemical)) {
							continue;
						}
						if (newChemical.length() <= 3) {
							continue;
						}
						newChemical = newChemical.toLowerCase();
						if (newChemical.contains("xxxxx")) {
							continue;
						}
						if (compare(newChemical, oriGenes)) {
							continue;
						}
						if (compare(newChemical, BP)) {
							continue;
						}
						String chemicalID = encoder.encode(newChemical);
						if (chemicalID.length() < 1) {
							continue;
						}

						Chemical.add(newChemical);
						ChemicalIDs.add(chemicalID);
						if (Config.SynonymsFlag) {
							ArrayList<String> Chemicalsyn = get_synonyms(chemicalID);
							if (Chemicalsyn != null) {
								ChemicalSyns.addAll(Chemicalsyn);
							}

						}

					}
				}
			}
			// output as json and write to file
			JSONObject jo = new JSONObject();
			construct_json_fields(jo, "gene", Genes);
			construct_json_fields(jo, "geneID", GeneIDs);
			construct_json_fields(jo, "BP", BP);
			construct_json_fields(jo, "goID", GOIDs);
			construct_json_fields(jo, "chemical", Chemical);
			construct_json_fields(jo, "chemicalID", ChemicalIDs);
			construct_json_fields(jo, "disease", Disease);
			construct_json_fields(jo, "diseaseID", DiseaseIDs);
			construct_json_fields(jo, "meshterm", Meshterms);
			construct_json_fields(jo, "meshID", MeshIDs);
			construct_json_fields(jo, "cellLine", CLs);
			if (Config.SynonymsFlag) {
				construct_json_fields(jo, "diseaseSynonyms", DiseaseSyns);
				construct_json_fields(jo, "meshSynonyms", MeshSyns);
				construct_json_fields(jo, "chemicalSynonyms", ChemicalSyns);
				construct_json_fields(jo, "geneSynonyms", GeneSyns);
			}
			try {
				write_json_file(listDir[i].toString(), jo, outDir);

			} catch (ParseException e) {
				e.printStackTrace();
			}

		}
	}

	public static void main(String[] args) {
		// input folder which has json files
		 
		//String dirPath = "/Users/xchen2/Documents/BioCADDIE/data_resource_for_NLP/arrayexpress/";
		//String outDir = "/Users/xchen2/Documents/BioCADDIE/data_resource_for_NLP/ArrayExpress_NLP_022117/";
		
		String dirPath = "input_example/";
		String field = "dataItem";
		String outDir = "output/";
		try {
			NLP_process_json_files(dirPath, outDir, field);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
