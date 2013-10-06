package m;

import java.util.LinkedHashMap;
import java.util.Map;



public class T {

	/**
	 * @param keyValues [key1,value1,key2,value2,key3,value3]
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <K,V> Map<K,V> dict(Object... args) {
		Map<K,V> result = (Map<K, V>) new LinkedHashMap<K,V>();
		for (int i = 0; i < args.length; i+=2) {
			result.put((K)args[i], (V)args[i+1]);
		}
		return result;
	}

}
