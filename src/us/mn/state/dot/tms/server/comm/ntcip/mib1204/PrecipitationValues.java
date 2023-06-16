package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import static us.mn.state.dot.tms.server.comm.ntcip.mib1204.MIB1204.*;

import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.PrecipSituation;
import us.mn.state.dot.tms.server.comm.snmp.ASN1Enum;
import us.mn.state.dot.tms.units.Speed;
import us.mn.state.dot.tms.units.Distance.Units;
import us.mn.state.dot.tms.utils.Json;

/**
 * Precipitation sensor sample values.
 *
 * @author Douglas Lau
 * @copyright 2019-2023 Minnesota Department of Transportation
 * @author Michael Darter, Isaac Nygaard
 * @copyright 2017-2023 Iteris, Inc
 * @license GPL-2.0
 */
public class PrecipitationValues {
	/** Water depth in cm */
	public final EssDistance water_depth =
		new EssDistance("water_depth", essWaterDepth)
			.setUnits(1, Units.CENTIMETERS);

	/** Relative humidity in percent from 0-100 */
	public final EssNumber relative_humidity =
		EssNumber.Percent("relative_humidity", essRelativeHumidity);

	/** Precipitation rate (tenths of grams/m^2/s) */
	// starting from tenths of g/m^2/s
	// divide by 10,000 => kg/m^2/s
	// equivalent to L/m^2/s (for water)
	// cancel out 0.001 m^3/m^2 => mm/s
	// multiply by 3600 => mm/hr
	// 3600 / 10,000 = 0.36
	public final EssSpeed precip_rate = 
		new EssSpeed("precip_rate", essPrecipRate)
			.setUnits(0.36, Speed.Units.MMPH)
			.setOutput(1, Speed.Units.MMPH, 1);

	/** One hour precipitation depth in mm per m^2 */
	public final EssDistance precip_1_hour =
		EssDistance.PrecipRate("precip_1_hour", essPrecipitationOneHour);

	/** Three hour precipitation depth in mm per m^2 */
	public final EssDistance precip_3_hours =
		EssDistance.PrecipRate("precip_3_hours", essPrecipitationThreeHours);

	/** Six hour precipitation depth in mm per m^2 */
	public final EssDistance precip_6_hours =
		EssDistance.PrecipRate("precip_6_hours", essPrecipitationSixHours);

	/** Twelve hour precipitation depth in mm per m^2 */
	public final EssDistance precip_12_hours =
		EssDistance.PrecipRate("precip_12_hours", essPrecipitationTwelveHours);

	/** Twenty-four hour precipitation depth in mm per m^2 */
	public final EssDistance precip_24_hours =
		EssDistance.PrecipRate("precip_24_hours", essPrecipitation24Hours);

	/** Precipitation situation */
	public final ASN1Enum<PrecipSituation> precip_situation =
		new ASN1Enum<PrecipSituation>(PrecipSituation.class,
		essPrecipSituation.node);

	/** Get the water depth in cm, else null if missing/invalid */
	public Integer getWaterDepth() {
		return water_depth.toInteger();
	}

	/** Get the precipitation rate in mm/hr */
	public Integer getPrecipRate() {
		return precip_rate.toInteger();
	}

	/** Get the one hour precipitation total in mm */
	public Integer getPrecip1Hour() {
		return precip_1_hour.toInteger();
	}

	/** Get the precipitation situation */
	public PrecipSituation getPrecipSituation() {
		PrecipSituation ps = precip_situation.getEnum();
		return (ps != PrecipSituation.undefined &&
		        ps != PrecipSituation.unknown)
		      ? ps
		      : null;
	}

	/** Get JSON representation */
	public String toJson() {
		StringBuilder sb = new StringBuilder();
		sb.append(water_depth.toJson());
		sb.append(relative_humidity.toJson());
		sb.append(precip_rate.toJson());
		sb.append(precip_1_hour.toJson());
		sb.append(precip_3_hours.toJson());
		sb.append(precip_6_hours.toJson());
		sb.append(precip_12_hours.toJson());
		sb.append(precip_24_hours.toJson());
		sb.append(Json.str("precip_situation", getPrecipSituation()));
		return sb.toString();
	}
}
