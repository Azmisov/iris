package us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums;
import us.mn.state.dot.tms.WeatherSensor;

/**
 * Pavement surface status as defined by essSurfaceStatus in NTCIP 1204.
 *
 * @author Douglas Lau
 * @copyright 2019-2022 Minnesota Department of Transportation
 * @author Michael Darter, Isaac Nygaard
 * @copyright 2017-2023 Iteris Inc.
 * @license GPL-2.0
 */
public enum SurfaceStatus implements EssEnumType {
	undefined,            // 0
	other,                // 1
	error,                // 2
	dry,                  // 3
	traceMoisture,        // 4
	wet,                  // 5
	chemicallyWet,        // 6
	iceWarning,           // 7
	iceWatch,             // 8
	snowWarning,          // 9
	snowWatch,            // 10
	absorption,           // 11
	dew,                  // 12
	frost,                // 13
	absorptionAtDewpoint; // 14

	public static SurfaceStatus fromOrdinal(Integer i){
		return EssEnumType.fromOrdinal(SurfaceStatus.class, i);
	}

	/** Get the surface status as an enum */
	static public SurfaceStatus from(WeatherSensor ws) {
		return ws != null
			? fromOrdinal(ws.getSurfStatus())
			: undefined;
	}
}
