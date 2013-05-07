package checkers.oigj;

import java.util.Collections;

import javax.lang.model.element.Element;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import checkers.basetype.BaseTypeVisitor;
import checkers.oigj.quals.Assignable;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

public class ImmutabilityVisitor extends BaseTypeVisitor<ImmutabilitySubchecker> {

    public ImmutabilityVisitor(ImmutabilitySubchecker checker, CompilationUnitTree root) {
        super(checker, root);
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
                             AnnotatedDeclaredType useType) {
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

        if (checker.getQualifierHierarchy().isSubtype(
                receiverType.getAnnotations(),
                Collections.singleton(checker.ASSIGNS_FIELDS)))
            return true;

        return false;
    }
}
