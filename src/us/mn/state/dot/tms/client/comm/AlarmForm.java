/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2008-2014  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.client.comm;

import us.mn.state.dot.tms.Alarm;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.ProxyTableForm2;
import us.mn.state.dot.tms.utils.I18N;

/**
 * A form for displaying and editing alarms
 *
 * @author Douglas Lau
 */
public class AlarmForm extends ProxyTableForm2<Alarm> {

	/** Check if the user is permitted to use the form */
	static public boolean isPermitted(Session s) {
		return s.canRead(Alarm.SONAR_TYPE);
	}

	/** Create a new alarm form */
	public AlarmForm(Session s) {
		super(I18N.get("alarm.plural"), new AlarmModel(s));
	}
}
