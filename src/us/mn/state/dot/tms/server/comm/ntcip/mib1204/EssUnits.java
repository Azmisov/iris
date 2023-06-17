package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

/** Class that adds helpers for handling unit conversion to
 * {@link EssConverter}. You can configure both the input/source type, and
 * specify an output type for methods like {@link #toInteger}. A subclass can
 * chose whether to eagerly compute the output type, or do the conversion lazily
 * on-the-fly. Default max fractional digits for Strings is 8.
 *
 * @param <U> enum type containing the units for the converted type
 * 
 * @author Isaac Nygaard
 * @copyright 2023 Iteris, Inc
 * @license GPL-2.0
 */
public class EssUnits<C, U extends Enum<U>> extends EssInteger<C>{
    public int digits = 8;
    public double src_scale, out_scale;
    public U src_units, out_units;

    public EssUnits(String k, MIB1204 n, int r){ super(k, n, r); }
	public EssUnits(String k, MIB1204 n){ super(k, n); }

    /** Set source and destination units when converting from the raw type. Call
     * {@link #setOutput} afterwards if you wish to use a different output type.
     * @param scale scale for `units`, e.g. for 1/4 meter, set to 0.25;
     *  prefer already defined units if possible
     * @param units what units to use
     */
    @SuppressWarnings("unchecked")
    public <T extends EssUnits<C,U>> T setUnits(double scale, U units){
        src_scale = out_scale = scale;
        src_units = out_units = units;
        return (T) this;
    }
    /** Set output digits configuration
     * @param digits Round to this many fractional digits for string output
     */
    @SuppressWarnings("unchecked")
    public <T extends EssUnits<C,U>> T setDigits(int digits){
        this.digits = digits;
        return (T) this;
    }
    /** Set output configuration
     * @param scale scale for `units`, e.g. for 1/4 meter, set to 0.25;
     *  prefer already defined units if possible
     * @param units units for outputs
     * @param digits Round to this many fractional digits for string output
     */
    public <T extends EssUnits<C,U>> T setOutput(double scale, U units, int digits){
        out_scale = scale;
        out_units = units;
        return setDigits(digits);
    }

    /** Takes {@link #toDouble} output and formats it with {@link #digits}
     * fractional digits
     */
    @Override
    public String toString(){
        var d = toDouble();
        return d != null ? Num.format(d, digits) : "";
    } 
}
