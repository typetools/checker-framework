package checkers.nonnull;

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

import checkers.commitment.CommitmentStore;
import checkers.commitment.CommitmentTransfer;
import checkers.flow.analysis.ConditionalTransferResult;
import checkers.flow.analysis.FlowExpressions;
import checkers.flow.analysis.FlowExpressions.FieldAccess;
import checkers.flow.analysis.FlowExpressions.Receiver;
import checkers.flow.analysis.FlowExpressions.ThisReference;
import checkers.flow.analysis.TransferResult;
import checkers.flow.analysis.checkers.CFAbstractStore;
import checkers.flow.analysis.checkers.CFValue;
import checkers.flow.cfg.UnderlyingAST;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.MethodInvocationNode;
import checkers.flow.cfg.node.Node;
import checkers.flow.cfg.node.NullLiteralNode;
import checkers.flow.cfg.node.ThisLiteralNode;
import checkers.nonnull.quals.NonNull;
import checkers.types.AnnotatedTypeMirror;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Transfer function for the non-null type system. Performs the following
 * refinements:
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
 */
public class NonNullTransfer extends CommitmentTransfer<NonNullTransfer> {

    /** Type-specific version of super.analysis. */
    protected final NonNullAnalysis analysis;

    public NonNullTransfer(NonNullAnalysis analysis, NonNullChecker checker) {
        super(analysis, checker);
        this.analysis = analysis;
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

    @Override
    protected Set<Element> initializedFieldsAfterCall(
            MethodInvocationNode node,
            ConditionalTransferResult<CFValue, CommitmentStore> transferResult) {
        Set<Element> result = new HashSet<>(super.initializedFieldsAfterCall(
                node, transferResult));
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
            markNonNullFieldsAsInitialized(result, clazzElem);
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
                markNonNullFieldsAsInitialized(result, clazzElem);
            }
        }

        // Case 2: After a method call that has some postcondition ensuring some
        // properties about fields, these fields can be known to be initialized.
        addInitializedFields(transferResult.getThenStore());
        addInitializedFields(transferResult.getElseStore());
        return result;
    }

    /**
     * Adds all the fields of the class {@code clazzElem} to the set of
     * initialized fields {@code result}.
     */
    protected void markNonNullFieldsAsInitialized(Set<Element> result,
            TypeElement clazzElem) {
        List<VariableElement> fields = ElementFilter.fieldsIn(clazzElem
                .getEnclosedElements());
        for (VariableElement field : fields) {
            AnnotatedTypeMirror fieldAnno = analysis.getFactory()
                    .getAnnotatedType(field);
            if (fieldAnno.hasAnnotation(NonNull.class)) {
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

    /**
     * {@inheritDoc}
     *
     * <p>
     * Furthermore, this method refines the type to {@code NonNull} for the
     * appropriate branch if an expression is compared to the {@code null}
     * literal.
     */
    @Override
    protected TransferResult<CFValue, CommitmentStore> strengthenAnnotationOfEqualTo(
            TransferResult<CFValue, CommitmentStore> res, Node firstNode,
            Node secondNode, CFValue firstValue, CFValue secondValue,
            boolean notEqualTo) {
        res = super.strengthenAnnotationOfEqualTo(res, firstNode, secondNode,
                firstValue, secondValue, notEqualTo);
        if (firstNode instanceof NullLiteralNode) {
            Receiver secondInternal = FlowExpressions.internalReprOf(
                    analysis.getFactory(), secondNode);
            if (CFAbstractStore.canInsertReceiver(secondInternal)) {
                CommitmentStore thenStore = res.getThenStore();
                CommitmentStore elseStore = res.getElseStore();
                AnnotationMirror nonNull = analysis.getFactory()
                        .annotationFromClass(NonNull.class);
                if (notEqualTo) {
                    thenStore.insertValue(secondInternal, nonNull);
                } else {
                    elseStore.insertValue(secondInternal, nonNull);
                }
                return new ConditionalTransferResult<>(res.getResultValue(),
                        thenStore, elseStore);
            }
        }
        return res;
    }
}
