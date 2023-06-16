package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.tms.server.comm.ntcip.mib1204.enums.EssEnumType;
import java.lang.reflect.ParameterizedType;
import us.mn.state.dot.tms.utils.Json;

/** Converts {@link MIB1204} raw types to a generic enum type. The enum
 * should implement {@link EssEnumType}
 * 
 * @author Isaac Nygaard
 * @copyright 2023 Iteris, Inc
 * @license GPL-2.0
 */
public class EssEnum<T extends Enum<T> & EssEnumType> extends EssConverter<T>{
	/** The enum class this converter is for */
	public final Class<T> enumClass;
	
	public EssEnum(String k, MIB1204 n, int r){
		super(k, n, r);
		enumClass = reflect();
	}
	public EssEnum(String k, MIB1204 n){
		super(k, n);
		enumClass = reflect();
	}
	/** Reflection on the generic enumeration type */
	@SuppressWarnings("unchecked")
	private Class<T> reflect(){
		// https://stackoverflow.com/questions/3437897;
		// only works here because we have superclass EssConverter<T>
		var type = (ParameterizedType) getClass().getGenericSuperclass();
		return (Class<T>) type.getActualTypeArguments()[0];
	}

	@Override
	public T convert(){
		int i = node.getInteger();
		// https://stackoverflow.com/questions/10121988
		T[] values = enumClass.getEnumConstants();
		// extends Enum enforced, so guranteed non-null values
		if (i >= 0 && i < values.length){
			T e = values[i];
			if (e.isValid())
				return e;
		}
		else System.err.print("%s out-of-range: %d".formatted(mib_attr, i));
		return null;
	}
	@Override
	public Integer toInteger(){
		return get(e -> e.ordinal());
	}
	@Override
	public Double toDouble(){
		return get(e -> Double.valueOf(e.ordinal()));
	}
	@Override
	public String toString(){
		String out = get(e -> e.toString());
		return out == null ? "" : out;
	}
	@Override
	public String toJson(){
		String out = get(e -> Json.str(json_key, e.toString()));
		return out == null ? "" : out;
	}
}
