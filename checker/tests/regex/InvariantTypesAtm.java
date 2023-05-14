import java.util.Map;
import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class InvariantTypesAtm {

  <K extends AnnotatedTypeMirror, V extends AnnotatedTypeMirror> V mapGetHelper(
      Map<K, V> mappings) {
    return null;
  }

  Map<? extends AnnotatedTypeMirror, ? extends AnnotatedTypeMirror> mappings;
  AnnotatedTypeMirror found = mapGetHelper(mappings);
}
