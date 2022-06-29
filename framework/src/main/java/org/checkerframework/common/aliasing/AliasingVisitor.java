package org.checkerframework.common.aliasing;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.common.aliasing.qual.LeakedToResult;
import org.checkerframework.common.aliasing.qual.NonLeaked;
import org.checkerframework.common.aliasing.qual.Unique;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This visitor ensures that every constructor whose result is annotated as {@literal @}Unique does
 * not leak aliases.
 *
 * <p>TODO: Implement {@literal @}NonLeaked and {@literal @}LeakedToResult verifications:
 *
 * <p>{@literal @}NonLeaked: When a method declaration has a parameter annotated as
 * {@literal @}NonLeaked, the method body must not leak a reference to that parameter.
 *
 * <p>{@literal @}LeakedToResult: When a method declaration has a parameter annotated as
 * {@literal @}LeakedToResult, the method body must not leak a reference to that parameter, except
 * at the method return statements.
 *
 * <p>Both of the checks above are similar to the @Unique check that is implemented in this visitor.
 */
public class AliasingVisitor extends BaseTypeVisitor<AliasingAnnotatedTypeFactory> {

  public AliasingVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * Checks that if a method call is being invoked inside a constructor with result type
   * {@literal @}Unique, it must not leak the "this" reference. There are 3 ways to make sure that
   * this is not happening:
   *
   * <ol>
   *   <li>{@code this} is not an argument of the method call.
   *   <li>{@code this} is an argument of the method call, but the respective parameter is annotated
   *       as {@literal @}NonLeaked.
   *   <li>{@code this} is an argument of the method call, but the respective parameter is annotated
   *       as {@literal @}LeakedToResult AND the result of the method call is not being stored (the
   *       method call is a statement).
   * </ol>
   *
   * The private method {@code isUniqueCheck} handles cases 2 and 3.
   */
  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
    // The check only needs to be done for constructors with result type
    // @Unique. We also want to avoid visiting the <init> method.
    if (isInUniqueConstructor()) {
      if (TreeUtils.isSuperConstructorCall(node)) {
        // Check if a call to super() might create an alias: that
        // happens when the parent's respective constructor is not @Unique.
        AnnotatedTypeMirror superResult = atypeFactory.getAnnotatedType(node);
        if (!superResult.hasAnnotation(Unique.class)) {
          checker.reportError(node, "unique.leaked");
        }
      } else {
        // TODO: Currently the type of "this" doesn't always return the type of the constructor
        // result, therefore we need this "else" block. Once constructors are implemented correctly
        // we could remove that code below, since the type of "this" in a @Unique constructor will
        // be @Unique.
        Tree parent = getCurrentPath().getParentPath().getLeaf();
        boolean parentIsStatement = parent.getKind() == Tree.Kind.EXPRESSION_STATEMENT;
        ExecutableElement methodElement = TreeUtils.elementFromUse(node);
        List<? extends VariableElement> params = methodElement.getParameters();
        List<? extends ExpressionTree> args = node.getArguments();
        assert (args.size() == params.size())
            : "Number of arguments in"
                + " the method call "
                + node
                + " is different from the "
                + "number of parameters for the method declaration: "
                + methodElement.getSimpleName();
        for (int i = 0; i < args.size(); i++) {
          // Here we are traversing the arguments of the method call.
          // For every argument we check if it is a reference to "this".
          if (TreeUtils.isExplicitThisDereference(args.get(i))) {
            // If it is a reference to "this", there is still hope that
            // it is not being leaked (2. and 3. from the javadoc).
            VariableElement param = params.get(i);
            boolean hasNonLeaked =
                atypeFactory.getAnnotatedType(param).hasAnnotation(NonLeaked.class);
            boolean hasLeakedToResult =
                atypeFactory.getAnnotatedType(param).hasAnnotation(LeakedToResult.class);
            isUniqueCheck(node, parentIsStatement, hasNonLeaked, hasLeakedToResult);
          } else {
            // Not possible to leak reference here (case 1. from the javadoc).
          }
        }

        // Now, doing the same as above for the receiver parameter
        AnnotatedExecutableType annotatedType = atypeFactory.getAnnotatedType(methodElement);
        AnnotatedDeclaredType receiverType = annotatedType.getReceiverType();
        if (receiverType != null) {
          boolean hasNonLeaked = receiverType.hasAnnotation(NonLeaked.class);
          boolean hasLeakedToResult = receiverType.hasAnnotation(LeakedToResult.class);
          isUniqueCheck(node, parentIsStatement, hasNonLeaked, hasLeakedToResult);
        }
      }
    }
    return super.visitMethodInvocation(node, p);
  }

  private void isUniqueCheck(
      MethodInvocationTree node,
      boolean parentIsStatement,
      boolean hasNonLeaked,
      boolean hasLeakedToResult) {
    if (hasNonLeaked || (hasLeakedToResult && parentIsStatement)) {
      // Not leaked according to cases 2. and 3. from the javadoc of
      // visitMethodInvocation.
    } else {
      // May be leaked, raise warning.
      checker.reportError(node, "unique.leaked");
    }
  }

  // TODO: Merge that code in commonAssignmentCheck(AnnotatedTypeMirror varType, ExpressionTree
  // valueExp, String errorKey, boolean isLocalVariableAssignement), because the method below isn't
  // called for pseudo-assignments, but the mentioned one is. The issue of copy-pasting the code
  // from this method to the other one is that a declaration such as: List<@Unique Object> will
  // raise a unique.leaked warning, as there is a pseudo-assignment from @Unique to a @MaybeAliased
  // object, if the @Unique annotation is not in the stubfile.  TODO: Change the documentation in
  // BaseTypeVisitor to point out that this isn't called for pseudo-assignments.
  @Override
  protected void commonAssignmentCheck(
      Tree varTree,
      ExpressionTree valueExp,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {
    super.commonAssignmentCheck(varTree, valueExp, errorKey, extraArgs);
    if (isInUniqueConstructor() && TreeUtils.isExplicitThisDereference(valueExp)) {
      // If an assignment occurs inside a constructor with result type @Unique, it will invalidate
      // the @Unique property by using the "this" reference.
      checker.reportError(valueExp, "unique.leaked");
    } else if (canBeLeaked(valueExp)) {
      checker.reportError(valueExp, "unique.leaked");
    }
  }

  @Override
  @FormatMethod
  protected void commonAssignmentCheck(
      AnnotatedTypeMirror varType,
      AnnotatedTypeMirror valueType,
      Tree valueTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {
    super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, extraArgs);

    // If we are visiting a pseudo-assignment, visitorLeafKind is either
    // Tree.Kind.NEW_CLASS or Tree.Kind.METHOD_INVOCATION.
    TreePath path = getCurrentPath();
    if (path == null) {
      return;
    }
    Tree.Kind visitorLeafKind = path.getLeaf().getKind();

    if (visitorLeafKind == Tree.Kind.NEW_CLASS || visitorLeafKind == Tree.Kind.METHOD_INVOCATION) {
      // Handling pseudo-assignments
      if (canBeLeaked(valueTree)) {
        Tree.Kind parentKind = getCurrentPath().getParentPath().getLeaf().getKind();

        if (!varType.hasAnnotation(NonLeaked.class)
            && !(varType.hasAnnotation(LeakedToResult.class)
                && parentKind == Tree.Kind.EXPRESSION_STATEMENT)) {
          checker.reportError(valueTree, "unique.leaked");
        }
      }
    }
  }

  @Override
  public Void visitThrow(ThrowTree node, Void p) {
    // throw is also an escape mechanism. If an expression of type
    // @Unique is thrown, it is not @Unique anymore.
    ExpressionTree exp = node.getExpression();
    if (canBeLeaked(exp)) {
      checker.reportError(exp, "unique.leaked");
    }
    return super.visitThrow(node, p);
  }

  @Override
  public Void visitVariable(VariableTree node, Void p) {
    // Component types are not allowed to have the @Unique annotation.
    AnnotatedTypeMirror varType = atypeFactory.getAnnotatedType(node);
    VariableElement elt = TreeUtils.elementFromDeclaration(node);
    if (elt.getKind().isField() && varType.hasExplicitAnnotation(Unique.class)) {
      checker.reportError(node, "unique.location.forbidden");
    } else if (node.getType().getKind() == Tree.Kind.ARRAY_TYPE) {
      AnnotatedArrayType arrayType = (AnnotatedArrayType) varType;
      if (arrayType.getComponentType().hasAnnotation(Unique.class)) {
        checker.reportError(node, "unique.location.forbidden");
      }
    } else if (node.getType().getKind() == Tree.Kind.PARAMETERIZED_TYPE) {
      AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) varType;
      for (AnnotatedTypeMirror atm : declaredType.getTypeArguments()) {
        if (atm.hasAnnotation(Unique.class)) {
          checker.reportError(node, "unique.location.forbidden");
        }
      }
    }
    return super.visitVariable(node, p);
  }

  @Override
  public Void visitNewArray(NewArrayTree node, Void p) {
    List<? extends ExpressionTree> initializers = node.getInitializers();
    if (initializers != null && !initializers.isEmpty()) {
      for (ExpressionTree exp : initializers) {
        if (canBeLeaked(exp)) {
          checker.reportError(exp, "unique.leaked");
        }
      }
    }
    return super.visitNewArray(node, p);
  }

  @Override
  protected void checkConstructorResult(
      AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
    // @Unique is verified, so don't check this.
    if (!constructorType.getReturnType().hasAnnotation(atypeFactory.UNIQUE)) {
      super.checkConstructorResult(constructorType, constructorElement);
    }
  }

  @Override
  protected void checkThisOrSuperConstructorCall(
      MethodInvocationTree superCall, @CompilerMessageKey String errorKey) {
    if (isInUniqueConstructor()) {
      // Check if a call to super() might create an alias: that
      // happens when the parent's respective constructor is not @Unique.
      AnnotatedTypeMirror superResult = atypeFactory.getAnnotatedType(superCall);
      if (!superResult.hasAnnotation(Unique.class)) {
        checker.reportError(superCall, "unique.leaked");
      }
    }
  }

  /**
   * Returns true if {@code exp} has type {@code @Unique} and is not a method invocation nor a new
   * class expression. It checks whether the tree expression is unique by either checking for an
   * explicit annotation or checking whether the class of the tree expression {@code exp} has type
   * {@code @Unique}
   *
   * @param exp the Tree to check
   * @return true if {@code exp} has type {@code @Unique} and is not a method invocation nor a new
   *     class expression
   */
  private boolean canBeLeaked(Tree exp) {
    AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(exp);
    boolean isMethodInvocation = exp.getKind() == Tree.Kind.METHOD_INVOCATION;
    boolean isNewClass = exp.getKind() == Tree.Kind.NEW_CLASS;
    boolean isUniqueType = isUniqueClass(type) || type.hasExplicitAnnotation(Unique.class);
    return isUniqueType && !isMethodInvocation && !isNewClass;
  }

  /**
   * Return true if the class declaration for annotated type {@code type} has annotation
   * {@code @Unique}.
   *
   * @param type the annotated type whose class must be checked
   * @return boolean true if class is unique and false otherwise
   */
  private boolean isUniqueClass(AnnotatedTypeMirror type) {
    Element el = types.asElement(type.getUnderlyingType());
    if (el == null) {
      return false;
    }
    Set<AnnotationMirror> annoMirrors = atypeFactory.getDeclAnnotations(el);
    if (annoMirrors == null) {
      return false;
    }
    if (atypeFactory.containsSameByClass(annoMirrors, Unique.class)) {
      return true;
    }
    return false;
  }

  /**
   * Returns true if the enclosing method is a constructor whose return type is annotated as
   * {@code @Unique}.
   *
   * @return true if the enclosing method is a constructor whose return type is annotated as
   *     {@code @Unique}
   */
  private boolean isInUniqueConstructor() {
    MethodTree enclosingMethod = TreePathUtil.enclosingMethod(getCurrentPath());
    if (enclosingMethod == null) {
      return false; // No enclosing method.
    }
    return TreeUtils.isConstructor(enclosingMethod)
        && atypeFactory
            .getAnnotatedType(enclosingMethod)
            .getReturnType()
            .hasAnnotation(Unique.class);
  }
}
