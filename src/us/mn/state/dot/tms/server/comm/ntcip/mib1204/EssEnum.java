package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.EssEnumType;
import us.mn.state.dot.tms.utils.JsonBuilder;

/** Converts {@link MIB1204} raw types to a generic enum type. The enum
 * should implement {@link EssEnumType}. A value that doesn't pass
 * {@link EssEnumType#isValid} will be set to null.
 * 
 * @author Isaac Nygaard
 * @copyright 2023 Iteris, Inc
 * @license GPL-2.0
 */
public class EssEnum<T extends Enum<T> & EssEnumType> extends EssInteger<T>{
	/** The enum class this converter is for */
	public final Class<T> enumClass;
	public final T[] enumValues;
	
	public EssEnum(Class<T> clazz, String k, MIB1204 n, int r){
		super(k, n, r);
		enumClass = clazz;
		enumValues = reflectValues();
		setRange(0, enumValues.length);
	}
	public EssEnum(Class<T> clazz, String k, MIB1204 n){
		super(k, n);
		enumClass = clazz;
		enumValues = reflectValues();
		setRange(0, enumValues.length);
	}
	/** Reflection on the generic enumeration type */
	private T[] reflectValues(){
		// https://stackoverflow.com/questions/10121988
		// extends Enum enforced, so guranteed non-null values
		return enumClass.getEnumConstants();
	}

	/** Factory method for creating a typed variant of EssEnum. It is not
	 * possible to get the type parameter on a *generic* subclass, only a
	 * raw subclass. See https://stackoverflow.com/questions/3437897. The next
	 * best thing is a factory method, in my opinion, as you only need to
	 * specify the class twice.
	 */
	static <K extends Enum<K> & EssEnumType> EssEnum<K> make(
		Class<K> clazz, String k, MIB1204 n, int r
	){
		return new EssEnum<K>(clazz, k, n, r);
	}
	/** Same as {@link #make} */
	static <K extends Enum<K> & EssEnumType> EssEnum<K> make(
		Class<K> clazz, String k, MIB1204 n
	){
		return new EssEnum<K>(clazz, k, n);
	} 

	@Override
	public T convert(){
		return ranged(i -> {
			T e = enumValues[i];
			return e.isValid() ? e : null;
		});
	}
	@Override
	public Double toDouble(){
		return get(e -> Double.valueOf(e.ordinal()));
	}
	@Override
	public String toString(){
		return get(e -> e.toString());
	}
	@Override
	public void toJson(JsonBuilder jb){
		String val = get(e -> e.toString());
		if (val != null)
			jb.pairOrValue(json_key, val);
	}
}
