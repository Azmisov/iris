/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2022  Minnesota Department of Transportation
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

import static us.mn.state.dot.tms.server.comm.ntcip.mib1204.MIB1204.*;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.CloudSituation;
import static us.mn.state.dot.tms.units.Interval.Units.*;
import us.mn.state.dot.tms.utils.JsonBuilder;

/**
 * Solar radiation sample values.
 *
 * @author Douglas Lau
 * @author Isaac Nygaard
 */
public class RadiationValues implements JsonBuilder.Buildable{
	/** Total daily minutes of sun (minutes) */
	public final EssInterval total_sun = 
		new EssInterval("total_sun", essTotalSun);

	/** Cloud situation */
	// overwritten manually to `clear` if no device support
	public final EssEnum<CloudSituation> cloud_situation =
		EssEnum.make(CloudSituation.class, "cloud_situation", essCloudSituation);

	/** Instantaneous terrestrial radiation (watts / m^2) */
	public final EssNumber instantaneous_terrestrial =
		EssNumber.Radiation("instantaneous_terrestrial_radiation", essInstantaneousTerrestrialRadiation);

	/** Instantaneous solar radiation (watts / m^2) */
	public final EssNumber instantaneous_solar =
		EssNumber.Radiation("instantaneous_solar_radiation", essInstantaneousSolarRadiation);

	/** Total radiation during collection period (watts / m^2) */
	public final EssNumber total_radiation =
		EssNumber.Radiation("total_radiation", essTotalRadiation);

	/** Total radiation period (seconds) */
	// technically all values are valid for essTotalRadiationPeriod, but our
	// code we treat zero as error
	public final EssInterval total_radiation_period =
		new EssInterval("total_radiation_period", essTotalRadiationPeriod)
			.setUnits(1, SECONDS)
			.setRange(0, 86401, 0);

	/** Solar radiation over 24 hours (Joules / m^2; deprecated in V2) */
	public final EssNumber solar_radiation = 
		new EssNumber("solar_radiation", essSolarRadiation);

	/** Get JSON representation */
	public void toJson(JsonBuilder jb){
		jb.extend(new EssConvertible[] {
			total_sun,
			cloud_situation
		});
		if (!solar_radiation.isNull()){
			jb.extend(solar_radiation);
		} else {
			jb.extend(new EssConvertible[]{
				instantaneous_terrestrial,
				instantaneous_solar,
				total_radiation,
				total_radiation_period
			});
		}
	}
}
