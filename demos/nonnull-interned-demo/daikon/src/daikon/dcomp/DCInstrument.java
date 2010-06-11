package daikon.dcomp;

import java.util.*;
import java.util.regex.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.io.*;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.verifier.*;
import org.apache.bcel.verifier.structurals.*;
import utilMDE.BCELUtil;

import utilMDE.*;
import org.apache.commons.io.*;

import daikon.chicory.MethodInfo;
import daikon.chicory.ClassInfo;
import daikon.chicory.DaikonWriter;

import daikon.DynComp;

/**
 * Instruments a class file to perform Dynamic Comparability.
 */
class DCInstrument {

  private JavaClass orig_class;
  private ClassGen gen;
  private ConstantPoolGen pool;
  private boolean in_jdk;
  private InstructionFactory ifact;
  private ClassLoader loader;

  /** Local that stores the tag frame for the current method **/
  private LocalVariableGen tag_frame_local;

  // Argument descriptors
  private static Type[] two_objects = new Type[] {Type.OBJECT, Type.OBJECT};
  // private Type[] two_ints = new Type[] {Type.INT, Type.INT};
  private static Type[] object_int = new Type[] {Type.OBJECT, Type.INT};
  private static Type[] string_arg = new Type[] {Type.STRING};
  private static Type[] integer_arg = new Type[] {Type.INT};
  private static Type[] object_arg = new Type[] {Type.OBJECT};

  // Type descriptors
  private static Type object_arr = new ArrayType (Type.OBJECT, 1);
  // private Type int_arr = new ArrayType (Type.INT, 1);
  private static ObjectType throwable = new ObjectType ("java.lang.Throwable");
  private static ObjectType dcomp_marker = null;

  // Debug loggers
  private SimpleLog debug_instrument = new SimpleLog (false);
  private SimpleLog debug_instrument_inst = new SimpleLog (false);
  private SimpleLog debug_native = new SimpleLog (false);
  private SimpleLog debug_dup = new SimpleLog (false);
  private SimpleLog debug_add_dcomp = new SimpleLog (false);
  private SimpleLog debug_track = new SimpleLog (false);

  /**
   * Keeps track of the methods that were not successfully instrumented.
   */
  private List<String> skipped_methods = new ArrayList<String>();

  /**
   * Specifies if the jdk is instrumented.  Calls to the JDK must be
   * modified to remove the arguments from the tag stack if it is not
   * instrumented.
   */
  public static boolean jdk_instrumented = true;
  private static boolean exclude_object = true;
  private static boolean use_StackVer = true;

  /**
   * Don't instrument toString functions.  Useful in debugging since
   * we call toString on objects from our code (which then triggers
   * (recursive) instrumentation)
   */
  private static boolean ignore_toString = true;

  /**
   * Double client methods like we do in the JDK.  This allows our
   * non-instrumentation of object methods to work better in client
   * code when they call other methods (because original versions of
   * those other methods will exist
   */
  private static boolean double_client = true;

  public static final String SET_TAG = "set_tag";
  public static final String GET_TAG = "get_tag";

  /**
   * Map from each static field name to its unique integer id
   * Note that while its intuitive to think that each static should
   * show up exactly once, that is not the case.  A static defined in a
   * superclass can be accessed through each of its subclasses.  Tag
   * accessor methods must be added in each subclass and each should
   * return the same id.  We thus will lookup the same name multiple
   * times.
   **/
  static Map<String,Integer> static_map = new LinkedHashMap<String,Integer>();

  /**
   * Array of classes whose fields are not initialized from java.  Since
   * the fields are not initialized from java, their tag storage is not
   * allocated as part of a store, but rather must be allocated as part
   * of a load.  We call a special runtime method for this so that we
   * can check for this in other cases
   */
  private static String[] uninit_classes = new String[] {
    "java.lang.String",
    "java.lang.Class",
    "java.lang.StringBuilder",
    "java.lang.AbstractStringBuilder",
  };

  /**
   * List of Object methods.  Since we can't instrument Object, none
   * of these can be instrumented, and most of them don't provide
   * useful comparability information anyway.  I've also added
   * newInstance because of a problem with code that the JDK generates
   * for newInstance.
   * The equals method IS instrumented.
   */
  private static MethodDef[] obj_methods = new MethodDef[] {
    new MethodDef ("finalize", new Type[0]),
    new MethodDef ("hashCode", new Type[0]),
    new MethodDef ("toString", new Type[0]),
    new MethodDef ("wait", new Type[0]),
    new MethodDef ("wait", new Type[] {Type.LONG}),
    new MethodDef ("wait", new Type[] {Type.LONG, Type.INT}),
    new MethodDef ("getClass", new Type[0]),
    new MethodDef ("notify", new Type[0]),
    new MethodDef ("notifyall", new Type[0]),
    new MethodDef ("newInstance", new Type[] {object_arr}),
  };

  /** Class that defines a method (by its name and argument types) **/
  static class MethodDef {
    String name;
    Type[] arg_types;

    MethodDef (String name, Type[] arg_types) {
      this.name = name;
      this.arg_types = arg_types;
    }

    boolean equals (String name, Type[] arg_types) {
      if (!name.equals (this.name))
        return false;
      if (this.arg_types.length != arg_types.length)
        return false;
      for (int ii = 0;  ii < arg_types.length; ii++)
        if (!arg_types[ii].equals (this.arg_types[ii]))
          return (false);
      return (true);
    }
  }

  /**
   * Initialize with the original class and whether or not the class
   * is part of the JDK
   */
  public DCInstrument (JavaClass orig_class, boolean in_jdk,
                       ClassLoader loader) {
    this.orig_class = orig_class;
    this.in_jdk = in_jdk;
    this.loader = loader;
    gen = new ClassGen (orig_class);
    pool = gen.getConstantPool();
    ifact = new InstructionFactory (gen);
    if (jdk_instrumented)
      dcomp_marker = new ObjectType ("java.lang.DCompMarker");
    else
      dcomp_marker = new ObjectType ("daikon.dcomp.DCompMarker");
    // System.out.printf ("DCInstrument %s%n", orig_class.getClassName());
  }

  /**
   * Instruments the original class to perform dynamic comparabilty and
   * returns the new class definition
   */
  public JavaClass instrument() {

    String classname = gen.getClassName();

    // Removed this check (1/20/06) as we are already doing this in premain.
    // Probably a better solution is needed.
    // Don't instrument classes in the JDK.  They are already instrumented.
    // Do instrument javac (its not in the JDK)
    // TODO: crosscheck the class for instrumentation rather than by name.
    //if (BCELUtil.in_jdk (gen)
    //    && !classname.startsWith ("com.sun.tools.javac")) {
    //  debug_track.log ("Skipping jdk class %s%n", gen.getClassName());
    //  return (null);
    // }

    // Don't instrument our classes.
    if (classname.startsWith ("daikon") &&
        !classname.startsWith ("daikon.dcomp.Test")) {
      debug_track.log ("Skipping daikon class %s%n", gen.getClassName());
      return (null);
    }

    // Don't instrument annotations.  They aren't executed and adding
    // the marker argument causes subtle errors
    if ((gen.getModifiers() & Constants.ACC_ANNOTATION) != 0) {
      debug_track.log ("Not instrumenting annotation %s%n",gen.getClassName());
      return gen.getJavaClass().copy();
    }

    debug_instrument.log ("Instrumenting class %s%n", gen.getClassName());
    debug_instrument.indent();

    // Create the ClassInfo for this class and its list of methods
    ClassInfo class_info = new ClassInfo (gen.getClassName(), loader);
    boolean track_class = false;

    // Have all top-level classes implement our interface
    if (gen.getSuperclassName().equals("java.lang.Object")) {
      // Add equals method if it doesn't already exist. This ensures
      // that an instrumented version, equals(Object, DCompMarker),
      // will be created in this class.
      Method eq = gen.containsMethod("equals", "(Ljava/lang/Object;)Z");
      if (eq == null) {
        debug_instrument.log ("Added equals method");
        add_equals_method (gen);
      }

      // Add clone method if it doesn't already exist. This ensures
      // that an instrumented version, clone(DCompMarker), will be
      // created in this class.
      Method cl = gen.containsMethod("clone", "()Ljava/lang/Object;");
      if (cl == null) {
        debug_instrument.log ("Added clone method");
        add_clone_method (gen);
      }

      // Add DCompInstrumented interface and the required
      // equals_dcomp_instrumented method.
      add_dcomp_interface (gen);
    }

    // Process each method
    for (Method m : gen.getMethods()) {

      try {
        // Note whether we want to track the daikon variables in this method
        boolean track = should_track (gen.getClassName(),
                                      methodEntryName (gen.getClassName(), m));
        if (track)
          track_class = true;

        // If we are tracking variables, make sure the class is public
        if (track && !gen.isPublic()) {
          gen.isPrivate(false);
          gen.isProtected(false);
          gen.isPublic(true);
        }

        MethodGen mg = new MethodGen (m, gen.getClassName(), pool);
        boolean has_code = (mg.getInstructionList() != null) ;
        debug_instrument.log ("  Processing method %s, track=%b\n", m, track);
        debug_instrument.indent();

        // Skip methods defined in Object
        if (!double_client) {
          if (is_object_method (mg.getName(), mg.getArgumentTypes())) {
            debug_instrument.log ("Skipped object method %s%n", mg.getName());
            continue;
          }
        }

        // Add an argument of java.lang.DCompMarker to match up with the
        // instrumented versions in the JDK.
        add_dcomp_arg (mg);

        // Create a MethodInfo that describes this methods arguments
        // and exit line numbers (information not available via reflection)
        // and add it to the list for this class.
        MethodInfo mi = null;
        if (track && has_code) {
          mi = create_method_info (class_info, mg);
          class_info.method_infos.add (mi);
          DCRuntime.methods.add (mi);
        }

        // Create the local to store the tag frame for this method
        tag_frame_local = create_tag_frame_local (mg);

        if (has_code) {
          instrument_method (mg);
          if (track) {
            add_enter (mg, mi, DCRuntime.methods.size()-1);
            add_exit (mg, mi, DCRuntime.methods.size()-1);
          }
          add_create_tag_frame (mg);
          handle_exceptions (mg);
        }


        if (has_code) {
          mg.setMaxLocals();
          mg.setMaxStack();
        } else {
          mg.removeCodeAttributes();
          mg.removeLocalVariables();
        }

        // Remove any LVTT tables
        BCELUtil.remove_local_variable_type_tables (mg);

        if (double_client && !BCELUtil.is_main (mg) && !BCELUtil.is_clinit(mg))
          gen.addMethod (mg.getMethod());
        else {
          gen.replaceMethod (m, mg.getMethod());
          if (BCELUtil.is_main (mg))
            gen.addMethod (create_dcomp_stub (mg).getMethod());
        }
        debug_instrument.exdent();
      } catch (Throwable t) {
        throw new Error ("Unexpected error processing " + classname
                         + "." + m.getName(), t);
      }
    }

    // Add tag accessor methods for each primitive in the class
    create_tag_accessors (gen);

    // Keep track of when the class is initialized (so we don't look
    // for fields in uninitialized classes)
    track_class_init();
    debug_instrument.exdent();

    // The code that builds the list of daikon variables for each ppt
    // needs to know what classes are instrumented.  Its looks in the
    // Chicory runtime for this information.
    if (track_class) {
      // System.out.printf ("adding class %s to all class list%n", class_info);
      daikon.chicory.Runtime.all_classes.add (class_info);
    }

    return (gen.getJavaClass().copy());
  }

  /**
   * Instruments the original class to perform dynamic comparabilty and
   * returns the new class definition.  A second version of each method
   * in the class is created which is instrumented for comparability
   */
  public JavaClass instrument_jdk() {

    // Add the tag fields
    // if (tag_fields_ok (gen.getClassName()))
    //  add_tag_fields();

    // Don't instrument annotations.  They aren't executed and adding
    // the marker argument causes subtle errors
    if ((gen.getModifiers() & Constants.ACC_ANNOTATION) != 0) {
      debug_track.log ("Not instrumenting annotation %s%n",gen.getClassName());
      return gen.getJavaClass().copy();
    }

    debug_instrument.log ("Instrumenting class %s%n", gen.getClassName());

    // Have all top-level classes implement our interface
    if (gen.getSuperclassName().equals("java.lang.Object")) {
      // Add equals method if it doesn't already exist. This ensures
      // that an instrumented version, equals(Object, DCompMarker),
      // will be created in this class.
      Method eq = gen.containsMethod("equals", "(Ljava/lang/Object;)Z");
      if (eq == null) {
        debug_instrument.log ("Added equals method");
        add_equals_method (gen);
      }

      // Add clone method if it doesn't already exist. This ensures
      // that an instrumented version, clone(DCompMarker), will be
      // created in this class.
      Method cl = gen.containsMethod("clone", "()Ljava/lang/Object;");
      if (cl == null) {
        debug_instrument.log ("Added clone method");
        add_clone_method (gen);
      }

      // Add DCompInstrumented interface and the required
      // equals_dcomp_instrumented method.
      add_dcomp_interface (gen);
    }

    // Process each method
    for (Method m : gen.getMethods()) {

      // Don't modify class initialization methods.  They can't affect
      // user comparability and there isn't any way to get a second
      // copy of them.
      if (BCELUtil.is_clinit (m))
        continue;

      debug_instrument.log ("  Processing method %s%n", m);

      MethodGen mg = new MethodGen (m, gen.getClassName(), pool);
      boolean has_code = (mg.getInstructionList() != null) ;

      // If the method is native
      if (mg.isNative()) {

        // Create java code that cleans up the tag stack and calls the
        // real native method
        fix_native (gen, mg);
        has_code = true;

        // Add an argument of java.lang.DCompMarker to distinguish our version
        add_dcomp_arg (mg);

      } else { // normal method

        // Add an argument of java.lang.DCompMarker to distinguish our version
        add_dcomp_arg (mg);

        // Create the local to store the tag frame for this method
        tag_frame_local = create_tag_frame_local (mg);

        // Instrument the method
        if (has_code) {
          instrument_method (mg);
          add_create_tag_frame (mg);
          handle_exceptions (mg);
        }
      }

      if (has_code) {
        mg.setMaxLocals();
        mg.setMaxStack();
      } else {
        mg.removeCodeAttributes();
        mg.removeLocalVariables();
      }
      gen.addMethod (mg.getMethod());
    }

    // Add tag accessor methods for each primitive in the class
    create_tag_accessors (gen);

    // We don't need to track class initialization in the JDK because
    // that is only used when printing comparability which is only done
    // for client classes
    // track_class_init();

    return (gen.getJavaClass().copy());
  }

