import java.io.Serializable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AnnotatedSupertype {

  class NullableSupertype
      // :: error: (nullness.on.supertype)
      extends @Nullable Object
      // :: error: (nullness.on.supertype)
      implements @Nullable Serializable {}

  @NonNull class NonNullSupertype
      // :: error: (nullness.on.supertype)
      extends @NonNull Object
      // :: error: (nullness.on.supertype)
      implements @NonNull Serializable {}
}
