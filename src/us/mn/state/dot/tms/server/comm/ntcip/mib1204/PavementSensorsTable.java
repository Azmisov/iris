/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2017-2018  Iteris Inc.
 * Copyright (C) 2019-2023  Minnesota Department of Transportation
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import static us.mn.state.dot.tms.server.comm.ntcip.mib1204.MIB1204.*;
import static us.mn.state.dot.tms.units.Distance.Units.*;
import us.mn.state.dot.tms.utils.JsonBuilder;
import us.mn.state.dot.tms.utils.SString;
import us.mn.state.dot.tms.utils.XmlBuilder;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.PavementSensorError;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.PavementSensorType;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.PavementType;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SurfaceBlackIceSignal;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SurfaceStatus;
import java.io.IOException;

/**
 * Pavement sensors data table, where each table row contains data read from
 * a single pavement sensor within the same controller.
 *
 * @author Michael Darter, Isaac Nygaard
 * @author Douglas Lau
 */
public class PavementSensorsTable extends EssTable<PavementSensorsTable.Row>
	implements XmlBuilder.Buildable
{
	/** Number of sensors in table; May be manually modified by High Sierra
	 * remapping */
	public final EssNumber num_sensors =
		EssNumber.Count("size", numEssPavementSensors);
	
	public PavementSensorsTable(){
		setSensorCount(num_sensors);
	}

	/** Table row */
	static public class Row implements JsonBuilder.Buildable, XmlBuilder.Buildable {
		/** Row/sensor number */
		public final int number;
		public final EssString location;
		/** Pavement type enum */
		public final EssEnum<PavementType> pavement_type;
		public final EssDistance height;
		/** Pavement exposure in percentage 0-100 */
		public final EssNumber exposure;
		/** Pavement sensor type enum */
		public final EssEnum<PavementSensorType> sensor_type;
		/** Surface status enum; may be manually overwritten by High Sierra
		 * remapping */
		public EssEnum<SurfaceStatus> surface_status;
		/** Surface temp in celcius; may be manually overwritten by High Sierra
		 * remapping */
		public EssTemperature surface_temp;
		/** Pavement temp in celcius */
		public final EssTemperature pavement_temp;
		/** Pavement sensor error enum; may be manually modified by High Sierra
		 * remapping */
		public final EssEnum<PavementSensorError> sensor_error;
		/** Surface water depth in meters */
		public final EssDistance water_depth;
		/** Surface ice or water depth in meters */
		public final EssDistance ice_or_water_depth;
		/** Surface salinity in ppm */
		public final EssNumber salinity;
		/** Surface freeze temp in celcius */
		public final EssTemperature freeze_point;
		/** Surface black ice signal enum */
		public final EssEnum<SurfaceBlackIceSignal> black_ice_signal;
		/** Pavement friction in percentage 0-100 */
		public final EssNumber friction;
		/** Conductivity in 0.1 milli-mhos/cm */
		public final EssNumber conductivity;
		/** Temperature sensor depth below surface in cm */
		public final EssDistance temp_depth;
		/** Sensor model info (0-255) */
		public final EssNumber sensor_model_info;

		/** Create a table row */
		private Row(int row) {
			number = row;
			location =
				new EssString("location", essPavementSensorLocation, row);
			pavement_type =
				EssEnum.make(PavementType.class, "pavement_type", essPavementType, row);
			height =
				new EssDistance("height", essPavementElevation, row);
			exposure =
				new EssNumber("exposure", essPavementExposure, row)
					.setRange(0, 101);
			sensor_type =
				EssEnum.make(PavementSensorType.class, "pavement_sensor_type", essPavementSensorType, row);
			surface_status =
				EssEnum.make(SurfaceStatus.class, "surface_status", essSurfaceStatus, row);
			surface_temp =
				new EssTemperature("surface_temp", essSurfaceTemperature, row);
			pavement_temp =
				new EssTemperature("pavement_temp", essPavementTemperature, row);
			sensor_error = 
				EssEnum.make(PavementSensorError.class, "pavement_sensor_error", essPavementSensorError, row);
			water_depth =
				new EssDistance("water_depth", essSurfaceWaterDepth, row)
					.setUnits(1, MILLIMETERS)
					.setOutput(1, METERS, 3)
					.setRange(0, 255);
			ice_or_water_depth =
				new EssDistance("ice_or_water_depth", essSurfaceIceOrWaterDepth, row)
					.setUnits(0.1, MILLIMETERS)
					.setOutput(1, METERS, 4)
					.setRange(0, EssDistance.MAX_WORD);
			salinity =
				new EssNumber("salinity", essSurfaceSalinity, row)
					.setScale(10); // parts per 100,000 -> ppm
			freeze_point =
				new EssTemperature("freeze_point", essSurfaceFreezePoint, row);
			black_ice_signal =
				EssEnum.make(SurfaceBlackIceSignal.class, "surface_black_ice_signal", essSurfaceBlackIceSignal, row);
			friction =
				EssNumber.Percent("friction", pavementSensorFrictionCoefficient, row);
			conductivity =
				new EssNumber("conductivity", essSurfaceConductivityV2, row);
			temp_depth =
				new EssDistance("temp_depth", pavementSensorTemperatureDepth, row)
					.setUnits(1, CENTIMETERS)
					.setRange(2, 11);
			sensor_model_info =
				new EssNumber("sensor_model_info", pavementSensorModelInformation, row)
					.setRange(1, 256, 0);
		}

		/** Get surface ice or water depth formatted to meter units */
		private Double getIceOrWaterDepth(){
			Double out = ice_or_water_depth.toDouble();
			// fallback to water depth if not present
			return out == null ? water_depth.toDouble() : out;
		}

		/** Is the nth sensor active? */
		public boolean isActive(){
			var pse = sensor_error.get();
			// These values were determined empirically, with valid
			// temps present when sensor err was one of those below.
			// This was validated by NCAR.
			return (pse == PavementSensorError.none ||
				pse == PavementSensorError.noResponse ||
				pse == PavementSensorError.other);
		}

		/** Logging string */
		public String toString(){
			return new StringBuilder()
				.append(EssConvertible.toLogString(new EssConvertible[]{
					sensor_error,
					surface_status,
					surface_temp,
					pavement_temp,
					water_depth,
					freeze_point,
					ice_or_water_depth,
					black_ice_signal
				}, number))
				.append(EssConvertible.toLogString("isActive", isActive(), number))
				.toString();
		}
		/** Get JSON representation */
		public void toJson(JsonBuilder jb) {
			jb.beginObject();
			jb.extend(new EssConvertible[]{
				location,
				pavement_type,
				height,
				exposure,
				sensor_type,
				surface_status,
				surface_temp,
				pavement_temp,
				sensor_error,
				salinity,
				freeze_point,
				black_ice_signal,
				friction,
				conductivity,
				temp_depth
			});
			var iw = getIceOrWaterDepth();
			if (iw != null)
				jb.pair("ice_or_water_depth", iw);
			jb.endObject();
		}
		/** Get XML representation */
		public void toXml(XmlBuilder xb) throws IOException{
			xb.tag("pvmt_sensor")
				.attr("index", number)
				.attr("isactive", isActive())
				.attr("pvmt_sens_err",
					SString.camelToUpperSnake(sensor_error.toString()))
				.attr("surf_status",
					SString.camelToUpperSnake(surface_status.toString()))
				.attr("surf_temp_c", surface_temp)
				.attr("pvmt_temp_c", pavement_temp)
				.attr("surf_water_depth_mm",
					water_depth.get(v -> v.round(MILLIMETERS)))
				.attr("surf_freeze_temp_c", freeze_point)
				.attr("ice_water_depth_mm",
					ice_or_water_depth.get(v -> v.round(MILLIMETERS)))
				.attr("surf_black_ice_signal",
					SString.camelToUpperSnake(black_ice_signal.toString()));
		}
	}

	@Override
	protected Row createRow(int row_num){
		return new Row(row_num);
	}

	/** Get the first valid surf freeze temp or null on error */
	public Integer getFirstValidSurfFreezeTemp(){
		return findRowValue(r -> r.isActive() ? r.freeze_point.toInteger() : null);
	}

	/** Get the first valid pavement temp or null on error */
	public Integer getFirstValidPvmtTemp() {
		return findRowValue(r -> r.isActive() ? r.pavement_temp.toInteger() : null);
	}

	/** Get the row for the first valid surface temp or null on error */
	public Row getFirstValidSurfTempRow() {
		return findRow(r -> r.isActive() && !r.surface_temp.isNull());
	}

	/** Get the row for the first valid High Sierra surface temp,
	 * which is stored in rows 3 or 4 (1-based) */
	private Row getFirstValidSurfTempRowHighSierra() {
		for (int i = 2; i < Math.min(table_rows.size(), 4); ++i) {
			var row = table_rows.get(i);
			if (!row.surface_temp.isNull())
				return row;
		}
		return null;
	}

	/** Recreate High Sierra table. Valid pavement sensor rows start
	 * at 3 or 4. This is remapped to row 1 */
	public void recreateHighSierra() {
		// Have seen rows with missing temperature but status present.
		// In that case, the logic below returns no surf status.
		// The row must have temperature present to be used.
		var row = getFirstValidSurfTempRowHighSierra();
		clear();
		if (row != null){
			num_sensors.setValue(1);
			var nrow = addRow();
			nrow.surface_status = row.surface_status;
			nrow.surface_temp = row.surface_temp;
			nrow.sensor_error.setValue(PavementSensorError.none);
		}
	}



	/** Get the specified nth active sensor row or -1 if none.
	 * @param nth Nth active sensor, ranges between 1 and size.
	 * @return One-based row number of nth active sensor */
	public Row getNthActive(int nth) {
		int[] active_count = {0}; // lambda can't capture primitives by ref
		return findRow(r -> r.isActive() ? ++active_count[0] == nth : false);
	}

	/** To debug/log string */
	public String toString() {
		return new StringBuilder()
			.append("PavementSensorsTable: ")
			.append(num_sensors.toLogString())
			.append(EssConvertible.toLogString(
				"firstValidSurfTempRow", getFirstValidSurfTempRow()))
			.append(EssConvertible.toLogString(
				"firstValidPvmtTemp", getFirstValidPvmtTemp()))
			.append(EssConvertible.toLogString(
				"firstValidSurfFreezeTemp", getFirstValidSurfFreezeTemp()))
			.append(super.toString())
			.toString();
	}

	/** Get JSON representation */
	public void toJson(JsonBuilder jb){
		if (!isEmpty()){
			jb.key("pavement_sensor");
			super.toJson(jb);
		}
	}
	/** Get XML representation */
	public void toXml(XmlBuilder xb) throws IOException{
		xb.tag("pvmt_sensors").attr("size", size()).child();
		for (var row : table_rows)
			row.toXml(xb);
		xb.parent();
	}
}
