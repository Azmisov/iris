package us.mn.state.dot.tms.units;

/**
 * Immutable angle with methods that return the angle in
 * radians or degrees.
 *
 * @author Michael Darter, Isaac Nygaard
 * @copyright 2011 AHMCT, University of California
 * @copyright 2017 Iteris Inc.
 * @license GPL-2.0
 */
final public class Angle {

	/** Enumeration of distance units */
	public enum Units {
		RADIANS(1.0, "rad"),
		DEGREES(Math.PI/180.0, "deg"),
		REVOLUTIONS(2*Math.PI, "rev");

		/** Conversion rate to meters */
		public final double rads;

		/** Unit label */
		public final String label;

		/** Create units */
		private Units(double rads, String lbl) {
			this.rads = rads;
			label = lbl;
		}
	}

	/** Angular value in {@link #units} */
	public final double value;
	/** Units for {@link #value} */
	public final Units units;

	/** Create a new angle
	 * @param value the angular value
	 * @param units the units for `value`
	 */
	public Angle(double value, Units units){
		this.value = value;
		this.units = units;
	}
	/** Create a new angle with default unit of degrees
	 * @param value the angular value
	 */
	public Angle(double value){
		this.value = value;
		this.units = Units.DEGREES;
	}
	/** Create new zero degree angle */
	public Angle(){ this(0); }

	/** Convert a distance to specified units.
	 * @param u Units to convert to.
	 */
	public Angle convert(Units u) {
		if (u == units)
			return this;
		return new Angle(asDouble(u), u);
	}
	/** Get double representation when converted to specified units */
	public double asDouble(Units u){
		if (u == units)
			return value;
		return value*(units.rads/u.rads);
	}

	/** Factory which handles the null case.
	 * @param ang Angle in degrees or null
	 * @return A new Angle or null */
	static public Angle create(Integer ang) {
		return (ang != null ? new Angle(ang) : null);
	}

	/** One complete revolution in radians */
	final static double REV = Units.REVOLUTIONS.rads;

	/** Return the ceiling revolution in radians.
	 * @param rads Angle in radians.
	 * @return The angle in radians of the next complete revolution. */
	static public double ceilRev(double rads) {
		if(rads >= 0)
			return REV * Math.ceil(rads / (REV));
		else
			return REV * Math.floor(rads / (REV));
	}

	/** Return the floor revolution in radians.
	 * @param rads Angle in radians.
	 * @return Angle in radians of the previous complete revolution. */
	static public double floorRev(double rads) {
		if(rads >= 0)
			return REV * Math.floor(rads / (REV));
		else
			return REV * Math.ceil(rads / (REV));
	}

	/** Normalize degrees to 0 - 359. See the test cases for more info.
	 * @param d Angle in degrees.
	 * @return Angle in degrees 0 - 359. */
	static public int toNormalizedDegs(double d) {
		int i = (int) round(d);
		return i < 0 ? 360 + (i % 360) : (i % 360);
	}

	/** Round a number */
	static protected double round(double num) {
		return round(num, 1);
	}

	/** Round a number to the specified precision.
	 * @param p Rounding precision, e.g. 1 for 0 digits after decimal,
	 *          10 for 1 digit after decimal etc. */
	static protected double round(double num, int p) {
		p = (p > 0 ? p : 1);
		return Math.round(num / (double)p) * (double)p;
	}

	/** Get units, which are degrees */
	public String getUnits() {
		return "\u00B0";
	}

	/** To string
	 * @return Angle in degrees with unit */
	public String toString() {
		return Integer.toString(toDegsInt()) + getUnits();
	}

	/** Equals */
	public boolean equals(Angle a) {
		if (a == null)
			return false;
		return a.asDouble(Units.RADIANS) == asDouble(Units.RADIANS);
	}

	/** Get angle in integer degrees */
	public int toDegsInt() {
		return (int)round(asDouble(Units.DEGREES));
	}

	/** Return a new Angle rounded in degrees.
	 * @param p Rounding precision, e.g. 10 for 1 digit after decimal.
	 * @return A new angle rounded in degrees. */
	public Angle round(int p) {
		return new Angle(round(asDouble(Units.DEGREES), p));
	}

	/** Get the angle in normalized degrees.
	 * @return Angle in degrees (0-359) */
	public int toNormalizedDegs() {
		return toNormalizedDegs(asDouble(Units.DEGREES));
	}

	/** Return an inverted angle, which is the equivalent angle 
	 * in the other direction. */
	public Angle invert() {
		var rads = asDouble(Units.RADIANS);
		return new Angle(floorRev(rads) + (ceilRev(rads) - rads), Units.RADIANS);
	}

	/** Return the direction as a human readable string.
	 * @return The direction as N, NE, E, SE, S, SW, W, NW */
	public String toShortDir() {
		int d = toNormalizedDegs();
		if(d <= 22)
			return "N";
		else if(d >= 23 && d <= 68)
			return "NE";
		else if(d >= 69 && d <= 112)
			return "E";
		else if(d >= 113 && d <= 158)
			return "SE";
		else if(d >= 159 && d <= 202)
			return "S";
		else if(d >= 203 && d <= 248)
			return "SW";
		else if(d >= 249 && d <= 292)
			return "W";
		else if(d >= 293 && d <= 337)
			return "NW";
		else
			return "N";
	}
}
