package checkers.commitment;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Name;

import checkers.basetype.BaseTypeChecker;
import checkers.commitment.quals.Committed;
import checkers.commitment.quals.FBCBottom;
import checkers.commitment.quals.Free;
import checkers.commitment.quals.NotOnlyCommitted;
import checkers.commitment.quals.Unclassified;
import checkers.quals.TypeQualifiers;
import checkers.source.SourceChecker;
import checkers.types.QualifierHierarchy;
import checkers.util.AnnotationUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

@TypeQualifiers({ Free.class, Committed.class, Unclassified.class,
        FBCBottom.class })
public abstract class CommitmentChecker extends BaseTypeChecker {

    /** Annotation constants */
    protected AnnotationMirror COMMITTED, FREE, UNCLASSIFIED, FBCBOTTOM,
            NOT_ONLY_COMMITTED;

    @Override
    public void initChecker(ProcessingEnvironment processingEnv) {
        AnnotationUtils annoFactory = AnnotationUtils
                .getInstance(processingEnv);
        COMMITTED = annoFactory.fromClass(Committed.class);
        FREE = annoFactory.fromClass(Free.class);
        UNCLASSIFIED = annoFactory.fromClass(Unclassified.class);
        FBCBOTTOM = annoFactory.fromClass(FBCBottom.class);
        NOT_ONLY_COMMITTED = annoFactory.fromClass(NotOnlyCommitted.class);

        super.initChecker(processingEnv);
    }

    /**
     * @return The list of type annotations of the commitment type system (i.e.,
     *         not including declaration annotations like {@link CommmittedOnly}
     *         or {@link NotOnlyCommitted}).
     */
    public Set<AnnotationMirror> getCommitmentAnnotations() {
        Set<AnnotationMirror> result = new HashSet<>();
        result.add(FREE);
        result.add(COMMITTED);
        result.add(UNCLASSIFIED);
        result.add(FBCBOTTOM);
        return result;
    }

    /*
     * The following method can be used to appropriately configure the
     * commitment type-system.
     */

    /**
     * @return The list of annotations that is forbidden for the constructor
     *         return type.
     */
    public Set<AnnotationMirror> getInvalidConstructorReturnTypeAnnotations() {
        return getCommitmentAnnotations();
    }

    /**
     * Returns the annotation that makes up the invariant of this commitment
     * type system.
     */
    public abstract AnnotationMirror getFieldInvariantAnnotation();

    /**
     * Returns a list of all fields of the given class
     */
    public static Set<VariableTree> getAllFields(ClassTree clazz) {
        Set<VariableTree> fields = new HashSet<>();
        for (Tree t : clazz.getMembers()) {
            if (t.getKind().equals(Tree.Kind.VARIABLE)) {
                VariableTree vt = (VariableTree) t;
                fields.add(vt);
            }
        }
        return fields;
    }

