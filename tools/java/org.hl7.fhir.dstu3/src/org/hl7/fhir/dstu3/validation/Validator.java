package org.hl7.fhir.dstu3.validation;
/*
Copyright (c) 2011+, HL7, Inc
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this 
   list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
   this list of conditions and the following disclaimer in the documentation 
   and/or other materials provided with the distribution.
 * Neither the name of HL7 nor the names of its contributors may be used to 
   endorse or promote products derived from this software without specific 
   prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.

*/

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.hl7.fhir.dstu3.formats.XmlParser;
import org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.dstu3.utils.Transformer;
import org.hl7.fhir.utilities.Utilities;

/**
 * A service that will validate one or more FHIR resources against 
 * the specification
 * 
 * @author Grahame
 *
 */
public class Validator {

  public static void main(String[] args) throws Exception {
    String output = null;
    if (args.length == 0) {
      System.out.println("FHIR Validation tool. ");
      System.out.println("");
      System.out.println("The FHIR validation tool validates a FHIR resource or bundle.");
      System.out.println("The validation tool compares a resource against the base definitions and whatever");
      System.out.println("profiles are declared in the resource or specificed on the command line");
      System.out.println("For XML: Schema and schematron checking is also performed");
      System.out.println("RDF/Json-LD are not supported at this time");
      System.out.println("");
      System.out.println("Usage: org.hl7.fhir.validator.jar [source] (-defn [definitions]) (-folder [name]) (-profile [profile]) (-questionnaire [questionnaire]) (-output [output]) (-tsserver [server]) where: ");
      System.out.println("* [source] is a file name or url of the resource or bundle feed to validate");
      System.out.println("* [definitions] is the file name or url of the validation pack (validation(-min).xml|json.zip)");
      System.out.println("* [folder] is the name of a folder containing additional structure definitions. You can have multiple folder parameters");
      System.out.println("* [txserver] is the url of a FHIR terminology service. Default is http://fhir3.healthintersections.com.au/open");
      System.out.println("* [profile] is an optional filename or URL for a specific profile to validate a resource");
      System.out.println("    against. In the absence of this parameter, the resource will be checked against the ");
      System.out.println("    base specification using the definitions specified.");
      System.out.println("* [questionnaire] is an optional filename or URL for a specific questionnaire to validate a ");
      System.out.println("    QuestionnaireResponse against, if it is nominated in the response");
      System.out.println("* [output] is a filename for the results (OperationOutcome). Default: results are sent to the std out.");
      System.out.println("");
      System.out.println("Alternatively, you can use this to execute a transformation as described by a structure map.");
      System.out.println("To do this, you must provide some additional parameters:");
      System.out.println("");
      System.out.println(" -transform -folder [folder] -map [map-file]");
      System.out.println("");
      System.out.println("* [map] the URI of the map that the transform starts with");
      System.out.println("");
      System.out.println("-transform requires the parameters -defn, -txserver, -folder (at least one with the map files), and -output");
    } else {
      if (args[0].equals("-profile-tests")) {
        String pack = null;
        String registry = null;
        for (int i = 0; i < args.length - 1; i++) {
          if (args[i].equals("-profile-tests"))
            registry = args[i+1];
          if (args[i].equals("-defn"))
            pack = args[i+1];
          	
        }
        ProfileValidatorTests tests = new ProfileValidatorTests(new File(pack), new File(registry));
        tests.execute();
      } else if (hasTransformParam(args)) {
        Transformer exe = new Transformer();
        exe.setSource(args[0]);
        for (int i = 1; i < args.length; i++) {
          if (args[i].equals("-defn"))
            exe.setDefinitions(args[i+1]);
          if (args[i].equals("-output"))
            exe.setOutput(args[i+1]);
          if (args[i].equals("-txserver"))
            exe.setTxServer(args[i+1]);
          if (args[i].equals("-folder"))
            exe.addFolder(args[i+1]);
          if (args[i].equals("-map"))
            exe.setMapUri(args[i+1]);
        }
        if (exe.process()) 
          System.out.println(" ...success");
        else
          System.out.println(" ...failure: "+exe.getMessage());
      } else { 
        Validator exe = new Validator();
        exe.setSource(args[0]);
        for (int i = 1; i < args.length; i++) {
          if (args[i].equals("-defn"))
            exe.setDefinitions(args[i+1]);
          if (args[i].equals("-output"))
            output = args[i+1];
          if (args[i].equals("-profile"))
            exe.setProfile(args[i+1]);
          if (args[i].equals("-questionnaire"))
            exe.setQuestionnaire(args[i+1]);
          if (args[i].equals("-txserver"))
            exe.setTsServer(args[i+1]);
          if (args[i].equals("-folder"))
            exe.addFolder(args[i+1]);
          if (args[i].equals("-noxslt"))
          	exe.engine.setNoSchematron(true);
        }
        exe.process();
        if (output == null) {
          System.out.println("Validating "+args[0]+": "+Integer.toString(exe.outputs().size())+" messages");
          for (ValidationMessage v : exe.outputs()) {
            System.out.println(v.summary());
          }
          int count = 0;
          for (ValidationMessage t : exe.outputs()) {
          	if (t.getLevel() == IssueSeverity.ERROR || t.getLevel() == IssueSeverity.FATAL)
          		count++;
          }
          if (count == 0)
            System.out.println(" ...success");
          else
            System.out.println(" ...failure");
        } else {
          FileOutputStream s = new FileOutputStream(output);
          new XmlParser().compose(s, exe.engine.getOutcome(), true);
          s.close();
        }
      }
    }
  }


