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
import checkers.initialization.quals.UnderInitializion;
import checkers.initialization.quals.NotOnlyInitialized;
import checkers.initialization.quals.UnkownInitialization;
import checkers.nullness.quals.NonRaw;
import checkers.nullness.quals.Raw;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.QualifierHierarchy;
import checkers.util.AnnotationBuilder;
import checkers.util.MultiGraphQualifierHierarchy;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * The checker for the freedom-before-commitement type-system.
 *
 * @author Stefan Heule
 */
public abstract class InitializationChecker extends BaseTypeChecker {

    /** Annotation constants */
    public AnnotationMirror COMMITTED, FREE, FBCBOTTOM, NOT_ONLY_COMMITTED;

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
            FREE = AnnotationUtils.fromClass(elements, UnderInitializion.class);
            NOT_ONLY_COMMITTED = AnnotationUtils.fromClass(elements,
                    NotOnlyInitialized.class);
            FBCBOTTOM = AnnotationUtils.fromClass(elements, FBCBottom.class);
        } else {
            COMMITTED = AnnotationUtils.fromClass(elements, NonRaw.class);
            FBCBOTTOM = COMMITTED; // @NonRaw is also bottom
        }

        super.initChecker();
    }

    @Override
    public Collection<String> getSuppressWarningsKeys() {
        return Collections.singleton("initialization");
    }

    public Set<Class<? extends Annotation>> getInitializationAnnotations() {
        Set<Class<? extends Annotation>> result = new HashSet<>();
        if (useFbc) {
            result.add(UnderInitializion.class);
            result.add(Initialized.class);
            result.add(UnkownInitialization.class);
            result.add(FBCBottom.class);
        } else {
            result.add(Raw.class);
            result.add(NonRaw.class);
        }
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
     * Returns the {@link QualifierHierarchy} of the child type system.
     */
    protected abstract QualifierHierarchy getChildQualifierHierarchy();

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new InitializationQualifierHierarchy();
    }

    /**
     * Returns a {@link UnderInitializion} annotation with a given type frame.
     */
    public AnnotationMirror createFreeAnnotation(TypeMirror typeFrame) {
        assert typeFrame != null;
        assert useFbc : "The rawness type system does not have a @UnderInitializion annotation.";
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv,
                UnderInitializion.class);
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Returns a {@link UnderInitializion} annotation with a given type frame.
     */
    public AnnotationMirror createFreeAnnotation(Class<?> typeFrame) {
        assert typeFrame != null;
        assert useFbc : "The rawness type system does not have a @UnderInitializion annotation.";
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv,
                UnderInitializion.class);
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Returns a {@link UnkownInitialization} or {@link Raw} annotation with a given
     * type frame.
     */
    public AnnotationMirror createUnclassifiedAnnotation(Class<?> typeFrame) {
        assert typeFrame != null;
        Class<? extends Annotation> clazz = useFbc ? UnkownInitialization.class
                : Raw.class;
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, clazz);
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Returns a {@link UnkownInitialization} annotation with a given type frame.
     */
    public AnnotationMirror createUnclassifiedAnnotation(TypeMirror typeFrame) {
        assert typeFrame != null;
        Class<? extends Annotation> clazz = useFbc ? UnkownInitialization.class
                : Raw.class;
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, clazz);
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Returns the type frame of a given annotation. The annotation must either
     * be {@link UnderInitializion} or {@link UnkownInitialization}.
     */
    public TypeMirror getTypeFrameFromAnnotation(AnnotationMirror annotation) {
        TypeMirror name = AnnotationUtils.getElementValue(annotation, "value",
                TypeMirror.class, true);
        return name;
    }

    /**
     * Is {@code anno} the {@link UnderInitializion} annotation (with any type frame)? Always
     * returns false if {@code useFbc} is false.
     */
    public boolean isFree(AnnotationMirror anno) {
        return useFbc && AnnotationUtils.areSameByClass(anno, UnderInitializion.class);
    }

    /**
     * Is {@code anno} the {@link UnkownInitialization} annotation (with any type
     * frame)? If {@code useFbc} is false, then {@link Raw} is used in the
     * comparison.
     */
    public boolean isUnclassified(AnnotationMirror anno) {
        Class<? extends Annotation> clazz = useFbc ? UnkownInitialization.class
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
     * Does {@code anno} have the annotation {@link UnderInitializion} (with any type frame)?
     * Always returns false if {@code useFbc} is false.
     */
    public boolean isFree(AnnotatedTypeMirror anno) {
        return useFbc && anno.hasEffectiveAnnotation(UnderInitializion.class);
    }

    /**
     * Does {@code anno} have the annotation {@link UnkownInitialization} (with any type
     * frame)? If {@code useFbc} is false, then {@link Raw} is used in the
     * comparison.
     */
    public boolean isUnclassified(AnnotatedTypeMirror anno) {
        Class<? extends Annotation> clazz = useFbc ? UnkownInitialization.class
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

    /**
     * The {@link QualifierHierarchy} for the initialization type system. This
     * hierarchy also includes the child type system, whose hierarchy is
     * provided through {@link #getChildQualifierHierarchy()}.
     */
    protected class InitializationQualifierHierarchy extends
            MultiGraphQualifierHierarchy {

        protected Set<AnnotationMirror> tops;
        protected Set<AnnotationMirror> bottoms;
        protected Set</*@Interned*/String> typeQualifiers;
        protected QualifierHierarchy childHierarchy = getChildQualifierHierarchy();
        protected Types types = processingEnv.getTypeUtils();

        public InitializationQualifierHierarchy() {
            super();

            tops = new HashSet<>();
            tops.add(createUnclassifiedAnnotation(Object.class));
            tops.addAll(childHierarchy.getTopAnnotations());

            bottoms = new HashSet<>();
            Class<? extends Annotation> clazz = useFbc ? FBCBottom.class
                    : NonRaw.class;
            bottoms.add(AnnotationUtils.fromClass(
                    processingEnv.getElementUtils(), clazz));
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
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public Set</*@Interned*/String> getTypeQualifiers() {
            if (typeQualifiers != null)
                return typeQualifiers;
            Set</*@Interned*/String> names = new HashSet<>();
            Set<Class<?>> clazzes = new HashSet<>();
            clazzes.addAll(getInitializationAnnotations());
            // Add qualifiers from the initialization type system.
            for (Class<?> clazz : clazzes) {
                Elements elements = processingEnv.getElementUtils();
                AnnotationMirror anno = AnnotationUtils.fromClass(elements,
                        (Class) clazz);
                names.add(AnnotationUtils.annotationName(anno));
            }
            // Add qualifiers from the child type system.
            names.addAll(childHierarchy.getTypeQualifiers());

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
            // @Initialized is only a supertype of @FBCBottom.
            if (isCommitted(anno2)) {
                return isFbcBottom(anno1);
            }
            // @Initialized is only a subtype of @UnkownInitialization.
            boolean unc2 = isUnclassified(anno2);
            if (isCommitted(anno1)) {
                return unc2;
            }
            // @FBCBottom is a supertype of nothing.
            if (isFbcBottom(anno2)) {
                return false;
            }
            // @FBCBottom is a subtype of everything.
            if (isFbcBottom(anno1)) {
                return true;
            }
            boolean unc1 = isUnclassified(anno1);
            boolean free1 = isFree(anno1);
            boolean free2 = isFree(anno2);
            // @UnkownInitialization is not a subtype of @UnderInitializion.
            if (unc1 && free2) {
                return false;
            }
            // Now, either both annotations are @UnderInitializion, both annotations are
            // @UnkownInitialization or anno1 is @UnderInitializion and anno2 is @UnkownInitialization.
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
            assert anno != null;
            for (/*@Interned*/ String c : childHierarchy.getTypeQualifiers()) {
                if (AnnotationUtils.areSameByName(anno, c)) {
                    return true;
                }
            }
            return false;
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

        @Override
        public AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
            return childHierarchy.getPolymorphicAnnotation(start);
        }

    }
}
