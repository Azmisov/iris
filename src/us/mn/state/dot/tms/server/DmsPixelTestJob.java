/*
 * IRIS -- Intelligent Roadway Information System
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
package us.mn.state.dot.tms.server;

import java.util.Calendar;
import java.util.Iterator;
import us.mn.state.dot.sched.Job;
import us.mn.state.dot.tms.DeviceRequest;
import us.mn.state.dot.tms.DMS;
import us.mn.state.dot.tms.DMSHelper;

/**
 * Job to periodically pixel test all DMS.
 * @author Michael Darter
 */
public class DmsPixelTestJob extends Job {

	/** Constructor */
	public DmsPixelTestJob() {
		// execute every morning at 3AM
		super(Calendar.DATE, 1, Calendar.HOUR, 3);
	}

	/** Perform the job */
	public void perform() {
		int req = DeviceRequest.TEST_PIXELS.ordinal();
		Iterator<DMS> it = DMSHelper.iterator();
		while(it.hasNext()) {
			DMS d = it.next();
			if(d instanceof DMSImpl) {
				DMSImpl dms = (DMSImpl)d;
				dms.setDeviceRequest(req);
			}
		}
	}
}