package org.checkerframework.framework.util;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.ArrayAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.ClassName;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.LocalVariable;
import org.checkerframework.dataflow.analysis.FlowExpressions.MethodCall;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.FlowExpressions.ThisReference;
import org.checkerframework.dataflow.analysis.FlowExpressions.ValueLiteral;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.framework.source.Result;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.Resolver;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.javacutil.trees.TreeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type.ClassType;

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
    protected static final Pattern thisPattern = Pattern.compile("^this$");
    /** Matches 'itself' - it refers to the variable that is annotated, which is different from 'this' */
    protected static final Pattern itselfPattern = Pattern.compile("^itself$");
    /** Matches 'super' */
    protected static final Pattern superPattern = Pattern.compile("^super$");
    /** Matches an identifier */
    protected static final Pattern identifierPattern = Pattern.compile("^"
            + identifierRegex + "$");
    /** Matches a method call */
    protected static final Pattern methodPattern = Pattern.compile("^("
            + identifierRegex + ")\\((.*)\\)$");
    /** Matches an array access */
    protected static final Pattern arrayPattern = Pattern.compile("^(.*)\\[(.*)\\]$");
    /** Matches a field access */
    protected static final Pattern dotPattern = Pattern
            .compile("^([^.]+)\\.(.+)$");
    /** Matches integer literals */
    protected static final Pattern intPattern = Pattern
            .compile("^[1-9][0-9]*$");
    /** Matches long literals */
    protected static final Pattern longPattern = Pattern
            .compile("^[1-9][0-9]*L$");
    /** Matches string literals */
    protected static final Pattern stringPattern = Pattern
            .compile("^\"([^\"\\\\]|\\\\.)*\"$");
    /** Matches the null literal */
    protected static final Pattern nullPattern = Pattern.compile("^null$");
    /** Matches an expression contained in matching start and end parentheses */
    protected static final Pattern parenthesesPattern = Pattern.compile("^\\((.*)\\)$");

    /**
     * Parse a string and return its representation as a {@link Receiver}, or
     * throw an {@link FlowExpressionParseException}. The expression is assumed
     * to be used in the context of a method.
     *
     * @param s
     *            The string to parse.
     * @param context
     *            information about any receiver and arguments
     * @param path
     *            The current tree path.
     */
    public static FlowExpressions. /*@Nullable*/ Receiver parse(String s,
            FlowExpressionContext context, TreePath path)
            throws FlowExpressionParseException {
        return parse(s, context, path, false, false);
    }

    /**
     * Parse a string and return its representation as a {@link Receiver}, or
     * throw an {@link FlowExpressionParseException}. The expression is assumed
     * to be used in the context of a method.
     * Returns null if 'itself' is passed in as the string to parse
     * and no receiver named 'itself' could be found.
     *
     * @param s
     *            The string to parse.
     * @param context
     *            information about any receiver and arguments
     * @param path
     *            The current tree path.
     */
    public static FlowExpressions. /*@Nullable*/ Receiver parseAllowingItself(String s,
            FlowExpressionContext context, TreePath path)
            throws FlowExpressionParseException {
        return parse(s, context, path, true, false);
    }

    private static FlowExpressions. /*@Nullable*/ Receiver parse(String s,
            FlowExpressionContext context, TreePath path, boolean allowItself, boolean recursiveCall)
            throws FlowExpressionParseException {
        Receiver result = parse(s, context, path, true, true, true, true, true, true,
                true, true, allowItself, recursiveCall);
        return result;
    }

    /**
     * Private implementation of {@link #parse} with a choice of which classes
     * of expressions should be parsed.
     */
    private static FlowExpressions. /*@Nullable*/ Receiver parse(String s,
            FlowExpressionContext context, TreePath path, boolean allowSelf,
            boolean allowIdentifier, boolean allowParameter, boolean allowDot,
            boolean allowMethods, boolean allowArrays, boolean allowLiterals,
            boolean allowLocalVariables, boolean allowItself, boolean recursiveCall)
            throws FlowExpressionParseException {
        s = s.trim();

        Matcher selfMatcher = thisPattern.matcher(s);

        // Do not do this in recursive calls, otherwise we can get an infinite loop where
        // "this" gets converted to "this.<fieldname>" in the line below, then
        // dotMatcher matches "this.<fieldname>" and calls this function recursively
        // with s == "this"
        if (selfMatcher.matches() && allowSelf && !recursiveCall) {
            s = context.receiver.toString(); // it is possible that s == "this" after this call
            selfMatcher = thisPattern.matcher(s); // Refresh the matcher
        }

        Matcher itselfMatcher = itselfPattern.matcher(s);
        Matcher identifierMatcher = identifierPattern.matcher(s);
        Matcher superMatcher = superPattern.matcher(s);
        Matcher parameterMatcher = parameterPattern.matcher(s);
        Matcher methodMatcher = methodPattern.matcher(s);
        Matcher arraymatcher = arrayPattern.matcher(s);
        Matcher dotMatcher = dotPattern.matcher(s);
        Matcher intMatcher = intPattern.matcher(s);
        Matcher longMatcher = longPattern.matcher(s);
        Matcher stringMatcher = stringPattern.matcher(s);
        Matcher nullMatcher = nullPattern.matcher(s);
        Matcher parenthesesMatcher = parenthesesPattern.matcher(s);

        ProcessingEnvironment env = context.checkerContext.getProcessingEnvironment();
        Types types = env.getTypeUtils();

        if (intMatcher.matches() && allowLiterals) {
            int val = Integer.parseInt(s);
            return new ValueLiteral(types.getPrimitiveType(TypeKind.INT), val);
        } else if (nullMatcher.matches() && allowLiterals) {
            return new ValueLiteral(types.getNullType(), (Object) null);
        } else if (longMatcher.matches() && allowLiterals) {
            long val = Long.parseLong(s.substring(0, s.length() - 1));
            return new ValueLiteral(types.getPrimitiveType(TypeKind.LONG), val);
        } else if (stringMatcher.matches() && allowLiterals) {
            TypeElement stringTypeElem = env.getElementUtils().getTypeElement(
                    "java.lang.String");
            return new ValueLiteral(types.getDeclaredType(stringTypeElem),
                    s.substring(1, s.length() - 1));
        } else if (selfMatcher.matches() && allowSelf) {
            // "this" literal, even after the call above to set s = context.receiver.toString();
            if (context.receiver == null || context.receiver.containsUnknown()) {
                return new ThisReference(context.receiver == null ? null : context.receiver.getType());
            } else { // If we already know the receiver, return it.
                return context.receiver;
            }
        } else if (superMatcher.matches() && allowSelf) {
            // super literal
            List<? extends TypeMirror> superTypes = types
                    .directSupertypes(context.receiver.getType());
            // find class supertype
            TypeMirror superType = null;
            for (TypeMirror t : superTypes) {
                // ignore interface types
                if (!(t instanceof ClassType)) {
                    continue;
                }
                ClassType tt = (ClassType) t;
                if (!tt.isInterface()) {
                    superType = t;
                    break;
                }
            }
            if (superType == null) {
                throw constructParserException(s);
            }
            return new ThisReference(superType);
        } else if (identifierMatcher.matches() && allowIdentifier) {
            Resolver resolver = new Resolver(env);
            try {
                if (allowLocalVariables) {
                    VariableElement varElem = resolver.findLocalVariableOrParameter(s, path);
                    if (varElem != null) {
                        return new LocalVariable(varElem);
                    }
                }

                // field access
                TypeMirror receiverType = context.receiver.getType();
                boolean originalReceiver = true;
                VariableElement fieldElem = null;

                // Search for field in each enclosing class.
                while (receiverType.getKind() == TypeKind.DECLARED) {
                    fieldElem = resolver.findField(s, receiverType, path);
                    if (fieldElem != null) {
                        break;
                    }
                    receiverType = ((DeclaredType)receiverType).getEnclosingType();
                    originalReceiver = false;
                }

                if (fieldElem == null) { // Try static fields of the enclosing class
                    Element classElem = context.checkerContext.getTreeUtils().getElement(TreeUtils.pathTillClass(path));
                    receiverType = ElementUtils.getType(classElem);
                    originalReceiver = false;

                    // Search for field in each enclosing class.
                    while (receiverType.getKind() == TypeKind.DECLARED) {
                        fieldElem = resolver.findField(s, receiverType, path);
                        if (fieldElem != null) {
                            break;
                        }
                        receiverType = ((DeclaredType)receiverType).getEnclosingType();
                    }
                }

                if (fieldElem == null || fieldElem.getKind() != ElementKind.FIELD) {
                    throw constructParserException(s); // TODO: It is poor design to use exceptions to pass information around. We should change this.
                }
                TypeMirror fieldType = ElementUtils.getType(fieldElem);
                if (ElementUtils.isStatic(fieldElem)) {
                    Element classElem = fieldElem.getEnclosingElement();
                    Receiver staticClassReceiver = new ClassName(
                            ElementUtils.getType(classElem));
                    return new FieldAccess(staticClassReceiver,
                            fieldType, fieldElem);
                } else {
                    if (originalReceiver) {
                        return new FieldAccess(context.receiver,
                                fieldType, fieldElem);
                    } else {
                        return new FieldAccess(FlowExpressions.internalReprOf(context.checkerContext.getAnnotationProvider(), new ImplicitThisLiteralNode(receiverType)),
                                fieldType, fieldElem);
                    }
                }
            } catch (Throwable t) { // TODO: It is poor design to use exceptions to pass information around. We should change this.
                try {
                    // class literal
                    Element classElem = resolver.findClass(s, path);
                    TypeMirror classType = ElementUtils.getType(classElem);
                    if (classType == null) {
                        throw constructParserException(s);
                    }
                    return new ClassName(classType);
                } catch (Throwable t2) {

                    if (allowItself && itselfMatcher.matches()) {
                        return null; // Don't throw an exception if 'itself' does not match an identifier.
                        // The callee knows that it passed in 'itself' and will handle the null return value.
                        // DO however throw an exception below if the call is recursive and 'itself' matches,
                        // because that might mean that the original expression was "classname.itself",
                        // which means a field named itself was explicitly sought.
                    }

                    throw constructParserException(s);
                }
            }
        } else if (parameterMatcher.matches() && allowParameter && context.arguments != null) {
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
        } else if (arraymatcher.matches() && allowArrays) {
            String receiverStr = arraymatcher.group(1);
            String indexStr = arraymatcher.group(2);
            Receiver receiver = parse(receiverStr, context, path);
            Receiver index = parse(indexStr, context, path);
            TypeMirror receiverType = receiver.getType();
            if (!(receiverType instanceof ArrayType)) {
                throw constructParserException(s);
            }
            TypeMirror componentType = ((ArrayType) receiverType)
                    .getComponentType();
            ArrayAccess result = new ArrayAccess(componentType, receiver, index);
            return result;
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
                TypeMirror receiverType = context.receiver.getType();

                // Search for method in each enclosing class.
                while (receiverType.getKind() == TypeKind.DECLARED) {
                    methodElement = resolver.findMethod(methodName, receiverType,
                            path, parameterTypes);
                    if (methodElement.getKind() == ElementKind.METHOD) {
                        break;
                    }
                    receiverType = ((DeclaredType)receiverType).getEnclosingType();
                }

                if (methodElement == null || methodElement.getKind() != ElementKind.METHOD) {
                    throw constructParserException(s);
                }

                ExecutableElement mElem = (ExecutableElement) methodElement;

                for (int i = 0; i < parameters.size(); i++) {
                    VariableElement formal = mElem.getParameters().get(i);
                    TypeMirror formalType = formal.asType();
                    Receiver actual = parameters.get(i);
                    TypeMirror actualType = actual.getType();
                    // boxing necessary
                    if (TypesUtils.isBoxedPrimitive(formalType) && TypesUtils.isPrimitive(actualType)) {
                        MethodSymbol valueOfMethod = TreeBuilder.getValueOfMethod(env, formalType);
                        List<Receiver> p = new ArrayList<>();
                        p.add(actual);
                        Receiver boxedParam = new MethodCall(formalType, valueOfMethod, new ClassName(formalType), p);
                        parameters.set(i, boxedParam);
                    }
                }
            } catch (Throwable t) {
                throw constructParserException(s);
            }
            // check that the method is pure (this is no longer required)
            assert methodElement != null;
            /*if (!PurityUtils.isDeterministic(context.checkerContext.getAnnotationProvider(),
                    methodElement)) {
                throw new FlowExpressionParseException(Result.failure(
                        "flowexpr.method.not.deterministic",
                        methodElement.getSimpleName()));
            }*/
            if (ElementUtils.isStatic(methodElement)) {
                Element classElem = methodElement.getEnclosingElement();
                Receiver staticClassReceiver = new ClassName(
                        ElementUtils.getType(classElem));
                return new MethodCall(ElementUtils.getType(methodElement),
                        methodElement, staticClassReceiver, parameters);
            } else {
                TypeMirror methodType = InternalUtils
                        .substituteMethodReturnType(
                                ElementUtils.getType(methodElement),
                                context.receiver.getType());
                return new MethodCall(methodType, methodElement,
                        context.receiver, parameters);
            }
        } else if (dotMatcher.matches() && allowDot) {
            String receiverString = dotMatcher.group(1);
            String remainingString = dotMatcher.group(2);

            // Parse the receiver first.
            Receiver receiver = parse(receiverString, context, path, allowItself, true);

            if (allowItself && receiver == null) {
                // "itself.<someexpression>", where "itself" is not a variable name. Let the caller handle it.

                return null;
            }

            if (receiver instanceof FlowExpressions.ClassName && remainingString.equals("class")) {
                return receiver;
            }

            // Parse the rest, with a new receiver.
            FlowExpressionContext newContext = context.changeReceiver(receiver);
            // Parameter allowItself is set to false since "itself" can only be
            // in the receiver, not in the remaining string.
            return parse(remainingString, newContext, path, false, true, false,
                    true, true, false, false, false, false, true);
        } else if (parenthesesMatcher.matches()) {
            String expressionString = parenthesesMatcher.group(1);
            // Do not modify the value of recursiveCall, since a parenthesis match is essentially
            // a match to a no-op and should not semantically affect the parsing.
            return parse(expressionString, context, path, allowSelf,
                    allowIdentifier, allowParameter, allowDot,
                    allowMethods, allowArrays, allowLiterals,
                    allowLocalVariables, allowItself, recursiveCall);
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
        public final Receiver outerReceiver;
        public final BaseContext checkerContext;

        public FlowExpressionContext(Receiver receiver,
                List<Receiver> arguments, BaseContext checkerContext) {
            assert checkerContext != null;
            this.receiver = receiver;
            this.arguments = arguments;
            this.outerReceiver = receiver;
            this.checkerContext = checkerContext;
        }

        public FlowExpressionContext(Receiver receiver, Receiver outerReceiver,
                List<Receiver> arguments, BaseContext checkerContext) {
            assert checkerContext != null;
            this.receiver = receiver;
            this.arguments = arguments;
            this.outerReceiver = outerReceiver;
            this.checkerContext = checkerContext;

        }

        /**
         * Returns a copy of the context that is identical, but has a different
         * receiver. The outer receiver remains unchanged.
         */
        public FlowExpressionContext changeReceiver(Receiver receiver) {
            return new FlowExpressionContext(receiver, outerReceiver,
                    arguments, checkerContext);
        }

        /**
         * Returns a copy of the context that is identical, but uses the outer
         * receiver as main receiver.
         */
        public FlowExpressionContext useOuterReceiver() {
            return new FlowExpressionContext(outerReceiver, outerReceiver,
                    arguments, checkerContext);
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
            MethodTree node, Tree classTree, BaseContext checkerContext) {
        Node receiver = new ImplicitThisLiteralNode(
                InternalUtils.typeOf(classTree));
        Receiver internalReceiver = FlowExpressions.internalReprOf(checkerContext.getAnnotationProvider(),
                receiver);
        List<Receiver> internalArguments = new ArrayList<>();
        for (VariableTree arg : node.getParameters()) {
            internalArguments.add(FlowExpressions.internalReprOf(checkerContext.getAnnotationProvider(),
                    new LocalVariableNode(arg, receiver)));
        }
        FlowExpressionContext flowExprContext = new FlowExpressionContext(
                internalReceiver, internalArguments, checkerContext);
        return flowExprContext;
    }

    /**
     * @return A {@link FlowExpressionContext} for the method {@code node} as
     *         seen at the method declaration.
     */
    public static FlowExpressionContext buildFlowExprContextForDeclaration(
            MethodTree node, TypeMirror classType, BaseContext checkerContext) {
        Node receiver = new ImplicitThisLiteralNode(classType);
        Receiver internalReceiver = FlowExpressions.internalReprOf(checkerContext.getAnnotationProvider(),
                receiver);
        List<Receiver> internalArguments = new ArrayList<>();
        for (VariableTree arg : node.getParameters()) {
            internalArguments.add(FlowExpressions.internalReprOf(checkerContext.getAnnotationProvider(),
                    new LocalVariableNode(arg, receiver)));
        }
        FlowExpressionContext flowExprContext = new FlowExpressionContext(
                internalReceiver, internalArguments, checkerContext);
        return flowExprContext;
    }

    /**
     * @return A {@link FlowExpressionContext} for the method {@code node} as
     *         seen at the method declaration.
     */
    public static FlowExpressionContext buildFlowExprContextForDeclaration(
            MethodTree node, TreePath currentPath, BaseContext checkerContext) {
        Tree classTree = TreeUtils.enclosingClass(currentPath);
        return buildFlowExprContextForDeclaration(node, classTree, checkerContext);
    }

    /**
     * @return A {@link FlowExpressionContext} for the class {@code classTree} as
     *         seen at the class declaration.
     */
    public static FlowExpressionContext buildFlowExprContextForDeclaration(
            ClassTree classTree, TreePath currentPath, BaseContext checkerContext) {
        Node receiver = new ImplicitThisLiteralNode(
                InternalUtils.typeOf(classTree));
        Receiver internalReceiver = FlowExpressions.internalReprOf(checkerContext.getAnnotationProvider(),
                receiver);
        List<Receiver> internalArguments = new ArrayList<>();
        FlowExpressionContext flowExprContext = new FlowExpressionContext(
                internalReceiver, internalArguments, checkerContext);
        return flowExprContext;
    }

    /**
     * @return A {@link FlowExpressionContext} for the method {@code n}
     *         (represented as a {@link Node} as seen at the method use (i.e.,
     *         at a method call site).
     */
    public static FlowExpressionContext buildFlowExprContextForUse(
            MethodInvocationNode n, BaseContext checkerContext) {
        Node receiver = n.getTarget().getReceiver();
        Receiver internalReceiver = FlowExpressions.internalReprOf(checkerContext.getAnnotationProvider(),
                receiver);
        List<Receiver> internalArguments = new ArrayList<>();
        for (Node arg : n.getArguments()) {
            internalArguments.add(FlowExpressions.internalReprOf(checkerContext.getAnnotationProvider(), arg));
        }
        FlowExpressionContext flowExprContext = new FlowExpressionContext(
                internalReceiver, internalArguments, checkerContext);
        return flowExprContext;
    }

    /**
     * @return A {@link FlowExpressionContext} for the constructor {@code n}
     *         (represented as a {@link Node} as seen at the method use (i.e.,
     *         at a method call site).
     */
    public static FlowExpressionContext buildFlowExprContextForUse(
            ObjectCreationNode n, TreePath currentPath, BaseContext checkerContext) {

        // Since the object that is being created does not exist yet,
        // the receiver of the constructor will be the current object if
        // the constructor is called within a nonstatic method body.
        // Otherwise it will be the enclosing class.

        Node receiver = null;

        MethodTree enclosingMethod = TreeUtils.enclosingMethod(currentPath);
        ClassTree enclosingClass = TreeUtils.enclosingClass(currentPath);

        if (enclosingMethod != null && !enclosingMethod.getModifiers().getFlags().contains(Modifier.STATIC)) {
            receiver = new ImplicitThisLiteralNode(InternalUtils.typeOf(enclosingClass));
        } else {
            receiver = new ClassNameNode(enclosingClass);
        }

        Receiver internalReceiver = FlowExpressions.internalReprOf(checkerContext.getAnnotationProvider(), receiver);

        List<Receiver> internalArguments = new ArrayList<>();
        for (Node arg : n.getArguments()) {
            internalArguments.add(FlowExpressions.internalReprOf(checkerContext.getAnnotationProvider(), arg));
        }

        FlowExpressionContext flowExprContext = new FlowExpressionContext(
                internalReceiver, internalArguments, checkerContext);

        return flowExprContext;
    }

    /**
     * Calls the method of the same name but with a first argument type of either MethodInvocationNode or
     * ObjectCreationNode depending on the type of node.
     * @throws java.lang.IllegalArgumentException if node is not a MethodInvocationNode or ObjectCreationNode
     */
    public static FlowExpressionContext buildFlowExprContextForViewpointUse(
            Node node, TreePath pathToInvocation, TreePath enclosingMethodPath, BaseContext checkerContext) {
        if (node instanceof MethodInvocationNode) {
            return buildFlowExprContextForViewpointUse((MethodInvocationNode) node, enclosingMethodPath, checkerContext);
        } else if (node instanceof ObjectCreationNode) {
            return buildFlowExprContextForViewpointUse((ObjectCreationNode) node, pathToInvocation, enclosingMethodPath, checkerContext);
        }

        throw new IllegalArgumentException(
           "Node must be either a MethodInvocationNode or an ObjectCreationNode\n"
         + "node=" + node + "\n"
         + "pathToInvocation=" + pathToInvocation + "\n"
         + "enclosingMethodPath=" + enclosingMethodPath
        );
    }

    /**
     * This method is the same as building a flow expression for a MethodInvocationNode except
     * it uses the PARAMETERS list from the enclosing method context.  If there is no enclosing
     * method then this method functions exactly like the buildFowExprContextForUse.
     * This is really built for a use in KeyFor and should likely be replaced
     */
    public static FlowExpressionContext buildFlowExprContextForViewpointUse(
            MethodInvocationNode n, TreePath enclosingMethodPath, BaseContext checkerContext) {
        final FlowExpressionContext fromCreation = buildFlowExprContextForUse(n, checkerContext);

        if (enclosingMethodPath == null) {
            return fromCreation;
        }
        final MethodTree methodTree = (MethodTree) enclosingMethodPath.getLeaf();

        final FlowExpressionContext fromEnclosingMethod =
                buildFlowExprContextForDeclaration(methodTree, enclosingMethodPath.getLeaf(), checkerContext);


        FlowExpressionContext flowExprContext = new FlowExpressionContext(
                fromCreation.receiver, fromEnclosingMethod.arguments, checkerContext);

        return flowExprContext;
    }

    /**
     * This method is the same as building a flow expression for a ObjectCreationNode except
     * it uses the PARAMETERS list from the enclosing method context.  If there is no enclosing
     * method then this method functions exactly like the buildFowExprContextForUse.
     * This is really built for a use in KeyFor and should likely be replaced
     */
    public static FlowExpressionContext buildFlowExprContextForViewpointUse(
            ObjectCreationNode n, TreePath currentPath, TreePath enclosingMethodPath, BaseContext checkerContext) {
        final FlowExpressionContext fromCreation = buildFlowExprContextForUse(n, currentPath, checkerContext);

        if (enclosingMethodPath == null) {
            return fromCreation;
        }
        final MethodTree methodTree = (MethodTree) enclosingMethodPath.getLeaf();

        final FlowExpressionContext fromEnclosingMethod =
                buildFlowExprContextForDeclaration(methodTree, enclosingMethodPath.getLeaf(), checkerContext);


        FlowExpressionContext flowExprContext = new FlowExpressionContext(
               fromCreation.receiver, fromEnclosingMethod.arguments, checkerContext);

        return flowExprContext;
    }
}
