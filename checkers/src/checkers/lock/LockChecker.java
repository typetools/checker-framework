package checkers.lock;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import checkers.basetype.BaseTypeChecker;
import checkers.lock.quals.GuardedBy;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.types.QualifierHierarchy;
import checkers.util.AnnotationUtils;
import checkers.util.GraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy;

/**
 * A typechecker plug-in for the JCIP type system qualifier that finds (and
 * verifies the absence of) locking and concurrency errors.
 *
 * @see GuardedBy
 */
@TypeQualifiers( { GuardedBy.class, Unqualified.class } )
public class LockChecker extends BaseTypeChecker {

    protected AnnotationMirror GUARDEDBY, UNQUALIFIED;

    @Override
    public void initChecker() {
        Elements elements = processingEnv.getElementUtils();
        GUARDEDBY = AnnotationUtils.fromClass(elements, GuardedBy.class);
        UNQUALIFIED = AnnotationUtils.fromClass(elements, Unqualified.class);

        super.initChecker();
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory ignorefactory) {
        MultiGraphQualifierHierarchy.MultiGraphFactory factory = createQualifierHierarchyFactory();

        factory.addQualifier(GUARDEDBY);
        factory.addQualifier(UNQUALIFIED);
        factory.addSubtype(UNQUALIFIED, GUARDEDBY);

        return new LockQualifierHierarchy(factory);
    }

    private final class LockQualifierHierarchy extends GraphQualifierHierarchy {

        public LockQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
            super(factory, UNQUALIFIED);
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(rhs, UNQUALIFIED)
                    && AnnotationUtils.areSameIgnoringValues(lhs, GUARDEDBY)) {
                return true;
            }
            // Ignore annotation values to ensure that annotation is in supertype map.
            if (AnnotationUtils.areSameIgnoringValues(lhs, GUARDEDBY)) {
                lhs = GUARDEDBY;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, GUARDEDBY)) {
                rhs = GUARDEDBY;
            }
            return super.isSubtype(rhs, lhs);
        }
    }
}
