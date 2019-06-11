package org.checkerframework.checker.initialization;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.checkerframework.checker.initialization.qual.FBCBottom;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.NullnessAnnotatedTypeFactory;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.checker.nullness.qual.NonRaw;
import org.checkerframework.checker.nullness.qual.Raw;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.qual.Unused;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * The annotated type factory for the freedom-before-commitment type-system. The
 * freedom-before-commitment type-system and this class are abstract and need to be combined with
 * another type-system whose safe initialization should be tracked. For an example, see the {@link
 * NullnessChecker}. Also supports rawness as a type-system for tracking initialization, though FBC
 * is preferred.
 */
public abstract class InitializationAnnotatedTypeFactory<
                Value extends CFAbstractValue<Value>,
                Store extends InitializationStore<Value, Store>,
                Transfer extends InitializationTransfer<Value, Transfer, Store>,
                Flow extends CFAbstractAnalysis<Value, Store, Transfer>>
        extends GenericAnnotatedTypeFactory<Value, Store, Transfer, Flow> {

    /** {@link UnknownInitialization} or {@link Raw}. */
    protected final AnnotationMirror UNCLASSIFIED;

    /** {@link Initialized} or {@link NonRaw}. */
    protected final AnnotationMirror COMMITTED;

    /** {@link UnderInitialization} or null. */
    protected final AnnotationMirror FREE;

    /** {@link NotOnlyInitialized} or null. */
    protected final AnnotationMirror NOT_ONLY_COMMITTED;

    /** {@link FBCBottom} or {@link NonRaw}. */
    protected final AnnotationMirror FBCBOTTOM;

    /**
     * Should the initialization type system be FBC? If not, the rawness type system is used for
     * initialization.
     */
    protected final boolean useFbc;

    // Cache for the initialization annotations
    protected final Set<Class<? extends Annotation>> initAnnos;

    public InitializationAnnotatedTypeFactory(BaseTypeChecker checker, boolean useFbc) {
        super(checker, true);

        this.useFbc = useFbc;

        Set<Class<? extends Annotation>> tempInitAnnos = new LinkedHashSet<>();

        if (useFbc) {
            COMMITTED = AnnotationBuilder.fromClass(elements, Initialized.class);
            FREE = AnnotationBuilder.fromClass(elements, UnderInitialization.class);
            NOT_ONLY_COMMITTED = AnnotationBuilder.fromClass(elements, NotOnlyInitialized.class);
            FBCBOTTOM = AnnotationBuilder.fromClass(elements, FBCBottom.class);
            UNCLASSIFIED = AnnotationBuilder.fromClass(elements, UnknownInitialization.class);

            tempInitAnnos.add(UnderInitialization.class);
            tempInitAnnos.add(Initialized.class);
            tempInitAnnos.add(UnknownInitialization.class);
            tempInitAnnos.add(FBCBottom.class);
        } else {
            COMMITTED = AnnotationBuilder.fromClass(elements, NonRaw.class);
            FBCBOTTOM = COMMITTED; // @NonRaw is also bottom
            UNCLASSIFIED = AnnotationBuilder.fromClass(elements, Raw.class);
            FREE = null; // unused
            NOT_ONLY_COMMITTED = null; // unused

            tempInitAnnos.add(Raw.class);
            tempInitAnnos.add(NonRaw.class);
        }

        initAnnos = Collections.unmodifiableSet(tempInitAnnos);

        // No call to postInit() because this class is abstract.
        // Its subclasses must call postInit().
    }

    public Set<Class<? extends Annotation>> getInitializationAnnotations() {
        return initAnnos;
    }

    /**
     * Is the annotation {@code anno} an initialization qualifier?
     *
     * @param anno the annotation to check
     * @return true if the argument is an initialization qualifier
     */
    protected boolean isInitializationAnnotation(AnnotationMirror anno) {
        assert anno != null;
        return AnnotationUtils.areSameByName(anno, UNCLASSIFIED)
                || AnnotationUtils.areSameByName(anno, FREE)
                || AnnotationUtils.areSameByName(anno, COMMITTED)
                || AnnotationUtils.areSameByName(anno, FBCBOTTOM);
    }

    /*
     * The following method can be used to appropriately configure the
     * commitment type-system.
     */

    /** @return the list of annotations that is forbidden for the constructor return type */
    public Set<Class<? extends Annotation>> getInvalidConstructorReturnTypeAnnotations() {
        return getInitializationAnnotations();
    }

    /**
     * Returns the annotation that makes up the invariant of this commitment type system, such as
     * {@code @NonNull}.
     *
     * @return the invariant annotation for this type system
     */
    public abstract AnnotationMirror getFieldInvariantAnnotation();

    /**
     * Returns whether or not {@code field} has the invariant annotation.
     *
     * <p>This method is a convenience method for {@link
     * #hasFieldInvariantAnnotation(AnnotatedTypeMirror)}.
     *
     * <p>If the {@code field} is a type variable, this method returns true if any possible
     * instantiation of the type parameter could have the invariant annotation. See {@link
     * NullnessAnnotatedTypeFactory#hasFieldInvariantAnnotation(VariableTree)} for an example.
     *
     * @param field field that might have invariant annotation
     * @return whether or not field has the invariant annotation
     */
    protected final boolean hasFieldInvariantAnnotation(VariableTree field) {
        AnnotatedTypeMirror type = getAnnotatedType(field);
        return hasFieldInvariantAnnotation(type);
    }

    /**
     * Returns whether or not {@code type} has the invariant annotation.
     *
     * <p>If the {@code type} is a type variable, this method returns true if any possible
     * instantiation of the type parameter could have the invariant annotation. See {@link
     * NullnessAnnotatedTypeFactory#hasFieldInvariantAnnotation(VariableTree)} for an example.
     *
     * @param type of field that might have invariant annotation
     * @return whether or not the type has the invariant annotation
     */
    protected abstract boolean hasFieldInvariantAnnotation(AnnotatedTypeMirror type);

    /**
     * Creates a {@link UnderInitialization} annotation with the given type as its type frame
     * argument.
     *
     * @param typeFrame the type down to which some value has been initialized
     * @return an {@link UnderInitialization} annotation with the given argument
     */
    public AnnotationMirror createFreeAnnotation(TypeMirror typeFrame) {
        assert typeFrame != null;
        assert useFbc : "The rawness type system does not have a @UnderInitialization annotation.";
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, UnderInitialization.class);
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Creates a {@link UnderInitialization} annotation with the given type frame.
     *
     * @param typeFrame the type down to which some value has been initialized
     * @return an {@link UnderInitialization} annotation with the given argument
     */
    public AnnotationMirror createFreeAnnotation(Class<?> typeFrame) {
        assert typeFrame != null;
        assert useFbc : "The rawness type system does not have a @UnderInitialization annotation.";
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, UnderInitialization.class);
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Creates a {@link UnknownInitialization} or {@link Raw} annotation with a given type frame.
     *
     * @param typeFrame the type down to which some value has been initialized
     * @return an {@link UnknownInitialization} or {@link Raw} annotation with the given argument
     */
    public AnnotationMirror createUnclassifiedAnnotation(Class<?> typeFrame) {
        assert typeFrame != null;
        Class<? extends Annotation> clazz = useFbc ? UnknownInitialization.class : Raw.class;
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, clazz);
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Creates an {@link UnknownInitialization} or {@link Raw} annotation with a given type frame.
     *
     * @param typeFrame the type down to which some value has been initialized
     * @return an {@link UnknownInitialization} or {@link Raw} annotation with the given argument
     */
    public AnnotationMirror createUnclassifiedAnnotation(TypeMirror typeFrame) {
        assert typeFrame != null;
        Class<? extends Annotation> clazz = useFbc ? UnknownInitialization.class : Raw.class;
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, clazz);
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Returns the type frame of a given annotation.
     *
     * @param annotation a {@link UnderInitialization} or {@link UnknownInitialization} annotation
     * @return the annotation's argument
     */
    public TypeMirror getTypeFrameFromAnnotation(AnnotationMirror annotation) {
        TypeMirror name =
                AnnotationUtils.getElementValue(annotation, "value", TypeMirror.class, true);
        return name;
    }

    /**
     * Is {@code anno} the {@link UnderInitialization} annotation (with any type frame)? Always
     * returns false if {@code useFbc} is false.
     *
     * @param anno the annotation to check
     * @return true if {@code anno} is {@link UnderInitialization}
     */
    public boolean isFree(AnnotationMirror anno) {
        return useFbc && AnnotationUtils.areSameByClass(anno, UnderInitialization.class);
    }

    /**
     * Is {@code anno} the {@link UnknownInitialization} annotation (with any type frame)? If {@code
     * useFbc} is false, then {@link Raw} is used in the comparison.
     *
     * @param anno the annotation to check
     * @return true if {@code anno} is {@link UnknownInitialization} or {@link Raw}
     */
    public boolean isUnclassified(AnnotationMirror anno) {
        Class<? extends Annotation> clazz = useFbc ? UnknownInitialization.class : Raw.class;
        return AnnotationUtils.areSameByClass(anno, clazz);
    }

    /**
     * Is {@code anno} the bottom annotation?
     *
     * @param anno the annotation to check
     * @return true if {@code anno} is {@link FBCBottom} or {@link NonRaw}
     */
    public boolean isFbcBottom(AnnotationMirror anno) {
        return AnnotationUtils.areSame(anno, FBCBOTTOM);
    }

    /**
     * Is {@code anno} the {@link Initialized} annotation? If {@code useFbc} is false, then {@link
     * NonRaw} is used in the comparison.
     *
     * @param anno the annotation to check
     * @return true if {@code anno} is {@link Initialized} or {@link NonRaw}
     */
    public boolean isCommitted(AnnotationMirror anno) {
        return AnnotationUtils.areSame(anno, COMMITTED);
    }

    /**
     * Does {@code anno} have the annotation {@link UnderInitialization} (with any type frame)?
     * Always returns false if {@code useFbc} is false.
     *
     * @param anno the annotation to check
     * @return true if {@code anno} has {@link UnderInitialization}
     */
    public boolean isFree(AnnotatedTypeMirror anno) {
        return useFbc && anno.hasEffectiveAnnotation(UnderInitialization.class);
    }

    /**
     * Does {@code anno} have the annotation {@link UnknownInitialization} (with any type frame)? If
     * {@code useFbc} is false, then {@link Raw} is used in the comparison.
     *
     * @param anno the annotation to check
     * @return true if {@code anno} has {@link UnknownInitialization} or {@link Raw}
     */
    public boolean isUnclassified(AnnotatedTypeMirror anno) {
        Class<? extends Annotation> clazz = useFbc ? UnknownInitialization.class : Raw.class;
        return anno.hasEffectiveAnnotation(clazz);
    }

    /**
     * Does {@code anno} have the bottom annotation?
     *
     * @param anno the annotation to check
     * @return true if {@code anno} has {@link FBCBottom} or {@link NonRaw}
     */
    public boolean isFbcBottom(AnnotatedTypeMirror anno) {
        Class<? extends Annotation> clazz = useFbc ? FBCBottom.class : NonRaw.class;
        return anno.hasEffectiveAnnotation(clazz);
    }

    /**
     * Does {@code anno} have the annotation {@link Initialized}? If {@code useFbc} is false, then
     * {@link NonRaw} is used in the comparison.
     *
     * @param anno the annotation to check
     * @return true if {@code anno} has {@link Initialized} or {@link NonRaw}
     */
    public boolean isCommitted(AnnotatedTypeMirror anno) {
        Class<? extends Annotation> clazz = useFbc ? Initialized.class : NonRaw.class;
        return anno.hasEffectiveAnnotation(clazz);
    }

    @Override
    protected MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    /** Are all fields committed-only? */
    protected boolean areAllFieldsCommittedOnly(ClassTree classTree) {
        if (!useFbc) {
            // In the rawness type system, no fields can store not fully
            // initialized objects.
            return true;
        }
        for (Tree member : classTree.getMembers()) {
            if (!member.getKind().equals(Tree.Kind.VARIABLE)) {
                continue;
            }
            VariableTree var = (VariableTree) member;
            VariableElement varElt = TreeUtils.elementFromDeclaration(var);
            // var is not committed-only
            if (getDeclAnnotation(varElt, NotOnlyInitialized.class) != null) {
                // var is not static -- need a check of initializer blocks,
                // not of constructor which is where this is used
                if (!varElt.getModifiers().contains(Modifier.STATIC)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>In most cases, subclasses want to call this method first because it may clear all
     * annotations and use the hierarchy's root annotations.
     */
    @Override
    public void postAsMemberOf(
            AnnotatedTypeMirror type, AnnotatedTypeMirror owner, Element element) {
        super.postAsMemberOf(type, owner, element);

        if (element.getKind().isField()) {
            Collection<? extends AnnotationMirror> declaredFieldAnnotations =
                    getDeclAnnotations(element);
            AnnotatedTypeMirror fieldAnnotations = getAnnotatedType(element);
            computeFieldAccessType(
                    type, declaredFieldAnnotations, owner, fieldAnnotations, element);
        }
    }

    /**
     * Controls which hierarchies' qualifiers are changed based on the receiver type and the
     * declared annotations for a field.
     *
     * @see #computeFieldAccessType
     * @see #getAnnotatedTypeLhs(Tree)
     */
    private boolean computingAnnotatedTypeMirrorOfLHS = false;

    @Override
    public AnnotatedTypeMirror getAnnotatedTypeLhs(Tree lhsTree) {
        boolean oldComputingAnnotatedTypeMirrorOfLHS = computingAnnotatedTypeMirrorOfLHS;
        computingAnnotatedTypeMirrorOfLHS = true;
        AnnotatedTypeMirror result = super.getAnnotatedTypeLhs(lhsTree);
        computingAnnotatedTypeMirrorOfLHS = oldComputingAnnotatedTypeMirrorOfLHS;
        return result;
    }

    @Override
    public AnnotatedDeclaredType getSelfType(Tree tree) {
        AnnotatedDeclaredType selfType = super.getSelfType(tree);

        TreePath path = getPath(tree);
        Tree topLevelMember = findTopLevelClassMemberForTree(path);
        if (topLevelMember != null) {
            if (topLevelMember.getKind() != Kind.METHOD
                    || TreeUtils.isConstructor((MethodTree) topLevelMember)) {

                setSelfTypeInInitializationCode(tree, selfType, path);
            }
        }

        return selfType;
    }

    /**
     * In the first enclosing class, find the top-level member that contains tree. TODO: should we
     * look whether these elements are enclosed within another class that is itself under
     * construction.
     *
     * <p>Are there any other type of top level objects?
     */
    private Tree findTopLevelClassMemberForTree(TreePath path) {
        ClassTree enclosingClass = TreeUtils.enclosingClass(path);
        if (enclosingClass != null) {

            List<? extends Tree> classMembers = enclosingClass.getMembers();
            TreePath searchPath = path;
            while (searchPath.getParentPath() != null
                    && searchPath.getParentPath() != enclosingClass) {
                searchPath = searchPath.getParentPath();
                if (classMembers.contains(searchPath.getLeaf())) {
                    return searchPath.getLeaf();
                }
            }
        }
        return null;
    }

    /**
     * Side-effects argument {@code selfType} to make it @Initialized or @UnderInitialization,
     * depending on whether all fields have been set.
     */
    protected void setSelfTypeInInitializationCode(
            Tree tree, AnnotatedDeclaredType selfType, TreePath path) {
        ClassTree enclosingClass = TreeUtils.enclosingClass(path);
        Type classType = ((JCTree) enclosingClass).type;
        AnnotationMirror annotation = null;

        // If all fields are committed-only, and they are all initialized,
        // then:
        // - if the class is final, this is @Initialized
        // - otherwise, this is @UnderInitialization(CurrentClass) as
        // there might still be subclasses that need initialization.
        if (areAllFieldsCommittedOnly(enclosingClass)) {
            Store store = getStoreBefore(tree);
            if (store != null
                    && getUninitializedInvariantFields(store, path, false, Collections.emptyList())
                            .isEmpty()) {
                if (classType.isFinal()) {
                    annotation = COMMITTED;
                } else if (useFbc) {
                    annotation = createFreeAnnotation(classType);
                } else {
                    annotation = createUnclassifiedAnnotation(classType);
                }
            }
        }

        if (annotation == null) {
            annotation = getFreeOrRawAnnotationOfSuperType(classType);
        }
        selfType.replaceAnnotation(annotation);
    }

    /**
     * Returns a {@link UnderInitialization} annotation (or {@link UnknownInitialization} if rawness
     * is used) that has the superclass of {@code type} as type frame.
     */
    protected AnnotationMirror getFreeOrRawAnnotationOfSuperType(TypeMirror type) {
        // Find supertype if possible.
        AnnotationMirror annotation;
        List<? extends TypeMirror> superTypes = types.directSupertypes(type);
        TypeMirror superClass = null;
        for (TypeMirror superType : superTypes) {
            ElementKind kind = types.asElement(superType).getKind();
            if (kind == ElementKind.CLASS) {
                superClass = superType;
                break;
            }
        }
        // Create annotation.
        if (superClass != null) {
            if (useFbc) {
                annotation = createFreeAnnotation(superClass);
            } else {
                annotation = createUnclassifiedAnnotation(superClass);
            }
        } else {
            // Use Object as a valid super-class.
            if (useFbc) {
                annotation = createFreeAnnotation(Object.class);
            } else {
                annotation = createUnclassifiedAnnotation(Object.class);
            }
        }
        return annotation;
    }

    /**
     * Returns the (non-static) fields that have the invariant annotation and are not yet
     * initialized in a given store.
     */
    public List<VariableTree> getUninitializedInvariantFields(
            Store store,
            TreePath path,
            boolean isStatic,
            List<? extends AnnotationMirror> receiverAnnotations) {
        ClassTree currentClass = TreeUtils.enclosingClass(path);
        List<VariableTree> fields = InitializationChecker.getAllFields(currentClass);
        List<VariableTree> violatingFields = new ArrayList<>();
        for (VariableTree field : fields) {
            if (isUnused(field, receiverAnnotations)) {
                continue; // don't consider unused fields
            }
            VariableElement fieldElem = TreeUtils.elementFromDeclaration(field);
            if (ElementUtils.isStatic(fieldElem) == isStatic) {
                // Does this field need to satisfy the invariant?
                if (hasFieldInvariantAnnotation(field)) {
                    // Has the field been initialized?
                    if (!store.isFieldInitialized(fieldElem)) {
                        violatingFields.add(field);
                    }
                }
            }
        }
        return violatingFields;
    }

    /**
     * Returns the (non-static) fields that have the invariant annotation and are initialized in a
     * given store.
     */
    public List<VariableTree> getInitializedInvariantFields(Store store, TreePath path) {
        // TODO: Instead of passing the TreePath around, can we use
        // getCurrentClassTree?
        ClassTree currentClass = TreeUtils.enclosingClass(path);
        List<VariableTree> fields = InitializationChecker.getAllFields(currentClass);
        List<VariableTree> initializedFields = new ArrayList<>();
        for (VariableTree field : fields) {
            VariableElement fieldElem = TreeUtils.elementFromDeclaration(field);
            if (!ElementUtils.isStatic(fieldElem)) {
                // Does this field need to satisfy the invariant?
                if (hasFieldInvariantAnnotation(field)) {
                    // Has the field been initialized?
                    if (store.isFieldInitialized(fieldElem)) {
                        initializedFields.add(field);
                    }
                }
            }
        }
        return initializedFields;
    }

    /** Returns whether the field {@code f} is unused, given the annotations on the receiver. */
    private boolean isUnused(
            VariableTree field, Collection<? extends AnnotationMirror> receiverAnnos) {
        if (receiverAnnos.isEmpty()) {
            return false;
        }

        AnnotationMirror unused =
                getDeclAnnotation(TreeUtils.elementFromDeclaration(field), Unused.class);
        if (unused == null) {
            return false;
        }

        Name when = AnnotationUtils.getElementValueClassName(unused, "when", false);
        for (AnnotationMirror anno : receiverAnnos) {
            Name annoName = ((TypeElement) anno.getAnnotationType().asElement()).getQualifiedName();
            if (annoName.contentEquals(when)) {
                return true;
            }
        }

        return false;
    }

    public boolean isInitializedForFrame(AnnotatedTypeMirror type, TypeMirror frame) {
        AnnotationMirror initializationAnno = type.getEffectiveAnnotationInHierarchy(UNCLASSIFIED);
        TypeMirror typeFrame = getTypeFrameFromAnnotation(initializationAnno);
        Types types = processingEnv.getTypeUtils();
        return types.isSubtype(typeFrame, types.erasure(frame));
    }

    /**
     * Determine the type of a field access (implicit or explicit) based on the receiver type and
     * the declared annotations for the field.
     *
     * @param type type of the field access expression
     * @param declaredFieldAnnotations annotations on the element
     * @param receiverType inferred annotations of the receiver
     */
    private void computeFieldAccessType(
            AnnotatedTypeMirror type,
            Collection<? extends AnnotationMirror> declaredFieldAnnotations,
            AnnotatedTypeMirror receiverType,
            AnnotatedTypeMirror fieldAnnotations,
            Element element) {
        // not necessary for primitive fields
        if (TypesUtils.isPrimitive(type.getUnderlyingType())) {
            return;
        }
        // not necessary if there is an explicit UnknownInitialization
        // annotation on the field
        if (AnnotationUtils.containsSameByName(fieldAnnotations.getAnnotations(), UNCLASSIFIED)) {
            return;
        }
        if (isUnclassified(receiverType) || isFree(receiverType)) {

            TypeMirror fieldDeclarationType = element.getEnclosingElement().asType();
            boolean isInitializedForFrame =
                    isInitializedForFrame(receiverType, fieldDeclarationType);
            if (isInitializedForFrame) {
                // The receiver is initialized for this frame.
                // Change the type of the field to @UnknownInitialization or @Raw so that
                // anything can be assigned to this field.
                type.replaceAnnotation(UNCLASSIFIED);
            } else if (computingAnnotatedTypeMirrorOfLHS) {
                // The receiver is not initialized for this frame, but the type of a lhs is being
                // computed.
                // Change the type of the field to @UnknownInitialization or @Raw so that
                // anything can be assigned to this field.
                type.replaceAnnotation(UNCLASSIFIED);
            } else {
                // The receiver is not initialized for this frame and the type being computed is not
                // a LHS.
                // Replace all annotations with the top annotation for that hierarchy.
                type.clearAnnotations();
                type.addAnnotations(qualHierarchy.getTopAnnotations());
            }

            if (!AnnotationUtils.containsSame(declaredFieldAnnotations, NOT_ONLY_COMMITTED)
                    || !useFbc) {
                // add root annotation for all other hierarchies, and
                // Committed for the commitment hierarchy
                type.replaceAnnotation(COMMITTED);
            }
        }
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new ListTypeAnnotator(
                super.createTypeAnnotator(), new CommitmentTypeAnnotator(this));
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                super.createTreeAnnotator(), new CommitmentTreeAnnotator(this));
    }

    protected class CommitmentTypeAnnotator extends TypeAnnotator {
        public CommitmentTypeAnnotator(
                InitializationAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitExecutable(AnnotatedExecutableType t, Void p) {
            Void result = super.visitExecutable(t, p);
            Element elem = t.getElement();
            if (elem.getKind() == ElementKind.CONSTRUCTOR) {
                AnnotatedDeclaredType returnType = (AnnotatedDeclaredType) t.getReturnType();
                DeclaredType underlyingType = returnType.getUnderlyingType();
                returnType.replaceAnnotation(getFreeOrRawAnnotationOfSuperType(underlyingType));
            }
            return result;
        }
    }

    protected class CommitmentTreeAnnotator extends TreeAnnotator {

        public CommitmentTreeAnnotator(
                InitializationAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitMethod(MethodTree node, AnnotatedTypeMirror p) {
            Void result = super.visitMethod(node, p);
            if (TreeUtils.isConstructor(node)) {
                assert p instanceof AnnotatedExecutableType;
                AnnotatedExecutableType exeType = (AnnotatedExecutableType) p;
                DeclaredType underlyingType =
                        (DeclaredType) exeType.getReturnType().getUnderlyingType();
                AnnotationMirror a = getFreeOrRawAnnotationOfSuperType(underlyingType);
                exeType.getReturnType().replaceAnnotation(a);
            }
            return result;
        }

        @Override
        public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror p) {
            super.visitNewClass(node, p);
            if (useFbc) {
                boolean allCommitted = true;
                Type type = ((JCTree) node).type;
                for (ExpressionTree a : node.getArguments()) {
                    final AnnotatedTypeMirror t = getAnnotatedType(a);
                    allCommitted &= (isCommitted(t) || isFbcBottom(t));
                }
                if (!allCommitted) {
                    p.replaceAnnotation(createFreeAnnotation(type));
                    return null;
                }
            }
            p.replaceAnnotation(COMMITTED);
            return null;
        }

        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (tree.getKind() != Tree.Kind.NULL_LITERAL) {
                type.addAnnotation(COMMITTED);
            }
            return super.visitLiteral(tree, type);
        }
    }

    /**
     * The {@link QualifierHierarchy} for the initialization type system. Type systems extending the
     * Initialization Checker should call methods {@link
     * InitializationQualifierHierarchy#isSubtypeInitialization(AnnotationMirror, AnnotationMirror)}
     * and {@link InitializationQualifierHierarchy#leastUpperBoundInitialization(AnnotationMirror,
     * AnnotationMirror)} for appropriate qualifiers. See protected subclass
     * NullnessQualifierHierarchy within class {@link
     * org.checkerframework.checker.nullness.AbstractNullnessChecker} for an example.
     */
    protected abstract class InitializationQualifierHierarchy extends MultiGraphQualifierHierarchy {

        public InitializationQualifierHierarchy(MultiGraphFactory f, Object... arg) {
            super(f, arg);
        }

        /**
         * Subtype testing for initialization annotations. Will return false if either qualifier is
         * not an initialization annotation. Subclasses should override isSubtype and call this
         * method for initialization qualifiers.
         */
        public boolean isSubtypeInitialization(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (!isInitializationAnnotation(rhs) || !isInitializationAnnotation(lhs)) {
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

            // @FBCBottom is a supertype of nothing.
            if (isFbcBottom(lhs)) {
                return false;
            }
            // @FBCBottom is a subtype of everything.
            if (isFbcBottom(rhs)) {
                return true;
            }
            boolean unc1 = isUnclassified(rhs);
            boolean unc2 = isUnclassified(lhs);
            boolean free1 = isFree(rhs);
            boolean free2 = isFree(lhs);

            // @Initialized is only a subtype of @UnknownInitialization.
            if (isCommitted(rhs)) {
                return unc2;
            }
            // @UnknownInitialization is not a subtype of @UnderInitialization.
            if (unc1 && free2) {
                return false;
            }
            // Now, either both annotations are @UnderInitialization, both annotations are
            // @UnknownInitialization or anno1 is @UnderInitialization and anno2 is
            // @UnknownInitialization.
            assert (free1 && free2) || (unc1 && unc2) || (free1 && unc2);
            // Thus, we only need to look at the type frame.
            TypeMirror frame1 = getTypeFrameFromAnnotation(rhs);
            TypeMirror frame2 = getTypeFrameFromAnnotation(lhs);
            return types.isSubtype(frame1, frame2);
        }

        /**
         * Compute the least upper bound of two initialization qualifiers. Returns null if one of
         * the qualifiers is not in the initialization hierarachy. Subclasses should override
         * leastUpperBound and call this method for initialization qualifiers.
         *
         * @param anno1 an initialization qualifier
         * @param anno2 an initialization qualifier
         * @return the lub of anno1 and anno2
         */
        protected AnnotationMirror leastUpperBoundInitialization(
                AnnotationMirror anno1, AnnotationMirror anno2) {
            if (!isInitializationAnnotation(anno1) || !isInitializationAnnotation(anno2)) {
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
                return createFreeAnnotation(
                        lubTypeFrame(
                                getTypeFrameFromAnnotation(anno1),
                                getTypeFrameFromAnnotation(anno2)));
            }

            assert (unc1 || free1) && (unc2 || free2);
            return createUnclassifiedAnnotation(
                    lubTypeFrame(
                            getTypeFrameFromAnnotation(anno1), getTypeFrameFromAnnotation(anno2)));
        }

        /** Returns the least upper bound of two types. */
        protected TypeMirror lubTypeFrame(TypeMirror a, TypeMirror b) {
            if (types.isSubtype(a, b)) {
                return b;
            } else if (types.isSubtype(b, a)) {
                return a;
            }

            return TypesUtils.leastUpperBound(a, b, processingEnv);
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror anno1, AnnotationMirror anno2) {
            return super.greatestLowerBound(anno1, anno2);
        }
    }
}
