/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2011-2012  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.client.schedule;

import java.awt.GridBagConstraints;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import us.mn.state.dot.sched.SwingRunner;
import us.mn.state.dot.sonar.client.ProxyListener;
import us.mn.state.dot.sonar.client.TypeCache;
import us.mn.state.dot.tms.ActionPlan;
import us.mn.state.dot.tms.DMS;
import us.mn.state.dot.tms.DmsAction;
import us.mn.state.dot.tms.DmsActionHelper;
import us.mn.state.dot.tms.LaneAction;
import us.mn.state.dot.tms.LaneActionHelper;
import us.mn.state.dot.tms.LaneMarking;
import us.mn.state.dot.tms.MeterAction;
import us.mn.state.dot.tms.MeterActionHelper;
import us.mn.state.dot.tms.PlanPhase;
import us.mn.state.dot.tms.SignGroup;
import us.mn.state.dot.tms.SignGroupHelper;
import us.mn.state.dot.tms.RampMeter;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.ProxySelectionListener;
import us.mn.state.dot.tms.client.proxy.ProxySelectionModel;
import us.mn.state.dot.tms.client.widget.FormPanel;
import us.mn.state.dot.tms.client.widget.ILabel;
import us.mn.state.dot.tms.utils.I18N;

/**
 * A plan dispatcher is a GUI panel for dispatching action plans
 *
 * @author Douglas Lau
 */
