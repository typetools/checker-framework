import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class Issue2330<T extends @Tainted Object> {
  public @Untainted Issue2330(@PolyTainted int i) {}

  public @Untainted Issue2330() {}

  public static void f(@PolyTainted int i) {
    new @Untainted Issue2330<@PolyTainted Integer>(i);
    new @Untainted Issue2330<@PolyTainted Integer>();
  }
}
