package checkers.nonnull;

import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import checkers.commitment.CommitmentChecker;
import checkers.commitment.CommitmentTransfer;
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
    protected Set<Element> initializedFieldsAfterCall(MethodInvocationNode node) {
        Set<Element> result = new HashSet<>(
                super.initializedFieldsAfterCall(node));
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
        return result;
    }
}
