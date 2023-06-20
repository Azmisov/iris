package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.sched.TimeSteward;
import us.mn.state.dot.tms.server.WeatherSensorImpl;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.PrecipSituation;
import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.SurfaceStatus;

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
public class EssRec {

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
		ws.setPressureNotify(atmospheric_values.atmospheric_pressure.toInteger());
		ws.setVisibilityNotify(atmospheric_values.visibility.toInteger());
		ws.setPressureSensorHeightNotify(
			atmospheric_values.pressure_sensor_height.toInteger());
		ws.setElevationNotify(atmospheric_values.reference_elevation.toInteger());
	}

	/** Store the wind sensor data */
	private void storeWinds(WeatherSensorImpl ws) {
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
		// Air temperature is assumed to be the first sensor
		// in the table.  Additional sensors are ignored.
		TemperatureSensorsTable.Row row = ts_table.getRow(1);
		Integer t = (row != null) ? row.air_temp.toInteger() : null;
		ws.setAirTempNotify(t);
	}

	/** Store precipitation samples */
	private void storePrecip(WeatherSensorImpl ws) {
		ws.setWaterDepthNotify(precip_values.water_depth.toInteger());
		ws.setHumidityNotify(precip_values.relative_humidity.toInteger());
		ws.setPrecipRateNotify(precip_values.precip_rate.toInteger());
		ws.setPrecipOneHourNotify(precip_values.precip_1_hour.toInteger());
		PrecipSituation ps = precip_values.getPrecipSituation();
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
					if (row.getPavementSensorError() != null) {
						surf_temp = row.getSurfTempC();
						pvmt_surf_status = row.getSurfStatus();
						break;
					}
				}
			}
			// Otherwise generic type; ignore all but first sensor
			else{
				pvmt_surf_temp = row.getPvmtTempC();
				surf_temp = row.getSurfTempC();
				surf_freeze_temp = row.getFreezePointC();
				pvmt_surf_status = row.getSurfStatus();
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
		var row = ss_table.getRow(1);
		Integer t = null;
		// High Sierra stores nothing in this table;
		// Subsurface temps are stored in pvmt table. (not currently!)
		if (row != null && ws.getType() != EssType.HIGH_SIERRA)
			t = row.temp.toInteger();
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
	public String toJson() {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		sb.append(atmospheric_values.toJson());
		sb.append(ws_table.toJson());
		sb.append(ts_table.toJson());
		sb.append(precip_values.toJson());
		sb.append(ps_table.toJson());
		sb.append(ss_table.toJson());
		sb.append(rad_values.toJson());
		// remove trailing comma
		if (sb.charAt(sb.length() - 1) == ',')
			sb.setLength(sb.length() - 1);
		sb.append('}');
		return sb.toString();
	}
}
