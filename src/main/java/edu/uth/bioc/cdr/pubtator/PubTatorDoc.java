package edu.uth.bioc.cdr.pubtator;

import java.util.ArrayList;
import java.util.List;

 public class PubTatorDoc {

	public String PMID="";
	public String title="";
	public String abs="";
	public List <PubTatorAnnotation> anns= new ArrayList<PubTatorAnnotation> ();
	
	public int num_Disease=0;
	public int num_Chemical=0;
	
	
	public PubTatorDoc( String pubtatordoc) 
	{
		//System.out.println(pubtatordoc);
		//System.out.println("------------------------------------------------------------");
		
		
		String tmp[] =  pubtatordoc.trim().split("\\n");
		
	//	if (tmp.length < 2)
	//		throw new IllegalArgumentException("Invalid title text=\"" + pubtatordoc + "\"");
				
		setTitle(tmp[0]);
		setAbs(tmp[1]);
		
	if(tmp.length >2) 		
		for(int i=2;i<tmp.length;i++)
		{
			PubTatorAnnotation ann= new PubTatorAnnotation(tmp[i]);
			anns.add(ann);
			if(ann.type==AnnType.DISEASE)
				num_Disease+=1;
			if(ann.type==AnnType.CHEMICAL)
				num_Chemical+=1;
			
		}
	}
	
	public String toString()
	{
		String ret="";
		ret+=this.PMID+"|t|"+this.title+"\n";
		ret+=this.PMID+"|a|"+this.abs+"\n";
		for(PubTatorAnnotation ann:anns)
			ret+= ann.toString()+"\n";
		return ret;
	}

	public String toString2()
	{
		String ret="";
		ret+=this.PMID+"|t|"+this.title+"\n";
		ret+=this.PMID+"|a|"+this.abs+"\n";
		for(PubTatorAnnotation ann:anns)
			ret+= ann.toString2()+"\n";
		return ret;
	}
	
	
	private void setTitle(String currentLine)
	{
		System.out.println(currentLine);
		String[] split = currentLine.split("\\|");
		if (split.length != 3)
			throw new IllegalArgumentException(this.PMID+ " has Invalid title text=\"" + currentLine + "\"");
		this.PMID=split[0];
		if (!split[1].equals("t"))
			throw new IllegalArgumentException("Invalid title text=\"" + currentLine + "\"");
		this.title=split[2];
	}
	
	private void setAbs(String currentLine)
	{
		String[] split = currentLine.split("\\|");
		if (split.length != 3)
			throw new IllegalArgumentException("Invalid title text=\"" + currentLine + "\"");
		this.PMID=split[0];
		if (!split[1].equals("a"))
			throw new IllegalArgumentException("Invalid title text=\"" + currentLine + "\"");
		this.abs=split[2];
	}

}
