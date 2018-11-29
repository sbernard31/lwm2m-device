package org.eclipse.leshan.server.demo;

import static org.eclipse.leshan.LwM2mId.DEVICE;
import static org.eclipse.leshan.LwM2mId.SECURITY;
import static org.eclipse.leshan.LwM2mId.SERVER;
import static org.eclipse.leshan.client.object.Security.noSec;
import static org.eclipse.leshan.client.object.Security.noSecBootstap;
import static org.eclipse.leshan.client.object.Security.psk;
import static org.eclipse.leshan.client.object.Security.pskBootstrap;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.leshan.LwM2m;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.observer.LwM2mClientObserverAdapter;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.servers.DmServerInfo;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.server.demo.coap.LwM2mCoAPClient;
import org.eclipse.leshan.server.demo.coap.ResponseCallback;
import org.eclipse.leshan.server.demo.codec.JsonToCbor;
import org.eclipse.leshan.server.demo.model.MyDevice;
import org.eclipse.leshan.server.demo.model.Object10250;
import org.eclipse.leshan.server.demo.scriptedpush.ScriptEntry;
import org.eclipse.leshan.server.demo.scriptedpush.ScriptedPushParser;
import org.eclipse.leshan.server.demo.scriptedpush.ScriptedPusher;
import org.eclipse.leshan.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

public class LwM2mDeviceDemo {

	private static final Logger LOG = LoggerFactory.getLogger(LwM2mDeviceDemo.class);

	// Add new models in this arrays.
	// private final static String[] modelPaths = new String[] {};

	private final static String DEFAULT_ENDPOINT = "LeshanClientDemo";
	private final static String USAGE = "java -jar leshan-client-demo.jar [OPTION]";

