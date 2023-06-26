/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2017-2019 Iteris Inc.
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
package us.mn.state.dot.tms.server.comm.ntcip;

import java.io.IOException;
import us.mn.state.dot.tms.server.WeatherSensorImpl;
import us.mn.state.dot.tms.server.comm.CommMessage;
import us.mn.state.dot.tms.server.comm.PriorityLevel;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.EssRec;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.EssNumber;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.EssConvertible;
import static us.mn.state.dot.tms.server.comm.ntcip.mib1204.MIB1204.essMobileFriction;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.PavementSensorsTable;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.SubSurfaceSensorsTable;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.TemperatureSensorsTable;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.WindSensorsTable;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SubSurfaceSensorError;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SubSurfaceType;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.CloudSituation;
import us.mn.state.dot.tms.server.comm.snmp.ASN1Object;
import us.mn.state.dot.tms.utils.JsonBuilder;

/**
 * Operation to query the status of a weather sensor.
 *
 * @author Michael Darter, Isaac Nygaard
 * @author Douglas Lau
 */
public class OpQueryEssStatus extends OpEss {

	/** Record of values read from the controller */
	private final EssRec ess_rec = new EssRec();

	/** Wind sensors table */
	private final WindSensorsTable ws_table = ess_rec.ws_table;

	/** Temperature sensors table */
	private final TemperatureSensorsTable ts_table = ess_rec.ts_table;

	/** Pavement sensors table */
	private final PavementSensorsTable ps_table = ess_rec.ps_table;

	/** Sub-surface sensors table */
	private final SubSurfaceSensorsTable ss_table = ess_rec.ss_table;

	/** Create new query ESS status operation */
	public OpQueryEssStatus(WeatherSensorImpl ws) {
		super(PriorityLevel.POLL_LOW, ws);
	}

	/** Phase to query radiation values (V1) */
	final Pollable<ASN1Object> QueryRadiationV1 = mess -> {
		// Note: this object was deprecated in V2
		queryMany(mess, new EssConvertible[]{
			ess_rec.rad_values.solar_radiation
		});
		return null;
	};

	/** Phase to query radiation values (V2) */
	final Pollable<ASN1Object> QueryRadiationV2 = mess -> {
		var R = ess_rec.rad_values;
		// Note: these objects were introduced in V2
		var err = queryMany(mess, new EssConvertible[]{
			R.instantaneous_terrestrial,
			R.instantaneous_solar,
			R.total_radiation,
			R.total_radiation_period
		}, true);
		return err != null ? QueryRadiationV1 : null;
	};

	/** Phase to query cloud situation value */
	final Pollable<ASN1Object> QueryCloudSituation = mess -> {
		// Not supported by some vendors...
		var R = ess_rec.rad_values;
		var err = queryMany(mess, new EssConvertible[]{
			R.cloud_situation
		}, true);
		if (err != null)
			R.cloud_situation.setValue(CloudSituation.clear);
		log("   essCloudCoverSituation=" + R.cloud_situation);
		return QueryRadiationV2;
	};

	/** Phase to query total sun value */
	final Pollable<ASN1Object> QueryTotalSun = mess -> {
		// Not supported by some vendors...
		queryMany(mess, new EssConvertible[]{
			ess_rec.rad_values.total_sun
		});
		return QueryCloudSituation;
	};

	/** Phase to query sub-surface moisture */
	protected class QuerySubSurfaceMoisture extends Phase {
		private final SubSurfaceSensorsTable.Row sr;
		private QuerySubSurfaceMoisture(SubSurfaceSensorsTable.Row r) {
			sr = r;
		}

		public Pollable<ASN1Object> poll(CommMessage<ASN1Object> mess)
			throws IOException
		{
			// Note: some vendors do not support this object
			var err = queryMany(mess, new EssConvertible[]{
				sr.moisture
			}, true);
			if (err != null)
				sr.moisture.reset();
			if (ss_table.isDone()){
				log(EssConvertible.toLogString("ss_table", ss_table));
				return QueryTotalSun;
			}
			return QuerySubSurfaceTable;
		}
	}

	/** Phase to query rows in sub-surface table */
	final Pollable<ASN1Object> QuerySubSurfaceTable = mess -> {
		var sr = ss_table.addRow();
		var err = queryMany(mess, new EssConvertible[]{
			sr.location,
			sr.sub_surface_type,
			sr.depth,
			sr.temp,
			sr.sensor_error,
		}, true);
		// High Sierra RWIS controller can generates err: essSubSurfaceSensorError;
		// this applies to subsequent subsurf phases too
		if (err != null){
			sr.location.setValue(null);
			sr.sub_surface_type.setValue(SubSurfaceType.unknown);
			sr.depth.reset();
			sr.temp.reset();
			sr.sensor_error.setValue(SubSurfaceSensorError.noResponse);
		}
		log(EssConvertible.toLogString("SubSurfaceSensorError",sr.sensor_error));
		
		return new QuerySubSurfaceMoisture(sr);
	};

