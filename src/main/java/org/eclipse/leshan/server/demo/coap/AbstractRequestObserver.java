package org.eclipse.leshan.server.demo.coap;

import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;

public abstract class AbstractRequestObserver<T> extends MessageObserverAdapter {
    Request coapRequest;

    public AbstractRequestObserver(Request coapRequest) {
        this.coapRequest = coapRequest;
    }

    public abstract T buildResponse(Response coapResponse);
}