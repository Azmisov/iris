/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2009-2019  Minnesota Department of Transportation
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

import java.awt.Color;
import us.mn.state.dot.tms.client.map.MapObject;
import us.mn.state.dot.tms.client.map.Outline;
import us.mn.state.dot.tms.client.map.Style;
import us.mn.state.dot.tms.client.map.Theme;
import static us.mn.state.dot.tms.client.widget.Widgets.UI;
import us.mn.state.dot.tms.utils.I18N;

/**
 * A simple theme which uses one symbol to draw all segment objects.
 *
 * @author Douglas Lau
 * @author Michael Darter
 */
abstract public class SegmentTheme extends Theme {

	/** Color for rendering gray stations */
	static public final Color GRAY = Color.GRAY;

	/** Color for rendering green stations */
	static public final Color GREEN = new Color(48, 160, 48);

	/** Color for rendering dark green stations */
	static public final Color DGREEN = new Color(6, 98, 6);

	/** Color for rendering yellow stations */
	static public final Color YELLOW = new Color(240, 240, 0);

	/** Color for rendering orange stations */
	static public final Color ORANGE = new Color(255, 192, 0);

	/** Color for rendering red stations */
	static public final Color RED = new Color(208, 0, 0);

	/** Color for rendering violet stations */
	static public final Color VIOLET = new Color(192, 0, 240);

	/** Transparent black outline */
	static public final Outline OUTLINE = Outline.createSolid(
		new Color(0, 0, 0, 192), 0.6f);

	/** Segment style theme for no data */
	static protected final Style NO_DATA_STYLE = new Style(I18N.get(
		"detector.no.data"), OUTLINE, GRAY);

	/** Segment style theme for no detection */
	static protected final Style NO_DETECTION_STYLE = new Style(I18N.get(
		"detector.no.detection"), OUTLINE, Color.WHITE);


	/** R_Node color */
	static public final Color R_NODE_COLOR = new Color(128, 64, 255, 128);

	/** R_node style theme */
	static private final Style R_NODE_STYLE = new Style(I18N.get("r_node"),
		OUTLINE, R_NODE_COLOR);

	/** Create a new segment theme */
	protected SegmentTheme(String name) {
		super(name, new SegmentSymbol(), R_NODE_STYLE);
		addStyle(NO_DETECTION_STYLE);
		addStyle(NO_DATA_STYLE);
		addStyle(R_NODE_STYLE);
	}

	/** Get the style to draw a given map object */
	@Override
	public Style getStyle(MapObject mo) {
		if (mo instanceof MapSegment) {
			MapSegment ms = (MapSegment) mo;
			return getSegmentStyle(ms);
		} else if (mo instanceof ParkingSpace)
			return getStyle((ParkingSpace) mo);
		else
			return R_NODE_STYLE;
	}

	/** Get the style to draw a parking space */
	private Style getStyle(ParkingSpace ps) {
		Float o = ps.getOcc();
		if (null == o)
			return NO_DATA_STYLE;
		else {
			int s = Math.round(o * 18);
			if (s < 150)
				return O_STYLES[0];
			else
				return O_STYLES[1];
		}
	}

	/** Parking occupancy styles */
	static private final Style[] O_STYLES = new Style[] {
		new Style(I18N.get("parking.vacant"), OUTLINE, GREEN),
		new Style(I18N.get("parking.occupied"), OUTLINE, RED),
	};

	/** Get the style to draw a given segment */
	abstract protected Style getSegmentStyle(MapSegment ms);

	/** Get the tooltip text for a given segment */
	@Override
	public String getTip(MapObject mo) {
		if (mo instanceof MapSegment)
			return ((MapSegment) mo).getTip();
		else if (mo instanceof ParkingSpace)
			return ((ParkingSpace) mo).getTip();
		else
			return null;
	}
}
