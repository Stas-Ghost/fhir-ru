package org.hl7.fhir.dstu3.utils;

import java.util.*;

import org.hl7.fhir.dstu3.model.Element;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.StringUtils;

import org.stringtemplate.v4.*;

public class ShExGenerator {

  public enum HTMLLinkPolicy {
    NONE, EXTERNAL, INTERNAL
  }

  private static String SHEX_TEMPLATE = "$header$\n\n" +
          "$shapeDefinitions$";

  // A header is a list of prefixes plus a BASE
  private static String HEADER_TEMPLATE =
          "PREFIX fhir: <http://hl7.org/fhir/> \n" +
          "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
          "BASE <http://hl7.org/fhir/shape/>\n\n" +
          "start=<$id$>\n";

  // A shape definition template -- an id followed by an optional resource declaration (type + treeRoot) +
  // a list of definition elements
  private static String SHAPE_DEFINITION_TEMPLATE = "<$id$> {$resourceDecl$$elements$,\n\tfhir:index xsd:integer?\n}\n";

  // Additional declaration that appears only in a resource definition
  private static String RESOURCE_DECL_TEMPLATE = "\n\ta [fhir:$id$],\n\tfhir:nodeRole [fhir:treeRoot],\n\t";

  // An element definition within a shape
  private static String ELEMENT_TEMPLATE = "fhir:$id$ $defn$$card$";

  // A simple element definition
  private static String SIMPLE_ELEMENT_TEMPLATE = "@<$typ$>";

  // A primitive element definition
  private static String PRIMITIVE_ELEMENT_TEMPLATE = "xsd:$typ$";

  // A list of alternative shape definitions
  private static String ALTERNATIVE_TEMPLATE = "\n\t(\n\t\t$altEntries$\n\t)";

  // A typed reference definition
  private static String REFERENCE_TEMPLATE = "@<$ref$Reference>";

  // A choice of different predicate / types
  private static String CHOICE_TEMPLATE = "\n(\t$choiceEntries$\n\t)$card$";

  // What we emit for an xhtml
  private static String XHTML_TEMPLATE = "xsd:string";

  // Additional type for Coding
  private static String CODING_TEMPLATE = "fhir:uri IRI?";

  // A typed reference -- a fhir:uri with an optional type and the possibility of a resolvable shape
//  fhir:Element.id @<id>?,
//  fhir:Element.extension @<Extension>*,
//  fhir:Reference.reference @<string>?,
//  fhir:Reference.display @<string>?
  private static String TYPED_REFERENCE_TEMPLATE = "\n<$refType$Reference> {\n" +
                                                   "\ta [fhir:$refType$Reference]?,\n" +
                                                   "\tfhir:Element.id @<id>?,\n" +
                                                   "\tfhir:Element.extension @<Extension>*,\n" +
                                                   "\tfhir:link (@<$refType$> OR IRI)?,\n" +
                                                   "\tfhir:Reference.reference @<string>?,\n" +
                                                   "\tfhir:Reference.display @<string>?,\n" +
                                                   "\tfhir:index xsd:integer?\n" +
                                                   "}";

  // TODO: find the literal for this
  private static String XML_DEFN_TYPE = "http://hl7.org/fhir/StructureDefinition/structuredefinition-xml-type";

  /**
   * this makes internal metadata services available to the generator - retrieving structure definitions, and value set expansion etc
   */
  private IWorkerContext context;

  /**
   * innerTypes -- inner complex types.  Flattened in ShEx (doesn't have to be, btw)
   * emittedInnerTypes -- set of inner types that have been generated
   * datatypes, emittedDatatypes -- types used in the definition, types that have been generated
   * references -- Reference types (Patient, Specimen, etc)
   * doDataTypes -- whether or not to emit the data types.
   */
  private LinkedList<Pair<StructureDefinition, ElementDefinition>> innerTypes;
  private LinkedList<String> emittedInnerTypes;
  private HashSet<String> datatypes, emittedDatatypes;
  private HashSet<String> references;
  private boolean doDatatypes = true;

  public ShExGenerator(IWorkerContext context) {
    super();
    this.context = context;
    innerTypes = new LinkedList<Pair<StructureDefinition, ElementDefinition>>();
    emittedInnerTypes = new LinkedList<String>();
    datatypes = new HashSet<String>();
    emittedDatatypes = new HashSet<String>();
    references = new HashSet<String>();
  }
  
