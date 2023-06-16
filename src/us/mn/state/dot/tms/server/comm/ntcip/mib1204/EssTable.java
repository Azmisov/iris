package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import java.util.ArrayList;
import java.util.Iterator;

import us.mn.state.dot.tms.server.comm.ntcip.EssValues;

/**
 * Abstract base class for ESS tables.
 *
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
abstract public class EssTable<R extends EssValues> extends EssValues implements Iterable<R>{
    /** Rows in table */
	protected final ArrayList<R> table_rows = new ArrayList<R>();
	protected EssNumber sensor_count;

	/** Set number of sensors reported for this table */
	public void setSensorCount(EssNumber sensor_count){
		this.sensor_count = sensor_count;
	}

    /** Get number of rows/sensors in table reported by ESS, defaulting to
	 * zero when there is an error
	 */
	public int size(){
		Integer s = sensor_count.toInteger();
		return s == null ? 0 : s;
	}

	/** Check if all rows have been added */
	public boolean isDone() {
		return table_rows.size() >= size();
	}

    /** Used to instantiate a new row with that row number */
    abstract protected R createRow(int row_num);

	protected static interface FallbackLambda<T, R>{
		public T operation(R row);
	}

	/** Retrieve a member from the first row, if exists, otherwise fallback
	 * to the `overall` variable
	 */
	public <T> T fallback(FallbackLambda<T, R> lambda, T overall){
		if (table_rows.isEmpty())
			return overall;
		return lambda.operation(table_rows.get(0));
	}

	/** Add a row to the table */
	public R addRow(){
        R row = createRow(table_rows.size() + 1);
		table_rows.add(row);
		return row;
    }

	/** Get one table row, where row indices start with 1
     * @return null if row is out of range */
	public R getRow(int row) {
		if (row >= 1 && row <= table_rows.size())
		    return table_rows.get(row - 1);
		return null;
	}

	/** Interface for iterating through rows */
	@Override
	public Iterator<R> iterator(){
		return table_rows.iterator();
	}

    /** Convert to string representation; the abstract implementation will
     * serialize the table rows via simple concatentation */
    public String toString(){
        StringBuilder sb = new StringBuilder();
		for (R row : table_rows)
            sb.append(row.toString());
        return sb.toString();
    }

    /** Convert to Json representation; the abstract implementation will
     * serialize the table rows as a Json list, or an empty string if no rows */
    public String toJson(){
        StringBuilder sb = new StringBuilder();
		if (table_rows.size() > 0) {
			for (R row : table_rows)
				sb.append(row.toJson());
			// remove trailing comma
			if (sb.charAt(sb.length() - 1) == ',')
				sb.setLength(sb.length() - 1);
		}
		return sb.toString();
    }
}