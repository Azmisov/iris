/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2009-2020  Minnesota Department of Transportation
 * Copyright (C) 2021  Iteris Inc.
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

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import us.mn.state.dot.tms.GeoLoc;
import us.mn.state.dot.tms.R_Node;
import us.mn.state.dot.tms.Road;
import us.mn.state.dot.tms.RoadClass;
import us.mn.state.dot.tms.client.map.MapObject;
import us.mn.state.dot.tms.client.proxy.MapGeoLoc;
import static us.mn.state.dot.tms.client.widget.Widgets.UI;
import us.mn.state.dot.tms.geo.MapVector;
import us.mn.state.dot.tms.geo.SphericalMercatorPosition;
import us.mn.state.dot.tms.utils.I18N;

/**
 * A segment is the shape of a roadway segment on a map.
 *
 * @author Douglas Lau
 * @author Michael Darter
 */
public class MapSegment implements MapObject {

	/** Identity transform */
	static private final AffineTransform IDENTITY_TRANSFORM =
		new AffineTransform();

	/** Get the coordinate transform */
	@Override
	public AffineTransform getTransform() {
		return IDENTITY_TRANSFORM;
	}

	/** Get the inverse coordinate transform */
	@Override
	public AffineTransform getInverseTransform() {
		return IDENTITY_TRANSFORM;
	}

	/** Segment object */
	private final Segment segment;

	/** Get the upstream station r_node */
	public R_Node getStationR_Node() {
		return segment.getStationR_Node();
	}

	/** Lane for segment (null for all lanes) */
	private final Integer lane;

	/** Shape to render */
	private final Shape shape;

	/** Get the shape to draw this object */
	@Override
	public Shape getShape() {
		return shape;
	}

	/** Shape to draw outline */
	private final Shape outline;

	/** Get the outline to draw this object */
	@Override
	public Shape getOutlineShape() {
		return outline;
	}

	/** Create a new map segment */
	public MapSegment(Segment s, float scale) {
		segment = s;
		lane = null;
		float inner = calculateInner(scale);
		float outer = inner + calculateWidth(scale);
		shape = createShape(inner, outer, inner, outer, false);
		outline = createShape(inner, outer, inner, outer, true);
	}

	/** Calculate the spacing between the centerline and segment */
	private float calculateInner(float scale) {
		return scale * roadClassScale() / 14;
	}

	/** Calculate the ideal segment width */
	private float calculateWidth(float scale) {
		return 2 * scale * roadClassScale();
	}

	/** Get the scale factor for the road class */
	private float roadClassScale() {
		Road r = getStationR_Node().getGeoLoc().getRoadway();
		RoadClass rc = RoadClass.fromOrdinal(r.getRClass());
		return rc.scale * UI.scale;
	}

	/** Create a new map segment */
	public MapSegment(Segment s, int sh, float scale) {
		segment = s;
		lane = segment.getLane(sh);
		float inner = calculateInner(scale);
		float width = calculateLaneWidth(scale);
		R_NodeModel mdl = segment.getModel();
		float in_a = inner + width * mdl.getUpstreamOffset(sh);
		float out_a = inner + width * mdl.getUpstreamOffset(sh + 1);
		float in_b = inner + width * mdl.getDownstreamOffset(sh);
		float out_b = inner + width * mdl.getDownstreamOffset(sh + 1);
		shape = createShape(in_a, out_a, in_b, out_b, false);
		outline = createShape(in_a, out_a, in_b, out_b, true);
	}

	/** Calculate the width of one lane */
	private float calculateLaneWidth(float scale) {
		return calculateWidth(scale) / 2 +
		       5 * (20 - scale) / 20;
	}

	/** Create the shape to draw this object */
	private Shape createShape(float inner_a, float outer_a,
		float inner_b, float outer_b, boolean outline)
	{
		Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO);
		Point2D.Float p = new Point2D.Float();
		setPoint(p, segment.pos_a, segment.normal_a, outer_a);
		path.moveTo(p.getX(), p.getY());
		setPoint(p, segment.pos_b, segment.normal_b, outer_b);
		path.lineTo(p.getX(), p.getY());
		setPoint(p, segment.pos_b, segment.normal_b, inner_b);
		if (outline)
			path.moveTo(p.getX(), p.getY());
		else
			path.lineTo(p.getX(), p.getY());
		setPoint(p, segment.pos_a, segment.normal_a, inner_a);
		path.lineTo(p.getX(), p.getY());
		if (!outline)
			path.closePath();
		return path;
	}

	/** Set a point relative to the location, offset by the normal vector.
	 * @param p Point to set.
	 * @param distance Distance from the location, in meter units. */
	private void setPoint(Point2D p, SphericalMercatorPosition pos,
		MapVector nv, float distance)
	{
		assert (pos != null);
		double x = pos.getX();
		double y = pos.getY();
		double a = nv.getAngle();
		double xo = distance * Math.cos(a);
		double yo = distance * Math.sin(a);
		p.setLocation(x + xo, y + yo);
	}

	/** Get the map segment tool tip */
	public String getTip() {
		StringBuilder sb = new StringBuilder();
		String label = segment.getLabel(lane);
		if (label != null)
			sb.append(label);
		String lm = getLandmark();
		if (lm != null) {
			sb.append("\n ");
			sb.append(I18N.get("location.landmark"));
			sb.append(" = ");
			sb.append(lm);
		}
		Integer flow = getFlow();
		if (flow != null) {
			sb.append("\n ");
			sb.append(I18N.get("units.flow"));
			sb.append(" = ");
			sb.append(flow);
		}
		Integer density = getDensity();
		if (density != null) {
			sb.append("\n ");
			sb.append(I18N.get("units.density"));
			sb.append(" = ");
			sb.append(density);
		}
		Integer speed = getSpeed();
		if (speed != null) {
			sb.append("\n ");
			sb.append(I18N.get("units.speed"));
			sb.append(" = ");
			sb.append(speed);
		}
		if (sb.length() > 0)
			return sb.toString();
		else
			return null;
	}

	/** Get the landmark for the associated r_node.
	 * @return Landmark for associated geoloc or null. */
	private String getLandmark() {
		R_Node proxy = getStationR_Node();
		if (proxy != null) {
			GeoLoc gl = proxy.getGeoLoc();
			if (gl != null)
				return(gl.getLandmark());
		}
		return null;
	}

	/** Get the segment flow */
	public Integer getFlow() {
		return segment.getFlow(lane);
	}

	/** Get the segment density */
	public Integer getDensity() {
		return segment.getDensity(lane);
	}

	/** Get the segment speed */
	public Integer getSpeed() {
		return segment.getSpeed(lane);
	}
}
