package org.emerse.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import com.fasterxml.jackson.core.JsonFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collector;

public class Main
{
	private static JsonFactory jsonFactory;
	private static String fhirBaseUrl;
	private static int port = 8080;
	private static IClientInterceptor basicAuth;
	private static AdditionalRequestHeadersInterceptor headers;

	public static void main(String[] args) throws Exception
	{
		parseArguments(args);

		jsonFactory = new JsonFactory();

		var server = new Server(port);
		var handlerMap = new HandlerMap();

		var fhir = FhirContext.forR4();
		var fhirClient = fhir.getRestfulClientFactory().newGenericClient(fhirBaseUrl);
		//https://hapifhir.io/hapi-fhir/docs/interceptors/built_in_client_interceptors.html
		if(basicAuth != null)
			fhirClient.registerInterceptor(basicAuth);
		if(headers != null)
			fhirClient.registerInterceptor(headers);

		var fhirHandler = new FhirHandler();
		fhirHandler.jsonFactory = jsonFactory;
		fhirHandler.client = fhirClient;

		handlerMap.put("fhir", fhirHandler);

		server.setHandler(new HandlerList(handlerMap, new DefaultHandler()));
		System.out.println("Listening on " + port);
		server.start();
	}

	private static void parseArguments(String[] args) throws IOException {
		for (int i = 0; i < args.length; i++)
		{
			switch (args[i])
			{
				case "--fhir-url" -> fhirBaseUrl = args[++i];
				case "--port" -> port = Integer.parseInt(args[++i]);
				case "--credentials" -> {
					Properties props = new Properties();
					try(var f = new FileInputStream(args[++i])) {
						props.load(f);
						basicAuth = new BasicAuthInterceptor(Optional.ofNullable(props.get("username")).orElse("").toString(),
															 Optional.ofNullable(props.get("password")).orElse("").toString());
						headers = props.entrySet().stream().filter(ks -> !ks.getKey().equals("username") && !ks.getKey().equals("password"))
												  .collect(
													  Collector.of(AdditionalRequestHeadersInterceptor::new,
													  (h, ks) -> h.addHeaderValue(ks.getKey().toString(), ks.getValue().toString()),
													  (h1, h2) -> null,
													  Function.identity()
												  ));
					}
				}
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
