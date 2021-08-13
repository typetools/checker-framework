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
  public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
    ExecutableElement methodElt = TreeUtils.elementFromUse(node);
    if (methodElt != null && atypeFactory.getDeclAnnotation(methodElt, DisbarUse.class) != null) {
      checker.reportError(node, "disbar.use");
    }
    return super.visitMethodInvocation(node, p);
  }

  @Override
  public Void visitNewClass(NewClassTree node, Void p) {
    ExecutableElement consElt = TreeUtils.elementFromUse(node);
    if (consElt != null && atypeFactory.getDeclAnnotation(consElt, DisbarUse.class) != null) {
      checker.reportError(node, "disbar.use");
    }
    return super.visitNewClass(node, p);
  }

  @Override
  public Void visitIdentifier(IdentifierTree node, Void p) {
    MemberSelectTree enclosingMemberSel = enclosingMemberSelect();
    ExpressionTree[] memberSelectTrees =
        enclosingMemberSel == null
            ? new ExpressionTree[] {node}
            : new ExpressionTree[] {enclosingMemberSel, node};

    for (ExpressionTree memberSel : memberSelectTrees) {
      final Element elem = TreeUtils.elementFromUse(memberSel);

      if (elem == null || (!elem.getKind().isField() && elem.getKind() != ElementKind.PARAMETER)) {
        continue;
      }

      if (atypeFactory.getDeclAnnotation(elem, DisbarUse.class) != null) {
        checker.reportError(memberSel, "disbar.use");
      }
    }

    return super.visitIdentifier(node, p);
  }
}
