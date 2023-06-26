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
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SurfaceStatus;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.EssEnumType;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.PikalertRoadState;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.PrecipSituation;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.VisibilitySituation;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.EssTemperature;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.EssDistance;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.PavementSensorsTable;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.PresWx;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.SubSurfaceSensorsTable;

/**
 * Weather sensor file types
 *
 * @author Michael Darter, Isaac Nygaard
 */
public enum WeatherSensorFileEnum {

	/* standard atmospheric and surface files */
	ATMO("weather_sensor1.csv",
		"Siteid,DtTm,AirTemp,Dewpoint,Rh," + 
		"SpdAvg,SpdGust,DirMin,DirAvg,DirMax," + 
		"Pressure,PcIntens,PcType,PcRate,PcAccum," + 
		"Visibility"),
	SURF("weather_sensor2.csv",
		"Siteid,senid,DtTm,sfcond,sftemp," + 
		"frztemp,chemfactor,chempct,depth,icepct," + 
		"subsftemp,waterlevel"),

	/* pikalert */
	PIKA("weather_sensor_pikalert.csv",
		"stationID,observationTime,latitude," +
		"longitude,altitude,dewpoint,precipRate," +
		"relHumidity,roadTemperature1," +
		"roadTemperature2,temperature,visibility," +
		"windDir,windDirMax,windGust,windSpeed," +
		"roadState1,presWx"),

	/* alternative atmospheric and surface files */
	ATMO2("weather_sensor_alt1.csv",
		"sysid,Rpuid,Senid,DtTm,AirTemp,Dewpoint,Rh," +
		"SpdAvg,SpdGust,DirMin,DirAvg,DirMax," + 
		"Pressure,PcIntens,PcType,PcRate,Pc10Min," + 
		"Visibility,Pc1Hr,Pc3Hr,Pc6Hr,Pc12Hr,Pc24Hr," + 
		"Height,snowdepth,Situation,,"),
	SURF2("weather_sensor_alt2.csv",
		"Sysid,Rpuid,senid,DtTm,sfcond,sftemp," + 
		"frztemp,chemfactor,chempct,depth,icepct," + 
		"SubSfTemp,friction,IceWaterThickness,waterlevel,,");

	/** Missing value */
	static final String MSNG = "";
	static final String MSNG_101 = "101";
	static final String MSNG_32767 = "32767";
	static final String MSNG_65535 = "65535";

	/** Capitalize the specified string, e.g. "aBC" becomes "Abc" */
	static private String cap(String str) {
		if (str != null && str.length() >= 1) {
			return str.substring(0, 1).toUpperCase() + 
				str.substring(1).toLowerCase();
		} else {
			return MSNG;
		}
	}

	/** Remove trailing character */
	static StringBuilder delLastChar(StringBuilder sb) {
		if (sb != null) {
			int len = sb.length();
			if (len > 0)
				sb.setLength(len - 1);
		}
		return sb;
	}

	/** Return the specified date as a string in UTC.
	 * @param stamp A time stamp, null or < 0 for missing
	 * @return A string in UTC as MM/dd/yyyy HH:mm:ss */
	static private String formatDate(Long stamp) {
		if (stamp == null || stamp < 0)
			return MSNG;
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
			return MSNG;
		Date d = new Date(stamp);
		SimpleDateFormat sdf = 
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(d);
	}

	/** Return the specified date as a string in UTC.
	 * @param stamp A time stamp, null or < 0 for missing
	 * @return A string in UTC as MM/dd/yy HH:mm */
	static private String formatDate3(Long stamp) {
		if (stamp == null || stamp < 0)
			return MSNG;
		Date d = new Date(stamp);
		SimpleDateFormat sdf = 
			new SimpleDateFormat("MM/dd/yy HH:mm");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(d);
	}

	/** Get system id
	 * @return System id or the empty string for missing */
	static private String getSysId() {
		return "351"; //TODO get from system attribute
	}

