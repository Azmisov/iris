/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2008  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.client.sonar;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JPopupMenu;
import us.mn.state.dot.map.MapObject;
import us.mn.state.dot.map.LayerState;
import us.mn.state.dot.map.event.LayerChangedEvent;
import us.mn.state.dot.sonar.SonarObject;

/**
 * Base class for all SONAR map layer states.
 *
 * @author Douglas Lau
 */
public class SonarLayerState<T extends SonarObject> extends LayerState {

	/** Proxy manager */
	protected final ProxyManager<T> manager;

	/** Proxy selection model */
	protected final ProxySelectionModel<T> model;

	/** Listener for proxy selection events */
	protected final ProxySelectionListener<T> listener;

	/** Create a new sonar layer state */
	public SonarLayerState(SonarLayer<T> layer) {
		super(layer);
		manager = layer.getManager();
		model = manager.getSelectionModel();
		listener = new ProxySelectionListener<T>() {
			public void selectionAdded(T proxy) {
				setSelection();
			}
			public void selectionRemoved(T proxy) {
				setSelection();
			}
		};
		model.addProxySelectionListener(listener);
	}

	/** Set the selection */
	protected void setSelection() {
		List<T> proxies = model.getSelected();
		MapGeoLoc[] sel = new MapGeoLoc[proxies.size()];
		for(int i = 0; i < sel.length; i++)
			sel[i] = manager.findGeoLoc(proxies.get(i));
		setSelections(sel);
	}

	/** Dispose of the layer state */
	public void dispose() {
		super.dispose();
		model.removeProxySelectionListener(listener);
	}

	/** Do left-click event processing */
	protected void doLeftClick(MouseEvent e, MapObject o) {
		// FIXME: find the proxy from the MapObject
		//model.setSelected((TmsMapProxy)o);
	}

	/** Show a popup menu for the given proxy */
	protected void showPopupMenu(MouseEvent e) {
		manager.showPopupMenu(e);
	}

	/** Do right-click event processing */
	protected void doRightClick(MouseEvent e, MapObject o) {
		// FIXME: find the proxy from the MapObject
		//model.setSelected(proxy);
		showPopupMenu(e);
	}
}
