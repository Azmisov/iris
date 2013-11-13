/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2006-2013  Minnesota Department of Transportation
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import us.mn.state.dot.geokit.Position;
import us.mn.state.dot.geokit.SphericalMercatorPosition;
import us.mn.state.dot.map.PointSelector;
import us.mn.state.dot.sched.Job;
import static us.mn.state.dot.sched.SwingRunner.runSwing;
import us.mn.state.dot.sonar.client.ProxyListener;
import us.mn.state.dot.sonar.client.TypeCache;
import us.mn.state.dot.tms.CorridorBase;
import us.mn.state.dot.tms.GeoLoc;
import us.mn.state.dot.tms.GeoLocHelper;
import us.mn.state.dot.tms.R_Node;
import static us.mn.state.dot.tms.R_Node.MID_SHIFT;
import us.mn.state.dot.tms.client.IrisClient;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.ProxyLayer;
import us.mn.state.dot.tms.client.proxy.ProxySelectionListener;
import us.mn.state.dot.tms.client.proxy.ProxySelectionModel;
import us.mn.state.dot.tms.client.widget.IAction;
import us.mn.state.dot.tms.client.widget.ILabel;
import us.mn.state.dot.tms.client.widget.WrapperComboBoxModel;
import us.mn.state.dot.tms.utils.I18N;

/**
 * This component allows a corridor to be chosen from a list
 *
 * @author Douglas Lau
 */
public class CorridorList extends JPanel {

	/** User session */
	protected final Session session;

	/** Roadway node manager */
	protected final R_NodeManager manager;

	/** Selected r_node panel */
	protected final R_NodePanel panel;

	/** Roadway node creator */
	protected final R_NodeCreator creator;

	/** Client frame */
	protected final IrisClient client;

	/** Roadway node type cache */
	protected final TypeCache<R_Node> r_nodes;

	/** Location type cache */
	protected final TypeCache<GeoLoc> geo_locs;

	/** Roadway node layer */
	protected final ProxyLayer<R_Node> layer;

	/** Selected roadway corridor */
	protected CorridorBase corridor;

	/** Corridor action */
	private final IAction corr_act = new IAction("r_node.corridor") {
		protected void do_perform() {
			Object s = corridor_cbx.getSelectedItem();
			if(s instanceof CorridorBase)
				setCorridor((CorridorBase)s);
			else
				setCorridor(null);
		}
	};

	/** Combo box to select a roadway corridor */
	private final JComboBox corridor_cbx = new JComboBox();

	/** Action to add a new roadway node */
	private final IAction add_node = new IAction("r_node.add") {
		protected void do_perform() {
			doAddNode();
		}
	};

	/** Action to delete the currently selected roadway node */
	private final IAction delete_node = new IAction("r_node.delete") {
		protected void do_perform() {
			doDeleteNode();
		}
	};

	/** R_Node selection model */
	protected final ProxySelectionModel<R_Node> sel_model;

	/** List component for nodes */
	protected final JList n_list = new JList();

	/** Roadway node list model */
	protected R_NodeListModel n_model = new R_NodeListModel();

	/** R_Node list selection model */
	protected R_NodeListSelectionModel smodel;

	/** R_Node selection listener */
	protected final ProxySelectionListener<R_Node> sel_listener =
		new ProxySelectionListener<R_Node>()
	{
		protected R_Node r_node;
		public void selectionAdded(R_Node proxy) {
			if(!manager.checkCorridor(proxy)) {
				CorridorBase cb = manager.getCorridor(proxy);
				corridor_cbx.setSelectedItem(cb);
			}
			updateNodeSelection(proxy);
			r_node = proxy;
		}
		public void selectionRemoved(R_Node proxy) {
			if(proxy == r_node)
				updateNodeSelection(null);
		}
	};

	/** Listener for r_node changes */
	protected final ProxyListener<R_Node> listener =
		new ProxyListener<R_Node>()
	{
		protected boolean enumerated = false;
		public void proxyAdded(R_Node proxy) {
			if(enumerated)
				nodeAdded(proxy);
		}
		public void enumerationComplete() {
			enumerated = true;
			updateListModel();
		}
		public void proxyRemoved(R_Node proxy) {
			nodeRemoved(proxy);
		}
		public void proxyChanged(R_Node proxy, String a) {
			nodeChanged(proxy, a);
		}
	};

	/** Listener for geo_loc changes */
	protected final ProxyListener<GeoLoc> loc_listener =
		new ProxyListener<GeoLoc>()
	{
		public void proxyAdded(GeoLoc proxy) { }
		public void enumerationComplete() { }
		public void proxyRemoved(GeoLoc proxy) { }
		public void proxyChanged(GeoLoc proxy, String a) {
			geoLocChanged(proxy, a);
		}
	};

