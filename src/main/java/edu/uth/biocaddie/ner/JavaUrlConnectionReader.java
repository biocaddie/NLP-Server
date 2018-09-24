package edu.uth.biocaddie.ner;

import java.net.*;
import java.io.*;
 
/**
 * A complete Java class that demonstrates how to read content (text) from a URL
 * using the Java URL and URLConnection classes.
 * @author alvin alexander, devdaily.com
 */
public class JavaUrlConnectionReader
{
  /*public static void main(String[] args)
  {
    String output  = getUrlContents("http://textmining.ls.manchester.ac.uk:8081/?text=EGFR%20p53%20gene&species=9606,10090,7227");
    System.out.println(output);
  }*/
 
  public  String getUrlContents(String theUrl)
  {
    StringBuilder content = new StringBuilder();
 
    // many of these calls can throw exceptions, so i've just
    // wrapped them all in one try/catch statement.
    try
    {
      // create a url object
      URL url = new URL(theUrl);
 
      // create a urlconnection object
      URLConnection urlConnection = url.openConnection();
 
      // wrap the urlconnection in a bufferedreader
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
 
      String line;
 
      // read from the urlconnection via the bufferedreader
      while ((line = bufferedReader.readLine()) != null)
      {
        content.append(line + "\n");
      }
      bufferedReader.close();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    return content.toString();
  }
}