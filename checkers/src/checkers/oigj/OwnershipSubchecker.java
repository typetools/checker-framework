package checkers.oigj;

import checkers.basetype.BaseTypeChecker;
import checkers.oigj.quals.Dominator;
import checkers.oigj.quals.Modifier;
import checkers.oigj.quals.O;
import checkers.oigj.quals.World;
import checkers.quals.TypeQualifiers;
import checkers.source.SuppressWarningsKeys;
import checkers.types.QualifierHierarchy;
import checkers.util.GraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

import javacutils.AnnotationUtils;
import javacutils.ErrorReporter;

import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

@TypeQualifiers({ Dominator.class, Modifier.class, World.class, O.class, OIGJMutabilityBottom.class })
@SuppressWarningsKeys({ "ownership", "oigj" })
public class OwnershipSubchecker extends BaseTypeChecker<OwnershipAnnotatedTypeFactory> {
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
            // TODO warn if bottom is not supported.
            super(factory, BOTTOM_QUAL);
        }

        @Override
        public boolean isSubtype(Collection<? extends AnnotationMirror> rhs, Collection<? extends AnnotationMirror> lhs) {
            if (lhs.isEmpty() || rhs.isEmpty()) {
                ErrorReporter.errorAbort("OwnershipQualifierHierarchy: Empty annotations in lhs: " + lhs + " or rhs: " + rhs);
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
