package daikon.chicory;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * Keeps information about a class that is useful for writing out
 * decl and/or dtrace information.  Original information is filled
 * out during the transformation and other information is added the
 * after the class is first loaded
 */
public class ClassInfo {

  /** fully qualified name of the  class **/
  public String class_name;

  /** reflection object for this class **/
  public Class<?> clazz;

  /** list of methods in the class **/
  public List<MethodInfo> method_infos = new ArrayList<MethodInfo>();

  /** this class's classloader**/
  private ClassLoader loader;

  /** DaikonVariables for the object (instance and static) **/
  public RootInfo traversalObject;

  /** DaikonVariables for the class (static vars only) **/
  public RootInfo traversalClass;

  /** Whether or not any methods in this class were instrumented **/
  public boolean shouldInclude = false;

  /** Mapping from field name to string representation of its value**/
  //only for static final primitives
  //which are declared by a CONSTANT VALUE in the code
  public Map <String, String> staticMap = new HashMap<String,String>();

  /** Create ClassInfo with specified name **/
  public ClassInfo (String class_name, ClassLoader theLoader) {
    this.class_name = class_name;
    loader = theLoader;
  }

  /** Set the list of methods **/
  public void set_method_infos (List<MethodInfo> method_infos) {
    this.method_infos = method_infos;
  }

  public List<MethodInfo> get_method_infos() {
    return (method_infos);
  }

  /**
   * Gets the reflection object Class for this class and the Method objects
   * for each method
   */
  public void initViaReflection() {

    // get the reflection class
    try {
      //clazz = Class.forName (class_name);
      //change class loading

        //TODO referring class?
      clazz = Class.forName (class_name, false, loader);

    } catch (Exception e) {
      throw new Error (e);
    }

    for (MethodInfo mi : method_infos)
      mi.initViaReflection();

    if (ChicoryPremain.shouldDoPurity())
    {
        for (String pureMeth: ChicoryPremain.getPureMethods())
        {
            if (isInThisClass(pureMeth))
            {
                boolean foundMatch = false;
                for (MethodInfo mi: method_infos)
                {
                  // System.out.println(mi.member.toString() + "\n" + pureMeth + "\n\n");
                    if (mi.member.toString().trim().equals(pureMeth))
                    {
                        foundMatch = true;
                        break;
                    }
                }

                if (!foundMatch)
                {
                    // pureMeth must not actually be in this class
                    throw new Error(String.format("Could not find pure method \"%s\" in class %s", pureMeth, clazz));
                }
            }
        }
    }
  }

  /**
   * Determines if fully qualified method name is in this class
   * Example methodName: public static String doStuff(int, java.lang.Object)
   */
  private boolean isInThisClass(String methodName)
  {
      // A heuristical way to determine if the method is in this class.
      // Match anything of the form: ____class_name____(____
      // Where ____ corresponds to any sequence of characters
      return methodName.matches(".*" + class_name + ".*\\(.*");
  }

  /** dumps all of the class info to the specified stream **/
  public void dump (PrintStream ps) {
    ps.printf ("ClassInfo for %s [%s]%n", class_name, clazz);
    for (MethodInfo mi : method_infos) {
      ps.printf ("  method %s [%s]%n", mi.method_name, mi.member);
      ps.printf ("    arguments: ");
      for (int ii = 0; ii < mi.arg_names.length; ii++) {
        if (ii > 0)
          ps.printf (", ");
        ps.printf ("%s [%s] %s", mi.arg_type_strings[ii], mi.arg_types[ii],
                   mi.arg_names[ii]);
      }
      ps.printf ("%n    exits: ");
      for (Integer exit_loc : mi.exit_locations)
        ps.printf ("%s ", exit_loc);
      ps.printf ("%n");
    }
  }

  /** Initializes the daikon variables for the object and class ppts **/
  public void init_traversal (int depth) {
    if (traversalObject == null)
      traversalObject = RootInfo.getObjectPpt (this, depth);
    if (traversalClass == null)
      traversalClass = RootInfo.getClassPpt (this, depth);
    assert traversalObject != null : class_name;
    assert traversalClass != null : class_name;

  }

  public String toString() {
    return (String.format ("ClassInfo %08X [%s] %s",
                           System.identityHashCode (this), class_name, clazz));
  }
}
