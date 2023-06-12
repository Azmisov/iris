/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2017-2018  Iteris Inc.
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
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Iterator;
import us.mn.state.dot.tms.WeatherSensor;
import us.mn.state.dot.tms.WeatherSensorHelper;
import us.mn.state.dot.tms.units.Pressure;
import us.mn.state.dot.tms.units.Speed;
import us.mn.state.dot.tms.utils.SString;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.PavementSensorsTable;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.SubSurfaceSensorsTable;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.SurfaceStatus;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.PrecipSituation;

/**
 * Write SSI ScanWeb CSV weather export files.
 *
 * @author Michael Darter
 */
public class WeatherSensorCsvWriter extends XmlWriter {

	/** CSV file name */
	static private final String OUTPUT_FNAME_1 = "weather_sensor1.csv";

	/** CSV file name */
	static private final String OUTPUT_FNAME_2 = "weather_sensor2.csv";

	/** CSV missing value */
	static private final String MISSING = "";

	/** Get the site id from the notes field.
	 * @param ws Weather sensor
	 * @return The string X extracted from the word "siteid=X" 
	 * 	   within the notes field or the sensor's name. */
	static private String getSiteId(WeatherSensorImpl ws) {
		if (ws == null)
			return MISSING;
		String notes = ws.getNotes();
		if (notes == null)
			return ws.getName();
		notes = SString.stripCrLf(notes);
		String[] words = notes.split(" ");
		if (words == null)
			return ws.getName();
		for (String w : words) {
			if (!w.contains("siteid="))
				continue;
			String[] kv = w.split("=");
			if (kv == null || kv.length != 2)
				continue;
			return kv[1].trim();
		}
		return ws.getName();
	}

	/** File type to generate */
	final private int f_type;

	/** Capitalize the specified string, e.g. "aBC" becomes "Abc". Null or
	 * empty string is returned as empty string
	 */
	static private String capitalizeFirstLetter(String str) {
		if (str == null || str.length() == 0)
			return MISSING;
		return str.substring(0, 1).toUpperCase() + 
			str.substring(1).toLowerCase();
	}

	/** Return the specified date as a string in UTC.
	 * @param stamp A time stamp, null or < 0 for missing
	 * @return A string in UTC as MM/dd/yyyy HH:mm:ss */
	static private String formatDate(Long stamp) {
		if (stamp == null || stamp < 0)
			return MISSING;
		Date d = new Date(stamp);
		SimpleDateFormat sdf = 
			new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(d);
	}

	/** Factory to create a new CSV file writer and write the file.
	 * @param ft File type
	 * @return Null on error or a new file writer */
	static public WeatherSensorCsvWriter createWrite(int ft) 
		throws IOException
	{
		WeatherSensorCsvWriter wsw = null;
		// atmospheric data
		if (ft == 1)
			wsw = new WeatherSensorCsvWriter(OUTPUT_FNAME_1, 1);
		// surface data
		else if (ft == 2)
			wsw = new WeatherSensorCsvWriter(OUTPUT_FNAME_2, 2);
		if (wsw != null)
			wsw.write();
		return wsw;
	}

	/** Convert a temperature to CSV temperature.
	 * @arg v Temperature in C or null if missing.
	 * @return Temperature as hundredths of a degree C or 
	 * 	   the empty string for missing */
	static private String tToCsv(Integer v) {
		if (v != null) {
			int i = v.intValue() * 100;
			return String.valueOf(i);
		}	
		return MISSING;
	}

	/** Convert pavement surface status to CSV string
	 * @arg status - surface status
	 * @return Pavement surface status description or empty if missing. */
	static private String pssToN(SurfaceStatus status) {
		if (status != null && status != SurfaceStatus.undefined)
			return SString.splitCamel(status.toString());
		return MISSING;
	}

	/** Convert surface water depth to CSV string
	 * @arg row - row to fetch from
	 * @return Pavement surface water depth in .1mm or empty if missing */
	static private String swdToN(PavementSensorsTable.Row row) {
		// meters given; want to convert to 1/10th of mm
		String val = row.getWaterDepth(100);
		if (val != null)
			return val;
		return MISSING;
	}

