package utilMDE;

import static java.lang.System.out;

import java.util.*;
import java.io.*;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InstructionTargeter;
import org.apache.bcel.generic.LineNumberGen;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;
import org.apache.bcel.generic.RETURN;


/**
 * Static utility methods for working with BCEL
 */
public class BCELUtil {

  /** Controls whether the checks in checkMgen are actually performed * */
  public static boolean skip_checks = false;

  private static final Type string_array = Type.getType("[Ljava.lang.String;");

  static void dump_method_declarations(ClassGen gen) {
    out.printf("method signatures for class %s\n", gen.getClassName());
    for (Method m : gen.getMethods()) {
      out.printf("  %s\n", get_method_declaration(m));
    }
  }

  /**
   * Returns a string describing a method declaration. It contains the access
   * flags (public, private, static, etc), the return type, the method name, and
   * the types of each of its arguments.
   */
  public static String get_method_declaration(Method m) {

    StringBuilder sb = new StringBuilder();
    Formatter f = new Formatter(sb);

    f.format("%s %s %s (", get_access_flags(m), m.getReturnType(), m.getName());
    for (Type at : m.getArgumentTypes()) {
      f.format("%s, ", at);
    }
    f.format(")");
    return (sb.toString().replace(", )", ")"));
  }

  static String get_access_flags(Method m) {

    int flags = m.getAccessFlags();

    StringBuffer buf = new StringBuffer();
    for (int i = 0, pow = 1; i <= Constants.MAX_ACC_FLAG; i++) {
      if ((flags & pow) != 0) {
        if (buf.length() > 0)
          buf.append(" ");
        if (i < Constants.ACCESS_NAMES.length)
          buf.append(Constants.ACCESS_NAMES[i]);
        else
          buf.append(String.format("ACC_BIT %x", pow));
      }
      pow <<= 1;
    }

    return (buf.toString());
  }

  /**
   * Returns the attribute name for the specified attribute
   */
  public static String get_attribute_name(Attribute a) {

    ConstantPool pool = a.getConstantPool();
    int con_index = a.getNameIndex();
    Constant c = pool.getConstant(con_index);
    String att_name = ((ConstantUtf8) c).getBytes();
    return (att_name);
  }

  /** Returns the constant string at the specified offset */
  public static String get_constant_str(ConstantPool pool, int index) {

    Constant c = pool.getConstant(index);
    if (c instanceof ConstantUtf8)
      return ((ConstantUtf8) c).getBytes();
    else if (c instanceof ConstantClass) {
      ConstantClass cc = (ConstantClass) c;
      return cc.getBytes(pool) + " [" + cc.getNameIndex() + "]";
    } else
      assert false : "unexpected constant " + c + " class " + c.getClass();
    return (null);
  }

  /** returns whether or not the specified method is a constructor * */
  public static boolean is_constructor(MethodGen mg) {
    return (mg.getName().equals("<init>") || mg.getName().equals(""));
  }

  /** returns whether or not the specified method is a constructor * */
  public static boolean is_constructor(Method m) {
    return (m.getName().equals("<init>") || m.getName().equals(""));
  }

  /** returns whether or not the specified method is a class initializer */
  public static boolean is_clinit (MethodGen mg) {
    return (mg.getName().equals("<clinit>"));
  }

  /** returns whether or not the specified method is a class initializer */
  public static boolean is_clinit (Method m) {
    return (m.getName().equals("<clinit>"));
  }

  /** returns whether or not the class is part of the JDK (rt.jar) * */
  public static boolean in_jdk(ClassGen gen) {
    return (in_jdk(gen.getClassName()));
  }

  /** returns whether or not the classname is part of the JDK (rt.jar) * */
  public static boolean in_jdk(String classname) {
    return classname.startsWith("java.") || classname.startsWith("com.sun.")
        || classname.startsWith("javax.") || classname.startsWith("org.ietf.")
        || classname.startsWith("org.omg.") || classname.startsWith("org.w3c.")
        || classname.startsWith("org.xml.") || classname.startsWith("sun.")
        || classname.startsWith("sunw.");
  }

  static void dump_methods(ClassGen gen) {

    System.out.printf("Class %s methods:\n", gen.getClassName());
    for (Method m : gen.getMethods())
      System.out.printf("  %s\n", m);
  }

