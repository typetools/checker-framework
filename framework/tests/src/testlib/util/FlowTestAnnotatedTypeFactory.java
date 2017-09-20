package testlib.util;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.Bottom;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.qual.Unqualified;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

public class FlowTestAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    protected final AnnotationMirror VALUE, BOTTOM;

    public FlowTestAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);
        VALUE = AnnotationBuilder.fromClass(elements, Value.class);
        BOTTOM = AnnotationBuilder.fromClass(elements, Bottom.class);

        this.postInit();

        addTypeNameImplicit(java.lang.Void.class, BOTTOM);
        this.defaults.addCheckedCodeDefault(BOTTOM, TypeUseLocation.LOWER_BOUND);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<Class<? extends Annotation>>(
                Arrays.asList(
                        Value.class,
                        Odd.class,
                        MonotonicOdd.class,
                        Unqualified.class,
                        Bottom.class));
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        ImplicitsTreeAnnotator implicitsTreeAnnotator = new ImplicitsTreeAnnotator(this);
        implicitsTreeAnnotator.addTreeKind(com.sun.source.tree.Tree.Kind.NULL_LITERAL, BOTTOM);

        return new ListTreeAnnotator(new PropagationTreeAnnotator(this), implicitsTreeAnnotator);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new FlowQualifierHierarchy(factory, BOTTOM);
    }

    class FlowQualifierHierarchy extends GraphQualifierHierarchy {

        public FlowQualifierHierarchy(MultiGraphFactory f, AnnotationMirror bottom) {
            super(f, bottom);
        }

        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (AnnotationUtils.areSameIgnoringValues(superAnno, VALUE)
                    && AnnotationUtils.areSameIgnoringValues(subAnno, VALUE)) {
                return AnnotationUtils.areSame(superAnno, subAnno);
            }
            if (AnnotationUtils.areSameIgnoringValues(superAnno, VALUE)) {
                superAnno = VALUE;
            }
            if (AnnotationUtils.areSameIgnoringValues(subAnno, VALUE)) {
                subAnno = VALUE;
            }
            return super.isSubtype(subAnno, superAnno);
        }
    }
}
