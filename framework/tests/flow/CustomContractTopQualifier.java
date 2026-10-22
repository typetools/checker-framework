import org.checkerframework.common.subtyping.qual.Unqualified;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.PreconditionAnnotation;
import org.checkerframework.framework.qual.RequiresQualifier;
import org.checkerframework.framework.testchecker.util.Odd;

// Tests the "contracts.toptype" warning for contract annotations whose qualifier is the top
// qualifier, including type-system-specific contract annotations that are defined by a contract
// meta-annotation.
public class CustomContractTopQualifier {

  @PreconditionAnnotation(qualifier = Unqualified.class)
  @interface RequiresUnqualified {
    String[] value();
  }

  @PostconditionAnnotation(qualifier = Unqualified.class)
  @interface EnsuresUnqualified {
    String[] value();
  }

  @ConditionalPostconditionAnnotation(qualifier = Unqualified.class)
  @interface EnsuresUnqualifiedIf {
    String[] expression();

    boolean result();
  }

  @PreconditionAnnotation(qualifier = Odd.class)
  @interface RequiresOdd {
    String[] value();
  }

  String f;

  // :: warning: [contracts.toptype]
  @RequiresUnqualified("f")
  void requiresUnqualified() {}

  // :: warning: [contracts.toptype]
  @EnsuresUnqualified("f")
  void ensuresUnqualified() {}

  // :: warning: [contracts.toptype]
  @EnsuresUnqualifiedIf(expression = "f", result = true)
  boolean ensuresUnqualifiedIf() {
    return true;
  }

  @RequiresOdd("f")
  void requiresOdd() {}

  @RequiresQualifier.List({
    @RequiresQualifier(expression = "f", qualifier = Odd.class),
    // :: warning: [contracts.toptype]
    @RequiresQualifier(expression = "f", qualifier = Unqualified.class)
  })
  void requiresList() {}
}
