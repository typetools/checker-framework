import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Commitment {

  @NonNull String t;

  // :: error: (initialization.field.type)
  @NonNull @UnderInitialization String a;
  // :: error: (initialization.field.type)
  @Initialized String b;
  @UnknownInitialization @Nullable String c;

  // :: error: (initialization.constructor.return.type)
  public @UnderInitialization Commitment(int i) {
    a = "";
    t = "";
    b = "";
  }

  // :: error: (initialization.constructor.return.type)
  public @Initialized Commitment(int i, int j) {
    a = "";
    t = "";
    b = "";
  }

  // :: error: (initialization.constructor.return.type)
  // :: error: (nullness.on.constructor)
  public @Initialized @NonNull Commitment(boolean i) {
    a = "";
    t = "";
    b = "";
  }

  public
  // :: error: (nullness.on.constructor)
  @Nullable Commitment(char i) {
    a = "";
    t = "";
    b = "";
  }

  // :: error: (initialization.fields.uninitialized)
  public Commitment() {
    // :: error: (dereference.of.nullable)
    t.toLowerCase();

    t = "";

    @UnderInitialization @NonNull Commitment c = this;

    @UnknownInitialization @NonNull Commitment c1 = this;

    // :: error: (assignment)
    @Initialized @NonNull Commitment c2 = this;
  }

  // :: error: (initialization.fields.uninitialized)
  public Commitment(@UnknownInitialization Commitment arg) {
    t = "";

    // :: error: (argument)
    @UnderInitialization Commitment t = new Commitment(this, 1);

    // :: error: (assignment)
    @Initialized Commitment t1 = new Commitment(this);

    @UnderInitialization Commitment t2 = new Commitment(this);
  }

  // :: error: (initialization.fields.uninitialized)
  public Commitment(Commitment arg, int i) {}
}
