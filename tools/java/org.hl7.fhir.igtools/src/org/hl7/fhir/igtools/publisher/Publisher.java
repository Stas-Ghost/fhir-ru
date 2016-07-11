package org.hl7.fhir.igtools.publisher;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.UIManager;

import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.SystemUtils;
import org.hl7.fhir.dstu3.elementmodel.Element;
import org.hl7.fhir.dstu3.elementmodel.Manager.FhirFormat;
import org.hl7.fhir.dstu3.elementmodel.ObjectConverter;
import org.hl7.fhir.dstu3.elementmodel.ParserBase.ValidationPolicy;
import org.hl7.fhir.dstu3.exceptions.FHIRException;
import org.hl7.fhir.dstu3.formats.IParser.OutputStyle;
import org.hl7.fhir.dstu3.formats.JsonParser;
import org.hl7.fhir.dstu3.formats.XmlParser;
import org.hl7.fhir.dstu3.model.BaseConformance;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.ConceptMap;
import org.hl7.fhir.dstu3.model.Constants;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.ImplementationGuide;
import org.hl7.fhir.dstu3.model.ImplementationGuide.ImplementationGuidePackageComponent;
import org.hl7.fhir.dstu3.model.ImplementationGuide.ImplementationGuidePackageResourceComponent;
import org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.StructureMap;
import org.hl7.fhir.dstu3.model.StructureDefinition.StructureDefinitionKind;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.dstu3.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.dstu3.terminologies.ValueSetExpander.ValueSetExpansionOutcome;
import org.hl7.fhir.dstu3.utils.EOperationOutcome;
import org.hl7.fhir.dstu3.utils.NarrativeGenerator;
import org.hl7.fhir.dstu3.utils.ProfileUtilities;
import org.hl7.fhir.dstu3.utils.SimpleWorkerContext;
import org.hl7.fhir.dstu3.utils.StructureMapUtilities;
import org.hl7.fhir.dstu3.utils.Turtle;
import org.hl7.fhir.dstu3.validation.InstanceValidator;
import org.hl7.fhir.dstu3.validation.ValidationMessage;
import org.hl7.fhir.igtools.renderers.BaseRenderer;
import org.hl7.fhir.igtools.renderers.CodeSystemRenderer;
import org.hl7.fhir.igtools.renderers.JsonXhtmlRenderer;
import org.hl7.fhir.igtools.renderers.StructureDefinitionRenderer;
import org.hl7.fhir.igtools.renderers.ValidationPresenter;
import org.hl7.fhir.igtools.renderers.ValueSetRenderer;
import org.hl7.fhir.igtools.renderers.XmlXHtmlRenderer;
import org.hl7.fhir.igtools.spreadsheets.IgSpreadsheetParser;
import org.hl7.fhir.igtools.ui.GraphicalPublisher;
import org.hl7.fhir.utilities.CSFile;
import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.ZipGenerator;
import org.hl7.fhir.utilities.xhtml.XhtmlComposer;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Implementation Guide Publisher
 * 
 * If you want to use this inside a FHIR server, and not to access content 
 * on a local folder, provide your own implementation of the file fetcher
 * 
 * rough sequence of activities:
 * 
 *   load the context using the internal validation pack
 *   connect to the terminology service
 *   
 *   parse the implementation guide
 *   find all the source files and determine the resource type
 *   load resources in this order:
 *     naming system
 *     code system
 *     value set 
 *     data element?
 *     structure definition
 *     concept map
 *     structure map
 *      
 *   validate all source files (including the IG itself)
 *   
 *   for each source file:
 *     generate all outputs
 *     
 *   generate summary file
 *   
 *   
 * @author Grahame Grieve
 *
 */

public class Publisher implements IGLogger {

  public static final boolean USE_COMMONS_EXEC = true;
  
  public enum GenerationTool {
    Jekyll
  }

  private static final String IG_NAME = "!ig!";

  private String configFile;
  private String txServer = "http://fhir3.healthintersections.com.au/open";
//  private String txServer = "http://local.healthintersections.com.au:960/open";
  private boolean watch;

  private GenerationTool tool;

  private String resourcesDir;
  private String pagesDir;
  private String tempDir;
  private String outputDir;
  private String specPath;
  private String qaDir;
  private String rubyExe;
  private String jekyllGem;

  private String igName;


  private IFetchFile fetcher = new SimpleFetcher();
  private SimpleWorkerContext context;
  private InstanceValidator validator;
  private IGKnowledgeProvider igpkp;
  private JsonObject specDetails;
  private boolean first;

  private Map<ImplementationGuidePackageResourceComponent, FetchedFile> fileMap = new HashMap<ImplementationGuidePackageResourceComponent, FetchedFile>();
  private Map<String, FetchedFile> altMap = new HashMap<String, FetchedFile>();
  private List<FetchedFile> fileList = new ArrayList<FetchedFile>();
  private List<FetchedFile> changeList = new ArrayList<FetchedFile>();
  private Set<String> bndIds = new HashSet<String>();
  private List<Resource> loaded = new ArrayList<Resource>();
  private ImplementationGuide ig;
  private List<ValidationMessage> errors = new ArrayList<ValidationMessage>();
  private JsonObject configuration;
  private Calendar execTime = Calendar.getInstance();
  private Set<String> otherFilesStartup = new HashSet<String>();
  private Set<String> otherFilesRun = new HashSet<String>();

  private long globalStart;

  private IGLogger logger = this;

  public void execute(boolean clearCache) throws Exception {
    globalStart = System.nanoTime();
    initialize(clearCache);
    log("Load Implementation Guide");
    load();

    long startTime = System.nanoTime();
    log("Processing Conformance Resources");
    loadConformance();
    log("Generating Narratives");
    generateNarratives();
    log("Validating Resources");
    validate();
    log("Generating Outputs in "+outputDir);
    generate();
    long endTime = System.nanoTime();
    clean();
    log(" ... finished. "+presentDuration(endTime - startTime)+". Validation output in "+new ValidationPresenter(context).generate(ig.getName(), errors, fileList, Utilities.path(qaDir, "validation.html")));

    if (watch) {
      first = false;
      log("Watching for changes on a 5sec cycle");
      while (watch) { // terminated externally
        Thread.sleep(5000);
        if (load()) {
          log("Processing changes to "+Integer.toString(changeList.size())+(changeList.size() == 1 ? " file" : " files")+" @ "+genTime());
          startTime = System.nanoTime();
          loadConformance();
          generateNarratives();
          checkDependencies();
          validate();
          generate();
          clean();
          endTime = System.nanoTime();
          log(" ... finished. "+presentDuration(endTime - startTime)+". Validation output in "+new ValidationPresenter(context).generate(ig.getName(), errors, fileList, Utilities.path(qaDir, "validation.html")));
        }
      }
    } else
      log("Done");
  }

  private void generateNarratives() throws IOException, EOperationOutcome, FHIRException {
    dlog("gen narratives");
    NarrativeGenerator gen = new NarrativeGenerator("", "", context);
    for (FetchedFile f : fileList) {
      for (FetchedResource r : f.getResources()) {
        dlog("narrative for "+f.getName()+" : "+r.getId());
        if (r.getResource() != null) {
          if (r.getResource() instanceof DomainResource && !((DomainResource) r.getResource()).getText().hasDiv())
            gen.generate((DomainResource) r.getResource());
        } else {
          if ("http://hl7.org/fhir/StructureDefinition/DomainResource".equals(r.getElement().getProperty().getStructure().getBaseDefinition()) && !hasNarrative(r.getElement())) {
            gen.generate(r.getElement(), true);
          }
        }
      }
    }
  }

  private boolean hasNarrative(Element element) {
    return element.hasChild("text") && element.getNamedChild("text").hasChild("div");
  }

  private void clean() throws Exception {
    for (Resource r : loaded)
      context.dropResource(r);
    for (FetchedFile f : fileList)
      f.dropSource();
  }

  private String genTime() {
    return new SimpleDateFormat("EEE, MMM d, yyyy HH:mmZ", new Locale("en", "US")).format(execTime.getTime());
  }

