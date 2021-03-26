import org.checkerframework.common.returnsreceiver.qual.*;

// Test basic subtyping relationships for the Returns Receiver Checker.
public class OverrideTest {

  static class Super {

    @This Super retThis() {
      return this;
    }

    Super retWhatever() {
      return null;
    }
  }

  static class Sub extends Super {

    @Override
    // :: error: override.return.invalid
    Super retThis() {
      return null;
    }

    @Override
    // we do not support this case for now; would need to write explicit @This on receiver in
    // superclass
    // :: error: override.receiver.invalid
    @This Super retWhatever() {
      return this;
    }
  }

  static class Sub2 extends Super {

    @Override
    @This Sub2 retThis() {
      return this;
    }
  }
}
