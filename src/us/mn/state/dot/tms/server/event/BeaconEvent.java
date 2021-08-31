/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2014-2021  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.event;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import us.mn.state.dot.tms.EventType;
import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.TMSException;

/**
 * This is a class for logging beacon events to a database.
 *
 * @author Douglas Lau
 */
public class BeaconEvent extends BaseEvent {

	/** Database table name */
	static private final String TABLE = "event.beacon_event";

	/** Get beacon event purge threshold (days) */
	static private int getPurgeDays() {
		return SystemAttrEnum.BEACON_EVENT_PURGE_DAYS.getInt();
	}

	/** Purge old records */
	static public void purgeRecords() throws TMSException {
		int age = getPurgeDays();
		if (store != null && age > 0) {
			store.update("DELETE FROM " + TABLE +
				" WHERE event_date < now() - '" + age +
				" days'::interval;");
		}
	}

	/** Is the specified event a beacon event? */
	static private boolean isBeaconEvent(EventType et) {
		return EventType.BEACON_ON_EVENT == et
		    || EventType.BEACON_OFF_EVENT == et;
	}

	/** Beacon ID */
	private final String beacon;

	/** Create a new beacon event */
	public BeaconEvent(EventType et, String bid) {
		super(et);
		assert isBeaconEvent(et);
		beacon = bid;
	}

	/** Get the database table name */
	@Override
	public String getTable() {
		return TABLE;
	}

	/** Get a mapping of the columns */
	@Override
	public Map<String, Object> getColumns() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("event_date", new Timestamp(event_date.getTime()));
		map.put("event_desc_id", event_type.id);
		map.put("beacon", beacon);
		return map;
	}
}
