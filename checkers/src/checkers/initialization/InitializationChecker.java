package checkers.initialization;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import checkers.basetype.BaseTypeChecker;
import checkers.initialization.quals.Committed;
import checkers.initialization.quals.FBCBottom;
import checkers.initialization.quals.Free;
import checkers.initialization.quals.NotOnlyCommitted;
import checkers.initialization.quals.Unclassified;
import checkers.quals.TypeQualifiers;
import checkers.source.SourceChecker;
import checkers.types.QualifierHierarchy;
import checkers.util.AnnotationUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

@TypeQualifiers({ Free.class, Committed.class, Unclassified.class,
        FBCBottom.class })
public abstract class InitializationChecker extends BaseTypeChecker {

    /** Annotation constants */
    protected AnnotationMirror COMMITTED, FREE, FBCBOTTOM, NOT_ONLY_COMMITTED;

    @Override
    public void initChecker(ProcessingEnvironment processingEnv) {
        AnnotationUtils annoFactory = AnnotationUtils
                .getInstance(processingEnv);
        COMMITTED = annoFactory.fromClass(Committed.class);
        FREE = annoFactory.fromClass(Free.class);
        FBCBOTTOM = annoFactory.fromClass(FBCBottom.class);
        NOT_ONLY_COMMITTED = annoFactory.fromClass(NotOnlyCommitted.class);

        super.initChecker(processingEnv);
    }

