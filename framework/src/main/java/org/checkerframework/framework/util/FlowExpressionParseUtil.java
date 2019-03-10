package org.checkerframework.framework.util;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;
import java.util.ArrayList;
import java.util.Arrays;
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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
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
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.util.dependenttypes.DependentTypesError;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.Resolver;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.javacutil.trees.TreeBuilder;

/**
 * A collection of helper methods to parse a string that represents a restricted Java expression.
 * Such expressions can be found in annotations (e.g., to specify a pre- or postcondition).
 */
public class FlowExpressionParseUtil {

    /**
     * Regular expression for an identifier. Permits '$' in the name, though that character never
     * appears in Java source code.
     */
    protected static final String IDENTIFIER_REGEX = "[a-zA-Z_$][a-zA-Z_$0-9]*";
    /** Regular expression for a formal parameter use. */
    protected static final String PARAMETER_REGEX = "#([1-9][0-9]*)";

    /** Regular expression for a string literal. */
    // Regex can be found at, for example, http://stackoverflow.com/a/481587/173852
    protected static final String STRING_REGEX = "\"(?:[^\"\\\\]|\\\\.)*\"";

    /** Unanchored; can be used to find all formal parameter uses. */
    protected static final Pattern UNANCHORED_PARAMETER_PATTERN = Pattern.compile(PARAMETER_REGEX);

    /** Returns a Pattern, anchored at the beginning and end, for the regex. */
    private static Pattern anchored(String regex) {
        return Pattern.compile("^" + regex + "$");
    }

    // Each of the below patterns is anchored with ^...$.
    /** Matches a parameter. */
    protected static final Pattern PARAMETER_PATTERN = anchored(PARAMETER_REGEX);
    /** Matches an identifier. */
    protected static final Pattern IDENTIFIER_PATTERN = anchored(IDENTIFIER_REGEX);
    /** Matches a string starting with an identifier. */
    protected static final Pattern STARTS_WITH_IDENTIFIER_PATTERN =
            anchored("(" + IDENTIFIER_REGEX + ").*");
    /** Matches integer literals. */
    protected static final Pattern INT_PATTERN = anchored("[-+]?[0-9]+");
    /** Matches long literals. */
    protected static final Pattern LONG_PATTERN = anchored("[-+]?[0-9]+[Ll]");
    /** Matches (some) floating-point and double literals. */
    protected static final Pattern FLOAT_PATTERN = anchored("[+-]?([0-9]+[.][0-9]*|[.][0-9]+)");

    /** Matches string literals. */
    protected static final Pattern STRING_PATTERN = anchored(STRING_REGEX);
    /** Matches an expression contained in matching start and end parentheses. */
    protected static final Pattern PARENTHESES_PATTERN = anchored("\\((.*)\\)");
    /** Matches an expression that starts with a string. */
    protected static final Pattern STARTS_WITH_STRING_PATTERN =
            anchored("(" + STRING_REGEX + ").*");
    /** Matches member select on a string, such as {@code "hello".length}. */
    protected static final Pattern STRING_SELECT_PATTERN =
            anchored("(" + STRING_REGEX + ")" + "\\.(.*)");

    /**
     * Parse a string and return its representation as a {@link Receiver}, or throw an {@link
     * FlowExpressionParseException}.
     *
     * @param expression flow expression to parse
     * @param context information about any receiver and arguments
     * @param localScope path to local scope to use
     * @param useLocalScope whether {@code localScope} should be used to resolve identifiers
     */
    public static FlowExpressions.Receiver parse(
            String expression,
            FlowExpressionContext context,
            TreePath localScope,
            boolean useLocalScope)
            throws FlowExpressionParseException {
        context = context.copyAndSetUseLocalScope(useLocalScope);
        FlowExpressions.Receiver result = parseHelper(expression, context, localScope);
        if (result instanceof ClassName && !expression.endsWith("class")) {
            throw constructParserException(
                    expression, "a class name cannot terminate a flow expression string");
        }
        return result;
    }

    private static FlowExpressions.Receiver parseHelper(
            String expression, FlowExpressionContext context, TreePath path)
            throws FlowExpressionParseException {
        expression = expression.trim();

        ProcessingEnvironment env = context.checkerContext.getProcessingEnvironment();
        Types types = env.getTypeUtils();

        if (isNullLiteral(expression, context)) {
            return parseNullLiteral(types);
        } else if (isIntLiteral(expression, context)) {
            return parseIntLiteral(expression, types);
        } else if (isLongLiteral(expression, context)) {
            return parseLongLiteral(expression, types);
        } else if (isFloatLiteral(expression, context)) {
            throw constructParserException(
                    expression,
                    String.format("floating-point values '%s' cannot be parsed", expression));
        } else if (isStringLiteral(expression, context)) {
            return parseStringLiteral(expression, types, env.getElementUtils());
        } else if (isThisLiteral(expression, context)) {
            return parseThis(context);
        } else if (isSuperLiteral(expression, context)) {
            return parseSuper(expression, types, context);
        } else if (isIdentifier(expression)) {
            return parseIdentifier(expression, env, path, context);
        } else if (isParameter(expression, context)) {
            return parseParameter(expression, context);
        } else if (isArray(expression)) {
            return parseArray(expression, context, path);
        } else if (isMethodCall(expression)) {
            return parseMethodCall(expression, context, path, env);
        } else if (isMemberSelect(expression)) {
            return parseMemberSelect(expression, env, context, path);
        } else if (isParentheses(expression, context)) {
            return parseParentheses(expression, context, path);
        } else {
            String message;
            if (expression.equals("#0")) {
                message =
                        "one should use \"this\" for the receiver or \"#1\" for the first formal parameter";
            } else {
                message = String.format("is an unrecognized expression");
            }
            if (context.parsingMember) {
                message += " in a context with parsingMember=true";
            }
            throw constructParserException(expression, message);
        }
    }

