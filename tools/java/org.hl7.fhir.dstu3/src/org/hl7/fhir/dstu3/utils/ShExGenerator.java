package org.hl7.fhir.dstu3.utils;

import java.util.*;

import org.hl7.fhir.dstu3.elementmodel.Property;
import org.hl7.fhir.dstu3.exceptions.FHIRException;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.elementmodel.TurtleParser;
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

  // A header is a list of prefixes, a base declaration and a start node
  private static String FHIR = "http://hl7.org/fhir/";
  private static String HEADER_TEMPLATE =
          "PREFIX fhir: <$fhir$> \n" +
          "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
          "BASE <http://hl7.org/fhir/shape/>\n$start$";

  private static String START_TEMPLATE = "\nstart=<$id$>\n";

  // Shape Definition
  //      the shape name
  //      an optional resource declaration (type + treeRoot)
  //      the list of element declarations
  //      an optional index element (for appearances inside ordered lists)
  private static String SHAPE_DEFINITION_TEMPLATE =
          "$comment$\n<$id$> CLOSED {\n    $resourceDecl$" +
          "\n    $elements$" +
          "\n    fhir:index xsd:integer?\n}\n";

  // Resource Definition
  //      an open shape of type Resource
  private static String RESOURCE_SHAPE_TEMPLATE =
          "$comment$\n<$id$> {a .+;" +
          "\n    $elements$" +
          "\n    fhir:index xsd:integer?" +
          "\n}\n";

  // Resource Declaration
  //      a type node
  //      an optional treeRoot declaration (identifies the entry point)
  private static String RESOURCE_DECL_TEMPLATE = "\na [fhir:$id$];$root$";

  // Root Declaration
  private static String ROOT_TEMPLATE = "\nfhir:nodeRole [fhir:treeRoot];";

  // Element
  //    a predicate, type and cardinality triple
  private static String ELEMENT_TEMPLATE = "fhir:$id$ $defn$$card$$comment$";
  private static int COMMENT_COL = 40;
  private static int MAX_CHARS = 35;
  private static int MIN_COMMENT_SEP = 2;

  // Inner Shape Definition
  private static String INNER_SHAPE_TEMPLATE = "($comment$\n    $defn$\n)$card$";

  // Simple Element
  //    a shape reference
  private static String SIMPLE_ELEMENT_DEFN_TEMPLATE = "@<$typ$>";

  // A primitive element definition
  //    the actual type reference
  private static String PRIMITIVE_ELEMENT_DEFN_TEMPLATE = "$typ$$facets$";

  // Facets
  private static String MINVALUE_TEMPLATE = " MININCLUSIVE $val$";
  private static String MAXVALUE_TEMPLATE = " MAXINCLUSIVE $val$";
  private static String MAXLENGTH_TEMPLATE = " MAXLENGTH $val$";
  private static String PATTERN_TEMPLATE = " PATTERN \"$val$\"";

  // A choice of alternative shape definitions
  //  rendered as an inner anonymous shape
  private static String ALTERNATIVE_SHAPES_TEMPLATE = "fhir:$id$$comment$\n(   $altEntries$\n)$card$";

  // A typed reference definition
  private static String REFERENCE_DEFN_TEMPLATE = "@<$ref$Reference>";

  // What we emit for an xhtml
  private static String XHTML_TYPE_TEMPLATE = "xsd:string";

  // Additional type for Coding
  private static String CONCEPT_REFERENCE_TEMPLATE = "fhir:concept IRI?;";

  // Additional type for CodedConcept -- still under debate
  private static String CONCEPT_REFERENCES_TEMPLATE = "fhir:concept IRI*;";

  // Untyped resource has the extra link entry
  private static String RESOURCE_LINK_TEMPLATE = "fhir:link IRI?;";

  // Extension template
  private static String EXTENSION_TEMPLATE = "<Extension> {fhir:extension @<Extension>*;" +
          "\n    fhir:index xsd:integer?" +
          "\n}\n";

  // A typed reference -- a fhir:uri with an optional type and the possibility of a resolvable shape
  private static String TYPED_REFERENCE_TEMPLATE = "\n<$refType$Reference> CLOSED {" +
                                                   "\n    fhir:Element.id @<id>?;" +
                                                   "\n    fhir:extension @<Extension>*;" +
                                                   "\n    fhir:link IRI?;" +
                                                   "\n    fhir:Reference.reference @<string>?;" +
                                                   "\n    fhir:Reference.display @<string>?;" +
                                                   "\n    fhir:index xsd:integer?" +
                                                   "\n}";

  /**
   * this makes internal metadata services available to the generator - retrieving structure definitions, and value set expansion etc
   */
  private IWorkerContext context;

  /**
   * innerTypes -- inner complex types.  Currently flattened in ShEx (doesn't have to be, btw)
   * emittedInnerTypes -- set of inner types that have been generated
   * datatypes, emittedDatatypes -- types used in the definition, types that have been generated
   * references -- Reference types (Patient, Specimen, etc)
   * uniq_structures -- set of structures on the to be generated list...
   * doDataTypes -- whether or not to emit the data types.
   */
  private HashSet<Pair<StructureDefinition, ElementDefinition>> innerTypes, emittedInnerTypes;
  private HashSet<String> datatypes, emittedDatatypes;
  private HashSet<String> references;
  private LinkedList<StructureDefinition> uniq_structures;
  private HashSet<String> uniq_structure_urls;
  public boolean doDatatypes = true;                 // add data types
  public boolean withComments = true;                // include comments
  public boolean completeModel = false;              // doing complete build (fhir.shex)

  public ShExGenerator(IWorkerContext context) {
    super();
    this.context = context;
    innerTypes = new HashSet<Pair<StructureDefinition, ElementDefinition>>();
    emittedInnerTypes = new HashSet<Pair<StructureDefinition, ElementDefinition>>();
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
    String start_cmd = completeModel? tmplt(START_TEMPLATE).add("id", structures.get(0).getId()).render() : " ";
    shex_def.add("header", tmplt(HEADER_TEMPLATE).add("start", start_cmd).add("fhir", FHIR).render());

    Collections.sort(structures, new SortById());
    StringBuilder shapeDefinitions = new StringBuilder();

    // For unknown reasons, the list of structures carries duplicates.  We remove them
    // Also, it is possible for the same sd to have multiple hashes...
    uniq_structures = new LinkedList<StructureDefinition>();
    uniq_structure_urls = new HashSet<String>();
    for (StructureDefinition sd : structures) {
      if (!uniq_structure_urls.contains(sd.getUrl())) {
        uniq_structures.add(sd);
        uniq_structure_urls.add(sd.getUrl());
      }
    }


    for (StructureDefinition sd : uniq_structures) {
      shapeDefinitions.append(genShapeDefinition(sd, true));
    }

    shapeDefinitions.append(emitInnerTypes());
    if(doDatatypes) {
      shapeDefinitions.append("\n#---------------------- Data Types -------------------\n");
      while (emittedDatatypes.size() < datatypes.size() ||
              emittedInnerTypes.size() < innerTypes.size()) {
        shapeDefinitions.append(emitDataTypes());
        shapeDefinitions.append(emitInnerTypes());
      }
    }

    shapeDefinitions.append("\n#---------------------- Reference Types -------------------\n");
    for(String r: references)
      shapeDefinitions.append("\n").append(tmplt(TYPED_REFERENCE_TEMPLATE).add("refType", r).render()).append("\n");

    shex_def.add("shapeDefinitions", shapeDefinitions);
    return shex_def.render();
  }


  /**
   * Emit a ShEx definition for the supplied StructureDefinition
   * @param sd Structure definition to emit
   * @param top_level True means outermost type, False means recursively called
   * @return ShEx definition
   */
  private String genShapeDefinition(StructureDefinition sd, boolean top_level) {
    if("xhtml".equals(sd.getName()))
      return "";
    ST shape_defn = tmplt("Resource".equals(sd.getName())? RESOURCE_SHAPE_TEMPLATE : SHAPE_DEFINITION_TEMPLATE);
    shape_defn.add("id", sd.getId());

    if(!"Resource".equals(sd.getName())) {
      if (sd.getKind().name().equals("RESOURCE")) {
        ST resource_decl = tmplt(RESOURCE_DECL_TEMPLATE).
                add("id", sd.getId()).
                add("root", top_level ? tmplt(ROOT_TEMPLATE) : "");
        shape_defn.add("resourceDecl", resource_decl.render());
      } else {
        shape_defn.add("resourceDecl", "");
      }
    }

    // Generate the defining elements
    List<String> elements = new ArrayList<String>();

    // Add the additional entries for special types
    String sdn = sd.getName();
    if (sdn.equals("Coding"))
      elements.add(tmplt(CONCEPT_REFERENCE_TEMPLATE).render());
    else if (sdn.equals("CodeableConcept"))
      elements.add(tmplt(CONCEPT_REFERENCES_TEMPLATE).render());
    else if (sdn.equals("Reference"))
      elements.add(tmplt(RESOURCE_LINK_TEMPLATE).render());
    else if (sdn.equals("Extension"))
      return tmplt(EXTENSION_TEMPLATE).render();

    String root_comment = null;
    for (ElementDefinition ed : sd.getSnapshot().getElement()) {
      if(!ed.getPath().contains("."))
        root_comment = ed.getShort();
      else if (StringUtils.countMatches(ed.getPath(), ".") == 1 && !"0".equals(ed.getMax())) {
        elements.add(genElementDefinition(sd, ed));
      }
    }
    shape_defn.add("elements", StringUtils.join(elements, "\n"));
    shape_defn.add("comment", root_comment == null? " " : "# " + root_comment);
    return shape_defn.render();
  }


  /**
   * Generate a flattened definition for the inner types
   * @return stringified inner type definitions
   */
  private String emitInnerTypes() {
    StringBuilder itDefs = new StringBuilder();
    while(emittedInnerTypes.size() < innerTypes.size()) {
      for (Pair<StructureDefinition, ElementDefinition> it : new HashSet<Pair<StructureDefinition, ElementDefinition>>(innerTypes)) {
        if (!emittedInnerTypes.contains(it)) {
          itDefs.append("\n").append(genInnerTypeDef(it.getLeft(), it.getRight()));
          emittedInnerTypes.add(it);
        }
      }
    }
    return itDefs.toString();
  }

  /**
   * Generate a shape definition for the current set of datatypes
   * @return stringified data type definitions
   */
  private String emitDataTypes() {
    StringBuilder dtDefs = new StringBuilder();
    while (emittedDatatypes.size() < datatypes.size()) {
      for (String dt : new HashSet<String>(datatypes)) {
        if (!emittedDatatypes.contains(dt)) {
          StructureDefinition sd = context.fetchResource(StructureDefinition.class,
                  "http://hl7.org/fhir/StructureDefinition/" + dt);
          // TODO: Figure out why the line below doesn't work
          // if (sd != null && !uniq_structures.contains(sd))
          if(sd != null && !uniq_structure_urls.contains(sd.getUrl()))
            dtDefs.append("\n").append(genShapeDefinition(sd, false));
          emittedDatatypes.add(dt);
        }
      }
    }
    return dtDefs.toString();
  }

  private ArrayList<String> split_text(String text, int max_col) {
    int pos = 0;
    ArrayList<String> rval = new ArrayList<String>();
    if (text.length() <= max_col) {
      rval.add(text);
    } else {
      String[] words = text.split(" ");
      int word_idx = 0;
      while(word_idx < words.length) {
        StringBuilder accum = new StringBuilder();
        while (word_idx < words.length && accum.length() + words[word_idx].length() < max_col)
          accum.append(words[word_idx++] + " ");
        if (accum.length() == 0) {
          accum.append(words[word_idx].substring(0, max_col - 3) + "-");
          words[word_idx] = words[word_idx].substring(max_col - 3);
        }
        rval.add(accum.toString());
        accum = new StringBuilder();
      }
    }
    return rval;
  }

  private void addComment(ST tmplt, ElementDefinition ed) {
    if(withComments && ed.hasShort()) {
      int nspaces;
      char[] sep;
      nspaces = Integer.max(COMMENT_COL - tmplt.add("comment", "#").render().indexOf('#'), MIN_COMMENT_SEP);
      tmplt.remove("comment");
      sep = new char[nspaces];
      Arrays.fill(sep, ' ');
      ArrayList<String> comment_lines = split_text(ed.getShort(), MAX_CHARS);
      StringBuilder comment = new StringBuilder("# ");
      char[] indent = new char[COMMENT_COL];
      Arrays.fill(indent, ' ');
      for(int i = 0; i < comment_lines.size();) {
        comment.append(comment_lines.get(i++));
        if(i < comment_lines.size())
          comment.append("\n" + new String(indent) + "# ");
      }
      tmplt.add("comment", new String(sep) + comment.toString());
    } else {
      tmplt.add("comment", " ");
    }
  }


  /**
   * Generate a ShEx element definition
   * @param sd Containing structure definition
   * @param ed Containing element definition
   * @return ShEx definition
   */
  private String genElementDefinition(StructureDefinition sd, ElementDefinition ed) {
    String id = ed.hasBase() ? ed.getBase().getPath() : ed.getPath();
    String shortId = id.substring(id.lastIndexOf(".") + 1);
    String defn;
    ST element_def;
    String card = ("*".equals(ed.getMax()) ? (ed.getMin() == 0 ? "*" : "+") : (ed.getMin() == 0 ? "?" : "")) + ";";

    if(id.endsWith("[x]")) {
      element_def = tmplt(INNER_SHAPE_TEMPLATE);
    } else {
      element_def = tmplt(ELEMENT_TEMPLATE);
      element_def.add("id", id.charAt(0) == id.toLowerCase().charAt(0) ||
              (ed.hasType() && "Extension".equals(ed.getType().get(0).getCode()))? shortId : id);
    }

    List<ElementDefinition> children = ProfileUtilities.getChildList(sd, ed);
    if (children.size() > 0) {
      innerTypes.add(new ImmutablePair<StructureDefinition, ElementDefinition>(sd, ed));
      defn = tmplt(SIMPLE_ELEMENT_DEFN_TEMPLATE).add("typ", id).render();
    } else if (ed.getType().size() == 1) {
      // Single entry
      defn = genTypeRef(sd, ed, id, ed.getType().get(0));
    } else if (ed.getContentReference() != null) {
      // Reference to another element
      String ref = ed.getContentReference();
      if(!ref.startsWith("#"))
        throw new AssertionError("Not equipped to deal with absolute path references: " + ref);
      String refPath = null;
      for(ElementDefinition ed1: sd.getSnapshot().getElement()) {
        if(ed1.getId() != null && ed1.getId().equals(ref.substring(1))) {
          refPath = ed1.getPath();
          break;
        }
      }
      if(refPath == null)
        throw new AssertionError("Reference path not found: " + ref);
      // String typ = id.substring(0, id.indexOf(".") + 1) + ed.getContentReference().substring(1);
      defn = tmplt(SIMPLE_ELEMENT_DEFN_TEMPLATE).add("typ", refPath).render();
    } else if(id.endsWith("[x]")) {
      defn = genChoiceTypes(sd, ed, id);
    } else {
      // TODO: Refactoring required here
      element_def = genAlternativeTypes(ed, id, shortId);
      element_def.add("id", id.charAt(0) == id.toLowerCase().charAt(0) ||
              (ed.hasType() && "Extension".equals(ed.getType().get(0).getCode()))? shortId : id);
      element_def.add("card", card);
      addComment(element_def, ed);
      return element_def.render();
    }
    element_def.add("defn", defn);
    element_def.add("card", card);
    addComment(element_def, ed);
    return element_def.render();
  }

  /**
   * Generate a type reference
   * @param sd Containing structure definition
   * @param ed Containing element definition
   * @param id Element id
   * @param typ Element type
   * @return Type reference string
   */
  private String genTypeRef(StructureDefinition sd, ElementDefinition ed, String id, ElementDefinition.TypeRefComponent typ) {

    if(typ.hasProfile()) {
      if(typ.getCode().equals("Reference"))
        return genReference("", typ);
      else if(ProfileUtilities.getChildList(sd, ed).size() > 0) {
        // inline anonymous type - give it a name and factor it out
        innerTypes.add(new ImmutablePair<StructureDefinition, ElementDefinition>(sd, ed));
        return tmplt(SIMPLE_ELEMENT_DEFN_TEMPLATE).add("typ", id).render();
      }
      else {
        String ref = getTypeName(typ);
        datatypes.add(ref);
        return tmplt(SIMPLE_ELEMENT_DEFN_TEMPLATE).add("typ", ref).render();
      }

    } else if (typ.getCodeElement().getExtensionsByUrl(ToolingExtensions.EXT_RDF_TYPE).size() > 0) {
      String xt = null;
      try {
        xt = typ.getCodeElement().getExtensionString(ToolingExtensions.EXT_RDF_TYPE);
      } catch (FHIRException e) {
        e.printStackTrace();
      }
      // TODO: Remove the next line when the type of token gets switched to string
      ST td_entry = tmplt(PRIMITIVE_ELEMENT_DEFN_TEMPLATE).add("typ", xt.replace("xsd:token", "xsd:string"));
      StringBuilder facets =  new StringBuilder();
      if(ed.hasMinValue()) {
        Type mv = ed.getMinValue();
        facets.append(tmplt(MINVALUE_TEMPLATE).add("val", TurtleParser.ttlLiteral(mv.primitiveValue(), mv.fhirType())).render());
      }
      if(ed.hasMaxValue()) {
        Type mv = ed.getMaxValue();
        facets.append(tmplt(MAXVALUE_TEMPLATE).add("val", TurtleParser.ttlLiteral(mv.primitiveValue(), mv.fhirType())).render());
      }
      if(ed.hasMaxLength()) {
        int ml = ed.getMaxLength();
        facets.append(tmplt(MAXLENGTH_TEMPLATE).add("val", ml).render());
      }
      if(ed.hasPattern()) {
        Type pat = ed.getPattern();
        facets.append(tmplt(PATTERN_TEMPLATE).add("val",pat.primitiveValue()).render());
      }
      td_entry.add("facets", facets.toString());
      return td_entry.render();

    } else if (typ.getCode() == null) {
      ST primitive_entry = tmplt(PRIMITIVE_ELEMENT_DEFN_TEMPLATE);
      primitive_entry.add("typ", "xsd:string");
      return primitive_entry.render();

    } else if(typ.getCode().equals("xhtml")) {
        return tmplt(XHTML_TYPE_TEMPLATE).render();
    } else {
      datatypes.add(typ.getCode());
      return tmplt(SIMPLE_ELEMENT_DEFN_TEMPLATE).add("typ", typ.getCode()).render();
    }
  }

  /**
   * Generate a set of alternative shapes
   * @param ed Containing element definition
   * @param id Element definition identifier
   * @param shortId id to use in the actual definition
   * @return ShEx list of alternative anonymous shapes separated by "OR"
   */
  private ST genAlternativeTypes(ElementDefinition ed, String id, String shortId) {
    ST shex_alt = tmplt(ALTERNATIVE_SHAPES_TEMPLATE);
    List<String> altEntries = new ArrayList<String>();


    for(ElementDefinition.TypeRefComponent typ : ed.getType())  {
      altEntries.add(genAltEntry(id, typ));
    }
    shex_alt.add("altEntries", StringUtils.join(altEntries, " OR\n    "));
    return shex_alt;
  }



  /**
   * Generate an alternative shape for a reference
   * @param id reference name
   * @param typ shape type
   * @return ShEx equivalent
   */
  private String genAltEntry(String id, ElementDefinition.TypeRefComponent typ) {
    if(!typ.getCode().equals("Reference"))
      throw new AssertionError("We do not handle " + typ.getCode() + " alternatives");

    return genReference(id, typ);
  }

  /**
   * Generate a list of type choices for a "name[x]" style id
   * @param sd Structure containing ed
   * @param ed element definition
   * @param id choice identifier
   * @return ShEx fragment for the set of choices
   */
  private String genChoiceTypes(StructureDefinition sd, ElementDefinition ed, String id) {
    List<String> choiceEntries = new ArrayList<String>();
    String base = id.replace("[x]", "");

    for(ElementDefinition.TypeRefComponent typ : ed.getType())
      choiceEntries.add(genChoiceEntry(sd, ed, id, base, typ));

    return StringUtils.join(choiceEntries, " |\n");
  }

  /**
   * Generate an entry in a choice list
   * @param base base identifier
   * @param typ type/discriminant
   * @return ShEx fragment for choice entry
   */
  private String genChoiceEntry(StructureDefinition sd, ElementDefinition ed, String id, String base, ElementDefinition.TypeRefComponent typ) {
    ST shex_choice_entry = tmplt(ELEMENT_TEMPLATE);

    String ext = typ.getCode();
    shex_choice_entry.add("id", base+Character.toUpperCase(ext.charAt(0)) + ext.substring(1));
    shex_choice_entry.add("card", ";");
    shex_choice_entry.add("defn", genTypeRef(sd, ed, id, typ));
    shex_choice_entry.add("comment", " ");
    return shex_choice_entry.render();
  }

  /**
   * Generate a definition for a referenced element
   * @param sd Containing structure definition
   * @param ed Inner element
   * @return ShEx representation of element reference
   */
  private String genInnerTypeDef(StructureDefinition sd, ElementDefinition ed) {
    String path = ed.hasBase() ? ed.getBase().getPath() : ed.getPath();;
    ST element_reference = tmplt(SHAPE_DEFINITION_TEMPLATE);
    element_reference.add("resourceDecl", "");  // Not a resource
    element_reference.add("id", path);
    String comment = ed.getShort();
    element_reference.add("comment", comment == null? " " : "# " + comment);

    List<String> elements = new ArrayList<String>();
    for (ElementDefinition child: ProfileUtilities.getChildList(sd, path))
      elements.add(genElementDefinition(sd, child));

    element_reference.add("elements", StringUtils.join(elements, "\n"));
    return element_reference.render();
  }

  /**
   * Generate a reference to a resource
   * @param id attribute identifier
   * @param typ possible reference types
   * @return string that represents the result
   */
  private String genReference(String id, ElementDefinition.TypeRefComponent typ) {
    ST shex_ref = tmplt(REFERENCE_DEFN_TEMPLATE);

    String ref = getTypeName(typ);
    shex_ref.add("id", id);
    shex_ref.add("ref", ref);
    references.add(ref);
    return shex_ref.render();
  }

	/**
     * Return the type name for typ
     * @param typ type to get name for
     * @return name
     */
  private String getTypeName(ElementDefinition.TypeRefComponent typ) {
    // TODO: This is brittle. There has to be a utility to do this...
    if(typ.hasProfile()) {
      String[] els = typ.getProfile().split("/");
      return els[els.length - 1];
    } else {
      return typ.getCode();
    }
  }
}
