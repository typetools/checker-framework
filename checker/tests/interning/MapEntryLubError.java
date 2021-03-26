import java.util.Map;

public class MapEntryLubError<V> {
  public boolean lubError(Map.Entry<Object, V> ent) {
    Object v;
    return (v = ent.getValue()) == null;
  }
}
