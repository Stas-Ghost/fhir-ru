unit FHIRUtilities;

{
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
}

interface

uses
  SysUtils, Classes,
  StringSupport, GuidSupport, DateSupport, BytesSupport, OidSupport, EncodeSupport,
  AdvObjects, AdvStringBuilders, AdvGenerics,

  IdSoapMime, TextUtilities, ZLib,

  FHIRSupport, FHIRParserBase, FHIRParser, FHIRBase, FHIRTypes, FHIRResources, FHIRConstants;

Type
  ETooCostly = class (Exception);

const
  MIN_DATE = DATETIME_MIN;
  MAX_DATE = DATETIME_MAX;
  ANY_CODE_VS = 'http://www.healthintersections.com.au/fhir/ValueSet/anything';


function HumanNameAsText(name : TFhirHumanName):String;
function GetEmailAddress(contacts : TFhirContactPointList):String;
function ResourceTypeByName(name : String) : TFhirResourceType;
function isResourceName(name : String) : boolean;

Function RecogniseFHIRResourceName(Const sName : String; out aType : TFhirResourceType): boolean;
Function RecogniseFHIRResourceManagerName(Const sName : String; out aType : TFhirResourceType): boolean;
Function RecogniseFHIRFormat(Const sName : String): TFHIRFormat;
function MakeParser(lang : String; aFormat: TFHIRFormat; oContent: TStream; policy : TFHIRXhtmlParserPolicy): TFHIRParser; overload;
function MakeParser(lang : String; aFormat: TFHIRFormat; content: TBytes; policy : TFHIRXhtmlParserPolicy): TFHIRParser; overload;
function MakeComposer(lang : string; mimetype : String) : TFHIRComposer;
Function FhirGUIDToString(aGuid : TGuid):String;
function ParseXhtml(lang : String; content : String; policy : TFHIRXhtmlParserPolicy):TFhirXHtmlNode;
function geTFhirResourceNarrativeAsText(resource : TFhirDomainResource) : String;
function IsId(s : String) : boolean;
function fullResourceUri(base: String; aType : TFhirResourceType; id : String) : String; overload;
function fullResourceUri(base: String; url : String) : String; overload;
function fullResourceUri(base: String; ref : TFhirReference) : String; overload;
function isHistoryURL(url : String) : boolean;
procedure splitHistoryUrl(var url : String; var history : String);
procedure RemoveBOM(var s : String);

procedure listReferences(resource : TFhirResource; list : TFhirReferenceList);
procedure listAttachments(resource : TFhirResource; list : TFhirAttachmentList);
Function FhirHtmlToText(html : TFhirXHtmlNode):String;
function FindContainedResource(resource : TFhirDomainResource; ref : TFhirReference) : TFhirResource;
function LoadFromFormParam(part : TIdSoapMimePart; lang : String) : TFhirResource;
function LoadDTFromFormParam(part : TIdSoapMimePart; lang, name : String; type_ : TFHIRTypeClass) : TFhirType;
function LoadDTFromParam(value : String; lang, name : String; type_ : TFHIRTypeClass) : TFhirType;

function BuildOperationOutcome(lang : String; e : exception) : TFhirOperationOutcome; overload;
Function BuildOperationOutcome(lang, message : String) : TFhirOperationOutcome; overload;

function getChildMap(profile : TFHIRStructureDefinition; name, path, nameReference : String) : TFHIRElementDefinitionList;

function asUTCMin(value : TFhirInstant) : TDateTime; overload;
function asUTCMax(value : TFhirInstant) : TDateTime; overload;
function asUTCMin(value : TFhirDate) : TDateTime; overload;
function asUTCMax(value : TFhirDate) : TDateTime; overload;
function asUTCMin(value : TFhirDateTime) : TDateTime; overload;
function asUTCMax(value : TFhirDateTime) : TDateTime; overload;
function asUTCMin(value : TFhirPeriod) : TDateTime; overload;
function asUTCMax(value : TFhirPeriod) : TDateTime; overload;
function asUTCMin(value : TFhirTiming) : TDateTime; overload;
function asUTCMax(value : TFhirTiming) : TDateTime; overload;

function HasExtension(element : TFhirElement; url : string):Boolean;
function GetExtension(element : TFhirElement; url : string) : TFhirExtension;

procedure BuildNarrative(op: TFhirOperationOutcome; opDesc : String); overload;
procedure BuildNarrative(vs : TFhirValueSet); overload;
procedure ComposeXHtml(s : TAdvStringBuilder; node: TFhirXHtmlNode); overload;
function ComposeXHtml(node: TFhirXHtmlNode) : String; overload;

Function removeCaseAndAccents(s : String) : String;

type
  TFHIRProfileStructureHolder = TFhirStructureDefinitionSnapshot;
  TFHIRProfileStructureElement = TFhirElementDefinition;
  TFhirProfileStructureElementList = TFhirElementDefinitionList;
  TFhirProfileStructureElementDefinitionBinding = TFhirElementDefinitionBinding;

  TResourceWithReference = class (TAdvObject)
  private
    FReference: String;
    FResource: TFHIRResource;
    procedure SetResource(const Value: TFHIRResource);
  public
    Constructor Create(reference : String; resource : TFHIRResource);
    Destructor Destroy; override;
    property Reference : String read FReference write FReference;
    property Resource : TFHIRResource read FResource write SetResource;
  end;

  TFHIRElementHelper = class helper for TFHIRElement
  public
    procedure addExtension(url : String; t : TFhirType); overload;
    procedure addExtension(url : String; v : String); overload;
    function hasExtension(url : String) : boolean;
    function getExtension(url : String) : Integer;
    function getExtensionCount(url : String) : Integer;
    function getExtensionString(url : String) : String; overload;
    function getExtensionString(url : String; index : integer) : String; overload;
    procedure removeExtension(url : String);
    procedure setExtensionString(url, value : String);
  end;

  TFhirElementDefinitionTypeHelper = class helper for TFhirElementDefinitionType
  private
    function GetProfile: String;
  public
    property profile : String read GetProfile;
  end;

  TFHIRResourceHelper = class helper for TFHIRResource
  private
    function GetXmlId: String;
    procedure SetmlId(const Value: String);
  public
    property xmlId : String read GetXmlId write SetmlId;
  end;

  TFHIRDomainResourceHelper = class helper (TFHIRResourceHelper) for TFHIRDomainResource
  private
    function GetContained(id: String): TFhirResource;
  public
    property Contained[id : String] : TFhirResource read GetContained; default;
    procedure collapseAllContained;
    function addExtension(url : String; t : TFhirType) : TFhirExtension; overload;
    function addExtension(url : String; v : String) : TFhirExtension; overload;
    function hasExtension(url : String) : boolean;
    function getExtension(url : String) : Integer;
    function getExtensionCount(url : String) : Integer;
    function getExtensionString(url : String) : String; overload;
    function getExtensionString(url : String; index : integer) : String; overload;
    procedure removeExtension(url : String);
    procedure setExtensionString(url, value : String);
  end;

  TFhirProfileStructureSnapshotElementDefinitionTypeListHelper = class helper for TFhirElementDefinitionList
  public
    function summary : String;
  end;

  TFHIRConformanceHelper = class helper (TFHIRDomainResourceHelper) for TFHIRConformance
  public
    function rest(type_ : TFhirResourceType) : TFhirConformanceRestResource;
  end;

  TFHIRCodeableConceptHelper = class helper (TFHIRElementHelper) for TFHIRCodeableConcept
  public
    function hasCode(System, Code : String) : boolean;
  end;

  TFhirConformanceRestResourceHelper = class helper (TFHIRElementHelper) for TFhirConformanceRestResource
  public
    function interaction(type_ : TFhirTypeRestfulInteraction) : TFhirConformanceRestResourceInteraction;
  end;

  TFHIRContactPointListHelper = class helper for TFhirContactPointList
  public
    function system(type_ : TFhirContactPointSystem) : String;
    procedure setSystem(type_ : TFhirContactPointSystem; value : String);
  end;

  TFhirValueSetContactListHelper = class helper for TFhirValueSetContactList
  public
    function system(type_ : TFhirContactPointSystem) : String;
    procedure setSystem(type_ : TFhirContactPointSystem; value : String);
  end;

  TFhirValueSetHelper = class helper for TFhirValueSet
  public
    function context : string;
    function source : string;
  end;

  TFHIROperationOutcomeHelper = class helper (TFHIRDomainResourceHelper) for TFhirOperationOutcome
  public
    function rule(level : TFhirIssueSeverity; source : String; typeCode : TFhirIssueType; path : string; test : boolean; msg : string) : boolean;
    function error(source : String; typeCode : TFhirIssueType; path : string; test : boolean; msg : string) : boolean;
    function warning(source : String; typeCode : TFhirIssueType; path : string; test : boolean; msg : string) : boolean;
    function hint(source : String; typeCode : TFhirIssueType; path : string; test : boolean; msg : string) : boolean;

    function hasErrors : boolean;
  end;

  TFhirConceptMapElementHelper = class helper (TFhirElementHelper) for TFhirConceptMapElement
  public
    function systemObject : TFhirUri;
    function system : String;
  end;

  TFhirConceptMapHelper = class helper (TFhirResourceHelper) for TFhirConceptMap
  public
    function conceptList : TFhirConceptMapElementList;
    function context : string;
    function sourceDesc: String;
    function targetDesc: String;
  end;

  TFHIRBundleHelper = class helper (TFhirResourceHelper) for TFHIRBundle
  private
    function GetLinks(s: string): String;
  public
    property Links[s : string] : String read GetLinks;
    procedure deleteEntry(resource : TFHIRResource);
    class function Create(aType : TFhirBundleType) : TFhirBundle; overload;
  end;

  TFHIRCodingListHelper = class helper for TFHIRCodingList
  public
    function AddCoding(system, code, display : String) : TFHIRCoding;
  end;

  TFhirBundleLinkListHelper = class helper for TFhirBundleLinkList
  private
    function getMatch(rel: String): string;
    procedure SetMatch(rel: String; const Value: string);
  public
    procedure AddRelRef(rel, ref : String);
    function AsHeader : String;
    property Matches[rel : String] : string read getMatch write SetMatch;
  end;

  TFhirParametersHelper = class helper for TFhirParameters
  private
    function GetNamedParameter(name: String): TFhirBase;
    function GetStringParameter(name: String): String;
  public
    function hasParameter(name : String):Boolean;
    Property NamedParameter[name : String] : TFhirBase read GetNamedParameter; default;
    Property str[name : String] : String read GetStringParameter;
    procedure AddParameter(name: String; value: TFhirType); overload;
    procedure AddParameter(name: String; value: TFhirResource); overload;
    procedure AddParameter(name: String; value: boolean); overload;
    procedure AddParameter(name, value: string); overload;
  end;

  TFhirResourceMetaHelper = class helper for TFhirMeta
  public
    function HasTag(system, code : String)  : boolean;
    function addTag(system, code, display : String) : TFhirCoding;
    function removeTag(system, code : String) : boolean;
  end;

