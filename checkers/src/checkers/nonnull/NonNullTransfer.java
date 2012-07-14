package checkers.nonnull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import checkers.commitment.CommitmentChecker;
import checkers.commitment.CommitmentStore;
import checkers.commitment.CommitmentTransfer;
import checkers.flow.analysis.ConditionalTransferResult;
import checkers.flow.analysis.FlowExpressions.FieldAccess;
import checkers.flow.analysis.FlowExpressions.ThisReference;
import checkers.flow.analysis.checkers.CFValue;
import checkers.flow.cfg.UnderlyingAST;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.MethodInvocationNode;
import checkers.flow.cfg.node.Node;
import checkers.flow.cfg.node.ThisLiteralNode;
import checkers.nonnull.quals.NonNull;
import checkers.types.AnnotatedTypeMirror;
import checkers.util.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;

/**
 * Transfer function for the non-null type system. Performs the following
 * refinements:
 * <ol>
 * <li>After the call to a constructor ("this()" call), all non-null fields of
 * the current class can safely be considered initialized.
 * <li>TODO: After a method call with a postcondition that ensures a field to be
 * non-null, that field can safely be considered initialized.
 * <li>All non-null fields with an initializer can be considered initialized.
 * </ol>
 * 
 * @author Stefan Heule
 */
public class NonNullTransfer extends CommitmentTransfer<NonNullTransfer> {

    /** Type-specific version of super.analysis. */
    protected final NonNullAnalysis analysis;

    public NonNullTransfer(NonNullAnalysis analysis) {
        super(analysis);
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
            Set<VariableTree> fields = CommitmentChecker.getAllFields(clazz);
            for (VariableTree field : fields) {
                AnnotatedTypeMirror fieldAnno = analysis.getFactory()
                        .getAnnotatedType(field);
                if (fieldAnno.hasAnnotation(NonNull.class)) {
                    result.add(TreeUtils.elementFromDeclaration(field));
                }
            }
        }

        // Case 2: After a method call that has some postcondition ensuring some
        // properties about fields, these fields can be known to be initialized.
        addInitializedFields(transferResult.getThenStore());
        addInitializedFields(transferResult.getElseStore());
        return result;
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
}
