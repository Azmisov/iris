/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2009-2012  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.client.lcs;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import us.mn.state.dot.sched.ActionJob;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.marking.LaneMarkingForm;
import us.mn.state.dot.tms.client.widget.SmartDesktop;
import us.mn.state.dot.tms.utils.I18N;

/**
 * LaneUseMenu is a menu for LCS-related items.
 *
 * @author Douglas Lau
 */
public class LaneUseMenu extends JMenu {

	/** User Session */
	protected final Session session;

	/** Desktop */
	protected final SmartDesktop desktop;

	/** Create a new lane use menu */
	public LaneUseMenu(final Session s) {
		super(I18N.get("lane.use"));
		session = s;
		desktop = s.getDesktop();
		JMenuItem item = createLcsItem();
		if(item != null)
			add(item);
		item = createGraphicItem();
		if(item != null)
			add(item);
		item = createLaneUseMultiItem();
		if(item != null)
			add(item);
		item = createLaneMarkingItem();
		if(item != null)
			add(item);
	}

	/** Create the LCS menu item */
	protected JMenuItem createLcsItem() {
		if(!LcsForm.isPermitted(session))
			return null;
		JMenuItem item = new JMenuItem(I18N.get("lcs"));
		item.setMnemonic('L');
		new ActionJob(item) {
			public void perform() throws Exception {
				desktop.show(new LcsForm(session));
			}
		};
		return item;
	}

	/** Create the graphics menu item */
	protected JMenuItem createGraphicItem() {
		if(!GraphicForm.isPermitted(session))
			return null;
		JMenuItem item = new JMenuItem(I18N.get("graphics"));
		new ActionJob(item) {
			public void perform() throws Exception {
				desktop.show(new GraphicForm(session));
			}
		};
		return item;
	}

	/** Create the lane-use MULTI menu item */
	protected JMenuItem createLaneUseMultiItem() {
		if(!LaneUseMultiForm.isPermitted(session))
			return null;
		JMenuItem item = new JMenuItem(I18N.get("lane.use.multi"));
		new ActionJob(item) {
			public void perform() throws Exception {
				desktop.show(new LaneUseMultiForm(session));
			}
		};
		return item;
	}

	/** Create the lane marking menu item */
	protected JMenuItem createLaneMarkingItem() {
		if(!LaneMarkingForm.isPermitted(session))
			return null;
		JMenuItem item = new JMenuItem(I18N.get("lane.markings"));
		new ActionJob(item) {
			public void perform() throws Exception {
				desktop.show(new LaneMarkingForm(session));
			}
		};
		return item;
	}
}
