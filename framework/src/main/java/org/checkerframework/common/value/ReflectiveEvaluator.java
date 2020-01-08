package org.checkerframework.common.value;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiers;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.Result;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.PluginUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Evaluates expressions (such as method calls and field accesses) at compile time, to determine
 * whether they have compile-time constant values.
 */
public class ReflectiveEvaluator {

    /** The checker that is using this ReflectiveEvaluator. */
    private BaseTypeChecker checker;

    /**
     * Whether to report warnings about problems with evaluation. Controlled by the
     * -AreportEvalWarns command-line option.
     */
    private boolean reportWarnings;

    public ReflectiveEvaluator(
            BaseTypeChecker checker, ValueAnnotatedTypeFactory factory, boolean reportWarnings) {
        this.checker = checker;
        this.reportWarnings = reportWarnings;
    }

    /**
     * @param allArgValues a list of list where the first list corresponds to all possible values
     *     for the first argument. Pass null to indicate that the method has no arguments.
     * @param receiverValues a list of possible receiver values. null indicates that the method has
     *     no receiver.
     * @param tree location to report any errors
     * @return all possible values that the method may return, or null if the method could not be
     *     evaluated
     */
    public List<?> evaluateMethodCall(
            List<List<?>> allArgValues, List<?> receiverValues, MethodInvocationTree tree) {
        Method method = getMethodObject(tree);
        if (method == null) {
            return null;
        }

        if (receiverValues == null) {
            // Method does not have a receiver
            // the first parameter of Method.invoke should be null
            receiverValues = Collections.singletonList(null);
        }

        List<Object[]> listOfArguments;
        if (allArgValues == null) {
            // Method does not have arguments
            listOfArguments = new ArrayList<>();
            listOfArguments.add(null);
        } else {
            // Find all possible argument sets
            listOfArguments = cartesianProduct(allArgValues, allArgValues.size() - 1);
        }

        if (method.isVarArgs()) {
            List<Object[]> newList = new ArrayList<>();
            int numberOfParameters = method.getParameterTypes().length;
            for (Object[] args : listOfArguments) {
                newList.add(normalizeVararg(args, numberOfParameters));
            }
            listOfArguments = newList;
        }

        List<Object> results = new ArrayList<>();
        for (Object[] arguments : listOfArguments) {
            for (Object receiver : receiverValues) {
                try {
                    results.add(method.invoke(receiver, arguments));
                } catch (InvocationTargetException e) {
                    if (reportWarnings) {
                        checker.report(
                                Result.warning(
                                        "method.evaluation.exception",
                                        method,
                                        e.getTargetException().toString()),
                                tree);
                    }
                    // Method evaluation will always fail, so don't bother
                    // trying again
                    return null;
                } catch (ExceptionInInitializerError e) {
                    if (reportWarnings) {
                        checker.report(
                                Result.warning(
                                        "method.evaluation.exception",
                                        method,
                                        e.getCause().toString()),
                                tree);
                    }
                    return null;
                } catch (IllegalArgumentException e) {
                    if (reportWarnings) {
                        String args = PluginUtil.join(", ", arguments);
                        checker.report(
                                Result.warning(
                                        "method.evaluation.exception",
                                        method,
                                        e.getLocalizedMessage() + ": " + args),
                                tree);
                    }
                    return null;
                } catch (Throwable e) {
                    // Catch any exception thrown because they shouldn't crash the type checker.
                    if (reportWarnings) {
                        checker.report(Result.warning("method.evaluation.failed", method), tree);
                    }
                    return null;
                }
            }
        }
        return results;
    }

