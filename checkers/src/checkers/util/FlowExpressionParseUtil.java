package checkers.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javacutils.ElementUtils;
import javacutils.InternalUtils;
import javacutils.PurityUtils;
import javacutils.Resolver;
import javacutils.TreeUtils;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import checkers.source.Result;
import checkers.types.AnnotatedTypeFactory;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

import dataflow.analysis.FlowExpressions;
import dataflow.analysis.FlowExpressions.ClassName;
import dataflow.analysis.FlowExpressions.FieldAccess;
import dataflow.analysis.FlowExpressions.PureMethodCall;
import dataflow.analysis.FlowExpressions.Receiver;
import dataflow.analysis.FlowExpressions.ThisReference;
import dataflow.analysis.FlowExpressions.ValueLiteral;
import dataflow.cfg.node.ImplicitThisLiteralNode;
import dataflow.cfg.node.LocalVariableNode;
import dataflow.cfg.node.MethodInvocationNode;
import dataflow.cfg.node.Node;

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
    /**
     * Matches the self reference. In the future we could allow "#0" as a
     * synonym for "this".
     */
    protected static final Pattern selfPattern = Pattern.compile("^(this)$");
    /** Matches an identifier */
    protected static final Pattern identifierPattern = Pattern.compile("^"
            + identifierRegex + "$");
    /** Matches a method call */
    protected static final Pattern methodPattern = Pattern.compile("^("
            + identifierRegex + ")\\((.*)\\)$");
    /** Matches a field access */
    protected static final Pattern dotPattern = Pattern
            .compile("^([^.]+)\\.(.+)$");
    /** Regular expression for an integer literal */
    protected static final Pattern intPattern = Pattern
            .compile("^([1-9][0-9]*)$");
    /** Regular expression for a long literal */
    protected static final Pattern longPattern = Pattern
            .compile("^([1-9][0-9]*L)$");
    /** Regular expression for a string literal */
    protected static final Pattern stringPattern = Pattern
            .compile("^(\"([^\"\\\\]|\\\\.)*\")$");

    /**
     * Parse a string and return its representation as a
     * {@link Receiver}, or throw an
     * {@link FlowExpressionParseException}. The expression is assumed to be
     * used in the context of a method.
     *
     * @param s
     *            The string to parse.
     * @param context
     *            information about any receiver and arguments
     * @param path
     *            The current tree path.
     * @throws FlowExpressionParseException
     */
    public static/* @Nullable */FlowExpressions.Receiver parse(String s,
            FlowExpressionContext context, TreePath path)
            throws FlowExpressionParseException {
        Receiver result = parse(s, context, path, true, true, true, true, true,
                true);
        return result;
    }

    /**
     * Private implementation of {@link #parse} with a choice of which classes
     * of expressions should be parsed.
     */
    private static/* @Nullable */FlowExpressions.Receiver parse(String s,
            FlowExpressionContext context, TreePath path, boolean allowSelf,
            boolean allowIdentifier, boolean allowParameter, boolean allowDot,
            boolean allowMethods, boolean allowLiterals)
            throws FlowExpressionParseException {
        s = s.trim();

        Matcher identifierMatcher = identifierPattern.matcher(s);
        Matcher selfMatcher = selfPattern.matcher(s);
        Matcher parameterMatcher = parameterPattern.matcher(s);
        Matcher methodMatcher = methodPattern.matcher(s);
        Matcher dotMatcher = dotPattern.matcher(s);
        Matcher intMatcher = intPattern.matcher(s);
        Matcher longMatcher = longPattern.matcher(s);
        Matcher stringMatcher = stringPattern.matcher(s);

        ProcessingEnvironment env = context.atypeFactory.getProcessingEnv();
        Types types = env.getTypeUtils();

        if (intMatcher.matches() && allowLiterals) {
            int val = Integer.parseInt(s);
            return new ValueLiteral(types.getPrimitiveType(TypeKind.INT), val);
        } else if (longMatcher.matches() && allowLiterals) {
            long val = Long.parseLong(s.substring(0, s.length() - 1));
            return new ValueLiteral(types.getPrimitiveType(TypeKind.LONG), val);
        } else if (stringMatcher.matches() && allowLiterals) {
            TypeElement stringTypeElem = env.getElementUtils().getTypeElement(
                    "java.lang.String");
            return new ValueLiteral(types.getDeclaredType(stringTypeElem),
                    s.substring(1, s.length() - 1));
        } else if (selfMatcher.matches() && allowSelf) {
            // this literal
            return new ThisReference(context.receiver.getType());
        } else if (identifierMatcher.matches() && allowIdentifier) {
            Resolver resolver = new Resolver(env);
            try {
                // field access
                Element fieldElem = resolver.findField(s,
                        context.receiver.getType(), path);
                if (ElementUtils.isStatic(fieldElem)) {
                    Element classElem = fieldElem.getEnclosingElement();
                    Receiver staticClassReceiver = new ClassName(
                            ElementUtils.getType(classElem));
                    return new FieldAccess(staticClassReceiver,
                            ElementUtils.getType(fieldElem), fieldElem);
                } else {
                    return new FieldAccess(context.receiver,
                            ElementUtils.getType(fieldElem), fieldElem);
                }
            } catch (Throwable t) {
                try {
                    // class literal
                    Element classElem = resolver.findClass(s, path);
                    return new ClassName(ElementUtils.getType(classElem));
                } catch (Throwable t2) {
                    throw constructParserException(s);
                }
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

            // parse parameter list
            String parameterList = methodMatcher.group(2);
            List<Receiver> parameters = ParameterListParser.parseParameterList(
                    parameterList, true, context.useOuterReceiver(), path);

            // get types for parameters
            List<TypeMirror> parameterTypes = new ArrayList<>();
            for (Receiver p : parameters) {
                parameterTypes.add(p.getType());
            }
            Element methodElement = null;
            try {
                // try to find the correct method
                Resolver resolver = new Resolver(env);
                methodElement = resolver.findMethod(methodName,
                        context.receiver.getType(), path, parameterTypes);
            } catch (Throwable t) {
                throw constructParserException(s);
            }
            // check that the method is pure
            assert methodElement != null;
            if (!PurityUtils.isDeterministic(context.atypeFactory,
                    methodElement)) {
                throw new FlowExpressionParseException(Result.failure(
                        "flowexpr.method.not.pure",
                        methodElement.getSimpleName()));
            }
            return new PureMethodCall(ElementUtils.getType(methodElement),
                    methodElement, context.receiver, parameters);
        } else if (dotMatcher.matches() && allowDot) {
            String receiverString = dotMatcher.group(1);
            String remainingString = dotMatcher.group(2);

            // Parse the receiver first.
            Receiver receiver = parse(receiverString, context, path);

            // Parse the rest, with a new receiver.
            FlowExpressionContext newContext = context.changeReceiver(receiver);
            return parse(remainingString, newContext, path, false, true, false,
                    true, true, false);
        } else {
            throw constructParserException(s);
        }
    }

    /**
     * Returns a {@link FlowExpressionParseException} for the string {@code s}.
     */
    private static FlowExpressionParseException constructParserException(
            String s) {
        return new FlowExpressionParseException(Result.failure(
                "flowexpr.parse.error", s));
    }

    /**
     * A very simple parser for parameter lists, i.e. strings of the form
     * {@code a, b, c} for some expressions {@code a}, {@code b} and {@code c}.
     *
     * @author Stefan Heule
     */
    private static class ParameterListParser {

        /**
         * Parse a parameter list and return the parameters as a list (or throw
         * a {@link FlowExpressionParseException}).
         */
        private static List<Receiver> parseParameterList(
                String parameterString, boolean allowEmptyList,
                FlowExpressionContext context, TreePath path)
                throws FlowExpressionParseException {
            ArrayList<Receiver> result = new ArrayList<>();
            // the index of the character in 'parameterString' that the parser
            // is currently looking at
            int idx = 0;
            // how deeply are method calls nested at this point? callLevel is 0
            // in the beginning, and increases with every method call by 1. For
            // instance it would be 2 at the end of the following string:
            // "get(get(1,2,"
            int callLevel = 0;
            // is the parser currently in a string literal?
            boolean inString = false;
            while (true) {
                // end of string reached
                if (idx == parameterString.length()) {
                    // finish current param
                    if (inString || callLevel > 0) {
                        throw constructParserException(parameterString);
                    } else {
                        finishParam(parameterString, allowEmptyList, context,
                                path, result, idx);
                        return result;
                    }
                }

                // get next character
                char next = parameterString.charAt(idx);
                idx++;

                // case split on character
                switch (next) {
                case ',':
                    if (inString) {
                        // stay in same state and consume the character
                    } else {
                        if (callLevel == 0) {
                            // parse first parameter
                            finishParam(parameterString, allowEmptyList,
                                    context, path, result, idx - 1);
                            // parse remaining parameters
                            List<Receiver> rest = parseParameterList(
                                    parameterString.substring(idx), false,
                                    context, path);
                            result.addAll(rest);
                            return result;
                        } else {
                            // not the outermost method call, defer parsing of
                            // this parameter list to recursive call.
                        }
                    }
                    break;
                case '"':
                    // start or finish string
                    inString = !inString;
                    break;
                case '(':
                    if (inString) {
                        // stay in same state and consume the character
                    } else {
                        callLevel++;
                    }
                    break;
                case ')':
                    if (inString) {
                        // stay in same state and consume the character
                    } else {
                        if (callLevel == 0) {
                            throw constructParserException(parameterString);
                        } else {
                            callLevel--;
                        }
                    }
                    break;
                default:
                    // stay in same state and consume the character
                    break;
                }
            }
        }

        private static void finishParam(String parameterString,
                boolean allowEmptyList, FlowExpressionContext context,
                TreePath path, ArrayList<Receiver> result, int idx)
                throws FlowExpressionParseException {
            if (idx == 0) {
                if (allowEmptyList) {
                    return;
                } else {
                    throw constructParserException(parameterString);
                }
            } else {
                result.add(parse(parameterString.substring(0, idx), context,
                        path));
            }
        }
    }

    /**
     * Context used to parse a flow expression.
     */
    public static class FlowExpressionContext {
        public final Receiver receiver;
        public final List<Receiver> arguments;
        public final AnnotatedTypeFactory atypeFactory;
        public final Receiver outerReceiver;

        public FlowExpressionContext(Receiver receiver,
                List<Receiver> arguments, AnnotatedTypeFactory factory) {
            assert factory != null;
            this.receiver = receiver;
            this.arguments = arguments;
            this.atypeFactory = factory;
            this.outerReceiver = receiver;
        }

        public FlowExpressionContext(Receiver receiver, Receiver outerReceiver,
                List<Receiver> arguments, AnnotatedTypeFactory factory) {
            assert factory != null;
            this.receiver = receiver;
            this.arguments = arguments;
            this.atypeFactory = factory;
            this.outerReceiver = outerReceiver;
        }

        /**
         * Returns a copy of the context that is identical, but has a different
         * receiver. The outer receiver remains unchanged.
         */
        public FlowExpressionContext changeReceiver(Receiver receiver) {
            return new FlowExpressionContext(receiver, outerReceiver,
                    arguments, atypeFactory);
        }

        /**
         * Returns a copy of the context that is identical, but uses the outer
         * receiver as main receiver.
         */
        public FlowExpressionContext useOuterReceiver() {
            return new FlowExpressionContext(outerReceiver, outerReceiver,
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
                internalReceiver, internalArguments, factory);
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
                internalReceiver, internalArguments, factory);
        return flowExprContext;
    }
}
