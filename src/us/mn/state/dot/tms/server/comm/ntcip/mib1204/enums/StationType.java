package us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums;

/**
 * Station type, in terms of data collection
 *
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public enum StationType implements EssEnumType{
	automatic,		// 0
	staffed,		// 1
	reserved,		// 2
	missingValue;	// 3

	public boolean isValid(){
		return this != missingValue;
	}

	public static StationType fromOrdinal(Integer i){
		return EssEnumType.fromOrdinal(StationType.class, i);
	}
}
