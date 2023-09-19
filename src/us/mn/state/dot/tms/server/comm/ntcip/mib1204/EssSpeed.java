package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.tms.units.Speed;
import us.mn.state.dot.tms.units.Speed.Units;

/** Converts {@link MIB1204} raw types to a {@link Speed}. By default, it is
 * configured for 1/10th meters/second between [0,MAX_WORD), with max value
 * indicating an error/missing value; output value is in meters/second, with
 * Strings rounded to one fractional digit. Output conversion is lazy, in case
 * you want to manually edit/access the value prior.
 *
 * @author Isaac Nygaard
 * @copyright 2023 Iteris, Inc
 * @license GPL-2.0
 */
public class EssSpeed extends EssUnits<Speed, Units>{
	public EssSpeed(String k, MIB1204 n, int r){
		super(k, n, r);
        init();
	}
	public EssSpeed(String k, MIB1204 n){
		super(k, n);
        init();
	}
    /** Default configuration */
    private void init(){
		setUnits(0.1, Units.MPS);
		setOutput(1, Units.MPS, 1);
    }

	@Override
	protected Speed convert(){
		return ranged(i -> new Speed(src_scale*i, src_units));
	}
	@Override
	public Double toDouble(){
		return get(t -> t.asDouble(out_units)/out_scale);
	}
}

