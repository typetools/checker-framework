import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

public abstract class NonStandardSpacing {

  public void m1() {
    int a = 1 + 2 + 3;
    int b = 1 + /* comment */ 2 + 3;
    int c = 1 + 2 + 3;
    int d =
        1 + // comment
            2 + 3;
    int e = 1 + /* comment
            end */ 2 + 3;
  }

  public void m2() // comment()
      {}

  public void m3() throws @AnnoField(1) Exception {}

  // ()
  public abstract void m4();

  // ()
  public abstract void // comment
      m5();

  // ()
  public abstract void /* comment
        end */ m6();

  // ()
  public abstract void m7();
}

@Target(ElementType.TYPE_USE)
@interface AnnoField {
  int value();
}
