package org.emerse.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import com.fasterxml.jackson.core.JsonFactory;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.MultiMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestBatchLoad {
    static String fhirServerUrl;
    private static IClientInterceptor basicAuth;
    private static AdditionalRequestHeadersInterceptor headers;
    private static IGenericClient fhirClient;
    private static List<String> mrns;

    @BeforeAll
    public static void loadConfig() throws IOException, URISyntaxException {

        Files.readAllLines(
                Path.of(
                        Objects.requireNonNull(
                                TestBatchLoad.class.getClassLoader().getResource("fhir.config")).toURI()
                )
        ).forEach(line -> {
            var ps = line.split("=");
            switch (ps[0]) {
                case "fhir-url" -> fhirServerUrl = ps[1];
                case "mrn-list" -> {
                    try {
                        mrns = Files.readAllLines(Path.of(ps[1])).stream()
                                .map(m -> m.length() == 9 ? m : StringUtils.leftPad(m, 9, '0'))
                                .collect(Collectors.toList());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                case "fhir-cred" -> {
                    if (ps.length > 1) {
                        Main.FHIRAuth rt = null;
                        try {
                            rt = Main.loadCredentials(ps[1]);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        if (rt != null) {
                            basicAuth = rt.basicAuth();
                            headers = rt.headers();
                        }
                    }
                }
            }
        });

        fhirClient = FhirContext.forR4().getRestfulClientFactory().newGenericClient(fhirServerUrl);
        if (basicAuth != null)
            fhirClient.registerInterceptor(basicAuth);
        if (headers != null)
            fhirClient.registerInterceptor(headers);
    }

    @Test
    public void testBatchMode() throws Exception {
        List<FhirQueryStat> results = new ArrayList<>();
        var fhirHandler = new FhirHandler();
        fhirHandler.jsonFactory = new JsonFactory();
        fhirHandler.client = fhirClient;
        AtomicReference<Integer> ref = new AtomicReference<>(0);
        mrns.forEach(mrn -> {
            try {
                MockHttpRequest request = new MockHttpRequest();
                MultiMap<String> params = new MultiMap<>();
                params.put("mrn", mrn);
                params.put("after", "2021-11-01");
                request.setQueryParameters(params);

                MockHttpResponse response = new MockHttpResponse();

                var t = System.nanoTime();
                fhirHandler.doHandle("fhir", null, request, response);
                t = System.nanoTime() - t;

                assertEquals(HttpServletResponse.SC_OK, response.getStatus());
                results.add(new FhirQueryStat(t, response.getTotalLength(), response.getNoteCount(), mrn));
            } catch (Exception e) {
                e.printStackTrace();
                ref.set(ref.getAcquire() + 1);
            }
        });
        System.out.println("***Query notes after 2021-11-01 till now**");
        System.out.println("******************************************");
        System.out.println("Total MRNs: " + (mrns.size() - ref.get()));
        System.out.println("******************************************");
        LongSummaryStatistics timeStat = results.stream()
                .collect(Collectors.summarizingLong(FhirQueryStat::timespan));
        System.out.println("Total time span (sec): " + timeStat.getSum() * 1e-9);
        System.out.println("Average time span per MRN (sec): " + timeStat.getAverage() * 1e-9);
        System.out.println("Max time span per MRN (sec): " + timeStat.getMax() * 1e-9);
        System.out.println("******************************************");
        IntSummaryStatistics byteStat = results.stream()
                .collect(Collectors.summarizingInt(FhirQueryStat::length));
        System.out.println("Total size (byte): " + byteStat.getSum());
        System.out.println("Average size per MRN (byte): " + byteStat.getAverage());
        System.out.println("Max size per MRN (byte): " + byteStat.getMax());
        System.out.println("******************************************");
        IntSummaryStatistics noteStat = results.stream()
                .collect(Collectors.summarizingInt(FhirQueryStat::size));
        System.out.println("Total note count: " + noteStat.getSum());
        System.out.println("Average note count per MRN: " + noteStat.getAverage());
        System.out.println("Max note count per MRN: " + noteStat.getMax());
        System.out.println("Min note count per MRN: " + noteStat.getMin());
        System.out.println("******************************************");
    }

    public record FhirQueryStat(long timespan, int length, int size, String mrn) {
    }
}
