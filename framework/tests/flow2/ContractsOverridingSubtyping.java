import org.checkerframework.common.subtyping.qual.Unqualified;
import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.qual.RequiresQualifier;
import org.checkerframework.framework.testchecker.util.Odd;

public class ContractsOverridingSubtyping {
  static class Base {
    String f;
    @Odd String g;

    @RequiresQualifier(expression = "f", qualifier = Odd.class)
    void requiresOdd() {}

    @RequiresQualifier(expression = "f", qualifier = Unqualified.class)
    void requiresUnqual() {}

    @EnsuresQualifier(expression = "f", qualifier = Odd.class)
    void ensuresOdd() {
      f = g;
    }

    @EnsuresQualifier(expression = "f", qualifier = Unqualified.class)
    void ensuresUnqual() {}

    @EnsuresQualifierIf(expression = "f", result = true, qualifier = Odd.class)
    boolean ensuresIfOdd() {
      f = g;
      return true;
    }

    @EnsuresQualifierIf(expression = "f", result = true, qualifier = Unqualified.class)
    boolean ensuresIfUnqual() {
      return true;
    }
  }

  static class Derived extends Base {

    @Override
    @RequiresQualifier(expression = "f", qualifier = Unqualified.class)
    void requiresOdd() {}

    @Override
    @RequiresQualifier(expression = "f", qualifier = Odd.class)
    // :: error: (contracts.precondition.override.invalid)
    void requiresUnqual() {}

    @Override
    @EnsuresQualifier(expression = "f", qualifier = Unqualified.class)
    // :: error: (contracts.postcondition.override.invalid)
    void ensuresOdd() {
      f = g;
    }

    @Override
    @EnsuresQualifier(expression = "f", qualifier = Odd.class)
    void ensuresUnqual() {
      f = g;
    }

    @Override
    @EnsuresQualifierIf(expression = "f", result = true, qualifier = Unqualified.class)
    // :: error: (contracts.conditional.postcondition.true.override.invalid)
    boolean ensuresIfOdd() {
      f = g;
      return true;
    }

    @Override
    @EnsuresQualifierIf(expression = "f", result = true, qualifier = Odd.class)
    boolean ensuresIfUnqual() {
      f = g;
      return true;
    }
  }
}
