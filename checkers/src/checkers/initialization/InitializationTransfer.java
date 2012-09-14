package checkers.initialization;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import checkers.flow.analysis.ConditionalTransferResult;
import checkers.flow.analysis.FlowExpressions;
import checkers.flow.analysis.FlowExpressions.FieldAccess;
import checkers.flow.analysis.FlowExpressions.Receiver;
import checkers.flow.analysis.FlowExpressions.ThisReference;
import checkers.flow.analysis.RegularTransferResult;
import checkers.flow.analysis.TransferInput;
import checkers.flow.analysis.TransferResult;
import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFAbstractTransfer;
import checkers.flow.analysis.checkers.CFAbstractValue;
import checkers.flow.analysis.checkers.CFAbstractValue.InferredAnnotation;
import checkers.flow.analysis.checkers.CFValue;
import checkers.flow.cfg.UnderlyingAST;
import checkers.flow.cfg.node.AssignmentNode;
import checkers.flow.cfg.node.FieldAccessNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.MethodInvocationNode;
import checkers.flow.cfg.node.Node;
import checkers.flow.cfg.node.ThisLiteralNode;
import checkers.types.AnnotatedTypeMirror;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * A transfer function that extends {@link CFAbstractTransfer} and tracks
 * {@link InitializationStore}s. In addition to the features of
 * {@link CFAbstractTransfer}, this transfer function also track which fields of
 * the current class ('self' receiver) have been initialized.
 *
 * <p>
 * More precisely, the following refinements are performed:
 * <ol>
 * <li>After the call to a constructor ("this()" call), all non-null fields of
 * the current class can safely be considered initialized.
 * <li>TODO: After a method call with a postcondition that ensures a field to be
 * non-null, that field can safely be considered initialized.
 * <li>All non-null fields with an initializer can be considered initialized.
 * <li>After the call to a super constructor ("super()" call), all non-null
 * fields of the super class can safely be considered initialized.
 * </ol>
 *
 * @author Stefan Heule
 * @see InitializationStore
 *
 * @param <T>
 *            The type of the transfer function.
 */
