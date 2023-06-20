package us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums;
import us.mn.state.dot.tms.WeatherSensor;

/**
 * Visibility situation as defined by essVisibilitySituation in NTCIP 1204.
 *
 * @author Douglas Lau
 * @copyright 2019-2022 Minnesota Department of Transportation
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public enum VisibilitySituation implements EssEnumType {
	undefined,         // 0
	other,             // 1
	unknown,           // 2
	clear,             // 3
	fogNotPatchy,      // 4
	patchyFog,         // 5
	blowingSnow,       // 6
	smoke,             // 7
	seaSpray,          // 8
	vehicleSpray,      // 9
	blowingDustOrSand, // 10
	sunGlare,          // 11
	swarmOfInsects;    // 12

	public boolean isValid(){
		return this != unknown && EssEnumType.super.isValid();
	}
	public static VisibilitySituation fromOrdinal(Integer i){
		return EssEnumType.fromOrdinal(VisibilitySituation.class, i);
	}

	/** Get the surface status as an enum */
	static public VisibilitySituation from(WeatherSensor ws) {
		return ws != null
			? fromOrdinal(ws.getVisibilitySituation())
			: undefined;
	}
}