    /**
     * Returns the {@link QualifierHierarchy} of the child type system.
     */
    protected abstract QualifierHierarchy getChildQualifierHierarchy();

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new InitializationQualifierHierarchy();
    }

    /**
     * The {@link QualifierHierarchy} for the initialization type system. This
     * hierarchy also includes the child type system, whose hierarchy is
     * provided through {@link #getChildQualifierHierarchy()}.
     */
    protected class InitializationQualifierHierarchy extends QualifierHierarchy {

        protected Set<AnnotationMirror> tops;
        protected Set<AnnotationMirror> bottoms;
        protected Set<Name> typeQualifiers;
        protected QualifierHierarchy childHierarchy = getChildQualifierHierarchy();

        public InitializationQualifierHierarchy() {
            super(CommitmentChecker.this);

            AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);

            tops = new HashSet<>();
            tops.add(UNCLASSIFIED);
            tops.addAll(childHierarchy.getTopAnnotations());

            bottoms = new HashSet<>();
            bottoms.add(annoFactory.fromClass(FBCBottom.class));
            bottoms.addAll(childHierarchy.getBottomAnnotations());
        }

        @Override
        public Set<AnnotationMirror> getTopAnnotations() {
            return tops;
        }

        @Override
        public AnnotationMirror getTopAnnotation(AnnotationMirror start) {
            for (AnnotationMirror top : tops) {
                if (AnnotationUtils.areSame(start, top)
                        || isSubtype(start, top)) {
                    return top;
                }
            }
            assert false : "invalid start annotation provided (" + start + ")";
            return null;
        }

        @Override
        public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
            for (AnnotationMirror bot : bottoms) {
                if (AnnotationUtils.areSame(start, bot)
                        || isSubtype(bot, start)) {
                    return bot;
                }
            }
            assert false : "invalid start annotation provided (" + start + ")";
            return null;
        }

        @Override
        public Set<AnnotationMirror> getBottomAnnotations() {
            return bottoms;
        }

        @Override
        public Set<AnnotationMirror> getAnnotations() {
            assert false : "the set of annotations is very large and hard to compute";
            return null;
        }

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public Set<Name> getTypeQualifiers() {
            if (typeQualifiers != null)
                return typeQualifiers;
            Set<Name> names = new HashSet<>();
            Set<Class<?>> clazzes = new HashSet<>();
            clazzes.add(Free.class);
            clazzes.add(Unclassified.class);
            clazzes.add(Committed.class);
            clazzes.add(FBCBottom.class);
            // Add qualifiers from the initialization type system.
            for (Class<?> clazz : clazzes) {
                AnnotationMirror anno = AnnotationUtils.getInstance(env)
                        .fromClass((Class) clazz);
                names.add(AnnotationUtils.annotationName(anno));
            }
            // Add qualifiers from the child type system.
            for (AnnotationMirror anno : childHierarchy.getAnnotations()) {
                names.add(AnnotationUtils.annotationName(anno));
            }
            typeQualifiers = names;
            return typeQualifiers;
        }

        @Override
        public boolean isSubtype(AnnotationMirror anno1, AnnotationMirror anno2) {
            boolean isChild1 = isChildAnnotation(anno1);
            boolean isChild2 = isChildAnnotation(anno2);
            if (isChild1 && isChild2) {
                return childHierarchy.isSubtype(anno1, anno2);
            }
            if (isChild1 || isChild2) {
                return false;
            }
            if (AnnotationUtils.areSame(anno2, UNCLASSIFIED)) {
                return true;
            }
            if (AnnotationUtils.areSame(anno1, FBCBOTTOM)) {
                return true;
            }
            return AnnotationUtils.areSame(anno1, anno2);
        }

        /**
         * Is the annotation {@code anno} part of the child type system?
         */
        protected boolean isChildAnnotation(AnnotationMirror anno) {
            return AnnotationUtils.containsSame(
                    childHierarchy.getAnnotations(), anno);
        }

        @Override
        public boolean isSubtype(Collection<AnnotationMirror> rhs,
                Collection<AnnotationMirror> lhs) {
            if (lhs.isEmpty() || rhs.isEmpty()) {
                SourceChecker
                        .errorAbort("InitializationQualifierHierarchy: empty annotations in lhs: "
                                + lhs + " or rhs: " + rhs);
            }
            if (lhs.size() != rhs.size()) {
                SourceChecker
                        .errorAbort("InitializationQualifierHierarchy: mismatched number of annotations in lhs: "
                                + lhs + " and rhs: " + rhs);
            }
            int valid = 0;
            for (AnnotationMirror lhsAnno : lhs) {
                for (AnnotationMirror rhsAnno : rhs) {
                    if (AnnotationUtils.areSame(getTopAnnotation(lhsAnno),
                            getTopAnnotation(rhsAnno))
                            && isSubtype(rhsAnno, lhsAnno)) {
                        ++valid;
                    }
                }
            }
            return lhs.size() == valid;
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror anno1,
                AnnotationMirror anno2) {
            boolean isChild1 = isChildAnnotation(anno1);
            boolean isChild2 = isChildAnnotation(anno2);
            if (isChild1 && isChild2) {
                return childHierarchy.leastUpperBound(anno1, anno2);
            }
            // If the two annotations are not from the same hierarchy, then null
            // should be returned.
            if (isChild1 || isChild2) {
                return null;
            }
            if (AnnotationUtils.areSame(anno1, FBCBOTTOM)) {
                return anno2;
            }
            if (AnnotationUtils.areSame(anno2, FBCBOTTOM)) {
                return anno1;
            }
            if (AnnotationUtils.areSame(anno1, anno2)) {
                return anno1;
            }
            return UNCLASSIFIED;
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror anno1,
                AnnotationMirror anno2) {
            boolean isChild1 = isChildAnnotation(anno1);
            boolean isChild2 = isChildAnnotation(anno2);
            if (isChild1 && isChild2) {
                return childHierarchy.greatestLowerBound(anno1, anno2);
            }
            // If the two annotations are not from the same hierarchy, then null
            // should be returned.
            if (isChild1 || isChild2) {
                return null;
            }
            if (AnnotationUtils.areSame(anno1, UNCLASSIFIED)) {
                return anno2;
            }
            if (AnnotationUtils.areSame(anno2, UNCLASSIFIED)) {
                return anno1;
            }
            if (AnnotationUtils.areSame(anno1, anno2)) {
                return anno1;
            }
            return FBCBOTTOM;
        }

    }
}