    public Set<Class<? extends Annotation>> getCommitmentAnnotations() {
        Set<Class<? extends Annotation>> result = new HashSet<>();
        result.add(Free.class);
        result.add(Committed.class);
        result.add(FBCBottom.class);
        result.add(Unclassified.class);
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
    public Set<Class<? extends Annotation>> getInvalidConstructorReturnTypeAnnotations() {
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
     * Returns a {@link Free} annotation with a given type frame.
     */
    public AnnotationMirror createFreeAnnotation(TypeMirror typeFrame) {
        assert typeFrame != null;
        AnnotationUtils.AnnotationBuilder builder = new AnnotationUtils.AnnotationBuilder(
                env, Free.class.getCanonicalName());
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Returns a {@link Free} annotation with a given type frame.
     */
    public AnnotationMirror createFreeAnnotation(Class<?> typeFrame) {
        assert typeFrame != null;
        AnnotationUtils.AnnotationBuilder builder = new AnnotationUtils.AnnotationBuilder(
                env, Free.class.getCanonicalName());
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Returns a {@link Free} annotation with a given type frame.
     */
    public AnnotationMirror createUnclassifiedAnnotation(Class<?> typeFrame) {
        assert typeFrame != null;
        AnnotationUtils.AnnotationBuilder builder = new AnnotationUtils.AnnotationBuilder(
                env, Unclassified.class.getCanonicalName());
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Returns a {@link Unclassified} annotation with a given type frame.
     */
    public AnnotationMirror createUnclassifiedAnnotation(TypeMirror typeFrame) {
        assert typeFrame != null;
        AnnotationUtils.AnnotationBuilder builder = new AnnotationUtils.AnnotationBuilder(
                env, Unclassified.class.getCanonicalName());
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Returns the type frame of a given annotation. The annotation must either
     * be {@link Free} or {@link Unclassified}.
     */
    public TypeMirror getTypeFrameFromAnnotation(AnnotationMirror annotation) {
        TypeMirror name = AnnotationUtils.elementValueWithDefaults(annotation,
                "value", TypeMirror.class);
        return name;
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
        protected Types types = env.getTypeUtils();

        public InitializationQualifierHierarchy() {
            super(InitializationChecker.this);

            AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);

            tops = new HashSet<>();
            tops.add(createUnclassifiedAnnotation(Object.class));
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
            // If both annotations are from the child hierarchy, use
            // childHierarchy.isSubtyp.
            if (isChild1 && isChild2) {
                return childHierarchy.isSubtype(anno1, anno2);
            }
            // If one annotation is from the child hierarchy, but the other is
            // not, then they cannot be subtypes of each other.
            if (isChild1 || isChild2) {
                return false;
            }
            // 't' is always a subtype of 't'
            if (AnnotationUtils.areSame(anno1, anno2)) {
                return true;
            }
            // @Committed is only a supertype of @FBCBottom.
            if (AnnotationUtils.areSame(anno2, COMMITTED)) {
                return AnnotationUtils.areSame(anno1, FBCBOTTOM);
            }
            // @Committed is only a subtype of @Unclassified.
            boolean unc2 = AnnotationUtils.areSameByClass(anno2,
                    Unclassified.class);
            if (AnnotationUtils.areSame(anno1, COMMITTED)) {
                return unc2;
            }
            // @FBCBottom is a supertype of nothing.
            if (AnnotationUtils.areSame(anno2, FBCBOTTOM)) {
                return false;
            }
            // @FBCBottom is a subtype of everything.
            if (AnnotationUtils.areSame(anno1, FBCBOTTOM)) {
                return true;
            }
            boolean unc1 = AnnotationUtils.areSameByClass(anno1,
                    Unclassified.class);
            boolean free1 = AnnotationUtils.areSameByClass(anno1, Free.class);
            boolean free2 = AnnotationUtils.areSameByClass(anno2, Free.class);
            // @Unclassified is not a subtype of @Free.
            if (unc1 && free2) {
                return false;
            }
            // Now, either both annotations are @Free, both annotations are
            // @Unclassified or anno1 is @Free and anno2 is @Unclassified.
            assert (free1 && free2) || (unc1 && unc2) || (free1 && unc2);
            // Thus, we only need to look at the type frame.
            TypeMirror frame1 = getTypeFrameFromAnnotation(anno1);
            TypeMirror frame2 = getTypeFrameFromAnnotation(anno2);
            return types.isSubtype(frame1, frame2);
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
            // If both annotations are from the child hierarchy, use
            // childHierarchy.leastUpperBound.
            if (isChild1 && isChild2) {
                return childHierarchy.leastUpperBound(anno1, anno2);
            }
            // If the two annotations are not from the same hierarchy, then null
            // should be returned.
            if (isChild1 || isChild2) {
                return null;
            }

            // Handle the case where one is a subtype of the other.
            if (isSubtype(anno1, anno2)) {
                return anno2;
            } else if (isSubtype(anno2, anno1)) {
                return anno1;
            }
            boolean unc1 = AnnotationUtils.areSameByClass(anno1,
                    Unclassified.class);
            boolean unc2 = AnnotationUtils.areSameByClass(anno2,
                    Unclassified.class);
            boolean free1 = AnnotationUtils.areSameByClass(anno1, Free.class);
            boolean free2 = AnnotationUtils.areSameByClass(anno2, Free.class);

            // Handle @Committed.
            if (AnnotationUtils.areSame(anno1, COMMITTED)) {
                assert free2;
                return createUnclassifiedAnnotation(getTypeFrameFromAnnotation(anno2));
            } else if (AnnotationUtils.areSame(anno2, COMMITTED)) {
                assert free1;
                return createUnclassifiedAnnotation(getTypeFrameFromAnnotation(anno1));
            }

            if (free1 && free2) {
                return createFreeAnnotation(lubTypeFrame(
                        getTypeFrameFromAnnotation(anno1),
                        getTypeFrameFromAnnotation(anno2)));
            }

            assert (unc1 || free1) && (unc2 || free2);
            return createUnclassifiedAnnotation(lubTypeFrame(
                    getTypeFrameFromAnnotation(anno1),
                    getTypeFrameFromAnnotation(anno2)));
        }

        /**
         * Returns the least upper bound of two types.
         */
        protected TypeMirror lubTypeFrame(TypeMirror a, TypeMirror b) {
            if (types.isSubtype(a, b)) {
                return b;
            } else if (types.isSubtype(b, a)) {
                return a;
            }
            assert false : "not fully implemented yet";
            return AnnotationUtils.getInstance(env).typeFromClass(Object.class);
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror anno1,
                AnnotationMirror anno2) {
            assert false : "This code is not needed for this type system so far.";
            return null;
        }

    }
}
