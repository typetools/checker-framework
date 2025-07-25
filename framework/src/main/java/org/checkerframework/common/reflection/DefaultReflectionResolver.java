package org.checkerframework.common.reflection;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.comp.Resolve.RecoveryLoadClass;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.reflection.qual.Invoke;
import org.checkerframework.common.reflection.qual.MethodVal;
import org.checkerframework.common.reflection.qual.NewInstance;
import org.checkerframework.common.reflection.qual.UnknownMethod;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeFactory.ParameterizedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Default implementation of {@link ReflectionResolver}. It resolves calls to:
 *
 * <ul>
 *   <li>{@link Method#invoke(Object, Object...)}
 *   <li>{@link Constructor#newInstance(Object...)}
 * </ul>
 *
 * @checker_framework.manual #reflection-resolution Reflection resolution
 */
// Error Prone is warning on calls to ClassSymbol#getEnclosedElements() because the JDK 11 return
// type is java.util.List, but the JDK 17 returns com.sun.tools.javac.util.List.
// All the calls in this class are to Symbol#getEnclosedElements(), so just suppress the warning.
@SuppressWarnings("ASTHelpersSuggestions")
public class DefaultReflectionResolver implements ReflectionResolver {

  /** Message prefix added to verbose reflection messages. */
  public static final String MSG_PREFEX_REFLECTION = "[Reflection] ";

  private final BaseTypeChecker checker;
  private final AnnotationProvider provider;
  private final ProcessingEnvironment processingEnv;
  private final Trees trees;
  private final boolean debug;

  public DefaultReflectionResolver(
      BaseTypeChecker checker, MethodValAnnotatedTypeFactory methodValProvider, boolean debug) {
    this.checker = checker;
    this.provider = methodValProvider;
    this.processingEnv = checker.getProcessingEnvironment();
    this.trees = Trees.instance(processingEnv);
    this.debug = debug;
  }

  @Override
  public boolean isReflectiveMethodInvocation(MethodInvocationTree tree) {
    ExecutableElement methodElt = TreeUtils.elementFromUse(tree);
    return provider.getDeclAnnotation(methodElt, Invoke.class) != null
        || provider.getDeclAnnotation(methodElt, NewInstance.class) != null;
  }

  @Override
  public ParameterizedExecutableType resolveReflectiveCall(
      AnnotatedTypeFactory factory,
      MethodInvocationTree tree,
      ParameterizedExecutableType origResult) {
    assert isReflectiveMethodInvocation(tree);
    if (provider.getDeclAnnotation(TreeUtils.elementFromUse(tree), NewInstance.class) != null) {
      return resolveConstructorCall(factory, tree, origResult);
    } else {
      return resolveMethodCall(factory, tree, origResult);
    }
  }

  /**
   * Resolves a call to {@link Method#invoke(Object, Object...)}.
   *
   * @param factory the {@link AnnotatedTypeFactory} of the underlying type system
   * @param tree the method invocation tree that has to be resolved
   * @param origResult the original result from {@code factory.methodFromUse}
   * @return the resolved type of the call
   */
  private ParameterizedExecutableType resolveMethodCall(
      AnnotatedTypeFactory factory,
      MethodInvocationTree tree,
      ParameterizedExecutableType origResult) {
    debugReflection("Try to resolve reflective method call: " + tree);
    List<MethodInvocationTree> possibleMethods = resolveReflectiveMethod(tree, factory);

    // Reflective method could not be resolved
    if (possibleMethods.isEmpty()) {
      return origResult;
    }

    Set<? extends AnnotationMirror> returnLub = null;
    Set<? extends AnnotationMirror> receiverGlb = null;
    Set<? extends AnnotationMirror> paramsGlb = null;

    // Iterate over all possible methods: lub return types, and glb receiver and parameter types
    for (MethodInvocationTree resolvedTree : possibleMethods) {
      debugReflection("Resolved method invocation: " + resolvedTree);
      if (!checkMethodArguments(resolvedTree)) {
        debugReflection("Spoofed tree's arguments did not match declaration" + resolvedTree);
        // Calling methodFromUse on these sorts of trees will cause an assertion to fail in
        // QualifierPolymorphism.PolyCollector.visitArray(...)
        continue;
      }
      ParameterizedExecutableType resolvedResult = factory.methodFromUse(resolvedTree);

      AnnotatedTypeMirror returnType = resolvedResult.executableType.getReturnType();
      TypeMirror returnTM = returnType.getUnderlyingType();

      // Lub return types
      returnLub = lub(returnLub, returnTM, returnType.getPrimaryAnnotations(), returnTM, factory);

      // Glb receiver types (actual method receiver is passed as first
      // argument to invoke(Object, Object[]))
      // Check for static methods whose receiver is null
      AnnotatedTypeMirror receiverType = resolvedResult.executableType.getReceiverType();
      if (receiverType == null) {
        // If the method is static the first argument to Method.invoke isn't used, so assume
        // top.
        if (receiverGlb == null) {
          receiverGlb =
              new AnnotationMirrorSet(factory.getQualifierHierarchy().getTopAnnotations());
        }
      } else {
        TypeMirror receiverTM = receiverType.getUnderlyingType();
        receiverGlb =
            glb(receiverGlb, receiverTM, receiverType.getPrimaryAnnotations(), receiverTM, factory);
      }

      // Glb parameter types.  All formal parameter types get combined together because
      // Method#invoke takes as argument an array of parameter types, so there is no way to
      // distinguish the types of different formal parameters.
      for (AnnotatedTypeMirror mirror : resolvedResult.executableType.getParameterTypes()) {
        TypeMirror mirrorTM = mirror.getUnderlyingType();
        paramsGlb = glb(paramsGlb, mirrorTM, mirror.getPrimaryAnnotations(), mirrorTM, factory);
      }
    }

    if (returnLub == null) {
      // None of the spoofed tree's arguments matched the declared method
      return origResult;
    }

    /*
     * Clear all original (return, receiver, parameter type) annotations and
     * set lub/glb annotations from resolved method(s)
     */

    // return value
    origResult.executableType.getReturnType().clearPrimaryAnnotations();
    origResult.executableType.getReturnType().addAnnotations(returnLub);

    // receiver type
    origResult.executableType.getParameterTypes().get(0).clearPrimaryAnnotations();
    origResult.executableType.getParameterTypes().get(0).addAnnotations(receiverGlb);

    // parameter types
    if (paramsGlb != null) {
      AnnotatedArrayType origArrayType =
          (AnnotatedArrayType) origResult.executableType.getParameterTypes().get(1);
      origArrayType.getComponentType().clearPrimaryAnnotations();
      origArrayType.getComponentType().addAnnotations(paramsGlb);
    }

    debugReflection("Resolved annotations: " + origResult.executableType);
    return origResult;
  }

  /**
   * Checks that arguments of a method invocation are consistent with their corresponding
   * parameters.
   *
   * @param resolvedTree a method invocation
   * @return true if arguments are consistent with parameters
   */
  private boolean checkMethodArguments(MethodInvocationTree resolvedTree) {
    // type.getKind() == actualType.getKind()
    ExecutableElement methodDecl = TreeUtils.elementFromUse(resolvedTree);
    return checkArguments(methodDecl.getParameters(), resolvedTree.getArguments());
  }

  /**
   * Checks that arguments of a constructor invocation are consistent with their corresponding
   * parameters.
   *
   * @param resolvedTree a constructor invocation
   * @return true if arguments are consistent with parameters
   */
  private boolean checkNewClassArguments(NewClassTree resolvedTree) {
    ExecutableElement methodDecl = TreeUtils.elementFromUse(resolvedTree);
    return checkArguments(methodDecl.getParameters(), resolvedTree.getArguments());
  }

  /**
   * Checks that argument are consistent with their corresponding parameter types. Common code used
   * by {@link #checkMethodArguments} and {@link #checkNewClassArguments}.
   *
   * @param parameters formal parameters
   * @param arguments actual arguments
   * @return true if argument are consistent with their corresponding parameter types
   */
  private boolean checkArguments(
      List<? extends VariableElement> parameters, List<? extends ExpressionTree> arguments) {
    if (parameters.size() != arguments.size()) {
      return false;
    }

    for (int i = 0; i < parameters.size(); i++) {
      VariableElement param = parameters.get(i);
      ExpressionTree arg = arguments.get(i);
      TypeMirror argType = TreeUtils.typeOf(arg);
      TypeMirror paramType = param.asType();
      if (argType.getKind() == TypeKind.ARRAY && paramType.getKind() != argType.getKind()) {
        return false;
      }
    }

    return true;
  }

  /**
   * Resolves a call to {@link Constructor#newInstance(Object...)}.
   *
   * @param factory the {@link AnnotatedTypeFactory} of the underlying type system
   * @param tree the method invocation tree (representing a constructor call) that has to be
   *     resolved
   * @param origResult the original result from {@code factory.methodFromUse}
   * @return the resolved type of the call
   */
  private ParameterizedExecutableType resolveConstructorCall(
      AnnotatedTypeFactory factory,
      MethodInvocationTree tree,
      ParameterizedExecutableType origResult) {
    debugReflection("Try to resolve reflective constructor call: " + tree);
    List<JCNewClass> possibleConstructors = resolveReflectiveConstructor(tree, factory);

    // Reflective constructor could not be resolved
    if (possibleConstructors.isEmpty()) {
      return origResult;
    }

    Set<? extends AnnotationMirror> returnLub = null;
    Set<? extends AnnotationMirror> paramsGlb = null;

    // Iterate over all possible constructors: lub return types and glb parameter types
    for (JCNewClass resolvedTree : possibleConstructors) {
      debugReflection("Resolved constructor invocation: " + resolvedTree);
      if (!checkNewClassArguments(resolvedTree)) {
        debugReflection("Spoofed tree's arguments did not match declaration" + resolvedTree);
        // Calling methodFromUse on these sorts of trees will cause an assertion to fail in
        // QualifierPolymorphism.PolyCollector.visitArray(...)
        continue;
      }
      ParameterizedExecutableType resolvedResult = factory.constructorFromUse(resolvedTree);
      AnnotatedExecutableType executableType = resolvedResult.executableType;
      AnnotatedTypeMirror returnType = executableType.getReturnType();
      TypeMirror returnTM = returnType.getUnderlyingType();

      // Lub return types
      returnLub = lub(returnLub, returnTM, returnType.getPrimaryAnnotations(), returnTM, factory);

      // Glb parameter types
      for (AnnotatedTypeMirror mirror : executableType.getParameterTypes()) {
        TypeMirror mirrorTM = mirror.getUnderlyingType();
        paramsGlb = glb(paramsGlb, mirrorTM, mirror.getPrimaryAnnotations(), mirrorTM, factory);
      }
    }
    if (returnLub == null) {
      // None of the spoofed tree's arguments matched the declared method
      return origResult;
    }
    /*
     * Clear all original (return, parameter type) annotations and set
     * lub/glb annotations from resolved constructors.
     */

    // return value
    origResult.executableType.getReturnType().clearPrimaryAnnotations();
    origResult.executableType.getReturnType().addAnnotations(returnLub);

    // parameter types
    if (paramsGlb != null) {
      AnnotatedArrayType origArrayType =
          (AnnotatedArrayType) origResult.executableType.getParameterTypes().get(0);
      origArrayType.getComponentType().clearPrimaryAnnotations();
      origArrayType.getComponentType().addAnnotations(paramsGlb);
    }

    debugReflection("Resolved annotations: " + origResult.executableType);
    return origResult;
  }

  /**
   * Resolves a reflective method call and returns all possible corresponding method calls.
   *
   * @param tree the MethodInvocationTree AST node that is to be resolved (Method.invoke)
   * @return a (potentially empty) list of all resolved MethodInvocationTrees
   */
  private List<MethodInvocationTree> resolveReflectiveMethod(
      MethodInvocationTree tree, AnnotatedTypeFactory reflectionFactory) {
    assert isReflectiveMethodInvocation(tree);
    JCMethodInvocation methodInvocation = (JCMethodInvocation) tree;

    Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
    TreeMaker make = TreeMaker.instance(context);
    TreePath path = reflectionFactory.getPath(tree);
    JavacScope scope = (JavacScope) trees.getScope(path);
    Env<AttrContext> env = scope.getEnv();

    boolean unknown = isUnknownMethod(tree);

    AnnotationMirror estimate = getMethodVal(tree);

    if (estimate == null) {
      debugReflection("MethodVal is unknown for: " + tree);
      debugReflection("UnknownMethod annotation: " + unknown);
      return Collections.emptyList();
    }

    debugReflection("MethodVal type system annotations: " + estimate);

    List<String> listClassNames =
        AnnotationUtils.getElementValueArray(
            estimate, reflectionFactory.methodValClassNameElement, String.class);
    List<String> listMethodNames =
        AnnotationUtils.getElementValueArray(
            estimate, reflectionFactory.methodValMethodNameElement, String.class);
    List<Integer> listParamLengths =
        AnnotationUtils.getElementValueArray(
            estimate, reflectionFactory.methodValParamsElement, Integer.class);
    assert listClassNames.size() == listMethodNames.size()
        && listClassNames.size() == listParamLengths.size();

    List<MethodInvocationTree> methodInvocations = new ArrayList<>();
    for (int i = 0; i < listClassNames.size(); ++i) {
      String className = listClassNames.get(i);
      String methodName = listMethodNames.get(i);
      int paramLength = listParamLengths.get(i);

      // Get receiver, which is always the first argument of the invoke method
      JCExpression receiver = methodInvocation.args.head;
      // The remaining list contains the arguments
      com.sun.tools.javac.util.List<JCExpression> args = methodInvocation.args.tail;

      // Resolve the Symbol(s) for the current method
      for (Symbol symbol : getMethodSymbolsfor(className, methodName, paramLength, env)) {
        if (!processingEnv.getTypeUtils().isSubtype(receiver.type, symbol.owner.type)) {
          continue;
        }
        if ((symbol.flags() & Flags.PUBLIC) > 0) {
          debugReflection("Resolved public method: " + symbol.owner + "." + symbol);
        } else {
          debugReflection("Resolved non-public method: " + symbol.owner + "." + symbol);
        }

        JCExpression method = TreeUtils.Select(make, receiver, symbol);
        args = getCorrectedArgs(symbol, args);
        // Build method invocation tree depending on the number of
        // parameters
        JCMethodInvocation syntTree = paramLength > 0 ? make.App(method, args) : make.App(method);

        // add method invocation tree to the list of possible method invocations
        methodInvocations.add(syntTree);
      }
    }
    return methodInvocations;
  }

  private com.sun.tools.javac.util.List<JCExpression> getCorrectedArgs(
      Symbol symbol, com.sun.tools.javac.util.List<JCExpression> args) {
    if (symbol.getKind() == ElementKind.METHOD) {
      MethodSymbol method = ((MethodSymbol) symbol);
      // neg means too many arg,
      // pos means to few args
      int diff = method.getParameters().size() - args.size();
      if (diff > 0) {
        // means too few args
        int origArgSize = args.size();
        for (int i = 0; i < diff; i++) {
          args = args.append(args.get(i % origArgSize));
        }
      } else if (diff < 0) {
        // means too many args
        com.sun.tools.javac.util.List<JCExpression> tmp = com.sun.tools.javac.util.List.nil();
        for (int i = 0; i < method.getParameters().size(); i++) {
          tmp = tmp.append(args.get(i));
        }
        args = tmp;
      }
    }
    return args;
  }

  /**
   * Resolves a reflective constructor call and returns all possible corresponding constructor
   * calls.
   *
   * @param tree the MethodInvocationTree AST node that is to be resolved (Constructor.newInstance)
   * @return a (potentially empty) list of all resolved MethodInvocationTrees
   */
  private List<JCNewClass> resolveReflectiveConstructor(
      MethodInvocationTree tree, AnnotatedTypeFactory reflectionFactory) {
    assert isReflectiveMethodInvocation(tree);
    JCMethodInvocation methodInvocation = (JCMethodInvocation) tree;

    Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
    TreeMaker make = TreeMaker.instance(context);
    TreePath path = reflectionFactory.getPath(tree);
    JavacScope scope = (JavacScope) trees.getScope(path);
    Env<AttrContext> env = scope.getEnv();

    AnnotationMirror estimate = getMethodVal(tree);

    if (estimate == null) {
      debugReflection("MethodVal is unknown for: " + tree);
      debugReflection("UnknownMethod annotation: " + isUnknownMethod(tree));
      return Collections.emptyList();
    }

    debugReflection("MethodVal type system annotations: " + estimate);

    List<String> listClassNames =
        AnnotationUtils.getElementValueArray(
            estimate, reflectionFactory.methodValClassNameElement, String.class);
    List<Integer> listParamLengths =
        AnnotationUtils.getElementValueArray(
            estimate, reflectionFactory.methodValParamsElement, Integer.class);
    assert listClassNames.size() == listParamLengths.size();

    List<JCNewClass> constructorInvocations = new ArrayList<>();
    for (int i = 0; i < listClassNames.size(); ++i) {
      String className = listClassNames.get(i);
      int paramLength = listParamLengths.get(i);

      // Resolve the Symbol for the current constructor
      for (Symbol symbol : getConstructorSymbolsfor(className, paramLength, env)) {
        debugReflection("Resolved constructor: " + symbol.owner + "." + symbol);

        JCNewClass syntTree = (JCNewClass) make.Create(symbol, methodInvocation.args);

        // add constructor invocation tree to the list of possible constructor invocations
        constructorInvocations.add(syntTree);
      }
    }
    return constructorInvocations;
  }

  private AnnotationMirror getMethodVal(MethodInvocationTree tree) {
    return provider.getAnnotationMirror(TreeUtils.getReceiverTree(tree), MethodVal.class);
  }

  /** Returns true if the receiver's type is @UnknownMethod. */
  private boolean isUnknownMethod(MethodInvocationTree tree) {
    return provider.getAnnotationMirror(TreeUtils.getReceiverTree(tree), UnknownMethod.class)
        != null;
  }

  /**
   * Returns set of MethodSymbols based on class name, method name, and parameter length.
   *
   * @param className the class that contains the method
   * @param methodName the method's name
   * @param paramLength the number of parameters
   * @param env the environment
   * @return the (potentially empty) set of corresponding method Symbol(s)
   */
  private List<Symbol> getMethodSymbolsfor(
      String className, String methodName, int paramLength, Env<AttrContext> env) {
    Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
    Resolve resolve = Resolve.instance(context);
    Names names = Names.instance(context);

    Symbol sym = getSymbol(className, env, names, resolve);
    if (!sym.exists()) {
      debugReflection("Unable to resolve class: " + className);
      return Collections.emptyList();
    }

    // The common case is probably that `result` is a singleton at method exit.
    List<Symbol> result = new ArrayList<>();
    ClassSymbol classSym = (ClassSymbol) sym;
    while (classSym != null) {
      // Upcast to Symbol to avoid bytecode incompatibility; see comment on the
      // @SuppressWarnings("ASTHelpersSuggestions") on the class.
      for (Symbol s : ((Symbol) classSym).getEnclosedElements()) {
        // check all member methods
        if (s.getKind() == ElementKind.METHOD) {
          // Check for method name and number of arguments
          if (names.fromString(methodName) == s.name
              && ((MethodSymbol) s).getParameters().size() == paramLength) {
            result.add(s);
          }
        }
      }
      if (!result.isEmpty()) {
        break;
      }
      Type t = classSym.getSuperclass();
      if (!t.hasTag(TypeTag.CLASS) || t.isErroneous()) {
        break;
      }
      classSym = (ClassSymbol) t.tsym;
    }
    if (result.isEmpty()) {
      debugReflection("Unable to resolve method: " + className + "@" + methodName);
    }
    return result;
  }

  /**
   * Returns set of Symbols for constructors based on class name and parameter length.
   *
   * @return the (potentially empty) set of corresponding constructor Symbol(s)
   */
  private List<Symbol> getConstructorSymbolsfor(
      String className, int paramLength, Env<AttrContext> env) {
    Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
    Resolve resolve = Resolve.instance(context);
    Names names = Names.instance(context);

    Symbol symClass = getSymbol(className, env, names, resolve);
    if (!symClass.exists()) {
      debugReflection("Unable to resolve class: " + className);
      return Collections.emptyList();
    }

    // TODO: Should this be used instead of the below??
    ElementFilter.constructorsIn(symClass.getEnclosedElements());

    // The common case is probably that there is one constructor of the given parameter length.
    List<Symbol> result = new ArrayList<>(2);
    for (Symbol s : symClass.getEnclosedElements()) {
      // Check all constructors
      if (s.getKind() == ElementKind.CONSTRUCTOR) {
        // Check for number of parameters
        if (((MethodSymbol) s).getParameters().size() == paramLength) {
          result.add(s);
        }
      }
    }
    if (result.isEmpty()) {
      debugReflection("Unable to resolve constructor!");
    }
    return result;
  }

  private Symbol getSymbol(String className, Env<AttrContext> env, Names names, Resolve resolve) {
    Method loadClass;
    try {
      loadClass =
          Resolve.class.getDeclaredMethod(
              "loadClass", Env.class, Name.class, RecoveryLoadClass.class);
      loadClass.setAccessible(true);
    } catch (SecurityException | NoSuchMethodException | IllegalArgumentException e) {
      // A problem with javac is serious and must be reported.
      throw new BugInCF("Error in obtaining reflective method.", e);
    }
    try {
      RecoveryLoadClass noRecovery = (e, n) -> null;
      return (Symbol) loadClass.invoke(resolve, env, names.fromString(className), noRecovery);
    } catch (SecurityException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException e) {
      // A problem with javac is serious and must be reported.
      throw new BugInCF("Error in invoking reflective method.", e);
    }
  }

  /**
   * Build lub of the two types (represented by sets {@code set1} and {@code set2}) using the
   * provided AnnotatedTypeFactory.
   *
   * <p>If {@code set1} is {@code null} or empty, {@code set2} is returned.
   *
   * @param set1 the first type
   * @param tm1 the type that is annotated by qualifier1
   * @param set2 the second type
   * @param tm2 the type that is annotated by qualifier2
   * @param atypeFactory the type factory
   * @return the lub of the two types
   */
  private Set<? extends AnnotationMirror> lub(
      @Nullable Set<? extends AnnotationMirror> set1,
      TypeMirror tm1,
      Set<? extends AnnotationMirror> set2,
      TypeMirror tm2,
      AnnotatedTypeFactory atypeFactory) {
    if (set1 == null || set1.isEmpty()) {
      return set2;
    } else {
      return atypeFactory.getQualifierHierarchy().leastUpperBoundsShallow(set1, tm1, set2, tm2);
    }
  }

  /**
   * Build glb of the two types (represented by sets {@code set1} and {@code set2}) using the
   * provided AnnotatedTypeFactory.
   *
   * <p>If {@code set1} is {@code null} or empty, {@code set2} is returned.
   *
   * @param set1 the first type
   * @param tm1 the type that is annotated by qualifier1
   * @param set2 the second type
   * @param tm2 the type that is annotated by qualifier2
   * @param atypeFactory the type factory
   * @return the glb of the two types
   */
  private Set<? extends AnnotationMirror> glb(
      @Nullable Set<? extends AnnotationMirror> set1,
      TypeMirror tm1,
      Set<? extends AnnotationMirror> set2,
      TypeMirror tm2,
      AnnotatedTypeFactory atypeFactory) {
    if (set1 == null || set1.isEmpty()) {
      return set2;
    } else {
      return atypeFactory.getQualifierHierarchy().greatestLowerBoundsShallow(set1, tm1, set2, tm2);
    }
  }

  /**
   * Reports debug information about the reflection resolution iff the corresponding debug flag is
   * set.
   *
   * @param msg the debug message
   */
  private void debugReflection(String msg) {
    if (debug) {
      checker.message(javax.tools.Diagnostic.Kind.NOTE, MSG_PREFEX_REFLECTION + msg);
    }
  }
}