function Path(const parts : array of String) : String;


function ZCompressBytes(const s: TBytes): TBytes;
function ZDecompressBytes(const s: TBytes): TBytes;
function TryZDecompressBytes(const s: TBytes): TBytes;

function gen(coding : TFHIRCoding):String; overload;
function gen(code : TFhirCodeableConcept):String; overload;
function gen(t : TFhirType):String; overload;


implementation


function DetectFormat(oContent : TStream) : TFHIRParserClass;
var
  i : integer;
  s : String;
begin
  i := oContent.Position;
  setlength(s, ocontent.Size - oContent.Position);
  ocontent.Read(s[1], length(s));
  oContent.Position := i;
  if (pos('<', s) > 0) and ((pos('<', s) < 10)) then
    result := TFHIRXmlParser
  else
    result := TFHIRJsonParser;

end;

function MakeParser(lang : String; aFormat: TFHIRFormat; content: TBytes; policy : TFHIRXhtmlParserPolicy): TFHIRParser;
var
  mem : TBytesStream;
begin
  mem := TBytesStream.Create(content);
  try
    result := MakeParser(lang, aformat, mem, policy);
  finally
    mem.Free;
  end;
end;

function MakeParser(lang : String; aFormat: TFHIRFormat; oContent: TStream; policy : TFHIRXhtmlParserPolicy): TFHIRParser;
begin
  if aFormat = ffJSON Then
    result := TFHIRJsonParser.Create(lang)
  else if aFormat = ffXhtml then
    result := DetectFormat(oContent).create(lang)
  else
    result := TFHIRXmlParser.Create(lang);
  try
    result.source := oContent;
    result.ParserPolicy := policy;
    result.Parse;
    result.Link;
  finally
    result.free;
  end;
end;

function MakeComposer(lang : string; mimetype : String) : TFHIRComposer;
begin
  if mimeType.StartsWith('text/xml') or mimeType.StartsWith('application/xml') or mimeType.StartsWith('application/fhir+xml') or (mimetype = 'xml') then
    result := TFHIRXmlComposer.Create(lang)
  else if mimeType.StartsWith('text/json') or mimeType.StartsWith('application/json') or mimeType.StartsWith('application/fhir+json') or (mimetype = 'xml') then
    result := TFHIRJsonComposer.Create(lang)
  else if mimeType.StartsWith('text/html') or mimeType.StartsWith('text/xhtml') or mimeType.StartsWith('application/fhir+xhtml') or (mimetype = 'xhtml') then
    result := TFHIRXhtmlComposer.Create(lang)
  else
    raise Exception.Create('Format '+mimetype+' not recognised');
end;

Function FhirGUIDToString(aGuid : TGuid):String;
begin
  result := Copy(GUIDToString(aGuid), 2, 34).ToLower;
end;


function ResourceTypeByName(name : String) : TFhirResourceType;
var
  index : Integer;
begin
  index := StringArrayIndexOfSensitive(CODES_TFhirResourceType, name);
  if index < 1 then
    raise Exception.Create('Unknown resource name "'+name+'"');
  result := TFhirResourceType(index);
end;

function isResourceName(name : String) : boolean;
var
  index : Integer;
begin
  index := StringArrayIndexOfSensitive(CODES_TFhirResourceType, name);
  result := index > 0;
end;

Function RecogniseFHIRResourceName(Const sName : String; out aType : TFhirResourceType): boolean;
var
  iIndex : Integer;
Begin
  iIndex := StringArrayIndexOfSensitive(CODES_TFhirResourceType, sName);
  result := iIndex > -1;
  if result then
    aType := TFhirResourceType(iIndex);
End;

Function RecogniseFHIRResourceManagerName(Const sName : String; out aType : TFhirResourceType): boolean;
var
  iIndex : Integer;
Begin
  iIndex := StringArrayIndexOfInsensitive(CODES_TFhirResourceType, sName);
  result := iIndex > -1;
  if result then
    aType := TFhirResourceType(iIndex);
End;

Function RecogniseFHIRFormat(Const sName : String): TFHIRFormat;
Begin
  if (sName = '.xml') or (sName = 'xml') or (sName = '.xsd') or (sName = 'xsd') Then
    result := ffXml
  else if (sName = '.json') or (sName = 'json') then
    result := ffJson
  else if sName = '' then
    result := ffAsIs
  else
    raise ERestfulException.create('FHIRBase', 'RecogniseFHIRFormat', 'Unknown format '+sName, HTTP_ERR_BAD_REQUEST);
End;


function ParseXhtml(lang : String; content : String; policy : TFHIRXhtmlParserPolicy):TFhirXHtmlNode;
var
  parser : TFHIRXmlParser;
begin
  parser := TFHIRXmlParser.create(lang);
  try
    parser.ParserPolicy := policy;
    parser.source := TStringStream.Create(content);
    result := parser.ParseHtml;
  finally
    parser.free;
  end;
end;


function geTFhirResourceNarrativeAsText(resource : TFhirDomainResource) : String;
begin
  result := resource.text.div_.Content;
end;

function IsId(s : String) : boolean;
var
  i : integer;
begin
  result := length(s) in [1..ID_LENGTH];
  if result then
    for i := 1 to length(s) do
      result := result and CharInset(s[i], ['0'..'9', 'a'..'z', 'A'..'Z', '-', '.']);
end;

procedure iterateReferences(path : String; node : TFHIRObject; list : TFhirReferenceList);
var
  iter : TFHIRPropertyIterator;
  i : integer;
begin
  iter := node.createIterator(true);
  try
    while iter.More do
    begin
      if (iter.Current.list <> nil)  then
      begin
        if StringStartsWith(iter.Current.Type_, 'Reference(') then
      begin
        for i := 0 to iter.Current.List.count - 1 do
            if (iter.current.list[i] <> nil)  and not StringStartsWith(TFhirReference(iter.current.list[i]).reference, '#') then
            list.add(iter.Current.list[i].Link)
      end
        else if (iter.Current.Type_ = 'Resource') then
      begin
          for i := 0 to iter.Current.List.count - 1 do
            iterateReferences(path+'/'+iter.Current.Name, TFhirReference(iter.current.list[i]), list)
        end
        else if not ((node is TFHIRPrimitiveType) and (iter.current.name = 'value')) then
        for i := 0 to iter.Current.list.Count - 1 Do
            iterateReferences(path+'/'+iter.Current.Name, iter.Current.list[i], list);
      end;
      iter.Next;
    end;
  finally
    iter.free;
  end;
end;

procedure listReferences(resource : TFhirResource; list : TFhirReferenceList);
begin
  iterateReferences(CODES_TFhirResourceType[resource.resourceType], resource, list);
end;

procedure iterateAttachments(path : String; node : TFHIRObject; list : TFhirAttachmentList);
var
  iter : TFHIRPropertyIterator;
  i : integer;
