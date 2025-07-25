package org.checkerframework.framework.util.dependenttypes;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.FormalParameter;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.JavaExpressionConverter;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.SuperReference;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.dataflow.expression.Unknown;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.type.visitor.DoubleAnnotatedTypeScanner;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionParseException;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.CollectionsPlume;

/**
 * A class that helps checkers use qualifiers that are represented by annotations with Java
 * expression strings. This class performs the following main functions:
 *
 * <ol>
 *   <li>Converts the expression strings in an {@link AnnotationMirror} {@code am}, by creating a
 *       new annotation whose Java expression elements are the result of the conversion. See {@link
 *       #convertAnnotationMirror(StringToJavaExpression, AnnotationMirror)}, though clients do not
 *       call it (they call other methods in this class, which eventually call it). Subclasses can
 *       specialize this process by overriding methods in this class. Methods in this class always
 *       standardize Java expressions and may additionally viewpoint-adapt or delocalize
 *       expressions. Below is an explanation of each kind of conversion.
 *       <ul>
 *         <li>Standardization: the expressions in the annotations are converted such that two
 *             expression strings that are equivalent are made to be equal. For example, an instance
 *             field f may appear in an expression string as "f" or "this.f"; this class
 *             standardizes both strings to "this.f". All dependent type annotations must be
 *             standardized so that the implementation of {@link
 *             org.checkerframework.framework.type.QualifierHierarchy#isSubtypeShallow(AnnotationMirror,
 *             TypeMirror, AnnotationMirror, TypeMirror)} can assume that two expressions are
 *             equivalent if their string representations are {@code equals()}.
 *         <li>Viewpoint-adaption: converts an expression to some use site. For example, in method
 *             bodies, formal parameter references such as "#2" are converted to the name of the
 *             formal parameter. Another example, is at method call site, "this" is converted to the
 *             receiver of the method invocation.
 *         <li>Delocalization: removes all expressions with references to local variables that are
 *             not parameters and changes parameters to the "#1" syntax.
 *       </ul>
 *   <li>If any of the conversions above results in an invalid expression, this class changes
 *       invalid expression strings to an error string that includes the reason why the expression
 *       is invalid. For example, {@code @KeyFor("m")} would be changed to {@code @KeyFor("[error
 *       for expression: m error: m: identifier not found]")} if m is not a valid identifier. This
 *       allows subtyping checks to assume that if two strings are equal and not errors, they
 *       reference the same valid Java expression.
 *   <li>Checks annotated types for error strings that have been added by this class and issues an
 *       error if any are found.
 * </ol>
 *
 * <p>Steps 2 and 3 are separated so that an error is issued only once per invalid expression string
 * rather than every time the expression string is parsed. (The expression string is parsed multiple
 * times because annotated types are created multiple times.)
 */
public class DependentTypesHelper {

  /** AnnotatedTypeFactory. */
  protected final AnnotatedTypeFactory factory;

  /**
   * Maps from an annotation name, the fully-qualified name of its class, to its elements that are
   * Java expressions.
   */
  private final Map<String, List<ExecutableElement>> annoToElements;

  /** This scans an annotated type and returns a list of {@link DependentTypesError}. */
  private final ExpressionErrorCollector expressionErrorCollector = new ExpressionErrorCollector();

  /**
   * This scans the annotated type and replaces any dependent type annotation that has a parse error
   * with the top annotation in the hierarchy.
   */
  protected final ErrorAnnoReplacer errorAnnoReplacer;

  /**
   * A scanner that applies a function to each {@link AnnotationMirror} and replaces it in the given
   * {@code AnnotatedTypeMirror}. (This side-effects the {@code AnnotatedTypeMirror}.)
   */
  private final AnnotatedTypeReplacer annotatedTypeReplacer = new AnnotatedTypeReplacer();

  /**
   * Copies annotations that might have been viewpoint adapted from the visited type (the first
   * formal parameter of {@code ViewpointAdaptedCopier#visit}) to the second formal parameter.
   */
  protected final ViewpointAdaptedCopier viewpointAdaptedCopier = new ViewpointAdaptedCopier();

  /** The type mirror for java.lang.Object. */
  protected final TypeMirror objectTM;

  /**
   * Creates a {@code DependentTypesHelper}.
   *
   * @param factory annotated type factory
   */
  public DependentTypesHelper(AnnotatedTypeFactory factory) {
    this.factory = factory;
    this.errorAnnoReplacer = new ErrorAnnoReplacer(factory.getQualifierHierarchy());
    this.annoToElements = new HashMap<>();
    for (Class<? extends Annotation> expressionAnno : factory.getSupportedTypeQualifiers()) {
      List<ExecutableElement> elementList =
          getExpressionElements(expressionAnno, factory.getProcessingEnv());
      if (!elementList.isEmpty()) {
        annoToElements.put(expressionAnno.getCanonicalName(), elementList);
      }
    }

    this.objectTM =
        TypesUtils.typeFromClass(Object.class, factory.types, factory.getElementUtils());
  }

  /**
   * Returns true if any qualifier in the type system is a dependent type annotation.
   *
   * @return true if any qualifier in the type system is a dependent type annotation
   */
  public boolean hasDependentAnnotations() {
    return !annoToElements.isEmpty();
  }

  /**
   * Returns a list of the elements in the annotation class that should be interpreted as Java
   * expressions, namely those annotated with {@code @}{@link JavaExpression}.
   *
   * @param clazz annotation class
   * @param env processing environment for getting the ExecutableElement
   * @return a list of the elements in the annotation class that should be interpreted as Java
   *     expressions
   */
  private static List<ExecutableElement> getExpressionElements(
      Class<? extends Annotation> clazz, ProcessingEnvironment env) {
    Method[] methods = clazz.getMethods();
    if (methods == null) {
      return Collections.emptyList();
    }
    List<ExecutableElement> elements = new ArrayList<>();
    for (Method method : methods) {
      org.checkerframework.framework.qual.JavaExpression javaExpressionAnno =
          method.getAnnotation(org.checkerframework.framework.qual.JavaExpression.class);
      if (javaExpressionAnno != null) {
        elements.add(TreeUtils.getMethod(clazz, method.getName(), method.getParameterCount(), env));
      }
    }
    return elements;
  }

