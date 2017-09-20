package testlib.flowexpression;

import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.dependenttypes.DependentTypesHelper;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import testlib.flowexpression.qual.FEBot;
import testlib.flowexpression.qual.FETop;

public class FlowExpressionAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    private AnnotationMirror TOP, BOTTOM;

    public FlowExpressionAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        TOP = AnnotationBuilder.fromClass(elements, FETop.class);
        BOTTOM = AnnotationBuilder.fromClass(elements, FEBot.class);
        postInit();
    }

    @Override
    protected DependentTypesHelper createDependentTypesHelper() {
        return new DependentTypesHelper(this);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new FlowExpressionQualifierHierarchy(factory);
    }

    private class FlowExpressionQualifierHierarchy extends GraphQualifierHierarchy {
        public FlowExpressionQualifierHierarchy(MultiGraphFactory f) {
            super(f, BOTTOM);
        }

        @Override
        public boolean isSubtype(AnnotationMirror subtype, AnnotationMirror supertype) {
            if (AnnotationUtils.areSameByClass(supertype, FETop.class)
                    || AnnotationUtils.areSameByClass(subtype, FEBot.class)) {
                return true;
            }
            if (AnnotationUtils.areSameByClass(subtype, FETop.class)
                    || AnnotationUtils.areSameByClass(supertype, FEBot.class)) {
                return false;
            }
            List<String> subtypeExpressions =
                    AnnotationUtils.getElementValueArray(subtype, "value", String.class, true);
            List<String> supertypeExpressions =
                    AnnotationUtils.getElementValueArray(supertype, "value", String.class, true);
            return subtypeExpressions.containsAll(supertypeExpressions)
                    && supertypeExpressions.containsAll(subtypeExpressions);
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (isSubtype(a1, a2)) {
                return a2;
            }

            if (isSubtype(a2, a1)) {
                return a1;
            }
            List<String> a1Expressions =
                    AnnotationUtils.getElementValueArray(a1, "value", String.class, true);
            List<String> a2Expressions =
                    AnnotationUtils.getElementValueArray(a2, "value", String.class, true);
            if (a1Expressions.containsAll(a2Expressions)
                    && a2Expressions.containsAll(a1Expressions)) {
                return a1;
            }
            return TOP;
        }
    }
}
