/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2009  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.ss125;

import us.mn.state.dot.tms.server.ControllerImpl;
import us.mn.state.dot.tms.server.DebugLog;
import us.mn.state.dot.tms.server.comm.OpController;

/**
 * Operation for SS125 device
 *
 * @author Douglas Lau
 */
abstract public class OpSS125 extends OpController {

	/** SS 125 debug log */
	static protected final DebugLog SS125_LOG = new DebugLog("ss125");

	/** Create a new SS125 operation */
	protected OpSS125(int p, ControllerImpl c) {
		super(p, c);
	}
}
