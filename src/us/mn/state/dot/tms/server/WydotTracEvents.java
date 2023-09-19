/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2014  Iteris Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package us.mn.state.dot.tms.server;

import java.util.concurrent.ConcurrentLinkedQueue;
import us.mn.state.dot.sched.DebugLog;
import us.mn.state.dot.sonar.server.Server;
import us.mn.state.dot.sonar.server.ServerNamespace;

/**
 * Container for WYDOT TRAC events which also provides
 * methods for sending events to WYDOT TRAC.
 * @author Michael Darter
 */
public class WydotTracEvents extends Thread {

	/** Factory
	 * @param s Sonar server
	 * @param ns Server namespace
	 * @returns New container or null on error */
	static public WydotTracEvents create(Server s, ServerNamespace ns) {
		if(s == null || ns == null)
			return null;
		return new WydotTracEvents(s, ns);
	}

	/** Sleep */
	static private void sleepy(int ms) {
		try {
			Thread.sleep(ms);
		} catch(Exception e) {}
	}

	/** Log */
	static private void log(String s) {
		logRoot("WydotTracEvents: " + s);
	}

	/** Log root */
	static protected void logRoot(String s) {
		WydotTracEvents.TRAC_LOG.log(s);
	}

	/** Logger */
	static protected final DebugLog TRAC_LOG = 
		new DebugLog("wydot_trac");

	/** Sonar server */
	private final Server sonar_server;

	/** Sonar namespace */
	private final ServerNamespace server_namespace;

	/** List of events */
	private final ConcurrentLinkedQueue<WydotTracEvent> events = 
		new ConcurrentLinkedQueue<WydotTracEvent>();

	/** Constructor
	 * @param s Sonar server, never null
	 * @param ns Server namespace, never null */
	private WydotTracEvents(Server s, ServerNamespace ns) {
		sonar_server = s;
		server_namespace = ns;
		log("called");
		start();
	}

	/** Run the thread */
	public void run() {
		log("thread started");
		while(true) {
			sendEvents();
			sleepy(5 * 1000);
		}
	}

	/** Add an event to the end of the TRAC event queue.
	 * @param te Event that may be null
	 * @return True on success else false */
	public boolean add(WydotTracEvent te) {
		log("added new event=" + te);
		return (te == null ? true : events.add(te));
	}

	/** Send all queued events to TRAC */
	private void sendEvents() {
		log("size=" + events.size());
		while(true) {
			if(events.isEmpty())
				return;
			WydotTracEvent te = events.peek();
			if(te == null)
				return;
			if(te.sendEvent()) {
				log("sent event=" + te);
				if(!events.remove(te))
					log("failed to remove from queue");
				if(events.isEmpty())
					return;
			} else {
				log("failed to send event to Trac=" + te);
			}
			sleepy(1 * 1000);
		}
	}
}
