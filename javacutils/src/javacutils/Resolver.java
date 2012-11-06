package javacutils;

import static com.sun.tools.javac.code.Kinds.VAR;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

/**
 * A Utility class to find symbols corresponding to string references.
 */
public class Resolver {
    private final Resolve resolve;
    private final Names names;
    private final Trees trees;

    private final Method FIND_METHOD;
    private final Method FIND_IDENT_IN_TYPE;
    private final Method FIND_IDENT_IN_PACKAGE;
    private final Method FIND_TYPE;

    public Resolver(ProcessingEnvironment env) {
        Context context = ((JavacProcessingEnvironment) env).getContext();
        this.resolve = Resolve.instance(context);
        this.names = Names.instance(context);
        this.trees = Trees.instance(env);

        try {
            FIND_METHOD = Resolve.class.getDeclaredMethod("findMethod",
                    Env.class, Type.class, Name.class, List.class, List.class,
                    boolean.class, boolean.class, boolean.class);
            FIND_METHOD.setAccessible(true);

            FIND_IDENT_IN_TYPE = Resolve.class.getDeclaredMethod(
                    "findIdentInType", Env.class, Type.class, Name.class,
                    int.class);
            FIND_IDENT_IN_TYPE.setAccessible(true);

            FIND_IDENT_IN_PACKAGE = Resolve.class.getDeclaredMethod(
                    "findIdentInPackage", Env.class, TypeSymbol.class, Name.class,
                    int.class);
            FIND_IDENT_IN_PACKAGE.setAccessible(true);

            FIND_TYPE = Resolve.class.getDeclaredMethod(
                    "findType", Env.class, Name.class);
            FIND_TYPE.setAccessible(true);
        } catch (Exception e) {
            Error err = new AssertionError(
                    "Compiler 'Resolve' class doesn't contain required 'find' method");
            err.initCause(e);
            throw err;
        }
    }

    /**
     * Finds the field with name {@code name} in a given type.
     *
     * <p>
     * The method adheres to all the rules of Java's scoping (while also
     * considering the imports) for name resolution.
     *
     * @param name
     *            The name of the field.
     * @param type
     *            The type of the receiver (i.e., the type in which to look for
     *            the field).
     * @param path
     *            The tree path to the local scope.
     * @return The element for the field.
     */
    public Element findField(String name, TypeMirror type, TreePath path) {
        JavacScope scope = (JavacScope) trees.getScope(path);
        Env<AttrContext> env = scope.getEnv();
        return wrapInvocation(FIND_IDENT_IN_TYPE, env, type,
                names.fromString(name), VAR);
    }

    /**
     * Finds the class literal with name {@code name}.
     *
     * <p>
     * The method adheres to all the rules of Java's scoping (while also
     * considering the imports) for name resolution.
     *
     * @param name
     *            The name of the class.
     * @param path
     *            The tree path to the local scope.
     * @return The element for the class.
     */
    public Element findClass(String name, TreePath path) {
        JavacScope scope = (JavacScope) trees.getScope(path);
        Env<AttrContext> env = scope.getEnv();
        return wrapInvocation(FIND_TYPE, env, names.fromString(name));
    }

    /**
     * Finds the method element for a given name and list of expected parameter
     * types.
     *
     * <p>
     * The method adheres to all the rules of Java's scoping (while also
     * considering the imports) for name resolution.
     *
     * @param methodName
     *            Name of the method to find.
     * @param receiverType
     *            Type of the receiver of the method
     * @param path
     *            Tree path.
     * @return The method element (if found).
     */
    public Element findMethod(String methodName, TypeMirror receiverType,
            TreePath path, java.util.List<TypeMirror> argumentTypes) {
        JavacScope scope = (JavacScope) trees.getScope(path);
        Env<AttrContext> env = scope.getEnv();

        Type site = (Type) receiverType;
        Name name = names.fromString(methodName);
        List<Type> argtypes = List.nil();
        for (TypeMirror a : argumentTypes) {
            argtypes = argtypes.append((Type) a);
        }
        List<Type> typeargtypes = List.nil();
        boolean allowBoxing = true;
        boolean useVarargs = false;
        boolean operator = true;
        return wrapInvocation(FIND_METHOD, env, site, name, argtypes,
                typeargtypes, allowBoxing, useVarargs, operator);
    }

    private Symbol wrapInvocation(Method method, Object... args) {
        try {
            return (Symbol) method.invoke(resolve, args);
        } catch (IllegalAccessException e) {
            Error err = new AssertionError("Unexpected Reflection error");
            err.initCause(e);
            throw err;
        } catch (IllegalArgumentException e) {
            Error err = new AssertionError("Unexpected Reflection error");
            err.initCause(e);
            throw err;
        } catch (InvocationTargetException e) {
            Error err = new AssertionError("Unexpected Reflection error");
            err.initCause(e);
            throw err;
        }
    }
}
