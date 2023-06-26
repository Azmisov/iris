package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.sched.TimeSteward;
import us.mn.state.dot.tms.server.WeatherSensorImpl;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.EssType;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.PrecipSituation;
import us.mn.state.dot.tms.utils.JsonBuilder;
import us.mn.state.dot.sched.DebugLog;

/**
 * A collection of weather condition values which can be converted to JSON.
 * Only values which have been successfully read will be included.
 *
 * @author Michael Darter, Isaac Nygaard
 * @copyright 2017-2023 Iteris Inc.
 * @author Douglas Lau
 * @copyright 2019-2022 Minnesota Department of Transportation
 * @license GPL-2.0
 */
public class EssRec implements JsonBuilder.Buildable {

	/** Station, device, or instrumentation values */
	public final InstrumentValues instrument_values = new InstrumentValues();

	/** Atmospheric values */
	public final AtmosphericValues atmospheric_values = new AtmosphericValues();

	/** Wind sensors table */
	public final WindSensorsTable ws_table = new WindSensorsTable();

	/** Temperature sensors table */
	public final TemperatureSensorsTable ts_table = new TemperatureSensorsTable();

	/** Precipitation sensor values */
	public final PrecipitationValues precip_values = new PrecipitationValues();

	/** Pavement sensors table */
	public final PavementSensorsTable ps_table = new PavementSensorsTable();

	/** Sub-surface sensors table */
	public final SubSurfaceSensorsTable ss_table = new SubSurfaceSensorsTable();

	/** Solar radiation values */
	public final RadiationValues rad_values = new RadiationValues();

	/** Variables for debugging */
	private DebugLog dlog;
	private String dlog_prefix;


	/** Logger */
	private void log(String msg) {
		dlog.log(dlog_prefix + msg);
	}

	/** Store the atmospheric values */
	private void storeAtmospheric(WeatherSensorImpl ws) {
		var A = atmospheric_values;
		ws.setPressureNotify(A.atmospheric_pressure.toInteger());
		ws.setVisibilityNotify(A.visibility.toInteger());
		ws.setPressureSensorHeightNotify(A.pressure_sensor_height.toInteger());
		ws.setElevationNotify(A.reference_elevation.toInteger());
		ws.setVisibilitySituationNotify(A.visibility_situation.toInteger());
	}

	/** Store the wind sensor data */
	private void storeWinds(WeatherSensorImpl ws) {
		// these getters aggregate, so are needed
		ws.setWindSpeedNotify(ws_table.getAvgSpeed());
		ws.setWindDirNotify(ws_table.getAvgDir());
		ws.setSpotWindSpeedNotify(ws_table.getSpotSpeed());
		ws.setSpotWindDirNotify(ws_table.getSpotDir());
		ws.setMaxWindGustSpeedNotify(ws_table.getGustSpeed());
		ws.setMaxWindGustDirNotify(ws_table.getGustDir());
	}

	/** Store the temperatures */
	private void storeTemps(WeatherSensorImpl ws) {
		ws.setDewPointTempNotify(ts_table.dew_point_temp.toInteger());
		ws.setMaxTempNotify(ts_table.max_air_temp.toInteger());
		ws.setMinTempNotify(ts_table.min_air_temp.toInteger());
		ws.setAirTempNotify(ts_table.getFirstValidTemp().toInteger());
	}

	/** Store precipitation samples */
	private void storePrecip(WeatherSensorImpl ws) {
		var P = precip_values;
		ws.setWaterDepthNotify(P.water_depth.toInteger());
		ws.setAdjacentSnowDepthNotify(P.snow_depth.toInteger());
		ws.setHumidityNotify(P.relative_humidity.toInteger());
		ws.setPrecipRateNotify(P.precip_rate.toInteger());
		ws.setPrecipOneHourNotify(P.precip_1_hour.toFloat());
		ws.setPrecip3HourNotify(P.precip_3_hours.toFloat());
		ws.setPrecip6HourNotify(P.precip_6_hours.toFloat());
		ws.setPrecip12HourNotify(P.precip_12_hours.toFloat());
		ws.setPrecip24HourNotify(P.precip_24_hours.toFloat());
		PrecipSituation ps = P.getPrecipSituation();
		ws.setPrecipSituationNotify((ps != null) ? ps.ordinal() : null);
	}

	/** Store pavement sensor related values */
	private void storePavement(WeatherSensorImpl ws) {
		// extract table values to locals
		// the surface status and first valid surf temp
		// should come from the same sensor.
		var valid_row = ps_table.getFirstValidSurfTempRow();
		ws.setSurfTempNotify(valid_row.surface_temp.toInteger());
		ws.setPvmtSurfStatusNotify(valid_row.surface_status.toInteger());
		ws.setPvmtTempNotify(ps_table.getFirstValidPvmtTemp());
		ws.setSurfFreezeTempNotify(ps_table.getFirstValidSurfFreezeTemp());
		ws.setPavementSensorsTable(ps_table);
	}

	/** Store subsurface sensor values */
	private void storeSubSurface(WeatherSensorImpl ws) {
		ws.setSubSurfTempNotify(ss_table.getFirstValidTemp());
		ws.setSubsurfaceSensorsTable(ss_table);
	}
	  
	/** Reorganize the tables for the High Sierra controllers, 
	 * so standard data extraction can be used. This must be 
	 * performed before data is extracted. */
	private void reorgHighSierra(){
		// High Sierra always has 4 pavement sensor rows
		if (ps_table.size() < 4) {
			log("reorgHighSierra: bad pst.size=" + ps_table.size());
			ps_table.clear();
			ss_table.clear();
			return;
		}

		// High Sierra stores subsurface temp in row 1 of pavement
		// sensor table. Ignore other row 1 values.
		var ss_temp = ps_table.getRow(1).pavement_temp;
		log("EssRec.reorg: subSurfTemp=" + ss_temp);

		// rebuild tables so they are not vendor specific
		ps_table.recreateHighSierra();
		ss_table.recreateHighSierra(ss_temp);
	}

	/** Store all sample values */
	public void store(DebugLog dl, WeatherSensorImpl ws) {
		// setup logging
		dlog = dl;
		dlog_prefix = ws.getName()+": ";
		// High Sierra device value mapping is a little different
		log("EssRec.store: sensorType=" + ws.getType());
		if (ws.getType() == EssType.HIGH_SIERRA) {
			log("EssRec.store: pre.pst=" + ps_table);
			log("EssRec.store: pre.sst=" + ss_table);
			reorgHighSierra();
		}
		storeAtmospheric(ws);
		storeWinds(ws);
		storeTemps(ws);
		storePrecip(ws);
		storePavement(ws);
		storeSubSurface(ws);
		long st = TimeSteward.currentTimeMillis();
		ws.setStampNotify(st);
	}

	/** Get JSON representation */
	public void toJson(JsonBuilder jb) {
		jb.object(new JsonBuilder.Buildable[]{
			instrument_values,
			atmospheric_values,
			ws_table,
			ts_table,
			precip_values,
			ps_table,
			ss_table,
			rad_values
		});
	}
}
