package checkers.commitment;

import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

import checkers.basetype.BaseTypeChecker;
import checkers.commitment.quals.NotOnlyCommitted;
import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFValue;
import checkers.types.AbstractBasicAnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
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

public abstract class CommitmentAnnotatedTypeFactory<Checker extends CommitmentChecker, Transfer extends CommitmentTransfer<Transfer>, Flow extends CFAbstractAnalysis<CFValue, CommitmentStore, Transfer>>
        extends
        AbstractBasicAnnotatedTypeFactory<Checker, CFValue, CommitmentStore, Transfer, Flow> {

    /** The annotations */
    public final AnnotationMirror COMMITTED, FREE, UNCLASSIFIED,
            NOT_ONLY_COMMITTED;

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
                exeType.getReceiverType().replaceAnnotation(FREE);
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
                p.replaceAnnotation(FREE);
            }
            return null;
        }
    }
}
