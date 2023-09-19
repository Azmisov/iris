/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2010-2021  Minnesota Department of Transportation
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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import us.mn.state.dot.sched.TimeSteward;

/**
 * Factory for creating sample archive files.
 *
 * @author Douglas Lau
 * @author Michale Darter
 */
public class SampleArchiveFactoryImpl implements SampleArchiveFactory {

	/** Sample archive directory */
	static public File sampleArchiveDir() {
		return new File("/var/lib/iris/traffic",
			MainServer.districtId());
	}

	/** Get a valid directory for a given date stamp.
	 * @param stamp Time stamp
	 * @return Directory to store sample data.
	 * @throws IOException If directory cannot be created. */
	static private String directory(long stamp) throws IOException {
		File arc = sampleArchiveDir();
		if(!arc.exists() && !arc.mkdir())
			throw new IOException("mkdir failed: " + arc);
		String d = TimeSteward.dateShortString(stamp);
		File year = new File(arc, d.substring(0, 4));
		if(!year.exists() && !year.mkdir())
			throw new IOException("mkdir failed: " + year);
		File dir = new File(year.getPath(), d);
		if(!dir.exists() && !dir.mkdir())
			throw new IOException("mkdir failed: " + dir);
		return dir.getCanonicalPath();
	}

	/** Set of all archive file extensions */
	private final HashSet<String> extensions = new HashSet<String>();

	/** Add a file extension */
	private void addExtension(String ext) {
		synchronized(extensions) {
			extensions.add(ext);
		}
	}

	/** Test if a sample file name has a known extension */
	public boolean hasKnownExtension(String name) {
		final boolean ARCHIVE_ALL_FILES = true;
		if (ARCHIVE_ALL_FILES) {
			// archive all files in the archive directory
			return true;
		} else {
			// Archive only known file types in archive dir.
			// If an unknown file type is encountered, operation
			// is halted and partial daily archive file is not 
			// deleted. This stops future archive attempts for 
			// this day.
			synchronized(extensions) {
				for(String ext: extensions) {
					if(name.endsWith(ext))
						return true;
				}
			}
			return false;
		}
	}

	/** Get a list of all valid extensions */
	public String getKnownExtensions() {
		StringBuilder sb = new StringBuilder();
		synchronized(extensions) {
			for(String ext: extensions) {
				sb.append(ext);
				sb.append(" ");
			}
		}
		return sb.toString();
	}

	/** Create an archive file.
	 * @param sensor_id Sensor identifier.
	 * @param ext File extension.
	 * @param stamp Time stamp.
	 * @return File to archive sample data from that time stamp. */
	public File createFile(String sensor_id, String ext, long stamp)
		throws IOException
	{
		String dext = "." + ext;
		addExtension(dext);
		return new File(directory(stamp), sensor_id + dext);
	}

	/** Create an archive file.
	 * @param sensor_id Sensor identifier.
	 * @param s_type Periodic sample type.
	 * @param ps Periodic sample to be archived.
	 * @return File to archive periodic sample. */
	public File createFile(String sensor_id, PeriodicSampleType s_type,
		PeriodicSample ps) throws IOException
	{
		return createFile(sensor_id, s_type.extension + ps.per_sec,
			ps.start());
	}
}