  /**
   * Instrument the specified method for dynamic comparability
   */
  public void instrument_method (MethodGen mg) {

    // Get Stack information
    StackTypes stack_types = null;
    TypeStack type_stack = null;
    if (use_StackVer) {
      StackVer stackver = new StackVer ();
      VerificationResult vr = null;
      try {
      vr = stackver.do_stack_ver (mg);
      } catch (Exception e) {
        System.out.printf ("Warning: StackVer failed for %s: %s\n", mg, e);
        System.out.printf ("Method is NOT instrumented%n");
        skip_method (mg);
        return;
      }
      if (vr != VerificationResult.VR_OK) {
        System.out.printf ("Warning: StackVer failed for %s: %s%n", mg, vr);
        System.out.printf ("Method is NOT instrumented%n");
        skip_method (mg);
        return;
      }
      assert vr == VerificationResult.VR_OK : " vr failed " + vr;
      stack_types = stackver.get_stack_types();
    } else { // Use Eric's version
      type_stack = new TypeStack (mg);
    }

    // Loop through each instruction, making substitutions
    InstructionList il = mg.getInstructionList();
    OperandStack stack = null;
    for (InstructionHandle ih = il.getStart(); ih != null; ) {
      if (debug_instrument_inst.enabled()) {
        debug_instrument_inst.log ("instrumenting instruction %s%n", ih);
                     // ih.getInstruction().toString(pool.getConstantPool()));
      }
      InstructionList new_il = null;

      // Remember the next instruction to process
      InstructionHandle next_ih = ih.getNext();

      // Get the stack information
      if (use_StackVer)
        stack = stack_types.get (ih.getPosition());

      // Get the translation for this instruction (if any)
      new_il = xform_inst (mg, ih, stack);
      if (debug_instrument_inst.enabled())
        debug_instrument_inst.log ("  new inst: %s%n", new_il);

      if (!use_StackVer)
        stack = type_stack.getAfterInst (ih);

      // If this instruction was modified, replace it with the new
      // instruction list. If this instruction was the target of any
      // jumps or line numbers , replace them with the first
      // instruction in the new list
      replace_instructions (il, ih, new_il);

      ih = next_ih;
    }
  }

  /**
   * Adds the method name and containing class name to the list of
   * uninstrumented methods.
   */
  private void skip_method (MethodGen mgen) {
    skipped_methods.add(mgen.getClassName() + ":" + mgen.toString());
  }

  /**
   * Returns the list of uninstrumented methods. (Note:
   * instrument_jdk() needs to have been called first.)
   */
  public List<String> get_skipped_methods () {
    return new ArrayList<String>(skipped_methods);
  }

  /**
   * Adds a try/catch block around the entire method.  If an exception
   * occurs, the tag stack is cleaned up and the exception is rethrown.
   */
  public void handle_exceptions (MethodGen mgen) {

    InstructionList il = new InstructionList();
    il.append (ifact.createInvoke (DCRuntime.class.getName(),
                                       "exception_exit", Type.VOID,
                                       Type.NO_ARGS, Constants.INVOKESTATIC));
    il.append (new ATHROW());

    InstructionList cur_il = mgen.getInstructionList();
    InstructionHandle start = cur_il.getStart();
    InstructionHandle end = cur_il.getEnd();
    InstructionHandle exc = cur_il.append (il);

    mgen.addExceptionHandler (start, end, exc, throwable);
  }

  /**
   * Adds the code to create the tag frame to the beginning of the method.
   * This needs to be before the call to DCRuntime.enter (since it passed
   * to that method).
   */
  public void add_create_tag_frame (MethodGen mg) {

    // Create the tag frame and place it at the beginning of the method
    // Move line number and local variable targeters to the new
    // instructions, but leave other targeters (branches, exceptions)
    // unchanged.
    InstructionList il = mg.getInstructionList();
    InstructionList tf_il = create_tag_frame (mg, tag_frame_local);
    InstructionHandle old_start = il.getStart();
    InstructionHandle new_start = il.insert (tf_il);
    if (old_start.hasTargeters()) {
      for (InstructionTargeter it : old_start.getTargeters()) {
        if ((it instanceof LineNumberGen) || (it instanceof LocalVariableGen))
          it.updateTarget (old_start, new_start);
      }
    }
  }

  /**
   * Adds the call to DCRuntime.enter to the beginning of the method.
   */
  public void add_enter (MethodGen mg, MethodInfo mi, int method_info_index) {

    // Ignore methods with no instructions
    InstructionList il = mg.getInstructionList();
    if (il == null)
      return;

    // Create the call that processes daikon varaibles upon enter
    InstructionList enter_il = call_enter_exit (mg, method_info_index,
                                                "enter", -1);

    // Add the new code to the beginning of the method.  Move any
    // line number or local variable targeters to point to the new
    // instructions.  Other targeters (branches, exceptions) are left
    // unchanged.
    InstructionHandle old_start = il.getStart();
    InstructionHandle new_start = il.insert (enter_il);
    if (old_start.hasTargeters()) {
      for (InstructionTargeter it : old_start.getTargeters()) {
        if ((it instanceof LineNumberGen) || (it instanceof LocalVariableGen))
          it.updateTarget (old_start, new_start);
      }
    }

  }

  /**
   * Creates the local used to store the tag frame and returns it
   */
  LocalVariableGen create_tag_frame_local (MethodGen mgen) {

    return mgen.addLocalVariable ("dcomp_tag_frame$5a", object_arr, null,
                                  null);
  }

  /**
   * Creates code to create the tag frame for this method and store it
   * in tag_frame_local
   */
  InstructionList create_tag_frame (MethodGen mgen,
                                    LocalVariableGen tag_frame_local) {

    Type arg_types[] = mgen.getArgumentTypes();
    // LocalVariableGen[] locals = mgen.getLocalVariables();

    // Determine the offset of the first argument in the frame
    int offset = 1;
    if (mgen.isStatic())
      offset = 0;

    // Encode the primitive parameter information in a string
    mgen.setMaxLocals();
    int frame_size = mgen.getMaxLocals();
    assert frame_size < 100
      : frame_size + " " + mgen.getClassName() + "." + mgen.getName();
    String params = "" + (char)(frame_size + '0');
      // Character.forDigit (frame_size, Character.MAX_RADIX);
    List<Integer> plist = new ArrayList<Integer>();
    for (Type arg_type : arg_types) {
      if (arg_type instanceof BasicType) {
        plist.add (offset);
      }
      offset += arg_type.getSize();
    }
    for (int ii = plist.size()-1; ii >= 0; ii--) {
      params += (char)(plist.get(ii) + '0');
        //Character.forDigit (plist.get(ii), Character.MAX_RADIX);
    }

    // Create code to create/init the tag frame and store in tag_frame_local
    InstructionList il = new InstructionList();
    il.append (ifact.createConstant (params));
    il.append (ifact.createInvoke (DCRuntime.class.getName(),
                                   "create_tag_frame", object_arr, string_arg,
                                   Constants.INVOKESTATIC));
    il.append (ifact.createStore (object_arr, tag_frame_local.getIndex()));
    debug_instrument_inst.log ("Store Tag frame local at index %d%n",
                               tag_frame_local.getIndex());

    return (il);
  }

  /**
   * Pushes the object, method info index,  parameters, and return value
   * on the stack and calls the specified Method (normally
   * enter or exit) in DCRuntime.  The parameters are passed
   * as an array of objects.
   */
   InstructionList call_enter_exit (MethodGen mgen, int method_info_index,
                                    String method_name, int line) {

     InstructionList il = new InstructionList();
     Type[] arg_types = mgen.getArgumentTypes();

     // Push the tag frame
    il.append (ifact.createLoad (tag_frame_local.getType(),
                                 tag_frame_local.getIndex()));

     // Push the object.  Null if this is a static method or a constructor
     if (mgen.isStatic() ||
         (method_name.equals ("enter") && BCELUtil.is_constructor (mgen))) {
       il.append (new ACONST_NULL());
     } else { // must be an instance method
       il.append (ifact.createLoad (Type.OBJECT, 0));
     }

     // Determine the offset of the first parameter
     int param_offset = 1;
     if (mgen.isStatic())
       param_offset = 0;

     // Push the MethodInfo index
     il.append (ifact.createConstant (method_info_index));

     // Create an array of objects with elements for each parameter
     il.append (ifact.createConstant (arg_types.length));
     il.append (ifact.createNewArray (Type.OBJECT, (short) 1));

     // Put each argument into the array
     int param_index = param_offset;
     for (int ii = 0; ii < arg_types.length; ii++) {
       il.append (ifact.createDup (object_arr.getSize()));
       il.append (ifact.createConstant (ii));
       Type at = arg_types[ii];
       if (at instanceof BasicType) {
         il.append (new ACONST_NULL());
         // il.append (create_wrapper (c, at, param_index));
       } else { // must be reference of some sort
         il.append (ifact.createLoad (Type.OBJECT, param_index));
       }
       il.append (ifact.createArrayStore (Type.OBJECT));
       param_index += at.getSize();
     }

     // If this is an exit, push the return value and line number.
     // The return value
     // is stored in the local "return__$trace2_val"  If the return
     // value is a primitive, wrap it in the appropriate runtime wrapper
     if (method_name.equals ("exit")) {
       Type ret_type = mgen.getReturnType();
       if (ret_type == Type.VOID) {
         il.append (new ACONST_NULL());
       } else {
         LocalVariableGen return_local = get_return_local (mgen, ret_type);
         if (ret_type instanceof BasicType) {
           il.append (new ACONST_NULL());
           //il.append (create_wrapper (c, ret_type, return_local.getIndex()));
         } else {
           il.append (ifact.createLoad (Type.OBJECT, return_local.getIndex()));
         }
       }

       //push line number
       il.append (ifact.createConstant (line));
     }

     // Call the specified method
     Type[] method_args = null;
     if (method_name.equals ("exit"))
       method_args = new Type[] {object_arr, Type.OBJECT, Type.INT,
                                 object_arr, Type.OBJECT, Type.INT};
     else
       method_args = new Type[] {object_arr, Type.OBJECT, Type.INT,
                                 object_arr};
     il.append (ifact.createInvoke (DCRuntime.class.getName(), method_name,
                             Type.VOID, method_args, Constants.INVOKESTATIC));


     return (il);
   }

