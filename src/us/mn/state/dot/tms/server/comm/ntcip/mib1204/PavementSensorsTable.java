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
import us.mn.state.dot.tms.units.Distance;
import static us.mn.state.dot.tms.units.Distance.Units.*;
import us.mn.state.dot.tms.utils.JsonBuilder;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.PavementSensorError;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.PavementSensorType;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.PavementType;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SurfaceBlackIceSignal;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SurfaceStatus;

/**
 * Pavement sensors data table, where each table row contains data read from
 * a single pavement sensor within the same controller.
 *
 * @author Michael Darter, Isaac Nygaard
 * @author Douglas Lau
 */
public class PavementSensorsTable extends EssTable<PavementSensorsTable.Row>{
	/** Number of sensors in table */
	public final EssNumber num_sensors =
		EssNumber.Count("size", numEssPavementSensors);
	
	public PavementSensorsTable(){
		setSensorCount(num_sensors);
	}

	/** Table row */
	static public class Row implements JsonBuilder.Buildable {
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
		/** Surface status enum */
		public final EssEnum<SurfaceStatus> surface_status;
		/** Surface temp in celcius */
		public final EssTemperature surface_temp;
		/** Pavement temp in celcius */
		public final EssTemperature pavement_temp;
		/** Pavement sensor error enum */
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
				new EssEnum<PavementType>("pavement_type", essPavementType, row);
			height =
				new EssDistance("height", essPavementElevation, row);
			exposure =
				new EssNumber("exposure", essPavementExposure, row)
					.setRange(0, 101);
			sensor_type =
				new EssEnum<PavementSensorType>("pavement_sensor_type", essPavementSensorType, row);
			surface_status =
				new EssEnum<SurfaceStatus>("surface_status", essSurfaceStatus, row);
			surface_temp =
				new EssTemperature("surface_temp", essSurfaceTemperature, row);
			pavement_temp =
				new EssTemperature("pavement_temp", essPavementTemperature, row);
			sensor_error = 
				new EssEnum<PavementSensorError>("pavement_sensor_error", essPavementSensorError, row);
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
				new EssEnum<SurfaceBlackIceSignal>("surface_black_ice_signal", essSurfaceBlackIceSignal, row);
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

		/** Get the sensor location */
		public String getSensorLocation() {
			return location.get();
		}

		/** Get pavement type or null on error */
		public PavementType getPavementType() {
			return pavement_type.get();
		}

		/** Get pavement sensor type or null on error */
		public PavementSensorType getPavementSensorType() {
			return sensor_type.get();
		}

		/** Get surface status or null on error */
		public SurfaceStatus getSurfStatus() {
			return surface_status.get();
		}

		/** Get surface temp or null on error */
		public Integer getSurfTempC() {
			return surface_temp.toInteger();
		}

		/** Get pavement temp or null on error */
		public Integer getPvmtTempC() {
			return pavement_temp.toInteger();
		}

		/** Get pavement sensor error or null on error */
		public PavementSensorError getPavementSensorError(){
			return sensor_error.get();
		}

		/** Get surface water depth formatted to meter units, mulitplied by
		 * `scale` to do on-the-fly conversion to another format
		 */
		public String getWaterDepth(float scale){
			return water_depth.get(d -> {
				return Num.format(d.asDouble(METERS)*scale, 3);
			});
		}

		/** Get surface ice or water depth formatted to meter units */
		private Double getIceOrWaterDepth(){
			Double out = ice_or_water_depth.toDouble();
			// fallback to water depth if not present
			return out == null ? water_depth.toDouble() : out;
		}

		/** Get surface freeze temp or null on error */
		public Integer getFreezePointC() {
			return freeze_point.toInteger();
		}

		/** Get black ice signal or null on error */
		public SurfaceBlackIceSignal getBlackIceSignal() {
			return black_ice_signal.get();
		}

		/** Get conductivity in 0.1 milli-mhos/cm, or null if missing */
		public Integer getConductivity(){
			return conductivity.toInteger();
		}

		/** Get pavement temperature sensor depth in cm, or null if out of 2-10cm range */
		public Distance getTempDepth(){
			return temp_depth.get();
		}

		/** Get sensor model information, or null if not available */
		public Integer getModelInfo(){
			return sensor_model_info.toInteger();
		}

		/** Is the nth sensor active? */
		public boolean isActive(){
			var pse = sensor_error.get();
			// These values were determined empirically, with valid
			// temps present when sensor err was one of these:
			return (pse == PavementSensorError.none ||
				pse == PavementSensorError.noResponse ||
				pse == PavementSensorError.other);
		}

		/** Logging string */
		public String toString(){
			return new StringBuilder()
				.append(EssConvertible.toLogString(new EssConvertible[]{
					sensor_error,
					surface_temp,
					pavement_temp,
					water_depth
				}, number))
				.append(EssConvertible.toLogString("isActive", isActive(), number))
				.toString();
		}
		/** Get JSON representation */
		public void toJson(JsonBuilder jb) throws JsonBuilder.Exception {
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

	/** Get the row for the first valid surface temp or -1 on error */
	public Row getFirstValidSurfTempRow() {
		return findRow(r -> r.isActive() && !r.surface_temp.isNull());
	}

	/** Get the specified nth active sensor row or -1 if none.
	 * @param nth Nth active sensor, ranges between 1 and size.
	 * @return One-based row number of nth active sensor */
	public int getNthActive(int nth) {
		int[] active_count = {0};
		var row = findRow(r -> r.isActive() ? ++active_count[0] == nth : false);
		return row == null ? -1 : row.number;
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
	public void toJson(JsonBuilder jb) throws JsonBuilder.Exception{
		jb.key("pavement_sensor");
		super.toJson(jb);
	}
}
