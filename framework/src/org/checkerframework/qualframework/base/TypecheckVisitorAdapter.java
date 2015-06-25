package org.checkerframework.qualframework.base;

import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedUnionType;
import org.checkerframework.qualframework.util.QualifierContext;

import javax.lang.model.type.TypeKind;

import com.sun.source.tree.CatchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

/**
 * This class is a shim to allow writing typecheck-visitors using {@link QualifiedTypeMirror}s instead of
 * {@link AnnotatedTypeMirror}s.
 *
 * Extending classes will use the QualifiedTypeFactory provided by {@link context}.
 *
 */
public class TypecheckVisitorAdapter<Q> extends BaseTypeVisitor<GenericAnnotatedTypeFactory<?,?,?,?>> {

    protected final QualifierContext<Q> context;

    public TypecheckVisitorAdapter(CheckerAdapter<Q> checker) {
        super(checker);
        this.context = checker.getUnderlying();

    }

    @SuppressWarnings("unchecked")
    @Override
    protected QualifiedTypeFactoryAdapter<Q> createTypeFactory() {
        return ((CheckerAdapter<Q>) checker).getTypeFactory();
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType, Tree tree) {

        if (tree.getKind() == Kind.VARIABLE &&
            InternalUtils.symbol(tree).asType().getKind() == TypeKind.TYPEVAR) {
            // We get an error annotating type variable uses otherwise.
            return true;
        }

        return super.isValidUse(declarationType, useType, tree);
    }

    @Override
    protected void checkExceptionParameter(CatchTree node) {
        QualifiedTypeMirror<Q> exceptionParam = context.getTypeFactory()
                .getQualifiedType((node.getParameter()));
        Q required = getExceptionParameterLowerBoundQualifier();

        Q found = context.getTypeFactory()
                .getQualifiedType((node.getParameter())).getQualifier();

        if (!context.getTypeFactory().getQualifierHierarchy().isSubtype(required, found)) {
            checker.report(
                    Result.failure("exception.parameter.invalid", found, required),
                    node.getParameter());
        }

        if (exceptionParam.getKind() == TypeKind.UNION) {
            QualifiedUnionType<Q> aut = (QualifiedUnionType<Q>) exceptionParam;
            for (QualifiedTypeMirror<Q> alterntive : aut.getAlternatives()) {
                Q foundAltern = alterntive.getQualifier();
                if (!context.getTypeFactory().getQualifierHierarchy().isSubtype(required, foundAltern)) {
                    checker.report(Result.failure("exception.parameter.invalid",
                            foundAltern, required), node.getParameter());
                }
            }
        }
    }

    protected Q getExceptionParameterLowerBoundQualifier() {
        return context.getTypeFactory().getQualifierHierarchy().getTop();
    }
}
