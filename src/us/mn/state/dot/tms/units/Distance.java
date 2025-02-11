package us.mn.state.dot.tms.units;

import java.text.NumberFormat;
import us.mn.state.dot.tms.SystemAttrEnum;

/**
 * Distance between two points.
 *
 * @author Douglas Lau
 * @copyright 2012-2020  Minnesota Department of Transportation
 * @author Michael Darter, Isaac Nygaard
 * @copyright 2017-2023  Iteris Inc.
 * @license GPL-2.0
 */
public final class Distance implements Comparable<Distance> {

	/** Enumeration of distance units */
	public enum Units {
		METERS(1.0, "m"),
		KILOMETERS(1000.0, "km"),
		DECIMETERS(0.1, "dm"),
		CENTIMETERS(0.01, "cm"),
		MILLIMETERS(0.001, "mm"),
		TENTH_MILLIMETERS(0.0001, "\u2152mm"),
		HUNDREDTH_MILLIMETERS(0.00001, "[1/100mm]"),
		MICROMETERS(0.000001, "um"),
		MILES(1609.344, "mi"),
		FEET(MILES.meters / 5280, "ft"),
		INCHES(FEET.meters / 12, "in"),
		YARDS(FEET.meters * 3, "yd");

		/** Conversion rate to meters */
		public final double meters;

		/** Unit label */
		public final String label;

		/** Create units */
		private Units(double m, String l) {
			meters = m;
			label = l;
		}
	}

	/** Factory to create a new quantity with the null case handled.
	 * @param v Value in units u or null.
	 * @param u Units for arg v.
	 * @return A new quantity in system units or null */
	static public Distance create(Integer v, Units u) {
		Distance d = null;
		if (v != null) {
			d = new Distance(v, u);
			if (!useSi())
				d = d.convert(Units.FEET);
		}
		return d;
	}

	/** Get system units */
	static private boolean useSi() {
		return SystemAttrEnum.CLIENT_UNITS_SI.getBoolean();
	}

	/** Distance value */
	public final double value;

	/** Distance units */
	public final Units units;

	/** Create a new distance.
	 * @param v Value of distance.
	 * @param u Units of distance. */
	public Distance(double v, Units u) {
		value = v;
		units = u;
	}

	/** Create a new distance.
	 * @param v Value in meters. */
	public Distance(double v) {
		this(v, Units.METERS);
	}

	/** Get the distance in meters */
	public double m() {
		return value * units.meters;
	}

	/** Convert a distance to specified units.
	 * @param u Units to convert to.
	 * @return Distance in specified units. */
	public Distance convert(Units u) {
		if (u == units)
			return this;
		else {
			double v = m();
			return new Distance(v / u.meters, u);
		}
	}

	/** Get a distance as a float in specified units.
	 * @param u Units to return.
	 * @return Distance as a float value. */
	public float asFloat(Units u) {
		return (float) asDouble(u);
	}

	/** Get a distance as a double in specified units.
	 * @param u Units to return.
	 * @return Distance as a double value. */
	public double asDouble(Units u) {
		if (u == units)
			return value;
		return (m() / u.meters);
	}

	/** Round a distance to nearest whole unit.
	 * @param u Units to return.
	 * @return Distance rounded to nearest whole unit. */
	public int round(Units u) {
		return (int) Math.round(asDouble(u));
	}

	/** Add another distance.
	 * @param d Other distance.
	 * @return Sum of distances. */
	public Distance add(Distance d) {
		if (d.units == units)
			return new Distance(value + d.value, units);
		else
			return new Distance(m() + d.m());
	}

	/** Subtract another distance.
	 * @param d Other distance.
	 * @return Result. */
	public Distance sub(Distance d) {
		if (d.units == units)
			return new Distance(value - d.value, units);
		else
			return new Distance(m() - d.m());
	}

	/** Compare for equality */
	@Override
	public boolean equals(Object other) {
		if (other instanceof Distance) {
			Distance o = (Distance) other;
			if (units == o.units)
				return value == o.value;
			else
				return m() == o.m();
		} else
			return false;
	}

	/** Compare with another distance */
	@Override
	public int compareTo(Distance o) {
		if (units == o.units)
			return Double.compare(value, o.value);
		else
			return Double.compare(m(), o.m());
	}

	/** Get a distance hash code */
	@Override
	public int hashCode() {
		return Double.valueOf(m()).hashCode();
	}

	/** Get a string representation of a distance */
	@Override
	public String toString() {
		return new Formatter(1).format(this);
	}

	/** Distance formatter */
	static public class Formatter {
		private final NumberFormat format;
		public Formatter(int d) {
			format = NumberFormat.getInstance();
			format.setMaximumFractionDigits(d);
			format.setMinimumFractionDigits(d);
		}
		public String format(Distance d) {
			return format.format(d.value) + " " + d.units.label;
		}
	}
}
