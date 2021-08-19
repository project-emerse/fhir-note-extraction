package org.emerse.fhir;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;

public class Main
{
	private static JsonFactory jsonFactory;
	private static String fhirBaseUrl;
	private static int port = 8080;

	public static void main(String[] args) throws Exception
	{
		parseArguments(args);

		jsonFactory = new JsonFactory();

		var server = new Server(port);
		var handlerMap = new HandlerMap();

		var fhir = FhirContext.forDstu3();
		var fhirClient = fhir.getRestfulClientFactory().newGenericClient(fhirBaseUrl);

		var fhirHandler = new FhirHandler();
		fhirHandler.jsonFactory = jsonFactory;
		fhirHandler.client = fhirClient;

		handlerMap.put("fhir", fhirHandler);

		server.setHandler(new HandlerList(handlerMap, new DefaultHandler()));
		System.out.println("Listening on " + port);
		server.start();
	}

	private static void parseArguments(String[] args)
	{
		for (int i = 0; i < args.length; i++)
		{
			switch (args[i])
			{
				case "--fhir-url" -> fhirBaseUrl = args[++i];
				case "--port" -> port = Integer.parseInt(args[++i]);
				case "--help" -> printHelp();
			}
		}
		if (fhirBaseUrl == null)
		{
			System.err.println("--fhir-url must be set");
			printHelp();
			System.exit(1);
		}
	}

	private static void printHelp()
	{
		System.out.println("""
			Options:
			  --fhir-url <url>       [required] the url of the fhir endpoint
			  --port <port>          the url to listen on
			""");
		System.exit(1);
	}
}
