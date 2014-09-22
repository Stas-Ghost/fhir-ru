package org.hl7.fhir.definitions.generators.specification;
/*
Copyright (c) 2011-2014, HL7, Inc
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
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.definitions.model.BindingSpecification;
import org.hl7.fhir.definitions.model.Definitions;
import org.hl7.fhir.definitions.model.ElementDefn;
import org.hl7.fhir.definitions.model.Invariant;
import org.hl7.fhir.definitions.model.TypeRef;
import org.hl7.fhir.instance.formats.XmlComposer;
import org.hl7.fhir.instance.model.AtomEntry;
import org.hl7.fhir.instance.model.IdType;
import org.hl7.fhir.instance.model.Profile;
import org.hl7.fhir.instance.model.Profile.ElementComponent;
import org.hl7.fhir.instance.model.Profile.ElementDefinitionComponent;
import org.hl7.fhir.instance.model.Profile.ElementDefinitionConstraintComponent;
import org.hl7.fhir.instance.model.Profile.ElementDefinitionMappingComponent;
import org.hl7.fhir.instance.model.Profile.ProfileExtensionDefnComponent;
import org.hl7.fhir.instance.model.Profile.ProfileMappingComponent;
import org.hl7.fhir.instance.model.Profile.ProfileStructureComponent;
import org.hl7.fhir.instance.model.Profile.TypeRefComponent;
import org.hl7.fhir.instance.model.StringType;
import org.hl7.fhir.instance.model.Type;
import org.hl7.fhir.tools.publisher.PageProcessor;
import org.hl7.fhir.utilities.CommaSeparatedStringBuilder;
import org.hl7.fhir.utilities.Utilities;

import com.github.rjeschke.txtmark.Processor;

public class DictHTMLGenerator  extends OutputStreamWriter {

	private Definitions definitions;
	private PageProcessor page;
	
	public DictHTMLGenerator(OutputStream out, PageProcessor page) throws UnsupportedEncodingException {
		super(out, "UTF-8");
		this.definitions = page.getDefinitions();
		this.page = page;
	}

  public void generate(Profile profile) throws Exception {
    if (!profile.getExtensionDefn().isEmpty()) {
      write("<p><a name=\"i0\"><b>Extensions</b></a></p>\r\n");
      write("<table class=\"dict\">\r\n");
      
      for (ProfileExtensionDefnComponent e : profile.getExtensionDefn()) {
         generateExtension(profile, e);
      }
      write("</table>\r\n");
      
    }
    int i = 1;
    for (ProfileStructureComponent s : profile.getStructure()) {
      write("<p><a name=\"i"+Integer.toString(i)+"\"><b>"+s.getNameSimple()+"</b></a></p>\r\n");
      write("<table class=\"dict\">\r\n");
      
      for (ElementComponent ec : s.getSnapshot().getElement()) {
        if (isProfiledExtension(ec)) {
          String name = s.getNameSimple()+"."+ makePathLink(ec);
          String title = ec.getPathSimple() + " ("+(ec.getDefinition().getType().get(0).getProfileSimple().startsWith("#") ? profile.getUrlSimple() : "")+ec.getDefinition().getType().get(0).getProfileSimple()+")";
          write("  <tr><td colspan=\"2\" class=\"structure\"><a name=\""+name+"\"> </a><b>"+title+"</b></td></tr>\r\n");
          ElementDefinitionComponent extDefn = getExtensionDefinition(profile, ec.getDefinition().getType().get(0).getProfileSimple(), ec.getDefinition());
          generateElementInner(profile, extDefn);
        } else {
          String name = s.getNameSimple()+"."+ makePathLink(ec);
          String title = ec.getPathSimple() + (ec.getNameSimple() == null ? "" : "(" +ec.getNameSimple() +")");
          write("  <tr><td colspan=\"2\" class=\"structure\"><a name=\""+name+"\"> </a><b>"+title+"</b></td></tr>\r\n");
          generateElementInner(profile, ec.getDefinition());
        }
      }
      write("</table>\r\n");
      i++;      
    }
    flush();
    close();
  }

  private String makePathLink(ElementComponent element) {
    if (element.getName() == null)
      return element.getPathSimple();
    if (!element.getPathSimple().contains("."))
      return element.getNameSimple();
    return element.getPathSimple().substring(0, element.getPathSimple().lastIndexOf("."))+"."+element.getNameSimple();
  }
  
  private ElementDefinitionComponent getExtensionDefinition(Profile context, String url, ElementDefinitionComponent defaultDefn) {
    String code;
    if (url.startsWith("#")) {
      code = url.substring(1);
    } else {
      String[] parts = url.split("\\#");
      code = parts[1];
      context = definitions.getProfileByURL(parts[0]);
    }
    
    for (ProfileExtensionDefnComponent ext : context.getExtensionDefn()) {
      if (ext.getCodeSimple().equals(code))
        return ext.getElement().get(0).getDefinition();
    }

    return defaultDefn;
  }

  private boolean isProfiledExtension(ElementComponent ec) {
    return ec.getDefinition().getType().size() == 1 && ec.getDefinition().getType().get(0).getCodeSimple().equals("Extension") && ec.getDefinition().getType().get(0).getProfile() != null;
  }

  private void generateExtension(Profile profile, ProfileExtensionDefnComponent e) throws Exception {
    write("  <tr><td colspan=\"2\" class=\"structure\"><a name=\"extension."+e.getCodeSimple()+"\"> </a><b>Extension "+e.getCodeSimple()+"</b></td></tr>\r\n");
    generateElementInner(profile, e.getElement().get(0).getDefinition());
    if (e.getElement().size() > 1) {
      for (int i = 1; i < e.getElement().size(); i++) {
        ElementComponent ec = e.getElement().get(i);
        write("  <tr><td colspan=\"2\" class=\"structure\"><a name=\"extension."+ec.getPathSimple()+"\"> </a><b>&nbsp;"+ec.getPathSimple()+"</b></td></tr>\r\n");
        generateElementInner(profile, ec.getDefinition());
      }
    }
  }
    
  private void generateElementInner(Profile profile, ElementDefinitionComponent d) throws Exception {
    tableRowMarkdown("Определение", d.getFormalSimple());
    tableRow("Множество", "conformance-rules.html#conformance", describeCardinality(d) + summariseConditions(d.getCondition()));
    tableRowNE("Привязка", "terminologies.html", describeBinding(d));
    if (d.getNameReference() != null)
      tableRow("Тип", null, "See "+d.getNameReferenceSimple());
    else
      tableRowNE("Тип", "datatypes.html", describeTypes(d.getType()));
    tableRow("Это модификатор?", "conformance-rules.html#ismodifier", displayBoolean(d.getIsModifierSimple()));
    tableRow("Должен поддерживать", "conformance-rules.html#mustSupport", displayBoolean(d.getMustSupportSimple()));
    tableRowMarkdown("Требования", d.getRequirementsSimple());
    tableRow("Альтернативные имена", null, describeAliases(d.getSynonym()));
    tableRowMarkdown("Комментарии", d.getCommentsSimple());
    tableRow("Макс длина", null, d.getMaxLength() == null ? null : Integer.toString(d.getMaxLengthSimple()));
    tableRow("Фиксированное значение", null, encodeValue(d.getValue()));
    tableRow("Пример", null, encodeValue(d.getExample()));
    tableRowNE("Инварианты", null, invariants(d.getConstraint()));
    tableRow("LOINC-код", null, getMapping(profile, d, Definitions.LOINC_MAPPING));
    tableRow("SNOMED-CT-код", null, getMapping(profile, d, Definitions.SNOMED_MAPPING));
  }

  private String encodeValue(Type value) throws Exception {
    if (value == null)
      return null;
    ByteArrayOutputStream b = new ByteArrayOutputStream();
    new XmlComposer().compose(b, value);
    return b.toString();
  }

  private String describeTypes(List<TypeRefComponent> types) throws Exception {
    if (types.isEmpty())
      return null;
    StringBuilder b = new StringBuilder();
    if (types.size() == 1)
      describeType(b, types.get(0));
    else {
      boolean first = true;
      b.append("Choice of: ");
      for (TypeRefComponent t : types) {
        if (first)
          first = false;
        else
          b.append(", ");
        describeType(b, t);
      }
    }
    return b.toString();
  }

  private void describeType(StringBuilder b, TypeRefComponent t) throws Exception {
    b.append("<a href=\"");
    b.append(GeneratorUtils.getSrcFile(t.getCodeSimple(), false));
    b.append(".html#");
    String type = t.getCodeSimple();
    if (type.equals("*"))
      b.append("open");
    else 
      b.append(t.getCodeSimple());
    b.append("\">");
    b.append(t.getCodeSimple());
    b.append("</a>");
    if (t.getProfileSimple() != null) {
      b.append("<a href=\"todo.html\">");
      b.append("(Profile = "+t.getProfileSimple()+")");
      b.append("</a>");
      
    }
  }

  private String invariants(List<ElementDefinitionConstraintComponent> constraints) {
    if (constraints.isEmpty())
      return null;
    StringBuilder s = new StringBuilder();
    if (constraints.size() > 0) {
      s.append("<b>Определено на этом элементе</b><br/>\r\n");
      List<String> ids = new ArrayList<String>();
      for (ElementDefinitionConstraintComponent id : constraints)
        ids.add(id.getKeySimple());
      Collections.sort(ids);
      boolean b = false;
      for (String id : ids) {
        ElementDefinitionConstraintComponent inv = getConstraint(constraints, id);
        if (b)
          s.append("<br/>");
        else
          b = true;
        s.append("<b title=\"Formal Invariant Identifier\">Inv-"+id+"</b>: "+Utilities.escapeXml(inv.getHumanSimple())+" (xpath: "+Utilities.escapeXml(inv.getXpathSimple())+")");
      }
    }
    
    return s.toString();
  }

  private ElementDefinitionConstraintComponent getConstraint(List<ElementDefinitionConstraintComponent> constraints, String id) {
    for (ElementDefinitionConstraintComponent c : constraints)
      if (c.getKeySimple().equals(id))
        return c;
    return null;
  }

  private String describeAliases(List<StringType> synonym) {
    CommaSeparatedStringBuilder b = new CommaSeparatedStringBuilder();
    for (StringType s : synonym) 
      b.append(s.getValue());
    return b.toString();
  }

  private String getMapping(Profile profile, ElementDefinitionComponent d, String uri) {
    String id = null;
    for (ProfileMappingComponent m : profile.getMapping()) {
      if (m.getUriSimple().equals(uri))
        id = m.getIdentitySimple();
    }
    if (id == null)
      return null;
    for (ElementDefinitionMappingComponent m : d.getMapping()) {
      if (m.getIdentitySimple().equals(id))
        return m.getMapSimple();
    }
    return null;
  }

  private String summariseConditions(List<IdType> conditions) {
    if (conditions.isEmpty())
      return "";
    else
      return " ?";
  }

  private String describeCardinality(ElementDefinitionComponent d) {
    if (d.getMax() == null)
      return Integer.toString(d.getMinSimple()) + "..?";
    else
      return Integer.toString(d.getMinSimple()) + ".." + d.getMaxSimple();
  }

  public void generate(ElementDefn root) throws Exception
	{
		write("<table class=\"dict\">\r\n");
		writeEntry(root.getName(), "1..1", "", "", root);
		for (ElementDefn e : root.getElements()) {
		   generateElement(root.getName(), e);
		}
		write("</table>\r\n");
		write("\r\n");
		flush();
		close();
	}

	private void generateElement(String name, ElementDefn e) throws Exception {
		writeEntry(name+"."+e.getName(), e.describeCardinality(), describeType(e), e.getBindingName(), e);
		for (ElementDefn c : e.getElements())	{
		   generateElement(name+"."+e.getName(), c);
		}
	}

	private void writeEntry(String path, String cardinality, String type, String conceptDomain, ElementDefn e) throws Exception {
		write("  <tr><td colspan=\"2\" class=\"structure\"><a name=\""+path.replace("[", "_").replace("]", "_")+"\"> </a><b>"+path+"</b></td></tr>\r\n");
		tableRow("Definition", null, e.getDefinition());
		tableRow("Control", "conformance-rules.html#conformance", cardinality + (e.hasCondition() ? ": "+  e.getCondition(): ""));
		tableRowNE("Binding", "terminologies.html", describeBinding(e));
		if (!Utilities.noString(type) && type.startsWith("@"))
		  tableRowNE("Type", null, "<a href=\"#"+type.substring(1)+"\">See "+type.substring(1)+"</a>");
		else
		  tableRowNE("Type", "datatypes.html", type);
		tableRow("Is Modifier", "conformance-rules.html#ismodifier", displayBoolean(e.isModifier()));
		tableRowNE("Requirements", null, page.processMarkdown(e.getRequirements()));
    tableRow("Aliases", null, toSeperatedString(e.getAliases()));
    if (e.isSummaryItem())
      tableRow("Summary", "search.html#summary", Boolean.toString(e.isSummaryItem()));
    tableRow("Comments", null, e.getComments());
    tableRowNE("Invariants", null, invariants(e.getInvariants(), e.getStatedInvariants()));
    tableRow("LOINC Code", null, e.getMapping(Definitions.LOINC_MAPPING));
    tableRow("SNOMED-CT Code", null, e.getMapping(Definitions.SNOMED_MAPPING));
		tableRow("To Do", null, e.getTodo());
		if (e.getTasks().size() > 0) {
	    tableRowNE("gForge Tasks", null, tasks(e.getTasks()));
		}
	}
	
  private String tasks(List<String> tasks) {
    StringBuilder b = new StringBuilder();
    boolean first = true;
    for (String t : tasks) {
      if (first)
        first = false;
      else
        b.append(", ");
      b.append("<a href=\"http://gforge.hl7.org/gf/project/fhir/tracker/?action=TrackerItemEdit&amp;tracker_item_id=");
      b.append(t);
      b.append("\">");
      b.append(t);
      b.append("</a>");
    }
    return b.toString();
  }

  private String describeBinding(ElementDefn e) throws Exception {

	  if (!e.hasBinding())
	    return null;
	  
	  StringBuilder b = new StringBuilder();
	  BindingSpecification cd =  definitions.getBindingByName(e.getBindingName());
    b.append(cd.getName()+": ");
    b.append(TerminologyNotesGenerator.describeBinding(cd, page));
//    if (cd.getBinding() == Binding.Unbound)
//      b.append(" (Not Bound to any codes)");
//    else
//      b.append(" ("+(cd.getExtensibility() == null ? "--" : "<a href=\"terminologies.html#extensibility\">"+cd.getExtensibility().toString().toLowerCase())+"</a>/"+
//          "<a href=\"terminologies.html#conformance\">"+(cd.getBindingStrength() == null ? "--" : cd.getBindingStrength().toString().toLowerCase())+"</a>)");
    return b.toString();
  }

  private String describeBinding(ElementDefinitionComponent d) throws Exception {

    if (d.getBinding() == null)
      return null;
    else
      return TerminologyNotesGenerator.describeBinding(d.getBinding(), page);
  }

  private String invariants(Map<String, Invariant> invariants, List<Invariant> stated) {
	  StringBuilder s = new StringBuilder();
	  if (invariants.size() > 0) {
	    s.append("<b>Определено на этом элементе</b><br/>\r\n");
	    List<Integer> ids = new ArrayList<Integer>();
	    for (String id : invariants.keySet())
	      ids.add(Integer.parseInt(id));
	    Collections.sort(ids);
	    boolean b = false;
	    for (Integer i : ids) {
	      Invariant inv = invariants.get(i.toString());
	      if (b)
	        s.append("<br/>");
	      s.append("<b title=\"Formal Invariant Identifier\">Inv-"+i.toString()+"</b>: "+Utilities.escapeXml(inv.getEnglish())+" (xpath: "+Utilities.escapeXml(inv.getXpath())+")");
	      b = true;
	    }
	  }
    if (stated.size() > 0) {
      if (s.length() > 0)
        s.append("<br/>");
      s.append("<b>Влияет на этот элемент</b><br/>\r\n");
      boolean b = false;
      for (Invariant id : stated) {
        if (b)
          s.append("<br/>");
        s.append("<b>Инв-"+id.getId().toString()+"</b>: "+Utilities.escapeXml(id.getEnglish())+" (xpath: "+Utilities.escapeXml(id.getXpath())+")");
        b = true;
      }
    }
	  
    return s.toString();
  }

  private String toSeperatedString(List<String> list) {
	  if (list.size() == 0)
	    return "";
	  else {
	    StringBuilder s = new StringBuilder();
	    boolean first = true;
	    for (String v : list) {
	      if (!first)
	        s.append("; ");
	      first = false;
	      s.append(v);
	    }
	    return s.toString();
	  }
	  
  }

  private String displayBoolean(boolean mustUnderstand) {
		if (mustUnderstand)
			return "true";
		else
			return null;
	}

  private void tableRowMarkdown(String name, String value) throws Exception {
    String text;
    if (value == null)
      text = "";
    else {
      text = value.replace("||", "\r\n\r\n");
      while (text.contains("[[[")) {
        String left = text.substring(0, text.indexOf("[[["));
        String linkText = text.substring(text.indexOf("[[[")+3, text.indexOf("]]]"));
        String right = text.substring(text.indexOf("]]]")+3);
        String url = "";
        String[] parts = linkText.split("\\#");
        Profile p = definitions.getProfileByURL(parts[0]);
        if (p != null)
          url = p.getTag("filename")+".html";
        else if (definitions.hasResource(linkText)) {
          url = linkText.toLowerCase()+".html#";
        } else if (definitions.hasElementDefn(linkText)) {
          url = GeneratorUtils.getSrcFile(linkText, false)+".html#"+linkText;
        } else if (definitions.hasPrimitiveType(linkText)) {
          url = "datatypes.html#"+linkText;
        } else {
          System.out.println("Error: Unresolved logical URL "+linkText);
          //        throw new Exception("Unresolved logical URL "+url);
        }
        text = left+"["+linkText+"]("+url+")"+right;
      }
    }
    write("  <tr><td>"+name+"</td><td>"+Processor.process(Utilities.escapeXml(text))+"</td></tr>\r\n");
  }
	private void tableRow(String name, String defRef, String value) throws IOException {
		if (value != null && !"".equals(value)) {
		  if (defRef != null) 
	      write("  <tr><td><a href=\""+defRef+"\">"+name+"</a></td><td>"+Utilities.escapeXml(value)+"</td></tr>\r\n");
		  else
		    write("  <tr><td>"+name+"</td><td>"+Utilities.escapeXml(value)+"</td></tr>\r\n");
		}
	}

	
  private void tableRowNE(String name, String defRef, String value) throws IOException {
    if (value != null && !"".equals(value))
      if (defRef != null) 
        write("  <tr><td><a href=\""+defRef+"\">"+name+"</a></td><td>"+value+"</td></tr>\r\n");
      else
        write("  <tr><td>"+name+"</td><td>"+value+"</td></tr>\r\n");
  }


	private String describeType(ElementDefn e) throws Exception {
		StringBuilder b = new StringBuilder();
		boolean first = true;
		if (e.typeCode().startsWith("@")) {
      b.append("<a href=\"#"+e.typeCode().substring(1)+"\">See "+e.typeCode().substring(1)+"</a>");		  
		} else {
		  for (TypeRef t : e.getTypes())
		  {
		    if (!first)
		      b.append("|");
		    if (t.getName().equals("*"))
		      b.append("<a href=\"datatypes.html#open\">*</a>");
		    else
		      b.append("<a href=\""+typeLink(t.getName())+"\">"+t.getName()+"</a>");
		    if (t.hasParams()) {
		      b.append("(");
		      boolean firstp = true;
		      for (String p : t.getParams()) {
		        if (!firstp)
		          b.append(" | ");
		        if (definitions.getFutureResources().containsKey(p))
		          b.append("<span title=\"This resource is not been defined yet\">"+p+"</span>");
		        else
		          b.append("<a href=\""+typeLink(p)+"\">"+p+"</a>");
		        firstp = false;
		      }
		      b.append(")");
		    }		  first = false;
		  }
		}
		return b.toString();
	}

  private String typeLink(String name) throws Exception {
    String srcFile = GeneratorUtils.getSrcFile(name, false);
    if (srcFile.equalsIgnoreCase(name))
      return srcFile+ ".html";
    else
      return srcFile+ ".html#" + name;
  }
	
}
