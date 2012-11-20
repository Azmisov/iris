/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2009-2012  Minnesota Department of Transportation
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
import us.mn.state.dot.tms.server.comm.ParsingException;

/**
 * General Configuration Property.
 *
 * @author Douglas Lau
 */
public class GeneralConfigProperty extends SS125Property {

	/** Message ID for general config request */
	protected int msgId() {
		return MSG_ID_GENERAL_CONFIG;
	}

	/** Format the body of a GET request */
	byte[] formatBodyGet() throws IOException {
		byte[] body = new byte[3];
		format8(body, OFF_MSG_ID, msgId());
		format8(body, OFF_MSG_SUB_ID, SUB_ID_DONT_CARE);
		format8(body, OFF_READ_WRITE, REQ_READ);
		return body;
	}

	/** Format the body of a SET request */
	byte[] formatBodySet() throws IOException {
		byte[] body = new byte[86];
		format8(body, OFF_MSG_ID, msgId());
		format8(body, OFF_MSG_SUB_ID, SUB_ID_DONT_CARE);
		format8(body, OFF_READ_WRITE, REQ_WRITE);
		formatString(body, 3, 2, orientation);
		formatString(body, 5, 32, location);
		formatString(body, 37, 32, description);
		formatString(body, 69, 16, serialNumber);
		formatBool(body, 85, metric);
		return body;
	}

	/** Parse the payload of a GET response */
	void parsePayload(byte[] body) throws IOException {
		if(body.length != 86)
			throw new ParsingException("BODY LENGTH");
		orientation = parseString(body, 3, 2);
		location = parseString(body, 5, 32);
		description = parseString(body, 37, 32);
		serialNumber = parseString(body, 69, 16);
		metric = parseBoolean(body[85]);
		setComplete(true);
	}

	/** Sensor orientation */
	protected String orientation = "";

	/** Get the sensor orientation */
	public String getOrientation()  {
		return orientation;
	}

	/** Sensor location */
	protected String location = "";

	/** Get the sensor location */
	public String getLocation() {
		return location;
	}

	/** Set the sensor location */
	public void setLocation(String loc) {
		location = loc;
	}

	/** Sensor description */
	protected String description = "";

	/** Get the sensor description */
	public String getDescription() {
		return description;
	}

	/** Sensor serial number */
	protected String serialNumber = "";

	/** Get the sensor serial number */
	public String getSerialNumber() {
		return serialNumber;
	}

	/** Metric flag */
	protected boolean metric = false;

	/** Get the metric flag */
	public boolean isMetric() {
		return metric;
	}

	/** Set the metric flag */
	public void setMetric(boolean m) {
		metric = m;
	}
}
