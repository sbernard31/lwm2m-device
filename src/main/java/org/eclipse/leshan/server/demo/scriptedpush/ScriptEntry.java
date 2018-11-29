package org.eclipse.leshan.server.demo.scriptedpush;

import com.eclipsesource.json.JsonValue;

public class ScriptEntry {
	
	public final long time;
	public final JsonValue json;
	
	public ScriptEntry(long time, JsonValue json) {
		this.time = time;
		this.json =json;
	}
}
