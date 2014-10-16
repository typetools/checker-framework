package org.checkerframework.qualframework.base;

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

    public QualifierHierarchyAdapter(QualifierHierarchy<Q> underlying,
            TypeMirrorConverter<Q> converter) {
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
            // TODO: Clean up this nonsense.
            //
            // 2014-03-21: Commenting out this check currently causes only one
            // test failure (tests/tainting/ExtendsAndAnnotation.java) with the
            // expected "@Key annotations contains no index()" error.  I'm not
            // sure if the explanation given below is still accurate.
            //
            // ---
            //
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
            // Having this check might cause us to skip some important checks
            // in the case where the type variable has annotations on its
            // upper/lower bounds.
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
