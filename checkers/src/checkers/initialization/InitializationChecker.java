package checkers.initialization;

/*>>>
import checkers.interning.quals.*;
*/

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javacutils.AnnotationUtils;
import javacutils.TypesUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import checkers.basetype.BaseTypeChecker;
import checkers.initialization.quals.Initialized;
import checkers.initialization.quals.FBCBottom;
import checkers.initialization.quals.UnderInitialization;
import checkers.initialization.quals.NotOnlyInitialized;
import checkers.initialization.quals.UnknownInitialization;
import checkers.nullness.quals.NonRaw;
import checkers.nullness.quals.Raw;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.QualifierHierarchy;
import checkers.util.AnnotationBuilder;
import checkers.util.MultiGraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * The checker for the freedom-before-commitment type-system. Also supports
 * rawness as a type-system for tracking initialization, though FBC is
 * preferred.
 *
 * @author Stefan Heule
 */
public abstract class InitializationChecker<Factory extends InitializationAnnotatedTypeFactory<?, ?, ?, ?, ?>>
    extends BaseTypeChecker<Factory> {

    /** Annotation constants */
    public AnnotationMirror COMMITTED, FREE, FBCBOTTOM, NOT_ONLY_COMMITTED, UNCLASSIFIED;

    /**
     * Should the initialization type system be FBC? If not, the rawness type
     * system is used for initialization.
     */
    public final boolean useFbc;

    public InitializationChecker(boolean useFbc) {
        this.useFbc = useFbc;
    }

    @Override
    public void initChecker() {
        Elements elements = processingEnv.getElementUtils();

        if (useFbc) {
            COMMITTED = AnnotationUtils.fromClass(elements, Initialized.class);
            FREE = AnnotationUtils.fromClass(elements, UnderInitialization.class);
            NOT_ONLY_COMMITTED = AnnotationUtils.fromClass(elements, NotOnlyInitialized.class);
            FBCBOTTOM = AnnotationUtils.fromClass(elements, FBCBottom.class);
            UNCLASSIFIED = AnnotationUtils.fromClass(elements, UnknownInitialization.class);
        } else {
            COMMITTED = AnnotationUtils.fromClass(elements, NonRaw.class);
            FBCBOTTOM = COMMITTED; // @NonRaw is also bottom
            UNCLASSIFIED = AnnotationUtils.fromClass(elements, Raw.class);
        }

        super.initChecker();
    }

    @Override
    public Collection<String> getSuppressWarningsKeys() {
        Collection<String> result = new HashSet<>(super.getSuppressWarningsKeys());
        result.add("initialization");
        return result;
    }

    // Cache for the initialization annotations
    protected Set<Class<? extends Annotation>> initAnnos;

    public Set<Class<? extends Annotation>> getInitializationAnnotations() {
        if (initAnnos == null) {
            Set<Class<? extends Annotation>> result = new HashSet<>();
            if (useFbc) {
                result.add(UnderInitialization.class);
                result.add(Initialized.class);
                result.add(UnknownInitialization.class);
                result.add(FBCBottom.class);
            } else {
                result.add(Raw.class);
                result.add(NonRaw.class);
            }
            initAnnos = Collections.unmodifiableSet(result);
        }
        return initAnnos;
    }

    /**
     * Is the annotation {@code anno} an initialization qualifier?
     */
    protected boolean isInitializationAnnotation(AnnotationMirror anno) {
        assert anno != null;
        return AnnotationUtils.areSameIgnoringValues(anno, UNCLASSIFIED) ||
                AnnotationUtils.areSameIgnoringValues(anno, FREE) ||
                AnnotationUtils.areSameIgnoringValues(anno, COMMITTED) ||
                AnnotationUtils.areSameIgnoringValues(anno, FBCBOTTOM);
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
        return getInitializationAnnotations();
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
     * Returns a {@link UnderInitialization} annotation with a given type frame.
     */
    public AnnotationMirror createFreeAnnotation(TypeMirror typeFrame) {
        assert typeFrame != null;
        assert useFbc : "The rawness type system does not have a @UnderInitialization annotation.";
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv,
                UnderInitialization.class);
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Returns a {@link UnderInitialization} annotation with a given type frame.
     */
    public AnnotationMirror createFreeAnnotation(Class<?> typeFrame) {
        assert typeFrame != null;
        assert useFbc : "The rawness type system does not have a @UnderInitialization annotation.";
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv,
                UnderInitialization.class);
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Returns a {@link UnknownInitialization} or {@link Raw} annotation with a given
     * type frame.
     */
    public AnnotationMirror createUnclassifiedAnnotation(Class<?> typeFrame) {
        assert typeFrame != null;
        Class<? extends Annotation> clazz = useFbc ? UnknownInitialization.class
                : Raw.class;
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, clazz);
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Returns a {@link UnknownInitialization} annotation with a given type frame.
     */
    public AnnotationMirror createUnclassifiedAnnotation(TypeMirror typeFrame) {
        assert typeFrame != null;
        Class<? extends Annotation> clazz = useFbc ? UnknownInitialization.class
                : Raw.class;
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, clazz);
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Returns the type frame of a given annotation. The annotation must either
     * be {@link UnderInitialization} or {@link UnknownInitialization}.
     */
    public TypeMirror getTypeFrameFromAnnotation(AnnotationMirror annotation) {
        TypeMirror name = AnnotationUtils.getElementValue(annotation, "value",
                TypeMirror.class, true);
        return name;
    }

    /**
     * Is {@code anno} the {@link UnderInitialization} annotation (with any type frame)? Always
     * returns false if {@code useFbc} is false.
     */
    public boolean isFree(AnnotationMirror anno) {
        return useFbc && AnnotationUtils.areSameByClass(anno, UnderInitialization.class);
    }

    /**
     * Is {@code anno} the {@link UnknownInitialization} annotation (with any type
     * frame)? If {@code useFbc} is false, then {@link Raw} is used in the
     * comparison.
     */
    public boolean isUnclassified(AnnotationMirror anno) {
        Class<? extends Annotation> clazz = useFbc ? UnknownInitialization.class
                : Raw.class;
        return AnnotationUtils.areSameByClass(anno, clazz);
    }

    /**
     * Is {@code anno} the bottom annotation?
     */
    public boolean isFbcBottom(AnnotationMirror anno) {
        return AnnotationUtils.areSame(anno, FBCBOTTOM);
    }

    /**
     * Is {@code anno} the {@link Initialized} annotation? If {@code useFbc} is
     * false, then {@link NonRaw} is used in the comparison.
     */
    public boolean isCommitted(AnnotationMirror anno) {
        return AnnotationUtils.areSame(anno, COMMITTED);
    }

    /**
     * Does {@code anno} have the annotation {@link UnderInitialization} (with any type frame)?
     * Always returns false if {@code useFbc} is false.
     */
    public boolean isFree(AnnotatedTypeMirror anno) {
        return useFbc && anno.hasEffectiveAnnotation(UnderInitialization.class);
    }

    /**
     * Does {@code anno} have the annotation {@link UnknownInitialization} (with any type
     * frame)? If {@code useFbc} is false, then {@link Raw} is used in the
     * comparison.
     */
    public boolean isUnclassified(AnnotatedTypeMirror anno) {
        Class<? extends Annotation> clazz = useFbc ? UnknownInitialization.class
                : Raw.class;
        return anno.hasEffectiveAnnotation(clazz);
    }

    /**
     * Does {@code anno} have the bottom annotation?
     */
    public boolean isFbcBottom(AnnotatedTypeMirror anno) {
        Class<? extends Annotation> clazz = useFbc ? FBCBottom.class
                : NonRaw.class;
        return anno.hasEffectiveAnnotation(clazz);
    }

    /**
     * Does {@code anno} have the annotation {@link Initialized}? If
     * {@code useFbc} is false, then {@link NonRaw} is used in the comparison.
     */
    public boolean isCommitted(AnnotatedTypeMirror anno) {
        Class<? extends Annotation> clazz = useFbc ? Initialized.class
                : NonRaw.class;
        return anno.hasEffectiveAnnotation(clazz);
    }

    @Override
    protected MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    /**
     * The {@link QualifierHierarchy} for the initialization type system. This
     * hierarchy also includes the child type system, whose hierarchy is
     * provided through {@link #getChildQualifierHierarchy()}.
     */
    protected abstract class InitializationQualifierHierarchy extends MultiGraphQualifierHierarchy {
        protected Types types = processingEnv.getTypeUtils();

        public InitializationQualifierHierarchy(MultiGraphFactory f, Object... arg) {
            super(f, arg);
        }

        /**
         * Subtype testing for initialization annotations.
         * Will return false if either qualifier is not an initialization annotation.
         * Subclasses should override isSubtype and call this method for
         * initialization qualifiers.
         *
         * @param rhs
         * @param lhs
         * @return
         */
        public boolean isSubtypeInitialization(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (!isInitializationAnnotation(rhs) ||
                    !isInitializationAnnotation(lhs)) {
                return false;
            }

            // 't' is always a subtype of 't'
            if (AnnotationUtils.areSame(rhs, lhs)) {
                return true;
            }
            // @Initialized is only a supertype of @FBCBottom.
            if (isCommitted(lhs)) {
                return isFbcBottom(rhs);
            }
            // @Initialized is only a subtype of @UnknownInitialization.
            boolean unc2 = isUnclassified(lhs);
            if (isCommitted(rhs)) {
                return unc2;
            }
            // @FBCBottom is a supertype of nothing.
            if (isFbcBottom(lhs)) {
                return false;
            }
            // @FBCBottom is a subtype of everything.
            if (isFbcBottom(rhs)) {
                return true;
            }
            boolean unc1 = isUnclassified(rhs);
            boolean free1 = isFree(rhs);
            boolean free2 = isFree(lhs);
            // @UnknownInitialization is not a subtype of @UnderInitialization.
            if (unc1 && free2) {
                return false;
            }
            // Now, either both annotations are @UnderInitialization, both annotations are
            // @UnknownInitialization or anno1 is @UnderInitialization and anno2 is @UnknownInitialization.
            assert (free1 && free2) || (unc1 && unc2) || (free1 && unc2);
            // Thus, we only need to look at the type frame.
            TypeMirror frame1 = getTypeFrameFromAnnotation(rhs);
            TypeMirror frame2 = getTypeFrameFromAnnotation(lhs);
            return types.isSubtype(frame1, frame2);
        }

        /**
         * Compute the least upper bound of two initialization qualifiers.
         * Returns null if one of the qualifiers is not in the initialization hierarachy.
         * Subclasses should override leastUpperBound and call this method for
         * initialization qualifiers.
         *
         * @param anno1 an initialization qualifier
         * @param anno2 an initialization qualifier
         * @return the lub of anno1 and anno2
         */
        protected AnnotationMirror leastUpperBoundInitialization(AnnotationMirror anno1,
                AnnotationMirror anno2) {
            if (!isInitializationAnnotation(anno1) ||
                    !isInitializationAnnotation(anno2)) {
                return null;
            }

            // Handle the case where one is a subtype of the other.
            if (isSubtypeInitialization(anno1, anno2)) {
                return anno2;
            } else if (isSubtypeInitialization(anno2, anno1)) {
                return anno1;
            }
            boolean unc1 = isUnclassified(anno1);
            boolean unc2 = isUnclassified(anno2);
            boolean free1 = isFree(anno1);
            boolean free2 = isFree(anno2);

            // Handle @Initialized.
            if (isCommitted(anno1)) {
                assert free2;
                return createUnclassifiedAnnotation(getTypeFrameFromAnnotation(anno2));
            } else if (isCommitted(anno2)) {
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
            return TypesUtils.typeFromClass(processingEnv.getTypeUtils(),
                    processingEnv.getElementUtils(), Object.class);
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror anno1,
                AnnotationMirror anno2) {
            assert false : "This code is not needed for this type system so far.";
            return null;
        }

    }

}
