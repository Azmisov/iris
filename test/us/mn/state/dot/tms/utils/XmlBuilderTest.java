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
	public void testSibling(){
		noThrow(xml -> {
			xml.tag("test");
			assertEquals("<?xml version='1.0' encoding='UTF-8'?><test />", xml.toXml());
		});
	}
	@Test
	public void testMultiSibling(){
		noThrow(xml -> {
			xml.setStrict(false);
			xml.tag("test").tag("foo").tag("bar");
			assertEquals("<?xml version='1.0' encoding='UTF-8'?><test /><foo /><bar />", xml.toXml());
		});
	}
	@Test
	public void testMultiSiblingPretty(){
		noThrow(xml -> {
			xml.setPrettyPrint(true).setStrict(false);
			xml.tag("test").tag("foo").tag("bar");
			assertEquals("""
				<?xml version='1.0' encoding='UTF-8'?>
				<test />
				<foo />
				<bar />""", xml.toXml());
		});
	}
	@Test
	public void testChild(){
		noThrow(xml -> {
			xml.tag("test").child().tag("foo").child().tag("bar");
			assertEquals("<?xml version='1.0' encoding='UTF-8'?><test><foo><bar /></foo></test>", xml.toXml());
		});
	}
	@Test
	public void testChildPretty(){
		noThrow(xml -> {
			xml.setPrettyPrint(true);
			xml.tag("test").child().tag("foo").child().tag("bar");
			assertEquals("""
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
			assertEquals("<?xml version='1.0' encoding='UTF-8'?><a><b><c /></b><x><y /></x></a>", xml.toXml());
		});
	}
	@Test
	public void testParentRevisit(){
		noThrow(xml -> {
			xml.tag("a").child()
					.tag("b").parent().child()
					.tag("c");
			assertEquals("<?xml version='1.0' encoding='UTF-8'?><a><b /><c /></a>", xml.toXml());
		});
	}
	@Test
	public void testChildRevisit(){
		noThrow(xml -> {
			xml.tag("a").child().parent().child()
					.tag("b");
			assertEquals("<?xml version='1.0' encoding='UTF-8'?><a><b /></a>", xml.toXml());
		});
	}
	@Test
	public void testChildGreedy(){
		noThrow(xml -> {
			xml.tag("a").child(true);
			assertEquals("<?xml version='1.0' encoding='UTF-8'?><a></a>", xml.toXml());
			xml.clear().tag("a").child();
			assertEquals("<?xml version='1.0' encoding='UTF-8'?><a />", xml.toXml());
			xml.clear().tag("a").child().parent().attr("x",5).child().parent().attr("y",6);
			assertEquals("<?xml version='1.0' encoding='UTF-8'?><a x='5' y='6' />", xml.toXml());
		});
	}
	@Test
	public void testAttrs(){
		noThrow(xml -> {
			xml.tag("x").attr("a",5).attr("b","'co&< bar>\"z");
			assertEquals("<?xml version='1.0' encoding='UTF-8'?><x a='5' b='&apos;co&amp;&lt; bar&gt;&quot;z' />", xml.toXml());
		});
	}
	@Test
	public void testAttrsChild(){
		noThrow(xml -> {
			xml.tag("x").attr("a",5).child()
				.tag("y");
			assertEquals("<?xml version='1.0' encoding='UTF-8'?><x a='5'><y /></x>", xml.toXml());
		});
	}
	@Test
	public void testText(){
		noThrow(xml -> {
			xml.tag("root").child()
				.text("text");
			assertEquals("<?xml version='1.0' encoding='UTF-8'?><root>text</root>", xml.toXml());
		});
	}
	@Test
	public void testTextRevisit(){
		noThrow(xml -> {
			xml.tag("root").child().parent().attr("x",5).child(true)
				.text("text");
			assertEquals("<?xml version='1.0' encoding='UTF-8'?><root x='5'>text</root>", xml.toXml());
		});
	}
	@Test
	public void testTextPretty(){
		noThrow(xml -> {
			xml.setPrettyPrint(true);
			xml.tag("root").child()
				.text("a").text("b");
			assertEquals("""
				<?xml version='1.0' encoding='UTF-8'?>
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
			xml.tag("a").child()
				.tag("b").child().setPP(false)
					.text("no whitespace")
					.text("please!").parent(true).setPP(true)
				.tag("c");
			assertEquals("""
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
