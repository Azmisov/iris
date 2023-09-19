package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.tms.units.Distance;
import us.mn.state.dot.tms.units.Distance.Units;

/** Converts {@link MIB1204} raw types to a {@link Distance}. By default, it is
 * configured for meters between [-1000,1001), with max value indicating an
 * error/missing value; output value is in meters. Output conversion is lazy, in
 * case you want to manually edit/access the value prior.
 *
 * @author Isaac Nygaard
 * @copyright 2023 Iteris, Inc
 * @license GPL-2.0
 */
public class EssDistance extends EssUnits<Distance, Units>{     
	public EssDistance(String k, MIB1204 n, int r){
		super(k, n, r);
        init();
	}
	public EssDistance(String k, MIB1204 n){
		super(k, n);
        init();
	}
    /** Default configuration */
    private void init(){
        setRange(-1000, 1001);
        setUnits(1, Units.METERS);
    }

	@Override
	protected Distance convert(){
		return ranged(i -> new Distance(src_scale*i, src_units));
	}
    @Override
    public Double toDouble(){
        return get(d -> d.asDouble(out_units)/out_scale);
    }

    // ------ Presets -------
    
    /** Conversion for precipitation rate aggregates into mm per m^2 */
	public static EssDistance PrecipRate(String json_key, MIB1204 attr){
		// .1 kg/m^2 -> approximately .1 mm/m^2 of water (.1 mm depth)
		// So technically these are volumes, if we wanted to add a Volume unit
		return new EssDistance(json_key, attr)
			.setUnits(0.1, Units.MILLIMETERS)
			.setOutput(1, Units.MILLIMETERS, 1)
			.setRange(0, EssDistance.MAX_WORD);
	}
}
