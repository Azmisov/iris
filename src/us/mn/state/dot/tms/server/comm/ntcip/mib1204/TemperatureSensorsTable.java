package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import static us.mn.state.dot.tms.server.comm.ntcip.mib1204.MIB1204.*;
import us.mn.state.dot.tms.utils.JsonBuilder;

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
	static public class Row implements JsonBuilder.Buildable{
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
			return new StringBuilder()
				.append(EssConvertible.toLogString("isActive",isActive(),number))
				.append(EssConvertible.toLogString(new EssConvertible[]{
					height,
					air_temp,
				}))
				.toString();
		}
		/** Get JSON representation */
		public void toJson(JsonBuilder jb) throws JsonBuilder.Exception{
			jb.object(new EssConvertible[] {
				height,
				air_temp
			});
		}
	}

	@Override
	protected Row createRow(int row_num) {
		return new Row(row_num);
	}

	/** Get the first valid temperature or null on error */
	public EssTemperature getFirstValidTemp(){
		return findRowValue(r -> r.isActive() ? r.air_temp : null);
	}

	/** Get log/debug string representation */
	public String toString(){
		return new StringBuilder()
			.append("TemperatureSensorsTable:")
			.append(num_temp_sensors.toLogString())
			.append(EssConvertible.toLogString("firstValidTemp", getFirstValidTemp()))
			.append(super.toString())
			.toString();
	}
	/** Get JSON representation */
	public void toJson(JsonBuilder jb) throws JsonBuilder.Exception{
		if (!isEmpty()){
			jb.key("temperature_sensor");
			super.toJson(jb);
		}
		jb.extend(new EssConvertible[]{
			wet_bulb_temp,
			dew_point_temp,
			max_air_temp,
			min_air_temp
		});
	}
}
