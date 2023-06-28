/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2009  AHMCT, University of California Davis
 * Copyright (C) 2009-2020  Minnesota Department of Transportation
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
package us.mn.state.dot.tms;

import java.util.Iterator;

/**
 * Helper for comm links.
 *
 * @author Michael Darter
 * @author Douglas Lau
 */
public class CommLinkHelper extends BaseHelper {

	/** Disallow instantiation */
	private CommLinkHelper() {
		assert false;
	}

	/** Get a comm link iterator */
	static public Iterator<CommLink> iterator() {
		return new IteratorWrapper<CommLink>(namespace.iterator(
			CommLink.SONAR_TYPE));
	}

	/** Get the polling enabled flag */
	static public boolean getPollEnabled(CommLink cl) {
		// If the user doesn't have permission to read CommLink stuff,
		// just pretend that polling is enabled
		return (cl != null && cl.getPollEnabled())
		    || !canRead(CommLink.SONAR_TYPE);
	}

	/** Enable or disable all comm links with an active controller */
	static public void enableActive(boolean enable) {
		Iterator<Controller> it = ControllerHelper.iterator();
		while (it.hasNext()) {
			Controller co = it.next();
			if (co != null) {
				CtrlCondition cc = CtrlCondition.fromOrdinal(
					co.getCondition());
				if (cc == CtrlCondition.ACTIVE) {
					CommLink cl = co.getCommLink();
					if (cl != null)
						cl.setPollEnabled(enable);
				}
			}
		}
	}

	/** Enable or disable all comm links */
	static public void enableAll(boolean enable) {
		Iterator<Controller> it = ControllerHelper.iterator();
		while (it.hasNext()) {
			Controller co = it.next();
			if (co != null) {
				CommLink cl = co.getCommLink();
				if (cl != null)
					cl.setPollEnabled(enable);
			}
		}
	}

	/** Lookup the CommLink with the specified name */
	static public CommLink lookup(String name) {
		return (CommLink) namespace.lookupObject(CommLink.SONAR_TYPE,
			name);
	}

	/** Get the protocol for a CommLink */
	static public CommProtocol getProtocol(CommLink cl) {
		return (cl != null)
		      ? CommConfigHelper.getProtocol(cl.getCommConfig())
		      : null;
	}
}
