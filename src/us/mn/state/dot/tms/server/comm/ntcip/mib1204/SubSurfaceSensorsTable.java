package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import static us.mn.state.dot.tms.server.comm.ntcip.mib1204.MIB1204.*;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SubSurfaceSensorError;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SubSurfaceType;
import static us.mn.state.dot.tms.units.Distance.Units.*;
import java.io.IOException;
import us.mn.state.dot.tms.utils.JsonBuilder;
import us.mn.state.dot.tms.utils.XmlBuilder;

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
public class SubSurfaceSensorsTable extends EssTable<SubSurfaceSensorsTable.Row>
	implements XmlBuilder.Buildable
{
	/** Number of temperature sensors in table; may be manually modified by High
	 * Sierra remapping */
	public final EssNumber num_sensors =
		EssNumber.Count("subsurface_sensors", numEssSubSurfaceSensors);
	
	public SubSurfaceSensorsTable(){
		setSensorCount(num_sensors);
	}

	/** Table row */
	static public class Row implements JsonBuilder.Buildable, XmlBuilder.Buildable{
		/** Row/sensor number */
		public final int number;
		/** Sensor location as a display string */
		public final EssString location;
		/** Subsurface type enum */
		public final EssEnum<SubSurfaceType> sub_surface_type;
		/** Subsurface depth in meters */
		public final EssDistance depth;
		/** Subsurface temperature in celcius; may be overwritten by High Sierra
		 * remapping */
		public EssTemperature temp;
		/** Moisture in percent 0-100 */
		public final EssNumber moisture;
		/** Subsurface sensor error enum; may be manually modified by High
		 * Sierra remapping */
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
		public void toJson(JsonBuilder jb){
			jb.extend(new EssConvertible[]{
				location,
				sub_surface_type,
				depth,
				temp,
				moisture,
				sensor_error
			}).pair("active", isActive());
		}
		/** Get Xml representation */
		public void toXml(XmlBuilder xb) throws IOException{
			xb.tag("subsurf_sensor")
				.attr("index", number)
				.attr("isactive",isActive())
				.attr("location",location)
				.attr("subsurf_type",sub_surface_type)
				.attr("subsurf_depth",depth)
				.attr("subsurf_temp_c",temp)
				.attr("subsurf_moisture",moisture)
				.attr("subsurf_sensor_error",sensor_error);
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

	/** Recreate High Sierra table with a single row containing 
	 * subsurface temperature and status
	 * @param ss_temp subsurface temperature to use */
	public void recreateHighSierra(EssTemperature ss_temp) {
		clear();
		num_sensors.setValue(1);
		var nrow = addRow();
		nrow.temp = ss_temp;
		nrow.sensor_error.setValue(SubSurfaceSensorError.none);
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
	public void toJson(JsonBuilder jb){
		jb.key("sub_surface_sensor");
		super.toJson(jb);
	}
	/** Get XML representation */
	public void toXml(XmlBuilder xb) throws IOException{
		xb.tag("subsurf_sensors").attr("size",size()).child();
		for (var row: table_rows)
			row.toXml(xb);
		xb.parent();
	}
}
