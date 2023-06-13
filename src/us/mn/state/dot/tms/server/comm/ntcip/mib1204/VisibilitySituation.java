/*
 * IRIS -- Intelligent Roadway Information System
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
package us.mn.state.dot.tms.server.comm.ntcip.mib1204;
import us.mn.state.dot.tms.WeatherSensor;

/**
 * Visibility situation as defined by essVisibilitySituation in NTCIP 1204.
 *
 * @author Douglas Lau
 */
public enum VisibilitySituation {
	undefined,         // 0
	other,             // 1
	unknown,           // 2
	clear,             // 3
	fogNotPatchy,      // 4
	patchyFog,         // 5
	blowingSnow,       // 6
	smoke,             // 7
	seaSpray,          // 8
	vehicleSpray,      // 9
	blowingDustOrSand, // 10
	sunGlare,          // 11
	swarmOfInsects;    // 12

	/** Values array */
	static private final VisibilitySituation[] VALUES = values();

	/** Get a SurfaceStatus from an ordinal value */
	static public VisibilitySituation fromOrdinal(int o) {
		return (o >= 0 && o < VALUES.length) ? VALUES[o] : undefined;
	}

	/** Get an enum from an ordinal value */
	static public VisibilitySituation fromOrdinal(Integer o) {
		return (o != null ? fromOrdinal(o.intValue()) : undefined);
    }

	/** Get the surface status as an enum */
	static public VisibilitySituation from(WeatherSensor ws) {
		if (ws != null) {
			Integer val = ws.getVisibilitySituation();
			if (val != null)
				return fromOrdinal(val);
		}
		return undefined;
	}

	/** Convert to string, with empty string if null/empty */
	static public String toStringValid(VisibilitySituation value){
		return toStringValid(value, "");
	}
	/** Convert to string, with custom string if null/empty
	 * @arg invalid - string to use if status is invalid */
	static public String toStringValid(VisibilitySituation value, String invalid){
		if (value != null && value != undefined)
			return value.toString();
		return invalid;
	}
}
