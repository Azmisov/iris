package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import static us.mn.state.dot.tms.server.comm.ntcip.mib1204.MIB1204.*;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SubSurfaceSensorError;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SubSurfaceType;
import static us.mn.state.dot.tms.units.Distance.Units.*;
import us.mn.state.dot.tms.utils.JsonBuilder;

/**
 * SubSurface sensors data table, where each table row contains data read from a
 * single sensor within the same controller.
 *
 * @author Michael Darter, Isaac Nygaard
 * @copyright 2017-2023 Iteris, Inc
 * @author Douglas Lau
 * @copyright 2019-2023  Minnesota Department of Transportation
 * @license GPL-2.0
 */
public class SubSurfaceSensorsTable extends EssTable<SubSurfaceSensorsTable.Row>{
	/** Number of temperature sensors in table */
	public final EssNumber num_sensors =
		EssNumber.Count("subsurface_sensors", numEssSubSurfaceSensors);
	
	public SubSurfaceSensorsTable(){
		setSensorCount(num_sensors);
	}

	/** Table row */
	static public class Row implements JsonBuilder.Buildable{
		/** Row/sensor number */
		public final int number;
		/** Sensor location as a display string */
		public final EssString location;
		/** Subsurface type enum */
		public final EssEnum<SubSurfaceType> sub_surface_type;
		/** Subsurface depth in meters */
		public final EssDistance depth;
		/** Subsurface temperature in celcius */
		public final EssTemperature temp;
		/** Moisture in percent 0-100 */
		public final EssNumber moisture;
		/** Subsurface sensor error enum */
		public final EssEnum<SubSurfaceSensorError> sensor_error;

		/** Create a table row */
		private Row(int row) {
			number = row;
			location =
				new EssString("location", essSubSurfaceSensorLocation, row);
			sub_surface_type =
				new EssEnum<SubSurfaceType>("sub_surface_type", essSubSurfaceType, row);
			depth = 
				new EssDistance("depth", essSubSurfaceSensorDepth, row)
					.setUnits(1, CENTIMETERS)
					.setOutput(1, METERS, 2);
			temp =
				new EssTemperature("temp", essSubSurfaceTemperature, row);
			moisture =
				EssNumber.Percent("moisture", essSubSurfaceMoisture, row);
			sensor_error =
				new EssEnum<SubSurfaceSensorError>("sensor_error", essSubSurfaceSensorError, row);
		}

		/** Is this sensor active? */
		public boolean isActive(){
			// These values were determined empirically, with valid
			// surface temps present when sensor err was one of these:
			var se = sensor_error.get();
			return (se == SubSurfaceSensorError.none || 
				se == SubSurfaceSensorError.noResponse ||
				se == SubSurfaceSensorError.other);
		}

		/** Logging format */
		public String toString() {
			return new StringBuilder()
				.append(EssConvertible.toLogString(new EssConvertible[]{
					temp,
					location,
					sub_surface_type,
					depth,
					moisture,
					sensor_error
				}, number))
				.append(EssConvertible.toLogString("isActive",isActive(),number))
				.toString();
		}
		/** Get JSON representation */
		public void toJson(JsonBuilder jb) throws JsonBuilder.Exception{
			jb.extend(new EssConvertible[]{
				location,
				sub_surface_type,
				depth,
				temp,
				moisture,
				sensor_error
			}).pair("active", isActive());
		}
	}

	@Override
	protected Row createRow(int row_num) {
		return new Row(row_num);
	}

	/** Get the first valid temperature or null on error */
	public Integer getFirstValidTemp() {
		return findRowValue(r -> r.isActive() ? r.temp.toInteger() : null);
	}

	/** To string */
	public String toString() {
		return new StringBuilder()
			.append("SubsurfaceSensorsTable: ")
			.append(num_sensors.toLogString())
			.append(super.toString())
			.toString();
	}

	/** Get JSON representation */
	public void toJson(JsonBuilder jb) throws JsonBuilder.Exception{
		jb.key("sub_surface_sensor");
		super.toJson(jb);
	}
}
