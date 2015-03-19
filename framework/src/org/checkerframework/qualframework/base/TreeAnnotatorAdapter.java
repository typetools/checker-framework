package org.checkerframework.qualframework.base;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.qualframework.util.ExtendedTypeMirror;
import org.checkerframework.qualframework.util.WrappedAnnotatedTypeMirror;

import com.sun.source.tree.*;

/**
 * Adapter for {@link TreeAnnotator}, extending
 * {@link org.checkerframework.framework.type.treeannotator.TreeAnnotator org.checkerframework.framework.type.treeannotator.TreeAnnotator}.
 */
class TreeAnnotatorAdapter<Q> extends PropagationTreeAnnotator {
    private final TreeAnnotator<Q> underlying;
    private final TypeMirrorConverter<Q> converter;

    public TreeAnnotatorAdapter(TreeAnnotator<Q> underlying,
                                TypeMirrorConverter<Q> converter,
                                QualifiedTypeFactoryAdapter<Q> factoryAdapter) {
        super(factoryAdapter);
        this.underlying = underlying;
        this.converter = converter;
    }

    TypeMirrorConverter<Q> getConverter() {
        return converter;
    }

    @Override
    public Void visitAnnotation(AnnotationTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitAnnotation(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitAnnotation(AnnotationTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitAnnotation(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitArrayAccess(ArrayAccessTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitArrayAccess(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitArrayAccess(ArrayAccessTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitArrayAccess(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitArrayType(ArrayTypeTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitArrayType(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitArrayType(ArrayTypeTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitArrayType(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitAssert(AssertTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitAssert(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitAssert(AssertTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitAssert(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitAssignment(AssignmentTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitAssignment(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitAssignment(AssignmentTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitAssignment(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitBinary(BinaryTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitBinary(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitBinary(BinaryTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm =((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitBinary(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitBlock(BlockTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitBlock(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitBlock(BlockTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitBlock(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitBreak(BreakTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitBreak(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitBreak(BreakTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitBreak(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitCase(CaseTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitCase(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitCase(CaseTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitCase(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitCatch(CatchTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitCatch(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitCatch(CatchTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitCatch(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitClass(ClassTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitClass(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitClass(ClassTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitClass(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitCompilationUnit(CompilationUnitTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitCompilationUnit(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitCompilationUnit(CompilationUnitTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitCompilationUnit(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitCompoundAssignment(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitCompoundAssignment(CompoundAssignmentTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitCompoundAssignment(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitConditionalExpression(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitConditionalExpression(ConditionalExpressionTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitConditionalExpression(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitContinue(ContinueTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitContinue(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitContinue(ContinueTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitContinue(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitDoWhileLoop(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitDoWhileLoop(DoWhileLoopTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitDoWhileLoop(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitEmptyStatement(EmptyStatementTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitEmptyStatement(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitEmptyStatement(EmptyStatementTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitEmptyStatement(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitEnhancedForLoop(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitEnhancedForLoop(EnhancedForLoopTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitEnhancedForLoop(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitErroneous(ErroneousTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitErroneous(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitErroneous(ErroneousTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitErroneous(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitExpressionStatement(ExpressionStatementTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitExpressionStatement(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitExpressionStatement(ExpressionStatementTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitExpressionStatement(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitForLoop(ForLoopTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitForLoop(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitForLoop(ForLoopTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitForLoop(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitIdentifier(IdentifierTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitIdentifier(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitIdentifier(IdentifierTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitIdentifier(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitIf(IfTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitIf(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitIf(IfTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitIf(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitImport(ImportTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitImport(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitImport(ImportTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitImport(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitInstanceOf(InstanceOfTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitInstanceOf(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitInstanceOf(InstanceOfTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitInstanceOf(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitLabeledStatement(LabeledStatementTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitLabeledStatement(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitLabeledStatement(LabeledStatementTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitLabeledStatement(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitLiteral(LiteralTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitLiteral(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitLiteral(LiteralTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitLiteral(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitMemberSelect(MemberSelectTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitMemberSelect(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitMemberSelect(MemberSelectTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitMemberSelect(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitMethod(MethodTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitMethod(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitMethod(MethodTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitMethod(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitMethodInvocation(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitMethodInvocation(MethodInvocationTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitMethodInvocation(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitModifiers(ModifiersTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitModifiers(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitModifiers(ModifiersTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitModifiers(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitNewArray(NewArrayTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitNewArray(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitNewArray(NewArrayTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitNewArray(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitNewClass(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitNewClass(NewClassTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitNewClass(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitOther(Tree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitOther(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitOther(Tree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitOther(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitParameterizedType(ParameterizedTypeTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitParameterizedType(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitParameterizedType(ParameterizedTypeTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitParameterizedType(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitParenthesized(ParenthesizedTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitParenthesized(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitParenthesized(ParenthesizedTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitParenthesized(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitPrimitiveType(PrimitiveTypeTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitPrimitiveType(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitPrimitiveType(PrimitiveTypeTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitPrimitiveType(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitReturn(ReturnTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitReturn(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitReturn(ReturnTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitReturn(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitSwitch(SwitchTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitSwitch(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitSwitch(SwitchTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitSwitch(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitSynchronized(SynchronizedTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitSynchronized(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitSynchronized(SynchronizedTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitSynchronized(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitThrow(ThrowTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitThrow(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitThrow(ThrowTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitThrow(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitTry(TryTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitTry(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitTry(TryTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitTry(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitTypeCast(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitTypeCast(TypeCastTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitTypeCast(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitTypeParameter(TypeParameterTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitTypeParameter(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitTypeParameter(TypeParameterTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitTypeParameter(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitUnary(UnaryTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitUnary(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitUnary(UnaryTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitUnary(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitUnionType(UnionTypeTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitUnionType(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitUnionType(UnionTypeTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitUnionType(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitVariable(VariableTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitVariable(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitVariable(VariableTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitVariable(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitWhileLoop(WhileLoopTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitWhileLoop(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitWhileLoop(WhileLoopTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitWhileLoop(node, atm);
        return converter.getQualifiedType(atm);
    }


    @Override
    public Void visitWildcard(WildcardTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitWildcard(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        converter.applyQualifiers(qtm, atm);
        return null;
    }

    QualifiedTypeMirror<Q> superVisitWildcard(WildcardTree node, ExtendedTypeMirror type) {
        AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap().deepCopy();
        super.visitWildcard(node, atm);
        return converter.getQualifiedType(atm);
    }
}
