import java.util.Map;

class MyAnnotatedTypeMirror {
  void addAnnotations() {}
}

class MyAnnotatedTypeVariable extends MyAnnotatedTypeMirror {}

public class CastTypeVariable {
  public static <K extends MyAnnotatedTypeMirror, V extends MyAnnotatedTypeMirror> V mapGetHelper(
      Map<K, V> mappings, MyAnnotatedTypeVariable key) {
    V possValue = (V) mappings.get(key);
    // :: error: (dereference.of.nullable)
    possValue.addAnnotations();
    return possValue;
  }
}
