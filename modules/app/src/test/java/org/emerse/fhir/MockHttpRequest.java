package org.emerse.fhir;

import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.Request;

public class MockHttpRequest extends Request {
    public MockHttpRequest() {
        this((HttpChannel)null, (HttpInput)null);
    }
    public MockHttpRequest(HttpChannel channel, HttpInput input) {
        super(channel, input);
    }
}
