/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2009-2016  Minnesota Department of Transportation
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
 */
public class SpeedTheme extends SegmentTheme {

	/** Speed styles */
	static private final Style[] S_STYLES = new Style[] {
		new Style(I18N.get("units.speed.low"), OUTLINE, RED),
		new Style(I18N.get("units.speed.low.med"), OUTLINE, VIOLET),
		new Style(I18N.get("units.speed.medium"), OUTLINE, YELLOW),
		new Style(I18N.get("units.speed.med.high"), OUTLINE, GREEN),
		new Style(I18N.get("units.speed.high"), OUTLINE, DGREEN)
	};

	/** Create a new speed theme */
	public SpeedTheme() {
		super(I18N.get("units.speed"));
		for (Style s: S_STYLES)
			addStyle(s);
	}

	/** Get the style to draw a given segment */
	@Override
	protected Style getSegmentStyle(MapSegment ms) {
		Integer spd = ms.getSpeed();
		if(spd == null)
			return DEFAULT_STYLE;	// gray
		if(spd <= 35)
			return S_STYLES[0];	// red
		if(spd <= 45)
			return S_STYLES[1];	// violet
		if(spd <= 55)
			return S_STYLES[2];	// yellow
		if(spd <= 65)
			return S_STYLES[3];	// green
		return S_STYLES[4];		// dark green
	}
}
