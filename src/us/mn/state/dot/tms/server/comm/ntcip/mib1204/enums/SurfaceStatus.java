/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2017  Iteris Inc.
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
 * Pavement surface status as defined by essSurfaceStatus in NTCIP 1204.
 *
 * @author Michael Darter
 * @author Douglas Lau
 */
public enum SurfaceStatus {
	undefined,            // 0
	other,                // 1
	error,                // 2
	dry,                  // 3
	traceMoisture,        // 4
	wet,                  // 5
	chemicallyWet,        // 6
	iceWarning,           // 7
	iceWatch,             // 8
	snowWarning,          // 9
	snowWatch,            // 10
	absorption,           // 11
	dew,                  // 12
	frost,                // 13
	absorptionAtDewpoint; // 14

	/** Values array */
	static private final SurfaceStatus[] VALUES = values();

	/** Get a SurfaceStatus from an ordinal value */
	static public SurfaceStatus fromOrdinal(int o) {
		return (o >= 0 && o < VALUES.length) ? VALUES[o] : undefined;
	}

	/** Get an enum from an ordinal value */
	static public SurfaceStatus fromOrdinal(Integer o) {
		return (o != null ? fromOrdinal(o.intValue()) : undefined);
    }

	/** Get the surface status as an enum */
	static public SurfaceStatus from(WeatherSensor ws) {
		if (ws != null) {
			Integer ss = ws.getPvmtSurfStatus();
			if (ss != null)
				return fromOrdinal(ss);
		}
		return undefined;
	}

	/** Convert to string, with empty string if null/empty */
	static public String toStringValid(SurfaceStatus value){
		return toStringValid(value, "");
	}
	/** Convert to string, with custom string if null/empty
	 * @arg invalid - string to use if status is invalid */
	static public String toStringValid(SurfaceStatus value, String invalid){
		if (value != null && value != undefined)
			return value.toString();
		return invalid;
	}
}