  /**
   * Transforms instructions to track comparability.  Returns a list
   * of instructions that replaces the specified instruction.  Returns
   * null if the instruction should not be replaced.
   *
   *    @param mg Method being instrumented
   *    @param ih Handle of Instruction to translate
   *    @param stack Current contents of the stack.
   */
  InstructionList xform_inst (MethodGen mg, InstructionHandle ih,
                             OperandStack stack) {

    Instruction inst = ih.getInstruction();

    switch (inst.getOpcode()) {

    // Replace the object comparison instructions with a call to
    // DCRuntime.object_eq or DCRuntime.object_ne.  Those methods
    // return a boolean which is used in a ifeq/ifne instruction
    case Constants.IF_ACMPEQ:
      return (object_comparison ((BranchInstruction) inst, "object_eq",
                                 Constants.IFNE));
    case Constants.IF_ACMPNE:
      return (object_comparison ((BranchInstruction) inst, "object_ne",
                                 Constants.IFNE));

    // These instructions compare the integer on the top of the stack
    // to zero.  Nothing is made comparable by this, so we need only
    // discard the tag on the top of the stack.
    case Constants.IFEQ:
    case Constants.IFNE:
    case Constants.IFLT:
    case Constants.IFGE:
    case Constants.IFGT:
    case Constants.IFLE: {
      return discard_tag_code (inst, 1);
    }

    // Instanceof pushes either 0 or 1 on the stack depending on whether
    // the object on top of stack is of the specified type.  We push a
    // tag for a constant, since nothing is made comparable by this.
    case Constants.INSTANCEOF:
      return build_il (dcr_call ("push_const", Type.VOID, Type.NO_ARGS), inst);

    // Duplicates the item on the top of stack.  If the value on the
    // top of the stack is a primitive, we need to do the same on the
    // tag stack.  Otherwise, we need do nothing.
    case Constants.DUP: {
      Type top = stack.peek();
      if (is_primitive (top)) {
        return build_il (dcr_call ("dup", Type.VOID, Type.NO_ARGS), inst);
      }
      return (null);
    }

    // Duplicates the item on the top of the stack and inserts it 2
    // values down in the stack.  If the value at the top of the stack
    // is not a primitive, there is nothing to do here.  If the second
    // value is not a primitive, then we need only to insert the duped
    // value down 1 on the tag stack (which contains only primitives)
    case Constants.DUP_X1: {
      Type top = stack.peek();
      if (!is_primitive (top))
        return (null);
      String method = "dup_x1";
      if (!is_primitive (stack.peek(1)))
        method = "dup";
      return build_il (dcr_call (method, Type.VOID, Type.NO_ARGS), inst);
    }

      // Duplicates either the top 2 category 1 values or a single
      // category 2 value and inserts it 2 or 3 values down on the
      // stack.
    case Constants.DUP2_X1: {
      String op = null;
      Type top = stack.peek();
      if (is_category2 (top)) {
        if (is_primitive (stack.peek(1)))
          op = "dup_x1";
        else // not a primitive, so just dup
          op = "dup";
      } else if (is_primitive (top)) {
        if (is_primitive (stack.peek(1)) && is_primitive (stack.peek(2)))
          op = "dup2_x1";
        else if (is_primitive (stack.peek(1)))
          op = "dup2";
        else if (is_primitive (stack.peek(2)))
          op = "dup_x1";
        else // neither value 1 nor value 2 is primitive
          op = "dup";
      } else { // top is not primitive
        if (is_primitive (stack.peek(1)) && is_primitive (stack.peek(2)))
          op = "dup_x1";
        else if (is_primitive (stack.peek(1)))
          op = "dup";
        else // neither of the top two values are primitive
          op = null;
      }
      if (debug_dup.enabled())
        debug_dup.log ("DUP2_X1 -> %s [... %s]%n", op,
                       stack_contents (stack, 3));

      if (op != null)
        return build_il (dcr_call (op, Type.VOID, Type.NO_ARGS), inst);
      return (null);
    }

    // Duplicate either one category 2 value or two category 1 values.
    case Constants.DUP2: {
      Type top = stack.peek();
      String op = null;
      if (is_category2 (top))
        op = "dup";
      else if (is_primitive (top) && is_primitive(stack.peek(1)))
        op = "dup2";
      else if (is_primitive (top) || is_primitive(stack.peek(1)))
        op = "dup";
      else // both of the top two items are not primitive, nothing to dup
        op = null;
      if (debug_dup.enabled())
        debug_dup.log ("DUP2 -> %s [... %s]%n", op,
                       stack_contents (stack, 2));
      if (op != null)
        return build_il (dcr_call (op, Type.VOID, Type.NO_ARGS), inst);
      return (null);
    }

    // Dup the category 1 value on the top of the stack and insert it either
    // two or three values down on the stack.
    case Constants.DUP_X2: {
      Type top = stack.peek();
      String op = null;
      if (is_primitive (top)) {
        if (is_category2 (stack.peek(1)))
          op = "dup_x1";
        else if (is_primitive (stack.peek(1)) && is_primitive (stack.peek(2)))
          op = "dup_x2";
        else if (is_primitive (stack.peek(1)) || is_primitive(stack.peek(2)))
          op = "dup_x1";
        else
          op = "dup";
      }
      if (debug_dup.enabled())
        debug_dup.log ("DUP_X2 -> %s [... %s]%n", op,
                       stack_contents (stack, 3));
      if (op != null)
        return build_il (dcr_call (op, Type.VOID, Type.NO_ARGS), inst);
      return (null);
    }

    case Constants.DUP2_X2: {
      Type top = stack.peek();
      String op = null;
      if (is_category2 (top)) {
        if (is_category2 (stack.peek(1)))
          op = "dup_x1";
        else if (is_primitive(stack.peek(1)) && is_primitive(stack.peek(2)))
          op = "dup_x2";
        else if (is_primitive(stack.peek(1)) || is_primitive(stack.peek(2)))
          op = "dup_x1";
        else // both values are references
          op = "dup";
      } else if (is_primitive (top)) {
        if (is_category2 (stack.peek(1)))
          assert false : "not supposed to happen " + stack_contents(stack, 3);
        else if (is_category2(stack.peek(2))) {
          if (is_primitive (stack.peek(1)))
            op = "dup2_x1";
          else
            op = "dup_x1";
        } else if (is_primitive (stack.peek(1))) {
          if (is_primitive(stack.peek(2)) && is_primitive(stack.peek(3)))
            op = "dup2_x2";
          else if (is_primitive(stack.peek(2)) || is_primitive(stack.peek(3)))
            op = "dup2_x1";
          else // both 2 and 3 are references
            op = "dup2";
        } else { // 1 is a reference
          if (is_primitive(stack.peek(2)) && is_primitive(stack.peek(3)))
            op = "dup_x2";
          else if (is_primitive(stack.peek(2)) || is_primitive(stack.peek(3)))
            op = "dup_x1";
          else // both 2 and 3 are references
            op = "dup";
        }
      } else { // top is a reference
        if (is_category2 (stack.peek(1)))
          assert false : "not supposed to happen " + stack_contents(stack, 3);
        else if (is_category2(stack.peek(2))) {
          if (is_primitive (stack.peek(1)))
            op = "dup_x1";
          else
            op = null; // nothing to dup
        } else if (is_primitive (stack.peek(1))) {
          if (is_primitive(stack.peek(2)) && is_primitive(stack.peek(3)))
            op = "dup_x2";
          else if (is_primitive(stack.peek(2)) || is_primitive(stack.peek(3)))
            op = "dup_x1";
          else // both 2 and 3 are references
            op = "dup";
        } else { // 1 is a reference
          op = null; // nothing to dup
        }
      }
      if (debug_dup.enabled())
        debug_dup.log ("DUP_X2 -> %s [... %s]%n", op,
                       stack_contents (stack, 3));
      if (op != null)
        return build_il (dcr_call (op, Type.VOID, Type.NO_ARGS), inst);
      return (null);
    }

    // Pop instructions discard the top of the stack.  We want to discard
    // the top of the tag stack iff the item on the top of the stack is a
    // primitive.
    case Constants.POP: {
      Type top = stack.peek();
      if (is_primitive (top))
        return discard_tag_code (inst, 1);
      return (null);
    }

    // Pops either the top 2 category 1 values or a single category 2 value
    // from the top of the stack.  We must do the same to the tag stack
    // if the values are primitives.
    case Constants.POP2: {
      Type top = stack.peek();
      if (is_category2 (top))
        return discard_tag_code (inst, 1);
      else {
        int cnt = 0;
        if (is_primitive (top))
          cnt++;
        if (is_primitive (stack.peek(1)))
          cnt++;
        if (cnt > 0)
          return discard_tag_code (inst, cnt);
      }
      return (null);
    }

    // Swaps the two category 1 types on the top of the stack.  We need
    // to swap the top of the tag stack if the two top elements on the
    // real stack are primitives.
    case Constants.SWAP: {
      Type type1 = stack.peek();
      Type type2 = stack.peek(1);
      if (is_primitive(type1) && is_primitive(type2)) {
        return build_il (dcr_call ("swap", Type.VOID, Type.NO_ARGS), inst);
      }
      return (null);
    }

    case Constants.IF_ICMPEQ:
    case Constants.IF_ICMPGE:
    case Constants.IF_ICMPGT:
    case Constants.IF_ICMPLE:
    case Constants.IF_ICMPLT:
    case Constants.IF_ICMPNE: {
      return build_il (dcr_call ("cmp_op", Type.VOID, Type.NO_ARGS), inst);
    }

    case Constants.GETFIELD: {
      return load_store_field (mg, (GETFIELD) inst);
    }

    case Constants.PUTFIELD: {
      return load_store_field (mg, (PUTFIELD) inst);
    }

    case Constants.GETSTATIC: {
      return load_store_field (mg, ((GETSTATIC) inst));
      // return load_store_static ((GETSTATIC) inst, "push_static_tag");
    }

    case Constants.PUTSTATIC: {
      return load_store_field (mg, ((PUTSTATIC) inst));
      // return load_store_static ((PUTSTATIC) inst, "pop_static_tag");
    }

    case Constants.DLOAD:
    case Constants.DLOAD_0:
    case Constants.DLOAD_1:
    case Constants.DLOAD_2:
    case Constants.DLOAD_3:
    case Constants.FLOAD:
    case Constants.FLOAD_0:
    case Constants.FLOAD_1:
    case Constants.FLOAD_2:
    case Constants.FLOAD_3:
    case Constants.ILOAD:
    case Constants.ILOAD_0:
    case Constants.ILOAD_1:
    case Constants.ILOAD_2:
    case Constants.ILOAD_3:
    case Constants.LLOAD:
    case Constants.LLOAD_0:
    case Constants.LLOAD_1:
    case Constants.LLOAD_2:
    case Constants.LLOAD_3: {
      return load_store_local ((LoadInstruction)inst, tag_frame_local,
                               "push_local_tag");
    }

    case Constants.DSTORE:
    case Constants.DSTORE_0:
    case Constants.DSTORE_1:
    case Constants.DSTORE_2:
    case Constants.DSTORE_3:
    case Constants.FSTORE:
    case Constants.FSTORE_0:
    case Constants.FSTORE_1:
    case Constants.FSTORE_2:
    case Constants.FSTORE_3:
    case Constants.ISTORE:
    case Constants.ISTORE_0:
    case Constants.ISTORE_1:
    case Constants.ISTORE_2:
    case Constants.ISTORE_3:
    case Constants.LSTORE:
    case Constants.LSTORE_0:
    case Constants.LSTORE_1:
    case Constants.LSTORE_2:
    case Constants.LSTORE_3: {
      return load_store_local ((StoreInstruction) inst, tag_frame_local,
                               "pop_local_tag");
    }

    case Constants.LDC:
    case Constants.LDC_W:
    case Constants.LDC2_W: {
      Type type;
      if (inst instanceof LDC) // LDC_W extends LDC
        type = ((LDC)inst).getType (pool);
      else
        type = ((LDC2_W)inst).getType (pool);
      if (!(type instanceof BasicType))
        return null;
      return build_il (dcr_call ("push_const", Type.VOID, Type.NO_ARGS), inst);
    }

    // Push the tag for the array onto the tag stack.  This causes
    // anything comparable to the length to be comparable to the array
    // as an index.
    case Constants.ARRAYLENGTH: {
      return array_length (inst);
    }

    case Constants.BIPUSH:
    case Constants.SIPUSH:
    case Constants.DCONST_0:
    case Constants.DCONST_1:
    case Constants.FCONST_0:
    case Constants.FCONST_1:
    case Constants.FCONST_2:
    case Constants.ICONST_0:
    case Constants.ICONST_1:
    case Constants.ICONST_2:
    case Constants.ICONST_3:
    case Constants.ICONST_4:
    case Constants.ICONST_5:
    case Constants.ICONST_M1:
    case Constants.LCONST_0:
    case Constants.LCONST_1: {
      return build_il (dcr_call ("push_const", Type.VOID, Type.NO_ARGS), inst);
    }

    // Primitive Binary operators.  Each is augmented with a call to
    // DCRuntime.binary_tag_op that merges the tags and updates the tag
    // Stack.
    case Constants.DADD:
    case Constants.DCMPG:
    case Constants.DCMPL:
    case Constants.DDIV:
    case Constants.DMUL:
    case Constants.DREM:
    case Constants.DSUB:
    case Constants.FADD:
    case Constants.FCMPG:
    case Constants.FCMPL:
    case Constants.FDIV:
    case Constants.FMUL:
    case Constants.FREM:
    case Constants.FSUB:
    case Constants.IADD:
    case Constants.IAND:
    case Constants.IDIV:
    case Constants.IMUL:
    case Constants.IOR:
    case Constants.IREM:
    case Constants.ISHL:
    case Constants.ISHR:
    case Constants.ISUB:
    case Constants.IUSHR:
    case Constants.IXOR:
    case Constants.LADD:
    case Constants.LAND:
    case Constants.LCMP:
    case Constants.LDIV:
    case Constants.LMUL:
    case Constants.LOR:
    case Constants.LREM:
    case Constants.LSHL:
    case Constants.LSHR:
    case Constants.LSUB:
    case Constants.LUSHR:
    case Constants.LXOR:
      return build_il (dcr_call ("binary_tag_op", Type.VOID, Type.NO_ARGS),
                       inst);

    // Computed jump based on the int on the top of stack.  Since that int
    // is not made comparable to anything, we just discard its tag.  One
    // might argue that the key should be made comparable to each value in
    // the jump table.  But the tags for those values are not available.
    // And since they are all constants, its not clear how interesting it
    // would be anyway.
    case Constants.LOOKUPSWITCH:
    case Constants.TABLESWITCH:
      return discard_tag_code (inst, 1);

    // Make the integer argument to ANEWARRAY comparable to the new
    // array's index.
    case Constants.ANEWARRAY:
    case Constants.NEWARRAY: {
      return new_array (inst);
    }

    // If the new array has 2 dimensions, make the integer arguments
    // comparable to the corresponding indices of the new array.
    // For any other number of dimensions, discard the tags for the
    // arguments.
    case Constants.MULTIANEWARRAY: {
      int dims = ((MULTIANEWARRAY)inst).getDimensions();
      if (dims == 2) {
        return multiarray2 (inst);
      } else {
        return discard_tag_code (inst, dims);
      }
    }

    // Mark the array and its index as comparable.  Also for primitives,
    // push the tag of the array element on the tag stack
    case Constants.AALOAD:
    case Constants.BALOAD:
    case Constants.CALOAD:
    case Constants.DALOAD:
    case Constants.FALOAD:
    case Constants.IALOAD:
    case Constants.LALOAD:
    case Constants.SALOAD: {
      return array_load (inst);
    }

    // Mark the array and its index as comparable.  For primitives, store
    // the tag for the value on the top of the stack in the tag storage
    // for the array.
    case Constants.AASTORE:
      return array_store (inst, "aastore", Type.OBJECT);
    case Constants.BASTORE:
      return array_store (inst, "bastore", Type.BYTE);
    case Constants.CASTORE:
      return array_store (inst, "castore", Type.CHAR);
    case Constants.DASTORE:
      return array_store (inst, "dastore", Type.DOUBLE);
    case Constants.FASTORE:
      return array_store (inst, "fastore", Type.FLOAT);
    case Constants.IASTORE:
      return array_store (inst, "iastore", Type.INT);
    case Constants.LASTORE:
      return array_store (inst, "lastore", Type.LONG);
    case Constants.SASTORE:
      return array_store (inst, "sastore", Type.SHORT);

    // Prefix the return with a call to the correct normal_exit method
    // to handle the tag stack
    case Constants.ARETURN:
    case Constants.DRETURN:
    case Constants.FRETURN:
    case Constants.IRETURN:
    case Constants.LRETURN:
    case Constants.RETURN: {
      Type type = mg.getReturnType();
      InstructionList il = new InstructionList();
      if ((type instanceof BasicType) && (type != Type.VOID))
        il.append (dcr_call ("normal_exit_primitive", Type.VOID,Type.NO_ARGS));
      else
        il.append (dcr_call ("normal_exit", Type.VOID, Type.NO_ARGS));
      il.append (inst);
      return (il);
    }

    // Handles calls outside of instrumented code by discarding the tags.
    // This may not work correctly for interfaces (who do we tell if we
    // are instrumenting the destination.  This code needs to be updated
    // when we instrument the JDK.
    case Constants.INVOKESTATIC:
    case Constants.INVOKEVIRTUAL:
    case Constants.INVOKESPECIAL:
    case Constants.INVOKEINTERFACE:
      return handle_invoke ((InvokeInstruction) inst);

    // Throws an exception.  This clears the operand stack of the current
    // frame.  We need to clear the tag stack as well.
    case Constants.ATHROW:
      return build_il (dcr_call ("throw_op", Type.VOID, Type.NO_ARGS), inst);

    // Opcodes that don't need any modifications.  Here for reference
    case Constants.ACONST_NULL:
    case Constants.ALOAD:
    case Constants.ALOAD_0:
    case Constants.ALOAD_1:
    case Constants.ALOAD_2:
    case Constants.ALOAD_3:
    case Constants.ASTORE:
    case Constants.ASTORE_0:
    case Constants.ASTORE_1:
    case Constants.ASTORE_2:
    case Constants.ASTORE_3:
    case Constants.CHECKCAST:
    case Constants.D2F:     // double to float
    case Constants.D2I:     // double to integer
    case Constants.D2L:     // double to long
    case Constants.DNEG:    // Negate double on top of stack
    case Constants.F2D:     // float to double
    case Constants.F2I:     // float to integer
    case Constants.F2L:     // float to long
    case Constants.FNEG:    // Negate float on top of stack
    case Constants.GOTO:
    case Constants.GOTO_W:
    case Constants.I2B:     // integer to byte
    case Constants.I2C:     // integer to char
    case Constants.I2D:     // integer to double
    case Constants.I2F:     // integer to float
    case Constants.I2L:     // integer to long
    case Constants.I2S:     // integer to short
    case Constants.IFNONNULL:
    case Constants.IFNULL:
    case Constants.IINC:    // increment local variable by a constant
    case Constants.INEG:    // negate integer on top of stack
    case Constants.JSR:     // pushes return address on the stack, but that
                            // is thought of as an object, so we don't need
                            // a tag for it.
    case Constants.JSR_W:
    case Constants.L2D:     // long to double
    case Constants.L2F:     // long to float
    case Constants.L2I:     // long to int
    case Constants.LNEG:    // negate long on top of stack
    case Constants.MONITORENTER:
    case Constants.MONITOREXIT:
    case Constants.NEW:
    case Constants.NOP:
    case Constants.RET:     // this is the internal JSR return
      return (null);

    // Make sure we didn't miss anything
    default:
      assert false: "instruction " + inst + " unsupported";
      return (null);
    }

  }