	/** Create a new corridor list */
	public CorridorList(Session s, R_NodeManager m, R_NodePanel p) {
		super(new GridBagLayout());
		session = s;
		manager = m;
		panel = p;
		creator = new R_NodeCreator(s);
		client = s.getDesktop().client;
		layer = m.getLayer();
		r_nodes = creator.getR_Nodes();
		geo_locs = creator.getGeoLocs();
		corridor_cbx.setAction(corr_act);
		corridor_cbx.setModel(new WrapperComboBoxModel(
			manager.getCorridorModel()));
		sel_model = manager.getSelectionModel();
		n_list.setCellRenderer(new R_NodeCellRenderer());
		n_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setBorder(BorderFactory.createTitledBorder(
			I18N.get("r_node.corridor.selected")));
	}

	/** Initialize the corridor list */
	public void initialize() {
		GridBagConstraints bag = new GridBagConstraints();
		bag.gridx = GridBagConstraints.RELATIVE;
		bag.gridy = 0;
		bag.insets = new Insets(2, 4, 2, 4);
		add(new ILabel("r_node.corridor"), bag);
		bag.weightx = 0.5f;
		bag.fill = GridBagConstraints.BOTH;
		add(corridor_cbx, bag);
		bag.weightx = 0;
		bag.fill = GridBagConstraints.NONE;
		add(new ILabel("r_node"), bag);
		add(new JButton(add_node), bag);
		add(new JButton(delete_node), bag);
		bag.gridx = 0;
		bag.gridy = 1;
		bag.gridwidth = 5;
		bag.fill = GridBagConstraints.BOTH;
		bag.weightx = 1;
		bag.weighty = 1;
		JScrollPane scroll = new JScrollPane(n_list);
		add(scroll, bag);
		r_nodes.addProxyListener(listener);
		geo_locs.addProxyListener(loc_listener);
		sel_model.addProxySelectionListener(sel_listener);
		createJobs();
		updateNodeSelection(null);
		add_node.setEnabled(canAdd());
		delete_node.setEnabled(false);
	}

	/** Create the jobs */
	protected void createJobs() {
	}

	/** Set a new selected corridor */
	protected void setCorridor(CorridorBase c) {
		client.setPointSelector(null);
		manager.setCorridor(c);
		updateListModel();
		layer.updateExtent();
	}

	/** Dispose of the corridor chooser */
	public void dispose() {
		sel_model.removeProxySelectionListener(sel_listener);
		geo_locs.removeProxyListener(loc_listener);
		r_nodes.removeProxyListener(listener);
		removeAll();
	}

	/** Called when an r_node has been added */
	protected void nodeAdded(R_Node proxy) {
		if(manager.checkCorridor(proxy))
			updateListModel();
	}

	/** Called when an r_node has been removed */
	protected void nodeRemoved(R_Node proxy) {
		if(manager.checkCorridor(proxy))
			updateListModel();
	}

	/** Called when an r_node attribute has changed */
	protected void nodeChanged(R_Node proxy, String a) {
		if(a.equals("abandoned"))
			updateListModel();
		else if(manager.checkCorridor(proxy))
			n_model.updateItem(proxy);
	}

	/** Called when a GeoLoc proxy attribute has changed */
	protected void geoLocChanged(final GeoLoc loc, String a) {
		// Don't hog the SONAR TaskProcessor thread
		client.WORKER.addJob(new Job() {
			public void perform() {
				if(checkCorridor(loc))
					updateListModel();
			}
		});
	}

	/** Check the corridor for a geo location */
	protected boolean checkCorridor(GeoLoc loc) {
		// NOTE: The fast path assumes that GeoLoc name matches R_Node
		//       name.  If that is not the case, the GeoLoc should
		//       still be found by checkNodeList(GeoLoc).
		return checkCorridor(loc.getName()) || checkNodeList(loc);
	}

	/** Check the corridor for an r_node with the given name */
	protected boolean checkCorridor(String name) {
		R_Node proxy = r_nodes.lookupObject(name);
		return proxy != null && manager.checkCorridor(proxy);
	}

	/** Check the node list for a geo location. This is needed in case
	 * the geo location has changed to a different corridor. */
	protected boolean checkNodeList(GeoLoc loc) {
		ListModel lm = n_list.getModel();
		for(int i = 0; i < lm.getSize(); i++) {
			Object obj = lm.getElementAt(i);
			if(obj instanceof R_NodeModel) {
				R_NodeModel m = (R_NodeModel)obj;
				if(m.r_node.getGeoLoc() == loc)
					return true;
			}
		}
		return false;
	}

	/** Create a sorted list of roadway nodes for one corridor */
	static protected CorridorBase createCorridor(Set<R_Node> node_s) {
		GeoLoc loc = getCorridorLoc(node_s);
		if(loc != null) {
			CorridorBase c = new CorridorBase(loc);
			for(R_Node n: node_s)
				c.addNode(n);
			c.arrangeNodes();
			return c;
		} else
			return null;
	}

	/** Get a location for a corridor */
	static protected GeoLoc getCorridorLoc(Set<R_Node> node_s) {
		Iterator<R_Node> it = node_s.iterator();
		if(it.hasNext()) {
			R_Node n = it.next();
			return n.getGeoLoc();
		} else
			return null;
	}

