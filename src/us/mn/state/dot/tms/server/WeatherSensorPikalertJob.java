/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2019  Iteris Inc.
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
import java.util.Calendar;
import us.mn.state.dot.sched.Job;

/**
 * Job to write out weather sensor Pikalert CSV file every 5m.
 *
 * @author Michael Darter
 */
public class WeatherSensorPikalertJob extends Job {

	/** Seconds to offset each poll from start of interval */
	static protected final int OFFSET_SECS = 20;

	/** Create a new job */
	public WeatherSensorPikalertJob() {
		super(Calendar.MINUTE, 5, Calendar.SECOND, OFFSET_SECS);
	}

	/** Perform the job */
	public void perform() throws IOException {
		WeatherSensorCsvWriter wsw = WeatherSensorCsvWriter.create(
			WeatherSensorFileEnum.PIKA);
		if (wsw != null)
			wsw.write();
	}
}