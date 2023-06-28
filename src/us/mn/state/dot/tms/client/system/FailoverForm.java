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
package us.mn.state.dot.tms.client.system;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import us.mn.state.dot.tms.CommLink;
import us.mn.state.dot.tms.CommLinkHelper;
import us.mn.state.dot.tms.Controller;
import us.mn.state.dot.tms.client.EditModeListener;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.widget.AbstractForm;
import us.mn.state.dot.tms.client.widget.IAction;
import us.mn.state.dot.tms.client.widget.ILabel;
import us.mn.state.dot.tms.utils.I18N;

/**
 * A form for displaying and editing failover information.
 *
 * @author Michael Darter
 */
public class FailoverForm extends AbstractForm {

	/** Check if the user is permitted to use the form */
	static public boolean isPermitted(Session s) {
		return s.canRead(CommLink.SONAR_TYPE) &&
		       s.canRead(Controller.SONAR_TYPE);
	}

	/** Enable all comm links that are active */
	private final IAction eb_act1 = new IAction(
		"failover.form.enable")
	{
		protected void doActionPerformed(ActionEvent e) {
			CommLinkHelper.enableActive(true);
		}
	};

	/** Disable all comm links that are active */
	private final IAction db_act1 = new IAction(
		"failover.form.disable")
	{
		protected void doActionPerformed(ActionEvent e) {
			CommLinkHelper.enableActive(false);
		}
	};

	/** Enable all comm links */
	private final IAction eb_act2 = new IAction("failover.form.enable") {
		protected void doActionPerformed(ActionEvent e) {
			CommLinkHelper.enableAll(true);
		}
	};

	/** Disable all comm links */
	private final IAction db_act2 = new IAction("failover.form.disable") {
		protected void doActionPerformed(ActionEvent e) {
			CommLinkHelper.enableAll(false);
		}
	};

	/** Edit mode listener */
	private final EditModeListener edit_mode_lsnr = new EditModeListener(){
		public void editModeChanged() {
			updateEditMode();
		}
	};

	/** User session */
	private final Session session;

	/** Button to enable all comm links with active controllers */
	private JButton en_act_btn = new JButton();

	/** Button to disable all comm links with active controllers */
	private JButton di_act_btn = new JButton();

	/** Button to enable all comm links */
	private JButton en_all_btn = new JButton();

	/** Button to disable all comm links */
	private JButton di_all_btn = new JButton();

	/** Constructor */
	public FailoverForm(Session se) {
		super(I18N.get("failover.form.title"));
		session = se;
	}

	/** Initialize the form */
	@Override
	protected void initialize() {
		super.initialize();
		JPanel pa = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		Insets ins = new Insets(3, 3, 3, 3);

		// row 0
		int row = 0;
		JLabel lbl1 = new ILabel("failover.form.commlinks");
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.insets = ins;
		pa.add(lbl1, gbc);

		en_act_btn.setAction(eb_act1);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		gbc.gridy = row;
		gbc.insets = ins;
		pa.add(en_act_btn, gbc);

		di_act_btn.setAction(db_act1);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 2;
		gbc.gridy = row;
		gbc.insets = ins;
		pa.add(di_act_btn, gbc);

		// row 1
		++row;
		JLabel lbl2 = new ILabel("failover.form.allcommlinks");
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.insets = ins;
		pa.add(lbl2, gbc);

		en_all_btn.setAction(eb_act2);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		gbc.gridy = row;
		gbc.insets = ins;
		pa.add(en_all_btn, gbc);

		di_all_btn.setAction(db_act2);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 2;
		gbc.gridy = row;
		gbc.insets = ins;
		pa.add(di_all_btn, gbc);
		add(pa);

		updateEditMode();
		session.addEditModeListener(edit_mode_lsnr);
	}

	/** Determine if user can update this form */
	private boolean canUpdate() {
		return session.canWrite(session.getUser(), "password");
	}

	/** Update UI components when edit mode changes */
	private void updateEditMode() {
		final boolean cu = canUpdate();
		en_act_btn.setEnabled(cu);
		di_act_btn.setEnabled(cu);
		en_all_btn.setEnabled(cu);
		di_all_btn.setEnabled(cu);
	}

	/** Dispose of the panel */
	@Override
	public void dispose() {
		session.removeEditModeListener(edit_mode_lsnr);
		super.dispose();
	}
}