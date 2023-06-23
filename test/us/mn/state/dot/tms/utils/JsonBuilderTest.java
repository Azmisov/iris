package us.mn.state.dot.tms.utils;

import static org.junit.Assert.*;
import org.junit.Test;

public class JsonBuilderTest{
	private interface JsonTest{
		public void run();
	}

	public void noThrow(JsonTest lambda){
		try{
			lambda.run();
		} catch(JsonBuilder.Exception e){
			System.err.println(e);
			assertTrue("error shouldn't have been thrown", false);
		}
	}
	public void yesThrow(JsonTest lambda){
		try{
			lambda.run();
		} catch(JsonBuilder.Exception e){
			return;
		}
		assertTrue("error should have been thrown", false);
	}
	public void ensureComplete(JsonBuilder jb){
		// everything should fail
		Object null_obj = null;
		String null_str = null;
		yesThrow(() -> jb.value(null_obj));
		yesThrow(() -> jb.value(null_str));
		yesThrow(() -> jb.value("test"));
		yesThrow(() -> jb.string("test"));
		yesThrow(() -> jb.value(5.6d));
		yesThrow(() -> jb.value(5.6f));
		yesThrow(() -> jb.value(5));
		yesThrow(() -> jb.value(5123123123l));
		yesThrow(() -> jb.value(true));
		yesThrow(() -> jb.value(false));		
		yesThrow(() -> jb.beginObject());
		yesThrow(() -> jb.endObject());
		yesThrow(() -> jb.beginList());
		yesThrow(() -> jb.endList());
		yesThrow(() -> jb.beginString());
		yesThrow(() -> jb.endString());
	}

