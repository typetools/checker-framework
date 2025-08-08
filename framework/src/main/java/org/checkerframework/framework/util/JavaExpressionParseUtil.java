package org.checkerframework.framework.util;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.dataflow.expression.ArrayAccess;
import org.checkerframework.dataflow.expression.ArrayCreation;
import org.checkerframework.dataflow.expression.BinaryOperation;
import org.checkerframework.dataflow.expression.ClassName;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.FormalParameter;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.MethodCall;
import org.checkerframework.dataflow.expression.SuperReference;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.dataflow.expression.UnaryOperation;
import org.checkerframework.dataflow.expression.ValueLiteral;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.framework.util.dependenttypes.DependentTypesError;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Resolver;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.javacutil.trees.TreeBuilder;
import org.plumelib.util.CollectionsPlume;
import org.plumelib.util.StringsPlume;

/**
 * Helper methods to parse a string that represents a restricted Java expression.
 *
 * @checker_framework.manual #java-expressions-as-arguments Writing Java expressions as annotation
 *     arguments
 * @checker_framework.manual #dependent-types Annotations whose argument is a Java expression
 *     (dependent type annotations)
 */
public class JavaExpressionParseUtil {

  /** Regular expression for a formal parameter use. */
  protected static final String PARAMETER_REGEX = "#([1-9][0-9]*)";

  /**
   * Anchored pattern for a formal parameter use; matches a string that is exactly a formal
   * parameter use.
   */
  protected static final @Regex(1) Pattern ANCHORED_PARAMETER_PATTERN =
      Pattern.compile("^" + PARAMETER_REGEX + "$");

  /**
   * Parsable replacement for formal parameter references. It is parsable because it is a Java
   * identifier.
   */
  private static final String PARAMETER_PREFIX = "_param_";

  /** The length of {@link #PARAMETER_PREFIX}. */
  private static final int PARAMETER_PREFIX_LENGTH = PARAMETER_PREFIX.length();

  /** A pattern that matches the start of a formal parameter in "#2" syntax. */
  private static final Pattern FORMAL_PARAMETER = Pattern.compile("#(\\d)");

  /** The replacement for a formal parameter in "#2" syntax. */
  private static final String PARAMETER_REPLACEMENT = PARAMETER_PREFIX + "$1";

  private static final Pattern ERROR_MARKER = Pattern.compile("\\[error for expression:");

  /** Binary operations that return {@code boolean}. */
  private static final Set<Tree.Kind> COMPARISON_OPERATORS =
      EnumSet.of(
          Tree.Kind.EQUAL_TO,
          Tree.Kind.NOT_EQUAL_TO,
          Tree.Kind.LESS_THAN,
          Tree.Kind.LESS_THAN_EQUAL,
          Tree.Kind.GREATER_THAN,
          Tree.Kind.GREATER_THAN_EQUAL);

  /**
   * Parses a string to a {@link JavaExpression}.
   *
   * <p>For most uses, clients should call one of the static methods in {@link
   * StringToJavaExpression} rather than calling this method directly.
   *
   * @param expression the string expression to parse
   * @param enclosingType type of the class that encloses the JavaExpression
   * @param thisReference the JavaExpression to which to parse "this", or null if "this" should not
   *     appear in the expression
   * @param parameters list of JavaExpressions to which to parse formal parameter references such as
   *     "#2", or null if formal parameter references should not appear in the expression
   * @param localVarPath if non-null, the expression is parsed as if it were written at this
   *     location; affects only parsing of local variables
   * @param pathToCompilationUnit required to use the underlying Javac API
   * @param env the processing environment
   * @return {@code expression} as a {@code JavaExpression}
   * @throws JavaExpressionParseException if the string cannot be parsed
   */
  public static JavaExpression parse(
      String expression,
      TypeMirror enclosingType,
      @Nullable ThisReference thisReference,
      @Nullable List<FormalParameter> parameters,
      @Nullable TreePath localVarPath,
      TreePath pathToCompilationUnit,
      ProcessingEnvironment env)
      throws JavaExpressionParseException {

    String expressionWithParameterNames =
        StringsPlume.replaceAll(expression, FORMAL_PARAMETER, PARAMETER_REPLACEMENT);
    // bail out early on clearly non-Java inputs
    if (ERROR_MARKER.matcher(expression).find()) {
      throw constructJavaExpressionParseError(expression, "the expression did not parse");
    }
    if (expressionWithParameterNames.indexOf('#') >= 0) {
      throw constructJavaExpressionParseError(expression, "the expression did not parse");
    }
    ExpressionTree exprTree;
    try {
      exprTree = JavacParseUtil.parseExpression(expressionWithParameterNames);
    } catch (RuntimeException e) {
      String extra = ".";
      if (!e.getMessage().isEmpty()) {
        String message = e.getMessage();
        extra = ". Error message: " + message;
      }
      throw constructJavaExpressionParseError(expression, "the expression did not parse" + extra);
    }

    JavaExpression result =
        ExpressionTreeToJavaExpressionVisitor.convert(
            exprTree,
            enclosingType,
            thisReference,
            parameters,
            localVarPath,
            pathToCompilationUnit,
            env);

    if (result instanceof ClassName && !expression.endsWith(".class")) {
      throw constructJavaExpressionParseError(
          expression,
          String.format(
              "a class name cannot terminate a Java expression string, where result=%s [%s]",
              result, result.getClass()));
    }
    return result;
  }

