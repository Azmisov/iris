/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2016-2018  Minnesota Department of Transportation
 * Copyright (C) 2018-2021  Iteris Inc.
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
package us.mn.state.dot.tms.server.comm.incfeed;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import us.mn.state.dot.tms.LaneImpact;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import us.mn.state.dot.tms.server.ControllerImpl;
import us.mn.state.dot.tms.server.CommLinkImpl;
import us.mn.state.dot.tms.server.comm.ControllerProperty;
import us.mn.state.dot.tms.utils.LineReader;

/**
 * Incident feed property.
 *
 * @author Douglas Lau
 * @author Kevin Wood, Michael Darter, Isaac Nygaard
 */
public class IncFeedProperty extends ControllerProperty {

	/** Maximum number of chars in response for line reader */
	static private final int MAX_RESP = 1024;

	/** Incident cache */
	private final IncidentCache cache;

	/** Create a new incident feed property */
	public IncFeedProperty(IncidentCache ic) {
		cache = ic;
	}

	/** Determine if the feed is JSON or something else */
	static private boolean isJson(ControllerImpl ci) {
		CommLinkImpl cli = (CommLinkImpl)ci.getCommLink();
		if (cli != null) {
			String uri = cli.getUri();
			if (uri != null)
				return uri.toLowerCase().contains("json");
		}
		return false;
	}

	/** Decode a QUERY response */
	@Override
	public void decodeQuery(ControllerImpl c, InputStream is)
		throws IOException
	{
		if (!isJson(c))
			decodeCsvQuery(c, is);
		else
			decodeJsonQuery(c, is);
	}
	
	/** Decode a QUERY response for a CSV feed */
	private void decodeCsvQuery(ControllerImpl c, InputStream is)
		throws IOException
	{
		log("reading CSV feed");
		LineReader lr = new LineReader(is, MAX_RESP);
		String line = lr.readLine();
		while (line != null) {
			cache.put(new ParsedIncident(line, LaneImpact.FREE_FLOWING));
			line = lr.readLine();
		}
		cache.clearOld();
	}

	/** Decode a JSON QUERY response for a JSON feed */
	private void decodeJsonQuery(ControllerImpl c, InputStream is)
		throws IOException
	{
		log("reading JSON feed");
		JSONObject json_objs = null;
		try {
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) != -1)
				result.write(buffer, 0, length);
			json_objs = new JSONObject(result.toString("UTF-8"));
		} catch (Exception ex) {
			log("decodeJsonQuery: ex=" + ex);
			throw new IOException(ex);
		}
		log("loading json object");
		JSONArray incidents;
		try{
			incidents = json_objs.getJSONArray("incidents");
		} catch (JSONException ex){
			// no incidents key, or not an array
			log("decodeJsonQuery: unexpected JSON structure");
			return;
		}
		incidents.forEach(elem -> {
			cache.put(parse((JSONObject) elem));
		});
		cache.clearOld();
	}

	/** Log a message */
	static private void log(String msg) {
		IncFeedPoller.INC_LOG.log(msg);
	}

	/** Get a string representation */
	@Override
	public String toString() {
		return "inc.feed";
	}

	///// JSON PARSING /////

	/** Parse the specified JSON object into an incident string, for which
	 * further parsing can be performed by ParsedIncident class. */
	private ParsedIncident parse(JSONObject elem) {
		String iid = safeGet(elem, "incidentId");
		String ity = safeGet(elem, "irisType");
		// String det = safeGet(elem, "problemOtherText");
		String lat = safeGet(elem, "latitude");
		String lon = safeGet(elem, "longitude");
		String cam = ""; // triggers cam lookup
		String imp = safeGet(elem, "impact");
		String not = safeGet(elem, "problemOtherText");
		StringBuilder sb = new StringBuilder();
		sb.append(iid).append(",");
		sb.append(parseIrisType(ity)).append(",");
		sb.append(","); // det currently ignored
		sb.append(lat).append(",");
		sb.append(lon).append(",");
		sb.append(cam).append(",");
		ParsedIncident inc =
			new ParsedIncident(sb.toString(), convertImpact(imp));
		inc.notes = not;
		log("Parsed incident = " + inc);
		return inc;
	}

	/** Safe get
 	 * @return Specified value or empty string, never null */
	static private String safeGet(JSONObject elem, String key) {
		if (elem == null)
			return "";
		if (key == null || key.isEmpty())
			return "";
		Object val = elem.get(key);
		return (val != null ? val.toString() : "");
	}

	/** Parse incident type */
	static private String parseIrisType(String ity) {
		return (ity != null ? ity.toUpperCase() : "");
	}

	/** Convert to a valid impact code */
	static private LaneImpact convertImpact(String im) {
		log("impact_code=" + im);
		if (im == null) 
			return LaneImpact.FREE_FLOWING;
		else if (im.equals("C"))
			return LaneImpact.FREE_FLOWING;
		else if (im.equals("L"))
			return LaneImpact.FREE_FLOWING;
		else if (im.equals("M"))
			return LaneImpact.AFFECTED;
		else if (im.equals("H"))
			return LaneImpact.BLOCKED;
		else {
			log("unknown impact code=" + im);
			return LaneImpact.FREE_FLOWING;
		}
	}
}
