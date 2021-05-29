import org.checkerframework.common.returnsreceiver.qual.*;

// Test basic subtyping relationships for the Returns Receiver Checker.
public class SimpleTest {

  @This SimpleTest retNull() {
    // :: error: return
    return null;
  }

  @This SimpleTest retThis() {
    return this;
  }

  @This SimpleTest retThisWrapper(@UnknownThis SimpleTest other, boolean flag) {
    if (flag) {
      // :: error: return
      return other.retThis();
    } else {
      return this.retThis();
    }
  }

  @This SimpleTest retLocalThis() {
    SimpleTest x = this;
    return x;
  }

  @This SimpleTest retNewLocal() {
    SimpleTest x = new SimpleTest();
    // :: error: return
    return x;
  }

  // :: error: this.location
  @This SimpleTest thisOnParam(@This SimpleTest x) {
    return x;
  }

  void thisOnLocal() {
    // :: error: this.location
    // :: error: assignment
    @This SimpleTest x = new SimpleTest();

    // :: error: this.location
    // :: error: type.argument
    java.util.List<@This String> l = null;
  }

  // can write @This on receiver
  void thisOnReceiver(@This SimpleTest this) {}

  // :: error: this.location :: error: invalid.polymorphic.qualifier.use
  @This Object f;

  interface I {

    Object foo();

    SimpleTest.@This I setBar();
  }

  // :: error: this.location
  static @This Object thisOnStatic() {
    // :: error: return
    return new Object();
  }
}