  public String generate(HTMLLinkPolicy links, StructureDefinition structure) {
    List<StructureDefinition> list = new ArrayList<StructureDefinition>();
    list.add(structure);
    innerTypes.clear();
    emittedInnerTypes.clear();
    datatypes.clear();
    emittedDatatypes.clear();
    references.clear();
    return generate(links, list);
  }
  
  public class SortById implements Comparator<StructureDefinition> {

    @Override
    public int compare(StructureDefinition arg0, StructureDefinition arg1) {
      return arg0.getId().compareTo(arg1.getId());
    }

  }

  private ST tmplt(String template) {
    return new ST(template, '$', '$');
  }

  /**
   * this is called externally to generate a set of structures to a single ShEx file
   * generally, it will be called with a single structure, or a long list of structures (all of them)
   *
   * @param links HTML link rendering policy
   * @param structures list of structure definitions to render
   * @return ShEx definition of structures
   */
  public String generate(HTMLLinkPolicy links, List<StructureDefinition> structures) {
    ST shex_def = tmplt(SHEX_TEMPLATE);
    shex_def.add("header", genHeader(structures.get(0).getId()));

    // Process the requested definitions
    Collections.sort(structures, new SortById());
    StringBuilder shapeDefinitions = new StringBuilder();
    for (StructureDefinition sd : structures) {
      shapeDefinitions.append(genShapeDefinition(sd));
    }

    // Add all of the inner types
    emitInnerTypes(shapeDefinitions);

    // Generate all of the referenced data types if requested
    while((doDatatypes && emittedDatatypes.size() < datatypes.size()) ||
            emittedInnerTypes.size() < innerTypes.size()) {
      emitDatatypes(shapeDefinitions);
      emitInnerTypes(shapeDefinitions);
    }

    // Add all of the reference types
    for(String r: references)
      shapeDefinitions.append("\n").append(genReferenceEntry(r)).append("\n");

    shex_def.add("shapeDefinitions", shapeDefinitions);
    return shex_def.render();
  }

  /**
   * Generate a reference to a typed fhir:uri
   * @param refType reference type name
   * @return reference
   */
  private String genReferenceEntry(String refType) {
    ST typed_ref = tmplt(TYPED_REFERENCE_TEMPLATE);
    typed_ref.add("refType", refType);
    return typed_ref.render();
  }

  /**
   * Generate a flattened definition for the inner types
   * @param shapeDefinitions shape definitions string
   */
  private void emitInnerTypes(StringBuilder shapeDefinitions) {
    while(emittedInnerTypes.size() < innerTypes.size()) {
      LinkedList<Pair<StructureDefinition, ElementDefinition>> sdedicopy =
              new LinkedList<Pair<StructureDefinition, ElementDefinition>>();
      for(Pair<StructureDefinition, ElementDefinition> e : innerTypes)
        sdedicopy.add(e);
      Iterator<Pair<StructureDefinition, ElementDefinition>> sdedi = sdedicopy.iterator();
      while(sdedi.hasNext()) {
        Pair<StructureDefinition, ElementDefinition> sded = sdedi.next();
        StructureDefinition sd = sded.getLeft();
        ElementDefinition ed = sded.getRight();
        if (!emittedInnerTypes.contains(ed.getPath()))
          shapeDefinitions.append("\n").append(genElementReference(sd, ed));
        emittedInnerTypes.add(ed.getPath());
      }
    }
  }

  /**
   * Generate a shape definition for the current set of datatypes
   * @param shapeDefinitions shape definitions string
   */
  private void emitDatatypes(StringBuilder shapeDefinitions) {
    while (emittedDatatypes.size() < datatypes.size()) {
      Iterator<String> dti = datatypes.iterator();
      while(dti.hasNext()) {
        String dt = dti.next();
        if (!emittedDatatypes.contains(dt)) {
          StructureDefinition sd = context.fetchResource(StructureDefinition.class,
                  "http://hl7.org/fhir/StructureDefinition/" + dt);
          if (sd != null)
            shapeDefinitions.append("\n").append(genShapeDefinition(sd));
          emittedDatatypes.add(dt);
          break;
        }
      }
    }
  }

