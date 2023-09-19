package us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums;

/**
 * Pavement type as defined by essPavementType in NTCIP 1204.
 *
 * @author Douglas Lau
 * @copyright 2019-2022 Minnesota Department of Transportation
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public enum PavementType implements EssEnumType {
	undefined,            // 0
	other,                // 1
	unknown,              // 2
	asphalt,              // 3
	openGradedAsphalt,    // 4
	concrete,             // 5
	steelBridge,          // 6
	concreteBridge,       // 7
	asphaltOverlayBridge, // 8
	timberBridge;         // 9

	public boolean isValid(){
		return this != unknown && EssEnumType.super.isValid();
	}
	public static PavementType fromOrdinal(Integer i){
		return EssEnumType.fromOrdinal(PavementType.class, i);
	}
}