  /**
   * Returns the elements of the annotation that are Java expressions.
   *
   * @param am an annotation
   * @return the elements of the annotation that are Java expressions
   */
  private List<ExecutableElement> getListOfExpressionElements(AnnotationMirror am) {
    return annoToElements.getOrDefault(AnnotationUtils.annotationName(am), Collections.emptyList());
  }

  /**
   * Creates a TreeAnnotator that viewpoint-adapts dependent type annotations.
   *
   * @return a new TreeAnnotator that viewpoint-adapts dependent type annotations
   */
  public TreeAnnotator createDependentTypesTreeAnnotator() {
    assert hasDependentAnnotations();
    return new DependentTypesTreeAnnotator(factory, this);
  }

  //
  // Methods that convert annotations
  //

  /** If true, log information about where lambdas are created. */
  // This variable is only set here; edit the source code to modify it.
  private static final boolean debugStringToJavaExpression = false;

  /**
   * Viewpoint-adapts the dependent type annotations on the bounds of the type parameters of the
   * declaration of {@code typeUse} to {@code typeUse}.
   *
   * @param bounds annotated types of the bounds of the type parameters; its elements are
   *     side-effected by this method (but the list itself is not side-effected)
   * @param typeUse a use of a type with type parameter bounds {@code bounds}
   */
  public void atParameterizedTypeUse(
      List<AnnotatedTypeParameterBounds> bounds, TypeElement typeUse) {
    if (!hasDependentAnnotations()) {
      return;
    }

    StringToJavaExpression stringToJavaExpr =
        stringExpr -> StringToJavaExpression.atTypeDecl(stringExpr, typeUse, factory.getChecker());
    if (debugStringToJavaExpression) {
      System.out.printf(
          "atParameterizedTypeUse(%s, %s) created %s%n", bounds, typeUse, stringToJavaExpr);
    }
    for (AnnotatedTypeParameterBounds bound : bounds) {
      convertAnnotatedTypeMirror(stringToJavaExpr, bound.getUpperBound());
      convertAnnotatedTypeMirror(stringToJavaExpr, bound.getLowerBound());
    }
  }

  /**
   * Viewpoint-adapts the dependent type annotations in the methodType to the methodInvocationTree.
   *
   * <p>{@code methodType} has been viewpoint-adapted to the call site, except for any dependent
   * type annotations. This method viewpoint-adapts the dependent type annotations.
   *
   * @param methodType type of the method invocation; is side-effected by this method
   * @param methodInvocationTree use of the method
   */
  public void atMethodInvocation(
      AnnotatedExecutableType methodType, MethodInvocationTree methodInvocationTree) {
    if (!hasDependentAnnotations()) {
      return;
    }
    atInvocation(methodType, methodInvocationTree);
  }

  /**
   * Viewpoint-adapts the dependent type annotations in the constructorType to the newClassTree.
   *
   * <p>{@code constructorType} has been viewpoint-adapted to the call site, except for any
   * dependent type annotations. This method viewpoint-adapts the dependent type annotations.
   *
   * @param constructorType type of the constructor invocation; is side-effected by this method
   * @param newClassTree invocation of the constructor
   */
  public void atConstructorInvocation(
      AnnotatedExecutableType constructorType, NewClassTree newClassTree) {
    if (!hasDependentAnnotations()) {
      return;
    }
    atInvocation(constructorType, newClassTree);
  }

  /**
   * Viewpoint-adapts dependent type annotations in a method or constructor type.
   *
   * <p>{@code methodType} has been viewpoint-adapted to the call site, except for any dependent
   * type annotations. (For example, type variables have been substituted and polymorphic qualifiers
   * have been resolved.) This method viewpoint-adapts the dependent type annotations.
   *
   * @param methodType type of the method or constructor invocation; is side-effected by this method
   * @param tree invocation of the method or constructor
   */
  private void atInvocation(AnnotatedExecutableType methodType, ExpressionTree tree) {
    assert hasDependentAnnotations();
    Element methodElt = TreeUtils.elementFromUse(tree);
    // Because methodType is the type post type variable substitution, it has annotations from
    // both the method declaration and the type arguments at the use of the method. Annotations
    // from type arguments must not be viewpoint-adapted to the call site. For example:
    //   Map<String, String> map = ...;
    //   List<@KeyFor("this.map") String> list = ...;
    //   list.get(0)
    //
    // methodType is @KeyFor("this.map") String get(int)
    // "this.map" must not be viewpoint-adapted to the invocation because it is not from
    // the method declaration, but added during type variable substitution.
    //
    // So this implementation gets the declared type of the method, declaredMethodType,
    // viewpoint-adapts all dependent type annotations in declaredMethodType to the call site,
    // and then copies the viewpoint-adapted annotations from methodType except for types that
    // are replaced by type variable substitution. (Those annotations are viewpoint-adapted
    // before type variable substitution.)

    // The annotations on `declaredMethodType` will be copied to `methodType`.
    AnnotatedExecutableType declaredMethodType =
        (AnnotatedExecutableType) factory.getAnnotatedType(methodElt);
    if (!hasDependentType(declaredMethodType)) {
      return;
    }

    StringToJavaExpression stringToJavaExpr;
    if (tree instanceof MethodInvocationTree) {
      stringToJavaExpr =
          stringExpr ->
              StringToJavaExpression.atMethodInvocation(
                  stringExpr, (MethodInvocationTree) tree, factory.getChecker());
      if (debugStringToJavaExpression) {
        System.out.printf(
            "atInvocation(%s, %s) 1 created %s%n",
            methodType, TreeUtils.toStringTruncated(tree, 65), stringToJavaExpr);
      }
    } else if (tree instanceof NewClassTree) {
      stringToJavaExpr =
          stringExpr ->
              StringToJavaExpression.atConstructorInvocation(
                  stringExpr, (NewClassTree) tree, factory.getChecker());
      if (debugStringToJavaExpression) {
        System.out.printf(
            "atInvocation(%s, %s) 2 created %s%n",
            methodType, TreeUtils.toStringTruncated(tree, 65), stringToJavaExpr);
      }
    } else {
      throw new BugInCF("Unexpected tree: %s kind: %s", tree, tree.getKind());
    }
    convertAnnotatedTypeMirror(stringToJavaExpr, declaredMethodType);
    this.viewpointAdaptedCopier.visit(declaredMethodType, methodType);
  }

