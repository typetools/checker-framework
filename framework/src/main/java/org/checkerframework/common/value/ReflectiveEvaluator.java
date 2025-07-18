package org.checkerframework.common.value;

import com.sun.source.tree.ExpressionTree;
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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.CanonicalNameOrEmpty;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.CollectionsPlume;
import org.plumelib.util.StringsPlume;

// The use of reflection in ReflectiveEvaluator is troubling.
// A static analysis such as the Checker Framework should always use compiler APIs, never
// reflection, to obtain values, for these reasons:
//  * The program being compiled is not necessarily on the classpath nor the processorpath.
//  * There might even be a different class of the same fully-qualified name on the processorpath.
//  * Loading a class can have side effects (say, caused by static initializers).
//
// A better implementation strategy would be to use BeanShell or the like to perform evaluation.

/**
 * Evaluates expressions (such as method calls and field accesses) at compile time, to determine
 * whether they have compile-time constant values.
 */
public class ReflectiveEvaluator {

  /** The checker that is using this ReflectiveEvaluator. */
  private final BaseTypeChecker checker;

  /**
   * Whether to report warnings about problems with evaluation. Controlled by the -AreportEvalWarns
   * command-line option.
   */
  private final boolean reportWarnings;

  /**
   * Create a new ReflectiveEvaluator.
   *
   * @param checker the BaseTypeChecker
   * @param factory the annotated type factory
   * @param reportWarnings if true, report warnings about problems with evaluation
   */
  public ReflectiveEvaluator(
      BaseTypeChecker checker, ValueAnnotatedTypeFactory factory, boolean reportWarnings) {
    this.checker = checker;
    this.reportWarnings = reportWarnings;
  }

  /**
   * Returns all possible values that the method may return, or null if the method could not be
   * evaluated.
   *
   * @param allArgValues a list of lists where the first list corresponds to all possible values for
   *     the first argument. Pass null to indicate that the method has no arguments.
   * @param receiverValues a list of possible receiver values. null indicates that the method has no
   *     receiver.
   * @param tree location to report any errors
   * @return all possible values that the method may return, or null if the method could not be
   *     evaluated
   */
  public @Nullable List<?> evaluateMethodCall(
      @Nullable List<List<?>> allArgValues,
      @Nullable List<?> receiverValues,
      MethodInvocationTree tree) {
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
      listOfArguments = Collections.singletonList(null);
    } else {
      // Find all possible argument sets
      listOfArguments = cartesianProduct(allArgValues, allArgValues.size() - 1);
    }

    if (method.isVarArgs()) {
      int numberOfParameters = method.getParameterTypes().length;
      listOfArguments =
          CollectionsPlume.mapList(
              (Object[] args) -> normalizeVararg(args, numberOfParameters), listOfArguments);
    }

