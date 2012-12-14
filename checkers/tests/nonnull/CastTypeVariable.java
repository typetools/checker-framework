
import java.util.Map;

class AnnotatedTypeMirror {
    void addAnnotations() {}
}

class AnnotatedTypeVariable extends AnnotatedTypeMirror {}

public class CastTypeVariable {
    public static <K extends AnnotatedTypeMirror, V extends AnnotatedTypeMirror>
        V mapGetHelper(Map<K, V> mappings, AnnotatedTypeVariable key) {
        V possValue = (V)mappings.get(key);
        //:: error: (dereference.of.nullable)
        possValue.addAnnotations();
        return possValue;
    }
}

