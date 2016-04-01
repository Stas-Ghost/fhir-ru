package org.hl7.fhir.tools.converters;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import org.hl7.fhir.dstu3.formats.IParser;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.CodeSystem.CodeSystemContactComponent;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetContactComponent;
import org.hl7.fhir.dstu3.terminologies.ValueSetUtilities;
import org.hl7.fhir.utilities.Utilities;

public class CodeSystemConvertor {

  private Map<String, CodeSystem> codeSystems;

  public CodeSystemConvertor(Map<String, CodeSystem> codeSystems) {
    super();
    this.codeSystems = codeSystems;
  }

  public void convert(IParser p, ValueSet vs, String name) throws Exception {
    String nname = name.replace("valueset-", "codesystem-");
    if (nname.equals(name))
      nname = Utilities.changeFileExt(name, "-cs.xml");
    if (new File(nname).exists()) {
      FileInputStream input = new FileInputStream(nname);
      CodeSystem cs = ValueSetUtilities.makeShareable((CodeSystem) p.parse(input));
      populate(cs, vs);
//      if (codeSystems.containsKey(cs.getUrl())) 
//        throw new Exception("Duplicate Code System: "+cs.getUrl());
      codeSystems.put(cs.getUrl(), cs);
    }    
  }

  public static void populate(CodeSystem cs, ValueSet vs) {
    if (vs.getUserData("filename") == null)
      throw new Error("No filename on vs "+vs.getUrl());
    if (!vs.hasName())
      throw new Error("No name vs "+vs.getUrl());
    if (!vs.hasDescription())
      throw new Error("No description vs "+vs.getUrl());
    
    if (cs.getUserData("conv-vs") != null)
      throw new Error("This code system has already been converted");
    cs.setUserData("conv-vs", "done");
    vs.setUserData("cs", cs);
    cs.setUserData("filename", vs.getUserString("filename").replace("valueset-", "codesystem-"));
    cs.setUserData("path", vs.getUserString("path").replace("valueset-", "codesystem-"));
    cs.setUserData("committee", vs.getUserData("committee"));
    cs.setId(vs.getId());
    cs.setVersion(vs.getVersion());
    cs.setName(vs.getName());
    cs.setStatus(vs.getStatus());
    cs.setExperimental(vs.getExperimental());
    cs.setPublisher(vs.getPublisher());
    for (ValueSetContactComponent csrc : vs.getContact()) {
      CodeSystemContactComponent ctgt = cs.addContact();
      ctgt.setName(csrc.getName());
      for (ContactPoint cc : csrc.getTelecom())
        ctgt.addTelecom(cc);
    }
    cs.setDate(vs.getDate());
    cs.setDescription(vs.getDescription());
    for (CodeableConcept cc : vs.getUseContext())
      cs.addUseContext(cc);
    cs.setRequirements(vs.getRequirements());
    cs.setCopyright(vs.getCopyright());
    cs.setValueSet(vs.getUrl());    
  }

}