  /**
   * Viewpoint-adapts the Java expressions in annotations written on a field declaration to the use
   * at {@code fieldAccess}.
   *
   * @param type its type; is side-effected by this method
   * @param fieldAccess a field access
   */
  public void atFieldAccess(AnnotatedTypeMirror type, MemberSelectTree fieldAccess) {
    if (!hasDependentType(type)) {
      return;
    }

    StringToJavaExpression stringToJavaExpr =
        stringExpr ->
            StringToJavaExpression.atFieldAccess(stringExpr, fieldAccess, factory.getChecker());
    if (debugStringToJavaExpression) {
      System.out.printf(
          "atFieldAccess(%s, %s) created %s%n",
          type, TreeUtils.toStringTruncated(fieldAccess, 65), stringToJavaExpr);
    }
    convertAnnotatedTypeMirror(stringToJavaExpr, type);
  }

  /**
   * Viewpoint-adapts the Java expressions in annotations written on the signature of the method
   * declaration (for example, a return type) to the body of the method. This means the parameter
   * syntax, e.g. "#2", is converted to the names of the parameter.
   *
   * @param atm a type at the method signature; is side-effected by this method
   * @param methodDeclTree a method declaration
   */
  public void atMethodBody(AnnotatedTypeMirror atm, MethodTree methodDeclTree) {
    if (!hasDependentType(atm)) {
      return;
    }

    StringToJavaExpression stringToJavaExpr =
        stringExpr ->
            StringToJavaExpression.atMethodBody(stringExpr, methodDeclTree, factory.getChecker());
    if (debugStringToJavaExpression) {
      System.out.printf(
          "atMethodBody(%s, %s) 1 created %s%n",
          atm, TreeUtils.toStringTruncated(methodDeclTree, 65), stringToJavaExpr);
    }
    convertAnnotatedTypeMirror(stringToJavaExpr, atm);
  }

  /**
   * Standardizes the Java expressions in annotations to a type declaration.
   *
   * @param type the type of the type declaration; is side-effected by this method
   * @param typeElt the element of the type declaration
   */
  public void atTypeDecl(AnnotatedTypeMirror type, TypeElement typeElt) {
    if (!hasDependentType(type)) {
      return;
    }

    StringToJavaExpression stringToJavaExpr =
        stringExpr -> StringToJavaExpression.atTypeDecl(stringExpr, typeElt, factory.getChecker());
    if (debugStringToJavaExpression) {
      System.out.printf("atTypeDecl(%s, %s) created %s%n", type, typeElt, stringToJavaExpr);
    }
    convertAnnotatedTypeMirror(stringToJavaExpr, type);
  }

  /** A set containing {@link Tree.Kind#METHOD} and {@link Tree.Kind#LAMBDA_EXPRESSION}. */
  private static final Set<Tree.Kind> METHOD_OR_LAMBDA =
      EnumSet.of(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION);

  /**
   * Standardize the Java expressions in annotations in a variable declaration. Converts the
   * parameter syntax, e.g "#1", to the parameter name.
   *
   * @param type the type of the variable declaration; is side-effected by this method
   * @param declarationTree the variable declaration
   * @param variableElt the element of the variable declaration
   */
  public void atVariableDeclaration(
      AnnotatedTypeMirror type, Tree declarationTree, VariableElement variableElt) {
    if (!hasDependentType(type)) {
      return;
    }

    TreePath pathToVariableDecl = factory.getPath(declarationTree);
    if (pathToVariableDecl == null) {
      // If this is a synthetic created by dataflow, the path will be null.
      return;
    }
    ElementKind variableKind = variableElt.getKind();
    if (ElementUtils.isBindingVariable(variableElt)) {
      // Treat binding variables the same as local variables.
      variableKind = ElementKind.LOCAL_VARIABLE;
    }
    switch (variableKind) {
      case PARAMETER:
        TreePath pathTillEnclTree =
            TreePathUtil.pathTillOfKind(pathToVariableDecl, METHOD_OR_LAMBDA);
        if (pathTillEnclTree == null) {
          throw new BugInCF("no enclosing method or lambda found for " + variableElt);
        }
        Tree enclTree = pathTillEnclTree.getLeaf();

        if (enclTree instanceof MethodTree) {
          MethodTree methodDeclTree = (MethodTree) enclTree;
          StringToJavaExpression stringToJavaExpr =
              stringExpr ->
                  StringToJavaExpression.atMethodBody(
                      stringExpr, methodDeclTree, factory.getChecker());
          if (debugStringToJavaExpression) {
            System.out.printf(
                "atVariableDeclaration(%s, %s, %s) 1 created %s%n",
                type,
                TreeUtils.toStringTruncated(declarationTree, 65),
                variableElt,
                stringToJavaExpr);
          }
          convertAnnotatedTypeMirror(stringToJavaExpr, type);
        } else {
          // Lambdas can use local variables defined in the enclosing method, so allow
          // identifiers to be locals in scope at the location of the lambda.
          StringToJavaExpression stringToJavaExpr =
              stringExpr ->
                  StringToJavaExpression.atLambdaParameter(
                      stringExpr,
                      (LambdaExpressionTree) enclTree,
                      pathToVariableDecl.getParentPath(),
                      factory.getChecker());
          if (debugStringToJavaExpression) {
            System.out.printf(
                "atVariableDeclaration(%s, %s, %s) 2 created %s%n",
                type,
                TreeUtils.toStringTruncated(declarationTree, 65),
                variableElt,
                stringToJavaExpr);
          }
          convertAnnotatedTypeMirror(stringToJavaExpr, type);
        }
        break;

      case LOCAL_VARIABLE:
      case RESOURCE_VARIABLE:
      case EXCEPTION_PARAMETER:
        StringToJavaExpression stringToJavaExprVar =
            stringExpr ->
                StringToJavaExpression.atPath(stringExpr, pathToVariableDecl, factory.getChecker());
        if (debugStringToJavaExpression) {
          System.out.printf(
              "atVariableDeclaration(%s, %s, %s) 3 created %s%n",
              type,
              TreeUtils.toStringTruncated(declarationTree, 65),
              variableElt,
              stringToJavaExprVar);
        }
        convertAnnotatedTypeMirror(stringToJavaExprVar, type);
        break;

      case FIELD:
      case ENUM_CONSTANT:
        StringToJavaExpression stringToJavaExprField =
            stringExpr ->
                StringToJavaExpression.atFieldDecl(stringExpr, variableElt, factory.getChecker());
        if (debugStringToJavaExpression) {
          System.out.printf(
              "atVariableDeclaration(%s, %s, %s) 4 created %s%n",
              type,
              TreeUtils.toStringTruncated(declarationTree, 65),
              variableElt,
              stringToJavaExprField);
        }
        convertAnnotatedTypeMirror(stringToJavaExprField, type);
        break;

      default:
        throw new BugInCF(
            "unexpected element kind " + variableElt.getKind() + " for " + variableElt);
    }
  }

