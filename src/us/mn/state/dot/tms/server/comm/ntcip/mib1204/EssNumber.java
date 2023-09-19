package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

/** Handles conversion from a MIB1204 integer to a number. Use when there isn't
 * a more specific type available for conversion. Default range is unsigned
 * word, with max word indicating a missing value. It uses double for internal
 * storage, so max safe integer is ~9 quadrillion.
 *
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public class EssNumber extends EssInteger<Double>{
    private int digits = 1;
    // unlike EssUnits, doesn't make sense to have lazy output scaling here
    private double scale = 1;
     
	public EssNumber(String k, MIB1204 n, int r){ super(k, n, r); }
	public EssNumber(String k, MIB1204 n){ super(k, n); }

    /** Set scale for the value when converting
     * @param scale scale for the value, with unspecified units
     */
    public EssNumber setScale(double scale){
        this.scale = scale;
        return this;
    }
    /** Set output digits configuration
     * @param digits Round to this many fractional digits for string output
     */
    public EssNumber setDigits(int digits){
        this.digits = digits;
        return this;
    }

    /** Allows int to double for use with {@link #setValue} */
    public void setValue(int val){
		setValue(Double.valueOf(val));
	}

	@Override
	protected Double convert(){
		return ranged(i -> scale*i);
	}
    /** Takes {@link #value} output and formats it with {@link #digits}
     * fractional digits
     */
    @Override
    public String toString(){
        return this.value != null ? Num.format(this.value, digits) : null;
    }

    // -------- Presets --------
    // These don't have a more specialized Unit class that could be used

    /** Preset for percentages between [0,101) */
    public static EssNumber Percent(String json_key, MIB1204 attr){
        return new EssNumber(json_key, attr)
            .setRange(0, 101);
    }
    /** Preset for percentages between [0,101) */
    public static EssNumber Percent(String json_key, MIB1204 attr, int row){
        return new EssNumber(json_key, attr, row)
            .setRange(0, 101);
    }
    /** Count from (0,255], with zero indicating missing/error */
    public static EssNumber Count(String json_key, MIB1204 attr){
        return new EssNumber(json_key, attr)
            .setRange(0, 255, 0);
    }
    /** Preset for Radiation values between [-2048,2049), zero fractional digits,
     * and units in (watts / m^2)
     */
    public static EssNumber Radiation(String json_key, MIB1204 attr){
		return new EssNumber(json_key, attr)
			.setDigits(0)
			.setRange(-2048, 2049);
	}
}