public class PlanDispatcher extends FormPanel
	implements ProxyListener<ActionPlan>, ProxySelectionListener<ActionPlan>
{
	/** Name component */
	private final JLabel name_lbl = createValueLabel();

	/** Description component */
	private final JLabel description_lbl = createValueLabel();

	/** DMS count component */
	private final JLabel dms_lbl = createValueLabel();

	/** Lane count component */
	private final JLabel lane_lbl = createValueLabel();

	/** Meter count component */
	private final JLabel meter_lbl = createValueLabel();

	/** Plan phase combo box */
	private final JComboBox phaseCmb = new JComboBox();

	/** Current session */
	private final Session session;

	/** Action plan manager */
	private final PlanManager manager;

	/** Selection model */
	private final ProxySelectionModel<ActionPlan> selectionModel;

	/** Action plan proxy cache */
	private final TypeCache<ActionPlan> cache;

	/** Selected action plan */
	private ActionPlan selected = null;

	/** Create a new plan dispatcher */
	public PlanDispatcher(Session s, PlanManager m) {
		super(true);
		session = s;
		manager = m;
		selectionModel = manager.getSelectionModel();
		cache = session.getSonarState().getActionPlans();
		setTitle(I18N.get("action.plan.selected"));
		setEnabled(false);
		add(new ILabel("action.plan.name"));
		bag.weightx = 0.4f;
		bag.weighty = 0.4f;
		setWest();
		add(name_lbl);
		bag.weightx = 0.6f;
		bag.weighty = 0.6f;
		addRow(new JLabel(""));
		add(new ILabel("device.description"));
		setFill();
		setWidth(2);
		add(description_lbl);
		finishRow();
		add(I18N.get("dms"), dms_lbl);
		finishRow();
		add(I18N.get("lane.markings"), lane_lbl);
		finishRow();
		add(I18N.get("ramp.meter.long.plural"), meter_lbl);
		finishRow();
		add(I18N.get("action.plan.phase"), phaseCmb);
		finishRow();
		setSelected(null);
		cache.addProxyListener(this);
		selectionModel.addProxySelectionListener(this);
	}

	/** Dispose of the panel */
	public void dispose() {
		selectionModel.removeProxySelectionListener(this);
		cache.removeProxyListener(this);
		setSelected(null);
		super.dispose();
	}

	/** A new proxy has been added */
	public void proxyAdded(ActionPlan proxy) {
		// we're not interested
	}

	/** Enumeration of the proxy type has completed */
	public void enumerationComplete() {
		// we're not interested
	}

	/** A proxy has been removed */
	public void proxyRemoved(ActionPlan proxy) {
		if(proxy == selected) {
			SwingRunner.invoke(new Runnable() {
				public void run() {
					setSelected(null);
				}
			});
		}
	}

	/** A proxy has been changed */
	public void proxyChanged(final ActionPlan proxy, final String a) {
		if(proxy == selected) {
			SwingRunner.invoke(new Runnable() {
				public void run() {
					updateAttribute(proxy, a);
				}
			});
		}
	}

	/** Called whenever a plan is added to the selection */
	public void selectionAdded(ActionPlan s) {
		if(selectionModel.getSelectedCount() <= 1)
			setSelected(s);
	}

	/** Called whenever a plan is removed from the selection */
	public void selectionRemoved(ActionPlan s) {
		if(selectionModel.getSelectedCount() == 1) {
			for(ActionPlan p: selectionModel.getSelected())
				setSelected(p);
		} else if(s == selected)
			setSelected(null);
	}

	/** Select a action plan to display */
	public void setSelected(ActionPlan proxy) {
		if(selected != null)
			cache.ignoreObject(selected);
		if(proxy != null)
			cache.watchObject(proxy);
		selected = proxy;
		if(proxy != null) {
			phaseCmb.setAction(null);
			phaseCmb.setModel(createPhaseModel(proxy));
			updateAttribute(proxy, null);
		} else {
			name_lbl.setText("");
			description_lbl.setText("");
			dms_lbl.setText("");
			lane_lbl.setText("");
			meter_lbl.setText("");
			phaseCmb.setAction(null);
			phaseCmb.setModel(new DefaultComboBoxModel());
			phaseCmb.setSelectedItem(null);
		}
		setEnabled(canUpdate(proxy));
	}

	/** Create a combo box model for plan phases */
	private DefaultComboBoxModel createPhaseModel(final ActionPlan plan) {
		TreeSet<PlanPhase> phases = createPhaseSet(plan);
		removeNextPhases(phases);
		DefaultComboBoxModel model = new DefaultComboBoxModel();
		model.addElement(plan.getDefaultPhase());
		phases.remove(plan.getDefaultPhase());
		for(PlanPhase p: phases)
			model.addElement(p);
		model.setSelectedItem(plan.getPhase());
		return model;
	}

	/** Create a set of phases for an action plan */
	private TreeSet<PlanPhase> createPhaseSet(final ActionPlan plan) {
		final TreeSet<PlanPhase> phases =
			new TreeSet<PlanPhase>(comparator);
		Iterator<DmsAction> dit = DmsActionHelper.iterator();
		while(dit.hasNext()) {
			DmsAction da = dit.next();
			if(da.getActionPlan() == plan)
				phases.add(da.getPhase());
		}
		Iterator<LaneAction> lit = LaneActionHelper.iterator();
		while(lit.hasNext()) {
			LaneAction la = lit.next();
			if(la.getActionPlan() == plan)
				phases.add(la.getPhase());
		}
		Iterator<MeterAction> mit = MeterActionHelper.iterator();
		while(mit.hasNext()) {
			MeterAction ma = mit.next();
			if(ma.getActionPlan() == plan)
				phases.add(ma.getPhase());
		}
		return phases;
	}

	/** Comparator for plan phases */
	private final Comparator<PlanPhase> comparator =
		new Comparator<PlanPhase>()
	{
		public int compare(PlanPhase a, PlanPhase b) {
			String aa = a.getName();
			String bb = b.getName();
			return aa.compareTo(bb);
		}
	};

	/** Remove phases which are "next" phases */
	private void removeNextPhases(TreeSet<PlanPhase> phases) {
		TreeSet<PlanPhase> n_phases =
			new TreeSet<PlanPhase>(comparator);
		for(PlanPhase p: phases) {
			PlanPhase np = p.getNextPhase();
			if(np != null)
				n_phases.add(np);
		}
		phases.removeAll(n_phases);
	}

	/** Enable or disable the panel */
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		phaseCmb.setEnabled(enabled);
		phaseCmb.setAction(null);
	}

	/** Update one attribute on the form */
	protected void updateAttribute(ActionPlan plan, String a) {
		if(a == null || a.equals("name"))
			name_lbl.setText(plan.getName());
		if(a == null || a.equals("description"))
			description_lbl.setText(plan.getDescription());
		if(a == null || a.equals("active")) {
			dms_lbl.setText(Integer.toString(countDMS(plan)));
			lane_lbl.setText(Integer.toString(countLanes(plan)));
			meter_lbl.setText(Integer.toString(countMeters(plan)));
		}
		if(a == null || a.equals("phase")) {
			phaseCmb.setAction(null);
			ComboBoxModel mdl = phaseCmb.getModel();
			// We must call setSelectedItem on the model, because
			// it might not contain the phase.  In that case,
			// calling JComboBox.setSelectedItem will fail.
			if(mdl instanceof DefaultComboBoxModel) {
				DefaultComboBoxModel model =
					(DefaultComboBoxModel)mdl;
				model.setSelectedItem(plan.getPhase());
			}
			phaseCmb.setAction(new ChangePhaseAction(plan,
				phaseCmb));
		}
	}

	/** Get a count of DMS controlled by an action plan */
	private int countDMS(ActionPlan p) {
		HashSet<SignGroup> plan_groups = new HashSet<SignGroup>();
		Iterator<DmsAction> dit = DmsActionHelper.iterator();
		while(dit.hasNext()) {
			DmsAction da = dit.next();
			if(da.getActionPlan() == p)
				plan_groups.add(da.getSignGroup());
		}
		HashSet<DMS> plan_signs = new HashSet<DMS>();
		for(SignGroup sg: plan_groups)
			plan_signs.addAll(SignGroupHelper.find(sg));
		return plan_signs.size();
	}

	/** Get a count a lane markings controlled by an action plan */
	private int countLanes(ActionPlan p) {
		HashSet<LaneMarking> plan_lanes = new HashSet<LaneMarking>();
		Iterator<LaneAction> lit = LaneActionHelper.iterator();
		while(lit.hasNext()) {
			LaneAction la = lit.next();
			if(la.getActionPlan() == p)
				plan_lanes.add(la.getLaneMarking());
		}
		return plan_lanes.size();
	}

	/** Get a count a ramp meters controlled by an action plan */
	private int countMeters(ActionPlan p) {
		HashSet<RampMeter> plan_meters = new HashSet<RampMeter>();
		Iterator<MeterAction> mit = MeterActionHelper.iterator();
		while(mit.hasNext()) {
			MeterAction ma = mit.next();
			if(ma.getActionPlan() == p)
				plan_meters.add(ma.getRampMeter());
		}
		return plan_meters.size();
	}

	/** Check if the user can update the given action plan */
	private boolean canUpdate(ActionPlan plan) {
		return session.canUpdate(plan, "phase") && plan.getActive();
	}
}
