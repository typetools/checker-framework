import org.checkerframework.checker.interning.qual.Interned;

public class ArrayInitializers {
  public static final String STATIC_FIELD = "m";
  public static final @Interned String OTHER_FIELD = "n";

  public static final @Interned String[] STATIC_ARRAY = {STATIC_FIELD, OTHER_FIELD};
}