  /**
   * Generate the ShEx Header
   * @return String representation
   */
  private String genHeader(String id) {
    return tmplt(HEADER_TEMPLATE).add("id", id).render();
  }

  /**
   * Emit a ShEx definition for the supplied StructureDefinition
   * @param sd Structure definition to emit
   * @return ShEx definition
   */
  private String genShapeDefinition(StructureDefinition sd) {
    ST resource_decl = tmplt(RESOURCE_DECL_TEMPLATE);
    ST struct_def = tmplt(SHAPE_DEFINITION_TEMPLATE);

    struct_def.add("resourceDecl", sd.getKind().name().equals("RESOURCE") ? resource_decl.add("id", sd.getId()).render() : "");

    struct_def.add("id", sd.getId());
    List<String> elements = new ArrayList<String>();

    if (sd.getName().equals("Coding"))
      elements.add(tmplt(CODING_TEMPLATE).render());

    for (ElementDefinition ed : sd.getSnapshot().getElement()) {
      if(StringUtils.countMatches(ed.getPath(), ".") == 1) {
        elements.add(genElementDefinition(sd, ed));
      }
    }
    struct_def.add("elements", StringUtils.join(elements, ",\n\t"));
    return struct_def.render();
  }

  /**
   * Generate a ShEx element definition
   * @param sd Containing structure definition
   * @param ed Containing element definition
   * @return ShEx definition
   */
  private String genElementDefinition(StructureDefinition sd, ElementDefinition ed) {
    ST element_def =  tmplt(ELEMENT_TEMPLATE);

    String id = ed.hasBase() ? ed.getBase().getPath() : ed.getPath();
    String shortId = id.substring(id.lastIndexOf(".") + 1);
    String card = "*".equals(ed.getMax()) ? (ed.getMin() == 0 ? "*" : "+") : "?";
    String defn;
    element_def.add("id", sd.getKind().toCode().equals("datatype")? shortId : id);
    element_def.add("card", card);
    
    List<ElementDefinition> children = ProfileUtilities.getChildList(sd, ed);
    if (children.size() > 0) {
      // inline anonymous type - give it a name and factor it out
      innerTypes.add(new ImmutablePair<StructureDefinition, ElementDefinition>(sd, ed));
      ST anon_link = tmplt(SIMPLE_ELEMENT_TEMPLATE);
      anon_link.add("typ", id);
      defn = anon_link.render();
    } else if (ed.getType().size() == 1) {
      // Single entry
      defn = genTypeRef(ed.getType().get(0));
    } else { 
      // multiple types
      // todo: figure out how to do this with the API
      if(id.endsWith("[x]")) {
        return genChoiceTypes(ed, id, shortId, card);
      } else {
        defn = genAlternativeTypes(ed, id, shortId, card);
      }
    }
    element_def.add("defn", defn);
    return element_def.render();
  }

  private String genTypeRef(ElementDefinition.TypeRefComponent typ) {
    ST single_entry = tmplt(SIMPLE_ELEMENT_TEMPLATE);

    if(typ.getProfile().size() > 0) {
      if (typ.getProfile().size() != 1) throw new AssertionError("Can't handle multiple profiles");
      single_entry.add("typ", getProfiledType(typ));
      // TODO: Figure out how to get into the interior codes
//    } else if (typ.code.hasExtension()) {
//        for (Extension ext : typ.getExtension()) {
//          if(ext.getUrl() == XML_DEFN_TYPE ) {
//            ST primitive_entry = tmplt(PRIMITIVE_ELEMENT_TEMPLATE);
//            primitive_entry.add("typ", ext.getValue());
//            return primitive_entry.render();
//          }
//        }
//        return "UNKNOWN";
      } else if (typ.getCode() == null) {
        ST primitive_entry = tmplt(PRIMITIVE_ELEMENT_TEMPLATE);
        primitive_entry.add("typ", "string");
        return primitive_entry.render();
      } else if(typ.getCode().equals("xhtml")) {
          return tmplt(XHTML_TEMPLATE).render();
      } else {
        single_entry.add("typ", typ.getCode());
        datatypes.add(typ.getCode());
      }
    return single_entry.render();
  }

