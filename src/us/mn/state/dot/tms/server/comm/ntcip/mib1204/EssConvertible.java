package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.tms.server.comm.snmp.ASN1Object;
import us.mn.state.dot.tms.utils.JsonBuilder;

/** Interface shared by all EssConverter generic types. This allows us to
 * handle them all in an array generically and do mass queries/logging/etc.
 * 
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public interface EssConvertible extends JsonBuilder.Buildable {
	/** Get name, which is currently the json_key */
	public String getName();
	/** Get the raw type prior to conversion */
	public ASN1Object getRaw();
	/** Check whether the converted type is ull */
	public boolean isNull();
	/** Transforms the converted type to Double; may not be supported */
	public Double toDouble() throws UnsupportedOperationException;
	/** Transforms the converted type to Integer; may not be supported */
	public Integer toInteger() throws UnsupportedOperationException;
	/** Transforms the converted type to String */
	public String toString();

	///////// LOGGING HELPERS //////////

	/** Convert an arbitrary string-value pair to logging format */
	public static <T> String toLogString(String name, T value, Integer row){
		var s = String.valueOf(value);
		return row == null
			? " %s=%s".formatted(name, s)
			: " %s(%d)=%s".formatted(name, row, s);
	}
	/** Convert an arbitrary string-value pair to logging format */
	public static <T> String toLogString(String name, T value){
		return toLogString(name, value, null);
	}
	/** Write multiple values into logging format */
	public static String toLogString(EssConvertible[] vals, Integer row){
		if (row == null)
			return toLogString(vals);
		StringBuilder sb = new StringBuilder();
		for (var val : vals)
			sb.append(val.toLogString(row));
		return sb.toString();
	}
	/** Write multiple values into logging format */
	public static String toLogString(EssConvertible[] vals){
		return toLogString(vals, null);
	}
	/** Write single value into logging format */
	default String toLogString(Integer row){
		return toLogString(getName(), toString(), row);
	}
	/** Write single value into logging format */
	default String toLogString(){
		return toLogString(getName(), toString());
	}
}
