package us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums;

import us.mn.state.dot.tms.utils.SString;

/** An EssEnumType is used as an interface for enums in MIB1204. It is assumed
 * that 0 = undefined for default implementations, which is used also used as
 * the default value in {@link EssEnum}. This can be manually overriden
 * by calling {@link EssEnum#setMissing} if needed. However, currently
 * the {@link #fromOrdinal} defaults to 0 still, so bear that in mind
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

    /** Serializes to string, with special case if not {@link #isValid} */
    default String toStringValid(String invalid){
        return isValid() ? toString() : invalid;
    }
    /** Serializes to string, with empty string if not {@link #isValid} */
    default String toStringValid(){
        return toStringValid("");
    }
	/** Serialize to string using {@link #toString} and convert to upper snake case */
	default String toStringUpperSnake(){
		return SString.camelToUpperSnake(toString());
	}
    /** Serializes to string, with special case if not {@link #isValid} */
    static <T extends EssEnumType> String toStringValid(T val, String invalid){
        return val != null && val.isValid() ? val.toString() : invalid;
    }
    /** Serializes to string, with empty string if not {@link #isValid} */
    static <T extends EssEnumType> String toStringValid(T val){
        return toStringValid(val, "");
    }

    /** Checks whether value is non-null, then calls {@link #isValid} */
    static <T extends EssEnumType> boolean isValid(T val){
        return val != null && val.isValid();
    }

    /** Convert an ordinal value to an enum. Defaults to the zero enum value,
     * whatever that may be, if null or out of bounds */
    static <T extends Enum<T>> T fromOrdinal(Class<T> enumClass, Integer o) {
        var values = enumClass.getEnumConstants();
        // undefined if out-of-bounds/null
        if (o == null || o < 0 || o > values.length)
            o = 0;
        return values[o];
    }

    // Enum methods; since default interface methods don't know the type
    int ordinal();
}
