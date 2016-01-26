unit FHIRSupport;

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



{!Wrapper uses Classes,FHIRBase,FHIRResources,FHIRResources_Wrapper, FHIRTypes_Wrapper, FHIRTypes}

interface

uses
  Classes,
  SysUtils,
  IdGlobal,
  Parsemap, TextUtilities,
  StringSupport, DecimalSupport, GuidSupport,
  AdvObjects, AdvBuffers, AdvStringLists, AdvStringMatches, AdvJson,
  MimeMessage, DateAndTime, JWT, SCIMObjects,
  FHirBase, FHirResources, FHIRConstants, FHIRTypes, FHIRSecurity, FHIRTags, FHIRLang;

Const
   HTTP_OK_200 = 200;
   HTTP_OK_220 = 220;
   HTTP_CREATED = 201;
   HTTP_ACCEPTED = 202;
   HTTP_NO_CONTENT = 204;
   HTTP_REDIR_MULTIPLE_CHOICE = 300;
   HTTP_REDIR_MOVED_PERMANENT = 301;
   HTTP_REDIR_MOVED_TEMPORARY = 302;
   HTTP_REDIR_AFTER_POST = 303;
   HTTP_REDIR_NOT_MODIFIED = 304;
   HTTP_ERR_BAD_REQUEST = 400;
   HTTP_ERR_UNAUTHORIZED = 401;
   HTTP_ERR_FORBIDDEN = 403;
   HTTP_ERR_NOTFOUND = 404;
   HTTP_ERR_METHOD_ILLEGAL = 405;
   HTTP_ERR_CONFLICT = 409;
   HTTP_ERR_DELETED = 410;
   HTTP_ERR_PRECONDITION_FAILED = 412;
   HTTP_ERR_BUSINESS_RULES_FAILED = 422;
   HTTP_ERR_INTERNAL = 500;

