package us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums;

/**
 * Cloud situation as defined by essCloudSituation in NTCIP 1204
 *
 * @author Douglas Lau
 * @copyright 2022 Minnesota Department of Transportation
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public enum CloudSituation implements EssEnumType{
	undefined,    // 0
	overcast,     // 1 (100% cloud cover)
	cloudy,       // 2 (62.5% - 99% cover)
	partlyCloudy, // 3 (37.5% - 62.5% cover)
	mostlyClear,  // 4 (1% - 37.5% cover)
	clear;        // 5 (0% cover)

	public static CloudSituation fromOrdinal(Integer i){
		return EssEnumType.fromOrdinal(CloudSituation.class, i);
	}
}
