package org.eclipse.leshan.server.demo.coap;

/**
 * On success callback for request.
 */
public interface ResponseCallback<T> {
    // We should keep this as a 1 method interface to be java 8 lambda compatible.

    /**
     * Called when the request succeed.
     */
    void onResponse(T response);

}
