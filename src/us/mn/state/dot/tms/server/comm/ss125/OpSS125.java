/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2009-2016  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.ss125;

import us.mn.state.dot.tms.server.ControllerImpl;
import us.mn.state.dot.tms.server.comm.OpController;
import us.mn.state.dot.tms.server.comm.PriorityLevel;

/**
 * Operation for SS125 device
 *
 * @author Douglas Lau
 * @author Michael Darter
 */
abstract public class OpSS125 extends OpController<SS125Property> {

	/** Create a new SS125 operation */
	protected OpSS125(PriorityLevel p, ControllerImpl c) {
		super(p, c);
	}

	/** Log a msg */
	protected void log(String msg) {
		if (SS125Poller.SS125_LOG.isOpen()) {
			SS125Poller.SS125_LOG.log(controller.getName() + 
				" " + msg);
		}
	}

	/** Log a message */
	protected void logError(String msg) {
		if (SS125Poller.SS125_LOG.isOpen())
			SS125Poller.SS125_LOG.log(controller.getName() + 
				"! " + msg);
	}

}