  /**
   * Standardize the Java expressions in annotations in written in the {@code expressionTree}. Also,
   * converts the parameter syntax, e.g. "#1", to the parameter name.
   *
   * <p>{@code expressionTree} must be an expressions which can contain explicitly written
   * annotations, namely a {@link NewClassTree}, {@link com.sun.source.tree.NewArrayTree}, or {@link
   * com.sun.source.tree.TypeCastTree}. For example, this method standardizes the {@code KeyFor}
   * annotation in {@code (@KeyFor("map") String) key }.
   *
   * @param annotatedType its type; is side-effected by this method
   * @param expressionTree a {@link NewClassTree}, {@link com.sun.source.tree.NewArrayTree}, or
   *     {@link com.sun.source.tree.TypeCastTree}
   */
  public void atExpression(AnnotatedTypeMirror annotatedType, ExpressionTree expressionTree) {
    if (!hasDependentType(annotatedType)) {
      return;
    }

    TreePath path = factory.getPath(expressionTree);
    if (path == null) {
      return;
    }
    StringToJavaExpression stringToJavaExpr =
        stringExpr -> StringToJavaExpression.atPath(stringExpr, path, factory.getChecker());
    if (debugStringToJavaExpression) {
      System.out.printf(
          "atExpression(%s, %s) created %s%n",
          annotatedType, TreeUtils.toStringTruncated(expressionTree, 65), stringToJavaExpr);
    }
    convertAnnotatedTypeMirror(stringToJavaExpr, annotatedType);
  }

  /**
   * Standardize the Java expressions in annotations in a type. Converts the parameter syntax, e.g.
   * "#2", to the parameter name.
   *
   * @param type the type to standardize; is side-effected by this method
   * @param elt the element whose type is {@code type}
   */
  public void atLocalVariable(AnnotatedTypeMirror type, Element elt) {
    if (!hasDependentType(type)) {
      return;
    }

    switch (elt.getKind()) {
      case PARAMETER:
      case LOCAL_VARIABLE:
      case RESOURCE_VARIABLE:
      case EXCEPTION_PARAMETER:
        Tree declarationTree = factory.declarationFromElement(elt);
        if (declarationTree == null) {
          if (elt.getKind() == ElementKind.PARAMETER) {
            // The tree might be null when
            // org.checkerframework.framework.flow.CFAbstractTransfer.getValueFromFactory() gets the
            // assignment context for a pseudo assignment of an argument to a method
            // parameter.
            return;
          }
          throw new BugInCF(this.getClass() + ": tree not found");
        } else if (TreeUtils.typeOf(declarationTree) == null) {
          // org.checkerframework.framework.flow.CFAbstractTransfer.getValueFromFactory()
          // gets the assignment context for a pseudo assignment of an argument to a
          // method parameter.
          return;
        }

        atVariableDeclaration(type, declarationTree, (VariableElement) elt);
        return;

      default:
        // It's not a local variable (it might be METHOD, CONSTRUCTOR, CLASS, or INTERFACE,
        // for example), so there is nothing to do.
        break;
    }
  }

  /** Thrown when a non-parameter local variable is found. */
  @SuppressWarnings("serial")
  private static class FoundLocalVarException extends RuntimeException {
    /** Creates a FoundLocalVarException. */
    public FoundLocalVarException() {}
  }

  /**
   * Viewpoint-adapt all dependent type annotations to the method declaration, {@code
   * methodDeclTree}. This method changes occurrences of formal parameter names to the "#2" syntax,
   * and it removes expressions that contain other local variables.
   *
   * <p>If a Java expression in {@code atm} references local variables (other than formal
   * parameters), the expression is removed from the annotation. This could result in dependent type
   * annotations with empty lists of expressions. If this is a problem, a subclass can override
   * {@link #buildAnnotation(AnnotationMirror, Map)} to do something besides creating an annotation
   * with a empty list.
   *
   * @param atm type to viewpoint-adapt; is side-effected by this method
   * @param methodDeclTree the method declaration to which the annotations are viewpoint-adapted
   */
  public void delocalize(AnnotatedTypeMirror atm, MethodTree methodDeclTree) {
    if (!hasDependentType(atm)) {
      return;
    }

    TreePath pathToMethodDecl = factory.getPath(methodDeclTree);
    ExecutableElement methodElement = TreeUtils.elementFromDeclaration(methodDeclTree);
    List<FormalParameter> parameters = JavaExpression.getFormalParameters(methodElement);
    List<JavaExpression> paramsAsLocals =
        JavaExpression.getParametersAsLocalVariables(methodElement);

    StringToJavaExpression stringToJavaExpr =
        expression -> {
          JavaExpression javaExpr;
          try {
            javaExpr =
                StringToJavaExpression.atPath(expression, pathToMethodDecl, factory.getChecker());
          } catch (JavaExpressionParseException ex) {
            return null;
          }
          JavaExpressionConverter jec =
              new JavaExpressionConverter() {
                @Override
                protected JavaExpression visitLocalVariable(
                    LocalVariable localVarExpr, Void unused) {
                  int index = paramsAsLocals.indexOf(localVarExpr);
                  if (index == -1) {
                    throw new FoundLocalVarException();
                  }
                  return parameters.get(index);
                }
              };
          try {
            return jec.convert(javaExpr);
          } catch (FoundLocalVarException ex) {
            return null;
          }
        };
    if (debugStringToJavaExpression) {
      System.out.printf(
          "delocalize(%s, %s) created %s%n",
          atm, TreeUtils.toStringTruncated(methodDeclTree, 65), stringToJavaExpr);
    }
    convertAnnotatedTypeMirror(stringToJavaExpr, atm);
  }

