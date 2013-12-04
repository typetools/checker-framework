package checkers.oigj;

import checkers.basetype.BaseTypeChecker;
import checkers.basetype.BaseTypeVisitor;
import checkers.oigj.quals.Dominator;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;

import javax.lang.model.element.Element;

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
