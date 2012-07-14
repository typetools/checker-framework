package checkers.commitment;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import checkers.basetype.BaseTypeVisitor;
import checkers.source.Result;
import checkers.types.AbstractBasicAnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.AnnotationUtils;
import checkers.util.ElementUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;

// TODO/later: documentation
public class CommitmentVisitor<Checker extends CommitmentChecker> extends
        BaseTypeVisitor<Checker> {

    // Error message keys
    private static final String COMMITMENT_INVALID_CAST = "commitment.invalid.cast";
    private static final String COMMITMENT_FIELDS_UNINITIALIZED = "commitment.fields.uninitialized";
    private static final String COMMITMENT_INVALID_FIELD_ANNOTATION = "commitment.invalid.field.annotation";
    private static final String CONSTRUCTOR_RETURN_TYPE_FORBIDDEN = "constructor.return.type.forbidden";
    private static final String COMMITMENT_REDUNDANT_CONSTRUCTOR_RETURN_TYPE = "commitment.redundant.constructor.return.type";
    private static final String COMMITMENT_INVALID_CONSTRUCTOR_RETRUN_TYPE = "commitment.invalid.constructor.return.type";
    private static final String COMMITMENT_INVALID_FIELD_WRITE_UNCLASSIFIED = "commitment.invalid.field.write.unclassified";
    private static final String COMMITMENT_INVALID_FIELD_WRITE_COMMITTED = "commitment.invalid.field.write.committed";

    // Annotation constants
    protected final AnnotationMirror COMMITTED, FREE, UNCLASSIFIED;

    public CommitmentVisitor(Checker checker, CompilationUnitTree root) {
        super(checker, root);
        COMMITTED = checker.COMMITTED;
        FREE = checker.FREE;
        UNCLASSIFIED = checker.UNCLASSIFIED;

        checkForAnnotatedJdk();
    }

    @Override
    protected boolean checkConstructorInvocation(AnnotatedDeclaredType dt,
            AnnotatedExecutableType constructor, Tree src) {
        // receiver annotations for constructors are forbidden, therefore no
        // check is necessary
        return true;
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType) {
        return true;
    }

    @Override
    protected void commonAssignmentCheck(Tree varTree, ExpressionTree valueExp,
            String errorKey) {
        // field write of the form x.f = y
        if (TreeUtils.isFieldAccess(varTree)) {
            // cast is safe: a field access can only be an IdentifierTree or
            // MemberSelectTree
            ExpressionTree lhs = (ExpressionTree) varTree;
            ExpressionTree y = valueExp;
            Element el = TreeUtils.elementFromUse(lhs);
            AnnotatedTypeMirror xType = atypeFactory.getReceiverType(lhs);
            AnnotatedTypeMirror yType = atypeFactory.getAnnotatedType(y);
            if (!ElementUtils.isStatic(el)
                    && !(yType.hasAnnotation(COMMITTED) || xType
                            .hasAnnotation(FREE))) {
                String err;
                if (xType.hasAnnotation(COMMITTED)) {
                    err = COMMITMENT_INVALID_FIELD_WRITE_COMMITTED;
                } else {
                    err = COMMITMENT_INVALID_FIELD_WRITE_UNCLASSIFIED;
                }
                checker.report(Result.failure(err, varTree), varTree);
                return; // prevent issuing another errow about subtyping
            }
        }
        super.commonAssignmentCheck(varTree, valueExp, errorKey);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        // TODO: this is a hack, it seems like this could be done at the
        // TypeFactory level.
        ExecutableElement elt = TreeUtils.elementFromUse(node);
        if (elt.getSimpleName().toString().equals("<init>")) {
            return p;
        }
        return super.visitMethodInvocation(node, p);
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        // is this a field (and not a local variable)?
        if (TreeUtils.elementFromDeclaration(node).getKind().isField()) {
            Set<AnnotationMirror> annotationMirrors = atypeFactory
                    .getAnnotatedType(node).getExplicitAnnotations();
            // Fields cannot have commitment annotations.
            for (AnnotationMirror a : checker.getCommitmentAnnotations()) {
                if (AnnotationUtils.containsSame(annotationMirrors, a)) {
                    checker.report(Result.failure(
                            COMMITMENT_INVALID_FIELD_ANNOTATION, node), node);
                    break;
                }
            }
        }
        return super.visitVariable(node, p);
    }

    @Override
    public Void visitTypeCast(TypeCastTree node, Void p) {
        AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(node
                .getExpression());
        AnnotatedTypeMirror castType = atypeFactory.getAnnotatedType(node);
        AnnotationMirror exprAnno = null, castAnno = null;

        // find commitment annotation
        for (AnnotationMirror a : checker.getCommitmentAnnotations()) {
            if (castType.hasAnnotation(a)) {
                assert castAnno == null;
                castAnno = a;
            }
            if (exprType.hasAnnotation(a)) {
                assert exprAnno == null;
                exprAnno = a;
            }
        }

        // TODO: this is most certainly unsafe!! (and may be hiding some
        // problems)
        // If we don't find a commitment annotation, then we just assume that
        // the subtyping is alright
        // The case that has come up is with wildcards not getting a type for
        // some reason, even though
        // the default is @Committed.
        boolean isSubtype;
        if (exprAnno == null || castAnno == null) {
            isSubtype = true;
        } else {
            assert exprAnno != null && castAnno != null;
            isSubtype = checker.getQualifierHierarchy().isSubtype(exprAnno,
                    castAnno);
        }

        if (!isSubtype) {
            checker.report(Result.failure(COMMITMENT_INVALID_CAST, node), node);
            return p; // suppress cast.unsafe warning
        }

        return super.visitTypeCast(node, p);
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        if (TreeUtils.isConstructor(node)) {
            Collection<? extends AnnotationMirror> returnTypeAnnotations = atypeFactory
                    .getAnnotatedType(node).getReturnType()
                    .getExplicitAnnotations();
            // check for invalid constructor return type
            for (AnnotationMirror a : checker
                    .getInvalidConstructorReturnTypeAnnotations()) {
                if (AnnotationUtils.containsSame(returnTypeAnnotations, a)) {
                    checker.report(Result.failure(
                            CONSTRUCTOR_RETURN_TYPE_FORBIDDEN, node), node);
                    break;
                }
            }
            if (AnnotationUtils.containsSame(returnTypeAnnotations, COMMITTED)
                    || AnnotationUtils.containsSame(returnTypeAnnotations,
                            UNCLASSIFIED)) {
                checker.report(Result.failure(
                        COMMITMENT_INVALID_CONSTRUCTOR_RETRUN_TYPE, node), node);
            }
            if (AnnotationUtils.containsSame(returnTypeAnnotations, FREE)) {
                checker.report(Result.warning(
                        COMMITMENT_REDUNDANT_CONSTRUCTOR_RETURN_TYPE, node),
                        node);
            }

            // Check that all fields have been initialized at the end of the
            // constructor.
            ClassTree currentClass = TreeUtils.enclosingClass(getCurrentPath());
            Set<VariableTree> fields = CommitmentChecker
                    .getAllFields(currentClass);
            Set<VariableTree> violatingFields = new HashSet<>();
            AnnotationMirror invariant = checker.getFieldInvariantAnnotations();
            // TODO: we should not need to cast here?
            @SuppressWarnings("unchecked")
            AbstractBasicAnnotatedTypeFactory<?, ?, CommitmentStore, ?, ?> factory = (AbstractBasicAnnotatedTypeFactory<?, ?, CommitmentStore, ?, ?>) atypeFactory;
            CommitmentStore store = factory.getRegularExitStore(node);
            for (VariableTree field : fields) {
                // Does this field need to satisfy the invariant?
                if (factory.getAnnotatedType(field).hasAnnotation(invariant)) {
                    // Has the field been initialized?
                    if (!store.isFieldInitialized(TreeUtils
                            .elementFromDeclaration(field))) {
                        violatingFields.add(field);
                    }
                }
            }
            if (!violatingFields.isEmpty()) {
                StringBuilder fieldsString = new StringBuilder();
                boolean first = true;
                for (VariableTree f : violatingFields) {
                    if (!first) {
                        fieldsString.append(", ");
                    }
                    first = false;
                    fieldsString.append(f.getName());
                }
                checker.report(Result.failure(COMMITMENT_FIELDS_UNINITIALIZED,
                        fieldsString), node);
            }
        }
        return super.visitMethod(node, p);
    }
}