public class InitializationTransfer<T extends InitializationTransfer<T>>
        extends CFAbstractTransfer<CFValue, InitializationStore, T> {

    protected final InitializationChecker checker;

    public InitializationTransfer(
            CFAbstractAnalysis<CFValue, InitializationStore, T> analysis) {
        super(analysis);
        this.checker = (InitializationChecker) analysis.getFactory().getChecker();
    }

    @Override
    public InitializationStore initialStore(UnderlyingAST underlyingAST,
            List<LocalVariableNode> parameters) {
        InitializationStore result = super.initialStore(underlyingAST,
                parameters);
        // Case 3: all invariant fields that have an initializer are part of
        // 'fieldValues', and can be considered initialized.
        addInitializedFields(result);
        return result;
    }

    /**
     * Returns the set of fields that can safely be considered initialized after
     * the method call {@code node}.
     */
    protected Set<Element> initializedFieldsAfterCall(
            MethodInvocationNode node,
            ConditionalTransferResult<CFValue, InitializationStore> transferResult) {
        Set<Element> result = new HashSet<>();
        MethodInvocationTree tree = node.getTree();
        ExecutableElement method = TreeUtils.elementFromUse(tree);
        boolean isConstructor = method.getSimpleName().contentEquals("<init>");
        Node receiver = node.getTarget().getReceiver();
        String methodString = tree.getMethodSelect().toString();

        // Case 1: After a call to the constructor of the same class, all
        // invariant fields are guaranteed to be initialized.
        if (isConstructor && receiver instanceof ThisLiteralNode
                && methodString.equals("this")) {
            ClassTree clazz = TreeUtils.enclosingClass(analysis.getFactory()
                    .getPath(tree));
            TypeElement clazzElem = TreeUtils.elementFromDeclaration(clazz);
            markInvariantFieldsAsInitialized(result, clazzElem);
        }

        // Case 4: After a call to the constructor of the super class, all
        // invariant fields of any super class are guaranteed to be initialized.
        if (isConstructor && receiver instanceof ThisLiteralNode
                && methodString.equals("super")) {
            ClassTree clazz = TreeUtils.enclosingClass(analysis.getFactory()
                    .getPath(tree));
            TypeElement clazzElem = TreeUtils.elementFromDeclaration(clazz);
            TypeMirror superClass = clazzElem.getSuperclass();

            while (superClass != null && superClass.getKind() != TypeKind.NONE) {
                clazzElem = (TypeElement) analysis.getTypes().asElement(
                        superClass);
                superClass = clazzElem.getSuperclass();
                markInvariantFieldsAsInitialized(result, clazzElem);
            }
        }

        // Case 2: After a method call that has some postcondition ensuring some
        // properties about fields, these fields can be known to be initialized.
        addInitializedFields(transferResult.getThenStore());
        addInitializedFields(transferResult.getElseStore());
        return result;
    }

    /**
     * Adds all the fields of the class {@code clazzElem} that have the
     * 'invariant annotation' to the set of initialized fields {@code result}.
     */
    protected void markInvariantFieldsAsInitialized(Set<Element> result,
            TypeElement clazzElem) {
        List<VariableElement> fields = ElementFilter.fieldsIn(clazzElem
                .getEnclosedElements());
        for (VariableElement field : fields) {
            AnnotatedTypeMirror fieldAnno = analysis.getFactory()
                    .getAnnotatedType(field);
            if (fieldAnno.hasAnnotation(checker.getFieldInvariantAnnotation())) {
                result.add(field);
            }
        }
    }

    /**
     * For the given store {@code store}, add all fields for which we know some
     * property (by looking at 'fieldValues' in the store) to the set of
     * initialized fields.
     */
    protected void addInitializedFields(InitializationStore store) {
        Map<FieldAccess, CFValue> fieldValues = store.getFieldValues();
        for (Entry<FieldAccess, CFValue> e : fieldValues.entrySet()) {
            FieldAccess field = e.getKey();
            if (field.getReceiver() instanceof ThisReference) {
                // There is no need to check what CFValue the field has, as any
                // value means that is has been initialized.
                store.addInitializedField(field.getField());
            }
        }
    }

    @Override
    public TransferResult<CFValue, InitializationStore> visitAssignment(
            AssignmentNode n, TransferInput<CFValue, InitializationStore> in) {
        TransferResult<CFValue, InitializationStore> result = super
                .visitAssignment(n, in);
        assert result instanceof RegularTransferResult;
        Receiver expr = FlowExpressions.internalReprOf(analysis.getFactory(),
                n.getTarget());

        // If this is an assignment to a field of 'this', then mark the field as
        // initialized.
        if (!expr.containsUnknown()) {
            if (expr instanceof FieldAccess) {
                FieldAccess fa = (FieldAccess) expr;
                if (fa.getReceiver() instanceof ThisReference) {
                    Element field = fa.getField();
                    result.getRegularStore().addInitializedField(field);
                }
            }
        }
        return result;
    }

    /**
     * If an invariant field is initialized and has the invariant annotation,
     * than it has at least the invariant annotation. Note that only field of
     * the 'this' receiver are tracked for initialization.
     */
    @Override
    public TransferResult<CFValue, InitializationStore> visitFieldAccess(
            FieldAccessNode n, TransferInput<CFValue, InitializationStore> p) {
        TransferResult<CFValue, InitializationStore> result = super
                .visitFieldAccess(n, p);
        assert !result.containsTwoStores();
        InitializationStore store = result.getRegularStore();
        if (store.isFieldInitialized(n.getElement())
                && n.getReceiver() instanceof ThisLiteralNode) {
            AnnotatedTypeMirror fieldAnno = analysis.getFactory()
                    .getAnnotatedType(n.getElement());
            // Only if the field has the 'invariant' annotation.
            if (fieldAnno.hasAnnotation(checker.getFieldInvariantAnnotation())) {
                AnnotationMirror inv = checker.getFieldInvariantAnnotation();
                InferredAnnotation[] annotations = CFAbstractValue
                        .createInferredAnnotationArray(analysis, inv);
                CFValue refinedResultValue = analysis
                        .createAbstractValue(annotations);
                CFValue oldResultValue = result.getResultValue();
                result.setResultValue(refinedResultValue.mostSpecific(
                        oldResultValue, null));
            }
        }
        return result;
    }

    @Override
    public TransferResult<CFValue, InitializationStore> visitMethodInvocation(
            MethodInvocationNode n,
            TransferInput<CFValue, InitializationStore> in) {
        TransferResult<CFValue, InitializationStore> result = super
                .visitMethodInvocation(n, in);
        assert result instanceof ConditionalTransferResult;
        Set<Element> newlyInitializedFields = initializedFieldsAfterCall(
                n,
                (ConditionalTransferResult<CFValue, InitializationStore>) result);
        if (newlyInitializedFields.size() > 0) {
            for (Element f : newlyInitializedFields) {
                result.getThenStore().addInitializedField(f);
                result.getElseStore().addInitializedField(f);
            }
        }
        return result;
    }
}
