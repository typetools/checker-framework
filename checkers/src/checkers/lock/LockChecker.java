package checkers.lock;

import javax.lang.model.element.AnnotationMirror;

import checkers.basetype.BaseTypeChecker;
import checkers.lock.quals.GuardedBy;
import checkers.types.QualifierHierarchy;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.AnnotationUtils;
import checkers.util.GraphQualifierHierarchy;

/**
 * A typechecker plug-in for the JCIP type system qualifier that finds (and
 * verifies the absence of) locking and concurrency errors.
 *
 * @see GuardedBy
 */
public class LockChecker extends BaseTypeChecker {

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);

        AnnotationMirror guardedBy = annoFactory.fromClass(GuardedBy.class);
        AnnotationMirror unqualified = null;

        GraphQualifierHierarchy.Factory factory=
            new GraphQualifierHierarchy.Factory();

        factory.addQualifier(guardedBy);
        factory.addQualifier(unqualified);
        factory.addSubtype(unqualified, guardedBy);

        return factory.build();
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType) {
        return true;
    }
}
