package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.tms.utils.JsonBuilder;

/** Handles conversion from a MIB1204 integer to a boolean. True is given by the
 * max value, with all others being false. The default range is 0 to 1, so 1
 * indicates a true value. Out-of-range will be null.
 *
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public class EssBoolean extends EssInteger<Boolean>{     
	public EssBoolean(String k, MIB1204 n, int r){
        super(k, n, r);
        setRange(0,2);
    }
	public EssBoolean(String k, MIB1204 n){
        super(k, n);
        setRange(0,2);
    }

	@Override
	protected Boolean convert(){
		return ranged(i -> i == max);
	}
    /** Takes {@link #value} output and formats it with {@link #digits}
     * fractional digits
     */
    @Override
    public String toString(){
        return this.value != null ? String.valueOf(this.value) : null;
    }
    @Override
    public Double toDouble(){
        return this.value != null ? (double) this.value.compareTo(false) : null;
    }

    /** Serialize to a JSON. The default implementation uses
	 * {@link JsonBuilder#pairOrValue} with {@link #json_key} and
	 * boolean output. If the value is null, nothing is written
	 */
	public void toJson(JsonBuilder jb){
        Boolean v = get();
		if (v != null)
			jb.pairOrValue(json_key, v);
	}
}
