package us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums;

/**
 * Pavement sensor errors as defined by essPavementSensorError in NTCIP 1204.
 *
 * @author Michael Darter, Isaac Nygaard
 * @copyright 2017-2023 Iteris Inc.
 * @author Douglas Lau
 * @copyright 2019-2022 Minnesota Department of Transportation
 * @license GPL-2.0
 */
public enum PavementSensorError implements EssEnumType{
	undefined,    // 0
	other,        // 1
	none,         // 2
	noResponse,   // 3
	cutCable,     // 4
	shortCircuit, // 5
	dirtyLens;    // 6

	public static boolean isValid(PavementSensorError v){
		return v != none && EssEnumType.isValid(v);
	}
	public static PavementSensorError fromOrdinal(Integer i){
		return EssEnumType.fromOrdinal(PavementSensorError.class, i);
	}
}
