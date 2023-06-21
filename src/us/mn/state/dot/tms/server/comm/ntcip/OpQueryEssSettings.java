/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2017 Iteris Inc.
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

import us.mn.state.dot.tms.server.WeatherSensorImpl;
import us.mn.state.dot.tms.server.comm.PriorityLevel;
import us.mn.state.dot.tms.utils.JsonBuilder;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.EssConvertible;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.EssRec;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.EssType;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.PavementSensorsTable;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.SubSurfaceSensorsTable;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.TemperatureSensorsTable;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.WindSensorsTable;
import us.mn.state.dot.tms.server.comm.snmp.ASN1Object;

/**
 * Operation to query a weather sensor's settings.
 *
 * @author Michael Darter, Isaac Nygaard
 * @author Douglas Lau
 */
public class OpQueryEssSettings extends OpEss {

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

	/** Create a new query settings object */
	public OpQueryEssSettings(WeatherSensorImpl ws) {
		super(PriorityLevel.POLL_LOW, ws);
	}

	/** Phase to query all rows in sub-surface table */
	final Pollable<ASN1Object> QuerySubSurfaceTable = mess -> {
		var sr = ss_table.addRow();
		queryMany(mess, new EssConvertible[]{
			sr.location,
			sr.sub_surface_type,
			sr.depth
		});
		return ss_table.isDone() ? null : this.QuerySubSurfaceTable;
	};

	/** Phase to query sub-surface values */
	final Pollable<ASN1Object> QuerySubSurface = mess -> {
		queryMany(mess, new EssConvertible[]{
			ss_table.num_sensors
		});
		return ss_table.isDone() ? null : QuerySubSurfaceTable;
	};

	/** Phase to query all rows in pavement table */
	final Pollable<ASN1Object> QueryPavementTable = mess -> {
		var pr = ps_table.addRow();
		queryMany(mess, new EssConvertible[]{
			pr.location,
			pr.pavement_type,
			pr.height,
			pr.exposure,
			pr.sensor_type
		});
		log("   PavementSensorType=" + pr.pavement_type);
		return ps_table.isDone()
				? QuerySubSurface
				: this.QueryPavementTable;
	};

	/** Phase to query pavement values */
	final Pollable<ASN1Object> QueryPavement = mess -> {
		queryMany(mess, new EssConvertible[]{
			ps_table.num_sensors
		});
		return ps_table.isDone()
				? QuerySubSurface
				: QueryPavementTable;
	};

	/** Phase to query all rows in temperature table */
	final Pollable<ASN1Object> QueryTemperatureTable = mess -> {
		var tr = ts_table.addRow();
		queryMany(mess, new EssConvertible[]{
			tr.height
		});
		return ts_table.isDone()
				? QueryPavement
				: this.QueryTemperatureTable;
	};

	/** Phase to query the temperature sensors and other data */
	final Pollable<ASN1Object> QueryTemperatureSensors = mess -> {
		queryMany(mess, new EssConvertible[]{
			ts_table.num_temp_sensors
		});
		return ts_table.isDone()
				? QueryPavement
				: QueryTemperatureTable;
	};

	/** Phase to query wind sensor values (V1) */
	final Pollable<ASN1Object> QueryWindSensorV1 = mess -> {
		queryMany(mess, new EssConvertible[]{
			ws_table.height
		});
		return QueryTemperatureSensors;
	};

	/** Phase to query all rows in wind table (V2+) */
	final Pollable<ASN1Object> QueryWindTableV2 = mess -> {
		var tr = ws_table.addRow();
		queryMany(mess, new EssConvertible[]{
			tr.height
		});
		return ws_table.isDone()
				? QueryTemperatureSensors
				: this.QueryWindTableV2;
	};

	/** Phase to query the wind sensor count (V2+) */
	final Pollable<ASN1Object> QueryWindSensorsV2  = mess -> {
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

	/** Phase to query elevation values */
	final Pollable<ASN1Object> QueryElevation = mess -> {
		var A = ess_rec.atmospheric_values;
		queryMany(mess, new EssConvertible[]{
			A.reference_elevation,
			A.pressure_sensor_height
		});
		return QueryWindSensorsV2;
	};

	/** Create the second phase of the operation */
	@Override
	protected Pollable<ASN1Object> phaseTwo() {
		// before any queries, identify EssType from sys_descr system setting
		w_sensor.setType(EssType.create(controller));
		log("Inferring EssType:" + 
			" rwis_type=" + w_sensor.getType() + 
			" sys_descr=" + controller.getSetup("sys_descr"));
		return QueryElevation;
	}

	/** Cleanup the operation */
	@Override
	public void cleanup() {
		if (isSuccess()){
			try{
				w_sensor.setSettings(new JsonBuilder().extend(ess_rec).toJson());
			} catch (JsonBuilder.Exception e){
				log("Ess JSON serialization error: "+e);
				log("\t: "+e.json);
			}
		}
		super.cleanup();
	}
}
