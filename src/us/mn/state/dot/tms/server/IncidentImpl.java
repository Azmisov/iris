/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2009-2022  Minnesota Department of Transportation
 * Copyright (C) 2018  Iteris Inc.
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

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import us.mn.state.dot.sched.TimeSteward;
import us.mn.state.dot.sonar.Namespace;
import us.mn.state.dot.tms.Camera;
import us.mn.state.dot.tms.ChangeVetoException;
import us.mn.state.dot.tms.Direction;
import us.mn.state.dot.tms.EventType;
import us.mn.state.dot.tms.GeoLocHelper;
import us.mn.state.dot.tms.Incident;
import us.mn.state.dot.tms.IncidentDetail;
import us.mn.state.dot.tms.LaneImpact;
import us.mn.state.dot.tms.LaneCode;
import us.mn.state.dot.tms.R_Node;
import us.mn.state.dot.tms.Road;
import us.mn.state.dot.tms.TMSException;
import us.mn.state.dot.tms.geo.Position;
import static us.mn.state.dot.tms.server.XmlWriter.createAttribute;

/**
 * An incident is an event (crash, stall, etc.) which has an effect on traffic.
 *
 * @author Douglas Lau
 * @author Michael Darter
 */
public class IncidentImpl extends BaseObjectImpl implements Incident {

	/** Validate an impact code */
	static private void validateImpact(String imp)
		throws ChangeVetoException
	{
		String vimp = LaneImpact.fromArray(LaneImpact.fromString(imp));
		if (!vimp.equals(imp))
			throw new ChangeVetoException("Invalid impact: " + imp);
	}

	/** Load all the incidents */
	static protected void loadAll() throws TMSException {
		namespace.registerType(SONAR_TYPE, IncidentImpl.class);
		store.query("SELECT name, replaces, event_desc_id, " +
			"event_date, detail, lane_code, road, dir, lat, " +
			"lon, camera, impact, cleared, confirmed, notes " +
			"FROM event." + SONAR_TYPE +
			" WHERE cleared = 'f';",new ResultFactory()
		{
			public void create(ResultSet row) throws Exception {
				namespace.addObject(new IncidentImpl(namespace,
					row.getString(1),	// name
					row.getString(2),	// replaces
					row.getInt(3),		// event_desc_id
					row.getTimestamp(4),	// event_date
					row.getString(5),	// detail
					row.getString(6),	// lane_code
					row.getString(7),	// road
					row.getShort(8),	// dir
					row.getDouble(9),	// lat
					row.getDouble(10),	// lon
					row.getString(11),	// camera
					row.getString(12),	// impact
					row.getBoolean(13),	// cleared
					row.getBoolean(14),	// confirmed
					row.getString(15)	// notes
				));
			}
		});
	}

