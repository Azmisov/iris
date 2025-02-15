/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2009-2018  Minnesota Department of Transportation
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

import java.awt.event.MouseEvent;
import us.mn.state.dot.tms.R_Node;
import us.mn.state.dot.tms.client.map.MapBean;
import us.mn.state.dot.tms.client.map.MapObject;
import us.mn.state.dot.tms.client.map.MapSearcher;
import us.mn.state.dot.tms.client.proxy.ProxyLayer;
import us.mn.state.dot.tms.client.proxy.ProxyLayerState;
import us.mn.state.dot.tms.geo.MapVector;

/**
 * SegmentLayerState is a class for drawing roadway segments.
 *
 * @author Douglas Lau
 * @author Michael Darter
 */
public class SegmentLayerState extends ProxyLayerState<R_Node> {

	/** R_Node manager */
	private final R_NodeManager manager;

	/** Segment builder */
	private final SegmentBuilder builder;

	/** Create a new segment layer */
	public SegmentLayerState(R_NodeManager m, ProxyLayer<R_Node> l,
		MapBean mb, SegmentBuilder sb)
	{
		super(l, mb);
		setTheme(new SpeedTheme());
		addTheme(new SpeedTheme());
		addTheme(new FlowTheme());
		addTheme(new DensityTheme());
		addTheme(new FreewayTheme());
		manager = m;
		builder = sb;
	}

	/** Iterate through the segments in the layer */
	@Override
	public MapObject forEach(MapSearcher s) {
		if (isPastLaneZoomThreshold())
			return forEachLane(s);
		else
			return forEachStation(s);
	}

	/** Is the zoom level past the "individual lane" threshold? */
	private boolean isPastLaneZoomThreshold() {
		return map.getModel().getZoomLevel().ordinal() >= 14;
	}

	/** Is the zoom level past the "parking space" threshold? */
	private boolean isPastParkingZoomThreshold() {
		return map.getModel().getZoomLevel().ordinal() >= 17;
	}

	/** Get the current map scale */
	@Override
	public float getScale() {
		// Don't adjust scale for segments
		return (float) map.getScale();
	}

	/** Iterate through the stations in the layer */
	private MapObject forEachStation(MapSearcher s) {
		float scale = getScale();
		for (Segment seg: builder) {
			MapSegment ms = new MapSegment(seg, scale);
			if (s.next(ms))
				return ms;
		}
		return null;
	}

	/** Iterate through each lane segment in the layer.
	 * @param s Map searcher callback.
	 * @return Map object found, if any. */
	private MapObject forEachLane(MapSearcher s) {
		float scale = getScale();
		boolean parking = isPastParkingZoomThreshold();
		MapVector normal = null;
		for (Segment seg: builder) {
			if (parking && seg.parking) {
				ParkingSpace ps;
				if (seg.laneCount() > 1) {
					ps = new ParkingSpace(seg, 1, scale,
						normal);
					if (s.next(ps))
						return ps;
					ps = new ParkingSpace(seg, 2, scale,
						normal);
					if (s.next(ps))
						return ps;
					if (seg.laneCount() > 2) {
						ps = new ParkingSpace(seg, 3,
							scale, normal);
						if (s.next(ps))
							return ps;
					}
					if (seg.laneCount() > 3) {
						ps = new ParkingSpace(seg, 4,
							scale, normal);
						if (s.next(ps))
							return ps;
					}
				} else {
					ps = new ParkingSpace(seg, null, scale,
						normal);
					if (s.next(ps))
						return ps;
				}
				boolean n = (normal != null);
				normal = ps.normal;
				if (n)
					continue;
			} else
				normal = null;
			for (int sh = seg.getLeftMin(); sh < seg.getRightMax();
			     sh++)
			{
				MapSegment ms = new MapSegment(seg, sh, scale);
				if (s.next(ms))
					return ms;
			}
		}
		return null;
	}

	/** Do left-click event processing */
	@Override
	protected void doLeftClick(MouseEvent e, MapObject o) {
		if (o instanceof MapSegment) {
			MapSegment ms = (MapSegment) o;
			doLeftClick(e, ms.getStationR_Node());
		} else if (o instanceof ParkingSpace) {
			ParkingSpace ps = (ParkingSpace) o;
			doLeftClick(e, ps.getR_Node());
		} else
			super.doLeftClick(e, null);
	}

	/** Do left-click event processing */
	private void doLeftClick(MouseEvent e, R_Node n) {
		MapObject mo = builder.findGeoLoc(n);
		super.doLeftClick(e, mo);
	}

	/** Do right-click event processing */
	@Override
	protected void doRightClick(MouseEvent e, MapObject o) {
		if (o instanceof MapSegment) {
			MapSegment ms = (MapSegment) o;
			R_Node n = ms.getStationR_Node();
			MapObject mo = builder.findGeoLoc(n);
			super.doRightClick(e, mo);
		}
	}
}
