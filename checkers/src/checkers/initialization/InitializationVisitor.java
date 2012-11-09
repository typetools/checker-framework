package checkers.initialization;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

import javacutils.AnnotationUtils;
import javacutils.ElementUtils;
import javacutils.Pair;
import javacutils.TreeUtils;

import checkers.basetype.BaseTypeVisitor;
import checkers.flow.analysis.checkers.CFValue;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.Tree.Kind;

// TODO/later: documentation
public class InitializationVisitor<Checker extends InitializationChecker>
        extends BaseTypeVisitor<Checker> {

    // Error message keys
    private static final String COMMITMENT_INVALID_CAST = "commitment.invalid.cast";
    private static final String COMMITMENT_FIELDS_UNINITIALIZED = "commitment.fields.uninitialized";
    private static final String COMMITMENT_INVALID_FIELD_ANNOTATION = "commitment.invalid.field.annotation";
    private static final String COMMITMENT_INVALID_CONSTRUCTOR_RETRUN_TYPE = "commitment.invalid.constructor.return.type";
    private static final String COMMITMENT_INVALID_FIELD_WRITE_UNCLASSIFIED = "commitment.invalid.field.write.unclassified";
    private static final String COMMITMENT_INVALID_FIELD_WRITE_COMMITTED = "commitment.invalid.field.write.committed";

    /** A better typed version of the ATF. */
    protected final InitializationAnnotatedTypeFactory<?, ?, ?> factory = (InitializationAnnotatedTypeFactory<?, ?, ?>) atypeFactory;

    public InitializationVisitor(Checker checker, CompilationUnitTree root) {
        super(checker, root);
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
    protected void commonAssignmentCheck(Tree varTree, ExpressionTree valueExp,
            String errorKey) {
        // field write of the form x.f = y
        if (TreeUtils.isFieldAccess(varTree)) {
            // cast is safe: a field access can only be an IdentifierTree or
            // MemberSelectTree
            ExpressionTree lhs = (ExpressionTree) varTree;
            ExpressionTree y = valueExp;
            Element el = TreeUtils.elementFromUse(lhs);
            AnnotatedTypeMirror xType = factory.getReceiverType(lhs);
            AnnotatedTypeMirror yType = factory.getAnnotatedType(y);
            if (!ElementUtils.isStatic(el)
                    && !(checker.isCommitted(yType) || checker.isFree(xType))) {
                String err;
                if (checker.isCommitted(xType)) {
                    err = COMMITMENT_INVALID_FIELD_WRITE_COMMITTED;
                } else {
                    err = COMMITMENT_INVALID_FIELD_WRITE_UNCLASSIFIED;
                }
                checker.report(Result.failure(err, varTree), varTree);
                return; // prevent issuing another errow about subtyping
            }
            // for field access on the current object, make sure that we don't
            // allow
            // invalid assignments. that is, even though reading this.f in a
            // constructor yields @Nullable (or similar for other typesystems),
            // it
            // is not allowed to write @Nullable to a @NonNull field.
            // This is done by first getting the type as usual (var), and then
            // again not using the postAsMember method (which takes care of
            // transforming the type of o.f for a free receiver to @Nullable)
            // (var2). Then, we take the child annotation from var2 and use it
            // for var.
            AnnotatedTypeMirror var = atypeFactory.getAnnotatedType(lhs);
            boolean old = factory.HACK_DONT_CALL_POST_AS_MEMBER;
            factory.HACK_DONT_CALL_POST_AS_MEMBER = true;
            boolean old2 = factory.shouldReadCache;
            factory.shouldReadCache = false;
            AnnotatedTypeMirror var2 = atypeFactory.getAnnotatedType(lhs);
            factory.HACK_DONT_CALL_POST_AS_MEMBER = old;
            factory.shouldReadCache = old2;
            var.replaceAnnotation(var2.getEffectiveAnnotationInHierarchy(checker
                    .getFieldInvariantAnnotation()));
            checkAssignability(var, varTree);
            commonAssignmentCheck(var, valueExp, errorKey, false);
            return;
        }
        super.commonAssignmentCheck(varTree, valueExp, errorKey);
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        // is this a field (and not a local variable)?
        if (TreeUtils.elementFromDeclaration(node).getKind().isField()) {
            Set<AnnotationMirror> annotationMirrors = factory.getAnnotatedType(
                    node).getExplicitAnnotations();
            // Fields cannot have commitment annotations.
            for (Class<? extends Annotation> c : checker
                    .getInitializationAnnotations()) {
                for (AnnotationMirror a : annotationMirrors) {
                    if (AnnotationUtils.areSameByClass(a, c)) {
                        checker.report(Result.failure(
                                COMMITMENT_INVALID_FIELD_ANNOTATION, node),
                                node);
                        break;
                    }
                }
            }
        }
        return super.visitVariable(node, p);
    }

    @Override
    public Void visitTypeCast(TypeCastTree node, Void p) {
        AnnotatedTypeMirror exprType = factory.getAnnotatedType(node
                .getExpression());
        AnnotatedTypeMirror castType = factory.getAnnotatedType(node);
        AnnotationMirror exprAnno = null, castAnno = null;

        // find commitment annotation
        for (Class<? extends Annotation> a : checker.getInitializationAnnotations()) {
            if (castType.hasAnnotation(a)) {
                assert castAnno == null;
                castAnno = castType.getAnnotation(a);
            }
            if (exprType.hasAnnotation(a)) {
                assert exprAnno == null;
                exprAnno = exprType.getAnnotation(a);
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
    public Void visitBlock(BlockTree node, Void p) {
        ClassTree enclosingClass = TreeUtils.enclosingClass(getCurrentPath());
        // Is this a initializer block?
        if (enclosingClass.getMembers().contains(node)) {
            if (node.isStatic()) {
                boolean isStatic = true;
                InitializationStore store = factory.getRegularExitStore(node);
                // Add field values for fields with an initializer.
                for (Pair<VariableElement, CFValue> t : store.getAnalysis()
                        .getFieldValues()) {
                    store.addInitializedField(t.first);
                }
                // Check that all static fields are initialized.
                checkFieldsInitialized(node, isStatic, store);
            }
        }
        return super.visitBlock(node, p);
    }

    @Override
    public Void visitClass(ClassTree node, Void p) {
        Void result = super.visitClass(node, p);

        // Is there a static initializer block?
        boolean hasStaticInitializer = false;
        for (Tree t : node.getMembers()) {
            switch (t.getKind()) {
            case BLOCK:
                if (((BlockTree) t).isStatic()) {
                    hasStaticInitializer = true;
                }
                break;

            default:
                break;
            }
        }

        // Warn about uninitialized static fields if there is no static
        // initializer (otherwise, errors are reported there).
        if (!hasStaticInitializer && node.getKind() == Kind.CLASS) {
            boolean isStatic = true;
            InitializationStore store = factory.getEmptyStore();
            // Add field values for fields with an initializer.
            for (Pair<VariableElement, CFValue> t : store.getAnalysis()
                    .getFieldValues()) {
                store.addInitializedField(t.first);
            }
            checkFieldsInitialized(node, isStatic, store);
        }

        return result;
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        if (TreeUtils.isConstructor(node)) {
            Collection<? extends AnnotationMirror> returnTypeAnnotations = getExplicitReturnTypeAnnotations(node);
            // check for invalid constructor return type
            for (Class<? extends Annotation> c : checker
                    .getInvalidConstructorReturnTypeAnnotations()) {
                for (AnnotationMirror a : returnTypeAnnotations) {
                    if (AnnotationUtils.areSameByClass(a, c)) {
                        checker.report(Result.failure(
                                COMMITMENT_INVALID_CONSTRUCTOR_RETRUN_TYPE,
                                node), node);
                        break;
                    }
                }
            }

            // Check that all fields have been initialized at the end of the
            // constructor.
            boolean isStatic = false;
            InitializationStore store = factory.getRegularExitStore(node);
            checkFieldsInitialized(node, isStatic, store);
        }
        return super.visitMethod(node, p);
    }

    /**
     * Checks that all fields (all static fields if {@code staticFields} is
     * true) are initialized in the given store.
     */
    protected void checkFieldsInitialized(Tree blockNode, boolean staticFields,
            InitializationStore store) {
        // If the store is null, then the constructor cannot terminate
        // successfully
        if (store != null) {
            Set<VariableTree> violatingFields = factory
                    .getUninitializedInvariantFields(store, getCurrentPath(),
                            staticFields);
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
                        fieldsString), blockNode);
            }
        }
    }

    public Set<AnnotationMirror> getExplicitReturnTypeAnnotations(
            MethodTree node) {
        AnnotatedTypeMirror t = factory.fromMember(node);
        assert t instanceof AnnotatedExecutableType;
        AnnotatedExecutableType type = (AnnotatedExecutableType) t;
        return type.getReturnType().getAnnotations();
    }
}