  /**
   * Delocalizes dependent type annotations in {@code atm} so that they can be placed on the
   * declaration of the given method or constructor being invoked. Used by whole program inference
   * to infer dependent types for method/constructor parameters based on the actual arguments used
   * at call sites.
   *
   * @param atm the annotated type mirror to delocalize
   * @param invocationTree the method or constructor invocation
   * @param arguments the actual arguments to the method or constructor
   * @param receiver the actual receiver, if there was one; null if not
   * @param methodElt the declaration of the method or constructor being invoked
   */
  public void delocalizeAtCallsite(
      AnnotatedTypeMirror atm,
      Tree invocationTree,
      List<Node> arguments,
      @Nullable Node receiver,
      ExecutableElement methodElt) {

    // TODO: this method should also take the receiver parameter, if there was one at the
    // callsite, as an argument. Before it does, WPI needs to infer receiver types from
    // callsites.

    if (!hasDependentType(atm)) {
      return;
    }

    // For use in stringToJavaExpr below, to avoid re-computation. Especially
    // important for the TreePath, which is expensive to compute.
    List<JavaExpression> argsAsExprs = CollectionsPlume.mapList(LocalVariable::fromNode, arguments);
    JavaExpression receiverAsExpr = receiver == null ? null : LocalVariable.fromNode(receiver);
    TreePath path = factory.getPath(invocationTree);

    StringToJavaExpression stringToJavaExpr =
        stringExpr -> {
          JavaExpression expr =
              StringToJavaExpression.atPath(stringExpr, path, factory.getChecker());
          JavaExpressionConverter jec =
              new JavaExpressionConverter() {
                @Override
                public JavaExpression convert(JavaExpression javaExpr) {
                  // if javaExpr is an argument to the method,
                  // then return formal parameter expression.
                  int index = argsAsExprs.indexOf(javaExpr);
                  if (index != -1) {
                    return FormalParameter.getFormalParameters(methodElt).get(index);
                  }
                  if (javaExpr.equals(receiverAsExpr)) {
                    return new ThisReference(ElementUtils.enclosingTypeElement(methodElt).asType());
                  }
                  return super.convert(javaExpr);
                }

                // Local variables and this references at the call site that do not
                // correspond to any parameter need to be removed from the dependent
                // type annotation, which returning null from these methods
                // accomplishes.
                @Override
                public JavaExpression visitLocalVariable(LocalVariable local, Void unused) {
                  throw new FoundLocalVarException();
                }

                @Override
                public JavaExpression visitThisReference(ThisReference thisRef, Void unused) {
                  throw new FoundLocalVarException();
                }

                @Override
                public JavaExpression visitSuperReference(SuperReference superRef, Void unused) {
                  throw new FoundLocalVarException();
                }
              };

          try {
            return jec.convert(expr);
          } catch (FoundLocalVarException ex) {
            return null;
          }
        };

    convertAnnotatedTypeMirror(stringToJavaExpr, atm);
  }

  /**
   * Calls {@link #convertAnnotationMirror(StringToJavaExpression, AnnotationMirror)} on each
   * annotation mirror on type with {@code stringToJavaExpr}. And replaces the annotation with the
   * one created by {@code convertAnnotationMirror}, if it's not null. If it is null, the original
   * annotation is used. See {@link #convertAnnotationMirror(StringToJavaExpression,
   * AnnotationMirror)} for more details.
   *
   * @param stringToJavaExpr function to convert a string to a {@link JavaExpression}
   * @param type the type that is side-effected by this method
   */
  protected void convertAnnotatedTypeMirror(
      StringToJavaExpression stringToJavaExpr, AnnotatedTypeMirror type) {
    this.annotatedTypeReplacer.visit(type, anno -> convertAnnotationMirror(stringToJavaExpr, anno));
  }

  /**
   * Given an annotation {@code anno}, this method builds a new annotation with the Java expressions
   * transformed according to {@code stringToJavaExpr}. If {@code anno} is not a dependent type
   * annotation, {@code null} is returned.
   *
   * <p>If {@code stringToJavaExpr} returns {@code null}, then that expression is removed from the
   * returned annotation.
   *
   * <p>Instead of overriding this method, subclasses can override the following methods to change
   * the behavior of this class:
   *
   * <ul>
   *   <li>{@link #shouldPassThroughExpression(String)}: to control which expressions are skipped.
   *       If this method returns true, then the expression string is not parsed and is included in
   *       the new annotation unchanged.
   *   <li>{@link #transform(JavaExpression)}: make changes to the JavaExpression produced by {@code
   *       stringToJavaExpr}.
   *   <li>{@link #buildAnnotation(AnnotationMirror, Map)}: to change the annotation returned by
   *       this method.
   * </ul>
   *
   * @param stringToJavaExpr function that converts strings to {@code JavaExpression}s
   * @param anno annotation mirror
   * @return an annotation created by applying {@code stringToJavaExpr} to all expression strings in
   *     {@code anno}, or null if there would be no effect
   */
  public @Nullable AnnotationMirror convertAnnotationMirror(
      StringToJavaExpression stringToJavaExpr, AnnotationMirror anno) {
    if (!isExpressionAnno(anno)) {
      return null;
    }

    Map<ExecutableElement, List<JavaExpression>> newElements = new HashMap<>();
    for (ExecutableElement element : getListOfExpressionElements(anno)) {
      List<String> expressionStrings =
          AnnotationUtils.getElementValueArray(
              anno, element, String.class, Collections.emptyList());
      List<JavaExpression> javaExprs = new ArrayList<>(expressionStrings.size());
      newElements.put(element, javaExprs);
      for (String expression : expressionStrings) {
        JavaExpression result;
        if (shouldPassThroughExpression(expression)) {
          result = new PassThroughExpression(objectTM, expression);
        } else {
          try {
            result = stringToJavaExpr.toJavaExpression(expression);
          } catch (JavaExpressionParseException e) {
            result = createError(expression, e);
          }
        }

        if (result != null) {
          result = transform(result);
          javaExprs.add(result);
        }
      }
    }
    return buildAnnotation(anno, newElements);
  }

