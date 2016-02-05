package org.hl7.fhir.dstu21.model.valuesets;

/*
  Copyright (c) 2011+, HL7, Inc.
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

// Generated on Sat, Jan 23, 2016 23:37-0700 for FHIR v1.3.0


import org.hl7.fhir.dstu21.model.EnumFactory;

public class PatientMpiMatchEnumFactory implements EnumFactory<PatientMpiMatch> {

  public PatientMpiMatch fromCode(String codeString) throws IllegalArgumentException {
    if (codeString == null || "".equals(codeString))
      return null;
    if ("certain".equals(codeString))
      return PatientMpiMatch.CERTAIN;
    if ("probable".equals(codeString))
      return PatientMpiMatch.PROBABLE;
    if ("possible".equals(codeString))
      return PatientMpiMatch.POSSIBLE;
    if ("certainly-not".equals(codeString))
      return PatientMpiMatch.CERTAINLYNOT;
    throw new IllegalArgumentException("Unknown PatientMpiMatch code '"+codeString+"'");
  }

  public String toCode(PatientMpiMatch code) {
    if (code == PatientMpiMatch.CERTAIN)
      return "certain";
    if (code == PatientMpiMatch.PROBABLE)
      return "probable";
    if (code == PatientMpiMatch.POSSIBLE)
      return "possible";
    if (code == PatientMpiMatch.CERTAINLYNOT)
      return "certainly-not";
    return "?";
  }

    public String toSystem(PatientMpiMatch code) {
      return code.getSystem();
      }

}
