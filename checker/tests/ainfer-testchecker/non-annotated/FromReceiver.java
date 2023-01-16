// A test that an annotation on a receiver can be used when inferring
// the types of the method being invoked.

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

public class FromReceiver {

  public void source(@AinferSibling1 FromReceiver this) {
    this.sinkNoThis();
    this.sinkExplicitThis();

    sinkNoThis2();
    sinkExplicitThis2();
  }

  public void sinkNoThis() {
    // :: warning: (assignment)
    @AinferSibling1 FromReceiver f = this;
  }

  public void sinkExplicitThis(FromReceiver this) {
    // :: warning: (assignment)
    @AinferSibling1 FromReceiver f = this;
  }

  public void sinkNoThis2() {
    // :: warning: (assignment)
    @AinferSibling1 FromReceiver f = this;
  }

  public void sinkExplicitThis2(FromReceiver this) {
    // :: warning: (assignment)
    @AinferSibling1 FromReceiver f = this;
  }

  public static void source2(@AinferSibling1 FromReceiver f1) {
    f1.sinkNoThis3();
    f1.sinkExplicitThis3();
  }

  public void sinkNoThis3() {
    // :: warning: (assignment)
    @AinferSibling1 FromReceiver f = this;
  }

  public void sinkExplicitThis3(FromReceiver this) {
    // :: warning: (assignment)
    @AinferSibling1 FromReceiver f = this;
  }
}
