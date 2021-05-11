import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

@SuppressWarnings({"inconsistent.constructor.type", "super.invocation"}) // ignore these warnings
public class PolyReturn {
  @PolyTainted PolyReturn() {}

  @PolyTainted PolyReturn method() {
    return new PolyReturn();
  }

  void use() {
    @Untainted PolyReturn untainted = new PolyReturn();
    @Untainted PolyReturn untainted2 = new @Untainted PolyReturn();

    @Untainted PolyReturn untainted3 = method();

    @Tainted PolyReturn tainted = new PolyReturn();
    @Tainted PolyReturn tainted2 = new @Tainted PolyReturn();

    @Tainted PolyReturn tainted3 = method();
  }
}
