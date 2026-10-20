import java.lang.annotation.*;

@Target(ElementType.TYPE_USE)
@interface Nullable {}

@Target(ElementType.TYPE_USE)
@interface NonNull {}

public class NewInAnnotatedVariable {
  @SuppressWarnings({"deprecation", "removal"})
  Number b1 = new Integer(0);

  @NonNull Object b2 = new @Nullable Double(1);
  @NonNull Runnable b3 = new @NonNull Thread();
  ThreadLocal[] b4 = new InheritableThreadLocal[3];
}
