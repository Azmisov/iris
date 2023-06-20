package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import java.util.ArrayList;
import java.util.Iterator;
import us.mn.state.dot.tms.utils.JsonSerializable;

/** Abstract base class for ESS tables. To implement, call
 * {@link #setSensorCount} when constructing the subclass. You'll need to
 * define an internal row class to be used for the table, and also provide a
 * {@link #createRow} implementation to construct said rows. All other table
 * management is automatically handled
 *
 * @param <R> the internal row class for the table
 *
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
abstract public class EssTable<R extends JsonSerializable>
	implements JsonSerializable, Iterable<R>
{
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
		public T get(R row);
	}

	/** Retrieve a member from the first row, if exists, otherwise fallback
	 * to the `overall` variable
	 */
	public <T> T fallback(FallbackLambda<T, R> lambda, T overall){
		if (table_rows.isEmpty())
			return overall;
		return lambda.get(table_rows.get(0));
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

	protected static interface FindLambda<R>{
		public boolean matches(R row);
	}

	/** Retrieve the first row that meets a condition, or null if no rows meet
	 * the condition */
	public R findRow(FindLambda<R> lambda){
		for (R row : table_rows){
			if (lambda.matches(row))
				return row;
		}
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
     * serialize the table rows by appending, removing any trailing comma; or an
     * empty string is returned if there are no rows */
    public String toJson(){
		return JsonSerializable.toJson(this, true);
    }
}