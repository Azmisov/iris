package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.tms.units.Pressure;
import us.mn.state.dot.tms.units.Pressure.Units;

/** Converts {@link MIB1204} raw types to a {@link Pressure}. By default, it is
 * configured for 1/10th millibar between [0,MAX_WORD), with max value
 * indicating an error/missing value; output value is in pascals, with zero
 * fractional digits for String conversion. Output conversion is lazy, in case
 * you want to manually edit/access the value prior.
 *
 * @author Isaac Nygaard
 * @copyright 2023 Iteris, Inc
 * @license GPL-2.0
 */
public class EssPressure extends EssUnits<Pressure, Units>{     
	public EssPressure(String k, MIB1204 n, int r){
		super(k, n, r);
        init();
	}
	public EssPressure(String k, MIB1204 n){
		super(k, n);
        init();
	}
    /** Default configuration */
    private void init(){
        setUnits(0.1, Units.MILLIBARS);
		setOutput(1, Units.PASCALS, 0);
    }

	@Override
	protected Pressure convert(){
		return ranged(i -> new Pressure(src_scale*i, src_units));
	}
    @Override
    public Double toDouble(){
        return get(d -> d.asDouble(out_units)/out_scale);
    }
}
