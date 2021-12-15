package org.checkerframework.framework.ajava;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.ExportsTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.OpensTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ProvidesTree;
import com.sun.source.tree.RequiresTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.UsesTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.TreeScanner;

/**
 * A visitor that performs some default action on a tree and then all of its children. To use this
 * class, override {@code defaultAction}.
 */
public abstract class TreeScannerWithDefaults extends TreeScanner<Void, Void> {

  /**
   * Action performed on each visited tree.
   *
   * @param tree tree to perform action on
   */
  public abstract void defaultAction(Tree tree);

  @Override
  public Void scan(Tree tree, Void unused) {
    if (tree != null) {
      if (tree.getKind().name().equals("SWITCH_EXPRESSION")) {
        visitSwitchExpression17(tree, unused);
        return null;
      } else if (tree.getKind().name().equals("YIELD")) {
        visitYield17(tree, unused);
        return null;
      }
    }
    return super.scan(tree, unused);
  }

  @Override
  public Void visitAnnotatedType(AnnotatedTypeTree tree, Void p) {
    defaultAction(tree);
    return super.visitAnnotatedType(tree, p);
  }

  @Override
  public Void visitAnnotation(AnnotationTree tree, Void p) {
    defaultAction(tree);
    return super.visitAnnotation(tree, p);
  }

  @Override
  public Void visitArrayAccess(ArrayAccessTree tree, Void p) {
    defaultAction(tree);
    return super.visitArrayAccess(tree, p);
  }

  @Override
  public Void visitArrayType(ArrayTypeTree tree, Void p) {
    defaultAction(tree);
    return super.visitArrayType(tree, p);
  }

  @Override
  public Void visitAssert(AssertTree tree, Void p) {
    defaultAction(tree);
    return super.visitAssert(tree, p);
  }

  @Override
  public Void visitAssignment(AssignmentTree tree, Void p) {
    defaultAction(tree);
    return super.visitAssignment(tree, p);
  }

  @Override
  public Void visitBinary(BinaryTree tree, Void p) {
    defaultAction(tree);
    return super.visitBinary(tree, p);
  }

  @Override
  public Void visitBlock(BlockTree tree, Void p) {
    defaultAction(tree);
    return super.visitBlock(tree, p);
  }

  @Override
  public Void visitBreak(BreakTree tree, Void p) {
    defaultAction(tree);
    return super.visitBreak(tree, p);
  }

  @Override
  public Void visitCase(CaseTree tree, Void p) {
    defaultAction(tree);
    return super.visitCase(tree, p);
  }

  @Override
  public Void visitCatch(CatchTree tree, Void p) {
    defaultAction(tree);
    return super.visitCatch(tree, p);
  }

