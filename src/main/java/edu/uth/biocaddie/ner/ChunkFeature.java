package edu.uth.biocaddie.ner;

import edu.uth.clamp.nlp.core.OpenNLPChunker;
import edu.uth.clamp.nlp.ner.NERFeature;
import edu.uth.clamp.nlp.ner.NERFeatureExtractor;
import edu.uth.clamp.nlp.ner.NERSentence;

public class ChunkFeature implements NERFeatureExtractor {

	public static final OpenNLPChunker chunker = (OpenNLPChunker) OpenNLPChunker
			.getDefault();
	public static ChunkFeature INSTANCE = new ChunkFeature();

	public ChunkFeature() {
	}

	// public void extract( NERSentence sent, int index, String[] sems ) {
	// sent.addFeature( index, new NERFeature( "CHUNK", sems[ index ] ) );
	// }

	//@Override
	public int extract(NERSentence sent) {
		String[] tokens = new String[sent.length()];
		String[] pos = new String[sent.length()];
		for (int i = 0; i < sent.length(); i++) {
			tokens[i] = sent.getToken(i);
			pos[i] = sent.getPos(i);
		}

		String chunkTag[] = chunker.chunk(tokens, pos);

		String chunks[] = new String[sent.length() + 4];
		chunks[0] = "CHOS";
		chunks[1] = "CHOS";

		chunks[sent.length() + 3] = "CHOE";
		chunks[sent.length() + 2] = "CHOE";
		for (int i = 0; i < sent.length(); i++) {
			chunks[i+2] = chunkTag[i];
		}

		for (int i = 0; i < sent.length(); i++) {
			extract(sent, i, chunks);
		}

		return 0;
	}

	public void extract(NERSentence sent, int index, String[] sems) {
		int newi = index + 2;

		sent.addFeature(index, new NERFeature("ChunkFeaUNI-2", sems[newi - 2]));
		sent.addFeature(index, new NERFeature("ChunkFeaUNI-1", sems[newi - 1]));
		sent.addFeature(index, new NERFeature("ChunkFeaUNI-0", sems[newi - 0]));
		sent.addFeature(index, new NERFeature("ChunkFeaUNI+1", sems[newi + 1]));
		sent.addFeature(index, new NERFeature("ChunkFeaUNI+2", sems[newi + 2]));
		sent.addFeature(index, new NERFeature("ChunkFeaBI-2", sems[newi - 2]
				+ "+" + sems[newi - 1]));
		sent.addFeature(index, new NERFeature("ChunkFeaBI-1", sems[newi - 1]
				+ "+" + sems[newi - 0]));
		sent.addFeature(index, new NERFeature("ChunkFeaBI-0", sems[newi - 0]
				+ "+" + sems[newi + 1]));
		sent.addFeature(index, new NERFeature("ChunkFeaBI+1", sems[newi + 1]
				+ "+" + sems[newi + 2]));
		sent.addFeature(index, new NERFeature("ChunkFeaTRI-1", sems[newi - 2]
				+ "+" + sems[newi - 1] + "+" + sems[newi - 0]));
		sent.addFeature(index, new NERFeature("ChunkFeaTRI-0", sems[newi - 1]
				+ "+" + sems[newi - 0] + "+" + sems[newi + 1]));
		sent.addFeature(index, new NERFeature("ChunkFeaTRI+1", sems[newi - 0]
				+ "+" + sems[newi + 1] + "+" + sems[newi + 2]));
	}

}
