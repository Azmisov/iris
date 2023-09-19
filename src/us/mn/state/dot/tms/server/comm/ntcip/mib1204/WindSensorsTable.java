package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import static us.mn.state.dot.tms.server.comm.ntcip.mib1204.MIB1204.*;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.WindSituation;
import us.mn.state.dot.tms.utils.JsonBuilder;
import static us.mn.state.dot.tms.units.Speed.Units.*;

/**
 * Wind sensors data table, where each table row contains data read from a
 * single wind sensor within the same controller.
 *
 * @author Douglas Lau
 * @copyright 2019-2022 Minnesota Department of Transportation
 * @author Michael Darter, Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public class WindSensorsTable extends EssTable<WindSensorsTable.Row>{
	/** Number of sensors in table (V2+) */
	public final EssNumber num_sensors =
		EssNumber.Count("num_wind_sensors", windSensorTableNumSensors);

	public WindSensorsTable(){
		setSensorCount(num_sensors);
	}

	/** Wind sensor height in meters (deprecated in V2) */
	public final EssDistance height =
		new EssDistance("height", essWindSensorHeight);

	/** Wind situation.
	 * Note: this object not supported by all vendors */
	public final EssEnum<WindSituation> situation =
		EssEnum.make(WindSituation.class, "situation", essWindSituation);

	/** Two minute average wind speed in m/s (deprecated in V2) */
	public final EssSpeed avg_speed =
		new EssSpeed("avg_speed", essAvgWindSpeed);

	/** Two minute average wind direction (deprecated in V2) */
	public final EssAngle avg_direction =
		new EssAngle("avg_direction", essAvgWindDirection);

	/** Spot wind speed in m/s  (deprecated in V2) */
	public final EssSpeed spot_speed =
		new EssSpeed("spot_speed", essSpotWindSpeed);

	/** Spot wind direction (deprecated in V2) */
	public final EssAngle spot_direction =
		new EssAngle("spot_direction", essSpotWindDirection);

	/** Ten minute max gust wind speed in m/s (deprecated in V2) */
	public final EssSpeed gust_speed =
		new EssSpeed("gust_speed", essMaxWindGustSpeed);

	/** Ten minute max gust wind direction (deprecated in V2) */
	public final EssAngle gust_direction =
		new EssAngle("gust_direction", essMaxWindGustDir);

	/** Wind sensor row */
	static public class Row implements JsonBuilder.Buildable {
		public final EssDistance height;
		public final EssSpeed avg_speed;
		public final EssAngle avg_direction;
		public final EssSpeed spot_speed;
		public final EssAngle spot_direction;
		public final EssSpeed gust_speed;
		public final EssAngle gust_direction;
		/** Wind situation enum */
		public final EssEnum<WindSituation> situation;

		/** Create a table row */
		private Row(int row) {
			height = new EssDistance("height", windSensorHeight, row);
			avg_speed =
				new EssSpeed("avg_speed", windSensorAvgSpeed, row);
			avg_direction =
				new EssAngle("avg_direction", windSensorAvgDirection, row);
			spot_speed =
				new EssSpeed("spot_speed", windSensorSpotSpeed, row);
			spot_direction =
				new EssAngle("spot_direction", windSensorSpotDirection, row);
			gust_speed =
				new EssSpeed("gust_speed", windSensorGustSpeed, row);
			gust_direction =
				new EssAngle("gust_direction", windSensorGustDirection, row);
			// Note: this object not supported by all vendors
			situation =
				EssEnum.make(WindSituation.class, "situation", windSensorSituation, row);
		}

		/** Get JSON representation */
		public void toJson(JsonBuilder jb){
			jb.object(new EssConvertible[]{
				height,
				avg_speed,
				avg_direction,
				spot_speed,
				spot_direction,
				gust_speed,
				gust_direction,
				situation
			});
		}
	}

	@Override
	protected Row createRow(int row_num) {
		return new Row(row_num);
	}

	// TODO: so Json was previously specified to use m/s, but the storage
	// in WeatherSensorImpl is km/h; I'm leaving as is, but think we should
	// settle on a single unit to use for both

	/** Get two minute average wind speed */
	public Integer getAvgSpeed() {
		return fallback(r -> r.avg_speed, avg_speed).get(v -> v.round(KPH));
	}

	/** Get two minute average wind direction */
	public Integer getAvgDir() {
		return fallback(r -> r.avg_direction, avg_direction).toInteger();
	}

	/** Get spot wind speed */
	public Integer getSpotSpeed() {
		return fallback(r -> r.spot_speed, spot_speed).get(v -> v.round(KPH));
	}

	/** Get spot wind direction */
	public Integer getSpotDir() {
		return fallback(r -> r.spot_direction, spot_direction).toInteger();
	}

	/** Get ten minute max gust wind speed */
	public Integer getGustSpeed() {
		return fallback(r -> r.gust_speed, gust_speed).get(v -> v.round(KPH));
	}

	/** Get ten minute max gust wind direction */
	public Integer getGustDir() {
		return fallback(r -> r.gust_direction, gust_direction).toInteger();
	}

	/** Get JSON representation */
	public void toJson(JsonBuilder jb){
		jb.key("wind_sensor");
		if (!isEmpty())
			super.toJson(jb);
		else {
			// emulate sensor table w/ V1 properties
			jb.beginList()
				.object(new EssConvertible[]{
					height,
					avg_speed,
					avg_direction,
					spot_speed,
					spot_direction,
					gust_speed,
					gust_direction,
					situation
				})
				.endList();
		}
	}
}
