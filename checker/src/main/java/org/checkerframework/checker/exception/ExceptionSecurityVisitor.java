package org.checkerframework.checker.exception;

import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.VariableTree;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.exception.qual.Insecure;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/** Visitor Class for Exception Security Checker */
public class ExceptionSecurityVisitor extends BaseTypeVisitor<BaseAnnotatedTypeFactory> {

  /**
   * Constructor for ExceptionSecurityVisitor
   *
   * @param checker Injected BaseTypeChecker
   */
  public ExceptionSecurityVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  public Void visitCatch(CatchTree tree, Void p) {
    checkForNestedExceptions(tree);

    return super.visitCatch(tree, p);
  }

  @Override
  public Void visitMethod(MethodTree tree, Void p) {
    checkForDirectlyThrownExceptions(tree);

    return super.visitMethod(tree, p);
  }

  /**
   * Checks if any of the exceptions are thrown directly on a method.
   *
   * @param methodTree The tree of the method
   */
  private void checkForDirectlyThrownExceptions(MethodTree methodTree) {
    List<? extends ExpressionTree> throwList = methodTree.getThrows();

    for (ExpressionTree throwable : throwList) {
      if (throwable.getKind().equals(Tree.Kind.IDENTIFIER)) {
        tryRaisingWarning((IdentifierTree) throwable);
      }
    }
  }

  /**
   * Check if any of the exceptions thrown is a child of the filtered exceptions
   *
   * @param catchTree The tree of the catch clause
   */
  private void checkForNestedExceptions(CatchTree catchTree) {
    if (catchTree.getBlock().getStatements().stream()
        .noneMatch(statementTree -> statementTree.getKind().equals(Tree.Kind.THROW))) {
      return;
    }

    VariableTree catchTreeVariable = catchTree.getParameter();

    if (catchTreeVariable.getKind().equals(Tree.Kind.VARIABLE)) {
      if (catchTreeVariable.getType().getKind().equals(Tree.Kind.IDENTIFIER)) {
        tryRaisingWarning((IdentifierTree) catchTreeVariable.getType());
      }
    } else if (catchTreeVariable.getKind().equals(Tree.Kind.UNION_TYPE)) {
      UnionTypeTree unionTypeTree = (UnionTypeTree) catchTreeVariable.getType();

      unionTypeTree
          .getTypeAlternatives()
          .forEach(
              type -> {
                if (type.getKind().equals(Tree.Kind.IDENTIFIER)) {
                  tryRaisingWarning((IdentifierTree) type);
                }
              });
    }
  }

  /**
   * Checks if the given identifierTree is a child of a filtered exception.
   *
   * @param identifierTree The tree of a class
   */
  private void tryRaisingWarning(IdentifierTree identifierTree) {
    Element identifierElement = TreeUtils.elementFromTree(identifierTree);
    TypeMirror identifierTypeMirror = identifierElement.asType();

    Optional<Warnings> warningOptional =
        Arrays.stream(Warnings.values())
            .filter(
                warning -> {
                  Class<?> exceptionClass = warning.getExceptionClass();

                  TypeMirror exceptionTypeMirror =
                      TypesUtils.typeFromClass(exceptionClass, types, elements);
                  return types.isSubtype(identifierTypeMirror, exceptionTypeMirror);
                })
            .findFirst();

    if (warningOptional.isPresent()) {
      checker.reportWarning(identifierTree, warningOptional.get().getWarning());
    } else if (ElementUtils.hasAnnotation(identifierElement, Insecure.class.getCanonicalName())) {
      checker.reportWarning(identifierTree, "warning.insecure");
    }
  }
}