  @Override
  public Void visitClass(ClassTree tree, Void p) {
    defaultAction(tree);
    return super.visitClass(tree, p);
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree tree, Void p) {
    defaultAction(tree);
    return super.visitCompilationUnit(tree, p);
  }

  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
    defaultAction(tree);
    return super.visitCompoundAssignment(tree, p);
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpressionTree tree, Void p) {
    defaultAction(tree);
    return super.visitConditionalExpression(tree, p);
  }

  @Override
  public Void visitContinue(ContinueTree tree, Void p) {
    defaultAction(tree);
    return super.visitContinue(tree, p);
  }

  @Override
  public Void visitDoWhileLoop(DoWhileLoopTree tree, Void p) {
    defaultAction(tree);
    return super.visitDoWhileLoop(tree, p);
  }

  @Override
  public Void visitEmptyStatement(EmptyStatementTree tree, Void p) {
    defaultAction(tree);
    return super.visitEmptyStatement(tree, p);
  }

  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree tree, Void p) {
    defaultAction(tree);
    return super.visitEnhancedForLoop(tree, p);
  }

  @Override
  public Void visitErroneous(ErroneousTree tree, Void p) {
    defaultAction(tree);
    return super.visitErroneous(tree, p);
  }

  @Override
  public Void visitExports(ExportsTree tree, Void p) {
    defaultAction(tree);
    return super.visitExports(tree, p);
  }

  @Override
  public Void visitExpressionStatement(ExpressionStatementTree tree, Void p) {
    defaultAction(tree);
    return super.visitExpressionStatement(tree, p);
  }

  @Override
  public Void visitForLoop(ForLoopTree tree, Void p) {
    defaultAction(tree);
    return super.visitForLoop(tree, p);
  }

  @Override
  public Void visitIdentifier(IdentifierTree tree, Void p) {
    defaultAction(tree);
    return super.visitIdentifier(tree, p);
  }

  @Override
  public Void visitIf(IfTree tree, Void p) {
    defaultAction(tree);
    return super.visitIf(tree, p);
  }

  @Override
  public Void visitImport(ImportTree tree, Void p) {
    defaultAction(tree);
    return super.visitImport(tree, p);
  }

  @Override
  public Void visitInstanceOf(InstanceOfTree tree, Void p) {
    defaultAction(tree);
    return super.visitInstanceOf(tree, p);
  }

  @Override
  public Void visitIntersectionType(IntersectionTypeTree tree, Void p) {
    defaultAction(tree);
    return super.visitIntersectionType(tree, p);
  }

  @Override
  public Void visitLabeledStatement(LabeledStatementTree tree, Void p) {
    defaultAction(tree);
    return super.visitLabeledStatement(tree, p);
  }

  @Override
  public Void visitLambdaExpression(LambdaExpressionTree tree, Void p) {
    defaultAction(tree);
    return super.visitLambdaExpression(tree, p);
  }

  @Override
  public Void visitLiteral(LiteralTree tree, Void p) {
    defaultAction(tree);
    return super.visitLiteral(tree, p);
  }

  @Override
  public Void visitMemberReference(MemberReferenceTree tree, Void p) {
    defaultAction(tree);
    return super.visitMemberReference(tree, p);
  }

  @Override
  public Void visitMemberSelect(MemberSelectTree tree, Void p) {
    defaultAction(tree);
    return super.visitMemberSelect(tree, p);
  }

  @Override
  public Void visitMethod(MethodTree tree, Void p) {
    defaultAction(tree);
    return super.visitMethod(tree, p);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
    defaultAction(tree);
    return super.visitMethodInvocation(tree, p);
  }

  @Override
  public Void visitModifiers(ModifiersTree tree, Void p) {
    defaultAction(tree);
    return super.visitModifiers(tree, p);
  }

  @Override
  public Void visitModule(ModuleTree tree, Void p) {
    defaultAction(tree);
    return super.visitModule(tree, p);
  }

  @Override
  public Void visitNewArray(NewArrayTree tree, Void p) {
    defaultAction(tree);
    return super.visitNewArray(tree, p);
  }

  @Override
  public Void visitNewClass(NewClassTree tree, Void p) {
    defaultAction(tree);
    return super.visitNewClass(tree, p);
  }

  @Override
  public Void visitOpens(OpensTree tree, Void p) {
    defaultAction(tree);
    return super.visitOpens(tree, p);
  }

  @Override
  public Void visitOther(Tree tree, Void p) {
    defaultAction(tree);
    return super.visitOther(tree, p);
  }

  @Override
  public Void visitPackage(PackageTree tree, Void p) {
    defaultAction(tree);
    return super.visitPackage(tree, p);
  }

  @Override
  public Void visitParameterizedType(ParameterizedTypeTree tree, Void p) {
    defaultAction(tree);
    return super.visitParameterizedType(tree, p);
  }

  @Override
  public Void visitParenthesized(ParenthesizedTree tree, Void p) {
    defaultAction(tree);
    return super.visitParenthesized(tree, p);
  }

  @Override
  public Void visitPrimitiveType(PrimitiveTypeTree tree, Void p) {
    defaultAction(tree);
    return super.visitPrimitiveType(tree, p);
  }

  @Override
  public Void visitProvides(ProvidesTree tree, Void p) {
    defaultAction(tree);
    return super.visitProvides(tree, p);
  }

  @Override
  public Void visitRequires(RequiresTree tree, Void p) {
    defaultAction(tree);
    return super.visitRequires(tree, p);
  }

  @Override
  public Void visitReturn(ReturnTree tree, Void p) {
    defaultAction(tree);
    return super.visitReturn(tree, p);
  }

  @Override
  public Void visitSwitch(SwitchTree tree, Void p) {
    defaultAction(tree);
    return super.visitSwitch(tree, p);
  }

  public Void visitSwitchExpression17(Tree tree, Void p) {
    defaultAction(tree);
    return super.scan(tree, p);
  }

  @Override
  public Void visitSynchronized(SynchronizedTree tree, Void p) {
    defaultAction(tree);
    return super.visitSynchronized(tree, p);
  }

  @Override
  public Void visitThrow(ThrowTree tree, Void p) {
    defaultAction(tree);
    return super.visitThrow(tree, p);
  }

  @Override
  public Void visitTry(TryTree tree, Void p) {
    defaultAction(tree);
    return super.visitTry(tree, p);
  }

  @Override
  public Void visitTypeCast(TypeCastTree tree, Void p) {
    defaultAction(tree);
    return super.visitTypeCast(tree, p);
  }

  @Override
  public Void visitTypeParameter(TypeParameterTree tree, Void p) {
    defaultAction(tree);
    return super.visitTypeParameter(tree, p);
  }

  @Override
  public Void visitUnary(UnaryTree tree, Void p) {
    defaultAction(tree);
    return super.visitUnary(tree, p);
  }

  @Override
  public Void visitUnionType(UnionTypeTree tree, Void p) {
    defaultAction(tree);
    return super.visitUnionType(tree, p);
  }

  @Override
  public Void visitUses(UsesTree tree, Void p) {
    defaultAction(tree);
    return super.visitUses(tree, p);
  }

  @Override
  public Void visitVariable(VariableTree tree, Void p) {
    defaultAction(tree);
    return super.visitVariable(tree, p);
  }

  @Override
  public Void visitWhileLoop(WhileLoopTree tree, Void p) {
    defaultAction(tree);
    return super.visitWhileLoop(tree, p);
  }

  @Override
  public Void visitWildcard(WildcardTree tree, Void p) {
    defaultAction(tree);
    return super.visitWildcard(tree, p);
  }

  public Void visitYield17(Tree tree, Void p) {
    defaultAction(tree);
    return super.scan(tree, p);
  }
}
