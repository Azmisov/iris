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
import us.mn.state.dot.tms.server.comm.snmp.ASN1Enum;
import us.mn.state.dot.tms.utils.Json;
import us.mn.state.dot.tms.server.comm.ntcip.EssValues;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.CloudSituation;
import us.mn.state.dot.tms.units.Interval;

/**
 * Solar radiation sample values.
 *
 * @author Douglas Lau
 * @author Isaac Nygaard
 */
public class RadiationValues extends EssValues{
	/** Total daily minutes of sun (minutes) */
	public final EssInterval total_sun = 
		new EssInterval("total_sun", essTotalSun);

	/** Cloud situation */
	public final ASN1Enum<CloudSituation> cloud_situation =
		new ASN1Enum<CloudSituation>(CloudSituation.class,
		essCloudSituation.node);

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
			.setUnits(1, Interval.Units.SECONDS)
			.setRange(0, 86401, 0);

	/** Solar radiation over 24 hours (Joules / m^2; deprecated in V2) */
	public final EssNumber solar_radiation = 
		new EssNumber("solar_radiation", essSolarRadiation);

	/** Get the total sun minutes */
	public Integer getTotalSun() {
		return total_sun.toInteger();
	}

	/** Get the cloud situation */
	public CloudSituation getCloudSituation() {
		CloudSituation cs = cloud_situation.getEnum();
		return (cs != CloudSituation.undefined) ? cs : null;
	}

	/** Get the total radiation period (seconds) */
	public Integer getTotalRadiationPeriod() {
		return total_radiation_period.toInteger();
	}

	/** Get the solar radiation (joules / m^2) */
	public Integer getSolarRadiation() {
		return solar_radiation.toInteger();
	}

	/** Get JSON representation */
	public String toJson() {
		StringBuilder sb = new StringBuilder();
		sb.append(total_sun.toJson());
		sb.append(Json.str("cloud_situation", getCloudSituation()));
		Integer s = getSolarRadiation();
		if (s != null) {
			sb.append(Json.num("solar_radiation", s));
		} else {
			sb.append(instantaneous_terrestrial.toJson());
			sb.append(instantaneous_solar.toJson());
			sb.append(total_radiation.toJson());
			sb.append(total_radiation_period.toJson());
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "TODO";
	}
}
