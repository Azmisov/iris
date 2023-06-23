package us.mn.state.dot.tms.utils;

import java.util.Arrays;
import java.util.Stack;
import java.util.regex.Matcher;

/** Wrapper around StringBuilder for building JSON directly to a string
 * 
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public class JsonBuilder {
	/** Holds JSON serialization state */
	public enum State{
		/** root JSON object */
		root(0b1),
		/** inside string */
		string(0b10),
		/** inside list */
		list(0b100),
		/** inside object, awaiting string key */
		object_key(0b1000),
		/** inside object, awaiting value */
		object_val(0b10000),
		/** JSON finisehd */
		complete(0b100000);

		/** Bitflag for state testing */
		final public int f;
		private State(int flag){
			this.f = flag;
		}
	};
	/** Currently serialized JSON */
	private final StringBuilder sb = new StringBuilder();
	/** State stack, since not a context free grammar */
	private final Stack<State> stack = new Stack<>();
	/** Whether added list/object items need a comma/colon prepended */
	private boolean separator = false;

	/** Interface to indicate JSON can be generated from that object */
	public interface Buildable{
		public void toJson(JsonBuilder jb);
	}

	/** Custom Exception thrown when trying to build invalid JSON */
	public static class Exception extends RuntimeException{
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
	private void ensureNotState(int mask){
		if ((state().f & mask) != 0)
			throw error();
	}
	/** Throws exception if state doesn't matches mask */
	private void ensureState(int mask){
		if ((state().f & mask) == 0)
			throw error();
	}
	/** Close the current object being built; perform state transitions
	 * @param virtual whether the state change was inferred virtually; so no
	 * 	need to update stack
	 */
	private void pop(boolean virtual){
		if (!virtual)
			stack.pop();
		// it is trivial to see that all token starts except the first are
		// preceded by comma or in the case of key-value pairs, a colon
		separator = true;
		State next;
		switch (state()){
			case object_key:
				next = State.object_val;
				break;
			case object_val:
				next = State.object_key;
				break;
			case root:
				next = State.complete;
				break;
			// no transition
			default: return;
		}
		stack.set(stack.size()-1, next);
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
	public String toJson(){
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
		while (true) {
			var state = state();
			switch (state){
				// final cases
				case complete:
					return this;
				case root:
					sb.append("null");
					stack.set(0, State.complete);
					return this;
				// intermediate cases
				case object_val:
					sb.append(":null}");
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
			// takes care of object val<->key and complete transition
			pop();
		}
	}

	/** Reset JsonBuilder */
	public JsonBuilder clear(){
		sb.setLength(0);
		stack.clear();
		stack.push(State.root);
		separator = false;
		return this;
	}

	///////////// JSON STRUCTURES ////////////////

	/** Begin JSON object, `{...}` */
	public JsonBuilder beginObject(){
		ensureNotState(State.object_key.f | State.string.f | State.complete.f);
		separator();
		separator = false;
		sb.append('{');
		stack.push(State.object_key);
		return this;
	}
	/** End JSON object, `{...}` */
	public JsonBuilder endObject(){
		ensureState(State.object_key.f);
		sb.append('}');
		pop();
		return this;        
	}

	/** Begin JSON list, `[...]` */
	public JsonBuilder beginList(){
		ensureNotState(State.object_key.f | State.string.f | State.complete.f);
		separator();
		separator = false;
		sb.append('[');
		stack.push(State.list);
		return this;
	}
	/** End JSON object, `[...]` */
	public JsonBuilder endList(){
		ensureState(State.list.f);
		sb.append(']');
		pop();
		return this;
	}

	/** Begin JSON string, `"..."` */
	public JsonBuilder beginString(){
		ensureNotState(State.complete.f | State.string.f);
		separator();
		separator = false;
		sb.append('"');
		stack.push(State.string);
		return this;
	}
	/** End JSON object, `"..."` */
	public JsonBuilder endString(){
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

	/** Create an empty list */
	public JsonBuilder list(){
		return beginList().endList();
	}
	/** Create a list with contents delegated to a single object */
	public JsonBuilder list(Buildable val){
		return beginList().extend(val).endList();
	}
	/** Create a list with contents delegated to multiple objects */
	public JsonBuilder list(Iterable<Buildable> lst){
		return beginList().extend(lst).endList();
	}
	/** Create a list with contents delegated to multiple objects */
	public JsonBuilder list(Buildable[] lst){
		return beginList().extend(lst).endList();
	}
	/** Create a list with contents given by multiple values */
	public <T> JsonBuilder list(T[] vals){
		return beginList().values(vals).endList();
	}
	/** Create a list with contents given by multiple values */
	public JsonBuilder list(Float[] vals){
		return beginList().values(vals).endList();
	}
	/** Create a list with contents given by multiple values */
	public JsonBuilder list(Double[] vals){
		return beginList().values(vals).endList();
	}
	/** Create a list with contents given by multiple values */
	public JsonBuilder list(String[] vals){
		return beginList().values(vals).endList();
	}

	/** Create an empty object */
	public JsonBuilder object(){
		return beginObject().endObject();
	}
	/** Create an object with contents delegated to a single object */
	public JsonBuilder object(Buildable val){
		return beginObject().extend(val).endObject();
	}
	/** Create an object with contents given by multiple ordered objects */
	public JsonBuilder object(Iterable<Buildable> lst){
		return beginObject().extend(lst).endObject();
	}
	/** Create an object with contents given by multiple ordered objects */
	public JsonBuilder object(Buildable[] lst){
		return beginObject().extend(lst).endObject();
	}

	/** Inserts an object key, which is a string*/
	public <K> JsonBuilder key(K key){
		ensureState(State.object_key.f);
		return string(key);
	}

	/** Insert a key+value pair into an object */
	public <K,V> JsonBuilder pair(K key, V val){
		return key(key).value(val);
	}
	/** Specialization of {@link #pair} */
	public <K> JsonBuilder pair(K key, String val){
		return key(key).string(val);
	}
	/** Specialization of {@link #pair} */
	public <K> JsonBuilder pair(K key, Double val){
		return key(key).value(val);
	}
	/** Specialization of {@link #pair} */
	public <K> JsonBuilder pair(K key, Float val){
		return key(key).value(val);
	}
	/** Specialization of {@link #pair} */
	public <K> JsonBuilder pair(K key, Buildable val){
		return key(key).extend(val);
	}

	/** Insert key, but only if in an object */
	private <K> JsonBuilder maybeKey(K key){
		var s = state();
		if (s == State.complete)
			throw error();
		if (s == State.object_key)
			string(key);
		return this;		
	}

	/** This is the same as {@link #pair}, but does some intelligent analysis of
	 * the current context. If we're not inside an object, just the value will
	 * be inserted.
	 */
	public <K, V> JsonBuilder pairOrValue(K key, V val){
		return maybeKey(key).value(val);
	}
	/** Specialization of {@link #pairOrValue} */
	public <K> JsonBuilder pairOrValue(K key, String val){
		return maybeKey(key).string(val);
	}
	/** Specialization of {@link #pairOrValue} */
	public <K> JsonBuilder pairOrValue(K key, Double val){
		return maybeKey(key).value(val);
	}
	/** Specialization of {@link #pairOrValue} */
	public <K> JsonBuilder pairOrValue(K key, Float val){
		return maybeKey(key).value(val);
	}
	/** Specialization of {@link #pairOrValue} */
	public <K> JsonBuilder pairOrValue(K key, Buildable val){
		return maybeKey(key).extend(val);
	}

	//////////// JSON ELEMENTS ////////////

	/** Escapes value as JSON string */
	private <T> String escaped(T value){
		// we could allow null -> "null", but I don't see that being a commmon use
		if (value == null)
			return "";
		String s = String.valueOf(value);
		return s.replaceAll("\"", Matcher.quoteReplacement("\\\""));
	}
	/** For use with {@link #beginString} and {@link #endString}. This
	 * serializes data as part of the string in between, taking care of proper
	 * character escaping
	 * @param value will be converted to a string via {@link String#valueOf};
	 * 	null is converted to empty string
	 */
	public <T> JsonBuilder append(T value){
		ensureState(State.string.f);
		sb.append(escaped(value));
		return this;
	}
	/** Inserts a value as a new JSON string. Null values are serialized as
	 * an empty string
	 */
	public <T> JsonBuilder string(T value){
		ensureNotState(State.complete.f | State.string.f);
		separator();
		sb.append('"').append(escaped(value)).append('"');
		pop(true);
		return this;
	}

	/** Convert Float to raw JSON string */
	private String serializeFloat(Float v){
		if (v == null || !Float.isFinite(v))
			return "null";
		// remove unnecessary decimal
		int i = v.intValue();
		if (i == v)
			return String.valueOf(i);
		return String.valueOf(v);
	}
	/** Convert Double to raw JSON string */
	private String serializeDouble(Double v){
		if (v == null || !Double.isFinite(v))
			return "null";
		// remove unnecessary decimal
		int i = v.intValue();
		if (i == v)
			return String.valueOf(i);
		return String.valueOf(v);
	}

	/** Inserts the string representation of a value directly into the raw JSON.
	 * Use this to insert a boolean, null, or objects. It serializes
	 * using `String.valueOf`, so can result in invalid JSON if the object
	 * is not returning a valid literal.
	 */
	public <T> JsonBuilder value(T value){
		ensureNotState(State.object_key.f | State.string.f | State.complete.f);
		return raw(value);
	}
	/** Same as {@link #value}, but converts infinities and NaN's to null,
	 * as they are not suppoted in JSON. Other than these, the numbers are
	 * serialized with full precision. */
	public JsonBuilder value(Float value){
		ensureNotState(State.object_key.f | State.string.f | State.complete.f);
		return raw(serializeFloat(value));
	}
	/** Same as {@link #value}, but converts infinities and NaN's to null,
	 * as they are not suppoted in JSON. Other than these, the numbers are
	 * serialized with full precision. */
	public JsonBuilder value(Double value){
		ensureNotState(State.object_key.f | State.string.f | State.complete.f);
		return raw(serializeDouble(value));
	}
	/** Inserts a string literal, e.g. an alias of {@link #string} specifically
	 * for String objects. If you want to insert a raw literal as a String,
	 * e.g. you want to serialize a number in a custom foramt, use {@link #raw}
	 * instead.
	 */
	public JsonBuilder value(String value){
		return string(value);
	}

	/** Same as {@link #value}, but inserts multiple boolean, null, or objects.
	 * Use this for inserting the contents of a list. Due to problematic type
	 * erasure on collections, no `Iterable<T>` method signature is currently
	 * provided
	 */
	public <T> JsonBuilder values(T[] lst){
		ensureState(State.list.f);
		return raws(Arrays.asList(lst));
	}
	/** Same as {@link #value}, but inserts multiple values. Use this for
	 * inserting the contents of a list
	 */
	public JsonBuilder values(Float[] lst){
		ensureState(State.list.f);
		return rawsTransform(Arrays.asList(lst), v -> serializeFloat(v));
	}
	/** Same as {@link #value}, but inserts multiple values. Use this for
	 * inserting the contents of a list
	 */
	public JsonBuilder values(Double[] lst){
		ensureState(State.list.f);
		return rawsTransform(Arrays.asList(lst), v -> serializeDouble(v));
	}
	/** Same as {@link #value}, but inserts multiple values. Use this for
	 * inserting the contents of a list
	 */
	public JsonBuilder values(String[] lst){
		ensureState(State.list.f);
		return rawsTransform(Arrays.asList(lst), v -> {
			return '"'+escaped(v)+'"';
		});
	}

	/** This is the same as {@link #value}, but does not validate the current
	 * JSON state. You can use this for more powerful raw, unsafe JSON
	 * manipulation. E.g. inserting a full list, full object, or key-value pair.
	 * The {@link #state} should not have changed, had the value been inserted
	 * using the safe JSON methods, otherwise the resulting JSON could be
	 * invalid.
	 */
	public <T> JsonBuilder raw(T value){
		separator();
		sb.append(String.valueOf(value));
		pop(true);
		return this;		
	}
	/** Same as {@link #values}, but does not validate the current state. This
	 * bears the same warnings as {@link #raw}
	 */
	public <T> JsonBuilder raws(Iterable<T> lst){
		for (var val : lst){
			separator();
			sb.append(String.valueOf(val));
			separator = true;
		}
		pop(true);
		return this;
	}

	private interface TransformLambda<O,T>{
		T transform(O val);
	}
	/** Used internally to implement {@link #values} variants */
	private <O,T> JsonBuilder rawsTransform(
		Iterable<O> lst, TransformLambda<O,T> transformation
	){
		for (var val : lst){
			separator();
			sb.append(String.valueOf(
				transformation.transform(val)
			));
			separator = true;
		}
		pop(true);
		return this;
	}
}
