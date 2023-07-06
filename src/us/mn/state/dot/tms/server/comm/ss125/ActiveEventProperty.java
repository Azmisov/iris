/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2021  Minnesota Department of Transportation
 * Copyright (C) 2017  Iteris Inc.
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
package us.mn.state.dot.tms.server.comm.ss125;

import java.io.IOException;
import java.util.Date;
import us.mn.state.dot.tms.CommProtocol;
import us.mn.state.dot.tms.server.ControllerImpl;
import us.mn.state.dot.tms.server.DetectorImpl;
import us.mn.state.dot.tms.server.comm.ParsingException;

/**
 * Active Event Data Property.
 *
 * @author Douglas Lau
 * @author Michael Darter
 */
public class ActiveEventProperty extends SS125Property {

	/** Valid age of vehicle events, [max in past, max in future] */
	static private final long VALID_AGE_MS[] = {
		1000 * 60 * 60 * 4, // 4 hours
		1000 * 60 * 5 // 5 mins
	};

	/** Valid event */
	private boolean valid;

	/** Event timestamp from controller's clock */
	private long stamp;

	/** Lane ID given by pin number */
	private int lane_id;

	/** Range (ft or m) */
	private float range;

	/** Duration (ms) */
	private int duration;

	/** Speed (mph or kph), or -1 for invalid */
	private float speed;

	/** Vehicle classification */
	private int v_class;

	/** Vehicle length (ft or m) */
	private float length;

	public ActiveEventProperty(String cn){
		super(cn);
	}

	/** Get event timestamp */
	public long getTime() {
		return stamp;
	}

	/** Is time stamp within valid range? */
	public boolean isValidStamp(long now) {
		return (stamp > now - VALID_AGE_MS[0]) &&
				(stamp < now + VALID_AGE_MS[1]);
	}

	/** Message ID for interval data request */
	@Override
	protected MessageID msgId() {
		return MessageID.ACTIVE_EVENT;
	}

	/** Format a QUERY request */
	@Override
	protected byte[] formatQuery() throws IOException {
		byte[] body = new byte[4];
		formatBody(body, MessageType.READ);
		return body;
	}

	/** Validate the message sub id in the message response body.
	 * If the sub id is valid assign value to field.
	 * @param rbody Response body to parse.
	 * @return True if msg sub id is valid else false */
	@Override
	protected boolean parseMsgSubId(byte[] rbody) {
		msg_sub_id = parse8(rbody, OFF_MSG_SUB_ID);
		var ok = validMsgSubId();
		log("EventsProperty.validMsgSubId: sid=" + 
			msg_sub_id + " valid=" + ok);
		if (!ok) {
			logError("Bad message sub id: msg_sub_id from " + 
				"rsp body=" + msg_sub_id);
		}
		return ok;
	}
	/** Whether message sub id is a valid value */
	private boolean validMsgSubId(){
		return msg_sub_id == 0 || msg_sub_id == 1;
	}

	/** Parse a QUERY response */
	@Override
	protected void parseQuery(byte[] body) throws IOException {
		// no more events?
		if (body.length == 4)
			return;
		// includes response code
		if (body.length == 6){
			parseResult(body);
			return;
		}
		if (body.length != 24)
			throw new ParsingException("BODY LENGTH");
		if (!validMsgSubId())
			throw new ParsingException("BAD MSG SUB ID");
		valid = msg_sub_id == 1;
		if (valid){
			log("read event containing vehicle data.");
			stamp = parseDate(body, 3);
			lane_id = parse8(body, 11);
			if (lane_id < 0 || lane_id > 9)
				throw new ParsingException("BAD LANE");
			range = parse16Fixed(body, 12);
			duration = parse24(body, 14);
			Float speed_raw = parse24Fixed(body, 17);
			speed = speed_raw == null || speed_raw < 0 ? -1 : speed_raw;
			v_class = parse8(body, 20);
			if (v_class < 0 || v_class > 3)
				throw new ParsingException("BAD VEHICLE CLASS");
			length = parse16Fixed(body, 21);
			log("created event=" + this);
		}
		else log("read blank event, no more events");
	}

	/** Check if this is a valid event (e.g. has data) */
	public boolean isValidEvent() {
		return valid;
	}

	/** Get a string representation of the property */
	@Override
	public String toString() {
		if (valid) {
			StringBuilder sb = new StringBuilder()
				.append("SS125 Vehicle Event,")
				.append(" stamp=").append(new Date(stamp))
				.append(" lane=").append(lane_id)
				.append(" range=").append(range)
				.append(" duration=").append(duration)
				.append(" speed=").append(speed)
				.append(" v_class=").append(v_class)
				.append(" length=").append(length);
			return sb.toString();
		} else
			return "No event";
	}

	/** Log a vehicle detection event */
	public void logVehicle(ControllerImpl controller) {
		if (!valid)
			return;
		DetectorImpl det = controller.getDetectorAtPin(lane_id + 1);
		if (det != null) {
			det.logVehicle(CommProtocol.SS_125_VLOG, duration, -1, stamp,
				speed, length, v_class, range);
			log("ActiveEventProperty.logVehicle: logged event=" + 
				this + " to det=" + det + " lane=" + lane_id);
		} else{
			log("ActiveEventProperty.logVehicle: NO detector for: veh_event="+
				this);
		}
	}
}
