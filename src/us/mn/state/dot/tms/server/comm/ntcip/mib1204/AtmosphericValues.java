/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2019-2022  Minnesota Department of Transportation
 * Copyright (C) 2017-2023  Iteris Inc.
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

import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.VisibilitySituation;
import us.mn.state.dot.tms.utils.JsonBuilder;

import static us.mn.state.dot.tms.server.comm.ntcip.mib1204.MIB1204.*;
import static us.mn.state.dot.tms.units.Distance.Units.*;

/**
 * Atmospheric / visibility sample values.
 *
 * @author Douglas Lau
 * @author Michael Darter, Isaac Nygaard
 */
public class AtmosphericValues implements JsonBuilder.Buildable{
	/** Elevation of reference in meters */
	public final EssDistance reference_elevation =
		new EssDistance("ref_elevation", essReferenceHeight)
			.setRange(-400, 8001);

	/** Height of pressure sensor in meters */
	public final EssDistance pressure_sensor_height =
		new EssDistance("pressure_sensor_height", essPressureHeight);

	/** Atmospheric pressure in pascals */
	public final EssPressure atmospheric_pressure = 
		new EssPressure("atmospheric_pressure", essAtmosphericPressure);

	/** Visibility in decimeters */
	public final EssDistance visibility = 
		new EssDistance("visibility", essVisibility)
			.setUnits(1, DECIMETERS) // 1/10th meter
			.setOutput(1, METERS, 0)
			.setRange(0, 1000001);

	/** Visibility situation enum */
	public final EssEnum<VisibilitySituation> visibility_situation =
		new EssEnum<VisibilitySituation>("visibility_situation", essVisibilitySituation);

	/** Get reference elevation in meters above mean sea level */
	public Integer getReferenceElevation() {
		return reference_elevation.toInteger();
	}

	/** Get atmospheric pressure in pascals */
	public Integer getAtmosphericPressure() {
		return atmospheric_pressure.toInteger();
	}

	/** Get visibility in meters */
	public Integer getVisibility() {
		return visibility.toInteger();
	}

	/** Get the visibility situation */
	public VisibilitySituation getVisibilitySituation() {
		return visibility_situation.get();
	}

	/** Get JSON representation */
	public void toJson(JsonBuilder jb){
		jb.extend(new EssConvertible[]{
			reference_elevation,
			pressure_sensor_height,
			atmospheric_pressure,
			visibility,
			visibility_situation
		});
	}
}
