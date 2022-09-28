// A test that an annotation on a receiver can be used when inferring
// the types of the method being invoked.

import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;

public class FromReceiver {

  public void source(@Sibling1 FromReceiver this) {
    this.sinkNoThis();
    this.sinkExplicitThis();
  }

  public void sinkNoThis() {
    // :: warning: assignment
    @Sibling1 FromReceiver f = this;
  }

  public void sinkExplicitThis(FromReceiver this) {
    // :: warning: assignment
    @Sibling1 FromReceiver f = this;
  }
}
