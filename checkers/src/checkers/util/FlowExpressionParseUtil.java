package checkers.flow.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import javacutils.InternalUtils;
import javacutils.Resolver;
import javacutils.TreeUtils;

import checkers.flow.analysis.FlowExpressions;
import checkers.flow.analysis.FlowExpressions.FieldAccess;
import checkers.flow.analysis.FlowExpressions.PureMethodCall;
import checkers.flow.analysis.FlowExpressions.Receiver;
import checkers.flow.analysis.FlowExpressions.ThisReference;
import checkers.flow.cfg.node.ImplicitThisLiteralNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.MethodInvocationNode;
import checkers.flow.cfg.node.Node;
import checkers.source.Result;
import checkers.types.AnnotatedTypeFactory;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

/**
 * A collection of helper methods to parse a string that represents a restricted
 * Java expression. Such expressions can be found in annotations (e.g., to
 * specify a pre- or postcondition).
 *
 * @author Stefan Heule
 */
public class FlowExpressionParseUtil {

    /** Regular expression for an identifier */
    protected static final String identifierRegex = "[a-zA-Z_$][a-zA-Z_$0-9]*";
    /** Matches a parameter */
    protected static final Pattern parameterPattern = Pattern
            .compile("^#([1-9]+[0-9]*)$");
    /** Finds all parameters */
    protected static final Pattern parametersPattern = Pattern
            .compile("#([1-9]+[0-9]*)");
    /** Matches the self reference */
    protected static final Pattern selfPattern = Pattern.compile("^(this|#0)$");
    /** Matches an identifier */
    protected static final Pattern identifierPattern = Pattern.compile("^"
            + identifierRegex + "$");
    /** Matches a method call */
    protected static final Pattern methodPattern = Pattern.compile("^("
            + identifierRegex + ")\\((.*)\\)$");
    /** Matches a field access */
    protected static final Pattern dotPattern = Pattern
            .compile("^([^.]+)\\.(.+)$");

    /**
     * Parse a string and return its representation as a
     * {@link FlowExpression.Receiver}, or throw an
     * {@link FlowExpressionParseException}. The expression is assumed to be
     * used in the context of a method.
     *
     * @param s
     *            The string to parse.
     * @param path
     *            The current tree path.
     * @param receiverType
     *            The type of the receiver that this expression might refer to.
     * @param receiver
     *            The receiver to be used in the result (if it occurs).
     * @param arguments
     *            The arguments of the method.
     * @throws FlowExpressionParseException
     */
    public static/* @Nullable */FlowExpressions.Receiver parse(String s,
            FlowExpressionContext context, TreePath path)
            throws FlowExpressionParseException {
        return parse(s, context, path, true, true, true, true, true);
    }

