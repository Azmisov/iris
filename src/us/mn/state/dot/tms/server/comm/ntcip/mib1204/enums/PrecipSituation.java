package us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums;
import us.mn.state.dot.tms.WeatherSensor;

/**
 * Precipitation situation as defined by essPrecipSituation in NTCIP 1204.
 *
 * @author Douglas Lau
 * @copyright 2019-2022 Minnesota Department of Transportation
 * @author Michael Darter, Isaac Nygaard
 * @copyright 2017-2023 Iteris Inc.
 * @license GPL-2.0
 */
public enum PrecipSituation implements EssEnumType {
	undefined(""),                   				// 0
	other("None"),                       			// 1
	unknown("None"),                     			// 2
	noPrecipitation("None"),             			// 3
	unidentifiedSlight("Rain"),          			// 4
	unidentifiedModerate("Rain"),        			// 5
	unidentifiedHeavy("Rain"),           			// 6
	snowSlight("Snow"),                  			// 7
	snowModerate("Snow"),                			// 8
	snowHeavy("Snow"),                   			// 9
	rainSlight("Light"),                 			// 10
	rainModerate("Rain"),                			// 11
	rainHeavy("Rain"),                   			// 12
	frozenPrecipitationSlight("Freezing Rain"),	// 13
	frozenPrecipitationModerate("Freezing Rain"),	// 14
	frozenPrecipitationHeavy("Freezing Rain");		// 15

	/** Simple description for CSV export */
	public final String desc_csv;

	private PrecipSituation(String dcsv){
		desc_csv = dcsv;
	}

	public boolean isValid(){
		return this != unknown && EssEnumType.super.isValid();
	}
	public static PrecipSituation fromOrdinal(Integer i){
		return EssEnumType.fromOrdinal(PrecipSituation.class, i);
	}

	/** Get the precipitation situation as an enum */
	static public PrecipSituation from(WeatherSensor ws) {
		if (ws != null) {
			Integer ps = ws.getPrecipSituation();
			if (ps != null)
				return fromOrdinal(ps);
		}
		return undefined;
	}

	/** Convert to string, with empty string if null/empty */
	static public String toStringValid(PrecipSituation value){
		return toStringValid(value, "");
	}
	/** Convert to string, with custom string if null/empty
	 * @arg invalid - string to use if status is invalid */
	static public String toStringValid(PrecipSituation value, String invalid){
		if (value != null && value != undefined)
			return value.toString();
		return invalid;
	}
}
