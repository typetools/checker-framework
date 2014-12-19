package org.checkerframework.qualframework.base;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.qualframework.util.QualifierContext;

import javax.lang.model.type.TypeKind;

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
}