	private static boolean hasTransformParam(String[] args) {
		for (String s : args) {
			if (s.equals("-transform"))
				return true;
		}
		return false;
	}


	private String txServer;



  private void setTsServer(String txServer) {
	  this.txServer = txServer;
	}



	private void setProfile(String profile) {
	  this.profile = profile;
  }

  private void setQuestionnaire(String questionnaire) {
    this.questionnaire = questionnaire;
  }
  

	private List<ValidationMessage> outputs() {
    return engine.getOutputs();
  }


  /**
   * The source (file name, folder name, url) of the FHIR validation pack. This can be the 
   * fhir url, an alternative url of a local copy of the fhir spec, the name of 
   * a zip file containing the fhir spec, the name of a directory containing the
   * fhir spec 
   */
  private String definitions;

  /**
   * Additional location to get structures from
   */
  private List<String> folders = new ArrayList<String>();
  
  /**
   * A specific profile against which to validate the instance (optional)
   */
  private String profile;

  private String questionnaire;

  /**
   * The name of the resource/feed to validate. this can be the actual source as json or xml, a file name, a zip file, 
   * or a url. If the source identifies a collection of resources and/or feeds, they
   * will all be validated
   */
  private String source;
  

  ValidationEngine engine = new ValidationEngine();

  public void process() throws Exception {
    engine.readDefinitions(definitions);
    for (String folder : folders)
      engine.getContext().loadFromFolder(folder);
    engine.connectToTSServer(txServer == null ? "http://fhir3.healthintersections.com.au/open" : txServer);
    engine.loadProfile(profile);
    engine.loadQuestionnaire(questionnaire);
    engine.setSource(loadSource());
    engine.process();
  }

  private byte[] loadSource() throws IOException {
    System.out.println("  .. load "+source);
    byte[] src;
    if (new File(source).exists())
      src = loadFromFile(source);
    else if (source.startsWith("https:") || source.startsWith("http:"))
      src = loadFromUrl(source);
    else 
      src = source.getBytes();
    return src;
  }

  private byte[] loadFromUrl(String src) throws IOException {
  	URL url = new URL(src);
    byte[] str = IOUtils.toByteArray(url.openStream());
    return str;
  }

  private byte[] loadFromFile(String src) throws IOException {
    FileInputStream in = new FileInputStream(src);
    byte[] b = new byte[in.available()];
    in.read(b);
    in.close();
    return b;
  }

 
  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }


  public String getOutcome() throws IOException {
    ByteArrayOutputStream b = new ByteArrayOutputStream();
    new XmlParser().compose(b, engine.getOutcome(), true); 
    b.close();
    return b.toString();
  }

  public String getDefinitions() {
    return definitions;
  }

  public void setDefinitions(String definitions) {
    this.definitions = definitions;
  }



  public List<String> getFolders() {
    return folders;
  }

  public void addFolder(String value) {
    folders.add(value);
  }


  
}
