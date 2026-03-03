package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ExpressionTree;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/** Utility class for dealing with CaseTree. */
public class CaseUtils {
  // This is a copy of a method from checker-framework's {@code TreeUtils}.  Keep them in sync.
  /**
   * Returns the list of expressions from a case expression. In JDK 11 and earlier, this will be a
   * singleton list. In JDK 12 onwards, there can be multiple expressions per case.
   *
   * @param caseTree the case expression to get the expressions from
   * @return the list of expressions in the case
   */
  public static List<? extends ExpressionTree> caseTreeGetExpressions(CaseTree caseTree) {
    try {
      Method method = CaseTree.class.getDeclaredMethod("getExpressions");
      @SuppressWarnings("unchecked")
      List<? extends ExpressionTree> result =
          (List<? extends ExpressionTree>) method.invoke(caseTree);
      return result;
    } catch (NoSuchMethodException e) {
      // Must be on JDK 11 or earlier.
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      // May as well fall back to old method.
    }

    @SuppressWarnings("deprecation") // getExpression() is deprecated on JDK 12 and later
    ExpressionTree expression = caseTree.getExpression();
    if (expression == null) {
      return Collections.emptyList();
    } else {
      return Collections.singletonList(expression);
    }
  }
}
