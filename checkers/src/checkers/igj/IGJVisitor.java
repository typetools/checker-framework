package checkers.igj;

import java.util.*;

import javax.lang.model.element.*;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

import checkers.basetype.BaseTypeVisitor;
import checkers.igj.quals.Assignable;
import checkers.igj.quals.AssignsFields;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.ElementUtils;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

/**
 * A type-checking visitor for the IGJ type
 * qualifier that uses the {@link BaseTypeVisitor} implementation. This visitor
 * reports errors or warnings for violations for the following cases:
 *
 * <ol>
 * <li value="1">constructing an infeasible type
 * </ol>
 *
 * @see BaseTypeVisitor
 */
public class IGJVisitor extends BaseTypeVisitor<IGJChecker> {

    public IGJVisitor(IGJChecker checker, CompilationUnitTree root) {
        super(checker, root);
        checkForAnnotatedJdk();
    }

    @Override
    protected boolean checkConstructorInvocation(AnnotatedDeclaredType dt,
            AnnotatedExecutableType constructor, Tree src) {
        Collection<AnnotationMirror> annos = constructor.getReceiverType().getAnnotations();
        if (annos.contains(checker.I) || annos.contains(checker.ASSIGNS_FIELDS))
            return true;
        else
            return super.checkConstructorInvocation(dt, constructor, src);
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType elemType, AnnotatedDeclaredType use) {
        if (elemType.hasEffectiveAnnotation(checker.I) || use.hasEffectiveAnnotation(checker.READONLY))
            return true;

        return super.isValidUse(elemType, use);
    }

    /**
     * Return true if the assignment variable is an assignable field or
     * variable, and returns false otherwise.
     *
     * A field is assignable if it is
     *
     * 1. a static field
     * 2. marked {@link Assignable}
     * 3. accessed through a mutable reference
     * 4. reassigned with an {@link AssignsFields} method and owned by 'this'
     *
     */
    @Override
    public boolean isAssignable(AnnotatedTypeMirror varType,
            AnnotatedTypeMirror receiverType, Tree varTree) {
        if (!TreeUtils.isExpressionTree(varTree))
            return true;

        Element varElement = InternalUtils.symbol(varTree);
        if (varTree.getKind() != Tree.Kind.ARRAY_ACCESS
                && (varElement == null // a variable element should never be null
                        || !varElement.getKind().isField()
                        || ElementUtils.isStatic(varElement)
                        || atypeFactory.getDeclAnnotation(varElement, Assignable.class) != null))
            return true;

        assert receiverType != null;

        final boolean isAssignable =
            receiverType.hasEffectiveAnnotation(checker.MUTABLE)
             || receiverType.hasEffectiveAnnotation(checker.BOTTOM_QUAL)
             || (receiverType.hasEffectiveAnnotation(checker.ASSIGNS_FIELDS)
                     && atypeFactory.isMostEnclosingThisDeref((ExpressionTree)varTree));

        return isAssignable;
    }

}