  /**
   * Checks the specific method for consistency.
   */
  public static void checkMgen(MethodGen mgen) {

    if (skip_checks)
      return;

    try {
      mgen.toString();
      mgen.getLineNumberTable(mgen.getConstantPool());

      InstructionList ilist = mgen.getInstructionList();
      if (ilist == null || ilist.getStart() == null)
        return;
      CodeExceptionGen[] exceptionHandlers = mgen.getExceptionHandlers();
      for (CodeExceptionGen gen : exceptionHandlers) {
        assert ilist.contains(gen.getStartPC()) : "exception handler " + gen
            + " has been forgotten in " + mgen.getClassName() + "."
            + mgen.getName();
      }
      MethodGen nmg = new MethodGen(mgen.getMethod(), mgen.getClassName(), mgen
          .getConstantPool());
      nmg.getLineNumberTable(mgen.getConstantPool());
    } catch (Throwable t) {
      System.out.printf("failure in method %s.%s\n", mgen.getClassName(), mgen
          .getName());
      t.printStackTrace();
      throw new Error(t);
    }

  }

  /**
   * Checks all of the methods in gen for consistency
   */
  public static void checkMgens(final ClassGen gen) {

    if (skip_checks)
      return;

    Method[] methods = gen.getMethods();
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      // System.out.println ("Checking method " + method + " in class "
      // + gen.getClassName());
      checkMgen(new MethodGen(method, gen.getClassName(), gen.getConstantPool()));
    }

