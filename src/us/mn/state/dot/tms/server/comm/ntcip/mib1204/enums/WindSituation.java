package us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums;

/**
 * Wind situation as defined by essWindSituation in NTCIP 1204v1 and
 * windSensorSituation in 1204v2+.
 *
 * @author Douglas Lau
 * @copyright 2022 Minnesota Department of Transportation
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public enum WindSituation implements EssEnumType {
	undefined,           // 0
	other,               // 1
	unknown,             // 2
	calm,                // 3
	lightBreeze,         // 4
	moderateBreeze,      // 5
	strongBreeze,        // 6
	gale,                // 7
	moderateGale,        // 8
	strongGale,          // 9
	stormWinds,          // 10
	hurricaneForceWinds, // 11
	gustyWinds;          // 12

	public boolean isValid(){
		return this != unknown && EssEnumType.super.isValid();
	}
	public static WindSituation fromOrdinal(Integer i){
		return EssEnumType.fromOrdinal(WindSituation.class, i);
	}
}
