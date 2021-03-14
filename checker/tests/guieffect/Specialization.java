import org.checkerframework.checker.guieffect.qual.*;

public class Specialization {

  @PolyUIType
  public static interface I {
    @PolyUIEffect
    public void m();
  }

  public static void reqSafe(@AlwaysSafe I i) {}

  @UIEffect
  public static void reqUI(@UI I i) {}

  @PolyUIType
  public static interface Doer {

    public void doStuff(@PolyUI Doer this, @PolyUI I i);
  }

  @AlwaysSafe public static class SafeDoer implements @AlwaysSafe Doer {
    // :: error: (override.param.invalid)
    public void doStuff(@AlwaysSafe SafeDoer this, @AlwaysSafe I i) {}
  }

  public void q(@AlwaysSafe Doer doer, @UI I i) {
    doer.doStuff(i);
  }

  public static void main(String[] args) {

    @AlwaysSafe Doer d =
        new @AlwaysSafe Doer() {
          @SafeEffect
          // :: error: (override.param.invalid)
          public void doStuff(@AlwaysSafe I i) {
            reqSafe(i);
          }
        };
    @UI I ui =
        new @UI I() {
          public void m() {
            reqUI(null);
          }
        };
    Specialization q = new Specialization();
    q.q(d, ui);
  }
}
