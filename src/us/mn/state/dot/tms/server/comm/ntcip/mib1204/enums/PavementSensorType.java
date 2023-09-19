package us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums;

/**
 * Pavement sensor type as defined by essPavementSensorType in NTCIP 1204.
 *
 * @author Douglas Lau
 * @copyright 2019-2022 Minnesota Department of Transportation
 * @author Michael Darter, Isaac Nygaard
 * @copyright 2017-2023 Iteris Inc.
 * @license GPL-2.0
 */
public enum PavementSensorType implements EssEnumType {
	undefined,      // 0
	other,          // 1
	contactPassive, // 2
	contactActive,  // 3
	infrared,       // 4
	radar,          // 5
	vibrating,      // 6
	microwave,      // 7
	laser;          // 8

	public static PavementSensorType fromOrdinal(Integer i){
		return EssEnumType.fromOrdinal(PavementSensorType.class, i);
	}
}
