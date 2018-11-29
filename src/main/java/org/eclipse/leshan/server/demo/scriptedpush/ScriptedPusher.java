package org.eclipse.leshan.server.demo.scriptedpush;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.leshan.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonValue;

public abstract class ScriptedPusher {

	private static final Logger LOG = LoggerFactory.getLogger(ScriptedPusher.class);

	private final ScheduledExecutorService executor = Executors
			.newSingleThreadScheduledExecutor(new NamedThreadFactory("Script Pusher"));;

	private final List<ScriptEntry> scriptedPushPayload;
	private final AtomicBoolean started = new AtomicBoolean(false);

	public ScriptedPusher(List<ScriptEntry> scriptedPushPayload) {
		this.scriptedPushPayload = scriptedPushPayload;
	}

	public void start() {
		if (scriptedPushPayload.isEmpty())
			return;

		if (started.getAndSet(true))
			return;

		// get first script to schedule first push
		int index = 0;
		ScriptEntry scriptEntry = scriptedPushPayload.get(index);
		LOG.info("Schedule next push in {}ms", scriptEntry.time);
		executor.schedule(createTask(scriptEntry.json, index), scriptEntry.time, TimeUnit.MILLISECONDS);
	}

	public void stop() {
		if (!started.getAndSet(false))
			executor.shutdownNow();
	}

	private Runnable createTask(final JsonValue jsonToSend, final int jsonIndex) {
		return new Runnable() {

			@Override
			public void run() {
				// push data
				sendJson(jsonToSend);
		
				if (!started.get())
					return;
				
				// schedule next task
				int nextIndex = jsonIndex + 1;
				if (nextIndex < scriptedPushPayload.size()) {
						ScriptEntry scriptEntry = scriptedPushPayload.get(nextIndex);
						LOG.info("Schedule next push in {}ms", scriptEntry.time);
						executor.schedule(createTask(scriptEntry.json, nextIndex), scriptEntry.time,
								TimeUnit.MILLISECONDS);
				} else {
					LOG.info("No more data to push.");
				}
			}
		};
	}

	protected abstract void sendJson(JsonValue json);
}
