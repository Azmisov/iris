package us.mn.state.dot.tms.utils;

import static org.junit.Assert.*;
import java.io.IOException;
import org.junit.Test;

public class XmlBuilderTest {
	public interface XmlTest{
		public void run(XmlBuilder xml) throws IOException;
	}
	public void yesThrow(XmlTest lambda){
		try{
			var xml = new XmlBuilder();
			lambda.run(xml);
		} catch(XmlBuilder.Exception e){
			return;
		} catch(IOException e){}
		assertTrue("error should have been thrown", false);
	}
	public void noThrow(XmlTest lambda){
		try{
			var xml = new XmlBuilder();
			lambda.run(xml);
		} catch(JsonBuilder.Exception e){
			System.err.println(e);
			assertTrue("error shouldn't have been thrown", false);
		} catch(IOException e){
			System.err.println(e);
			assertTrue("error shouldn't have been thrown IOException", false);
		}
	}

	@Test
	public void testProlog(){
		noThrow(xml -> {
			xml.prolog("7.6", "UTF-16LE");
			assertEquals(null, "<?xml version='7.6' encoding='UTF-16LE'?>", xml.toXml());
		});
	}

	@Test
	public void testSibling(){
		noThrow(xml -> {
			xml.prolog().tag("test");
			assertEquals(null, "<?xml version='1.0' encoding='UTF-8'?><test />", xml.toXml());
		});
	}
	@Test
	public void testMultiSibling(){
		noThrow(xml -> {
			xml.setStrict(false);
			xml.tag("test").tag("foo").tag("bar");
			assertEquals(null, "<test /><foo /><bar />", xml.toXml());
		});
	}
	@Test
	public void testMultiSiblingPretty(){
		noThrow(xml -> {
			xml.setPrettyPrint(true).setStrict(false);
			xml.prolog().tag("test").tag("foo").tag("bar");
			assertEquals(null, """
				<?xml version='1.0' encoding='UTF-8'?>
				<test />
				<foo />
				<bar />""", xml.toXml());
		});
	}
	@Test
	public void testChild(){
		noThrow(xml -> {
			xml.prolog().tag("test").child().tag("foo").child().tag("bar");
			assertEquals(null, "<?xml version='1.0' encoding='UTF-8'?><test><foo><bar /></foo></test>", xml.toXml());
		});
	}
	@Test
	public void testChildPretty(){
		noThrow(xml -> {
			xml.setPrettyPrint(true);
			xml.prolog().tag("test").child().tag("foo").child().tag("bar");
			assertEquals(null, """
				<?xml version='1.0' encoding='UTF-8'?>
				<test>
					<foo>
						<bar />
					</foo>
				</test>""", xml.toXml());
		});
	}
	@Test
	public void testParent(){
		noThrow(xml -> {
			xml.tag("a").child()
					.tag("b").child()
						.tag("c").parent()
					.tag("x").child()
						.tag("y");
			assertEquals(null, "<a><b><c /></b><x><y /></x></a>", xml.toXml());
		});
	}
	@Test
	public void testParentRevisit(){
		noThrow(xml -> {
			xml.prolog().tag("a").child()
					.tag("b").parent().child()
					.tag("c");
			assertEquals(null, "<?xml version='1.0' encoding='UTF-8'?><a><b /><c /></a>", xml.toXml());
		});
	}
	@Test
	public void testChildRevisit(){
		noThrow(xml -> {
			xml.prolog().tag("a").child().parent().child()
					.tag("b");
			assertEquals(null, "<?xml version='1.0' encoding='UTF-8'?><a><b /></a>", xml.toXml());
		});
	}
	@Test
	public void testChildGreedy(){
		noThrow(xml -> {
			xml.prolog().tag("a").child(true);
			assertEquals(null, "<?xml version='1.0' encoding='UTF-8'?><a></a>", xml.toXml());
			xml.clear().prolog().tag("a").child();
			assertEquals(null, "<?xml version='1.0' encoding='UTF-8'?><a />", xml.toXml());
			xml.clear().prolog().tag("a").child().parent().attr("x",5).child().parent().attr("y",6);
			assertEquals(null, "<?xml version='1.0' encoding='UTF-8'?><a x='5' y='6' />", xml.toXml());
		});
	}
	@Test
	public void testAttrs(){
		noThrow(xml -> {
			xml.prolog().tag("x").attr("a",5).attr("b","'co&< bar>\"z");
			assertEquals(null, "<?xml version='1.0' encoding='UTF-8'?><x a='5' b='&apos;co&amp;&lt; bar&gt;&quot;z' />", xml.toXml());
		});
	}
	@Test
	public void testAttrsChild(){
		noThrow(xml -> {
			xml.prolog().tag("x").attr("a",5).child()
				.tag("y");
			assertEquals(null, "<?xml version='1.0' encoding='UTF-8'?><x a='5'><y /></x>", xml.toXml());
		});
	}
	@Test
	public void testText(){
		noThrow(xml -> {
			xml.tag("root").child()
				.text("text");
			assertEquals(null, "<root>text</root>", xml.toXml());
		});
	}
	@Test
	public void testTextRevisit(){
		noThrow(xml -> {
			xml.tag("root").child().parent().attr("x",5).child(true)
				.text("text");
			assertEquals(null, "<root x='5'>text</root>", xml.toXml());
		});
	}
	@Test
	public void testTextPretty(){
		noThrow(xml -> {
			xml.setPrettyPrint(true);
			xml.tag("root").child()
				.text("a").text("b");
			assertEquals(null, """
				<root>
					a
					b
				</root>""", xml.toXml());
		});
	}
	@Test
	public void testTextPrettyToggling(){
		noThrow(xml -> {
			xml.setPrettyPrint(true);
			xml.prolog().tag("a").child()
				.tag("b").child().setPP(false)
					.text("no whitespace")
					.text("please!").parent(true).setPP(true)
				.tag("c");
			assertEquals(null, """
				<?xml version='1.0' encoding='UTF-8'?>
				<a>
					<b>no whitespaceplease!</b>
					<c />
				</a>""", xml.toXml());
		});
	}


	@Test
	public void testErrors(){
		yesThrow(xml -> xml.tag("a").tag("b")); // multiple roots
		yesThrow(xml -> xml.prolog().prolog()); // multiple prologs
		yesThrow(xml -> xml.tag(null)); // bad name
		yesThrow(xml -> xml.tag("-x")); // bad name
		yesThrow(xml -> xml.child()); // child with no parent
		yesThrow(xml -> xml.tag("x").child().tag(null)); // bad name
		yesThrow(xml -> xml.parent()); // not a child
		yesThrow(xml -> xml.tag("a").child().tag("b").ancestor(3)); // too far
		yesThrow(xml -> xml.attr("a",5)); // no active
		yesThrow(xml -> xml.tag("a").attr("",5)); // bad name
		yesThrow(xml -> xml.tag("a").child(true).parent().attr("b",5)); // child is greedy
	}
}
