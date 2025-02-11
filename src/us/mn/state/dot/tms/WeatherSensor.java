/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2010-2022  Minnesota Department of Transportation
 * Copyright (C) 2011  AHMCT, University of California
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
package us.mn.state.dot.tms;

/**
 * A weather sensor is a device for sampling weather data, such as precipitation
 * rates, visibility and wind speed.
 *
 * @author Douglas Lau
 * @author Michael Darter
 */
public interface WeatherSensor extends Device {

	/** SONAR type name */
	String SONAR_TYPE = "weather_sensor";

	/** Get the device location */
	GeoLoc getGeoLoc();

	/** Get the site id */
	String getSiteId();

	/** Set the site id */
	void setSiteId(String sid);

	/** Get the alt id */
	String getAltId();

	/** Set the alt id */
	void setAltId(String aid);

	/** Get air temp in C (null for missing) */
	Integer getAirTemp();

	/** Get dew point temp in C (null for missing) */
	Integer getDewPointTemp();

	/** Get max temp in C (null for missing) */
	Integer getMaxTemp();

	/** Get min temp in C (null for missing) */
	Integer getMinTemp();

	/** Get wind speed in KPH (null for missing) */
	Integer getWindSpeed();

	/** Get average wind direction in degrees (null for missing).
	 * Wind direction is the direction the wind is blowing measured 
	 * clockwise from true North, as defined by NTCIP 1204. */
	Integer getWindDir();

	/** Get wind gust speed in KPH (null for missing) */
	Integer getMaxWindGustSpeed();

	/** Get wind gust direction in degrees (null for missing).
	 * Wind direction is the direction the wind is blowing measured 
	 * clockwise from true North, as defined by NTCIP 1204. */
	Integer getMaxWindGustDir();

	/** Get spot wind direction in degrees (null for missing).
	 * Wind direction is the direction the wind is blowing measured 
	 * clockwise from true North, as defined by NTCIP 1204. */
	Integer getSpotWindDir();

	/** Get spot wind speed in KPH (null for missing) */
	Integer getSpotWindSpeed();

	/** Get precipitation rate in mm/hr (null for missing) */
	Integer getPrecipRate();

	/** Get precip situation essPrecipSituation (null for missing) */
	Integer getPrecipSituation();

	/** Get precipitation accumulation in mm (null for missing) */
	Float getPrecipOneHour();
	Float getPrecip3Hour();
	Float getPrecip6Hour();
	Float getPrecip12Hour();
	Float getPrecip24Hour();

	/** Get visibility in meters (null for missing) */
	Integer getVisibility();

	/** Get visibility situation code essVisibilitySituation 
	 * (null for missing) */
	Integer getVisibilitySituation();

	/** Get cloud cover situation (null for missing) */
	Integer getCloudCoverSituation();

	/** Get water depth in cm (null for missing) */
	Integer getWaterDepth();

	/** Get the adjacent snow depth in cm (null for missing) */
	Integer getAdjacentSnowDepth();

	/** Get relative humidity as a percent (null for missing) */
	Integer getHumidity();

	/** Get friction as a percent 0 - 100 (null for missing) */
	Integer getFriction();

	/** Get the atmospheric pressure in pascals (null for missing) */
	Integer getPressure();

	/** Get the pavement temp 2-10 cm below surface (null for missing) */
	Integer getPvmtTemp();

	/** Get the surface temperature (null for missing) */
	Integer getSurfTemp();

	/** Get the pavement surface status (null for missing) */
	Integer getSurfaceStatus();

	/** Get the pavement surface freeze temperature (null for missing) */
	Integer getSurfFreezeTemp();

	/** Get the subsurface temperature (null for missing) */
	Integer getSubSurfTemp();

	/** Get the station elevation in meters (null for missing) */
	Integer getElevation();

	/** Get the pressure sensor height w.r.t. elevation (null for missing)*/
	Integer getPressureSensorHeight();

	/** Get the latest sample time stamp */
	Long getStamp();
}