	public static void main(final String[] args) {

		// Define options for command line tools
		Options options = new Options();

		options.addOption("h", "help", false, "Display help information.");
		options.addOption("n", true, String.format(
				"Set the endpoint name of the Client.\nDefault: the local hostname or '%s' if any.", DEFAULT_ENDPOINT));
		options.addOption("b", false, "If present use bootstrap.");
		options.addOption("lh", true, "Set the local CoAP address of the Client.\n  Default: any local address.");
		options.addOption("lp", true,
				"Set the local CoAP port of the Client.\n  Default: A valid port value is between 0 and 65535.");
		options.addOption("slh", true, "Set the secure local CoAP address of the Client.\nDefault: any local address.");
		options.addOption("slp", true,
				"Set the secure local CoAP port of the Client.\nDefault: A valid port value is between 0 and 65535.");
		options.addOption("u", true, String.format("Set the LWM2M or Bootstrap server URL.\nDefault: localhost:%d.",
				LwM2m.DEFAULT_COAP_PORT));
		options.addOption("i", true,
				"Set the LWM2M or Bootstrap server PSK identity in ascii.\nUse none secure mode if not set.");
		options.addOption("p", true,
				"Set the LWM2M or Bootstrap server Pre-Shared-Key in hexa.\nUse none secure mode if not set.");
		options.addOption("l", true, "Set the LWM2M lifetime in seconds. It should be higher than COAP_MAX_EXCHANGE_LIFETIME (~247s).\nDefault: 500s");
		options.addOption("pv", true,
				"This demo allow you to send data to coap(s)://server.uri:port/push?ep=deviceendpoint using CBOR. This option define the payload to send in JSON.");
		options.addOption("pf", true,
				"This demo allow you to send data to coap(s)://server.uri:port/push?ep=deviceendpoint using CBOR. The value of this option is the path to a file which contains data to send and when to send it. First line contains time to wait in ms before to send data, second line contains data to json payload and so on. (Limitation your JSON payload should stay in one line)");
		HelpFormatter formatter = new HelpFormatter();
		formatter.setOptionComparator(null);

		// Parse arguments
		CommandLine cl;
		try {
			cl = new DefaultParser().parse(options, args);
		} catch (ParseException e) {
			System.err.println("Parsing failed.  Reason: " + e.getMessage());
			formatter.printHelp(USAGE, options);
			return;
		}

		// Print help
		if (cl.hasOption("help")) {
			formatter.printHelp(USAGE, options);
			return;
		}

		// Abort if unexpected options
		if (cl.getArgs().length > 0) {
			System.err.println("Unexpected option or arguments : " + cl.getArgList());
			formatter.printHelp(USAGE, options);
			return;
		}

		// Abort if we have not identity and key for psk.
		if ((cl.hasOption("i") && !cl.hasOption("p")) || !cl.hasOption("i") && cl.hasOption("p")) {
			System.err.println("You should precise identity and Pre-Shared-Key if you want to connect in PSK");
			formatter.printHelp(USAGE, options);
			return;
		}

		// Get endpoint name
		String endpoint;
		if (cl.hasOption("n")) {
			endpoint = cl.getOptionValue("n");
		} else {
			try {
				endpoint = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				endpoint = DEFAULT_ENDPOINT;
			}
		}

		// Get server URI
		String serverURI;
		if (cl.hasOption("u")) {
			if (cl.hasOption("i"))
				serverURI = "coaps://" + cl.getOptionValue("u");
			else
				serverURI = "coap://" + cl.getOptionValue("u");
		} else {
			if (cl.hasOption("i"))
				serverURI = "coaps://localhost:" + LwM2m.DEFAULT_COAP_SECURE_PORT;
			else
				serverURI = "coap://localhost:" + LwM2m.DEFAULT_COAP_PORT;
		}

		// get security info
		byte[] pskIdentity = null;
		byte[] pskKey = null;
		if (cl.hasOption("i") && cl.hasOption("p")) {
			pskIdentity = cl.getOptionValue("i").getBytes();
			pskKey = Hex.decodeHex(cl.getOptionValue("p").toCharArray());
		}

		// get local address
		String localAddress = null;
		int localPort = 0;
		if (cl.hasOption("lh")) {
			localAddress = cl.getOptionValue("lh");
		}
		if (cl.hasOption("lp")) {
			localPort = Integer.parseInt(cl.getOptionValue("lp"));
		}

		// get secure local address
		String secureLocalAddress = null;
		int secureLocalPort = 0;
		if (cl.hasOption("slh")) {
			secureLocalAddress = cl.getOptionValue("slh");
		}
		if (cl.hasOption("slp")) {
			secureLocalPort = Integer.parseInt(cl.getOptionValue("slp"));
		}

		// get lifetime
		long lifetime = 300; // default lifetime to 300.
		if (cl.hasOption("l")) {
			try {
				lifetime = Long.parseLong(cl.getOptionValue("l"));
				if (lifetime < 1) {
					System.err.println("Invalid value for -l option, lifetime should be an postive not null integer: ");
					return;
				} else if (lifetime <= 247) {
					LOG.info("WARNING a lifetime should be largely higher than COAP_MAX_EXCHANGE_LIFETIME (~247s)");
				}
			} catch (NumberFormatException e) {
				System.err.println("Invalid value for -l option, lifetime should be an integer : " + e.getMessage());
				return;
			}
		}

		// get push payload
		JsonValue onDemandPushPayload = null;
		if (cl.hasOption("pv")) {
			String stringPayload = cl.getOptionValue("pv");
			// parse to check if payload is valid
			try {
				onDemandPushPayload = Json.parse(stringPayload);
			} catch (com.eclipsesource.json.ParseException e) {
				System.err.println("Invalid JSON for -pv option : " + e.getMessage());
				return;
			}
		}

		// get script entry
		List<ScriptEntry> scriptedPushPayload = null;
		if (cl.hasOption("pf")) {
			String filePath = cl.getOptionValue("pf");
			// parse file to check if format is correct.
			try {
				scriptedPushPayload = ScriptedPushParser.parse(filePath);
			} catch (Exception e) {
				System.err.println("-pf option error : " + e.getMessage());
				return;
			}
		}

		createAndStartClient(endpoint, localAddress, localPort, secureLocalAddress, secureLocalPort, cl.hasOption("b"),
				serverURI, pskIdentity, pskKey, lifetime,  onDemandPushPayload, scriptedPushPayload);
	}

