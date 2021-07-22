import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.qual.EnsuresQualifierIf;

public class Initializer {

  public String a;
  public String b = "abc";

  // :: error: (assignment)
  public String c = null;

  public String d = ("");

  // :: error: (initialization.fields.uninitialized)
  public Initializer() {
    // :: error: (assignment)
    a = null;
    a = "";
    c = "";
  }

  // :: error: (initialization.fields.uninitialized)
  public Initializer(boolean foo) {}

  public Initializer(int foo) {
    a = "";
    c = "";
    f = "";
  }

  public Initializer(float foo) {
    setField();
    c = "";
    f = "";
  }

  public Initializer(double foo) {
    if (!setFieldMaybe()) {
      a = "";
    }
    c = "";
    f = "";
  }

  // :: error: (initialization.fields.uninitialized)
  public Initializer(double foo, boolean t) {
    if (!setFieldMaybe()) {
      // on this path, 'a' is not initialized
    }
    c = "";
  }

  @EnsuresQualifier(expression = "a", qualifier = NonNull.class)
  public void setField(@UnknownInitialization Initializer this) {
    a = "";
  }

  @EnsuresQualifierIf(result = true, expression = "a", qualifier = NonNull.class)
  public boolean setFieldMaybe(@UnknownInitialization Initializer this) {
    a = "";
    return true;
  }

  String f;

  void t1(@UnknownInitialization Initializer this) {
    // :: error: (dereference.of.nullable)
    this.f.toString();
  }

  String fieldF = "";
}

class SubInitializer extends Initializer {

  // :: error: (initialization.fields.uninitialized)
  String f;

  void subt1(@UnknownInitialization(Initializer.class) SubInitializer this) {
    fieldF.toString();
    super.f.toString();
    // :: error: (dereference.of.nullable)
    this.f.toString();
  }

  void subt2(@UnknownInitialization SubInitializer this) {
    // :: error: (dereference.of.nullable)
    fieldF.toString();
    // :: error: (dereference.of.nullable)
    super.f.toString();
    // :: error: (dereference.of.nullable)
    this.f.toString();
  }
}
