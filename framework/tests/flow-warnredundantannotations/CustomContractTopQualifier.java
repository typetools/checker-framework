import java.lang.annotation.Repeatable;
import org.checkerframework.common.subtyping.qual.Unqualified;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.PreconditionAnnotation;
import org.checkerframework.framework.qual.RequiresQualifier;
import org.checkerframework.framework.testchecker.util.Odd;

// Tests the "contracts.toptype" warning, which -AwarnRedundantAnnotations enables, for contract
// annotations whose qualifier is the top qualifier.  The tests cover framework contract
// annotations and type-system-specific contract annotations that are defined by a contract
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

  // Only the postcondition's qualifier is the top qualifier.
  @PreconditionAnnotation(qualifier = Odd.class)
  @PostconditionAnnotation(qualifier = Unqualified.class)
  @interface RequiresOddEnsuresUnqualified {
    String[] value();
  }

  @Repeatable(RequiresUnqualifiedRepeatable.List.class)
  @PreconditionAnnotation(qualifier = Unqualified.class)
  @interface RequiresUnqualifiedRepeatable {
    String[] value();

    @PreconditionAnnotation(qualifier = Unqualified.class)
    @interface List {
      RequiresUnqualifiedRepeatable[] value();
    }
  }

  String f;

  String g;

  // :: warning: [contracts.toptype]
  @RequiresQualifier(expression = "f", qualifier = Unqualified.class)
  void requiresQualifierUnqualified() {}

  // :: warning: [contracts.toptype]
  @EnsuresQualifier(expression = "f", qualifier = Unqualified.class)
  void ensuresQualifierUnqualified() {}

  // :: warning: [contracts.toptype]
  @EnsuresQualifierIf(expression = "f", result = true, qualifier = Unqualified.class)
  boolean ensuresQualifierIfUnqualified() {
    return true;
  }

  @RequiresQualifier(expression = "f", qualifier = Odd.class)
  void requiresQualifierOdd() {}

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

  // :: warning: [contracts.toptype]
  @RequiresOddEnsuresUnqualified("f")
  void requiresOddEnsuresUnqualified() {}

  @RequiresQualifier.List({
    @RequiresQualifier(expression = "f", qualifier = Odd.class),
    // :: warning: [contracts.toptype]
    @RequiresQualifier(expression = "f", qualifier = Unqualified.class)
  })
  void requiresList() {}

  // Implicitly repeated framework contract annotations.
  @RequiresQualifier(expression = "f", qualifier = Odd.class)
  // :: warning: [contracts.toptype]
  @RequiresQualifier(expression = "g", qualifier = Unqualified.class)
  void requiresRepeated() {}

  @RequiresUnqualifiedRepeatable.List({
    // :: warning: [contracts.toptype]
    @RequiresUnqualifiedRepeatable("f"),
    // :: warning: [contracts.toptype]
    @RequiresUnqualifiedRepeatable("g")
  })
  void requiresCustomList() {}

  // Implicitly repeated type-system-specific contract annotations.
  // :: warning: [contracts.toptype]
  @RequiresUnqualifiedRepeatable("f")
  // :: warning: [contracts.toptype]
  @RequiresUnqualifiedRepeatable("g")
  void requiresCustomRepeated() {}
}
