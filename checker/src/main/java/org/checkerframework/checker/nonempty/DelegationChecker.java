package org.checkerframework.checker.nonempty;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.nonempty.qual.Delegate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This class enforces checks for the {@link Delegate} annotation.
 *
 * <p>It is not a checker for a type system. It enforces the following syntactic checks:
 *
 * <ul>
 *   <li>A class may have up to exactly one field marked with the {@link Delegate} annotation.
 *   <li>An overridden method's implementation must be exactly a call to the delegate field.
 * </ul>
 */
public class DelegationChecker extends BaseTypeChecker {

  @Override
  protected BaseTypeVisitor<BaseAnnotatedTypeFactory> createSourceVisitor() {
    return new Visitor(this);
  }

  static class Visitor extends BaseTypeVisitor<BaseAnnotatedTypeFactory> {

    /** The maximum number of fields marked with {@link Delegate} permitted in a class. */
    private final int MAX_NUM_DELEGATE_FIELDS = 1;

    /** The field marked with {@link Delegate} for the current class. */
    private @Nullable VariableTree delegate;

    public Visitor(DelegationChecker checker) {
      super(checker);
    }

    @Override
    public void processClassTree(ClassTree tree) {
      delegate = null; // Unset the previous delegate whenever a new class is visited
      // TODO: what about inner classes?
      List<VariableTree> delegates = getDelegateFields(tree);
      if (delegates.size() > MAX_NUM_DELEGATE_FIELDS) {
        VariableTree latestDelegate = delegates.get(delegates.size() - 1);
        checker.reportError(latestDelegate, "multiple.delegate.annotations");
      } else if (delegates.size() == 1) {
        delegate = delegates.get(0);
      }
      super.processClassTree(tree);
    }

    @Override
    public Void visitMethod(MethodTree tree, Void p) {
      Void result = super.visitMethod(tree, p);
      if (delegate == null || !isMarkedWithOverride(tree)) {
        return result;
      }
      MethodInvocationTree delegatedMethodCall = getDelegatedCall(tree.getBody());
      if (delegatedMethodCall == null) {
        checker.reportWarning(tree, "invalid.delegate", tree.getName(), delegate.getName());
        return result;
      }
      Name enclosingMethodName = tree.getName();
      if (!isValidDelegateCall(enclosingMethodName, delegatedMethodCall)) {
        checker.reportWarning(tree, "invalid.delegate", tree.getName(), delegate.getName());
      }
      return result;
    }

    /**
     * Return true if the given method call is a valid delegate call for the enclosing method.
     *
     * <p>A delegate method call must fulfill the following properties: its receiver must be the
     * current field marked with {@link Delegate} in the class, and the name of the method call must
     * match that of the enclosing method.
     *
     * @param enclosingMethodName the name of the enclosing method
     * @param delegatedMethodCall the delegated method call
     * @return true if the given method call is a valid delegate call for the enclosing method
     */
    private boolean isValidDelegateCall(
        Name enclosingMethodName, MethodInvocationTree delegatedMethodCall) {
      assert delegate != null; // This method should only be invoked when delegate is non-null
      ExpressionTree methodSelectTree = delegatedMethodCall.getMethodSelect();
      MemberSelectTree fieldAccessTree = (MemberSelectTree) methodSelectTree;
      VariableElement delegatedField = TreeUtils.asFieldAccess(fieldAccessTree.getExpression());
      Name delegatedMethodName = TreeUtils.methodName(delegatedMethodCall);
      // TODO: is there a better way to check? Comparing names seems fragile.
      return enclosingMethodName.equals(delegatedMethodName)
          && delegatedField != null
          && delegatedField.getSimpleName().equals(delegate.getName());
    }

    /**
     * Returns the fields of a class marked with a {@link Delegate} annotation.
     *
     * @param tree a class
     * @return the fields of a class marked with a {@link Delegate} annotation
     */
    private List<VariableTree> getDelegateFields(ClassTree tree) {
      List<VariableTree> fields = TreeUtils.fieldsFromTree(tree);
      List<VariableTree> delegateFields = new ArrayList<>();
      for (VariableTree field : fields) {
        List<AnnotationMirror> annosOnField =
            TreeUtils.annotationsFromTypeAnnotationTrees(field.getModifiers().getAnnotations());
        if (annosOnField.stream()
            .anyMatch(anno -> atypeFactory.areSameByClass(anno, Delegate.class))) {
          delegateFields.add(field);
        }
      }
      return delegateFields;
    }

    /**
     * Return true if a method is marked with {@link Override}.
     *
     * @param tree a method declaration
     * @return true if the given method declaration is annotated with {@link Override}
     */
    private boolean isMarkedWithOverride(MethodTree tree) {
      Element method = TreeUtils.elementFromDeclaration(tree);
      return atypeFactory.getDeclAnnotation(method, Override.class) != null;
    }

    /**
     * Returns the delegate method call, if found, in a method body.
     *
     * <p>A delegate method call should be the only statement in a method body. If this is not the
     * case, or if there are other statements, return null.
     *
     * @param tree a method body
     * @return the delegate method call
     */
    private @Nullable MethodInvocationTree getDelegatedCall(BlockTree tree) {
      List<? extends StatementTree> stmts = tree.getStatements();
      if (stmts.size() != 1) {
        return null;
      }
      StatementTree stmt = stmts.get(0);
      if (!(stmt instanceof ReturnTree)) {
        return null;
      }
      ReturnTree returnStmt = (ReturnTree) stmt;
      ExpressionTree returnExpr = returnStmt.getExpression();
      if (!(returnExpr instanceof MethodInvocationTree)) {
        return null;
      }
      return (MethodInvocationTree) returnExpr;
    }
  }
}
