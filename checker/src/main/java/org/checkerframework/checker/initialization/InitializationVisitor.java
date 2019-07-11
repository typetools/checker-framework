package org.checkerframework.checker.initialization;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.analysis.FlowExpressions.ClassName;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.LocalVariable;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.FlowExpressions.ThisReference;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotationFormatter;
import org.checkerframework.framework.util.DefaultAnnotationFormatter;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The visitor for the freedom-before-commitment type-system. The freedom-before-commitment
 * type-system and this class are abstract and need to be combined with another type-system whose
 * safe initialization should be tracked. For an example, see the {@link NullnessChecker}.
 */
public class InitializationVisitor<
                Factory extends InitializationAnnotatedTypeFactory<Value, Store, ?, ?>,
                Value extends CFAbstractValue<Value>,
                Store extends InitializationStore<Value, Store>>
        extends BaseTypeVisitor<Factory> {

    protected final AnnotationFormatter annoFormatter;

    // Error message keys
    private static final @CompilerMessageKey String COMMITMENT_INVALID_CAST =
            "initialization.invalid.cast";
    private static final @CompilerMessageKey String COMMITMENT_FIELDS_UNINITIALIZED =
            "initialization.fields.uninitialized";
    private static final @CompilerMessageKey String COMMITMENT_STATIC_FIELDS_UNINITIALIZED =
            "initialization.static.fields.uninitialized";
    private static final @CompilerMessageKey String COMMITMENT_INVALID_FIELD_TYPE =
            "initialization.invalid.field.type";
    private static final @CompilerMessageKey String COMMITMENT_INVALID_CONSTRUCTOR_RETURN_TYPE =
            "initialization.invalid.constructor.return.type";
    private static final @CompilerMessageKey String
            COMMITMENT_INVALID_FIELD_WRITE_UNKNOWN_INITIALIZATION =
                    "initialization.invalid.field.write.unknown";
    private static final @CompilerMessageKey String COMMITMENT_INVALID_FIELD_WRITE_INITIALIZED =
            "initialization.invalid.field.write.initialized";

    public InitializationVisitor(BaseTypeChecker checker) {
        super(checker);
        annoFormatter = new DefaultAnnotationFormatter();
        initializedFields = new ArrayList<>();
    }

    @Override
    public void setRoot(CompilationUnitTree root) {
        // Clean up the cache of initialized fields once per compilation unit.
        // Alternatively, but harder to determine, this could be done once per
        // top-level class.
        initializedFields.clear();
        super.setRoot(root);
    }

    @Override
    protected void checkConstructorInvocation(
            AnnotatedDeclaredType dt, AnnotatedExecutableType constructor, NewClassTree src) {
        // receiver annotations for constructors are forbidden, therefore no
        // check is necessary
        // TODO: nested constructors can have receivers!
    }

    @Override
    protected void checkConstructorResult(
            AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
        // Nothing to check
    }

    @Override
    protected void checkThisOrSuperConstructorCall(
            MethodInvocationTree superCall, @CompilerMessageKey String errorKey) {
        // Nothing to check
    }

    @Override
    protected void commonAssignmentCheck(
            Tree varTree, ExpressionTree valueExp, @CompilerMessageKey String errorKey) {
        // field write of the form x.f = y
        if (TreeUtils.isFieldAccess(varTree)) {
            // cast is safe: a field access can only be an IdentifierTree or
            // MemberSelectTree
            ExpressionTree lhs = (ExpressionTree) varTree;
            ExpressionTree y = valueExp;
            Element el = TreeUtils.elementFromUse(lhs);
            AnnotatedTypeMirror xType = atypeFactory.getReceiverType(lhs);
            AnnotatedTypeMirror yType = atypeFactory.getAnnotatedType(y);
            // the special FBC rules do not apply if there is an explicit
            // UnknownInitialization annotation
            Set<AnnotationMirror> fieldAnnotations =
                    atypeFactory.getAnnotatedType(TreeUtils.elementFromUse(lhs)).getAnnotations();
            if (!AnnotationUtils.containsSameByName(
                    fieldAnnotations, atypeFactory.UNKNOWN_INITIALIZATION)) {
                if (!ElementUtils.isStatic(el)
                        && !(atypeFactory.isCommitted(yType)
                                || atypeFactory.isFree(xType)
                                || atypeFactory.isFbcBottom(yType))) {
                    @CompilerMessageKey String err;
                    if (atypeFactory.isCommitted(xType)) {
                        err = COMMITMENT_INVALID_FIELD_WRITE_INITIALIZED;
                    } else {
                        err = COMMITMENT_INVALID_FIELD_WRITE_UNKNOWN_INITIALIZATION;
                    }
                    checker.report(Result.failure(err, varTree), varTree);
                    return; // prevent issuing another errow about subtyping
                }
            }
        }
        super.commonAssignmentCheck(varTree, valueExp, errorKey);
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        // is this a field (and not a local variable)?
        if (TreeUtils.elementFromDeclaration(node).getKind().isField()) {
            Set<AnnotationMirror> annotationMirrors =
                    atypeFactory.getAnnotatedType(node).getExplicitAnnotations();
            // Fields cannot have commitment annotations.
            for (Class<? extends Annotation> c : atypeFactory.getInitializationAnnotations()) {
                for (AnnotationMirror a : annotationMirrors) {
                    if (atypeFactory.isUnclassified(a)) {
                        continue; // unclassified is allowed
                    }
                    if (AnnotationUtils.areSameByClass(a, c)) {
                        checker.report(Result.failure(COMMITMENT_INVALID_FIELD_TYPE, node), node);
                        break;
                    }
                }
            }
        }
        return super.visitVariable(node, p);
    }

    @Override
    protected boolean checkContract(
            Receiver expr,
            AnnotationMirror necessaryAnnotation,
            AnnotationMirror inferredAnnotation,
            CFAbstractStore<?, ?> store) {
        // also use the information about initialized fields to check contracts
        final AnnotationMirror invariantAnno = atypeFactory.getFieldInvariantAnnotation();

        if (!atypeFactory.getQualifierHierarchy().isSubtype(invariantAnno, necessaryAnnotation)
                || !(expr instanceof FieldAccess)) {
            return super.checkContract(expr, necessaryAnnotation, inferredAnnotation, store);
        }

        FieldAccess fa = (FieldAccess) expr;
        if (fa.getReceiver() instanceof ThisReference || fa.getReceiver() instanceof ClassName) {
            @SuppressWarnings("unchecked")
            Store s = (Store) store;
            if (s.isFieldInitialized(fa.getField())) {
                AnnotatedTypeMirror fieldType = atypeFactory.getAnnotatedType(fa.getField());
                // is this an invariant-field?
                if (AnnotationUtils.containsSame(fieldType.getAnnotations(), invariantAnno)) {
                    return true;
                }
            }
        } else {
            Set<AnnotationMirror> recvAnnoSet;
            @SuppressWarnings("unchecked")
            Value value = (Value) store.getValue(fa.getReceiver());

            if (value != null) {
                recvAnnoSet = value.getAnnotations();
            } else if (fa.getReceiver() instanceof LocalVariable) {
                Element elem = ((LocalVariable) fa.getReceiver()).getElement();
                AnnotatedTypeMirror recvType = atypeFactory.getAnnotatedType(elem);
                recvAnnoSet = recvType.getAnnotations();
            } else {
                // Is there anything better we could do?
                return false;
            }

            boolean isRecvCommitted = false;
            for (AnnotationMirror anno : recvAnnoSet) {
                if (atypeFactory.isCommitted(anno)) {
                    isRecvCommitted = true;
                }
            }

            AnnotatedTypeMirror fieldType = atypeFactory.getAnnotatedType(fa.getField());
            // The receiver is fully initialized and the field type
            // has the invariant type.
            if (isRecvCommitted
                    && AnnotationUtils.containsSame(fieldType.getAnnotations(), invariantAnno)) {
                return true;
            }
        }
        return super.checkContract(expr, necessaryAnnotation, inferredAnnotation, store);
    }

    @Override
    public Void visitTypeCast(TypeCastTree node, Void p) {
        AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(node.getExpression());
        AnnotatedTypeMirror castType = atypeFactory.getAnnotatedType(node);
        AnnotationMirror exprAnno = null, castAnno = null;

        // find commitment annotation
        for (Class<? extends Annotation> a : atypeFactory.getInitializationAnnotations()) {
            if (castType.hasAnnotation(a)) {
                assert castAnno == null;
                castAnno = castType.getAnnotation(a);
            }
            if (exprType.hasAnnotation(a)) {
                assert exprAnno == null;
                exprAnno = exprType.getAnnotation(a);
            }
        }

        // TODO: this is most certainly unsafe!! (and may be hiding some problems)
        // If we don't find a commitment annotation, then we just assume that
        // the subtyping is alright.
        // The case that has come up is with wildcards not getting a type for
        // some reason, even though the default is @Initialized.
        boolean isSubtype;
        if (exprAnno == null || castAnno == null) {
            isSubtype = true;
        } else {
            assert exprAnno != null && castAnno != null;
            isSubtype = atypeFactory.getQualifierHierarchy().isSubtype(exprAnno, castAnno);
        }

        if (!isSubtype) {
            checker.report(
                    Result.failure(
                            COMMITMENT_INVALID_CAST,
                            annoFormatter.formatAnnotationMirror(exprAnno),
                            annoFormatter.formatAnnotationMirror(castAnno)),
                    node);
            return p; // suppress cast.unsafe warning
        }

        return super.visitTypeCast(node, p);
    }

    protected final List<VariableTree> initializedFields;

    @Override
    public void processClassTree(ClassTree node) {
        // go through all members and look for initializers.
        // save all fields that are initialized and do not report errors about
        // them later when checking constructors.
        for (Tree member : node.getMembers()) {
            if (member.getKind() == Tree.Kind.BLOCK && !((BlockTree) member).isStatic()) {
                BlockTree block = (BlockTree) member;
                Store store = atypeFactory.getRegularExitStore(block);

                // Add field values for fields with an initializer.
                for (Pair<VariableElement, Value> t : store.getAnalysis().getFieldValues()) {
                    store.addInitializedField(t.first);
                }
                final List<VariableTree> init =
                        atypeFactory.getInitializedInvariantFields(store, getCurrentPath());
                initializedFields.addAll(init);
            }
        }

        super.processClassTree(node);

        // Warn about uninitialized static fields.
        if (node.getKind() == Kind.CLASS) {
            boolean isStatic = true;
            // See GenericAnnotatedTypeFactory.performFlowAnalysis for why we use
            // the regular exit store of the class here.
            Store store = atypeFactory.getRegularExitStore(node);
            // Add field values for fields with an initializer.
            for (Pair<VariableElement, Value> t : store.getAnalysis().getFieldValues()) {
                store.addInitializedField(t.first);
            }
            List<AnnotationMirror> receiverAnnotations = Collections.emptyList();
            checkFieldsInitialized(node, isStatic, store, receiverAnnotations);
        }
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        if (TreeUtils.isConstructor(node)) {
            Collection<? extends AnnotationMirror> returnTypeAnnotations =
                    AnnotationUtils.getExplicitAnnotationsOnConstructorResult(node);
            // check for invalid constructor return type
            for (Class<? extends Annotation> c :
                    atypeFactory.getInvalidConstructorReturnTypeAnnotations()) {
                for (AnnotationMirror a : returnTypeAnnotations) {
                    if (AnnotationUtils.areSameByClass(a, c)) {
                        checker.report(
                                Result.failure(COMMITMENT_INVALID_CONSTRUCTOR_RETURN_TYPE, node),
                                node);
                        break;
                    }
                }
            }

            // Check that all fields have been initialized at the end of the
            // constructor.
            boolean isStatic = false;
            Store store = atypeFactory.getRegularExitStore(node);
            List<? extends AnnotationMirror> receiverAnnotations = getAllReceiverAnnotations(node);
            checkFieldsInitialized(node, isStatic, store, receiverAnnotations);
        }
        return super.visitMethod(node, p);
    }

    /** Returns the full list of annotations on the receiver. */
    private List<? extends AnnotationMirror> getAllReceiverAnnotations(MethodTree node) {
        // TODO: get access to a Types instance and use it to get receiver type
        // Or, extend ExecutableElement with such a method.
        // Note that we cannot use the receiver type from AnnotatedExecutableType, because that
        // would only have the nullness annotations; here we want to see all annotations on the
        // receiver.
        List<? extends AnnotationMirror> rcvannos = null;
        if (TreeUtils.isConstructor(node)) {
            com.sun.tools.javac.code.Symbol meth =
                    (com.sun.tools.javac.code.Symbol) TreeUtils.elementFromDeclaration(node);
            rcvannos = meth.getRawTypeAttributes();
            if (rcvannos == null) {
                rcvannos = Collections.emptyList();
            }
        }
        return rcvannos;
    }

    /**
     * Checks that all fields (all static fields if {@code staticFields} is true) are initialized in
     * the given store.
     */
    // TODO: the code for checking if fields are initialized should be re-written,
    // as the current version contains quite a few ugly parts, is hard to understand,
    // and it is likely that it does not take full advantage of the information
    // about initialization we compute in
    // GenericAnnotatedTypeFactory.initializationStaticStore and
    // GenericAnnotatedTypeFactory.initializationStore.
    protected void checkFieldsInitialized(
            Tree blockNode,
            boolean staticFields,
            Store store,
            List<? extends AnnotationMirror> receiverAnnotations) {
        // If the store is null, then the constructor cannot terminate
        // successfully
        if (store == null) {
            return;
        }

        String COMMITMENT_FIELDS_UNINITIALIZED_KEY =
                (staticFields
                        ? COMMITMENT_STATIC_FIELDS_UNINITIALIZED
                        : COMMITMENT_FIELDS_UNINITIALIZED);

        List<VariableTree> violatingFields =
                atypeFactory.getUninitializedInvariantFields(
                        store, getCurrentPath(), staticFields, receiverAnnotations);

        if (staticFields) {
            // TODO: Why is nothing done for static fields?
            // Do we need the following?
            // violatingFields.removeAll(store.initializedFields);
        } else {
            // remove fields that have already been initialized by an
            // initializer block
            violatingFields.removeAll(initializedFields);
        }

        // Remove fields with a relevant @SuppressWarnings annotation.
        Iterator<VariableTree> itor = violatingFields.iterator();
        while (itor.hasNext()) {
            VariableTree f = itor.next();
            Element e = TreeUtils.elementFromTree(f);
            if (checker.shouldSuppressWarnings(e, COMMITMENT_FIELDS_UNINITIALIZED_KEY)) {
                itor.remove();
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
            checker.report(
                    Result.failure(COMMITMENT_FIELDS_UNINITIALIZED_KEY, fieldsString), blockNode);
        }
    }
}
