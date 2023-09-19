package us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums;

/**
 * Surface black ice signal as defined by essSurfaceStatus in NTCIP 1204.
 *
 * @author Douglas Lau
 * @copyright 2019-2022 Minnesota Department of Transportation
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public enum SurfaceBlackIceSignal implements EssEnumType {
	undefined,     // 0
	other,         // 1
	noIce,         // 2
	blackIce,      // 3
	detectorError; // 4

	// for `isValid` including `other` for v47 backcompat
	
	public static SurfaceBlackIceSignal fromOrdinal(Integer i){
		return EssEnumType.fromOrdinal(SurfaceBlackIceSignal.class, i);
	}
}
