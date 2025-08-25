package org.checkerframework.dataflow.expression;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
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
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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
import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.qual.Format;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.expression.JavaExpressionParseException.JavaExpressionParseExceptionUnchecked;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Resolver;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.javacutil.javacparse.JavacParse;
import org.checkerframework.javacutil.javacparse.JavacParseResult;
import org.checkerframework.javacutil.trees.TreeBuilder;
import org.plumelib.util.CollectionsPlume;

/**
 * A visitor class that converts a javac {@link ExpressionTree} to a {@link JavaExpression}. This
 * class does not viewpoint-adapt the expression.
 */
class ExpressionTreeToJavaExpressionVisitor extends SimpleTreeVisitor<JavaExpression, Void> {

  /** How to format warnings about use of formal parameter name. */
  public static final @Format({ConversionCategory.INT, ConversionCategory.GENERAL}) String
      FORMAL_PARAM_NAME_STRING = "Use \"#%d\" rather than \"%s\"";

  /** Binary operations that return {@code boolean}. */
  private static final Set<Tree.Kind> COMPARISON_OPERATORS =
      EnumSet.of(
          Tree.Kind.EQUAL_TO,
          Tree.Kind.NOT_EQUAL_TO,
          Tree.Kind.LESS_THAN,
          Tree.Kind.LESS_THAN_EQUAL,
          Tree.Kind.GREATER_THAN,
          Tree.Kind.GREATER_THAN_EQUAL);

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
   * appears in the expression. Not relevant to qualified "SomeClass.this" or
   * "package.SomeClass.this".
   */
  private final @Nullable ThisReference thisReference;

  /**
   * For each formal parameter, the expression to which to parse it. For example, the second (index
   * 1) element of the list is what "#2" parses to. If this field is {@code null}, a parse error
   * will be thrown if "#2" appears in the expression.
   */
  private final @Nullable List<FormalParameter> parameters;

  /**
   * Create a new ExpressionTreeToJavaExpressionVisitor.
   *
   * @param enclosingType type of the class that encloses the JavaExpression
   * @param thisReference a JavaExpression to which to parse "this", or null if "this" should not
   *     appear in the expression; not relevant to qualified "SomeClass.this" or
   *     "package.SomeClass.this"
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
   *     appear in the expression; not relevant to qualified "SomeClass.this" or
   *     "package.SomeClass.this"
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
    } catch (JavaExpressionParseExceptionUnchecked e) {
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

  /**
   * If the expression is not supported, throw a {@link JavaExpressionParseExceptionUnchecked} by
   * default.
   */
  @Override
  public JavaExpression defaultAction(Tree treeNode, Void unused) {
    throw new JavaExpressionParseExceptionUnchecked(
        JavaExpressionParseException.construct(
            treeNode.toString(),
            "ExpressionTreeToJavaExpressionVisitor has no override for "
                + treeNode.getClass().getSimpleName()));
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
      throw new JavaExpressionParseExceptionUnchecked(
          JavaExpressionParseException.construct(
              value.toString(), "Unsupported literal type: " + value.getClass()));
    }

