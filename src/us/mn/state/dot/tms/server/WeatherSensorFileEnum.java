/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2020-2023  Iteris Inc.
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import us.mn.state.dot.tms.GeoLocHelper;
import us.mn.state.dot.tms.WeatherSensorHelper;
import us.mn.state.dot.tms.geo.Position;
import us.mn.state.dot.tms.units.Pressure;
import us.mn.state.dot.tms.units.Distance;
import us.mn.state.dot.tms.units.Temperature;
import us.mn.state.dot.tms.utils.SString;
import us.mn.state.dot.tms.server.comm.ntcip.PikalertRoadState;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SurfaceStatus;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.EssEnumType;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.PrecipSituation;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.EssTemperature;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.PavementSensorsTable;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.SubSurfaceSensorsTable;

/**
 * Weather sensor file type
 *
 * @author Michael Darter, Isaac Nygaard
 */
public enum WeatherSensorFileEnum {

	/** Weather sensor file types */
	ATMO("weather_sensor1.csv",
		"Siteid,DtTm,AirTemp,Dewpoint,Rh," + 
		"SpdAvg,SpdGust,DirMin,DirAvg,DirMax," + 
		"Pressure,PcIntens,PcType,PcRate,PcAccum," + 
		"Visibility"),
	SURF("weather_sensor2.csv",
		"Siteid,senid,DtTm,sfcond,sftemp," + 
		"frztemp,chemfactor,chempct,depth,icepct," + 
		"subsftemp,waterlevel"),
	PIKA("weather_sensor_pikalert.csv",
		"stationID,observationTime,latitude," +
		"longitude,altitude,dewpoint,precipRate," +
		"relHumidity,roadTemperature1," +
		"roadTemperature2,temperature,visibility," +
		"windDir,windDirMax,windGust,windSpeed," +
		"roadState1,presWx");

	/** Missing value */
	static final String MISSING = "";

