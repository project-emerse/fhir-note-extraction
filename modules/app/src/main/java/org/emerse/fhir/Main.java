package org.emerse.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import com.fasterxml.jackson.core.JsonFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.util.URIUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collector;

public class Main
{
	private static JsonFactory jsonFactory;
	private static String fhirBaseUrl;
	private static int port = 8080;
	private static String htmlRoot;
	private static DefaultHandler fallbackHandler = new DefaultHandler();
	private static IClientInterceptor basicAuth;
	private static AdditionalRequestHeadersInterceptor headers;

	public static void main(String[] args) throws Exception
	{
		parseArguments(args);

		jsonFactory = new JsonFactory();

		var server = new Server();
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

		//server.setHandler(new HandlerList(handlerMap, new DefaultHandler()));

		server.setHandler(new AbstractHandler() {
							  @Override
							  protected void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws Exception {
								  try {
									  switch (target) {
										  case "/fhir" -> fhirHandler.doHandle(target, baseRequest, request, response);
										  default -> writeResource(baseRequest, request, response, target);
									  }
								  } catch (Exception e) {
									  if (e instanceof IOException ioe) {
										  throw ioe;
									  } else if (e instanceof ServletException se) {
										  throw se;
									  } else {
										  throw new RuntimeException(e);
									  }
								  }
							  }
						  }

		);

		var connector = new ServerConnector(server);
		server.addConnector(connector);
		connector.setPort(port);
		System.out.println("Will listen on: " + port);
		System.out.println("See page http://localhost:" + port + "/index.html");

		server.start();
	}

	private static void writeResource(Request baseRequest, HttpServletRequest request, HttpServletResponse response, String resource)
			throws IOException, ServletException {
		if(htmlRoot == null) {
			fallbackHandler.handle(resource, baseRequest, request, response);
			return;
		}
		var path = Path.of(URIUtil.canonicalPath(htmlRoot + resource));
		if (!Files.exists(path))
		{
			response.setStatus(404);
			response.getWriter().println("Resource not found");
			return;
		}
		var stream = Files.newInputStream(path);
		response.setStatus(200);
		if (resource.endsWith(".mjs"))
		{
			response.addHeader("Content-Type", "text/javascript");
		}
		else if (resource.endsWith(".html"))
		{
			response.addHeader("Content-Type", "text/html");
		}
		else if (resource.endsWith(".css"))
		{
			response.addHeader("Content-Type", "text/css");
		}
		else if (resource.endsWith(".ico"))
		{
			response.addHeader("Content-Type", "image/png");
		}
		try (
				var outC = Channels.newChannel(response.getOutputStream());
				var inC = Channels.newChannel(stream)
		)
		{
			var buf = ByteBuffer.allocate(1024);
			buf.clear();
			while (-1 != inC.read(buf))
			{
				buf.flip();
				while (buf.hasRemaining())
				{
					outC.write(buf);
				}
				buf.compact();
			}
		}
	}

	static FHIRAuth loadCredentials(String filePath) throws IOException {
		Properties props = new Properties();
		BasicAuthInterceptor ba;
		AdditionalRequestHeadersInterceptor hd;
		try(var f = new FileInputStream(filePath)) {
			props.load(f);
			ba = new BasicAuthInterceptor(Optional.ofNullable(props.get("username")).orElse("").toString(),
					Optional.ofNullable(props.get("password")).orElse("").toString());
			hd = props.entrySet().stream().filter(ks -> !ks.getKey().equals("username") && !ks.getKey().equals("password"))
					.collect(
							Collector.of(AdditionalRequestHeadersInterceptor::new,
									(h, ks) -> h.addHeaderValue(ks.getKey().toString(), ks.getValue().toString()),
									(h1, h2) -> null,
									Function.identity()
							));
		}
		return ba != null && hd != null ? new FHIRAuth(ba, hd) : null;
	}

	private static void parseArguments(String[] args) throws IOException {
		for (int i = 0; i < args.length; i++)
		{
			switch (args[i])
			{
				case "--fhir-url" -> fhirBaseUrl = args[++i];
				case "--port" -> port = Integer.parseInt(args[++i]);
				case "--html-root" -> htmlRoot = args[++i];
				case "--credentials" -> {
					var fa = loadCredentials(args[++i]);
					if(fa != null)
					{
						basicAuth = fa.basicAuth;
						headers = fa.headers;
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
			  --fhir-url <url>       	    [required] the url of the fhir endpoint
			  --credentials <credential>	the credential file containing info of basic auth and header parameters
			  --port <port>          	    the url to listen on
			  --html-root <root>	 		the html root absolute path
			""");
		System.exit(1);
	}

	public record FHIRAuth(IClientInterceptor basicAuth, AdditionalRequestHeadersInterceptor headers){};
}
