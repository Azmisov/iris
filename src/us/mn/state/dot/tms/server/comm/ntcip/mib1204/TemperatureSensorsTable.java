package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import static us.mn.state.dot.tms.server.comm.ntcip.mib1204.MIB1204.*;
import us.mn.state.dot.tms.server.comm.ntcip.EssValues;

/**
 * Temperature sensors data table, where each table row contains data read from
 * a single temperature sensor within the same controller.
 *
 * @author Douglas Lau
 * @copyright 2019-2022 Minnesota Department of Transportation
 * @author Michael Darter, Isaac Nygaard
 * @copyright 2017-2023 Iteris Inc.
 * @license GPL-2.0
 */
public class TemperatureSensorsTable extends EssTable<TemperatureSensorsTable.Row>{
	/** Number of sensors in table */
	public final EssNumber num_temp_sensors =
		EssNumber.Count("num_temp_sensors", essNumTemperatureSensors);

	public TemperatureSensorsTable(){
		setSensorCount(num_temp_sensors);
	}

	/** Wet-bulb temperature */
	public final EssTemperature wet_bulb_temp =
		new EssTemperature("wet_bulb_temp", essWetbulbTemp);

	/** Dew point temperature */
	public final EssTemperature dew_point_temp =
		new EssTemperature("dew_point_temp", essDewpointTemp);

	/** Maximum air temperature */
	public final EssTemperature max_air_temp =
		new EssTemperature("max_air_temp", essMaxTemp);

	/** Minimum air temperature */
	public final EssTemperature min_air_temp =
		new EssTemperature("min_air_temp", essMinTemp);

	/** Temperature table row */
	static public class Row extends EssValues{
		public final EssDistance height;
		public final EssTemperature air_temp;

		/** Create a table row */
		private Row(int row) {
			height = new EssDistance("height", essTemperatureSensorHeight, row);
			air_temp = new EssTemperature("air_temp", essAirTemperature, row);
		}

		public String toString(){ return null; }
		/** Get JSON representation */
		public String toJson() {
			StringBuilder sb = new StringBuilder();
			sb.append('{');
			sb.append(height.toJson());
			sb.append(air_temp.toJson());
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

	/** Get the dew point temp */
	public Integer getDewPointTempC() {
		return dew_point_temp.toInteger();
	}

	/** Get the max temp */
	public Integer getMaxTempC() {
		return max_air_temp.toInteger();
	}

	/** Get the min temp */
	public Integer getMinTempC() {
		return min_air_temp.toInteger();
	}

	public String toString(){ return null; }
	/** Get JSON representation */
	public String toJson() {
		StringBuilder sb = new StringBuilder();
		if (table_rows.size() > 0) {
			sb.append("\"temperature_sensor\":[");
			sb.append(super.toJson());
			sb.append("],");
		}
		sb.append(wet_bulb_temp.toJson());
		sb.append(dew_point_temp.toJson());
		sb.append(max_air_temp.toJson());
		sb.append(min_air_temp.toJson());
		return sb.toString();
	}
}
