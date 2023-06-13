/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2019  Iteris Inc.
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
package us.mn.state.dot.tms.server;

import us.mn.state.dot.tms.server.comm.ntcip.mib1204.PrecipSituation;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.VisibilitySituation;
import us.mn.state.dot.tms.WeatherSensorHelper;

/**
 * Present Weather Status, indicated as a collection of characters
 * and symbols, sets of strings and represent a category and intensity
 * For example "+SN" is heavy snow. See essPrecipSituation in NTCIP 1204.
 * @author Michael Darter
 */
public class PresWx {

	public enum Category {

		UNDEFINED("???"),

		// snow
		SNOW("SN"),
		GRAUPEL("SG"),

		// ice
		ICE("IC"),
		ICE_PELLETS("PL"),

		// rain
		RAIN("RA"),
		DRIZZLE("DZ"),
		FREEZING_RAIN("FZ"),

		// visibility
		FOG("FG"),
		HAZE("HZ"),
		DUST("DU"),
		SMOKE("FU");

		/** Category code */
		public final String code;

		/** Constructor */
		private Category(String co) {
			code = co;
		}
	}

	public enum Intensity {

		HEAVY("+"),
		MODERATE(""),
		LIGHT("-");

		/** Intensity code */
		public final String code;

		/** Constructor */
		private Intensity(String co) {
			code = co;
		}
	}

	/** Factory */
	static public PresWx create(WeatherSensorImpl w) {
		PresWx px = new PresWx();
		px = px.append(PrecipSituation.from(w));
		px = px.append(VisibilitySituation.from(w));
		return px;
	}

	/** Present weather condition */
	public final String pres_wx;

	/** Constructor */
	public PresWx(PresWx px, Intensity in, Category ca) {
		pres_wx = px.toString() + in.code + ca.code;
	}

	/** Constructor */
	private PresWx(String pw) {
		pres_wx = (pw != null ? pw : "");
	}

	/** Constructor */
	public PresWx() {
		this("");
	}

	/** Append a new intensity and category to the existing */
	public PresWx append(Intensity in, Category ca) {
		return new PresWx(this, in, ca);
	}

	/** Append Precip Situation */
	private PresWx append(PrecipSituation ps) {
		PresWx px = new PresWx(pres_wx);
		// snow
		if (ps == PrecipSituation.snowSlight)
			px = px.append(Intensity.LIGHT, Category.SNOW);
		else if (ps == PrecipSituation.snowModerate)
			px = px.append(Intensity.MODERATE, Category.SNOW);
		else if (ps == PrecipSituation.snowHeavy)
			px = px.append(Intensity.HEAVY, Category.SNOW);
		// rain
		else if (ps == PrecipSituation.rainSlight)
			px = px.append(Intensity.LIGHT, Category.RAIN);
		else if (ps == PrecipSituation.rainModerate)
			px = px.append(Intensity.MODERATE, Category.RAIN);
		else if (ps == PrecipSituation.rainHeavy)
			px = px.append(Intensity.HEAVY, Category.RAIN);
		// frozen precip
		else if (ps == PrecipSituation.frozenPrecipitationSlight) {
			px = px.append(Intensity.LIGHT, 
				Category.FREEZING_RAIN);
		} else if (ps == PrecipSituation.frozenPrecipitationModerate){
			px = px.append(Intensity.MODERATE, 
				Category.FREEZING_RAIN);
		} else if (ps == PrecipSituation.frozenPrecipitationHeavy) {
			px = px.append(Intensity.HEAVY, 
				Category.FREEZING_RAIN);
		}
		return px;
	}

	/** Append Visibility Situation */
	private PresWx append(VisibilitySituation vs) {
		PresWx px = new PresWx(pres_wx);
		if (vs == VisibilitySituation.fogNotPatchy)
			px = px.append(Intensity.MODERATE, Category.FOG);
		else if (vs == VisibilitySituation.patchyFog)
			px = px.append(Intensity.LIGHT, Category.FOG);
		else if (vs == VisibilitySituation.blowingSnow)
			;
		else if (vs == VisibilitySituation.smoke)
			px = px.append(Intensity.MODERATE, Category.SMOKE);
		else if (vs == VisibilitySituation.seaSpray)
			px = px.append(Intensity.MODERATE, Category.HAZE);
		else if (vs == VisibilitySituation.vehicleSpray)
			px = px.append(Intensity.MODERATE, Category.HAZE);
		else if (vs == VisibilitySituation.blowingDustOrSand)
			px = px.append(Intensity.MODERATE, Category.DUST);
		else if (vs == VisibilitySituation.sunGlare)
			;
		else if (vs == VisibilitySituation.swarmOfInsects)
			px = px.append(Intensity.MODERATE, Category.DUST);
		return px;
	}

	/** Get the present weather condition */
	public String toString() {
		return pres_wx;
	}
}