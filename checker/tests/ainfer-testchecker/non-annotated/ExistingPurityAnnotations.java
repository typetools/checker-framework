// This test checks that purity annotations are emitted, as expected,
// when an astub/ajava/jaif file with existing purity annotations is
// supplied.

import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;
import org.checkerframework.framework.qual.EnsuresQualifierIf;

public class ExistingPurityAnnotations {

  Object obj;

  public Object pureMethod(Object object) {
    return null;
  }

  @SuppressWarnings("ainfertest")
  @EnsuresQualifierIf(expression = "#1", result = true, qualifier = Sibling1.class)
  public boolean checkSibling1(Object obj1) {
    return true;
  }

  public @Sibling1 Object usePureMethod() {

    if (checkSibling1(obj)) {
      // If pureMethod doesn't have (and can't infer) an @Pure annotation, this call should
      // unrefine the type of obj, and an error will be issued when
      // we try to return obj on the next line.
      pureMethod(obj);
      return obj;
    }
    return null;
  }
}
