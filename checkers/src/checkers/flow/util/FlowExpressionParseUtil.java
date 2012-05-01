package checkers.flow.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

import checkers.flow.analysis.FlowExpressions;
import checkers.flow.analysis.FlowExpressions.FieldAccess;
import checkers.flow.analysis.FlowExpressions.Receiver;
import checkers.flow.analysis.FlowExpressions.ThisReference;
import checkers.flow.cfg.node.ImplicitThisLiteralNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.MethodInvocationNode;
import checkers.flow.cfg.node.Node;
import checkers.source.Result;
import checkers.util.ElementUtils;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

/**
 * A collection of helper methods to parse a string that represents a restricted
 * Java expression. Such expressions can be found in annotations (e.g., to
 * specify a pre- or postcondition).
 * 
 * @author Stefan Heule
 * 
 */
public class FlowExpressionParseUtil {

    /** Matches a parameter */
    protected static Pattern parameterPattern = Pattern
            .compile("^#([1-9]+[0-9]*)$");
    /** Finds all parameters */
    protected static Pattern parametersPattern = Pattern
            .compile("#([1-9]+[0-9]*)");
    /** Matches the self reference */
    protected static Pattern selfPattern = Pattern.compile("^(this|#0)$");
    /** Matches an identifier */
    protected static Pattern identifierPattern = Pattern
            .compile("^[a-z_$][a-z_$0-9]*$");

    /**
     * Parse a string and return its representation as a
     * {@link FlowExpression.Receiver}, or throw an
     * {@link FlowExpressionParseException}. The expression is assumed to be
     * used in the context of a method.
     * 
     * @param s
     *            The string to parse.
     * @param receiverType
     *            The type of the receiver that this expression might refer to.
     * @param receiver
     *            The receiver to be used in the result (if it occurs).
     * @param arguments
     *            The arguments of the method.
     * @throws FlowExpressionParseException
     */
    public static/* @Nullable */FlowExpressions.Receiver parse(String s,
            FlowExpressionContext context) throws FlowExpressionParseException {

        Matcher identifierMatcher = identifierPattern.matcher(s);
        Matcher selfMatcher = selfPattern.matcher(s);
        Matcher parameterMatcher = parameterPattern.matcher(s);

        if (identifierMatcher.matches()) {

            // this literal
            if (selfMatcher.matches()) {
                return new ThisReference(context.receiverType);
            }

            // field of a the receiver (implicit self reference as receiver)
            assert context.receiverType instanceof DeclaredType;
            Element el = ((DeclaredType) context.receiverType).asElement();
            assert el instanceof TypeElement;
            TypeElement elType = (TypeElement) el;
            VariableElement fieldElement = ElementUtils.findFieldInType(elType,
                    s);
            return new FieldAccess(context.receiver, context.receiverType,
                    fieldElement);

        } else if (parameterMatcher.matches()) {
            // parameter syntax
            int idx = -1;
            try {
                idx = Integer.parseInt(parameterMatcher.group(1));
            } catch (NumberFormatException e) {
                // cannot occur by the way the pattern is defined (matches only
                // numbers)
            }
            if (idx > context.arguments.size()) {
                throw new FlowExpressionParseException(Result.failure(
                        "flowexpr.parse.index.too.big", Integer.toString(idx)));
            }
            return context.arguments.get(idx - 1);
        } else {
            throw new FlowExpressionParseException(Result.failure(
                    "flowexpr.parse.error", s));
        }
    }

    /**
     * Context used to parse a flow expression.
     */
    public static class FlowExpressionContext {
        public final TypeMirror receiverType;
        public final Receiver receiver;
        public final List<Receiver> arguments;

        public FlowExpressionContext(TypeMirror receiverType,
                Receiver receiver, List<Receiver> arguments) {
            this.receiverType = receiverType;
            this.receiver = receiver;
            this.arguments = arguments;
        }
    }

    /**
     * @return The list of parameters that occur in {@code s}, identified by the
     *         number of the parameter (starting at 1).
     */
    public static List<Integer> parameterIndices(String s) {
        List<Integer> result = new ArrayList<>();
        Matcher matcher = parametersPattern.matcher(s);
        while (matcher.find()) {
            int idx = Integer.parseInt(matcher.group(1));
            result.add(idx);
        }
        return result;
    }

    /**
     * An exception that indicates a parse error. It contains a {@link Result}
     * that can be used for error reporting.
     */
    public static class FlowExpressionParseException extends Exception {
        private static final long serialVersionUID = 1L;

        protected final Result result;

        public FlowExpressionParseException(Result result) {
            this.result = result;
        }

        public Result getResult() {
            return result;
        }
    }

    /**
     * @return A {@link FlowExpressionContext} for the method {@code node} as
     *         seen at the method declaration.
     */
    public static FlowExpressionContext buildFlowExprContextForDeclaration(
            MethodTree node, TreePath currentPath) {
        Tree classTree = TreeUtils.enclosingClass(currentPath);
        Node receiver = new ImplicitThisLiteralNode(
                InternalUtils.typeOf(classTree));
        Receiver internalReceiver = FlowExpressions.internalReprOf(receiver);
        List<Receiver> internalArguments = new ArrayList<>();
        for (VariableTree arg : node.getParameters()) {
            internalArguments.add(FlowExpressions
                    .internalReprOf(new LocalVariableNode(arg)));
        }
        FlowExpressionContext flowExprContext = new FlowExpressionContext(
                receiver.getType(), internalReceiver, internalArguments);
        return flowExprContext;
    }

    /**
     * @return A {@link FlowExpressionContext} for the method {@code node} as
     *         seen at the method use (i.e., at a method call site).
     */
    public static FlowExpressionContext buildFlowExprContextForUse(
            MethodInvocationNode n) {
        Node receiver = n.getTarget().getReceiver();
        Receiver internalReceiver = FlowExpressions.internalReprOf(receiver);
        List<Receiver> internalArguments = new ArrayList<>();
        for (Node arg : n.getArguments()) {
            internalArguments.add(FlowExpressions.internalReprOf(arg));
        }
        FlowExpressionContext flowExprContext = new FlowExpressionContext(
                receiver.getType(), internalReceiver, internalArguments);
        return flowExprContext;
    }
}
