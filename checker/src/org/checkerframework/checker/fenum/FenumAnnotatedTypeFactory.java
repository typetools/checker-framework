package org.checkerframework.checker.fenum;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.checker.fenum.qual.Fenum;
import org.checkerframework.checker.fenum.qual.FenumBottom;
import org.checkerframework.checker.fenum.qual.FenumTop;
import org.checkerframework.checker.fenum.qual.FenumUnqualified;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
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
    }

    /** Copied from SubtypingChecker.
     * Instead of returning an empty set if no "quals" option is given,
     * we return Fenum as the only qualifier.
     */
    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        AnnotationClassLoader loader = new AnnotationClassLoader(checker);

        Set<Class<? extends Annotation>> qualSet = new LinkedHashSet<Class<? extends Annotation>>();

        // Load externally defined quals given in the -Aquals and/or -AqualDirs options
        String qualNames = checker.getOption("quals");
        String qualDirectories = checker.getOption("qualDirs");

        // load individually named qualifiers
        if (qualNames != null) {
            for (String qualName : qualNames.split(",")) {
                qualSet.add(loader.loadExternalAnnotationClass(qualName));
            }
        }

        // load directories of qualifiers
        if (qualDirectories != null) {
            for (String dirName : qualDirectories.split(":")) {
                qualSet.addAll(loader.loadExternalAnnotationClassesFromDirectory(dirName));
            }
        }

        // Load top, bottom, unqualified, and fake enum
        qualSet.add(FenumTop.class);
        qualSet.add(Fenum.class);
        qualSet.add(FenumUnqualified.class);
        qualSet.add(FenumBottom.class);

        // Also call super to load everything in qual directory
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
