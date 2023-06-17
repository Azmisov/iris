package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.tms.server.comm.snmp.ASN1Object;

/** Interface shared by all EssConverter generic types. We could instead have a
 * non-generic superclass instead, but it ends up being needless abstraction in
 * my opinion.
 * 
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public interface EssConvertible {
    /** Get the raw type prior to conversion */
    public ASN1Object getRaw();
    /** Check whether the converted type is ull */
    public boolean isNull();
    /** Transforms the converted type to Double; may not be supported */
    public Double toDouble() throws UnsupportedOperationException;
    /** Transforms the converted type to Integer; may not be supported */
    public Integer toInteger() throws UnsupportedOperationException;
    /** Transforms the converted type to String */
    public String toString();
    /** Transforms the converted type to Json string */
    public String toJson();
}