    return new ValueLiteral(type, value);
  }

  @Override
  public JavaExpression visitParenthesized(ParenthesizedTree exprTree, Void unused) {
    return exprTree.getExpression().accept(this, null);
  }

  @Override
  public JavaExpression visitArrayAccess(ArrayAccessTree exprTree, Void unused) {
    JavaExpression array = exprTree.getExpression().accept(this, null);
    TypeMirror arrayType = array.getType();
    if (arrayType.getKind() != TypeKind.ARRAY) {
      throw new JavaExpressionParseExceptionUnchecked(
          JavaExpressionParseException.construct(
              exprTree.toString(),
              String.format(
                  "expected an array, found %s of type %s [%s]",
                  array, arrayType, arrayType.getKind())));
    }
    TypeMirror componentType = ((ArrayType) arrayType).getComponentType();

    JavaExpression index = exprTree.getIndex().accept(this, null);

    return new ArrayAccess(componentType, array, index);
  }

  // `idTree` is an identifier with no dots in its name.
  @Override
  public JavaExpression visitIdentifier(IdentifierTree idTree, Void unused) {
    setResolverField();
    String idName = idTree.getName().toString();
    // this and super logic
    if (idName.equals("this") || idName.equals("super")) {
      if (thisReference == null) {
        throw new JavaExpressionParseExceptionUnchecked(
            JavaExpressionParseException.construct(
                idName, "\"" + idName + "\" is not allowed here"));
      }
      if (idName.equals("this")) {
        return thisReference;
      } else {
        // super literal
        TypeMirror superclass = TypesUtils.getSuperclass(enclosingType, types);
        if (superclass == null) {
          throw new JavaExpressionParseExceptionUnchecked(
              JavaExpressionParseException.construct(
                  "super", enclosingType + " has no superclass"));
        }
        return new SuperReference(superclass);
      }
    }

    // Formal parameter, using "#2" syntax.
    JavaExpression parameter = parseAsParameter(idName);
    if (parameter != null) {
      return parameter;
    }

    // Local variable or parameter.
    // Attempt to match a local variable within the scope of the
    // given path before attempting to match a field.
    if (localVarPath != null) {
      VariableElement varElem = resolver.findLocalVariableOrParameter(idName, localVarPath);
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
    FieldAccess fieldAccess = getIdentifierAsFieldAccess(fieldAccessReceiver, idName);
    if (fieldAccess != null) {
      return fieldAccess;
    }

    // Class name
    if (localVarPath != null) {
      Element classElem = resolver.findClass(idName, localVarPath);
      TypeMirror classType = ElementUtils.getType(classElem);
      if (classType != null) {
        return new ClassName(classType);
      }
    }
    ClassName classType = getIdentifierAsUnqualifiedClassName(idName);
    if (classType != null) {
      return classType;
    }

    // Err if a formal parameter name is used, instead of the "#2" syntax.
    if (parameters != null) {
      for (int i = 0; i < parameters.size(); i++) {
        Element varElt = parameters.get(i).getElement();
        if (varElt.getSimpleName().contentEquals(idName)) {
          throw new JavaExpressionParseExceptionUnchecked(
              JavaExpressionParseException.construct(
                  idName, String.format(FORMAL_PARAM_NAME_STRING, i + 1, idName)));
        }
      }
    }

    throw new JavaExpressionParseExceptionUnchecked(
        JavaExpressionParseException.construct(idName, "identifier not found"));
  }

  /**
   * If {@code s} is a parameter expressed using the "_param_NN" syntax, then returns a
   * JavaExpression for the given parameter; that is, returns an element of {@code parameters}.
   * Otherwise, returns {@code null}.
   *
   * <p>The user writes formal parameters like "#2", which is not a legal Java identifier. The
   * Checker Framework converts that string to "_param_2" before passing it to the Java parser.
   *
   * @param s a String that may be a parameter in the "_param_NN" syntax
   * @return the JavaExpression for the given parameter or {@code null} if {@code s} is not a
   *     parameter
   */
  private @Nullable JavaExpression parseAsParameter(String s) {
    if (!s.startsWith(JavaExpressionParseUtil.PARAMETER_PREFIX)) {
      return null;
    }

    int idx = Integer.parseInt(s.substring(JavaExpressionParseUtil.PARAMETER_PREFIX_LENGTH));
    if (idx == 0) {
      throw new JavaExpressionParseExceptionUnchecked(
          JavaExpressionParseException.construct(
              "#0", "Use \"this\" for the receiver or \"#1\" for the first formal parameter"));
    }

    if (parameters == null) {
      throw new JavaExpressionParseExceptionUnchecked(
          JavaExpressionParseException.construct(s, "no parameters found"));
    }

    if (idx > parameters.size()) {
      @SuppressWarnings("compilermessages:argument") // defined in the framework project
      JavaExpressionParseException jepe =
          new JavaExpressionParseException(
              "flowexpr.parse.index.too.big",
              Integer.toString(idx),
              Integer.toString(parameters.size()));
      throw new JavaExpressionParseExceptionUnchecked(jepe);
    }
    return parameters.get(idx - 1);
  }

  @Override
  public JavaExpression visitPrimitiveType(PrimitiveTypeTree node, Void unused) {
    TypeKind typeKind = node.getPrimitiveTypeKind();
    if (typeKind == TypeKind.VOID) {
      return new ClassName(types.getNoType(typeKind));
    } else {
      return new ClassName(types.getPrimitiveType(typeKind));
    }
  }

  @Override
  public JavaExpression visitArrayType(ArrayTypeTree node, Void unused) {
    Tree elementTypeTree = node.getType();
    JavaExpression elementTypeJE = elementTypeTree.accept(this, null);
    if (elementTypeJE instanceof ClassName) {
      return new ClassName(types.getArrayType(((ClassName) elementTypeJE).getType()));
    } else {
      throw new JavaExpressionParseExceptionUnchecked(
          JavaExpressionParseException.construct(
              node.toString(),
              "array element type is " + elementTypeJE.getClass().getSimpleName()));
    }
  }

  /**
   * If {@code identifier} is the simple class name of any inner class of {@code type}, return the
   * {@link ClassName} for the inner class. If not, return null.
   *
   * @param type the type in which to search for {@code identifier}
   * @param identifier possible simple class name
   * @return the {@code ClassName} for {@code identifier}, or null if it is not a simple class name
   */
  protected @Nullable ClassName getIdentifierAsInnerClassName(TypeMirror type, String identifier) {
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
   * If {@code identifier} is a class name that can be referenced using only its simple name within
   * {@code enclosingType}, return the {@link ClassName} for the class. If not, return null.
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
          (PackageSymbol) ElementUtils.enclosingPackage(((DeclaredType) enclosingType).asElement());
      ClassSymbol classSymbol =
          resolver.findClassInPackage(identifier, packageSymbol, pathToCompilationUnit);
      if (classSymbol != null) {
        return new ClassName(classSymbol.asType());
      }
    }
    // Is identifier a simple name for a class in java.lang?
    PackageSymbol packageSymbol = resolver.findPackage("java.lang", pathToCompilationUnit);
    if (packageSymbol == null) {
      throw new BugInCF("Can't find the java.lang package.");
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
   * Returns the {@link FieldAccess} expression for the field with name {@code identifier} accessed
   * via {@code receiverExpr}. If no such field exists, then {@code null} is returned.
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
          && thisReference != null
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
      throw new JavaExpressionParseExceptionUnchecked(
          JavaExpressionParseException.construct(
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
        throw new JavaExpressionParseExceptionUnchecked(
            JavaExpressionParseException.construct(identifier, msg));
      }
      TypeElement receiverTypeElement = TypesUtils.getTypeElement(receiverExpr.getType());
      if (receiverTypeElement == null || ElementUtils.isStatic(receiverTypeElement)) {
        String msg =
            String.format("%s is a non-static field declared in an outer type this.", identifier);
        throw new JavaExpressionParseExceptionUnchecked(
            JavaExpressionParseException.construct(identifier, msg));
      }
      JavaExpression locationOfField = new ThisReference(enclosingTypeOfField);
      return new FieldAccess(locationOfField, fieldElem);
    }
  }

  @Override
  public JavaExpression visitMethodInvocation(MethodInvocationTree invocation, Void unused) {
    setResolverField();
    ExpressionTree methodSelect = invocation.getMethodSelect();

    // Resolve receiver type and method name.  (Receiver itself is resolved below.)
    JavaExpression receiverExprTmp; // null if not yet computed
    TypeMirror receiverType;
    String methodName;
    if (methodSelect instanceof MemberSelectTree) {
      // Method call with explicit receiver, like `obj.method()` or `Class.staticMethod()`.
      MemberSelectTree memberSelect = (MemberSelectTree) methodSelect;
      receiverExprTmp = memberSelect.getExpression().accept(this, null);
      receiverType = receiverExprTmp.getType();
      methodName = memberSelect.getIdentifier().toString();
    } else if (methodSelect instanceof IdentifierTree) {
      // Static or instance method call with implicit receiver, like `method()`.
      methodName = ((IdentifierTree) methodSelect).getName().toString();
      receiverExprTmp = null;
      receiverType = enclosingType;
    } else {
      throw new JavaExpressionParseExceptionUnchecked(
          JavaExpressionParseException.construct(
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
          getMethodElement(methodName, receiverType, pathToCompilationUnit, arguments, resolver);
    } catch (JavaExpressionParseException e) {
      throw new JavaExpressionParseExceptionUnchecked(e);
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

    // The compiler will optimize out the redundant variable.
    boolean isStatic = ElementUtils.isStatic(methodElement);
    boolean isInstance = !isStatic;

    JavaExpression receiverExpr;

    if (methodSelect instanceof MemberSelectTree) {
      // Method call with explicit receiver, like `obj.method()` or `Class.staticMethod()`.
      assert receiverExprTmp != null : "@AssumeAssertion(nullness): established in `if` above";
      receiverExpr = receiverExprTmp;
      if (isInstance && receiverExpr instanceof ClassName) {
        throw new JavaExpressionParseExceptionUnchecked(
            JavaExpressionParseException.construct(
                invocation.toString(),
                "Use a value, not a class name, as the receiver when calling an instance"
                    + " method"));
      } else if (isStatic && !(receiverExpr instanceof ClassName)) {
        // TODO: Should we instead issue an error "Use a class name, not a value, as the receiver
        // when calling a static method"?
        receiverExpr = new ClassName(receiverExpr.getType());
      }
    } else if (methodSelect instanceof IdentifierTree) {
      // Static or instance method call with implicit receiver, like `method()`.
      if (isInstance) {
        assert thisReference != null : "@AssumeAssertion(nullness): isInstance => thisReference";
        receiverExpr = thisReference;
      } else {
        Element classElem = methodElement.getEnclosingElement();
        receiverExpr = new ClassName(ElementUtils.getType(classElem));
      }
      receiverType = receiverExpr.getType();
    } else {
      throw new BugInCF("this can't happen");
    }

    TypeMirror returnType;
    if (isInstance) {
      returnType = TypesUtils.substituteMethodReturnType(methodElement, receiverType, env);
    } else {
      returnType = ElementUtils.getType(methodElement);
    }

    return new MethodCall(returnType, methodElement, receiverExpr, arguments);
  }

  /**
   * Returns the ExecutableElement for a method, or throws an exception.
   *
   * <p>This method takes into account autoboxing.
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
        throw JavaExpressionParseException.construct(methodName, "no such method");
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
    throw JavaExpressionParseException.construct(methodName, "no such method");
  }

  // `exprTree` should be a field access, a fully qualified class name, or a class name qualified
  // with another class name (e.g. {@code OuterClass.InnerClass}).  It can also end with ".class"
  // or ".this".  If the expression refers
  // to a class that is not available to the resolver (the class wasn't passed to javac on
  // the command line), then `exprTree` can be "outerpackage.innerpackage", which will lead
  // to a confusing error message.
  @Override
  public JavaExpression visitMemberSelect(MemberSelectTree exprTree, Void unused) {
    setResolverField();

    Tree scope = exprTree.getExpression();
    String name = exprTree.getIdentifier().toString();

    // Handle class literal (e.g., SomeClass.class or pkg.pkg2.OuterClass.InnerClass.class).
    if (name.equals("class")) {
      JavaExpression className = scope.accept(this, null);
      if (className instanceof ClassName) {
        return className;
      } else {
        throw new JavaExpressionParseExceptionUnchecked(
            JavaExpressionParseException.construct(
                exprTree.toString(),
                "\".class\" preceded by " + className.getClass().getSimpleName()));
      }
    }

    // Handle outer "this" (e.g., Foo.this).
    if (name.equals("this")) {
      JavaExpression className = scope.accept(this, null);
      if (className instanceof ClassName) {
        return new ThisReference(className.getType());
      } else {
        throw new JavaExpressionParseExceptionUnchecked(
            JavaExpressionParseException.construct(
                exprTree.toString(),
                "\".this\" preceded by " + className.getClass().getSimpleName()));
      }
    }

    // Check if the expression refers to a fully-qualified non-nested class name.
    PackageSymbol packageSymbol = resolver.findPackage(scope.toString(), pathToCompilationUnit);
    if (packageSymbol != null) {
      ClassSymbol classSymbol =
          resolver.findClassInPackage(name, packageSymbol, pathToCompilationUnit);
      if (classSymbol != null) {
        return new ClassName(classSymbol.asType());
      }
      throw new JavaExpressionParseExceptionUnchecked(
          JavaExpressionParseException.construct(
              exprTree.toString(),
              "could not find class " + name + " in package " + scope.toString()));
    }

    // Otherwise treat as field access or inner class.
    JavaExpression receiver = scope.accept(this, null);

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
    throw new JavaExpressionParseExceptionUnchecked(
        JavaExpressionParseException.construct(
            name, String.format("field or class %s not found in %s", name, receiver)));
  }

  @Override
  public JavaExpression visitNewArray(NewArrayTree exprTree, Void unused) {
    List<@Nullable JavaExpression> dimensions = new ArrayList<>();
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
      throw new JavaExpressionParseExceptionUnchecked(
          JavaExpressionParseException.construct(
              exprTree.getType().toString(), "type not parsable"));
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
        throw new JavaExpressionParseExceptionUnchecked(
            JavaExpressionParseException.construct(
                exprTree.toString(),
                String.format("inconsistent types %s %s for %s", leftType, rightType, exprTree)));
      }
    } else if (types.isSubtype(leftType, rightType)) {
      type = rightType;
    } else if (types.isSubtype(rightType, leftType)) {
      type = leftType;
    } else {
      throw new JavaExpressionParseExceptionUnchecked(
          JavaExpressionParseException.construct(
              exprTree.toString(),
              String.format("inconsistent types %s %s for %s", leftType, rightType, exprTree)));
    }
    return new BinaryOperation(type, operator, leftJe, rightJe);
  }

  /**
   * Converts the Javac {@link JCTree} to a {@link TypeMirror}. Returns null if {@code tree} is not
   * handled; this method does not handle type variables, union types, or intersection types.
   *
   * @param typeTree a type
   * @return a TypeMirror corresponding to {@code typeTree}, or null if {@code typeTree} isn't
   *     handled
   */
  private @Nullable TypeMirror convertTreeToTypeMirror(JCTree typeTree) {
    if (typeTree instanceof MemberSelectTree) {
      MemberSelectTree memberSelectTree = (MemberSelectTree) typeTree;
      String identifier = memberSelectTree.getIdentifier().toString();
      JavacParseResult<ExpressionTree> jpr = JavacParse.parseExpression(identifier);
      if (jpr.hasParseError()) {
        throw new Error(identifier + " :" + jpr.getParseErrorMessages());
      }
      ExpressionTree parsed = jpr.getTree();

      if (parsed instanceof IdentifierTree) {
        return parsed.accept(this, null).getType();
      } else {
        String msg =
            String.format(
                "parsed is not IdentifierTree: %s [%s]", parsed, parsed.getClass().getSimpleName());
        throw new BugInCF(msg);
      }
    } else if (typeTree instanceof IdentifierTree) {
      try {
        return typeTree.accept(this, null).getType();
      } catch (Throwable e) {
        throw new BugInCF("Problem while parsing " + typeTree, e);
      }
    } else if (typeTree instanceof JCTree.JCPrimitiveTypeTree) {
      switch (((JCTree.JCPrimitiveTypeTree) typeTree).getPrimitiveTypeKind()) {
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
    } else if (typeTree instanceof JCTree.JCArrayTypeTree) {
      TypeMirror componentType =
          convertTreeToTypeMirror(((JCTree.JCArrayTypeTree) typeTree).getType());
      if (componentType == null) {
        return null;
      }
      return types.getArrayType(componentType);
    }
    System.out.printf(
        "convertTreeToTypeMirror does not handle %s [%s]%n",
        typeTree, typeTree.getClass().getSimpleName());
    return null;
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Contexts
  //

  /**
   * Returns the innermost enclosing class. Returns Type.noType if the argument is a top-level
   * class.
   *
   * <p>If the innermost enclosing class is static, this method returns that class. By contrast,
   * {@link DeclaredType#getEnclosingType()} returns the innermost enclosing class that is not
   * static.
   *
   * @param type a DeclaredType
   * @return the innermost enclosing class or Type.noType
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
}
