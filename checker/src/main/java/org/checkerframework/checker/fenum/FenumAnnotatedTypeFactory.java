package org.checkerframework.checker.fenum;

import org.checkerframework.checker.fenum.qual.Fenum;
import org.checkerframework.checker.fenum.qual.FenumBottom;
import org.checkerframework.checker.fenum.qual.FenumTop;
import org.checkerframework.checker.fenum.qual.FenumUnqualified;
import org.checkerframework.checker.fenum.qual.PolyFenum;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.MostlyNoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.DefaultQualifierKindHierarchy;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.framework.util.QualifierKindHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.UserError;
import org.plumelib.reflection.Signatures;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

/** The type factory for the Fenum Checker. */
public class FenumAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** AnnotationMirror for {@link FenumUnqualified}. */
    protected AnnotationMirror FENUM_UNQUALIFIED;
    /** AnnotationMirror for {@link FenumBottom}. */
    protected AnnotationMirror FENUM_BOTTOM;
    /** AnnotationMirror for {@link FenumTop}. */
    protected AnnotationMirror FENUM_TOP;

    /**
     * Create a FenumAnnotatedTypeFactory.
     *
     * @param checker checker
     */
    public FenumAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        FENUM_BOTTOM = AnnotationBuilder.fromClass(elements, FenumBottom.class);
        FENUM_UNQUALIFIED = AnnotationBuilder.fromClass(elements, FenumUnqualified.class);
        FENUM_TOP = AnnotationBuilder.fromClass(elements, FenumTop.class);

        this.postInit();
    }

    /**
     * Copied from SubtypingChecker. Instead of returning an empty set if no "quals" option is
     * given, we return Fenum as the only qualifier.
     *
     * @return the supported type qualifiers
     */
    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        // Load everything in qual directory, and top, bottom, unqualified, and fake enum
        Set<Class<? extends Annotation>> qualSet =
                getBundledTypeQualifiers(
                        FenumTop.class,
                        Fenum.class,
                        FenumUnqualified.class,
                        FenumBottom.class,
                        PolyFenum.class);

        // Load externally defined quals given in the -Aquals and/or -AqualDirs options
        String qualNames = checker.getOption("quals");
        String qualDirectories = checker.getOption("qualDirs");

        // load individually named qualifiers
        if (qualNames != null) {
            for (String qualName : qualNames.split(",")) {
                if (!Signatures.isBinaryName(qualName)) {
                    throw new UserError(
                            "Malformed qualifier \"%s\" in -Aquals=%s", qualName, qualNames);
                }
                qualSet.add(loader.loadExternalAnnotationClass(qualName));
            }
        }

        // load directories of qualifiers
        if (qualDirectories != null) {
            for (String dirName : qualDirectories.split(":")) {
                qualSet.addAll(loader.loadExternalAnnotationClassesFromDirectory(dirName));
            }
        }

        // TODO: warn if no qualifiers given?
        // Just Fenum("..") is still valid, though...
        return qualSet;
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy() {
        return new FenumQualifierHierarchy(getSupportedTypeQualifiers(), elements);
    }

    /** Fenum qualifier hierarchy. */
    protected class FenumQualifierHierarchy extends MostlyNoElementQualifierHierarchy {

        /** QualifierKind for {@link Fenum} qualifier. */
        private final QualifierKind FENUM_KIND;

        /**
         * Creates FenumQualifierHierarchy.
         *
         * @param qualifierClasses qualifier classes
         * @param elements element utils
         */
        public FenumQualifierHierarchy(
                Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
            super(qualifierClasses, elements);
            this.FENUM_KIND =
                    this.qualifierKindHierarchy.getQualifierKind(Fenum.class.getCanonicalName());
        }

        @Override
        protected QualifierKindHierarchy createQualifierKindHierarchy(
                @UnderInitialization FenumQualifierHierarchy this,
                Collection<Class<? extends Annotation>> qualifierClasses) {
            return new DefaultQualifierKindHierarchy(qualifierClasses, FenumBottom.class);
        }

        @Override
        protected boolean isSubtypeWithElements(
                AnnotationMirror subAnno,
                QualifierKind subKind,
                AnnotationMirror superAnno,
                QualifierKind superKind) {
            return AnnotationUtils.areSame(subAnno, superAnno);
        }

        @Override
        protected AnnotationMirror leastUpperBoundWithElements(
                AnnotationMirror a1,
                QualifierKind qualifierKind1,
                AnnotationMirror a2,
                QualifierKind qualifierKind2,
                QualifierKind lubKind) {
            if (qualifierKind1 == FENUM_KIND && qualifierKind2 == FENUM_KIND) {
                if (AnnotationUtils.areSame(a1, a2)) {
                    return a1;
                }
                return FENUM_TOP;
            } else if (qualifierKind1 == FENUM_KIND) {
                return a1;
            } else if (qualifierKind2 == FENUM_KIND) {
                return a2;
            }
            throw new TypeSystemError(
                    "Unexpected QualifierKinds %s %s", qualifierKind1, qualifierKind2);
        }

        @Override
        protected AnnotationMirror greatestLowerBoundWithElements(
                AnnotationMirror a1,
                QualifierKind qualifierKind1,
                AnnotationMirror a2,
                QualifierKind qualifierKind2,
                QualifierKind glbKind) {
            if (qualifierKind1 == FENUM_KIND && qualifierKind2 == FENUM_KIND) {
                return FENUM_BOTTOM;
            } else if (qualifierKind1 == FENUM_KIND) {
                return a2;
            } else if (qualifierKind2 == FENUM_KIND) {
                return a1;
            }
            throw new TypeSystemError(
                    "Unexpected QualifierKinds %s %s", qualifierKind1, qualifierKind2);
        }
    }
}
