package checkers.igj;

import java.util.*;

import javax.lang.model.element.*;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import checkers.basetype.BaseTypeVisitor;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;

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
}
