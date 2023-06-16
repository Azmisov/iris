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

import us.mn.state.dot.tms.server.comm.snmp.ASN1Enum;
import us.mn.state.dot.tms.server.comm.snmp.DisplayString;
import us.mn.state.dot.tms.units.Distance;
import us.mn.state.dot.tms.units.Distance.Units;
import us.mn.state.dot.tms.utils.Json;
import us.mn.state.dot.tms.server.comm.ntcip.EssValues;
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
	static public class Row extends EssValues {
		public final int number;
		public final DisplayString location;
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
			location = new DisplayString(essPavementSensorLocation.node, row);
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
					.setUnits(1, Units.MILLIMETERS)
					.setOutput(1, Units.METERS, 3)
					.setRange(0, 255);
			ice_or_water_depth =
				new EssDistance("ice_or_water_depth", essSurfaceIceOrWaterDepth, row)
					.setUnits(0.1, Units.MILLIMETERS)
					.setOutput(1, Units.METERS, 4)
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
					.setUnits(1, Units.CENTIMETERS)
					.setRange(2, 11);
			sensor_model_info =
				new EssNumber("sensor_model_info", pavementSensorModelInformation, row)
					.setRange(1, 256, 0);
		}

		/** Get the sensor location */
		public String getSensorLocation() {
			String sl = location.getValue();
			return (sl.length() > 0) ? sl : null;
		}

		/** Get pavement type or null on error */
		public PavementType getPavementType() {
			PavementType pt = pavement_type.getEnum();
			return (pt != PavementType.undefined) ? pt : null;
		}

		/** Get pavement sensor type or null on error */
		public PavementSensorType getPavementSensorType() {
			PavementSensorType pst = sensor_type.getEnum();
			return (pst != PavementSensorType.undefined)
			      ? pst
			      : null;
		}

		/** Get surface status or null on error */
		public SurfaceStatus getSurfStatus() {
			SurfaceStatus ess = surface_status.getEnum();
			return (ess != SurfaceStatus.undefined) ? ess : null;
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
		public PavementSensorError getPavementSensorError() {
			PavementSensorError pse = sensor_error.getEnum();
			return (pse != null && pse.isError()) ? pse : null;
		}

		/** Get surface water depth formatted to meter units, mulitplied by
		 * `scale` to do on-the-fly conversion to another format
		 */
		public String getWaterDepth(float scale){
			return water_depth.get(d -> {
				return Num.format(d.asDouble(Units.METERS)*scale, 3);
			});
		}

		/** Get surface water depth formatted to meter units */
		private String getWaterDepth(){
			return water_depth.toString();
		}

		/** Get surface ice or water depth formatted to meter units */
		private String getIceOrWaterDepth(){
			String out = ice_or_water_depth.toString();
			// fallback to water depth if not present
			return out == null ? getWaterDepth() : out;
		}

		/** Get surface freeze temp or null on error */
		public Integer getFreezePointC() {
			return freeze_point.toInteger();
		}

		/** Get black ice signal or null on error */
		public SurfaceBlackIceSignal getBlackIceSignal() {
			SurfaceBlackIceSignal bis = black_ice_signal.getEnum();
			return (bis != null && bis.isValue()) ? bis : null;
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

		public String toString(){
			StringBuilder sb = new StringBuilder();
			sb.append(" surftemp(").append(number).append(")=").
				append(getSurfTempC());
			sb.append(" pvmttemp(").append(number).append(")=").
				append(getSurfTempC());
			sb.append(" pvmtsurfdepth(").append(number).append(")=").
				append(getWaterDepth());
			return sb.toString();
		}
		/** Get JSON representation */
		public String toJson() {
			StringBuilder sb = new StringBuilder();
			sb.append('{');
			sb.append(Json.str("location", getSensorLocation()));
			sb.append(Json.str("pavement_type", getPavementType()));
			sb.append(height.toJson());
			sb.append(exposure.toJson());
			sb.append(Json.str("sensor_type", getPavementSensorType()));
			sb.append(Json.str("surface_status", getSurfStatus()));
			sb.append(surface_temp.toJson());
			sb.append(pavement_temp.toJson());
			sb.append(Json.str("sensor_error",
				getPavementSensorError()));
			sb.append(Json.num("ice_or_water_depth", getIceOrWaterDepth()));
			sb.append(salinity.toJson());
			sb.append(freeze_point.toJson());
			sb.append(Json.str("black_ice_signal", getBlackIceSignal()));
			sb.append(friction.toJson());
			sb.append(conductivity.toJson());
			sb.append(Json.num("temp_depth", getTempDepth().toString()));
			// remove trailing comma
			if (sb.charAt(sb.length() - 1) == ',')
				sb.setLength(sb.length() - 1);
			sb.append("},");
			return sb.toString();
		}
	}

	@Override
	protected Row createRow(int row_num){
		return new Row(row_num);
	}

	/** To debug/log string */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("PavementSensorsTable: ");
		sb.append(" size=").append(size());
		sb.append(super.toString());
		return sb.toString();
	}

	/** Get JSON representation */
	public String toJson() {
		String rows = super.toString();
		if (!rows.isEmpty())
			rows = "\"pavement_sensor\":["+rows+"],";
		return rows;
	}
}
