package checkers.util;

import static com.sun.tools.javac.code.Kinds.ERR;
import static com.sun.tools.javac.code.Kinds.PCK;
import static com.sun.tools.javac.code.Kinds.TYP;
import static com.sun.tools.javac.code.Kinds.VAR;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
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
 * A Utility class to find symbols corresponding to string references
 */
public class Resolver {
    private final Resolve resolve;
    private final Names names;
    private final Trees trees;

    private final Method FIND_IDENT;
    private final Method FIND_METHOD;
    private final Method FIND_IDENT_IN_PACKAGE;
    private final Method FIND_MEMBER_TYPE;
    private final Method FIND_IDENT_IN_TYPE;

    public Resolver(ProcessingEnvironment env) {
        Context context = ((JavacProcessingEnvironment)env).getContext();
        this.resolve = Resolve.instance(context);
        this.names = Names.instance(context);
        this.trees = Trees.instance(env);

        try {
            this.FIND_IDENT = Resolve.class.getDeclaredMethod(
                    "findIdent",
                    Env.class, Name.class, int.class);
            FIND_IDENT.setAccessible(true);
            
            this.FIND_METHOD = Resolve.class.getDeclaredMethod("findMethod",
                    Env.class, Type.class, Name.class, List.class, List.class,
                    boolean.class, boolean.class, boolean.class);
            FIND_METHOD.setAccessible(true);

            this.FIND_IDENT_IN_PACKAGE = Resolve.class.getDeclaredMethod(
                    "findIdentInPackage",
                    Env.class, TypeSymbol.class, Name.class, int.class);
            FIND_IDENT_IN_PACKAGE.setAccessible(true);

            this.FIND_MEMBER_TYPE = Resolve.class.getDeclaredMethod(
                    "findMemberType",
                    Env.class,
                    Type.class,
                    Name.class,
                    TypeSymbol.class);
            FIND_MEMBER_TYPE.setAccessible(true);

            this.FIND_IDENT_IN_TYPE = Resolve.class.getDeclaredMethod(
                    "findIdentInType",
                    Env.class, Type.class, Name.class, int.class);
            this.FIND_IDENT_IN_TYPE.setAccessible(true);
        } catch (Exception e) {
            Error err = new AssertionError("Compiler 'Resolve' class doesn't contain required 'findXXX' method");
            err.initCause(e);
            throw err;
        }
    }

    /**
     * Finds the variable referenced in the passed {@code String}.
     *
     * This method may only operate on variable references, e.g. local
     * variables, parameters, fields.
     *
     * The reference string may be either an single Java identifier (e.g. "field")
     * or dot-separated identifiers (e.g. "Collections.EMPTY_LIST").
     *
     * The method adheres to all the rules of Java's scoping (while also
     * considering the imports) for name resolution.
     *
     * @param reference     the variable reference string
     * @param path          the tree path to the local scope
     * @return  the variable reference
     */
    public Element findVariable(String reference, TreePath path) {
        JavacScope scope = (JavacScope) trees.getScope(path);
        Env<AttrContext> env = scope.getEnv();

        if (!reference.contains(".")) {
            // Simple variable
            return wrapInvocation(
                    FIND_IDENT,
                    env, names.fromString(reference), Kinds.VAR);
        } else {
            int lastDot = reference.lastIndexOf('.');
            String expr = reference.substring(0, lastDot);
            String name = reference.substring(lastDot + 1);

            Element site = findType(expr, env);
            Name ident = names.fromString(name);

            return wrapInvocation(
                    FIND_IDENT_IN_TYPE,
                    env, site.asType(), ident, VAR);
        }
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
            TreePath path) {
        JavacScope scope = (JavacScope) trees.getScope(path);
        Env<AttrContext> env = scope.getEnv();

        Type site = (Type) receiverType;
        Name name = names.fromString(methodName);
        List<Type> argtypes = List.nil();
        List<Type> typeargtypes = List.nil();
        boolean allowBoxing = true;
        boolean useVarargs = false;
        boolean operator = true;
        return wrapInvocation(FIND_METHOD, env, site, name, argtypes,
                typeargtypes, allowBoxing, useVarargs, operator);
    }

    private Element findType(String reference, Env<AttrContext> env) {
        if (!reference.contains(".")) {
            // Simple variable
            return wrapInvocation(
                    FIND_IDENT,
                    env, names.fromString(reference), Kinds.TYP | Kinds.PCK);
        } else {
            int lastDot = reference.lastIndexOf(".");
            String expr = reference.substring(0, lastDot);
            String idnt = reference.substring(lastDot + 1);

            Symbol site = (Symbol)findType(expr, env);
            if (site.kind == ERR)
                return site;
            Name name = names.fromString(idnt);
            if (site.kind == PCK) {
                env.toplevel.packge = (PackageSymbol)site;
                return wrapInvocation(
                        FIND_IDENT_IN_PACKAGE,
                        env, site, name, TYP | PCK);
            } else {
                env.enclClass.sym = (ClassSymbol)site;
                return wrapInvocation(
                        FIND_MEMBER_TYPE,
                        env, site.asType(), name, site);
            }
        }
    }

    private Symbol wrapInvocation(Method method, Object... args) {
        try {
            return (Symbol)method.invoke(resolve, args);
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
