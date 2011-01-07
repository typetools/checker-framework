package checkers.igj;

import java.util.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import checkers.basetype.BaseTypeVisitor;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.AnnotatedTypeMirror;

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
public class IGJVisitor extends BaseTypeVisitor<Void, Void> {
    IGJChecker checker;

    public IGJVisitor(IGJChecker checker, CompilationUnitTree root) {
        super(checker, root);
        this.checker = checker;
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
}
