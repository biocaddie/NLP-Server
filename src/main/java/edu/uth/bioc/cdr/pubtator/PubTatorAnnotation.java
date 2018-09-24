package edu.uth.bioc.cdr.pubtator;

public class PubTatorAnnotation {

	public PubTatorAnnotation() 
	{
	
	}
	
	public	String PMID="";
	public  int start= 0;
	public  int end= 0;
	public  String mention="";
	public  AnnType type;	
	public String normValue ="";

	public String e1;
	public String e2;
	
	
	public PubTatorAnnotation(String ann) 
	{
		String tmp[]= ann.split("\\t");
		if(tmp.length==4||tmp.length==5)
		{
			this.PMID=tmp[0].trim();
			this.type=AnnType.CID;
			this.e1 = tmp[2].trim();
			this.e2 = tmp[3].trim();
		}
		else if((tmp.length==6)||(tmp.length==7)||(tmp.length==8))
		{
			this.PMID=tmp[0].trim();
			this.start= Integer.parseInt(tmp[1]);
			this.end= Integer.parseInt(tmp[2]);
			this.mention=tmp[3].trim();
			if(tmp[4].equals("Disease"))
				this.type=AnnType.DISEASE;
			else
				this.type=AnnType.CHEMICAL;
			this.normValue=tmp[5].trim();
		}
		else
			System.err.println("ANN ERROR:\t"+ tmp.length +"\t"+ ann);
		
	}
	
	public String toString()
	{
		String ret=PMID;
		
		if(type==AnnType.CID)
			ret+="\tCID\t"+e1+"\t"+e2;
		else
			ret+="\t"+this.start+ "\t"+ this.end+ "\t"+this.mention+"\t"+this.type.getDesc()+"\t"+this.normValue;
		return ret;
	}
	
	public String toPubtatorString()
	{
		String ret=PMID;
		
		if(type==AnnType.CID)
			ret+="\tCID\t"+e1+"\t"+e2;
		else
		{
			String entityType="Disease";
			if(this.type==AnnType.CHEMICAL)
				entityType="Chemical";
			ret+="\t"+this.start+ "\t"+ this.end+ "\t"+this.mention+"\t"+entityType+"\t"+this.normValue;
		}
		return ret;
	}

	public String toString2()
	{
		String ret=PMID;
		
		if(type==AnnType.CID)
			ret+="\tCID\t"+e1+"\t"+e2;
		else
			ret+="\t"+this.start+ "\t"+ this.end+ "\t"+this.mention+"\t"+"Disease"+"\t"+this.normValue;
		return ret;
	}

}
