package org.checkerframework.checker.fenum;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.fenum.qual.Fenum;
import org.checkerframework.checker.fenum.qual.FenumBottom;
import org.checkerframework.checker.fenum.qual.FenumTop;
import org.checkerframework.checker.fenum.qual.FenumUnqualified;
import org.checkerframework.checker.fenum.qual.PolyFenum;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.ComplexHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.QualifierKindHierarchy;
import org.checkerframework.framework.util.QualifierKindHierarchy.QualifierKind;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

public class FenumAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    protected AnnotationMirror FENUM_UNQUALIFIED;
    protected AnnotationMirror FENUM, FENUM_BOTTOM, FENUM_TOP;

    public FenumAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        FENUM_BOTTOM = AnnotationBuilder.fromClass(elements, FenumBottom.class);
        FENUM = AnnotationBuilder.fromClass(elements, Fenum.class);
        FENUM_UNQUALIFIED = AnnotationBuilder.fromClass(elements, FenumUnqualified.class);
        FENUM_TOP = AnnotationBuilder.fromClass(elements, FenumTop.class);

        this.postInit();
    }

    /**
     * Copied from SubtypingChecker. Instead of returning an empty set if no "quals" option is
     * given, we return Fenum as the only qualifier.
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
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new FenumQualifierHierarchy(getSupportedTypeQualifiers(), elements);
    }

    protected static class FenumQualifierKindHierarchy extends QualifierKindHierarchy {

        public FenumQualifierKindHierarchy(
                Collection<Class<? extends Annotation>> qualifierClasses) {
            super(qualifierClasses);
        }

        @Override
        protected Map<QualifierKind, Set<QualifierKind>> initializeDirectSuperTypes() {
            Map<QualifierKind, Set<QualifierKind>> supersMap = super.initializeDirectSuperTypes();
            QualifierKind bottom =
                    getQualifierKindMap()
                            .get("org.checkerframework.checker.fenum.qual.FenumBottom");
            Set<QualifierKind> superTypes = supersMap.get(bottom);
            superTypes.addAll(supersMap.keySet());
            superTypes.remove(bottom);
            return supersMap;
        }
    }

    protected class FenumQualifierHierarchy extends ComplexHierarchy {
        private final QualifierKind FENUM_KIND;

        public FenumQualifierHierarchy(
                Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
            super(qualifierClasses, elements);
            this.FENUM_KIND =
                    this.qualifierKindHierarchy
                            .getQualifierKindMap()
                            .get(Fenum.class.getCanonicalName());
        }

        @Override
        protected QualifierKindHierarchy createQualifierKindHierarchy(
                Collection<Class<? extends Annotation>> qualifierClasses) {
            return new FenumQualifierKindHierarchy(qualifierClasses);
        }

        @Override
        protected boolean isSubtype(
                AnnotationMirror subAnno,
                QualifierKind subKind,
                AnnotationMirror superAnno,
                QualifierKind superKind) {
            return AnnotationUtils.areSame(subAnno, superAnno);
        }

        @Override
        protected AnnotationMirror leastUpperBound(
                AnnotationMirror a1,
                QualifierKind qual1,
                AnnotationMirror a2,
                QualifierKind qual2) {
            if (qual1 == FENUM_KIND && qual2 == FENUM_KIND) {
                return FENUM_TOP;
            } else if (qual1 == FENUM_KIND) {
                return a1;
            } else if (qual2 == FENUM_KIND) {
                return a2;
            }
            throw new BugInCF("Unexpected QualifierKinds %s %s", qual1, qual2);
        }

        @Override
        protected AnnotationMirror greatestLowerBound(
                AnnotationMirror a1,
                QualifierKind qual1,
                AnnotationMirror a2,
                QualifierKind qual2) {
            if (qual1 == FENUM_KIND && qual2 == FENUM_KIND) {
                return FENUM_BOTTOM;
            } else if (qual1 == FENUM_KIND) {
                return a2;
            } else if (qual2 == FENUM_KIND) {
                return a1;
            }
            throw new BugInCF("Unexpected QualifierKinds %s %s", qual1, qual2);
        }
    }
}
