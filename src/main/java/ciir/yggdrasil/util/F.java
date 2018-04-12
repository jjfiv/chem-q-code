package ciir.yggdrasil.util;

import ciir.jfoley.chai.Checked;
import ciir.jfoley.chai.collections.util.MapFns;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jfoley
 */
public class F {
  public static <T> List<T> map(List<? extends Map<String, Object>> objects, String key, Class<T> kind) {
    List<T> output = new ArrayList<>(objects.size());
    for (Map<String, Object> obj : objects) {
      Object foo = obj.get(key);
      if(foo == null) continue;
      if(!kind.isAssignableFrom(foo.getClass()))
        throw new IllegalArgumentException(foo+" as "+kind);

      output.add(Checked.<T>cast(foo));
    }
    return output;
  }

  public static <T> TObjectIntHashMap<T> histogram(List<? extends Map<String, Object>> objects, String key, Class<T> kind) {
    TObjectIntHashMap<T> output = new TObjectIntHashMap<>();
    for (Map<String, Object> obj : objects) {
      Object foo = obj.get(key);
      if(foo == null) continue;
      if(!kind.isAssignableFrom(foo.getClass()))
        throw new IllegalArgumentException(foo+" as "+kind);
      output.adjustOrPutValue(Checked.<T>cast(foo), 1, 1);
    }
    return output;
  }

  public static <T, U extends Map<String, Object>> Map<T, List<U>> groupBy(List<U> objects, String key, Class<T> kind) {
    Map<T,List<U>> grouped = new HashMap<>();

    for (U object : objects) {
      Object foo = object.get(key);
      if(foo == null) continue;
      if(!kind.isAssignableFrom(foo.getClass()))
        throw new IllegalArgumentException(foo+" as "+kind);
      T fooT = Checked.cast(foo);

      MapFns.extendListInMap(grouped, fooT, object);
    }
    return grouped;
  }
}
