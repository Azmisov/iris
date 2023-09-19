package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.tms.units.Interval;
import us.mn.state.dot.tms.units.Interval.Units;

/** Converts {@link MIB1204} raw types to an {@link Interval}. By default, it is
 * configured for minutes between [0,1441), which is 24 hrs; max value is used
 * to indicate an error/missing value; output value is in minutes, with zero
 * fractional digits for String conversion. Output conversion is lazy, in case
 * you want to manually edit/access the value prior.
 *
 * @author Isaac Nygaard
 * @copyright 2023 Iteris, Inc
 * @license GPL-2.0
 */
public class EssInterval extends EssUnits<Interval, Interval.Units>{     
	public EssInterval(String k, MIB1204 n, int r){
		super(k, n, r);
        init();
	}
	public EssInterval(String k, MIB1204 n){
		super(k, n);
        init();
	}
    /** Default configuration */
    private void init(){
        setRange(0, 1441);
        setUnits(1, Units.MINUTES);
        setDigits(0);
    }

	@Override
	protected Interval convert(){
		return ranged(i -> new Interval(src_scale*i, src_units));
	}
    @Override
    public Double toDouble(){
        return get(i -> i.asDouble(out_units)/out_scale);
    }
}
