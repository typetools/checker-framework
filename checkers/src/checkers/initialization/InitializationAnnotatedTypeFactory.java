package checkers.initialization;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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

import javacutils.AnnotationUtils;
import javacutils.ElementUtils;
import javacutils.TreeUtils;
import javacutils.TypesUtils;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFValue;
import checkers.initialization.quals.Free;
import checkers.initialization.quals.NotOnlyCommitted;
import checkers.initialization.quals.Unclassified;
import checkers.quals.Unused;
import checkers.types.AbstractBasicAnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.TreeAnnotator;
import checkers.types.TypeAnnotator;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;

public abstract class InitializationAnnotatedTypeFactory<Checker extends InitializationChecker, Transfer extends InitializationTransfer<Transfer>, Flow extends CFAbstractAnalysis<CFValue, InitializationStore, Transfer>>
        extends
        AbstractBasicAnnotatedTypeFactory<Checker, CFValue, InitializationStore, Transfer, Flow> {

    /** The annotations */
    public final AnnotationMirror COMMITTED, NOT_ONLY_COMMITTED;

    /**
     * Should the initialization type system be FBC? If not, the rawness type
     * system is used for initialization.
     */
    public final boolean useFbc;

    public InitializationAnnotatedTypeFactory(Checker checker,
            CompilationUnitTree root) {
        super(checker, root, true);

        COMMITTED = checker.COMMITTED;
        NOT_ONLY_COMMITTED = checker.NOT_ONLY_COMMITTED;
        useFbc = checker.useFbc;

        if (!useFbc) {
            addAliasedAnnotation(checkers.nullness.quals.Raw.class,
                    checker.createUnclassifiedAnnotation(Object.class));
        }
        addAliasedAnnotation(checkers.nullness.quals.NonRaw.class,
                checker.COMMITTED);
    }

    public AnnotatedTypeMirror getUnalteredAnnotatedType(Tree tree) {
        return super.getAnnotatedType(tree);
    }

    /**
     * Are all fields committed-only?
     */
    protected boolean areAllFieldsCommittedOnly(ClassTree classTree) {
        if (!useFbc) {
            // In the rawness type system, no fields can store not fully
            // initialized objects.
            return true;
        }
        for (Tree member : classTree.getMembers()) {
            if (!member.getKind().equals(Tree.Kind.VARIABLE))
                continue;
            VariableTree var = (VariableTree) member;
            VariableElement varElt = TreeUtils.elementFromDeclaration(var);
            // var is not committed-only
            if (getDeclAnnotation(varElt, NotOnlyCommitted.class) != null) {
                // var is not static -- need a check of initializer blocks,
                // not of constructor which is where this is used
                if (!varElt.getModifiers().contains(Modifier.STATIC)) {
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean HACK_DONT_CALL_POST_AS_MEMBER = false;

    /**
     * {@inheritDoc}
     *
     * <p>
     *
     * In most cases, subclasses want to call this method first because it may
     * clear all annotations and use the hierarchy's root annotations.
     *
     */
    @Override
    public void postAsMemberOf(AnnotatedTypeMirror type,
            AnnotatedTypeMirror owner, Element element) {
        super.postAsMemberOf(type, owner, element);

        if (!HACK_DONT_CALL_POST_AS_MEMBER) {
            if (element.getKind().isField()) {
                Collection<? extends AnnotationMirror> declaredFieldAnnotations = getDeclAnnotations(element);
                computeFieldAccessType(type, declaredFieldAnnotations, owner);
            }
        }
    }

    @Override
    public AnnotatedDeclaredType getSelfType(Tree tree) {
        AnnotatedDeclaredType selfType = super.getSelfType(tree);
        TreePath path = getPath(tree);
        MethodTree enclosingMethod = TreeUtils.enclosingMethod(path);
        // Set the correct type for 'this' inside of constructors.
        while (enclosingMethod != null) {
            if (!TreeUtils.isConstructor(enclosingMethod)) {
                // See whether any other enclosing method is a constructor.
                path = path.getParentPath();
                enclosingMethod = TreeUtils.enclosingMethod(path);
                continue;
            }
            ClassTree enclosingClass = TreeUtils.enclosingClass(path);
            Type classType = ((JCTree) enclosingClass).type;
            AnnotationMirror annotation = null;

            // If all fields are committed-only, and they are all initialized,
            // then it is save to switch to @Free(CurrentClass).
            if (areAllFieldsCommittedOnly(enclosingClass)) {
                InitializationStore store = getStoreBefore(tree);
                if (store != null) {
                    List<AnnotationMirror> annos = Collections.emptyList();
                    if (getUninitializedInvariantFields(store, path, false,
                            annos).size() == 0) {
                        if (useFbc) {
                            annotation = checker
                                    .createFreeAnnotation(classType);
                        } else {
                            annotation = checker
                                    .createUnclassifiedAnnotation(classType);
                        }
                        selfType.replaceAnnotation(annotation);
                    }
                }
            }

            if (annotation == null) {
                annotation = getFreeOrRawAnnotationOfSuperType(classType);
            }
            selfType.replaceAnnotation(annotation);
            // Found a constructor -> done.
            // TODO: should we look whether this constructor is
            // enclosed within another constructor?
            break;
        }
        return selfType;
    }

    /**
     * Returns a {@link Free} annotation (or {@link Unclassified} if rawness is
     * used) that has the supertype of {@code type} as type frame.
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
                annotation = checker.createFreeAnnotation(superClass);
            } else {
                annotation = checker.createUnclassifiedAnnotation(superClass);
            }
        } else {
            // Use Object as a valid super-class
            if (useFbc) {
                annotation = checker.createFreeAnnotation(Object.class);
            } else {
                annotation = checker.createUnclassifiedAnnotation(Object.class);
            }
        }
        return annotation;
    }

    /**
     * Returns the set of (non-static) fields that have the invariant annotation
     * and are not yet initialized in a given store.
     */
    public Set<VariableTree> getUninitializedInvariantFields(
            InitializationStore store, TreePath path, boolean isStatic,
            List<AnnotationMirror> receiverAnnotations) {
        ClassTree currentClass = TreeUtils.enclosingClass(path);
        Set<VariableTree> fields = InitializationChecker
                .getAllFields(currentClass);
        Set<VariableTree> violatingFields = new HashSet<>();
        AnnotationMirror invariant = checker.getFieldInvariantAnnotation();
        for (VariableTree field : fields) {
            if (ElementUtils.isStatic(TreeUtils.elementFromDeclaration(field)) == isStatic) {
                // Does this field need to satisfy the invariant?
                if (getAnnotatedType(field).hasAnnotation(invariant)) {
                    // Has the field been initialized?
                    if (!store.isFieldInitialized(TreeUtils
                            .elementFromDeclaration(field))) {
                        violatingFields.add(field);
                    }
                }
            }
        }
        return violatingFields;
    }

    /**
     * Returns whether the field {@code f} is unused, given the annotations on
     * the receiver.
     */
    private boolean isUnused(VariableElement f,
            Collection<? extends AnnotationMirror> receiverAnnos) {
        if (receiverAnnos.isEmpty()) {
            return false;
        }

        AnnotationMirror unused = getDeclAnnotation(f, Unused.class);
        if (unused == null)
            return false;

        Name when = AnnotationUtils.getElementValueClassName(unused, "when",
                false);
        for (AnnotationMirror anno : receiverAnnos) {
            Name annoName = ((TypeElement) anno.getAnnotationType().asElement())
                    .getQualifiedName();
            if (annoName.contentEquals(when)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determine the type of a field access (implicit or explicit) based on the
     * receiver type and the declared annotations for the field
     * (committed-only).
     *
     * @param type
     *            Type of the field access expression.
     * @param declaredFieldAnnotations
     *            Annotations on the element.
     * @param receiverType
     *            Inferred annotations of the receiver.
     */
    private void computeFieldAccessType(AnnotatedTypeMirror type,
            Collection<? extends AnnotationMirror> declaredFieldAnnotations,
            AnnotatedTypeMirror receiverType) {
        // not necessary for primitive fields
        if (TypesUtils.isPrimitive(type.getUnderlyingType())) {
            return;
        }
        if (checker.isUnclassified(receiverType)
                || checker.isFree(receiverType)) {

            type.clearAnnotations();
            type.addAnnotations(qualHierarchy.getTopAnnotations());

            if (!AnnotationUtils.containsSame(declaredFieldAnnotations,
                    NOT_ONLY_COMMITTED) || !useFbc) {
                // add root annotation for all other hierarchies, and
                // Committed for the commitment hierarchy
                type.replaceAnnotation(COMMITTED);
            }
        }
    }

    @Override
    protected TypeAnnotator createTypeAnnotator(Checker checker) {
        return new CommitmentTypeAnnotator(checker);
    }

    @Override
    protected TreeAnnotator createTreeAnnotator(Checker checker) {
        return new CommitmentTreeAnnotator(checker);
    }

    protected class CommitmentTypeAnnotator extends TypeAnnotator {
        public CommitmentTypeAnnotator(BaseTypeChecker checker) {
            super(checker, InitializationAnnotatedTypeFactory.this);
        }

        @Override
        public Void visitExecutable(AnnotatedExecutableType t, ElementKind p) {
            Void result = super.visitExecutable(t, p);
            if (p == ElementKind.CONSTRUCTOR) {
                AnnotatedDeclaredType receiverType = t.getReceiverType();
                DeclaredType underlyingType = receiverType.getUnderlyingType();
                receiverType
                        .replaceAnnotation(getFreeOrRawAnnotationOfSuperType(underlyingType));
            }
            return result;
        }
    }

    protected class CommitmentTreeAnnotator extends TreeAnnotator {

        public CommitmentTreeAnnotator(BaseTypeChecker checker) {
            super(checker, InitializationAnnotatedTypeFactory.this);
        }

        @Override
        public Void visitMethod(MethodTree node, AnnotatedTypeMirror p) {
            Void result = super.visitMethod(node, p);
            if (TreeUtils.isConstructor(node)) {
                assert p instanceof AnnotatedExecutableType;
                AnnotatedExecutableType exeType = (AnnotatedExecutableType) p;
                DeclaredType underlyingType = exeType.getReceiverType()
                        .getUnderlyingType();
                AnnotationMirror a = getFreeOrRawAnnotationOfSuperType(underlyingType);
                exeType.getReceiverType().replaceAnnotation(a);
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
                    allCommitted &= checker.isCommitted(getAnnotatedType(a));
                }
                if (!allCommitted) {
                    p.replaceAnnotation(checker.createFreeAnnotation(type));
                }
            }
            return null;
        }

        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            type.addAnnotation(COMMITTED);
            return super.visitLiteral(tree, type);
        }
    }
}