	@Test
	public void testSingleNull(){
		noThrow(() -> {
			var jb = new JsonBuilder();
			Object v = null;
			jb.value(v);
			assertEquals("null", jb.toJson());
			ensureComplete(jb);
		});
	}
	@Test
	public void testSingleString(){
		noThrow(() -> {
			var jb = new JsonBuilder();
			jb.value("test");
			assertEquals("\"test\"", jb.toJson());
			ensureComplete(jb);
		});
	}
	@Test
	public void testSingleStringNull(){
		noThrow(() -> {
			var jb = new JsonBuilder();
			String v = null;
			jb.value(v);
			assertEquals("\"\"", jb.toJson());
			ensureComplete(jb);
		});
	}
	@Test
	public void testSingleStringBoundsEmpty(){
		noThrow(() -> {
			var jb = new JsonBuilder();
			jb.beginString().endString();
			assertEquals("\"\"", jb.toJson());
			ensureComplete(jb);
		});
	}
	@Test
	public void testSingleStringBoundsFull(){
		noThrow(() -> {
			var jb = new JsonBuilder();
			jb.beginString().append("test").append("blah \"yeah").append(123).endString();
			assertEquals("\"testblah \\\"yeah123\"", jb.toJson());
			ensureComplete(jb);
		});
	}
	@Test
	public void testSingleBool(){
		noThrow(() -> {
			var jb = new JsonBuilder();
			jb.value(true);
			assertEquals("true", jb.toJson());
			ensureComplete(jb);
		});
	}
	@Test
	public void testSingleDouble(){
		noThrow(() -> {
			var jb = new JsonBuilder();
			jb.value(5.6d);
			assertEquals("5.6", jb.toJson());
			ensureComplete(jb);
		});
	}
	@Test
	public void testSingleFloat(){
		noThrow(() -> {
			var jb = new JsonBuilder();
			jb.value(5.6f);
			assertEquals("5.6", jb.toJson());
			ensureComplete(jb);
		});
	}
	@Test
	public void testDoubleInt(){
		// want to be able to use doubles for ints when serializing
		noThrow(() -> {
			var jb = new JsonBuilder();
			jb.value(5.0);
			assertEquals("5", jb.toJson());
			ensureComplete(jb);
		});
	}
	@Test
	public void testSingleInt(){
		noThrow(() -> {
			var jb = new JsonBuilder();
			jb.value(5);
			assertEquals("5", jb.toJson());
			ensureComplete(jb);
		});
	}
	@Test
	public void testSingleLong(){
		noThrow(() -> {
			var jb = new JsonBuilder();
			jb.value(51293123123l);
			assertEquals("51293123123", jb.toJson());
			ensureComplete(jb);
		});
	}
	@Test
	public void testSingleObject(){
		noThrow(() -> {
			var jb = new JsonBuilder();
			jb.beginObject().endObject();
			assertEquals("{}", jb.toJson());
			ensureComplete(jb);
		});
	}
	@Test
	public void testSingleList(){
		noThrow(() -> {
			var jb = new JsonBuilder();
			jb.beginList().endList();
			assertEquals("[]", jb.toJson());
			ensureComplete(jb);
		});
	}
	@Test
	public void testListValues(){
		noThrow(() -> {
			Object o = null;
			String s = null;
			var jb = new JsonBuilder();
			jb
				.beginList()
					.value(o)
					.value(s)
					.value("a\"b")
					.value(5)
					.value(3.5f)
					.value(3.5d)
					.value(true)
					.value(false)
					.value(Double.POSITIVE_INFINITY)
					.value(Double.NEGATIVE_INFINITY)
					.value(Double.NaN)
					.value(Float.POSITIVE_INFINITY)
					.value(Float.NEGATIVE_INFINITY)
					.value(Float.NaN)
					.values(new String[]{"x","y\"z", null})
					.values(new Double[]{1.2,Double.NaN,3.4})
					.values(new Float[]{Float.NEGATIVE_INFINITY,Float.NaN,3.4f})
					.values(new Boolean[]{})
					.values(new Boolean[]{true, false})
				.endList();
			assertEquals(
				"["+
					"null,\"\",\"a\\\"b\",5,3.5,3.5,true,false,"+
					"null,null,null,null,null,null,"+
					"\"x\",\"y\\\"z\",\"\","+
					"1.2,null,3.4,"+
					"null,null,3.4,"+
					"true,false"+
				"]",
				jb.toJson()
			);
			ensureComplete(jb);
		});
	}
	@Test
	public void testObjectValues(){
		noThrow(() -> {
			Object o = null;
			String s = null;
			var jb = new JsonBuilder();
			jb
				.beginObject()
					.string(5).value(5)
					.key(6).value(5)
					.pair(5,o)
					.pair("test","xyz")
					.pair(5,false)
					.pair(5,s)
					.key("x").beginList().endList()
				.endObject();
			assertEquals(
				"{"+
					"\"5\":5,"+
					"\"6\":5,"+
					"\"5\":null,"+
					"\"test\":\"xyz\","+
					"\"5\":false,"+
					"\"5\":\"\","+
					"\"x\":[]"+
				"}",
				jb.toJson()
			);
			ensureComplete(jb);
		});
	}
	@Test
	public void testListBulk(){
		noThrow(() -> {
			var jb = new JsonBuilder();
			jb
				.beginList()
					.list()
					.object()
					.list(new Float[]{1.2f,3.2f,Float.NEGATIVE_INFINITY})
					.list(new Double[]{1.2,3.2,Double.NaN})
					.list(new Boolean[]{true,false})
					.list(new String[]{"x",null})
					.list(new String[]{})
				.endList();
			assertEquals(
				"[[],{},[1.2,3.2,null],[1.2,3.2,null],"+
				"[true,false],[\"x\",\"\"],[]]",
				jb.toJson()
			);
			ensureComplete(jb);
		});
	}
	@Test
	public void testEnd(){
		noThrow(() -> {
			assertEquals("null", new JsonBuilder().end().toJson());
			assertEquals("{}", new JsonBuilder().beginObject().end().toJson());
			assertEquals("[]", new JsonBuilder().beginList().end().toJson());
			assertEquals("\"\"", new JsonBuilder().beginString().end().toJson());
			assertEquals("{\"x\":null}", new JsonBuilder().beginObject().key("x").end().toJson());
			assertEquals("{\"\":null}", new JsonBuilder().beginObject().beginString().end().toJson());
			assertEquals("{\"\":[{}]}", 
				new JsonBuilder().beginObject().key("").beginList().beginObject().end().toJson());
			assertEquals("5", new JsonBuilder().value(5).end().end().toJson());
			assertEquals("null", new JsonBuilder().value(5).end().end().clear().end().toJson());
		});
	}
	@Test
	public void testBadStates(){
		yesThrow(() -> {
			(new JsonBuilder()).beginObject().beginObject();
		});
		yesThrow(() -> {
			(new JsonBuilder()).endObject();
		});
		yesThrow(() -> {
			(new JsonBuilder()).endList();
		});
		yesThrow(() -> {
			(new JsonBuilder()).endString();
		});
		yesThrow(() -> {
			(new JsonBuilder()).beginList().toJson();
		});
		yesThrow(() -> {
			(new JsonBuilder()).beginList().beginList().endList().toJson();
		});
		yesThrow(() -> {
			(new JsonBuilder()).beginObject().toJson();
		});
		yesThrow(() -> {
			(new JsonBuilder()).beginObject().value(5.6);
		});
		yesThrow(() -> {
			(new JsonBuilder()).beginObject().value(true);
		});
		yesThrow(() -> {
			(new JsonBuilder()).beginString().value(5.0);
		});
		yesThrow(() -> {
			(new JsonBuilder()).beginString().beginString();
		});
		yesThrow(() -> {
			(new JsonBuilder()).beginString().beginList();
		});
		yesThrow(() -> {
			(new JsonBuilder()).beginString().string("test");
		});
	}
}
