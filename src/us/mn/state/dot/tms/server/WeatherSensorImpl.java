/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2010-2022  Minnesota Department of Transportation
 * Copyright (C) 2017-2021  Iteris Inc.
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.sql.ResultSet;
import java.sql.SQLException;
import us.mn.state.dot.sched.DebugLog;
import us.mn.state.dot.sonar.SonarException;
import us.mn.state.dot.tms.DeviceRequest;
import us.mn.state.dot.tms.GeoLoc;
import us.mn.state.dot.tms.GeoLocHelper;
import us.mn.state.dot.tms.TMSException;
import us.mn.state.dot.tms.WeatherSensor;
import us.mn.state.dot.tms.WeatherSensorHelper;
import us.mn.state.dot.tms.geo.Position;
import us.mn.state.dot.tms.units.Pressure;
import us.mn.state.dot.tms.utils.SString;
import us.mn.state.dot.tms.utils.XmlBuilder;
import static us.mn.state.dot.tms.server.Constants.MISSING_DATA;
import us.mn.state.dot.tms.server.comm.DevicePoller;
import us.mn.state.dot.tms.server.comm.WeatherPoller;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.PavementSensorsTable;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.SubSurfaceSensorsTable;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.VisibilitySituation;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.EssType;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.PrecipSituation;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SurfaceStatus;

/**
 * A weather sensor is a device for sampling weather data, such as 
 * precipitation rates, visibility, wind speed, etc. Weather sensor
 * drivers support:
 *   Optical Scientific ORG-815 optical rain gauge
 *   SSI CSV file interface
 *   Campbell Scientific CR1000 V27.05
 *   Vaisala dmc586 2.4.16
 *   QTT LX-RPU Elite Model Version 1.23
 *
 * @author Douglas Lau
 * @author Michael Darter
 */
public class WeatherSensorImpl extends DeviceImpl implements WeatherSensor {

	/** Debug log */
	static private final DebugLog LOG = new DebugLog("weather_sensor");

	/** Sample period for weather sensors (seconds) */
	static private final int SAMPLE_PERIOD_SEC = 60;

	/** Sample period for weather sensors (ms) */
	static private final int SAMPLE_PERIOD_MS = SAMPLE_PERIOD_SEC * 1000;

	/** Pavement sensors table, never null */
	private PavementSensorsTable ps_table = new PavementSensorsTable();

	/** Subsurface sensors table, never null */
	private SubSurfaceSensorsTable ss_table = new SubSurfaceSensorsTable();

	/** Round an integer to the nearest 45 */
	static private Integer round45(Integer d) {
		if (d != null)
			return 45 * Math.round(d / 45.0f);
		else
			return null;
	}

	/** Load all the weather sensors */
	static protected void loadAll() throws TMSException {
		namespace.registerType(SONAR_TYPE, WeatherSensorImpl.class);
		store.query("SELECT name, geo_loc, controller, pin, notes, " +
			"site_id, alt_id FROM iris." + SONAR_TYPE + ";", 
			new ResultFactory()
		{
			public void create(ResultSet row) throws Exception {
				namespace.addObject(new WeatherSensorImpl(row));
			}
		});
	}

	/** Return a value, given a key=value pair embedded in a string.
	 * @param field A string containing 0 or more key=value 
	 * 		pairs, never null.
	 * @param key A string key corresponding to the returned 
	 * 		value, never null.
	 * @param dvalue The default value returned if the key is not found.
	 * @return The value X extracted from the key value pair in the
	 * 	   form "key=X", otherwise the default */
	static private String parse(String field, String key, String dvalue) {
		if (field == null || field.isEmpty())
			return dvalue;
		if (key == null || key.isEmpty())
			return dvalue;
		field = SString.stripCrLf(field);
		String[] words = field.split(" ");
		if (words == null)
			return dvalue;
		key += "=";
		for (String w : words) {
			//if (!w.contains(key))
			//if (!w.equals(key))
			if (!w.startsWith(key))
				continue;
			String[] kv = w.split("=");
			if (kv == null || kv.length != 2)
				continue;
			return kv[1].trim();
		}
		return dvalue;
	}

