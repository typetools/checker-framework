import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.common.value.qual.MinLen;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.PreconditionAnnotation;
import org.checkerframework.framework.qual.QualifierArgument;

public class CustomContractWithArgs {
  // Postcondition for MinLen
  @PostconditionAnnotation(qualifier = MinLen.class)
  @interface EnsuresMinLen {
    public String[] value();

    @QualifierArgument("value")
    public int targetValue();
  }

  // Conditional postcondition for LTLengthOf
  @ConditionalPostconditionAnnotation(qualifier = LTLengthOf.class)
  @interface EnsuresLTLIf {
    public boolean result();

    public String[] expression();

    @JavaExpression
    @QualifierArgument("value")
    public String[] targetValue();

    @JavaExpression
    @QualifierArgument("offset")
    public String[] targetOffset();
  }

  // Precondition for LTLengthOf
  @PreconditionAnnotation(qualifier = LTLengthOf.class)
  @interface RequiresLTL {
    public String[] value();

    @JavaExpression
    @QualifierArgument("value")
    public String[] targetValue();

    @JavaExpression
    @QualifierArgument("offset")
    public String[] targetOffset();
  }

  class Base {
    @EnsuresMinLen(value = "#1", targetValue = 10)
    void minLenContract(int[] a) {
      if (a.length < 10) throw new RuntimeException();
    }

    @EnsuresMinLen(value = "#1", targetValue = 10)
    // :: error: (contracts.postcondition)
    void minLenWrong(int[] a) {
      if (a.length < 9) throw new RuntimeException();
    }

    void minLenUse(int[] b) {
      minLenContract(b);
      int @MinLen(10) [] c = b;
    }

    public int b, y;

    @EnsuresLTLIf(
        expression = "b",
        targetValue = {"#1", "#1"},
        targetOffset = {"#2 + 1", "10"},
        result = true)
    boolean ltlPost(int[] a, int c) {
      if (b < a.length - c - 1 && b < a.length - 10) {
        return true;
      } else {
        return false;
      }
    }

    @EnsuresLTLIf(expression = "b", targetValue = "#1", targetOffset = "#3", result = true)
    // :: error: (flowexpr.parse.error)
    boolean ltlPostInvalid(int[] a, int c) {
      return false;
    }

    @RequiresLTL(
        value = "b",
        targetValue = {"#1", "#1"},
        targetOffset = {"#2 + 1", "-10"})
    void ltlPre(int[] a, int c) {
      @LTLengthOf(value = "a", offset = "c+1") int i = b;
    }

    void ltlUse(int[] a, int c) {
      if (ltlPost(a, c)) {
        @LTLengthOf(value = "a", offset = "c+1") int i = b;

        ltlPre(a, c);
      }
      // :: error: (assignment)
      @LTLengthOf(value = "a", offset = "c+1") int j = b;
    }
  }

  class Derived extends Base {
    public int x;

    @Override
    @EnsuresLTLIf(
        expression = "b ",
        targetValue = {"#1", "#1"},
        targetOffset = {"#2 + 1", "11"},
        result = true)
    boolean ltlPost(int[] a, int d) {
      return false;
    }

    @Override
    @RequiresLTL(
        value = "b ",
        targetValue = {"#1", "#1"},
        targetOffset = {"#2 + 1", "-11"})
    void ltlPre(int[] a, int d) {
      @LTLengthOf(
          value = {"a", "a"},
          offset = {"d+1", "-10"})
      // :: error: (assignment)
      int i = b;
    }
  }

  class DerivedInvalid extends Base {
    public int x;

    @Override
    @EnsuresLTLIf(
        expression = "b ",
        targetValue = {"#1", "#1"},
        targetOffset = {"#2 + 1", "9"},
        result = true)
    // :: error: (contracts.conditional.postcondition.true.override)
    boolean ltlPost(int[] a, int c) {
      // :: error: (contracts.conditional.postcondition)
      return true;
    }

    @Override
    @RequiresLTL(
        value = "b ",
        targetValue = {"#1", "#1"},
        targetOffset = {"#2 + 1", "-9"})
    // :: error: (contracts.precondition.override)
    void ltlPre(int[] a, int d) {
      @LTLengthOf(
          value = {"a", "a"},
          offset = {"d+1", "-10"})
      int i = b;
    }
  }
}
