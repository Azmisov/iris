package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import static us.mn.state.dot.tms.server.comm.ntcip.mib1204.MIB1204.*;
import us.mn.state.dot.tms.server.comm.snmp.ASN1Enum;
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
		EssNumber.Count("subsurface_sensors", numEssSubSurfaceSensors)
			.setRange(0, 255, null);

	/** Table row */
	static public class Row extends EssValues{
		public final int number;
		public final DisplayString location;
		public final ASN1Enum<SubSurfaceType> sub_surface_type;
		/** Subsurface depth in meters */
		public final EssDistance depth;
		/** Subsurface temperature in celcius */
		public final EssTemperature temp;
		/** Moisture in percent 0-100 */
		public final EssNumber moisture;
		public final ASN1Enum<SubSurfaceSensorError> sensor_error;

		/** Create a table row */
		private Row(int row) {
			number = row;
			location = new DisplayString(
				essSubSurfaceSensorLocation.node, row);
			sub_surface_type = new ASN1Enum<SubSurfaceType>(
				SubSurfaceType.class, essSubSurfaceType.node, row);
			depth = 
				new EssDistance("depth", essSubSurfaceSensorDepth, row)
					.setUnits(1, Distance.Units.CENTIMETERS)
					.setOutput(1, Distance.Units.METERS, 2);
			temp =
				new EssTemperature("temp", essSubSurfaceTemperature, row);
			moisture =
				EssNumber.Percent("moisture", essSubSurfaceMoisture, row);
			sensor_error = new ASN1Enum<SubSurfaceSensorError>(
				SubSurfaceSensorError.class,
				essSubSurfaceSensorError.node, row);
		}

		/** Get the sensor location */
		public String getSensorLocation() {
			String sl = location.getValue();
			return (sl.length() > 0) ? sl : null;
		}

		/** Get sub-surface type or null on error */
		public SubSurfaceType getSubSurfaceType() {
			SubSurfaceType sst = sub_surface_type.getEnum();
			return (sst != SubSurfaceType.undefined) ? sst : null;
		}

		/** Get sub-surface sensor depth in meters */
		private String getDepth() {
			return depth.toString();
		}

		/** Get sub-surface temp or null on error */
		public Integer getTempC() {
			return temp.toInteger();
		}

		/** Get sensor error or null on error */
		public SubSurfaceSensorError getSensorError() {
			SubSurfaceSensorError se = sensor_error.getEnum();
			return (se != null && se.isError()) ? se : null;
		}

		public String toString() {
			return " subsurftemp(%d)=%d".formatted(number, getTempC());	
		}
		/** Get JSON representation */
		public String toJson() {
			StringBuilder sb = new StringBuilder();
			sb.append('{');
			sb.append(Json.str("location", getSensorLocation()));
			sb.append(Json.str("sub_surface_type",
				getSubSurfaceType()));
			sb.append(Json.num("depth", getDepth()));
			sb.append(temp.toJson());
			sb.append(moisture.toJson());
			sb.append(Json.str("sensor_error", getSensorError()));
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
