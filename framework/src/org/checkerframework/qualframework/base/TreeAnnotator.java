package org.checkerframework.qualframework.base;

import org.checkerframework.qualframework.util.ExtendedTypeMirror;

import com.sun.source.tree.*;
import com.sun.source.util.SimpleTreeVisitor;

/**
 * {@link DefaultQualifiedTypeFactory} component for computing the qualified
 * type of a {@link Tree}.
 */
public class TreeAnnotator<Q> extends SimpleTreeVisitor<QualifiedTypeMirror<Q>, ExtendedTypeMirror> {
    private TreeAnnotatorAdapter<Q> adapter;

    void setAdapter(TreeAnnotatorAdapter<Q> adapter) {
        this.adapter = adapter;
    }

    @Override
    public QualifiedTypeMirror<Q> defaultAction(Tree node, ExtendedTypeMirror type) {
        throw new UnsupportedOperationException("unsupported Tree kind: " + node.getKind());
    }

    @Override
    public QualifiedTypeMirror<Q> visitAnnotation(AnnotationTree node, ExtendedTypeMirror type) {
        return adapter.superVisitAnnotation(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitArrayAccess(ArrayAccessTree node, ExtendedTypeMirror type) {
        return adapter.superVisitArrayAccess(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitArrayType(ArrayTypeTree node, ExtendedTypeMirror type) {
        return adapter.superVisitArrayType(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitAssert(AssertTree node, ExtendedTypeMirror type) {
        return adapter.superVisitAssert(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitAssignment(AssignmentTree node, ExtendedTypeMirror type) {
        return adapter.superVisitAssignment(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitBinary(BinaryTree node, ExtendedTypeMirror type) {
        return adapter.superVisitBinary(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitBlock(BlockTree node, ExtendedTypeMirror type) {
        return adapter.superVisitBlock(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitBreak(BreakTree node, ExtendedTypeMirror type) {
        return adapter.superVisitBreak(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitCase(CaseTree node, ExtendedTypeMirror type) {
        return adapter.superVisitCase(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitCatch(CatchTree node, ExtendedTypeMirror type) {
        return adapter.superVisitCatch(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitClass(ClassTree node, ExtendedTypeMirror type) {
        return adapter.superVisitClass(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitCompilationUnit(CompilationUnitTree node, ExtendedTypeMirror type) {
        return adapter.superVisitCompilationUnit(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitCompoundAssignment(CompoundAssignmentTree node, ExtendedTypeMirror type) {
        return adapter.superVisitCompoundAssignment(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitConditionalExpression(ConditionalExpressionTree node, ExtendedTypeMirror type) {
        return adapter.superVisitConditionalExpression(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitContinue(ContinueTree node, ExtendedTypeMirror type) {
        return adapter.superVisitContinue(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitDoWhileLoop(DoWhileLoopTree node, ExtendedTypeMirror type) {
        return adapter.superVisitDoWhileLoop(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitEmptyStatement(EmptyStatementTree node, ExtendedTypeMirror type) {
        return adapter.superVisitEmptyStatement(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitEnhancedForLoop(EnhancedForLoopTree node, ExtendedTypeMirror type) {
        return adapter.superVisitEnhancedForLoop(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitErroneous(ErroneousTree node, ExtendedTypeMirror type) {
        return adapter.superVisitErroneous(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitExpressionStatement(ExpressionStatementTree node, ExtendedTypeMirror type) {
        return adapter.superVisitExpressionStatement(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitForLoop(ForLoopTree node, ExtendedTypeMirror type) {
        return adapter.superVisitForLoop(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitIdentifier(IdentifierTree node, ExtendedTypeMirror type) {
        return adapter.superVisitIdentifier(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitIf(IfTree node, ExtendedTypeMirror type) {
        return adapter.superVisitIf(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitImport(ImportTree node, ExtendedTypeMirror type) {
        return adapter.superVisitImport(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitInstanceOf(InstanceOfTree node, ExtendedTypeMirror type) {
        return adapter.superVisitInstanceOf(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitLabeledStatement(LabeledStatementTree node, ExtendedTypeMirror type) {
        return adapter.superVisitLabeledStatement(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitLiteral(LiteralTree node, ExtendedTypeMirror type) {
        return adapter.superVisitLiteral(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitMemberSelect(MemberSelectTree node, ExtendedTypeMirror type) {
        return adapter.superVisitMemberSelect(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitMethod(MethodTree node, ExtendedTypeMirror type) {
        return adapter.superVisitMethod(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitMethodInvocation(MethodInvocationTree node, ExtendedTypeMirror type) {
        return adapter.superVisitMethodInvocation(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitModifiers(ModifiersTree node, ExtendedTypeMirror type) {
        return adapter.superVisitModifiers(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitNewArray(NewArrayTree node, ExtendedTypeMirror type) {
        return adapter.superVisitNewArray(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitNewClass(NewClassTree node, ExtendedTypeMirror type) {
        return adapter.superVisitNewClass(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitOther(Tree node, ExtendedTypeMirror type) {
        return adapter.superVisitOther(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitParameterizedType(ParameterizedTypeTree node, ExtendedTypeMirror type) {
        return adapter.superVisitParameterizedType(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitParenthesized(ParenthesizedTree node, ExtendedTypeMirror type) {
        return adapter.superVisitParenthesized(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitPrimitiveType(PrimitiveTypeTree node, ExtendedTypeMirror type) {
        return adapter.superVisitPrimitiveType(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitReturn(ReturnTree node, ExtendedTypeMirror type) {
        return adapter.superVisitReturn(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitSwitch(SwitchTree node, ExtendedTypeMirror type) {
        return adapter.superVisitSwitch(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitSynchronized(SynchronizedTree node, ExtendedTypeMirror type) {
        return adapter.superVisitSynchronized(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitThrow(ThrowTree node, ExtendedTypeMirror type) {
        return adapter.superVisitThrow(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitTry(TryTree node, ExtendedTypeMirror type) {
        return adapter.superVisitTry(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitTypeCast(TypeCastTree node, ExtendedTypeMirror type) {
        return adapter.superVisitTypeCast(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitTypeParameter(TypeParameterTree node, ExtendedTypeMirror type) {
        return adapter.superVisitTypeParameter(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitUnary(UnaryTree node, ExtendedTypeMirror type) {
        return adapter.superVisitUnary(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitUnionType(UnionTypeTree node, ExtendedTypeMirror type) {
        return adapter.superVisitUnionType(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitVariable(VariableTree node, ExtendedTypeMirror type) {
        return adapter.superVisitVariable(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitWhileLoop(WhileLoopTree node, ExtendedTypeMirror type) {
        return adapter.superVisitWhileLoop(node, type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitWildcard(WildcardTree node, ExtendedTypeMirror type) {
        return adapter.superVisitWildcard(node, type);
    }
}
