import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.HasQualifierParameter;

public class HasQualParamDefaults {
  @HasQualifierParameter(Tainted.class)
  public class Buffer {
    final List<@PolyTainted String> list = new ArrayList<>();
    @PolyTainted String someString = "";

    public Buffer() {}

    public @Untainted Buffer(@Tainted String s) {
      // :: error: (assignment)
      this.someString = s;
    }

    public Buffer(Buffer copy) {
      this.list.addAll(copy.list);
      this.someString = copy.someString;
    }

    public Buffer append(@PolyTainted String s) {
      list.add(s);
      someString = s;
      return this;
    }

    public @PolyTainted String prettyPrint() {
      String prettyString = list.get(1);
      for (@PolyTainted String s : list) {
        prettyString += s + " ~~ ";
      }
      return prettyString;
    }

    public @PolyTainted String unTaintedOnly(@Untainted Buffer this, @PolyTainted String s) {
      // :: error: (argument)
      list.add(s);
      // :: error: (assignment)
      someString = s;
      return s;
    }

    void initializeLocalTainted(@Tainted Buffer b) {
      Buffer local = b;
      @Tainted Buffer copy1 = local;
      // :: error: (assignment)
      @Untainted Buffer copy2 = local;
    }

    void initializeLocalUntainted(@Untainted Buffer b) {
      Buffer local = b;
      @Untainted Buffer copy1 = local;
      // :: error: (assignment)
      @Tainted Buffer copy2 = local;
    }

    void initializeLocalPolyTainted(@PolyTainted Buffer b) {
      Buffer local = b;
      @PolyTainted Buffer copy = local;
    }

    void noInitializer(@Untainted Buffer b) {
      Buffer local;
      // :: error: (assignment)
      local = b;
    }
  }

  class Use {
    void passingUses(@Untainted String untainted, @Untainted Buffer buffer) {
      buffer.list.add(untainted);
      buffer.someString = untainted;
      buffer.append(untainted);
    }

    void failingUses(@Tainted String tainted, @Untainted Buffer buffer) {
      // :: error: (argument)
      buffer.list.add(tainted);
      // :: error: (assignment)
      buffer.someString = tainted;
      // :: error: (argument)
      buffer.append(tainted);
    }

    void casts(@Untainted Object untainted, @Tainted Object tainted) {
      @Untainted Buffer b1 = (@Untainted Buffer) untainted; // ok
      // :: error: (invariant.cast.unsafe)
      @Untainted Buffer b2 = (@Untainted Buffer) tainted;

      // :: error: (invariant.cast.unsafe)
      @Tainted Buffer b3 = (@Tainted Buffer) untainted; // error
      // :: error: (invariant.cast.unsafe)
      @Tainted Buffer b4 = (@Tainted Buffer) tainted; // error

      @Untainted Buffer b5 = (Buffer) untainted; // ok
      // :: error: (invariant.cast.unsafe)
      @Tainted Buffer b6 = (Buffer) tainted;
    }

    void creation() {
      @Untainted Buffer b1 = new @Untainted Buffer();
      @Tainted Buffer b2 = new @Tainted Buffer();
      @PolyTainted Buffer b3 = new @PolyTainted Buffer();
    }
  }

  // For classes with @HasQualifierParameter, different defaulting rules are applied on that type
  // inside the class body and outside the class body, so local variables need to be tested
  // outside the class as well.
  class LocalVars {
    void initializeLocalTainted(@Tainted Buffer b) {
      Buffer local = b;
      @Tainted Buffer copy1 = local;
      // :: error: (assignment)
      @Untainted Buffer copy2 = local;
    }

    void initializeLocalUntainted(@Untainted Buffer b) {
      Buffer local = b;
      @Untainted Buffer copy1 = local;
      // :: error: (assignment)
      @Tainted Buffer copy2 = local;
    }

    void initializeLocalPolyTainted(@PolyTainted Buffer b) {
      Buffer local = b;
      @PolyTainted Buffer copy = local;
    }

    void noInitializer(@Untainted Buffer b) {
      Buffer local;
      // :: error: (assignment)
      local = b;
    }

    // These next two cases test circular dependencies. Calculating the type of a local variable
    // looks at the type of initializer, but if the type of the initializer depends on the type
    // of the variable, then infinite recursion could occur.

    void testTypeVariableInference() {
      GenericWithQualParam<String> set = new GenericWithQualParam<>();
    }

    void testVariableInOwnInitializer() {
      Buffer b = (b = null);
    }
  }

  @HasQualifierParameter(Tainted.class)
  static class GenericWithQualParam<T> {}
}
