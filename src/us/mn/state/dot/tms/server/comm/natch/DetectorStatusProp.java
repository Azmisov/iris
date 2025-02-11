/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2021-2022  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.natch;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import us.mn.state.dot.sched.TimeSteward;
import us.mn.state.dot.tms.CommProtocol;
import us.mn.state.dot.tms.server.ControllerImpl;
import us.mn.state.dot.tms.server.DetectorImpl;
import us.mn.state.dot.tms.server.comm.Operation;

/**
 * Detector status property
 *
 * @author Douglas Lau
 */
public class DetectorStatusProp extends DetectorProp {

	/** Valid age of vehicle events (1 hour) */
	static private final long VALID_AGE_MS = 60 * 60 * 1000;

	/** Valid future vehicle events (30 second drift) */
	static private final long FUTURE_MS = 30 * 1000;

	/** Parse a time */
	static private long parseTime(String v) {
		if (v.length() == 8 &&
		    v.charAt(2) == ':' &&
		    v.charAt(5) == ':')
		{
			int hour = parseInt(v.substring(0, 2));
			int min = parseInt(v.substring(3, 5));
			int sec = parseInt(v.substring(6, 8));
			if (hour >= 0 && min >= 0 && sec >= 0) {
				long now = TimeSteward.currentTimeMillis();
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(now);
				cal.set(Calendar.HOUR_OF_DAY, hour);
				cal.set(Calendar.MINUTE, min);
				cal.set(Calendar.SECOND, sec);
				cal.set(Calendar.MILLISECOND, 0);
				// Is the stamp from yesterday?
				if (cal.getTimeInMillis() > now + FUTURE_MS)
					cal.add(Calendar.DAY_OF_MONTH, -1);
				return cal.getTimeInMillis();
			}
		}
		return 0;
	}

	/** Received Message ID */
	private String received_id;

	/** Duration (ms) */
	private int duration;

	/** Get the duration */
	public int getDuration() {
		return duration;
	}

	/** Headway (ms) */
	private int headway;

	/** Get the headway */
	public int getHeadway() {
		return headway;
	}

	/** Date/time stamp */
	private long stamp;

	/** Create a new detector status property */
	public DetectorStatusProp(Counter c) {
		super(c, -1);
		received_id = message_id;
	}

	/** Check received message ID */
	@Override
	protected boolean checkMessageId(String msg_id) {
		received_id = msg_id;
		return true;
	}

	/** Get the received message ID */
	public String getReceivedId() {
		return received_id;
	}

	/** Encode a QUERY request */
	@Override
	public void encodeQuery(Operation op, ByteBuffer tx_buf)
		throws IOException
	{
		String msg = "DS," + received_id + '\n';
		tx_buf.put(msg.getBytes(UTF8));
	}

	/** Get the message code */
	@Override
	protected String code() {
		return "ds";
	}

	/** Get the number of response parameters */
	@Override
	protected int parameters() {
		return 6;
	}

	/** Parse parameters for a received message */
	@Override
	protected boolean parseParams(String[] param) {
		detector_num = parseInt(param[2]);
		duration = parseInt(param[3]);
		headway = parseInt(param[4]);
		stamp = parseTime(param[5]);
		return isValidNum();
	}

	/** Log vehicle event */
	public void logEvent(Operation op) {
		ControllerImpl ctrl = op.getController();
		DetectorImpl det = lookupDet(ctrl);
		if (det != null && isValidStamp())
			det.logVehicle(CommProtocol.NATCH, duration, headway, stamp,
				0, 0, -1, -1);
		else
			ctrl.logGap();
		ctrl.completeOperation(op.getId(), true);
	}

	/** Is time stamp valid? */
	private boolean isValidStamp() {
		long now = TimeSteward.currentTimeMillis();
		return (stamp > now - VALID_AGE_MS)
		    && (stamp < now + FUTURE_MS);
	}
}