	/** Update the corridor list model */
	protected void updateListModel() {
		// Don't hog the SONAR TaskProcessor thread
		client.WORKER.addJob(new Job() {
			public void perform() {
				doUpdateListModel();
			}
		});
	}

	/** Update the corridor list model */
	protected void doUpdateListModel() {
		if(smodel != null)
			smodel.dispose();
		Set<R_Node> node_s = manager.createSet();
		n_model = createNodeList(node_s);
		smodel = new R_NodeListSelectionModel(n_model, sel_model);
		n_list.setModel(n_model);
		n_list.setSelectionModel(smodel);
		runSwing(new Runnable() {
			public void run() {
				n_list.ensureIndexIsVisible(
					n_list.getLeadSelectionIndex());
			}
		});
	}

	/** Create a list model of roadway node models for one corridor */
	protected R_NodeListModel createNodeList(Set<R_Node> node_s) {
		LinkedList<R_NodeModel> nodes = new LinkedList<R_NodeModel>();
		List<R_NodeModel> no_loc = createNullLocList(node_s);
		corridor = createCorridor(node_s);
		if(corridor != null) {
			R_NodeModel prev = null;
			for(R_Node proxy: corridor) {
				R_NodeModel mdl = new R_NodeModel(proxy, prev);
				nodes.add(0, mdl);
				prev = mdl;
			}
		}
		nodes.addAll(no_loc);
		return new R_NodeListModel(nodes);
	}

	/** Create a list of r_node models with null locations.  The r_nodes
	 * are then removed from the set passed in.
	 * @param node_s Set of nodes on the corridor.
	 * @return List of r_node models with null location. */
	protected List<R_NodeModel> createNullLocList(Set<R_Node> node_s) {
		LinkedList<R_NodeModel> no_loc =
			new LinkedList<R_NodeModel>();
		Iterator<R_Node> it = node_s.iterator();
		while(it.hasNext()) {
			R_Node proxy = it.next();
			if(isNullOrAbandoned(proxy)) {
				no_loc.add(new R_NodeModel(proxy, null));
				it.remove();
			}
		}
		return no_loc;
	}

	/** Check if location is null or r_node is abandoned */
	private boolean isNullOrAbandoned(R_Node n) {
		return n.getAbandoned() || GeoLocHelper.isNull(n.getGeoLoc());
	}

	/** Update the roadway node selection */
	protected void updateNodeSelection(R_Node proxy) {
		client.setPointSelector(null);
		panel.setR_Node(proxy);
		add_node.setEnabled(canAdd());
		delete_node.setEnabled(canRemove(proxy));
	}

	/** Do the add node action */
	protected void doAddNode() {
		client.setPointSelector(new PointSelector() {
			public boolean selectPoint(Point2D p) {
				createNode(corridor, p);
				return true;
			}
			public void finish() { }
		});
	}

	/** Create a new node at a specified point */
	protected void createNode(CorridorBase c, Point2D p) {
		Position pos = getWgs84Position(p);
		if(c != null) {
			int lanes = 2;
			int shift = MID_SHIFT + 1;
			R_NodeModel mdl = findModel(c, pos);
			if(mdl != null) {
				shift = mdl.getDownstreamLane(false);
				lanes = shift - mdl.getDownstreamLane(true);
			}
			creator.create(c.getRoadway(), c.getRoadDir(), pos,
				lanes, shift);
		} else
			creator.create(pos);
		add_node.setEnabled(canAdd());
	}

	/** Get a position */
	private Position getWgs84Position(Point2D p) {
		SphericalMercatorPosition smp = new SphericalMercatorPosition(
			p.getX(), p.getY());
		return smp.getPosition();
	}

	/** Find an r_node model near a point */
	private R_NodeModel findModel(CorridorBase c, Position pos) {
		R_Node found = c.findLastBefore(pos);
		for(int i = 0; i < n_model.getSize(); i++) {
			Object elem = n_model.get(i);
			if(elem instanceof R_NodeModel) {
				R_NodeModel mdl = (R_NodeModel)elem;
				if(mdl.r_node == found)
					return mdl;
			}
		}
		return null;
	}

	/** Do the delete node action */
	protected void doDeleteNode() {
		R_Node proxy = getSelectedNode();
		if(proxy != null) {
			GeoLoc loc = proxy.getGeoLoc();
			proxy.destroy();
			loc.destroy();
		}
	}

	/** Get the selected roadway node */
	protected R_Node getSelectedNode() {
		for(R_Node n: sel_model.getSelected())
			return n;
		return null;
	}

	/** Test if a new r_node can be added */
	protected boolean canAdd() {
		return session.canAdd(R_Node.SONAR_TYPE);
	}

	/** Test if an r_node can be removed */
	protected boolean canRemove(R_Node n) {
		return n != null && session.canRemove(n);
	}
}
