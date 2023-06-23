package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.tms.server.comm.snmp.ASN1Object;
import us.mn.state.dot.tms.utils.JsonBuilder;

/** An abstract class that manages conversion from raw MIB1204 nodes to
 * a converted Java object type. This reduces code duplication. There are 
 * five options for taking the converted value and transforming it into an
 * output value:
 * 
 * <ul>
 *  <li> {@link #get}: Fetches the converted value as-is. Optionally, you can
 *      pass a lambda to convert it to a custom representation on the fly
 *  </li>
 *  <li> {@link #toDouble}: Outputs a double in custom units </li>
 *  <li> {@link #toInteger}: A rounded/int representation in custom units; the
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
 * Subclasses should override {@link #convert} and the `toXXX` conversion
 * methods as needed.
 *
 * @param <C> the type we are converting to
 * @param <N> the raw MIB1204 node type (e.g. {@link ASN1Integer})
 * 
 * @author Isaac Nygaard
 * @copyright 2023 Iteris, Inc
 * @license GPL-2.0
 */
abstract public class EssConverter<C, N extends ASN1Object> implements EssConvertible{
	/** Json field name */
	public final String json_key;
	/** The raw value queried from the device */
	protected final N raw;
	/** Converted value. Access using {@link #get} */
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
	 * @param raw the raw node that will hold the raw value
	 */
	public EssConverter(String json_key, N raw){
		if (raw == null)
			throw new NullPointerException("ASN1Object can't be null");
		this.json_key = json_key;
		this.raw = raw;
	}

	/** Get name of this attribute */
	public String getName(){
		return json_key;
	}
	/** Fetch value and check if it is null */
	public boolean isNull(){
		return get() == null;
	}
	/** Fetch the raw value */
	public N getRaw(){
		return raw;
	}
	/** Manually set the converted value; this will bypass the conversion from
	 * raw ASN1Object using {@link #convert} */
	public void setValue(C val){
		value = val;
		converted = true;
	}
	
	/** Convert from the raw {@link #node} */
	abstract protected C convert();

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

	public static interface GetLambda<C, T>{
		/** Receives the non-null, converted value and should return a
		 * transformation on that value
		 */
		public T operation(C value);
	}

	/** Same as {@link #get}, but also perform an additional transformation if
	 * the value is not null
	 * @param fallback value to return if the value is null (the lambda can
	 * 	still return null, however)
	 */
	public <T> T get(GetLambda<C, T> lambda, T fallback){
		get();
		return value != null ? lambda.operation(value) : fallback;
	}
	/** Same as {@link #get} with lambda transformation with `fallback`
	 * defaulting to null
	 */
	public <T> T get(GetLambda<C, T> lambda){
		return get(lambda, null);
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

	/** Serialize to a JSON. The default implementation uses
	 * {@link JsonBuilder#pairOrValue} with {@link #json_key} and
	 * {@link #toDouble} output. If the value is null, nothing is written
	 */
	public void toJson(JsonBuilder jb){
		Double v = toDouble();
		if (v != null)
			jb.pairOrValue(json_key, v);
	}

	/** Get string representation. The default implementation passes the output
	 * of {@link #toDouble} to {@link Double#toString}. An empty string is
	 * returned if value is null. Double is converted to a long for more
	 * compact serialization if possible.
	 */
	public String toString(){
		var val = toDouble();
		if (val == null)
			return "";
		// can represent as long?
		if (Double.isFinite(val)){
			long i = val.longValue();
			if (i == val)
				return Long.toString(i);
		}
		return Double.toString(val);
	}
}