    if (false) {
      Throwable t = new Throwable();
      t.fillInStackTrace();
      StackTraceElement[] ste = t.getStackTrace();
      StackTraceElement caller = ste[1];
      System.out.printf("%s.%s (%s line %d)", caller.getClassName(), caller
          .getMethodName(), caller.getFileName(), caller.getLineNumber());
      for (int ii = 2; ii < ste.length; ii++)
        System.out.printf(" [%s line %d]", ste[ii].getFileName(), ste[ii]
            .getLineNumber());
      System.out.printf("\n");
      dump_methods(gen);
    }
  }

  /** Adds code in nl to start of method mg * */
  public static void add_to_start(MethodGen mg, InstructionList nl) {

    // Add the code before the first instruction
    InstructionList il = mg.getInstructionList();
    InstructionHandle old_start = il.getStart();
    InstructionHandle new_start = il.insert(nl);

    // Move any LineNumbers and local variable that currently point to
    // the first instruction to include the new instructions. Other
    // targeters (branches, exceptions) should not include the new
    // code
    if (old_start.hasTargeters()) {
      for (InstructionTargeter it : old_start.getTargeters()) {
        if ((it instanceof LineNumberGen) || (it instanceof LocalVariableGen))
          it.updateTarget(old_start, new_start);
      }
    }
    mg.setMaxStack();
    mg.setMaxLocals();
  }

  /** @see #dump(JavaClass, File) **/
  public static void dump (JavaClass jc, String dump_dir) {

    dump (jc, new File (dump_dir));
  }

  /**
   * Dumps the contents of the specified class to the specified directory.
   * The file is named dump_dir/[class].bcel.  It contains a synopsis
   * of the fields and methods followed by the jvm code for each method.
   *
   * @param jc javaclass to dump
   * @param dump_dir directory in which to write the file
   */
  public static void dump(JavaClass jc, File dump_dir) {

    try {
      dump_dir.mkdir();
      File path = new File(dump_dir, jc.getClassName() + ".bcel");
      PrintStream p = new PrintStream(path);

      // Print the class, super class and interfaces
      p.printf("class %s extends %s\n", jc.getClassName(), jc
          .getSuperclassName());
      String[] inames = jc.getInterfaceNames();
      if ((inames != null) && (inames.length > 0)) {
        p.printf("   ");
        for (String iname : inames)
          p.printf("implements %s ", iname);
        p.printf("\n");
      }

      // Print each field
      p.printf("\nFields\n");
      for (Field f : jc.getFields())
        p.printf("  %s\n", f);

      // Print the signature of each method
      p.printf("\nMethods\n");
      for (Method m : jc.getMethods())
        p.printf("  %s\n", m);

      // If this is not an interface, print the code for each method
      if (!jc.isInterface()) {
        for (Method m : jc.getMethods()) {
          p.printf("\nMethod %s\n", m);
          Code code = m.getCode();
          if (code != null)
            p.printf("  %s\n", code.toString().replace("\n", "\n  "));
        }
      }

      // Print the details of the constant pool.
      p.printf("Constant Pool:\n");
      ConstantPool cp = jc.getConstantPool();
      Constant[] constants = cp.getConstantPool();
      for (int ii = 0; ii < constants.length; ii++) {
        p.printf("  %d %s\n", ii, constants[ii]);
      }

      p.close();

    } catch (Exception e) {
      throw new Error("Unexpected error dumping javaclass", e);
    }
  }

  // TODO: write Javadoc
  public static String instruction_descr(InstructionList il,
      ConstantPoolGen pool) {

    String out = "";
    // not generic because BCEL is not generic
    for (Iterator i = il.iterator(); i.hasNext();) {
      InstructionHandle handle = (InstructionHandle) i.next();
      out += handle.getInstruction().toString(pool.getConstantPool()) + "\n";
    }
    return (out);
  }

  /**
   * Return a description of the local variables (one per line)
   */
  public static String local_var_descr(MethodGen mg) {

    String out = String.format("Locals for %s [cnt %d]\n", mg, mg
        .getMaxLocals());
    LocalVariableGen[] lvgs = mg.getLocalVariables();
    if ((lvgs != null) && (lvgs.length > 0)) {
      for (LocalVariableGen lvg : lvgs)
        out += String.format("  %s [index %d]\n", lvg, lvg.getIndex());
    }
    return (out);
  }

  /**
   * Builds an array of line numbers for the specified instruction list. Each
   * opcode is assigned the next source line number starting at 1000.
   */
  public static void add_line_numbers(MethodGen mg, InstructionList il) {

    il.setPositions(true);
    for (InstructionHandle ih : il.getInstructionHandles()) {
      mg.addLineNumber(ih, 1000 + ih.getPosition());
    }
  }

  /**
   * Sets the locals to 'this' and each of the arguments. Any other locals are
   * removed. An instruction list with at least one instruction must exist.
   */
  public static void setup_init_locals(MethodGen mg) {

    // Get the parameter types and names.
    Type[] arg_types = mg.getArgumentTypes();
    String[] arg_names = mg.getArgumentNames();

    // Remove any existing locals
    mg.setMaxLocals(0);
    mg.removeLocalVariables();

    // Add a local for the instance variable (this)
    if (!mg.isStatic())
      mg
          .addLocalVariable("this", new ObjectType(mg.getClassName()), null,
              null);

    // Add a local for each parameter
    for (int ii = 0; ii < arg_names.length; ii++) {
      mg.addLocalVariable(arg_names[ii], arg_types[ii], null, null);
    }

    // Reset the current number of locals so that when other locals
    // are added they get added at the correct offset
    mg.setMaxLocals();

    return;
  }

  /**
   * Empties the method of all code (except for a return).  This
   * includes line numbers, exceptions, local variables, etc.
   */
  public static void empty_method (MethodGen mg) {

    mg.setInstructionList(new InstructionList(new RETURN()));
    mg.removeExceptionHandlers();
    mg.removeLineNumbers();
    mg.removeLocalVariables();
    mg.setMaxLocals();
  }

  /**
   * Remove the local variable type table attribute (LVTT) from mg.
   * Evidently some changes require this to be updated, but without
   * BCEL support that would be hard to do.  It should be safe to just delete
   * it since it is optional and really only of use to a debugger.
   */
  public static void remove_local_variable_type_tables (MethodGen mg) {

    for (Attribute a : mg.getCodeAttributes()) {
      if (is_local_variable_type_table (a, mg.getConstantPool())) {
        mg.removeCodeAttribute (a);
      }
    }
  }

  /**
   * Returns whether or not the specified attribute is a local variable type
   * table.
   */
  public static boolean is_local_variable_type_table (Attribute a,
                                                      ConstantPoolGen pool) {
    return (get_attribute_name (a, pool).equals ("LocalVariableTypeTable"));
  }

  /**
   * Returns the attribute name for the specified attribute
   */
  public static String get_attribute_name (Attribute a, ConstantPoolGen pool) {

    int con_index = a.getNameIndex();
    Constant c = pool.getConstant (con_index);
    String att_name = ((ConstantUtf8) c).getBytes();
    return (att_name);
  }

  /**
   * Returns whether or not this is a standard main method (static,
   * name is 'main', and one argument of string array
   */
  public static boolean is_main (MethodGen mg) {
    Type[] arg_types = mg.getArgumentTypes();
    return (mg.isStatic() && mg.getName().equals("main")
            && (arg_types.length == 1) && arg_types[0].equals(string_array));
  }

}
