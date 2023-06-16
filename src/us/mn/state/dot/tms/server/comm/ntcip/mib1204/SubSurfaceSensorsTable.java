package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import static us.mn.state.dot.tms.server.comm.ntcip.mib1204.MIB1204.*;
import us.mn.state.dot.tms.server.comm.snmp.DisplayString;
import us.mn.state.dot.tms.utils.Json;
import us.mn.state.dot.tms.server.comm.ntcip.EssValues;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SubSurfaceSensorError;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SubSurfaceType;
import us.mn.state.dot.tms.units.Distance;

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
	static public class Row extends EssValues{
		/** Row/sensor number */
		public final int number;
		public final DisplayString location;
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
			location = new DisplayString(
				essSubSurfaceSensorLocation.node, row);
			sub_surface_type =
				new EssEnum<SubSurfaceType>("sub_surface_type", essSubSurfaceType, row);
			depth = 
				new EssDistance("depth", essSubSurfaceSensorDepth, row)
					.setUnits(1, Distance.Units.CENTIMETERS)
					.setOutput(1, Distance.Units.METERS, 2);
			temp =
				new EssTemperature("temp", essSubSurfaceTemperature, row);
			moisture =
				EssNumber.Percent("moisture", essSubSurfaceMoisture, row);
			sensor_error =
				new EssEnum<SubSurfaceSensorError>("sensor_error", essSubSurfaceSensorError, row);
		}

		/** Get the sensor location */
		public String getSensorLocation() {
			String sl = location.getValue();
			return (sl.length() > 0) ? sl : null;
		}

		/** Get sub-surface type or null on error */
		public SubSurfaceType getSubSurfaceType() {
			return sub_surface_type.get();
		}

		/** Get sub-surface temp or null on error */
		public Integer getTempC() {
			return temp.toInteger();
		}

		/** Get sensor error or null on error */
		public SubSurfaceSensorError getSensorError() {
			return sensor_error.get();
		}

		public String toString() {
			return " subsurftemp(%d)=%d".formatted(number, getTempC());	
		}
		/** Get JSON representation */
		public String toJson() {
			StringBuilder sb = new StringBuilder();
			sb.append('{');
			sb.append(Json.str("location", getSensorLocation()));
			sb.append(sub_surface_type.toJson());
			sb.append(depth.toJson());
			sb.append(temp.toJson());
			sb.append(moisture.toJson());
			sb.append(sensor_error.toJson());
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
