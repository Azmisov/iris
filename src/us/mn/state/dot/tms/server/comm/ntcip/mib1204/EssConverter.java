package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.tms.utils.Json;
import us.mn.state.dot.tms.server.comm.snmp.ASN1Integer;

/** An abstract class that manages conversion from raw MIB1204 ASN1Integer's, to
 * a converted Java object type `C`. This reduces code duplication. There are 
 * five options for taking the converted value and transforming it into an
 * output value:
 * 
 * <ul>
 *  <li> {@link #get}: Fetches the converted value as-is. Optionally, you can
 *      pass a lambda to convert it to a custom representation on the fly
 *  </li>
 *  <li> {@link #toDouble}: Outputs a double in custom units </li>
 *  <li> {@link #toInt}: A rounded/int representation in custom units; the
 *      default implementation rounds+casts the output of toDouble
 *  </li>
 *  <li> {@link #toString}: String representation; the default implementation
 *      converts the double representation to a string
 *  </li>
 *  <li> {@link #toJson}: Json representation; the default implmentation
 *      converts to a Json object member, with {@link #json_key} as the key
 *      and the string representation of the number as the value
 *  </li>
 * </ul>
 *
 * Subclasses should override {@link #convert} and the `toXXX` conversion methods
 * as needed. The {@link #ranged} method can/should be used here. It utilizes
 * the {@link #min}, {@link #max}, and {@link missing} values, which can be set
 * using {@link #setRange}.
 *
 * @author Isaac Nygaard
 * @copyright 2023 Iteris, Inc
 * @license GPL-2.0
 */
abstract public class EssConverter<C>{
	/** Max uint8 indicates error condition or missing value */
	static protected final int MAX_BYTE = 255;
	/** Max uint16 indicates error condition or missing value */
	static protected final int MAX_WORD = 65535;

	/** Minimum allowed value (inclusive) */
	protected int min = 0;
	/** Maximum allowed value (exclusive) */
	protected int max = MAX_WORD;
	/** Constant indicating a missing/error value. If null, it indicates no
	 * missing/error value is applicable for this type, and {@link #min} will be
	 * set as the initial default for the value instead.
	 */
	protected Integer missing = max;

	/** Json field name */
	public final String json_key;
	/** For error logging, the source field name */
	public final MIB1204 mib_attr;
	/** The raw value queried from the device */
	public final ASN1Integer node;
	/** Converted value */
	protected C value;
	/** Whether value has been converted from {@link #node} yet */
	protected boolean converted = false;

	/** Don't use; for convenience so intermediate subclasses don't have
	 * to implement a dummy constructor
	 * @throws UnsupportedOperationException
	 */
	public EssConverter(){
		throw new UnsupportedOperationException();
	}
	/** Create with predefined node
	 * @param json_key key for JSON serialization
	 * @param mib_attr MIB1204 attribute for this value, for logging
	 * @param node the node that will hold the raw value
	 */
	public EssConverter(String json_key, MIB1204 mib_attr, ASN1Integer node){
		this.json_key = json_key;
		this.mib_attr = mib_attr;
		this.node = node;
		this.node.setInteger(missing == null ? min : missing);
	}
	/** Create for a table row
	 * @param json_key key for JSON serialization
	 * @param mib_attr MIB1204 attribute for this value, for logging
	 * @param row row number for the table
	 */
	public EssConverter(String json_key, MIB1204 mib_attr, int row){
		this(json_key, mib_attr, mib_attr.makeInt(row));
	}
	/** Create independently, outside a table
	 * @param json_key key for JSON serialization
	 * @param mib_attr MIB1204 attribute for this value, for logging
	 */
	public EssConverter(String json_key, MIB1204 mib_attr){
		this(json_key, mib_attr, mib_attr.makeInt());
	}

	/** Set the valid range for the raw integer */
	@SuppressWarnings("unchecked")
	public <T extends EssConverter<C>> T setRange(int min, int max, Integer missing){
		this.min = min;
		this.max = max;
		this.missing = missing;
		return (T) this; // for chaining with inheritence
	}
	/** Set the valid range for the raw integer, with `missing` defaulting to
	 * the `max` range bound */
	public <T extends EssConverter<C>> T setRange(int min, int max){
		return setRange(min, max, max);
	}

	/** Convert from the raw {@link #node}. The default implementation just
	 * fetches an Integer and casts to the desired conversion type */
	@SuppressWarnings("unchecked")
	protected C convert(){
		return (C) ranged(i -> i);
	}

	/** Retreive the converted value, performing on-the-fly conversion if
	 * needed. This method enforces that we only perform conversion once, giving
	 * cleaner error logs and faster runtime at the expense of minor memory
	 * increases
	 */
	public C get(){
		if (!converted){
			converted = true;
			value = convert();
		}
		return value;
	}

	/** Fetch value and check if it is null */
	public boolean isNull(){
		return get() == null;
	}

	public static interface GetLambda<C, T>{
		/** Receives the non-null, converted value and should return a
		 * transformation on that value
		 */
		public T operation(C value);
	}

	/** Same as {@link #get}, but also perform an additional transformation if
	 * the value is not null
	 */
	public <T> T get(GetLambda<C, T> lambda){
		get();
		return value != null ? lambda.operation(value) : null;
	}

	/** Convert to a double output representation; by default, simply cast
	 * from the converted value
	 */
	public Double toDouble() throws UnsupportedOperationException{
		return (Double) get();
	}

	/** Convert to an integer output representation; by default, rounded from
	 * {@link #toDouble}
	 */
	public Integer toInteger() throws UnsupportedOperationException{
		var d = toDouble();
		return d != null ? Math.toIntExact(Math.round(d)) : null;
	}

	/** Serialize to a JSON string. The default implementation passes the
	 * output of {@link #toString} to {@link Json#num}. An empty string is
	 * returned if {@link #toString} was empty.
	 */
	public String toJson(){
		var val = toDouble();
		return val != null ? Json.num(json_key, val) : "";
	}

	/** Get string representation. The default implementation passes the output
	 * of {@link #toDouble} to {@link Double#toString}. An empty string is
	 * returned if value is null.
	 */
	public String toString(){
		var val = toDouble();
		return val != null ? Double.toString(val) : "";
	}

	protected static interface RangedLambda<P, T>{
		/** Receives a value with valid range and should return a converted
		 * object
		 */
		public T operation(P value);
	}

	/** Validates that an ASN1Integer is inside the valid range, and calls a
	 * conversion lambda function if it is. Otherwise, it returns null
	 * @param lambda function to be called to finalize conversion if valid
	 */
	protected <T> T ranged(RangedLambda<Integer, T> lambda){
		if (node != null){
			int i = node.getInteger();
			// TODO: throw a range error here instead? then downstream logger
			// could attach sensor metadata
			if (i < min || i >= max)
				System.err.print("%s out-of-range: %d".formatted(mib_attr, i));
			else if (i != missing)
				return lambda.operation(i);
		}
		return null;
	}
}