	/** Convert an integer to string, ignoring any error values.
	 * @return Integer as a string or empty for missing */
	static private String intToCsv(Integer iv, String missing) {
		return (iv != null ? iv.toString() : missing);
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
			return MSNG;
		}
	}

    /** Convert an NTCIP temperature to CSV temperature.
 	 * @arg v base temperature
 	 * @return Temperature as hundredths of a degree C or 
 	 * 	   the empty string for missing */
	static private String essTempToCsv100(EssTemperature v, String missing) {
		return v.get(
			t -> String.valueOf(t.round(Temperature.Units.HUNDREDTH_CELSIUS)),
			missing
		);
	}

	/** Convert a temperature in C to a string.
	 * @arg tc NTCIP temp in C or null on error.
	 * @return Temperature in degrees C or empty string for missing */
	static private String tToCsv(Double tc) {
		return (tc != null ? SString.doubleToString(tc, 4) : MSNG);
	}

	/** Convert pavement surface status to CSV string
	 * @arg status - surface status
	 * @return Pavement surface status description or empty if missing. */
	static private String pssToN(SurfaceStatus status) {
		if (EssEnumType.isValid(status))
			return SString.splitCamel(status.toString());
		return MSNG;
	}

	/** Convert surface water depth to CSV string
	 * @arg row - row to fetch from
	 * @return Pavement surface water depth in .1 mm or empty if missing */
	static private String distanceTo10thMM(EssDistance v, String missing) {
        return v.get(
			// m -> 1/10 mm
			d -> String.valueOf(d.round(Distance.Units.TENTH_MILLIMETERS)),
			missing
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
			return MSNG;
	}

	/** Return precip rate in CSV units. See essPrecipRate.
	 * @return Precip rate as .025 mm/hr or empty for missing */
	static private String praToCsv(WeatherSensorImpl w) {
		Integer v = w.getPrecipRate(); // mm/hr or null
		return (v != null ? String.valueOf(v * 40): MSNG);
	}

	/** Convert precip rate to Pikalert CSV units. See essPrecipRate.
	 * @arg v Precip rate in mm/hr, null for missing.
	 * @return Precip rate in mm over 1 hour or empty for missing */
	static private String praToCsvPikalert(WeatherSensorImpl w) {
		Integer v = w.getPrecipRate(); // mm/hr or null
		return (v != null ? String.valueOf(v): MSNG);
	}

	/** Convert visibility to CSV units, see essVisibility.
	 * @arg w Weather sensor
	 * @return Distance in meters or empty for missing. */
	static private String visToCsv(WeatherSensorImpl w) {
		Integer v = w.getVisibility();
		return (v != null ? String.valueOf(v) : MSNG);
	}

	/** Convert precipitation accumulation to CSV units.
	 * @arg v Precip accum in mm, null for missing.
	 * @return Precip accumulation in .025 mm or empty for missing. 
	 *         See essPrecipitationOneHour */
	static private String pToCsv(Float v) {
		return (v != null ? 
			String.valueOf(Math.round(v * 40f)) : MSNG);
	}

	/** Convert speed to CSV units.
	 * @arg v Speed in KPH, null for missing.
	 * @return Speed in KPH or empty for missing. See essAvgWindSpeed. */
	static private String spToCsv(Integer v) {
		if (v != null)
			return String.valueOf(v);
		else
			return MSNG;
	}

	/** Convert speed to Pikalert CSV units.
	 * @arg v Speed in KPH, null for missing.
	 * @return Speed in m/s or empty for missing. See essAvgWindSpeed. */
	static private String spToPikalertCsv(Integer v) {
		if (v != null)
			return SString.doubleToString(v * .2777777778, 4);
		else
			return MSNG;
	}

	/** Get the precipitation situation */
	static private String psToCsv(WeatherSensorImpl w) {
		var ps = PrecipSituation.from(w);
		return (ps != null ? ps.desc_csv : MSNG);
	}

	/** Get the visibility situation as an integer or empty fo missing */
	static private String vsToCsv(WeatherSensorImpl w) {
		int ord = VisibilitySituation.from(w).ordinal();
		return (ord != 0 ? SString.intToString(ord) : MSNG);
	}

	/** Convert adjacent snow depth in cm to a string. A value of 
	 * 3001 is returned for missing. 
	 * @return Snow depth in cm, empty string if missing, 3001 on error */
	static private String asdToCsv(WeatherSensorImpl w, String missing) {
		return intToCsv(w.getAdjacentSnowDepth(), missing);
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
		return (elv != null ? Integer.toString(elv) : MSNG);
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
			return MSNG;
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

	/** Return a missing value of 101 */
	static public Integer missing101(Integer i) {
		return (i != null ? i : 101);
	}

	/** Return a missing value of 65535 */
	static public String missing65535(String str) {
		return (str != null && !str.isEmpty() ? str : MSNG_65535);
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

	/** Get record(s) to be written to the file.
	 * @return CSV line(s) with no trailing comma and no line 
	 * 	   terminator on the last line. */
	String getRecs(WeatherSensorImpl w) {
		StringBuilder sb = new StringBuilder();
		if (w != null) {
			if (this == ATMO)
				return getAtmoRec(w);
			else if (this == SURF)
				return getSurfRecs(w);
			else if (this == PIKA)
				return getPikaRec(w);
			else if (this == ATMO2)
				return getAtmoAltRec(w);
			else if (this == SURF2)
				return getSurfAltRecs(w);
			else
				System.err.println("wsfe logic error");
		}
		return sb.toString();
	}

	/** Get a record for the ATMO file type.
	 * @return CSV line with no trailing comma and no line terminator */
	String getAtmoRec(WeatherSensorImpl w) {
		StringBuilder sb = new StringBuilder();
		append(sb, w.getSiteId());		 //Siteid
		append(sb, formatDate(w.getStamp()));	 //DtTm
		append(sb, tIntToCsv100(w.getAirTemp()));//AirTemp
		append(sb, tIntToCsv100(
			w.getDewPointTemp()));		 //Dewpoint
		append(sb, w.getHumidity());		 //Rh
		append(sb, spToCsv(w.getWindSpeed()));	 //SpdAvg
		append(sb, spToCsv(
			w.getMaxWindGustSpeed()));	 //SpdGust
		append(sb, MSNG);			 //DirMin
		append(sb, w.getWindDir());		 //DirAvg
		append(sb, w.getMaxWindGustDir());	 //DirMax
		append(sb, prToCsv(w.getPressureQC()));	 //Pressure
		append(sb, cap(WeatherSensorHelper.
			getPrecipRateIntensity(w)));	 //PcIntens
		append(sb, psToCsv(w));			 //PcType
		append(sb, praToCsv(w));		 //PcRate
		append(sb, pToCsv(w.getPrecipOneHour()));//PcAccum 1h
		append(sb, visToCsv(w));		 //Visibility
		delLastChar(sb);			 //remove last comma
		return sb.toString();
	}

	/** Get recs for SURF file type.
	 * @return CSV lines with the last line having no trailing comma 
	 * 		and no line terminator */
	String getSurfRecs(WeatherSensorImpl w) {
		StringBuilder sb = new StringBuilder();
		String sid = w.getSiteId();
		int senid = 0;
		String dat = formatDate(w.getStamp());
		PavementSensorsTable ps_t = w.getPavementSensorsTable();
		SubSurfaceSensorsTable ss_t = w.getSubSurfaceSensorsTable();

		// Configuration for pairing pavement and subsurface sensors.
		// At some point this will be enabled for WYDOT.
		final boolean PAIR_SENSORS = false;

        // iterate through pavement sensor table
		for (var row: ps_t){
			String pss = pssToN(row.surface_status.get());
			String sft = essTempToCsv100(row.surface_temp, MSNG);
			String fzt = essTempToCsv100(row.freeze_point, MSNG);
			String sst = (PAIR_SENSORS ?
				essTempToCsv100(ss_t.getRow(row.number).temp, MSNG) : MSNG);
			String swd = distanceTo10thMM(row.water_depth, MSNG); // .1 mm
			sb.append(getSurfRec(sid, senid, dat, pss, sft, fzt, 
				sst, swd));
			++senid;
		}
		// iterate through subsurface sensors table
		final int irow = (PAIR_SENSORS ? ps_t.size() + 1 : 1);
		for (int r = irow; r <= ss_t.size(); ++r){
			var row = ss_t.getRow(r);
			String pss = MSNG;
			String sft = MSNG;
			String fzt = MSNG;
			String sst = essTempToCsv100(row.temp, MSNG);
			String swd = MSNG;
			sb.append(getSurfRec(sid, senid, dat, pss, sft, fzt,
				sst, swd));
			++senid;
		}
		delLastChar(sb); //remove trailing line terminator
		return sb.toString();
	}

	/** Get a sensor record for SURF file type.
	 * @return CSV line with no trailing comma and a trailing line term */
	private String getSurfRec(String sid, int senid, String dat, 
		String sfc, String sft, String fzt, String sst, 
		String swd)
	{
		String ssenid = String.valueOf(senid);
		StringBuilder sb = new StringBuilder();
		append(sb, sid);	//Siteid
		append(sb, ssenid);	//senid
		append(sb, dat);	//DtTm
		append(sb, sfc);	//sfcond
		append(sb, sft);	//sftemp
		append(sb, fzt);	//frztemp
		append(sb, MSNG);	//chemfactor
		append(sb, MSNG);	//chempct
		append(sb, swd);	//depth .1mm
		append(sb, MSNG);	//icepct
		append(sb, sst);	//subsftemp
		append(sb, MSNG);	//waterlevel
		delLastChar(sb);	//remove trailing comma
		sb.append('\n');
		return sb.toString();
	}

	/** Get record for PIKA file type.
	 * @return CSV line with no trailing comma and no trailing line term */
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
		delLastChar(sb);
		return sb.toString();
	}

	/** Get an alternative record for the ATMO file type.
	 * @return CSV line with no trailing comma and no line terminator */
	String getAtmoAltRec(WeatherSensorImpl w) {
		StringBuilder sb = new StringBuilder();
		append(sb, getSysId());				//1:1: Sysid
		append(sb, SString.stringToInt(w.getSiteId()));	//2:1: Rpuid
		append(sb, "0");				//3:1: Senid
		append(sb, formatDate3(w.getStamp()));		//4:2: DtTm
		append(sb, tIntToCsv100(w.getAirTemp()));	//5:3: AirTemp
		append(sb, tIntToCsv100(w.getDewPointTemp()));	//6:4: Dewpoint
		append(sb, missing101(w.getHumidity()));	//7:5: Rh
		append(sb, spToCsv(w.getWindSpeed()));		//8:6: SpdAvg
		append(sb, spToCsv(
			w.getMaxWindGustSpeed()));		//9:7: SpdGust
		append(sb, MSNG);				//10:8: DirMin
		append(sb, w.getWindDir());			//11:9: DirAvg
		append(sb, w.getMaxWindGustDir()); 		//12:10: DirMax
		append(sb, missing65535(
			prToCsv(w.getPressure())));		//13:11: Pres
		append(sb, cap(WeatherSensorHelper.
			getPrecipRateIntensity(w)));		//14:12: PcInten
		append(sb, psToCsv(w));				//15:13: PcType
		append(sb, praToCsv(w));			//16:14: PcRate
		append(sb, "-1");				//17:*: Pc10Min
		append(sb, visToCsv(w));			//18:16: Vis
		append(sb, pToCsv(w.getPrecipOneHour()));	//19: Pc1Hr
		append(sb, pToCsv(w.getPrecip3Hour()));		//20: Pc3Hr
		append(sb, pToCsv(w.getPrecip6Hour()));		//21: Pc6Hr
		append(sb, pToCsv(w.getPrecip12Hour()));	//22: Pc12Hr
		append(sb, pToCsv(w.getPrecip24Hour()));	//23: Pc24Hr
		append(sb, MSNG);				//24: Height?
		append(sb, asdToCsv(w, MSNG));			//25: snowdepth
		append(sb, vsToCsv(w));				//26: situation
		append(sb, MSNG);				//27: ?
		append(sb, MSNG);				//28: empty
		delLastChar(sb);
		return sb.toString();
	}

	/** Get recs for SURF file type.
	 * @return CSV lines with the last line having no trailing comma 
	 * 		and no line terminator */
	String getSurfAltRecs(WeatherSensorImpl w) {
		StringBuilder sb = new StringBuilder();
		String rpu = w.getSiteId();
		int senid = 0;
		String dat = formatDate(w.getStamp());
		// iterate through pavement sensor table
		PavementSensorsTable pst = w.getPavementSensorsTable();
		SubSurfaceSensorsTable ss_t = w.getSubSurfaceSensorsTable();
		for (var row : pst) {
			String pss = pssToN(row.surface_status.get());
			String sft = essTempToCsv100(row.surface_temp, MSNG_32767);
			String fzt = essTempToCsv100(row.freeze_point, MSNG_32767);
			String cfa = MSNG_101;
			String chp = MSNG_101;
			String swd = distanceTo10thMM(row.water_depth, MSNG_32767); // tenths mm
			String ice = MSNG_101;
			// use subsurf temp from paired subsurf sensor
			String sst = essTempToCsv100(ss_t.getRow(row.number).temp, MSNG);
			String fri = intToCsv(w.getFriction(), MSNG_101);
			String iwd = distanceTo10thMM(row.ice_or_water_depth, MSNG_65535);
			sb.append(getSurfAltRec(rpu, senid, dat, pss, sft, fzt,
				cfa, chp, swd, ice, sst, fri, iwd));
			++senid;
		}
		// iterate through subsurface sensor table
		for (var row : ss_t) {
			String pss = MSNG;
			String sft = MSNG;
			String fzt = MSNG_32767;
			String cfa = MSNG_101;
			String chp = MSNG_101;
			String swd = MSNG_32767;
			String ice = MSNG_101;
			String sst = essTempToCsv100(row.temp, MSNG);
			String fri = MSNG_101;
			String iwd = MSNG_65535;
			sb.append(getSurfAltRec(rpu, senid, dat, pss, sft,
				fzt, cfa, chp, swd, ice, sst, fri, iwd));
			++senid;
		}
		delLastChar(sb); //remove trailing line terminator
		return sb.toString();
	}

	/** Get a sensor record for an alternate SURF file type.
	 * @return CSV line with no trailing comma and a trailing line term */
	private String getSurfAltRec(String rpu, int sid, String dat, 
		String sfc, String sft, String fzt, String cfa, String chp, 
		String swd, String ice, String sst, String fri, String iwd)
	{
		StringBuilder sb = new StringBuilder();
		append(sb, getSysId());		// 1: Sysid
		append(sb, rpu);		// 2: Rpuid
		append(sb, sid);		// 3: Senid
		append(sb, dat);		// 4: DtTm
		append(sb, sfc);		// 5: sfcond
		append(sb, sft);		// 6: sftemp
		append(sb, fzt);		// 7: frztemp
		append(sb, cfa);		// 8: chemfactor
		append(sb, chp);		// 9: chempct
		append(sb, swd);		//10: depth .1mm
		append(sb, ice);		//11: icepct
		append(sb, sst);		//12: subsftemp
		append(sb, fri);		//13: friction
		append(sb, iwd);		//14: IceWaterThickness
		append(sb, MSNG);		//15: waterlevel, empty
		append(sb, MSNG);		//16: empty
		append(sb, MSNG);		//17: empty
		delLastChar(sb);		//remove trailing comma
		sb.append('\n');
		return sb.toString();
	}
}