package us.mn.state.dot.tms.units;

import java.text.NumberFormat;

/**
 * Time interval values.
 *
 * @author Douglas Lau
 * @author Isaac Nygaard
 * @copyright 2012-2020 Minnesota Department of Transportation
 * @copyright 2023 Iteris, Inc
 * @license GPL-2.0
 */
public final class Interval implements Comparable<Interval> {

	/** Enumeration of interval units */
	public enum Units {
		MILLISECONDS(0.001, "ms"),
		DECISECONDS(0.1, "ds"),
		SECONDS(1, "s"),
		MINUTES(60, "min"),
		HOURS(60 * MINUTES.seconds, "hr"),
		DAYS(24 * HOURS.seconds, "d"),
		WEEKS(7 * DAYS.seconds, "wk");

		/** Conversion rate to seconds */
		public final double seconds;

		/** Unit label */
		public final String label;

		/** Create units */
		private Units(double s, String l) {
			seconds = s;
			label = l;
		}
	}

	/** 1 Hour interval */
	static public final Interval HOUR = new Interval(1, Units.HOURS);

	/** 1 Day interval */
	static public final Interval DAY = new Interval(1, Units.DAYS);

	/** Interval value */
	public final double value;

	/** Interval units */
	public final Units units;

	/** Create a new time interval.
	 * @param v Value of interval.
	 * @param u Units of interval. */
	public Interval(double v, Units u) {
		value = v;
		units = u;
	}

	/** Create a new time interval.
	 * @param v Value in seconds. */
	public Interval(double v) {
		this(v, Units.SECONDS);
	}

	/** Get the interval in milliseconds */
	public long ms() {
		return asLong(Units.MILLISECONDS);
	}

	/** Number of seconds in interval */
	public double seconds() {
		return asDouble(Units.SECONDS);
	}

	/** Get rounded, long representation of interval, converted to specified units */
	public long asLong(Units units){
		return (long) Math.round(asDouble(units));
	}

	/** Get float representation of interval, converted to specified units */
	public float asFloat(Units units){
		return (float) asDouble(units);
	}

	/** Get double representation of interval, converted to specified units */
	public double asDouble(Units units){
		return (value*this.units.seconds)/units.seconds;
	}

	/** Convert an interval to specified units.
	 * @param u Units to convert to.
	 * @return Interval in specified units. */
	public Interval convert(Units u) {
		if (u == units)
			return this;
		else {
			double s = seconds();
			return new Interval(s / u.seconds, u);
		}
	}

	/** Round an interval to nearest whole unit.
	 * @param u Units to return.
	 * @return Interval rounded to nearest whole unit. */
	public int round(Units u) {
		if (u == units)
			return (int)Math.round(value);
		else
			return (int)Math.round(seconds() / u.seconds);
	}

	/** Floor an interval to whole unit.
	 * @param u Units to return.
	 * @return Interval floored to whole unit. */
	public int floor(Units u) {
		if (u == units)
			return (int)value;
		else
			return (int)(seconds() / u.seconds);
	}

	/** Divide into another interval */
	public float per(Interval i) {
		double s = seconds();
		if (s > 0)
			return (float)(i.seconds() / s);
		else
			return 0;
	}

	/** Divide into equal parts */
	public float divide(int i) {
		return (float)(seconds() / i);
	}

	/** Add another interval.
	 * @param o Other interval.
	 * @return Sum of intervals. */
	public Interval add(Interval o) {
		if (o.units == units)
			return new Interval(value + o.value, units);
		else
			return new Interval(seconds() + o.seconds());
	}

	/** Compare for equality */
	@Override
	public boolean equals(Object other) {
		if (other instanceof Interval) {
			Interval o = (Interval)other;
			if (units == o.units)
				return value == o.value;
			else
				return seconds() == o.seconds();
		} else
			return false;
	}

	/** Compare with another interval */
	@Override
	public int compareTo(Interval o) {
		if (units == o.units)
			return Double.compare(value, o.value);
		else
			return Double.compare(seconds(), o.seconds());
	}

	/** Get an interval hash code */
	@Override
	public int hashCode() {
		return Double.valueOf(seconds()).hashCode();
	}

	/** Get a string representation of an interval */
	@Override
	public String toString() {
		return new Formatter(0).format(this);
	}

	/** Interval formatter */
	static public class Formatter {
		private final NumberFormat format;
		public Formatter(int d) {
			format = NumberFormat.getInstance();
			format.setMaximumFractionDigits(d);
			format.setMinimumFractionDigits(d);
		}
		public String format(Interval i) {
			return format.format(i.value) + " " + i.units.label;
		}
	}
}