begin
  iter := node.createIterator(true);
  try
    while iter.More do
    begin
      if (iter.Current.List <> nil)  then
        for i := 0 to iter.Current.List.Count - 1 do
          if (iter.Current.Type_ = 'Attachment') then
            list.add(iter.Current.list[i].Link)
          else if not ((node is TFHIRPrimitiveType) and (iter.current.name = 'value'))  then
            iterateAttachments(path+'/'+iter.Current.Name, iter.Current.list[i], list);
      iter.Next;
    end;
  finally
    iter.free;
  end;
end;

procedure listAttachments(resource : TFhirResource; list : TFhirAttachmentList);
begin
  iterateAttachments(CODES_TFhirResourceType[resource.resourceType], resource, list);
end;


function asUTCMin(value : TFhirInstant) : TDateTime;
begin
  if (value = nil) or (value.value = nil) then
    result := MIN_DATE
  else
    result := value.value.AsUTCDateTimeMin;
end;

function asUTCMax(value : TFhirInstant) : TDateTime;
begin
  if (value = nil) or (value.value = nil) then
    result := MAX_DATE
  else
    result := value.value.AsUTCDateTimeMax;
end;

function asUTCMin(value : TFhirDateTime) : TDateTime;
begin
  if (value = nil) or (value.value = nil) then
    result := MIN_DATE
  else
    result := value.value.AsUTCDateTimeMin;
end;

function asUTCMax(value : TFhirDateTime) : TDateTime;
begin
  if (value = nil) or (value.value = nil) then
    result := MAX_DATE
  else
    result := value.value.AsUTCDateTimeMax;
end;

function asUTCMin(value : TFhirDate) : TDateTime;
begin
  if (value = nil) or (value.value = nil) then
    result := MIN_DATE
  else
    result := value.value.AsUTCDateTimeMin;
end;

function asUTCMax(value : TFhirDate) : TDateTime;
begin
  if (value = nil) or (value.value = nil) then
    result := MAX_DATE
  else
    result := value.value.AsUTCDateTimeMax;
end;

function asUTCMin(value : TFhirPeriod) : TDateTime;
begin
  if (value = nil) or (value.start = nil) then
    result := MIN_DATE
  else
    result := value.start.AsUTCDateTimeMin;
end;

function asUTCMax(value : TFhirPeriod) : TDateTime;
begin
  if (value = nil) or (value.end_ = nil) then
    result := MAX_DATE
  else
    result := value.end_.AsUTCDateTimeMax;
end;

function asUTCMin(value : TFhirTiming) : TDateTime;
var
  i : integer;
begin
  if (value = nil) or (value.eventList.Count = 0) then
    result := MIN_DATE
  else
  begin
    result := MAX_DATE;
    for i := 0 to value.eventList.count - 1 do
      result := DateTimeMin(result, AsUTCMin(value.eventList[i]));
  end;
end;

function asUTCMax(value : TFhirTiming) : TDateTime;
var
  duration : TDateTime;
  i : integer;
begin
  if (value = nil) then
    result := MAX_DATE
  else if (value.repeat_ = nil) then
  begin
    if value.eventList.Count = 0 then
      result := MAX_DATE
    else
      result := MIN_DATE;
      for i := 0 to value.eventList.count - 1 do
        result := DateTimeMax(result, AsUTCMax(value.eventList[i]));
  end
  else if (value.repeat_.bounds <> nil) and (value.repeat_.bounds is TFhirPeriod) and (TFhirPeriod(value.repeat_.bounds).end_ <> nil) then
    result := asUTCMax(TFhirPeriod(value.repeat_.bounds).end_Element)
  else if (value.repeat_.count <> '') and (value.eventList.Count > 0) and
    (value.repeat_.frequency <> '') and (value.repeat_.period <> '') and (value.repeat_.periodunits <> UnitsOfTimeNull) then
  begin
    result := MIN_DATE;
    for i := 0 to value.eventList.count - 1 do
      result := DateTimeMax(result, AsUTCMax(value.eventList[i]));
    if result = MIN_DATE then
      result := MAX_DATE
    else
    begin
      case value.repeat_.periodunits of
        UnitsOfTimeS : duration := DATETIME_SECOND_ONE;
        UnitsOfTimeMin : duration := DATETIME_MINUTE_ONE;
        UnitsOfTimeH : duration := DATETIME_HOUR_ONE;
        UnitsOfTimeD : duration := 1;
        UnitsOfTimeWk : duration := 7;
        UnitsOfTimeMo : duration := 30;
        UnitsOfTimeA : duration := 365 // todo - how to correct for leap years?;
      else
        raise exception.create('unknown duration units "'+value.repeat_.periodunitsElement.value+'"');
      end;
      result := result + (StrToInt(value.repeat_.count) * duration / StrToInt(value.repeat_.frequency));
    end;
  end
  else
    result := MAX_DATE;
end;

{
function GetResourceFromFeed(feed : TFHIRAtomFeed; ref : TFhirReference) : TFHIRResource;
var
  i : integer;
begin
  result := nil;
  for i := 0 to feed.entries.count - 1 do
  begin
    if feed.entries[i].id = ref.reference then
    begin
      result := feed.entries[i].resource;
      break;
    end;
  end;
end;
}

function FindContainedResource(resource : TFhirDomainResource; ref : TFhirReference) : TFhirResource;
var
  i : integer;
begin
  result := nil;
  for i := 0 to resource.containedList.Count - 1 do
    if ('#'+resource.containedList[i].Id = ref.reference) then
    begin
      result := resource.containedList[i];
      exit;
    end;
end;

Function FhirHtmlToText(html : TFhirXHtmlNode):String;
begin
  result := html.AsPlainText;
end;

function BuildOperationOutcome(lang : String; e : exception) : TFhirOperationOutcome;
begin
  result := BuildOperationOutcome(lang, e.message);
end;

Function BuildOperationOutcome(lang, message : String) : TFhirOperationOutcome; overload;
var
  outcome : TFhirOperationOutcome;
  report :  TFhirOperationOutcomeIssue;
begin
  outcome := TFhirOperationOutcome.create;
  try
    outcome.text := TFhirNarrative.create;
    outcome.text.status := NarrativeStatusGenerated;
    outcome.text.div_ := ParseXhtml(lang, '<div><p>'+FormatTextToHTML(message)+'</p></div>', xppReject);
    report := outcome.issueList.Append;
    report.severity := issueSeverityError;
    report.diagnostics := message;
    result := outcome.Link;
  finally
    outcome.free;
  end;
end;

function HasExtension(element : TFhirElement; url : string):Boolean;
begin
  result := GetExtension(element, url) <> nil;
end;

function GetExtension(element : TFhirElement; url : string) : TFhirExtension;
var
  i : integer;
  ex : TFhirExtension;
begin
  result := nil;
  for i := 0 to element.ExtensionList.count - 1 do
  begin
    ex := element.ExtensionList[i];
    if ex.url = url then
    begin
      result := ex;
      exit;
    end;
  end;
end;

function gen(coding : TFHIRCoding):String; overload;
begin
  if (coding = nil) then
     result := ''
  else if (coding.DisplayElement <> nil) then
    result := coding.Display
  else if (coding.CodeElement <> nil) then
    result := coding.Code
  else
    result := '';
end;

function gen(code : TFhirCodeableConcept):String; overload;
begin
  if (code = nil) then
    result := ''
  else if (code.text <> '') then
    result := code.text
  else if (code.codingList.Count > 0) then
    result := gen(code.codingList[0])
  else
    result := '';
end;

function gen(extension : TFHIRExtension):String; overload;
begin
  if extension = nil then
    result := ''
  else if (extension.Value is TFHIRCode) then
    result := TFHIRCode(extension.value).value
  else if (extension.value is TFHIRCoding) then
    result := gen(TFHIRCoding(extension.value))
  else
    raise Exception.create('Unhandled type '+extension.Value.ClassName);
end;

procedure BuildNarrative(op: TFhirOperationOutcome; opDesc : String);
var
  x, tbl, tr, td : TFhirXHtmlNode;
  hasSource, success, d : boolean;
  i, j : integer;
  issue : TFhirOperationOutcomeIssue;
  s : TFhirString;