    List<Object> results = new ArrayList<>(listOfArguments.size());
    for (Object[] arguments : listOfArguments) {
      for (Object receiver : receiverValues) {
        try {
          results.add(method.invoke(receiver, arguments));
        } catch (InvocationTargetException e) {
          if (reportWarnings) {
            checker.reportWarning(
                tree, "method.evaluation.exception", method, e.getTargetException().toString());
          }
          // Method evaluation will always fail, so don't bother
          // trying again
          return null;
        } catch (ExceptionInInitializerError e) {
          if (reportWarnings) {
            checker.reportWarning(
                tree, "method.evaluation.exception", method, e.getCause().toString());
          }
          return null;
        } catch (IllegalArgumentException e) {
          if (reportWarnings) {
            String args = StringsPlume.join(", ", arguments);
            checker.reportWarning(
                tree, "method.evaluation.exception", method, e.getLocalizedMessage() + ": " + args);
          }
          return null;
        } catch (Throwable e) {
          // Catch any exception thrown because they shouldn't crash the type checker.
          if (reportWarnings) {
            checker.reportWarning(tree, "method.evaluation.failed", method);
          }
          return null;
        }
      }
    }
    return results;
  }

  /** An empty Object array. */
  private static Object[] emptyObjectArray = new Object[] {};

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
      arguments = emptyObjectArray;
    }
    Object[] newArgs = new Object[numberOfParameters];
    Object[] varArgsArray;
    int numOfVarargs = arguments.length - numberOfParameters + 1;
    if (numOfVarargs > 0) {
      System.arraycopy(arguments, 0, newArgs, 0, numberOfParameters - 1);
      varArgsArray = new Object[numOfVarargs];
      System.arraycopy(arguments, numberOfParameters - 1, varArgsArray, 0, numOfVarargs);
    } else {
      System.arraycopy(arguments, 0, newArgs, 0, numberOfParameters - 1);
      varArgsArray = emptyObjectArray;
    }
    newArgs[numberOfParameters - 1] = varArgsArray;
    return newArgs;
  }

  /**
   * Method for reflectively obtaining a method object so it can (potentially) be statically
   * executed by the checker for constant propagation.
   *
   * @param tree a method invocation tree
   * @return the Method object corresponding to the method invocation tree
   */
  private @Nullable Method getMethodObject(MethodInvocationTree tree) {
    ExecutableElement ele = TreeUtils.elementFromUse(tree);
    List<Class<?>> paramClasses = null;
    try {
      @CanonicalNameOrEmpty String className =
          TypesUtils.getQualifiedName((DeclaredType) ele.getEnclosingElement().asType());
      paramClasses = getParameterClasses(ele);
      @SuppressWarnings("signature") // https://tinyurl.com/cfissue/658 for Class.toString
      Class<?> clazz = Class.forName(className.toString());
      Method method =
          clazz.getMethod(ele.getSimpleName().toString(), paramClasses.toArray(new Class<?>[0]));
      @SuppressWarnings("deprecation") // TODO: find alternative
      boolean acc = method.isAccessible();
      if (!acc) {
        method.setAccessible(true);
      }
      return method;
    } catch (ClassNotFoundException | UnsupportedClassVersionError | NoClassDefFoundError e) {
      if (reportWarnings) {
        checker.reportWarning(tree, "class.find.failed", ele.getEnclosingElement());
      }
      return null;

    } catch (Throwable e) {
      // The class we attempted to getMethod from inside the call to getMethodObject.
      Element classElem = ele.getEnclosingElement();

      if (classElem == null) {
        if (reportWarnings) {
          checker.reportWarning(tree, "method.find.failed", ele.getSimpleName(), paramClasses);
        }
      } else {
        if (reportWarnings) {
          checker.reportWarning(
              tree, "method.find.failed.in.class", ele.getSimpleName(), paramClasses, classElem);
        }
      }
      return null;
    }
  }

  /**
   * Returns the classes of the given method's formal parameters.
   *
   * @param ele a method or constructor
   * @return the classes of the given method's formal parameters
   * @throws ClassNotFoundException if the class cannot be found
   */
  private List<Class<?>> getParameterClasses(ExecutableElement ele) throws ClassNotFoundException {
    return CollectionsPlume.mapList(
        (Element e) -> TypesUtils.getClassFromType(ElementUtils.getType(e)), ele.getParameters());
  }

  /**
   * Returns all combinations of the elements of the given lists.
   *
   * @param allArgValues the lists whose cartesian product to form
   * @param whichArg pass {@code allArgValues.size() - 1}
   * @return all combinations of the elements of the given lists
   */
  private List<Object[]> cartesianProduct(List<List<?>> allArgValues, int whichArg) {
    List<?> argValues = allArgValues.get(whichArg);
    List<Object[]> tuples = new ArrayList<>(argValues.size());

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

  /**
   * Returns a depth-2 copy of the given list. In the returned value, the list and the arrays in it
   * are new, but the elements of the arrays are shared with the argument.
   *
   * @param lastTuples a list of arrays
   * @return a depth-2 copy of the given list
   */
  private List<Object[]> copy(List<Object[]> lastTuples) {
    return CollectionsPlume.mapList(
        (Object[] list) -> Arrays.copyOf(list, list.length), lastTuples);
  }

  /**
   * Return the value of a static field access. Return null if accessing the field reflectively
   * fails.
   *
   * @param classname the class containing the field
   * @param fieldName the name of the field
   * @param tree the static field access in the program. It is a MemberSelectTree or an
   *     IdentifierTree and is used for diagnostics.
   * @return the value of the static field access, or null if it cannot be determined
   */
  public @Nullable Object evaluateStaticFieldAccess(
      @ClassGetName String classname, String fieldName, ExpressionTree tree) {
    try {
      Class<?> recClass = Class.forName(classname);
      Field field = recClass.getField(fieldName);
      return field.get(recClass);

    } catch (ClassNotFoundException | UnsupportedClassVersionError | NoClassDefFoundError e) {
      if (reportWarnings) {
        checker.reportWarning(
            tree, "class.find.failed", classname, e.getClass() + ": " + e.getMessage());
      }
      return null;
    } catch (Throwable e) {
      // Catch all exceptions so that the checker doesn't crash.
      if (reportWarnings) {
        checker.reportWarning(
            tree,
            "field.access.failed",
            fieldName,
            classname,
            e.getClass() + ": " + e.getMessage());
      }
      return null;
    }
  }

  public @Nullable List<?> evaluteConstructorCall(
      List<List<?>> argValues, NewClassTree tree, TypeMirror typeToCreate) {
    Constructor<?> constructor;
    try {
      // get the constructor
      constructor = getConstructorObject(tree, typeToCreate);
    } catch (Throwable e) {
      // Catch all exception so that the checker doesn't crash
      if (reportWarnings) {
        checker.reportWarning(tree, "constructor.invocation.failed");
      }
      return null;
    }
    if (constructor == null) {
      return null;
    }

    List<Object[]> listOfArguments;
    if (argValues == null) {
      // Method does not have arguments
      listOfArguments = Collections.singletonList(null);
    } else {
      // Find all possible argument sets
      listOfArguments = cartesianProduct(argValues, argValues.size() - 1);
    }

    List<Object> results = new ArrayList<>(listOfArguments.size());
    for (Object[] arguments : listOfArguments) {
      try {
        results.add(constructor.newInstance(arguments));
      } catch (Throwable e) {
        if (reportWarnings) {
          checker.reportWarning(
              tree,
              "constructor.evaluation.failed",
              typeToCreate,
              StringsPlume.join(", ", arguments));
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
    Class<?> recClass = boxPrimitives(TypesUtils.getClassFromType(typeToCreate));
    Constructor<?> constructor = recClass.getConstructor(paramClasses.toArray(new Class<?>[0]));
    return constructor;
  }

  /**
   * Returns the boxed primitive type if the passed type is an (unboxed) primitive. Otherwise it
   * returns the passed type.
   *
   * @param type a type to box or to return unchanged
   * @return a boxed primitive type, if the argument was primitive; otherwise the argument
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