    /**
     * Private implementation of {@link #parse} with a choice of which classes
     * of expressions should be parsed.
     */
    private static/* @Nullable */FlowExpressions.Receiver parse(String s,
            FlowExpressionContext context, TreePath path, boolean allowSelf,
            boolean allowIdentifier, boolean allowParameter, boolean allowDot,
            boolean allowMethods) throws FlowExpressionParseException {

        Matcher identifierMatcher = identifierPattern.matcher(s);
        Matcher selfMatcher = selfPattern.matcher(s);
        Matcher parameterMatcher = parameterPattern.matcher(s);
        Matcher methodMatcher = methodPattern.matcher(s);
        Matcher dotMatcher = dotPattern.matcher(s);

        ProcessingEnvironment env = context.atypeFactory.getProcessingEnv();

        // this literal
        if (selfMatcher.matches() && allowSelf) {
            return new ThisReference(context.receiverType);
        } else if (identifierMatcher.matches() && allowIdentifier) {
            // field access
            try {
                Resolver resolver = new Resolver(env);
                Element fieldElem = resolver.findField(s, context.receiverType, path);
                return new FieldAccess(context.receiver, context.receiverType,
                        fieldElem);
            } catch (Throwable t) {
                throw new FlowExpressionParseException(Result.failure(
                        "flowexpr.parse.error", s));
            }
        } else if (parameterMatcher.matches() && allowParameter) {
            // parameter syntax
            int idx = -1;
            try {
                idx = Integer.parseInt(parameterMatcher.group(1));
            } catch (NumberFormatException e) {
                // cannot occur by the way the pattern is defined (matches only
                // numbers)
                assert false;
            }
            if (idx > context.arguments.size()) {
                throw new FlowExpressionParseException(Result.failure(
                        "flowexpr.parse.index.too.big", Integer.toString(idx)));
            }
            return context.arguments.get(idx - 1);
        } else if (methodMatcher.matches() && allowMethods) {
            String methodName = methodMatcher.group(1);
            String parameterList = methodMatcher.group(2);
            if (parameterList.length() != 0) {
                throw new FlowExpressionParseException(
                        Result.failure("flowexpr.parse.nonempty.parameter"));
            }
            try {
                Resolver resolver = new Resolver(env);
                Element methodElement = resolver.findMethod(methodName, context.receiverType, path);
                List<Receiver> parameters = new ArrayList<>();
                return new PureMethodCall(context.receiverType, methodElement,
                        context.receiver, parameters);
            } catch (Throwable t) {
                throw new FlowExpressionParseException(Result.failure(
                        "flowexpr.parse.error", s));
            }
        } else if (dotMatcher.matches() && allowDot) {
            String receiverString = dotMatcher.group(1);
            String remainingString = dotMatcher.group(2);

            // Parse the receiver first.
            Receiver receiver = parse(receiverString, context, path);

            // Parse the rest, with a new receiver.
            FlowExpressionContext newContext = context.changeReceiver(receiver);
            return parse(remainingString, newContext, path, false, true, false,
                    true, true);
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
        public final AnnotatedTypeFactory atypeFactory;

        public FlowExpressionContext(TypeMirror receiverType,
                Receiver receiver, List<Receiver> arguments,
                AnnotatedTypeFactory factory) {
            assert factory != null;
            this.receiverType = receiverType;
            this.receiver = receiver;
            this.arguments = arguments;
            this.atypeFactory = factory;
        }

        /**
         * Returns a copy of the context that is identical, but has a different
         * receiver.
         */
        public FlowExpressionContext changeReceiver(Receiver receiver) {
            return new FlowExpressionContext(receiver.getType(), receiver,
                    arguments, atypeFactory);
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
            MethodTree node, Tree classTree, AnnotatedTypeFactory factory) {
        Node receiver = new ImplicitThisLiteralNode(
                InternalUtils.typeOf(classTree));
        Receiver internalReceiver = FlowExpressions.internalReprOf(factory,
                receiver);
        List<Receiver> internalArguments = new ArrayList<>();
        for (VariableTree arg : node.getParameters()) {
            internalArguments.add(FlowExpressions.internalReprOf(factory,
                    new LocalVariableNode(arg)));
        }
        FlowExpressionContext flowExprContext = new FlowExpressionContext(
                receiver.getType(), internalReceiver, internalArguments,
                factory);
        return flowExprContext;
    }

    /**
     * @return A {@link FlowExpressionContext} for the method {@code node} as
     *         seen at the method declaration.
     */
    public static FlowExpressionContext buildFlowExprContextForDeclaration(
            MethodTree node, TreePath currentPath, AnnotatedTypeFactory factory) {
        Tree classTree = TreeUtils.enclosingClass(currentPath);
        return buildFlowExprContextForDeclaration(node, classTree, factory);
    }

    /**
     * @return A {@link FlowExpressionContext} for the method {@code node}
     *         (represented as a {@link Node} as seen at the method use (i.e.,
     *         at a method call site).
     */
    public static FlowExpressionContext buildFlowExprContextForUse(
            MethodInvocationNode n, AnnotatedTypeFactory factory) {
        Node receiver = n.getTarget().getReceiver();
        Receiver internalReceiver = FlowExpressions.internalReprOf(factory,
                receiver);
        List<Receiver> internalArguments = new ArrayList<>();
        for (Node arg : n.getArguments()) {
            internalArguments.add(FlowExpressions.internalReprOf(factory, arg));
        }
        FlowExpressionContext flowExprContext = new FlowExpressionContext(
                receiver.getType(), internalReceiver, internalArguments,
                factory);
        return flowExprContext;
    }
}
