/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2017-2018  Iteris Inc.
 * Copyright (C) 2019-2022  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums;
import us.mn.state.dot.tms.WeatherSensor;

/**
 * Precipitation situation as defined by essPrecipSituation in NTCIP 1204.
 *
 * @author Michael Darter
 * @author Douglas Lau
 */
public enum PrecipSituation {
	undefined(""),                   				// 0
	other("None"),                       			// 1
	unknown("None"),                     			// 2
	noPrecipitation("None"),             			// 3
	unidentifiedSlight("Rain"),          			// 4
	unidentifiedModerate("Rain"),        			// 5
	unidentifiedHeavy("Rain"),           			// 6
	snowSlight("Snow"),                  			// 7
	snowModerate("Snow"),                			// 8
	snowHeavy("Snow"),                   			// 9
	rainSlight("Light"),                 			// 10
	rainModerate("Rain"),                			// 11
	rainHeavy("Rain"),                   			// 12
	frozenPrecipitationSlight("Freezing Rain"),	// 13
	frozenPrecipitationModerate("Freezing Rain"),	// 14
	frozenPrecipitationHeavy("Freezing Rain");		// 15

	/** Simple description for CSV export */
	public final String desc_csv;

	private PrecipSituation(String dcsv){
		desc_csv = dcsv;
	}

	/** Get an enum from an ordinal value */
	static public PrecipSituation fromOrdinal(int o) {
		if (o >= 0 && o < values().length)
			return values()[o];
		else
			return undefined;
	}

	/** Get the precipitation situation as an enum */
	static public PrecipSituation from(WeatherSensor ws) {
		if (ws != null) {
			Integer ps = ws.getPrecipSituation();
			if (ps != null)
				return fromOrdinal(ps);
		}
		return undefined;
	}

	/** Convert to string, with empty string if null/empty */
	static public String toStringValid(PrecipSituation value){
		return toStringValid(value, "");
	}
	/** Convert to string, with custom string if null/empty
	 * @arg invalid - string to use if status is invalid */
	static public String toStringValid(PrecipSituation value, String invalid){
		if (value != null && value != undefined)
			return value.toString();
		return invalid;
	}
}
