import checkers.nullness.quals.*;
import checkers.initialization.quals.*;

public class RawPartial {

  public RawPartial() {
    //:: error: (method.invocation.invalid)
    foo1();
    foo2();
    foo3();
    foo4();
  }

  public void foo1() {}

  public void foo2(@Raw(RawPartial.class) @UnkownInitialization(RawPartial.class) RawPartial this)  {}

  public void foo3(@Raw(RawPartial.class) @UnkownInitialization(RawPartial.class) RawPartial this) {}

  public void foo4(@Raw(RawPartial.class) @UnkownInitialization(RawPartial.class) RawPartial this) {}

  public static void main(String[] args) {
    new SubRP();
  }
}

class SubRP extends RawPartial {

  @NonNull String f;

  public SubRP() {
    f = "";
  }

  @Override
  //:: error: (override.receiver.invalid)
  public void foo2() {
    f.toLowerCase();
  }

  @Override
  public void foo3(@Raw(RawPartial.class) @UnkownInitialization(RawPartial.class) SubRP this) {
    //:: error: (dereference.of.nullable)
    f.toLowerCase();
  }

  @Override
  public void foo4(@Raw @UnkownInitialization SubRP this) {
    //:: error: (dereference.of.nullable)
    f.toLowerCase();
  }
}
