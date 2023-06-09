/*
 * IRIS -- Intelligent Roadway Information System
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
package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.tms.server.ControllerImpl;

/**
 * ESS controller type, which is used to support controller 
 * specific functionality.
 *
 * @author Michael Darter
 */
public enum EssType {

	/** ESS Controller type */
	UNKNOWN(),
	GENERIC(),
	HIGH_SIERRA();

	/** Constructor */
	private EssType() {
	}

	/** Get an enum from an ordinal value */
	static public EssType fromOrdinal(int o) {
		if (o >= 0 && o < values().length)
			return values()[o];
		else
			return UNKNOWN;
	}

	/** Determine the ESS controller type using the associated 
	 * controller's version.
	 * @param co Associated controller
	 * @return ESS controller type or UKNOWN */
	static public EssType create(ControllerImpl co) {
		if (co == null)
			return UNKNOWN;
		String cver = co.getSetup("sys_descr");
		if (cver == null)
			return UNKNOWN;
		else if (cver.contains("ESS configurable on 1965"))
			return HIGH_SIERRA;
		else
			return GENERIC;
	}
}