  /**
   * Adds a call to DCruntime.exit() at each return from the
   * method.  This call calculates comparability on the daikon
   * variables.  It is only necessary if we are tracking comparability
   * for the variables of this method
   */
  public void add_exit (MethodGen mg, MethodInfo mi, int method_info_index) {

    // Iterator over all of the exit line numbers for this method
    Iterator<Integer> exit_iter = mi.exit_locations.iterator();

    // Loop through each instruction
    InstructionList il = mg.getInstructionList();
    for (InstructionHandle ih = il.getStart(); ih != null; ) {

      // Remember the next instruction to process
      InstructionHandle next_ih = ih.getNext();

      // If this is a return instruction, Call DCRuntime.exit to calculate
      // comparability on Daikon variables
      Instruction inst = ih.getInstruction();
      if (inst instanceof ReturnInstruction) {
        Type type = mg.getReturnType();
        InstructionList new_il = new InstructionList();
        if (type != Type.VOID) {
          LocalVariableGen return_loc = get_return_local (mg, type);
          new_il.append (ifact.createDup (type.getSize()));
          new_il.append (ifact.createStore (type, return_loc.getIndex()));
        }
        new_il.append (call_enter_exit (mg, method_info_index, "exit",
                                    exit_iter.next()));
        new_il.append (inst);
        replace_instructions (il, ih, new_il);
      }

      ih = next_ih;
    }
  }


  /**
   * Discards primitive tags for each primitive argument to a non-instrumented
   * method and adds a tag for a primitive return value.  Insures that the
   * tag stack is correct for non-instrumented methods
   */
  InstructionList handle_invoke (InvokeInstruction invoke) {

    String classname = invoke.getClassName (pool);

    InstructionList il = new InstructionList();

    boolean callee_instrumented = !BCELUtil.in_jdk (classname)
      || (jdk_instrumented // && classname.startsWith ("java")
          && (exclude_object && !classname.equals ("java.lang.Object")));

    // We don't instrument any of the Object methods
    String method_name = invoke.getMethodName(pool);
    Type ret_type = invoke.getReturnType(pool);
    Type[] arg_types = invoke.getArgumentTypes(pool);
    if (is_object_method (method_name, invoke.getArgumentTypes(pool)))
      callee_instrumented = false;


    // Replace calls to Object's equals method with calls to our
    // replacement, a static method in DCRuntime
    ObjectType javalangObject = new ObjectType("java.lang.Object");
    if (method_name.equals("equals")
        && ret_type == Type.BOOLEAN
        && arg_types.length == 1
        && arg_types[0].equals(javalangObject)) {

      Type[] new_arg_types = new Type[] {javalangObject, javalangObject};

      if (invoke.getOpcode() == Constants.INVOKESPECIAL) {
        // this is a super.equals(Object) call
        il.append (ifact.createInvoke ("daikon.dcomp.DCRuntime",
                                       "dcomp_super_equals",
                                       ret_type, new_arg_types,
                                       Constants.INVOKESTATIC));
      } else {
        // just a regular equals(Object) call
        il.append (ifact.createInvoke ("daikon.dcomp.DCRuntime",
                                       "dcomp_equals",
                                       ret_type, new_arg_types,
                                       Constants.INVOKESTATIC));
      }

    } else if (method_name.equals("clone")
               && ret_type.equals(javalangObject)
               && arg_types.length == 0) {

      Type[] new_arg_types = new Type[] {javalangObject};

      if (invoke.getOpcode() == Constants.INVOKESPECIAL) {
        // this is a super.clone() call
        il.append (ifact.createInvoke ("daikon.dcomp.DCRuntime",
                                       "dcomp_super_clone",
                                       ret_type, new_arg_types,
                                       Constants.INVOKESTATIC));
      } else {
        // just a regular clone() call
        il.append (ifact.createInvoke ("daikon.dcomp.DCRuntime",
                                       "dcomp_clone",
                                       ret_type, new_arg_types,
                                       Constants.INVOKESTATIC));
      }

    } else if (callee_instrumented) {
      // If the callee is instrumented then, add the dcomp argument

      // Add the DCompMarker argument so that the instrumented version
      // will be used
      il.append (new ACONST_NULL());
      Type[] new_arg_types = add_type (arg_types, dcomp_marker);
      il.append (ifact.createInvoke (classname, method_name, ret_type,
                                     new_arg_types, invoke.getOpcode()));

    } else { // not instrumented, discard the tags before making the call

      // Discard the tags for any primitive arguments passed to system
      // methods
      int primitive_cnt = 0;
      for (Type arg_type : arg_types) {
        if (arg_type instanceof BasicType)
          primitive_cnt++;
      }
      if (primitive_cnt > 0)
        il.append (discard_tag_code (new NOP(), primitive_cnt));

      // Add a tag for the return type if it is primitive
      if ((ret_type instanceof BasicType) && (ret_type != Type.VOID)) {
        // System.out.printf ("push tag for return  type of %s%n",
        //                   invoke.getReturnType(pool));
        il.append (dcr_call ("push_const", Type.VOID, Type.NO_ARGS));
      }
      il.append (invoke);
    }
    return (il);
  }


  /**
   * Create the instructions that replace the object eq or ne branch
   * instruction.  They are replaced by a call to the specified
   * compare_method (which returns a boolean) followed by the specified
   * boolean ifeq or ifne instruction
   */
  InstructionList object_comparison (BranchInstruction branch,
                                     String compare_method, short boolean_if) {

    InstructionList il = new InstructionList();
    il.append (ifact.createInvoke (DCRuntime.class.getName(),
                                   compare_method, Type.BOOLEAN,
                                   two_objects, Constants.INVOKESTATIC));
    assert branch.getTarget() != null;
    il.append (ifact.createBranchInstruction (boolean_if,
                                              branch.getTarget()));
    return (il);
  }

  /**
   * Handles load and store field instructions.  The instructions must
   * be augmented to either push (load) or pop (store) the tag on the
   * tag stack.  This is accomplished by calling the specified method
   * in DCRuntime and passing that method the object containing the
   * the field and the offset of that field within the object
   */
  InstructionList old_load_store_field (FieldInstruction f, String method) {

    Type field_type = f.getFieldType (pool);
    if (field_type instanceof ReferenceType)
      return (null);
    ObjectType obj_type = (ObjectType) f.getReferenceType (pool);
    InstructionList il = new InstructionList();

    if (f instanceof GETFIELD) {
      il.append (ifact.createDup (obj_type.getSize()));
    } else {
      il.append (new SWAP());
      il.append (ifact.createDup (obj_type.getSize()));
    }

    int field_num = get_field_num (f.getFieldName(pool), obj_type);
    il.append (ifact.createConstant (field_num));
    il.append (ifact.createInvoke (DCRuntime.class.getName(), method,
                                  Type.VOID, object_int,
                                  Constants.INVOKESTATIC));
    if (f instanceof PUTFIELD)
      il.append (new SWAP());
    il.append (f);
    return (il);
  }

  /**
   * Handles load and store field instructions.  The instructions must
   * be augmented to either push (load) or pop (store) the tag on the
   * tag stack.  If tag storage is supported for the class that contains
   * the field, this is accomplished by loading/storing from the tag
   * field of the specified field.
   *
   * If tag storage is not supported, then (for now) we just discard stores
   * and push constant tags for loads.  This may miss some comparability
   */
  InstructionList load_store_field_tag_fields (FieldInstruction f) {

    Type field_type = f.getFieldType (pool);
    if (field_type instanceof ReferenceType)
      return (null);
    ObjectType obj_type = (ObjectType) f.getReferenceType (pool);
    InstructionList il = new InstructionList();
    String classname = obj_type.getClassName();

    // If this class doesn't support tag fields, don't load/store them
    if (!tag_fields_ok (classname)) {
      if (f instanceof GETFIELD) {
        il.append (dcr_call ("push_const", Type.VOID, Type.NO_ARGS));
      } else {
        il.append (ifact.createConstant (1));
        il.append (dcr_call ("discard_tag", Type.VOID, integer_arg));
      }
      il.append (f);
      return (il);
    }

    if (f instanceof GETFIELD) {
      // Dup the object ref on the top of stack
      il.append (ifact.createDup (obj_type.getSize()));

      // Get the tag value from the tag field
      il.append (ifact.createGetField (classname,
               DCRuntime.tag_field_name (f.getFieldName(pool)), Type.OBJECT));

      // Push the tag on the tag stack
      il.append (dcr_call ("push_tag", Type.VOID, object_arg));

    } else { // Must be putfield

      // Put the object ref on the stop of stack and dup it
      il.append (new SWAP());
      il.append (ifact.createDup (obj_type.getSize()));

      // Get the tag from the top of the tag stack
      il.append (dcr_call ("pop_tag", Type.OBJECT, Type.NO_ARGS));

      // Store the tag in the tag field for this field
      il.append (ifact.createPutField (classname,
               DCRuntime.tag_field_name (f.getFieldName(pool)), Type.OBJECT));

      // Restore the original order of the items on the stack
      il.append (new SWAP());
    }

    // Perform the normal field command
    il.append (f);

    return (il);
  }

