package checkers.oigj;

import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import checkers.basetype.BaseTypeChecker;
import checkers.oigj.quals.*;
import checkers.quals.TypeQualifiers;
import checkers.source.SourceChecker;
import checkers.types.QualifierHierarchy;
import checkers.util.AnnotationUtils;
import checkers.util.GraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

@TypeQualifiers({ Dominator.class, Modifier.class, World.class, O.class})
public class OwnershipSubchecker extends BaseTypeChecker {
    protected AnnotationMirror BOTTOM_QUAL;

    @Override
    public void initChecker() {
        Elements elements = processingEnv.getElementUtils();
        BOTTOM_QUAL = AnnotationUtils.fromClass(elements, OIGJMutabilityBottom.class);
        super.initChecker();
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new OwnershipQualifierHierarchy(factory);
    }

    private final class OwnershipQualifierHierarchy extends GraphQualifierHierarchy {
        public OwnershipQualifierHierarchy(MultiGraphFactory factory) {
            super(factory, BOTTOM_QUAL);
        }

        @Override
        public boolean isSubtype(Collection<AnnotationMirror> rhs, Collection<AnnotationMirror> lhs) {
            if (lhs.isEmpty() || rhs.isEmpty()) {
                SourceChecker.errorAbort("GraphQualifierHierarchy: Empty annotations in lhs: " + lhs + " or rhs: " + rhs);
            }
            // TODO: sometimes there are multiple mutability annotations in a type and
            // the check in the superclass that the sets contain exactly one annotation
            // fails. I replaced "addAnnotation" calls with "replaceAnnotation" calls,
            // but then other test cases fail. Some love needed here.
            for (AnnotationMirror lhsAnno : lhs) {
                for (AnnotationMirror rhsAnno : rhs) {
                    if (isSubtype(rhsAnno, lhsAnno)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
