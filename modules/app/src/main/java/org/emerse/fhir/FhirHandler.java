package org.emerse.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import com.fasterxml.jackson.core.JsonFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;

public class FhirHandler extends AbstractHandler
{
	public IGenericClient client;
	public JsonFactory jsonFactory;

	@Override
	public void doHandle(
		String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response
	) throws Exception
	{
		var source = new ParameterSource(request);
		var mrn = source.getParameter("fhir_id", String.class);
		var
			patient =
			(Patient) client
				.search()
				.forResource(Patient.class)
				.where(new StringClientParam("_id").matches().value(mrn))
				.returnBundle(Bundle.class)
				.execute()
				.getEntryFirstRep()
				.getResource();
		response.setStatus(HttpServletResponse.SC_OK);
		try (
			var w = response.getWriter();
			var g = jsonFactory.createGenerator(w)
		)
		{
			g.writeStartObject();
			g.writeFieldName("names");
			g.writeStartArray();
			for (HumanName humanName : patient.getName())
			{
				g.writeString(humanName.getText());
			}
			g.writeEndArray();
			g.writeEndObject();
		}
	}
}
