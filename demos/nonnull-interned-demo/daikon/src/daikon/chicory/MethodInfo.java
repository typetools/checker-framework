package daikon.chicory;

import java.util.*;
import java.lang.reflect.*;

/**
 * Keeps information about a method that is useful for writing out
 * decl and/or dtrace information.  Original information is filled
 * out during the transformation and other information is added the
 * first time a method is called.
 */
public class MethodInfo {

  /** Class that contains this method **/
  public ClassInfo class_info = null;

  /** Reflection information on this method **/
  public Member member = null;

  /**
   * Method name.  For example: "public static void sort(int[] arr)"
   * would have method_name "sort"
   **/
  public String method_name;

  /** Array of argument names for this method **/
  public String[] arg_names;

  /**
   * Array of argument types for this method (fully qualified).  For
   * example: "public static void examineObject(Object x)" would have
   * arg_types {"java.lang.Object"}
   **/
  public String[] arg_type_strings;

  /** Array of argument types as classes for this method **/
  public Class[] arg_types;

  /** exit locations for this method **/
  public List<Integer> exit_locations;

  /**
   * Tells whether each exit point in method is instrumented, based on
   * filters **/
  public List<Boolean> is_included;

  /**
   * The root of the variable tree for the method entry program point.
   *
   * Set by DeclWriter and read by DTraceWriter.
   **/
  public RootInfo traversalEnter = null;

  /**
   * The root of the variable tree for the method exit program point(s).
   *
   * Set by DeclWriter and read by DTraceWriter.
   **/
  public RootInfo traversalExit = null;

  /** The number of times this method has been called **/
  public int call_cnt = 0;

  /** The number of times we have captured the output for this method **/
  public int capture_cnt = 0;

  /**
   * Whether or not the method is pure (has no side-effects).
   * Will only be set to true if the --purity-analysis switch is given
   * to Chicory, and the method returns some value and takes no parameters.
   * Only set during initViaReflection() method
   */
  private boolean isPure;

  /**
   * Creates a MethodInfo with the specified class, arg_names, and
   * exit locations
   */
  public MethodInfo (ClassInfo class_info, String method_name,
                     String[] arg_names, String[] arg_type_strings,
                     List<Integer> exit_locations,
                     List<Boolean> is_included) {

    this.class_info = class_info;
    this.method_name = method_name;
    this.arg_names = arg_names;
    this.arg_type_strings = arg_type_strings;
    this.exit_locations = exit_locations;
    this.is_included = is_included;
  }

  private static HashMap<String,Class> primitive_classes
    = new HashMap<String,Class>(8);
  static {
    primitive_classes.put("Z", Boolean.TYPE);
    primitive_classes.put("B", Byte.TYPE);
    primitive_classes.put("C", Character.TYPE);
    primitive_classes.put("D", Double.TYPE);
    primitive_classes.put("F", Float.TYPE);
    primitive_classes.put("I", Integer.TYPE);
    primitive_classes.put("J", Long.TYPE);
    primitive_classes.put("S", Short.TYPE);
  }

  /** Populates this class with data from reflection **/
  public void initViaReflection () {

    // Get the Class for each argument type
    arg_types = new Class[arg_names.length];
    for (int ii = 0; ii < arg_type_strings.length; ii++) {
      try {
        String aname = arg_type_strings[ii];
        Class c = primitive_classes.get (aname);

        if (c == null)
        {
          //c = Class.forName (aname);
          //change class loading
          //TODO referring class?
          c = Class.forName (aname, false, this.class_info.clazz.getClassLoader());
        }

        arg_types[ii] = c;
      } catch (Exception e) {
        throw new Error ("can't find class for " + arg_type_strings[ii]
                         + " in  method "+ class_info.class_name + "."
                         + method_name + ": " + e);
      }
    }

    // Look up the method
    try {
      if (is_class_init())
        member = null;
      else if (is_constructor())
        member = class_info.clazz.getDeclaredConstructor (arg_types);
      else
        member = class_info.clazz.getDeclaredMethod (method_name, arg_types);
    } catch (Exception e) {
      throw new Error ("can't find method " + method_name, e);
    }


    if (ChicoryPremain.shouldDoPurity())
    {
        int mod = member.getModifiers();


        // Only consider purity on non-abstract, non-static, non-constructor
        // methods which return a value and take no parameters!
        if (!Modifier.isAbstract(mod) && !Modifier.isStatic(mod) &&
                !(member instanceof Constructor) &&
                !((Method) member).getReturnType().equals(Void.TYPE) &&
                ((Method) member).getParameterTypes().length == 0)
        {
            if (ChicoryPremain.isMethodPure(member))
            {
                isPure = true;
            }
        }
    }
  }

  /**
   * Returns true iff this method is a constructor
   * @return true iff this method is a constructor
   */
  public boolean is_constructor() {
    return (method_name.equals ("<init>") || method_name.equals(""));
  }

  /** Returns whether or not this method is a class initializer **/
  public boolean is_class_init() {
    return (method_name.equals ("<clinit>"));
  }

  /** Returns whether or not this method is static **/
  public boolean is_static() {
    return Modifier.isStatic(member.getModifiers());
  }

  /**
   * Initialize the enter and exit daikon variable trees (traversalEnter and
   * traversalExit).  The reflection information must have already been
   * initialized.
   */
  public void init_traversal (int depth) {

    traversalEnter = RootInfo.enter_process (this, depth);
    // System.out.printf ("Method %s.%s: %n ", class_info.clazz.getName(),
    //                    this);
    // System.out.printf ("Enter daikon variable tree%n%s%n",
    //                    traversalEnter.treeString());

    traversalExit = RootInfo.exit_process (this, depth);
    // System.out.printf ("Exit daikon variable tree%n%s%n",
    //                    traversalExit.treeString());
  }


  public String toString() {
    String out = "";
    if (class_info != null)
      out = class_info.class_name + ".";
    out += method_name + "(";
    for (int ii = 0; ii < arg_names.length; ii++) {
      if (ii > 0)
        out += ", ";
      out += arg_type_strings[ii] + " " + arg_names[ii];
    }
    return (out + ")");
  }

  public boolean isPure()
  {
      return isPure;
  }

  /** Returns the turn type of the method.  Constructors return Void.TYPE **/
  public Class return_type() {
    if (member instanceof Method) {
      Method m = (Method) member;
      return m.getReturnType();
    } else {
      return Void.TYPE;
    }
  }
}