begin
  x := TFhirXHtmlNode.create;
  try
    x.NodeType := fhntElement;
    x.Name := 'div';
    x.AddTag('p').addTag('b').addText('Operation Outcome for :'+opDesc);

    hasSource := false;
    success := true;
    for i := 0 to op.issueList.count - 1 do
    begin
      issue := op.issueList[i];
      success := success and (issue.Severity = IssueSeverityInformation);
      hasSource := hasSource or (hasExtension(issue, 'http://hl7.org/fhir/tools#issue-source'));
    end;
    if (success) then
      x.AddChild('p').addText('All OK');
    if op.issueList.count > 0 then
    begin
      tbl := x.addTag('table');
      tbl.setAttribute('class', 'grid'); // on the basis that we'll most likely be rendered using the standard fhir css, but it doesn't really matter
      tr := tbl.addTag('tr');
      tr.addTag('td').addTag('b').addText('Severity');
      tr.addTag('td').addTag('b').addText('Location');
      tr.addTag('td').addTag('b').addText('Details');
      tr.addTag('td').addTag('b').addText('Diagnostics');
      tr.addTag('td').addTag('b').addText('Type');
      if (hasSource) then
        tr.addTag('td').addTag('b').addText('Source');
      for i := 0 to op.issueList.count - 1 do
      begin
        issue := op.issueList[i];
        tr := tbl.addTag('tr');
        tr.addTag('td').addText(CODES_TFhirIssueSeverity[issue.severity]);
        td := tr.addTag('td');
        d := false;
        for j := 0 to issue.locationList.count -1 do
        begin
           s := issue.locationList[j];
           if (d) then
             td.addText(', ')
           else
             d := true;
           td.addText(s.Value);
        end;
        tr.addTag('td').addText(gen(issue.details));
        tr.addTag('td').addText(issue.diagnostics);
        tr.addTag('td').addText(CODES_TFhirIssueType[issue.code]);
        if (hasSource) then
          tr.addTag('td').addText(gen(getExtension(issue, 'http://hl7.org/fhir/tools#issue-source')));
      end;
    end;
    if (op.Text = nil) then
      op.Text := TFhirNarrative.create;
    op.Text.div_ := x.link;
    if hasSource then
      op.Text.status := NarrativeStatusExtensions
    else
      op.Text.status := NarrativeStatusGenerated;
  finally
    x.free;
  end;
end;

procedure addTableHeaderRowStandard(t : TFhirXHtmlNode);
var
  tr, td, b : TFhirXHtmlNode;
begin
  tr := t.addTag('tr');
  td := tr.addTag('td');
  b := td.addTag('b');
  b.addText('Code');
  td := tr.addTag('td');
  b := td.addTag('b');
  b.addText('Display');
  td := tr.addTag('td');
  b := td.addTag('b');
  b.addText('Definition');
end;

procedure addTableHeaderRowExpansion(t : TFhirXHtmlNode);
var
  tr, td, b : TFhirXHtmlNode;
begin
  tr := t.addTag('tr');
  td := tr.addTag('td');
  b := td.addTag('b');
  b.addText('Code');
  td := tr.addTag('td');
  b := td.addTag('b');
  b.addText('System');
  td := tr.addTag('td');
  b := td.addTag('b');
  b.addText('Display');
end;


procedure addDefineRowToTable(t : TFhirXHtmlNode; c : TFhirValueSetCodeSystemConcept; indent : integer);
var
  tr, td : TFhirXHtmlNode;
  s : string;
  i : integer;
begin
  tr := t.addTag('tr');
  td := tr.addTag('td');
  s := StringpadLeft('', '.', indent*2);
  td.addText(s+c.Code);
  td := tr.addTag('td');
  td.addText(c.Display);
  td := tr.addTag('td');
  td.addText(c.Definition);
  for i := 0 to c.ConceptList.count - 1 do
    addDefineRowToTable(t, c.conceptList[i], indent+1);
end;

procedure addContainsRowToTable(t : TFhirXHtmlNode; c : TFhirValueSetExpansionContains; indent : integer);
var
  tr, td : TFhirXHtmlNode;
  s : string;
  i : integer;
begin
  tr := t.addTag('tr');
  td := tr.addTag('td');
  s := StringpadLeft('', '.', indent*2);
  if c.code = '' then
    td.addText(s+'+')
  else
    td.addText(s+c.Code);
  td := tr.addTag('td');
  td.addText(c.System);
  td := tr.addTag('td');
  td.addText(c.Display);
  for i := 0 to c.containsList.count - 1 do
    addContainsRowToTable(t, c.containsList[i], indent+1);
end;

procedure generateDefinition(x : TFhirXHtmlNode; vs : TFHIRValueSet);
var
  p, t : TFhirXHtmlNode;
  i : integer;
begin
  p := x.addTag('p');
  p.addText('This value set defines it''s own terms in the system '+vs.CodeSystem.System);
  t := x.addTag('table');
  addTableHeaderRowStandard(t);
  for i := 0 to vs.CodeSystem.ConceptList.Count - 1 do
    addDefineRowToTable(t, vs.CodeSystem.ConceptList[i], 0);
end;


procedure generateExpansion(x : TFhirXHtmlNode; vs : TFhirValueSet);
var
  h, p, t : TFhirXHtmlNode;
  i : integer;
begin
  h := x.addTag('h2');
  h.addText('Expansion for '+vs.Name);
  p := x.addTag('p');
  p.addText(vs.Description);
  p := x.addTag('p');
  p.addText('This value set is an expansion, and includes the following terms in the expansion');
  t := x.addTag('table');
  addTableHeaderRowExpansion(t);
  for i := 0 to vs.expansion.containsList.Count - 1 do
    addContainsRowToTable(t, vs.expansion.containsList[i], 0);
end;

procedure generateComposition(x : TFhirXHtmlNode; vs : TFhirValueSet);
begin
   raise Exception.create('todo');
end;

procedure BuildNarrative(vs : TFhirValueSet);
var
  x, h, p : TFhirXHtmlNode;
begin
  x := TFhirXHtmlNode.create;
  try
    x.NodeType := fhntElement;
    x.Name := 'div';

    if (vs.Expansion <> nil) then
      generateExpansion(x, vs)
    else
    begin
      h := x.addTag('h2');
      h.addText(vs.Name);
      p := x.addTag('p');
      p.addText(vs.Description);
      if (vs.CodeSystem <> nil) then
        generateDefinition(x, vs);
      if (vs.Compose <> nil) then
        generateComposition(x, vs);
    end;

    if (vs.Text = nil) then
      vs.Text := TFhirNarrative.create;
    vs.Text.div_ := x.link;
    vs.Text.status := NarrativeStatusGenerated;
  finally
    x.free;
  end;
end;

function GetEmailAddress(contacts : TFhirContactPointList):String;
var
  i : integer;
begin
  result := '';
  if contacts <> nil then
    for i := 0 to contacts.Count - 1 do
      if contacts[i].system = ContactPointSystemEmail then
        result := contacts[i].value;
end;

function HumanNameAsText(name : TFhirHumanName):String;
var
  i : integer;
begin
  if name = nil then
    result := ''
  else if name.text <> '' then
    result := name.text
  else
  begin
    result := '';
    for i := 0 to name.givenList.Count - 1 do
      result := result + name.givenList[i].value+' ';
    for i := 0 to name.familyList.Count - 1 do
      result := result + name.familyList[i].value+' ';
  end;
end;

function LoadDTFromFormParam(part : TIdSoapMimePart; lang, name : String; type_ : TFHIRTypeClass) : TFhirType;
var
  ct : String;
  parser : TFHIRParser;
begin
  parser := nil;
  try
    // first, figure out the format
    ct := part.Headers.Values['Content-Type'];
    if ct <> '' then
    begin
      if StringStartsWithInsensitive(ct, 'application/json') or StringStartsWithInsensitive(ct, 'application/fhir+json') or StringStartsWithInsensitive(ct, 'application/json+fhir') or StringStartsWithInsensitive(ct, 'json') or StringStartsWithInsensitive(ct, 'text/json') Then
        parser := TFHIRJsonParser.Create(lang)
      else if StringStartsWithInsensitive(ct, 'text/xml') or StringStartsWithInsensitive(ct, 'application/xml') or
          StringStartsWithInsensitive(ct, 'application/fhir+xml') or StringStartsWithInsensitive(ct, 'application/xml+fhir') or StringStartsWithInsensitive(ct, 'xml') Then
        parser := TFHIRXMLParser.Create(lang);
    end;
    if parser = nil then
      parser := DetectFormat(part.content).Create(lang);
    parser.source := part.Content;
    result := parser.ParseDT(name, type_);
  finally
    parser.Free;
  end;
end;

function LoadDTFromParam(value : String; lang, name : String; type_ : TFHIRTypeClass) : TFhirType;
var
  ct : String;
  parser : TFHIRParser;
  mem : TStringStream;
begin
  parser := TFHIRJsonParser.Create(lang);
  try
    // first, figure out the format
    mem := TStringStream.Create(value, TEncoding.UTF8);
    try
      parser.source := mem;
      result := parser.ParseDT(name, type_);
    finally
      mem.Free;
    end;
  finally
    parser.Free;
  end;
end;

function LoadFromFormParam(part : TIdSoapMimePart; lang : String) : TFhirResource;
var
  ct : String;
  parser : TFHIRParser;
