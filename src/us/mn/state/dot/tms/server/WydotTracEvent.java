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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import us.mn.state.dot.tms.SystemAttrEnum;

/**
 * WYDOT TRAC event
 * @author Michael Darter
 */
public class WydotTracEvent {

	/** Factory
	 * @return True on success else false */
	static public WydotTracEvent create(String desc) {
		WydotTracEvent te = new WydotTracEvent(desc);
		return te;
	}

	/** Event Priority */
	static private enum Priority {
		Emergency(1), High(2), Medium(3), Low(4);
		public final int value;
		/** Constructor */
		private Priority(int v) {
			value = v;
		}
	}

	/** Event District */
	static private enum District {
		All(0), One(1), Two(2), Three(3), Four(4), Five(5);
		public final int value;
		/** Constructor */
		private District(int v) {
			value = v;
		}
	}

	/** URL encode a string */
	static private String encode(String s) {
		try {
			return URLEncoder.encode(s, "utf-8");
		} catch(Exception ex) {
			log("ex=" + ex);
			return s;
		}
	}

	/** Log */
	static private void log(String s) {
		WydotTracEvents.logRoot("WydotTracEvent: " + s);
	}

	/** Get the host name system attribute.
	 * @return Hostname and port in the form: hostname:port */
	static private String getHostName() {
		return SystemAttrEnum.WYDOT_TRAC_HOST.getString();
	}

	/** Event description */
	public final String description;

	/** Event created by */
	public final String created_by = "IRIS.AWS";

	/** Event priority */
	public final Priority priority = Priority.Low;

	/** District */
	//FIXME: geo-lookup
	public final District district = District.All;

	/** Maintenance district */
	public final District maint_district = District.All;

	/** Event URL */
	public final String event_url = "";

	/** Constructor */
	private WydotTracEvent(String de) {
		description = de;
	}

	/** Convert the event to a TRAC request URL */
	private String toTracUrl() {
		StringBuilder sb = new StringBuilder();
		String host = getHostName();
		sb.append("http://").append(host);
		sb.append("/trac/newtask.text");
		sb.append("?priority=").append(priority.value);
		sb.append("&district=").append(district.value);
		//FIXME: add maintenance district
		sb.append("&description=").append(encode(description));
		sb.append("&createdBy=").append(encode(created_by));
		sb.append("&url=").append(event_url);
		log("url=" + sb);
		return sb.toString();
	}

	/** Send event to TRAC
	 * @return True on success else false */
	public boolean sendEvent() {
		String url = toTracUrl();
		log("sending event=" + toString() + 
			" to url=" + url);
		String res = httpGet(url);
		log("res=" + res);
		return (res == null ? false : res.startsWith("OK"));
	}

	/** Get HTTP request
	 * @param u URL to read 
	 * @return Text read from URL or null on error */
	private String httpGet(String u) {
		if(u == null || u.isEmpty())
			return null;
		URL url;
		StringBuffer res = new StringBuffer();
		try {
			url = new URL(u);
			HttpURLConnection con = 
				(HttpURLConnection)url.openConnection();
			con.setRequestMethod("GET");
			BufferedReader rd = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null) {
				res.append(line);
			}
			rd.close();
			return res.toString();
		} catch(FileNotFoundException ex) {
			log("ex=" + ex);
			return null;
		} catch(IOException ex) {
			log("ex=" + ex);
			return null;
		}
	}

	/** To string */
	public String toString() {
		StringBuilder sb = new StringBuilder("(WydotTracEvent:");
		sb.append(" desc=").append(description);
		sb.append(" created_by=").append(created_by);
		sb.append(" priority=").append(priority.value);
		sb.append(" district=").append(district.value);
		sb.append(" maint_district=").append(maint_district.value);
		sb.append(" event_url=").append(event_url);
		sb.append(")");
		return sb.toString();
	}
}