	/** Capitalize the specified string, e.g. "aBC" becomes "Abc" */
	static private String cap(String str) {
		if (str != null && str.length() >= 1) {
			return str.substring(0, 1).toUpperCase() + 
				str.substring(1).toLowerCase();
		} else {
			return MISSING;
		}
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

	/** Return the specified date as a string in UTC.
	 * @param stamp A time stamp, null or < 0 for missing
	 * @return A string in UTC as yyyy-MM-dd HH:mm:ss */
	static private String formatDate2(Long stamp) {
		if (stamp == null || stamp < 0)
			return MISSING;
		Date d = new Date(stamp);
		SimpleDateFormat sdf = 
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(d);
	}

	/** Convert a temperature to CSV temperature.
	 * @arg v Integer temperature in C or null if missing.
	 * @return Temperature as hundredths of a degree C or 
	 * 	   the empty string for missing */
	static private String tIntToCsv100(Integer v) {
		if (v != null) {
			int i = v.intValue() * 100;
			return String.valueOf(i);
		} else {
			return MISSING;
		}
	}

    /** Convert an NTCIP temperature to CSV temperature.
 	 * @arg v base temperature
 	 * @return Temperature as hundredths of a degree C or 
 	 * 	   the empty string for missing */
	static private String essTempToCsv100(EssTemperature v) {
		return v.get(
			t -> String.valueOf(t.round(Temperature.Units.HUNDREDTH_CELSIUS)),
			MISSING
		);
	}

	/** Convert a temperature in C to a string.
	 * @arg tc NTCIP temp in C or null on error.
	 * @return Temperature in degrees C or empty string for missing */
	static private String tToCsv(Double tc) {
		return (tc != null ? SString.doubleToString(tc, 4) : MISSING);
	}

	/** Convert pavement surface status to CSV string
	 * @arg status - surface status
	 * @return Pavement surface status description or empty if missing. */
	static private String pssToN(SurfaceStatus status) {
		if (EssEnumType.isValid(status))
			return SString.splitCamel(status.toString());
		return MISSING;
	}

	/** Convert surface water depth to CSV string
	 * @arg row - row to fetch from
	 * @return Pavement surface water depth in .1 mm or empty if missing */
	static private String swdToN(PavementSensorsTable.Row row) {
        return row.water_depth.get(
			// m -> 1/10 mm
			d -> String.valueOf(d.round(Distance.Units.TENTH_MILLIMETERS)),
			MISSING
		);
	}

	/** Convert pressure in pascals to CSV pressure.
	 * @arg v Pressure in Pascals or null.
	 * @return Pressure Pressure in .1 millibar, which are tenths 
	 *                  of hectoPascal or empty if missing */
	static private String prToCsv(Integer v) {
		if (v != null) {
			return String.valueOf(new Pressure((double)v).ntcip());
		} else
			return MISSING;
	}

	/** Return precip rate in CSV units. See essPrecipRate.
	 * @return Precip rate as .025 mm/hr or empty for missing */
	static private String praToCsv(WeatherSensorImpl w) {
		Integer v = w.getPrecipRate(); // mm/hr or null
		return (v != null ? String.valueOf(v * 40): MISSING);
	}

	/** Convert precip rate to Pikalert CSV units. See essPrecipRate.
	 * @arg v Precip rate in mm/hr, null for missing.
	 * @return Precip rate in mm over 1 hour or empty for missing */
	static private String praToCsvPikalert(WeatherSensorImpl w) {
		Integer v = w.getPrecipRate(); // mm/hr or null
		return (v != null ? String.valueOf(v): MISSING);
	}

	/** Convert visibility to CSV units, see essVisibility.
	 * @arg w Weather sensor
	 * @return Distance in meters or empty for missing. */
	static private String visToCsv(WeatherSensorImpl w) {
		Integer v = w.getVisibility();
		return (v != null ? String.valueOf(v) : MISSING);
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
	static private String spToCsv(Integer v) {
		if (v != null)
			return String.valueOf(v);
		else
			return MISSING;
	}

	/** Convert speed to Pikalert CSV units.
	 * @arg v Speed in KPH, null for missing.
	 * @return Speed in m/s or empty for missing. See essAvgWindSpeed. */
	static private String spToPikalertCsv(Integer v) {
		if (v != null)
			return SString.doubleToString(v * .2777777778, 4);
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

	/** Get altitude from the RWIS sensor.
	 * @param w Weather sensor to read altitude from
	 * @return Altitude in meters above sealevel or empty 
	 * 	   string if missing */
	static private String getAltitude(WeatherSensorImpl w) {
		Integer elv = w.getElevation();
		return (elv != null ? Integer.toString(elv) : MISSING);
	}

	/** Get Pikalert road surface temperature of the nth active sensor.
	 * @param rown One-based nth active sensor
	 * @param w Weather sensor
	 * @return Road surface temperature in degrees C or empty */
	static private String getRoadTempPikalert(int arown, 
		WeatherSensorImpl w) 
	{
		PavementSensorsTable pst = w.getPavementSensorsTable();
		if (pst == null)
			return MISSING;
        var row = pst.getNthActive(arown);
		return tToCsv(row == null ? null : row.surface_temp.toDouble());
	}

	/** Get the Pikalert road state ordinal, which is the road state of
	 * of the first active sensor */
	static private int getPikalertRoadState(WeatherSensorImpl w) {
		PavementSensorsTable pst = w.getPavementSensorsTable();
        PikalertRoadState state = PikalertRoadState.RS_NO_REPORT;
		if (pst != null) {
			var row = pst.getNthActive(1);
			if (row != null){
				state = PikalertRoadState.convert(
					row.surface_status.get(),
					row.black_ice_signal.get()
				);
			}
		}
		return state.ordinal();
	}

	/** Get the Pikalert present weather code */
	static private String getPikalertPresWx(WeatherSensorImpl w) {
		return PresWx.create(w).toString();
	}

	/** File name */
	public final String file_name;

	/** Header row */
	public final String header_row;

	/** Constructor */
	WeatherSensorFileEnum(String fn, String hr) {
		file_name = fn;
		header_row = hr;
	}

	/** Get record(s) to be written to the file */
	String getRecs(WeatherSensorImpl w) {
		StringBuilder sb = new StringBuilder();
		if (w != null) {
			if (this == ATMO)
				return getAtmoRec(w);
			if (this == SURF)
				return getSurfRecs(w);
			if (this == PIKA)
				return getAtmoRec(w);
		}
		return sb.toString();
	}

	/** Get a record for the ATMO file type */
	String getAtmoRec(WeatherSensorImpl w) {
		StringBuilder sb = new StringBuilder();
		append(sb, w.getSiteId());		 //Siteid
		append(sb, formatDate(w.getStamp()));	 //DtTm
		append(sb, tIntToCsv100(w.getAirTemp()));//AirTemp
		append(sb, tIntToCsv100(w.getDewPointTemp()));//Dewpoint
		append(sb, w.getHumidity());		 //Rh
		append(sb, spToCsv(w.getWindSpeed()));	 //SpdAvg
		append(sb, spToCsv(
			w.getMaxWindGustSpeed()));	 //SpdGust
		append(sb, MISSING);			 //DirMin
		append(sb, w.getWindDir());		 //DirAvg
		append(sb, w.getMaxWindGustDir());	 //DirMax
		append(sb, prToCsv(w.getPressureQC()));	 //Pressure
		append(sb, cap(WeatherSensorHelper.
			getPrecipRateIntensity(w)));	 //PcIntens
		append(sb, psToCsv(w));			 //PcType
		append(sb, praToCsv(w));		 //PcRate
		append(sb, apToCsv(w));			 //PcAccum
		append(sb, visToCsv(w));		 //Visibility
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	/** Get recs for SURF file type */
	String getSurfRecs(WeatherSensorImpl w) {
		StringBuilder sb = new StringBuilder();
		String sid = w.getSiteId();
		int senid = 0;
		String dat = formatDate(w.getStamp());
        // iterate through pavement sensor table
		PavementSensorsTable ps_t = w.getPavementSensorsTable();
		for (var row: ps_t){
			String pss = pssToN(row.getSurfStatus());
			String sft = essTempToCsv100(row.surface_temp);
			String fzt = essTempToCsv100(row.freeze_point);
			String swd = swdToN(row);
			String sst = MISSING; // pvmt temp is not subsurf
			sb.append(getSurfRec(sid, senid, dat, pss, sft, fzt, 
				sst, swd));
			++senid;
		}
		// iterate through subsurface sensors table
		SubSurfaceSensorsTable ss_t = w.getSubsurfaceSensorsTable();
		for (var row: ss_t){
			String pss = MISSING;
			String sft = MISSING;
			String fzt = MISSING;
			String swd = MISSING;
			String sst = essTempToCsv100(row.temp);
			sb.append(getSurfRec(sid, senid, dat, pss, sft, fzt, sst, swd));
			++senid;
		}
		return sb.toString();
	}

	/** Get a sensor record for SURF file type */
	private String getSurfRec(String sid, int senid, String dat, String sfc, 
		String sft, String fzt, String sst, String swd)
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
		append(sb, swd);	//depth .1mm
		append(sb, MISSING);	//icepct
		append(sb, sst);	//subsftemp
		append(sb, MISSING);	//waterlevel
//mtod need \n? 
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	/** Get record for PIKA file type */
	String getPikaRec(WeatherSensorImpl w) {
		StringBuilder sb = new StringBuilder();
		Position pos = GeoLocHelper.getWgs84Position(w.getGeoLoc());
		String lat = SString.doubleToString(pos.getLatitude(), 8); //1mm
		String lon = SString.doubleToString(pos.getLongitude(), 8); //1mm

		append(sb, w.getPikalertSiteId());	//pikalert siteid
		append(sb, formatDate2(w.getStamp()));	//observationTime
		append(sb, lat);			//latitude
		append(sb, lon);			//longitude
		append(sb, getAltitude(w));		//altitude
		append(sb, w.getDewPointTemp());	//dewpoint
		append(sb, praToCsvPikalert(w));	//precipRate
		append(sb, w.getHumidity());		//relHumidity
		append(sb, getRoadTempPikalert(1, w));	//roadTemperature1
		append(sb, getRoadTempPikalert(2, w));	//roadTemperature2
		append(sb, w.getAirTemp());		//temperature
		append(sb, visToCsv(w));		//visibility
		append(sb, w.getWindDir());		//windDir
		append(sb, w.getMaxWindGustDir());	//windDirMax
		append(sb, spToPikalertCsv(
			w.getMaxWindGustSpeed()));	//windGust
		append(sb, spToPikalertCsv(
			w.getWindSpeed()));		//windSpeed
		append(sb, getPikalertRoadState(w));	//roadState1
		append(sb, getPikalertPresWx(w));	//presWx
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}
}