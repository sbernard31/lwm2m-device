package org.eclipse.leshan.server.demo.codec;

import java.io.ByteArrayOutputStream;
import java.util.List;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.builder.ArrayBuilder;
import co.nstant.in.cbor.builder.MapBuilder;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.SimpleValue;
import co.nstant.in.cbor.model.UnicodeString;

/**
 * Convert JSON string to CBOR
 */
public class JsonToCbor {
	
	public static byte[] convert(String jsonString) throws CborException {
		// parse json string
		JsonValue json = Json.parse(jsonString);
		
		return convert(json);
	}
	
	public static byte[] convert(JsonValue json) throws CborException {
		// convert from minimal json structure to cbor-java structure
		DataItem cbor = json2Cbor(json);
		
		// encode cbor-java structure to cbor byte[]
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		new CborEncoder(baos).encode(cbor);
		return baos.toByteArray();
	}
		
	private static DataItem json2Cbor(JsonValue jsonValue) {
		// Use CborBuilder to create a DataItem from JsonValue
		CborBuilder cborBuilder = new CborBuilder();		
		if (jsonValue.isArray()) {
			ArrayBuilder<?> array = cborBuilder.addArray();
			for (JsonValue e : jsonValue.asArray()) {
				array.add(json2Cbor(e));
			}
			array.end();
		}else if (jsonValue.isObject()) {
			MapBuilder<?> map = cborBuilder.addMap();
			for (Member e : jsonValue.asObject()) {
				map.put(new UnicodeString(e.getName()), json2Cbor(e.getValue()));
			}
			map.end();
		}else if (jsonValue.isBoolean()) {
			cborBuilder.add(jsonValue.asBoolean());
		}else if (jsonValue.isString()) {
			cborBuilder.add(jsonValue.asString());
		}else if (jsonValue.isNumber()) {
			    try {
			    	cborBuilder.add(jsonValue.asLong());
			    } catch (NumberFormatException e) {
			    	cborBuilder.add(jsonValue.asDouble());
			    }
		}else if (jsonValue.isNull()) {
			cborBuilder.add(SimpleValue.NULL);
		}
		
		// One JsonValue will give only one DataItem
		List<DataItem> dataItems = cborBuilder.build();
		if (dataItems == null || dataItems.isEmpty())
			return null;
		else {
			return dataItems.get(0);
		}
	}	
}
