package edu.uth.bioc.cdr.pubtator;

public enum AnnType 
{
	CHEMICAL("Chemical",0),
	DISEASE("Disease",1),
	CID("CID",2);
	
	private  String desc;
	private  int value;
	
	private AnnType(String descp,int val)
	{
		this.desc = descp;
		this.value =val;
	}
	
	public String getDesc()
	{
		return this.desc;
	}
	
	public int getValue()
	{
		return this.value;
	}
	
	
}
