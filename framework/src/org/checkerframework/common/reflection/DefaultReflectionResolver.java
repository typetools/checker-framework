package org.checkerframework.common.reflection;

import static com.sun.tools.javac.code.TypeTag.CLASS;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
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

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.reflection.qual.Invoke;
import org.checkerframework.common.reflection.qual.MethodVal;
import org.checkerframework.common.reflection.qual.NewInstance;
import org.checkerframework.common.reflection.qual.UnknownMethod;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

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
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

/**
 * Default implementation of {@link ReflectionResolver}, which resolves calls
 * to:
 * <ul>
 * <li>{@link Method#invoke(Object, Object...)}</li>
 * <li>{@link Constructor#newInstance(Object...)}</li>
 * </ul>
 *
 * @checker_framework.manual #reflection-resolution Reflection resolution
 *
 * @author rjust
 *
 */
public class DefaultReflectionResolver implements ReflectionResolver {
    // Message prefix added to verbose reflection messages
    public static final String MSG_PREFEX_REFLECTION = "[Reflection] ";

    private final BaseTypeChecker checker;
    private final AnnotationProvider provider;
    private final ProcessingEnvironment processingEnv;
    private final Trees trees;
    private final boolean debug;

    public DefaultReflectionResolver(BaseTypeChecker checker,
            MethodValAnnotatedTypeFactory methodValProvider, boolean debug) {
        this.checker = checker;
        this.provider = methodValProvider;
        this.processingEnv = checker.getProcessingEnvironment();
        this.trees = Trees.instance(processingEnv);
        this.debug = debug;
    }

    @Override
    public boolean isReflectiveMethodInvocation(MethodInvocationTree tree) {
        if ((provider.getDeclAnnotation(InternalUtils.symbol(tree),
                Invoke.class) != null || provider.getDeclAnnotation(
                        InternalUtils.symbol(tree), NewInstance.class) != null)) {
            return true;
        }
        // Called method is neither Method.invoke nor Constructor.newInstance
        return false;
    }

    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> resolveReflectiveCall(
            AnnotatedTypeFactory factory, MethodInvocationTree tree,
            Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> origResult) {
        assert isReflectiveMethodInvocation(tree);
        if (provider.getDeclAnnotation(InternalUtils.symbol(tree),
                NewInstance.class) != null) {
            return resolveConstructorCall(factory, tree, origResult);
        } else {
            return resolveMethodCall(factory, tree, origResult);
        }
    }