  private void checkDependencies() {
    // first, we load all the direct dependency lists
    for (FetchedFile f : fileList) 
      if (f.getDependencies() == null)
        loadDependencyList(f);

    // now, we keep adding to the change list till there's no change
    boolean changed;
    do {
      changed = false;
      for (FetchedFile f : fileList) {
        if (!changeList.contains(f)) {
          boolean dep = false;
          for (FetchedFile d : f.getDependencies()) 
            if (changeList.contains(d)) 
              dep = true;
          if (dep) {
            changeList.add(f);
            changed = true;
          }
        }
      }
    } while (changed);    
  }

  private void loadDependencyList(FetchedFile f) {
    f.setDependencies(new ArrayList<FetchedFile>());
    for (FetchedResource r : f.getResources()) {
      if (r.getElement().fhirType().equals("ValueSet"))
        loadValueSetDependencies(f, r);
      else if (r.getElement().fhirType().equals("StructureDefinition"))
        loadProfileDependencies(f, r);
      else
        ; // all other resource types don't have dependencies that we care about for rendering purposes
    }
  }

  private void loadValueSetDependencies(FetchedFile f, FetchedResource r) {
    ValueSet vs = (ValueSet) r.getResource();
    for (UriType vsi : vs.getCompose().getImport()) {
      FetchedFile fi = getFileForUri(vsi.getValue());
      if (fi != null)
        f.getDependencies().add(fi);
    }
    for (ConceptSetComponent vsc : vs.getCompose().getInclude()) {
      FetchedFile fi = getFileForUri(vsc.getSystem());
      if (fi != null)
        f.getDependencies().add(fi);
    }
    for (ConceptSetComponent vsc : vs.getCompose().getExclude()) {
      FetchedFile fi = getFileForUri(vsc.getSystem());
      if (fi != null)
        f.getDependencies().add(fi);
    }
  }

  private FetchedFile getFileForUri(String uri) {
    for (FetchedFile f : fileList) {
      for (FetchedResource r : f.getResources()) {
        if (r.getResource() != null && r.getResource() instanceof BaseConformance) {
          BaseConformance bc = (BaseConformance) r.getResource();
          if (bc.getUrl().equals(uri)) 
            return f;
        }
      }
    }
    return null;
  }

  private void loadProfileDependencies(FetchedFile f, FetchedResource r) {
    StructureDefinition sd = (StructureDefinition) r.getResource();
    FetchedFile fi = getFileForUri(sd.getBaseDefinition());
    if (fi != null)
      f.getDependencies().add(fi);
  }

  private String str(JsonObject obj, String name) throws Exception {
    if (!obj.has(name))
      throw new Exception("Property "+name+" not found");
    if (!(obj.get(name) instanceof JsonPrimitive))
      throw new Exception("Property "+name+" not a primitive");
    JsonPrimitive p = (JsonPrimitive) obj.get(name);
    if (!p.isString())
      throw new Exception("Property "+name+" not a string");
    return p.getAsString();
  }

  private String str(JsonObject obj, String name, String defValue) throws Exception {
    if (!obj.has(name))
      return defValue;
    if (!(obj.get(name) instanceof JsonPrimitive))
      throw new Exception("Property "+name+" not a primitive");
    JsonPrimitive p = (JsonPrimitive) obj.get(name);
    if (!p.isString())
      throw new Exception("Property "+name+" not a string");
    return p.getAsString();
  }

