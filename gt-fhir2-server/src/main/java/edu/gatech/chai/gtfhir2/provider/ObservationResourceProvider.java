package edu.gatech.chai.gtfhir2.provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.InstantType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import edu.gatech.chai.gtfhir2.mapping.OmopObservation;
import edu.gatech.chai.omopv5.jpa.service.ParameterWrapper;

public class ObservationResourceProvider implements IResourceProvider {

	private WebApplicationContext myAppCtx;
	private String myDbType;
	private OmopObservation myMapper;
	private int preferredPageSize = 30;

	public ObservationResourceProvider() {
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
		myDbType = myAppCtx.getServletContext().getInitParameter("backendDbType");
		if (myDbType.equalsIgnoreCase("omopv5") == true) {
			myMapper = new OmopObservation(myAppCtx);
		} else {
			myMapper = new OmopObservation(myAppCtx);
		}
		
		String pageSizeStr = myAppCtx.getServletContext().getInitParameter("preferredPageSize");
		if (pageSizeStr != null && pageSizeStr.isEmpty() == false) {
			int pageSize = Integer.parseInt(pageSizeStr);
			if (pageSize > 0) {
				preferredPageSize = pageSize;
			} 
		}
	}
	
	/**
	 * The "@Create" annotation indicates that this method implements "create=type", which adds a 
	 * new instance of a resource to the server.
	 */
	@Create()
	public MethodOutcome createPatient(@ResourceParam Observation theObservation) {
		validateResource(theObservation);
		
		Long id = myMapper.toDbase(theObservation);		
		return new MethodOutcome(new IdDt(id));
	}

	@Search()
	public IBundleProvider findPatientsByParams(
			@OptionalParam(name=Observation.SP_RES_ID) TokenParam theObservationId,
			
			@IncludeParam(allow={"Observation:based-on", "Observation:context", 
					"Observation:device", "Observation:encounter", "Observation:patient", 
					"Observation:performer", "Observation:related-target", 
					"Observation:specimen", "Observation:subject"})
			final Set<Include> theIncludes,
			
			@IncludeParam(reverse=true)
            final Set<Include> theReverseIncludes
			) {
		final InstantType searchTime = InstantType.withCurrentTime();
		
		Map<String, List<ParameterWrapper>> paramMap = new HashMap<String, List<ParameterWrapper>> ();

		if (theObservationId != null) {
			mapParameter (paramMap, Observation.SP_RES_ID, theObservationId);
		}
		
		// Now finalize the parameter map.
		final Map<String, List<ParameterWrapper>> finalParamMap = paramMap;
		final Long totalSize;
		if (paramMap.size() == 0) {
			totalSize = myMapper.getSize();
		} else {
			totalSize = myMapper.getSize(finalParamMap);
		}

		return new IBundleProvider() {

			@Override
			public IPrimitiveType<Date> getPublished() {
				return searchTime;
			}

			@Override
			public List<IBaseResource> getResources(int fromIndex, int toIndex) {
				List<IBaseResource> retv = new ArrayList<IBaseResource>();
				
				// _Include
				List<String> includes = new ArrayList<String>();
				
				if (theIncludes.contains(new Include("Observation:based-on"))) {
					includes.add("Observation:based-on");
				}
				
				if (theIncludes.contains(new Include("Observation:context"))) {
					includes.add("Observation:context");
				}

				if (theIncludes.contains(new Include("Observation:device"))) {
					includes.add("Observation:device");
				}
				
				if (theIncludes.contains(new Include("Observation:encounter"))) {
					includes.add("Observation:encounter");
				}

				if (theIncludes.contains(new Include("Observation:patient"))) {
					includes.add("Observation:patient");
				}

				if (theIncludes.contains(new Include("Observation:performer"))) {
					includes.add("Observation:performer");
				}

				if (theIncludes.contains(new Include("Observation:related-target"))) {
					includes.add("Observation:related-target");
				}

				if (theIncludes.contains(new Include("Observation:specimen"))) {
					includes.add("Observation:specimen");
				}

				if (theIncludes.contains(new Include("Observation:subject"))) {
					includes.add("Observation:subject");
				}

				if (finalParamMap.size() == 0) {
					myMapper.searchWithoutParams(fromIndex, toIndex, retv, includes);
				} else {
					myMapper.searchWithParams(fromIndex, toIndex, finalParamMap, retv, includes);
				}

				return retv;
			}

			@Override
			public String getUuid() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Integer preferredPageSize() {
				return preferredPageSize;
			}

			@Override
			public Integer size() {
				return totalSize.intValue();
			}
		};
	}

	private void mapParameter(Map<String, List<ParameterWrapper>> paramMap, String FHIRparam, Object value) {
		List<ParameterWrapper> paramList = myMapper.mapParameter(FHIRparam, value);
		if (paramList != null) {
			paramMap.put(FHIRparam, paramList);
		}
	}

	// TODO: Add more validation code here.
	private void validateResource(Observation theObservation) {
		if (theObservation.getCode().isEmpty()) {
			OperationOutcome outcome = new OperationOutcome();
			CodeableConcept detailCode = new CodeableConcept();
			detailCode.setText("No family name provided, Patient resources must have at least one family name.");
			outcome.addIssue().setSeverity(IssueSeverity.FATAL).setDetails(detailCode);
			throw new UnprocessableEntityException(FhirContext.forDstu3(), outcome);
		}		
	}

	@Override
	public Class<Observation> getResourceType() {
		return Observation.class;
	}

}