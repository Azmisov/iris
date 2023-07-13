package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import static us.mn.state.dot.tms.server.comm.ntcip.mib1204.MIB1204.*;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.PrecipSituation;
import us.mn.state.dot.tms.utils.JsonBuilder;
import static us.mn.state.dot.tms.units.Speed.Units.*;
import static us.mn.state.dot.tms.units.Distance.Units.*;

/**
 * Precipitation sensor sample values.
 *
 * @author Douglas Lau
 * @copyright 2019-2023 Minnesota Department of Transportation
 * @author Michael Darter, Isaac Nygaard
 * @copyright 2017-2023 Iteris, Inc
 * @license GPL-2.0
 */
public class PrecipitationValues implements JsonBuilder.Buildable{
	public final EssNumber num_level_sensors =
		EssNumber.Count("num_level_sensors", waterLevelSensorTableNumSensors);

	/** Water depth in cm */
	public final EssDistance water_depth =
		new EssDistance("water_depth", essWaterDepth)
			.setUnits(1, CENTIMETERS);

	/** Adjacent snow depth in cm */
	public final EssDistance snow_depth =
		new EssDistance("snow_depth", essAdjacentSnowDepth)
			.setUnits(1, CENTIMETERS)
			.setRange(0, 3001);

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
			.setUnits(0.36, MMPH)
			.setOutput(1, MMPH, 1);

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
	public final EssEnum<PrecipSituation> precip_situation =
		EssEnum.make(PrecipSituation.class, "precip_situation", essPrecipSituation);

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
		return precip_situation.get();
	}

	/** Get JSON representation */
	public void toJson(JsonBuilder jb){
		jb.extend(new EssConvertible[]{
			water_depth,
			relative_humidity,
			precip_rate,
			precip_1_hour,
			precip_3_hours,
			precip_6_hours,
			precip_12_hours,
			precip_24_hours,
			precip_situation,
		});
	}
}