    /**
     * Resolves a call to {@link Method#invoke(Object, Object...)}.
     *
     * @param factory
     *            The {@link AnnotatedTypeFactory} of the underlying type
     *            system.
     * @param tree
     *            The method invocation tree that has to be resolved.
     * @param origResult
     *            The original result from <code>factory.methodFromUse</code>.
     */
    private Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> resolveMethodCall(
            AnnotatedTypeFactory factory, MethodInvocationTree tree,
            Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> origResult) {
        debugReflection("Try to resolve reflective method call: " + tree);
        List<MethodInvocationTree> possibleMethods = resolveReflectiveMethod(
                tree, factory);

        // Reflective method could not be resolved
        if (possibleMethods.size() == 0) {
            return origResult;
        }

        Set<? extends AnnotationMirror> returnLub = null;
        Set<? extends AnnotationMirror> receiverGlb = null;
        Set<? extends AnnotationMirror> paramsGlb = null;

        // Iterate over all possible methods: lub return types, and glb receiver
        // and parameter types
        for (MethodInvocationTree resolvedTree : possibleMethods) {
            debugReflection("Resolved method invocation: " + resolvedTree);
            if (!checkMethodAgruments(resolvedTree)) {
                debugReflection("Spoofed tree's arguments did not match declaration"
                        + resolvedTree.toString());
                // Calling methodFromUse on these sorts of trees will cause an
                // assertion to fail
                // in QualifierPolymorphism.PolyCollector.visitArray(...)
                continue;
            }
            Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> resolvedResult = factory
                    .methodFromUse(resolvedTree);

            // Lub return types
            returnLub = lub(returnLub, resolvedResult.first.getReturnType()
                    .getAnnotations(), factory);

            // Glb receiver types (actual method receiver is passed as first
            // argument to invoke(Object, Object[]))
            // Check for static methods whose receiver is null
            if (resolvedResult.first.getReceiverType() == null) {
                // If the method is static the first argument to Method.invoke isn't used,
                // so assume top.
                receiverGlb = glb(receiverGlb, factory.getQualifierHierarchy()
                        .getTopAnnotations(), factory);
            } else {
                receiverGlb = glb(receiverGlb, resolvedResult.first
                        .getReceiverType().getAnnotations(), factory);
            }

            // Glb parameter types.  All formal parameter types get
            // combined together because Method#invoke takes as argument an
            // array of parameter types, so there is no way to distinguish
            // the types of different formal parameters.
            for (AnnotatedTypeMirror mirror : resolvedResult.first
                    .getParameterTypes()) {
                paramsGlb = glb(paramsGlb, mirror.getAnnotations(), factory);
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
        origResult.first.getReturnType().clearAnnotations();
        origResult.first.getReturnType().addAnnotations(returnLub);

        // receiver type
        origResult.first.getParameterTypes().get(0).clearAnnotations();
        origResult.first.getParameterTypes().get(0).addAnnotations(receiverGlb);

        // parameter types
        if (paramsGlb != null) {
            AnnotatedArrayType origArrayType = (AnnotatedArrayType) origResult.first
                    .getParameterTypes().get(1);
            origArrayType.getComponentType().clearAnnotations();
            origArrayType.getComponentType().addAnnotations(paramsGlb);
        }

        debugReflection("Resolved annotations: " + origResult.first);
        return origResult;
    }

    private boolean checkMethodAgruments(MethodInvocationTree resolvedTree) {
        // type.getKind() == actualType.getKind()
        ExecutableElement methodDecl = TreeUtils.elementFromUse(resolvedTree);
        return checkAgruments(methodDecl.getParameters(),
                resolvedTree.getArguments());
    }

    private boolean checkNewClassArguments(NewClassTree resolvedTree) {
        ExecutableElement methodDecl = TreeUtils.elementFromUse(resolvedTree);
        return checkAgruments(methodDecl.getParameters(),
                resolvedTree.getArguments());

    }

    private boolean checkAgruments(List<? extends VariableElement> parameters,
            List<? extends ExpressionTree> arguments) {
        if (parameters.size() != arguments.size()) {
            return false;
        }

        for (int i = 0; i < parameters.size(); i++) {
            VariableElement param = parameters.get(i);
            ExpressionTree arg = arguments.get(i);
            TypeMirror argType = InternalUtils.typeOf(arg);
            TypeMirror paramType = param.asType();
            if (argType.getKind() == TypeKind.ARRAY
                    && paramType.getKind() != argType.getKind()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Resolves a call to {@link Constructor#newInstance(Object...)}.
     *
     * @param factory
     *            The {@link AnnotatedTypeFactory} of the underlying type
     *            system.
     * @param tree
     *            The method invocation tree (representing a constructor call)
     *            that has to be resolved.
     * @param origResult
     *            The original result from <code>factory.methodFromUse</code>.
     */
    private Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> resolveConstructorCall(
            AnnotatedTypeFactory factory, MethodInvocationTree tree,
            Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> origResult) {
        debugReflection("Try to resolve reflective constructor call: " + tree);
        List<JCNewClass> possibleConstructors = resolveReflectiveConstructor(
                tree, factory);

        // Reflective constructor could not be resolved
        if (possibleConstructors.size() == 0) {
            return origResult;
        }

        Set<? extends AnnotationMirror> returnLub = null;
        Set<? extends AnnotationMirror> paramsGlb = null;

        // Iterate over all possible constructors: lub return types and glb
        // parameter types
        for (JCNewClass resolvedTree : possibleConstructors) {
            debugReflection("Resolved constructor invocation: " + resolvedTree);
            if (!checkNewClassArguments(resolvedTree)) {
                debugReflection("Spoofed tree's arguments did not match declaration"
                        + resolvedTree.toString());
                // Calling methodFromUse on these sorts of trees will cause an
                // assertion to fail
                // in QualifierPolymorphism.PolyCollector.visitArray(...)
                continue;
            }
            Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> resolvedResult = factory
                    .constructorFromUse(resolvedTree);

            // Lub return types
            returnLub = lub(returnLub, resolvedResult.first.getReturnType()
                    .getAnnotations(), factory);

            // Glb parameter types
            for (AnnotatedTypeMirror mirror : resolvedResult.first
                    .getParameterTypes()) {
                paramsGlb = glb(paramsGlb, mirror.getAnnotations(), factory);
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
        origResult.first.getReturnType().clearAnnotations();
        origResult.first.getReturnType().addAnnotations(returnLub);

        // parameter types
        if (paramsGlb != null) {
            AnnotatedArrayType origArrayType = (AnnotatedArrayType) origResult.first
                    .getParameterTypes().get(0);
            origArrayType.getComponentType().clearAnnotations();
            origArrayType.getComponentType().addAnnotations(paramsGlb);
        }

        debugReflection("Resolved annotations: " + origResult.first);
        return origResult;
    }

    /**
     * Resolves a reflective method call and returns all possible corresponding
     * method calls.
     *
     * @param tree
     *            The MethodInvocationTree node that is to be resolved
     *            (Method.invoke)
     * @return a (potentially empty) list of all resolved MethodInvocationTrees
     */
    private List<MethodInvocationTree> resolveReflectiveMethod(
            MethodInvocationTree tree, AnnotatedTypeFactory reflectionFactory) {
        assert isReflectiveMethodInvocation(tree);
        JCMethodInvocation methodInvocation = (JCMethodInvocation) tree;

        Context context = ((JavacProcessingEnvironment) processingEnv)
                .getContext();
        TreeMaker make = TreeMaker.instance(context);
        TreePath path = reflectionFactory.getPath(tree);
        JavacScope scope = (JavacScope) trees.getScope(path);
        Env<AttrContext> env = scope.getEnv();

        List<MethodInvocationTree> methods = new ArrayList<>();

        boolean unknown = isUnknownMethod(tree);

        AnnotationMirror estimate = getMethodVal(tree);

        if (estimate == null) {
            debugReflection("MethodVal is unknown for: " + tree);
            debugReflection("UnknownMethod annotation: " + unknown);
            return methods;
        }

        debugReflection("MethodVal type system annotations: " + estimate);

        List<String> listClassNames = AnnotationUtils.getElementValueArray(
                estimate, "className", String.class, true);
        List<String> listMethodNames = AnnotationUtils.getElementValueArray(
                estimate, "methodName", String.class, true);
        List<Integer> listParamLenghts = AnnotationUtils.getElementValueArray(
                estimate, "params", Integer.class, true);

        assert listClassNames.size() == listMethodNames.size()
                && listClassNames.size() == listParamLenghts.size();
        for (int i = 0; i < listClassNames.size(); ++i) {
            String className = listClassNames.get(i);
            String methodName = listMethodNames.get(i);
            int paramLength = listParamLenghts.get(i);

            // Get receiver, which is always the first argument of the invoke
            // method
            JCExpression receiver = methodInvocation.args.head;
            // The remaining list contains the arguments
            com.sun.tools.javac.util.List<JCExpression> args = methodInvocation.args.tail;

            // Resolve the Symbol(s) for the current method
            for (Symbol symbol : getMethodSymbolsfor(className, methodName,
                    paramLength, env)) {
                if ((symbol.flags() & Flags.PUBLIC) > 0) {
                    debugReflection("Resolved public method: " + symbol.owner
                            + "." + symbol);
                } else {
                    debugReflection("Resolved non-public method: "
                            + symbol.owner + "." + symbol);
                }

                JCExpression method = make.Select(receiver, symbol);
                args = getCorrectedArgs(symbol, args);
                // Build method invocation tree depending on the number of
                // parameters
                JCMethodInvocation syntTree = paramLength > 0 ? make.App(
                        method, args) : make.App(method);

                // add method invocation tree to the list of possible methods
                methods.add(syntTree);
            }

        }
        return methods;
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
                com.sun.tools.javac.util.List<JCExpression> tmp = com.sun.tools.javac.util.List
                        .nil();
                for (int i = 0; i < method.getParameters().size(); i++) {
                    tmp = tmp.append(args.get(i));
                }
                args = tmp;
            }

        }
        return args;
    }

    /**
     * Resolves a reflective constructor call and returns all possible
     * corresponding constructor calls.
     *
     * @param tree
     *            The MethodInvocationTree node that is to be resolved
     *            (Constructor.newInstance)
     * @return a (potentially empty) list of all resolved MethodInvocationTrees
     */
    private List<JCNewClass> resolveReflectiveConstructor(
            MethodInvocationTree tree, AnnotatedTypeFactory reflectionFactory) {
        assert isReflectiveMethodInvocation(tree);
        JCMethodInvocation methodInvocation = (JCMethodInvocation) tree;

        Context context = ((JavacProcessingEnvironment) processingEnv)
                .getContext();
        TreeMaker make = TreeMaker.instance(context);
        TreePath path = reflectionFactory.getPath(tree);
        JavacScope scope = (JavacScope) trees.getScope(path);
        Env<AttrContext> env = scope.getEnv();

        List<JCNewClass> constructors = new ArrayList<>();

        AnnotationMirror estimate = getMethodVal(tree);

        if (estimate == null) {
            debugReflection("MethodVal is unknown for: " + tree);
            debugReflection("UnknownMethod annotation: " + isUnknownMethod(tree));
            return constructors;
        }

        debugReflection("MethodVal type system annotations: " + estimate);

        List<String> listClassNames = AnnotationUtils.getElementValueArray(
                estimate, "className", String.class, true);
        List<Integer> listParamLenghts = AnnotationUtils.getElementValueArray(
                estimate, "params", Integer.class, true);

        assert listClassNames.size() == listParamLenghts.size();
        for (int i = 0; i < listClassNames.size(); ++i) {
            String className = listClassNames.get(i);
            int paramLength = listParamLenghts.get(i);

            // Resolve the Symbol for the current constructor
            for (Symbol symbol : getConstructorSymbolsfor(className,
                    paramLength, env)) {
                debugReflection("Resolved constructor: " + symbol.owner + "."
                        + symbol);

                JCNewClass syntTree = (JCNewClass) make.Create(symbol,
                        methodInvocation.args);

                // add constructor invocation tree to the list of possible
                // constructors
                constructors.add(syntTree);
            }
        }
        return constructors;
    }

    private AnnotationMirror getMethodVal(MethodInvocationTree tree) {
        return provider.getAnnotationMirror(
                TreeUtils.getReceiverTree(tree), MethodVal.class);
    }

    private boolean isUnknownMethod(MethodInvocationTree tree) {
        return provider.getAnnotationMirror(
                TreeUtils.getReceiverTree(tree), UnknownMethod.class) != null;
    }

    /**
     * Get set of MethodSymbols based on class name, method name, and parameter
     * length.
     *
     * @return the (potentially empty) set of corresponding method Symbol(s)
     */
    private List<Symbol> getMethodSymbolsfor(String className,
            String methodName, int paramLength, Env<AttrContext> env) {
        Context context = ((JavacProcessingEnvironment) processingEnv)
                .getContext();
        Resolve resolve = Resolve.instance(context);
        Names names = Names.instance(context);

        List<Symbol> result = new LinkedList<>();
        try {
            Method loadClass = Resolve.class.getDeclaredMethod("loadClass",
                    Env.class, Name.class);
            loadClass.setAccessible(true);
            Symbol sym = (Symbol) loadClass.invoke(resolve, env,
                    names.fromString(className));
            if (!sym.exists()) {
                debugReflection("Unable to resolve class: " + className);
                return Collections.emptyList();
            }

            ClassSymbol classSym = (ClassSymbol) sym;
            while (classSym != null) {
                for (Symbol s : classSym.getEnclosedElements()) {
                    // check all member methods
                    if (s.getKind() == ElementKind.METHOD) {
                        // Check for method name and number of arguments
                        if (names.fromString(methodName).equals(s.name)
                                && ((MethodSymbol) s).getParameters().size() == paramLength) {
                            result.add(s);
                        }
                    }
                }
                if (result.size() != 0) {
                    break;
                }
                Type t = classSym.getSuperclass();
                if (!t.hasTag(CLASS) || t.isErroneous()) {
                    break;
                }
                classSym = (ClassSymbol) t.tsym;
            }
            if (result.size() == 0) {
                debugReflection("Unable to resolve method: " + className + "@"
                        + methodName);
            }
        } catch (SecurityException | NoSuchMethodException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            debugReflection("Exception during resolution of reflective method: "
                    + e.getMessage());
            return Collections.emptyList();
        }
        return result;
    }

    /**
     * Get set of Symbols for constructors based on class name and parameter
     * length.
     *
     * @return the (potentially empty) set of corresponding constructor
     *         Symbol(s)
     */
    private List<Symbol> getConstructorSymbolsfor(String className,
            int paramLength, Env<AttrContext> env) {
        Context context = ((JavacProcessingEnvironment) processingEnv)
                .getContext();
        Resolve resolve = Resolve.instance(context);
        Names names = Names.instance(context);

        List<Symbol> result = new LinkedList<>();
        try {
            Method loadClass = Resolve.class.getDeclaredMethod("loadClass",
                    Env.class, Name.class);
            loadClass.setAccessible(true);
            Symbol symClass = (Symbol) loadClass.invoke(resolve, env,
                    names.fromString(className));
            if (!symClass.exists()) {
                debugReflection("Unable to resolve class: " + className);
                return Collections.emptyList();
            }

            ElementFilter.constructorsIn(symClass.getEnclosedElements());

            for (Symbol s : symClass.getEnclosedElements()) {
                // Check all constructors
                if (s.getKind() == ElementKind.CONSTRUCTOR) {
                    // Check for number of parameters
                    if (((MethodSymbol) s).getParameters().size() == paramLength) {
                        result.add(s);
                    }
                }
            }
            if (result.size() == 0) {
                debugReflection("Unable to resolve constructor!");
            }
        } catch (SecurityException | NoSuchMethodException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            debugReflection("Exception during resolution of reflective constructor: "
                    + e.getMessage());
            return Collections.emptyList();
        }
        return result;
    }

    /**
     * Build lub of the two types (represented by sets <code>set1</code>
     * and <code>set2</code>) using the provided AnnotatedTypeFactory.
     * <p>
     * If <code>set1</code> is <code>null</code> or empty, <code>set2</code> is
     * returned.
     */
    private Set<? extends AnnotationMirror> lub(
            Set<? extends AnnotationMirror> set1,
            Set<? extends AnnotationMirror> set2, AnnotatedTypeFactory factory) {
        if (set1 == null || set1.size() == 0) {
            return set2;
        } else {
            return factory.getQualifierHierarchy().leastUpperBounds(set1, set2);
        }
    }

    /**
     * Build glb of the two types (represented by sets <code>set1</code>
     * and <code>set2</code>) using the provided AnnotatedTypeFactory.
     * <p>
     * If <code>set1</code> is <code>null</code> or empty, <code>set2</code> is
     * returned.
     */
    private Set<? extends AnnotationMirror> glb(
            Set<? extends AnnotationMirror> set1,
            Set<? extends AnnotationMirror> set2, AnnotatedTypeFactory factory) {
        if (set1 == null || set1.size() == 0) {
            return set2;
        } else {
            return factory.getQualifierHierarchy().greatestLowerBounds(set1,
                    set2);
        }
    }

    /**
     * Reports debug information about the reflection resolution iff the
     * corresponding debug flag is set
     *
     * @param msg
     *            the debug message
     */
    private void debugReflection(String msg) {
        if (debug) {
            checker.message(javax.tools.Diagnostic.Kind.NOTE,
                    MSG_PREFEX_REFLECTION + msg);
        }
    }
}