begin
  parser := nil;
  try
    part.content.position := 0;
    // first, figure out the format
    ct := part.Headers.Values['Content-Type'];
    if ct <> '' then
    begin
      if StringStartsWithInsensitive(ct, 'application/json') or StringStartsWithInsensitive(ct, 'application/fhir+json') or StringStartsWithInsensitive(ct, 'application/json+fhir') or StringStartsWithInsensitive(ct, 'json') or StringStartsWithInsensitive(ct, 'text/json') Then
        parser := TFHIRJsonParser.Create(lang)
      else if StringStartsWithInsensitive(ct, 'text/xml') or StringStartsWithInsensitive(ct, 'application/xml') or
          StringStartsWithInsensitive(ct, 'application/fhir+xml') or StringStartsWithInsensitive(ct, 'application/xml+fhir') or StringStartsWithInsensitive(ct, 'xml') Then
        parser := TFHIRXMLParser.Create(lang);
    end;
    if parser = nil then
      parser := DetectFormat(part.content).Create(lang);
    parser.source := part.Content;
    parser.Parse;
    result := parser.resource.Link;
  finally
    parser.Free;
  end;
end;


(*



  procedure generateComposition(x : TFhirTFhirXHtmlNode; vs : TFHIRValueSet, Map<String, AtomEntry> codeSystems) throws Exception begin
    TFhirXHtmlNode h := x.addTag('h2');
    h.addText(vs.Name);
    TFhirXHtmlNode p := x.addTag('p');
    p.addText(vs.Description);
    p := x.addTag('p');
    p.addText('This value set includes terms defined in other code systems, using the following rules:');
    TFhirXHtmlNode ul := x.addTag('ul');
    TFhirXHtmlNode li;
    for (Uri imp : vs.Compose.Import) begin
      li := ul.addTag('li');
      li.addText('Import all the codes that are part of '+imp.Value);
    end;
    for (ConceptSetComponent inc : vs.Compose.Include) begin
      genInclude(ul, inc, 'Include', codeSystems);      
    end;
    for (ConceptSetComponent exc : vs.Compose.Exclude) begin
      genInclude(ul, exc, 'Exclude', codeSystems);      
    end;
  end;

  procedure genInclude(TFhirXHtmlNode ul, ConceptSetComponent inc, String type, Map<String, AtomEntry> codeSystems) throws Exception begin
    TFhirXHtmlNode li;
    li := ul.addTag('li');
    AtomEntry e := codeSystems.(inc.System.toString);
    
    if (inc.Code.size :=:= 0 && inc.Filter.size :=:= 0) begin then 
      li.addText(type+' all codes defined in ');
      addCsRef(inc, li, e);
    end; else begin 
      if (inc.Code.size > 0) begin then
        li.addText(type+' these codes as defined in ');
        addCsRef(inc, li, e);
      
        TFhirXHtmlNode t := li.addTag('table');
        addTableHeaderRowStandard(t);
        for (Code c : inc.Code) begin
          TFhirXHtmlNode tr := t.addTag('tr');
          TFhirXHtmlNode td := tr.addTag('td');
          td.addText(c.Value);         
          ValueSetDefineConceptComponent cc := getConceptForCode(e, c.Value);
          if (cc <> nil) begin then
            td := tr.addTag('td');
            if (!Utilities.noString(cc.Display)) then
              td.addText(cc.Display);
            td := tr.addTag('td');
            if (!Utilities.noString(cc.Definition)) then
              td.addText(cc.Definition);
          end;
        end;
      end;
      for (ConceptSetFilterComponent f : inc.Filter) begin
        li.addText(type+' codes from ');
        addCsRef(inc, li, e);
        li.addText(' where '+f.PropertyST+' '+describe(f.Op)+' ');
        if (e <> nil && codeExistsInValueSet(e, f.Value)) begin then
          TFhirXHtmlNode a := li.addTag('a');
          a.addTag(f.Value);
          a.setAttribute('href', getCsRef(e)+'#'+f.Value);
        end; else
          li.addText(f.Value);
      end;
    end;
  end;

  private String describe(FilterOperator op) begin
    switch (op) begin
    case equal: return ' := ';
    case isA: return ' is-a ';
    case isNotA: return ' is-not-a ';
    case regex: return ' matches (by regex) ';
    
    end;
    return nil;
  end;

  private ValueSetDefineConceptComponent getConceptForCode(AtomEntry e, String code) begin
    if (e :=:= nil) then
      return nil;
    vs : TFHIRValueSet := (ValueSet) e.Resource;
    if (vs.CodeSystem :=:= nil) then
      return nil;
    for (ValueSetDefineConceptComponent c : vs.CodeSystem.Concept) begin
      ValueSetDefineConceptComponent v := getConceptForCode(c, code);   
      if (v <> nil) then
        return v;
    end;
    return nil;
  end;
  
  
  
  private ValueSetDefineConceptComponent getConceptForCode(ValueSetDefineConceptComponent c, String code) begin
    if (code.equals(c.Code)) then
      return c;
    for (ValueSetDefineConceptComponent cc : c.Concept) begin
      ValueSetDefineConceptComponent v := getConceptForCode(cc, code);
      if (v <> nil) then
        return v;
    end;
    return nil;
  end;

  procedure addCsRef(ConceptSetComponent inc, TFhirXHtmlNode li, AtomEntry cs) begin
    if (cs <> nil && cs.Links.('self') <> nil) begin then
      TFhirXHtmlNode a := li.addTag('a');
      a.setAttribute('href', cs.Links.('self').replace('\\', '/'));
      a.addText(inc.System.toString);
    end; else 
      li.addText(inc.System.toString);
  end;

  private String getCsRef(AtomEntry cs) begin
    return cs.Links.('self').replace('\\', '/');
  end;

  private boolean codeExistsInValueSet(AtomEntry cs, String code) begin
    vs : TFHIRValueSet := (ValueSet) cs.Resource;
    for (ValueSetDefineConceptComponent c : vs.CodeSystem.Concept) begin
      if (inConcept(code, c)) then
        return true;
    end;
    return false;
  end;

  private boolean inConcept(String code, ValueSetDefineConceptComponent c) begin
    if (c.Code <> nil && c.Code.equals(code)) then
      return true;
    for (ValueSetDefineConceptComponent g : c.Concept) begin
      if (inConcept(code, g)) then
        return true;
    end;
    return false;
  end;

*)

Function removeCaseAndAccents(s : String) : String;
begin
  result := lowercase(s);
end;

{ TFHIROperationOutcomeHelper }


function TFHIROperationOutcomeHelper.error(source : String; typeCode : TFhirIssueType; path: string; test: boolean; msg: string): boolean;
var
  issue : TFhirOperationOutcomeIssue;
  ex : TFhirExtension;
begin
  if not test then
  begin
    issue := TFhirOperationOutcomeIssue.create;
    try
      issue.severity := IssueSeverityError;
      issue.code := typeCode;
      issue.diagnostics := msg;
      issue.locationList.Append.value := path;
      ex := issue.ExtensionList.Append;
      ex.url := 'http://hl7.org/fhir/tools#issue-source';
      ex.value := TFhirCode.create;
      TFhirCode(ex.value).value := source;
      self.issueList.add(issue.link);
      if self.text = nil then
      begin
        self.text := TFhirNarrative.Create;
        self.text.div_ := TFhirXHtmlNode.Create;
        self.text.div_.NodeType := fhntElement;
        self.text.div_.Name := 'div';
        self.text.div_.AddChild('div').SetAttribute('style', 'background: Salmon').AddText(msg);
      end;
    finally
      issue.free;
    end;
  end;
  result := test;
end;

function TFHIROperationOutcomeHelper.hasErrors: boolean;
var
  i : integer;
begin
  result := false;
  for i := 0 to issueList.Count - 1 do
    result := result or (issueList[i].severity in [IssueSeverityFatal, IssueSeverityError]);
end;

function TFHIROperationOutcomeHelper.hint(source : String; typeCode : TFhirIssueType; path: string; test: boolean; msg: string): boolean;
var
  issue : TFhirOperationOutcomeIssue;
  ex : TFhirExtension;
begin
  if not test then
  begin
    issue := TFhirOperationOutcomeIssue.create;
    try
      issue.severity := IssueSeverityInformation;
      issue.code := typeCode;
      issue.diagnostics := msg;
      issue.locationList.Append.value := path;
      ex := issue.ExtensionList.Append;
      ex.url := 'http://hl7.org/fhir/tools#issue-source';
      ex.value := TFhirCode.create;
      TFhirCode(ex.value).value := source;
      self.issueList.add(issue.link);
    finally
      issue.free;
    end;
  end;
  result := test;
end;

function TFHIROperationOutcomeHelper.rule(level: TFhirIssueSeverity; source : String; typeCode : TFhirIssueType; path: string; test: boolean; msg: string): boolean;
var
  issue : TFhirOperationOutcomeIssue;
  ex : TFhirExtension;
begin
  if not test then
  begin
    issue := TFhirOperationOutcomeIssue.create;
    try
      issue.severity := level;
      issue.code := typeCode;
      issue.diagnostics := msg;
      issue.locationList.Append.value := path;
      ex := issue.ExtensionList.Append;
      ex.url := 'http://hl7.org/fhir/tools#issue-source';
      ex.value := TFhirCode.create;
      TFhirCode(ex.value).value := source;
      self.issueList.add(issue.link);
    finally
      issue.free;
    end;
  end;
  result := test;
end;

