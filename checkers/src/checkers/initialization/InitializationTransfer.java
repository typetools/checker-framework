package checkers.initialization;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import javacutils.TreeUtils;

import dataflow.analysis.ConditionalTransferResult;
import dataflow.analysis.FlowExpressions;
import dataflow.analysis.FlowExpressions.FieldAccess;
import dataflow.analysis.FlowExpressions.Receiver;
import dataflow.analysis.RegularTransferResult;
import dataflow.analysis.TransferInput;
import dataflow.analysis.TransferResult;
import dataflow.cfg.node.AssignmentNode;
import dataflow.cfg.node.FieldAccessNode;
import dataflow.cfg.node.MethodInvocationNode;
import dataflow.cfg.node.Node;
import dataflow.cfg.node.ThisLiteralNode;

import checkers.flow.CFAbstractAnalysis;
import checkers.flow.CFAbstractTransfer;
import checkers.flow.CFAbstractValue;
import checkers.flow.CFValue;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;

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
 * <li>After a method call with a postcondition that ensures a field to be
 * non-null, that field can safely be considered initialized (this is done in
 * {@link InitializationStore#insertValue(Receiver, CFAbstractValue)}).
 * <li>All non-null fields with an initializer can be considered initialized
 * (this is done in {@link InitializationStore#insertValue(Receiver, CFAbstractValue)}).
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
public class InitializationTransfer<V extends CFAbstractValue<V>, T extends InitializationTransfer<V, T, S>, S extends InitializationStore<V, S>>
        extends CFAbstractTransfer<V, S, T> {

    protected final InitializationChecker checker;

    public InitializationTransfer(CFAbstractAnalysis<V, S, T> analysis) {
        super(analysis);
        this.checker = (InitializationChecker) analysis.getFactory()
                .getChecker();
    }

    @Override
    protected boolean isNotFullyInitializedReceiver(MethodTree methodTree) {
        if (super.isNotFullyInitializedReceiver(methodTree)) {
            return true;
        }
        final AnnotatedDeclaredType receiverType = analysis.getFactory()
                .getAnnotatedType(methodTree).getReceiverType();
        return checker.isUnclassified(receiverType) || checker.isFree(receiverType);
    }

    /**
     * Returns the set of fields that can safely be considered initialized after
     * the method call {@code node}.
     */
    protected Set<Element> initializedFieldsAfterCall(
            MethodInvocationNode node,
            ConditionalTransferResult<V, S> transferResult) {
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

    @Override
    public TransferResult<V, S> visitAssignment(AssignmentNode n,
            TransferInput<V, S> in) {
        TransferResult<V, S> result = super.visitAssignment(n, in);
        assert result instanceof RegularTransferResult;
        Receiver expr = FlowExpressions.internalReprOf(analysis.getFactory(),
                n.getTarget());

        // If this is an assignment to a field of 'this', then mark the field as
        // initialized.
        if (!expr.containsUnknown()) {
            if (expr instanceof FieldAccess) {
                FieldAccess fa = (FieldAccess) expr;
                result.getRegularStore().addInitializedField(fa);
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
    public TransferResult<V, S> visitFieldAccess(FieldAccessNode n,
            TransferInput<V, S> p) {
        TransferResult<V, S> result = super.visitFieldAccess(n, p);
        assert !result.containsTwoStores();
        S store = result.getRegularStore();
        if (store.isFieldInitialized(n.getElement())
                && n.getReceiver() instanceof ThisLiteralNode) {
            AnnotatedTypeMirror fieldAnno = analysis.getFactory()
                    .getAnnotatedType(n.getElement());
            // Only if the field has the 'invariant' annotation.
            if (fieldAnno.hasAnnotation(checker.getFieldInvariantAnnotation())) {
                AnnotationMirror inv = checker.getFieldInvariantAnnotation();
                V oldResultValue = result.getResultValue();
                V refinedResultValue = analysis.createSingleAnnotationValue(
                        inv, oldResultValue.getType().getUnderlyingType());
                V newResultValue = refinedResultValue.mostSpecific(
                        oldResultValue, null);
                result.setResultValue(newResultValue);
            }
        }
        return result;
    }

    @Override
    public TransferResult<V, S> visitMethodInvocation(MethodInvocationNode n,
            TransferInput<V, S> in) {
        TransferResult<V, S> result = super.visitMethodInvocation(n, in);
        assert result instanceof ConditionalTransferResult;
        Set<Element> newlyInitializedFields = initializedFieldsAfterCall(n,
                (ConditionalTransferResult<V, S>) result);
        if (newlyInitializedFields.size() > 0) {
            for (Element f : newlyInitializedFields) {
                result.getThenStore().addInitializedField(f);
                result.getElseStore().addInitializedField(f);
            }
        }
        return result;
    }
}
