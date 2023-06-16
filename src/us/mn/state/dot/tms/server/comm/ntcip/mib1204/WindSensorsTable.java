package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import static us.mn.state.dot.tms.server.comm.ntcip.mib1204.MIB1204.*;
import us.mn.state.dot.tms.server.comm.snmp.ASN1Enum;
import us.mn.state.dot.tms.utils.Json;
import us.mn.state.dot.tms.server.comm.ntcip.EssValues;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.WindSituation;

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
		EssNumber.Count("num_sensors", windSensorTableNumSensors);

	public WindSensorsTable(){
		setSensorCount(num_sensors);
	}

	/** Wind sensor height in meters (deprecated in V2) */
	public final EssDistance height =
		new EssDistance("height", essWindSensorHeight);

	/** Wind situation.
	 * Note: this object not supported by all vendors */
	public final ASN1Enum<WindSituation> situation =
		new ASN1Enum<WindSituation>(WindSituation.class,
		essWindSituation.node);

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
	static public class Row extends EssValues {
		public final EssDistance height;
		public final EssSpeed avg_speed;
		public final EssAngle avg_direction;
		public final EssSpeed spot_speed;
		public final EssAngle spot_direction;
		public final EssSpeed gust_speed;
		public final EssAngle gust_direction;
		public final ASN1Enum<WindSituation> situation;

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
			situation = new ASN1Enum<WindSituation>(
				WindSituation.class, windSensorSituation.node, row);
		}

		private WindSituation getSituation() {
			WindSituation ws = situation.getEnum();
			if (ws == WindSituation.undefined || ws == WindSituation.unknown)
				ws = null;
			return ws;
		}

		public String toString(){
			return toJson();
		}
		/** Get JSON representation */
		public String toJson() {
			StringBuilder sb = new StringBuilder();
			sb.append('{');
			sb.append(height.toJson());
			sb.append(avg_speed.toJson());
			sb.append(avg_direction.toJson());
			sb.append(spot_speed.toJson());
			sb.append(spot_direction.toJson());
			sb.append(gust_speed.toJson());
			sb.append(gust_direction.toJson());
			sb.append(Json.str("situation", getSituation()));
			// remove trailing comma
			if (sb.charAt(sb.length() - 1) == ',')
				sb.setLength(sb.length() - 1);
			sb.append("},");
			return sb.toString();
		}
	}

	@Override
	protected Row createRow(int row_num) {
		return new Row(row_num);
	}

	/** Get the wind situation */
	private WindSituation getSituation() {
		WindSituation ws = situation.getEnum();
		if (ws == WindSituation.undefined || ws == WindSituation.unknown)
			ws = null;
		return ws;
	}

	/** Get two minute average wind speed */
	public EssSpeed getAvgSpeed() {
		return fallback(r -> r.avg_speed, avg_speed);
	}

	/** Get two minute average wind direction */
	public EssAngle getAvgDir() {
		return fallback(r -> r.avg_direction, avg_direction);
	}

	/** Get spot wind speed */
	public EssSpeed getSpotSpeed() {
		return fallback(r -> r.spot_speed, spot_speed);
	}

	/** Get spot wind direction */
	public EssAngle getSpotDir() {
		return fallback(r -> r.spot_direction, spot_direction);
	}

	/** Get ten minute max gust wind speed */
	public EssSpeed getGustSpeed() {
		return fallback(r -> r.gust_speed, gust_speed);
	}

	/** Get ten minute max gust wind direction */
	public EssAngle getGustDir() {
		return fallback(r -> r.gust_direction, gust_direction);
	}

	/** Get JSON representation */
	public String toJson() {
		StringBuilder sb = new StringBuilder();
		sb.append("\"wind_sensor\":[");
		if (!table_rows.isEmpty()) {
			sb.append(super.toJson());
		} else {
			sb.append('{');
			sb.append(height.toJson());
			sb.append(avg_speed.toJson());
			sb.append(avg_direction.toJson());
			sb.append(spot_speed.toJson());
			sb.append(spot_direction.toJson());
			sb.append(gust_speed.toJson());
			sb.append(gust_direction.toJson());
			sb.append(Json.str("situation", getSituation()));
			// remove trailing comma
			if (sb.charAt(sb.length() - 1) == ',')
				sb.setLength(sb.length() - 1);
			sb.append("}");			
		}
		sb.append("],");
		return sb.toString();
	}
}
