import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class Commitment2 {

  // :: error: (assignment)
  Commitment2 g = create();

  Commitment2 h;

  @NotOnlyInitialized Commitment2 c;

  @NotOnlyInitialized Commitment2 f;

  public void test(@UnderInitialization Commitment2 c) {
    // :: error: (initialization.field.write.initialized)
    f = c;
  }

  public static @UnknownInitialization Commitment2 create() {
    return new Commitment2();
  }

  // :: error: (initialization.fields.uninitialized)
  public Commitment2() {}

  // :: error: (initialization.fields.uninitialized)
  public Commitment2(@UnderInitialization Commitment2 likeAnEagle) {
    // :: error: (assignment)
    h = likeAnEagle;

    c = likeAnEagle;
  }
}
