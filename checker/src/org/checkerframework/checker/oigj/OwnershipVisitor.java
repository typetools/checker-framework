package org.checkerframework.checker.oigj;

import javax.lang.model.element.Element;

import org.checkerframework.checker.oigj.qual.Dominator;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

public class OwnershipVisitor extends BaseTypeVisitor<OwnershipAnnotatedTypeFactory> {

    public OwnershipVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType, Tree tree) {
        return true;
    }

    @Override
    protected boolean isAccessAllowed(Element field,
            AnnotatedTypeMirror receiver, ExpressionTree accessTree) {
        AnnotatedTypeMirror fType = atypeFactory.getAnnotatedType(field);
        if (fType.hasAnnotation(Dominator.class)
            && !atypeFactory.isMostEnclosingThisDeref(accessTree))
            return false;
        return super.isAccessAllowed(field, receiver, accessTree);
    }
}
