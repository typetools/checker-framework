package checkers.commitment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

import checkers.basetype.BaseTypeChecker;
import checkers.commitment.quals.NotOnlyCommitted;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.BasicAnnotatedTypeFactory;
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

public class CommitmentAnnotatedTypeFactory<Checker extends CommitmentChecker>
        extends BasicAnnotatedTypeFactory<Checker> {

    /** The annotations */
    public final AnnotationMirror COMMITTED, FREE, UNCLASSIFIED,
            NOT_ONLY_COMMITTED;

    @SuppressWarnings("rawtypes")
    protected Map<Class, Set<Class>> ourAliases = new HashMap<>();

    public CommitmentAnnotatedTypeFactory(Checker checker,
            CompilationUnitTree root) {
        super(checker, root, true);

        COMMITTED = checker.COMMITTED;
        FREE = checker.FREE;
        UNCLASSIFIED = checker.UNCLASSIFIED;
        NOT_ONLY_COMMITTED = checker.NOT_ONLY_COMMITTED;
    }

    public AnnotatedTypeMirror getUnalteredAnnotatedType(Tree tree) {
        return super.getAnnotatedType(tree);
    }

    @Override
    public AnnotatedDeclaredType getSelfType(Tree tree) {
        AnnotatedDeclaredType selfType = super.getSelfType(tree);
        MethodTree enclosingMethod = TreeUtils.enclosingMethod(getPath(tree));
        // TODO: handle the case where 'this' is used in field initializer
        if (enclosingMethod != null && TreeUtils.isConstructor(enclosingMethod)) {
            // NonNullFlow nonNullFlow = getFlow();
            // if (nonNullFlow.doAllFieldsSatisfyInvariant(tree)
            // && areAllFieldsCommittedOnly(tree)) {
            // TODO add class frame type
            // } else {
            changeAnnotationInOneHierarchy(selfType, FREE);
            // }
        }
        return selfType;
    }

    // left in because it may be useful in the future for determining if you can
    // safely apply class frame types
    @SuppressWarnings("unused")
    private boolean areAllFieldsCommittedOnly(Tree tree) {
        ClassTree classTree = TreeUtils.enclosingClass(getPath(tree));

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

    /**
     * {@inheritDoc}
     * 
     * <p>
     * 
     * In most cases, subclasses want to call this method first because it may
     * clear all annotations and use the hierarchy's root annotations (as part
     * of the call to postAsMemberOf).
     * 
     */
    @Override
    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type) {
        super.annotateImplicit(tree, type);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * 
     * In most cases, subclasses want to call this method first because it may
     * clear all annotations and use the hierarchy's root annotations.
     * 
     */

    protected boolean HACK_DONT_CALL_POST_AS_MEMBER = false;

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
        if (receiverType.hasAnnotation(UNCLASSIFIED)
                || receiverType.hasAnnotation(FREE)) {

            type.clearAnnotations();
            type.addAnnotations(qualHierarchy.getRootAnnotations());

            if (!AnnotationUtils.containsSame(declaredFieldAnnotations,
                    NOT_ONLY_COMMITTED)) {
                // add root annotation for all other hierarchies, and
                // Committed for the commitment hierarchy
                type.removeAnnotation(UNCLASSIFIED);
                type.addAnnotation(COMMITTED);
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
                changeAnnotationInOneHierarchy(exeType.getReceiverType(), FREE);
                // TODO: find out why this doesn't allow for this() constructor
                // to be called from another constructor (the @Free annotation
                // doesn't stay?)
            }
            return result;
        }

        @Override
        public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror p) {
            super.visitNewClass(node, p);
            boolean allCommitted = true;
            for (ExpressionTree a : node.getArguments()) {
                allCommitted = allCommitted
                        && getAnnotatedType(a).hasAnnotation(COMMITTED);
            }
            if (!allCommitted) {
                changeAnnotationInOneHierarchy(p, FREE);
            }
            return null;
        }

    }

    /**
     * Replace the currently present annotation from the type hierarchy of a
     * from type and add a instead.
     * 
     * @param type
     *            The type to modify.
     * @param a
     *            The annotation that should be present afterwards.
     */
    protected void changeAnnotationInOneHierarchy(AnnotatedTypeMirror type,
            AnnotationMirror a) {
        // Is this different from
        //   type.removeAnnotationInHierarchy(a)
        //   type.addAnnotation(a)
        // Instead of these two steps, should we introduce
        //   type.replaceAnnotation(a)
        // ?
        // Also, this code only removes commitment annotations, which
        // is not clear from the documentation.
        for (AnnotationMirror other : checker.getCommitmentAnnotations()) {
            type.removeAnnotation(other);
        }
        type.addAnnotation(a);
    }

}