	public static void createAndStartClient(final String endpoint, String localAddress, int localPort,
			String secureLocalAddress, int secureLocalPort, boolean needBootstrap, String serverURI, byte[] pskIdentity,
			byte[] pskKey, long lifetime, final JsonValue onDemandPushPayload, final List<ScriptEntry> scriptedPushPayload) {

		// Initialize model
		List<ObjectModel> models = ObjectLoader.loadDefault();
		List<ObjectModel> jsonModels = ObjectLoader.loadJsonStream(ClassLoader.getSystemResourceAsStream("models/10250.json"));
		models.addAll(jsonModels);

		// Initialize object list
		ObjectsInitializer initializer = new ObjectsInitializer(new LwM2mModel(models));
		if (needBootstrap) {
			if (pskIdentity == null)
				initializer.setInstancesForObject(SECURITY, noSecBootstap(serverURI));
			else
				initializer.setInstancesForObject(SECURITY, pskBootstrap(serverURI, pskIdentity, pskKey));
		} else {
			if (pskIdentity == null) {
				initializer.setInstancesForObject(SECURITY, noSec(serverURI, 123));
				initializer.setInstancesForObject(SERVER, new Server(123, lifetime, BindingMode.U, false));
			} else {
				initializer.setInstancesForObject(SECURITY, psk(serverURI, 123, pskIdentity, pskKey));
				initializer.setInstancesForObject(SERVER, new Server(123, lifetime, BindingMode.U, false));
			}
		}
		initializer.setClassForObject(DEVICE, MyDevice.class);
		// we add the object 10250 for our test
		final Object10250 object10250 = new Object10250();
		for(ObjectModel model : jsonModels) {
			if (model.id == 10250) {
				object10250.setObjectModel(model);				
			}
		}
		initializer.setInstancesForObject(10250, object10250);
		List<LwM2mObjectEnabler> enablers = initializer.create(SECURITY, SERVER, DEVICE, 10250);

		// Create CoAP Config
		NetworkConfig coapConfig;
		File configFile = new File(NetworkConfig.DEFAULT_FILE_NAME);
		if (configFile.isFile()) {
			coapConfig = new NetworkConfig();
			coapConfig.load(configFile);
		} else {
			coapConfig = LeshanClientBuilder.createDefaultNetworkConfig();
			// we want CON for all notifications
			coapConfig.set(NetworkConfig.Keys.NOTIFICATION_CHECK_INTERVAL_COUNT, 1);
			coapConfig.store(configFile);
		}

		// Create client
		LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);
		builder.setLocalAddress(localAddress, localPort);
		builder.setLocalSecureAddress(secureLocalAddress, secureLocalPort);
		builder.setObjects(enablers);
		builder.setCoapConfig(coapConfig);
		// if we don't use bootstrap, client will always use the same unique
		// endpoint so we can disable the other one.
		if (!needBootstrap) {
			if (pskIdentity == null)
				builder.disableSecuredEndpoint();
			else
				builder.disableUnsecuredEndpoint();
		}
		final LeshanClient leshanClient = builder.build();
		final LwM2mCoAPClient client = new LwM2mCoAPClient(leshanClient);

		// Start the client
		client.start();

		// activate scripted push if -pf option is used
		final ScriptedPusher pusher = new ScriptedPusher(scriptedPushPayload) {
			@Override
			protected void sendJson(JsonValue json) {
				push(client, endpoint, json);
			}
		};
		if (scriptedPushPayload != null) {
			// start pusher when registration succeed
			client.addObserver(new LwM2mClientObserverAdapter()	{
				@Override
				public void onRegistrationSuccess(DmServerInfo server, String registrationID) {
					// start pusher on registration
					pusher.start();
				}
			});
		}

		// De-register on shutdown and stop client.
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				// stop script pushed if we start it
				if (scriptedPushPayload != null)
					pusher.stop();

				// we don't send de-registration request before destroy To be able to test re-register.
				client.destroy(false);
			}
		});

		// each 2 seconds force session resumption and send notification for object 10250. 
		Timer timer = new Timer("Device-Current Time");
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				// we want a large payload only for observe response, notifications are small.
				object10250.setLargePayload(false);
				for (Endpoint endpoint : leshanClient.getCoapServer().getEndpoints()) {
					Connector connector = ((CoapEndpoint) endpoint).getConnector();
					if (connector instanceof DTLSConnector) {
						LOG.info("Force session resumption...");
						((DTLSConnector) connector).forceResumeAllSessions();
					}
				}
				object10250.fireResourcesChange(4);
			}
		}, 4000, 2000);

		// activate scanner only if -pv option is used
		if (onDemandPushPayload != null) {
			LOG.info("Press 'p' push value define by -pv option.");
			// Allow to send push through the console
			try (Scanner scanner = new Scanner(System.in)) {
				while (scanner.hasNext()) {
					String nextMove = scanner.next();
					if ("p".equals(nextMove)) {
						push(client, endpoint, onDemandPushPayload);
					}
				}
			}
		}
	}

	private static void push(LwM2mCoAPClient client, String endpoint, JsonValue payload) {
		final Request request = Request.newPost();
		request.getOptions().setUriPath("push");
		request.getOptions().setUriQuery("?ep=" + endpoint);
		request.getOptions().setContentFormat(60); // CBOR
		LOG.info("Trying to send push request...");
		try {
			// build payload.
			byte[] cbor = JsonToCbor.convert(payload);
			request.setPayload(cbor);
			// send request
			client.send(request, 2 * 60 * 1000l, new ResponseCallback<Response>() {

				public void onResponse(Response response) {
					LOG.info("Response recieved : {}", response.toString());
				};
			}, new ErrorCallback() {

				@Override
				public void onError(Exception e) {
					LOG.error("Unable to send request : {} ", e.getMessage());
				}
			});
		} catch (Exception e) {
			LOG.error("Unable to send Request : {} ", e.getMessage());
		}

	}
}
