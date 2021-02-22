package org.checkerframework.framework.testchecker.util;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.subtyping.qual.Bottom;
import org.checkerframework.common.subtyping.qual.Unqualified;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.MostlyNoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.DefaultQualifierKindHierarchy;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.framework.util.QualifierKindHierarchy;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

public class FlowTestAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    protected final AnnotationMirror VALUE, BOTTOM, TOP;

    public FlowTestAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);
        VALUE = AnnotationBuilder.fromClass(elements, Value.class);
        BOTTOM = AnnotationBuilder.fromClass(elements, Bottom.class);
        TOP = AnnotationBuilder.fromClass(elements, Unqualified.class);

        this.postInit();
    }

    @Override
    protected void addCheckedCodeDefaults(QualifierDefaults defs) {
        defs.addCheckedCodeDefault(BOTTOM, TypeUseLocation.LOWER_BOUND);
        defs.addCheckedCodeDefault(TOP, TypeUseLocation.OTHERWISE);
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
    @SuppressWarnings("deprecation") // TODO: REVERT: Just testing backward compatibility.
    public QualifierHierarchy createQualifierHierarchy() {
        return org.checkerframework.framework.util.MultiGraphQualifierHierarchy
                .createMultiGraphQualifierHierarchy(this);
    }

    @Override
    @SuppressWarnings("deprecation") // TODO: REVERT: Just testing backward compatibility.
    public QualifierHierarchy createQualifierHierarchyWithMultiGraphFactory(
            org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory
                    factory) {
        return new OldFlowQualifierHierarchy(factory, BOTTOM);
    }

    @SuppressWarnings("deprecation") // TODO: REVERT: Just testing backward compatibility.
    class OldFlowQualifierHierarchy
            extends org.checkerframework.framework.util.GraphQualifierHierarchy {

        public OldFlowQualifierHierarchy(MultiGraphFactory f, AnnotationMirror bottom) {
            super(f, bottom);
        }

        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (AnnotationUtils.areSameByName(superAnno, VALUE)
                    && AnnotationUtils.areSameByName(subAnno, VALUE)) {
                return AnnotationUtils.areSame(superAnno, subAnno);
            }
            if (AnnotationUtils.areSameByName(superAnno, VALUE)) {
                superAnno = VALUE;
            }
            if (AnnotationUtils.areSameByName(subAnno, VALUE)) {
                subAnno = VALUE;
            }
            return super.isSubtype(subAnno, superAnno);
        }
    }

    //    @Override
    //    protected QualifierHierarchy createQualifierHierarchy() {
    //        return new FlowQualifierHierarchy(this.getSupportedTypeQualifiers(), elements);
    //    }

    /** FlowQualifierHierarchy: {@code @Value(a) <: @Value(b) iff a == b} */
    class FlowQualifierHierarchy extends MostlyNoElementQualifierHierarchy {
        final QualifierKind VALUE_KIND;

        /**
         * Creates a FlowQualifierHierarchy from the given classes.
         *
         * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
         * @param elements element utils
         */
        public FlowQualifierHierarchy(
                Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
            super(qualifierClasses, elements);
            this.VALUE_KIND = getQualifierKind(VALUE);
        }

        @Override
        protected QualifierKindHierarchy createQualifierKindHierarchy(
                Collection<Class<? extends Annotation>> qualifierClasses) {
            return new DefaultQualifierKindHierarchy(qualifierClasses, Bottom.class);
        }

        @Override
        protected boolean isSubtypeWithElements(
                AnnotationMirror subAnno,
                QualifierKind subKind,
                AnnotationMirror superAnno,
                QualifierKind superKind) {
            return AnnotationUtils.areSame(superAnno, subAnno);
        }

        @Override
        protected AnnotationMirror leastUpperBoundWithElements(
                AnnotationMirror a1,
                QualifierKind qualifierKind1,
                AnnotationMirror a2,
                QualifierKind qualifierKind2,
                QualifierKind lubKind) {
            if (qualifierKind1 == qualifierKind2) {
                // Both are Value
                if (AnnotationUtils.areSame(a1, a2)) {
                    return a1;
                } else {
                    return TOP;
                }
            } else if (qualifierKind1 == VALUE_KIND) {
                return a1;
            } else if (qualifierKind2 == VALUE_KIND) {
                return a2;
            }
            throw new BugInCF(
                    "Unexpected annotations: leastUpperBoundWithElements(%s, %s)", a1, a2);
        }

        @Override
        protected AnnotationMirror greatestLowerBoundWithElements(
                AnnotationMirror a1,
                QualifierKind qualifierKind1,
                AnnotationMirror a2,
                QualifierKind qualifierKind2,
                QualifierKind glbKind) {
            if (qualifierKind1 == qualifierKind2) {
                // Both are Value
                if (AnnotationUtils.areSame(a1, a2)) {
                    return a1;
                } else {
                    return BOTTOM;
                }
            } else if (qualifierKind1 == VALUE_KIND) {
                return a1;
            } else if (qualifierKind2 == VALUE_KIND) {
                return a2;
            }
            throw new BugInCF(
                    "Unexpected annotations: greatestLowerBoundWithElements(%s, %s)", a1, a2);
        }
    }
}
