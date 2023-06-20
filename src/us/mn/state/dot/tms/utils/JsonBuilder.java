package us.mn.state.dot.tms.utils;

import java.util.Arrays;
import java.util.Stack;

/** Wrapper around StringBuilder for building JSON directly to a string
 * 
 * TODO:
 * 	- key-value pair
 * 	- conditional key-value, key, or string
 *
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public class JsonBuilder {
	public enum State{
		root(0b1), // root object
		string(0b10), // inside string
		list(0b100), // inside list
		object_key(0b1000), // inside object, awaiting string
		object_val(0b10000), // inside object, awaiting value
		complete(0b100000); // done

		/** Bitflag for state testing */
		final public int f;
		private State(int flag){
			this.f = flag;
		}
	};
	private Stack<State> stack = new Stack<>();
	/** Whether added list/object items need a comma/colon prepended */
	private boolean separator = false;
	/** Currently serialized JSON */
	private StringBuilder sb = new StringBuilder();

	/** Interface to indicate JSON can be generated from that object */
	public interface Buildable{
		public void toJson(JsonBuilder jb);
	}

	/** Custom Exception thrown when trying to build invalid JSON */
	public static class Exception extends java.lang.Exception{
		/** JSON string at time of error */
		public final String json;
		/** What was expected */
		public final String expected;

		public Exception(String json, String expected){
			super("Attempted to create invalid JSON; expected "+expected);
			this.json = json;
			this.expected = expected;
		}
	}

	public JsonBuilder(){
		// initial state
		stack.push(State.root);
	}

	/////////// STATE MANAGEMENT ////////////

	/** Create a new Exception arising from the current builder state */
	private Exception error(){
		String json = toString();
		String expected = null;
		switch (state()){
			case root:
			case object_val:
				expected = "value";
				break;
			case string:
				expected = "characters or string end";
				break;
			case list:
				expected = "value or list end";
				break;
			case object_key:
				expected = "string or object end";
				break;
			case complete:
				expected = "EOF (JSON is complete)";
				break;
		}
		return new Exception(json, expected);
	}

	/** Get current state. Can be used to implement state dependent
	 * serialization of objects. E.g. if inside an object, generating a
	 * key-value pair, or if inside a list, simply generating the value
	 */
	public State state(){
		return stack.peek();
	}
	/** Throws exception if state matches mask */
	private void ensureNotState(int mask) throws Exception{
		if ((state().f & mask) != 0)
			throw error();
	}
	/** Throws exception if state doesn't matches mask */
	private void ensureState(int mask) throws Exception{
		if ((state().f & mask) == 0)
			throw error();
	}
	/** Close the current object being built
	 * @param virtual whether the state change was inferred virtually; so no
	 * 	need to update stack
	 */
	private void pop(boolean virtual){
		if (!virtual)
			stack.pop();
		// it is trivial to see that all token starts except the first are
		// preceded by comma or in the case of key-value pairs, a colon
		separator = true;
		// finished?
		if (state() == State.root){
			stack.pop();
			stack.push(State.complete);
		}
	}
	/** Same as {@link #pop}, but virtual defaulting to false */
	private void pop(){
		pop(false);
	}
	/** Add separator if state is appropriate */
	private void separator(){
		if (separator)
			sb.append(state() == State.object_val ? ':' : ',');
	}

	///////////// FINALIZATION //////////////

	/** Return serialized JSON */
	public String toJson() throws Exception{
		ensureState(State.complete.f);
		return toString();
	}
	/** Returns the current raw, unvalidated string. Use {@link #toJson} to
	 * validate that the JSON has been completed
	 */
	public String toString(){
		return sb.toString();
	}

	/** Close all unclosed lists/objects/strings; if an object value is missing,
	 * insert null. This places the JSON in a valid stringified state */
	public JsonBuilder end(){
		do {
			var state = state();
			switch (state){
				case complete:
					return this;
				case root:
					sb.append("null");
					break;
				case object_val:
					sb.append("null}");
					break;
				case object_key:
					sb.append('}');
					break;
				case list:
					sb.append(']');
					break;
				case string:
					sb.append('"');
					break;
			}
			stack.pop();
		} while (!stack.isEmpty());
		stack.push(State.complete);
		return this;
	}

	///////////// JSON STRUCTURES ////////////////

	/** Begin JSON object, `{...}` */
	public JsonBuilder beginObject() throws Exception{
		ensureNotState(State.object_key.f | State.complete.f);
		separator();
		separator = false;
		sb.append('{');
		stack.push(State.object_key);
		return this;
	}
	/** End JSON object, `{...}` */
	public JsonBuilder endObject() throws Exception{
		ensureState(State.object_key.f);
		sb.append('}');
		pop();
		return this;        
	}

	/** Begin JSON list, `[...]` */
	public JsonBuilder beginList() throws Exception{
		ensureNotState(State.object_key.f | State.complete.f);
		separator();
		separator = false;
		sb.append('[');
		stack.push(State.list);
		return this;
	}
	/** End JSON object, `[...]` */
	public JsonBuilder endList() throws Exception{
		ensureState(State.list.f);
		sb.append(']');
		pop();
		return this;
	}

	/** Begin JSON string, `"..."` */
	public JsonBuilder beginString() throws Exception{
		ensureNotState(State.complete.f);
		separator();
		separator = false;
		sb.append('"');
		stack.push(State.string);
		return this;
	}
	/** End JSON object, `"..."` */
	public JsonBuilder endString() throws Exception{
		ensureState(State.string.f);
		sb.append('"');
		pop();
		return this;
	}

	/** Extend current JSON with serialization from multiple ordered objects */
	public JsonBuilder extend(Iterable<Buildable> lst){
		for (var val : lst)
			val.toJson(this);
		return this;
	}
	/** Extend current JSON with serialization from multiple ordered objects */
	public JsonBuilder extend(Buildable[] lst){
		return extend(Arrays.asList(lst));
	}
	/** Extend current JSON with serialization from a single object */
	public JsonBuilder extend(Buildable val){
		val.toJson(this);
		return this;
	}

	/** Create a list with contents given by a lambda */
	public JsonBuilder list(Runnable lambda) throws Exception{
		beginList();
		lambda.run();
		return endList();
	}
	/** Create a list with contents given by multiple ordered objects */
	public JsonBuilder list(Iterable<Buildable> lst) throws Exception{
		return list(() -> extend(lst));
	}
	/** Create a list with contents given by multiple ordered objects */
	public JsonBuilder list(Buildable[] lst) throws Exception{
		return list(() -> extend(lst));
	}
	/** Create a list with contents given by a single object */
	public JsonBuilder list(Buildable val) throws Exception{
		return list(() -> extend(val));
	}

	/** Create an object with contents given by a lambda */
	public JsonBuilder object(Runnable lambda) throws Exception{
		beginObject();
		lambda.run();
		return endObject();
	}
	/** Create an object with contents given by multiple ordered objects */
	public JsonBuilder object(Iterable<Buildable> lst) throws Exception{
		return object(() -> extend(lst));
	}
	/** Create an object with contents given by multiple ordered objects */
	public JsonBuilder object(Buildable[] lst) throws Exception{
		return object(() -> extend(lst));        
	}
	/** Create an object with contents given by a single object */
	public JsonBuilder object(Buildable val) throws Exception{
		return object(() -> extend(val));
	}

	//////////// JSON ELEMENTS ////////////

	/** Escapes value as JSON string */
	private <T> String escaped(T value){
		// we could allow null -> "null", but I don't see that being a commmon use
		if (value == null)
			return "";
		String s = String.valueOf(value);
		return s.replaceAll("\"", "\\\"");
	}
	/** For use with {@link #beginString} and {@link #endString}. This
	 * serializes data as part of the string in between, taking care of proper
	 * character escaping
	 * @param value will be converted to a string via {@link String#valueOf};
	 * 	null is converted to empty string
	 */
	public <T> JsonBuilder append(T value) throws Exception{
		ensureState(State.string.f);
		sb.append(escaped(value));
		return this;
	}
	/** Inserts a value as a new JSON string. Null values are serialized as
	 * an empty string
	 */
	public <T> JsonBuilder string(T value) throws Exception{
		ensureNotState(State.complete.f);
		separator();
		sb.append('"').append(escaped(value)).append('"');
		pop(true);
		return this;
	}

	/** Inserts the string representation of a value directly into the raw JSON.
	 * Use this to insert a boolean, null, or objects. It serializes
	 * using `String.valueOf`, so can result in invalid JSON if the object
	 * is not returning a valid literal.
	 */
	public <T> JsonBuilder value(T value) throws Exception{
		ensureNotState(State.object_key.f | State.complete.f);
		return raw(value);
	}
	/** Same as {@link #value}, but converts infinities and NaN's to null,
	 * as they are not suppoted in JSON. Other than these, the numbers are
	 * serialized with full precision. */
	public JsonBuilder value(Float value) throws Exception{
		ensureNotState(State.object_key.f | State.complete.f);
		if (value != null && !Float.isFinite(value))
			value = null;
		return raw(value);
	}
	/** Same as {@link #value}, but converts infinities and NaN's to null,
	 * as they are not suppoted in JSON. Other than these, the numbers are
	 * serialized with full precision. */
	public JsonBuilder value(Double value) throws Exception{
		ensureNotState(State.object_key.f | State.complete.f);
		if (value != null && !Double.isFinite(value))
			value = null;
		return raw(value);
	}
	/** Inserts a string literal, e.g. an alias of {@link #string} specifically
	 * for String objects. If you want to insert a raw literal as a String,
	 * e.g. you want to serialize a number in a custom foramt, use {@link #raw}
	 * instead.
	 */
	public <T> JsonBuilder value(String value) throws Exception{
		return string(value);
	}
	/** This is the same as {@link #value}, but does not validate the current
	 * JSON state. You can use this for more powerful raw, unsafe JSON
	 * manipulation. E.g. inserting a full list, full object, or key-value pair.
	 * The {@link #state} should not have changed, had the value been inserted
	 * using the safe JSON methods, otherwise the resulting JSON could be
	 * invalid.
	 */
	public <T> JsonBuilder raw(T value) throws Exception{
		separator();
		sb.append(String.valueOf(value));
		pop(true);
		return this;		
	}
}
