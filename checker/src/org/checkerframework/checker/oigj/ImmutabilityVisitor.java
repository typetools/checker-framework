package org.checkerframework.checker.oigj;

import java.util.Collections;

import javax.lang.model.element.Element;

import org.checkerframework.checker.oigj.qual.Assignable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TreeUtils;

import com.sun.source.tree.Tree;

public class ImmutabilityVisitor extends BaseTypeVisitor<ImmutabilityAnnotatedTypeFactory> {

    public ImmutabilityVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
                             AnnotatedDeclaredType useType, Tree tree) {
        return true;
    }

    @Override
    public boolean validateTypeOf(Tree tree) {
        return true;
    }

    //
    // OIGJ Rule 2. Field assignment
    // Field assignment o.f = ... is legal  iff
    //   (i) I(o) <= AssignsFields or f is annotated as @Assignable, and
    //   (ii) o = this or the type of f does not contain the owner Dominator
    //        or Modifier
    //
    @Override
    public boolean isAssignable(AnnotatedTypeMirror varType,
                                AnnotatedTypeMirror receiverType, Tree varTree) {
        if (!TreeUtils.isExpressionTree(varTree))
            return true;

        Element varElement = InternalUtils.symbol(varTree);
        if (varElement != null && atypeFactory.getDeclAnnotation(varElement, Assignable.class) != null)
            return true;

        if (receiverType==null) {
            // Happens e.g. for local variable, which doesn't have a receiver.
            return true;
        }

        if (atypeFactory.getQualifierHierarchy().isSubtype(
                receiverType.getAnnotations(),
                Collections.singleton(atypeFactory.ASSIGNS_FIELDS)))
            return true;

        return false;
    }
}
