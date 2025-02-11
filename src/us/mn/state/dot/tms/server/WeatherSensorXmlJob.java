/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2017-2019  Iteris Inc.
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
 * Job to write out weather sensor XML file.
 *
 * @author Michael Darter
 */
public class WeatherSensorXmlJob extends Job {

	/** Seconds to offset each poll from start of interval */
	static protected final int OFFSET_SECS = 20;

	/** Create a new job */
	public WeatherSensorXmlJob() {
		super(Calendar.MINUTE, 1, Calendar.SECOND, OFFSET_SECS);
	}

	/** Perform the job */
	public void perform() throws IOException {
		new WeatherSensorXmlWriter().write();
		writeCsvFiles();
	}

	/** Write CSV files */
	public void writeCsvFiles() throws IOException {
		WeatherSensorCsvWriter ww;
		ww = WeatherSensorCsvWriter.create(WeatherSensorFileEnum.ATMO);
		if (ww != null)
			ww.write();
		ww = WeatherSensorCsvWriter.create(WeatherSensorFileEnum.SURF);
		if (ww != null)
			ww.write();
		ww = WeatherSensorCsvWriter.create(WeatherSensorFileEnum.ATMO2);
		if (ww != null)
			ww.write();
		ww = WeatherSensorCsvWriter.create(WeatherSensorFileEnum.SURF2);
		if (ww != null)
			ww.write();
	}
}
