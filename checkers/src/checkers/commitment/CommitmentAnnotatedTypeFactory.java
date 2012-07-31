package checkers.commitment;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import checkers.basetype.BaseTypeChecker;
import checkers.commitment.quals.Free;
import checkers.commitment.quals.NotOnlyCommitted;
import checkers.commitment.quals.Unclassified;
import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFValue;
import checkers.types.AbstractBasicAnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.TreeAnnotator;
import checkers.types.TypeAnnotator;
import checkers.util.AnnotationUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;

public abstract class CommitmentAnnotatedTypeFactory<Checker extends CommitmentChecker, Transfer extends CommitmentTransfer<Transfer>, Flow extends CFAbstractAnalysis<CFValue, CommitmentStore, Transfer>>
        extends
        AbstractBasicAnnotatedTypeFactory<Checker, CFValue, CommitmentStore, Transfer, Flow> {

    /** The annotations */
    public final AnnotationMirror COMMITTED, NOT_ONLY_COMMITTED;

    public CommitmentAnnotatedTypeFactory(Checker checker,
            CompilationUnitTree root) {
        super(checker, root, true);

        COMMITTED = checker.COMMITTED;
        NOT_ONLY_COMMITTED = checker.NOT_ONLY_COMMITTED;
    }

    public AnnotatedTypeMirror getUnalteredAnnotatedType(Tree tree) {
        return super.getAnnotatedType(tree);
    }

    // left in because it may be useful in the future for determining if you can
    // safely apply class frame types
    protected boolean areAllFieldsCommittedOnly(ClassTree classTree) {
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
    protected void postAsMemberOf(AnnotatedTypeMirror type,
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
        if (enclosingMethod != null && TreeUtils.isConstructor(enclosingMethod)) {
            ClassTree enclosingClass = TreeUtils.enclosingClass(path);
            Type classType = ((JCTree) enclosingClass).type;

            // If all fields are committed-only, and they are all initialized,
            // then it is save to switch to @Free(CurrentClass).
            if (areAllFieldsCommittedOnly(enclosingClass)) {
                CommitmentStore store = getStoreBefore(tree);
                if (store != null) {
                    if (getUninitializedInvariantFields(store, path).size() == 0) {
                        AnnotationMirror annotation = checker.createFreeAnnotation(classType);
                        selfType.replaceAnnotation(annotation);
                    }
                }
            }

            // Find the super-class (if any)
            List<? extends TypeMirror> superTypes = types
                    .directSupertypes(classType);
            TypeMirror superClass = null;
            for (TypeMirror superType : superTypes) {
                ElementKind kind = types.asElement(superType).getKind();
                if (kind == ElementKind.CLASS) {
                    superClass = superType;
                    break;
                }
            }
            // Create annotation.
            AnnotationMirror annotation;
            if (superClass != null) {
                annotation = checker.createFreeAnnotation(superClass);
            } else {
                // Use Object as a valid super-class
                annotation = checker.createFreeAnnotation(Object.class);
            }
            selfType.replaceAnnotation(annotation);
        }
        return selfType;
    }

    /**
     * Returns the set of fields that have the invariant annotation and are not yet initialized in a given store.
     */
    public Set<VariableTree> getUninitializedInvariantFields(
            CommitmentStore store, TreePath path) {
        ClassTree currentClass = TreeUtils.enclosingClass(path);
        Set<VariableTree> fields = CommitmentChecker
                .getAllFields(currentClass);
        Set<VariableTree> violatingFields = new HashSet<>();
        AnnotationMirror invariant = checker.getFieldInvariantAnnotation();
        for (VariableTree field : fields) {
            // Does this field need to satisfy the invariant?
            if (getAnnotatedType(field).hasAnnotation(
                    invariant)) {
                // Has the field been initialized?
                if (!store.isFieldInitialized(TreeUtils
                        .elementFromDeclaration(field))) {
                    violatingFields.add(field);
                }
            }
        }
        return violatingFields;
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
        if (receiverType.hasAnnotation(Unclassified.class)
                || receiverType.hasAnnotation(Free.class)) {

            type.clearAnnotations();
            type.addAnnotations(qualHierarchy.getTopAnnotations());

            if (!AnnotationUtils.containsSame(declaredFieldAnnotations,
                    NOT_ONLY_COMMITTED)) {
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
            super(checker);
        }

        @Override
        public Void visitExecutable(AnnotatedExecutableType t, ElementKind p) {
            Void result = super.visitExecutable(t, p);
            if (p == ElementKind.CONSTRUCTOR) {
                AnnotatedDeclaredType receiverType = t.getReceiverType();
                DeclaredType underlyingType = receiverType.getUnderlyingType();
                receiverType.replaceAnnotation(checker
                        .createFreeAnnotation(underlyingType));
            }
            return result;
        }
    }

    protected class CommitmentTreeAnnotator extends TreeAnnotator {

        public CommitmentTreeAnnotator(BaseTypeChecker checker) {
            super(checker, CommitmentAnnotatedTypeFactory.this);
        }

        @Override
        public Void visitMethod(MethodTree node, AnnotatedTypeMirror p) {
            Void result = super.visitMethod(node, p);
            if (TreeUtils.isConstructor(node)) {
                assert p instanceof AnnotatedExecutableType;
                AnnotatedExecutableType exeType = (AnnotatedExecutableType) p;
                DeclaredType underlyingType = exeType.getReceiverType()
                        .getUnderlyingType();
                AnnotationMirror a = checker
                        .createFreeAnnotation(underlyingType);
                exeType.getReceiverType().replaceAnnotation(a);
            }
            return result;
        }

        @Override
        public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror p) {
            super.visitNewClass(node, p);
            boolean allCommitted = true;
            Type type = ((JCTree) node).type;
            for (ExpressionTree a : node.getArguments()) {
                allCommitted = allCommitted
                        && getAnnotatedType(a).hasAnnotation(COMMITTED);
            }
            if (!allCommitted) {
                p.replaceAnnotation(checker.createFreeAnnotation(type));
            }
            return null;
        }
    }
}
