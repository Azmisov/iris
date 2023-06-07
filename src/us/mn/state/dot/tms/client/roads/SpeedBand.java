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
package us.mn.state.dot.tms.client.roads;

import java.awt.Color;
import java.util.LinkedList;
import us.mn.state.dot.tms.utils.I18N;

/**
 * Speed band enumeration.
 *
 * @author Michael Darter
 */
public enum SpeedBand {

	/** Speed bands (order must be increasing speeds, 
	 * last speed must be Integer.MAX_VALUE) */
	SB_1(35, "units.speed.low", SegmentTheme.RED),
	SB_2(45, "units.speed.low.med", SegmentTheme.VIOLET),
	SB_3(55, "units.speed.medium", SegmentTheme.YELLOW),
	SB_4(65, "units.speed.med.high", SegmentTheme.GREEN),
	SB_5(Integer.MAX_VALUE, "units.speed.high", SegmentTheme.DGREEN);

	/** Create a new speed band
	 * @param maxsp Maximum inclusive speed for band in mph
	 * @param i18n ID of I18N string describing speed band
	 * @param c Band color */
	private SpeedBand(int maxsp, String i18n, Color c) {
		max_speed_mph = maxsp;
		desc_i18n_id = i18n;
		color = c;
	}

	/** Get array of speed bands */
	static public SpeedBand[] toArray() {
		LinkedList<SpeedBand> sbs = new LinkedList<SpeedBand>();
		for(SpeedBand sb : SpeedBand.values())
			sbs.add(sb);
		return sbs.toArray(new SpeedBand[0]);
	}

	/** Get the band associated with the specified speed in mph */
	static public SpeedBand getBand(int spd) {
		for(SpeedBand sb: SpeedBand.values()) {
			if(spd <= sb.max_speed_mph)
				return sb;
		}
		assert false;
		return SB_1;
	}

	/** Maximum inclusive speed for speed band */
	public final int max_speed_mph;

	/** I18N string id for speed band description */
	public final String desc_i18n_id;

	/** Band color */
	public final Color color;

	/** Get the description of the speed band */
	public String getDesc() {
		return I18N.get(desc_i18n_id);
	}
}
