package edu.uth.bioc.cdr.pubtator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PubTatorDataSet {
	
	public List<PubTatorDoc> docs= new ArrayList<PubTatorDoc> ();
	
	public PubTatorDataSet(File f) 
	{
		load(f);
	}

	private void load(File pubtatorfile )  
	{
		//File pubtatorfile = new File(this.pubtatorfile);
		FileInputStream prefis;
		
		 int num_Disease=0;
		 int num_Chemical=0;
		
		try {
			prefis = new FileInputStream(pubtatorfile);
			byte[] data = new byte[(int) pubtatorfile.length()];
			prefis.read(data);
			prefis.close();
			String text = new String(data).trim();
			//text=text.replaceAll("\\r", "");
			String[] tdocs = text.split("\\n\\n");
			
			for(int i=0;i<tdocs.length;i++)
			{
				PubTatorDoc doc = new PubTatorDoc(tdocs[i]);
				docs.add(doc);
				num_Disease+=doc.num_Disease;
				num_Chemical+=doc.num_Chemical;
			}
			
			System.out.println("#Docs:\t"+docs.size());
			System.out.println("#Desase:\t"+num_Disease);
			System.out.println("#Chemical:\t"+num_Chemical);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PubTatorDataSet dat= new PubTatorDataSet(new File("C:/JunXu/BioCreative-Track3-CDR/data/NCBIdevelopset_corpus.txt")); 
		
		for(PubTatorDoc doc: dat.docs)
		{
			System.out.println(doc.PMID);
			System.out.println(doc.title);
			System.out.println(doc.abs);
			for(PubTatorAnnotation ann:doc.anns)
				System.out.println(ann.toString());
		
			System.out.println();
		}
		
	}

}
