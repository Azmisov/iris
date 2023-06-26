package us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums;

/**
 * Station type, in terms of mobility
 *
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public enum StationMobility implements EssEnumType{
	undefined,		// 0
	other,			// 1
	permanent,		// 2
	transportable,	// 3
	mobile;			// 4

	public static StationMobility fromOrdinal(Integer i){
		return EssEnumType.fromOrdinal(StationMobility.class, i);
	}
}
