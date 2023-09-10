// This test checks the accumulation value for a field with a wildcard type.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

public class AccumulationValueFieldTest {

  @InheritableMustCall({"a"})
  class MCAB {
    void a() {}

    void b() {}
  }

  @InheritableMustCall({"a"})
  class FieldTest<T extends MCAB> {

    @Owning
    @MustCall({"a"}) T m = null;

    FieldTest(@Owning @MustCall({"a"}) T mcab) {
      m = mcab;
    }

    @RequiresCalledMethods(
        value = {"this.m"},
        methods = {"a"})
    @CreatesMustCallFor("this")
    void overwriteMCorrect(@Owning @MustCall({"a"}) T mcab) {
      this.m = mcab;
    }

    @EnsuresCalledMethods(
        value = {"this.m"},
        methods = {"a"})
    void a() {
      m.a();
    }
  }
}
