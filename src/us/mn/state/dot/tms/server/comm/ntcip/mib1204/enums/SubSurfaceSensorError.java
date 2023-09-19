package us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums;

/**
 * Subsurface sensor errors as defined by NTCIP 1204 essSubSurfaceSensorError.
 *
 * @author Douglas Lau
 * @copyright 2019-2022 Minnesota Department of Transportation
 * @author Michael Darter, Isaac Nygaard
 * @copyright 2017-2023 Iteris Inc.
 * @license GPL-2.0
 */
public enum SubSurfaceSensorError implements EssEnumType {
	undefined,    // 0
	other,        // 1
	none,         // 2
	noResponse,   // 3
	cutCable,     // 4
	shortCircuit; // 5

	// isValid method not excluding `none` currently for v47 backcompat

	public static SubSurfaceSensorError fromOrdinal(Integer i){
		return EssEnumType.fromOrdinal(SubSurfaceSensorError.class, i);
	}
}
