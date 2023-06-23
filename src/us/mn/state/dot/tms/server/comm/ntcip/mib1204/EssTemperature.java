package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.tms.units.Temperature;
import us.mn.state.dot.tms.units.Temperature.Units;

/** Converts {@link MIB1204} raw types to a {@link Temperature}. By default, it
 * is configured for 1/10th degrees C between [-1000,1001), with max value
 * indicating an error/missing value; output value is in degrees C, with Strings
 * rounded to one fractional digit. Output conversion is lazy, in case you want
 * to manually edit/access the value prior.
 *
 * @author Isaac Nygaard
 * @copyright 2023 Iteris, Inc
 * @license GPL-2.0
 */
public class EssTemperature extends EssUnits<Temperature, Temperature.Units>{
	public EssTemperature(String k, MIB1204 n, int r){
		super(k, n, r);
        init();
	}
	public EssTemperature(String k, MIB1204 n){
		super(k, n);
        init();
	}
    /** Default configuration */
    private void init(){
        setRange(-1000, 1001);
		setUnits(0.1, Units.CELSIUS);
		setOutput(1, Units.CELSIUS, 2);
    }

	@Override
	protected Temperature convert(){
		return ranged(i -> new Temperature(src_scale*i, src_units));
	}
	@Override
	public Double toDouble(){
		return get(t -> t.asDouble(out_units)/out_scale);
	}
}
