package org.checkerframework.qualframework.base;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

import javax.lang.model.element.AnnotationMirror;

/** Adapter class for {@link TypeHierarchy}, extending
 * {@link org.checkerframework.framework.type.TypeHierarchy org.checkerframework.framework.type.TypeHierarchy}.
 */
class TypeHierarchyAdapter<Q> extends org.checkerframework.framework.type.DefaultTypeHierarchy {

    private final TypeHierarchy<Q> underlying;

    private final TypeMirrorConverter<Q> converter;

    public TypeHierarchyAdapter(TypeHierarchy<Q> underlying,
            TypeMirrorConverter<Q> converter,
            CheckerAdapter<Q> checker,
            QualifierHierarchyAdapter<Q>.Implementation qualifierHierarchy,
            boolean ignoreRawTypes,
            boolean invariantArrayComponents) {
        super(checker, qualifierHierarchy, ignoreRawTypes, invariantArrayComponents);
        this.underlying = underlying;
        this.converter = converter;
    }


    @Override
    public boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype) {
        return underlying.isSubtype(
                converter.getQualifiedType(subtype),
                converter.getQualifiedType(supertype));
    }

    @Override
    public boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, AnnotationMirror top) {
        //NOTE: This may be insufficient for multi-rooted qualifier hierarchies.  David McArthur and
        //Jonathan Burke have had a discussion on this.  This method will work for single-root hierarchies
        //which are the only ones that have a Qual implementation at the moment.  We will take this up again
        //before expanding to multi-rooted type systems (e.g. NullnessInitialization)
        //The reason we have this particular method in the first place is because the appropriate location
        //to check for an Annotation may be different between two type systems, that is:
        // For a declaration:
        //
        //  <@Initialized T extends @UnknownInitialization Object> void m( @NonNull @Initialized T tNonNullInit,
        //                                                                 @NonNull T tq)
        // t = tNonNullInit;  //for this assignment, the location that holds the effective annotation for parameter
        //                    // tq varies by qualifier hierarchy.  In the Nullness hierarchy, it is the primary
        //                    // annotation.  In the initialization hierarchy, it is the lower bound.
        //
        // To deal with this issue, this method isSubtype(subtype, supertype, top) handles these
        // hierarchies individually.  However, for the qualifier system there is only 1 qualifier for both
        // hierarchies.  So QualifiedTypeMirrors would not be able to model the above situation.

        //One alternative to this approach is to always move the primary annotation to the bounds and
        //then remove the actual qualifiers.  This would mean, the primary annotations are always pushed to
        //a concrete type where there must be one annotation in each hierarchy. E.g.
        // <@Initialized T extends @UnknownInitialization Object>
        //  @NonNull T t;   would typed as
        //      T super @NonNull Initialized Void extends @NonNull @UnknownInitialized Object
        //
        // If a type variable extended another, the primary annotation would be pushed to its concrete bounds.  E.g.
        // <E extends @NonNull Object, @Initialized T extends @Initialized E>
        // @Nullable T t;   is typed as
        //      T super @Nullable @Initialized Void extends (E super @Nullable @Initialized Void
        //                                                     extends @Nullable @Initialized Object)
        //
        // This system would work, but would require still greater changes to the Type Variable implementation
        // of the Checker Framework.
        return underlying.isSubtype(
                converter.getQualifiedType(subtype),
                converter.getQualifiedType(supertype));
    }

    boolean superIsSubtype(QualifiedTypeMirror<Q> subtype, QualifiedTypeMirror<Q> supertype) {
        return super.isSubtype(
                converter.getAnnotatedType(subtype),
                converter.getAnnotatedType(supertype),
                qualifierHierarchy.getTopAnnotations().iterator().next());
    }
}