  /**
   * This method is for subclasses to override to change JavaExpressions in some way before they are
   * inserted into new annotations. This method is called after parsing and viewpoint-adaptation
   * have occurred. {@code javaExpr} may be a {@link DependentTypesHelper.PassThroughExpression}.
   *
   * <p>If {@code null} is returned then the expression is not added to the new annotation.
   *
   * <p>The default implementation returns the argument, but subclasses may override it.
   *
   * @param javaExpr a JavaExpression
   * @return a transformed JavaExpression or {@code null} if no transformation exists
   */
  protected @Nullable JavaExpression transform(JavaExpression javaExpr) {
    return javaExpr;
  }

  /**
   * Returns true if {@code expression} should be passed to the new annotation unchanged. If this
   * method returns true, the {@code expression} is not parsed.
   *
   * <p>The default implementation returns true if the {@code expression} is an expression error
   * according to {@link DependentTypesError#isExpressionError(String)}. Subclasses may override
   * this method to add additional logic.
   *
   * @param expression an expression string in a dependent types annotation
   * @return true if {@code expression} should be passed through unchanged to the new annotation
   */
  protected boolean shouldPassThroughExpression(String expression) {
    return DependentTypesError.isExpressionError(expression);
  }

  /**
   * Create a new annotation of the same type as {@code originalAnno} using the provided {@code
   * elementMap}.
   *
   * @param originalAnno the annotation passed to {@link
   *     #convertAnnotationMirror(StringToJavaExpression, AnnotationMirror)} (this method is a
   *     helper method for {@link #convertAnnotationMirror(StringToJavaExpression,
   *     AnnotationMirror)})
   * @param elementMap a mapping from element of {@code originalAnno} to {@code JavaExpression}s
   * @return an annotation created from {@code elementMap}
   */
  protected AnnotationMirror buildAnnotation(
      AnnotationMirror originalAnno, Map<ExecutableElement, List<JavaExpression>> elementMap) {
    AnnotationBuilder builder =
        new AnnotationBuilder(
            factory.getProcessingEnv(), AnnotationUtils.annotationName(originalAnno));
    builder.copyElementValuesFromAnnotation(originalAnno, elementMap.keySet());
    for (Map.Entry<ExecutableElement, List<JavaExpression>> entry : elementMap.entrySet()) {
      List<String> strings = CollectionsPlume.mapList(JavaExpression::toString, entry.getValue());
      builder.setValue(entry.getKey(), strings);
    }
    return builder.build();
  }

  /**
   * A {@link JavaExpression} that does not represent a {@link JavaExpression}, but rather allows an
   * expression string to be converted to a JavaExpression and then to a string without parsing.
   */
  static class PassThroughExpression extends Unknown {
    /** Some string. */
    public final String string;

    /**
     * Creates a PassThroughExpression.
     *
     * @param type some type
     * @param string the string to convert to a JavaExpression
     */
    public PassThroughExpression(TypeMirror type, String string) {
      super(type);
      this.string = string;
    }

    @Override
    public String toString() {
      return string;
    }
  }

  /**
   * Creates a {@link JavaExpression} representing the exception thrown when parsing {@code
   * expression}.
   *
   * @param expression an expression that caused {@code e} when parsed
   * @param e the exception thrown when parsing {@code expression}
   * @return a Java expression
   */
  protected PassThroughExpression createError(String expression, JavaExpressionParseException e) {
    return new PassThroughExpression(objectTM, new DependentTypesError(expression, e).toString());
  }

  /**
   * Creates a {@link JavaExpression} representing the error caused when parsing {@code expression}
   *
   * @param expression an expression that caused {@code error} when parsed
   * @param error the error message caused by {@code expression}
   * @return a Java expression
   */
  protected PassThroughExpression createError(String expression, String error) {
    return new PassThroughExpression(
        objectTM, new DependentTypesError(expression, error).toString());
  }

  /**
   * Applies the passed function to each annotation in the given {@link AnnotatedTypeMirror}. If the
   * function returns a non-null annotation, then the original annotation is replaced with the
   * result. If the function returns null, the original annotation is retained.
   */
  private static class AnnotatedTypeReplacer
      extends AnnotatedTypeScanner<Void, Function<AnnotationMirror, AnnotationMirror>> {

    @Override
    public Void visitTypeVariable(
        AnnotatedTypeMirror.AnnotatedTypeVariable type,
        Function<AnnotationMirror, AnnotationMirror> func) {
      if (visitedNodes.containsKey(type)) {
        return visitedNodes.get(type);
      }
      visitedNodes.put(type, null);

      // If the type variable has a primary annotation, then it is viewpoint-adapted before
      // this method is called.  The viewpoint-adapted primary annotation was already copied
      // to the upper and lower bounds.  These annotations cannot be viewpoint-adapted again,
      // so remove them, viewpoint-adapt any other annotations in the bound, and then add them
      // back.
      AnnotationMirrorSet primarys = type.getPrimaryAnnotations();
      type.getLowerBound().removePrimaryAnnotations(primarys);
      Void r = scan(type.getLowerBound(), func);
      type.getLowerBound().addAnnotations(primarys);
      visitedNodes.put(type, r);

      type.getUpperBound().removePrimaryAnnotations(primarys);
      r = scanAndReduce(type.getUpperBound(), func, r);
      type.getUpperBound().addAnnotations(primarys);
      visitedNodes.put(type, r);
      return r;
    }

    @Override
    protected Void scan(
        AnnotatedTypeMirror type, Function<AnnotationMirror, AnnotationMirror> func) {
      if (visitedNodes.containsKey(type)) {
        return null;
      }
      for (AnnotationMirror anno : new AnnotationMirrorSet(type.getPrimaryAnnotations())) {
        AnnotationMirror newAnno = func.apply(anno);
        if (newAnno != null) {
          // This code must remove and then add, rather than call `replace`, because a
          // type may have multiple annotations with the same class, but different
          // elements.  (This is a bug; see
          // https://github.com/typetools/checker-framework/issues/4451 .)
          // AnnotatedTypeMirror#replace only removes one annotation that is in the same
          // hierarchy as the passed argument.
          type.removePrimaryAnnotation(anno);
          type.addAnnotation(newAnno);
        }
      }
      return super.scan(type, func);
    }
  }

