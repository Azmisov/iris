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
import java.util.EnumMap;

/**
 * Pikalert Road State
 * @author Michael Darter
 */
public enum PikalertRoadState {

	RS_NO_REPORT("NoReport"),					//0
	RS_DRY("Dry"),							//1
	RS_MOIST("Moist"),						//2
	RS_MOIST_CHEM_TMT("MoistChemTmt"),		//3
	RS_WET("Wet"),							//4
	RS_WET_CHEM_TMT("WetChemTmt"),			//5
	RS_ICE("Ice"),							//6
	RS_FROST("Frost"),						//7
	RS_SNOW("Snow"),							//8
	RS_SNOW_ICE_WACH("SnowIceWach"),			//9
	RS_SNOW_ICE_WARN("SnowIceWarn"),			//10
	RS_WET_ABOVE_FRZ("WetAboveFrz"),			//11
	RS_WET_BELOW_FRZ("WetBelowFrz"),			//12
	RS_ABSORPTION("Absorption"),				//13
	RS_ABSORPTION_DEWPT("AbsorptionDewPt"),	//14
	RS_DEW("Dew"),							//15
	RS_BLACK_ICE("BlackIce"),					//16
	RS_OTHER("Other"),						//17
	RS_SLUSH("Slush");						//18

	static private EnumMap<SurfaceStatus, PikalertRoadState> SurfaceStatusMapping
		= new EnumMap<>(SurfaceStatus.class);
	static {
		var s = SurfaceStatusMapping;
		s.put(SurfaceStatus.other, RS_OTHER);
		s.put(SurfaceStatus.error, RS_NO_REPORT);
		s.put(SurfaceStatus.dry, RS_DRY);
		s.put(SurfaceStatus.traceMoisture, RS_MOIST);
		s.put(SurfaceStatus.wet, RS_WET);
		s.put(SurfaceStatus.chemicallyWet, RS_WET_CHEM_TMT);
		s.put(SurfaceStatus.iceWarning, RS_SNOW_ICE_WARN);
		s.put(SurfaceStatus.iceWatch, RS_SNOW_ICE_WACH);
		s.put(SurfaceStatus.snowWarning, RS_SNOW_ICE_WARN);
		s.put(SurfaceStatus.snowWatch, RS_SNOW_ICE_WACH);
		s.put(SurfaceStatus.absorption, RS_ABSORPTION);
		s.put(SurfaceStatus.dew, RS_DEW);
		s.put(SurfaceStatus.frost, RS_FROST);
		s.put(SurfaceStatus.absorptionAtDewpoint, RS_ABSORPTION_DEWPT);
	}

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
	static public PikalertRoadState convert(
		SurfaceStatus pss, SurfaceBlackIceSignal bis
	) {
		// check black ice first
		if (bis == SurfaceBlackIceSignal.blackIce)
			return RS_BLACK_ICE;

		// check surface statuses
		if (pss != null){
			var mapped = SurfaceStatusMapping.get(pss);
			if (mapped != null)
				return mapped;
		}
		
		return RS_NO_REPORT;
	}
}