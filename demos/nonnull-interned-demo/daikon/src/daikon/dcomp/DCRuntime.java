package daikon.dcomp;

import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
import java.io.PrintWriter;

import daikon.chicory.*;
import utilMDE.WeakIdentityHashMap;
import utilMDE.SimpleLog;
import utilMDE.ArraysMDE;
import utilMDE.Stopwatch;

public final class DCRuntime {

  /** List of all instrumented methods **/
  public static final List<MethodInfo> methods = new ArrayList<MethodInfo>();

  /**
   * Keep track of whether or not we are already processing an enter/exit
   * so we can avoid recursion.  Only really necessary during debugging
   * (where we call toString()
   */
  private static boolean in_enter_exit = false;

  /** Object used to represent nonsensical values **/
  private static final Object nonsensical = new Object();

  /** Object used to represent nonsensical list values **/
  private static final Object nonsensical_list = new Object();

  /** Depth to follow fields in classes **/
  public static int depth = 2;

  /** static count in the JDK.  Used as an offset for non-jdk code **/
  static int max_jdk_static = 100000;

  /**
   * Map from each primitive static name to the offset in static_tags
   */
  // public static Map<String,Integer> static_map
  //   = new LinkedHashMap<String,Integer>();

  /** Storage for each static tag **/
  public static List<Object> static_tags = new ArrayList<Object>();

  /** Tag stack **/
  public static Stack<Object> tag_stack = new Stack<Object>();

  /**
   * Object used to mark procedure entries in the tag stack.  It is pushed
   * on the stack at entry and checked on exit to make sure it is in on the
   * top of the stack.  That allows us to determine which method caused
   * a tag stack problem.
   */
  public static Object method_marker = new Object();

  // Control debug printing
  public static final boolean debug = false;
  public static final boolean debug_tag_frame = false;
  public static final boolean debug_objects = false;
  public static final SimpleLog merge_dv = new SimpleLog (false);
  public static final SimpleLog debug_arr_index = new SimpleLog(false);
  public static final SimpleLog debug_primitive = new SimpleLog (false);
  public static final SimpleLog debug_merge_comp = new SimpleLog (false);
  public static final SimpleLog debug_timing = new SimpleLog (false);
  public static final SimpleLog debug_decl_print = new SimpleLog (false);
  public static final SimpleLog time_decl = new SimpleLog (false);

  /** Simplifies printouts for debugging if we ignore toString **/
  private static boolean ignore_toString = true;

  /** If true, merge arrays and their indices **/
  private static boolean merge_arrays_and_indices = true;

  /**
   * Map from each object to the tags used for each primitive value in
   * the object
   */
  public static WeakIdentityHashMap<Object,Object[]> field_map
    = new WeakIdentityHashMap<Object,Object[]>();

  /**
   * List of all classes encountered.  These are the classes that will
   * have comparability output
   **/
  private static List<ClassInfo> all_classes = new ArrayList<ClassInfo>();

  /** Set of classes whose static initializer has run **/
  private static Set<String> init_classes = new HashSet<String>();

  /**
   * Class used as a tag for primitive constants.  Only different from
   * Object for debugging purposes
   */
  private static class Constant {
  }

  /**
   * Class used as a tag for uninitialized instance fields. Only different
   * from Object for debugging purposes
   */
  private static class UninitFieldTag {
  }

  /**
   * Class used as a tag for uninitialized array elements.  Only different
   * from Object for debugging purposes
   */
  private static class UninitArrayElem {
  }

  /** Perform any initialization required before instrumentation begins **/
  public static void init() {

    // Initialize the array of static tags
    while (static_tags.size() <= max_jdk_static)
      static_tags.add (null);
  }


  /**
   * Handles calls to instrumented equals() methods.
   */
  public static boolean dcomp_equals(Object o1, Object o2) {
    // Make obj1 and obj2 comparable
    if ((o1 != null) && (o2 != null))
      TagEntry.union (o1, o2);

    if (o1 instanceof DCompInstrumented) {
      // Call the instrumented version
      return ((DCompInstrumented)o1).equals_dcomp_instrumented(o2);
    } else {
      // Push tag for return value, and call the uninstrumented version
      push_const();
      return o1.equals(o2);
    }
  }


  /**
   * This map keeps track of active super.equals() calls.<p>
   *
   * Each time we make a call on a particular Object, we keep track of
   * which superclass's equals method we called last. If that Object
   * makes another call to super.equals before the original call is
   * done, then invoke the equals method of the next-higher class in
   * the class hierarchy (i.e. the next superclass).<p>
   *
   * The map maps an Object to the last Class whose equals method we
   * invoked while invoking that Object's original super.equals
   * call. Once the equals call terminates, whether by returning a
   * value or by throwing an exception, the corresponding key is
   * removed from this map.
   */
  static Map<Object, Class<?>> active_equals_calls =
    new HashMap<Object, Class<?>>();

  /**
   * Tracks active super.clone() calls. (See active_equals_calls.)
   */
  static Map<Object, Class<?>> active_clone_calls =
    new HashMap<Object, Class<?>>();


