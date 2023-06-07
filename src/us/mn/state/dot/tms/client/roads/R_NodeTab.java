/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2006-2014  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.client.roads;

import java.awt.BorderLayout;
import java.util.Calendar;
import us.mn.state.dot.sched.Job;
import us.mn.state.dot.sched.Scheduler;
import us.mn.state.dot.tms.R_Node;
import us.mn.state.dot.tms.client.MapTab;
import us.mn.state.dot.tms.client.Session;

/**
 * The R_NodeTab class provides the GUI for editing roadway nodes.
 *
 * @author Douglas Lau
 * @author Michael Darter
 */
public class R_NodeTab extends MapTab<R_Node> {

	/** R_Node panel */
	private final R_NodePanel panel;

	/** Corridor list */
	private final CorridorList clist;

	/** Corridor list updater thread */
	static private final Scheduler CLIST_UPDATER = 
		new Scheduler("clist_updater");

	/** Seconds to offset each read from start of interval */
	static public final int OFFSET_SECS = SensorReader.OFFSET_SECS + 5;

	/** Job to perform */
	private final Job job = 
		new Job(Calendar.SECOND, 30, Calendar.SECOND, OFFSET_SECS)
	{
		public void perform() {
			if(clist != null)
				clist.updateCorridor();
		}
	};

	/** Create a new roadway node tab */
	public R_NodeTab(Session session, R_NodeManager man) {
		super(man);
		panel = new R_NodePanel(session);
		clist = new CorridorList(session, man, panel);
		add(panel, BorderLayout.NORTH);
		add(clist, BorderLayout.CENTER);
		CLIST_UPDATER.addJob(job);
	}

	/** Initialize the roadway node tab */
	@Override
	public void initialize() {
		panel.initialize();
		clist.initialize();
	}

	/** Dispose of the roadway tab */
	@Override
	public void dispose() {
		super.dispose();
		clist.dispose();
		panel.dispose();
		CLIST_UPDATER.removeJob(job);
	}

	/** Get the tab ID */
	@Override
	public String getTabId() {
		return "r_node";
	}
}
