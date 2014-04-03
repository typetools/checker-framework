package org.checkerframework.qualframework.base;

import com.sun.source.tree.*;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

import org.checkerframework.qualframework.util.WrappedAnnotatedTypeMirror;

/**
 * Adapter for {@link TreeAnnotator}, extending
 * {@link org.checkerframework.framework.type.TreeAnnotator org.checkerframework.framework.type.TreeAnnotator}. 
 */
class TreeAnnotatorAdapter<Q> extends org.checkerframework.framework.type.TreeAnnotator {
    private TreeAnnotator<Q> underlying;
    private TypeMirrorConverter<Q> converter;

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

    // TODO: Having the underlying method return 'null' to signal 'use default
    // implementation' is a total hack and will probably make some checker
    // developers unhappy.  Provide proper 'superVisitX' methods instead.
    @Override
    public Void visitAnnotation(AnnotationTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitAnnotation(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitAnnotation(node, atm);
        }
        return null;
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitArrayAccess(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitArrayAccess(node, atm);
        }
        return null;
    }

    @Override
    public Void visitArrayType(ArrayTypeTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitArrayType(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitArrayType(node, atm);
        }
        return null;
    }

    @Override
    public Void visitAssert(AssertTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitAssert(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitAssert(node, atm);
        }
        return null;
    }

    @Override
    public Void visitAssignment(AssignmentTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitAssignment(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitAssignment(node, atm);
        }
        return null;
    }

    @Override
    public Void visitBinary(BinaryTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitBinary(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitBinary(node, atm);
        }
        return null;
    }

    @Override
    public Void visitBlock(BlockTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitBlock(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitBlock(node, atm);
        }
        return null;
    }

    @Override
    public Void visitBreak(BreakTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitBreak(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitBreak(node, atm);
        }
        return null;
    }

    @Override
    public Void visitCase(CaseTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitCase(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitCase(node, atm);
        }
        return null;
    }

    @Override
    public Void visitCatch(CatchTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitCatch(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitCatch(node, atm);
        }
        return null;
    }

    @Override
    public Void visitClass(ClassTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitClass(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitClass(node, atm);
        }
        return null;
    }

    @Override
    public Void visitCompilationUnit(CompilationUnitTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitCompilationUnit(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitCompilationUnit(node, atm);
        }
        return null;
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitCompoundAssignment(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitCompoundAssignment(node, atm);
        }
        return null;
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitConditionalExpression(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitConditionalExpression(node, atm);
        }
        return null;
    }

    @Override
    public Void visitContinue(ContinueTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitContinue(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitContinue(node, atm);
        }
        return null;
    }

    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitDoWhileLoop(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitDoWhileLoop(node, atm);
        }
        return null;
    }

    @Override
    public Void visitEmptyStatement(EmptyStatementTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitEmptyStatement(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitEmptyStatement(node, atm);
        }
        return null;
    }

    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitEnhancedForLoop(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitEnhancedForLoop(node, atm);
        }
        return null;
    }

    @Override
    public Void visitErroneous(ErroneousTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitErroneous(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitErroneous(node, atm);
        }
        return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitExpressionStatement(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitExpressionStatement(node, atm);
        }
        return null;
    }

    @Override
    public Void visitForLoop(ForLoopTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitForLoop(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitForLoop(node, atm);
        }
        return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitIdentifier(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitIdentifier(node, atm);
        }
        return null;
    }

    @Override
    public Void visitIf(IfTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitIf(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitIf(node, atm);
        }
        return null;
    }

    @Override
    public Void visitImport(ImportTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitImport(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitImport(node, atm);
        }
        return null;
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitInstanceOf(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitInstanceOf(node, atm);
        }
        return null;
    }

    @Override
    public Void visitLabeledStatement(LabeledStatementTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitLabeledStatement(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitLabeledStatement(node, atm);
        }
        return null;
    }

    @Override
    public Void visitLiteral(LiteralTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitLiteral(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitLiteral(node, atm);
        }
        return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitMemberSelect(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitMemberSelect(node, atm);
        }
        return null;
    }

    @Override
    public Void visitMethod(MethodTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitMethod(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitMethod(node, atm);
        }
        return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitMethodInvocation(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitMethodInvocation(node, atm);
        }
        return null;
    }

    @Override
    public Void visitModifiers(ModifiersTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitModifiers(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitModifiers(node, atm);
        }
        return null;
    }

    @Override
    public Void visitNewArray(NewArrayTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitNewArray(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitNewArray(node, atm);
        }
        return null;
    }

    @Override
    public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitNewClass(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitNewClass(node, atm);
        }
        return null;
    }

    @Override
    public Void visitOther(Tree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitOther(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitOther(node, atm);
        }
        return null;
    }

    @Override
    public Void visitParameterizedType(ParameterizedTypeTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitParameterizedType(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitParameterizedType(node, atm);
        }
        return null;
    }

    @Override
    public Void visitParenthesized(ParenthesizedTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitParenthesized(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitParenthesized(node, atm);
        }
        return null;
    }

    @Override
    public Void visitPrimitiveType(PrimitiveTypeTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitPrimitiveType(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitPrimitiveType(node, atm);
        }
        return null;
    }

    @Override
    public Void visitReturn(ReturnTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitReturn(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitReturn(node, atm);
        }
        return null;
    }

    @Override
    public Void visitSwitch(SwitchTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitSwitch(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitSwitch(node, atm);
        }
        return null;
    }

    @Override
    public Void visitSynchronized(SynchronizedTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitSynchronized(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitSynchronized(node, atm);
        }
        return null;
    }

    @Override
    public Void visitThrow(ThrowTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitThrow(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitThrow(node, atm);
        }
        return null;
    }

    @Override
    public Void visitTry(TryTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitTry(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitTry(node, atm);
        }
        return null;
    }

    @Override
    public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitTypeCast(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitTypeCast(node, atm);
        }
        return null;
    }

    @Override
    public Void visitTypeParameter(TypeParameterTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitTypeParameter(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitTypeParameter(node, atm);
        }
        return null;
    }

    @Override
    public Void visitUnary(UnaryTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitUnary(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitUnary(node, atm);
        }
        return null;
    }

    @Override
    public Void visitUnionType(UnionTypeTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitUnionType(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitUnionType(node, atm);
        }
        return null;
    }

    @Override
    public Void visitVariable(VariableTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitVariable(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitVariable(node, atm);
        }
        return null;
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitWhileLoop(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitWhileLoop(node, atm);
        }
        return null;
    }

    @Override
    public Void visitWildcard(WildcardTree node, AnnotatedTypeMirror atm) {
        QualifiedTypeMirror<Q> qtm = underlying.visitWildcard(node,
                WrappedAnnotatedTypeMirror.wrap(atm));
        if (qtm != null) {
            converter.applyQualifiers(qtm, atm);
        } else {
            super.visitWildcard(node, atm);
        }
        return null;
    }

}
