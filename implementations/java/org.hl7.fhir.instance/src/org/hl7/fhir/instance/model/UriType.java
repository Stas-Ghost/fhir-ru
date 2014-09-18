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
package org.hl7.fhir.instance.model;

/**
 * Primitive type "uri" in FHIR: any valid URI. Sometimes constrained to be only an absolute URI, and sometimes constrained to be a literal reference
 */
public class UriType extends PrimitiveType {
	
  private static final long serialVersionUID = -4774715915772053479L;
	/**
	 * any valid URI
	 */
	protected String value;

  public UriType() {
  }

  public UriType(String uri) {
    this.value = uri;
  }

	/**
	 * @return - the URI value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value any valid URI
	 */
	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
  public UriType copy() {
		UriType dst = new UriType();
		dst.value = value;
		return dst;
	}
	
	@Override
  protected Type typedCopy() {
		return copy();
	}

  @Override
  public String asStringValue() {
    return value;
  }

}