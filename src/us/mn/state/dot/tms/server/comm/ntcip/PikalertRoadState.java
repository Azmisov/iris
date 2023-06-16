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
package us.mn.state.dot.tms.server.comm.ntcip;

import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SurfaceBlackIceSignal;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SurfaceStatus;

/**
 * Pikalert Road State
 * @author Michael Darter
 */
public enum PikalertRoadState {

	RS_NO_REPORT("NoReport"),		//0
	RS_DRY("Dry"),				//1
	RS_MOIST("Moist"),			//2
	RS_MOIST_CHEM_TMT("MoistChemTmt"),	//3
	RS_WET("Wet"),				//4
	RS_WET_CHEM_TMT("WetChemTmt"),		//5
	RS_ICE("Ice"),				//6
	RS_FROST("Frost"),			//7
	RS_SNOW("Snow"),			//8
	RS_SNOW_ICE_WACH("SnowIceWach"),	//9
	RS_SNOW_ICE_WARN("SnowIceWarn"),	//10
	RS_WET_ABOVE_FRZ("WetAboveFrz"),	//11
	RS_WET_BELOW_FRZ("WetBelowFrz"),	//12
	RS_ABSORPTION("Absorption"),		//13
	RS_ABSORPTION_DEWPT("AbsorptionDewPt"),	//14
	RS_DEW("Dew"),				//15
	RS_BLACK_ICE("BlackIce"),		//16
	RS_OTHER("Other"),			//17
	RS_SLUSH("Slush");			//18

	/** Description string */
	public final String description;

	/** Constructor */
	private PikalertRoadState(String d) {
		description = d;
	}

	/** Get an enum from an ordinal value */
	static public PikalertRoadState fromOrdinal(int o) {
		if (o >= 0 && o < values().length)
			return values()[o];
		else
			return RS_NO_REPORT;
	}

	/** Determine the current road condition status */
	static public PikalertRoadState convert(SurfaceStatus pss,
        SurfaceBlackIceSignal bis) 
	{
		// check black ice first
		if (bis == SurfaceBlackIceSignal.blackIce)
			return RS_BLACK_ICE;

		// check surface statuses
		if (pss == SurfaceStatus.undefined)
			return RS_NO_REPORT;
		else if (pss == SurfaceStatus.other)
			return RS_OTHER;
		else if (pss == SurfaceStatus.error)
			return RS_NO_REPORT;
		else if (pss == SurfaceStatus.dry)
			return RS_DRY;
		else if (pss == SurfaceStatus.traceMoisture)
			return RS_MOIST;
		else if (pss == SurfaceStatus.wet)
			return RS_WET;
		else if (pss == SurfaceStatus.chemicallyWet)
			return RS_WET_CHEM_TMT;
		else if (pss == SurfaceStatus.iceWarning)
			return RS_SNOW_ICE_WARN;
		else if (pss == SurfaceStatus.iceWatch)
			return RS_SNOW_ICE_WACH;
		else if (pss == SurfaceStatus.snowWarning)
			return RS_SNOW_ICE_WARN;
		else if (pss == SurfaceStatus.snowWatch)
			return RS_SNOW_ICE_WACH;
		else if (pss == SurfaceStatus.absorption)
			return RS_ABSORPTION;
		else if (pss == SurfaceStatus.dew)
			return RS_DEW;
		else if (pss == SurfaceStatus.frost)
			return RS_FROST;
		else if (pss == SurfaceStatus.absorptionAtDewpoint)
			return RS_ABSORPTION_DEWPT;
		else
			return RS_NO_REPORT;
	}
}