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

import static us.mn.state.dot.tms.server.comm.ntcip.mib1204.MIB1204.*;

import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.VisibilitySituation;
import static us.mn.state.dot.tms.units.Distance.Units.*;

/**
 * Atmospheric / visibility sample values.
 *
 * @author Douglas Lau
 * @author Michael Darter, Isaac Nygaard
 */
public class AtmosphericValues {
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
		return visibility_situation.get(e -> {
			return VisibilitySituation.isValid(e) ? e : null;
		});
	}

	/** Get JSON representation */
	public String toJson() {
		StringBuilder sb = new StringBuilder();
		sb.append(reference_elevation.toJson());
		sb.append(pressure_sensor_height.toJson());
		sb.append(atmospheric_pressure.toJson());
		sb.append(visibility.toJson());
		sb.append(visibility_situation.toJson());
		return sb.toString();
	}
}