  /**
   * A visitor class that converts a javac {@link ExpressionTree} to a {@link JavaExpression}. This
   * class does not viewpoint-adapt the expression.
   */
  private static class ExpressionTreeToJavaExpressionVisitor
      extends SimpleTreeVisitor<JavaExpression, Void> {

    /** The processing environment. */
    private final ProcessingEnvironment env;

    /** The type utilities. */
    private final Types types;

    /** The resolver. Computed from the environment, but lazily initialized. */
    private @MonotonicNonNull Resolver resolver = null;

    /** The java.lang.String type. */
    private final TypeMirror stringTypeMirror;

    /** The primitive boolean type. */
    private final TypeMirror booleanTypeMirror;

    /**
     * The underlying javac API used to convert from Strings to Elements requires a tree path even
     * when the information could be deduced from elements alone. So use the path to the current
     * CompilationUnit.
     */
    private final TreePath pathToCompilationUnit;

    /** If non-null, the expression is parsed as if it were written at this location. */
    private final @Nullable TreePath localVarPath;

    /** The enclosing type. Used to look up unqualified method, field, and class names. */
    private final TypeMirror enclosingType;

    /**
     * The expression to use for "this". If {@code null}, a parse error will be thrown if "this"
     * appears in the expression.
     */
    private final @Nullable ThisReference thisReference;

    /**
     * For each formal parameter, the expression to which to parse it. For example, the second
     * (index 1) element of the list is what "#2" parses to. If this field is {@code null}, a parse
     * error will be thrown if "#2" appears in the expression.
     */
    private final @Nullable List<FormalParameter> parameters;

    /**
     * Create a new ExpressionTreeToJavaExpressionVisitor.
     *
     * @param enclosingType type of the class that encloses the JavaExpression
     * @param thisReference a JavaExpression to which to parse "this", or null if "this" should not
     *     appear in the expression
     * @param parameters list of JavaExpressions to which to parse a formal parameter reference such
     *     as "#2", or null if parameters should not appear in the expression
     * @param localVarPath if non-null, the expression is parsed as if it were written at this
     *     location
     * @param pathToCompilationUnit required to use the underlying Javac API
     * @param env the processing environment
     */
    private ExpressionTreeToJavaExpressionVisitor(
        TypeMirror enclosingType,
        @Nullable ThisReference thisReference,
        @Nullable List<FormalParameter> parameters,
        @Nullable TreePath localVarPath,
        TreePath pathToCompilationUnit,
        ProcessingEnvironment env) {
      this.pathToCompilationUnit = pathToCompilationUnit;
      this.localVarPath = localVarPath;
      this.env = env;
      this.types = env.getTypeUtils();
      this.stringTypeMirror = ElementUtils.getTypeElement(env, String.class).asType();
      this.booleanTypeMirror = types.getPrimitiveType(TypeKind.BOOLEAN);
      this.enclosingType = enclosingType;
      this.thisReference = thisReference;
      this.parameters = parameters;
    }

    /**
     * Converts a Javac {@link ExpressionTree} to a {@link JavaExpression}.
     *
     * @param exprTree the Javac {@link ExpressionTree} to convert
     * @param enclosingType type of the class that encloses the JavaExpression
     * @param thisReference a JavaExpression to which to parse "this", or null if "this" should not
     *     appear in the expression
     * @param parameters list of JavaExpressions to which to parse parameters, or null if parameters
     *     should not appear in the expression
     * @param localVarPath if non-null, the expression is parsed as if it were written at this
     *     location
     * @param pathToCompilationUnit required to use the underlying Javac API
     * @param env the processing environment
     * @return {@code exprTree} as a {@code JavaExpression}
     * @throws JavaExpressionParseException if {@code exprTree} cannot be converted to a {@code
     *     JavaExpression}
     */
    public static JavaExpression convert(
        ExpressionTree exprTree,
        TypeMirror enclosingType,
        @Nullable ThisReference thisReference,
        @Nullable List<FormalParameter> parameters,
        @Nullable TreePath localVarPath,
        TreePath pathToCompilationUnit,
        ProcessingEnvironment env)
        throws JavaExpressionParseException {
      try {
        return exprTree.accept(
            new ExpressionTreeToJavaExpressionVisitor(
                enclosingType, thisReference, parameters, localVarPath, pathToCompilationUnit, env),
            null);
      } catch (ParseRuntimeException e) {
        // Convert unchecked to checked exception. Visitor methods can't throw checked
        // exceptions. They override the methods in the superclass, and a checked exception
        // would change the method signature.
        throw e.getCheckedException();
      }
    }

    /**
     * Initializes the {@code resolver} field if necessary. Does nothing on invocations after the
     * first.
     */
    @EnsuresNonNull("resolver")
    private void setResolverField() {
      if (resolver == null) {
        resolver = new Resolver(env);
      }
    }

    /** If the expression is not supported, throw a {@link ParseRuntimeException} by default. */
    @Override
    public JavaExpression defaultAction(com.sun.source.tree.Tree treeNode, Void unused) {
      throw new ParseRuntimeException(
          constructJavaExpressionParseError(
              treeNode.toString(), treeNode.getClass() + " is not a supported expression"));
    }

    @Override
    public JavaExpression visitLiteral(LiteralTree exprTree, Void unused) {
      Object value = exprTree.getValue();
      TypeMirror type;

      if (value == null) {
        type = types.getNullType();
      } else if (value instanceof Integer) {
        type = types.getPrimitiveType(TypeKind.INT);
      } else if (value instanceof Boolean) {
        type = types.getPrimitiveType(TypeKind.BOOLEAN);
      } else if (value instanceof Long) {
        type = types.getPrimitiveType(TypeKind.LONG);
      } else if (value instanceof Double) {
        type = types.getPrimitiveType(TypeKind.DOUBLE);
      } else if (value instanceof Float) {
        type = types.getPrimitiveType(TypeKind.FLOAT);
      } else if (value instanceof Character) {
        type = types.getPrimitiveType(TypeKind.CHAR);
      } else if (value instanceof String) {
        type = this.stringTypeMirror;
      } else {
        throw new ParseRuntimeException(
            constructJavaExpressionParseError(
                value.toString(), "Unsupported literal type: " + value.getClass()));
      }

      return new ValueLiteral(type, value);
    }

    @Override
    public JavaExpression visitParenthesized(ParenthesizedTree exprTree, Void unused) {
      // Handles expressions inside parentheses
      return exprTree.getExpression().accept(this, null);
    }

    @Override
    public JavaExpression visitArrayAccess(ArrayAccessTree exprTree, Void unused) {
      // Handles array[index] expressions
      JavaExpression array = exprTree.getExpression().accept(this, null);
      TypeMirror arrayType = array.getType();
      if (arrayType.getKind() != TypeKind.ARRAY) {
        throw new ParseRuntimeException(
            constructJavaExpressionParseError(
                exprTree.toString(),
                String.format(
                    "expected an array, found %s of type %s [%s]",
                    array, arrayType, arrayType.getKind())));
      }
      TypeMirror componentType = ((ArrayType) arrayType).getComponentType();

      JavaExpression index = exprTree.getIndex().accept(this, null);

      return new ArrayAccess(componentType, array, index);
    }

    // `id` is an identifier with no dots in its name.
    @Override
    public JavaExpression visitIdentifier(IdentifierTree id, Void unused) {
      String s = id.getName().toString();
      setResolverField();
      // this and super logic
      if (s.equals("this") || s.equals("super")) {
        if (thisReference == null) {
          throw new ParseRuntimeException(
              constructJavaExpressionParseError(s, "\"" + s + "\" isn't allowed here"));
        }
        if (s.equals("this")) {
          return thisReference;
        } else {
          // super literal
          TypeMirror superclass = TypesUtils.getSuperclass(enclosingType, types);
          if (superclass == null) {
            throw new ParseRuntimeException(
                constructJavaExpressionParseError("super", enclosingType + " has no superclass"));
          }
          return new SuperReference(superclass);
        }
      }

      // Formal parameter, using "#2" syntax.
      JavaExpression parameter = getParameterJavaExpression(s);
      if (parameter != null) {
        // A parameter is a local variable, but it can be referenced outside of local scope
        // (at the method scope) using the special #NN syntax.
        return parameter;
      }

      // Local variable or parameter.
      if (localVarPath != null) {
        // Attempt to match a local variable within the scope of the
        // given path before attempting to match a field.
        VariableElement varElem = resolver.findLocalVariableOrParameter(s, localVarPath);
        if (varElem != null) {
          return new LocalVariable(varElem);
        }
      }

      // Field access
      JavaExpression fieldAccessReceiver;
      if (thisReference != null) {
        fieldAccessReceiver = thisReference;
      } else {
        fieldAccessReceiver = new ClassName(enclosingType);
      }
      FieldAccess fieldAccess = getIdentifierAsFieldAccess(fieldAccessReceiver, s);
      if (fieldAccess != null) {
        return fieldAccess;
      }

      // Class name
      if (localVarPath != null) {
        Element classElem = resolver.findClass(s, localVarPath);
        TypeMirror classType = ElementUtils.getType(classElem);
        if (classType != null) {
          return new ClassName(classType);
        }
      }
      ClassName classType = getIdentifierAsUnqualifiedClassName(s);
      if (classType != null) {
        return classType;
      }

      // Err if a formal parameter name is used, instead of the "#2" syntax.
      if (parameters != null) {
        for (int i = 0; i < parameters.size(); i++) {
          Element varElt = parameters.get(i).getElement();
          if (varElt.getSimpleName().contentEquals(s)) {
            throw new ParseRuntimeException(
                constructJavaExpressionParseError(
                    s, String.format(DependentTypesError.FORMAL_PARAM_NAME_STRING, i + 1, s)));
          }
        }
      }

      throw new ParseRuntimeException(constructJavaExpressionParseError(s, "identifier not found"));
    }

    /**
     * If {@code s} a parameter expressed using the {@code #NN} syntax, then returns a
     * JavaExpression for the given parameter; that is, returns an element of {@code parameters}.
     * Otherwise, returns {@code null}.
     *
     * @param s a String that starts with PARAMETER_PREFIX
     * @return the JavaExpression for the given parameter or {@code null} if {@code s} is not a
     *     parameter
     */
    private @Nullable JavaExpression getParameterJavaExpression(String s) {
      if (!s.startsWith(PARAMETER_PREFIX)) {
        return null;
      }
      if (parameters == null) {
        throw new ParseRuntimeException(
            constructJavaExpressionParseError(s, "no parameters found"));
      }
      int idx = Integer.parseInt(s.substring(PARAMETER_PREFIX_LENGTH));

      if (idx == 0) {
        throw new ParseRuntimeException(
            constructJavaExpressionParseError(
                "#0", "Use \"this\" for the receiver or \"#1\" for the first formal parameter"));
      }
      if (idx > parameters.size()) {
        throw new ParseRuntimeException(
            new JavaExpressionParseException(
                "flowexpr.parse.index.too.big", Integer.toString(idx)));
      }
      return parameters.get(idx - 1);
    }

    /**
     * If {@code identifier} is the simple class name of any inner class of {@code type}, return the
     * {@link ClassName} for the inner class. If not, return null.
     *
     * @param type type to search for {@code identifier}
     * @param identifier possible simple class name
     * @return the {@code ClassName} for {@code identifier}, or null if it is not a simple class
     *     name
     */
    protected @Nullable ClassName getIdentifierAsInnerClassName(
        TypeMirror type, String identifier) {
      if (type.getKind() != TypeKind.DECLARED) {
        return null;
      }

      Element outerClass = ((DeclaredType) type).asElement();
      for (Element memberElement : outerClass.getEnclosedElements()) {
        if (!(memberElement.getKind().isClass() || memberElement.getKind().isInterface())) {
          continue;
        }
        if (memberElement.getSimpleName().contentEquals(identifier)) {
          return new ClassName(ElementUtils.getType(memberElement));
        }
      }
      return null;
    }

    /**
     * If {@code identifier} is a class name with that can be referenced using only its simple name
     * within {@code enclosingType}, return the {@link ClassName} for the class. If not, return
     * null.
     *
     * <p>{@code identifier} may be
     *
     * <ol>
     *   <li>the simple name of {@code type}.
     *   <li>the simple name of a class declared in {@code type} or in an enclosing type of {@code
     *       type}.
     *   <li>the simple name of a class in the java.lang package.
     *   <li>the simple name of a class in the unnamed package.
     * </ol>
     *
     * @param identifier possible class name
     * @return the {@code ClassName} for {@code identifier}, or null if it is not a class name
     */
    protected @Nullable ClassName getIdentifierAsUnqualifiedClassName(String identifier) {
      // Is identifier an inner class of enclosingType or of any enclosing class of
      // enclosingType?
      TypeMirror searchType = enclosingType;
      while (searchType.getKind() == TypeKind.DECLARED) {
        DeclaredType searchDeclaredType = (DeclaredType) searchType;
        if (searchDeclaredType.asElement().getSimpleName().contentEquals(identifier)) {
          return new ClassName(searchType);
        }
        ClassName className = getIdentifierAsInnerClassName(searchType, identifier);
        if (className != null) {
          return className;
        }
        searchType = getTypeOfEnclosingClass(searchDeclaredType);
      }

      setResolverField();

      if (enclosingType.getKind() == TypeKind.DECLARED) {
        // Is identifier in the same package as this?
        PackageSymbol packageSymbol =
            (PackageSymbol)
                ElementUtils.enclosingPackage(((DeclaredType) enclosingType).asElement());
        ClassSymbol classSymbol =
            resolver.findClassInPackage(identifier, packageSymbol, pathToCompilationUnit);
        if (classSymbol != null) {
          return new ClassName(classSymbol.asType());
        }
      }
      // Is identifier a simple name for a class in java.lang?
      PackageSymbol packageSymbol = resolver.findPackage("java.lang", pathToCompilationUnit);
      if (packageSymbol == null) {
        throw new BugInCF("Can't find java.lang package.");
      }
      ClassSymbol classSymbol =
          resolver.findClassInPackage(identifier, packageSymbol, pathToCompilationUnit);
      if (classSymbol != null) {
        return new ClassName(classSymbol.asType());
      }

      // Is identifier a class in the unnamed package?
      Element classElem = resolver.findClass(identifier, pathToCompilationUnit);
      if (classElem != null) {
        PackageElement pkg = ElementUtils.enclosingPackage(classElem);
        if (pkg != null && pkg.isUnnamed()) {
          TypeMirror classType = ElementUtils.getType(classElem);
          if (classType != null) {
            return new ClassName(classType);
          }
        }
      }

      return null;
    }

    /**
     * Returns the {@link FieldAccess} expression for the field with name {@code identifier}
     * accessed via {@code receiverExpr}. If no such field exists, then {@code null} is returned.
     *
     * @param receiverExpr the receiver of the field access; the expression used to access the field
     * @param identifier possibly a field name
     * @return a field access, or null if {@code identifier} is not a field that can be accessed via
     *     {@code receiverExpr}
     */
    protected @Nullable FieldAccess getIdentifierAsFieldAccess(
        JavaExpression receiverExpr, String identifier) {
      setResolverField();
      // Find the field element.
      TypeMirror enclosingTypeOfField = receiverExpr.getType();
      VariableElement fieldElem;
      if (identifier.equals("length") && enclosingTypeOfField.getKind() == TypeKind.ARRAY) {
        fieldElem = resolver.findField(identifier, enclosingTypeOfField, pathToCompilationUnit);
        if (fieldElem == null) {
          throw new BugInCF("length field not found for type %s", enclosingTypeOfField);
        }
      } else {
        fieldElem = null;
        // Search for field in each enclosing class.
        while (enclosingTypeOfField.getKind() == TypeKind.DECLARED) {
          fieldElem = resolver.findField(identifier, enclosingTypeOfField, pathToCompilationUnit);
          if (fieldElem != null) {
            break;
          }
          enclosingTypeOfField = getTypeOfEnclosingClass((DeclaredType) enclosingTypeOfField);
        }
        if (fieldElem == null) {
          // field not found.
          return null;
        }
        if (receiverExpr instanceof SuperReference
            && thisReference.getType().getKind() == TypeKind.DECLARED) {
          Element thisFieldElem =
              resolver.findField(identifier, thisReference.getType(), pathToCompilationUnit);
          if (thisFieldElem == null) {
            receiverExpr = thisReference;
          }
        }
      }

      // `fieldElem` is now set.  Construct a FieldAccess expression.

      if (ElementUtils.isStatic(fieldElem)) {
        Element classElem = fieldElem.getEnclosingElement();
        JavaExpression staticClassReceiver = new ClassName(ElementUtils.getType(classElem));
        return new FieldAccess(staticClassReceiver, fieldElem);
      }

      // fieldElem is an instance field.

      if (receiverExpr instanceof ClassName) {
        throw new ParseRuntimeException(
            constructJavaExpressionParseError(
                identifier.toString(),
                "a non-static field "
                    + fieldElem.getSimpleName().toString()
                    + " cannot have a class name "
                    + receiverExpr
                    + " as a receiver."));
      }

      // There are two possibilities, captured by local variable fieldDeclaredInReceiverType:
      //  * true: it's an instance field declared in the type (or supertype) of receiverExpr.
      //  * false: it's an instance field declared in an enclosing type of receiverExpr.

      @SuppressWarnings("interning:not.interned") // Checking for exact object
      boolean fieldDeclaredInReceiverType = enclosingTypeOfField == receiverExpr.getType();
      if (fieldDeclaredInReceiverType) {
        TypeMirror fieldType = ElementUtils.getType(fieldElem);
        return new FieldAccess(receiverExpr, fieldType, fieldElem);
      } else {
        if (!(receiverExpr instanceof ThisReference)) {
          String msg =
              String.format(
                  "%s is declared in an outer type of the type of the receiver expression, %s.",
                  identifier, receiverExpr);
          throw new ParseRuntimeException(constructJavaExpressionParseError(identifier, msg));
        }
        TypeElement receiverTypeElement = TypesUtils.getTypeElement(receiverExpr.getType());
        if (receiverTypeElement == null || ElementUtils.isStatic(receiverTypeElement)) {
          String msg =
              String.format("%s is a non-static field declared in an outer type this.", identifier);
          throw new ParseRuntimeException(constructJavaExpressionParseError(identifier, msg));
        }
        JavaExpression locationOfField = new ThisReference(enclosingTypeOfField);
        return new FieldAccess(locationOfField, fieldElem);
      }
    }

    @Override
    public JavaExpression visitMethodInvocation(MethodInvocationTree invocation, Void unused) {
      setResolverField();
      ExpressionTree methodSelect = invocation.getMethodSelect();

      // Resolve receiver and method name
      JavaExpression receiverExpr;
      String methodName;
      if (methodSelect instanceof MemberSelectTree) {
        // method call like `obj.method()` or `Class.staticMethod()`
        MemberSelectTree memberSelect = (MemberSelectTree) methodSelect;
        receiverExpr = memberSelect.getExpression().accept(this, null);
        methodName = memberSelect.getIdentifier().toString();
      } else if (methodSelect instanceof IdentifierTree) {
        // method call like `method()` (implicit this)
        methodName = ((IdentifierTree) methodSelect).getName().toString();
        if (thisReference != null) {
          receiverExpr = thisReference;
        } else {
          receiverExpr = new ClassName(enclosingType);
        }
      } else {
        throw new ParseRuntimeException(
            constructJavaExpressionParseError(
                invocation.toString(), "unsupported method invocation syntax"));
      }

      // Convert argument expressions
      List<JavaExpression> arguments =
          CollectionsPlume.mapList(
              argument -> argument.accept(this, null), invocation.getArguments());

      // Resolve method
      ExecutableElement methodElement;
      try {
        methodElement =
            getMethodElement(
                methodName, receiverExpr.getType(), pathToCompilationUnit, arguments, resolver);
      } catch (JavaExpressionParseException e) {
        throw new ParseRuntimeException(e);
      }

      // Box arguments if needed.
      for (int i = 0; i < arguments.size(); i++) {
        VariableElement parameter = methodElement.getParameters().get(i);
        TypeMirror parameterType = parameter.asType();
        JavaExpression argument = arguments.get(i);
        TypeMirror argumentType = argument.getType();

        if (TypesUtils.isBoxedPrimitive(parameterType) && TypesUtils.isPrimitive(argumentType)) {
          // Boxing is necessary.
          MethodSymbol valueOfMethod = TreeBuilder.getValueOfMethod(env, parameterType);
          JavaExpression boxedParam =
              new MethodCall(
                  parameterType,
                  valueOfMethod,
                  new ClassName(parameterType),
                  Collections.singletonList(argument));
          arguments.set(i, boxedParam);
        }
      }

      // Build the MethodCall expression object.
      if (ElementUtils.isStatic(methodElement)) {
        Element classElem = methodElement.getEnclosingElement();
        JavaExpression staticClassReceiver = new ClassName(ElementUtils.getType(classElem));
        return new MethodCall(
            ElementUtils.getType(methodElement), methodElement, staticClassReceiver, arguments);
      } else {
        if (receiverExpr instanceof ClassName) {
          throw new ParseRuntimeException(
              constructJavaExpressionParseError(
                  invocation.toString(),
                  "a non-static method call cannot have a class name "
                      + receiverExpr
                      + " as a receiver"));
        }
        TypeMirror methodType =
            TypesUtils.substituteMethodReturnType(methodElement, receiverExpr.getType(), env);
        return new MethodCall(methodType, methodElement, receiverExpr, arguments);
      }
    }

    /**
     * Returns the ExecutableElement for a method, or throws an exception.
     *
     * <p>(This method takes into account autoboxing.)
     *
     * @param methodName the method name
     * @param receiverType the receiver type
     * @param pathToCompilationUnit the path to the compilation unit
     * @param arguments the arguments
     * @param resolver the resolver
     * @return the ExecutableElement for a method, or throws an exception
     * @throws JavaExpressionParseException if the string cannot be parsed as a method name
     */
    private ExecutableElement getMethodElement(
        String methodName,
        TypeMirror receiverType,
        TreePath pathToCompilationUnit,
        List<JavaExpression> arguments,
        Resolver resolver)
        throws JavaExpressionParseException {

      List<TypeMirror> argumentTypes = CollectionsPlume.mapList(JavaExpression::getType, arguments);

      if (receiverType.getKind() == TypeKind.ARRAY) {
        ExecutableElement element =
            resolver.findMethod(methodName, receiverType, pathToCompilationUnit, argumentTypes);
        if (element == null) {
          throw constructJavaExpressionParseError(methodName, "no such method");
        }
        return element;
      }

      // Search for method in each enclosing class.
      while (receiverType.getKind() == TypeKind.DECLARED) {
        ExecutableElement element =
            resolver.findMethod(methodName, receiverType, pathToCompilationUnit, argumentTypes);
        if (element != null) {
          return element;
        }
        receiverType = getTypeOfEnclosingClass((DeclaredType) receiverType);
      }

      // Method not found.
      throw constructJavaExpressionParseError(methodName, "no such method");
    }

    @Override
    public JavaExpression visitMemberSelect(MemberSelectTree exprTree, Void unused) {
      setResolverField();
      // Handle class literal (e.g., SomeClass.class)
      if (exprTree.getIdentifier().contentEquals("class")) {
        Tree selected = exprTree.getExpression();
        TypeMirror result = convertTreeToTypeMirror((JCTree) selected);
        if (result == null) {
          throw new ParseRuntimeException(
              constructJavaExpressionParseError(
                  exprTree.toString(), "it is an unparsable class literal"));
        }
        return new ClassName(result);
      }
      // Handle "this" identifier in a Field access (e.g., Foo.this)
      if (exprTree.getIdentifier().contentEquals("this")) {
        if (thisReference == null) {
          throw new ParseRuntimeException(
              constructJavaExpressionParseError("this", "\"this\" isn't allowed here"));
        }
        return thisReference;
      }

      Tree expr = exprTree.getExpression();
      String name = exprTree.getIdentifier().toString();

      // Check if the expression refers to a fully-qualified class name.
      PackageSymbol packageSymbol = resolver.findPackage(expr.toString(), pathToCompilationUnit);
      if (packageSymbol != null) {
        ClassSymbol classSymbol =
            resolver.findClassInPackage(name, packageSymbol, pathToCompilationUnit);
        if (classSymbol != null) {
          return new ClassName(classSymbol.asType());
        }
        throw new ParseRuntimeException(
            constructJavaExpressionParseError(
                exprTree.toString(),
                "could not find class " + name + " in package " + expr.toString()));
      }

      // Otherwise treat as field access or inner class.
      JavaExpression receiver = expr.accept(this, null);

      // Try as a field.
      FieldAccess fieldAccess = getIdentifierAsFieldAccess(receiver, name);
      if (fieldAccess != null) {
        return fieldAccess;
      }

      // Try as an inner class.
      ClassName innerClass = getIdentifierAsInnerClassName(receiver.getType(), name);
      if (innerClass != null) {
        return innerClass;
      }

      // Nothing matched.
      throw new ParseRuntimeException(
          constructJavaExpressionParseError(
              name, String.format("field or class %s not found in %s", name, receiver)));
    }

    @Override
    public JavaExpression visitNewArray(NewArrayTree exprTree, Void unused) {
      List<JavaExpression> dimensions = new ArrayList<>();
      for (ExpressionTree dim : exprTree.getDimensions()) {
        dimensions.add(dim == null ? null : dim.accept(this, null));
      }
      if (dimensions.isEmpty()) {
        dimensions.add(null);
      }
      List<JavaExpression> initializers = new ArrayList<>();
      if (exprTree.getInitializers() != null) {
        for (ExpressionTree init : exprTree.getInitializers()) {
          initializers.add(init.accept(this, null));
        }
      }
      TypeMirror arrayType = convertTreeToTypeMirror((JCTree) exprTree.getType());
      if (arrayType == null) {
        throw new ParseRuntimeException(
            constructJavaExpressionParseError(exprTree.getType().toString(), "type not parsable"));
      }
      for (int i = 0; i < dimensions.size(); i++) {
        arrayType = TypesUtils.createArrayType(arrayType, env.getTypeUtils());
      }
      return new ArrayCreation(arrayType, dimensions, initializers);
    }

    @Override
    public JavaExpression visitUnary(UnaryTree exprTree, Void unused) {
      Tree.Kind treeKind = exprTree.getKind();
      JavaExpression operand = exprTree.getExpression().accept(this, null);
      // This eliminates + and performs constant-folding for -; it could also do so for other
      // operations.
      switch (treeKind) {
        case UNARY_PLUS:
          return operand;
        case UNARY_MINUS:
          if (operand instanceof ValueLiteral) {
            return ((ValueLiteral) operand).negate();
          }
          break;
        default:
          // Not optimization for this operand
          break;
      }
      return new UnaryOperation(operand.getType(), treeKind, operand);
    }

    @Override
    public JavaExpression visitBinary(BinaryTree exprTree, Void unused) {
      Tree.Kind operator = exprTree.getKind();
      JavaExpression leftJe = exprTree.getLeftOperand().accept(this, null);
      JavaExpression rightJe = exprTree.getRightOperand().accept(this, null);
      TypeMirror leftType = leftJe.getType();
      TypeMirror rightType = rightJe.getType();
      TypeMirror type;
      // isSubtype() first does the cheaper test isSameType(), so no need to do it here.
      if (operator == Tree.Kind.PLUS
          && (TypesUtils.isString(leftType) || TypesUtils.isString(rightType))) {
        // JLS 15.18.1 says, "If only one operand expression is of type String, then string
        // conversion is performed on the other operand to produce a string at run time."
        type = stringTypeMirror;
      } else if (COMPARISON_OPERATORS.contains(operator)) {
        if (types.isSubtype(leftType, rightType) || types.isSubtype(rightType, leftType)) {
          type = booleanTypeMirror;
        } else {
          // Don't fall through, issue an error immediately instead.
          throw new ParseRuntimeException(
              constructJavaExpressionParseError(
                  exprTree.toString(),
                  String.format("inconsistent types %s %s for %s", leftType, rightType, exprTree)));
        }
      } else if (types.isSubtype(leftType, rightType)) {
        type = rightType;
      } else if (types.isSubtype(rightType, leftType)) {
        type = leftType;
      } else {
        throw new ParseRuntimeException(
            constructJavaExpressionParseError(
                exprTree.toString(),
                String.format("inconsistent types %s %s for %s", leftType, rightType, exprTree)));
      }
      return new BinaryOperation(type, operator, leftJe, rightJe);
    }

    /**
     * Converts the Javac {@link JCTree} to a {@link TypeMirror}. Returns null if {@code tree} is
     * not handled; this method does not handle type variables, union types, or intersection types.
     *
     * @param tree a JCTree
     * @return a TypeMirror corresponding to {@code tree}, or null if {@code tree} isn't handled
     */
    private @Nullable TypeMirror convertTreeToTypeMirror(JCTree tree) {

      if (tree instanceof MemberSelectTree) {
        MemberSelectTree memberSelectTree = (MemberSelectTree) tree;
        ExpressionTree parsed =
            JavacParseUtil.parseExpression(memberSelectTree.getIdentifier().toString());
        if (parsed instanceof IdentifierTree) {
          try {
            return JavacParseUtil.parseExpression(tree.toString()).accept(this, null).getType();
          } catch (RuntimeException e) {
            return null;
          }
        }
      }

      if (tree instanceof IdentifierTree) {
        try {
          return JavacParseUtil.parseExpression(tree.toString()).accept(this, null).getType();
        } catch (RuntimeException e) {
          return null;
        }
      } else if (tree instanceof JCTree.JCPrimitiveTypeTree) {
        switch (((JCTree.JCPrimitiveTypeTree) tree).getPrimitiveTypeKind()) {
          case BOOLEAN:
            return types.getPrimitiveType(TypeKind.BOOLEAN);
          case BYTE:
            return types.getPrimitiveType(TypeKind.BYTE);
          case SHORT:
            return types.getPrimitiveType(TypeKind.SHORT);
          case INT:
            return types.getPrimitiveType(TypeKind.INT);
          case CHAR:
            return types.getPrimitiveType(TypeKind.CHAR);
          case FLOAT:
            return types.getPrimitiveType(TypeKind.FLOAT);
          case LONG:
            return types.getPrimitiveType(TypeKind.LONG);
          case DOUBLE:
            return types.getPrimitiveType(TypeKind.DOUBLE);
          case VOID:
            return types.getNoType(TypeKind.VOID);
          default:
            return null;
        }
      } else if (tree instanceof JCTree.JCArrayTypeTree) {
        TypeMirror componentType =
            convertTreeToTypeMirror(((JCTree.JCArrayTypeTree) tree).getType());
        if (componentType == null) {
          return null;
        }
        return types.getArrayType(componentType);
      }
      return null;
    }
  }