function TFHIROperationOutcomeHelper.warning(source : String; typeCode : TFhirIssueType; path: string; test: boolean; msg: string): boolean;
var
  issue : TFhirOperationOutcomeIssue;
  ex : TFhirExtension;
begin
  if not test then
  begin
    issue := TFhirOperationOutcomeIssue.create;
    try
      issue.severity := IssueSeverityWarning;
      issue.code := typeCode;
      issue.diagnostics := msg;
      issue.locationList.Append.value := path;
      ex := issue.ExtensionList.Append;
      ex.url := 'http://hl7.org/fhir/tools#issue-source';
      ex.value := TFhirCode.create;
      TFhirCode(ex.value).value := source;
      self.issueList.add(issue.link);
    finally
      issue.free;
    end;
  end;
  result := test;
end;

{ TFHIRElementHelper }

procedure TFHIRElementHelper.addExtension(url: String; t: TFhirType);
var
  ex : TFhirExtension;
begin
  ex := self.ExtensionList.Append;
  ex.url := url;
  ex.value := t; // nolink here (done outside)
end;

procedure TFHIRElementHelper.addExtension(url, v: String);
begin
  addExtension(url, TFhirString.Create(v));
end;

function TFHIRElementHelper.getExtension(url: String): Integer;
var
  i : integer;
begin
  result := -1;
  for i := 0 to self.ExtensionList.Count -1 do
    if self.ExtensionList[i].url = url then
      result := i;
end;

function TFHIRElementHelper.getExtensionString(url: String): String;
var
  ndx : Integer;
begin
  ndx := getExtension(url);
  if (ndx = -1) then
    result := ''
  else if (self.ExtensionList.Item(ndx).value is TFhirString) then
    result := TFhirString(self.ExtensionList.Item(ndx).value).value
  else if (self.ExtensionList.Item(ndx).value is TFhirCode) then
    result := TFhirCode(self.ExtensionList.Item(ndx).value).value
  else if (self.ExtensionList.Item(ndx).value is TFhirUri) then
    result := TFhirUri(self.ExtensionList.Item(ndx).value).value
  else
    result := '';
end;

function TFHIRElementHelper.hasExtension(url: String): boolean;
begin
  result := getExtension(url) > -1;
end;

procedure TFHIRElementHelper.removeExtension(url: String);
var
  ndx : integer;
begin
  ndx := getExtension(url);
  while ndx > -1 do
  begin
    Self.ExtensionList.DeleteByIndex(ndx);
    ndx := getExtension(url);
  end;

end;

procedure TFHIRElementHelper.setExtensionString(url, value: String);
var
  ext : TFhirExtension;
begin
  removeExtension(url);
  ext := self.ExtensionList.Append;
  ext.url := url;
  ext.value := TFhirString.Create(value);
end;

function TFHIRElementHelper.getExtensionCount(url: String): Integer;
var
  i : integer;
begin
  result := 0;
  for i := 0 to self.ExtensionList.Count - 1 do
    if self.ExtensionList[i].url = url then
      inc(result);
end;

{ TFHIRDomainResourceHelper }

function TFHIRDomainResourceHelper.addExtension(url: String; t: TFhirType) : TFhirExtension;
begin
  result := self.ExtensionList.Append;
  result.url := url;
  result.value := t; // nolink here (done outside)
end;

function TFHIRDomainResourceHelper.addExtension(url, v: String) : TFhirExtension;
begin
  result := addExtension(url, TFhirString.Create(v));
end;

function TFHIRDomainResourceHelper.getExtension(url: String): Integer;
var
  i : integer;
begin
  result := -1;
  for i := 0 to self.ExtensionList.Count -1 do
    if self.ExtensionList[i].url = url then
      result := i;
end;

function TFHIRDomainResourceHelper.getExtensionCount(url: String): Integer;
var
  i : integer;
begin
  result := 0;
  for i := 0 to self.ExtensionList.Count - 1 do
    if self.ExtensionList[i].url = url then
      inc(result);
end;

function TFHIRDomainResourceHelper.getExtensionString(url: String; index: integer): String;
var
  ndx : Integer;
begin
  result := '';
  for ndx := 0 to self.ExtensionList.Count - 1 do
  begin
    if self.ExtensionList[ndx].url = url then
    begin
      if index > 0 then
        dec(index)
      else
      begin
        if (self.ExtensionList.Item(ndx).value is TFhirString) then
          result := TFhirString(self.ExtensionList.Item(ndx).value).value
        else if (self.ExtensionList.Item(ndx).value is TFhirCode) then
          result := TFhirCode(self.ExtensionList.Item(ndx).value).value
        else if (self.ExtensionList.Item(ndx).value is TFhirUri) then
          result := TFhirUri(self.ExtensionList.Item(ndx).value).value
        else
          result := '';
      end;
    end;
  end;
end;

function TFHIRDomainResourceHelper.getExtensionString(url: String): String;
var
  ndx : Integer;
begin
  ndx := getExtension(url);
  if (ndx = -1) then
    result := ''
  else if (self.ExtensionList.Item(ndx).value is TFhirString) then
    result := TFhirString(self.ExtensionList.Item(ndx).value).value
  else if (self.ExtensionList.Item(ndx).value is TFhirCode) then
    result := TFhirCode(self.ExtensionList.Item(ndx).value).value
  else if (self.ExtensionList.Item(ndx).value is TFhirUri) then
    result := TFhirUri(self.ExtensionList.Item(ndx).value).value
  else
    result := '';
end;

function TFHIRDomainResourceHelper.hasExtension(url: String): boolean;
begin
  result := getExtension(url) > -1;
end;

procedure TFHIRDomainResourceHelper.removeExtension(url: String);
var
  ndx : integer;
begin
  ndx := getExtension(url);
  while ndx > -1 do
  begin
    Self.ExtensionList.DeleteByIndex(ndx);
    ndx := getExtension(url);
  end;

end;

procedure TFHIRDomainResourceHelper.setExtensionString(url, value: String);
var
  ext : TFhirExtension;
begin
  removeExtension(url);
  ext := self.ExtensionList.Append;
  ext.url := url;
  ext.value := TFhirString.Create(value);
end;

{ TFHIRConformanceHelper }

function TFHIRConformanceHelper.rest(type_: TFhirResourceType): TFhirConformanceRestResource;
var
  i : integer;
  j : integer;
begin
  result := nil;
  for I := 0 to self.restlist.count - 1 do
    if self.restlist[i].mode = RestfulConformanceModeServer then
      for j := 0 to self.restlist[i].resourceList.count - 1 do
        if self.restlist[i].resourceList[j].type_ = CODES_TFhirResourceType[type_] then
        begin
          result := self.restlist[i].resourceList[j];
          exit;
        end;
end;

{ TFhirConformanceRestResourceHelper }

function TFhirConformanceRestResourceHelper.interaction(type_: TFhirTypeRestfulInteraction): TFhirConformanceRestResourceInteraction;
var
  i : integer;
begin
  result := nil;
  for i := 0 to self.interactionList.count - 1 do
    if (self.interactionList[i].code = type_) then
      result := self.interactionList[i];



end;

{ TFhirValueSetHelper }


{ TFHIRContactPointListHelper }

procedure TFHIRContactPointListHelper.setSystem(type_: TFhirContactPointSystem; value: String);
var
  i : integer;
  c : TFhirContactPoint;
begin
  for i := 0 to self.Count - 1 do
    if Item(i).system = type_ then
    begin
      Item(i).value := value;
      exit;
    end;
  c := self.Append;
  c.system := type_;
  c.value := value;
end;

function TFHIRContactPointListHelper.system(type_: TFhirContactPointSystem): String;
var
  i : integer;
begin
  result := '';
  for i := 0 to self.Count - 1 do
    if Item(i).system = type_ then
      result := Item(i).value;
end;


function ZCompressBytes(const s: TBytes): TBytes;
begin
  ZCompress(s, result);
end;

function TryZDecompressBytes(const s: TBytes): TBytes;
begin
  try
    result := ZDecompressBytes(s);
  except
    result := s;
  end;
end;

function ZDecompressBytes(const s: TBytes): TBytes;
  {$IFNDEF WIN64}
var
  buffer: Pointer;
  size  : Integer;
  {$ENDIF}
begin
  {$IFDEF WIN64}
  ZDecompress(s, result);
  {$ELSE}
  ZDecompress(@s[0],Length(s),buffer,size);

  SetLength(result,size);
  Move(buffer^,result[0],size);

  FreeMem(buffer);
  {$ENDIF}
end;

{ TFhirConceptMapElementHelper }

function TFhirConceptMapElementHelper.systemObject: TFhirUri;
  begin
  result := codeSystemElement;
  end;

function TFhirConceptMapElementHelper.system: String;
begin
  result := codeSystem;
end;

{ TFhirConceptMapHelper }

function TFhirConceptMapHelper.conceptList: TFhirConceptMapElementList;
begin
  result := elementList;
end;

function TFhirConceptMapHelper.context: string;
var
  i: Integer;