	/** Convert pressure in pascals to CSV pressure.
	 * @arg v Pressure in Pascals or null.
	 * @return Pressure Pressure in .1 millibar, which are tenths 
	 *                  of hectoPascal or empty if missing */
	static private String prToCsv(Integer v) {
		if (v != null)
			return String.valueOf(new Pressure((double)v).ntcip());
		return MISSING;
	}

	/** Convert precip rate to CSV units. See essPrecipRate.
	 * @arg v Precip rate in mm/hr, null for missing.
	 * @return Precip rate as .025 mm/hr or empty for missing */
	static private String praToCsv(Integer v) {
		if (v != null)
			return String.valueOf(Math.round((double)v * 40));
		return MISSING;
	}

	/** Convert visibility to CSV units, see essVisibility.
	 * @arg v Distance in meters, null for missing.
	 * @return Distance in meters or empty for missing. */
	static private String visToCsv(Integer v) {
		if (v != null)
			return String.valueOf(v);
		return MISSING;
	}

	/** Convert precipitation accumulation to CSV units.
	 * @arg v Precip accum in mm, null for missing.
	 * @return Precip accumulation in .025 mm or empty for missing. 
	 *         See essPrecipitationOneHour */
	static private String pToCsv(Integer v) {
		return (v != null ? String.valueOf(v * 40) : MISSING);
	}

	/** Convert speed to CSV units.
	 * @arg v Speed in KPH, null for missing.
	 * @return Speed in KPH or empty for missing. See essAvgWindSpeed. */
	static private String sToCsv(Integer v) {
		if (v != null)
			return String.valueOf(v);
		else
			return MISSING;
	}

	/** Get the precipitation situation */
	static private String psToCsv(WeatherSensorImpl w) {
		var ps = PrecipSituation.from(w);
		return (ps != null ? ps.desc_csv : MISSING);
	}

	/** Get 1h accum precip in .025 mm or empty for missing */ 
	static private String apToCsv(WeatherSensorImpl w) {
		return pToCsv(w.getPrecipOneHour());
	}

	/** Append a CSV value to a StringBuffer */
	static private StringBuilder append(StringBuilder sb, String value) {
		if (value != null)
			sb.append(value);
		sb.append(",");
		return sb;
	}

	/** Append a CSV value to a StringBuffer */
	static private StringBuilder append(StringBuilder sb, Integer value) {
		if (value != null)
			sb.append(value);
		sb.append(",");
		return sb;
	}

	/** Append a CSV value to a StringBuffer */
	static private StringBuilder append(StringBuilder sb, Long value) {
		if (value != null)
			sb.append(value);
		sb.append(",");
		return sb;
	}

	/** Append a CSV value to a StringBuffer */
	static private StringBuilder append(StringBuilder sb, Double value) {
		if (value != null)
			sb.append(value);
		sb.append(",");
		return sb;
	}

	/** Append a CSV value to a StringBuffer */
	static private StringBuilder append(StringBuilder sb, PrecipSituation value){
		if (value != null && value != PrecipSituation.undefined)
			sb.append(value);
		sb.append(",");
		return sb;
	}

	/** Constructor */
	public WeatherSensorCsvWriter(String fn, int ft) {
		super(fn, true);
		f_type = ft;
	}

	/** Write the head of the CSV file */
	private void writeHead(Writer wr) throws IOException {
		if (f_type == 1) {
			writeLine(wr, "Siteid,DtTm,AirTemp,Dewpoint,Rh," + 
				"SpdAvg,SpdGust,DirMin,DirAvg,DirMax," + 
				"Pressure,PcIntens,PcType,PcRate,PcAccum," + 
				"Visibility");
		} else if (f_type == 2) {
			writeLine(wr, "Siteid,senid,DtTm,sfcond,sftemp," + 
				"frztemp,chemfactor,chempct,depth,icepct," + 
				"subsftemp,waterlevel");
		}
	}

	/** Write the body of the XML file */
	private void writeBody(Writer wr) throws IOException {
		Iterator<WeatherSensor> it = WeatherSensorHelper.iterator();
		while(it.hasNext()) {
			WeatherSensor ws = it.next();
			if(ws instanceof WeatherSensorImpl)
				writeLine(wr, (WeatherSensorImpl)ws);
		}
	}

