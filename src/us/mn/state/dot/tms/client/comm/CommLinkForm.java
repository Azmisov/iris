/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2008-2022  Minnesota Department of Transportation
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

import java.awt.event.ActionEvent;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.Group;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import us.mn.state.dot.sonar.client.TypeCache;
import us.mn.state.dot.tms.CommConfig;
import us.mn.state.dot.tms.CommLink;
import us.mn.state.dot.tms.Controller;
import us.mn.state.dot.tms.client.ITextField;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.ProxyTablePanel;
import us.mn.state.dot.tms.client.proxy.ProxyView;
import us.mn.state.dot.tms.client.proxy.ProxyWatcher;
import us.mn.state.dot.tms.client.widget.AbstractForm;
import us.mn.state.dot.tms.client.widget.IAction;
import us.mn.state.dot.tms.client.widget.ILabel;
import static us.mn.state.dot.tms.client.widget.Widgets.UI;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import us.mn.state.dot.tms.utils.I18N;

/**
 * A form for displaying and editing comm links.
 *
 * @author Douglas Lau
 */
public class CommLinkForm extends AbstractForm {

	/** Check if the user is permitted to use the form */
	static public boolean isPermitted(Session s) {
		return s.canRead(CommLink.SONAR_TYPE) &&
		       s.canRead(Controller.SONAR_TYPE);
	}

	/** User session */
	private final Session session;

	/** Comm link type cache */
	private final TypeCache<CommLink> comm_links;

	/** Proxy watcher */
	private final ProxyWatcher<CommLink> watcher;

	/** Proxy view for selected comm link */
	private final ProxyView<CommLink> view = new ProxyView<CommLink>() {
		public void enumerationComplete() { }
		public void update(CommLink cl, String a) {
			if (a == null || a.equals("connected")) {
				connected_lbl.setText(cl.getConnected()
					? I18N.get("comm.link.connected")
					: I18N.get("item.style.failed")
				);
			}
		}
		public void clear() {
			connected_lbl.setText("");
		}
	};

	/** Comm link table panel */
	private final ProxyTablePanel<CommLink> link_pnl;
	/** Comm link table filter input */
	private final ITextField link_search =
		new ITextField("Search Comm Links (RegExp)", 8);
	/** Comm link table filter reset */
	private final JButton link_search_clear = new JButton("Reset");
	/** Comm link table model */
	private final CommLinkModel link_mdl;

	/** Comm config panel */
	private final CommConfigPanel config_pnl;

	/** Clear action */
	private final IAction clear_act = new IAction(
		"comm.link.clear")
	{
		protected void doActionPerformed(ActionEvent e) {
			link_pnl.selectProxy(null);
		}
	};

	/** Clear comm link filter button */
	private final JButton clear_btn = new JButton();

	/** Comm link label */
	private final JLabel link_lbl = new ILabel("comm.link.selected");

	/** Comm link connected label */
	private final JLabel connected_lbl = new JLabel();

	/** Table panel for controllers */
	private final ControllerPanel controller_pnl;

	/** Create a new comm link form */
	public CommLinkForm(Session s) {
		super(I18N.get("comm.links"));
		session = s;
		comm_links = s.getSonarState().getConCache().getCommLinks();
		watcher = new ProxyWatcher<CommLink>(comm_links, view, false);
		link_mdl = new CommLinkModel(s);
		link_mdl.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.UPDATE)
					updateCommConfig();
			}
		});
		link_pnl = new ProxyTablePanel<CommLink>(link_mdl) {
			protected void selectProxy() {
				selectCommLink();
				super.selectProxy();
			}
		};
		config_pnl = new CommConfigPanel(s);
		controller_pnl = new ControllerPanel(s);
	}

	/** Initializze the widgets in the form */
	@Override
	protected void initialize() {
		super.initialize();
		watcher.initialize();
		link_pnl.initialize();
		config_pnl.initialize();
		controller_pnl.initialize();
		clear_btn.setAction(clear_act);
		link_search.addChangeListener((cur, prev) -> {
			link_mdl.setSearchString(cur);
		});
		link_search_clear.addActionListener(evt -> {
			link_search.setText("");
		});
		layoutPanel();
	}

	/** Dispose of the form */
	@Override
	protected void dispose() {
		watcher.dispose();
		controller_pnl.dispose();
		config_pnl.dispose();
		link_pnl.dispose();
		super.dispose();
	}

	/** Layout the panel */
	private void layoutPanel() {
		GroupLayout gl = new GroupLayout(this);
		gl.setHonorsVisibility(false);
		gl.setAutoCreateGaps(false);
		gl.setAutoCreateContainerGaps(false);
		gl.setHorizontalGroup(createHorizontalGroup(gl));
		gl.setVerticalGroup(createVerticalGroup(gl));
		setLayout(gl);
	}

	/** Create the horizontal group */
	private GroupLayout.Group createHorizontalGroup(GroupLayout gl) {
		// link filter
		var lf = gl.createSequentialGroup();
		lf.addComponent(link_search);
		lf.addComponent(link_search_clear);
		lf.addPreferredGap(RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
		// link filter | link table
		var lg = gl.createParallelGroup(Alignment.LEADING);
		lg.addGroup(lf);
		lg.addComponent(link_pnl);
		// (link filter | link table) / link metadata sidebar
		GroupLayout.SequentialGroup g0 = gl.createSequentialGroup();
		g0.addGroup(lg);
		g0.addGap(UI.hgap);
		g0.addComponent(config_pnl);
		// controller header
		GroupLayout.SequentialGroup g1 = gl.createSequentialGroup();
		g1.addComponent(clear_btn);
		g1.addGap(UI.hgap);
		g1.addComponent(link_lbl);
		g1.addGap(UI.hgap);
		g1.addComponent(connected_lbl);
		// link components... / controller header / controller table
		GroupLayout.ParallelGroup hg = gl.createParallelGroup();
		hg.addGroup(g0);
		hg.addGroup(g1);
		hg.addComponent(controller_pnl);
		return hg;
	}

	/** Create the vertical group */
	private GroupLayout.Group createVerticalGroup(GroupLayout gl) {
		// link filter
		var lf = gl.createBaselineGroup(false, false);
		lf.addComponent(link_search);
		lf.addComponent(link_search_clear);
		// link filter | link table
		var lg = gl.createSequentialGroup();
		lg.addGroup(lf);
		lg.addComponent(link_pnl);
		// (link filter | link table) / link metadata sidebar
		var g0 = gl.createBaselineGroup(false, false);
		g0.addGroup(lg);
		g0.addComponent(config_pnl);
		// controller header
		var g1 = gl.createBaselineGroup(false,false);
		g1.addComponent(clear_btn);
		g1.addComponent(link_lbl);
		g1.addComponent(connected_lbl);
		// link components... / controller header / controller table
		var vg = gl.createSequentialGroup();
		vg.addGroup(g0);
		vg.addGroup(g1);
		vg.addComponent(controller_pnl);
		return vg;
	}

	/** Update the comm config */
	private void updateCommConfig() {
		CommLink cl = watcher.getProxy();
		CommConfig cc = (cl != null) ? cl.getCommConfig() : null;
		config_pnl.setProxy(cc);
	}

	/** Change the selected comm link */
	private void selectCommLink() {
		CommLink cl = link_pnl.getSelectedProxy();
		watcher.setProxy(cl);
		updateCommConfig();
		controller_pnl.setCommLink(cl);
	}
}
