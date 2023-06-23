package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.sched.TimeSteward;
import us.mn.state.dot.tms.server.WeatherSensorImpl;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.PrecipSituation;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SurfaceStatus;
import us.mn.state.dot.tms.utils.JsonBuilder;

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

	/** Create a new ESS record */
	public EssRec() { }

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
		ws.setPrecipOneHourNotify(P.precip_1_hour.toInteger());
		ws.setPrecip3HourNotify(P.precip_3_hours.toInteger());
		ws.setPrecip6HourNotify(P.precip_6_hours.toInteger());
		ws.setPrecip12HourNotify(P.precip_12_hours.toInteger());
		ws.setPrecip24HourNotify(P.precip_24_hours.toInteger());
		PrecipSituation ps = P.getPrecipSituation();
		ws.setPrecipSituationNotify((ps != null) ? ps.ordinal() : null);
	}

	/** Store pavement sensor related values */
	private void storePavement(WeatherSensorImpl ws) {
		PavementSensorsTable.Row row = ps_table.getRow(1);
		// default is null if not available, or e.g. High Sierra has an error
		Integer pvmt_surf_temp = null,
			surf_temp = null,
			surf_freeze_temp = null;
		SurfaceStatus pvmt_surf_status = null;

		if (row != null){
			// High Sierra device value mapping is a little different
			if (ws.getType() == EssType.HIGH_SIERRA){
				// Note: Subsurf is stored in essPavementTemperature for this
				// device, e.g. row.getPvmtTempC() (not currently used)

				// High Sierra stores surface temp in sensors 3 and 4.
				// Try sensor 3 then 4
				for (int row_num = 3; row_num <=4; ++row_num) {
					row = ps_table.getRow(row_num);
					if (!row.sensor_error.isNull()) {
						surf_temp = row.surface_temp.toInteger();
						pvmt_surf_status = row.surface_status.get();
						break;
					}
				}
			}
			// Otherwise generic type; ignore all but first sensor
			else{
				// the surface status and first valid surf temp
				// should come from the same sensor.
				var valid_row = ps_table.getFirstValidSurfTempRow();
				pvmt_surf_status = valid_row.surface_status.get();
				surf_temp = valid_row.surface_temp.toInteger();
				pvmt_surf_temp = ps_table.getFirstValidPvmtTemp();
				surf_freeze_temp = ps_table.getFirstValidSurfFreezeTemp();				
			}
		}
		ws.setPvmtTempNotify(pvmt_surf_temp);
		ws.setSurfTempNotify(surf_temp);
		ws.setPvmtSurfStatusNotify(
			pvmt_surf_status == null ? null : pvmt_surf_status.ordinal());
		ws.setSurfFreezeTempNotify(surf_freeze_temp);
		ws.setPavementSensorsTable(ps_table);
	}

	/** Store subsurface sensor values */
	private void storeSubSurface(WeatherSensorImpl ws) {
		Integer t = null;
		// High Sierra stores nothing in this table;
		// Subsurface temps are stored in pvmt table. (not currently!)
		if (ws.getType() != EssType.HIGH_SIERRA)
			t = ss_table.getFirstValidTemp();
		ws.setSubSurfTempNotify(t);
		ws.setSubsurfaceSensorsTable(ss_table);
	}

	/** Store all sample values */
	public void store(WeatherSensorImpl ws) {
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