Type
  TFHIRRequestOrigin = (roRest, roOperation, roConfig, roSubscription, roSweep, roUpload);
  {$M+}


  {@Class TFhirSession
    User session associated with a FHIR request.
    There is always a session, but it may only have ip address

    If the user has logged in using OAuth, then the user may have identifying details
    if the user login matches a User resource, that will be available too. The information
    may differ if the OAuth provided information differs from that in the resource (the
    resource is not updated by the OAuth information)
  }
  {!.Net HL7Connect.Fhir.Request}
  TFhirSession = class (TAdvObject)
  private
    FProvider : TFHIRAuthProvider;
    FId : String;
    FName : String;
    FCookie : String;
    FExpires : TDateTime;
    FOriginal: String;
    FKey: Integer;
    FEmail: String;
    FInnerToken, FOuterToken: String;
    FNextTokenCheck: TDateTime;
    FUseCount: integer;
    FFirstCreated: TDateTime;
    FJwt : TJWT;
    FJwtPacked : String;
    FTaggedCompartments : TStringList;
    FPatientList: TStringList;
    FsessionLength: String;
    FUser : TSCIMUser;
    Fanonymous : boolean;
    FSecurity : TFHIRSecurityRights;
    FSecure : boolean;
    FUserKey: integer;

    procedure SetJwt(const Value: TJWT);
    procedure SetUser(const Value: TSCIMUser);
    function GetScopes: String;
    procedure setScopes(scopes: String);
  public
    Constructor Create(secure : boolean);
    destructor Destroy; Override;
    function Link : TFhirSession; overload;
    procedure describe(b : TStringBuilder);
    Property scopes : String read GetScopes write SetScopes;

    {@member Key
      Primary database key for this session. Don't change!
    }
    Property Key : Integer read FKey write FKey;

    {@member Provider
      Which Authorization provider scopes the login (blank = direct login)
    }
    Property Provider : TFHIRAuthProvider read FProvider write FProvider;

   {@member InnerToken
      the OAuth authorization token (Don't change!)
      This is the OAuth token for the identity server
    }
    Property InnerToken : String read FInnerToken write FInnerToken;

    {@member OuterToken
      the OAuth authorization token (Don't change!)
      This is the OAuth token for this server
    }

    Property OuterToken : String read FOuterToken write FOuterToken;    {@member Id
      OAuth provided user id
    }
    Property Id : String read FId write FId;

    {@member UserKey
      OAuth provider internal key
    }
    property UserKey : integer read FUserKey write FUserKey;

    {@member Name
      OAuth provider User name
    }
    Property Name : String read FName write FName;

    {@member Email
      OAuth provided Users email address
    }
    Property Email : String read FEmail write FEmail;

    {@member Cookie
      the shared secret between client and server that identifies this session (i.e. cookie value). (Don't change!)
    }
    Property Cookie : String read FCookie write FCookie;

    {@member Expires
      When this session expires
    }
    Property Expires : TDateTime read FExpires write FExpires;

    Property FirstCreated : TDateTime read FFirstCreated;

    {@member NextTokenCheck
      When the token is next going to be checked with the OAuth server
    }
    Property NextTokenCheck : TDateTime read FNextTokenCheck write FNextTokenCheck;

    {@member OriginalUrl
      The url that the session started with (used through the login process)
    }
    Property OriginalUrl : String read FOriginal write FOriginal;

    {@member User
      User resource associated with this session (if a matching one exists)
    }
    Property User : TSCIMUser read FUser write SetUser;

    {@member JWT
      The JWT token (Open ID Connect token) associated with this session
    }
    Property JWT : TJWT read FJwt write SetJwt;

    {@member JWTPacked
      The JWT packed and signed using RSA
    }
    Property JWTPacked : string read FJWTPacked write FJWTPacked;

    Property useCount : integer read FUseCount write FUseCount;
    function canRead(aResourceType : TFhirResourceType):boolean;
    function canReadAll : boolean;
    function canWrite(aResourceType : TFhirResourceType):boolean;
    function canGetUser : boolean;
    function canAdministerUsers : boolean;
    procedure allowAll; // for internal use

    property TaggedCompartments : TStringList read FTaggedCompartments;
    property sessionLength : String read FsessionLength write FsessionLength;
    property PatientList : TStringList read FPatientList;
    Property anonymous : boolean read Fanonymous write Fanonymous;
  end;

  {@Class TFHIRRequest
    A FHIR request.

    The request may have been received over a FHIR RESTful interface, or by receiving
    a resource or an atom bundle directly from some other kind of interface.

    The request may be modified by a script. HL7Connect will ignore changes to the following
    properties: url, baseURL, resourceType, and format. These properties should be treated as read-only,
    but the other properties can be changed as desired
  }
  {!.Net HL7Connect.Fhir.Request}
  TFHIRRequest = class (TAdvObject)
  Private
    FId: String;
    FSubId: String;
    FCommandType: TFHIRCommandType;
    FResourceType: TFhirResourceType;
    FFormat: TFHIRFormat;
    FResource: TFhirResource;
    FUrl: String;
    FBaseUrl: String;
    ForiginalId: String;
    FversionId: String;
    FlastModifiedDate: TDateTime;
    FParams: TParseMap;
    FSource: TAdvBuffer;
//    FcontentLocation: String;
    FDefaultSearch: boolean;
    FLang: String;
    FSession: TFhirSession;
    FTags : TFHIRTagList;
    FIp: string;
    FCompartments: String;
    FCompartmentId: String;
    FForm: TMimeMessage;
    FOperationName: String;
    FIfMatch : String;
    FProvenance: TFhirProvenance;
    FIfNoneMatch: String;
    FIfModifiedSince: TDateTime;
    FIfNoneExist: String;
    FSummary: TFHIRSummaryOption;
    FOrigin : TFHIRRequestOrigin;
    FSecure : Boolean;
    FPatch: TJsonArray;
    procedure SetResource(const Value: TFhirResource);
    procedure SetSource(const Value: TAdvBuffer);
    procedure SetSession(const Value: TFhirSession);
    function GetBundle: TFhirBundle;
    procedure SetBundle(const Value: TFhirBundle);
    procedure SetProvenance(const Value: TFhirProvenance);
    procedure processParams;
    procedure SetForm(const Value: TMimeMessage);
    procedure SetPatch(const Value: TJsonArray);
  Public
    Constructor Create(origin : TFHIRRequestOrigin);
    Destructor Destroy; Override;
    Function Link : TFHIRRequest; Overload;

    {!Script Hide}
    Function Compose : String;
    procedure LoadParams(s : String); overload;
    procedure LoadParams(form : TMimeMessage); overload;
    Function LogSummary : String;
    function XMLSummary : String;
    Procedure CopyPost(stream : TStream);
    Property Source : TAdvBuffer read FSource write SetSource;
    Property Session : TFhirSession read FSession write SetSession;
    Property ip : string read FIp write FIp;
    Property form : TMimeMessage read FForm write SetForm;
    function canRead(aResourceType : TFhirResourceType):boolean;
    function canWrite(aResourceType : TFhirResourceType):boolean;
    function canGetUser : boolean;
    procedure reset;

    // main rest function. Set the following things before calling this:
    // form
    // also, the base must be stipped out before calling this
    function preAnalyse(url : String) : String;
    procedure analyse(sCommand, sUrl : String; out relativeReferenceAdjustment : integer);

    Property DefaultSearch : boolean read FDefaultSearch write FDefaultSearch;

    {@member Parameters
      any parameters associated with the request (part after the ? in the url). Use
      for search/update
    }
    property Parameters : TParseMap read FParams;

    {!Script Show}

  published
    {@member url
      The full URL of the original request, if the request was made on a RESTful interface (else empty)
    }
    property url : String read FUrl write FUrl;

    {@member secure
      Whether the request was made on an SSL interface or not (SSL = smart on fhir as well)
    }
    property secure : Boolean read FSecure write FSecure;

    {@member baseUrl
      The baseURL (see the FHIR specification under "HTTP REST interface) of the interface,
      if the request was made on a RESTful interface (else empty)
    }
    property baseUrl : String read FBaseUrl write FBaseUrl;

    {@member ResourceType
      The type of the resource. Cannot be changed

      frtNull if this is a bundle
    }
    Property ResourceType : TFhirResourceType Read FResourceType write FResourceType;

    {@member CommandType
      The command (http transaction). This can be changed, though it is unusual to
      change the command (consequences can be unexpected and dramatic)

      fcmdUnknown if this is not on a RESTful interface
    }
    Property CommandType : TFHIRCommandType Read FCommandType Write FCommandType;

    {@member Id
      The resource id associated with the request, if one is identified as part of the request
    }
    Property Id : String Read FId write FId;

    {@member SubId
      A secondary id associated with the request (only used for the version id in a version specific request)
    }
    Property SubId : String Read FSubId write FSubId;

    {@member OperationName
      The name of an operation, if an operation was invoked
    }
    Property OperationName : String read FOperationName write FOperationName;

    {@member PostFormat
      The format of the request, if known and identified (xml, json, or xhtml). Derived
      from the content-type and/or extension in the url, or configuration
    }
    Property PostFormat : TFHIRFormat read FFormat write FFormat;

    {@member Resource
      the actual resource, if a resource was submitted as part of the request.

      Note that actual kind of the resource will be one of the ones defined as
      part of the FHIR specification
    }
    Property Resource : TFhirResource read FResource write SetResource;

    Property patch : TJsonArray read FPatch write SetPatch;
    {@member Tags
      Tags on the request - if it's a resource directly
    }
    property Tags : TFHIRTagList read FTags;

    {@member originalId
      The specified originalId of the resource in the request (if present) (i.e. in a transaction)
    }
    Property originalId : String read ForiginalId write ForiginalId;

    {@member compartments
      If the user is limited to a set of compartments, this is the list (comma separated, with quotes)
    }
    Property compartments : String read FCompartments write FCompartments;

    {@member compartmentId
      if operation is limited to a patient compartment, the id in the compartment
    }
    Property compartmentId : String read FCompartmentId write FCompartmentId;

    {@member contentLocation
      Quoted Content location on request. Used for version aware updates. Only on RESTful interface
    }
//    Property contentLocation : String read FcontentLocation write FcontentLocation;

//    {@member versionId
//      The ETag of the resource identified in the request (if present)
//    }
//    Property e_versionId : String read FversionId write FversionId;

    {@member lastModifiedDate
      The last modified date of the resource identified in the request (if present)
    }
    Property lastModifiedDate : TDateTime read FlastModifiedDate write FlastModifiedDate;

    {@member Lang
      Preferred language of the requester (used for error messages)
    }
    Property Lang : String read FLang write FLang;

    {@member Summary
      What kind of summary is requested
    }
    Property Summary : TFHIRSummaryOption read FSummary write FSummary;

    Property IfMatch : String read FIfMatch write FIfMatch;
    Property IfNoneMatch : String read FIfNoneMatch write FIfNoneMatch;
    Property IfModifiedSince : TDateTime read FIfModifiedSince write FIfModifiedSince;
    Property IfNoneExist : String read FIfNoneExist write FIfNoneExist;

    Property Provenance : TFhirProvenance read FProvenance write SetProvenance;
    Property Origin : TFHIRRequestOrigin read FOrigin;
  End;

  {@Class TFHIRResponse
    A FHIR response.

    This is a response for a RESTful interface, or some other kind of response. The
    HTTP code is used as the logical outcome with other kinds of interfaces.

    A response may have only one of
      * a body
      * a resource
      * a bundle

    The string body is used for error messages, or to return xhtml or schema etc.

    A script may modify any of the values of the response, though changing the
    type of the resource may have unexpected catastrophic outcomes.
  }
  {!.Net HL7Connect.Fhir.Response}
  TFHIRResponse = class (TAdvObject)
  private
    FHTTPCode: Integer;
    FBody: String;
    FMessage: String;
    FResource: TFhirResource;
    FversionId: String;
    ForiginalId: String;
    FlastModifiedDate: TDateTime;
    FContentType: String;
    FFormat: TFHIRFormat;
    FContentLocation: String;
    FLocation: String;
    FTags : TFHIRTagList;
    Flink_list : TFhirBundleLinkList;
    FOrigin: String;
    FId: String;
    procedure SetResource(const Value: TFhirResource);
    function GetBundle: TFhirBundle;
    procedure SetBundle(const Value: TFhirBundle);
  public
    Constructor Create; Override;
    Destructor Destroy; Override;
    {!Script Hide}
    Function Link : TFHIRResponse; Overload;
    {!Script Show}

    {@member HTTPCode
      The logical outcome of the request. Usual values are
        * 0 - the outcome of the transaction is not yet known
        * 200 - the operation completed successfully
        * 202 - the content was accepted (an http variation on 200)
        * 400 - the user made a bad request
        * 404 - the resource wasn't found
        * 500 - some general kind of error

      Any http status code may be used, including codes not defined in the
      http standard (i.e. return 299 to prevent a proxy from caching the response).
      HL7Connect will follow the http standard and use the first digit to determine
      the general outcome
    }
    Property HTTPCode : Integer read FHTTPCode write FHTTPCode;

    {@member Message
      a specific message to go in the HTTP response line. If left blank,
      HL7Connect will fill this out from the http specification
    }
    Property Message : String read FMessage write FMessage;

    {@member Body
      a series of characters that constitute the body of the response. Usually this is
      plain text, but other content (xhtml, schema) could be placed in here.
      If using other than plain text, set the @contentType
    }
    Property Body : String read FBody write FBody;

    {@member Resource
      the actual resource that is the result of the transaction.

      Note that actual kind of the resource will be one of the ones defined as
      part of the FHIR specification
    }
    Property Resource : TFhirResource read FResource write SetResource;
    Property Bundle : TFhirBundle read GetBundle write SetBundle;

    {@member Format
      The format for the response, if known and identified (xml, or json). Derived
      from the requested content-type and/or extension in the url, or configuration
    }
    Property Format : TFHIRFormat read FFormat write FFormat;

    {@member ContentType
      The content type of the response. if left blank, this will be determined
      automatically (text/plain for body, and type as specifed in the FHIR
      specification for resources and bundles.
    }
    Property ContentType : String read FContentType write FContentType;

    {@member originalId
      The originalId of the resource - if known
    }
    Property originalId : String read ForiginalId write ForiginalId;

    {@member Id
      The underlying id, if there is one. Only used internally - not represented on the wire
    }
    Property Id : String read FId write FId;

    {@member versionId
      The ETag to go in the response
    }
    Property versionId : String read FversionId write FversionId;

    {@member lastModifiedDate
      The Last Modified Date to go in the response
    }
    Property lastModifiedDate : TDateTime read FlastModifiedDate write FlastModifiedDate;

    {@member ContentLocation
      Content-Location in HTTP response
    }
    Property ContentLocation : String read FContentLocation write FContentLocation;

    {@member Location
      Location in HTTP response (only used for Create operation)
    }
    Property Location : String read FLocation write FLocation;

    {@member Tags
      Tags for the response
    }
    property Tags : TFHIRTagList read FTags;


    {@member link_list
      link_list for the response
    }
    property link_list : TFhirBundleLinkList read Flink_list;

    {@member Origin
      HTTP Origin header - see http://en.wikipedia.org/wiki/Cross-origin_resource_sharing

      If this has a value when the response is returned, then it will be returned in the Access-Control-Allow-Origin header
    }
    Property Origin : String read FOrigin write FOrigin;

  end;

  ERestfulException = class (EAdvException)
  Private
    FStatus : word;
    FCode : TFhirIssueTypeEnum;
  Public
    Constructor Create(Const sSender, sMethod, sReason : String; aStatus : word; code : TFhirIssueTypeEnum); Overload; Virtual;

    Property Status : word read FStatus write FStatus;
    Property Code : TFhirIssueTypeEnum read FCode write FCode;
  End;

  {@Class TFHIRFactory
    Creates FHIR types

    * new*: create a type with no values
    * make*: useful helper routines that take parameters and populate the type accordingly (mostly for data types)
  }
  {!.Net HL7Connect.Fhir.Factory}
  TFHIRFactory = class (TFhirResourceFactory)
  private
    FLang : String;
  public
    Constructor Create(lang : String);
    {@member makeAttachmentFromFile
      make a new Attachment, and load the contents from the file. The mime type will be filled out based on the systems interpretation of the file extension
    }
    {!script nolink}
    function makeAttachmentFromFile(filename : String) : TFhirAttachment;

    {@member makeAttachmentFromStream
      make a new Attachment From the stream
    }
    {!script nolink}
    function makeAttachmentFromStream(mimeType : String; stream : TStream) : TFhirAttachment;

    {@member makeAttachmentFromUrl
      make a new Attachment that references a url. The data will be copied into the attachment if specified.

      The URL can't be password protected etc.
    }
    {!script nolink}
    function makeAttachmentFromUrl(url : String; mimeType : String; inlineData : Boolean) : TFhirAttachment;

    {@member makeIdentifier
      make a new Identifier and use the provided parameters
    }
    {!script nolink}
    function makeIdentifier(system, key : String) : TFhirIdentifier;

    {@member makeIdentifierWithUse
      make a new Identifier and use the provided parameters
    }
    {!script nolink}
    function makeIdentifierWithUse(system, key, use, label_ : String) : TFhirIdentifier;

    {@member makeCodeableConcept
      make a new CodeableConcept and use the provided parameters
    }
    {!script nolink}
    function makeCodeableConcept(coding : TFhirCoding; text : String) : TFhirCodeableConcept;

    {@member makeCoding
      make a new Coding and use the provided parameters
    }
    {!script nolink}
    function makeCoding(system : String; code : string; display : String) : TFhirCoding;

    {@member makeQuantity
      make a new Quantity and use the provided parameters
    }
    {!script nolink}
    function makeQuantity(value, units : String):TFhirQuantity;

    {@member makeQuantityCoded
      make a new QuantityCoded and use the provided parameters
    }
    {!script nolink}
    function makeQuantityCoded(value, units, system, code : String):TFhirQuantity;

    {@member makeIntervalQuantity
      make a new IntervalQuantity and use the provided parameters
    }
    {!script nolink}
    function makeRange(low, high : TFhirQuantity):TFhirRange;

    {@member makeIntervalDateTime
      make a new IntervalDateTime and use the provided parameters
    }
    {!script nolink}
    function makePeriod(low, high : string):TFhirPeriod;

    {@member makeIdentifierWithLabel
      make a new HumanId and use the provided parameters
    }
    {!script nolink}
    function makeIdentifierWithLabel(use : string; label_, idSystem, id : String):TFhirIdentifier;

    {@member makeHumanName
      make a new HumanName and use the provided parameters
    }
    {!script nolink}
    function makeHumanName(use, family, given, prefix, suffix : String):TFhirHumanName;

    {@member makeHumanNameText
      make a new HumanName and fill out the text value
    }
    {!script nolink}
    function makeHumanNameText(use, text : String):TFhirHumanName;

    {@member makeAddress
      make a new HumanAddress and use the provided parameters
    }
    {!script nolink}
    function makeAddress(use, street, city, state, postalCode, country : String):TFhirAddress;

    {@member makeAddressText
      make a new HumanAddress and use the provided parameters
    }
    {!script nolink}
    function makeAddressText(use, text : String):TFhirAddress;

    {@member makeContactPoint
      make a new Contact and use the provided parameters
    }
    {!script nolink}
    function makeContactPoint(system, value, use : String):TFhirContactPoint;

    {@member makeReference
      make a new resource reference and use the provided parameters
    }
    {!script nolink}
    function makeReference(id : String) : TFhirReference;

    {@member makeReferenceText
      make a new resource reference and fill out the display only
    }
    {!script nolink}
    function makeReferenceText(s : String) : TFhirReference;

    {@member makeExtension
      make a new narrative with the provided status and html
    }
    {!script nolink}
    function makeExtension(url : String; value : TFhirType) : TFhirExtension;

    {@member makeNarrative
      make a new narrative with the provided status and html
    }
    {!script nolink}
    function makeNarrative(status, html : String; policy : TFHIRXhtmlParserPolicy) : TFhirNarrative;

    {@member parseHTML
      parse an html fragment into an html node (for use with narrative).

      the html fragment must begin and end with <xhtml></xhtml>
    }
    function parseHTML(source : String; policy : TFHIRXhtmlParserPolicy) : TFhirXHtmlNode;

    {@member makeBinary
      make a new Binary resource
    }
    {!script nolink}
    function makeBinary : TFhirBinary;

    {@member makeBinary
      make a new Binary resource
    }
    {!script nolink}
    function makeBinaryContent(source : TAdvBuffer; mimeType : String) : TFhirBinary;

    {@member makeRequest
      make a new Fhir request (for a conversion parameter)
    }
    {!script nolink}
    function makeRequest(origin : TFHIRRequestOrigin) : TFhirRequest;

    {@member makeRequest
      make a new OperationOutcome that claims success
    }
    {!script nolink}
    function makeSuccessfulOperation : TFhirOperationOutcome;
  end;

Function IdTail(s : String):String;
Function IdHead(s : String):String;

implementation

uses
  FHIRParser,
  FHIRParserBase,
  FHIRUtilities;

{ ERestfulException }

constructor ERestfulException.Create(const sSender, sMethod, sReason: String; aStatus : word; code : TFhirIssueTypeEnum);
begin
  Create(sSender, sMethod, sReason);
  FStatus := aStatus;
  FCode := code;
end;


{ TFHIRRequest }

function TFHIRRequest.preAnalyse(url : String) : String;
var
  i : integer;
begin
  i := StringFind(uRL, ['?']);
  if (i = 0) then
    LoadParams('')
  else
  begin
    LoadParams(copy(uRL, i+1, $FFF));
    url := copy(uRL, 1, i-1);
  end;
  if url.StartsWith('/') then
    url := url.Substring(1);
  if form <> nil then
    LoadParams(form);
  result := url;
end;

Procedure CheckId(lang, id : String);
var
  i : integer;
begin
  if (Length(id) > ID_LENGTH) then
    Raise ERestfulException.Create('TFhirWebServer', 'SplitId', StringFormat(GetFhirMessage('MSG_ID_TOO_LONG', lang), [id]), HTTP_ERR_BAD_REQUEST, IssueTypeInvalid);
  for i := 1 to length(id) do
    if not CharInSet(id[i], ['a'..'z', '0'..'9', 'A'..'Z', '.', '-']) then
      Raise ERestfulException.Create('TFhirWebServer', 'SplitId', StringFormat(GetFhirMessage('MSG_ID_INVALID', lang), [id, id[i]]), HTTP_ERR_BAD_REQUEST, IssueTypeInvalid);
end;


procedure TFHIRRequest.analyse(sCommand, sUrl: String; out relativeReferenceAdjustment : integer);
Var
  sId, sType : String;
  aResourceType : TFHIRResourceType;
  Function NextSegment(var url : String):String;
  var
    i : integer;
  Begin
    i := StringFind(url, ['/']);
    if i = 0 then
    begin
      result := url;
      url := '';
    end
    else
    begin
      inc(relativeReferenceAdjustment);
      result := copy(url, 1, i-1);
      url := copy(url, i + 1, $FFF);
    end;
  End;
  procedure ForceMethod(sMethod : String);
  begin
    if (sCommand <> sMethod) Then
      raise ERestfulException.Create('TFhirWebServer', 'HTTPRequest', StringFormat(GetFhirMessage('MSG_BAD_FORMAT', lang), [sUrl, sMethod]), HTTP_ERR_FORBIDDEN, IssueTypeInvalid);
  end;
var
  s, soURL : String;
begin
  soURL := sUrl;
  relativeReferenceAdjustment := 0;
  sType := NextSegment(sURL);
  if (sType = '') Then
  begin
    if sCommand = 'OPTIONS' then
      CommandType := fcmdConformanceStmt
    else if (sCommand = 'POST') then
    begin
      if form <> nil then
      begin
        s := Parameters.GetVar('op');
        if (s = 'transaction') or (s = '') then
          CommandType := fcmdTransaction
        else if (s = 'validation') or (s = 'validate') then
          CommandType := fcmdValidate
        else
          raise Exception.create('Unknown Operation: '+s);
      end
      else
        CommandType := fcmdTransaction
    end
    else
    begin
      ForceMethod('GET');
      if Parameters.Count > 0 then
        CommandType := fcmdSearch
      else
        CommandType := fcmdUnknown;
    end
  end
  else if (sCommand = 'OPTIONS') then
    CommandType := fcmdNull
  else if (sType = '_web') then // special
  begin
    if sCommand <> 'POST' then
      ForceMethod('GET');
    CommandType := fcmdWebUI;
    id := sUrl;
    sUrl := '';
  end
  else if StringStartsWith(sType, '$', false)  then
  begin
    CommandType := fcmdOperation;
    OperationName := sType.Substring(1);
    if sCommand <> 'GET' then
      ForceMethod('POST');
  end
  else if (sType = '_history') then
  begin
    ForceMethod('GET');
    CommandType := fcmdHistorySystem;
  end
  else if (sType = 'metadata') Then
  begin
    CommandType := fcmdConformanceStmt;
    ForceMethod('GET');
  end
  else if (sType = 'validation') then
  begin
    CommandType := fcmdValidate;
    ForceMethod('POST');
    if Parameters.VarExists('profile') and (Parameters.GetVar('profile') <> '') then
      tags.AddTag(0, tcProfile, 'urn:ietf:rfc:3986', 'http://localhost/'+Parameters.GetVar('profile'), '');
  end
  else if (sType = '_search') then
  begin
    CommandType := fcmdSearch;
    if (sCommand <> 'GET') and (sCommand <> 'POST') then
      raise ERestfulException.Create('TFhirWebServer', 'HTTPRequest', StringFormat(GetFhirMessage('MSG_BAD_SYNTAX', lang), [soURL]), HTTP_ERR_BAD_REQUEST, IssueTypeInvalid);
  end
  else
  begin
    if (sType <> '') And not RecogniseFHIRResourceManagerName(sType, aResourceType) Then
      Raise ERestfulException.Create('TFhirWebServer', 'HTTPRequest', StringFormat(GetFhirMessage('MSG_NO_MODULE', lang), [sType]), HTTP_ERR_NOTFOUND, IssueTypeNotSupported);
    ResourceType := aResourceType;
    sId := NextSegment(sURL);
    if sId = '' then
    begin
      if sCommand = 'GET' then
      begin
        CommandType := fcmdSearch;
        DefaultSearch := true;
      end
      else if sCommand = 'POST' then
        CommandType := fcmdCreate
      else if (scommand = 'DELETE') then
      begin
        CommandType := fcmdDelete;
        DefaultSearch := true;
      end
      else
        raise ERestfulException.Create('TFhirWebServer', 'HTTPRequest', StringFormat(GetFhirMessage('MSG_BAD_SYNTAX', lang), [soURL]), HTTP_ERR_BAD_REQUEST, IssueTypeInvalid);
    end
    else if StringStartsWith(sId, '$', false) then
    begin
      CommandType := fcmdOperation;
      OperationName := sId.Substring(1);
      if sCommand <> 'GET' then
        ForceMethod('POST');
    end
    else if not StringStartsWith(sId, '_', true) Then
    begin
      // operations on a resource
      CheckId(lang, sId);
      Id := sId;
      sId := NextSegment(sUrl);
      if (sId = '') Then
      begin
        if sCommand = 'GET' Then
          CommandType := fcmdRead
        else if sCommand = 'PUT' Then
          CommandType := fcmdUpdate
        else if sCommand = 'PATCH' Then
          CommandType := fcmdPatch
        else if sCommand = 'DELETE' Then
          CommandType := fcmdDelete
        else if sCommand = 'OPTIONS' then // CORS
          CommandType := fcmdConformanceStmt
        else
          raise ERestfulException.Create('TFhirWebServer', 'HTTPRequest', StringFormat(GetFhirMessage('MSG_BAD_FORMAT', lang), [soURL, 'GET, PUT or DELETE']), HTTP_ERR_FORBIDDEN, IssueTypeNotSupported);
      end
      else if StringStartsWith(sId, '$', false)  then
      begin
        CommandType := fcmdOperation;
        OperationName := sId.Substring(1);
        if sCommand <> 'GET' then
          ForceMethod('POST');
      end
      else if (sId = '_history') then
      begin
        sId := NextSegment(sURL);
        if sId = '' then
        begin
          ForceMethod('GET');
          CommandType := fcmdHistoryInstance;
        end
        else if not StringStartsWith(sId, '_', true) Then
        begin
          CheckId(lang, sId);
          SubId := sId;
          sId := NextSegment(sURL);
          if sid = '' then
          begin
            ForceMethod('GET');
            CommandType := fcmdVersionRead;
          end
          else if (sId = '$meta') then
            begin
            ForceMethod('GET');
            CommandType := fcmdOperation;
            OperationName := 'meta';
          end
          else if (sId = '$meta-add') then
            begin
              ForceMethod('POST');
            CommandType := fcmdOperation;
            OperationName := 'meta-add';
      end
          else if (sId = '$meta-delete') then
      begin
            ForceMethod('POST');
            CommandType := fcmdOperation;
            OperationName := 'meta-delete';
          end
          else
            raise ERestfulException.Create('TFhirWebServer', 'HTTPRequest', StringFormat(GetFhirMessage('MSG_BAD_SYNTAX', lang), [soURL]), HTTP_ERR_BAD_REQUEST, IssueTypeInvalid);
        end
        else
          raise ERestfulException.Create('TFhirWebServer', 'HTTPRequest', StringFormat(GetFhirMessage('MSG_BAD_SYNTAX', lang), [soURL]), HTTP_ERR_BAD_REQUEST, IssueTypeInvalid);
      end
      else if sId = '*' then // all tpes
      begin
        if ResourceType <> frtPatient then
          raise ERestfulException.Create('TFhirWebServer', 'HTTPRequest', StringFormat(GetFhirMessage('MSG_UNKNOWN_COMPARTMENT', lang), [soURL, 'GET, POST or DELETE']), HTTP_ERR_FORBIDDEN, IssueTypeNotSupported);

        CompartmentId := Id;
        CommandType := fcmdSearch;
        ResourceType := frtNull;
        Id := '';
      end
      else if StringArrayExistsInSensitive(CODES_TFhirResourceType, sId) then
      begin
        aResourceType := TFHIRResourceType(StringArrayIndexOfInSensitive(CODES_TFhirResourceType, sId));
        if (COMPARTMENT_PARAM_NAMES[ResourceType, aResourceType] <> '') then
        begin
          if ResourceType <> frtPatient then
            raise ERestfulException.Create('TFhirWebServer', 'HTTPRequest', StringFormat(GetFhirMessage('MSG_UNKNOWN_COMPARTMENT', lang), [soURL, 'GET, POST or DELETE']), HTTP_ERR_FORBIDDEN, IssueTypeNotSupported);

          CompartmentId := Id;
          CommandType := fcmdSearch;
          ResourceType := aResourceType;
          Id := '';
        end
        else
          raise ERestfulException.Create('TFhirWebServer', 'HTTPRequest', StringFormat(GetFhirMessage('MSG_BAD_SYNTAX', lang), [soURL]), HTTP_ERR_BAD_REQUEST, IssueTypeInvalid);
      end
      else
        raise ERestfulException.Create('TFhirWebServer', 'HTTPRequest', StringFormat(GetFhirMessage('MSG_BAD_SYNTAX', lang), [soURL]), HTTP_ERR_BAD_REQUEST, IssueTypeInvalid);
    end
    else if (sId = '_validate') Then
    begin
      ForceMethod('POST');
      CommandType := fcmdValidate;
      sId := NextSegment(sUrl);
      if sId <> '' Then
      Begin
        if (sURL <> '') or (sId = '') Then
          raise ERestfulException.Create('TFhirWebServer', 'HTTPRequest', 'Bad Syntax in "'+soURL+'"', HTTP_ERR_BAD_REQUEST, IssueTypeInvalid);
        CheckId(lang, copy(sId, 2, $FF));
        Id := copy(sId, 2, $FF);
      End;
    end
    else if (sId = '_search') or (sId = '_search.xml') or (sId = '_search.json') Then
      CommandType := fcmdSearch
    else if (sId = '_history') or (sId = '_history.xml') or (sId = '_history.json') Then
    begin
      ForceMethod('GET');
      CommandType := fcmdHistoryType
    end
    // Extension on this server - remove?
    else if (sId = '_upload') or (sId = '_upload.htm') then
    begin
      CommandType := fcmdUpload;
      PostFormat := ffXhtml;
    end
    else
      raise ERestfulException.Create('TFhirWebServer', 'HTTPRequest', StringFormat(GetFhirMessage('MSG_BAD_SYNTAX', lang), ['URL'+soURL]), HTTP_ERR_BAD_REQUEST, IssueTypeInvalid);
  End;
  if (CommandType <> fcmdNull)  then
    if (sURL <> '') then
      raise ERestfulException.Create('TFhirWebServer', 'HTTPRequest', StringFormat(GetFhirMessage('MSG_BAD_SYNTAX', lang), [soURL]), HTTP_ERR_BAD_REQUEST, IssueTypeInvalid);
end;

function TFHIRRequest.canGetUser: boolean;
begin
  if session = nil then
    result := true
  else
    result := session.canGetUser;
end;

function TFHIRRequest.canRead(aResourceType: TFhirResourceType): boolean;
begin
  if session = nil then
    result := true
  else
    result := session.canRead(aResourceType);
end;

function TFHIRRequest.canWrite(aResourceType: TFhirResourceType): boolean;
begin
  if session = nil then
    result := true
  else
    result := session.canWrite(aResourceType);
end;

function TFHIRRequest.Compose: String;
var
  comp : TFHIRXmlComposer;
  stream : TStringStream;
begin
  stream := TStringStream.Create('');
  try
    comp := TFHIRXmlComposer.create(lang);
    try
      comp.Compose(stream, resource, true, nil);
    finally
      comp.free;
    end;
    result := stream.DataString;
  finally
    stream.free;
  end;
end;

procedure TFHIRRequest.CopyPost(stream: TStream);
var
  p, t : integer;
  b : TBytes;
begin
  p := stream.Position;
  t := stream.size - stream.position;
  SetLength(b, t);
  Stream.Read(b[0], t);
  stream.position := p;
  FSource := TAdvBuffer.create;
  FSource.AsBytes := b;
end;

constructor TFHIRRequest.Create(origin : TFHIRRequestOrigin);
begin
  inherited Create;
  FTags := TFHIRTagList.create;
  FOrigin := origin;
end;

destructor TFHIRRequest.Destroy;
begin
  FPatch.Free;
  FTags.free;
  FSession.Free;
  FSource.Free;
  FResource.Free;
  FProvenance.Free;
  FForm.Free;
  FParams.Free;
  inherited;
end;

function TFHIRRequest.GetBundle: TFhirBundle;
begin
  if not (Resource is TFHIRBundle) then
    raise Exception.Create('Found a '+resource.FhirType+' expecting a Bundle');
  result := Resource as TFhirBundle;
end;

function TFHIRRequest.Link: TFHIRRequest;
begin
  result := TFHIRRequest(Inherited Link);
end;

procedure TFHIRRequest.LoadParams(s: String);
begin
  if (FParams <> nil) then
    FParams.Free;
  FParams := nil;
  FParams := TParseMap.create(s);
  processParams;
end;

procedure TFHIRRequest.LoadParams(form: TMimeMessage);
var
  i : integer;
  p : TMimePart;
  s, n : String;
begin
  for i := 0 to form.Parts.Count - 1 do
  begin
    p := form.Parts[i];
    if (p.MediaType = '') then
    begin
      n := p.ParamName;
      s := p.Content.AsUnicode.trimRight([#13, #10]);
      if (n <> '') and (not s.Contains(#10)) then
        FParams.addItem(n, s);
    end;
  end;
  processParams;
end;

function TFHIRRequest.LogSummary: String;
begin
  result := CODES_TFHIRCommandType[CommandType]+'\('+CODES_TFHIRFormat[PostFormat]+')'+CODES_TFhirResourceType[ResourceType]+'\'+Id;
  if SubId <> '' then
    result := result + '\'+SubId;
end;

procedure TFHIRRequest.processParams;
var
  s : String;
begin
  s := FParams.GetVar('_summary');
  if s = 'true' then
    Summary := soSummary
  else if s = 'text' then
    Summary := soText
  else if s = 'data' then
    Summary := soData
  else if s = 'count' then
    Summary := soCount
  else
    Summary := soFull;
end;

procedure TFHIRRequest.reset;
begin
  FId := '';
  FSubId := '';
  FCommandType := fcmdUnknown;
  FResourceType := frtNull;
  FResource := nil;
  FUrl := '';
  FParams.Free;
  FParams := nil;
  if FSource <> nil then
    FSource.Clear;
  FDefaultSearch := false;
  FForm := nil;
  FOperationName := '';
  FIfMatch  := '';
  FIfNoneMatch := '';
  FIfModifiedSince := 0;
  FIfNoneExist := '';
  FSummary := soFull;
end;

procedure TFHIRRequest.SetBundle(const Value: TFhirBundle);
begin
  setresource(value);
end;

procedure TFHIRRequest.SetForm(const Value: TMimeMessage);
begin
  FForm := Value;
end;

procedure TFHIRRequest.SetPatch(const Value: TJsonArray);
begin
  FPatch.Free;
  FPatch := Value;
end;

procedure TFHIRRequest.SetProvenance(const Value: TFhirProvenance);
begin
  FProvenance.Free;
  FProvenance := Value;
end;

procedure TFHIRRequest.SetResource(const Value: TFhirResource);
begin
  FResource.Free;
  FResource := Value;
end;

procedure TFHIRRequest.SetSession(const Value: TFhirSession);
begin
  FSession.Free;
  FSession := Value;
end;

procedure TFHIRRequest.SetSource(const Value: TAdvBuffer);
begin
  FSource.Free;
  FSource := Value;
end;

function TFHIRRequest.XMLSummary: String;
  procedure addValue(n, v : String; t : boolean);
  begin
    if (t) then
      result := result+'  '+n+': '+v+#13#10;
  end;

begin
  result := #13#10;
  addValue('Command', CODES_TFHIRCommandType[CommandType], true);
  addValue('Url', FUrl, true);
  addValue('format', CODES_TFHIRFormat[PostFormat], true);
  addValue('type', CODES_TFhirResourceType[ResourceType], ResourceType <> frtNull);
  addValue('id', FId, true);
  addValue('subId', FSubId, FSubId <> '');
  addValue('baseUrl', FBaseUrl, FBaseUrl <> '');
  addValue('originalId', ForiginalId, ForiginalId <> '');
  addValue('versionId', FversionId, FversionId <> '');
  addValue('Last-Modified', FormatDateTime('c', FlastModifiedDate), FlastModifiedDate <> 0);
//  addValue('Content-Location', FcontentLocation, FcontentLocation <> '');
  addValue('defaultSearch', BooleanToString(FDefaultSearch), CommandType = fcmdSearch);
  addValue('Language', FLang, FLang <> '');
  addValue('Tag', FTags.ToString, FTags.count > 0);
  addValue('ip', FIp, FIp <> '');
  addValue('compartments', FCompartments, FCompartments <> '');
  addValue('compartmentId', FCompartmentId, FCompartmentId <> '');
end;

{ TFHIRResponse }

constructor TFHIRResponse.Create;
begin
  inherited;
  FTags := TFHIRTagList.create;
  Flink_list := TFhirBundleLinkList.create;
end;

destructor TFHIRResponse.Destroy;
begin
  Flink_list.Free;
  FTags.free;
  FResource.Free;
  inherited;
end;

function TFHIRResponse.GetBundle: TFhirBundle;
begin
  result := FResource as TFhirBundle;
end;

function TFHIRResponse.Link: TFHIRResponse;
begin
  result := TFHIRResponse(Inherited Link);
end;

procedure TFHIRResponse.SetBundle(const Value: TFhirBundle);
begin
  SetResource(value);
end;

procedure TFHIRResponse.SetResource(const Value: TFhirResource);
begin
  FResource.free;
  FResource := nil;
  FResource := Value;
end;

{ TFHIRFactory }

function CheckEnum(Names: array of String; value : String): TFhirEnum;
begin
  if value = '' then
    result := nil
  else if StringIsInteger32(value) then
    result := TFhirEnum.create(Names[StrToInt(value)])
  else if StringArrayIndexOfSensitive(Names, value) > -1 then
    result := TFhirEnum.create(value)
  else
    Raise Exception.create('Invalid enumeration value "'+value+'"');
end;


function TFHIRFactory.makeAttachmentFromFile(filename: String): TFhirAttachment;
begin
  raise Exception.Create('to do');
{  result := TFhirAttachment.create;
  try
    result.data := TFhirBase64Binary.create;
    result.data.value := BinToBase64(FileToString(filename));
    result.contentType := TFhirCode.Create(GetMIMETypeFromFile(filename));
    result.link;
  finally
    result.free;
  end;}
end;

function TFHIRFactory.makeAttachmentFromStream(mimeType: String; stream: TStream): TFhirAttachment;
begin
  raise Exception.Create('to do');
{  result := TFhirAttachment.create;
  try
    result.data := TFhirBase64Binary.create;
    result.data.value := BinToBase64(StreamToString(stream));
    result.contentType := TFhirCode.create(mimeType);
    result.link;
  finally
    result.free;
  end;}
end;

function TFHIRFactory.makeAttachmentFromUrl(url, mimeType: String; inlineData: Boolean): TFhirAttachment;
begin
  raise Exception.Create('to do');
{  result := nil;
  try
    todo;
    result.link;
  finally
    result.free;
  end;}
end;

function TFHIRFactory.makeCodeableConcept(coding: TFhirCoding; text: String): TFhirCodeableConcept;
begin
  result := TFhirCodeableConcept.create;
  try
    result.codingList.add(coding);
    result.text := text;
    result.link;
  finally
    result.free;
  end;
end;

function TFHIRFactory.makeCoding(system, code, display: String): TFhirCoding;
begin
  result := TFhirCoding.create;
  try
    result.code := code;
    result.system := system;
    result.display := display;
    result.link;
  finally
    result.free;
  end;
end;

function TFHIRFactory.makeContactPoint(system, value, use: String): TFhirContactPoint;
begin
  result := TFhirContactPoint.create;
  try
    result.useElement := CheckEnum(CODES_TFhirContactPointUseEnum, use);
    result.systemElement := CheckEnum(CODES_TFhirContactPointSystemEnum, system);
    result.value := value;
    result.link;
  finally
    result.free;
  end;
end;

function TFHIRFactory.makeAddress(use, street, city, state, postalCode, country : String): TFhirAddress;
begin
  result := TFhirAddress.create;
  try
    result.useElement := CheckEnum(CODES_TFhirAddressUseEnum, use);
    result.lineList.AddItem(street);
    result.city := city;
    result.state := state;
    result.postalCode := postalCode;
    result.country := country;
    result.link;
  finally
    result.free;
  end;
end;

function TFHIRFactory.makeIdentifierWithLabel(use : string; label_, idSystem, id: String): TFhirIdentifier;
begin
  result := TFhirIdentifier.create;
  try
    result.useElement := CheckEnum(CODES_TFhirIdentifierUseEnum, use);
    result.type_ := TFhirCodeableConcept.Create;
    result.type_.text := label_;
    result.system := idsystem;
    result.value := id;
    result.link;
  finally
    result.free;
  end;
end;

function TFHIRFactory.makeHumanName(use, family, given, prefix, suffix: String): TFhirHumanName;
begin
  result := TFhirHumanName.create;
  try
    result.useElement := CheckEnum(CODES_TFhirNameUseEnum, use);
    result.familyList.addItem(family);
    result.givenList.addItem(given);
    result.prefixList.addItem(prefix);
    result.suffixList.addItem(suffix);
    result.link;
  finally
    result.free;
  end;
end;

function TFHIRFactory.makeIdentifierWithUse(system, key, use, label_ : String) : TFhirIdentifier;
begin
  result := TFhirIdentifier.create;
  try
    result.system := system;
    result.value := key;
    result.type_ := TFhirCodeableConcept.Create;
    result.type_.text := label_;
    result.useElement := CheckEnum(CODES_TFhirIdentifierUseEnum, use);
    result.link;
  finally
    result.free;
  end;
end;

function TFHIRFactory.makeIdentifier(system, key: String): TFhirIdentifier;
begin
  result := TFhirIdentifier.create;
  try
    result.system := system;
    result.value := key;
    result.link;
  finally
    result.free;
  end;
end;

function TFHIRFactory.makePeriod(low, high: string): TFhirPeriod;
begin
  result := TFhirPeriod.create;
  try
    if low <> '' then
      result.start := TDateAndTime.createXml(low);
    if high <> '' then
      result.end_ := TDateAndTime.createXml(high);
    result.link;
  finally
    result.free;
  end;
end;

function TFHIRFactory.makeRange(low, high: TFhirQuantity): TFhirRange;
begin
  result := TFhirRange.create;
  try
    result.low := low.Link;
    result.high := high.Link;
    result.link;
  finally
    result.free;
  end;
end;

function TFHIRFactory.makeQuantity(value, units: String): TFhirQuantity;
begin
  result := TFhirQuantity.create;
  try
    result.value := value;
    result.unit_ := units;
    result.link;
  finally
    result.free;
  end;
end;

function TFHIRFactory.makeQuantityCoded(value, units, system, code: String): TFhirQuantity;
begin
  result := TFhirQuantity.create;
  try
    result.value := value;
    result.unit_ := units;
    result.system := system;
    result.code := code;
    result.link;
  finally
    result.free;
  end;
end;

function TFHIRFactory.parseHTML(source: String; policy : TFHIRXhtmlParserPolicy): TFhirXHtmlNode;
var
  parser : TFHIRXmlParserBase;
begin
  parser := TFHIRXmlParserBase.create(Flang);
  try
    parser.ParserPolicy := policy;
    parser.Source := TStringStream.create(source);
    result := parser.ParseHtml;
    try
      if result.Name <> 'div' then
        raise Exception.create('Wrong name "'+result.Name+'+ on the xhtml fragment');
      result.link;
    finally
      result.free;
    end;
  finally
    parser.source.free;
    parser.Free;
  end;
end;

function TFHIRFactory.makeBinary: TFhirBinary;
begin
  result := TFhirBinary.create;
end;

function TFHIRFactory.makeBinaryContent(source: TAdvBuffer; mimeType: String): TFhirBinary;
begin
  result := makeBinary;
  result.Content := source.AsBytes;
  result.ContentType := mimeType;
end;

function TFHIRFactory.makeReference(id: String): TFhirReference;
begin
  result := TFhirReference.create;
  try
    result.reference := id;
    result.link;
  finally
    result.free;
  end;
end;

function TFHIRFactory.makeNarrative(status, html: String; policy : TFHIRXhtmlParserPolicy): TFhirNarrative;
begin
  result := TFhirNarrative.create;
  try
    result.statusElement := CheckEnum(CODES_TFhirNarrativeStatusEnum, status);
    result.div_ := parseHTML(html, policy);
    result.link;
  finally
    result.free;
  end;
end;

Function IdTail(s : String):String;
begin
  if (pos('/', s) = 0) then
    result := s
  else
  begin
    result := copy(s, LastDelimiter('/', s)+1, $FF);
    if result[1] = '@' then
      delete(result, 1, 1);
  end;
end;

Function IdHead(s : String):String;
begin
  if (pos('/', s) > 0) then
    result := copy(s, 1, LastDelimiter('/', s))
  else
    result := '';
end;

constructor TFHIRFactory.Create(lang: String);
begin
  Inherited Create;
  FLang := lang;
end;


function TFHIRFactory.makeReferenceText(s: String): TFhirReference;
begin
  result := TFhirReference.create;
  try
    result.display := s;
    result.link;
  finally
    result.free;
  end;
end;

function TFHIRFactory.makeRequest(origin : TFHIRRequestOrigin): TFhirRequest;
begin
  result := TFhirRequest.create(origin);
end;

function TFHIRFactory.makeSuccessfulOperation: TFhirOperationOutcome;
begin
  result := TFhirOperationOutcome.create;
  try
    result.text := makeNarrative('generated', '<div>The operation was successful</div>', xppReject);
    result.link;
  finally
    result.Free;
  end;
end;

function TFHIRFactory.makeHumanNameText(use, text: String): TFhirHumanName;
begin
  result := TFhirHumanName.create;
  try
    result.useElement := CheckEnum(CODES_TFhirNameUseEnum, use);
    result.text := text;
    result.link;
  finally
    result.free;
  end;

end;

function TFHIRFactory.makeAddressText(use, text: String): TFhirAddress;
begin
  result := TFhirAddress.create;
  try
    result.useElement := CheckEnum(CODES_TFhirAddressUseEnum, use);
    result.text := text;
    result.link;
  finally
    result.free;
  end;
end;

function TFHIRFactory.makeExtension(url: String; value: TFhirType): TFhirExtension;
begin
  result := TFhirExtension.create;
  try
    result.url := url;
    result.value := value.Link;
    result.link;
  finally
    result.free;    
  end;

end;


{ TFhirSession }

procedure TFhirSession.allowAll;
begin
  FSecurity.allowAll;
end;

function TFhirSession.canAdministerUsers: boolean;
begin
  result := FSecurity.canAdministerUsers;
end;

function TFhirSession.canGetUser: boolean;
begin
  result := FSecurity.canGetUserInfo;
end;

function TFhirSession.canRead(aResourceType: TFhirResourceType): boolean;
begin
  result := FSecurity.canRead(aResourceType);
end;

function TFhirSession.canReadAll: boolean;
begin
  result := FSecurity.canReadAll;
end;

function TFhirSession.canWrite(aResourceType: TFhirResourceType): boolean;
begin
  result := FSecurity.canWrite(aResourceType);
end;

constructor TFhirSession.Create(secure : boolean);
begin
  inherited Create;
  FSecure := secure;
  FFirstCreated := now;
  FTaggedCompartments := TStringList.create;
  FPatientList := TStringList.create;
end;

procedure TFhirSession.describe(b: TStringBuilder);
begin
  b.Append('<tr>');
//  session key
  b.Append('<td>');
  b.Append(inttostr(FKey));
  b.Append('</td>');
//  identity
  b.Append('<td>');
  if Fanonymous then
    b.Append('(anonymous)')
  else if FProvider = apNone then
    b.Append(FId)
  else
    b.Append(Names_TFHIRAuthProvider[FProvider]+'\'+FId);
  b.Append('</td>');
//  userkey
  b.Append('<td>');
  b.Append(inttostr(FUserKey));
  b.Append('</td>');
//  name
  b.Append('<td>');
  b.Append(FormatTextToHTML(FName));
  b.Append('</td>');
//  created
  b.Append('<td>');
  b.Append(FormatDateTime('c', FFirstCreated));
  b.Append('</td>');
//  expires
  b.Append('<td>');
  b.Append(FormatDateTime('c', FExpires));
  b.Append('</td>');
//  next check time
  b.Append('<td>');
  b.Append(FormatDateTime('c', FNextTokenCheck));
  b.Append('</td>');
//  next use count
  b.Append('<td>');
  b.Append(inttostr(FUseCount));
  b.Append('</td>');
//  scopes
  b.Append('<td>');
  b.Append(FSecurity.source);
  b.Append('</td>');
//  compartments
  b.Append('<td>');
  b.Append(FTaggedCompartments.CommaText +' '+FPatientList.CommaText);
  b.Append('</td>');
  b.Append('</tr>'#13#10);
end;

destructor TFhirSession.Destroy;
begin
  FSecurity.Free;
  FJwt.free;
  FTaggedCompartments.Free;
  FPatientList.Free;
  FUser.Free;
  inherited;
end;

function TFhirSession.GetScopes: String;
begin
  result := FSecurity.source;
end;

function TFhirSession.Link: TFhirSession;
begin
  result := TFhirSession(inherited Link);
end;


procedure TFhirSession.SetJwt(const Value: TJWT);
begin
  FJwt.free;
  FJwt := Value;
end;

procedure TFhirSession.setScopes(scopes: String);
begin
  FSecurity := TFHIRSecurityRights.create(FUser, scopes, FSecure);
end;

procedure TFhirSession.SetUser(const Value: TSCIMUser);
begin
  FUser.Free;
  FUser := Value;
end;


end.