  /**
   * If {@code s} is exactly a formal parameter, return its 1-based index. Returns -1 otherwise.
   *
   * @param s a Java expression
   * @return the 1-based index of the formal parameter that {@code s} represents, or -1
   */
  public static int parameterIndex(String s) {
    Matcher matcher = ANCHORED_PARAMETER_PATTERN.matcher(s);
    if (matcher.find()) {
      @SuppressWarnings(
          "nullness:assignment") // group 1 is non-null due to the structure of the regex
      @NonNull String group1 = matcher.group(1);
      return Integer.parseInt(group1);
    }
    return -1;
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Contexts
  //

  /**
   * Returns the type of the innermost enclosing class. Returns Type.noType if the type is a
   * top-level class.
   *
   * <p>If the innermost enclosing class is static, this method returns the type of that class. By
   * contrast, {@link DeclaredType#getEnclosingType()} returns the type of the innermost enclosing
   * class that is not static.
   *
   * @param type a DeclaredType
   * @return the type of the innermost enclosing class or Type.noType
   */
  private static TypeMirror getTypeOfEnclosingClass(DeclaredType type) {
    if (type instanceof ClassType) {
      // enclClass() needs to be called on tsym.owner, because tsym.enclClass() == tsym.
      Symbol sym = ((ClassType) type).tsym.owner;
      if (sym == null) {
        return com.sun.tools.javac.code.Type.noType;
      }

      ClassSymbol cs = sym.enclClass();
      if (cs == null) {
        return com.sun.tools.javac.code.Type.noType;
      }

      return cs.asType();
    } else {
      return type.getEnclosingType();
    }
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Exceptions
  //

  /**
   * An exception that indicates a parse error. Call {@link #getDiagMessage} to obtain a {@link
   * DiagMessage} that can be used for error reporting.
   */
  public static class JavaExpressionParseException extends Exception {
    /** The serial version identifier. */
    private static final long serialVersionUID = 2L;

    /** The error message key. */
    private final @CompilerMessageKey String errorKey;

    /** The arguments to the error message key. */
    @SuppressWarnings("serial") // I do not intend to serialize JavaExpressionParseException objects
    public final Object[] args;

    /**
     * Create a new JavaExpressionParseException.
     *
     * @param errorKey the error message key
     * @param args the arguments to the error message key
     */
    public JavaExpressionParseException(@CompilerMessageKey String errorKey, Object... args) {
      this(null, errorKey, args);
    }

    /**
     * Create a new JavaExpressionParseException.
     *
     * @param cause cause
     * @param errorKey the error message key
     * @param args the arguments to the error message key
     */
    public JavaExpressionParseException(
        @Nullable Throwable cause, @CompilerMessageKey String errorKey, Object... args) {
      super(cause);
      this.errorKey = errorKey;
      this.args = args;
    }

    @Override
    public String getMessage() {
      return errorKey + " " + Arrays.toString(args);
    }

    /**
     * Returns a DiagMessage that can be used for error reporting.
     *
     * @return a DiagMessage that can be used for error reporting
     */
    public DiagMessage getDiagMessage() {
      return new DiagMessage(Diagnostic.Kind.ERROR, errorKey, args);
    }

    public boolean isFlowParseError() {
      return errorKey.endsWith("flowexpr.parse.error");
    }

    @Override
    public String toString() {
      Throwable cause = getCause();
      if (cause == null) {
        return String.format("JavaExpressionParseException([null cause]: %s)", getMessage());
      } else {
        return String.format(
            "JavaExpressionParseException(%s [%s]: %s)",
            cause.toString(), cause.getClass(), getMessage());
      }
    }
  }

  /**
   * Returns a {@link JavaExpressionParseException} with error key "flowexpr.parse.error" for the
   * expression {@code exprString} with explanation {@code explanation}.
   *
   * @param exprString the string that could not be parsed
   * @param explanation an explanation of the parse failure
   * @return a {@link JavaExpressionParseException} for the expression {@code exprString} with
   *     explanation {@code explanation}
   */
  public static JavaExpressionParseException constructJavaExpressionParseError(
      String exprString, String explanation) {
    if (exprString == null) {
      throw new BugInCF("Must have an expression.");
    }
    if (explanation == null) {
      throw new BugInCF("Must have an explanation.");
    }
    return new JavaExpressionParseException(
        (Throwable) null,
        "flowexpr.parse.error",
        "Invalid '" + exprString + "' because " + explanation);
  }

  /**
   * The unchecked exception equivalent of checked exception {@link JavaExpressionParseException}.
   */
  private static class ParseRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 2L;
    private final JavaExpressionParseException exception;

    private ParseRuntimeException(JavaExpressionParseException exception) {
      this.exception = exception;
    }

    private JavaExpressionParseException getCheckedException() {
      return exception;
    }

    @Override
    public String getMessage() {
      return "JavaExpressionParseException(" + exception + ")";
    }
  }
}