  //
  // Methods that check and report errors
  //

  /**
   * Reports an expression.unparsable error for each Java expression in the given type that is an
   * expression error string.
   *
   * @param atm annotated type to check for expression errors
   * @param errorTree the tree at which to report any found errors
   */
  public void checkTypeForErrorExpressions(AnnotatedTypeMirror atm, Tree errorTree) {
    if (!hasDependentAnnotations()) {
      return;
    }

    List<DependentTypesError> errors = expressionErrorCollector.visit(atm);
    if (errors.isEmpty()) {
      return;
    }

    // Report the error at the type rather than at the variable.
    if (errorTree instanceof VariableTree) {
      Tree typeTree = ((VariableTree) errorTree).getType();
      // Don't report the error at the type if the type is not present in source code.
      if (((JCTree) typeTree).getPreferredPosition() != -1) {
        ModifiersTree modifiers = ((VariableTree) errorTree).getModifiers();
        errorTree = typeTree;
        for (AnnotationTree annoTree : modifiers.getAnnotations()) {
          String annoString = annoTree.toString();
          for (String annoName : annoToElements.keySet()) {
            // TODO: Simple string containment seems too simplistic.  At least check for
            // a word boundary.
            if (annoString.contains(annoName)) {
              errorTree = annoTree;
              break;
            }
          }
        }
      }
    }
    reportErrors(errorTree, errors);
  }

  /**
   * Report the given errors as "expression.unparsable".
   *
   * @param errorTree where to report the errors
   * @param errors the errors to report
   */
  protected void reportErrors(Tree errorTree, List<DependentTypesError> errors) {
    SourceChecker checker = factory.getChecker();
    for (DependentTypesError dte : errors) {
      checker.reportError(errorTree, "expression.unparsable", dte.format());
    }
  }

  /**
   * Returns a list of {@link DependentTypesError}s for all the Java expression elements of the
   * annotation that are an error string as specified by DependentTypesError#isExpressionError.
   *
   * @param am an annotation
   * @return a list of {@link DependentTypesError}s for the error strings in the given annotation
   */
  private List<DependentTypesError> errorElements(AnnotationMirror am) {
    assert hasDependentAnnotations();

    List<DependentTypesError> errors = new ArrayList<>();

    for (ExecutableElement element : getListOfExpressionElements(am)) {
      // It's always an array, not a single value, because @JavaExpression may only be written
      // on an annotation element of type String[].
      List<String> value =
          AnnotationUtils.getElementValueArray(am, element, String.class, Collections.emptyList());
      for (String v : value) {
        if (DependentTypesError.isExpressionError(v)) {
          errors.add(DependentTypesError.unparse(v));
        }
      }
    }
    return errors;
  }

  /**
   * Reports a flowexpr.parse.error error for each Java expression in the given annotation that is
   * an expression error string.
   *
   * @param annotation annotation to check
   * @param errorTree location at which to issue errors
   */
  public void checkAnnotationForErrorExpressions(AnnotationMirror annotation, Tree errorTree) {
    if (!hasDependentAnnotations()) {
      return;
    }

    List<DependentTypesError> errors = errorElements(annotation);
    if (errors.isEmpty()) {
      return;
    }
    SourceChecker checker = factory.getChecker();
    for (DependentTypesError error : errors) {
      checker.reportError(errorTree, "flowexpr.parse.error", error);
    }
  }

  /**
   * Reports an expression.unparsable error for each Java expression in the given class declaration
   * AnnotatedTypeMirror that is an expression error string. Note that this reports errors in the
   * class declaration itself, not the body or extends/implements clauses.
   *
   * @param classTree class to check
   * @param type annotated type of the class
   */
  public void checkClassForErrorExpressions(ClassTree classTree, AnnotatedDeclaredType type) {
    if (!hasDependentAnnotations()) {
      return;
    }

    // TODO: check that invalid annotations in type variable bounds are properly
    // formatted. They are part of the type, but the output isn't nicely formatted.
    checkTypeForErrorExpressions(type, classTree);
  }

  /**
   * Reports an expression.unparsable error for each Java expression in the method declaration
   * AnnotatedTypeMirror that is an expression error string.
   *
   * @param methodDeclTree method to check
   * @param type annotated type of the method
   */
  public void checkMethodForErrorExpressions(
      MethodTree methodDeclTree, AnnotatedExecutableType type) {
    if (!hasDependentAnnotations()) {
      return;
    }

    // Parameters and receivers are checked by visitVariable
    // So only type parameters and return type need to be checked here.

    checkTypeVariablesForErrorExpressions(methodDeclTree, type);
    // Check return type
    if (type.getReturnType().getKind() != TypeKind.VOID) {
      AnnotatedTypeMirror returnType = factory.getMethodReturnType(methodDeclTree);
      Tree treeForError =
          TreeUtils.isConstructor(methodDeclTree) ? methodDeclTree : methodDeclTree.getReturnType();
      checkTypeForErrorExpressions(returnType, treeForError);
    }
  }

  /**
   * Reports an expression.unparsable error for each Java expression in the given type variables
   * that is an expression error string.
   *
   * @param tree a method declaration
   * @param methodType annotated type of the method
   */
  private void checkTypeVariablesForErrorExpressions(
      MethodTree tree, AnnotatedExecutableType methodType) {
    for (int i = 0; i < methodType.getTypeVariables().size(); i++) {
      AnnotatedTypeMirror atm = methodType.getTypeVariables().get(i);
      StringToJavaExpression stringToJavaExpr =
          stringExpr -> StringToJavaExpression.atMethodBody(stringExpr, tree, factory.getChecker());
      if (debugStringToJavaExpression) {
        System.out.printf(
            "checkTypeVariablesForErrorExpressions(%s, %s) created %s%n",
            tree, methodType, stringToJavaExpr);
      }
      convertAnnotatedTypeMirror(stringToJavaExpr, atm);
      checkTypeForErrorExpressions(atm, tree.getTypeParameters().get(i));
    }
  }

