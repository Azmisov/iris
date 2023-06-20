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
		/** Row number */
		public final int number;
		/** Sensor height in meters */
		public final EssDistance height;
		/** Air temperature in degrees C */
		public final EssTemperature air_temp;

		/** Create a table row */
		private Row(int row) {
			number = row;
			height = new EssDistance("height", essTemperatureSensorHeight, row);
			air_temp = new EssTemperature("air_temp", essAirTemperature, row);
		}

		/** Is the nth sensor active? The temperature sensor table has
		 * no error codes so a sensor is always active if present. */
		public boolean isActive(){
			return true;
		}

		/** Get log/debug string representation */
		public String toString(){
			StringBuilder sb = new StringBuilder();
			sb.append(" isActive(").append(number).append(")=").
				append(isActive());
			sb.append(" height(").append(number).append(")=").
				append(height.toInteger());
			sb.append(" temp(").append(number).append(")=").
				append(air_temp.toInteger());
			return sb.toString();
		}
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

	/** Get the first valid temperature or null on error */
	public Double getFirstValidTemp(){
		var row = findRow(r -> r.isActive());
		return row != null ? row.air_temp.toDouble() : null;
	}

	/** Get log/debug string representation */
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("TemperatureSensorsTable:");
		sb.append(" size=").append(num_temp_sensors);
		sb.append(" firstValidTemp=").append(getFirstValidTemp());
		sb.append(super.toString());
		return sb.toString();
	}
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
