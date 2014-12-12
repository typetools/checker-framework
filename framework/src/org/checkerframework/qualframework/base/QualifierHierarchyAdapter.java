package org.checkerframework.qualframework.base;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.javacutil.AnnotationUtils;

import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

/**
 * An adapter for {@link QualifierHierarchy}, extending {@link
 * MultiGraphQualifierHierarchy}.
 *
 * Note that {@link QualifierHierarchyAdapter.Implementation} is the actual
 * {@link MultiGraphQualifierHierarchy} implementation, not {@link
 * QualifierHierarchyAdapter}.  To construct an instance, call:
 * <code>new QualifierHierarchyAdapter(underlying, converter).createImplementation(factory)</code>.
 */
/* We need this 'Implementation' silliness because MultiGraphQualifierHierarchy
 * calls some of its own methods from inside the constructor.  The call to the
 * 'super' constructor has to be the first statement in the subtype
 * constructor, so 'underlying' and 'converter' (which QHA methods rely on to
 * do their jobs) must be available *before* the constructor is called.  The
 * only good way I know of to do this is to put the necessary information in a
 * nonstatic inner class.  This requires presenting a slightly bizarre API to
 * users of QualifierHierarchyAdapter, but it's an internal ("package" access)
 * class, so that's not too much of a problem. */
class QualifierHierarchyAdapter<Q> {
    private QualifierHierarchy<Q> underlying;
    private TypeMirrorConverter<Q> converter;
    private AnnotationConverter<Q> annotationConverter;

    public QualifierHierarchyAdapter(AnnotationConverter<Q> annotationConverter,
            QualifierHierarchy<Q> underlying,
            TypeMirrorConverter<Q> converter) {
        this.annotationConverter = annotationConverter;
        this.underlying = underlying;
        this.converter = converter;
    }

    /**
     * Construct an instance of {@link
     * QualifierHierarchyAdapter.Implementation} using the parameters passed to
     * the {@link QualifierHierarchyAdapter} constructor.
     */
    public Implementation createImplementation(MultiGraphFactory f) {
        return new Implementation(f);
    }

    public class Implementation extends MultiGraphQualifierHierarchy {
        public Implementation(MultiGraphFactory f) {
            super(f);
        }

        @Override
        public int getWidth() {
            return 1;
        }

        @Override
        protected Set<AnnotationMirror>
        findBottoms(Map<AnnotationMirror, Set<AnnotationMirror>> supertypes) {
            Set<AnnotationMirror> newBottoms = AnnotationUtils.createAnnotationSet();
            newBottoms.add(getBottomAnnotation(null));
            return newBottoms;
        }

        @Override
        protected Set<AnnotationMirror>
        findTops(Map<AnnotationMirror, Set<AnnotationMirror>> supertypes) {
            Set<AnnotationMirror> newTops = AnnotationUtils.createAnnotationSet();
            newTops.add(getTopAnnotation(null));
            return newTops;
        }

        @Override
        public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
            return converter.getAnnotation(underlying.getBottom());
        }

        @Override
        public AnnotationMirror getTopAnnotation(AnnotationMirror start) {
            return converter.getAnnotation(underlying.getTop());
        }

        @Override
        public AnnotationMirror getAnnotationInHierarchy(Collection<? extends AnnotationMirror> annos, AnnotationMirror top) {
            for (AnnotationMirror anno : annos) {
                if (converter.isKey(anno)) {
                    return anno;
                }
            }
            return null;
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {

            // Dataflow and Propagation tree annotator will sometimes call this method
            // with ATMs that do not have a qualifier(@Key) yet. In that case we must
            // do the conversion here.
            Q rhsQual = getOrCreateQualifier(rhs);
            Q lhsQual = getOrCreateQualifier(lhs);

            if (rhs == null || lhs == null) {
                return false;
            }

            return underlying.isSubtype(rhsQual, lhsQual);
        }

        /**
         * This method looks up the qualifier for AnnotatedTypeMirror using its @Key
         * annotation. If no @Key annotation is present, converter is used to
         * create a qualifier based on the annotation on mirror.
         *
         * @param mirror the AnnotationMirror to create a qualifier from
         * @return the resulting qualifier
         */
        private Q getOrCreateQualifier(AnnotationMirror mirror) {
            Q rhsQual;
            if (!converter.isKey(mirror)) {
                rhsQual = annotationConverter.fromAnnotations(Arrays.asList(mirror));
            } else {
                rhsQual = converter.getQualifier(mirror);
            }
            return rhsQual;
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            Q q1 = getOrCreateQualifier(a1);
            Q q2 = getOrCreateQualifier(a2);
            return converter.getAnnotation(underlying.leastUpperBound(q1, q2));
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
            Q q1 = getOrCreateQualifier(a1);
            Q q2 = getOrCreateQualifier(a2);
            return converter.getAnnotation(underlying.greatestLowerBound(q1, q2));
        }

        @Override
        public Set<AnnotationMirror> getTypeQualifiers() {
            Set<AnnotationMirror> names = new HashSet<AnnotationMirror>();
            names.add(converter.getBlankKeyAnnotation());
            return names;
        }
    }
}