	/** Write a terminated line */
	private void writeLine(Writer wr, String line) 
		throws IOException
	{
		if (line != null)
			wr.write(line + "\n");
	}

	/** Write a CSV line */
	private void writeLine(Writer wr, WeatherSensorImpl w) 
		throws IOException
	{
		if (w == null)
			return;
		if (f_type == 1)
			writeLine1(wr, w);
		else if (f_type == 2)
			writeLine2(wr, w);
	}

	/** Write the weather sensor CSV file */
	@Override protected void write(Writer w) throws IOException {
		writeHead(w);
		writeBody(w);
	}

	/** Write a CSV line for the atmospheric file */
	private void writeLine1(Writer wr, WeatherSensorImpl w) 
		throws IOException
	{
		StringBuilder sb = new StringBuilder();
		append(sb, getSiteId(w));		//Siteid
		append(sb, formatDate(w.getStamp()));	//DtTm
		append(sb, tToCsv(w.getAirTemp()));	//AirTemp
		append(sb, tToCsv(w.getDewPointTemp()));	//Dewpoint
		append(sb, w.getHumidity());		//Rh
		append(sb, sToCsv(w.getWindSpeed()));	//SpdAvg
		append(sb, sToCsv(
			w.getMaxWindGustSpeed()));	//SpdGust
		append(sb, MISSING);				//DirMin
		append(sb, w.getWindDir());		//DirAvg
		append(sb, w.getMaxWindGustDir());	//DirMax
		append(sb, prToCsv(w.getPressure()));	//Pressure
		append(sb, capitalizeFirstLetter(
			WeatherSensorHelper.getPrecipRateIntensity(w)));	//PcIntens
		append(sb, psToCsv(w));     //PcType
		append(sb, praToCsv(w.getPrecipRate()));	//PcRate
		append(sb, apToCsv(w));	//PcAccum
		append(sb, visToCsv(w.getVisibility()));	//Visibility
		sb.setLength(sb.length() - 1);
		writeLine(wr, sb.toString());
	}

	/** Write a CSV line for the surface file */
	private void writeLine2(Writer wr, WeatherSensorImpl w) 
		throws IOException
	{
		String sid = getSiteId(w);
		int senid = 0;
		String dat = formatDate(w.getStamp());
		// iterate through pavement sensor table
		PavementSensorsTable ps_t = w.getPavementSensorsTable();
		for (var row: ps_t){
			String pss = pssToN(row.getSurfStatus());
			String sft = tToCsv(row.getSurfTempC());
			String fzt = tToCsv(row.getFreezePointC());
			String swd = swdToN(row);
			String sst = MISSING;
			writeLine2(wr, sid, senid, dat, pss, sft, fzt, sst, swd);
			++senid;
		}
		// iterate through subsurface sensors table
		SubSurfaceSensorsTable ss_t = w.getSubsurfaceSensorsTable();
		for (var row: ss_t){
			String pss = MISSING;
			String sft = MISSING;
			String fzt = MISSING;
			String swd = MISSING;
			String sst = tToCsv(row.getTempC());
			writeLine2(wr, sid, senid, dat, pss, sft, fzt, sst, swd);
			++senid;
		}
	}

	/** Write a CSV line for the surface file */
	private void writeLine2(Writer wr, String sid, int senid, String dat, 
		String sfc, String sft, String fzt, String sst, String swd)
		throws IOException
	{
		String ssenid = String.valueOf(senid);
		StringBuilder sb = new StringBuilder();
		append(sb, sid);	//Siteid
		append(sb, ssenid);	//senid
		append(sb, dat);	//DtTm
		append(sb, sfc);	//sfcond
		append(sb, sft);	//sftemp
		append(sb, fzt);	//frztemp
		append(sb, MISSING);	//chemfactor
		append(sb, MISSING);	//chempct
		append(sb, swd);	//depth
		append(sb, MISSING);	//icepct
		append(sb, sst);	//subsftemp
		append(sb, MISSING);	//waterlevel
		sb.setLength(sb.length() - 1);
		writeLine(wr, sb.toString());
	}
}
