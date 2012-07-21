package checkers.commitment;

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
 * {@link CommitmentStore}s. In addition to the features of
 * {@link CFAbstractTransfer}, this transfer function also track which fields of
 * the current class ('self' receiver) have been initialized.
 *
 * @author Stefan Heule
 * @see CommitmentStore
 *
 * @param <T>
 *            The type of the transfer function.
 */
public class CommitmentTransfer<T extends CommitmentTransfer<T>> extends
        CFAbstractTransfer<CFValue, CommitmentStore, T> {

    protected final CommitmentChecker checker;

    public CommitmentTransfer(
            CFAbstractAnalysis<CFValue, CommitmentStore, T> analysis,
            CommitmentChecker checker) {
        super(analysis);
        this.checker = checker;
    }

    @Override
    public CommitmentStore initialStore(UnderlyingAST underlyingAST,
            List<LocalVariableNode> parameters) {
        CommitmentStore result = super.initialStore(underlyingAST, parameters);
        // Case 3: all non-null fields that have an initializer are part of
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
            ConditionalTransferResult<CFValue, CommitmentStore> transferResult) {
        Set<Element> result = new HashSet<>();
        MethodInvocationTree tree = node.getTree();
        ExecutableElement method = TreeUtils.elementFromUse(tree);
        boolean isConstructor = method.getSimpleName().contentEquals("<init>");
        Node receiver = node.getTarget().getReceiver();
        String methodString = tree.getMethodSelect().toString();

        // Case 1: After a call to the constructor of the same class, all
        // non-null fields are guaranteed to be initialized.
        if (isConstructor && receiver instanceof ThisLiteralNode
                && methodString.equals("this")) {
            ClassTree clazz = TreeUtils.enclosingClass(analysis.getFactory()
                    .getPath(tree));
            TypeElement clazzElem = TreeUtils.elementFromDeclaration(clazz);
            markInvariantFieldsAsInitialized(result, clazzElem);
        }

        // Case 4: After a call to the constructor of the super class, all
        // non-null fields of any super class are guaranteed to be initialized.
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
    protected void addInitializedFields(CommitmentStore store) {
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
    public TransferResult<CFValue, CommitmentStore> visitAssignment(
            AssignmentNode n, TransferInput<CFValue, CommitmentStore> in) {
        TransferResult<CFValue, CommitmentStore> result = super
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

    @Override
    public TransferResult<CFValue, CommitmentStore> visitFieldAccess(
            FieldAccessNode n, TransferInput<CFValue, CommitmentStore> p) {
        TransferResult<CFValue, CommitmentStore> result = super
                .visitFieldAccess(n, p);
        assert !result.containsTwoStores();
        CommitmentStore store = result.getRegularStore();
        if (store.isFieldInitialized(n.getElement())) {
            AnnotationMirror inv = checker.getFieldInvariantAnnotation();
            InferredAnnotation[] annotations = CFAbstractValue
                    .createInferredAnnotationArray(analysis, inv);
            CFValue refinedResultValue = analysis
                    .createAbstractValue(annotations);
            result.setResultValue(refinedResultValue.mostSpecific(result
                    .getResultValue()));
        }
        return result;
    }

    @Override
    public TransferResult<CFValue, CommitmentStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<CFValue, CommitmentStore> in) {
        TransferResult<CFValue, CommitmentStore> result = super
                .visitMethodInvocation(n, in);
        assert result instanceof ConditionalTransferResult;
        Set<Element> newlyInitializedFields = initializedFieldsAfterCall(n,
                (ConditionalTransferResult<CFValue, CommitmentStore>) result);
        if (newlyInitializedFields.size() > 0) {
            for (Element f : newlyInitializedFields) {
                result.getThenStore().addInitializedField(f);
                result.getElseStore().addInitializedField(f);
            }
        }
        return result;
    }
}