    /**
     * This method normalizes an array of arguments to a varargs method by changing the arguments
     * associated with the varargs parameter into an array.
     *
     * @param arguments an array of arguments for {@code method}. The length is at least {@code
     *     numberOfParameters - 1}.
     * @param numberOfParameters number of parameters of the vararg method
     * @return the length of the array is exactly {@code numberOfParameters}
     */
    private Object[] normalizeVararg(Object[] arguments, int numberOfParameters) {

        if (arguments == null) {
            // null means no arguments.  For varargs no arguments is an empty array.
            arguments = new Object[] {};
        }
        Object[] newArgs = new Object[numberOfParameters];
        Object[] varArgsArray;
        int numOfVarArgs = arguments.length - numberOfParameters + 1;
        if (numOfVarArgs > 0) {
            System.arraycopy(arguments, 0, newArgs, 0, numberOfParameters - 1);
            varArgsArray = new Object[numOfVarArgs];
            System.arraycopy(arguments, numberOfParameters - 1, varArgsArray, 0, numOfVarArgs);
        } else {
            System.arraycopy(arguments, 0, newArgs, 0, numberOfParameters - 1);
            varArgsArray = new Object[] {};
        }
        newArgs[numberOfParameters - 1] = varArgsArray;
        return newArgs;
    }

    /**
     * Method for reflectively obtaining a method object so it can (potentially) be statically
     * executed by the checker for constant propagation.
     *
     * @return the Method object corresponding to the method being invoke in tree
     */
    private Method getMethodObject(MethodInvocationTree tree) {
        try {
            ExecutableElement ele = TreeUtils.elementFromUse(tree);
            @DotSeparatedIdentifiers Name clazz =
                    TypesUtils.getQualifiedName((DeclaredType) ele.getEnclosingElement().asType());
            List<Class<?>> paramClzz = getParameterClasses(ele);
            @SuppressWarnings("signature") // https://tinyurl.com/cfissue/658 for Class.toString
            Class<?> clzz = Class.forName(clazz.toString());
            Method method =
                    clzz.getMethod(
                            ele.getSimpleName().toString(), paramClzz.toArray(new Class<?>[0]));
            @SuppressWarnings("deprecation") // TODO: find alternative
            boolean acc = method.isAccessible();
            if (!acc) {
                method.setAccessible(true);
            }
            return method;
        } catch (ClassNotFoundException | UnsupportedClassVersionError | NoClassDefFoundError e) {
            if (reportWarnings) {
                checker.report(
                        Result.warning(
                                "class.find.failed",
                                TreeUtils.elementFromUse(tree).getEnclosingElement()),
                        tree);
            }
            return null;

        } catch (Throwable e) {
            // The class we attempted to getMethod from inside the
            // call to getMethodObject.
            Element classElem = TreeUtils.elementFromUse(tree).getEnclosingElement();

            if (classElem == null) {
                if (reportWarnings) {
                    checker.report(Result.warning("method.find.failed"), tree);
                }
            } else {
                if (reportWarnings) {
                    checker.report(Result.warning("method.find.failed.in.class", classElem), tree);
                }
            }
            return null;
        }
    }

    private List<Class<?>> getParameterClasses(ExecutableElement ele)
            throws ClassNotFoundException {
        List<? extends VariableElement> paramEles = ele.getParameters();
        List<Class<?>> paramClzz = new ArrayList<>();
        for (Element e : paramEles) {
            TypeMirror pType = ElementUtils.getType(e);
            paramClzz.add(ValueCheckerUtils.getClassFromType(pType));
        }
        return paramClzz;
    }

    private List<Object[]> cartesianProduct(List<List<?>> allArgValues, int whichArg) {
        List<?> argValues = allArgValues.get(whichArg);
        List<Object[]> tuples = new ArrayList<>();

        for (Object value : argValues) {
            if (whichArg == 0) {
                Object[] objects = new Object[allArgValues.size()];
                objects[0] = value;
                tuples.add(objects);
            } else {
                List<Object[]> lastTuples = cartesianProduct(allArgValues, whichArg - 1);
                List<Object[]> copies = copy(lastTuples);
                for (Object[] copy : copies) {
                    copy[whichArg] = value;
                }
                tuples.addAll(copies);
            }
        }
        return tuples;
    }

