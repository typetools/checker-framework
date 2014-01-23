package org.checkerframework.framework.base;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import javacutils.AnnotationUtils;

import checkers.util.MultiGraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

class QualifierHierarchyAdapter<Q> {
    private QualifierHierarchy<Q> underlying;
    private TypeMirrorConverter<Q> converter;

    public QualifierHierarchyAdapter(QualifierHierarchy<Q> underlying,
            TypeMirrorConverter<Q> converter) {
        this.underlying = underlying;
        this.converter = converter;
    }

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
            // This check is a hack to work around a particular case that shows
            // up when annotations are applied to type variables.  Here is an
            // example:
            //  - The checker framework goes to annotated a member variable
            //    whose type is an annotated type variable, such as
            //    '@MyQual T x;'
            //  - The checker framework calls the UserQualAnnotatedTypeFactory.
            //    fromMember method.
            //  - UQATF.fromMember calls super.fromMethod (which is
            //    BasicAnnotatedTypeFactory.fromMember)
            //  - BATF.fromMember (through many nested calls) builds the
            //    original AnnotatedTypeMirror (which is '@MyQual T'), and
            //    checks it against the upper and lower bounds for 'T' to
            //    ensure that it's valid.
            //  - The check involves a call to this isSubtype method, with one
            //    argument being the original @MyQual AnnotationMirror that the
            //    programmer wrote.
            //  - Without this check, LookupTable.get would throw an exception
            //    (due to receiving a non-Key argument).  With this check in
            //    place, checking continues, and eventually the @MyQual ATM is
            //    returned to the UQATF's fromMember method, where it is
            //    rewritten to have an appropriate @Key annotation instead.
            //
            // TODO: having this check probably causes us to skip some
            // important checks in the case where the type variable has
            // annotations on its upper/lower bounds.
            //
            // TODO(2013-12-18): figure out if this is still necessary after all
            // the recent userqual design changes
            if (!converter.isKey(rhs) || !converter.isKey(lhs))
                return false;

            Q rhsQual = converter.getQualifier(rhs);
            Q lhsQual = converter.getQualifier(lhs);


            return underlying.isSubtype(rhsQual, lhsQual);
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            Q q1 = converter.getQualifier(a1);
            Q q2 = converter.getQualifier(a2);
            return converter.getAnnotation(underlying.leastUpperBound(q1, q2));
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
            Q q1 = converter.getQualifier(a1);
            Q q2 = converter.getQualifier(a2);
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