begin
  result := '';
  for i := 0 to useContextList.Count - 1 do
    result := result + gen(useContextList[i]);
end;

function TFhirConceptMapHelper.sourceDesc: String;
begin
  if source = nil then
    result := ''
  else if source is TFhirUri then
    result := TFhirUri(source).value
  else
    result := TFhirReference(source).reference
end;

function TFhirConceptMapHelper.targetDesc: String;
begin
  if target = nil then
    result := ''
  else if target is TFhirUri then
    result := TFhirUri(target).value
  else
    result := TFhirReference(target).reference
end;


{ TFHIRDomainResourceHelper }

procedure TFHIRDomainResourceHelper.collapseAllContained;
var
  i : integer;
begin
  i := 0;
  while (i < ContainedList.Count) do
  begin

    if containedList[i] is TFhirDomainResource then
begin
      containedList.AddAll(TFhirDomainResource(containedList[i]).containedList);
      TFhirDomainResource(containedList[i]).containedList.Clear;
    end;
    inc(i);
  end;
end;

function TFHIRDomainResourceHelper.GetContained(id: String): TFhirResource;
var
  i : integer;
begin
  result := nil;
  for i := 0 to containedList.Count - 1 do
    if containedList[i].Id = id then
      result := containedList[i];
end;

{ TFhirProfileStructureSnapshotElementDefinitionTypeListHelper }

function TFhirProfileStructureSnapshotElementDefinitionTypeListHelper.summary: string;
var
  i : integer;
begin
  result := '';
  for i := 0 to Count - 1 do
    result := result + ','+Item(i).name;
  if result <> '' then
    result := result.Substring(1);
end;

{ TResourceWithReference }

constructor TResourceWithReference.Create(reference: String; resource: TFHIRResource);
begin
  inherited Create;
  self.Reference := reference;
  self.Resource := resource;

end;

destructor TResourceWithReference.Destroy;
begin
  FResource.free;
  inherited;
end;

procedure TResourceWithReference.SetResource(const Value: TFHIRResource);
begin
  FResource.free;
  FResource := Value;
end;


{ TFHIRResourceHelper }

function TFHIRResourceHelper.GetXmlId: String;
begin
  result := id;
end;

procedure TFHIRResourceHelper.SetmlId(const Value: String);
begin
  id := value;
end;

{ TFHIRBundleHelper }

class function TFHIRBundleHelper.Create(aType: TFhirBundleType): TFhirBundle;
begin
  result := TFhirBundle.Create;
  result.type_ := aType;
end;

procedure TFHIRBundleHelper.deleteEntry(resource: TFHIRResource);
var
  i : integer;
begin
  for i := entryList.Count -1 downto 0 do
    if entryList[i].resource = resource then
      entrylist.DeleteByIndex(i);
end;

function TFHIRBundleHelper.GetLinks(s: string): String;
var
  i : integer;
begin
  result := '';
  for i := 0 to link_List.count -  1 do
    if link_List[i].relation = s then
    begin
      result := link_List[i].url;
      exit;
    end;
end;

{ TFHIRCodingListHelper }

function TFHIRCodingListHelper.AddCoding(system, code, display: String) : TFHIRCoding;
var
  c : TFHIRCoding;
begin
  c := append;
  c.system := system;
  c.code := code;
  c.display := display;
end;

//function TFHIRCodingListHelper.AsHeader: String;
//begin
//  raise Exception.Create('todo');
//end;
//
//procedure TFHIRCodingListHelper.CopyTags(meta: TFHIRMeta);
//begin
//  AddAll(meta.tagList);
//  AddAll(meta.securityList);
//  !
//end;
//
//function TFHIRCodingListHelper.getCoding(system, code: String): TFHIRCoding;
//begin
//  raise Exception.Create('todo');
//end;
//
//function TFHIRCodingListHelper.hasCoding(system, code: String): boolean;
//begin
//  raise Exception.Create('todo');
//end;
//
//procedure TFHIRCodingListHelper.CopyCodings(tags: TFHIRCodingList);
//begin
//  raise Exception.Create('todo');
//end;
//
//function TFHIRCodingListHelper.json: TBytes;
//begin
//  SetLength(result, 0);
//end;
//
//procedure TFHIRCodingListHelper.WriteTags(meta: TFHIRMeta);
//begin
//  raise Exception.Create('todo');
//end;
//
{ TFhirBundleLinkListHelper }

procedure TFhirBundleLinkListHelper.AddRelRef(rel, ref: String);
var
  link : TFhirBundleLink;
begin
  link := Append;
  link.relation := rel;
  link.url := ref;
end;

function TFhirBundleLinkListHelper.AsHeader: String;
var
  i : integer;
  bl : TFhirBundleLink;
begin
  result := '';
  for i := 0 to Count - 1 do
begin
    bl := Item(i);
    if (result <> '') then
      result := result +', ';
    result := result + '<'+bl.url+'>;rel='+bl.relation;
  end;
end;

function TFhirBundleLinkListHelper.getMatch(rel: String): string;
var
  i : integer;
begin
  result := '';
  for i := 0 to count - 1 do
    if Item(i).relation = rel then
      result := Item(i).url;

end;

procedure TFhirBundleLinkListHelper.SetMatch(rel: String; const Value: string);
begin
  raise Exception.Create('todo');
end;

function fullResourceUri(base: String; aType : TFhirResourceType; id : String) : String;
begin
  if (base = 'urn:oid:') then
  begin
    if isOid(id) then
      result := base+id
    else
      raise Exception.Create('The resource id "'+'" has a base of "urn:oid:" but is not a valid OID');
  end
  else if (base = 'urn:uuid:') then
  begin
    if isGuid(id) then
      result := base+id
    else
      raise Exception.Create('The resource id "'+id+'" has a base of "urn:uuid:" but is not a valid UUID');
  end
  else if not base.StartsWith('http://') and not base.StartsWith('https://')  then
    raise Exception.Create('The resource base of "'+base+'" is not understood')
  else
    result := AppendForwardSlash(base)+CODES_TFhirResourceType[aType]+'/'+id;
end;

function fullResourceUri(base: String; ref : TFhirReference) : String; overload;
var
  url : String;
begin
  url := ref.reference;
  if url = '' then
    result := ''
  else if url.StartsWith('urn:oid:') or url.StartsWith('urn:uuid:') or url.StartsWith('http://') or url.StartsWith('https://') then
    result := url
  else if not base.StartsWith('http://') and not base.StartsWith('https://')  then
    raise Exception.Create('The resource base of "'+base+'" is not understood')
  else
    result := AppendForwardSlash(base)+url;
end;

function fullResourceUri(base: String; url : String) : String; overload;
begin
  if url = '' then
    result := ''
  else if url.StartsWith('urn:oid:') or url.StartsWith('urn:uuid:') or url.StartsWith('http://') or url.StartsWith('https://') then
    result := url
  else if not base.StartsWith('http://') and not base.StartsWith('https://')  then
    raise Exception.Create('The resource base of "'+base+'" is not understood')
  else
    result := AppendForwardSlash(base)+url;
end;

function isHistoryURL(url : String) : boolean;
begin
  result := url.Contains('/_history/') and IsId(url.Substring(url.IndexOf('/_history/')+10));
end;

procedure splitHistoryUrl(var url : String; var history : String);
begin
  history := url.Substring(url.IndexOf('/_history/')+10);
  url := url.Substring(0, url.IndexOf('/_history/'));
end;
{ TFhirParametersHelper }

procedure TFhirParametersHelper.AddParameter(name: String; value: TFhirType);
var
  p : TFhirParametersParameter;
begin
  p := self.parameterList.Append;
  p.name := name;
  p.value := value.Link;
end;

procedure TFhirParametersHelper.AddParameter(name: String; value: TFhirResource);
var
  p : TFhirParametersParameter;
begin
  p := self.parameterList.Append;
  p.name := name;
  p.resource := value.Link;
end;

procedure TFhirParametersHelper.AddParameter(name: String; value: boolean);
var
  p : TFhirParametersParameter;
begin
  p := self.parameterList.Append;
  p.name := name;
  p.value := TFhirBoolean.Create(value);
end;

procedure TFhirParametersHelper.AddParameter(name, value: string);
var
  p : TFhirParametersParameter;
begin
  p := self.parameterList.Append;
  p.name := name;
  p.value := TFhirString.Create(value);
end;

function TFhirParametersHelper.GetNamedParameter(name: String): TFhirBase;
var
  i: Integer;
begin
  for i := 0 to parameterList.Count - 1 do
    if (parameterList[i].name = name) then
    begin
      if parameterList[i].valueElement <> nil then
        result := parameterList[i].valueElement.Link
      else
        result := parameterList[i].resourceElement.Link;
      exit;
    end;
  result := nil;
end;

function TFhirParametersHelper.GetStringParameter(name: String): String;
begin
  result := (NamedParameter[name] as TFhirPrimitiveType).StringValue;
end;

function TFhirParametersHelper.hasParameter(name: String): Boolean;
var
  i: Integer;
