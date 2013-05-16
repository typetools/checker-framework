package checkers.oigj;

import javax.lang.model.element.Element;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;

import checkers.basetype.BaseTypeVisitor;
import checkers.oigj.quals.Dominator;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;

public class OwnershipVisitor extends BaseTypeVisitor<OwnershipSubchecker> {

    public OwnershipVisitor(OwnershipSubchecker checker, CompilationUnitTree root) {
        super(checker, root);
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType) {
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
