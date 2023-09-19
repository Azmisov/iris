/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2023  Minnesota Department of Transportation
 * Copyright (C) 2023  Iteris
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
package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Number formatting utility.
 *
 * @author Douglas Lau
 * @authro Isaac Nygaard
 */
public class Num {

	/** Create a number formatter for a given number of digits */
	static private DecimalFormat createFormatter(int digits) {
		// used for Json serialization, so only use compatible formats
		// TODO: this will be localized, so can be incorrect (e.g. comma for decimal point)
		DecimalFormat f = new DecimalFormat();
		f.setRoundingMode(RoundingMode.HALF_EVEN);
		f.setGroupingUsed(false);
		f.setMaximumFractionDigits(digits);
		f.setMinimumFractionDigits(0); // since we format integers too
		return f;
	}

	/** Format long/int to string with specified fractional digits, or null if
	 * input is null */
	static public String format(Long num, int digits) {
		return num != null ? createFormatter(digits).format(num) : null;
	}

	/** Format double/float to string with specified fractional digits, or null
	 * if input is null */
	static public String format(Double num, int digits) {
		return num != null ? createFormatter(digits).format(num) : null;
	}
}
