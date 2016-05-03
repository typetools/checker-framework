package org.checkerframework.common.value;


import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

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

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.Result;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;

public class ReflectiveEvalutator {
    private BaseTypeChecker checker;
    private boolean reportWarnings;

    public ReflectiveEvalutator(BaseTypeChecker checker,
            ValueAnnotatedTypeFactory factory, boolean reportWarnings) {
        this.checker = checker;
        this.reportWarnings = reportWarnings;
    }

    /**
     *
     * @param allArgValues
     *            a list of list where the first list corresponds to all
     *            possible values for the first argument. Pass null to indicate
     *            that the method has no arguments.
     * @param receiverValues
     *            a list of possible receiver values. null indicates that the
     *            method has no receiver
     * @param tree
     *            location to report any errors
     * @return all possible values that the method may return.
     */
    public List<?> evaluteMethodCall(List<List<?>> allArgValues,
            List<?> receiverValues, MethodInvocationTree tree) {
        Method method = getMethodObject(tree);
        if (method == null) {
            return new ArrayList<>();
        }

        if (receiverValues == null) {
            // Method does not have a receiver
            // the first parameter of Method.invoke should be null
            receiverValues = Collections.singletonList(null);
        }

        List<Object[]> listOfArguments;
        if (allArgValues == null) {
            // Method does not have arguments
            listOfArguments = new ArrayList<Object[]>();
            listOfArguments.add(null);
        } else {
            // Find all possible argument sets
            listOfArguments = cartesianProduct(allArgValues,
                    allArgValues.size() - 1);
        }


        List<Object> results = new ArrayList<>();
        for (Object[] arguments : listOfArguments) {
            for (Object receiver : receiverValues) {
                try {
                    results.add(method.invoke(receiver, arguments));
                } catch (InvocationTargetException e) {
                    if (reportWarnings) {
                        checker.report(Result.warning(
                                "method.evaluation.exception", method, e
                                        .getTargetException().toString()), tree);
                    }
                    // Method evaluation will always fail, so don't bother
                    // trying again
                    return new ArrayList<Object>();

                } catch (ReflectiveOperationException e) {
                    if (reportWarnings) {
                        checker.report(Result.warning(
                                "method.evaluation.failed", method), tree);
                    }

                }
            }
        }
        return results;

    }

    /**
     * Method for reflectively obtaining a method object so it can (potentially)
     * be statically executed by the checker for constant propagation
     *
     * @return the Method object corresponding to the method being invoke in
     *         tree
     */
    private Method getMethodObject(MethodInvocationTree tree) {
        try {
            ExecutableElement ele = TreeUtils.elementFromUse(tree);
            Name clazz = TypesUtils.getQualifiedName((DeclaredType) ele
                    .getEnclosingElement().asType());
            List<Class<?>> paramClzz = getParameterClasses(tree, ele);
            Class<?> clzz = Class.forName(clazz.toString());
            Method method = clzz.getMethod(ele.getSimpleName().toString(),
                    paramClzz.toArray(new Class<?>[0]));
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            return method;
        } catch (ClassNotFoundException | UnsupportedClassVersionError e) {
            if (reportWarnings) {
                checker.report(
                        Result.warning("class.find.failed", (TreeUtils
                                .elementFromUse(tree)).getEnclosingElement()),
                        tree);
            }
            return null;

        } catch (NoSuchMethodException e) {
            // The class we attempted to getMethod from inside the
            // call to getMethodObject.
            Element classElem = TreeUtils.elementFromUse(tree)
                    .getEnclosingElement();

            if (classElem == null) {
                if (reportWarnings) {
                    checker.report(Result.warning("method.find.failed"), tree);
                }
            } else {
                if (reportWarnings) {
                    checker.report(Result.warning(
                            "method.find.failed.in.class", classElem), tree);
                }
            }
            return null;
        }
    }

    private List<Class<?>> getParameterClasses(Tree tree,
            ExecutableElement ele) throws ClassNotFoundException {
        List<? extends VariableElement> paramEles = ele.getParameters();
        List<Class<?>> paramClzz = new ArrayList<>();
        for (Element e : paramEles) {
            TypeMirror pType = ElementUtils.getType(e);
            paramClzz.add(ValueCheckerUtils.getClassFromType(pType));
        }
        return paramClzz;
    }

    private List<Object[]> cartesianProduct(List<List<?>> allArgValues,
            int whichArg) {
        List<?> argValues = allArgValues.get(whichArg);
        List<Object[]> tuples = new ArrayList<>();

        for (Object value : argValues) {
            if (whichArg == 0) {
                Object[] objects = new Object[allArgValues.size()];
                objects[0] = value;
                tuples.add(objects);
            } else {
                List<Object[]> lastTuples = cartesianProduct(allArgValues,
                        whichArg - 1);
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

    public Object evaluateStaticFieldAccess(String classname, String fieldName,
            MemberSelectTree tree) {
        try {
            Class<?> recClass = Class.forName(classname);
            Field field = recClass.getField(fieldName.toString());
            return field.get(recClass);

        } catch (ClassNotFoundException | UnsupportedClassVersionError e) {
            if (reportWarnings) {
                checker.report(Result.warning("class.find.failed", classname),
                        tree);
            }
            return null;
        } catch (ReflectiveOperationException e) {
            if (reportWarnings) {
                checker.report(Result.warning("field.access.failed", fieldName,
                        classname), tree);
            }
            return null;
        }
    }

    public List<?> evaluteConstrutorCall(ArrayList<List<?>> argValues,
            NewClassTree tree, TypeMirror typeToCreate) {
        try {
            // get the constructor
            Constructor<?> constructor =
                    getConstrutorObject(tree, typeToCreate);
            if (constructor == null) {
                return new ArrayList<>();
            }

            List<Object[]> listOfArguments;
            if (argValues == null) {
                // Method does not have arguments
                listOfArguments = new ArrayList<Object[]>();
                listOfArguments.add(null);
            } else {
                // Find all possible argument sets
                listOfArguments = cartesianProduct(argValues,
                        argValues.size() - 1);
            }

            List<Object> results = new ArrayList<>();
            for (Object[] arguments : listOfArguments) {
                try {
                    results.add(constructor.newInstance(arguments));
                } catch (ReflectiveOperationException e) {
                    if (reportWarnings) {
                        checker.report(
                                Result.warning("constructor.invocation.failed"),
                                tree);
                    }
                    return new ArrayList<Object>();
                }
                return results;
            }

        } catch (ReflectiveOperationException e) {
            if (reportWarnings) {
                checker.report(Result.warning("constructor.evaluation.failed"),
                        tree);
            }
        }
        return new ArrayList<>();
    }

    private Constructor<?> getConstrutorObject(NewClassTree tree, TypeMirror typeToCreate)
            throws ClassNotFoundException, NoSuchMethodException {
        ExecutableElement ele = TreeUtils.elementFromUse(tree);
        List<Class<?>> paramClasses = getParameterClasses(tree, ele);
        Class<?> recClass = boxPrimatives(ValueCheckerUtils.getClassFromType(typeToCreate));
        Constructor<?> constructor = recClass.getConstructor(paramClasses
                .toArray(new Class<?>[0]));
        return constructor;
    }
    /**
     * Returns the box primitive type if the passed type is an (unboxed)
     * primitive. Otherwise it returns the passed type
     */
    private static Class<?> boxPrimatives(Class<?> type) {
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