	/** Phase to query sub-surface values */
	final Pollable<ASN1Object> QuerySubSurface = mess -> {
		queryMany(mess, new EssConvertible[]{
			ss_table.num_sensors
		}, true);
		if (ss_table.isDone()){
			log(EssConvertible.toLogString("ss_table", ss_table));
			return QueryTotalSun;
		}
		return QuerySubSurfaceTable;
	};

	/** Phase to query mobile friction (as fallback).
	 * Note: some vendors support essMobileFriction for permanent stations
	 *       (non-mobile).  We'll pretend it's part of the pavement sensors
	 *       table (first row only). */
	protected class QueryMobileFriction extends Phase {
		private final PavementSensorsTable.Row pr;
		private QueryMobileFriction(PavementSensorsTable.Row r) {
			pr = r;
		}

		public Pollable<ASN1Object> poll(CommMessage<ASN1Object> mess)
			throws IOException
		{
			// Note: mobile friction is not part of pavement table
			// 	we're using pr.friction to hold the value though
			var mf = EssNumber.Percent("friction", essMobileFriction);
			var err = queryMany(mess, new EssConvertible[]{mf});
			// Note: some vendors do not support this object
			if (err != null)
				pr.friction.setRawValue(mf.getRawValue());
			return nextPavementRow();
		}
	}

	/** Phase to query one pavement sensor row (V4) */
	protected class QueryPavementRowV4 extends Phase {
		private final PavementSensorsTable.Row pr;
		private QueryPavementRowV4(PavementSensorsTable.Row r) {
			pr = r;
		}

		public Pollable<ASN1Object> poll(CommMessage<ASN1Object> mess) throws IOException{
			// Note: this object was added in V4
			var err = queryMany(mess, new EssConvertible[]{
				pr.friction
			});
			if (err == null)
				pr.friction.reset();
			// Fallback to mobile friction (1st row only)
			else if (pr.number == 1)
				return  new QueryMobileFriction(pr);
			return nextPavementRow();
		}
	}

	/** Phase to query one pavement sensor row (V1) */
	protected class QueryPavementRowV1 extends Phase {
		private final PavementSensorsTable.Row pr;
		private QueryPavementRowV1(PavementSensorsTable.Row r) {
			pr = r;
		}

		public Pollable<ASN1Object> poll(CommMessage<ASN1Object> mess) throws IOException {
			queryMany(mess, new EssConvertible[]{
				pr.water_depth
			});
			// Note: essSurfaceConductivity could be polled here
			// Note: for errors, this object was deprecated in V2
			return nextPavementRow();
		}
	}

	/** Phase to query one pavement sensor row (V2) */
	protected class QueryPavementRowV2 extends Phase {
		private final PavementSensorsTable.Row pr;
		private QueryPavementRowV2(PavementSensorsTable.Row r){
			pr = r;
		}

		public Pollable<ASN1Object> poll(CommMessage<ASN1Object> mess) throws IOException {
			// Note: this object was introduced in V2
			var err = queryMany(mess, new EssConvertible[]{
				pr.ice_or_water_depth,
				pr.conductivity,
				pr.sensor_model_info,
				pr.temp_depth
			}, true);
			// Fallback to V1 water depth
			if (err != null)
				return new QueryPavementRowV1(pr);
			return new QueryPavementRowV4(pr);
		}
	}

	/** Phase to query one pavement sensor row */
	final Pollable<ASN1Object> QueryPavementRow = mess -> {
		var pr = ps_table.addRow();
		queryMany(mess, new EssConvertible[]{
			pr.surface_status,
			pr.surface_temp,
			pr.pavement_temp,
			pr.water_depth,
			pr.freeze_point,
			pr.sensor_error,
			pr.salinity,
			pr.black_ice_signal
		});
		log(EssConvertible.toLogString("PavementSurfaceStatus", pr.surface_status));
		log(EssConvertible.toLogString("PavementSensorError", pr.sensor_error));
		return new QueryPavementRowV2(pr);
	};

	/** Get phase to query next pavement sensor row */
	Pollable<ASN1Object> nextPavementRow() {
		if (ps_table.isDone()){
			log(EssConvertible.toLogString("ps_table", ps_table));
			return QuerySubSurface;
		}
		return QueryPavementRow;
	}

	/** Phase to query pavement values */
	final Pollable<ASN1Object> QueryPavement = mess -> {
		queryMany(mess, new EssConvertible[]{
			ps_table.num_sensors
		}, true);
		return nextPavementRow();
	};

	/** Phase to query adjacent jsnow depth. This is broken out from the 
	 * precipitation phase because some RWIS incorrectly treat it 
	 * as optional, e.g. the QTT LX-RPU Elite Model Version 1.26. */
	final Pollable<ASN1Object> QueryAdjSnowDepth = mess -> {
		var P = ess_rec.precip_values;
		queryMany(mess, new EssConvertible[]{
			P.snow_depth
		}, true);
		return QueryPavement;
	};