    private static boolean isMemberSelect(String s) {
        return parseMemberSelect(s) != null;
    }

    /**
     * Matches a field access. First of returned pair is object and second is field.
     *
     * @param s expression string
     * @return pair of object and field
     */
    private static Pair<String, String> parseMemberSelect(String s) {
        Pair<Pair<String, String>, String> method = parseMethodCall(s);
        if (method != null && method.second.startsWith(".")) {
            return Pair.of(
                    method.first.first + "(" + method.first.second + ")",
                    method.second.substring(1));
        }

        Pair<Pair<String, String>, String> array = parseArray(s);
        if (array != null && array.second.startsWith(".")) {
            return Pair.of(
                    array.first.first + "[" + array.first.second + "]", array.second.substring(1));
        }

        Matcher m = STRING_SELECT_PATTERN.matcher(s);
        if (m.matches()) {
            return Pair.of(m.group(1), m.group(2));
        }

        int nextRParenPos = matchingCloseParen(s, 0, '(', ')');
        if (nextRParenPos != -1) {
            if (nextRParenPos + 1 < s.length() && s.charAt(nextRParenPos + 1) == '.') {
                String receiver = s.substring(0, nextRParenPos + 1);
                String remaining = s.substring(nextRParenPos + 2);
                return Pair.of(receiver, remaining);
            } else {
                return null;
            }
        }

        int i = s.indexOf(".");
        if (i == -1) {
            return null;
        }

        String receiver = s.substring(0, i);
        String remaining = s.substring(i + 1);

        return Pair.of(receiver, remaining);
    }

    private static Receiver parseMemberSelect(
            String s, ProcessingEnvironment env, FlowExpressionContext context, TreePath path)
            throws FlowExpressionParseException {
        Pair<String, String> select = parseMemberSelect(s);
        assert select != null : "isMemberSelect must be called first";

        Receiver receiver;
        String memberSelected;

        Resolver resolver = new Resolver(env);

        // Attempt to match a package and class name first.
        Pair<ClassName, String> classAndRemainingString =
                matchPackageAndClassNameWithinExpression(s, resolver, path);
        if (classAndRemainingString != null) {
            receiver = classAndRemainingString.first;
            memberSelected = classAndRemainingString.second;
            if (memberSelected == null) {
                throw constructParserException(
                        s, "a class cannot terminate a flow expression string");
            }
        } else {
            String receiverString = select.first;
            memberSelected = select.second;
            receiver = parseHelper(receiverString, context, path);
        }

        if (memberSelected.equals("class")) {
            if (receiver instanceof FlowExpressions.ClassName && !context.parsingMember) {
                return receiver;
            } else {
                throw constructParserException(s, "class is not a legal identifier");
            }
        }

        // Parse the rest, with a new receiver.
        FlowExpressionContext newContext = context.copyChangeToParsingMemberOfReceiver(receiver);
        return parseHelper(memberSelected, newContext, path);
    }

    // ########

    private static boolean isNullLiteral(String s, FlowExpressionContext context) {
        if (context.parsingMember) {
            return false;
        }
        return s.equals("null");
    }

    private static Receiver parseNullLiteral(Types types) {
        return new ValueLiteral(types.getNullType(), (Object) null);
    }

    private static boolean isIntLiteral(String s, FlowExpressionContext context) {
        if (context.parsingMember) {
            return false;
        }
        Matcher intMatcher = INT_PATTERN.matcher(s);
        return intMatcher.matches();
    }

    private static Receiver parseIntLiteral(String s, Types types) {
        int val = Integer.parseInt(s);
        return new ValueLiteral(types.getPrimitiveType(TypeKind.INT), val);
    }

    private static boolean isLongLiteral(String s, FlowExpressionContext context) {
        if (context.parsingMember) {
            return false;
        }
        Matcher longMatcher = LONG_PATTERN.matcher(s);
        return longMatcher.matches();
    }

    private static Receiver parseLongLiteral(String s, Types types) {
        // Remove L or l at the end of a long literal
        s = s.substring(0, s.length() - 1);
        long val = Long.parseLong(s);
        return new ValueLiteral(types.getPrimitiveType(TypeKind.LONG), val);
    }

    private static boolean isFloatLiteral(String s, FlowExpressionContext context) {
        if (context.parsingMember) {
            return false;
        }
        Matcher floatMatcher = FLOAT_PATTERN.matcher(s);
        return floatMatcher.matches();
    }

    /** Return true iff s is a string literal. */
    private static boolean isStringLiteral(String s, FlowExpressionContext context) {
        if (context.parsingMember) {
            return false;
        }
        Matcher stringMatcher = STRING_PATTERN.matcher(s);
        return stringMatcher.matches();
    }

    private static Receiver parseStringLiteral(String s, Types types, Elements elements) {
        TypeElement stringTypeElem = elements.getTypeElement("java.lang.String");
        return new ValueLiteral(
                types.getDeclaredType(stringTypeElem), s.substring(1, s.length() - 1));
    }

    private static boolean isThisLiteral(String s, FlowExpressionContext context) {
        if (context.parsingMember) {
            // TODO: this is probably wrong because you could have an inner class receiver
            // Outer.this
            return false;
        }
        // Do not allow "#0" because it's ambiguous:  a reader might assume that #0 is the first
        // formal parameter.
        return s.equals("this");
    }

    private static Receiver parseThis(FlowExpressionContext context) {
        if (!(context.receiver == null || context.receiver.containsUnknown())) {
            // "this" is the receiver of the context
            return context.receiver;
        } else {
            return new ThisReference(context.receiver == null ? null : context.receiver.getType());
        }
    }

    private static boolean isSuperLiteral(String s, FlowExpressionContext context) {
        if (context.parsingMember) {
            return false;
        }
        return s.equals("super");
    }

