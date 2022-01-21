package org.emerse.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.ICriterionInternal;
import ca.uhn.fhir.rest.gclient.StringClientParam;

public class EpicMRNStringCriterion <T extends StringClientParam> implements ICriterion<T>, ICriterionInternal {
    private String myValue;
    private String myName;

    public EpicMRNStringCriterion(String theName, String theValue) {
        myName = theName;
        myValue = theValue;
    }
    @Override
    public String getParameterValue(FhirContext theContext) {
        return myValue;
    }

    @Override
    public String getParameterName() {
        return myName;
    }
}
