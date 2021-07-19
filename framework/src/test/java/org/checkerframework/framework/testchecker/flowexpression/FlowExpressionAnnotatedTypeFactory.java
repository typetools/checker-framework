package org.checkerframework.framework.testchecker.flowexpression;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.flowexpression.qual.FEBottom;
import org.checkerframework.framework.testchecker.flowexpression.qual.FETop;
import org.checkerframework.framework.type.MostlyNoElementQualifierHierarchy;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.framework.util.dependenttypes.DependentTypesHelper;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

public class FlowExpressionAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    private AnnotationMirror TOP, BOTTOM;

    public FlowExpressionAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        TOP = AnnotationBuilder.fromClass(elements, FETop.class);
        BOTTOM = AnnotationBuilder.fromClass(elements, FEBottom.class);
        postInit();
    }

    @Override
    protected DependentTypesHelper createDependentTypesHelper() {
        return new DependentTypesHelper(this);
    }

    @Override
    protected FlowExpressionQualifierHierarchy createQualifierHierarchy() {
        return new FlowExpressionQualifierHierarchy(this.getSupportedTypeQualifiers(), elements);
    }

    private class FlowExpressionQualifierHierarchy extends MostlyNoElementQualifierHierarchy {

        /**
         * Create a {@code FlowExpressionQualifierHierarchy}.
         *
         * @param qualifierClasses classes of annotations that are the qualifiers
         * @param elements element utils
         */
        public FlowExpressionQualifierHierarchy(
                Set<Class<? extends Annotation>> qualifierClasses, Elements elements) {
            super(qualifierClasses, elements);
        }

        @Override
        protected boolean isSubtypeWithElements(
                AnnotationMirror subAnno,
                QualifierKind subKind,
                AnnotationMirror superAnno,
                QualifierKind superKind) {
            List<String> subtypeExpressions =
                    AnnotationUtils.getElementValueArray(subAnno, "value", String.class, false);
            List<String> supertypeExpressions =
                    AnnotationUtils.getElementValueArray(superAnno, "value", String.class, false);
            return subtypeExpressions.containsAll(supertypeExpressions)
                    && supertypeExpressions.containsAll(subtypeExpressions);
        }

        @Override
        protected AnnotationMirror leastUpperBoundWithElements(
                AnnotationMirror a1,
                QualifierKind qualifierKind1,
                AnnotationMirror a2,
                QualifierKind qualifierKind2,
                QualifierKind lubKind) {
            if (qualifierKind1.getName() == FEBottom.class.getCanonicalName()) {
                return a2;
            } else if (qualifierKind2.getName() == FEBottom.class.getCanonicalName()) {
                return a1;
            }
            List<String> a1Expressions =
                    AnnotationUtils.getElementValueArray(a1, "value", String.class, false);
            List<String> a2Expressions =
                    AnnotationUtils.getElementValueArray(a2, "value", String.class, false);
            if (a1Expressions.containsAll(a2Expressions)
                    && a2Expressions.containsAll(a1Expressions)) {
                return a1;
            }
            return TOP;
        }

        @Override
        protected AnnotationMirror greatestLowerBoundWithElements(
                AnnotationMirror a1,
                QualifierKind qualifierKind1,
                AnnotationMirror a2,
                QualifierKind qualifierKind2,
                QualifierKind glbKind) {
            if (qualifierKind1.getName() == FETop.class.getCanonicalName()) {
                return a2;
            } else if (qualifierKind2.getName() == FETop.class.getCanonicalName()) {
                return a1;
            }
            List<String> a1Expressions =
                    AnnotationUtils.getElementValueArray(a1, "value", String.class, false);
            List<String> a2Expressions =
                    AnnotationUtils.getElementValueArray(a2, "value", String.class, false);
            if (a1Expressions.containsAll(a2Expressions)
                    && a2Expressions.containsAll(a1Expressions)) {
                return a1;
            }
            return BOTTOM;
        }
    }
}