	/** Phase to query precipitation values */
	final Pollable<ASN1Object> QueryPrecipitation = mess -> {
		// essWaterDepth is V1 NTCIP only and was replaced
		// subsequently with the water level sensor table, but
		// may be supported by RWIS for compatibility.
		// See the note in NTCIP v2 D.7.
		var P = ess_rec.precip_values;
		queryMany(mess, new EssConvertible[]{
			P.water_depth,
			P.relative_humidity,
			P.precip_rate,
			P.precip_1_hour,
			P.precip_3_hours,
			P.precip_6_hours,
			P.precip_12_hours,
			P.precip_24_hours,
			P.precip_situation
		}, true);
		log(EssConvertible.toLogString("essPrecipSituation", P.precip_situation));
		return QueryAdjSnowDepth;
	};

	/** Phase to query all rows in temperature table */
	final Pollable<ASN1Object> QueryTemperatureTable = mess -> {
		var tr = ts_table.addRow();
		var err = queryMany(mess, new EssConvertible[]{
			tr.air_temp
		}, true);
		// Some controllers sometimes seem to randomly
		// forget what essAirTemperature is
		if (err != null)
			return QueryPrecipitation;
		if (ts_table.isDone()){
			log(EssConvertible.toLogString("ts_table",ts_table));
			return QueryPrecipitation;
		}
		return this.QueryTemperatureTable;
	};

	/** Phase to query the temperature sensors and other data */
	final Pollable<ASN1Object> QueryTemperatureSensors = mess -> {
		queryMany(mess, new EssConvertible[]{
			ts_table.num_temp_sensors,
			ts_table.wet_bulb_temp,
			ts_table.dew_point_temp,
			ts_table.max_air_temp,
			ts_table.min_air_temp
		}, true);
		if (ts_table.isDone()){
			log(EssConvertible.toLogString("ts_table",ts_table));
			return QueryPrecipitation;
		}
		return QueryTemperatureTable;
	};

	/** Phase to query wind sensor values (V1) */
	final Pollable<ASN1Object> QueryWindSensorV1 = mess -> {
		queryMany(mess, new EssConvertible[]{
			ws_table.avg_direction,
			ws_table.avg_speed,
			ws_table.spot_direction,
			ws_table.spot_speed,
			ws_table.gust_direction,
			ws_table.gust_speed
		});
		// Note: for errors, these objects are deprecated in V2
		return QueryTemperatureSensors;
	};

	/** Phase to query all rows in wind table (V2+) */
	final Pollable<ASN1Object> QueryWindTableV2 = mess -> {
		WindSensorsTable.Row tr = ws_table.addRow();
		var err = queryMany(mess, new EssConvertible[]{
			tr.avg_speed,
			tr.avg_direction,
			tr.spot_speed,
			tr.spot_direction,
			tr.gust_speed,
			tr.gust_direction
		}, true);
		// Some controllers sometimes seem to randomly
		// forget what windSensorGustDirection is
		return (err != null || ws_table.isDone())
			? QueryTemperatureSensors
			: this.QueryWindSensorsV2;
	};

	/** Phase to query the wind sensor count (V2+) */
	final Pollable<ASN1Object> QueryWindSensorsV2 = mess -> {
		var err = queryMany(mess, new EssConvertible[]{
			ws_table.num_sensors
		});
		// Note: this object was introduced in V2
		if (err != null)
			return QueryWindSensorV1;
		return ws_table.isDone()
			? QueryTemperatureSensors
			: QueryWindTableV2;
	};

	/** Get phase to query wind sensor data */
	Pollable<ASN1Object> queryWindSensors() {
		// LX model RPUs contain a bug which sometimes causes objects in
		// the wind sensor table to update only once every 12 hours or
		// so.  The workaround is to query the (deprecated) wind sensor
		// objects from 1204v1 (for LX controllers only).
		return (getSoftwareModel().contains("LX"))
			? QueryWindSensorV1
			: QueryWindSensorsV2;
	}

	/** Phase to query visibility */
	final Pollable<ASN1Object> QueryVisibility = mess -> {
		var A = ess_rec.atmospheric_values;
		queryMany(mess, new EssConvertible[]{
			A.visibility,
			A.visibility_situation
		}, true);
		log(EssConvertible.toLogString("essVisibilitySituation", A.visibility_situation));
		return queryWindSensors();
	};

	/** Phase to query atmospheric pressure */
	final Pollable<ASN1Object> QueryPressure = mess -> {
		var A = ess_rec.atmospheric_values;
		queryMany(mess, new EssConvertible[]{
			A.atmospheric_pressure,
			// also query pressure height (which depends on reference height)
			A.reference_elevation,
			A.pressure_sensor_height		
		}, true);
		return QueryVisibility;
	};

	/** Create the second phase of the operation */
	@Override
	protected Pollable<ASN1Object> phaseTwo() {
		log("phaseTwo: rwis_type=" + w_sensor.getType());
		log("phaseTwo: ntcip_version=" + ntcip_ver);
		return QueryPressure;
	}	

	/** Cleanup the operation */
	@Override
	public void cleanup() {
		if (isSuccess()) {
			w_sensor.setSettings(new JsonBuilder().extend(ess_rec).toJson());
			ess_rec.store(NTCIP_LOG, w_sensor);
		}
		super.cleanup();
	}
}
