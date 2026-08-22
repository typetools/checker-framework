// An annotation that is written within a method body is not code that runs, so it is not a side
// effect.  Its arguments must not be treated as assignments: javac rewrites the argument of a
// single-element annotation such as `@Marker("x")` into `value = "x"`, an assignment whose
// left-hand side stands for an annotation element rather than for a variable.

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class AnnotationInBody {

  @Target({ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
  @interface Marker {
    String value();
  }

  int field;

  @SideEffectsOnly("this")
  void singleElementAnnotation() {
    @Marker("x")
    int local = field;
  }

  @SideEffectsOnly("this")
  void namedElementAnnotation() {
    @Marker(value = "x")
    int local = field;
  }

  @SideEffectsOnly("this")
  void annotationOnTypeUse() {
    @Marker("x")
    String local = "s";
  }
}
