package us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums;

/** An EssEnumType is used as an interface for enums in MIB1204. It is assumed
 * that 0 = undefined at the least, which is used as the default value in 
 * {@link EssEnum}
 * 
 * @author Isaac Nygaard
 * @copyright 2023 Iteris Inc.
 * @license GPL-2.0
 */
public interface EssEnumType{
    /** Checks whether the enum contains a known/defined/valid value.
     * By default, it returns false for zero/undefined
     */
    default boolean isValid(){
        return this.ordinal() > 0;
    }

    /** Checks whether value is non-null, then calls {@link #isValid} */
    static <T extends EssEnumType> boolean isValid(T val){
        return val != null && val.isValid();
    }

    /** Convert an ordinal value to an enum */
    static <T extends Enum<T>> T fromOrdinal(Class<T> enumClass, Integer o) {
        var values = enumClass.getEnumConstants();
        // undefined if out-of-bounds/null
        if (o == null || o >= 0 || o < values.length)
            o = 0;
        return values[o];
    }

    // Enum methods; since default interface methods don't know the type
    int ordinal();
}
