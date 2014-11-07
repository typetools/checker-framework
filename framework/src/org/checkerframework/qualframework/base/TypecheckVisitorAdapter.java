package org.checkerframework.qualframework.base;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.qualframework.util.QualifierContext;

import javax.lang.model.type.TypeKind;

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
            // Need to allow any parameters on a declared type.
            return true;
        }

        return super.isValidUse(declarationType, useType, tree);
    }
}
