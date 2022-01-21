package org.emerse.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import com.fasterxml.jackson.core.JsonFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.eclipse.jetty.server.Request;
import org.hl7.fhir.r4.model.*;

import java.util.Optional;
import java.util.stream.Collectors;
//--fhir-url https://mcintvwrtst.med.umich.edu/Interconnect-TSTFHIR/api/FHIR/R4 --credentials C:\Users\wanggu\fhir-cred.properties
public class FhirHandler extends AbstractHandler
{
	public static final Base64 decoder = new Base64();

	public IGenericClient client;
	public JsonFactory jsonFactory;

	public static record Content(String text, String type) {}

	private Content getDocumentContent(DocumentReference document) {
		if (!document.getContent().isEmpty()) {
			DocumentReference.DocumentReferenceContentComponent content = document.getContentFirstRep();
			Attachment attachment = content.getAttachment();

			if (!attachment.getDataElement().isEmpty()) {
				return new Content(new String(attachment.getData()), attachment.getContentType());
			}

			if (!attachment.getUrlElement().isEmpty()) {
				Binary data = client.read().resource(Binary.class).withUrl(attachment.getUrl()).execute();
				return new Content(new String(decoder.decode(data.getContentAsBase64())), data.getContentType());
			}
		}

		return null;
	}

	@Override
	public void doHandle(
		String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response
	) throws Exception
	{
		var source = new ParameterSource(request);
		var fhirId = source.getParameter("fhir_id", String.class);
		var mrn = source.getParameter("mrn", String.class);
		var after = source.getParameter("after", String.class);
		var query =  client
				.search()
				.forResource(Patient.class);
		if(fhirId != null)
			query = query.where(new StringClientParam("_id").matches().value(fhirId));
		else if(mrn != null)
			//query = query.where(new StringClientParam("identifier").matches().value("MRN|" + mrn));
			query = query.where(new EpicMRNStringCriterion<StringClientParam>("identifier", "MRN|" + mrn));
		var resource = query
				.returnBundle(Bundle.class)
				.execute()
				.getEntryFirstRep()
				.getResource();
		if(!resource.getResourceType().name().equals("Patient"))
			throw new IllegalArgumentException("Patient does not exist");
		var
			patient =
			(Patient) resource;
		if(fhirId == null)
		{
			fhirId = patient.getIdentifier()
					.stream()
					.filter(id -> id.getType() != null
											   && id.getValue() != null
											   && Optional.ofNullable(id.getType().getText()).orElse("").equals("FHIR")
					)
					.map(Identifier::getValue)
					.findFirst().orElse(null);
		}
		var docBundle = client
							.search()
							.forResource(DocumentReference.class)
							//.lastUpdated(new DateRangeParam(new DateParam("ge" + after)))
							.where(DocumentReference.PATIENT.hasId(fhirId))
					.where(DocumentReference.PERIOD.afterOrEquals().day(after))
					.returnBundle(Bundle.class)
					.execute();
		var docList = docBundle.getEntry().stream()
					.filter(entry -> entry.getResource() instanceof DocumentReference)
					.map(entry -> (DocumentReference) entry.getResource())
					.collect(Collectors.toList());
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
			g.writeFieldName("notes");
			g.writeStartArray();
			for (var doc : docList)
			{
				var content = getDocumentContent(doc);
				if(content != null) {
					g.writeStartObject();
					g.writeNumberField("timestamp", doc.getDate().getTime());
					g.writeStringField("noteType", doc.getType().getText());
					g.writeStringField("contentType", content.type);
					g.writeStringField("text", content.text);
					g.writeEndObject();
				}
			}
			g.writeEndArray();
			g.writeEndObject();
		}
	}
}