    private static Receiver parseSuper(String s, Types types, FlowExpressionContext context)
            throws FlowExpressionParseException {
        // super literal
        List<? extends TypeMirror> superTypes = types.directSupertypes(context.receiver.getType());
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
            throw constructParserException(s, "super class not found");
        }
        return new ThisReference(superType);
    }

    private static boolean isIdentifier(String s) {
        return IDENTIFIER_PATTERN.matcher(s).matches();
    }

    private static Receiver parseIdentifier(
            String s, ProcessingEnvironment env, TreePath path, FlowExpressionContext context)
            throws FlowExpressionParseException {
        Resolver resolver = new Resolver(env);
        if (!context.parsingMember && context.useLocalScope) {
            // Attempt to match a local variable within the scope of the
            // given path before attempting to match a field.
            VariableElement varElem = resolver.findLocalVariableOrParameterOrField(s, path);
            if (varElem != null) {
                if (varElem.getKind() == ElementKind.FIELD) {
                    boolean isOriginalReceiver = context.receiver instanceof ThisReference;
                    return getReceiverField(s, context, isOriginalReceiver, varElem);
                } else {
                    return new LocalVariable(varElem);
                }
            }
        }

        // field access
        TypeMirror receiverType = context.receiver.getType();
        boolean originalReceiver = true;
        VariableElement fieldElem = null;

        if (receiverType.getKind() == TypeKind.ARRAY && s.equals("length")) {
            fieldElem = resolver.findField(s, receiverType, path);
        }

        // Search for field in each enclosing class.
        while (receiverType.getKind() == TypeKind.DECLARED) {
            fieldElem = resolver.findField(s, receiverType, path);
            if (fieldElem != null) {
                break;
            }
            receiverType = getTypeOfEnclosingClass((DeclaredType) receiverType);
            originalReceiver = false;
        }

        if (fieldElem != null && fieldElem.getKind() == ElementKind.FIELD) {
            return getReceiverField(s, context, originalReceiver, fieldElem);
        }

        // Class name
        Element classElem = resolver.findClass(s, path);
        TypeMirror classType = ElementUtils.getType(classElem);
        if (classType != null) {
            return new ClassName(classType);
        }

        MethodTree enclMethod = TreeUtils.enclosingMethod(path);
        if (enclMethod != null) {
            List<? extends VariableTree> params = enclMethod.getParameters();
            for (int i = 0; i < params.size(); i++) {
                if (params.get(i).getName().contentEquals(s)) {
                    throw constructParserException(
                            s,
                            String.format(DependentTypesError.FORMAL_PARAM_NAME_STRING, i + 1, s));
                }
            }
        }

        throw constructParserException(s, "identifier not found");
    }

    private static Receiver getReceiverField(
            String s,
            FlowExpressionContext context,
            boolean originalReceiver,
            VariableElement fieldElem)
            throws FlowExpressionParseException {
        TypeMirror receiverType = context.receiver.getType();

        TypeMirror fieldType = ElementUtils.getType(fieldElem);
        if (ElementUtils.isStatic(fieldElem)) {
            Element classElem = fieldElem.getEnclosingElement();
            Receiver staticClassReceiver = new ClassName(ElementUtils.getType(classElem));
            return new FieldAccess(staticClassReceiver, fieldType, fieldElem);
        }
        Receiver locationOfField;
        if (originalReceiver) {
            locationOfField = context.receiver;
        } else {
            locationOfField =
                    FlowExpressions.internalReprOf(
                            context.checkerContext.getAnnotationProvider(),
                            new ImplicitThisLiteralNode(receiverType));
        }
        if (locationOfField instanceof ClassName) {
            throw constructParserException(
                    s, "a non-static field cannot have a class name as a receiver.");
        }
        return new FieldAccess(locationOfField, fieldType, fieldElem);
    }

    private static boolean isParameter(String s, FlowExpressionContext context) {
        if (context.parsingMember) {
            return false;
        }
        Matcher parameterMatcher = PARAMETER_PATTERN.matcher(s);
        return parameterMatcher.matches();
    }

    private static Receiver parseParameter(String s, FlowExpressionContext context)
            throws FlowExpressionParseException {
        Matcher parameterMatcher = PARAMETER_PATTERN.matcher(s);
        if (!parameterMatcher.matches()) {
            return null;
        }
        if (context.arguments == null) {
            throw constructParserException(s, "no parameter found");
        }
        int idx = -1;
        try {
            idx = Integer.parseInt(parameterMatcher.group(1));
        } catch (NumberFormatException e) {
            // cannot occur by the way the pattern is defined (matches only numbers)
            assert false;
        }
        if (idx > context.arguments.size()) {
            throw new FlowExpressionParseException(
                    "flowexpr.parse.index.too.big", Integer.toString(idx));
        }
        return context.arguments.get(idx - 1);
    }

    /**
     * Parse a method call. First of returned pair is a pair of method name and arguments. Second of
     * returned pair is a remaining string.
     *
     * @param s expression string
     * @return pair of (pair of method name and arguments) and remaining
     */
    private static Pair<Pair<String, String>, String> parseMethodCall(String s) {
        // Parse Identifier
        Matcher m = STARTS_WITH_IDENTIFIER_PATTERN.matcher(s);
        if (!m.matches()) {
            return null;
        }
        String ident = m.group(1);
        int i = ident.length();

        int rparenPos = matchingCloseParen(s, i, '(', ')');
        if (rparenPos == -1) {
            return null;
        }

        String arguments = s.substring(i + 1, rparenPos);
        String remaining = s.substring(rparenPos + 1);
        return Pair.of(Pair.of(ident, arguments), remaining);
    }

    private static boolean isMethodCall(String s) {
        Pair<Pair<String, String>, String> result = parseMethodCall(s);
        return result != null && result.second.isEmpty();
    }

    private static Receiver parseMethodCall(
            String s, FlowExpressionContext context, TreePath path, ProcessingEnvironment env)
            throws FlowExpressionParseException {
        Pair<Pair<String, String>, String> method = parseMethodCall(s);
        if (method == null) {
            return null;
        }

        String methodName = method.first.first;

        // parse parameter list
        String parameterList = method.first.second;
        List<Receiver> parameters =
                ParameterListParser.parseParameterList(
                        parameterList, true, context.copyAndUseOuterReceiver(), path);

        // get types for parameters
        List<TypeMirror> parameterTypes = new ArrayList<>();
        for (Receiver p : parameters) {
            parameterTypes.add(p.getType());
        }
        ExecutableElement methodElement = null;
        try {
            Element element = null;

            // try to find the correct method
            Resolver resolver = new Resolver(env);
            TypeMirror receiverType = context.receiver.getType();

            if (receiverType.getKind() == TypeKind.ARRAY) {
                element = resolver.findMethod(methodName, receiverType, path, parameterTypes);
            }

            // Search for method in each enclosing class.
            while (receiverType.getKind() == TypeKind.DECLARED) {
                element = resolver.findMethod(methodName, receiverType, path, parameterTypes);
                if (element.getKind() == ElementKind.METHOD) {
                    break;
                }
                receiverType = getTypeOfEnclosingClass((DeclaredType) receiverType);
            }

            if (element == null) {
                throw constructParserException(s, "element==null");
            }
            if (element.getKind() != ElementKind.METHOD) {
                throw constructParserException(s, "element.getKind()==" + element.getKind());
            }

            methodElement = (ExecutableElement) element;

            for (int i = 0; i < parameters.size(); i++) {
                VariableElement formal = methodElement.getParameters().get(i);
                TypeMirror formalType = formal.asType();
                Receiver actual = parameters.get(i);
                TypeMirror actualType = actual.getType();
                // boxing necessary
                if (TypesUtils.isBoxedPrimitive(formalType) && TypesUtils.isPrimitive(actualType)) {
                    MethodSymbol valueOfMethod = TreeBuilder.getValueOfMethod(env, formalType);
                    List<Receiver> p = new ArrayList<>();
                    p.add(actual);
                    Receiver boxedParam =
                            new MethodCall(formalType, valueOfMethod, new ClassName(formalType), p);
                    parameters.set(i, boxedParam);
                }
            }
        } catch (Throwable t) {
            if (t.getMessage() == null) {
                throw new Error("no detail message in " + t.getClass(), t);
            }
            throw constructParserException(s, t.getMessage());
        }
        assert methodElement != null;
        // TODO: reinstate this test, but issue a warning that the user
        // can override, rather than halting parsing which the user cannot override.
        /*if (!PurityUtils.isDeterministic(context.checkerContext.getAnnotationProvider(),
                methodElement)) {
            throw new FlowExpressionParseException(Result.failure(
                    "flowexpr.method.not.deterministic",
                    methodElement.getSimpleName()));
        }*/
        if (ElementUtils.isStatic(methodElement)) {
            Element classElem = methodElement.getEnclosingElement();
            Receiver staticClassReceiver = new ClassName(ElementUtils.getType(classElem));
            return new MethodCall(
                    ElementUtils.getType(methodElement),
                    methodElement,
                    staticClassReceiver,
                    parameters);
        } else {
            if (context.receiver instanceof ClassName) {
                throw constructParserException(
                        s, "a non-static method call cannot have a class name as a receiver");
            }
            TypeMirror methodType =
                    TypesUtils.substituteMethodReturnType(
                            methodElement, context.receiver.getType(), env);
            return new MethodCall(methodType, methodElement, context.receiver, parameters);
        }
    }

    /**
     * Parse a array access. First of returned pair is a pair of an array to be accessed and an
     * index. Second of returned pair is a remaining string.
     *
     * @param s expression string
     * @return pair of (pair of an array to be accessed and an index) and remaining. Returns null if
     *     parsing fails.
     */
    private static Pair<Pair<String, String>, String> parseArray(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) != '[') {
            i++;
        }

        if (i >= s.length()) {
            return null;
        }

        while (true) {
            int nextRBracketPos = matchingCloseParen(s, i, '[', ']');
            if (nextRBracketPos == -1) {
                return null;
            }

            int nextLBracketPos = nextRBracketPos + 1;
            if (nextLBracketPos < s.length() && s.charAt(nextLBracketPos) == '[') {
                i = nextLBracketPos;
                continue;
            }

            return Pair.of(
                    Pair.of(s.substring(0, i), s.substring(i + 1, nextRBracketPos)),
                    s.substring(nextLBracketPos));
        }
    }

    /**
     * Find occurrence of {@code close} that matches the occurrence of {@code open} at {@code
     * openPos}. Handles nested occurrences of "{@code open} ... {@code close}".
     *
     * @return matching occurrence of {@code close}, or -1 if not found
     */
    private static int matchingCloseParen(String s, int openPos, char open, char close) {
        // expect `open` at `openPos` in `s`
        if (s.length() <= openPos || s.charAt(openPos) != open) {
            return -1;
        }

        int i = openPos + 1;
        int depth = 1;
        while (i < s.length()) {
            char ch = s.charAt(i++);
            if (ch == '"') {
                i--;
                Matcher m = STARTS_WITH_STRING_PATTERN.matcher(s.substring(i));
                if (!m.matches()) {
                    break;
                }
                i += m.group(1).length();
            } else if (ch == open) {
                depth++;
            } else if (ch == close) {
                depth--;
                if (depth == 0) {
                    return i - 1;
                }
            }
        }
        return -1;
    }

    private static boolean isArray(String s) {
        Pair<Pair<String, String>, String> result = parseArray(s);
        return result != null && result.second.isEmpty();
    }

    private static Receiver parseArray(String s, FlowExpressionContext context, TreePath path)
            throws FlowExpressionParseException {
        Pair<Pair<String, String>, String> array = parseArray(s);
        if (array == null) {
            return null;
        }

        String receiverStr = array.first.first;
        String indexStr = array.first.second;
        Receiver receiver = parseHelper(receiverStr, context, path);
        FlowExpressionContext contextForIndex = context.copyAndUseOuterReceiver();
        Receiver index = parseHelper(indexStr, contextForIndex, path);
        TypeMirror receiverType = receiver.getType();
        if (!(receiverType instanceof ArrayType)) {
            throw constructParserException(
                    s, String.format("receiver not an array: %s : %s", receiver, receiverType));
        }
        TypeMirror componentType = ((ArrayType) receiverType).getComponentType();
        ArrayAccess result = new ArrayAccess(componentType, receiver, index);
        return result;
    }

    // TODO: This incorrectly returns true for "(a)+(b)" where the inital and final parens do not
    // match.
    private static boolean isParentheses(
            String s, @SuppressWarnings("UnusedVariable") FlowExpressionContext contex) {
        return s.length() > 2 && s.charAt(0) == '(' && s.charAt(s.length() - 1) == ')';
    }

    private static Receiver parseParentheses(String s, FlowExpressionContext context, TreePath path)
            throws FlowExpressionParseException {
        if (!isParentheses(s, context)) {
            return null;
        }
        // TODO: this is the wrong thing for an expression like "(a)+(b)".
        String expressionString = s.substring(1, s.length() - 1);
        // Do not modify the value of recursiveCall, since a parenthesis match is essentially
        // a match to a no-op and should not semantically affect the parsing.
        return parseHelper(expressionString, context, path);
    }

    /**
     * Matches a substring of {@code expression} to a package and class name (starting from the
     * beginning of the string).
     *
     * @param expression the expression string that may start with a package and class name
     * @param resolver the {@code Resolver} for the current processing environment
     * @param path the tree path to the local scope
     * @return {@code null} if the expression string did not start with a package name; otherwise a
     *     {@code Pair} containing the {@code ClassName} for the matched class, and the remaining
     *     substring of the expression (possibly null) after the package and class name.
     * @throws FlowExpressionParseException if the entire expression string matches a package name
     *     (but no class name), or if a package name was matched but the class could not be found
     *     within the package (e.g., {@code "myExistingPackage.myNonExistentClass"}).
     */
    private static Pair<ClassName, String> matchPackageAndClassNameWithinExpression(
            String expression, Resolver resolver, TreePath path)
            throws FlowExpressionParseException {
        Pair<PackageSymbol, String> packageSymbolAndRemainingString =
                matchPackageNameWithinExpression(expression, resolver, path);

        if (packageSymbolAndRemainingString == null) {
            return null;
        }

        PackageSymbol packageSymbol = packageSymbolAndRemainingString.first;
        String packageRemainingString = packageSymbolAndRemainingString.second;

        Pair<String, String> select = parseMemberSelect(packageRemainingString);
        String classNameString;
        String remainingString;
        if (select != null) {
            classNameString = select.first;
            remainingString = select.second;
        } else {
            classNameString = packageRemainingString;
            remainingString = null;
        }
        ClassSymbol classSymbol;
        try {
            classSymbol = resolver.findClassInPackage(classNameString, packageSymbol, path);
        } catch (Throwable t) {
            if (t.getMessage() == null) {
                throw new Error("no detail message in " + t.getClass(), t);
            }
            throw constructParserException(
                    expression,
                    t.getMessage()
                            + " while looking up class "
                            + classNameString
                            + " in package "
                            + packageSymbol);
        }
        if (classSymbol == null) {
            throw constructParserException(
                    expression,
                    "classSymbol==null when looking up class "
                            + classNameString
                            + " in package "
                            + packageSymbol);
        }
        TypeMirror classType = ElementUtils.getType(classSymbol);
        if (classType == null) {
            throw constructParserException(
                    expression, "classType==null when looking for class symbol " + classSymbol);
        }
        return Pair.of(new ClassName(classType), remainingString);
    }

    /**
     * Greedily matches the longest substring of {@code expression} to a package (starting from the
     * beginning of the string).
     *
     * @param expression the expression string that may start with a package name
     * @param resolver the {@code Resolver} for the current processing environment
     * @param path the tree path to the local scope
     * @return {@code null} if the expression string did not start with a package name; otherwise a
     *     {@code Pair} containing the {@code PackageSymbol} for the matched package, and the
     *     remaining substring of the expression (always non-null) after the package name
     * @throws FlowExpressionParseException if the entire expression string matches a package name
     */
    private static Pair<PackageSymbol, String> matchPackageNameWithinExpression(
            String expression, Resolver resolver, TreePath path)
            throws FlowExpressionParseException {
        Pair<String, String> select = parseMemberSelect(expression);

        // To proceed past this point, at the minimum the expression must be composed of
        // packageName.className .  Do not remove the call to matches(), otherwise the dotMatcher
        // groups will not be filled in.
        if (select == null) {
            return null;
        }

        String packageName = select.first;
        String remainingString = select.second;
        String remainingStringIfPackageMatched = remainingString;

        PackageSymbol result = null; // the result of this method call

        while (true) {
            // At this point, packageName is one component longer than result, and that extra
            // component appears in remainingString but not in remainingStringIfPackageMatched.  In
            // other words, result and remainingStringIfPackageMatched are consistent, and
            // packageName and remainingString are consistent.  Try to set result to account for the
            // extra component in packageName.
            PackageSymbol longerResult;
            try {
                longerResult = resolver.findPackage(packageName, path);
            } catch (Throwable t) {
                if (t.getMessage() == null) {
                    throw new Error("no detail message in " + t.getClass(), t);
                }
                throw constructParserException(
                        expression, t.getMessage() + " while looking up package " + packageName);
            }
            if (longerResult == null) {
                break;
            }
            result = longerResult;
            remainingString = remainingStringIfPackageMatched;
            select = parseMemberSelect(remainingString);
            if (select != null) {
                packageName += "." + select.first;
                remainingStringIfPackageMatched = select.second;
            } else {
                // There are no dots in remainingString, so we are done.
                // Fail if the whole string represents a package, otherwise return.
                PackageSymbol wholeExpressionAsPackage;
                try {
                    wholeExpressionAsPackage = resolver.findPackage(expression, path);
                } catch (Throwable t) {
                    if (t.getMessage() == null) {
                        throw new Error("no detail message in " + t.getClass(), t);
                    }
                    throw constructParserException(
                            expression, t.getMessage() + " while looking up package " + expression);
                }
                if (wholeExpressionAsPackage != null) {
                    // The entire expression matches a package name.
                    throw constructParserException(
                            expression, "a flow expression string cannot be just a package name");
                }
                break;
            }
        }

        if (result == null) {
            return null;
        }

        // an exception would have been thrown above if the entire expression is a package name
        assert remainingString != null;

        return Pair.of(result, remainingString);
    }

    /**
     * A very simple parser for parameter lists, i.e. strings of the form {@code a, b, c} for some
     * expressions {@code a}, {@code b} and {@code c}.
     */
    private static class ParameterListParser {

        /**
         * Parse a parameter list and return the parameters as a list (or throw a {@link
         * FlowExpressionParseException}).
         */
        private static List<Receiver> parseParameterList(
                String parameterString,
                boolean allowEmptyList,
                FlowExpressionContext context,
                TreePath path)
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
                    if (inString) {
                        throw constructParserException(parameterString, "unterminated string");
                    } else if (callLevel > 0) {
                        throw constructParserException(
                                parameterString,
                                "unterminated method invocation, callLevel==" + callLevel);
                    } else {
                        finishParam(parameterString, allowEmptyList, context, path, result, idx);
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
                                finishParam(
                                        parameterString,
                                        allowEmptyList,
                                        context,
                                        path,
                                        result,
                                        idx - 1);
                                // parse remaining parameters
                                List<Receiver> rest =
                                        parseParameterList(
                                                parameterString.substring(idx),
                                                false,
                                                context,
                                                path);
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
                                throw constructParserException(parameterString, "callLevel==0");
                            } else {
                                callLevel--;
                            }
                        }
                        break;
                    case '\\':
                        idx++;
                        break;
                    default:
                        // stay in same state and consume the character
                        break;
                }
            }
        }

        private static void finishParam(
                String parameterString,
                boolean allowEmptyList,
                FlowExpressionContext context,
                TreePath path,
                ArrayList<Receiver> result,
                int idx)
                throws FlowExpressionParseException {
            if (idx == 0) {
                if (allowEmptyList) {
                    return;
                } else {
                    throw constructParserException(parameterString, "empty parameter list; idx==0");
                }
            } else {
                result.add(parseHelper(parameterString.substring(0, idx), context, path));
            }
        }
    }

    /**
     * @return a list of 1-based indices of all formal parameters that occur in {@code s}. Each
     *     formal parameter occurs in s as a string like "#1" or "#4". This routine does not do
     *     proper parsing; for instance, if "#2" appears within a string in s, then 2 would still be
     *     in the result list.
     */
    public static List<Integer> parameterIndices(String s) {
        List<Integer> result = new ArrayList<>();
        Matcher matcher = UNANCHORED_PARAMETER_PATTERN.matcher(s);
        while (matcher.find()) {
            int idx = Integer.parseInt(matcher.group(1));
            result.add(idx);
        }
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Contexts
    ///

    /**
     * Context used to parse a flow expression. When parsing flow expression E in annotation
     * {@code @A(E)}, the context is the program element that is annotated by {@code @A(E)}.
     */
    public static class FlowExpressionContext {
        public final Receiver receiver;
        public final List<Receiver> arguments;
        public final Receiver outerReceiver;
        public final BaseContext checkerContext;
        /**
         * Whether or not the FlowExpressionParser is parsing the "member" part of a member select.
         */
        public final boolean parsingMember;
        /** Whether the TreePath should be used to find identifiers. Defaults to true. */
        public final boolean useLocalScope;

        /**
         * Creates context for parsing a flow expression.
         *
         * @param receiver used to replace "this" in a flow expression and used to resolve
         *     identifiers in the flow expression with an implicit "this"
         * @param arguments used to replace parameter references, e.g. #1, in flow expressions, null
         *     if no arguments
         * @param checkerContext used to create {@link
         *     org.checkerframework.dataflow.analysis.FlowExpressions.Receiver}s
         */
        public FlowExpressionContext(
                Receiver receiver, List<Receiver> arguments, BaseContext checkerContext) {
            this(receiver, receiver, arguments, checkerContext);
        }

        private FlowExpressionContext(
                Receiver receiver,
                Receiver outerReceiver,
                List<Receiver> arguments,
                BaseContext checkerContext) {
            this(receiver, outerReceiver, arguments, checkerContext, false, true);
        }

        private FlowExpressionContext(
                Receiver receiver,
                Receiver outerReceiver,
                List<Receiver> arguments,
                BaseContext checkerContext,
                boolean parsingMember,
                boolean useLocalScope) {
            assert checkerContext != null;
            this.receiver = receiver;
            this.arguments = arguments;
            this.outerReceiver = outerReceiver;
            this.checkerContext = checkerContext;
            this.parsingMember = parsingMember;
            this.useLocalScope = useLocalScope;
        }

        /**
         * Creates a {@link FlowExpressionContext} for the method declared in {@code
         * methodDeclaration}.
         *
         * @param methodDeclaration used translate parameter numbers in a flow expression to formal
         *     parameters of the method
         * @param enclosingTree used to look up fields and as type of "this" in flow expressions
         * @param checkerContext use to build FlowExpressions.Receiver
         * @return context created of {@code methodDeclaration}
         */
        public static FlowExpressionContext buildContextForMethodDeclaration(
                MethodTree methodDeclaration, Tree enclosingTree, BaseContext checkerContext) {
            return buildContextForMethodDeclaration(
                    methodDeclaration, TreeUtils.typeOf(enclosingTree), checkerContext);
        }

        /**
         * Creates a {@link FlowExpressionContext} for the method declared in {@code
         * methodDeclaration}.
         *
         * @param methodDeclaration used translate parameter numbers in a flow expression to formal
         *     parameters of the method
         * @param enclosingType used to look up fields and as type of "this" in flow expressions
         * @param checkerContext use to build FlowExpressions.Receiver
         * @return context created of {@code methodDeclaration}
         */
        public static FlowExpressionContext buildContextForMethodDeclaration(
                MethodTree methodDeclaration,
                TypeMirror enclosingType,
                BaseContext checkerContext) {

            Node receiver;
            if (methodDeclaration.getModifiers().getFlags().contains(Modifier.STATIC)) {
                Element classElt =
                        ElementUtils.enclosingClass(
                                TreeUtils.elementFromDeclaration(methodDeclaration));
                receiver = new ClassNameNode(enclosingType, classElt);
            } else {
                receiver = new ImplicitThisLiteralNode(enclosingType);
            }
            Receiver internalReceiver =
                    FlowExpressions.internalReprOf(
                            checkerContext.getAnnotationProvider(), receiver);
            List<Receiver> internalArguments = new ArrayList<>();
            for (VariableTree arg : methodDeclaration.getParameters()) {
                internalArguments.add(
                        FlowExpressions.internalReprOf(
                                checkerContext.getAnnotationProvider(),
                                new LocalVariableNode(arg, receiver)));
            }
            FlowExpressionContext flowExprContext =
                    new FlowExpressionContext(internalReceiver, internalArguments, checkerContext);
            return flowExprContext;
        }

        public static FlowExpressionContext buildContextForLambda(
                LambdaExpressionTree lambdaTree, TreePath path, BaseContext checkerContext) {
            TypeMirror enclosingType = TreeUtils.typeOf(TreeUtils.enclosingClass(path));
            Node receiver = new ImplicitThisLiteralNode(enclosingType);
            Receiver internalReceiver =
                    FlowExpressions.internalReprOf(
                            checkerContext.getAnnotationProvider(), receiver);
            List<Receiver> internalArguments = new ArrayList<>();
            for (VariableTree arg : lambdaTree.getParameters()) {
                internalArguments.add(
                        FlowExpressions.internalReprOf(
                                checkerContext.getAnnotationProvider(),
                                new LocalVariableNode(arg, receiver)));
            }
            FlowExpressionContext flowExprContext =
                    new FlowExpressionContext(internalReceiver, internalArguments, checkerContext);
            return flowExprContext;
        }

        /**
         * Creates a {@link FlowExpressionContext} for the method declared in {@code
         * methodDeclaration}.
         *
         * @param methodDeclaration used translate parameter numbers in a flow expression to formal
         *     parameters of the method
         * @param currentPath to find the enclosing class, which is used to look up fields and as
         *     type of "this" in flow expressions
         * @param checkerContext use to build FlowExpressions.Receiver
         * @return context created of {@code methodDeclaration}
         */
        public static FlowExpressionContext buildContextForMethodDeclaration(
                MethodTree methodDeclaration, TreePath currentPath, BaseContext checkerContext) {
            Tree classTree = TreeUtils.enclosingClass(currentPath);
            return buildContextForMethodDeclaration(methodDeclaration, classTree, checkerContext);
        }

        /**
         * @return a {@link FlowExpressionContext} for the class {@code classTree} as seen at the
         *     class declaration.
         */
        public static FlowExpressionContext buildContextForClassDeclaration(
                ClassTree classTree, BaseContext checkerContext) {
            Node receiver = new ImplicitThisLiteralNode(TreeUtils.typeOf(classTree));

            Receiver internalReceiver =
                    FlowExpressions.internalReprOf(
                            checkerContext.getAnnotationProvider(), receiver);
            List<Receiver> internalArguments = new ArrayList<>();
            FlowExpressionContext flowExprContext =
                    new FlowExpressionContext(internalReceiver, internalArguments, checkerContext);
            return flowExprContext;
        }

        /**
         * @return a {@link FlowExpressionContext} for the method {@code methodInvocation}
         *     (represented as a {@link Node} as seen at the method use (i.e., at a method call
         *     site).
         */
        public static FlowExpressionContext buildContextForMethodUse(
                MethodInvocationNode methodInvocation, BaseContext checkerContext) {
            Node receiver = methodInvocation.getTarget().getReceiver();
            Receiver internalReceiver =
                    FlowExpressions.internalReprOf(
                            checkerContext.getAnnotationProvider(), receiver);
            List<Receiver> internalArguments = new ArrayList<>();
            for (Node arg : methodInvocation.getArguments()) {
                internalArguments.add(
                        FlowExpressions.internalReprOf(
                                checkerContext.getAnnotationProvider(), arg));
            }
            FlowExpressionContext flowExprContext =
                    new FlowExpressionContext(internalReceiver, internalArguments, checkerContext);
            return flowExprContext;
        }

        /**
         * @return a {@link FlowExpressionContext} for the method {@code methodInvocation}
         *     (represented as a {@link MethodInvocationTree} as seen at the method use (i.e., at a
         *     method call site).
         */
        public static FlowExpressionContext buildContextForMethodUse(
                MethodInvocationTree methodInvocation, BaseContext checkerContext) {
            ExpressionTree receiverTree = TreeUtils.getReceiverTree(methodInvocation);
            FlowExpressions.Receiver receiver;
            if (receiverTree == null) {
                receiver =
                        FlowExpressions.internalReprOfImplicitReceiver(
                                TreeUtils.elementFromUse(methodInvocation));
            } else {
                receiver =
                        FlowExpressions.internalReprOf(
                                checkerContext.getAnnotationProvider(), receiverTree);
            }

            List<? extends ExpressionTree> args = methodInvocation.getArguments();
            List<FlowExpressions.Receiver> argReceivers = new ArrayList<>(args.size());
            for (ExpressionTree argTree : args) {
                argReceivers.add(
                        FlowExpressions.internalReprOf(
                                checkerContext.getAnnotationProvider(), argTree));
            }

            return new FlowExpressionContext(receiver, argReceivers, checkerContext);
        }

        /**
         * @return a {@link FlowExpressionContext} for the constructor {@code n} (represented as a
         *     {@link Node} as seen at the method use (i.e., at a method call site).
         */
        public static FlowExpressionContext buildContextForNewClassUse(
                ObjectCreationNode n, BaseContext checkerContext) {

            // This returns an FlowExpressions.Unknown with the type set to the class in which the
            // constructor is declared
            Receiver internalReceiver =
                    FlowExpressions.internalReprOf(checkerContext.getAnnotationProvider(), n);

            List<Receiver> internalArguments = new ArrayList<>();
            for (Node arg : n.getArguments()) {
                internalArguments.add(
                        FlowExpressions.internalReprOf(
                                checkerContext.getAnnotationProvider(), arg));
            }

            FlowExpressionContext flowExprContext =
                    new FlowExpressionContext(internalReceiver, internalArguments, checkerContext);

            return flowExprContext;
        }

        /**
         * Returns a copy of the context that differs in that it has a different receiver and
         * parsingMember is set to true. The outer receiver remains unchanged.
         */
        public FlowExpressionContext copyChangeToParsingMemberOfReceiver(Receiver receiver) {
            return new FlowExpressionContext(
                    receiver,
                    outerReceiver,
                    arguments,
                    checkerContext,
                    /*parsingMember=*/ true,
                    useLocalScope);
        }

        /**
         * Returns a copy of the context that differs in that it uses the outer receiver as main
         * receiver (and also retains it as the outer receiver), and parsingMember is set to false.
         */
        public FlowExpressionContext copyAndUseOuterReceiver() {
            return new FlowExpressionContext(
                    outerReceiver, // NOTE different than in this object
                    outerReceiver,
                    arguments,
                    checkerContext,
                    /*parsingMember=*/ false,
                    useLocalScope);
        }

        /**
         * Returns a copy of the context that differs in that useLocalScope is set to the given
         * value.
         */
        public FlowExpressionContext copyAndSetUseLocalScope(boolean useLocalScope) {
            return new FlowExpressionContext(
                    receiver,
                    outerReceiver,
                    arguments,
                    checkerContext,
                    parsingMember,
                    useLocalScope);
        }
    }

    /**
     * Returns the type of the inner most enclosing class.Type.noType is returned if no enclosing
     * class is found. This is in contrast to {@link DeclaredType#getEnclosingType()} which returns
     * the type of the inner most instance. If the inner most enclosing class is static this method
     * will return the type of that class where as {@link DeclaredType#getEnclosingType()} will
     * return the type of the inner most enclosing class that is not static.
     *
     * @param type a DeclaredType
     * @return the type of the innermost enclosing class or Type.noType
     */
    private static TypeMirror getTypeOfEnclosingClass(DeclaredType type) {
        if (type instanceof ClassType) {
            // enclClass() needs to be called on tsym.owner,
            // otherwise it simply returns tsym.
            Symbol sym = ((ClassType) type).tsym.owner;

            if (sym == null) {
                return Type.noType;
            }

            ClassSymbol cs = sym.enclClass();

            if (cs == null) {
                return Type.noType;
            }

            return cs.asType();
        } else {
            return type.getEnclosingType();
        }
    }

    public static Receiver internalReprOfVariable(AnnotatedTypeFactory provider, VariableTree tree)
            throws FlowExpressionParseException {
        Element elt = TreeUtils.elementFromDeclaration(tree);

        if (elt.getKind() == ElementKind.LOCAL_VARIABLE
                || elt.getKind() == ElementKind.RESOURCE_VARIABLE
                || elt.getKind() == ElementKind.EXCEPTION_PARAMETER
                || elt.getKind() == ElementKind.PARAMETER) {
            return new LocalVariable(elt);
        }
        Receiver receiverF = FlowExpressions.internalReprOfImplicitReceiver(elt);
        FlowExpressionParseUtil.FlowExpressionContext context =
                new FlowExpressionParseUtil.FlowExpressionContext(
                        receiverF, null, provider.getContext());
        return FlowExpressionParseUtil.parse(
                tree.getName().toString(), context, provider.getPath(tree), false);
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Exceptions
    ///

    /**
     * An exception that indicates a parse error. Call {@link #getResult} to obtain a {@link Result}
     * that can be used for error reporting.
     */
    public static class FlowExpressionParseException extends Exception {
        private static final long serialVersionUID = 2L;
        private @CompilerMessageKey String errorKey;
        public final Object[] args;

        public FlowExpressionParseException(@CompilerMessageKey String errorKey, Object... args) {
            this(null, errorKey, args);
        }

        public FlowExpressionParseException(
                Throwable cause, @CompilerMessageKey String errorKey, Object... args) {
            super(cause);
            this.errorKey = errorKey;
            this.args = args;
        }

        @Override
        public String getMessage() {
            return errorKey + " " + Arrays.toString(args);
        }

        /** Return a Result that can be used for error reporting. */
        public Result getResult() {
            return Result.failure(errorKey, args);
        }

        public boolean isFlowParseError() {
            return errorKey.endsWith("flowexpr.parse.error");
        }
    }

    /**
     * Returns a {@link FlowExpressionParseException} for the expression {@code expr} with
     * explanation {@code explanation}.
     */
    private static FlowExpressionParseException constructParserException(
            String expr, String explanation) {
        if (expr == null) {
            throw new Error("Must have an expression.");
        }
        if (explanation == null) {
            throw new Error("Must have an explanation.");
        }
        return new FlowExpressionParseException(
                (Throwable) null,
                "flowexpr.parse.error",
                "Invalid '" + expr + "' because " + explanation);
    }
}
