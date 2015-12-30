package org.hl7.fhir.dstu21.hapi.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu21.formats.IParser;
import org.hl7.fhir.dstu21.formats.ParserType;
import org.hl7.fhir.dstu21.hapi.validation.IValidationSupport.CodeValidationResult;
import org.hl7.fhir.dstu21.model.CodeableConcept;
import org.hl7.fhir.dstu21.model.Coding;
import org.hl7.fhir.dstu21.model.ConceptMap;
import org.hl7.fhir.dstu21.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.dstu21.model.Resource;
import org.hl7.fhir.dstu21.model.ResourceType;
import org.hl7.fhir.dstu21.model.ValueSet;
import org.hl7.fhir.dstu21.model.ValueSet.ConceptDefinitionComponent;
import org.hl7.fhir.dstu21.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.dstu21.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.dstu21.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.dstu21.terminologies.ValueSetExpander;
import org.hl7.fhir.dstu21.terminologies.ValueSetExpanderFactory;
import org.hl7.fhir.dstu21.terminologies.ValueSetExpanderSimple;
import org.hl7.fhir.dstu21.utils.INarrativeGenerator;
import org.hl7.fhir.dstu21.utils.IWorkerContext;
import org.hl7.fhir.dstu21.validation.IResourceValidator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

public final class HapiWorkerContext implements IWorkerContext, ValueSetExpander, ValueSetExpanderFactory {
	private final FhirContext myCtx;
	private IValidationSupport myValidationSupport;

	public HapiWorkerContext(FhirContext theCtx, IValidationSupport theValidationSupport) {
		myCtx = theCtx;
		myValidationSupport = theValidationSupport;
	}

	@Override
	public ValueSetExpansionComponent expandVS(ConceptSetComponent theInc) {
		return myValidationSupport.expandValueSet(myCtx, theInc);
	}

	@Override
	public ValueSet fetchCodeSystem(String theSystem) {
		if (myValidationSupport == null) {
			return null;
		} else {
			return myValidationSupport.fetchCodeSystem(myCtx, theSystem);
		}
	}

	@Override
	public <T extends Resource> T fetchResource(Class<T> theClass, String theUri) {
		if (myValidationSupport == null) {
			return null;
		} else {
			return myValidationSupport.fetchResource(myCtx, theClass, theUri);
		}
	}

	@Override
	public INarrativeGenerator getNarrativeGenerator(String thePrefix, String theBasePath) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IParser getParser(ParserType theType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IParser getParser(String theType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends Resource> boolean hasResource(Class<T> theClass_, String theUri) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IParser newJsonParser() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IResourceValidator newValidator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IParser newXmlParser() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsSystem(String theSystem) {
		if (myValidationSupport == null) {
			return false;
		} else {
			return myValidationSupport.isCodeSystemSupported(myCtx, theSystem);
		}
	}

	@Override
	public ValidationResult validateCode(String theSystem, String theCode, String theDisplay) {
		CodeValidationResult result = myValidationSupport.validateCode(myCtx, theSystem, theCode, theDisplay);
		if (result == null) {
			return null;
		}
		return new ValidationResult(result.getSeverity(), result.getMessage(), result.asConceptDefinition());
	}

	@Override
	public ValidationResult validateCode(String theSystem, String theCode, String theDisplay, ConceptSetComponent theVsi) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ValidationResult validateCode(String theSystem, String theCode, String theDisplay, ValueSet theVs) {
		if (theSystem == null || StringUtils.equals(theSystem, theVs.getCodeSystem().getSystem())) {
			for (ConceptDefinitionComponent next : theVs.getCodeSystem().getConcept()) {
				ValidationResult retVal = validateCodeSystem(theCode, next);
				if (retVal != null && retVal.isOk()) {
					return retVal;
				}
			}
		}

		for (ConceptSetComponent nextComposeConceptSet : theVs.getCompose().getInclude()) {
			if (StringUtils.equals(theSystem, nextComposeConceptSet.getSystem())) {
				for (ConceptReferenceComponent nextComposeCode : nextComposeConceptSet.getConcept()) {
					ConceptDefinitionComponent conceptDef = new ConceptDefinitionComponent();
					conceptDef.setCode(nextComposeCode.getCode());
					conceptDef.setDisplay(nextComposeCode.getDisplay());
					ValidationResult retVal = validateCodeSystem(theCode, conceptDef);
					if (retVal != null && retVal.isOk()) {
						return retVal;
					}
				}
			}
		}
		return new ValidationResult(IssueSeverity.ERROR, "Unknown code[" + theCode + "] in system[" + theSystem + "]");
	}

	private ValidationResult validateCodeSystem(String theCode, ConceptDefinitionComponent theConcept) {
		if (StringUtils.equals(theCode, theConcept.getCode())) {
			return new ValidationResult(theConcept);
		} else {
			for (ConceptDefinitionComponent next : theConcept.getConcept()) {
				ValidationResult retVal = validateCodeSystem(theCode, next);
				if (retVal != null && retVal.isOk()) {
					return retVal;
				}
			}
			return null;
		}
	}

	@Override
	public ValueSetExpansionOutcome expand(ValueSet theSource) {
		ValueSetExpansionOutcome vso;
		try {
			vso = getExpander().expand(theSource);
		} catch (Exception e) {
			throw new InternalErrorException(e);
		}
		if (vso.getError() != null) {
			return null;
		} else {
			return vso;
		}
	}

	@Override
	public List<ConceptMap> findMapsForSource(String theUrl) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ValueSetExpansionOutcome expandVS(ValueSet theSource, boolean theCacheOk) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ValidationResult validateCode(Coding theCode, ValueSet theVs) {
		String system = theCode.getSystem();
		String code = theCode.getCode();
		String display = theCode.getDisplay();
		return validateCode(system, code, display, theVs);
	}

	@Override
	public ValidationResult validateCode(CodeableConcept theCode, ValueSet theVs) {
		for (Coding next : theCode.getCoding()) {
			ValidationResult retVal = validateCode(next, theVs);
			if (retVal != null && retVal.isOk()) {
				return retVal;
			}
		}

		return new ValidationResult(null, null);
	}

	@Override
	public List<String> getResourceNames() {
		List<String> result = new ArrayList<String>();
		for (ResourceType next : ResourceType.values()) {
			result.add(next.name());
		}
		Collections.sort(result);
		return result;
	}

	@Override
	public String getAbbreviation(String theName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ValueSetExpander getExpander() {
		return new ValueSetExpanderSimple(this, this);
	}
}