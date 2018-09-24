package edu.uth.bioc.cdr.pubtator;

import edu.uth.clamp.nlp.structure.ClampToken;

public class PubtatorPublicAccessor {

	public PubtatorPublicAccessor() {
		// TODO Auto-generated constructor stub
	}

	public 	static boolean isCID(PubTatorDoc pdoc, String c_normV,
			String d_normV) {
		for (PubTatorAnnotation ann : pdoc.anns) {
			if (ann.type == AnnType.CID) {
				if ((c_normV.contains(ann.e1)) && (d_normV.contains(ann.e2)))
					return true;
				if ((c_normV.contains(ann.e2)) && (d_normV.contains(ann.e1)))
					return true;
			}
		}
		return false;
	}
	
	
	
	public 	static boolean isCDEntityInCID(PubTatorDoc pdoc, String normV) {
		for (PubTatorAnnotation ann : pdoc.anns) {
			if (ann.type == AnnType.CID) {
				if(normV.equals(ann.e1))
					return true;
				if(normV.equals(ann.e2))
					return true;
			}
		}
		return false;
	}
	
	
	public static PubTatorAnnotation getAnn(ClampToken token, PubTatorDoc doc)
	{

		for (PubTatorAnnotation ann : doc.anns) {
			if (ann.type != AnnType.CID) {

				if ((token.getBegin() == ann.start)
						|| token.getEnd() == ann.end) {
					int len_ann = ann.end - ann.start;
					int len_tok = token.getEnd() - token.getBegin();
					if (len_tok > len_ann)
						System.out.println("ERROR! "+ann.toString());
					return ann;
				}

				if ((token.getBegin() >= ann.start)
						&& token.getEnd() <= ann.end) {
					return ann;
				}
				
				if((token.getBegin()<ann.end)&&token.getEnd()>ann.end)
				{
					System.out.println("ERROR ? "+ann.toString());
					return ann;
				}
				
				if((token.getBegin()<ann.start)&&token.getEnd()>ann.start)
				{
					System.out.println("ERROR ??? "+ann.toString());
					return ann;
				}
			}
		}
		return null;
	}

	public static PubTatorAnnotation getAnn(ClampToken token ,PubTatorDoc doc, AnnType entityT)
	{
		 
		for(PubTatorAnnotation ann:doc.anns)
		{
			if(ann.type==entityT)
			{	
				if((token.getBegin()==ann.start)||token.getEnd()==ann.end)
				{
					int len_ann=ann.end-ann.start;
					int len_tok=token.getEnd()-token.getBegin();
					if(len_tok>len_ann)
						System.out.println("ERROR! "+ann.toString());
					return ann;
				}
				
			if((token.getBegin()>=ann.start)&&token.getEnd()<=ann.end)
			{
				return ann;
			}	
			
			if((token.getBegin()<ann.end)&&token.getEnd()>ann.end)
			{
				System.out.println("ERROR ??? "+ann.toString());
				return ann;
			}
			
			if((token.getBegin()<ann.start)&&token.getEnd()>ann.start)
			{
				System.out.println("ERROR ??? "+ann.toString());
				return ann;
			}			
			
		}
		}
		return null;
	}
	
	
	public static String getBIOLabel(ClampToken token, PubTatorAnnotation ann) {
		if (ann == null)
			return "O";
		
		if (token.getBegin() == ann.start)
			return "B-" + ann.type.getDesc();
		else
			return "I-" + ann.type.getDesc();

	}

}