	/** Get a mapping of the columns */
	@Override
	public Map<String, Object> getColumns() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("name", name);
		map.put("geo_loc", geo_loc);
		map.put("controller", controller);
		map.put("pin", pin);
		map.put("notes", notes);
		map.put("site_id", site_id);
		map.put("alt_id", alt_id);
		return map;
	}

	/** Log a message */
	void logMsg(String msg) {
		if (LOG.isOpen()) {
			StringBuilder sb = new StringBuilder();
			sb.append("WeatherSensorImpl: ").append(getName()).
				append(": ").append(msg);
			LOG.log(sb.toString());
		}
	}

	/** Get the database table name */
	@Override
	public String getTable() {
		return "iris." + SONAR_TYPE;
	}

	/** Get the SONAR type name */
	@Override
	public String getTypeName() {
		return SONAR_TYPE;
	}

	/** Create a weather sensor */
	private WeatherSensorImpl(ResultSet row) throws SQLException {
		this(row.getString(1),		// name
		     row.getString(2),		// geo_loc
		     row.getString(3),		// controller
		     row.getInt(4),		// pin
		     row.getString(5),		// notes
		     row.getString(6),		// site_id
		     row.getString(7)		// alt_id
		);
	}

	/** Create a weather sensor */
	private WeatherSensorImpl(String n, String l, String c, int p,
		String nt, String sid, String aid)
	{
		super(n, lookupController(c), p, nt);
		site_id = sid;
		alt_id = aid;
		geo_loc = lookupGeoLoc(l);
		cache = new PeriodicSampleCache(PeriodicSampleType.PRECIP_RATE);
		pt_cache = new PeriodicSampleCache(
			PeriodicSampleType.PRECIP_TYPE);
		settings = null;
		initTransients();
	}

	/** Create a new weather sensor with a string name */
	public WeatherSensorImpl(String n) throws TMSException, SonarException {
		super(n);
		GeoLocImpl g = new GeoLocImpl(name, SONAR_TYPE);
		g.notifyCreate();
		geo_loc = g;
		cache = new PeriodicSampleCache(PeriodicSampleType.PRECIP_RATE);
		pt_cache = new PeriodicSampleCache(
			PeriodicSampleType.PRECIP_TYPE);
	}

	/** Destroy an object */
	@Override
	public void doDestroy() throws TMSException {
		super.doDestroy();
		geo_loc.notifyRemove();
	}

	/** Device location */
	private GeoLocImpl geo_loc;

	/** Get the device location */
	@Override
	public GeoLoc getGeoLoc() {
		return geo_loc;
	}

	/** Site id (null for missing) */
	private transient String site_id;

	/** Get the site id (null for missing) */
	@Override
	public String getSiteId() {
		return site_id;
	}

	/** Set the site id.
	 * @param sid Site id (null for missing) */
	public void setSiteId(String sid) {
		site_id = sid;
	}

	/** Set the site id.
	 * @param sid Site id (null for missing) */
	public void doSetSiteId(String sid) throws TMSException {
		if (!objectEquals(sid, site_id)) {
			store.update(this, "site_id", sid);
			setSiteId(sid);
		}
	}

	/** Get the pikalert site id from the notes field.
	 * @return The string X extracted from the word "psiteid=X" 
	 * 	   within the notes field else the siteid else the 
	 * 	   name. If the specified pikalert site id is not
	 * 	   between 4 and 6 chars an error message is logged */
	public String getPikalertSiteId() {
		final String PALERT_SITEID_KEY = "psiteid";
		String pid = parse(getNotes(), PALERT_SITEID_KEY, getSiteId());
		pid = (pid != null ? pid : "");
		if (pid.length() < 4 || pid.length() > 6) {
			logMsg("WeatherSensorImpl:" +
				" Ignored Pikalert site" + 
				" id for weather sensor " + getName() + 
				"(" + pid + "), width is not 4-6 chars.");
		}
		return pid;
 	}

	/** Alt id (null for missing) */
	private transient String alt_id;

	/** Get the alt id (null for missing) */
	@Override
	public String getAltId() {
		return alt_id;
	}

	/** Set the alt id.
 	 * @param aid Alt id (null for missing) */
	public void setAltId(String aid) {
		alt_id = aid;
	}

	/** Set the alt id.
	 * @param aid (null for missing) */
	public void doSetAltId(String aid) throws TMSException {
		if (!objectEquals(aid, alt_id)) {
			store.update(this, "alt_id", aid);
			setAltId(aid);
		}
	}

	/** Air temp in C (null for missing) */
	private transient Integer air_temp;

	/** Get the air temp in C (null if missing) */
	@Override
	public Integer getAirTemp() {
		return air_temp;
	}

	/** Set the air temperature.
	 * @param at Air temperature in Celsius (null for missing) */
	public void setAirTempNotify(Integer at) {
		if (!objectEquals(at, air_temp)) {
			air_temp = at;
			notifyAttribute("airTemp");
		}
	}

	/** Water depth in cm (null for missing) */
	private transient Integer water_depth;

	/** Get the water depth in cm (null if missing) */
	@Override
	public Integer getWaterDepth() {
		return water_depth;
	}

	/** Set the water depth.
	 * @param wad Water depth in cm or null for missing */
	public void setWaterDepthNotify(Integer wad) {
		if (!objectEquals(wad, water_depth)) {
			water_depth = wad;
			notifyAttribute("waterDepth");
		}
	}

	/** Adjacent snow depth in cm (null for missing) */
	private transient Integer adjacent_snow_depth;

	/** Get the adjacent snow depth cm (null if missing) */
	@Override
	public Integer getAdjacentSnowDepth() {
		return adjacent_snow_depth;
	}

	/** Set the adjacent snow depth.
	 * @param asd Adjacent snow depth in cm or null for missing */
	public void setAdjacentSnowDepthNotify(Integer asd) {
		if (!objectEquals(asd, adjacent_snow_depth)) {
			adjacent_snow_depth = asd;
			notifyAttribute("adjacentSnowDepth");
		}
	}

	/** Humidity as a percentage (null for missing) */
	private transient Integer humidity;

	/** Get the humidity as a percentage (null if missing) */
	@Override
	public Integer getHumidity() {
		return humidity;
	}

	/** Set the humidity.
	 * @param hu Humidity as a percentage or null for missing */
	public void setHumidityNotify(Integer hu) {
		// humidity of 0 is impossible and considered an error
		if (hu != null && hu == 0)
			hu = null;
		if (!objectEquals(hu, humidity)) {
			humidity = hu;
			notifyAttribute("humidity");
		}
	}

	/** Dew point temperature in C (null for missing) */
	private transient Integer dew_point_temp;

	/** Get the dew point temp in C (null if missing) */
	@Override
	public Integer getDewPointTemp() {
		return dew_point_temp;
	}

	/** Set the dew point temp in C.
	 * @param dp Dew point temperature in C (null for missing) */
	public void setDewPointTempNotify(Integer dp) {
		if (!objectEquals(dp, dew_point_temp)) {
			dew_point_temp = dp;
			notifyAttribute("dewPointTemp");
		}
	}

	/** Max temperature in C (null for missing) */
	private transient Integer max_temp;

	/** Get the max temperature in C (null if missing) */
	@Override
	public Integer getMaxTemp() {
		return max_temp;
	}

	/** Set the max temperature in C
	 * @param mt Max temperature in C (null for missing) */
	public void setMaxTempNotify(Integer mt) {
		if (!objectEquals(mt, max_temp)) {
			max_temp = mt;
			notifyAttribute("maxTemp");
		}
	}

	/** Min temperature in C (null for missing) */
	private transient Integer min_temp;

	/** Get the min temperature in C (null if missing) */
	@Override
	public Integer getMinTemp() {
		return min_temp;
	}

	/** Set the min temperature in C
	 * @param mt Min temperature in C (null for missing) */
	public void setMinTempNotify(Integer mt) {
		if (!objectEquals(mt, min_temp)) {
			min_temp = mt;
			notifyAttribute("minTemp");
		}
	}

	/** Wind speed in KPH (null if missing) */
	private transient Integer wind_speed;

	/** Get the wind speed in KPH (null if missing) */
	@Override
	public Integer getWindSpeed() {
		return wind_speed;
	}

	/** Set the average wind speed in KPH
	 * @param ws Wind speed in KPH or null if missing */
	public void setWindSpeedNotify(Integer ws) {
		if (!objectEquals(ws, wind_speed)) {
			wind_speed = ws;
			notifyAttribute("windSpeed");
		}
	}

	/** Max wind gust speed in KPH (null if missing) */
	private transient Integer max_wind_gust_speed;

	/** Get the max wind gust speed in KPH (null if missing) */
	@Override
	public Integer getMaxWindGustSpeed() {
		return max_wind_gust_speed;
	}

	/** Set the max wind gust speed in KPH
	 * @param ws Wind gust speed in KPH or null if missing */
	public void setMaxWindGustSpeedNotify(Integer ws) {
		if (!objectEquals(ws, max_wind_gust_speed)) {
			max_wind_gust_speed = ws;
			notifyAttribute("maxWindGustSpeed");
		}
	}

	/** Max wind gust direction in degress (null if missing) */
	private transient Integer max_wind_gust_dir;

	/** Get the max wind gust direction in degrees (null if missing) */
	@Override
	public Integer getMaxWindGustDir() {
		return max_wind_gust_dir;
	}

	/** Set the max wind gust direction in degrees
	 * @param wgd Max wind gust direction in degress or null if missing */
	public void setMaxWindGustDirNotify(Integer wgd) {
		if (!objectEquals(wgd, max_wind_gust_dir)) {
			max_wind_gust_dir = wgd;
			notifyAttribute("maxWindGustDir");
		}
	}

	/** Average wind direction in degrees (null for missing) */
	private transient Integer wind_dir;

	/** Get the average wind direction.
	 * @return Wind direction in degrees (null for missing) */
	@Override
	public Integer getWindDir() {
		return wind_dir;
	}

	/** Set the average wind direction.
	 * @param wd Wind direction in degrees (null for missing) */
	public void setWindDirNotify(Integer wd) {
		if (!objectEquals(wd, wind_dir)) {
			wind_dir = wd;
			notifyAttribute("windDir");
		}
	}

	/** Set the average wind direction and round to the nearest 45 degs.
	 * @param wd Wind direction in degrees (null for missing) */
	public void setWindDirRoundNotify(Integer wd) {
		setWindDirNotify(round45(wd));
	}

	/** Spot wind direction in degrees (null for missing) */
	private transient Integer spot_wind_dir;

	/** Get spot wind direction.
	 * @return Spot wind direction in degrees (null for missing) */
	@Override
	public Integer getSpotWindDir() {
		return spot_wind_dir;
	}

	/** Set spot wind direction.
	 * @param swd Spot wind direction in degrees (null for missing) */
	public void setSpotWindDirNotify(Integer swd) {
		if (!objectEquals(swd, spot_wind_dir)) {
			spot_wind_dir = swd;
			notifyAttribute("spotWindDir");
		}
	}

	/** Spot wind speed in KPH (null for missing) */
	private transient Integer spot_wind_speed;

	/** Get spot wind speed.
	 * @return Spot wind speed in degrees (null for missing) */
	@Override
	public Integer getSpotWindSpeed() {
		return spot_wind_speed;
	}

	/** Set spot wind speed.
	 * @param sws Spot wind speed in KPH (null for missing) */
	public void setSpotWindSpeedNotify(Integer sws) {
		if (!objectEquals(sws, spot_wind_speed)) {
			spot_wind_speed = sws;
			notifyAttribute("spotWindSpeed");
		}
	}

	/** Cache for precipitation samples */
	private transient final PeriodicSampleCache cache;

	/** Cache for precipitation type samples */
	private transient final PeriodicSampleCache pt_cache;

	/** Accumulation of precipitation (micrometers) */
	private transient int accumulation = MISSING_DATA;

	/** Set the accumulation of precipitation (micrometers) */
	public void updateAccumulation(Integer a, long st) {
		int per_sec = calculatePeriod(st);
		int value = calculatePrecipValue(a);
		if (per_sec > 0 && value >= 0) {
			cache.add(new PeriodicSample(st, per_sec, value), name);
			float per_h = 3600f / per_sec;  // periods per hour
			float umph = value * per_h;     // micrometers per hour
			float mmph = umph / 1000;       // millimeters per hour
			setPrecipRateNotify(Math.round(mmph));
		}
		if (value < 0)
			setPrecipRateNotify(null);
		if (per_sec > 0 || value < 0)
			accumulation = a != null ? a : MISSING_DATA;
	}

	/** Reset the precipitation accumulation */
	public void resetAccumulation() {
		accumulation = 0;
	}

	/** Calculate the period since the last recorded sample.  If
	 * communication is interrupted, this will allow accumulated
	 * precipitation to be spread out over the appropriate samples. */
	private int calculatePeriod(long now) {
		if (stamp != null && now >= stamp) {
			int n = (int) (now / SAMPLE_PERIOD_MS);
			int s = (int) (stamp / SAMPLE_PERIOD_MS);
			return (n - s) * SAMPLE_PERIOD_SEC;
		} else
			return 0;
	}

	/** Calculate the precipitation since the last recorded sample.
	 * @param a New accumulated precipitation. */
	private int calculatePrecipValue(Integer a) {
		if (a != null && accumulation >= 0) {
			int val = a - accumulation;
			if (val >= 0)
				return val;
		}
		return MISSING_DATA;
	}

	/** Precipitation rate in mm/hr (null for missing) */
	private transient Integer precip_rate;

	/** Get precipitation rate in mm/hr (null for missing) */
	@Override
	public Integer getPrecipRate() {
		return precip_rate;
	}

	/** Set precipitation rate in mm/hr (null for missing) */
	public void setPrecipRateNotify(Integer pr) {
		if (!objectEquals(pr, precip_rate)) {
			precip_rate = pr;
			notifyAttribute("precipRate");
		}
	}

	/** Set the type of precipitation */
	public void setPrecipitationType(PrecipitationType pt, long st) {
		pt_cache.add(new PeriodicSample(st, SAMPLE_PERIOD_SEC,
			pt.ordinal()), name);
	}

	/** Precipitation situation (null for missing) */
	private transient Integer precip_situation;

	/** Get precipitation situation (null for missing) */
	@Override
	public Integer getPrecipSituation() {
		return precip_situation;
	}

	/** Set precipitation situation (null for missing) */
	public void setPrecipSituationNotify(Integer prs) {
		if (!objectEquals(prs, precip_situation)) {
			precip_situation = prs;
			notifyAttribute("precipSituation");
		}
	}

	/** Precipitation accumulation 1h in mm (null for missing) */
	private transient Float precip_one_hour;

	/** Get precipitation 1h in mm (null for missing) */
	@Override
	public Float getPrecipOneHour() {
		return precip_one_hour;
	}

	/** Set precipitation 1h in mm (null for missing) */
	public void setPrecipOneHourNotify(Float pr) {
		if (!objectEquals(pr, precip_one_hour)) {
			precip_one_hour = pr;
			notifyAttribute("precipOneHour");
		}
	}

	/** Precipitation accumulation 3h in mm (null for missing) */
	private transient Float precip_3_hour;

	/** Get precipitation 3h in mm (null for missing) */
	@Override
	public Float getPrecip3Hour() {
		return precip_3_hour;
	}

	/** Set precipitation 3h in mm (null for missing) */
	public void setPrecip3HourNotify(Float pr) {
		if (!objectEquals(pr, precip_3_hour)) {
			precip_3_hour = pr;
			notifyAttribute("precip3Hour");
		}
	}

	/** Precipitation accumulation 6h in mm (null for missing) */
	private transient Float precip_6_hour;

	/** Get precipitation 6h in mm (null for missing) */
	@Override
	public Float getPrecip6Hour() {
		return precip_6_hour;
	}

	/** Set precipitation 6h in mm (null for missing) */
	public void setPrecip6HourNotify(Float pr) {
		if (!objectEquals(pr, precip_6_hour)) {
			precip_6_hour = pr;
			notifyAttribute("precip6Hour");
		}
	}

	/** Precipitation accumulation 12h in mm (null for missing) */
	private transient Float precip_12_hour;

	/** Get precipitation 12h in mm (null for missing) */
	@Override
	public Float getPrecip12Hour() {
		return precip_12_hour;
	}

	/** Set precipitation 12h in mm (null for missing) */
	public void setPrecip12HourNotify(Float pr) {
		if (!objectEquals(pr, precip_12_hour)) {
			precip_12_hour = pr;
			notifyAttribute("precip12Hour");
		}
	}

	/** Precipitation accumulation 24h in mm (null for missing) */
	private transient Float precip_24_hour;

	/** Get precipitation 24h in mm (null for missing) */
	@Override
	public Float getPrecip24Hour() {
		return precip_24_hour;
	}

	/** Set precipitation 24h in mm (null for missing) */
	public void setPrecip24HourNotify(Float pr) {
		if (!objectEquals(pr, precip_24_hour)) {
			precip_24_hour = pr;
			notifyAttribute("precip24Hour");
		}
	}

	/** Visiblity in meters (null for missing) */
	private transient Integer visibility_m;

	/** Get visibility in meters (null for missing) */
	@Override
	public Integer getVisibility() {
		return visibility_m;
	}

	/** Set visibility in meters (null for missing) */
	public void setVisibilityNotify(Integer v) {
		if (!objectEquals(v, visibility_m)) {
			visibility_m = v;
			notifyAttribute("visibility");
		}
	}

	/** Visiblity situation (null for missing) */
	private transient Integer vis_situ;

	/** Get visibility situation (null for missing) */
	@Override
	public Integer getVisibilitySituation() {
		return vis_situ;
	}

	/** Set visibility in meters (null for missing) */
	public void setVisibilitySituationNotify(Integer vs) {
		if (!objectEquals(vs, vis_situ)) {
			vis_situ = vs;
			notifyAttribute("visibilitySituation");
		}
	}


	/** Cloud cover situation (null for missing) */
	private transient Integer cloud_cover_situation;

	/** Get cloud cover situation (null for missing) */
	@Override
	public Integer getCloudCoverSituation() {
		return cloud_cover_situation;
	}

	/** Set visibility in meters (null for missing) */
	public void setCloudCoverSituationNotify(Integer ccs) {
		if (!objectEquals(ccs, cloud_cover_situation)) {
			cloud_cover_situation = ccs;
			notifyAttribute("CloudCoverSituation");
		}
	}

	/** Atmospheric pressure in pascals (null for missing) */
	private transient Integer pressure;

	/** Get atmospheric pressure in pascals (null for missing) */
	@Override
	public Integer getPressure() {
		return pressure;
	}

	/** Get QC atmospheric pressure in pascals (null for missing) */
	public Integer getPressureQC() {
		if (pressure == null)
			return null;
		String sid = getSiteId();
		if ("178047".equals(sid)) // F Street
			return null;
		else
			return pressure;
	}

	/** Set atmospheric pressure in pascals (null for missing) */
	public void setPressureNotify(Integer v) {
		if (!objectEquals(v, pressure)) {
			pressure = v;
			notifyAttribute("pressure");
		}
	}

	/** Pavement surface temperature (null for missing) */
	private transient Integer pvmt_temp;

	/** Get pavement temp 2-10 cm below surface (null for missing) */
	@Override
	public Integer getPvmtTemp() {
		return pvmt_temp;
	}

	/** Set pavement surface temperature (null for missing) */
	public void setPvmtTempNotify(Integer v) {
		if (!objectEquals(v, pvmt_temp)) {
			pvmt_temp = v;
			notifyAttribute("pvmtTemp");
		}
	}

	/** Surface temperature (null for missing) */
	private transient Integer surf_temp;

	/** Get surface temperature (null for missing) */
	@Override
	public Integer getSurfTemp() {
		return surf_temp;
	}

	/** Get QC surface temperature (null for missing) */
	public Integer getSurfTempQC() {
		if (surf_temp == null)
			return null;
		String sid = getSiteId();
		if ("20100023".equals(sid)) // walcott junction
			return null;
		else
			return surf_temp;
	}

	/** Set surface temperature (null for missing) */
	public void setSurfTempNotify(Integer v) {
		if (!objectEquals(v, surf_temp)) {
			surf_temp = v;
			notifyAttribute("surfTemp");
		}
	}

	/** Pavement surface status (null for missing) */
	private transient Integer pvmt_surf_status;

	/** Get pavement surface status (null for missing) */
	@Override
	public Integer getSurfStatus() {
		return pvmt_surf_status;
	}

	/** Set pavement surface status (null for missing) */
	public void setSurfStatusNotify(Integer v) {
		if (!objectEquals(v, pvmt_surf_status)) {
			pvmt_surf_status = v;
			notifyAttribute("surfStatus");
		}
	}

	/** Pavement surface freeze point (null for missing) */
	private transient Integer surf_freeze_temp;

	/** Get pavement surface freeze temp (null for missing) */
	@Override
	public Integer getSurfFreezeTemp() {
		return surf_freeze_temp;
	}

	/** Set pavement surface freeze temperature (null for missing) */
	public void setSurfFreezeTempNotify(Integer v) {
		if (!objectEquals(v, surf_freeze_temp)) {
			surf_freeze_temp = v;
			notifyAttribute("surfFreezeTemp");
		}
	}

	/** Pavement subsurface temperature (null for missing) */
	private transient Integer subsurf_temp;

	/** Get subsurface temp (null for missing) */
	@Override
	public Integer getSubSurfTemp() {
		return subsurf_temp;
	}

	/** Set subsurface temperature (null for missing) */
	public void setSubSurfTempNotify(Integer v) {
		if (!objectEquals(v, subsurf_temp)) {
			subsurf_temp = v;
			notifyAttribute("subSurfTemp");
		}
	}

	/** Settings (JSON) read from sensors */
	private String settings;

	/** Set the JSON settings */
	public void setSettings(String s) {
		if (!objectEquals(s, settings)) {
			try {
				store.update(this, "settings", s);
				settings = s;
			}
			catch (TMSException e) {
				logError("settings: " + e.getMessage());
			}
		}
	}

	/** Set the current JSON sample */
	public void setSample(String s) {
		try {
			store.update(this, "sample", s);
		}
		catch (TMSException e) {
			logError("sample: " + e.getMessage());
		}
	}

	/** Station elevation in meters (null for missing) */
	private transient Integer elevation;

	/** Get station elevation in meters (null for missing) */
	@Override
	public Integer getElevation() {
		return elevation;
	}

	/** Set subsurface temperature (null for missing) */
	public void setElevationNotify(Integer e) {
		if (!objectEquals(e, elevation)) {
			elevation = e;
			notifyAttribute("elevation");
		}
	}

	/** Friction 0-100 (null for missing) */
	private transient Integer friction;

	/** Get friction (null for missing) */
	@Override
	public Integer getFriction() {
		return friction;
	}

	/** Set friction (null for missing) */
	public void setFrictionNotify(Integer fri) {
		if (!objectEquals(fri, friction)) {
			friction = fri;
			notifyAttribute("friction");
		}
	}

	/** Pressure sensor height in meters (null for missing) */
	private transient Integer pressure_sensor_height;

	/** Get pressure sensor height in meters (null for missing) */
	@Override
	public Integer getPressureSensorHeight() {
		return pressure_sensor_height;
	}

	/** Set pressure sensor height in meters (null for missing) */
	public void setPressureSensorHeightNotify(Integer h) {
		if (!objectEquals(h, pressure_sensor_height)) {
			pressure_sensor_height = h;
			notifyAttribute("pressureSensorHeight");
		}
	}

	/** Get pressure adjusted to sea-level
	 * @return Pressure in pascals or null on error */
	private Integer getSeaLevelPressure() {
		Pressure slp = WeatherSensorHelper.calcSeaLevelPressure(this);
		if (slp != null) {
			double pi = slp.convert(Pressure.Units.PASCALS).value;
			return Integer.valueOf((int)Math.round(pi));
		} else
			return null;
	}

	/** Time stamp from the last sample */
	private transient Long stamp;

	/** Get the time stamp from the last sample.
	 * @return Time as long */
	@Override
	public Long getStamp() {
		return stamp;
	}

	/** Get the time stamp as a string */
	public String getStampString() {
		Long ts = getStamp();
		return (ts != null ? new Date(ts).toString() : "");
	}

	/** Set the time stamp for the current sample */
	public void setStampNotify(Long s) {
		try {
			store.update(this, "sample_time", asTimestamp(s));
			stamp = s;
			notifyAttribute("stamp");
		}
		catch (TMSException e) {
			// FIXME: what else can we do with this exception?
			e.printStackTrace();
		}
	}

	/** ESS type for vendor specific functionality */
	private EssType ess_type = EssType.UNKNOWN;

	/** Set the ESS type */
	public void setType(EssType esst) {
		ess_type = esst;
	}

	/** Get the ESS type */
	public EssType getType() {
		return ess_type;
	}

	/** Set pavement sensors table */
	public void setPavementSensorsTable(PavementSensorsTable pst) {
		if (pst != null)
			ps_table = pst;
	}

	/** Get pavement sensors table */
	public PavementSensorsTable getPavementSensorsTable() {
		return ps_table;
	}

	/** Set subsurface sensors table */
	public void setSubSurfaceSensorsTable(SubSurfaceSensorsTable sst) {
		if (sst != null)
			ss_table = sst;
	}

	/** Get subsurface sensors table */
	public SubSurfaceSensorsTable getSubSurfaceSensorsTable() {
		return ss_table;
	}

	/** Get a weather sensor poller */
	private WeatherPoller getWeatherPoller() {
		DevicePoller dp = getPoller();
		return (dp instanceof WeatherPoller) ? (WeatherPoller) dp :null;
	}

	/** Send a device request operation */
	@Override
	protected void sendDeviceRequest(DeviceRequest dr) {
		WeatherPoller p = getWeatherPoller();
		if (p != null)
			p.sendRequest(this, dr);
	}

	/** Perform a periodic poll */
	@Override
	public void periodicPoll(boolean is_long) {
		if (!is_long)
			sendDeviceRequest(DeviceRequest.QUERY_STATUS);
	}

	/** Flush buffered sample data to disk */
	public void flush(PeriodicSampleWriter writer) {
		logMsg("flush buffered samples to disk");
		writer.flush(cache, name);
		writer.flush(pt_cache, name);
	}

	/** Purge all samples before a given stamp. */
	public void purge(long before) {
		logMsg("purge samples before " + new Date(before));
		cache.purge(before);
		pt_cache.purge(before);
	}

	/** Get a string representation of the object */
	public String toStringDebug() {
		StringBuilder sb = new StringBuilder();
		sb.append("(WeatherSensor: name=").append(name);
		sb.append(" siteid=").append(getSiteId());
		sb.append(" pikalertsiteid=").append(getSiteId());
		sb.append(" time_stamp=").append(getStampString());
		sb.append(" siteId=").append(getSiteId());
		sb.append(" altId=").append(getAltId());
		sb.append(" airTemp_c=").append(getAirTemp());
		sb.append(" dewPointTemp_c=").append(getDewPointTemp());
		sb.append(" maxTemp_c=").append(getMaxTemp());
		sb.append(" minTemp_c=").append(getMinTemp());
		sb.append(" avgWindSpeed_kph=").append(getWindSpeed());
		sb.append(" avgWindDir_degs=").append(getWindDir());
		sb.append(" maxWindGustSpeed_kph=").
			append(getMaxWindGustSpeed());
		sb.append(" maxWindGustDir_degs=").append(getMaxWindGustDir());
		sb.append(" spotWindDir_degs=").append(getSpotWindDir());
		sb.append(" spotWindSpeed_kph=").append(getSpotWindSpeed());
		sb.append(" precip_rate_mmhr=").append(getPrecipRate());
		sb.append(" precip_situation=").append(getPrecipSituation());
		sb.append(" precip_1h_mm=").append(getPrecipOneHour());
		sb.append(" precip_3h_mm=").append(getPrecip3Hour());
		sb.append(" precip_6h_mm=").append(getPrecip6Hour());
		sb.append(" precip_12h_mm=").append(getPrecip12Hour());
		sb.append(" precip_24h_mm=").append(getPrecip24Hour());
		sb.append(" visibility_m=").append(getVisibility());
		sb.append(" visibility_situation=").append(
			VisibilitySituation.from(this));
		sb.append(" water_depth_cm=").append(getWaterDepth());
		sb.append(" adj_snow_depth_cm=").append(getAdjacentSnowDepth());
		sb.append(" humidity_perc=").append(getHumidity());
		sb.append(" atmos_pressure_pa=").append(getPressure());
		sb.append(" atmos_pressure_qc_pa=").append(getPressureQC());
		sb.append(" atmos_pressure_sealevel_pa=").append(
			getSeaLevelPressure());
		sb.append(" pvmt_temp_c=").append(getPvmtTemp());
		sb.append(" surf_temp_c=").append(getSurfTemp());
		sb.append(" surf_temp_qc_c=").append(getSurfTempQC());
		sb.append(" pvmt_surf_status=").append(
			SurfaceStatus.from(this));
		sb.append(" surf_freeze_temp_c=").append(getSurfFreezeTemp());
		sb.append(" subsurf_temp_c=").append(getSubSurfTemp());
		sb.append(" cloud_cover_situation=").append(
			getCloudCoverSituation());
		sb.append(" friction").append(getFriction());
		sb.append(")");
		return sb.toString();
	}

	/** Write weather sensor configuration data as an XML element */
	public void writeXml(Writer w) throws IOException {
		var xb = new XmlBuilder(w).setPrettyPrint(true);
		xb.INDENT = ""; // disable indent currently
		xb.tag("weather_sensor")
			.attr("name", getName())
			.attr("description",GeoLocHelper.getLocation(geo_loc))
			.attr("notes", SString.stripCrLf(getNotes()))
			.attr("siteid", getSiteId())
			.attr("pikalertsiteid", getPikalertSiteId());
		Position pos = GeoLocHelper.getWgs84Position(geo_loc);
		var lon = (pos != null ? pos.getLongitude() : 0);
		var lat = (pos != null ? pos.getLongitude() : 0);
		xb.attr("lon", formatDouble(lon))
			.attr("lat", formatDouble(lat))
			.ancestor(0); // force write
		w.write('\n');
	}

	/** Write real-time weather data as an xml element */
	public void writeWeatherSensorDataXml(Writer w) throws IOException {
		var xb = new XmlBuilder(w).setPrettyPrint(true);
		xb.INDENT = ""; // disable indent currently
		// metadata
		// FIXME: would be better to not write metadata 
		//        but customers already depend on it.
		xb.tag("weather_sensor")
			.attr("name", getName())
			.attr("description",GeoLocHelper.getLocation(geo_loc))
			.attr("notes", SString.stripCrLf(getNotes()))
			.attr("siteid", getSiteId())
			.attr("pikalertsiteid", getPikalertSiteId());
		Position pos = GeoLocHelper.getWgs84Position(geo_loc);
		if (pos != null) {
			xb.attr("lon", formatDouble(pos.getLongitude()))
				.attr("lat", formatDouble(pos.getLatitude()));
		}
		// real-time data
		xb.attr("site_id", getSiteId())
			.attr("alt_id", getAltId())
			.attr("alt_m", getElevation())
			.attr("air_temp_c", getAirTemp())
			.attr("water_depth_cm", getWaterDepth())
			.attr("adjacent_snow_depth_cm", getAdjacentSnowDepth())
			.attr("humidity_perc", getHumidity())
			.attr("dew_point_temp_c", getDewPointTemp())
			.attr("max_temp_c", getMaxTemp())
			.attr("min_temp_c", getMinTemp())
			.attr("avg_wind_speed_kph", getWindSpeed())
			.attr("max_wind_gust_speed_kph", getMaxWindGustSpeed())
			.attr("max_wind_gust_dir_degs", getMaxWindGustDir())
			.attr("avg_wind_dir_degs", getWindDir())
			.attr("spot_wind_speed_kph", getSpotWindSpeed())
			.attr("spot_wind_dir_degs", getSpotWindDir())
			.attr("precip_rate_mmhr", getPrecipRate())
			.attr("precip_situation", PrecipSituation.from(this))
			.attr("precip_1h_mm", getPrecipOneHour())
			.attr("precip_3h_mm", getPrecip3Hour())
			.attr("precip_6h_mm", getPrecip3Hour())
			.attr("precip_12h_mm", getPrecip6Hour())
			.attr("precip_24h_mm", getPrecip12Hour())
			.attr("visibility_m", getVisibility())
			.attr("visibility_situation", VisibilitySituation.from(this))
			.attr("atmos_pressure_pa", getPressureQC())
			.attr("atmos_pressure_sealevel_pa", getSeaLevelPressure())
			.attr("pvmt_temp_c", getPvmtTemp())
			.attr("surf_temp_c", getSurfTempQC())
			.attr("pvmt_surf_status", SurfaceStatus.from(this))
			.attr("surf_freeze_temp_c", getSurfFreezeTemp())
			.attr("subsurf_temp_c", getSubSurfTemp())
			.attr("cloud_cover_situation", getCloudCoverSituation())
			.attr("friction", getFriction())
			.attr("n_pvmt_sensors", ps_table.size())
			.attr("n_subsurf_sensors", ss_table.size())
			.attr("time_stamp", getStampString());
		xb.child()
			.extend(ps_table)
			.extend(ss_table)
			.parent(true);
		w.write("\n");
	}
}
