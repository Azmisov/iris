/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2018  Iteris Inc.
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
 * Road helper static methods.
 *
 * @author Michael Darter
 */
public class RoadHelper extends BaseHelper {

	/** Don't create any instances */
	private RoadHelper() {
		throw new UnsupportedOperationException("RoadHelper is static singleton");
	}

	/** Lookup */
	static public Road lookup(String name) {
		return (Road)namespace.lookupObject(Road.SONAR_TYPE, name);
	}
}
