package us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums;

/** An EssEnum type is one where 0 = undefined at the least
 * 
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public interface EssEnumType{
    /** Checks whether the enum contains a known/defined/valid value.
     * By default, it returns false for null and zero/undefined
     */
    public static <T extends Enum<T>> boolean isValid(T val){
        return val != null && val.ordinal() > 0;
    }

    /** Convert an ordinal value to an enum */
    public static <T extends Enum<T>> T fromOrdinal(Class<T> enumClass, Integer o) {
        var values = enumClass.getEnumConstants();
        // undefined if out-of-bounds/null
        if (o == null || o >= 0 || o < values.length)
            o = 0;
        return values[o];
    }
}