  /**
   * Generate a set of alternative shapes
   * @param ed Containing element definition
   * @param id Element definition identifier
   * @param shortId id to use in the actual definition
   * @param card Cardinality
   * @return ShEx list of alternative anonymous shapes separated by "OR"
   */
  private String genAlternativeTypes(ElementDefinition ed, String id, String shortId, String card) {
    ST shex_alt = tmplt(ALTERNATIVE_TEMPLATE);
    List<String> altEntries = new ArrayList<String>();

    shex_alt.add("id", shortId);

    for(ElementDefinition.TypeRefComponent typ : ed.getType())  {
      altEntries.add(genAltEntry(id, typ).render());
    }
    shex_alt.add("altEntries", StringUtils.join(altEntries, " OR\n"));
    shex_alt.add("card", card);
    return shex_alt.render();
  }

  private ST genReference(String id, ElementDefinition.TypeRefComponent  typ) {
    ST shex_ref = tmplt(REFERENCE_TEMPLATE);

    // todo: There has to be a better way to do this
    String ref;
    if(typ.getProfile().size() > 0) {
      String[] els = typ.getProfile().get(0).getValue().split("/");
      ref = els[els.length - 1];
    } else {
      ref = "";
    }
    shex_ref.add("id", id);
    shex_ref.add("ref", ref);
    references.add(ref);
    return shex_ref;
  }

  /**
   * Generate an alternative shape for a reference
   * @param id reference name
   * @param typ shape type
   * @return ShEx equivalent
   */
  private ST genAltEntry(String id, ElementDefinition.TypeRefComponent typ) {
    if(!typ.getCode().equals("Reference"))
      throw new AssertionError("We do not handle " + typ.getCode() + " alternatives");

    return genReference(id, typ);
  }

  /**
   * Return the type definition for a profiled type
   * @param typ type to generate the entry for
   * @return string of type
   */
  private String getProfiledType(ElementDefinition.TypeRefComponent typ)
  {
    return typ.getProfile().get(0).fhirType();
  }

  /**
   * Generate a list of type choices for a "name[x]" style id
   * @param ed containing elmentdefinition
   * @param id choice identifier
   * @param shortId identifier for definition
   * @return ShEx fragment for the set of choices
   */
  private String genChoiceTypes(ElementDefinition ed, String id, String shortId, String card) {
    ST shex_choice = tmplt(CHOICE_TEMPLATE);
    List<String> choiceEntries = new ArrayList<String>();
    String base = shortId.replace("[x]", "");

    for(ElementDefinition.TypeRefComponent typ : ed.getType())  {
      if(typ.getCode().equals("Reference")) {
        ST entry = genAltEntry(base, typ);
        ST shex_choice_entry = tmplt(ELEMENT_TEMPLATE);
        shex_choice_entry.add("id", base+entry.getAttribute("ref"));
        shex_choice_entry.add("card", "");
        shex_choice_entry.add("defn", entry.render());
        choiceEntries.add(shex_choice_entry.render());
      }
      else
        choiceEntries.add(genChoiceEntry(base, typ));
    }
    shex_choice.add("choiceEntries", StringUtils.join(choiceEntries, " |\n\t\t"));
    shex_choice.add("card", card);
    return shex_choice.render();
  }

  /**
   * Generate an entry in a choice list
   * @param base base identifier
   * @param typ type/discriminant
   * @return ShEx fragment for choice entry
   */
  private String genChoiceEntry(String base, ElementDefinition.TypeRefComponent typ) {
    ST shex_choice_entry = tmplt(ELEMENT_TEMPLATE);

    String ext = typ.getCode();
    shex_choice_entry.add("id", base+ext);
    shex_choice_entry.add("card", "");
    shex_choice_entry.add("defn", genTypeRef(typ));
    return shex_choice_entry.render();
  }

  /**
   * Generate a definition for a referenced element
   * @param sd StructureDefinition in which the element was referenced
   * @param ed Contained element definition
   * @return ShEx representation of element reference
   */
  private String genElementReference(StructureDefinition sd, ElementDefinition ed) {
    ST element_reference = tmplt(SHAPE_DEFINITION_TEMPLATE);
    element_reference.add("resourceDecl", "");  // Not a resource
    element_reference.add("id", ed.getPath());
    List<String> elements = new ArrayList<String>();

    for (ElementDefinition child: ProfileUtilities.getChildList(sd, ed)) {
      elements.add(genElementDefinition(sd, child));
    }

    element_reference.add("elements", StringUtils.join(elements, ",\n\t"));
    return element_reference.render();
  }

}