  private String presentDuration(long duration) {
    duration = duration / 1000000;
    String res = "";    // ;
    long days       = TimeUnit.MILLISECONDS.toDays(duration);
    long hours      = TimeUnit.MILLISECONDS.toHours(duration) -
        TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(duration));
    long minutes    = TimeUnit.MILLISECONDS.toMinutes(duration) -
        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
    long seconds    = TimeUnit.MILLISECONDS.toSeconds(duration) -
        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
    long millis     = TimeUnit.MILLISECONDS.toMillis(duration) - 
        TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(duration));

    if (days > 0)      
      res = String.format("%dd %02d:%02d:%02d.%04d", days, hours, minutes, seconds, millis);
    else if (hours > 0 || minutes > 0)                
      res = String.format("%02d:%02d:%02d.%04d", hours, minutes, seconds, millis);
    else
      res = String.format("%02d.%04d", seconds, millis);
    return res;  
  }

  private void initialize(boolean clearCache) throws Exception {
    first = true;
    log("Load Configuration");

    configuration = (JsonObject) new com.google.gson.JsonParser().parse(TextFile.fileToString(configFile));
    if (!"jekyll".equals(str(configuration, "tool")))
      throw new Exception("Error: configuration file must include a \"tool\" property with a value of 'jekyll'");
    tool = GenerationTool.Jekyll;
    if (!configuration.has("paths") || !(configuration.get("paths") instanceof JsonObject)) 
      throw new Exception("Error: configuration file must include a \"paths\" object");
    JsonObject paths = configuration.getAsJsonObject("paths");
    String root = Utilities.getDirectoryForFile(configFile);
    if (Utilities.noString(root))
      root = getCurentDirectory();
    resourcesDir = Utilities.path(root, str(paths, "resources", "resources"));
    pagesDir =  Utilities.path(root, str(paths, "pages", "pages"));
    tempDir = Utilities.path(root, str(paths, "temp", "temp"));
    outputDir = Utilities.path(root, str(paths, "output", "output"));
    qaDir = Utilities.path(root, str(paths, "qa"));
    specPath = str(paths, "specification");

    igName = Utilities.path(resourcesDir, configuration.get("source").getAsString());

    log("Publish "+igName);

    log("Check folders");
    checkDir(resourcesDir);
    checkDir(pagesDir);
    forceDir(tempDir);
    forceDir(Utilities.path(tempDir, "_includes"));
    forceDir(Utilities.path(tempDir, "data"));
    forceDir(outputDir);
    forceDir(qaDir);

    log("Load Validation Pack (internal)");
    try {
      context = SimpleWorkerContext.fromClassPath("igpack.zip");
    } catch (NullPointerException npe) {
      log("Unable to find igpack.zip in the jar");
      context = SimpleWorkerContext.fromPack("C:\\work\\org.hl7.fhir\\build\\temp\\igpack.zip");
    }
    log("Definitions "+context.getVersionRevision());
    context.setAllowLoadingDuplicates(true);
    context.setExpandCodesLimit(1000);
    log("Connect to Terminology Server at "+txServer);
    String vsCache = Utilities.path(System.getProperty("user.home"), "fhircache");
    Utilities.createDirectory(vsCache);
    context.initTS(vsCache, txServer);
    context.connectToTSServer(txServer);
    if (clearCache) {
      log("Terminology Cache is at "+vsCache+". Clearing now");
      Utilities.clearDirectory(vsCache);
    } else 
      log("Terminology Cache is at "+vsCache);
    // ;
    validator = new InstanceValidator(context);
    validator.setAllowXsiLocation(true);

    loadSpecDetails(context.getBinaries().get("spec.internals"));
    igpkp = new IGKnowledgeProvider(context, specPath, configuration, errors);
    igpkp.loadSpecPaths(specDetails.get("paths").getAsJsonObject());
    fetcher.setPkp(igpkp);
    for (String s : context.getBinaries().keySet())
      if (needFile(s)) {
        checkMakeFile(context.getBinaries().get(s), Utilities.path(qaDir, s), otherFilesStartup);    
        checkMakeFile(context.getBinaries().get(s), Utilities.path(tempDir, s), otherFilesStartup);
      }
    otherFilesStartup.add(Utilities.path(tempDir, "data"));
    otherFilesStartup.add(Utilities.path(tempDir, "data", "fhir.json"));
    otherFilesStartup.add(Utilities.path(tempDir, "_includes"));

  }

  private String getCurentDirectory() {
    String currentDirectory;
    File file = new File(".");
    currentDirectory = file.getAbsolutePath();
    log("Use Current directory: "+currentDirectory);
    return currentDirectory;
  }

  private void findRubyExe() {
    if (!USE_COMMONS_EXEC) {
      if (SystemUtils.IS_OS_WINDOWS) {
        String[] paths = System.getenv("path").split(File.pathSeparator);
        for (String s : paths) {
          String[] files = new File(s).list();
          if (files != null)
            for (String file : files)
              if (file.equals("ruby.exe")) {
                rubyExe = Utilities.path(s, file);
                jekyllGem = Utilities.path(s, "jekyll");
                if (!(new File(jekyllGem).exists()))
                  throw new Error("Found Ruby, but unable to find Jekyll Gem");
                log("Use Ruby at "+rubyExe);
                return;
              }
        }
      } else {
        rubyExe = "/usr/bin/ruby";
        if (!(new File(rubyExe).exists()))
          throw new Error("Unable to find Ruby at "+rubyExe);
        jekyllGem = "/usr/bin/jekyll";
        if (!(new File(jekyllGem).exists()))
          throw new Error("Found Ruby, but unable to find Jekyll at "+jekyllGem);
      }
      throw new Error("Unable to find Ruby Processor");
    }
  }

  private void checkDir(String dir) throws Exception {
    File f = new File(dir);
    if (!f.exists())
      throw new Exception(String.format("Error: folder %s not found", dir));
    else if (!f.isDirectory())
      throw new Exception(String.format("Error: Output must be a folder (%s)", dir));
  }

  private void checkFile(String fn) throws Exception {
    File f = new File(fn);
    if (!f.exists())
      throw new Exception(String.format("Error: folder %s not found", fn));
    else if (f.isDirectory())
      throw new Exception(String.format("Error: Output must be a file (%s)", fn));
  }

  private void forceDir(String dir) throws Exception {
    File f = new File(dir);
    if (!f.exists())
      Utilities.createDirectory(dir);
    else if (!f.isDirectory())
      throw new Exception(String.format("Error: Output must be a folder (%s)", dir));
  }

  private void checkMakeFile(byte[] bs, String path, Set<String> outputTracker) throws IOException {
    outputTracker.add(path);
    File f = new CSFile(path);
    byte[] existing = null;
    if (f.exists())
      existing = TextFile.fileToBytes(path);
    if (!Arrays.equals(bs, existing))
      TextFile.bytesToFile(bs, path);
  }

  private boolean needFile(String s) {
    if (s.endsWith(".css"))
      return true;
    if (s.startsWith("tbl"))
      return true;
    if (s.startsWith("icon"))
      return true;
    if (Utilities.existsInList(s, "modifier.png", "mustsupport.png", "summary.png", "lock.png", "external.png", "cc0.png", "target.png"))
      return true;
    return false;
  }

  public void loadSpecDetails(byte[] bs) throws IOException {
    String s = TextFile.bytesToString(bs);
    Gson g = new Gson();
    specDetails = g.fromJson(s, JsonObject.class);
  }


  private boolean load() throws Exception {
    fileList.clear();
    changeList.clear();
    bndIds.clear();
    boolean needToBuild = false;
    FetchedFile igf = fetcher.fetch(igName);
    needToBuild = noteFile(IG_NAME, igf) || needToBuild;
    if (needToBuild) {
      ig = (ImplementationGuide) parse(igf);
      FetchedResource igr = igf.addResource(); 
      igr.setElement(new ObjectConverter(context).convert(ig));
      igr.setResource(ig);
      igr.setId(ig.getId()).setTitle(ig.getName());
    } else
      ig = (ImplementationGuide) altMap.get(IG_NAME).getResources().get(0).getResource();

    // load any bundles 
    needToBuild = loadSpreadsheets(needToBuild, igf);
    needToBuild = loadBundles(needToBuild, igf);
    for (ImplementationGuidePackageComponent pack : ig.getPackage()) {
      for (ImplementationGuidePackageResourceComponent res : pack.getResource()) {
        if (!bndIds.contains(res.getSourceReference().getReference())) {
          FetchedFile f = fetcher.fetch(res.getSource(), igf);
          needToBuild = noteFile(res, f) || needToBuild;
          determineType(f);
        }
      }     
    }

    // load static pages
    needToBuild = loadPages() || needToBuild;
    execTime = Calendar.getInstance();
    return needToBuild;
  }

  private boolean loadPages() throws Exception {
    FetchedFile dir = fetcher.fetch(pagesDir);
    if (!dir.isFolder())
      throw new Exception("page reference is not a folder");
    return loadPages(dir);
  }

  private boolean loadPages(FetchedFile dir) throws Exception {
    boolean changed = false;
    if (!altMap.containsKey("page/"+dir.getPath())) {
      changed = true;
      altMap.put("page/"+dir.getPath(), dir);
      dir.setNoProcess(true);
      changeList.add(dir);
    }
    for (String link : dir.getFiles()) {
      FetchedFile f = fetcher.fetch(link);
      if (f.isFolder())
        changed = loadPages(f) || changed;
      else 
        changed = loadPage(f) || changed;
    }
    return changed;
  }

  private boolean loadPage(FetchedFile file) {
    FetchedFile existing = altMap.get("page/"+file.getPath());
    if (existing == null || existing.getTime() != file.getTime() || existing.getHash() != file.getHash()) {
      file.setNoProcess(true);
      changeList.add(file);
      altMap.put("page/"+file.getPath(), file);
      return true;
    } else {
      return false;
    }
  }

  private boolean loadBundles(boolean needToBuild, FetchedFile igf) throws Exception {
    JsonArray bundles = configuration.getAsJsonArray("bundles");
    if (bundles != null) {
      for (JsonElement be : bundles) {
        needToBuild = loadBundle((JsonPrimitive) be, needToBuild, igf);
      }
    }
    return needToBuild;
  }

  private boolean loadBundle(JsonPrimitive be, boolean needToBuild, FetchedFile igf) throws Exception {
    FetchedFile f = fetcher.fetch(new Reference().setReference("Bundle/"+be.getAsString()), igf);
    boolean changed = noteFile("Bundle/"+be.getAsString(), f);
    if (changed) {
      f.setBundle((Bundle) parse(f));
      for (BundleEntryComponent b : f.getBundle().getEntry()) {
        FetchedResource r = f.addResource();
        r.setResource(b.getResource());
        r.setId(b.getResource().getId());
        r.setElement(new ObjectConverter(context).convert(r.getResource()));  
        for (UriType p : b.getResource().getMeta().getProfile())
          r.getProfiles().add(p.asStringValue());
        r.setTitle(r.getElement().getChildValue("name"));
        igpkp.findConfiguration(f, r);
      }
    } else 
      f = altMap.get("Bundle/"+be.getAsString());
    for (FetchedResource r : f.getResources()) 
      bndIds.add(r.getElement().fhirType()+"/"+r.getId());
    return changed || needToBuild;
  }

  private boolean loadSpreadsheets(boolean needToBuild, FetchedFile igf) throws Exception {
    JsonArray spreadsheets = configuration.getAsJsonArray("spreadsheets");
    if (spreadsheets != null) {
      for (JsonElement be : spreadsheets) {
        needToBuild = loadSpreadsheet((JsonPrimitive) be, needToBuild, igf);
      }
    }
    return needToBuild;
  }

  private boolean loadSpreadsheet(JsonPrimitive be, boolean needToBuild, FetchedFile igf) throws Exception {
    String path = Utilities.path(Utilities.getDirectoryForFile(igName), be.getAsString());
    FetchedFile f = fetcher.fetch(path);
    boolean changed = noteFile("Spreadsheet/"+be.getAsString(), f);
    if (changed) {
      f.getValuesetsToLoad().clear();
      f.setBundle(new IgSpreadsheetParser(context, execTime, igpkp.getCanonical(), f.getValuesetsToLoad(), first, context.getBinaries().get("mappingSpaces.details")).parse(f));
      for (BundleEntryComponent b : f.getBundle().getEntry()) {
        FetchedResource r = f.addResource();
        r.setResource(b.getResource());
        r.setId(b.getResource().getId());
        r.setElement(new ObjectConverter(context).convert(r.getResource()));          
        r.setTitle(r.getElement().getChildValue("name"));
        igpkp.findConfiguration(f, r);
      }
    } else 
      f = altMap.get("Spreadsheet/"+be.getAsString());
    for (String vr : f.getValuesetsToLoad()) {
      path = Utilities.path(Utilities.getDirectoryForFile(igName), vr);
      FetchedFile fv = fetcher.fetchFlexible(path);
      boolean vrchanged = noteFile("sp-ValueSet/"+vr, fv);
      if (vrchanged) {
        determineType(fv);
        // check the canonical URL
        String url = fv.getResources().get(0).getElement().getChildValue("url");
        String id = fv.getResources().get(0).getId();
        if (!tail(url).equals(id)) 
          throw new Exception("resource id/url mismatch: "+id+" vs "+url+" for "+fv.getResources().get(0).getTitle()+" in "+fv.getName());
        //        if (!url.startsWith(igpkp.getCanonical())) 
        //          throw new Exception("base/ resource url mismatch: "+igpkp.getCanonical()+" vs "+url);

      }
      changed = changed || vrchanged;
    }
    for (FetchedResource r : f.getResources()) 
      bndIds.add(r.getElement().fhirType()+"/"+r.getId());
    return changed || needToBuild;
  }


  private String tail(String url) {
    return url.substring(url.lastIndexOf("/")+1);
  }

  private void loadConformance() throws Exception {
    load("NamingSystem");
    load("CodeSystem");
    load("ValueSet");
    load("DataElement");
    load("StructureDefinition");
    load("ConceptMap");
    load("StructureMap");
    generateSnapshots();
    generateLogicalMaps();
    generateAdditionalExamples();
  }

  private boolean noteFile(ImplementationGuidePackageResourceComponent key, FetchedFile file) {
    FetchedFile existing = fileMap.get(key);
    if (existing == null || existing.getTime() != file.getTime() || existing.getHash() != file.getHash()) {
      fileList.add(file);
      fileMap.put(key, file);
      changeList.add(file);
      return true;
    } else {
      fileList.add(existing); // this one is already parsed
      return false;
    }
  }

  private boolean noteFile(String key, FetchedFile file) {
    FetchedFile existing = altMap.get(key);
    if (existing == null || existing.getTime() != file.getTime() || existing.getHash() != file.getHash()) {
      fileList.add(file);
      altMap.put(key, file);
      changeList.add(file);
      return true;
    } else {
      fileList.add(existing); // this one is already parsed
      return false;
    }
  }

  private void determineType(FetchedFile file) throws Exception {
    if (file.getResources().isEmpty()) {
      file.getErrors().clear();
      Element e = null;
      try {
        if (file.getContentType().contains("json"))
          e = loadFromJson(file);
        else if (file.getContentType().contains("xml"))
          e = loadFromXml(file);
        else 
          throw new Exception("Unable to determine file type for "+file.getName());

      } catch (Exception ex) {
        throw new Exception("Unable to parse "+file.getName()+": " +ex.getMessage(), ex);
      }
      FetchedResource r = file.addResource();
      r.setElement(e).setId(e.getChildValue("id")).setTitle(e.getChildValue("name"));
      Element m = e.getNamedChild("meta");
      if (m != null) {
        List<Element> profiles = m.getChildrenByName("profile");
        for (Element p : profiles)
          r.getProfiles().add(p.getValue());
      }
      igpkp.findConfiguration(file, r);
    }
  }

  private Element loadFromXml(FetchedFile file) throws Exception {
    org.hl7.fhir.dstu3.elementmodel.XmlParser xp = new org.hl7.fhir.dstu3.elementmodel.XmlParser(context);
    xp.setupValidation(ValidationPolicy.EVERYTHING, file.getErrors()); 
    return xp.parse(new ByteArrayInputStream(file.getSource()));
  }

  private Element loadFromJson(FetchedFile file) throws Exception {
    org.hl7.fhir.dstu3.elementmodel.JsonParser jp = new org.hl7.fhir.dstu3.elementmodel.JsonParser(context);
    jp.setupValidation(ValidationPolicy.EVERYTHING, file.getErrors()); 
    return jp.parse(new ByteArrayInputStream(file.getSource()));
  }

  private void load(String type) throws Exception {
    dlog("process type: "+type);
    for (FetchedFile f : fileList) {
      for (FetchedResource r : f.getResources()) {
        dlog("process res: "+r.getId());
        if (r.getElement().fhirType().equals(type)) {
          if (!r.isValidated()) 
            validate(f, r);
          if (r.getResource() == null)
            try {
              r.setResource(parse(f)); // we won't get to here if we're a bundle
            } catch (Exception e) {
              throw new Exception("Error parsing "+f.getName()+": "+e.getMessage(), e);
            }
          BaseConformance bc = (BaseConformance) r.getResource();
          igpkp.checkForPath(f, r, bc);
          context.seeResource(bc.getUrl(), bc);
        }
      }
    }
  }

  private void dlog(String s) {
    logger.logDebugMessage(Utilities.padRight(s, ' ', 80)+" ("+presentDuration(System.nanoTime()-globalStart)+"sec)");
  }

  private void generateAdditionalExamples() throws Exception {
    ProfileUtilities utils = new ProfileUtilities(context, null, null);
    for (FetchedFile f : changeList) {
      List<StructureDefinition> list = new ArrayList<StructureDefinition>();
      for (FetchedResource r : f.getResources()) {
        if (r.getResource() instanceof StructureDefinition) {
          list.add((StructureDefinition) r.getResource());
        }
      }
      for (StructureDefinition sd : list) {
        for (Element e : utils.generateExamples(sd, false)) {
          FetchedResource nr = new FetchedResource();
          nr.setElement(e);
          nr.setId(e.getChildValue("id"));
          nr.setTitle("Generated Example");
          nr.getProfiles().add(sd.getUrl());
          f.getResources().add(nr);
          igpkp.findConfiguration(f, nr);
        }
      }
    }
  }

  private void generateSnapshots() throws Exception {
    ProfileUtilities utils = new ProfileUtilities(context, null, null);
    for (FetchedFile f : fileList) {
      for (FetchedResource r : f.getResources()) {
        if (r.getResource() instanceof StructureDefinition) {
          boolean changed = false;
          StructureDefinition sd = (StructureDefinition) r.getResource();
          String p = sd.getDifferential().getElement().get(0).getPath();
          if (p.contains(".")) {
            changed = true;
            sd.getDifferential().getElement().add(0, new ElementDefinition().setPath(p.substring(0, p.indexOf("."))));
          }
          if (!sd.hasSnapshot() && sd.getKind() != StructureDefinitionKind.LOGICAL) {
            StructureDefinition base = context.fetchResource(StructureDefinition.class, sd.getBaseDefinition());
            if (base != null) {
              utils.generateSnapshot(base, sd, sd.getUrl(), sd.getName());
              changed = true;
            }
          }
          if (!sd.hasSnapshot() && sd.getKind() == StructureDefinitionKind.LOGICAL) {
            utils.populateLogicalSnapshot(sd);
          }
          if (changed)
            r.setElement(new ObjectConverter(context).convert(sd));
        }
      }
    }

    for (StructureDefinition derived : context.allStructures()) {
      if (!derived.hasSnapshot()) {
        StructureDefinition base = context.fetchResource(StructureDefinition.class, derived.getBaseDefinition());
        if (base != null)
          utils.generateSnapshot(base, derived, derived.getUrl(), derived.getName());
      }
    }
  }
  
  private void generateLogicalMaps() throws Exception {
    StructureMapUtilities mu = new StructureMapUtilities(context, null, null);
    for (FetchedFile f : fileList) {
      List<StructureMap> maps = new ArrayList<StructureMap>();
      for (FetchedResource r : f.getResources()) {
        if (r.getResource() instanceof StructureDefinition) {
          StructureMap map = mu.generateMapFromMappings((StructureDefinition) r.getResource());
          if (map != null) {
            maps.add(map);
          }
        }
      }
      for (StructureMap map : maps) {
        FetchedResource nr = f.addResource();
        nr.setResource(map);
        nr.setElement(new ObjectConverter(context).convert(map));
        nr.setId(map.getId());
        nr.setTitle(map.getName());
        igpkp.findConfiguration(f, nr);
      }
    }
  }

  private Resource parse(FetchedFile file) throws Exception {

    if (file.getContentType().contains("json"))
      return new JsonParser().parse(file.getSource());
    else if (file.getContentType().contains("xml"))
      return new XmlParser().parse(file.getSource());
    else 
      throw new Exception("Unable to determine file type for "+file.getName());
  }

  private void validate() throws Exception {
    for (FetchedFile f : fileList) {
      if (first)
        dlog(" .. "+f.getName());
      for (FetchedResource r : f.getResources()) {
        if (!r.isValidated()) {
          validate(f, r);
        }
      }
    }
  }

  private void validate(FetchedFile file, FetchedResource r) throws Exception {
    validator.validate(file.getErrors(), r.getElement());
    r.setValidated(true);
    if (r.getConfig() == null)
      igpkp.findConfiguration(file, r);
  }

  private void generate() throws Exception {
    forceDir(tempDir);
    forceDir(Utilities.path(tempDir, "_includes"));
    forceDir(Utilities.path(tempDir, "data"));
    
    otherFilesRun.clear();
    for (FetchedFile f : changeList) 
      generateOutputs(f);

    if (!changeList.isEmpty())
      generateSummaryOutputs();

    cleanOutput(tempDir);
    
    if (runTool()) 
      if (!changeList.isEmpty())
        generateZips();
    
  }

  private void cleanOutput(String folder) throws IOException {
    for (File f : new File(folder).listFiles()) {
      if (!isValidFile(f.getAbsolutePath())) {
        if (f.isDirectory()) 
          Utilities.clearDirectory(f.getAbsolutePath());
        f.delete();
      }
    }
  }

  private boolean isValidFile(String p) {
    if (otherFilesStartup.contains(p))
      return true;
    if (otherFilesRun.contains(p))
      return true;
    for (FetchedFile f : fileList)
      if (f.getOutputNames().contains(p))
        return true;
    for (FetchedFile f : altMap.values())
      if (f.getOutputNames().contains(p))
        return true;
    return false;
  }

  private void generateZips() throws Exception {
    if (generateExampleZip(FhirFormat.XML))
      generateDefinitions(FhirFormat.XML);
    if (generateExampleZip(FhirFormat.JSON))
      generateDefinitions(FhirFormat.JSON);
    if (generateExampleZip(FhirFormat.TURTLE))
      generateDefinitions(FhirFormat.TURTLE);
  }

  private void generateDefinitions(FhirFormat fmt)  throws Exception {
    Set<String> files = new HashSet<String>();
    for (FetchedFile f : fileList) {
      for (FetchedResource r : f.getResources()) {
        if (r.getResource() != null && r.getResource() instanceof BaseConformance) {
          String fn = Utilities.path(outputDir, r.getElement().fhirType()+"-"+r.getId()+"."+fmt.getExtension());
          if (new File(fn).exists())
            files.add(fn);
        }
      }
    }
    if (!files.isEmpty()) {
      ZipGenerator zip = new ZipGenerator(Utilities.path(outputDir, "definitions."+fmt.getExtension()+".zip"));
      for (String fn : files)
        zip.addFileName(fn.substring(fn.lastIndexOf(File.separator)+1), fn, false);
      zip.close();

    }
  }

  private boolean generateExampleZip(FhirFormat fmt) throws Exception {
    Set<String> files = new HashSet<String>();
    for (FetchedFile f : fileList) {
      for (FetchedResource r : f.getResources()) {
        String fn = Utilities.path(outputDir, r.getElement().fhirType()+"-"+r.getId()+"."+fmt.getExtension());
        if (new File(fn).exists())
          files.add(fn);
      }
    }
    if (!files.isEmpty()) {
      ZipGenerator zip = new ZipGenerator(Utilities.path(outputDir, "examples."+fmt.getExtension()+".zip"));
      for (String fn : files)
        zip.addFileName(fn.substring(fn.lastIndexOf(File.separator)+1), fn, false);
      zip.close();

    }
    return !files.isEmpty();
  }

  private boolean runTool() throws Exception {
    switch (tool) {
    case Jekyll: return runJekyll();
    default:
      throw new Exception("unimplemented tool");
    }
  }

  public class MyFilterHandler extends OutputStream {

    private byte[] buffer;
    private int length;

    public MyFilterHandler() {
      buffer = new byte[256];
    }

    private boolean passJekyllFilter(String s) {
      if (Utilities.noString(s))
        return false;
      if (s.contains("Source:"))
        return true;
      if (s.contains("Destination:"))
        return false;
      if (s.contains("Configuration"))
        return false;
      if (s.contains("Incremental build:"))
        return false;
      if (s.contains("Auto-regeneration:"))
        return false;
      return true;
    }

    @Override
    public void write(int b) throws IOException {
      buffer[length] = (byte) b;
      length++;
      if (b == 10) { // eoln
        String s = new String(buffer, 0, length);
        if (passJekyllFilter(s))
          log("Jekyll: "+s.trim());
        length = 0;  
      }
    }
  }

  private boolean passJekyllFilter(String s) {
    if (Utilities.noString(s))
      return false;
    if (s.contains("Source:"))
      return false;
    if (s.contains("Destination:"))
      return false;
    if (s.contains("Configuration"))
      return false;
    if (s.contains("Incremental build:"))
      return false;
    if (s.contains("Auto-regeneration:"))
      return false;
    return true;
  }

  private boolean runJekyll() throws IOException, InterruptedException {
    if (USE_COMMONS_EXEC) {
      DefaultExecutor exec = new DefaultExecutor();
      exec.setExitValue(0);
      PumpStreamHandler pump = new PumpStreamHandler(new MyFilterHandler());
      exec.setStreamHandler(pump);
      exec.setWorkingDirectory(new File(tempDir));
      if (SystemUtils.IS_OS_WINDOWS) 
        exec.execute(org.apache.commons.exec.CommandLine.parse("cmd /C jekyll build --destination "+outputDir));
      else
        exec.execute(org.apache.commons.exec.CommandLine.parse("jekyll build --destination "+outputDir));      
    } else {
      findRubyExe();

      // set up jekyll 
      List<String> command = new ArrayList<String>();
      command.add(rubyExe);
      command.add(jekyllGem);
      command.add("build");
      command.add("--destination");
      command.add(outputDir);
      ProcessBuilder builder = new ProcessBuilder(command);
      builder.directory(new File(tempDir));

      // run and capture the output
      final Process process = builder.start();
      BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String s;
      while ((s = stdError.readLine()) != null) {
        if (passJekyllFilter(s))
          log("Jekyll: "+s);
      }
      process.waitFor();
    }
    return true;
  }

  private void generateSummaryOutputs() throws Exception {
    generateResourceReferences();

    JsonObject data = new JsonObject();
    data.addProperty("path", specPath);
    data.addProperty("canonical", igpkp.getCanonical());
    data.addProperty("errorCount", getErrorCount());
    data.addProperty("version", Constants.VERSION);
    data.addProperty("revision", Constants.REVISION);
    data.addProperty("versionFull", Constants.VERSION+"-"+Constants.REVISION);
    data.addProperty("totalFiles", fileList.size());
    data.addProperty("processedFiles", changeList.size());
    data.addProperty("genDate", genTime());

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String json = gson.toJson(data);
    TextFile.stringToFile(json, Utilities.path(tempDir, "data", "fhir.json"));
  }

  private void generateResourceReferences() throws Exception {
    for (ResourceType rt : ResourceType.values()) {
      generateResourceReferences(rt);
    }
  }

  private void generateResourceReferences(ResourceType rt) throws Exception {
    StringBuilder list = new StringBuilder();
    StringBuilder table = new StringBuilder();
    boolean found = false;
    for (FetchedFile f : fileList) {
      for (FetchedResource r : f.getResources()) {
        if (r.getElement().fhirType().equals(rt.toString())) {
          found = true;
          String name = r.getTitle();
          if (Utilities.noString(name))
            name = rt.toString();
          String ref = igpkp.getLinkFor(f, r);
          String desc = r.getTitle();
          if (r.getResource() != null && r.getResource() instanceof BaseConformance) {
            name = ((BaseConformance) r.getResource()).getName();
            desc = getDesc((BaseConformance) r.getResource(), desc);
          }
          list.append(" <li><a href=\""+ref+"\">"+Utilities.escapeXml(name)+"</a>"+Utilities.escapeXml(desc)+"</li>/r/n");
          table.append(" <tr><td><a href=\""+ref+"\">"+Utilities.escapeXml(name)+"</a></td><td>"+new BaseRenderer(context, null, igpkp).processMarkdown("description", desc)+"</td></tr>\r\n");
        }
      }
    }
    if (found) {
      fragment("list-"+Utilities.pluralizeMe(rt.toString().toLowerCase()), list.toString(), otherFilesRun);
      fragment("table-"+Utilities.pluralizeMe(rt.toString().toLowerCase()), table.toString(), otherFilesRun);
    }
  }

  private String getDesc(BaseConformance r, String desc) {
    if (r instanceof CodeSystem) {
      CodeSystem v = (CodeSystem) r;
      if (v.hasDescription())
        return v.getDescription();
    }
    if (r instanceof ValueSet) {
      ValueSet v = (ValueSet) r;
      if (v.hasDescription())
        return v.getDescription();
    }
    if (r instanceof StructureDefinition) {
      StructureDefinition v = (StructureDefinition) r;
      if (v.hasDescription())
        return v.getDescription();
    }
    return desc;
  }

  private Number getErrorCount() {
    int result = countErrs(errors);
    for (FetchedFile f : fileList) {
      result = result + countErrs(f.getErrors());
    }
    return result;
  }

  private int countErrs(List<ValidationMessage> list) {
    int i = 0;
    for (ValidationMessage vm : list) {
      if (vm.getLevel() == IssueSeverity.ERROR || vm.getLevel() == IssueSeverity.FATAL)
        i++;
    }
    return i;
  }

  private void log(String s) {
    if (first)
      logger.logMessage(Utilities.padRight(s, ' ', 80)+" ("+presentDuration(System.nanoTime()-globalStart)+"sec)");
    else
      logger.logMessage(s);
  }

  private void generateOutputs(FetchedFile f) {
//    log(" * "+f.getName());

    if (f.isNoProcess()) {
      String dst = tempDir + f.getPath().substring(pagesDir.length());
      try {
        if (f.isFolder()) {
          f.getOutputNames().add(dst);
          Utilities.createDirectory(dst);
        } else
          checkMakeFile(f.getSource(), dst, f.getOutputNames());
      } catch (IOException e) {
        log("Exception generating page "+dst+": "+e.getMessage());
      } 
    } else {
      for (FetchedResource r : f.getResources()) {
        try {
          saveDirectResourceOutputs(f, r);

          // now, start generating resource type specific stuff 
          if (r.getResource() != null) { // we only do this for conformance resources we've already loaded
            switch (r.getResource().getResourceType()) {
            case CodeSystem:
              generateOutputsCodeSystem(f, r, (CodeSystem) r.getResource());
              break;
            case ValueSet:
              generateOutputsValueSet(f, r, (ValueSet) r.getResource());
              break;
            case ConceptMap:
              generateOutputsConceptMap(f, r, (ConceptMap) r.getResource());
              break;

            case DataElement:
              break;
            case StructureDefinition:
              generateOutputsStructureDefinition(f, r, (StructureDefinition) r.getResource());
              break;
            case StructureMap:
              generateOutputsStructureMap(f, r, (StructureMap) r.getResource());
              break;
            default:
              // nothing to do...    
            }      
          }
        } catch (Exception e) {
          log("Exception generating resource "+f.getName()+"::"+r.getElement().fhirType()+"/"+r.getId()+": "+e.getMessage());
          e.printStackTrace();
        } 
      }
    }
  }

 
  private boolean wantGen(FetchedResource r, String code) {
    if (r.getConfig() != null && hasBoolean(r.getConfig(), code))
      return getBoolean(r.getConfig(), code);
    JsonObject cfg = configuration.getAsJsonObject("defaults");
    if (cfg != null)
      cfg = cfg.getAsJsonObject(r.getElement().fhirType());
    if (cfg != null && hasBoolean(cfg, code))
      return getBoolean(cfg, code);
    cfg = configuration.getAsJsonObject("defaults");
    if (cfg != null)
      cfg = cfg.getAsJsonObject("Any");
    if (cfg != null && hasBoolean(cfg, code))
      return getBoolean(cfg, code);
    return true;
  }

  private String getTemplate(FetchedResource r, String propertyName) {
    if (r.getConfig() != null && hasString(r.getConfig(), propertyName))
      return getString(r.getConfig(), propertyName);
    JsonObject cfg = configuration.getAsJsonObject("defaults");
    if (cfg != null)
      cfg = cfg.getAsJsonObject(r.getElement().fhirType());
    if (cfg != null && hasString(cfg, propertyName))
      return getString(cfg, propertyName);
    cfg = configuration.getAsJsonObject("defaults");
    if (cfg != null)
      cfg = cfg.getAsJsonObject("Any");
    if (cfg != null && hasString(cfg, propertyName))
      return getString(cfg, propertyName);
    return null;
  }


  private boolean hasBoolean(JsonObject obj, String code) {
    JsonElement e = obj.get(code);
    return e != null && e instanceof JsonPrimitive && ((JsonPrimitive) e).isBoolean();
  }

  private boolean getBoolean(JsonObject obj, String code) {
    JsonElement e = obj.get(code);
    return e != null && e instanceof JsonPrimitive && ((JsonPrimitive) e).getAsBoolean();
  }

  private boolean hasString(JsonObject obj, String code) {
    JsonElement e = obj.get(code);
    return e != null && (e instanceof JsonPrimitive && ((JsonPrimitive) e).isString()) || e instanceof JsonNull;
  }

  private String getString(JsonObject obj, String code) {
    JsonElement e = obj.get(code);
    if (e instanceof JsonNull)
      return null;
    else 
      return ((JsonPrimitive) e).getAsString();
  }

  /**
   * saves the resource as XML, JSON, Turtle, 
   * then all 3 of those as html with embedded links to the definitions
   * then the narrative as html
   *  
   * @param r
   * @throws FileNotFoundException
   * @throws Exception
   */
  private void saveDirectResourceOutputs(FetchedFile f, FetchedResource r) throws FileNotFoundException, Exception {
    genWrapperBase(r, getTemplate(r, "template-base"), f.getOutputNames());
    
    String template = getTemplate(r, "template-format");
    if (wantGen(r, "xml")) {
      String path = Utilities.path(tempDir, r.getElement().fhirType()+"-"+r.getId()+".xml");
      f.getOutputNames().add(path);
      new org.hl7.fhir.dstu3.elementmodel.XmlParser(context).compose(r.getElement(), new FileOutputStream(path), OutputStyle.PRETTY, "??");
      if (tool == GenerationTool.Jekyll)
        genWrapperFmt(r, template, "xml", f.getOutputNames());  
    }
    if (wantGen(r, "json")) {
      String path = Utilities.path(tempDir, r.getElement().fhirType()+"-"+r.getId()+".json");
      f.getOutputNames().add(path);
      new org.hl7.fhir.dstu3.elementmodel.JsonParser(context).compose(r.getElement(), new FileOutputStream(path), OutputStyle.PRETTY, "??");
      if (tool == GenerationTool.Jekyll)
        genWrapperFmt(r, template, "json", f.getOutputNames());  
    }
    if (wantGen(r, "ttl")) {
      String path = Utilities.path(tempDir, r.getElement().fhirType()+"-"+r.getId()+".ttl");
      f.getOutputNames().add(path);
      new org.hl7.fhir.dstu3.elementmodel.TurtleParser(context).compose(r.getElement(), new FileOutputStream(path), OutputStyle.PRETTY, "??");
      if (tool == GenerationTool.Jekyll)
        genWrapperFmt(r, template, "ttl", f.getOutputNames());  
    }

    if (wantGen(r, "xml-html")) {
      XmlXHtmlRenderer x = new XmlXHtmlRenderer();
      org.hl7.fhir.dstu3.elementmodel.XmlParser xp = new org.hl7.fhir.dstu3.elementmodel.XmlParser(context);
      xp.setLinkResolver(igpkp);
      xp.compose(r.getElement(), x);
      fragment(r.getElement().fhirType()+"-"+r.getId()+"-xml-html", x.toString(), f.getOutputNames());
    }
    if (wantGen(r, "json-html")) {
      JsonXhtmlRenderer j = new JsonXhtmlRenderer();
      org.hl7.fhir.dstu3.elementmodel.JsonParser jp = new org.hl7.fhir.dstu3.elementmodel.JsonParser(context);
      jp.setLinkResolver(igpkp);
      jp.compose(r.getElement(), j);
      fragment(r.getElement().fhirType()+"-"+r.getId()+"-json-html", j.toString(), f.getOutputNames());
    }

    if (wantGen(r, "ttl-html")) {
      org.hl7.fhir.dstu3.elementmodel.TurtleParser ttl = new org.hl7.fhir.dstu3.elementmodel.TurtleParser(context);
      ttl.setLinkResolver(igpkp);
      Turtle rdf = new Turtle();
      ttl.compose(r.getElement(), rdf, "??");
      fragment(r.getElement().fhirType()+"-"+r.getId()+"-ttl-html", rdf.asHtml(), f.getOutputNames());
    }

    if (wantGen(r, "html")) {
      XhtmlNode xhtml = getXhtml(r);
      String html = xhtml == null ? "" : new XhtmlComposer().compose(xhtml);
      fragment(r.getElement().fhirType()+"-"+r.getId()+"-html", html, f.getOutputNames());
    }
    //  NarrativeGenerator gen = new NarrativeGenerator(null, null, context);
    //  gen.generate(f.getElement(), false);
    //  xhtml = getXhtml(f);
    //  html = xhtml == null ? "" : new XhtmlComposer().compose(xhtml);
    //  fragment(f.getId()+"-gen-html", html);
  }

  private void genWrapperFmt(FetchedResource r, String template, String format, Set<String> outputTracker) throws FileNotFoundException, IOException {
    if (template != null) {
      template = TextFile.fileToString(Utilities.path(Utilities.getDirectoryForFile(configFile), template));
      template = template.replace("{{[title]}}", r.getTitle());
      template = template.replace("{{[name]}}", r.getId()+"-"+format+"-html");
      template = template.replace("{{[id]}}", r.getId());
      template = template.replace("{{[type]}}", r.getElement().fhirType());
      String path = Utilities.path(tempDir, r.getElement().fhirType()+"-"+r.getId()+"."+format+".html");
      TextFile.stringToFile(template, path, false);
      outputTracker.add(path);
    }
  }

  private void genWrapperBase(FetchedResource r, String template, Set<String> outputTracker) throws FileNotFoundException, IOException {
    if (template != null) {
      template = TextFile.fileToString(Utilities.path(Utilities.getDirectoryForFile(configFile), template));
      template = template.replace("{{[title]}}", r.getTitle());
      template = template.replace("{{[type]}}", r.getElement().fhirType());
      template = template.replace("{{[id]}}", r.getId());
      template = template.replace("{{[name]}}", r.getId()+"-html");
      String path = Utilities.path(tempDir, r.getElement().fhirType()+"-"+r.getId()+".html");
      TextFile.stringToFile(template, path, false);
      outputTracker.add(path);
    }
  }

  /**
   * Generate:
   *   summary
   *   content as html
   *   xref
   * @param resource
   * @throws org.hl7.fhir.exceptions.FHIRException 
   * @throws Exception 
   */
  private void generateOutputsCodeSystem(FetchedFile f, FetchedResource fr, CodeSystem cs) throws Exception {
    CodeSystemRenderer csr = new CodeSystemRenderer(context, specPath, cs, igpkp);
    if (wantGen(fr, "summary")) 
      fragment("CodeSystem-"+cs.getId()+"-summary", csr.summary(wantGen(fr, "xml"), wantGen(fr, "json"), wantGen(fr, "ttl")), f.getOutputNames());
    if (wantGen(fr, "content")) 
      fragment("CodeSystem-"+cs.getId()+"-content", csr.content(), f.getOutputNames());
    if (wantGen(fr, "xref")) 
      fragment("CodeSystem-"+cs.getId()+"-xref", csr.xref(), f.getOutputNames());
  }

  /**
   * Genrate: 
   *   summary
   *   Content logical definition
   *   cross-reference
   *   
   * and save the expansion as html. todo: should we save it as a resource too? at this time, no: it's not safe to do that; encourages abuse
   * @param vs
   * @throws org.hl7.fhir.exceptions.FHIRException 
   * @throws Exception 
   */
  private void generateOutputsValueSet(FetchedFile f, FetchedResource r, ValueSet vs) throws Exception {
    ValueSetRenderer vsr = new ValueSetRenderer(context, specPath, vs, igpkp);
    if (wantGen(r, "summary")) 
      fragment("ValueSet-"+vs.getId()+"-summary", vsr.summary(wantGen(r, "xml"), wantGen(r, "json"), wantGen(r, "ttl")), f.getOutputNames());
    if (wantGen(r, "cld")) 
      try {
        fragment("ValueSet-"+vs.getId()+"-cld", vsr.cld(), f.getOutputNames());
      } catch (Exception e) {
        fragmentError(vs.getId()+"-cld", e.getMessage(), f.getOutputNames());
      }

    if (wantGen(r, "xref")) 
      fragment("ValueSet-"+vs.getId()+"-xref", vsr.xref(), f.getOutputNames());
    if (wantGen(r, "expansion")) { 
      ValueSetExpansionOutcome exp = context.expandVS(vs, true);
      if (exp.getValueset() != null) {
        NarrativeGenerator gen = new NarrativeGenerator("", null, context);
        gen.setTooCostlyNote("This value set has >1000 codes in it. In order to keep the publication size manageable, only a selection (1000 codes) of the whole set of codes is shown");
        exp.getValueset().setCompose(null);
        exp.getValueset().setText(null);
        gen.generate(exp.getValueset(), false);
        String html = new XhtmlComposer().compose(exp.getValueset().getText().getDiv());
        fragment("ValueSet-"+vs.getId()+"-expansion", html, f.getOutputNames());
      } else if (exp.getError() != null) 
        fragmentError("ValueSet-"+vs.getId()+"-expansion", exp.getError(), f.getOutputNames());
      else 
        fragmentError("ValueSet-"+vs.getId()+"-expansion", "Unknown Error", f.getOutputNames());
    }
  }

  private void fragmentError(String name, String error, Set<String> outputTracker) throws IOException {
    fragment(name, "<p style=\"color: maroon; font-weight: bold\">"+Utilities.escapeXml(error)+"</p>\r\n", outputTracker);
  }

  /**
   * Generate:
   *   summary
   *   content as html
   *   xref
   * @param resource
   * @throws IOException 
   */
  private void generateOutputsConceptMap(FetchedFile f, FetchedResource r, ConceptMap cm) throws IOException {
    if (wantGen(r, "summary")) 
      fragmentError("ConceptMap-"+cm.getId()+"-summary", "yet to be done: concept map summary", f.getOutputNames());
    if (wantGen(r, "content")) 
      fragmentError("ConceptMap-"+cm.getId()+"-content", "yet to be done: table presentation of the concept map", f.getOutputNames());
    if (wantGen(r, "xref")) 
      fragmentError("ConceptMap-"+cm.getId()+"-xref", "yet to be done: list of all places where concept map is used", f.getOutputNames());
  }

  private void generateOutputsStructureDefinition(FetchedFile f, FetchedResource r, StructureDefinition sd) throws Exception {
    // todo : generate shex itself
    if (wantGen(r, "shex")) 
      fragmentError("StructureDefinition-"+sd.getId()+"-shex", "yet to be done: shex as html", f.getOutputNames());

    // todo : generate schematron itself
    if (wantGen(r, "sch")) 
      fragmentError("StructureDefinition-"+sd.getId()+"-sch", "yet to be done: schematron as html", f.getOutputNames());

    // todo : generate json schema itself
    if (wantGen(r, "json-schema")) 
      fragmentError("StructureDefinition-"+sd.getId()+"-json-schema", "yet to be done: json schema as html", f.getOutputNames());

    StructureDefinitionRenderer sdr = new StructureDefinitionRenderer(context, specPath+"/", sd, Utilities.path(tempDir), igpkp, specDetails.getAsJsonObject("maps"));
    if (wantGen(r, "summary")) 
      fragment("StructureDefinition-"+sd.getId()+"-summary", sdr.summary(), f.getOutputNames());
    if (wantGen(r, "header")) 
      fragment("StructureDefinition-"+sd.getId()+"-header", sdr.header(), f.getOutputNames());
    if (wantGen(r, "diff")) 
      fragment("StructureDefinition-"+sd.getId()+"-diff", sdr.diff(igpkp.getDefinitions(sd)), f.getOutputNames());
    if (wantGen(r, "snapshot")) 
      fragment("StructureDefinition-"+sd.getId()+"-snapshot", sdr.snapshot(igpkp.getDefinitions(sd)), f.getOutputNames());
    if (wantGen(r, "template-xml")) 
      fragmentError("StructureDefinition-"+sd.getId()+"-template-xml", "yet to be done: Xml template", f.getOutputNames());
    if (wantGen(r, "template-json")) 
      fragmentError("StructureDefinition-"+sd.getId()+"-template-json", "yet to be done: Json template", f.getOutputNames());
    if (wantGen(r, "template-ttl")) 
      fragmentError("StructureDefinition-"+sd.getId()+"-template-ttl", "yet to be done: Turtle template", f.getOutputNames());
    if (wantGen(r, "uml")) 
      fragmentError("StructureDefinition-"+sd.getId()+"-uml", "yet to be done: UML as SVG", f.getOutputNames());
    if (wantGen(r, "tx")) 
      fragment("StructureDefinition-"+sd.getId()+"-tx", sdr.tx(), f.getOutputNames());
    if (wantGen(r, "inv")) 
      fragment("StructureDefinition-"+sd.getId()+"-inv", sdr.inv(), f.getOutputNames());
    if (wantGen(r, "dict")) 
      fragment("StructureDefinition-"+sd.getId()+"-dict", sdr.dict(), f.getOutputNames());
    if (wantGen(r, "maps")) 
      fragment("StructureDefinition-"+sd.getId()+"-maps", sdr.mappings(), f.getOutputNames());
    if (wantGen(r, "xref")) 
      fragmentError("StructureDefinition-"+sd.getId()+"-sd-xref", "Yet to be done: xref", f.getOutputNames());

    if (wantGen(r, "example-list")) 
      fragment("StructureDefinition-example-list-"+sd.getId(), sdr.exampleList(fileList), f.getOutputNames());
  }

  private void generateOutputsStructureMap(FetchedFile f, FetchedResource r, StructureMap resource) {
    
  }

  private XhtmlNode getXhtml(FetchedResource r) {
    if (r.getResource() != null && r.getResource() instanceof DomainResource) {
      DomainResource dr = (DomainResource) r.getResource();
      if (dr.getText().hasDiv())
        return dr.getText().getDiv();
      else
        return null;
    }
    Element text = r.getElement().getNamedChild("text");
    if (text == null)
      return null;
    Element div = text.getNamedChild("div");
    if (div == null)
      return null;
    else
      return div.getXhtml();
  }

  private void fragment(String name, String content, Set<String> outputTracker) throws IOException {
    File f = new File(Utilities.path(tempDir, "_includes", name+".xhtml"));
    outputTracker.add(f.getAbsolutePath());
    String s = f.exists() ? TextFile.fileToString(f.getAbsolutePath()) : "";
    if (!f.exists() || !s.equals(content)) { 
      TextFile.stringToFile(content, Utilities.path(tempDir, "_includes", name+".xhtml"), false);
      TextFile.stringToFile(pageWrap(content, name), Utilities.path(qaDir, name+".html"), true);
    }
  }

  private String pageWrap(String content, String title) {
    return "<html>\r\n"+
        "<head>\r\n"+
        "  <title>"+title+"</title>\r\n"+
        "  <link rel=\"stylesheet\" href=\"fhir.css\"/>\r\n"+
        "</head>\r\n"+
        "<body>\r\n"+
        content+
        "</body>\r\n"+
        "</html>\r\n";
  }

  public static void main(String[] args) throws Exception {
    if (hasParam(args, "-gui") || args.length == 0) {
      runGUI();
    } else if (hasParam(args, "-multi")) {
      for (String ig : TextFile.fileToString(getNamedParam(args, "-multi")).split("\\r?\\n")) {
        if (!ig.startsWith(";")) {
          System.out.println("=======================================================================================");
          System.out.println("Publish IG "+ig);
          Publisher self = new Publisher();
          self.setConfigFile(ig);
          self.setTxServer(getNamedParam(args, "-tx"));
          try {
            self.execute(hasParam(args, "-resetTx"));
          } catch (Exception e) {
            System.out.println("Publishing Implementation Guide Failed: "+e.getMessage());
            System.out.println("");
            System.out.println("Stack Dump (for debugging):");
            e.printStackTrace();
            break;
          }
          System.out.println("=======================================================================================");
          System.out.println("");
          System.out.println("");
        }
      }
    } else {
      System.out.println("FHIR Implementation Guide Publisher ("+Constants.VERSION+"-"+Constants.REVISION+")");
      Publisher self = new Publisher();
      self.setConfigFile(getNamedParam(args, "-ig"));
      self.setTxServer(getNamedParam(args, "-tx"));
      self.watch = hasParam(args, "-watch");

      if (self.configFile == null) {
        System.out.println("");
        System.out.println("To use this publisher, run with the commands");
        System.out.println("");
        System.out.println("-ig [source] -out [folder] -spec [path] -tx [url] -watch");
        System.out.println("");
        System.out.println("-ig: a path or a url where the implementation guide control file is found");
        System.out.println("  see Wiki for Documentation");
        System.out.println("-out: a local folder where the output from the IG publisher will be generated");
        System.out.println("-spec: the location of the FHIR specification relative to the guide");
        System.out.println("  (can be an absolute URL, or relative if the guide will be published with FHIR)");
        System.out.println("-tx: (optional) Address to use for terminology server ");
        System.out.println("  (default is http://fhir3.healthintersections.com.au)");
        System.out.println("-watch (optional): if this is present, the publisher will not terminate;");
        System.out.println("  instead, it will stay running, an watch for changes to the IG or its ");
        System.out.println("  contents and re-run when it sees changes ");
        System.out.println("");
        System.out.println("The most important output from the publisher is validation.html");
        System.out.println("");
        System.out.println("For additional information, see http://wiki.hl7.org/index.php?title=Proposed_new_FHIR_IG_build_Process");
      } else 
        try {
          self.execute(hasParam(args, "-resetTx"));
        } catch (Exception e) {
          System.out.println("Publishing Implementation Guide Failed: "+e.getMessage());
          System.out.println("");
          System.out.println("Stack Dump (for debugging):");
          e.printStackTrace();
        }
    }
  }

  public void setConfigFile(String configFile) {
    this.configFile = configFile;
  }

  private static void runGUI() {
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          GraphicalPublisher window = new GraphicalPublisher();
          window.frame.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  private void setTxServer(String s) {
    if (!Utilities.noString(s))
      txServer = s;

  }

  private static boolean hasParam(String[] args, String param) {
    for (String a : args)
      if (a.equals(param))
        return true;
    return false;
  }

  private static String getNamedParam(String[] args, String param) {
    boolean found = false;
    for (String a : args) {
      if (found)
        return a;
      if (a.equals(param)) {
        found = true;
      }
    }
    return null;
  }

  public void setLogger(IGLogger logger) {
    this.logger = logger;

  }

  @Override
  public void logMessage(String msg) {
    System.out.println(msg);    
  }

  public String getQAFile() {
    return Utilities.path(qaDir, "validation.html");
  }

  @Override
  public void logDebugMessage(String msg) {
    // ignore 
    // System.out.println(msg);    
    
  }

}
