/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2019-2022  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums;

/**
 * Pavement sensor type as defined by essPavementSensorType in NTCIP 1204.
 *
 * @author Douglas Lau
 * @author Michael Darter
 */
public enum PavementSensorType {
	undefined,      // 0
	other,          // 1
	contactPassive, // 2
	contactActive,  // 3
	infrared,       // 4
	radar,          // 5
	vibrating,      // 6
	microwave,      // 7
	laser;          // 8

	/** Values array */
	static private final PavementSensorType[] VALUES = values();

	/** Get a SurfaceStatus from an ordinal value */
	static public PavementSensorType fromOrdinal(int o) {
		return (o >= 0 && o < VALUES.length) ? VALUES[o] : undefined;
	}
}
