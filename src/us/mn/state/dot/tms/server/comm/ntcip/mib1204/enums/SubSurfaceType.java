package us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums;

/**
 * Sub-surface type as defined by NTCIP 1204 essSubSurfaceType.
 *
 * @author Douglas Lau
 * @copyright 2019-2022 Minnesota Department of Transportation
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public enum SubSurfaceType implements EssEnumType {
	undefined,         // 0
	other,             // 1
	unknown,           // 2
	concrete,          // 3
	asphalt,           // 4
	openGradedAsphalt, // 5
	gravel,            // 6
	clay,              // 7
	loam,              // 8
	sand,              // 9
	permafrost,        // 10
	variousAggregates, // 11
	air;               // 12

	public boolean isValid(){
		return this != unknown && EssEnumType.super.isValid();
	}
	public static SubSurfaceType fromOrdinal(Integer i){
		return EssEnumType.fromOrdinal(SubSurfaceType.class, i);
	}
}
