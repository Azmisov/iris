package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.tms.units.Angle;
import us.mn.state.dot.tms.units.Angle.Units;

/** Converts {@link MIB1204} raw types to a {@link Angle}. By default, it is
 * configured for degrees between [0,361), with max value indicating an
 * error/missing value; output value is the same, with zero fractional digits
 * for strings. Output conversion is lazy, in case you want to manually
 * edit/access the value prior.
 *
 * @author Isaac Nygaard
 * @copyright 2023 Iteris, Inc
 * @license GPL-2.0
 */
public class EssAngle extends EssUnits<Angle, Units>{
	public EssAngle(String k, MIB1204 n, int r){
		super(k, n, r);
        init();
	}
	public EssAngle(String k, MIB1204 n){
		super(k, n);
        init();
	}
    /** Default configuration */
    private void init(){
        setRange(0, 361);
        setUnits(1, Units.DEGREES);
    }

	@Override
	protected Angle convert(){
		return ranged(i -> new Angle(src_scale*i, src_units));
	}
    @Override
    public Double toDouble(){
        return get(d -> d.asDouble(out_units)/out_scale);
    }
}
