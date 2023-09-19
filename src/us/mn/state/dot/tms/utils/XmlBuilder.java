package us.mn.state.dot.tms.utils;

import java.io.IOException;
import java.util.Stack;
import java.util.regex.Pattern;

/** Wrapper around StringBuilder or other Appendable for building XMl documents
 * directly as a string. Currently just supports basic XML tags and text with
 * prolog; no CDATA, comments, document type, processing instructions, or
 * namespaces. Can easily add these as they become necessary.
 *
 * Usage:
 * <pre>{@code
 *  var xml = new XmlBuilder().setStrict(true).setPrettyPrint(true);
 *  xml.tag("root").child()
 *      .tag("a").attr("attr","value").attr("num",10).child()
 *          .tag("b").parent()
 *      .tag("c");
 *  var str = xml.toXml();
 * }
 * </pre>
 *
 * You can toggle pretty printing for specific sections of the XML document.
 * You'll want to use the `greedy` parameter for {@link #child},
 * {@link #parent}, and {@link #ancestor} in between the {@link #setPrettyPrint}
 * calls; otherwise, it an be lazily written with pretty printing later on:
 * <pre>{@code
 *  var xml = new XmlBuilder().setPrettyPrint(true);
 *  xml.tag("root").child()
 *      .tag("childA").child().setPP(false)
 *          // using greedy parent() to ensure childA's closing tag doesn't get pretty printed
 *          .text("don't pretty print me").parent(true).setPP(true)
 *      .tag("childB");
 * }
 * </pre>
 * 
 * Writing the XML prolog is optional, in case you are generating sub-documents.
 * Disabling strict mode can also be useful for sub-documents.
 *
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public class XmlBuilder{
	// Making these non-static so they could be customized per XmlBuilder
	/** Line whitespace for pretty printing */
	public String LINE = "\n";
	/** Indentation whitespace for pretty printing */
	public String INDENT = "\t";

	/** Internal write buffer */
	private Appendable sb;
	/** Stack for the XML tree; empty if only prolog has been written */
	private Stack<Element> stack = new Stack<>();
	/** Waiting for child of active element (or root XML document) */
	private boolean await_child;
	/** True if we can add attributes to the active element */
	private boolean attrs;
	/** True if XML has been generated; used to control whether prolog writes */
	private boolean data_written;
	/** Whether to include pretty printing whitespace */
	public boolean pretty_print = false;
	/** Whether to operate in strict mode. This operates in a safer mode:
	 * <ul>
	 * 	<li>only allows one root element</li>
	 * 	<li>text cannot be inserted outside the root</li>
	 * 	<li>all entities are substituted in attribute values/text, rather than
	 * 		just those that are required by the XML grammar
	 * 	</li>
	 * </ul>
	 */
	public boolean strict = true;

	/** Valid XML name (not including tagname namespaces currently). Unlike
	 *  XmlWriter, I'm not auto-converting tag to valid name since bad tagnames
	 *  are likely a bug and so better to just throw an error
	 * @see https://www.w3.org/TR/2008/REC-xml-20081126/#NT-Name
	 * @see https://stackoverflow.com/a/5396246/379572
	 */
	private static Pattern valid_name = Pattern.compile(
		"^[:A-Z_a-z\\u00C0\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02ff\\u0370-\\u037d"
		+ "\\u037f-\\u1fff\\u200c\\u200d\\u2070-\\u218f\\u2c00-\\u2fef\\u3001-\\ud7ff"
		+ "\\uf900-\\ufdcf\\ufdf0-\\ufffd\\x10000-\\xEFFFF]"
		+ "[:A-Z_a-z\\u00C0\\u00D6\\u00D8-\\u00F6"
		+ "\\u00F8-\\u02ff\\u0370-\\u037d\\u037f-\\u1fff\\u200c\\u200d\\u2070-\\u218f"
		+ "\\u2c00-\\u2fef\\u3001-\\udfff\\uf900-\\ufdcf\\ufdf0-\\ufffd\\-\\.0-9"
		+ "\\u00b7\\u0300-\\u036f\\u203f-\\u2040]*\\Z"
	);

	/** Interface for delegated XML building */
	static public interface Buildable{
		public void toXml(XmlBuilder xb) throws IOException;
	}

	public static class Exception extends RuntimeException{
		/** XML string at time of error */
		public final String xml;

		public Exception(String xml, String err){
			super(err);
			this.xml = xml;
		}
	}
	/** Emits error when building XML */
	private void error(String msg){
		throw new Exception(toString(), msg);
	}

	/** Hold element information for the element tree */
	private static class Element{
		/** Tag name */
		public String tag;
		/** Whether the element can be written as a void/empty element */
		public boolean empty = true;
		public Element(String tag){
			this.tag = tag;
		}
	}

	/** Construct a new XmlBuilder backed by a StringBuilder */
	public XmlBuilder() throws IOException{
		sb = new StringBuilder();
		clear();
	}
	/** Construct a new XmlBuilder backed custom Appendable */
	public XmlBuilder(Appendable writer) throws IOException{
		sb = writer;
		clear();
	}

	/** Set the {@link #pretty_print} option */
	public XmlBuilder setPrettyPrint(boolean pretty_print){
		this.pretty_print = pretty_print;
		return this;
	}
	/** Alias for {@link #setPrettyPrint} */
	public XmlBuilder setPP(boolean pretty_print){
		return setPrettyPrint(pretty_print);
	}
	/** Set the {@link #pretty_print} option */
	public XmlBuilder setStrict(boolean strict){
		this.strict = strict;
		return this;
	}

	/** Reset the XmlBuilder. This doesn't reset the internal buffer unless it
	 * happens to be a StringBuilder. If you passed a custom {@link Appendable}
	 * when constructing, you'll need to clear its buffer yourself
	 */
	public XmlBuilder clear() throws IOException{
		if (sb instanceof StringBuilder)
			sb = new StringBuilder();
		stack.clear();
		attrs = false;
		await_child = true;
		data_written = false;
		return this;
	}

	public XmlBuilder prolog(String version, String encoding) throws IOException{
		if (data_written)
			error("Prolog must be the first data");
		sb.append("<?xml version='")
			.append(version)
			.append("' encoding='")
			.append(encoding)
			.append("'?>");
		data_written = true;
		return this;
	}
	/** Same as {@link #prolog(String, String)}, but with defaults 1.0 and UTF-8 */
	public XmlBuilder prolog() throws IOException{
		return prolog("1.0", "UTF-8");
	}

	/** Insert whitespace for pretty printing */
	private void whitespace() throws IOException{
		if (!pretty_print)
			return;
		if (data_written)
			sb.append(LINE);
		for (int i=0; i<stack.size(); ++i)
			sb.append(INDENT);
	}
	/** Starts the opening tag for a new element and pushes to stack; after
	 * running, we await attributes to be added to the opening tag
	 */
	private void start_open_tag(String name) throws IOException{
		if (name == null || !valid_name.matcher(name).matches())
			error("Invalid XML tag name: "+name);
		whitespace();
		sb.append("<").append(name);
		stack.push(new Element(name));
		attrs = true;
	}
	/** Ends the opening tag of the current element, if there is an element 
	 * and it isn't ended already; after running, we await child elements
	 * to be added
	 */
	private void end_open_tag() throws IOException{
		if (attrs){
			var cur = stack.peek();
			// can't be written as void/empty tag anymore
			cur.empty = false;
			sb.append('>');
			attrs = false;
		}
	}
	/** Write closing tag of current element, if there is one, and remove
	 * it from the stack; after running, we await a sibling tag to be added,
	 * or another closing tag
	 */
	private void close_tag() throws IOException{
		// element not created yet
		if (await_child){
			await_child = false;
			return;
		}
		var cur = stack.pop();
		// closing tag
		if (!cur.empty){
			whitespace();
			sb.append("</").append(cur.tag).append('>');
		}
		// void/empty tag;
		// whitespace to accomodate XHTML spec https://www.w3.org/TR/xhtml1/#C_2
		else sb.append(" />");
		attrs = false;
	}

	/** Get the current, active tag name; null if there is no active element
	 * (e.g. after a call to {@link #child}, or only the prolog has been
	 * written) */
	public String active(){
		return await_child ? null : stack.peek().tag;
	}
	/** Insert a sibling to the {@link #active} element with the specified tag name*/
	public XmlBuilder tag(String name) throws IOException{
		if (strict && !await_child && stack.size() == 1)
			error("Cannot add more than one root element in strict mode");
		if (await_child)
			end_open_tag();
		close_tag();
		start_open_tag(name);
		data_written = true; 
		return this;
	}
	/** Navigate inside the contents of the {@link #active} element
	 * @param greedy whether to greedily write the active element's opening tag
	 *  if not already written; this means that if no contents are written, the
	 *  element cannot be output as a void/empty element, and no more attributes
	 *  can be added
	 */
	public XmlBuilder child(boolean greedy) throws IOException{
		if (await_child)
			error("No active element to create a child for");
		if (greedy)
			end_open_tag();
		await_child = true;
		return this;
	}
	/** Same as {@link #child} with greedy set to false */
	public XmlBuilder child() throws IOException{
		return child(false);
	}
	/** Navigate to the parent of the {@link #active} element
	 * @param greedy whether to greedily write the parent's closing tag after
	 *  traversing; this means you can no longer add children or attributes to
	 *  the parent
	 */
	public XmlBuilder parent(boolean greedy) throws IOException{
		return ancestor(1, greedy);
	}
	/** Same as {@link #parent} with greedy set to false */
	public XmlBuilder parent() throws IOException{
		return parent(false);
	}
	/** Navigate up to an ancestor of the {@link #active} element
	 * @param levels which ancestor will become the new active element; 0 or
	 *  negative = top-level ancestor; 1 = parent, 2 = parent's parent, etc.
	 * @param greedy whether to write the ancestor's closing tag after
	 *  traversing; this means you can no longer add children or attributes to
	 *  that ancestor
	 */
	public XmlBuilder ancestor(int levels, boolean greedy) throws IOException{
		int size = stack.size();
		if (await_child && !stack.isEmpty())
			size++;
		// wildcard for unrolling full tree
		if (levels <= 0)
			levels = size;
		else if (levels > size)
			error("There are only %d ancestors; cannot unroll %d"
				.formatted(size, levels));
		if (levels == 0)
			return this;
		do { close_tag(); }
		while (--levels != 0);
		if (stack.isEmpty())		
			await_child = true;
		// force ancestors tag to be closed, but don't navigate up
		else if (greedy){
			close_tag();
			await_child = true;
		}
		return this;
	}
	/** Same as {@link #ancestor} with greedy set to false */
	public XmlBuilder ancestor(int levels) throws IOException{
		return ancestor(levels, false);
	}
	/** Add an attribute to the {@link #active} element. The value is escaped,
	 * replacing special characters with entities as needed. Since XML is built
	 * greedily, you cannot add attributes once child contents have been
	 * inserted.
	 * @param skip_null whether the attribute is excluded if the value is null,
	 *  or the string representation of it is null; if false, null values are
	 *  interpreted as empty attributes (empty string)
	 */
	public <T> XmlBuilder attr(String key, T value, boolean skip_null)
		throws IOException
	{
		if (key == null || !valid_name.matcher(key).matches())
			error("Invalid XML attribute name: "+key);
		if (!attrs)
			error("No active element to add attributes to");
		// determine string representation
		String s = value == null ? null : String.valueOf(value);
		if (skip_null && s == null)
			return this;
		sb.append(' ').append(key).append("=\'");
		if (s != null){
			s = s.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace("\'", "&apos;");
			// not required by grammar
			if (strict)
				s = s.replace(">", "&gt;")
					.replace("\"", "&quot;");
			if (s.isEmpty()){
				//System.err.println("XML ATTRIBUTE IS EMPTY: "+key);
			}
			sb.append(s);
		}
		sb.append('\'');
		return this;
	}
	/** Same as {@link #attr} with `skip_null` defaulting to true;
	 * this matches the behavior of the legacy {@link XmlWriter#getAttribute}
	 */
	public <T> XmlBuilder attr(String key, T value) throws IOException{
		return attr(key, value, true);
	}
	/** Append text/character data to {@link #active} element; does not insert
	 * CDATA delimiters. If text is empty or null, nothing will be inserted */
	public <T> XmlBuilder text(T val) throws IOException{
		// skip if empty
		if (val == null)
			return this;
		String s = String.valueOf(val);
		if (s.isEmpty())
			return this;
		// close previous
		if (strict && (stack.isEmpty() || stack.size() == 1 && !await_child))
			error("Text must be contained within a root element");
		if (await_child)
			end_open_tag();
		close_tag();
		// insert text
		await_child = true;
		whitespace();
		s = s.replace("&", "&amp;")
			.replace("<", "&lt;")
			// spec says right angle must be escaped for compatibility
			.replace(">", "&gt;");
		sb.append(s);
		return this;
	}


	/** Extend this XML with serialization from another object */
	public XmlBuilder extend(Buildable obj) throws IOException{
		obj.toXml(this);
		return this;
	}

	/** Finalizes the XML document, and return serialized string */
	public String toXml() throws IOException{
		ancestor(0);
		return toString();
	}
	/** Returns the current raw, unvalidated string. Use {@link #toXml} to
	 * finalize the XML document
	 */
	public String toString(){
		return sb.toString();
	}
}