begin
  for i := 0 to parameterList.Count - 1 do
    if (parameterList[i].name = name) then
begin
      result := true;
      exit;
    end;
  result := false;
end;

{ TFHIRCodeableConceptHelper }

function TFHIRCodeableConceptHelper.hasCode(System, Code: String): boolean;
var
  i : integer;
begin
  result :=  false;
  if self <> nil then
    for i := 0 to codingList.Count - 1 do
      if (codingList[i].system = system) and (codingList[i].code = code) then
      begin
        result := true;
        break;
      end;
end;

procedure RemoveBOM(var s : String);
begin
  if s.startsWith(#$FEFF) then
    s := s.substring(1);
end;


{ TFhirResourceMetaHelper }

function TFhirResourceMetaHelper.addTag(system, code, display: String): TFhirCoding;
var
  c : TFhirCoding;
begin
  if not hasTag(system, code) then
  begin
    c := tagList.Append;
    c.system := system;
    c.code := code;
    c.display := display;
  end;
end;

function TFhirResourceMetaHelper.HasTag(system, code: String): boolean;
var
  i : integer;
begin
  result := false;
  for i := 0 to taglist.Count - 1 do
    result := result or (taglist[i].system = system) and (taglist[i].code = code);
end;

function TFhirResourceMetaHelper.removeTag(system, code : String): boolean;
var
  i : integer;
  c : TFhirCoding;
begin
  result := false;
  for i := TagList.Count -1 downto 0 do
  begin
    c := TagList[i];
    if (c.system = system) and (c.code = code) then
    begin
      result := true;
      TagList.DeleteByIndex(i);
    end;
  end;
end;

function Path(const parts : array of String) : String;
var
  i : integer;
begin
  if length(parts) = 0 then
    result := ''
  else
    result := parts[0];
  for i := 1 to high(parts) do
    result := IncludeTrailingPathDelimiter(result) + parts[i];
end;


function TFHIRElementHelper.getExtensionString(url: String; index: integer): String;
var
  ndx : Integer;
begin
  result := '';
  for ndx := 0 to self.ExtensionList.Count - 1 do
  begin
    if self.ExtensionList[ndx].url = url then
    begin
      if index > 0 then
        dec(index)
      else
      begin
        if (self.ExtensionList.Item(ndx).value is TFhirString) then
          result := TFhirString(self.ExtensionList.Item(ndx).value).value
        else if (self.ExtensionList.Item(ndx).value is TFhirCode) then
          result := TFhirCode(self.ExtensionList.Item(ndx).value).value
        else if (self.ExtensionList.Item(ndx).value is TFhirUri) then
          result := TFhirUri(self.ExtensionList.Item(ndx).value).value
        else
          result := '';
      end;
    end;
  end;
end;

{ TFhirValueSetContactListHelper }

procedure TFhirValueSetContactListHelper.setSystem(type_: TFhirContactPointSystem; value: String);
var
  i : integer;
  c : TFhirContactPoint;
begin
  if Count = 0 then
    Append;
  for i := 0 to Item(0).telecomList.Count - 1 do
    if Item(0).telecomList[i].system = type_ then
    begin
      Item(0).telecomList[i].value := value;
      exit;
    end;
  c := Item(0).telecomList.Append;
  c.system := type_;
  c.value := value;
end;

function TFhirValueSetContactListHelper.system(type_: TFhirContactPointSystem): String;
var
  i, j : integer;
begin
  result := '';
  for j := 0 to Count - 1 do
    for i := 0 to Item(j).telecomList.Count - 1 do
     if Item(j).telecomList[i].system = type_ then
       result := Item(j).telecomList[i].value;
end;

{ TFhirValueSetHelper }

function TFhirValueSetHelper.context: string;
var
  i: Integer;
begin
  result := '';
  for i := 0 to useContextList.Count - 1 do
    result := result + gen(useContextList[i]);
end;

function csName(url : string) : String;
    begin
  if url.StartsWith('http://hl7.org/fhir/v2') then
    result := 'V2 '
  else if url.StartsWith('http://hl7.org/fhir/v3') then
    result := 'V3 '
  else if url.StartsWith('http://hl7.org/fhir') then
    result := 'FHIR '
  else if url = 'http://snomed.info/sct' then
    result := 'SCT '
  else if url = 'http://loinc.org' then
    result := 'LOINC '
  else
    result := 'Other';
end;
function TFhirValueSetHelper.source: string;
var
  b : TAdvStringBuilder;
  comp : TFhirValueSetComposeInclude;
begin
  b := TAdvStringBuilder.Create;
  try
    if codeSystem <> nil then
      b.Append(csName(codeSystem.system));
    if (compose <> nil) then
      for comp in compose.includeList do
        b.Append(csName(comp.system));
    result := b.AsString;
  finally
    b.Free;
  end;
end;

procedure ComposeXHtml(s : TAdvStringBuilder; node: TFhirXHtmlNode);
var
  i : Integer;
begin
  if node = nil then
    exit;
  case node.NodeType of
    fhntText : s.Append(EncodeXML(node.Content, xmlText));
    fhntComment : s.Append('<!-- '+EncodeXML(node.Content, xmlText)+' -->');
    fhntElement :
      begin
      s.append('<'+node.name);
      for i := 0 to node.Attributes.count - 1 do
        s.Append(' '+node.Attributes[i].Name+'="'+EncodeXML(node.Attributes[i].Value, xmlAttribute)+'"');
      if node.ChildNodes.count = 0 then
        s.append('/>')
      else
begin
        s.append('>');
        for i := 0 to node.ChildNodes.count - 1 do
          ComposeXHtml(s, node.ChildNodes[i]);
        s.append('</'+node.name+'>')
      end;
end;
    fhntDocument:
      for i := 0 to node.ChildNodes.count - 1 do
        ComposeXHtml(s, node.ChildNodes[i]);
  else
    raise exception.create('not supported');
  end;
end;


function ComposeXHtml(node: TFhirXHtmlNode) : String;
var
  b : TAdvStringBuilder;
begin
  b := TAdvStringBuilder.Create;
  try
    ComposeXHtml(b, node);
    result := b.AsString;
  finally
    b.Free;
  end;

end;

function gen(t : TFhirType):String;
begin
  if (t = nil) then
    result := ''
  else if t is TFhirCodeableConcept then
    result := gen(TFhirCodeableConcept(t))
  else if t is TFhirCoding then
    result := gen(TFhirCoding(t))
  else if t is TFhirString then
    result := TFhirString(t).value
  else if t is TFhirEnum then
    result := TFhirEnum(t).value
  else if t is TFhirBoolean then
    if TFhirBoolean(t).value then
      result := 'true'
    else
      result := 'false'
  else
    raise Exception.Create('Type '+t.className+' not handled yet');
end;


{ TFhirElementDefinitionTypeHelper }

function TFhirElementDefinitionTypeHelper.GetProfile: String;
begin
  if profileList.Count = 1 then
    result := profileList[0].value
  else
    result := '';
end;

{*
 * Given a Structure, navigate to the element given by the path and return the direct children of that element
 *
 * @param structure The structure to navigate into
 * @param path The path of the element within the structure to get the children for
 * @return A Map containing the name of the element child (not the path) and the child itself (an Element)
 * @throws Exception
 *}
function getChildMap(profile : TFHIRStructureDefinition; name, path, nameReference : String) : TFHIRElementDefinitionList;
var
   found : boolean;
   e : TFhirElementDefinition;
   p, tail : String;
begin
  result := TFHIRElementDefinitionList.create();
  // if we have a name reference, we have to find it, and iterate it's children
  if (nameReference <> '') then
  begin
    found := false;
    for e in profile.Snapshot.ElementList do
    begin
      if (nameReference = e.Name) then
      begin
        found := true;
        path := e.Path;
      end;
    end;
    if (not found) then
      raise Exception.create('Unable to resolve name reference '+nameReference+' at path '+path);
  end;

  for e in profile.Snapshot.ElementList do
  begin
    p := e.Path;
    if (path <> '') and (e.NameReference <> '') and (path.startsWith(p)) then
    begin
      {* The path we are navigating to is on or below this element, but the element defers its definition to another named part of the
       * structure.
       *}
      if (path.length > p.length) then
      begin
        // The path navigates further into the referenced element, so go ahead along the path over there
        result.free;
        result := getChildMap(profile, name, e.NameReference+'.'+path.substring(p.length+1), '');
        exit;
      end
      else
      begin
        // The path we are looking for is actually this element, but since it defers it definition, go get the referenced element
        result.free;
        result := getChildMap(profile, name, e.NameReference, '');
        exit;
      end;
    end
    else if (p.startsWith(path+'.')) then
    begin
      // The path of the element is a child of the path we're looking for (i.e. the parent),
      // so add this element to the result.
      tail := p.substring(path.length+1);
      // Only add direct children, not any deeper paths
      if (not tail.contains('.')) then
        result.add(e.Link);
    end;
  end;
end;

end.

