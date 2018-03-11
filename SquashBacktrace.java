// Copyright 2012 Square Inc.
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.

package com.squareup.squash;

import org.checkerframework.checker.nullness.qual.*;
import static org.checkerframework.checker.nullness.NullnessUtil.castNonNull;
import org.checkerframework.framework.qual.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AnnotatedFor({"nullness"})
/** Creates the Squash stacktrace format for serialization by gson. */
public final class SquashBacktrace {

  private SquashBacktrace() {
    // Should not be instantiated: this is a utility class.
  }
  //adding the annotation @Nullable as the return type may include a null value
   public static @Nullable List<SquashException> getBacktraces(Throwable error) {
    if (error == null) {
      return null;
    }
    final List<SquashException> threadList = new ArrayList<SquashException>();
    final SquashException currentThread =
        new SquashException(Thread.currentThread().getName(), true, getStacktraceArray(error));
    threadList.add(currentThread);
    return threadList;
  }

  private static List<StackElement> getStacktraceArray(Throwable error) {
    List<StackElement> stackElems = new ArrayList<StackElement>();
    for (StackTraceElement element : error.getStackTrace()) {
     @SuppressWarnings("nullness") StackElement elementList =
          new StackElement(element.getClassName(),element.getFileName(), element.getLineNumber(),
              element.getMethodName());/*The constructor of the StackElement class requires non-null 
                                         type arguments,but the method call element.getFileName() might 
                                         return an null value,it can be resolved by modifying parameter of the 
                                         constructor by adding the annotattion @Nullable, but for now the annotation @SuppressWarnings("nullness") 
                                         can be added assuming it doesn't return a null value and suppressing it 
                                         if it does,without crashing the program. */
      stackElems.add(elementList);
    }
    return stackElems;
  }
  //adding the annotation @Nullable as the return type may include a null value
  public static @Nullable Map<String, Object> getIvars(Throwable error) {
    if (error == null) {
      return null;
    }
    Map<String, Object> ivars = new HashMap<String, Object>();
    final Field[] fields = error.getClass().getDeclaredFields();
    for (Field field : fields) {
      try {
        if (!Modifier.isStatic(field.getModifiers()) // Ignore static fields.
            && !field.getName().startsWith("CGLIB")) { // Ignore mockito stuff in tests.
          if (!field.isAccessible()) {
            field.setAccessible(true);
          }
          Object val = castNonNull(field.get(error));/*Ivars is a HashMap object, we can't allow a nullable 
                                                       value at the place of a value for a specific key in the 
                                                       put() method of the HashMap.The castNonNull() method of  
                                                       NullnessUtil class can  be used as this method takes a 
                                                       possibly null reference unsafely casts it to have the @NonNull 
                                                       type qualifier.As it's an expression statement @SuppressWarnings("nullness") can't be used.*/
          ivars.put(field.getName(), val);
        }
      } catch (IllegalAccessException e) {
        ivars.put(field.getName(), "Exception accessing field: " + e);
      }
    }
    return ivars;
  }

  /**
   * Recursive method that follows the "cause" exceptions all the way down the stack, adding them to
   * the passed-in list.
   */
  public static void populateNestedExceptions(List<NestedException> nestedExceptions,
      Throwable error) {
    // Only keep processing if the "cause" exception is set and != the "parent" exception.
    if (error == null || error.getCause() == null || error.getCause() == error) {
      return;
    }
    final Throwable cause = error.getCause();
    @SuppressWarnings("nullness") NestedException doc =
        new NestedException(cause.getClass().getName(), cause.getMessage(), getBacktraces(cause),
            getIvars(cause));/*The contructor of the NestedException class can't take a null value,but the three method calls
                               1. cause.getMessage() , 2. getBacktrace(cause), 3.getIvars(cause) may return null values,
                               resolved it using @SuppressWarnings("nullness") reasons:         
                               1. cause.getMessage() : the constructor itself can be modified with the annotation @Nullable
                               2.getBacktrace(cause) : have a return type annotated with @Nullable(see line no. 33)
                               3.getIvars(cause) : have a return type annotated with @Nullable(see line no. 61), 
                               the method castNonNull() of the NullnessUtil class can't be used as there are chances of getting a null value.*/
    nestedExceptions.add(doc);
    // Exceptions all the way down!
    populateNestedExceptions(nestedExceptions, cause);
  }

  /** Wrapper object for top-level exceptions. */
  static final class SquashException {
    final String name;
    final boolean faulted;
    final List<StackElement> backtrace;

    public SquashException(String name, boolean faulted, List<StackElement> backtrace) {
      this.backtrace = backtrace;
      this.name = name;
      this.faulted = faulted;
    }
  }

  /** Wrapper object for nested exceptions. */
  static final class NestedException {
    final String class_name;
    final String message;
    final List<SquashException> backtraces;
    final Map<String, Object> ivars;

    public NestedException(String className, String message, List<SquashException> backtraces,
        Map<String, Object> ivars) {
      this.class_name = className;
      this.message = message;
      this.backtraces = backtraces;
      this.ivars = ivars;
    }
  }

  /** Wrapper object for a stacktrace entry. */
  static final class StackElement {
    // This field is necessary so Squash knows that this is a java stacktrace that might need
    // obfuscation lookup and git filename lookup.  Our stacktrace elements don't give us the full
    // path to the java file, so Squash has to do a SCM lookup to try and do its cause analysis.
    @SuppressWarnings("UnusedDeclaration") final String type = "obfuscated";
    final String file;
    final int line;
    final String symbol;
    final String class_name;

    private StackElement(String className, String file, int line, String methodName) {
      this.class_name = className;
      this.file = file;
      this.line = line;
      this.symbol = methodName;
    }
  }
}
