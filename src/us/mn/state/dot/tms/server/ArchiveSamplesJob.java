/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2010-2013  Minnesota Department of Transportation
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import us.mn.state.dot.sched.DebugLog;
import us.mn.state.dot.sched.Job;
import us.mn.state.dot.sched.TimeSteward;

/**
 * Job to create sample data archive files.
 *
 * @author Douglas Lau
 * @author Michale Darter
 */
public class ArchiveSamplesJob extends Job {

	/** Debug log */
	static private final DebugLog TA_LOG = new DebugLog("traffic_archive");

	/** Buffer for reading sample data files */
	protected final byte[] buffer = new byte[8192];

	/** Sample archive factory */
	private final SampleArchiveFactory a_factory;

	/** Log a DMS message */
	static public void log(String msg) {
		if (TA_LOG.isOpen())
			TA_LOG.log(msg);
	}

	/** Create a new job to archive sample data.  This needs to happen
	 * after 6 PM to allow for buffered data to be read in case of
	 * communication errors (MnDOT protocol). */
	public ArchiveSamplesJob(SampleArchiveFactory saf) {
		super(Calendar.DATE, 1, Calendar.HOUR, 22);
		a_factory = saf;
	}

	/** Perform the archive samples job */
	public void perform() throws IOException {
		archiveSamples();
	}

	/** Archive data samples */
	protected void archiveSamples() throws IOException {
		log("------starting daily archive");
		log("known_extensions=" + a_factory.getKnownExtensions());
		File[] years = listYears();
		if(years != null) {
			log("n_years=" + years.length);
			for(File year: years) {
				log("year=" + year);
				File[] days = listDays(year);
				if(days != null) {
					log("n_days=" + days.length);
					for(File day: days) {
						log("day=" + day);
						createSampleArchive(day);
					}
				} else {
					log("n_days=none");
				}
			}
		} else {
			log("n_years=none");
		}
		log("done with daily archive");
	}

	/** Get an array of years in the sample archive directory */
	protected File[] listYears() {
		File arc = SampleArchiveFactoryImpl.sampleArchiveDir();
		return arc.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory() &&
				       isValidYear(file.getName());
			}
		});
	}

	/** Get an array of days with sample data for a given year */
	protected File[] listDays(File year) {
		return year.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory() &&
				       isValidDate(file.getName());
			}
		});
	}

	/** Create a sample archive file for the given day */
	protected void createSampleArchive(File day) throws IOException {
		File traf = new File(day.toString() + ".traffic");
		if(!traf.exists()) {
			log("archive file does NOT exist: " + traf);
			createSampleArchive(traf, day);
		} else {
			log("archive file exists: " + traf);
		}
	}

	/** Create a sample archive file and delete the original sample files */
	protected void createSampleArchive(File traf, File day)
		throws IOException
	{
		FileOutputStream fos = new FileOutputStream(traf);
		try {
			log("createSampleArchive: " + traf + " day=" + day);
			addSampleEntries(fos, day);
			deleteOriginalSampleFiles(traf, day);
		}
		finally {
			fos.close();
		}
	}

	/** Add all valid sample file entries to an archive file */
	protected void addSampleEntries(FileOutputStream fos, File day)
		throws IOException
	{
		ZipOutputStream zos = new ZipOutputStream(
			new BufferedOutputStream(fos));
		addSampleEntries(zos, day);
		zos.finish();
		zos.close();
	}

	/** Add all valid sample file entries to an archive file */
	protected void addSampleEntries(ZipOutputStream zos, final File day)
		throws IOException
	{
		String[] entries = day.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				final boolean e = 
					a_factory.hasKnownExtension(name);
				if (!e) {
					log("addSampleEntries: ignoring " + 
						"invalid file extension " + 
						"for file=" + name);
				}
				return e;
				//return a_factory.hasKnownExtension(name);
			}
		});
		Arrays.sort(entries);
		for(String entry: entries)
			addSampleEntry(zos, day, entry);
	}

	/** Add one sample file entry to an archive file */
	protected void addSampleEntry(ZipOutputStream zos, File day,
		String name) throws IOException
	{
		log("addSampleEntry: adding name=" + name + " day=" + day);
		zos.putNextEntry(new ZipEntry(name));
		FileInputStream fis = new FileInputStream(new File(day, name));
		try {
			synchronized(buffer) {
				while(true) {
					int n_bytes = fis.read(buffer);
					if(n_bytes < 0)
						break;
					zos.write(buffer, 0, n_bytes);
				}
			}
		}
		finally {
			fis.close();
		}
	}

	/** Delete the original sample files that have been copied into an
	 * archive file */
	protected void deleteOriginalSampleFiles(File traf, File day)
		throws IOException
	{
		ZipFile zf = new ZipFile(traf);
		Enumeration e = zf.entries();
		while(e.hasMoreElements()) {
			ZipEntry ze = (ZipEntry)e.nextElement();
			String name = ze.getName();
			if(a_factory.hasKnownExtension(name)) {
				File file = new File(day, name);
				log("deleteOriginalSampleFiles: day=" + day + 
					" name=" + name);
				if(file.isFile())
					file.delete();
			} else {
				log("deleteOriginalSampleFiles: unknown " + 
					"extension=" + name);
			}
		}
		day.delete();
	}

	/** Test if a year is valid */
	static protected boolean isValidYear(String year) {
		if(year.length() != 4)
			return false;
		try {
			return Integer.parseInt(year) > 1900;
		}
		catch(NumberFormatException e) {
			log("invalid year=" + year);
			return false;
		}
	}

	/** Test if a date string is valid */
	static protected boolean isValidDate(String date) {
		if(date.length() != 8 ||
		   date.equals(TimeSteward.currentDateShortString()))
			return false;
		try {
			return Integer.parseInt(date) > 19000000;
		}
		catch(NumberFormatException e) {
			log("invalid date=" + date);
			return false;
		}
	}
}
