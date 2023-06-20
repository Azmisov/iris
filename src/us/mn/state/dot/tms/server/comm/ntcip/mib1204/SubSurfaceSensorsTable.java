package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import static us.mn.state.dot.tms.server.comm.ntcip.mib1204.MIB1204.*;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SubSurfaceSensorError;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SubSurfaceType;
import static us.mn.state.dot.tms.units.Distance.Units.*;
import us.mn.state.dot.tms.utils.JsonSerializable;

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
	static public class Row implements JsonSerializable{
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

		public String toString() {
			return " subsurftemp(%d)=%d".formatted(number, temp.toInteger());	
		}
		/** Get JSON representation */
		public String toJson() {
			return EssConvertible.toJsonObject(new EssConvertible[]{
				location,
				sub_surface_type,
				depth,
				temp,
				moisture,
				sensor_error
			});
		}
	}

	@Override
	protected Row createRow(int row_num) {
		return new Row(row_num);
	}

	/** To string */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SubsurfaceSensorsTable: ");
		sb.append(" size=").append(size());
		sb.append(super.toString());
		return sb.toString();
	}

	/** Get JSON representation */
	public String toJson() {
		String rows = super.toJson();
		if (!rows.isEmpty())
			rows = "\"sub_surface_sensor\":["+rows+"],";
		return rows;
	}
}