  /**
   * Handles load and store field instructions.  The instructions must
   * be augmented to either push (load) or pop (store) the tag on the
   * tag stack.  This is accomplished by calling the tag get/set method
   * for this field.
   */
  InstructionList load_store_field  (MethodGen mg, FieldInstruction f) {

    Type field_type = f.getFieldType (pool);
    if (field_type instanceof ReferenceType)
      return (null);
    ObjectType obj_type = (ObjectType) f.getReferenceType (pool);
    InstructionList il = new InstructionList();
    String classname = obj_type.getClassName();

    if (f instanceof GETSTATIC) {
      il.append (ifact.createInvoke (classname,
                 tag_method_name (GET_TAG, classname, f.getFieldName(pool)),
                 Type.VOID, Type.NO_ARGS, Constants.INVOKESTATIC));
    } else if (f instanceof PUTSTATIC) {
        il.append (ifact.createInvoke (classname,
                   tag_method_name(SET_TAG, classname, f.getFieldName(pool)),
                   Type.VOID, Type.NO_ARGS, Constants.INVOKESTATIC));
    } else if (f instanceof GETFIELD) {
      il.append (ifact.createDup (obj_type.getSize()));
      il.append (ifact.createInvoke (classname,
                 tag_method_name (GET_TAG, classname, f.getFieldName(pool)),
                 Type.VOID, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
    } else { // must be put field
      if (field_type.getSize() == 2) {
        LocalVariableGen lv = get_tmp2_local (mg, field_type);
        il.append (ifact.createStore (field_type, lv.getIndex()));
        il.append (ifact.createDup (obj_type.getSize()));
        il.append (ifact.createInvoke (classname,
                   tag_method_name(SET_TAG, classname, f.getFieldName(pool)),
                   Type.VOID, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
        il.append (ifact.createLoad (field_type, lv.getIndex()));
      } else {
        il.append (new SWAP());
        il.append (ifact.createDup (obj_type.getSize()));
        il.append (ifact.createInvoke (classname,
                   tag_method_name(SET_TAG, classname, f.getFieldName(pool)),
                   Type.VOID, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
        il.append (new SWAP());
      }
    }

    // Perform the normal field command
    il.append (f);

    return (il);
  }


  /**
   * Handles load and store static instructions.  The instructions must
   * be augmented to either push (load) or pop (store) the tag on the
   * tag stack.  This is accomplished by calling the specified method
   * in DCRuntime and passing that method the object containing the
   * the field and the offset of that field within the object
   *
   * @deprecated use load_store_field
   */
  @java.lang.Deprecated
  InstructionList load_store_static (FieldInstruction f, String method) {

    Type field_type = f.getFieldType (pool);
    if (field_type instanceof ReferenceType)
      return (null);
    String name = f.getClassName(pool) + "." + f.getFieldName(pool);
    System.out.printf ("static field name for %s = %s%n", f, name);

    // Get the index of this static in the list of all statics and allocate
    // a tag for it.
    Integer index = null; // DCRuntime.static_map.get (name);
    if (index == null) {
      // index = DCRuntime.static_map.size();
      // DCRuntime.static_map.put (name, index);
      DCRuntime.static_tags.add (new Object());
    }

    // Create code to call the method passing it the static's index
    InstructionList il = new InstructionList();
    il.append (ifact.createConstant (index));
    il.append (ifact.createInvoke (DCRuntime.class.getName(), method,
                                  Type.VOID, new Type[] {Type.INT},
                                  Constants.INVOKESTATIC));
    il.append (f);
    return (il);
  }

  /**
   * Handles load and store local instructions.  The instructions must
   * be augmented to either push (load) or pop (store) the tag on the
   * tag stack.  This is accomplished by calling the specified method
   * in DCRuntime and passing that method the tag frame and the offset
   * of local/parameter
   */
  InstructionList load_store_local  (LocalVariableInstruction lvi,
                                    LocalVariableGen tag_frame_local,
                                    String method) {

    // Don't need tags for objects
    assert !(lvi instanceof ALOAD) && !(lvi instanceof ASTORE) : "lvi " + lvi;

    InstructionList il = new InstructionList();

    // Push the tag frame and the index of this local
    il.append (ifact.createLoad (tag_frame_local.getType(),
                                 tag_frame_local.getIndex()));
    debug_instrument_inst.log ("CreateLoad %s %d%n", tag_frame_local.getType(),
                               tag_frame_local.getIndex());
    il.append (ifact.createConstant (lvi.getIndex()));

    // Call the runtime method to handle loading/storing the local/parameter
    il.append (ifact.createInvoke (DCRuntime.class.getName(), method,
                                  Type.VOID, new Type[] {object_arr, Type.INT},
                                  Constants.INVOKESTATIC));
    il.append (lvi);
    return (il);
  }

  /**
   * Returns the number of the specified field in the primitive fields
   * of obj_type
   */
  int get_field_num (String name, ObjectType obj_type) {

    // If this is the current class, get the information directly
    if (obj_type.getClassName().equals (orig_class.getClassName())) {
      int fcnt = 0;
      for (Field f : orig_class.getFields()) {
        if (f.getName().equals (name))
          return (fcnt);
        if (f.getType() instanceof BasicType)
          fcnt++;
      }
      assert false : "Can't find " + name + " in " + obj_type;
      return (-1);
    }

    // Look up the class using this classes class loader.  This may
    // not be the best way to accomplish this.
    Class obj_class = null;
    try {
      obj_class = Class.forName (obj_type.getClassName(), false, loader);
    } catch (Exception e) {
      throw new Error ("can't find class " + obj_type.getClassName(), e);
    }

    // Loop through all of the fields, counting the number of primitive fields
    int fcnt = 0;
    for (java.lang.reflect.Field f : obj_class.getDeclaredFields()) {
      if (f.getName().equals (name))
        return (fcnt);
      if (f.getType().isPrimitive())
        fcnt++;
    }
    assert false : "Can't find " + name + " in " + obj_class;
    return (-1);
  }

  /**
   * Gets the local variable used to store a category2 temporary.
   * This is used in the PUTFIELD code to temporarily store the value
   * being placed in the field
   */
  LocalVariableGen get_tmp2_local (MethodGen mgen, Type typ) {

    String name = "dcomp_$tmp_" + typ;
    // System.out.printf ("local var name = %s%n", name);

    // See if the local has already been created
    LocalVariableGen tmp_local = null;
    for (LocalVariableGen lv : mgen.getLocalVariables()) {
      if (lv.getName().equals (name)) {
        assert lv.getType().equals (typ) : lv + " " + typ;
        return (lv);
      }
    }

    // Create the variable
    return mgen.addLocalVariable (name, typ, null, null);
  }

  /**
   * Returns the local variable used to store the return result.  If it
   * is not present, creates it with the specified type.  If the variable
   * is known to already exist, the type can be null
   */
  LocalVariableGen get_return_local (MethodGen mgen, Type return_type) {

    // Find the local used for the return value
    LocalVariableGen return_local = null;
    for (LocalVariableGen lv : mgen.getLocalVariables()) {
      if (lv.getName().equals ("return__$trace2_val")) {
        return_local = lv;
        break;
      }
    }

    // If a type was specified and the variable was found, they must match
    if (return_local == null)
      assert (return_type != null) : " return__$trace2_val doesn't exist";
    else
      assert (return_type.equals (return_local.getType())) :
        " return_type = " + return_type + "current type = "
        + return_local.getType();

    if (return_local == null) {
      // log ("Adding return local of type %s%n", return_type);
      return_local = mgen.addLocalVariable ("return__$trace2_val", return_type,
                                            null, null);
    }

    return (return_local);
  }

  /**
   * Creates a MethodInfo corresponding to the specified method.  The
   * exit locations are filled in, but the reflection information is
   * not generated
   */
  private MethodInfo create_method_info (ClassInfo class_info, MethodGen mgen) {

    // Get the argument names for this method
    String[] arg_names = mgen.getArgumentNames();
    LocalVariableGen[] lvs = mgen.getLocalVariables();
    int param_offset = 1;
    if (mgen.isStatic())
      param_offset = 0;
    if (lvs != null) {
      for (int ii = 0; ii < arg_names.length; ii++) {
        if ((ii + param_offset) < lvs.length)
          arg_names[ii] = lvs[ii + param_offset].getName();
      }
    }

    boolean shouldInclude = false;

    shouldInclude = true;

    // Get the argument types for this method
    Type[] arg_types = mgen.getArgumentTypes();
    String[] arg_type_strings = new String[arg_types.length];
    for (int ii = 0; ii < arg_types.length; ii++) {
      Type t = arg_types[ii];
      if (t instanceof ObjectType)
        arg_type_strings[ii] = ((ObjectType) t).getClassName();
      else
        arg_type_strings[ii] = t.getSignature().replace('/', '.');
    }

    // Loop through each instruction and find the line number for each
    // return opcode
    List<Integer> exit_locs = new ArrayList<Integer>();

    // Tells whether each exit loc in the method is included or not
    // (based on filters)
    List<Boolean> isIncluded = new ArrayList<Boolean>();

    // log ("Looking for exit points in %s%n", mgen.getName());
    InstructionList il = mgen.getInstructionList();
    int line_number = 0;
    int last_line_number = 0;
    boolean foundLine;

    if (il == null)
      return (null);

    for (InstructionHandle ih = il.getStart(); ih != null; ih = ih.getNext()) {
      foundLine = false;

      if (ih.hasTargeters()) {
        for (InstructionTargeter it : ih.getTargeters()) {
          if (it instanceof LineNumberGen) {
            LineNumberGen lng = (LineNumberGen) it;
            // log ("  line number at %s: %d%n", ih, lng.getSourceLine());
            // System.out.printf("  line number at %s: %d%n", ih,
            // lng.getSourceLine());
            line_number = lng.getSourceLine();
            foundLine = true;
          }
        }
      }

      switch (ih.getInstruction().getOpcode()) {

      case Constants.ARETURN :
      case Constants.DRETURN :
      case Constants.FRETURN :
      case Constants.IRETURN :
      case Constants.LRETURN :
      case Constants.RETURN :
        // log ("Exit at line %d%n", line_number);
        //only do incremental lines if we don't have the line generator
        if (line_number == last_line_number && foundLine == false) {
          line_number++;
        }
        last_line_number = line_number;

        shouldInclude = true;
        exit_locs.add(new Integer(line_number));
        isIncluded.add(true);
        break;

      default :
        break;
      }
    }

    if (shouldInclude)
      return new MethodInfo(class_info, mgen.getName(), arg_names,
                            arg_type_strings, exit_locs, isIncluded);
    else
      return null;
  }

  /**
   * Adds a call to DCRuntime.class_init (String classname) to the
   * class initializer for this class.  Creates a class initializer if
   * one is not currently present
   */
  public void track_class_init () {

    // Look for the class init method.  If not found, create an empty one.
    Method cinit = null;
    for (Method m : gen.getMethods()) {
      if (m.getName().equals ("<clinit>")) {
        cinit = m;
        break;
      }
    }
    if (cinit == null) {
      InstructionList il = new InstructionList();
      il.append (ifact.createReturn (Type.VOID));
      MethodGen cinit_gen = new MethodGen (Constants.ACC_STATIC, Type.VOID,
        Type.NO_ARGS, new String[0], "<clinit>", gen.getClassName(), il, pool);
      cinit_gen.setMaxLocals();
      cinit_gen.setMaxStack();
      cinit_gen.update();
      cinit = cinit_gen.getMethod();
      gen.addMethod (cinit);

    }

    // Add a call to DCRuntime.class_init to the beginning of the method
    InstructionList il = new InstructionList();
    il.append (ifact.createConstant (gen.getClassName()));
    il.append (ifact.createInvoke (DCRuntime.class.getName(), "class_init",
                              Type.VOID, string_arg, Constants.INVOKESTATIC));

    MethodGen cinit_gen = new MethodGen (cinit, gen.getClassName(), pool);
    InstructionList cur = cinit_gen.getInstructionList();
    InstructionHandle old_start = cur.getStart();
    InstructionHandle new_start = cur.insert (il);
    if (old_start.hasTargeters()) {
      for (InstructionTargeter it : old_start.getTargeters()) {
        if ((it instanceof LineNumberGen) || (it instanceof LocalVariableGen))
          it.updateTarget (old_start, new_start);
      }
    }
    cinit_gen.setMaxLocals();
    cinit_gen.setMaxStack();
    gen.replaceMethod (cinit, cinit_gen.getMethod());
  }

  /**
   * Creates code that makes the index comparable (for indexing
   * purposes) with the array in array load instructions.  First the
   * arrayref and its index are duplicated on the stack.  Then the
   * appropriate array load method is called to mark them as
   * comparable and update the tag stack.  Finally the original load
   * instruction is performed.
   */
  public InstructionList array_load (Instruction inst) {

    InstructionList il = new InstructionList();

    // Duplicate the array ref and index and pass them to DCRuntime
    // which will make the index comparable with the array.  In the case
    // of primtives it will also get the tag for the primitive and push
    // it on the tag stack.
    il.append (new DUP2());
    String method = "primitive_array_load";
    if (inst instanceof AALOAD)
      method = "ref_array_load";
    else if (is_uninit_class (gen.getClassName()))
      method = "primitive_array_load_null_ok";

    il.append (dcr_call (method, Type.VOID,
                         new Type[] {Type.OBJECT, Type.INT}));

    // Perform the original instruction
    il.append (inst);

    return (il);
  }

  /**
   * Creates code to make the index comparable (for indexing purposes)
   * with the array in the array store instruction.  This is accomplished
   * by calling the specified method and passing it the array reference,
   * index, and value (of base_type).  The method will mark the array and
   * index as comparable and perform the array store.
   */
  public InstructionList array_store (Instruction inst, String method,
                                      Type base_type) {

    InstructionList il = new InstructionList();
    Type arr_type = new ArrayType(base_type, 1);
    il.append (dcr_call (method, Type.VOID, new Type[] {arr_type, Type.INT,
                                                        base_type}));
    return (il);
  }

  /**
   * Creates code that pushes the array's tag onto the tag stack, so
   * that the index is comparable to the array length.  First, the
   * arrayref is duplicated on the stack.  Then a method is called to
   * push the array's tag onto the tag stack. Finally the original
   * arraylength instruction is performed.
   */
  public InstructionList array_length (Instruction inst) {

    InstructionList il = new InstructionList();

    // Duplicate the array ref and pass it to DCRuntime which will push
    // it onto the tag stack.
    il.append (new DUP());
    il.append (dcr_call ("push_array_tag", Type.VOID,
                         new Type[] {Type.OBJECT}));

    // Perform the original instruction
    il.append (inst);

    return (il);
  }

  /**
   * Creates code to make the declared length of a new array
   * comparable to its index.
   */
  public InstructionList new_array (Instruction inst) {
    InstructionList il = new InstructionList();

    // Perform the original instruction
    il.append (inst);

    // Duplicate the array ref from the top of the stack and pass it
    // to DCRuntime which will push it onto the tag stack.
    il.append (new DUP());
    il.append (dcr_call ("push_array_tag", Type.VOID,
                         new Type[] {Type.OBJECT}));

    // Make the array and the count comparable. Also, pop the tags for
    // the array and the count off the tag stack.
    il.append (dcr_call ("cmp_op", Type.VOID, Type.NO_ARGS));

    return (il);
  }

  /**
   * Creates code to make the declared lengths of a new
   * two-dimensional array comparable to the corresponding indices.
   */
  public InstructionList multiarray2 (Instruction inst) {
    InstructionList il = new InstructionList();

    // Duplicate both count arguments
    il.append (new DUP2());

    // Perform the original instruction
    il.append (inst);

    // Duplicate the new arrayref and put it below the count arguments
    // Stack is now: ..., arrayref, count1, count2, arrayref
    il.append (new DUP_X2());

    Type objArray = new ArrayType(Type.OBJECT, 1);
    il.append (dcr_call ("multianewarray2", Type.VOID,
                         new Type[] {Type.INT, Type.INT, objArray}));

    return (il);
  }

  /**
   * Returns whether or not this ppt should be included.  A ppt is included
   * if it matches ones of the select patterns and doesn't match any of the
   * omit patterns.
   */
  public boolean should_track (String classname, String pptname) {

    debug_track.log ("Considering tracking ppt %s %s%n", classname, pptname);

    // Don't track any JDK classes
    if (classname.startsWith ("java.") || classname.startsWith ("com.")
        || classname.startsWith ("sun.")) {
      debug_track.log ("  jdk class, return false%n");
      return (false);
    }

    // Don't track toString methods because we call them in
    // our debug statements.
    if (ignore_toString && pptname.contains ("toString"))
      return (false);

    // If any of the omit patterns match, exclude the ppt
    for (Pattern p : DynComp.ppt_omit_pattern) {
      // System.out.printf ("should_track: pattern '%s' on ppt '%s'\n",
      //                    p, pptname);
      if (p.matcher (pptname).find()) {
        debug_track.log ("  Omitting program point %s%n", pptname);
        return (false);
      }
    }

    // If there are no select patterns, everything matches
    if (DynComp.ppt_select_pattern.size() == 0)
      return (true);

    // One of the select patterns must match the ppt or the class to include
    for (Pattern p : DynComp.ppt_select_pattern) {
      if (p.matcher (pptname).find()) {
        debug_track.log ("  matched pptname%n");
        return (true);
      }
      if (p.matcher (classname).find()) {
        debug_track.log (" matched classname%n");
        return (true);
      }
    }
    debug_track.log (" No Match%n");
    return (false);
  }

  /**
   * Constructs a ppt entry name from a Method
   */
  public static String methodEntryName (String fullClassName, Method m) {

    // System.out.printf ("classname = %s, method = %s, short_name = %s%n",
    //                   fullClassName, m, m.getName());

    // Get an array of the type names
    Type[] arg_types = m.getArgumentTypes();
    String[] type_names = new String[arg_types.length];
    for (int ii = 0; ii < arg_types.length; ii++)
        type_names[ii] = arg_types[ii].toString();

    // Remove exceptions from the name
    String full_name = m.toString();
    full_name = full_name.replaceFirst ("\\s*throws.*", "");

    return fullClassName + "." +
      DaikonWriter.methodEntryName (fullClassName, type_names,
                                    full_name, m.getName());
  }

  /** Convenience function to call a static method in DCRuntime **/
  private InvokeInstruction dcr_call (String method_name, Type ret_type,
                                             Type[] arg_types) {

    return ifact.createInvoke (DCRuntime.class.getName(), method_name,
                               ret_type, arg_types, Constants.INVOKESTATIC);
  }

  /**
   * Create the code to call discard_tag(tag_count) and append inst to the
   * end of that code
   */
  private InstructionList discard_tag_code (Instruction inst, int tag_count) {
    InstructionList il = new InstructionList();
    il.append (ifact.createConstant (tag_count));
    il.append (dcr_call ("discard_tag", Type.VOID, integer_arg));
    append_inst (il, inst);
    return (il);
  }

  /**
   * Appends the specified instruction to the end of the specified list.
   * Required because for some reason you can't directly append jump
   * instructions to the list -- but you can create new ones and append
   * them.
   */
  private void append_inst (InstructionList il, Instruction inst) {

    if (inst instanceof LOOKUPSWITCH) {
      LOOKUPSWITCH ls = (LOOKUPSWITCH)inst;
      il.append (new LOOKUPSWITCH (ls.getMatchs(), ls.getTargets(),
                                   ls.getTarget()));
    } else if (inst instanceof TABLESWITCH) {
      TABLESWITCH ts = (TABLESWITCH)inst;
      il.append (new TABLESWITCH (ts.getMatchs(), ts.getTargets(),
                                  ts.getTarget()));
    } else if (inst instanceof IfInstruction) {
      IfInstruction ifi = (IfInstruction) inst;
      il.append (ifact.createBranchInstruction (inst.getOpcode(),
                                                ifi.getTarget()));
    } else {
      il.append (inst);
    }
  }

  /**
   * Returns whether or not the specified type is a primitive (int, float,
   * double, etc)
   */
  private boolean is_primitive (Type type) {
    return ((type instanceof BasicType) && (type != Type.VOID));
  }

  /**
   * Returns whether or not the specified type is a category 2 (8 byte)
   * type
   */
  private boolean is_category2 (Type type) {
    return ((type == Type.DOUBLE) || (type == Type.LONG));
  }

  /**
   * Returns the type of the last instruction that modified the top of
   * stack.  A gross attempt to figure out what is on the top of stack.
   */
  private Type find_last_push (InstructionHandle ih) {

    for (ih = ih.getPrev(); ih != null; ih = ih.getPrev()) {
      Instruction inst = ih.getInstruction();
      if (inst instanceof InvokeInstruction) {
        return ((InvokeInstruction) inst).getReturnType(pool);
      }
      if (inst instanceof TypedInstruction)
        return ((TypedInstruction) inst).getType (pool);
    }
    assert false : "couldn't find any typed instructions";
    return null;
  }

  /** Convenience function to build an instruction list **/
  private InstructionList build_il (Instruction... instructions) {
    InstructionList il = new InstructionList();
    for (Instruction inst : instructions)
      append_inst (il, inst);
    return (il);
  }

  /**
   * Replace instruction ih in list il with the instructions in new_il.  If
   * new_il is null, do nothing
   */
  private static void replace_instructions (InstructionList il,
                                InstructionHandle ih, InstructionList new_il) {

    if (new_il == null)
      return;

    // If there is only one new instruction, just replace it in the handle
    if (new_il.getLength() == 1) {
      ih.setInstruction (new_il.getEnd().getInstruction());
      return;
    }

    // Get the start and end instruction of the new instructions
    InstructionHandle new_end = new_il.getEnd();
    InstructionHandle new_start = il.insert (ih, new_il);

    // Move all of the branches from the old instruction to the new start
    il.redirectBranches (ih, new_start);

    // Move other targets to the new instuctions.
    if (ih.hasTargeters()) {
      for (InstructionTargeter it : ih.getTargeters()) {
        if (it instanceof LineNumberGen) {
          it.updateTarget (ih, new_start);
        } else if (it instanceof LocalVariableGen) {
          it.updateTarget (ih, new_end);
        } else if (it instanceof CodeExceptionGen) {
          CodeExceptionGen exc = (CodeExceptionGen)it;
          if (exc.getStartPC() == ih)
            exc.updateTarget (ih, new_start);
          else if (exc.getEndPC() == ih)
            exc.updateTarget(ih, new_end);
          else if (exc.getHandlerPC() == ih)
            exc.setHandlerPC (new_start);
          else
            System.out.printf ("Malformed CodeException: %s%n", exc);
        } else {
          System.out.printf ("unexpected target %s%n", it);
        }
      }
    }

    // Remove the old handle.  There should be no targeters left to it.
    try {
      il.delete (ih);
    } catch (Exception e) {
      throw new Error ("Can't delete instruction", e);
    }
  }

  /**
   * Returns whether or not the invoke specified invokes a native method.
   * This requires that the class that contains the method to be loaded.
   */
  public boolean is_native (InvokeInstruction invoke) {

    // Get the class of the method
    ClassLoader loader = getClass().getClassLoader();
    Class<?> clazz = null;
    try {
      clazz = Class.forName (invoke.getClassName(pool), false, loader);
    } catch (Exception e) {
      throw new Error ("can't get class " + invoke.getClassName(pool), e);
    }

    // Get the arguments to the method
    Type[] arg_types = invoke.getArgumentTypes (pool);
    Class[] arg_classes = new Class[arg_types.length];
    for (int ii = 0; ii < arg_types.length; ii++) {
      arg_classes[ii] = type_to_class (arg_types[ii], loader);
    }

    // Find the method and determine if its native
    int modifiers = 0;
    String method_name = invoke.getMethodName(pool);
    String classes = clazz.getName();
    try {
      if (method_name.equals("<init>")) {
        Constructor c = clazz.getDeclaredConstructor (arg_classes);
        modifiers = c.getModifiers();
      } else if (clazz.isInterface()) {
        return (false);  // presume interfaces aren't native...
      } else {

        java.lang.reflect.Method m = null;
        while (m == null) {
          try {
            m = clazz.getDeclaredMethod (method_name, arg_classes);
            modifiers = m.getModifiers();
          } catch (NoSuchMethodException e) {
            clazz = clazz.getSuperclass();
            classes += ", " + clazz.getName();
          }
        }
      }
    } catch (Exception e) {
      throw new Error ("can't find method " + method_name + " " +
                       Arrays.toString(arg_classes) + " " + classes + " " +
                       invoke.toString (pool.getConstantPool()) , e);
    }

    return (Modifier.isNative (modifiers));

  }


  /**
   * Converts a BCEL type to a Class.  The class referenced will be
   * loaded but not initialized.  The specified loader must be able to
   * find it.  If load is null, the default loader will be used.
   */
  public static Class type_to_class (Type t, ClassLoader loader) {

    if (loader == null)
      loader = DCInstrument.class.getClassLoader();

    if (t == Type.BOOLEAN)
      return (Boolean.TYPE);
    else if (t == Type.BYTE)
      return (Byte.TYPE);
    else if (t == Type.CHAR)
      return (Character.TYPE);
    else if (t == Type.DOUBLE)
      return (Double.TYPE);
    else if (t == Type.FLOAT)
      return (Float.TYPE);
    else if (t == Type.INT)
      return (Integer.TYPE);
    else if (t == Type.LONG)
      return (Long.TYPE);
    else if (t == Type.SHORT)
      return (Short.TYPE);
    else if (t instanceof ObjectType) {
      try {
        return Class.forName (((ObjectType)t).getClassName(), false, loader);
      } catch (Exception e) {
        throw new Error ("can't get class "+((ObjectType)t).getClassName(), e);
      }
    } else if (t instanceof ArrayType) {
      String sig = t.getSignature().replace ('/', '.');
      try {
        return Class.forName (sig, false, loader);
      } catch (Exception e) {
        //System.out.printf ("classname of object[] = '%s'%n", Object[].class);
        throw new Error ("can't get class " + sig, e);
      }
    } else {
      assert false : "unexpected type " + t;
      return (null);
    }

  }

  /**
   * Returns a type array with new_type added to the end of types
   */
  public static Type[] add_type (Type[] types, Type new_type) {
      Type[] new_types = new Type[types.length + 1];
      for (int ii = 0; ii < types.length; ii++) {
        new_types[ii] = types[ii];
      }
      new_types[types.length] = new_type;
      return (new_types);
  }

  /**
   * Returns a String array with new_string added to the end of arr
   */
  public static String[] add_string (String[] arr, String new_string) {
      String[] new_arr = new String[arr.length + 1];
      for (int ii = 0; ii < arr.length; ii++) {
        new_arr[ii] = arr[ii];
      }
      new_arr[arr.length] = new_string;
      return (new_arr);
  }

  /**
   * Modify a doubled native method to call its original method.  It pops
   * all of the paramter tags off of the tag stack.  If there is a
   * primitive return value it puts a new tag value on the stack for
   * it.
   *
   * TODO: add a way to provide a synopsis for native methods that
   * affect comparability.
   *
   * @param gen ClassGen for current class
   * @param mg MethodGen for the interface method. Must be native
   */
  public void fix_native(ClassGen gen, MethodGen mg) {

    InstructionList il = new InstructionList();
    Type[] arg_types = mg.getArgumentTypes();
    String[] arg_names = mg.getArgumentNames();

    debug_native.log ("Native call %s%n", mg);

    // Build local variables for each argument to the method
    if (!mg.isStatic())
      mg.addLocalVariable ("this", new ObjectType(mg.getClassName()), null,
                           null);
    for (int ii = 0; ii < arg_types.length; ii++) {
      mg.addLocalVariable (arg_names[ii], arg_types[ii], null, null);
    }

    // Discard the tags for any primitive arguments passed to system
    // methods
    int primitive_cnt = 0;
    for (Type arg_type : arg_types) {
      if (arg_type instanceof BasicType)
        primitive_cnt++;
    }
    if (primitive_cnt > 0)
      il.append (discard_tag_code (new NOP(), primitive_cnt));

    // push a tag if there is a primitive return value
    Type ret_type = mg.getReturnType();
    if ((ret_type instanceof BasicType) && (ret_type != Type.VOID)) {
      il.append (dcr_call ("push_const", Type.VOID, Type.NO_ARGS));
    }

    // If the method is not static, push the instance on the stack
    if (!mg.isStatic())
      il.append(ifact.createLoad(new ObjectType(gen.getClassName()), 0));

    // if call is sun.reflect.Reflection.getCallerClass (realFramesToSkip)
    if (mg.getName().equals("getCallerClass")
        && gen.getClassName().equals("sun.reflect.Reflection")) {

      // The call returns the class realFramesToSkip up on the stack. Since we
      // have added this call in between, we need to increment that number
      // by 1.
      il.append(ifact.createLoad(Type.INT, 0));
      il.append(ifact.createConstant(1));
      il.append(new IADD());
      // System.out.printf("adding 1 in %s.%s\n", gen.getClassName(),
      //                   mg.getName());

    } else { // normal call

      // push each argument on the stack
      int param_index = 1;
      if (mg.isStatic())
        param_index = 0;
      for (Type arg_type : arg_types) {
        il.append(InstructionFactory.createLoad(arg_type, param_index));
        param_index += arg_type.getSize();
      }
    }

    // Call the method
    il.append(ifact.createInvoke(gen.getClassName(), mg.getName(),
      mg.getReturnType(), arg_types, (mg.isStatic() ? Constants.INVOKESTATIC
                                      : Constants.INVOKEVIRTUAL)));

    // If there is a return value, return it
    il.append(InstructionFactory.createReturn(mg.getReturnType()));

    // Add the instructions to the method
    mg.setInstructionList(il);
    mg.setMaxStack();
    mg.setMaxLocals();

    // turn off the native flag
    mg.setAccessFlags(mg.getAccessFlags() & ~Constants.ACC_NATIVE);
  }

  /**
   * Returns whether or not tag fields are used within the specified class.
   * We can safely use class fields except in Object, String, and Class
   */
  public boolean tag_fields_ok (String classname) {

    if (true)
      return (false);

    if (classname.startsWith ("java.lang"))
      return false;

    if (classname.equals ("java.lang.String")
        || classname.equals ("java.lang.Class")
        || classname.equals ("java.lang.Object")
        || classname.equals ("java.lang.ClassLoader"))
      return (false);

    return (true);
  }

  /**
   * Adds a tag field that parallels each primitive field in the class.
   * The tag field is of type object and holds the tag associated with that
   * primitive
   */
  public void add_tag_fields () {

    // Add fields for tag storage for each primitive field
    for (Field field : gen.getFields()) {
      if (is_primitive (field.getType()) && !field.isStatic()) {
        FieldGen tag_field
          = new FieldGen (field.getAccessFlags() | Constants.ACC_SYNTHETIC,
                Type.OBJECT, DCRuntime.tag_field_name(field.getName()), pool);
        gen.addField (tag_field.getField());
      }
    }
  }

  /**
   * Returns a string describing the top max_items items on the stack.
   */
  public static String stack_contents (OperandStack stack, int max_items) {
    String contents = "";
    if (max_items >= stack.size())
      max_items = stack.size()-1;
    for (int ii = max_items; ii >= 0; ii--) {
      if (contents.length() != 0)
        contents += ", ";
      System.out.printf ("ii = %d%n", ii);
      contents += stack.peek(ii);
    }
    return contents;
  }

  /**
   * Creates tag get and set accessor methods for each field in gen.
   * An accessor is created for each field (including final, static,
   * and private fields). The accessors share the modifiers of their
   * field (except that all are final).  Accessors are named
   * <field>_<class>__$get_tag and <field>_<class>__$set_tag.  The class
   * name must be included because field names can shadow one another.
   *
   * If tag_fields_ok is true for the class, then tag fields are created
   * and the accessor uses the tag fields.  If not, tag storage is created
   * separately and accessed via the field number.
   *
   * Accessors are also created for each visible superclass field that is
   * not hidden by a field in this class.  These accessors just call the
   * superclasses accessor.
   *
   * Returns the list of new accessors and adds them to the class
   */
  public List<MethodGen> create_tag_accessors (ClassGen gen) {

    if (gen.isInterface())
      return (null);

    String classname = gen.getClassName();
    List<MethodGen> mlist = new ArrayList<MethodGen>();

    Set<String> field_set = new HashSet<String>();
    Map<Field,Integer> field_map = build_field_map (gen.getJavaClass());

    // Build accessors for all fields declared in this class
    for (Field f : gen.getFields()) {

      assert !field_set.contains(f.getName()) : f.getName() + "-" + classname;
      field_set.add(f.getName());

      // skip primitive fields
      if (!is_primitive (f.getType()))
        continue;

      MethodGen get_method = null;
      MethodGen set_method = null;
      if (f.isStatic()) {
        String full_name = full_name (orig_class, f);
        get_method = create_get_tag (gen, f, static_map.get(full_name));
        set_method = create_set_tag (gen, f, static_map.get(full_name));
      } else {
        get_method = create_get_tag (gen, f, field_map.get(f));
        set_method = create_set_tag (gen, f, field_map.get(f));
      }
      gen.addMethod(get_method.getMethod());
      mlist.add(get_method);
      gen.addMethod(set_method.getMethod());
      mlist.add(set_method);
    }

    // Build accessors for each field declared in a superclass that is
    // is not shadowed in a subclass
    JavaClass[] super_classes = null;
    try {
      super_classes = gen.getJavaClass().getSuperClasses();
    } catch (Exception e) {
      throw new Error(e);
    }
    for (JavaClass super_class : super_classes) {
      for (Field f : super_class.getFields()) {
        if (f.isPrivate())
          continue;
        if (field_set.contains(f.getName()))
          continue;
        if (!is_primitive (f.getType()))
          continue;

        field_set.add(f.getName());
        MethodGen get_method = null;
        MethodGen set_method = null;
        if (f.isStatic()) {
          String full_name = full_name (super_class, f);
          get_method = create_get_tag (gen, f, static_map.get(full_name));
          set_method = create_set_tag (gen, f, static_map.get(full_name));
        } else {
          get_method = create_get_tag (gen, f, field_map.get(f));
          set_method = create_set_tag (gen, f, field_map.get(f));
        }
        gen.addMethod(get_method.getMethod());
        mlist.add(get_method);
        gen.addMethod(set_method.getMethod());
        mlist.add(set_method);
      }
    }

    return (mlist);
  }

  /**
   * Builds a Map that relates each field in jc and each of its
   * superclasses to a unique offset.  The offset can be used to
   * index into a tag array for this class.  Instance fields are
   * placed in the returned map and static fields are placed in static
   * map (shared between all classes)
   */
  public Map<Field,Integer> build_field_map (JavaClass jc) {

    // Object doesn't have any primitive fields
    if (jc.getClassName().equals ("java.lang.Object"))
      return new LinkedHashMap<Field,Integer>();

    // Get the offsets for each field in the superclasses.
    JavaClass super_jc = null;
    try {
      super_jc = jc.getSuperClass();
    } catch (Exception e) {
      throw new Error ("can't get superclass for " + jc, e);
    }
    Map<Field,Integer> field_map = build_field_map (super_jc);
    int offset = field_map.size();

    // Determine the offset for each primitive field in the class
    // Also make sure the the static_tags list is large enough for
    // of the tags.
    for (Field f : jc.getFields()) {
      if (!is_primitive (f.getType()))
        continue;
      if (f.isStatic()) {
        if (!in_jdk) {
          int min_size = static_map.size() + DCRuntime.max_jdk_static;
          while (DCRuntime.static_tags.size() <= min_size)
            DCRuntime.static_tags.add (null);
          static_map.put (full_name(jc,f), min_size);
        } else { // building jdk
          String full_name = full_name(jc,f);
          if (static_map.containsKey (full_name)) {
            // System.out.printf ("Reusing static field %s value %d%n",
            //                    full_name, static_map.get(full_name));
          } else {
            // System.out.printf ("Allocating new static field %s%n",
            //                    full_name);
            static_map.put (full_name, static_map.size() + 1);
          }
        }
      } else {
        field_map.put (f, offset);
        offset++;
      }
    }

    return field_map;
  }

  /**
   * Creates a get tag method for field f.   The tag corresponding to field
   * f will be pushed on the tag stack.
   *
   *  void <field>_<class>__$get_tag() {
   *    #if f.isStatic()
   *      DCRuntime.push_static_tag (tag_offset)
   *    #else
   *      DCRuntime.push_field_tag (this, tag_offset);
   *  }
   *
   * @param gen ClassGen of class whose accessors are being built. Not
   *          necessarily the class declaring f (if f is inherited)
   * @param f field to build an accessor for
   * @param tag_offset Offset of f in the tag storage for this field
   */
  public MethodGen create_get_tag (ClassGen gen, Field f, int tag_offset) {

    // Determine the method to call in DCRuntime.  Instance fields and static
    // fields are handled separately.  Also instance fields in special
    // classes that are created by the JVM are handled separately since only
    // in those classes can fields be read without being written (in java)
    String methodname = "push_field_tag";
    Type[] args = object_int;
    if (f.isStatic()) {
      methodname = "push_static_tag";
      args = integer_arg;
    } else if (is_uninit_class (gen.getClassName())) {
      methodname = "push_field_tag_null_ok";
    }

    String classname = gen.getClassName();
    String accessor_name = tag_method_name(GET_TAG, classname, f.getName());

    InstructionList il = new InstructionList();

    if (!f.isStatic())
      il.append (ifact.createThis());
    il.append (ifact.createConstant (tag_offset));
    il.append (dcr_call (methodname, Type.VOID, args));
    il.append (ifact.createReturn (Type.VOID));

    // Create the get accessor method
    MethodGen get_method
      = new MethodGen(f.getAccessFlags() | Constants.ACC_FINAL,
        Type.VOID, Type.NO_ARGS, new String[] {}, accessor_name,
        classname, il, pool);
    get_method.isPrivate(false);
    get_method.isProtected(false);
    get_method.isPublic(true);
    get_method.setMaxLocals();
    get_method.setMaxStack();
    // add_line_numbers(get_method, il);

    return (get_method);
  }

  /**
   * Creates a set tag method for field f.   The tag on the top of the tag
   * stack will be popped off and placed in the tag storeage corresponding
   * to field
   *
   *  void <field>_<class>__$set_tag() {
   *    #if f.isStatic()
   *      DCRuntime.pop_static_tag (tag_offset)
   *    #else
   *      DCRuntime.pop_field_tag (this, tag_offset);
   *  }
   *
   * @param gen ClassGen of class whose accessors are being built. Not
   *          necessarily the class declaring f (if f is inherited)
   * @param f field to build an accessor for
   * @param tag_offset Offset of f in the tag storage for this field
   */
  public MethodGen create_set_tag (ClassGen gen, Field f, int tag_offset) {

    String methodname = "pop_field_tag";
    Type[] args = object_int;
    if (f.isStatic()) {
      methodname = "pop_static_tag";
      args = integer_arg;
    }

    String classname = gen.getClassName();
    String accessor_name = tag_method_name(SET_TAG, classname, f.getName());

    InstructionList il = new InstructionList();

    if (!f.isStatic())
      il.append (ifact.createThis());
    il.append (ifact.createConstant (tag_offset));
    il.append (dcr_call (methodname, Type.VOID, args));
    il.append (ifact.createReturn (Type.VOID));

    // Create the get accessor method
    MethodGen set_method
      = new MethodGen(f.getAccessFlags() | Constants.ACC_FINAL,
        Type.VOID, Type.NO_ARGS, new String[] {}, accessor_name,
        classname, il, pool);
    set_method.setMaxLocals();
    set_method.setMaxStack();
    // add_line_numbers(set_method, il);

    return (set_method);
  }

  /**
   * Adds the DCompInstrumented interface to the given class.
   * Adds the following method to the class, so that it implements the
   * DCompInstrumented interface:
   *   public boolean equals_dcomp_instrumented(Object o) {
   *     return this.equals(o, null);
   *   }
   * The method does nothing except call the instrumented equals
   * method (boolean equals(Object, DCompMarker)).
   */
  public void add_dcomp_interface (ClassGen gen) {
    gen.addInterface("daikon.dcomp.DCompInstrumented");
    debug_instrument.log ("Added interface DCompInstrumented");

    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(Constants.ACC_PUBLIC, Type.BOOLEAN,
                                     new Type[] { Type.OBJECT },
                                     new String[] { "obj" },
                                     "equals_dcomp_instrumented",
                                     gen.getClassName(), il, pool);

    il.append(ifact.createLoad(Type.OBJECT, 0));  // load this
    il.append(ifact.createLoad(Type.OBJECT, 1));  // load obj
    il.append(new ACONST_NULL());                 // use null for marker
    il.append(ifact.createInvoke(gen.getClassName(),
                                 "equals",
                                 Type.BOOLEAN,
                                 new Type[] { Type.OBJECT, dcomp_marker },
                                 Constants.INVOKEVIRTUAL));
    il.append(ifact.createReturn(Type.BOOLEAN));
    method.setMaxStack();
    method.setMaxLocals();
    gen.addMethod(method.getMethod());
    il.dispose();
  }

  /**
   * Adds the following method to a class:
   *   public boolean equals(Object obj) {
   *     return super.equals(obj);
   *   }
   * Must only be called if the Object equals method has not been
   * overridden; if the equals method is already defined in the class,
   * a ClassFormatError will result because of the duplicate method.
   */
  public void add_equals_method (ClassGen gen) {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(Constants.ACC_PUBLIC, Type.BOOLEAN,
                                     new Type[] { Type.OBJECT },
                                     new String[] { "obj" }, "equals",
                                     gen.getClassName(), il, pool);

    il.append(ifact.createLoad(Type.OBJECT, 0));  // load this
    il.append(ifact.createLoad(Type.OBJECT, 1));  // load obj
    il.append(ifact.createInvoke(gen.getSuperclassName(),
                                 "equals",
                                 Type.BOOLEAN,
                                 new Type[] { Type.OBJECT },
                                 Constants.INVOKESPECIAL));
    il.append(ifact.createReturn(Type.BOOLEAN));
    method.setMaxStack();
    method.setMaxLocals();
    gen.addMethod(method.getMethod());
    il.dispose();
  }

  /**
   * Adds the following method to a class:
   *   protected Object clone() throws CloneNotSupportedException {
   *     return super.clone();
   *   }
   * Must only be called if the Object clone method has not been
   * overridden; if the clone method is already defined in the class,
   * a ClassFormatError will result because of the duplicate method.
   */
  public void add_clone_method (ClassGen gen) {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(Constants.ACC_PROTECTED, Type.OBJECT,
                                     Type.NO_ARGS,
                                     new String[] {  }, "clone",
                                     gen.getClassName(), il, pool);

    il.append(ifact.createLoad(Type.OBJECT, 0));  // load this
    il.append(ifact.createInvoke(gen.getSuperclassName(),
                                 "clone",
                                 Type.OBJECT,
                                 Type.NO_ARGS,
                                 Constants.INVOKESPECIAL));
    il.append(ifact.createReturn(Type.OBJECT));
    method.setMaxStack();
    method.setMaxLocals();
    gen.addMethod(method.getMethod());
    il.dispose();
  }

  /** Returns the tag accessor method name **/
  public static String tag_method_name (String typ, String classname,
                                        String fname) {
    return fname + "_" + classname.replace ('.', '_') + "__$" + typ;
  }

  public void add_dcomp_arg (MethodGen mg) {

    // Don't modify main or the JVM won't be able to find it.
    if (BCELUtil.is_main (mg))
      return;

    // Don't modify class init methods, they don't take arguments
    if (BCELUtil.is_clinit (mg))
      return;

    //if (!mg.getName().equals("double_check"))
    //  return;

    boolean has_code = (mg.getInstructionList() != null) ;

    // If the method has code, add the local representing the new parameter
    // as a local, move all of the other locals down one slot, and modify
    // all of the code that references those locals
    if (has_code) {

      // Get the current local variables (includes parameters)
      LocalVariableGen[] locals = get_fix_locals (mg);

      // Remove the existing locals
      mg.removeLocalVariables();
      mg.setMaxLocals (0);

      // Determine the first actual local in the local variables.  The object
      // and the parameters form the first n entries in the list.
      int first_local = mg.getArgumentTypes().length;
      if (!mg.isStatic())
        first_local++;

      if (first_local > locals.length) {
        Type[] arg_types = mg.getArgumentTypes();
        String[] arg_names = mg.getArgumentNames();
        for (int ii = 0; ii < arg_types.length; ii++)
          System.out.printf ("param %s %s%n", arg_types[ii], arg_names[ii]);
        for (LocalVariableGen lvg : locals)
          System.out.printf ("local[%d] = %s%n", lvg.getIndex(), lvg);
        assert false : mg.getClassName() + "." + mg + " "
          + first_local + " " + locals.length;
      }

      // Add back the object and the parameters
      for (int ii = 0; ii < first_local; ii++) {
        LocalVariableGen l = locals[ii];
        LocalVariableGen new_lvg
          = mg.addLocalVariable (l.getName(), l.getType(), l.getIndex(),
                                 l.getStart(), l.getEnd());
        debug_add_dcomp.log ("Added parameter %s%n", new_lvg);
      }

      // Add the new parameter
      LocalVariableGen dcomp_arg = mg.addLocalVariable ("marker", dcomp_marker,
                                                        null, null);
      debug_add_dcomp.log ("Added dcomp arg %s%n", dcomp_arg);

      // Add back the other locals
      for (int ii = first_local; ii < locals.length; ii++) {
        LocalVariableGen l = locals[ii];
        LocalVariableGen new_lvg
          = mg.addLocalVariable (l.getName(), l.getType(), l.getIndex()+1,
                                 l.getStart(), l.getEnd());
        debug_add_dcomp.log ("Added local %d-%s%n", new_lvg.getIndex(),
                             new_lvg);
      }

      // Get the index of the first local.  This may not be equal to first
      // local because of category 2 (long, double) parameters.  Note that
      // this is the OLD value of the index, not the new value
      int first_local_index = dcomp_arg.getIndex();
      debug_add_dcomp.log ("First local index = %d%n", first_local_index);

      // Process the instruction list, adding one to the index of each
      // LocalVariableInstruction that is not referencing a parameter
      InstructionList il = mg.getInstructionList();
      for (InstructionHandle ih = il.getStart(); ih != null; ih = ih.getNext()) {
        Instruction inst = ih.getInstruction();
        if ((inst instanceof LocalVariableInstruction)
            || (inst instanceof RET) || (inst instanceof IINC)) {
          IndexedInstruction index_inst = (IndexedInstruction) inst;
          if (index_inst.getIndex() >= first_local_index)
            index_inst.setIndex (index_inst.getIndex() + 1);
        }
      }
    }

    // Add an argument of type java.lang.DCompMarker to distinguish the
    // method as instrumented
    Type[] arg_types = add_type (mg.getArgumentTypes(), dcomp_marker);
    String[] arg_names = add_string (mg.getArgumentNames(), "marker");
    debug_add_dcomp.log ("%s:%n  args = %s, %n  names = %s%n", mg.getName(),
                     Arrays.toString (arg_types), Arrays.toString (arg_names));
    mg.setArgumentTypes (arg_types);
    mg.setArgumentNames (arg_names);

    if (has_code)
      mg.setMaxLocals();

    debug_add_dcomp.log ("new mg: %s [%d locals]%n", mg, mg.getMaxLocals());
  }

  /** Returns whether or not the method is defined in Object **/
  public boolean is_object_method (String method_name, Type[] arg_types) {
    for (MethodDef md : obj_methods) {
      if (md.equals (method_name, arg_types)) {
        return (true);
      }
    }
    return (false);
  }

  /**
   * Returns whether or not the class is one of those that has values
   * initialized by the JVM or native methods
   */
  public boolean is_uninit_class (String classname) {

    for (String u_name : uninit_classes)
      if (u_name.equals (classname))
        return (true);

    return (false);
  }

  /**
   * Fixes the local variable table so that all parameters are in the
   * local table.  In some special cases where parameters are added by
   * the compiler (eg, constructors for inner classes) the local variable
   * table is missing the entry for the additional parameter.  This
   * method creates a correct array of locals and returns it.
   */
  private LocalVariableGen[] get_fix_locals (MethodGen mg) {

    LocalVariableGen[] locals = mg.getLocalVariables();
    Type[] arg_types = mg.getArgumentTypes();

    // Initial offset into the stack frame of the first parameter
    int offset = 0;
    if (!mg.isStatic())
      offset = 1;

    // Index into locals of the first parameter
    int loc_index = 0;
    if (!mg.isStatic())
      loc_index = 1;

    // Loop through each argument
    for (int ii = 0; ii < arg_types.length; ii++) {

      // If this parameter doesn't have a matching local
      if ((loc_index >= locals.length)
          || (offset != locals[loc_index].getIndex())) {

        // Create a local variable to describe the missing argument
        LocalVariableGen missing_arg
          = mg.addLocalVariable (mg.getArgumentName(ii), arg_types[ii], offset,
                                   null, null);

        // Add the new local variable to a new locals array
        LocalVariableGen[] new_locals = new LocalVariableGen[locals.length+1];
        System.arraycopy (locals, 0, new_locals, 0, loc_index);
        new_locals[loc_index] = missing_arg;
        System.arraycopy (locals, loc_index, new_locals, loc_index+1,
                          locals.length-loc_index);
        // System.out.printf ("Added missing parameter %s%n", missing_arg);
        locals = new_locals;
        // System.out.printf ("New Local Array:%n");
        // for (LocalVariableGen lvg : locals)
        //  System.out.printf ("  local[%d] = %s%n", lvg.getIndex(), lvg);
      }

      loc_index++;
      offset += arg_types[ii].getSize();
    }

    return (locals);
  }

  /**
   * Creates a method with a DcompMarker argument that does nothing but
   * call the corresponding method without the DCompMarker argument
   */
  private MethodGen create_dcomp_stub (MethodGen mg) {

    InstructionList il = new InstructionList();
    Type ret_type = mg.getReturnType();


    // if mg is dynamic, Push 'this' on the stack
    int offset = 0;
    if (!mg.isStatic()) {
      il.append (ifact.createThis());
      offset = 1;
    }

    // push each argument on the stack
    for (Type arg_type : mg.getArgumentTypes()) {
      il.append (ifact.createLoad (arg_type, offset));
      offset += arg_type.getSize();
    }

    // Call the method
    short kind = Constants.INVOKEVIRTUAL;
    if (mg.isStatic())
      kind = Constants.INVOKESTATIC;
    il.append (ifact.createInvoke (mg.getClassName(), mg.getName(),
                                   ret_type, mg.getArgumentTypes(), kind));

    il.append (ifact.createReturn (ret_type));

    // Create the method
    Type[] arg_types = add_type (mg.getArgumentTypes(), dcomp_marker);
    String[] arg_names = add_string (mg.getArgumentNames(), "marker");
    MethodGen dcomp_mg
      = new MethodGen (mg.getAccessFlags(), ret_type, arg_types,
                       arg_names, mg.getName(), mg.getClassName(), il, pool);
    dcomp_mg.setMaxLocals();
    dcomp_mg.setMaxStack();

    return (dcomp_mg);
  }

  /**
   * Writes the static map from field names to their integer ids to
   * the specified file.  Can be read with restore_static_map.
   * Each line contains a key/value combination with a blank separating them.
   */
  public static void save_static_map (File filename) throws IOException {

    PrintStream ps = new PrintStream (filename);
    for (Map.Entry<String,Integer> entry : static_map.entrySet()) {
      ps.printf ("%s  %d%n", entry.getKey(), entry.getValue());
    }
    ps.close();
  }

  /**
   * Restores the static map from the specified file.
   * @see #save_static_map(File)
   */
  public static void restore_static_map (File filename) throws IOException {
    for (String line : new TextFile(filename, "UTF-8")) {
      String[] key_val = line.split ("  *");
      assert !static_map.containsKey (key_val[0])
        : key_val[0] + " " + key_val[1];
      static_map.put (key_val[0], new Integer (key_val[1]));
      // System.out.printf ("Adding %s %s to static map%n", key_val[0],
      //                   key_val[1]);
    }
  }

  /**
   * Return the fully qualified fieldname of the specified field
   */
  private String full_name (JavaClass jc, Field f) {
    return jc.getClassName() + "." + f.getName();
  }

}