    private List<Object[]> copy(List<Object[]> lastTuples) {
        List<Object[]> returnListOfLists = new ArrayList<>();
        for (Object[] list : lastTuples) {
            Object[] copy = Arrays.copyOf(list, list.length);
            returnListOfLists.add(copy);
        }
        return returnListOfLists;
    }

    /**
     * Return the value of a static field access. Return null if there is trouble.
     *
     * @param classname the class containing the field
     * @param fieldName the name of the field
     * @param tree the static field access in the program; used for diagnostics
     * @return the value of the static field access, or null if it cannot be determined
     */
    public Object evaluateStaticFieldAccess(
            @ClassGetName String classname, String fieldName, MemberSelectTree tree) {
        try {
            Class<?> recClass = Class.forName(classname);
            Field field = recClass.getField(fieldName);
            return field.get(recClass);

        } catch (ClassNotFoundException | UnsupportedClassVersionError | NoClassDefFoundError e) {
            if (reportWarnings) {
                checker.report(Result.warning("class.find.failed", classname), tree);
            }
            return null;
        } catch (Throwable e) {
            // Catch all exception so that the checker doesn't crash
            if (reportWarnings) {
                checker.report(Result.warning("field.access.failed", fieldName, classname), tree);
            }
            return null;
        }
    }

    public List<?> evaluteConstructorCall(
            ArrayList<List<?>> argValues, NewClassTree tree, TypeMirror typeToCreate) {
        Constructor<?> constructor;
        try {
            // get the constructor
            constructor = getConstructorObject(tree, typeToCreate);
        } catch (Throwable e) {
            // Catch all exception so that the checker doesn't crash
            if (reportWarnings) {
                checker.report(Result.warning("constructor.invocation.failed"), tree);
            }
            return null;
        }
        if (constructor == null) {
            return null;
        }

        List<Object[]> listOfArguments;
        if (argValues == null) {
            // Method does not have arguments
            listOfArguments = new ArrayList<>();
            listOfArguments.add(null);
        } else {
            // Find all possible argument sets
            listOfArguments = cartesianProduct(argValues, argValues.size() - 1);
        }

        List<Object> results = new ArrayList<>();
        for (Object[] arguments : listOfArguments) {
            try {
                results.add(constructor.newInstance(arguments));
            } catch (Throwable e) {
                if (reportWarnings) {
                    checker.report(
                            Result.warning(
                                    "constructor.evaluation.failed",
                                    typeToCreate,
                                    PluginUtil.join(", ", arguments)),
                            tree);
                }
                return null;
            }
        }
        return results;
    }

    private Constructor<?> getConstructorObject(NewClassTree tree, TypeMirror typeToCreate)
            throws ClassNotFoundException, NoSuchMethodException {
        ExecutableElement ele = TreeUtils.elementFromUse(tree);
        List<Class<?>> paramClasses = getParameterClasses(ele);
        Class<?> recClass = boxPrimitives(ValueCheckerUtils.getClassFromType(typeToCreate));
        Constructor<?> constructor = recClass.getConstructor(paramClasses.toArray(new Class<?>[0]));
        return constructor;
    }
    /**
     * Returns the box primitive type if the passed type is an (unboxed) primitive. Otherwise it
     * returns the passed type
     */
    private static Class<?> boxPrimitives(Class<?> type) {
        if (type == byte.class) {
            return Byte.class;
        } else if (type == short.class) {
            return Short.class;
        } else if (type == int.class) {
            return Integer.class;
        } else if (type == long.class) {
            return Long.class;
        } else if (type == float.class) {
            return Float.class;
        } else if (type == double.class) {
            return Double.class;
        } else if (type == char.class) {
            return Character.class;
        } else if (type == boolean.class) {
            return Boolean.class;
        }
        return type;
    }
}
