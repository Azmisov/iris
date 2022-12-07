/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2000-2022  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.client.dms;

import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import us.mn.state.dot.tms.DMS;
import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.client.widget.ILabel;
import static us.mn.state.dot.tms.client.widget.Widgets.UI;

/**
 * The ComposerMiscPanel is a GUI panel for miscellaneous widgets related to
 * the sign composer panel.
 *
 * @author Douglas Lau
 */
public class ComposerMiscPanel extends JPanel {

	/** Sign message composer */
	private final SignMessageComposer composer;

	/** Message pattern label */
	private final ILabel pattern_lbl = new ILabel("msg.pattern");

	/** Combobox used to select a message pattern */
	private final MsgPatternCBox pattern_cbx;

	/** Duration label */
	private final ILabel dur_lbl = new ILabel("dms.duration");

	/** Used to select the expires time for a message (optional) */
	private final JComboBox<Expiration> dur_cbx =
		new JComboBox<Expiration>(Expiration.values());

	/** Counter to indicate we're adjusting widgets.  This needs to be
	 * incremented before calling dispatcher methods which might cause
	 * callbacks to this class.  This prevents infinite loops. */
	private int adjusting = 0;

	/** Create a new composer miscellaneous panel */
	public ComposerMiscPanel(DMSDispatcher ds, SignMessageComposer smc) {
		composer = smc;
		pattern_cbx = new MsgPatternCBox(ds);
		layoutPanel();
		initializeWidgets();
	}

	/** Layout the panel */
	private void layoutPanel() {
		GroupLayout gl = new GroupLayout(this);
		gl.setHonorsVisibility(false);
		gl.setAutoCreateGaps(false);
		gl.setAutoCreateContainerGaps(false);
		setLayout(gl);
		GroupLayout.ParallelGroup lg = gl.createParallelGroup(
			GroupLayout.Alignment.TRAILING);
		GroupLayout.ParallelGroup vg = gl.createParallelGroup(
			GroupLayout.Alignment.LEADING);
		// Message pattern widgets
		pattern_lbl.setLabelFor(pattern_cbx);
		lg.addComponent(pattern_lbl);
		vg.addComponent(pattern_cbx);
		GroupLayout.ParallelGroup g1 = gl.createParallelGroup(
			GroupLayout.Alignment.CENTER);
		g1.addComponent(pattern_lbl).addComponent(pattern_cbx);
		// Duraton widgets
		dur_lbl.setLabelFor(dur_cbx);
		lg.addComponent(dur_lbl);
		vg.addComponent(dur_cbx);
		GroupLayout.ParallelGroup g2 = gl.createParallelGroup(
			GroupLayout.Alignment.CENTER);
		g2.addComponent(dur_lbl).addComponent(dur_cbx);
		// Finish group layout
		GroupLayout.SequentialGroup horz_g = gl.createSequentialGroup();
		horz_g.addGap(UI.hgap).addGroup(lg);
		horz_g.addGap(UI.hgap).addGroup(vg);
		gl.setHorizontalGroup(horz_g);
		GroupLayout.SequentialGroup vert_g = gl.createSequentialGroup();
		vert_g.addGap(UI.vgap).addGroup(g1).addGap(UI.vgap);
		vert_g.addGroup(g2).addGap(UI.vgap);
		gl.setVerticalGroup(vert_g);
	}

	/** Clear the widgets */
	public void clearWidgets() {
		adjusting++;
		pattern_cbx.setSelectedItem(null);
		adjusting--;
	}

	/** Dispose of the message selector */
	public void dispose() {
		removeAll();
		pattern_cbx.dispose();
	}

	/** Select a sign */
	public void setSign(DMS proxy) {
		initializeWidgets();
		pattern_cbx.populateModel(proxy);
	}

	/** Initialize the widgets */
	private void initializeWidgets() {
		dur_cbx.setSelectedIndex(0);
		boolean dur = SystemAttrEnum.DMS_DURATION_ENABLE.getBoolean();
		dur_lbl.setVisible(dur);
		dur_cbx.setVisible(dur);
	}

	/** Enable or Disable the message selector */
	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		pattern_cbx.setEnabled(b);
		boolean vis = pattern_cbx.getItemCount() > 0;
		pattern_lbl.setVisible(vis);
		pattern_cbx.setVisible(vis);
		dur_cbx.setEnabled(b);
		dur_cbx.setSelectedItem(0);
	}

	/** Get the selected duration */
	public Integer getDuration() {
		Expiration e = (Expiration) dur_cbx.getSelectedItem();
		return (e != null) ? e.duration : null;
	}
}
