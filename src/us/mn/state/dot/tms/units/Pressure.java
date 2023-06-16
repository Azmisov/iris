package us.mn.state.dot.tms.units;

import java.text.NumberFormat;
import us.mn.state.dot.tms.SystemAttrEnum;

/**
 * Immutable pressure in various units.
 *
 * @author Michael Darter, Isaac Nygaard
 * @copyright 2017-2023 Iteris Inc.
 * @license GPL-2.0
 */
final public class Pressure {

	/** Enumeration of units */
	public enum Units {
		PASCALS(1, "Pa", 0),
		MICROBARS(10, "μbar", 0),
		MILLIBARS(100, "mbar", 0),
		HECTOPASCALS(100, "hPa", 0),
		CENTIBARS(1e3, "cbar", 0),
		KILOPASCALS(1e3, "kPa", 0),
		INHG(3386.39, "inHg", 1),
		DECIBARS(1e4,"dbar", 0),
		BARS(1e5, "bar", 0),
		MEGAPASCALS(1e6, "MPa", 0),
		KILOBARS(1e8, "kbar", 0),
		MEGABARS(1e11, "Mbar", 0);

		/** Conversion scale to Pascals: pa = scale * X */
		public final double scale;

		/** Unit label */
		public final String label;

		/** Number of significant digits after decimal */
		public final int n_digits;

		/** Create units */
		private Units(double sc, String la, int nd) {
			scale = sc;
			label = la;
			n_digits = nd;
		}
	}

	/** Factory to create a new quantity with the null case handled.
	 * @param v Value in units u or null.
	 * @return A new quantity in system units or null */
	static public Pressure create(Integer v) {
		return create(v, Units.PASCALS);
	}

	/** Factory to create a new quantity with the null case handled.
	 * @param v Value in units u or null.
	 * @param u Units for arg v.
	 * @return A new quantity in system units or null */
	static public Pressure create(Integer v, Units u) {
		Pressure vr = null;
		if (v != null) {
			vr = new Pressure(v, u);
			if (!useSi())
				vr = vr.convert(Units.INHG);
		}
		return vr;
	}

        /** Get system units */
        static private boolean useSi() {
                return SystemAttrEnum.CLIENT_UNITS_SI.getBoolean();
        }

	/** Pressure in pascals */
	public final double value;

	/** units */
	public final Units units;

	/** Constructor with units
	 * @param v Value
	 * @param u Units */
	public Pressure(double v, Units u) {
		value = v;
		units = u;
	}

	/** Constructor with assummed units of Pascals
	 * @param v Pressure value in Pascals */
	public Pressure(double v) {
		this(v, Units.PASCALS);
	}

	/** To string
	 * @return Pressure rounded to the number of significant
	 * digit specified for the unit and the unit symbol. */
	public String toString() {
		return new Formatter(units.n_digits).format(this);
	}

	/** Equals */
	public boolean equals(Pressure a) {
		if(a == null)
			return false;
		else 
			return a.pascals() == pascals();
	}

	/** Convert to the specified units.
	 * @param nu Units to convert to.
	 * @return New object in the specified units */
	public Pressure convert(Units nu) {
		if (nu == units)
			return this;
		return new Pressure(asDouble(nu), nu);
	}
	/** Get double representation of the pressure converted to specified units */
	public double asDouble(Units u){
		if (units == u)
			return value;
		return value * (units.scale/u.scale);
	}

	/** Get value */
	public double pascals() {
		return asDouble(Units.PASCALS);
	}

	/** Get pressure as an NTCIP value.
	 * @return Pressure in 1/10 mbar = 1/10 hPa = 1 microbar;
	 * 	See NTCIP essAtmosphericPressure. */
	public Integer ntcip() {
		return Integer.valueOf((int) Math.round(asDouble(Units.MICROBARS)));
	}

	/** Unit formatter */
	static public class Formatter {
		private final NumberFormat format;
		public Formatter(int d) {
			format = NumberFormat.getInstance();
			format.setMaximumFractionDigits(d);
			format.setMinimumFractionDigits(d);
		}
		public String format(Pressure p) {
			StringBuilder sb = new StringBuilder();
			sb.append(format.format(p.value)).append(" ").
				append(p.units.label);
			return sb.toString();
		}
	}

	/** Calculate sea-level pressure
	 * @param altm Altitude in meters corresponding to php
	 * @param php Pressure in hPa
	 * @param tc Temperature in C
	 * @return Pressure at sea-level or null on error */
	static public Pressure toSeaLevel(double altm, double php, double tc) {
		if (tc < -273.15)
			return null;
		final double den = tc + .0065 * altm + 273.15;
		if (den != 0) {
			final double base = 1 - .0065 * altm / den;
			if (base != 0) {
				final double hpa = php * Math.pow(base, -5.257);
				final int hpai = (int)Math.round(hpa);
				return create(hpai, Units.HECTOPASCALS);
			}
		}
		return null;
	}

	/** Convert to sea-level pressure
	 * @param altm Altitude in meters corresponding to pressure
	 * @param tc Temperature in C
	 * @return Pressure at sea-level or null on error */
	public Pressure toSeaLevel(double altm, double tc) {
		final double php = convert(Units.HECTOPASCALS).value;
		return toSeaLevel(altm, php, tc);
	}
}