	/** Get a mapping of the columns */
	@Override
	public Map<String, Object> getColumns() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("name", name);
		map.put("replaces", replaces);
		map.put("event_desc_id", event_desc_id);
		map.put("event_date", event_date);
		map.put("detail", detail);
		map.put("lane_code", lane_code);
		map.put("road", road);
		map.put("dir", dir);
		map.put("lat", lat);
		map.put("lon", lon);
		map.put("camera", camera);
		map.put("impact", impact);
		map.put("cleared", cleared);
		map.put("confirmed", confirmed);
		map.put("notes", notes);
		return map;
	}

	/** Get the database table name */
	@Override
	public String getTable() {
		return "event." + SONAR_TYPE;
	}

	/** Get the SONAR type name */
	@Override
	public String getTypeName() {
		return SONAR_TYPE;
	}

	/** Create a new incident (by SONAR clients) */
	public IncidentImpl(String n) {
		super(n);
	}

	/** Create an incident */
	protected IncidentImpl(Namespace ns, String n, String rpl, int et,
		Date ed, String dtl, String lc, String r, short d, double lt,
		double ln, String cam, String im, boolean clr, boolean cnf, String no)
	{
		this(n, rpl, et, ed, (IncidentDetail)ns.lookupObject(
		     IncidentDetail.SONAR_TYPE, dtl), lc, lookupRoad(r), d, lt,
		     ln, lookupCamera(cam), im, clr, cnf, no);
	}

	/** Create an incident */
	public IncidentImpl(String n, String rpl, int et, Date ed,
		IncidentDetail dtl, String lc, Road r, short d, double lt,
		double ln, Camera cam, String im, boolean clr, boolean cnf, String no)
	{
		super(n);
		replaces = rpl;
		event_desc_id = et;
		event_date = new Date(ed.getTime());
		detail = dtl;
		lane_code = lc;
		road = r;
		dir = d;
		lat = lt;
		lon = ln;
		camera = cam;
		impact = im;
		cleared = clr;
		confirmed = cnf;
		notes = no;
	}

	/** Destroy an object */
	public void doDestroy() throws TMSException {
		// This happens when the SONAR object is being destroyed.
		// Incidents are stored in the event schema, and should never
		// be DELETEd.  Just set the 'cleared' column to true, and it
		// won't get loaded on the next server restart.
		doSetCleared(true);
	}

	/** Name of replaced incident */
	private String replaces;

	/** Get name of incident this replaces */
	@Override
	public String getReplaces() {
		return replaces;
	}

	/** Event type (id of EventType enum) */
	private int event_desc_id;

	/** Get the event type */
	@Override
	public int getEventType() {
		return event_desc_id;
	}

	/** Event date (timestamp) */
	private Date event_date = TimeSteward.getDateInstance();

	/** Get the event date (timestamp) */
	@Override
	public long getEventDate() {
		return event_date.getTime();
	}

	/** Incident detail */
	private IncidentDetail detail;

	/** Get the incident detail */
	@Override
	public IncidentDetail getDetail() {
		return detail;
	}

	/** Lane code */
	private String lane_code = LaneCode.MAINLINE.lcode;

	/** Get the lane code */
	@Override
	public String getLaneCode() {
		return lane_code;
	}

	/** Road for incident location */
	private Road road;

	/** Get the road */
	@Override
	public Road getRoad() {
		return road;
	}

	/** Direction on road */
	private short dir;

	/** Get the road direction */
	@Override
	public short getDir() {
		return dir;
	}

	/** Latitude */
	private double lat;

	/** Get the latitude */
	@Override
	public double getLat() {
		return lat;
	}

	/** Longitude */
	private double lon;

	/** Get the longitude */
	@Override
	public double getLon() {
		return lon;
	}

	/** Camera for verificaiton */
	private Camera camera;

	/** Get the verification camera */
	@Override
	public Camera getCamera() {
		return camera;
	}

	/** Impact code */
	private String impact = "";

	/** Get the current impact code.
	 * @see us.mn.state.dot.tms.Incident#getImpact */
	@Override
	public String getImpact() {
		return impact;
	}

	/** Set the impact code */
	@Override
	public void setImpact(String imp) {
		impact = imp;
	}

	/** Set the impact code */
	public void doSetImpact(String imp) throws TMSException {
		if (!imp.equals(impact)) {
			validateImpact(imp);
			setConfirmedNotify(true);
			store.update(this, "impact", imp);
			setImpact(imp);
		}
	}

	/** Incident cleared status */
	private boolean cleared = false;

	/** Get the cleared status */
	@Override
	public boolean getCleared() {
		return cleared;
	}

	/** Set the cleared status */
	@Override
	public void setCleared(boolean c) {
		cleared = c;
		clear_time = TimeSteward.currentTimeMillis();
	}

	/** Set the cleared status */
	public void doSetCleared(boolean c) throws TMSException {
		if (c != cleared) {
			store.update(this, "cleared", c);
			setCleared(c);
		}
	}

	/** Set the cleared status (notify clients) */
	public void setClearedNotify(boolean c) {
		try {
			if (c != cleared) {
				doSetCleared(c);
				notifyAttribute("cleared");
			}
		}
		catch (TMSException e) {
			e.printStackTrace();
		}
	}

	/** Time the incident was cleared */
	private long clear_time = TimeSteward.currentTimeMillis();

	/** Get the time the incident was cleared */
	public long getClearTime() {
		return clear_time;
	}

	/** Incident confirmed status */
	private boolean confirmed = true;

	/** Get the confirmed status */
	@Override
	public boolean getConfirmed() {
		return confirmed;
	}

	/** Set the confirmed status */
	private void setConfirmedNotify(boolean c) throws TMSException {
		if (c != confirmed) {
			store.update(this, "confirmed", c);
			confirmed = c;
			notifyAttribute("confirmed");
		}
	}

	/** Write the incident as xml */
	public void writeXml(Writer w) throws IOException {
		String dtl = lookupDetail();
		String loc = lookupLocation();
		w.write("<incident");
		w.write(createAttribute("name", getName()));
		if (replaces != null)
			w.write(createAttribute("replaces", replaces));
		w.write(createAttribute("event_type",
			EventType.fromId(event_desc_id)));
		w.write(createAttribute("event_date", event_date));
		if (dtl != null)
			w.write(createAttribute("detail", dtl));
		w.write(createAttribute("lane_code",
			LaneCode.fromCode(lane_code)));
		w.write(createAttribute("road", road));
		w.write(createAttribute("dir",
			Direction.fromOrdinal(dir).abbrev));
		if (loc != null && loc.length() > 0)
			w.write(createAttribute("location", loc));
		Position pos = getWgs84Position();
		w.write(createAttribute("lon",
			formatDouble(pos.getLongitude())));
		w.write(createAttribute("lat",
			formatDouble(pos.getLatitude())));
		w.write(createAttribute("camera", camera));
		w.write(createAttribute("impact", impact));
		w.write(createAttribute("cleared", cleared));
		w.write(createAttribute("confirmed", confirmed));
		w.write(createAttribute("notes", notes));
		w.write("/>\n");
	}

	/** Lookup the detail description */
	private String lookupDetail() {
		IncidentDetail dtl = detail;
		return (dtl != null) ? dtl.getDescription() : null;
	}

	/** Lookup the incident location */
	private String lookupLocation() {
		Corridor cor = corridors.getCorridor(
			GeoLocHelper.getCorridorName(road, dir));
		if (cor == null)
			return null;
		R_Node rnd = cor.findNearest(getWgs84Position());
		if (rnd == null)
			return null;
		return GeoLocHelper.getCrossLandmark(rnd.getGeoLoc());
	}

	/** Get Position in WGS84 */
	private Position getWgs84Position() {
		return new Position(lat, lon);
	}

	/** Incident notes, which are never null */
	private String notes = "";

	/** Get notes */
	@Override
	public String getNotes() {
		return notes;
	}

	/** Set notes */
	@Override
	public void setNotes(String n) {
		notes = n;
	}

	/** Set notes */
	public void doSetNotes(String n) throws TMSException {
		if (!n.equals(notes)) {
			store.update(this, "notes", n);
			setNotes(n);
		}
	}

	/** Set notes and notify clients */
	public void setNotesNotify(String n) {
		try {
			if (!n.equals(notes)) {
				doSetNotes(n);
				notifyAttribute("notes");
			}
		}
		catch (TMSException e) {
			e.printStackTrace();
		}
	}
}
