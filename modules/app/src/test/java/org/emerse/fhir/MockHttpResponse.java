package org.emerse.fhir;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class MockHttpResponse extends Response {
    private static ObjectMapper objectMapper = new ObjectMapper();
    private ByteArrayOutputStream outputStream;
    private PrintWriter writer;

    public MockHttpResponse() {
        this((HttpChannel)null, (HttpOutput)null);
        outputStream = new ByteArrayOutputStream();
    }

    public MockHttpResponse(HttpChannel channel, HttpOutput out) {
        super(channel, out);
    }

    public int getTotalLength() {
        return outputStream.toByteArray().length;
    }

    public int getNoteCount() throws JsonProcessingException {
        var node = objectMapper.readTree(outputStream.toString());
        return node.get("notes").size();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if(writer == null)
            writer = new PrintWriter(outputStream);
        else
            writer.flush();
        return writer;
    }

    @Override
    public String toString() {
        return "MockHttpResponse";
    }
}
