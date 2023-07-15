/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2016-2022  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.incfeed;

import java.util.Date;
import java.util.HashSet;
import us.mn.state.dot.sched.DebugLog;
import us.mn.state.dot.sonar.SonarException;
import us.mn.state.dot.tms.server.Corridor;
import us.mn.state.dot.tms.GeoLoc;
import us.mn.state.dot.tms.Incident;
import us.mn.state.dot.tms.IncidentHelper;
import us.mn.state.dot.tms.LaneImpact;
import us.mn.state.dot.tms.LaneCode;
import us.mn.state.dot.tms.geo.Position;
import us.mn.state.dot.tms.geo.SphericalMercatorPosition;
import static us.mn.state.dot.tms.server.BaseObjectImpl.corridors;
import us.mn.state.dot.tms.server.IncidentImpl;
import us.mn.state.dot.tms.units.Distance;
import static us.mn.state.dot.tms.units.Distance.Units.MILES;
import us.mn.state.dot.tms.Road;
import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.server.RoadImpl;
import us.mn.state.dot.tms.Direction;

/**
 * Cache of incidents in an incident feed.
 *
 * @author Douglas Lau
 * @author Michael Darter, Isaac Nygaard
 */
public class IncidentCache {

	/** Maximum distance to snap */
	static private final Distance MAX_DIST = new Distance(0.5, MILES);

	/** Threshold to check if an incident has moved (meters) */
	static private final double MOVE_THRESHOLD_M = 50.0;

	/** Check if an incident has moved */
	static private boolean hasMoved(IncidentImpl inc, ParsedIncident pi) {
		Position p0 = new Position(inc.getLat(), inc.getLon());
		Position p1 = new Position(pi.lat, pi.lon);
		return p0.distanceHaversine(p1) > MOVE_THRESHOLD_M;
	}

	/** Comm link name */
	private final String link;

	/** Incident feed debug log */
	private final DebugLog inc_log;

	/** Set of next incidents */
	private final HashSet<String> nxt = new HashSet<String>();

	/** Set of active incidents */
	private final HashSet<String> incidents = new HashSet<String>();

	/** Flag to incidate cache has been updated */
	private boolean updated = false;

	/** Create a new incident cache */
	public IncidentCache(String cl, DebugLog il) {
		link = cl;
		inc_log = il;
	}

	/** Video still functionality enabled? */
	static private boolean freeIncidentsEnabled() {
		return SystemAttrEnum.INCIDENT_FREE_ENABLE.getBoolean();
	}

	/** Put an incident into the cache */
	public void put(ParsedIncident pi) {
		if (pi.isValid()) {
			nxt.add(pi.id);
			if (updated) {
				if (pi.hasLocation())
					updateIncident(pi);
				else
					inc_log.log("No location: " + pi);
			}
		} else
			inc_log.log("Invalid incident: " + pi);
	}

	/** Lookup an incident */
	private IncidentImpl lookupIncident(String id) {
		Incident inc = IncidentHelper.lookupOriginal(originalId(id));
		return (inc instanceof IncidentImpl)
		      ? (IncidentImpl) inc
		      : null;
	}

	/** Get original incident ID */
	private String originalId(String id) {
		return link + "_" + id;
	}

	/** Update an incident */
	private void updateIncident(ParsedIncident pi) {
		Position pos = new Position(pi.lat, pi.lon);
		SphericalMercatorPosition smp =
			SphericalMercatorPosition.convert(pos);
		GeoLoc loc = corridors.snapGeoLoc(smp, LaneCode.MAINLINE,
			MAX_DIST, pi.dir);
		if (loc != null)
			updateIncident(pi, loc);
		else if (freeIncidentsEnabled())
			updateIncident(pi, null, -1);
		else
			inc_log.log("Failed to snap incident: " + pi);
	}

	/** Update an incident */
	private void updateIncident(ParsedIncident pi, GeoLoc loc) {
		int n_lanes = getLaneCount(LaneCode.MAINLINE, loc);
		if (n_lanes > 0)
			updateIncident(pi, loc, n_lanes);
		else
			inc_log.log("No lanes at location: " + loc);
	}

	/** Get the lane count at the incident location */
	private int getLaneCount(LaneCode lc, GeoLoc loc) {
		Corridor cb = corridors.getCorridor(loc);
		return (cb != null) ? cb.getLaneCount(lc, loc) : 0;
	}

	/** Update an incident
	 * @param loc set this to null for an incident free of infastructure
	 */
	private void updateIncident(ParsedIncident pi, GeoLoc loc, int n_lanes){
		IncidentImpl inc = lookupIncident(pi.id);
		String oid = originalId(pi.id);
		String nid = null;
		// Is this a new incident?
		if (null == inc && !incidents.contains(pi.id)) {
			inc_log.log("Creating incident: " + pi);
			nid = oid;
			oid = null;
		}
		// Is this a continuing incident?
		else if (isContinuing(inc, pi) &&
			(hasMoved(inc, pi) || pi.isDetailChanged(inc)))
		{
			inc_log.log("Updating incident: " + pi);
			inc.setClearedNotify(true);
			inc.notifyRemove();
			nid = IncidentHelper.createUniqueName();
		}
		// Notification needed
		if (nid != null){
			Road road; short dir; String im;
			// free incident
			if (loc == null){
				road = RoadImpl.createNotify("Unknown");
				dir = (short) Direction.UNKNOWN.ordinal();
				im = LaneImpact.FREE_FLOWING.toString();
			}
			else{
				road = loc.getRoadway();
				dir = loc.getRoadDir();
				im = LaneImpact.fromLanes(n_lanes);
			}
			createIncidentNotify(nid, oid, pi, road, dir, im);
		}		
	}

	/** Check if an incident in continuing */
	private boolean isContinuing(IncidentImpl inc, ParsedIncident pi) {
		return inc != null
		    && incidents.contains(pi.id)
		    && (!inc.getConfirmed())
		    && (!inc.getCleared());
	}

	/** Create an incident and notify clients.
	 * @param n Incident name.
	 * @param orig Original name.
	 * @param pi Parsed incident.
	 * @param road Snapped road from geolocation
	 * @param dir Roadway direction
	 * @param im String indicating the lane impact */
	private boolean createIncidentNotify(String n, String orig,
		ParsedIncident pi, Road road, short dir, String im)
	{
		IncidentImpl inc = new IncidentImpl(n, orig,
			pi.inc_type.id, new Date(), pi.detail,
			LaneCode.MAINLINE.lcode, road, dir,
			pi.lat, pi.lon, pi.lookupCamera(), im, false, false);
		try {
			inc.notifyCreate();
			return true;
		}
		catch (SonarException e) {
			// Probably a cleared incident with the same name
			inc_log.log("Incident not created: " + e.getMessage());
			System.err.println("createIncidentNotify @ " +
				new Date() + ": " + e.getMessage());
			return false;
		}
	}

	/** Clear old incidents.  Any incidents which have not been refreshed
	 * since this was last called will be cleared. */
	public void clearOld() {
		for (String id : incidents) {
			if (!nxt.contains(id))
				setCleared(id);
		}
		incidents.clear();
		incidents.addAll(nxt);
		nxt.clear();
		updated = true;
	}

	/** Set an incident to cleared status */
	private void setCleared(String id) {
		IncidentImpl inc = lookupIncident(id);
		// Don't automatically clear confirmed incidents
		if (inc != null && !inc.getConfirmed() && !inc.getCleared()) {
			inc.setClearedNotify(true);
			inc_log.log("Incident cleared: " + id);
		}
	}
}
