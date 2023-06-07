/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2009-2016  Minnesota Department of Transportation
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

import us.mn.state.dot.tms.client.map.Style;
import us.mn.state.dot.tms.utils.I18N;

/**
 * A theme for drawing segment objects based on speed thresholds.
 *
 * @author Douglas Lau
 * @author Michael Darter
 */
public class SpeedTheme extends SegmentTheme {

	/** Speed styles */
	static protected final Style[] S_STYLES = 
		new Style[SpeedBand.values().length];
	static {
		SpeedBand[] sbs = SpeedBand.toArray();
		for(int i = 0; i < sbs.length; ++i) {
			SpeedBand b = sbs[i];
			S_STYLES[i] = new Style(b.getDesc(), OUTLINE, b.color);
		}
	}

	/** Create a new speed theme */
	public SpeedTheme() {
		super(I18N.get("units.speed"));
		for (Style s: S_STYLES)
			addStyle(s);
	}

	/** Get the style to draw a given segment */
	@Override
	protected Style getSegmentStyle(MapSegment ms) {
		Integer f = ms.getFlow();
		// no detection
		if(f == null)
			return NO_DETECTION_STYLE;
		Integer spd = ms.getSpeed();
		// detection but no speed
		if(spd == null)
			return NO_DATA_STYLE;	// gray
		SpeedBand sb = SpeedBand.getBand(spd);
		return S_STYLES[sb.ordinal()];
	}
}