  /**
   * Returns true if {@code am} is an expression annotation, that is, an annotation whose element is
   * a Java expression.
   *
   * @param am an annotation
   * @return true if {@code am} is an expression annotation
   */
  private boolean isExpressionAnno(AnnotationMirror am) {
    if (!hasDependentAnnotations()) {
      return false;
    }
    return annoToElements.containsKey(AnnotationUtils.annotationName(am));
  }

  /**
   * Checks all dependent type annotations in the given annotated type to see if the expression
   * string is an error string as specified by DependentTypesError#isExpressionError. If the
   * annotated type has any errors, then a non-empty list of {@link DependentTypesError} is
   * returned.
   */
  private class ExpressionErrorCollector
      extends SimpleAnnotatedTypeScanner<List<DependentTypesError>, Void> {

    /** Create ExpressionErrorCollector. */
    private ExpressionErrorCollector() {
      super(
          (AnnotatedTypeMirror type, Void aVoid) -> {
            List<DependentTypesError> errors = new ArrayList<>();
            for (AnnotationMirror am : type.getPrimaryAnnotations()) {
              if (isExpressionAnno(am)) {
                errors.addAll(errorElements(am));
              }
            }
            return errors;
          },
          DependentTypesHelper::concatenate,
          Collections.emptyList());
    }
  }

  /**
   * Replaces a dependent type annotation with a parser error with the top qualifier in the
   * hierarchy.
   */
  protected class ErrorAnnoReplacer extends SimpleAnnotatedTypeScanner<Void, Void> {

    /**
     * Create an ErrorAnnoReplacer.
     *
     * @param qh the qualifier hierarchy
     */
    private ErrorAnnoReplacer(QualifierHierarchy qh) {
      super(
          (AnnotatedTypeMirror type, Void aVoid) -> {
            AnnotationMirrorSet replacementAnnos = null;
            for (AnnotationMirror am : type.getPrimaryAnnotations()) {
              if (isExpressionAnno(am) && !errorElements(am).isEmpty()) {
                if (replacementAnnos == null) {
                  replacementAnnos = new AnnotationMirrorSet();
                }
                replacementAnnos.add(qh.getTopAnnotation(am));
              }
            }

            if (replacementAnnos != null) {
              type.replaceAnnotations(replacementAnnos);
            }
            return null;
          });
    }
  }

  /**
   * Appends list2 to list1 in a new list. If either list is empty, returns the other. Thus, the
   * result may be aliased to one of the arguments and the client should only read, not write into,
   * the result.
   *
   * @param list1 a list
   * @param list2 a list
   * @return the lists, concatenated
   */
  private static List<DependentTypesError> concatenate(
      List<DependentTypesError> list1, List<DependentTypesError> list2) {
    if (list1.isEmpty()) {
      return list2;
    } else if (list2.isEmpty()) {
      return list1;
    }
    List<DependentTypesError> newList = new ArrayList<>(list1.size() + list2.size());
    newList.addAll(list1);
    newList.addAll(list2);
    return newList;
  }

  /**
   * The underlying type of the second parameter is the result of applying type variable
   * substitution to the visited type (the first parameter). This class copies annotations from the
   * visited type to the second formal parameter except for annotations on types that have been
   * substituted.
   */
  protected class ViewpointAdaptedCopier extends DoubleAnnotatedTypeScanner<Void> {

    /** Create a ViewpointAdaptedCopier. */
    private ViewpointAdaptedCopier() {}

    @Override
    protected Void scan(AnnotatedTypeMirror from, AnnotatedTypeMirror to) {
      if (from == null || to == null) {
        return null;
      }
      AnnotationMirrorSet replacements = new AnnotationMirrorSet();
      for (String vpa : annoToElements.keySet()) {
        AnnotationMirror anno = from.getPrimaryAnnotation(vpa);
        if (anno != null) {
          // Only replace annotations that might have been changed.
          replacements.add(anno);
        }
      }
      to.replaceAnnotations(replacements);

      if (from.getKind() != to.getKind()
          || (from.getKind() == TypeKind.TYPEVAR
              && TypesUtils.isCapturedTypeVariable(to.getUnderlyingType()))) {
        // If the underlying types don't match, then from has been substituted for a
        // from variable, so don't recur. The primary annotation was copied because
        // the from variable might have had a primary annotation at a use.
        // For example:
        // <T> void method(@KeyFor("a") T t) {...}
        // void use(@KeyFor("b") String s) {
        //      method(s);  // the from of the parameter should be @KeyFor("a") String
        // }
        return null;
      }
      return super.scan(from, to);
    }

    @Override
    protected Void defaultAction(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
      if (type1 == null || type2 == null) {
        return null;
      }
      if (type1.getKind() != type2.getKind()) {
        throw new BugInCF("Should be the same. type: %s p: %s ", type1, type2);
      }
      return null;
    }
  }

  /**
   * Returns true if {@code atm} has any dependent type annotations. If an annotated type does not
   * have a dependent type annotation, then no standardization or viewpoint adaption is performed.
   * (This check avoids calling time-intensive methods unless required.)
   *
   * @param atm a type
   * @return true if {@code atm} has any dependent type annotations
   */
  protected boolean hasDependentType(AnnotatedTypeMirror atm) {
    if (atm == null) {
      return false;
    }
    // This is a test about the type system.
    if (!hasDependentAnnotations()) {
      return false;
    }
    // This is a test about this specific type.
    return hasDependentTypeScanner.visit(atm);
  }

  /** Returns true if the passed AnnotatedTypeMirror has any dependent type annotations. */
  @SuppressWarnings("this-escape")
  private final AnnotatedTypeScanner<Boolean, Void> hasDependentTypeScanner =
      new SimpleAnnotatedTypeScanner<>(
          (type, __) -> {
            for (AnnotationMirror annotationMirror : type.getPrimaryAnnotations()) {
              if (isExpressionAnno(annotationMirror)) {
                return true;
              }
            }
            return false;
          },
          Boolean::logicalOr,
          false);
}
