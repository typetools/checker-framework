package org.checkerframework.checker.fenum;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.checker.fenum.qual.Fenum;
import org.checkerframework.checker.fenum.qual.FenumBottom;
import org.checkerframework.checker.fenum.qual.FenumTop;
import org.checkerframework.checker.fenum.qual.FenumUnqualified;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;

public class FenumAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    protected AnnotationMirror FENUM_UNQUALIFIED;
    protected AnnotationMirror FENUM, FENUM_BOTTOM;

    public FenumAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        FENUM_BOTTOM = AnnotationUtils.fromClass(elements, FenumBottom.class);
        FENUM = AnnotationUtils.fromClass(elements, Fenum.class);
        FENUM_UNQUALIFIED = AnnotationUtils.fromClass(elements, FenumUnqualified.class);

        this.postInit();
        // flow.setDebug(System.err);
        defaults.addAbsoluteDefault(FENUM_BOTTOM, DefaultLocation.LOWER_BOUNDS);
    }

    /** Copied from SubtypingChecker.
     * Instead of returning an empty set if no "quals" option is given,
     * we return Fenum as the only qualifier.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        Set<Class<? extends Annotation>> qualSet =
              new HashSet<Class<? extends Annotation>>();

        String qualNames = checker.getOption("quals");
        if (qualNames == null) {
            // maybe issue a warning?
        } else {
            for (String qualName : qualNames.split(",")) {
                try {
                    final Class<? extends Annotation> q =
                            (Class<? extends Annotation>) Class.forName(qualName);
                    qualSet.add(q);
                } catch (ClassNotFoundException e) {
                    checker.errorAbort("FenumChecker: could not load class for qualifier: " + qualName + "; ensure that your classpath is correct.");
                }
            }
        }
        qualSet.add(FenumTop.class);
        qualSet.add(Fenum.class);
        qualSet.add(FenumUnqualified.class);
        qualSet.add(FenumBottom.class);

        // Also call super. This way a subclass can use the
        // @TypeQualifiers annotation again.
        qualSet.addAll(super.createSupportedTypeQualifiers());

        // TODO: warn if no qualifiers given?
        // Just Fenum("..") is still valid, though...
        return Collections.unmodifiableSet(qualSet);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new FenumQualifierHierarchy(factory);
    }

    protected class FenumQualifierHierarchy extends GraphQualifierHierarchy {

        /* The user is expected to introduce additional fenum annotations.
         * These annotations are declared to be subtypes of FenumTop, using the
         * @SubtypeOf annotation.
         * However, there is no way to declare that it is a supertype of Bottom.
         * Therefore, we use the constructor of GraphQualifierHierarchy that allows
         * us to set a dedicated bottom qualifier.
         */
        public FenumQualifierHierarchy(MultiGraphFactory factory) {
            super(factory, FENUM_BOTTOM);
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(lhs, FENUM) &&
                    AnnotationUtils.areSameIgnoringValues(rhs, FENUM)) {
                return AnnotationUtils.areSame(lhs, rhs);
            }
            // Ignore annotation values to ensure that annotation is in supertype map.
            if (AnnotationUtils.areSameIgnoringValues(lhs, FENUM)) {
                lhs = FENUM;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, FENUM)) {
                rhs = FENUM;
            }
            return super.isSubtype(rhs, lhs);
        }
    }

}
