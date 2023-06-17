package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.tms.server.comm.snmp.ASN1Integer;

/** Implementation of {@link EssConverter} for {@link ASN1Integer}. It lets
 * you specify a permitted range for the raw value, as well as missing value.
 * By default these are [0,MAX_WORD), with the max doubling as the missing value.
 * The raw type is initialized to the missing value. Subclasses can utilize
 * the {@link #ranged} method in conversion, supplying a lambda for additional
 * transformations.
 * 
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public class EssInteger<C> extends EssConverter<C, ASN1Integer>{
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
	protected Integer missing;

    /** Create for a table row
	 * @param json_key key for JSON serialization
	 * @param mib_attr MIB1204 attribute for this value, for logging
	 * @param row row number for the table
	 */
	public EssInteger(String json_key, MIB1204 mib_attr, int row){
		super(json_key, mib_attr.makeInt(row));
        setMissing(max);
	}
	/** Create independently, outside a table
	 * @param json_key key for JSON serialization
	 * @param mib_attr MIB1204 attribute for this value, for logging
	 */
	public EssInteger(String json_key, MIB1204 mib_attr){
		super(json_key, mib_attr.makeInt());
        setMissing(max);
	}

    /** Set the missing/error value for the raw integer */
	@SuppressWarnings("unchecked")
	public <T extends EssInteger<C>> T setMissing(Integer missing){
		this.missing = missing;
        this.raw.setInteger(missing == null ? min : missing);
		return (T) this; // for chaining with inheritence
	}
    /** Set the valid range for the raw integer */
	public <T extends EssInteger<C>> T setRange(int min, int max, Integer missing){
		this.min = min;
		this.max = max;
		return setMissing(missing);
	}
	/** Set the valid range for the raw integer, with `missing` defaulting to
	 * the `max` range bound */
	public <T extends EssInteger<C>> T setRange(int min, int max){
		return setRange(min, max, max);
	}

	/** Manually overwrite the raw value. Used to accomodate fallback properties
	 * e.g. mobile friction gets stored in pavement friction
	*/
	public void setRawValue(int val){
		raw.setInteger(val);
	}
	/** Fetch the raw value */
	public int getRawValue(){
		return raw.getInteger();
	}

    /** Convert from the raw {@link #node}. The default implementation just
	 * fetches an Integer and casts to the desired conversion type */
	@SuppressWarnings("unchecked")
	protected C convert(){
		return (C) ranged(i -> i);
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
		int i = raw.getInteger();
		// TODO: throw a range error here instead? then downstream logger
		// could attach sensor metadata
		if (i < min || i >= max)
			System.err.print("%s out-of-range: %d".formatted(raw.getName(), i));
		else if (i != missing)
			return lambda.operation(i);
		return null;
	}
}
