package org.hl7.fhir.instance.model;

// Generated on Tue, Nov 18, 2014 14:45+1100 for FHIR v0.3.0

public enum ResourceType {
    Condition,
    Supply,
    ProcedureRequest,
    DeviceComponent,
    Organization,
    Group,
    OralHealthClaim,
    ValueSet,
    Coverage,
    ImmunizationRecommendation,
    Appointment,
    MedicationDispense,
    MedicationPrescription,
    Slot,
    Contraindication,
    AppointmentResponse,
    MedicationStatement,
    Composition,
    Questionnaire,
    OperationOutcome,
    Conformance,
    NamingSystem,
    Media,
    Binary,
    Other,
    HealthcareService,
    Profile,
    DocumentReference,
    Eligibility,
    Immunization,
    Bundle,
    ExtensionDefinition,
    Subscription,
    OrderResponse,
    ConceptMap,
    ImagingStudy,
    Practitioner,
    CarePlan,
    Provenance,
    Device,
    Query,
    Order,
    Procedure,
    Substance,
    DeviceUseRequest,
    DiagnosticReport,
    Medication,
    MessageHeader,
    DocumentManifest,
    DataElement,
    Availability,
    MedicationAdministration,
    QuestionnaireAnswers,
    Encounter,
    SecurityEvent,
    List,
    DeviceUseStatement,
    OperationDefinition,
    NutritionOrder,
    ClaimResponse,
    ReferralRequest,
    CommunicationRequest,
    RiskAssessment,
    FamilyHistory,
    Location,
    ExplanationOfBenefit,
    AllergyIntolerance,
    Observation,
    Contract,
    SupportingDocumentation,
    RelatedPerson,
    Basic,
    Specimen,
    Alert,
    Patient,
    DiagnosticOrder;


    public String getPath() {;
      switch (this) {
    case Condition:
      return "condition";
    case Supply:
      return "supply";
    case ProcedureRequest:
      return "procedurerequest";
    case DeviceComponent:
      return "devicecomponent";
    case Organization:
      return "organization";
    case Group:
      return "group";
    case OralHealthClaim:
      return "oralhealthclaim";
    case ValueSet:
      return "valueset";
    case Coverage:
      return "coverage";
    case ImmunizationRecommendation:
      return "immunizationrecommendation";
    case Appointment:
      return "appointment";
    case MedicationDispense:
      return "medicationdispense";
    case MedicationPrescription:
      return "medicationprescription";
    case Slot:
      return "slot";
    case Contraindication:
      return "contraindication";
    case AppointmentResponse:
      return "appointmentresponse";
    case MedicationStatement:
      return "medicationstatement";
    case Composition:
      return "composition";
    case Questionnaire:
      return "questionnaire";
    case OperationOutcome:
      return "operationoutcome";
    case Conformance:
      return "conformance";
    case NamingSystem:
      return "namingsystem";
    case Media:
      return "media";
    case Binary:
      return "binary";
    case Other:
      return "other";
    case HealthcareService:
      return "healthcareservice";
    case Profile:
      return "profile";
    case DocumentReference:
      return "documentreference";
    case Eligibility:
      return "eligibility";
    case Immunization:
      return "immunization";
    case Bundle:
      return "bundle";
    case ExtensionDefinition:
      return "extensiondefinition";
    case Subscription:
      return "subscription";
    case OrderResponse:
      return "orderresponse";
    case ConceptMap:
      return "conceptmap";
    case ImagingStudy:
      return "imagingstudy";
    case Practitioner:
      return "practitioner";
    case CarePlan:
      return "careplan";
    case Provenance:
      return "provenance";
    case Device:
      return "device";
    case Query:
      return "query";
    case Order:
      return "order";
    case Procedure:
      return "procedure";
    case Substance:
      return "substance";
    case DeviceUseRequest:
      return "deviceuserequest";
    case DiagnosticReport:
      return "diagnosticreport";
    case Medication:
      return "medication";
    case MessageHeader:
      return "messageheader";
    case DocumentManifest:
      return "documentmanifest";
    case DataElement:
      return "dataelement";
    case Availability:
      return "availability";
    case MedicationAdministration:
      return "medicationadministration";
    case QuestionnaireAnswers:
      return "questionnaireanswers";
    case Encounter:
      return "encounter";
    case SecurityEvent:
      return "securityevent";
    case List:
      return "list";
    case DeviceUseStatement:
      return "deviceusestatement";
    case OperationDefinition:
      return "operationdefinition";
    case NutritionOrder:
      return "nutritionorder";
    case ClaimResponse:
      return "claimresponse";
    case ReferralRequest:
      return "referralrequest";
    case CommunicationRequest:
      return "communicationrequest";
    case RiskAssessment:
      return "riskassessment";
    case FamilyHistory:
      return "familyhistory";
    case Location:
      return "location";
    case ExplanationOfBenefit:
      return "explanationofbenefit";
    case AllergyIntolerance:
      return "allergyintolerance";
    case Observation:
      return "observation";
    case Contract:
      return "contract";
    case SupportingDocumentation:
      return "supportingdocumentation";
    case RelatedPerson:
      return "relatedperson";
    case Basic:
      return "basic";
    case Specimen:
      return "specimen";
    case Alert:
      return "alert";
    case Patient:
      return "patient";
    case DiagnosticOrder:
      return "diagnosticorder";
    }
      return null;
  }

}
