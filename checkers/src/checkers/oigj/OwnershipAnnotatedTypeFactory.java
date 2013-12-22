package checkers.oigj;

import checkers.basetype.BaseAnnotatedTypeFactory;
import checkers.basetype.BaseTypeChecker;
import checkers.oigj.quals.World;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.QualifierHierarchy;
import checkers.types.TreeAnnotator;
import checkers.types.TypeAnnotator;
import checkers.util.GraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

import javacutils.AnnotationUtils;
import javacutils.ErrorReporter;
import javacutils.TypesUtils;

import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

import com.sun.source.tree.BinaryTree;

public class OwnershipAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    protected final AnnotationMirror BOTTOM_QUAL;

    public OwnershipAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        BOTTOM_QUAL = AnnotationUtils.fromClass(elements, OIGJMutabilityBottom.class);
        this.postInit();
    }


    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new OwnershipTreeAnnotator(this);
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new OwnershipTypeAnnotator(this);
    }

    private class OwnershipTypeAnnotator extends TypeAnnotator {

        public OwnershipTypeAnnotator(OwnershipAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Element elem) {
            if (type.isAnnotatedInHierarchy(BOTTOM_QUAL))
                return super.visitDeclared(type, elem);

            if (elem != null &&
                    elem.getKind() == ElementKind.CLASS &&
                    TypesUtils.isObject(type.getUnderlyingType()))
                type.addAnnotation(World.class);
            return super.visitDeclared(type, elem);
        }
    }

    private class OwnershipTreeAnnotator extends TreeAnnotator {
        public OwnershipTreeAnnotator(OwnershipAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(BOTTOM_QUAL);
            return super.visitBinary(node, type);
        }
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