  /**
   * Handles <code>super.equals(Object)</code> calls.<p>
   * @see active_equals_calls
   */
  public static boolean dcomp_super_equals(Object o1, Object o2) {
    // Make obj1 and obj2 comparable
    if ((o1 != null) && (o2 != null))
      TagEntry.union (o1, o2);

    Class<?> o1c = o1.getClass();
    Class<?> o1super;

    // Check to see if we're already in the middle of a super.equals
    // call for this Object
    if (null == active_equals_calls.get(o1)) {
      // No, we are not
      o1super = o1c.getSuperclass();
    } else {
      // Yes, we are -- continue up the class hierarchy
      o1super = active_equals_calls.get(o1).getSuperclass();
    }

    // Update the active_equals_calls map
    active_equals_calls.put(o1, o1super);

    Class<?>[] o1superifaces = o1super.getInterfaces();

    boolean instrumented = false;
    for (Class<?> c : o1superifaces) {
      if (c.getName().equals("daikon.dcomp.DCompInstrumented")) {
        instrumented = true;
        break;
      }
    }

    boolean return_val;
    Class<?> javalangobject, dcompmarker;

    try{
      javalangobject = Class.forName("java.lang.Object");

      if (DCInstrument.jdk_instrumented) {
        dcompmarker = Class.forName("java.lang.DCompMarker");
      } else {
        dcompmarker = Class.forName("daikon.dcomp.DCompMarker");
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }

    try {
      // if the superclass whose method we are calling is instrumented...
      if (instrumented) {
        // call the instrumented version
        Method m = o1super.getMethod("equals",
                           new Class<?>[] { javalangobject, dcompmarker });
        return_val = ((Boolean)(m.invoke(o1, o2, null)));
      } else {
        // call the uninstrumented version
        push_const();   // push a tag for the return value
        Method m = o1super.getMethod("equals",
                           new Class<?>[] { javalangobject });
        return_val = ((Boolean)(m.invoke(o1, o2)));
      }
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }

    // We are now done with the call, so remove the entry for this
    // call from the active_equals_calls map
    active_equals_calls.remove(o1);

    return return_val;
  }


  /**
   * Handles <code>clone()</code> calls.<p>
   * This method throws Throwable because it may throw any checked
   * exception that is thrown by <code>o.clone()</code>.
   */
  // XXX TODO consolidate this and dcomp_super_clone, since there is a
  // lot of duplicated code
  public static Object dcomp_clone(Object o) throws Throwable {
    Class<?> target_class = o.getClass();

    Class<?> dcomp_marker;
    try {
      if (DCInstrument.jdk_instrumented) {
        dcomp_marker = Class.forName("java.lang.DCompMarker");
      } else {
        dcomp_marker = Class.forName("daikon.dcomp.DCompMarker");
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }

    Object return_val;
    Method m;

    try {
      if (target_class.getName().equals("java.lang.Object")) {
        // call the uninstrumented Object.clone()
        // Use getDeclaredMethod instead of getMethod because clone is
        // protected
        m = target_class.getDeclaredMethod("clone", new Class<?>[] {});
        return_val = m.invoke(o);
      } else {
        // every other class has an instrumented version
        m = target_class.getDeclaredMethod("clone",
                                           new Class<?>[] { dcomp_marker });

        // Use length-1 array containing null to distinguish from just
        // null, which indicates 0 arguments
        return_val = m.invoke(o, new Object[] { null });
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }

    // Make o and its clone comparable
    if ((o != null) && (return_val != null)) {
      TagEntry.union (o, return_val);
    }

    return return_val;
  }

  /**
   * Handles <code>super.clone()</code> calls.<p>
   * @see active_clone_calls
   */
  public static Object dcomp_super_clone(Object o) throws Throwable {
    Class<?> oc = o.getClass();   // "Don't call it that."

    Class<?> target_class;  // The class whose method we will invoke
    if (null == active_clone_calls.get(o))
      target_class = oc;
    else
      target_class = active_clone_calls.get(o).getSuperclass();
    active_clone_calls.put(o, target_class);

    Class<?> dcomp_marker;
    try {
      if (DCInstrument.jdk_instrumented) {
        dcomp_marker = Class.forName("java.lang.DCompMarker");
      } else {
        dcomp_marker = Class.forName("daikon.dcomp.DCompMarker");
      }
    } catch (ClassNotFoundException e) {
      // The method call terminates here -- we must remember to remove
      // this from the active calls map
      active_clone_calls.remove(o);
      throw new RuntimeException(e);
    }

    Object return_val;
    Method m;

    try {
      if (target_class.getName().equals("java.lang.Object")) {
        // call the uninstrumented Object.clone()
        // Use getDeclaredMethod instead of getMethod because clone is
        // protected
        m = oc.getDeclaredMethod("clone", new Class<?>[] {});
        return_val = m.invoke(o);
      } else {
        // every other class has an instrumented version
        m = target_class.getDeclaredMethod("clone",
                                           new Class<?>[] { dcomp_marker });

        // Use length-1 array containing null to distinguish from just
        // null, which indicates 0 arguments
        return_val = m.invoke(o, new Object[] { null });
      }
    } catch (IllegalAccessException e) {
      // This shouldn't happen
      active_clone_calls.remove(o);
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      // This shouldn't happen
      active_clone_calls.remove(o);
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      // This might happen - if an exception is thrown from clone(),
      // propagate it by rethrowing it
      active_clone_calls.remove(o);
      throw e.getCause();
    }

    // Make o and its clone comparable
    if ((o != null) && (return_val != null)) {
      TagEntry.union (o, return_val);
    }
    active_clone_calls.remove(o);
    return return_val;
  }


  /**
   * Handle object comparison.  Marks the two objects as comparable and
   * returns whether or not they are equal.  Used as part of a replacement
   * for IF_ACMPEQ
   */
  public static boolean object_eq (Object obj1, Object obj2) {

    if (debug_objects)
      System.out.printf ("comparing (eq) '%s' and '%s'%n", obj_str(obj1),
                         obj_str(obj2));

    // Note that obj1 and obj2 are comparable
    if ((obj1 != null) && (obj2 != null))
      TagEntry.union (obj1, obj2);

    return (obj1 == obj2);
  }

  /**
   * Handle object comparison.  Marks the two objects as comparable and
   * returns whether or not they are equal.  Used as part of a replacement
   * for IF_ACMPNE
   */
  public static boolean object_ne (Object obj1, Object obj2) {

    if (debug_objects)
      System.out.printf ("comparing (ne) '%s' and '%s'%n", obj_str(obj1),
                         obj_str(obj2));
    // Note that obj1 and obj2 are comparable
    if ((obj1 != null) && (obj2 != null))
      TagEntry.union (obj1, obj2);

    return (obj1 != obj2);
  }

  /**
   * Create the tag frame for this method.  Pop the tags for any
   * primitive parameters off of the tag stack and store them in the
   * tag frame.
   *
   * @param params Encodes the position of the primitive parameters into
   * a string.  The first character is size of the tag frame.  The
   * remaining characters indicate where each parameter on the tag stack
   * should be stored into the frame.  For example "20" allocates a tag
   * frame with two elements and stores the top of the tag stack into
   * element 0.  A string is used for simplicity in code generation since
   * strings can easily be placed into the constant portion of the class
   * file.  Note that characters are determined by adding the integer
   * value to '0'.  Values greater than 9 will have unintuitive (but
   * printable) values.
   *
   * @return the allocated and initialized tag frame
   */
  public static Object[] create_tag_frame (String params) {

    int frame_size = ((int)params.charAt(0)) - '0';
      //Character.digit (params.charAt(0), Character.MAX_RADIX);
    Object[] tag_frame = new Object[frame_size];
    if (debug_tag_frame)
      System.out.printf ("Creating tag frame of size %d [%s] for %s%n",
                         frame_size, params, caller_name());
    for (int ii = 1; ii < params.length(); ii++) {
      int offset = params.charAt(ii) - '0';
        //Character.digit (params.charAt(ii), Character.MAX_RADIX);
      check_method_marker();
      tag_frame[offset] = tag_stack.pop();
      if (debug_tag_frame)
        System.out.printf ("popped %s into tag_frame[%d]%n", tag_frame[offset],
                           offset);
    }

    // Push the method marker on the tag stack (now that we have removed
    // the parameters
    tag_stack.push (method_marker);

    return (tag_frame);
  }

  /**
   * Make sure the tag stack for this method is empty before exit
   */
  public static void normal_exit() {

    Object top = tag_stack.pop();
    assert top == method_marker;
    if (debug)
      System.out.printf ("Normal exit ok%n");
  }

  /**
   * Called for exits from methods with a primitive return type.  Pop the
   * return type off of the tag stack, make sure the tags stack is empty for
   * this method and then put the return value back on the tag stack
   */
  public static void normal_exit_primitive() {

    Object ret_tag = pop_check();
    assert ret_tag != null;
    Object top = tag_stack.pop();
    assert top == method_marker;
    if (debug)
      System.out.printf ("Normal exit primitive ok%n");
    tag_stack.push (ret_tag);
  }

  /**
   * Clean up the tag stack on an exception exit from a method.  Pops
   * items off of the tag stack until the method marker is found
   */
  public static void exception_exit() {

    if (debug)
      System.out.printf ("Exception exit from %s%n", caller_name());
    while (!tag_stack.empty())
      if (tag_stack.pop() == method_marker)
        return;

    System.out.printf ("Method marker not found in exception exit%n");
  }

  /**
   * Cleans up the tag stack when an exception is thrown
   */
  public static void throw_op() {
    while (tag_stack.peek() != method_marker)
      tag_stack.pop();
  }

  /** Pushes the tag at tag_frame[index] on the tag stack */
  public static void push_local_tag (Object[] tag_frame, int index) {

    if (debug_primitive.enabled())
      debug_primitive.log ("push_local_tag[%d] %s%n", index, tag_frame[index]);
    assert tag_frame[index] != null : "index " + index;
    tag_stack.push (tag_frame[index]);
  }

  /** Pops the top of the tag stack into tag_frame[index] **/
  public static void pop_local_tag (Object[] tag_frame, int index) {

    check_method_marker();
    tag_frame[index] = tag_stack.pop();
    assert tag_frame[index] != null : "index " + index;
    if (debug_primitive.enabled())
      debug_primitive.log ("pop_local_tag[%d] %s%n", index, tag_frame[index]);

  }

  /** Pushes the argument tag on the tag stack **/
  public static void push_tag (Object tag) {
    tag_stack.push (tag);
  }

  /** Pops the top tag from the tag stack and returns it **/
  public static Object pop_tag() {
    return pop_check();
  }

  /** Pushes the tag associated with the static static_num on the tag stack */
  public static void push_static_tag (int static_num) {

    Object static_tag = static_tags.get (static_num);
    if (static_tag == null) {
      static_tag = new Object();
      static_tags.set (static_num, static_tag);
    }
    tag_stack.push (static_tag);
    debug_primitive.log ("push_static_tag[%d] %s%n", static_num, static_tag);
  }

  /** Pushes an array reference on the tag stack */
  public static void push_array_tag (Object arr_ref) {
    tag_stack.push (arr_ref);
    debug_arr_index.log ("push_array_tag %s%n", arr_ref);
  }

  /** Pops the top of the tag stack into the tag storage for static_num **/
  public static void pop_static_tag (int static_num) {

    check_method_marker();
    static_tags.set (static_num, tag_stack.pop());
    assert static_tags.get(static_num) != null;
    debug_primitive.log ("pop_static_tag[%d] %s%n", static_num,
                         static_tags.get(static_num));
  }

  /**
   * Discard the tag on the top of the tag stack.  Called when primitives
   * are pushed but not used in expressions (such as when allocating arrays)
   */
  public static void discard_tag(int cnt) {

    while (--cnt >= 0) {
      check_method_marker();
      tag_stack.pop();
    }
  }

  private static void primitive_array_store (Object arr_ref, int length,
                                             int index) {

    // look for the tag storage for this array
    Object[] obj_tags = field_map.get (arr_ref);

    // If none has been allocated, allocate the space and associate it with
    // the array
    if (obj_tags == null) {
      obj_tags = new Object[length];
      field_map.put (arr_ref, obj_tags);
    }

    // Pop the tag off of the stack and assign it into the tag storage for
    // this index
    obj_tags[index] = pop_check();
    debug_primitive.log ("array store %s[%d] = %s%n", arr_ref, index,
                         obj_tags[index]);

    // Mark the arry and its index as comparable
    Object index_tag = pop_check();
    debug_arr_index.log ("Merging array '%s' and index '%s'", arr_ref,
                         index_tag);
    if (merge_arrays_and_indices)
      TagEntry.union (arr_ref, index_tag);
  }

  /**
   * Execute an aastore instruction and mark the array and its index as
   * comparable.
   */
  public static void aastore (Object[] arr, int index, Object val) {

    // Mark the array and its index as comparable
    Object index_tag = pop_check();
    debug_arr_index.log ("Merging array '%s' and index '%s'", arr, index_tag);
    if (merge_arrays_and_indices)
      TagEntry.union (arr, index_tag);

    // Store the value
    arr[index] = val;
  }

  /**
   * Execute an bastore instruction and manipulate the tags accordingly.
   * The tag at the top of stack is stored into the tag storage for the
   * array.
   */
  public static void bastore (byte[] arr, int index, byte val) {

    // Store the tag for val in the tag storage for array and mark
    // the array and the index as comparable.
    primitive_array_store (arr, arr.length, index);

    // Execute the array store
    arr[index] = val;
  }

  /**
   * Execute an castore instruction and manipulate the tags accordingly.
   * The tag at the top of stack is stored into the tag storage for the
   * array.
   */
  public static void castore (char[] arr, int index, char val) {

    // Store the tag for val in the tag storage for array and mark
    // the array and the index as comparable.
    primitive_array_store (arr, arr.length, index);

    // Execute the array store
    arr[index] = val;
  }
  /**
   * Execute an dastore instruction and manipulate the tags accordingly.
   * The tag at the top of stack is stored into the tag storage for the
   * array.
   */
  public static void dastore (double[] arr, int index, double val) {

    // Store the tag for val in the tag storage for array and mark
    // the array and the index as comparable.
    primitive_array_store (arr, arr.length, index);

    // Execute the array store
    arr[index] = val;
  }

  /**
   * Execute an fastore instruction and manipulate the tags accordingly.
   * The tag at the top of stack is stored into the tag storage for the
   * array.
   */
  public static void fastore (float[] arr, int index, float val) {

    // Store the tag for val in the tag storage for array and mark
    // the array and the index as comparable.
    primitive_array_store (arr, arr.length, index);

    // Execute the array store
    arr[index] = val;
  }

  /**
   * Execute an iastore instruction and manipulate the tags accordingly.
   * The tag at the top of stack is stored into the tag storage for the
   * array.
   */
  public static void iastore (int[] arr, int index, int val) {

    // Store the tag for val in the tag storage for array and mark
    // the array and the index as comparable.
    primitive_array_store (arr, arr.length, index);

    // Execute the array store
    arr[index] = val;
  }

  /**
   * Execute an lastore instruction and manipulate the tags accordingly.
   * The tag at the top of stack is stored into the tag storage for the
   * array.
   */
  public static void lastore (long[] arr, int index, long val) {

    // Store the tag for val in the tag storage for array and mark
    // the array and the index as comparable.
    primitive_array_store (arr, arr.length, index);

    // Execute the array store
    arr[index] = val;
  }

  /**
   * Execute an sastore instruction and manipulate the tags accordingly.
   * The tag at the top of stack is stored into the tag storage for the
   * array.
   */
  public static void sastore (short[] arr, int index, short val) {

    // Store the tag for val in the tag storage for array and mark
    // the array and the index as comparable.
    primitive_array_store (arr, arr.length, index);

    // Execute the array store
    arr[index] = val;
  }

  /**
   * Make the count arguments to multianewarray comparable to the
   * corresponding array indices. count1 is made comparable to the
   * index of the given array (arr), and count2 is made comparable to the
   * index of each array that is an element of arr.
   */
  public static void multianewarray2 (int count1, int count2, Object[] arr) {

    Object count2tag = pop_check();
    Object count1tag = pop_check();

    TagEntry.union (count1tag, arr);

    for (Object subarr : arr) {
      TagEntry.union (count2tag, subarr);
    }

  }

  /**
   * Called when a user method is entered.  Any daikon variables whose current
   * values are comparable are marked as comparable.
   *
   * @param tag_frame tag_frame containing the tags for the primitive
   *        arguments of this method.
   * @param obj value of 'this'.  Null if the method is static
   * @param mi_index index into the list of all methods (methods)
   * @param args Array of the arguments to the method.
   */
  public static void enter (Object[] tag_frame, Object obj, int mi_index,
                            Object[] args) {

    // Don't be recursive
    if (in_enter_exit)
      return;
    in_enter_exit = true;

    if (debug) {
      Throwable stack = new Throwable ("enter");
      stack.fillInStackTrace();
      StackTraceElement[] ste_arr = stack.getStackTrace();
      StackTraceElement ste = ste_arr[1];
      if (ignore_toString && ste.getMethodName().equals ("toString")) {
        in_enter_exit = false;
        return;
      }
      System.out.printf ("%s.%s():::ENTER%n%n", ste.getClassName(),
                         ste.getMethodName());

      System.out.printf ("this = '%s', mi = %s%n", obj_str(obj),
                         methods.get(mi_index));
      System.out.printf ("args: ");
      for (Object arg : args)
        System.out.printf ("%s ", obj_str(arg));
      System.out.printf ("%n");
    }

    MethodInfo mi = methods.get (mi_index);
    mi.call_cnt++;
    ClassInfo ci = mi.class_info;
    if (ci.clazz == null) {
      ci.initViaReflection();
      all_classes.add (ci);
      // Moved to DCInstrument.instrument()
      // daikon.chicory.Runtime.all_classes.add (ci);
      merge_dv.log ("initializing traversal for %s%n", ci);
      ci.init_traversal(depth);
    }
    if (mi.traversalEnter == null) {
      mi.init_traversal (depth);
    }

    // Merge comparability information for the Daikon variables
    merge_dv.indent ("processing method %s:::ENTER%n", mi);
    process_all_vars (mi, mi.traversalEnter, tag_frame, obj, args, null);
    merge_dv.exdent();

    in_enter_exit = false;
  }

  /**
   * Called when a user method exits.  Any daikon variables whose current
   * values are comparable are marked as comparable.
   *
   * @param tag_frame tag_frame containing the tags for the primitive
   * arguments of this method.
   * @param obj value of 'this'.  Null if the method is static
   * @param mi_index index into the list of all methods (methods)
   * @param args Array of the arguments to the method.
   * @param ret_val Value returned by the method.  Null if the method is a
   * constructor or void,
   * @param exit_line_number the source line number of this exit point
   */

  public static void exit (Object[] tag_frame, Object obj, int mi_index,
                         Object[] args, Object ret_val, int exit_line_number) {

    // Don't be recursive
    if (in_enter_exit)
      return;
    in_enter_exit = true;

    if (debug) {
      Throwable stack = new Throwable ("exit");
      stack.fillInStackTrace();
      StackTraceElement[] ste_arr = stack.getStackTrace();
      StackTraceElement ste = ste_arr[1];
      if (ignore_toString && ste.getMethodName().equals ("toString")) {
        in_enter_exit = false;
        return;
      }
      System.out.printf ("%s.%s():::EXIT%n%n", ste.getClassName(),
                         ste.getMethodName());

      System.out.printf ("this = '%s', mi = %s%n", obj_str(obj),
                         methods.get(mi_index));
      System.out.printf ("args: ");
      for (Object arg : args)
        System.out.printf ("%s ", obj_str(arg));
      System.out.printf ("%n");
      System.out.printf ("ret_val = %s, exit_line_number= %d%n", ret_val,
                         exit_line_number);
    }

    MethodInfo mi = methods.get (mi_index);

    // Merge comparability information for the Daikon variables
    merge_dv.log ("processing method %s:::EXIT%n", mi);
    merge_dv.indent();
    process_all_vars (mi, mi.traversalExit, tag_frame, obj, args, ret_val);
    merge_dv.exdent();

    in_enter_exit = false;
  }

  /**
   * Process all of the daikon variables in the tree starting at root.
   * If the values referenced by those variables are comparable mark
   * the variables as comparable.
   */
  public static void process_all_vars (MethodInfo mi, RootInfo root,
                                       Object[] tag_frame, Object obj,
                                       Object[] args, Object ret_val) {


    debug_timing.log ("process_all_vars for %s%n", mi);

    if (merge_dv.enabled()) {
      merge_dv.log ("this: %s%n", obj);
      merge_dv.log ("arguments: %s%n", ArraysMDE.toString(args));
    }

    // Map from an Object to the Daikon variable that currently holds
    // that object.
    IdentityHashMap<Object,DaikonVariableInfo> varmap
      = new IdentityHashMap<Object,DaikonVariableInfo>();

    for (DaikonVariableInfo dv : root.children) {
      if (dv instanceof ThisObjInfo) {
        merge_comparability (varmap, null, obj, dv);
      } else if (dv instanceof ParameterInfo) {
        ParameterInfo pi = (ParameterInfo) dv;
        Object p = args[pi.getArgNum()];
        // Class arg_type = mi.arg_types[pi.getArgNum()];
        // if (arg_type.isPrimitive())
        if (pi.isPrimitive())
          p = tag_frame[pi.get_param_offset() + ((obj == null) ? 0 : 1)];
        merge_comparability (varmap, null, p, pi);
      } else if (dv instanceof ReturnInfo) {
        ReturnInfo ri = (ReturnInfo) dv;
        if (mi.return_type().isPrimitive())
          ret_val = tag_stack.peek();
        merge_comparability (varmap, null, ret_val, dv);
      } else if (dv instanceof FieldInfo) {
        assert ((FieldInfo)dv).isStatic() : "non static field at root " + dv;
        merge_comparability (varmap, null, null, dv);
      } else if (dv instanceof StaticObjInfo) {
        for (DaikonVariableInfo static_dv : dv.children) {
          assert ((FieldInfo)static_dv).isStatic()
            : "non static field at root " + dv;
          merge_comparability (varmap, null, null, static_dv);
        }
      } else {
        assert false : "unexpected node " + dv;
      }
    }
    debug_timing.log ("exit process_all_vars for %s%n", mi);
  }

  /**
   * Returns the tag for the specified field.  If that field is an array,
   * a list of tags will be returned.
   */
  static Object get_field_tag (FieldInfo fi, Object parent, Object obj) {

    // Initialize the code that gets the tag for various field types
    if (fi.field_tag == null) {
      if (fi.isStatic()) {
        if (fi.isPrimitive()) {
          fi.field_tag = new StaticPrimitiveTag (fi);
        } else {
          fi.field_tag = new StaticReferenceTag (fi);
        }
      } else { // not static
        if (fi.isPrimitive() && fi.isArray()) {
          fi.field_tag = new PrimitiveArrayTag (fi);
        } else if (fi.isPrimitive()) {
          fi.field_tag = new PrimitiveTag (fi);
        } else {
          fi.field_tag = new ReferenceTag (fi);
        }
      }
    }

    // get the tag
    return fi.field_tag.get_tag (parent, obj);
  }

  /**
   * Gets the tag for the specified field.  The tags for primitive instance
   * fields are stored in the tag storage for the parent.  Tags for
   * primitive static fields are stored a single global list (statics_tags)
   * indexed by the value for the static name in static_map.  If the field
   * is in an array a List<Object> with one tag per element is returned.
   *
   * The tag for non-primitive fields is the object itself.
   *
   * @param fi     DaikonVariable to process
   * @param parent Value of dv's parent
   * @param obj    Value of dv
   */
  static Object old_get_field_tag (FieldInfo fi, Object parent, Object obj) {

    Object tag = null;

    if (fi.isStatic()) {
      if (fi.isPrimitive()) {
        Field field = fi.getField();
        Class<?> clazz = field.getDeclaringClass();
        String name = DCInstrument.tag_method_name (DCInstrument.GET_TAG,
                                     clazz.getName(), field.getName());
        try {
          Method get_tag = clazz.getMethod (name);
          Object ret_val = get_tag.invoke (parent);
          assert ret_val == null;
          tag = pop_check();
          assert tag != null;
        } catch (Exception e) {
          throw new Error ("can't execute tag method " + name , e);
        }
      } else { // the tag is the object itself.  Get it via reflection
        Field field = fi.getField();
        if (is_class_init (field.getDeclaringClass())) {
          if (!field.isAccessible())
            field.setAccessible (true);
          try {
            tag = field.get (null);
          } catch (Exception e) {
            throw new RuntimeException("Can't get val for static field "
                                       + field, e);
          }
        } else {
          tag = nonsensical;
        }
      }
    } else {
      if (fi.getType().isPrimitive()) {
        String tag_field_name = tag_field_name(fi.getField().getName());
        if (fi.isArray()) {
          @SuppressWarnings("unchecked")
          List<Object> parent_list = (List<Object>)parent;
          Field tag_field = null;
          List<Object> tag_list = new ArrayList<Object>(parent_list.size());
          for (Object parent_element : parent_list) {
            // if (tag_field == null)
            //   tag_field = fi.get_tag_field (tag_field_name,
            //                                 parent_element.getClass());
            Object[] tags = field_map.get (parent_element);
            // assert tags != null : "array " + fi + " " + parent_element;
            if (tags == null)
              tag_list.add (nonsensical);
            else
              tag_list.add (tags[fi.get_field_num()]);
                          // get_object_field (tag_field, parent_element));

          }
          tag = tag_list;
        } else {
          //Field tag_field = fi.get_tag_field (tag_field_name,
          //                                    parent.getClass());
          // tag = get_object_field (tag_field, parent);
          Object[] tags = field_map.get (parent);
          if (tags == null)
            tag = nonsensical;  // happens if field has never been assigned to
          else
            tag = tags[fi.get_field_num()];
        }
      } else {
        tag = obj;
      }
    }

    merge_dv.log ("Tag for field %s = %s%n", fi.getField(), tag);
    return (tag);
  }

  /**
   * Gets the object in field f in object obj.  Exceptions are turned
   * into Errors
   */
  public static Object get_object_field (Field f, Object obj) {
    try {
      return f.get (obj);
    } catch (Exception e) {
      throw new Error ("can't get field " + f + " in " + obj_str (obj), e);
    }
  }

  /**
   * Merges the comparability of the daikon variable dv and its children
   * whose current values are comparable.
   *
   * @param varmap Map from value set leaders to the first daikon variable
   *               encountered with that leader.  Whenever a second daikon
   *               variable is encountered whose value has the same leader,
   *               that daikon variable is merged with the first daikon
   *               variable
   * @param parent Value of dv's parent
   * @param obj    Value of dv
   * @param dv     DaikonVariable to process
   */
  static void merge_comparability (IdentityHashMap<Object,DaikonVariableInfo> varmap,
                                   Object parent, Object obj,
                                   DaikonVariableInfo dv) {

    // merge_dv.enabled = dv.getName().contains ("mtfFreq");

    long start_millis = 0;
    if (debug_timing.enabled())
      start_millis = System.currentTimeMillis();

    if (merge_dv.enabled())
      merge_dv.log ("merge_comparability: checking var %s = '%s' %n",
                    dv, obj_str(obj));

    // Ignore ClassInfo and StringInfo variables.  These are not real
    // variables in the program
    if ((dv instanceof DaikonClassInfo) || (dv instanceof StringInfo)) {
      if (debug_timing.enabled())
        debug_timing.log ("  Variable %s : %d msecs%n", dv,
                          System.currentTimeMillis() - start_millis);
      return;
    }

    // Get the tag for this object.  For non-primitives this is normally the
    // object itself.  For static fields, the object is not passed in, but is
    // obtained via reflection.
    Object tag = obj;
    if (dv instanceof FieldInfo)
      tag = get_field_tag ((FieldInfo) dv, parent, obj);

    if (dv.isArray() && (tag instanceof List)) {
      @SuppressWarnings("unchecked")
      List<Object> elements = (List<Object>)tag;
      if (debug_timing.enabled())
        debug_timing.log ("  ArrayInfo %d elements", elements.size());
      for (Object atag : elements) {
        // Ignore null and nonsensical tags.  There is no reason to process
        // their children, because they can't have any with reasonable values
        if ((atag == null) || (atag == nonsensical)
            || (atag == nonsensical_list))
          continue;

        // Look up this object.  If it already is associated with a
        // DaikonVariable merge those variables.  Otherwise, add it to
        // the map
        Object leader = TagEntry.find(atag);
        if (merge_dv.enabled())
          merge_dv.log ("Leader for atag '%s' is '%s'%n", obj_str(atag),
                        obj_str (leader));
        DaikonVariableInfo current = varmap.get (leader);
        merge_dv.log ("Daikon variable for leader = %s%n", current);
        if (current != null) {
          merge_dv.log ("**Merging %s and %s\n", current, dv);
          TagEntry.union (current, dv);
        } else
          varmap.put (leader, dv);
      }
    } else if (dv.isArray()) {
      if (tag == null) {
        if (debug_timing.enabled())
          debug_timing.log ("  no array tags for Variable %s : %d msecs%n", dv,
                            System.currentTimeMillis() - start_millis);
        return;
      }
      Object[] elements = (Object[])tag;
      if (debug_timing.enabled())
        debug_timing.log ("  Prim ArrayInfo %d elements", elements.length);
      Object prev_tag = null;
      for (Object atag : elements) {
        // Ignore null and nonsensical tags.  There is no reason to process
        // their children, because they can't have any with reasonable values
        if ((atag == null) || (atag == nonsensical)
            || (atag == nonsensical_list))
          continue;

        // No need to handle the same tag twice
        if (prev_tag == atag)
          continue;
        prev_tag = atag;

        // Look up this object.  If it already is associated with a
        // DaikonVariable merge those variables.  Otherwise, add it to
        // the map
        Object leader = TagEntry.find(atag);
        if (merge_dv.enabled())
          merge_dv.log ("Leader for atag '%s' is '%s'%n", obj_str(atag),
                        obj_str (leader));
        DaikonVariableInfo current = varmap.get (leader);
        merge_dv.log ("Daikon variable for leader = %s%n", current);
        if (current != null) {
          merge_dv.log ("**Merging %s and %s\n", current, dv);
          TagEntry.union (current, dv);
        } else
          varmap.put (leader, dv);
      }
    } else {
      // Ignore null and nonsensical tags.  There is no reason to process
      // their children, because they can't have any with reasonable values
      if ((tag == null) || (tag == nonsensical) || (tag == nonsensical_list)) {
        if (debug_timing.enabled())
          debug_timing.log ("  Variable %s : %d msecs%n", dv,
                            System.currentTimeMillis() - start_millis);
        return;
      }

      // Look up this object.  If it already is associated with a
      // DaikonVariable merge those variables.  Otherwise, add it to
      // the map
      Object leader = TagEntry.find(tag);
      if (merge_dv.enabled())
        merge_dv.log ("Leader for tag '%s' is '%s'%n", obj_str(tag),
                      obj_str (leader));
      DaikonVariableInfo current = varmap.get (leader);
      assert leader != null : "null leader for " + obj_str(tag);
      merge_dv.log ("Daikon variable for leader = %s%n", current);
      if (current != null) {
        merge_dv.log ("**Merging variable '%s' and '%s'%n", current, dv);
        TagEntry.union (current, dv);
      } else
        varmap.put (leader, dv);
    }

    if (debug_timing.enabled())
      debug_timing.log ("  Variable %s : %d msecs%n", dv,
                        System.currentTimeMillis() - start_millis);

    // Process all of the children
    for (DaikonVariableInfo child : dv) {
      Object child_obj = null;
      if ((child instanceof ArrayInfo)
          && ((ArrayInfo)child).getType().isPrimitive()) {
        ArrayInfo ai = (ArrayInfo)child;
        // System.out.printf ("child array type %s = %s%n", ai, ai.getType());
        Object[] arr_tags = field_map.get (tag);
        // System.out.printf ("found arr_tag %s for arr %s\n", arr_tags, tag);
        // System.out.printf ("tag values = %s%n", Arrays.toString (arr_tags));
        child_obj = arr_tags;
      } else { // not a primitive array
        child_obj = child.getMyValFromParentVal (tag);
      }
      merge_comparability (varmap, tag, child_obj, child);
    }
  }

  /**
   * Dumps out comparability information for all classes that were
   * processed.
   */
  public static void print_all_comparable(PrintWriter ps) {

    for (ClassInfo ci : all_classes) {
      for (MethodInfo mi : ci.method_infos) {
        if (mi.is_class_init())
          continue;
        // skip our added method
        if (mi.method_name.equals("equals_dcomp_instrumented"))
          continue;
        ps.printf ("%n");
        print_comparable (ps, mi);
      }
    }
  }

  public static void trace_all_comparable(PrintWriter ps) {

    for (ClassInfo ci : all_classes) {
      for (MethodInfo mi : ci.method_infos) {
        if (mi.is_class_init()) continue;
        if (mi.method_name.equals("equals_dcomp_instrumented")) continue;
        ps.printf("%n");
        print_comparable_traced (ps, mi);
      }
    }
  }

  public static void print_decl_file (PrintWriter ps) {

    // Write the file header
    ps.printf ("// Declaration file written by daikon.dcomp%n%n");
    ps.printf ("VarComparability%nimplicit%n");

    // Write the information for each class
    for (ClassInfo ci : all_classes) {
      print_class_decl (ps, ci);
    }
    debug_decl_print.log ("finished %d classes%n", all_classes.size());
  }

  static int class_cnt = 0;
  static int method_cnt = 0;
  static int instance_var_cnt = 0;
  static int static_final_cnt = 0;
  static int hashcode_var_cnt = 0;
  static int hashcode_arr_cnt = 0;
  static int primitive_var_cnt = 0;
  static int tostring_cnt = 0;
  static int class_var_cnt = 0;
  static int this_instance_cnt = 0;
  static int other_instance_cnt = 0;
  static int other_cnt = 0;
  static int parameter_cnt = 0;
  static int static_cnt = 0;
  static int synthetic_cnt = 0;
  static int enum_cnt = 0;

  /**
   * prints statistics about the number of decls to stdout
   */
  public static void decl_stats () {


    for (ClassInfo ci : all_classes) {
      class_cnt++;
      System.out.printf ("processing class %s%n", ci);
      add_dv_stats (ci.traversalClass);
      add_dv_stats (ci.traversalObject);
      for (MethodInfo mi : ci.method_infos) {
        if (mi.is_class_init())
          continue;
        method_cnt++;
        System.out.printf ("  Processing method %s [%d calls]%n", mi,
                           mi.call_cnt);
        if (mi.traversalEnter == null) {
          System.out.printf ("  Skipping method %s%n", mi);
          continue;
        }
        System.out.printf ("    Enter%n");
        add_dv_stats (mi.traversalEnter);
        for (Integer ii : mi.exit_locations) {
          System.out.printf ("    Exit%d%n", ii);
          add_dv_stats (mi.traversalExit);
        }
      }
    }

    System.out.printf ("Classes             = %,d%n", class_cnt);
    System.out.printf ("Methods             = %,d%n", method_cnt);
    System.out.printf ("------------------------------%n");
    System.out.printf ("Hashcodes           = %,d%n", hashcode_var_cnt);
    System.out.printf ("Hashcode arrays     = %,d%n", hashcode_arr_cnt);
    System.out.printf ("primitives          = %,d%n", primitive_var_cnt);
    System.out.printf ("------------------------------%n");
    System.out.printf ("tostring vars       = %,d%n", tostring_cnt);
    System.out.printf ("class vars          = %,d%n", class_var_cnt);
    System.out.printf ("Enums               = %,d%n", enum_cnt);
    System.out.printf ("Synthetic           = %,d%n", synthetic_cnt);
    System.out.printf ("static final vars   = %,d%n", static_final_cnt);
    System.out.printf ("static vars         = %,d%n", static_cnt);
    System.out.printf ("this instance vars  = %,d%n", this_instance_cnt);
    System.out.printf ("other instance vars = %,d%n", other_instance_cnt);
    System.out.printf ("Parameters          = %,d%n", parameter_cnt);
    System.out.printf ("Others              = %,d%n", other_cnt);
  }

  private static void add_dv_stats (RootInfo root) {

    if (root == null)
      return;
    List<DaikonVariableInfo> dv_list = root.tree_as_list();
    for (DaikonVariableInfo dv : dv_list) {
      if (dv instanceof RootInfo)
        continue;
      // System.out.printf ("      processing dv %s [%s]%n", dv,
      //                    dv.getTypeName());
      if (dv.isHashcode())
        hashcode_var_cnt++;
      else if (dv.isHashcodeArray())
        hashcode_arr_cnt++;
      else
        primitive_var_cnt++;
      if (dv.getName().contains (".toString"))
        tostring_cnt++;
      else if (dv.getName().contains (DaikonVariableInfo.class_suffix))
        class_var_cnt++;
      else if (dv instanceof FieldInfo) {
        Field field = ((FieldInfo)dv).getField();
        int modifiers = field.getModifiers();
        if (field.isEnumConstant())
          enum_cnt++;
        else if (field.isSynthetic())
          synthetic_cnt++;
        else if (Modifier.isStatic (modifiers)
                 && Modifier.isFinal(modifiers))
          static_final_cnt++;
        else if (Modifier.isStatic (modifiers))
          static_cnt++;
        else if (dv.getName().startsWith ("this"))
          this_instance_cnt++;
        else
          other_instance_cnt++;
      } else if (dv instanceof ParameterInfo)
        parameter_cnt++;
      else
        other_cnt++;
    }
  }

  /**
   * Calculates and prints the declarations for the specified class
   */
  public static void print_class_decl (PrintWriter ps, ClassInfo ci) {

    Stopwatch watch = null;

    time_decl.start_time();
    time_decl.indent ("Printing decl file for class %s%n", ci.class_name);

    // Make sure that two variables have the same comparability at all
    // program points
    merge_class_comparability (ci);

    // Write the class ppt
    ps.printf ("DECLARE%n");
    ps.printf ("%s:::CLASS%n", ci.class_name);
    print_decl_vars (ps, get_comparable (ci.traversalClass),
                     ci.traversalClass);
    ps.printf ("%n");
    time_decl.log_time ("printed class ppt");

    // Write the object ppt
    ps.printf ("DECLARE%n");
    ps.printf ("%s:::OBJECT%n", ci.class_name);
    print_decl_vars (ps, get_comparable (ci.traversalObject),
                     ci.traversalObject);
    ps.printf ("%n");
    time_decl.log_time ("printed object ppt");

    // Print the information for each enter/exit point
    for (MethodInfo mi : ci.method_infos) {
      if (mi.is_class_init())
        continue;
      debug_decl_print.log ("  method %s%n", mi.method_name);
      ps.printf ("%n");
      print_decl (ps, mi);
    }

    time_decl.exdent_time ("finished class %s%n", ci.class_name);
  }

  static long comp_list_ms = 0;
  static long ppt_name_ms = 0;
  static long decl_vars_ms = 0;
  static long total_ms = 0;
  // static Stopwatch watch = new Stopwatch();

  /**
   * Prints a decl ENTER/EXIT records with comparability.  Returns the
   * list of comparabile DVSets for the exit.
   */
  public static List<DVSet> print_decl (PrintWriter ps, MethodInfo mi) {

    // long start = System.currentTimeMillis();
    // watch.reset();

    time_decl.start_time();
    time_decl.indent ("Print decls for method '%s'", mi.method_name);
    List<DVSet> l = get_comparable (mi.traversalEnter);
    // comp_list_ms += watch.snapshot(); watch.reset();
    if (l == null)
      return (null);
    time_decl.log_time ("got %d comparable sets", l.size());

    // Print the enter point
    ps.println ("DECLARE");
    ps.println (clean_decl_name (DaikonWriter.methodEntryName (mi.member)));
    // ppt_name_ms += watch.snapshot();  watch.reset();
    print_decl_vars (ps, l, mi.traversalEnter);
    // decl_vars_ms += watch.snapshot();  watch.reset();
    ps.println();
    time_decl.log_time ("after enter");

    // Print the exit points
    l = get_comparable (mi.traversalExit);
    // comp_list_ms += watch.snapshot();  watch.reset();

    time_decl.log_time ("got exit comparable sets");
    for (Integer ii : mi.exit_locations) {
      ps.println ("DECLARE");
      ps.println (clean_decl_name (DaikonWriter.methodExitName
                                   (mi.member, ii)));
      // ppt_name_ms += watch.snapshot();  watch.reset();

      time_decl.log_time ("after exit clean_decl_name");
      print_decl_vars (ps, l, mi.traversalExit);
      ps.println();
      // decl_vars_ms += watch.snapshot();  watch.reset();

    }

    // total_ms += System.currentTimeMillis() - start;
    time_decl.exdent_time ("Finished processing method '%s'", mi.method_name);
    return (l);
  }

  /**
   * Print the variables in sets to ps in DECL file format.  Each
   * variable in the same set is given the same comparability.  Constructed
   * classname variables are made comparable to opther classname variables
   * only.
   */
  private static void print_decl_vars (PrintWriter ps, List<DVSet> sets,
                                       RootInfo dv_tree) {

    time_decl.indent();
    time_decl.log ("print_decl_vars start");

    // Map from array name to comparability for its indices (if any)
    Map<String, Integer> arr_index_map = new LinkedHashMap<String,Integer>();

    // Map from daikon variable to its comparability
    Map<DaikonVariableInfo, Integer> dv_comp_map
      = new IdentityHashMap<DaikonVariableInfo, Integer>();

    // Initial comparability values
    int class_comp = 1;
    int comp = 2;

    // Loop through each set of comparable variables
    for (DVSet set : sets) {

      if ((set.size() == 1) && (set.get(0) instanceof StaticObjInfo))
        continue;

      // Determine if the set has both hashcode variables and integer
      // variables.  If it does, it is indicating index comparability
      boolean hashcode_vars = false;
      boolean non_hashcode_vars = false;
      for (DaikonVariableInfo dv : set) {
        if (dv.isHashcode() || dv.isHashcodeArray())
          hashcode_vars = true;
        else
          non_hashcode_vars = true;
      }
      debug_decl_print.log ("        %d vars in set, hashcode/non = %b/%b%n",
                            set.size(), hashcode_vars, non_hashcode_vars);

      // Loop through each variable and assign its comparability
      // Since hashcodes and their indices are in the same set, assign
      // hashcodes one higher comparability number
      for (DaikonVariableInfo dv : set) {
        debug_decl_print.log ("          dv %s%n", dv);
        if (dv instanceof DaikonClassInfo) {
          dv_comp_map.put (dv, class_comp);
          assert set.size() == 1 : "odd set " + set;
          comp--;   // negate increment of comp below
        } else if (dv.isHashcode() && non_hashcode_vars) {
          dv_comp_map.put (dv, comp+1);
          arr_index_map.put (dv.getName() + "[]", comp);
        } else {
          dv_comp_map.put (dv, comp);
        }
      }

      // Increment the comparability number to the next valid number
      comp++;
      if (hashcode_vars && non_hashcode_vars)
        comp++;
    }

    time_decl.log_time ("finished filling maps%n");

    // Loop through each variable and print out its comparability
    // Use the dv_tree rather than sets so that we print out in the
    // same order each time

    List<DaikonVariableInfo> dv_list = dv_tree.tree_as_list();
    time_decl.log_time ("built tree as list with %d elements", dv_list.size());
    for (DaikonVariableInfo dv : dv_list) {
      if ((dv instanceof RootInfo) || (dv instanceof StaticObjInfo))
        continue;
      ps.println(dv.getName());
      ps.println (dv.getTypeName());
      ps.println (dv.getRepTypeName());
      comp = dv_comp_map.get (dv);
      if (dv.isArray()) {
        Integer index_comp = arr_index_map.get (dv.getName());
        if (index_comp != null)
          ps.println (comp + "[" + index_comp + "]");
        else
          ps.println (comp);
      } else
        ps.println (comp);
    }

    time_decl.log_time ("print_decl_vars end%n");
    time_decl.exdent();
  }

  /**
   * Prints comparability information for the enter and exit points of
   * the specified method. By default, outputs to foo.txt-cset
   */
  /* TO DO: Find a way to make this work correctly without using normal
   * get_comparable.
   */
  public static void print_comparable (PrintWriter ps, MethodInfo mi) {

    List<DVSet> l = get_comparable (mi.traversalEnter);
    ps.printf ("Daikon Variable sets for %s enter%n",
               clean_decl_name(mi.toString()));
    if (l == null)
      ps.printf ("  not called%n");
    else {
      for (DVSet set : l) {
        if ((set.size() == 1) && (set.get(0) instanceof StaticObjInfo))
          continue;
        ArrayList stuff = shinyOutput(set, daikon.DynComp.shiny_print);
        // To see "daikon.chicory.FooInfo:variable", change true to false
        ps.printf ("  [%d] %s%n", stuff.size(), stuff);
      }
    }

    l = get_comparable (mi.traversalExit);
    ps.printf ("Daikon Variable sets for %s exit%n",
               clean_decl_name (mi.toString()));
    if (l == null)
      ps.printf ("  not called%n");
    else {
      for (DVSet set : l) {
        if ((set.size() == 1) && (set.get(0) instanceof StaticObjInfo))
          continue;
        ArrayList stuff = shinyOutput(set, daikon.DynComp.shiny_print);
        // To see "daikon.chicory.FooInfo:variable", change true to false
        ps.printf ("  [%d] %s%n", stuff.size(), stuff);
      }
    }
  }

  public static void print_comparable_traced (PrintWriter ps, MethodInfo mi) {
    List<DVSet> l = get_comparable (mi.traversalEnter);
    Map<DaikonVariableInfo, DVSet> t =
      get_comparable_traced(mi.traversalEnter);
    ps.printf ("DynComp Traced Tree for %s enter%n",
        clean_decl_name(mi.toString()));
    if (t == null) ps.printf("  not called%n");
    else {
      for(DVSet set : l) {
        if ((set.size() == 1) && (set.get(0) instanceof StaticObjInfo))
          continue;
        print_tree(ps, t,
            (DaikonVariableInfo) TagEntry.troot_find(set.get(0)), 0);
        ps.printf("%n");
      }
    }
    ps.printf("%n");

    l = get_comparable (mi.traversalExit);
    t = get_comparable_traced(mi.traversalExit);
    ps.printf ("DynComp Traced Tree for %s exit%n",
        clean_decl_name(mi.toString()));
    if (t == null) ps.printf("  not called%n");
    else {
      for(DVSet set : l) {
        if ((set.size() == 1) && (set.get(0) instanceof StaticObjInfo))
          continue;
        print_tree(ps, t,
            (DaikonVariableInfo) TagEntry.troot_find(set.get(0)), 0);
        ps.printf("%n");
      }
    }
    ps.printf("%n");
  }

  /**
   * Prints to [stream] the segment of the tree that starts at [node],
   * interpreting [node] as [depth] steps from the root.
   * Requires a Map [tree] that represents a tree though key-value sets
   * of the form <parent, set of children>
   */
  static void print_tree (PrintWriter ps, Map<DaikonVariableInfo, DVSet> tree,
                          DaikonVariableInfo node, int depth) {

    /* This method, for some reason, triggers a segfault due to the way
     * DVSets are handled conceptually. A trace-tree of one element creates
     * a key-value pair DVI foo --> DVSet {foo}, whereas a trace-tree of
     * two elements creates a key-value pair DVI foo --> DVSet {bar}.
     */

    if (depth == 0) {
      ps.printf("%s%n", shinyOutput(node, daikon.DynComp.shiny_print));
      if (tree.get(node) == null) return;
      for (DaikonVariableInfo child : tree.get(node))
        if (child != node) print_tree(ps, tree, child, depth + 1);
    } else {
      for (int i = 0; i < depth; i++) ps.printf("--");
      ps.printf("%s (%s)%n", shinyOutput(node, daikon.DynComp.shiny_print)
                           , TagEntry.get_line_trace(node));
      if (tree.get(node) == null ) return;
      for (DaikonVariableInfo child : tree.get(node))
        if (child != node) print_tree(ps, tree, child, depth + 1);
    }
  }


  /**
   * If on, returns an ArrayList of Strings that converts the usual
   * DVInfo.toString() output to a more readable form
   *
   * e.g. "daikon.chicory.ParameterInfo:foo" becomes "Parameter foo"
   *    "daikon.chicory.FieldInfo:this.foo" becomes "Field foo"
   */
  private static ArrayList<String> shinyOutput(DVSet l, boolean on) {
    ArrayList<String> o = new ArrayList<String>();
    for(DaikonVariableInfo dvi : l)
      o.add(shinyOutput(dvi, on));
    return o;
  }

  private static String shinyOutput(DaikonVariableInfo dv, boolean on) {
    if (!on) return dv.toString();
    String dvtxt = dv.toString();
    String type = dvtxt.split(":")[0];
    type = type.substring(type.lastIndexOf(".") + 1);
    String name = dvtxt.split(":")[1];
    if (type.equals("ThisObjInfo")) { dvtxt = "this"; }
    else if (type.equals("ReturnInfo")) { dvtxt = "return"; }
    else if (type.endsWith("Info")) {
      type = type.substring(0, type.length() - 4);
      if (name.endsWith(".getClass()")) {
        name = name.substring(0, name.length() - 11);
        type = "Class of";
      }
      if (name.startsWith("this.")) {
        name = name.substring(5);
        if (!type.endsWith("Field")) type = (type + " Field").trim();
      }
      dvtxt = type + " " + name;
    }
    return dvtxt;
  }

  /**
   * Set of Daikon variables.  Implements comparable on first DaikonVariable
   * in each set.
   */
  private static class DVSet extends ArrayList<DaikonVariableInfo>
    implements Comparable<DVSet> {
    static final long serialVersionUID = 20050923L;

    public int compareTo (DVSet s1) {
      if (s1.size() == 0)
        return 1;
      else if (size() == 0)
        return -1;
      else return get(0).compareTo(s1.get(0));
    }
    public void sort() {
      Collections.sort (this);
    }
  }


  /**
   * Gets a list of sets of comparable daikon variables.  For simplicity
   * the sets are represented as a list as well.  If the method has never
   * been executed returns null (it would probably be better to return
   * each variable in a separate set, but I wanted to differentiate this
   * case for now).
   *
   * The sets are calculated by processing each daikon variable and adding
   * it to a list associated with the leader of that set.
   */
  static List<DVSet> get_comparable (RootInfo root) {

    if (root == null)
      return (null);

    // List of all of the sets of comparable daikon variables
    Map<DaikonVariableInfo,DVSet> sets
      = new IdentityHashMap<DaikonVariableInfo, DVSet>();

    for (DaikonVariableInfo dv : root)
      add_variable (sets, dv);

    // Get each set, sort it, and add it to the list of all sets.  The sort
    // the list of all sets.  The sorting is not critical except to create
    // a reproducible order
    List<DVSet> set_list = new ArrayList<DVSet>(sets.size());
    for (DVSet dvs : sets.values()) {
      dvs.sort();
      set_list.add (dvs);
    }
    Collections.sort (set_list);

    return (set_list);

  }

  /**
   * Returns a map representing the tree of tracers.
   * Represents the tree as entries in a map with each parent node as the key
   *   to a set contains all its children. The parameter RootInfo node is
   *   included as a key to all its children.
   */
  static Map<DaikonVariableInfo, DVSet> get_comparable_traced (RootInfo root) {
    if (root == null) return null;

    // List of all of the parent-child relationships, where parent-child
    //   represents the equivalence relation of being comparable.
    // The keyset of this Map is exactly the RootInfo node and the set of all
    //   nodes that have children.
    // The valueset of this Map is exactly the set of all nodes.
    Map<DaikonVariableInfo, DVSet> sets
      = new IdentityHashMap<DaikonVariableInfo, DVSet>();

    for (DaikonVariableInfo child : root) add_variable_traced(sets, child);
    for (DVSet dvs : sets.values()) dvs.sort();

    return sets;
  }

  static void add_variable_traced(Map<DaikonVariableInfo, DVSet> sets,
                                  DaikonVariableInfo dv) {
    try {
      DaikonVariableInfo parent =
        (DaikonVariableInfo) TagEntry.tracer_find(dv);
      DVSet set = sets.get(parent);
      if (set == null) { set = new DVSet(); sets.put(parent, set); }
      set.add(dv);
    } catch (NullPointerException e) { }

    for (DaikonVariableInfo child : dv) add_variable_traced(sets, child);

  }


  /**
   * Merges comparability so that the same variable have the same
   * comparability at all points in the program point hierarchy.
   * The comparability at the class/object points is calculated by
   * merging the comparability at each exit point (i.e., if two variables
   * are in the same set it any exit point, they are in the same set at
   * the class point).  That comparability is then applied back to the
   * exit points so that if two class variables are comparable at any
   * exit point they are comparable at each exit point.  Finally exit
   * point comparability is merged to the enter point so that their
   * comparabilities are the same.
   *
   * This is not the only valid definition of comparability but it is
   * the one that Daikon expects because of how equality sets are handled.
   */
  static void merge_class_comparability (ClassInfo ci) {

    // Get the variables at the object and class point
    assert ci.traversalObject != null : ci;
    assert ci.traversalClass != null : ci;
    // ci.init_traversal (depth);

    // If any methods have not been executed, create their information
    // now (which will note all of their variables as not comparable)
    for (MethodInfo mi : ci.method_infos) {
      if (mi.is_class_init())
        continue;
      if (mi.traversalEnter == null) {
        // mi.initViaReflection();
        mi.init_traversal (depth);
        // System.out.printf ("Warning: Method %s never executed%n", mi);
      }
    }

    // Merge the comparability from each exit point into the object point
    for (MethodInfo mi : ci.method_infos) {
      if (mi.is_class_init())
        continue;
      debug_merge_comp.log ("Merging %s exit to object%n", mi);
      merge_dv_comparability (mi.traversalExit, ci.traversalObject);
      merge_dv_comparability (mi.traversalEnter, ci.traversalObject);
    }

    // Merge the comparability from the object point back to each exit point
    for (MethodInfo mi : ci.method_infos) {
      if (mi.is_class_init())
        continue;
      debug_merge_comp.log ("merging object to %s exit%n", mi);
      merge_dv_comparability (ci.traversalObject, mi.traversalExit);
    }

    // Merge the comparability for each exit point back to the enter
    for (MethodInfo mi : ci.method_infos) {
      if (mi.is_class_init())
        continue;
      debug_merge_comp.log ("merging %s exit to its enter%n", mi);
      merge_dv_comparability (mi.traversalExit, mi.traversalEnter);
    }

    // Merge the object comparability to the class
    debug_merge_comp.log ("merging %s object to class%n", ci);
    merge_dv_comparability (ci.traversalObject, ci.traversalClass);
  }

  /**
   * Merges any variables in the dest tree that are in the same set in
   * the source tree.  The source tree's comparability is unchanged.
   * Variables are identified by name
   */
  static void merge_dv_comparability (RootInfo src, RootInfo dest) {

    debug_merge_comp.indent();

    // Create a map relating destination names to their variables
    Map<String,DaikonVariableInfo> dest_map
      = new LinkedHashMap<String,DaikonVariableInfo>();
    for (DaikonVariableInfo dvi : varlist(dest)) {
      dest_map.put (dvi.getName(), dvi);
    }

    // Get the variable sets for the source
    List<DVSet> src_sets = get_comparable (src);

    // Merge any destination variables that are in the same source set
    for (DVSet set : src_sets) {
      if (set.size() == 1)
        continue;
      DaikonVariableInfo first_match = null;
      for (DaikonVariableInfo dvi : set) {
        if (first_match == null) {
          first_match = dest_map.get (dvi.getName());
          continue;
        }
        DaikonVariableInfo second_match = dest_map.get (dvi.getName());
        if (second_match != null) {
          TagEntry.union (first_match, second_match);
          debug_merge_comp.log ("merged '%s' and '%s'%n", first_match,
                                second_match);
        }
      }
    }
    debug_merge_comp.exdent();
  }


  /**
   * Adds this daikon variable and all of its children into their appropriate
   * sets (those of their leader) in sets.
   */
  static void add_variable (Map<DaikonVariableInfo,DVSet> sets,
                            DaikonVariableInfo dv) {

    // Add this variable into the set of its leader
    DaikonVariableInfo leader = (DaikonVariableInfo) TagEntry.find (dv);
    DVSet set = sets.get (leader);
    if (set == null) {
      set = new DVSet();
      sets.put (leader, set);
    }
    set.add (dv);

    // Process the children
    for (DaikonVariableInfo child : dv)
      add_variable (sets, child);

  }

  /**
   * Pushes the tag associated with field_num in obj on the tag stack.
   * A tag value must have been previously stored for this field.  Use
   * push_field_tag_null_ok() if the tag may not have been previously
   * stored.
   */
  public static void push_field_tag (Object obj, int field_num) {

    // Since instance variables by default initialize to zero, any field
    // can possibly be read before it is set.  So we can't use this crosscheck.
    if (true) {
      push_field_tag_null_ok (obj, field_num);
      return;
    }


    Object[] obj_tags = field_map.get (obj);
    if (obj_tags != null) {
      Object tag = obj_tags[field_num];
      assert tag != null : "Object " +obj.getClass() + " '"+ obj
        + "' field_num " + field_num;
      tag_stack.push (tag);
      if (debug_primitive.enabled())
        debug_primitive.log ("push_field_tag %s [%s] %d = %s%n", obj,
                     obj.getClass().getName(), field_num, obj_tags[field_num]);
    } else {
      assert false : "Object " +obj.getClass() + " '"+ obj + "' field_num "
        + field_num;
      tag_stack.push (null);
      if (debug_primitive.enabled())
        debug_primitive.log ("push_field_tag %s [%s] %d = null%n", obj,
                           obj.getClass().getName(), field_num);
    }
  }

  /**
   * Pushes the tag associated with field_num in obj on the tag stack.
   * If tag storage for this object has not been previously allocated it
   * is allocated now and a tag is allocated for this field.  This should
   * only be called for objects whose fields can be read without having been
   * previously written (in java)
   */
  public static void push_field_tag_null_ok (Object obj, int field_num) {

    Object[] obj_tags = field_map.get (obj);
    if (obj_tags != null) {
      Object tag = obj_tags[field_num];
      if (tag == null)
        obj_tags[field_num] = tag = new UninitFieldTag();
      tag_stack.push (tag);
      if (debug_primitive.enabled())
        debug_primitive.log ("push_field_tag %s [%s] %d = %s%n", obj,
                     obj.getClass().getName(), field_num, obj_tags[field_num]);
    } else {
      Class obj_class = obj.getClass();
      int fcnt = num_prim_fields (obj.getClass());
      assert field_num < fcnt : obj.getClass() + " " + field_num + " " + fcnt;
      obj_tags = new Object[fcnt];
      field_map.put (obj, obj_tags);
      if (debug_primitive.enabled())
        debug_primitive.log ("push_field_tag: Created tag storage%n");
      Object tag = new UninitFieldTag();
      obj_tags[field_num] = tag;
      tag_stack.push (tag);
      if (debug_primitive.enabled())
        debug_primitive.log ("push_field_tag %s [%s] %d = %s%n", obj,
                           obj.getClass().getName(), field_num, tag);
    }
  }

  /**
   * Pops the tag from the top of the tag stack and stores it in the
   * tag storage for the specified field of the specified object.  If
   * tag storage was not previously allocated, it is allocated now
   */
  public static void pop_field_tag (Object obj, int field_num) {

    // Look for the tag storage for this object
    Object[] obj_tags = field_map.get (obj);

    // If none has been allocated, determine how many locations are
    // required (the number of primitive fields), allocate the space,
    // and associate it with the object.
    if (obj_tags == null) {
      Class obj_class = obj.getClass();
      int fcnt = num_prim_fields (obj.getClass());
      assert field_num < fcnt : obj.getClass() + " " + field_num + " " + fcnt;
      obj_tags = new Object[fcnt];
      field_map.put (obj, obj_tags);
      debug_primitive.log ("pop_field_tag: Created tag storage%n");
    }

    // Pop the tag off of the stack and assign into the tag storage for
    // this field.
    check_method_marker();
    Object tag = tag_stack.pop();
    assert tag != null : "Object " +obj.getClass() + " '"+ obj
      + "' field_num " + field_num;
    obj_tags[field_num] = tag;
    debug_primitive.log ("pop_field_tag (%s [%s] %d = %s%n", obj,
                   obj.getClass().getName(), field_num, obj_tags[field_num]);

  }

    /**
     * Return the number of primitive fields in clazz and all of its
     * superclasses
     */
    public static int num_prim_fields (Class clazz) {
      if (clazz == Object.class)
        return 0;
      else {
        int field_cnt = num_prim_fields (clazz.getSuperclass());
        for (Field f : clazz.getDeclaredFields()) {
          if (f.getType().isPrimitive())
            field_cnt++;
        }
        return (field_cnt);
      }
    }

  /**
   * Handle a binary operation on the two items at the top of the tag
   * stack.  Binary operations pop the two items off of the top of the
   * stack perform an operation and push the result back on the stack.
   * The tags of the two items on the top of the stack must thus be
   * merged and a representative tag pushed back on the stack.
   */
  public static void binary_tag_op () {
    debug_primitive.log ("binary tag op%n");
    check_method_marker();
    Object tag1 = tag_stack.pop();
    check_method_marker();
    TagEntry.union (tag1, tag_stack.peek());
  }

  /**
   * Handles an i_cmpXX operation.  This opcode compares the two integers
   * on the top of the stack and jumps accordingly.  Thus the two tags on
   * the top of the stack are popped from the tag stack and merged.
   * Very similar to binary_tag_op except that nothing is pushed back on
   * the tag stack.
   */
  public static void cmp_op() {
    debug_primitive.log ("cmp_op%n");
    Object tag1 = pop_check();
    TagEntry.union (tag1, pop_check());
  }

  /** Handles a dup opcode on a primitive **/
  public static void dup() {
    check_method_marker();
    tag_stack.push (tag_stack.peek());
  }

  /** Handles a dup_x1 opcode on a primitive **/
  public static void dup_x1() {
    Object top = pop_check();
    Object nxt = pop_check();
    tag_stack.push (top);
    tag_stack.push (nxt);
    tag_stack.push (top);
  }

  /**
   * Handles a dup_x2 opcode on a primitive.  Currently only support
   * category 1 computational types
   **/
  public static void dup_x2() {
    Object top = pop_check();
    Object tag1 = pop_check();
    Object tag2 = pop_check();
    tag_stack.push (top);
    tag_stack.push (tag2);
    tag_stack.push (tag1);
    tag_stack.push (top);
  }

  public static void dup2() {
    Object top = pop_check();
    Object tag1 = pop_check();
    tag_stack.push (tag1);
    tag_stack.push (top);
    tag_stack.push (tag1);
    tag_stack.push (top);
  }

  public static void dup2_x1() {
    Object top = pop_check();
    Object tag1 = pop_check();
    Object tag2 = pop_check();
    tag_stack.push (tag1);
    tag_stack.push (top);
    tag_stack.push (tag2);
    tag_stack.push (tag1);
    tag_stack.push (top);
  }

  public static void dup2_x2() {
    Object top = pop_check();
    Object tag1 = pop_check();
    Object tag2 = pop_check();
    Object tag3 = pop_check();
    tag_stack.push (tag1);
    tag_stack.push (top);
    tag_stack.push (tag3);
    tag_stack.push (tag2);
    tag_stack.push (tag1);
    tag_stack.push (top);
  }

  /** swaps the two elements on the top of the tag stack **/
  public static void swap() {
    Object top = pop_check();
    Object tag1 = pop_check();
    tag_stack.push (top);
    tag_stack.push (tag1);
  }

  /**
   * Handles the various primitive (int, double, etc) array load instructions.
   * The array and its index are made comparable.  The tag for the
   * index is removed from the tag stack and the tag for the array
   * element is pushed on the stack.  The tag for the specified index
   * must exist.  If it is reasonable for the tag not to exist, then
   * primitive_array_load_null_ok() should be used instead.
   */
  public static void primitive_array_load (Object arr_ref, int index) {

    if (true) {
      primitive_array_load_null_ok (arr_ref, index);
      return;
    }

    // Get the tag for the index and mark it as comparable with the array
    Object index_tag = pop_check();
    debug_arr_index.log ("Merging array '%s' and index '%s'", arr_ref,
                         index_tag);
    if (merge_arrays_and_indices)
      TagEntry.union (arr_ref, index_tag);

    // Push the tag for the element on the tag stack.
    Object[] obj_tags = field_map.get (arr_ref);
    if (obj_tags != null) {
      Object tag = obj_tags[index];
      assert tag != null : "null tag: index " + index + " in array " + arr_ref;
      tag_stack.push (tag);
      if (debug_primitive.enabled())
        debug_primitive.log ("arrayload %s[%d] = %s%n", arr_ref, index,
                           obj_str(obj_tags[index]));
    } else {
      assert false : "no tag storage: index " + index + " in array " + arr_ref;
      tag_stack.push (null);
      debug_primitive.log ("iaload %s[%d]  = null%n", arr_ref, index);
    }
  }


  /**
   * Handles the various primitive (int, double, etc) array load instructions.
   * The array and its index are made comparable.  The tag for the
   * index is removed from the tag stack and the tag for the array
   * element is pushed on the stack.  Unlike primitive_array_load(), this
   * method handles array elements whose tags have not previously been
   * set.  This can happen when the JVM sets an array element directly and
   * there is no corresponding java code that can set the tag.
   */
  public static void primitive_array_load_null_ok (Object arr_ref, int index) {

    // Get the tag for the index and mark it as comparable with the array
    Object index_tag = pop_check();
    debug_arr_index.log ("Merging array '%s' and index '%s'", arr_ref,
                         index_tag);
    if (merge_arrays_and_indices)
      TagEntry.union (arr_ref, index_tag);

    // Push the tag for the element on the tag stack.
    Object[] obj_tags = field_map.get (arr_ref);
    if (obj_tags != null) {
      Object tag = obj_tags[index];
      if (tag == null)
        obj_tags[index] = tag = new UninitArrayElem();
      tag_stack.push (tag);
      if (debug_primitive.enabled())
        debug_primitive.log ("arrayload null-ok %s[%d] = %s%n", arr_ref,
                             index, obj_str(obj_tags[index]));
    } else {
      int length = Array.getLength (arr_ref);
      obj_tags = new Object[length];
      field_map.put (arr_ref, obj_tags);
      Object tag = new UninitArrayElem();
      obj_tags[index] = tag;
      tag_stack.push (tag);
      if (debug_primitive.enabled())
        debug_primitive.log ("arrayload null-ok %s[%d] = null%n", arr_ref,
                             index);
    }
  }

  /**
   * Handles the aaload instruction.  The arry and its index are made
   * comparable.  The tag for the index is removed from the tag
   * stack.
   */
  public static void ref_array_load (Object arr_ref, int index) {

    // Get the tag for the index and mark it as comparable with the array
    Object index_tag = pop_check();
    debug_arr_index.log ("Merging array '%s' and index '%s'", arr_ref,
                         index_tag);
    if (merge_arrays_and_indices)
      TagEntry.union (arr_ref, index_tag);
  }

  /**
   * Allocate a new tag for the constant and push it on the tag stack.
   * Note that this allocates a new tag each time the constant is pushed.
   * If the same code is executed multiple time (eg, in a loop), and
   * different values interact with the constant each time, those values
   * will not end up comparable to each other.
   */
  public static void push_const() {
    Object tag = new Constant();
    debug_primitive.log ("pushing literal constant %s%n", tag);
    tag_stack.push (tag);
  }

  /**
   * Marks the specified class as initialized.  We don't look at static
   * variables in classes until they are initialized
   */
  public static void class_init (String classname) {
    init_classes.add (classname);
  }

  /**
   * Returns whether or not the specified class is initialized
   */
  public static boolean is_class_init (Class clazz) {
    return (init_classes.contains (clazz.getName()));
  }

  /**
   * Returns the name of the method that called the caller of
   * caller_name()
   */
  private static String caller_name() {

    Throwable stack = new Throwable ("caller");
    stack.fillInStackTrace();
    StackTraceElement[] ste_arr = stack.getStackTrace();
    StackTraceElement ste = ste_arr[2];
    return (ste.getClassName() + "." + ste.getMethodName());
  }

  /**
   * Make sure that the top of the stack is not the method marker.  Should
   * be called before every pop
   */
  private static void check_method_marker() {

    assert tag_stack.peek() != method_marker;
  }

  /**
   * Pops the top item off the stack after checking to insure that it
   * is not the marker between methods
   */
  private static Object pop_check() {
    check_method_marker();
    return tag_stack.pop();
  }

  /**
   * Returns a string description of the object that includes its class,
   * identity hash code, and the result of its toString() function (if it
   * overrides the default implementation
   */
  private static String obj_str (Object obj) {

    if (obj == null)
      return ("null");
    else {
      String tostring = obj.toString();
      String default_tostring = String.format ("%s@%x",
                                               obj.getClass().getName(),
                                               System.identityHashCode (obj));
      if (tostring.equals (default_tostring))
        return tostring;
      else
        return String.format ("%s [%s]", default_tostring, tostring);
    }
  }

  /**
   * Returns all of the daikonvariables in the tree rooted at dvi
   * in a list
   */
  private static List<DaikonVariableInfo> varlist (DaikonVariableInfo dvi) {

    List<DaikonVariableInfo> list = new ArrayList<DaikonVariableInfo>();
    list.add (dvi);
    for (DaikonVariableInfo child : dvi) {
      list.addAll (varlist (child));
    }
    return (list);
  }

  /**
   * Returns the name of the tag field that corresponds to the specified
   * field
   */
  public static String tag_field_name (String field_name) {
    return (field_name + "__$tag");
  }

  private static Matcher jdk_decl_matcher
    = Pattern.compile ("(, )?java.lang.DCompMarker( marker)?").matcher("");
  private static Matcher non_jdk_decl_matcher
    = Pattern.compile ("(, )?daikon.dcomp.DCompMarker( marker)?").matcher("");

  /**
   * Removes DCompMarker from the signature
   */
  public static String clean_decl_name (String decl_name) {

    if (DCInstrument.jdk_instrumented) {
      jdk_decl_matcher.reset (decl_name);
      return jdk_decl_matcher.replaceFirst("");
    } else {
      non_jdk_decl_matcher.reset (decl_name);
      return non_jdk_decl_matcher.replaceFirst("");
    }
  }

  /**
   * Abstract base class for code that gets the tag associated with
   * a particular field.  There are specific implementors for the various
   * types of fields.  FieldTag instances are stored in FieldInfo so that
   * tags can be efficiently obtained.
   */
  public static abstract class FieldTag {

    /**
     * Gets the tag for the field
     * @param parent Object that contains the field (if any)
     * @param obj Value of the field itself (if available and if its an
     *            object
     */
    abstract Object get_tag (Object parent, Object obj);
  }

  /**
   * Class that gets the tag for static primitive fields.  We retrieve
   * the static tag by using the same method as we use during runtime
   * to push the tag on the tag stack.
   */
  public static class StaticPrimitiveTag extends FieldTag {

    Method get_tag;

    /** Initialize with information from the field **/
    StaticPrimitiveTag (FieldInfo fi) {
      assert fi.isStatic();
      assert fi.isPrimitive();
      Field field = fi.getField();
      Class<?> clazz = field.getDeclaringClass();
      String name = DCInstrument.tag_method_name (DCInstrument.GET_TAG,
                                     clazz.getName(), field.getName());
      try {
        get_tag = clazz.getMethod (name);
      } catch (Exception e) {
        throw new Error ("can't find tag method " + name , e);
      }
    }

    /** Return the tag associated with this field **/
    Object get_tag (Object parent, Object obj) {
      Object tag = null;
      // jhp - not sure why these are not null...
      //assert parent == null && obj == null
      //  : " parent/obj = " + obj_str(parent) + "/" + obj_str(obj);
      try {
        Object ret_val = get_tag.invoke (parent);
        assert ret_val == null;
        tag = pop_check();
        assert tag != null;
      } catch (Exception e) {
        throw new Error ("can't execute tag method " + get_tag , e);
      }
      return (tag);
    }
  }


  /**
   * Class that gets the tag for a static reference variable.  The
   * tag for a reference variable is the object itself, so that is
   * obtained via reflection
   */
  public static class StaticReferenceTag extends FieldTag {

    /** Corresponding java field **/
    Field field;

    /** Set to true when the class containing the field is initialized **/
    boolean is_class_initialized = false;

    /** Class that contains the field **/
    Class declaring_class;

    /** Initialize for this field **/
    public StaticReferenceTag (FieldInfo fi) {

      assert fi.isStatic();
      assert !fi.isPrimitive();
      field = fi.getField();
      declaring_class = field.getDeclaringClass();
    }

    /** Gets the tag for this static reference **/
    public Object get_tag (Object parent, Object obj) {

      // assert parent == null && obj == null;
      if (!is_class_initialized) {
        if (is_class_init (declaring_class)) {
          if (!field.isAccessible())
            field.setAccessible (true);
          is_class_initialized = true;
        } else {
          return nonsensical;
        }
      }

      try {
        return (field.get (null));
      } catch (Exception e) {
        throw new RuntimeException("Can't get val for static field "
                                   + field, e);
      }
    }
  }

  /**
   * Class that gets the list of tags for primitive arrays.  Note that
   * primitive arrays can both be actual arrays of primitives and also
   * arrays of classes containing primitives.  In the second case, there
   * is a separate object that contains each of the 'array' values
   */
  public static class PrimitiveArrayTag extends FieldTag {

    /** The field number for this field inside its object **/
    int field_num;

    public PrimitiveArrayTag (FieldInfo fi) {
      assert !fi.isStatic() && fi.isPrimitive() && fi.isArray();
      field_num = fi.get_field_num();
    }

    /** Returns a list of object tags **/
    public Object get_tag (Object parent, Object obj) {

      // Object is an array of objects containing each item
      // assert obj == null: "primitive array object = " + obj_str (obj);
      @SuppressWarnings("unchecked")
      List<Object> parent_list = (List<Object>)parent;
      Field tag_field = null;
      List<Object> tag_list = new ArrayList<Object>(parent_list.size());
      for (Object parent_element : parent_list) {
        Object[] tags = field_map.get (parent_element);
        if (tags == null)
          tag_list.add (nonsensical);
        else
          tag_list.add (tags[field_num]);
      }
      return (tag_list);
    }
  }

  /**
   * Class that gets the tag for a primitive instance field.
   * Each object with primitive fields has a corresponding tag array
   * in field map.  The tag for a particular field is stored at that
   * fields offset in the tag array.
   */
  public static class PrimitiveTag extends FieldTag {

    int field_num;

    public PrimitiveTag (FieldInfo fi) {
      assert !fi.isStatic() && fi.isPrimitive() && !fi.isArray();
      field_num = fi.get_field_num();
    }

    public Object get_tag (Object parent, Object obj) {

      // obj is the wrapper for the primitive
      // assert obj == null: "primitive object = " + obj_str (obj);
      Object[] tags = field_map.get (parent);
      if (tags == null)
        return (nonsensical); // happens if field has never been assigned to
      else
        return (tags[field_num]);
    }
  }

  /**
   * Class that returns the tag for a reference instance field.  In
   * this case, the tag is just the object itself
   */
  public static class ReferenceTag extends FieldTag {

    public ReferenceTag (FieldInfo fi) {
      assert !fi.isStatic() && !fi.isPrimitive();
    }

    public Object get_tag (Object parent, Object obj) {
      return (obj);
    }
  }
}
