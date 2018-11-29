package org.eclipse.leshan.server.demo.model;

import org.eclipse.leshan.client.resource.SimpleInstanceEnabler;
import org.eclipse.leshan.core.response.ReadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Object10250 extends SimpleInstanceEnabler {

	private static final Logger LOG = LoggerFactory.getLogger(Object10250.class);
	
	// boolean to switch on small or large payload
	private boolean largePayload = true;
	
	@Override
	public ReadResponse read(int resourceid) {
		if (largePayload) {
			return super.read(resourceid);
		}else {
			switch (resourceid) {
			case 4:
				LOG.info("Read resource {} on object 10250", resourceid);
				return ReadResponse.success(4, "1.0.0");
			}
			return ReadResponse.notFound();
		}
	}
	
	public void setLargePayload(boolean largePayload) {
		this.largePayload = largePayload;
	}
}
