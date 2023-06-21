package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import java.lang.UnsupportedOperationException;
import us.mn.state.dot.tms.server.comm.snmp.ASN1OctetString;
import us.mn.state.dot.tms.utils.JsonBuilder;

/** Implementaiton of {@link EssConverter} for "display strings". If other
 * use cases become needed, this functionality can be pulled out into a
 * superclass for {@link ASN1OctetString}. Empty strings are treated as null
 * 
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @author Douglas Lau
 * @copyright 2009-2023 Minnesota Department of Transportation
 */
public class EssString extends EssConverter<String, ASN1OctetString>{
	public EssString(String json_key, MIB1204 mib_attr, int row){
		super(json_key, new ASN1OctetString(mib_attr.node, row));
	}

	@Override
	protected String convert() {
		StringBuilder sb = new StringBuilder();
		for (byte b: raw.getByteValue()) {
			// Only display non-NUL ASCII
			if (b > 0 && b < 128)
				sb.append((char) b);
			else break;
		}
		return sb.isEmpty() ? null : sb.toString();
	}
	/** String or empty string if null */
	@Override
	public String toString(){
		var val = get();
		return val == null ? "" : val;
	}
	/** Json key + string, or an empty string if null */
	@Override
	public void toJson(JsonBuilder jb) throws JsonBuilder.Exception{
		String val = get(e -> e.toString());
		if (val != null)
			jb.pairOrValue(json_key, val);
	}

	// numeric conversion not supported
	@Override
	public Integer toInteger(){
		throw new UnsupportedOperationException();
	}
	@Override
	public Double toDouble(){
		throw new UnsupportedOperationException();
	}
}
