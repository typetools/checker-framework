import com.sun.javadoc.Doc;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class JavadocJdkAnnotations {

  @Nullable Object f = null;

  @SuppressWarnings("removal")
  void testPureAnnotation(Doc d) {
    f = "non-null value";
    d.isIncluded();
    @NonNull Object x = f;
    d.tags();
    // :: error: (assignment.type.incompatible)
    @NonNull Object y = f;
  }
}
