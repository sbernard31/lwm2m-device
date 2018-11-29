package org.eclipse.leshan.server.demo.coap;

import java.util.Collection;
import java.util.HashMap;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.servers.DmServerInfo;
import org.eclipse.leshan.client.servers.ServersInfo;
import org.eclipse.leshan.client.servers.ServersInfoExtractor;
import org.eclipse.leshan.core.response.ErrorCallback;

/**
 * A LWM2M client with CoAP facilities
 */
public class LwM2mCoAPClient implements LwM2mClient {

	private LeshanClient client;

	public LwM2mCoAPClient(LeshanClient client) {
		this.client = client;
	}

	@Override
	public Collection<LwM2mObjectEnabler> getObjectEnablers() {
		return client.getObjectEnablers();
	}

	@Override
	public void start() {
		client.start();
	}

	@Override
	public void stop(boolean deregister) {
		client.stop(deregister);
	}

	@Override
	public void destroy(boolean deregister) {
		client.destroy(deregister);
	}

	/**
	 * Allow to send CoAP request to the registered DM server.
	 * 
	 * @throws {@link IllegalStateException} if you try to send a request on a
	 *             device which is not register to a server.
	 */
	public void send(Request request, long timeout, ResponseCallback<Response> responseCallback,
			ErrorCallback errorCallback) {

		String registrationId = client.getRegistrationId();
		if (registrationId == null)
			throw new IllegalStateException("Device is not registered");

		// TODO will not be needed with next version of leshan:
		HashMap<Integer, LwM2mObjectEnabler> objectEnablers = new HashMap<>();
		for (LwM2mObjectEnabler enabler : getObjectEnablers()) {
			if (objectEnablers.containsKey(enabler.getId())) {
				throw new IllegalArgumentException(
						String.format("There is several objectEnablers with the same id %d.", enabler.getId()));
			}
			objectEnablers.put(enabler.getId(), enabler);
		}

		ServersInfo serversInfo = ServersInfoExtractor.getInfo(objectEnablers);
		DmServerInfo dmInfo = serversInfo.deviceMangements.values().iterator().next();

		request.setDestinationContext(new AddressEndpointContext(dmInfo.getAddress()));
		request.addMessageObserver(
				new AsyncRequestObserver<Response>(request, responseCallback, errorCallback, timeout) {

					@Override
					public Response buildResponse(Response coapResponse) {
						return coapResponse;
					}
				});
		getServerEndpoint(dmInfo).sendRequest(request);
	}

	private Endpoint getServerEndpoint(DmServerInfo dmInfo) {
		if (dmInfo.isSecure()) {
			return client.getCoapServer().getEndpoint(client.getSecuredAddress());
		} else {
			return client.getCoapServer().getEndpoint(client.getUnsecuredAddress());
		}
	}
	
	public void addObserver(LwM2mClientObserver observer) {
        client.addObserver(observer);
    }
}
