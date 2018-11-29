# lwm2m-device
A LWM2M command line client based on Leshan, mainly used to simulate SierraWireless Devices.

## To compile
```
$> mvn clean install
```

## Run it
Compile it first (jar is in target/ directory) or get jar available in [Release](https://github.com/sbernard31/lwm2m-device/releases).
 
```
$>  java -jar lwm2m-device-0.0.x.jar
```

## To get help 
```
$> java -jar lwm2m-device-0.0.x.jar --help

usage: java -jar leshan-client-demo.jar [OPTION]
 -h,--help    Display help information.
 -n <arg>     Set the endpoint name of the Client.
              Default: the local hostname or 'LeshanClientDemo' if any.
 -b           If present use bootstrap.
 -lh <arg>    Set the local CoAP address of the Client.
              Default: any local address.
 -lp <arg>    Set the local CoAP port of the Client.
              Default: A valid port value is between 0 and 65535.
 -slh <arg>   Set the secure local CoAP address of the Client.
              Default: any local address.
 -slp <arg>   Set the secure local CoAP port of the Client.
              Default: A valid port value is between 0 and 65535.
 -u <arg>     Set the LWM2M or Bootstrap server URL.
              Default: localhost:5683.
 -i <arg>     Set the LWM2M or Bootstrap server PSK identity in ascii.
              Use none secure mode if not set.
 -p <arg>     Set the LWM2M or Bootstrap server Pre-Shared-Key in hexa.
              Use none secure mode if not set.
 -l <arg>     Set the LWM2M lifetime in seconds. It should be higher than
              COAP_MAX_EXCHANGE_LIFETIME (~247s).
              Default: 500s
 -pv <arg>    This demo allow you to send data to
              coap(s)://server.uri:port/push?ep=deviceendpoint using CBOR.
              This option define the payload to send in JSON.
 -pf <arg>    This demo allow you to send data to
              coap(s)://server.uri:port/push?ep=deviceendpoint using CBOR.
              The value of this option is the path to a file which
              contains data to send and when to send it. First line
              contains time to wait in ms before to send data, second line
              contains data to json payload and so on. (Limitation your
              JSON payload should stay in one line)
```

## Airvantage CoAP push (NOT A LWM2M FEATURE)
#### Push manually

```
java -jar lwm2m-device-0.0.x.jar -n endpointName -i pskID -p aefdb0 -u eu.airvantage.net:5686  -pv "{\"Asset\":{\"Sensors\":{\"Light\":{\"Level\":500},\"Pressure\":{\"Temperature\":40}}}}"
```

once client is launched type : p+enter to push the payload.

#### Push automatically using a kind of "script file".

```

java -jar lwm2m-device-0.0.x.jar -n endpointName -i pskID -p aefdb0 -u eu.airvantage.net:5686  -pf pushpayload.txt

```

(see [pushpayload.txt](https://github.com/sbernard31/lwm2m-device/blob/master/pushpayload_example.txt)) 
