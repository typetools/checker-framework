package org.checkerframework.checker.testchecker.disbaruse;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.testchecker.disbaruse.qual.DisbarUse;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.javacutil.TreeUtils;

public class DisbarUseVisitor extends BaseTypeVisitor<DisbarUseTypeFactory> {
  public DisbarUseVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  protected DisbarUseVisitor(BaseTypeChecker checker, DisbarUseTypeFactory typeFactory) {
    super(checker, typeFactory);
  }

  @Override
  protected DisbarUseTypeFactory createTypeFactory() {
    return new DisbarUseTypeFactory(checker);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
    ExecutableElement methodElt = TreeUtils.elementFromUse(tree);
    if (methodElt != null && atypeFactory.getDeclAnnotation(methodElt, DisbarUse.class) != null) {
      checker.reportError(tree, "disbar.use");
    }
    return super.visitMethodInvocation(tree, p);
  }

  @Override
  public Void visitNewClass(NewClassTree tree, Void p) {
    ExecutableElement consElt = TreeUtils.elementFromUse(tree);
    if (consElt != null && atypeFactory.getDeclAnnotation(consElt, DisbarUse.class) != null) {
      checker.reportError(tree, "disbar.use");
    }
    return super.visitNewClass(tree, p);
  }

  @Override
  public Void visitIdentifier(IdentifierTree tree, Void p) {
    MemberSelectTree enclosingMemberSel = enclosingMemberSelect();
    ExpressionTree[] expressionTrees =
        enclosingMemberSel == null
            ? new ExpressionTree[] {tree}
            : new ExpressionTree[] {enclosingMemberSel, tree};

    for (ExpressionTree memberSel : expressionTrees) {
      Element elem = TreeUtils.elementFromUse(memberSel);

      // We only issue errors for variables that are fields or parameters:
      if (elem != null && (elem.getKind().isField() || elem.getKind() == ElementKind.PARAMETER)) {
        if (atypeFactory.getDeclAnnotation(elem, DisbarUse.class) != null) {
          checker.reportError(memberSel, "disbar.use");
        }
      }
    }

    return super.visitIdentifier(tree, p);
  }
}
