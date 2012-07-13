package daikon.chicory;

import static java.lang.System.out;
import java.lang.instrument.*;
import java.lang.reflect.Modifier;
import java.security.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

// Sun included version of BCEL
//import com.sun.org.apache.bcel.internal.*;
//import com.sun.org.apache.bcel.internal.classfile.*;
//import com.sun.org.apache.bcel.internal.generic.InstructionFactory;
//import com.sun.org.apache.bcel.internal.generic.*;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.*;
import org.apache.bcel.verifier.VerificationResult;

import sun.nio.cs.ext.ISCII91;
// uncomment this and uses of it below, to get bcel verify info
// import edu.mit.csail.pag.testfactoring.verify.StackVer;

import utilMDE.SimpleLog;

import daikon.Chicory;

/**
 * The Instrument class is responsible for modifying another class'
 * bytecode.  Specifically, its main task is to add "hooks" into the
 * other class at method entries and exits for instrumentation
 * purposes.
 */
public class Instrument implements ClassFileTransformer {

  boolean debug = false;
  boolean log_on = false;

  /** current Constant Pool * */
  static ConstantPoolGen pgen = null;

  /** the index of this method into Runtime.methods **/
  int cur_method_info_index = 0;

  /** the location of the runtime support class **/
  private static final String runtime_classname = "daikon.chicory.Runtime";

  /** Debug information about which classes are transformed and why **/
  public static SimpleLog debug_transform
    = new SimpleLog (Chicory.debug_transform);

  public Instrument () {
  }

  private void log (String format, Object... args) {
    if (!log_on)
      return;
    System.out.printf (format, args);
  }

  //uses Runtime.ppt_omit_pattern and Runtime.ppt_select_pattern
  //to see if the given ppt should be "filtered out"
  private boolean shouldFilter(String className, String methodName, String pptName)
  {
    boolean shouldExclude = false;

    // Don't instrument class if it matches an excluded regular expression
    for (Pattern pattern : Runtime.ppt_omit_pattern)
      {

        Matcher mPpt = pattern.matcher(pptName);
        Matcher mClass = pattern.matcher(className);
        Matcher mMethod = pattern.matcher(methodName);

        if (mPpt.find() || mClass.find() || mMethod.find())
          {
            log("not instrumenting %s, it matches regex %s%n", pptName, pattern);
            shouldExclude = true;

            //System.out.println("filtering 1 true on --- " + pptName);

            //omit takes priority over include
            return true;
          }
      }

    // If any include regular expressions are specified, only instrument
    // classes that match them
    if (Runtime.ppt_select_pattern.size() > 0)
      {
        for (Pattern pattern: Runtime.ppt_select_pattern)
          {

            Matcher mPpt = pattern.matcher(pptName);
            Matcher mClass = pattern.matcher(className);
            Matcher mMethod = pattern.matcher(methodName);

            //System.out.println("--->" + regex);

            if (mPpt.find() || mClass.find() || mMethod.find())
              {
                //System.out.printf ("instrumenting %s, it matches regex %s%n", pptName, regex);
                log("instrumenting %s, it matches regex %s%n", pptName, pattern);

                //System.out.println("filtering 2 false on --- " + pptName);
                return false; //don't filter out
              }
          }
      }

    //if we're here, this ppt not explicitly included or excluded
    //so keep unless there were items in the "include only" list
    boolean ret = (Runtime.ppt_select_pattern.size() > 0);

    //System.out.println("filtering 3: " + ret + " on --- " + pptName);
    return ret;

  }

  /**
   * Given another class, return a transformed version of the class which
   * contains "hooks" at method entries and exits
   */
  public byte[] transform (ClassLoader loader, String className,
                           Class<?> classBeingRedefined,
                           ProtectionDomain protectionDomain,
                           byte[] classfileBuffer)
    throws IllegalClassFormatException {

    // debug = className.equals ("DataStructures/StackAr");
    // debug = className.equals ("chicory/Test");
    // debug = className.equals ("DataStructures/BinarySearchTree");

    String fullClassName = className.replace("/", ".");
    //String fullClassName = className;

      debug_transform.log ("In Transform: class = %s%n", className);

    // Don't instrument boot classes.  They are uninteresting and will
    // not be able to access daikon.chicory.Runtime (because it is not
    // on the boot classpath.  Previously this code skipped classes
    // that started with java, com, javax, or sun, but this is not
    // correct in many cases.
    if (Chicory.boot_classes != null) {
      Matcher matcher = Chicory.boot_classes.matcher (fullClassName);
      if (matcher.find()) {
        debug_transform.log ("ignoring sys class %s, "
                             + "matches boot_classes regex", fullClassName);
        return (null);
      }
    } else if (loader == null) {
        debug_transform.log ("ignoring system class %s, class loader == null",
                             fullClassName);
      return (null);
    } else if (fullClassName.startsWith ("sun.reflect")) {
      System.out.printf ("class = %s, loader = %s, domain = %s, mod = %08x, "
                         + "synthetic=%b%n", fullClassName, loader,
                         protectionDomain, classBeingRedefined.getModifiers(),
                         classBeingRedefined.isSynthetic());
    }

    // Don't intrument our code
    if (is_chicory (className)) {
      debug_transform.log ("Not considering chicory class %s%n",fullClassName);
      return (null);
    }

      debug_transform.log ("transforming class %s%n", className);

    // Parse the bytes of the classfile, die on any errors
    JavaClass c = null;
    ClassParser parser = new ClassParser(new ByteArrayInputStream(classfileBuffer), className);
    try
      {
        c = parser.parse();
      }
    catch (Exception e)
      {
        throw new RuntimeException("Unexpected error", e);
      }

    try {
      // Get the class information
      ClassGen cg = new ClassGen (c);

      // Convert reach non-void method to save its result in a local
      // before returning
      ClassInfo c_info = save_ret_value (cg, fullClassName, loader);

      //get constant static fields!
      Field[] fields = cg.getFields();
      for (Field field: fields)
        {
          if (field.isFinal() && field.isStatic() && (field.getType() instanceof BasicType))
            {
              ConstantValue value = field.getConstantValue();
              String valString;

              if (value == null)
                {
                  //System.out.println("WARNING FROM " + field.getName());
                  //valString = "WARNING!!!";
                  valString = null;
                }
              else
                {
                  valString = value.toString();
                  //System.out.println("GOOD FROM " + field.getName() + " --- " + valString);
                }

              if (valString != null)
                c_info.staticMap.put(field.getName(), valString);
            }
        }

      if (Chicory.checkStaticInit)
        {
          //check for static initializer
          boolean hasInit = false;
          for (Method meth : cg.getMethods())
            {
              if (meth.getName().equals("<clinit>"))
                hasInit = true;
            }

          //if not found, add our own!
          if (!hasInit)
            cg.addMethod(createClinit(cg, fullClassName));
        }


      JavaClass njc = cg.getJavaClass();
      if (debug)
        njc.dump ("/tmp/ret/" + njc.getClassName() + ".class");

      if (c_info.shouldInclude)
        return (cg.getJavaClass().getBytes());
      else
        return null;

    } catch (Throwable e) {
      out.format ("Unexpected error %s in transform", e);
      e.printStackTrace();
      return (null);
    }

  }

  //used to add a "hook" into the <clinit> static initializer
  private Method addInvokeToClinit(ClassGen cg, MethodGen mg, String fullClassName)
  {
    call_initNotify(cg, cg.getConstantPool(), fullClassName, new MethodContext(cg, mg).ifact);

    InstructionList newList = mg.getInstructionList();
    mg.update();


    InstructionList il = mg.getInstructionList();
    MethodContext context = new MethodContext(cg,mg);

    for (InstructionHandle ih = il.getStart(); ih != null; ) {
      InstructionList new_il = null;
      Instruction inst = ih.getInstruction();

      // Get the translation for this instruction (if any)
      new_il = xform_clinit (cg, cg.getConstantPool(), fullClassName, inst, context);

      // Remember the next instruction to process
      InstructionHandle next_ih = ih.getNext();

      // If this instruction was modified, replace it with the new
      // instruction list. If this instruction was the target of any
      // jumps, replace it with the first instruction in the new list
      if (new_il != null) {
        if (true) {
          try
            {
              new_il.delete (new_il.getEnd());
            }
          catch (TargetLostException e)
            {
              throw new Error ("unexpected lost target exception", e);
            }
          InstructionHandle new_start = il.insert (ih, new_il);
          //out.format ("old start = %s, new_start = %s%n", ih, new_start);
          il.redirectBranches (ih, new_start);

          // Fix up line numbers to point at the new code
          if (ih.hasTargeters()) {
            for (InstructionTargeter it : ih.getTargeters()) {
              if (it instanceof LineNumberGen) {
                it.updateTarget (ih, new_start);
              }
            }
          }

          ih = next_ih;
          continue;
        }

        if (debug)
          out.format ("Replacing %s by %s%n", ih, new_il);

        il.append (ih, new_il);
        InstructionTargeter[] targeters = ih.getTargeters();
        if (targeters != null) {
          // out.format ("targeters length = %d%n", targeters.length);
          for (int j = 0; j < targeters.length; j++)
            targeters[j].updateTarget (ih, ih.getNext());
        }
        try {
          il.delete (ih);
        } catch (TargetLostException e) {
          throw new Error ("unexpected lost target exception", e);
        }
      }
      // Go on to the next instruction in the list
      ih = next_ih;
    }




    mg.setInstructionList (newList);



    for (Attribute a : mg.getCodeAttributes()) {
      if (is_local_variable_type_table (a)) {
        mg.removeCodeAttribute (a);
      }
    }

    // Update the max stack and Max Locals
    mg.setMaxLocals();
    mg.setMaxStack();
    mg.update();

    return mg.getMethod();
  }



  //called by addInvokeToClinit to add in a hook at return opcodes
  private InstructionList xform_clinit(ClassGen cg, ConstantPoolGen cp,
                                       String fullClassName, Instruction inst, MethodContext context) {
    switch (inst.getOpcode()) {

    case Constants.ARETURN:
    case Constants.DRETURN:
    case Constants.FRETURN:
    case Constants.IRETURN:
    case Constants.LRETURN:
    case Constants.RETURN:
      break;

    default:
      return (null);
    }

    InstructionList il = new InstructionList();
    il.append(call_initNotify(cg, cp, fullClassName, context.ifact));
    il.append(inst);
    return (il);
  }


  //created a <clinit> method if none exists, to guarantee we always have this hook
  private Method createClinit(ClassGen cg, String fullClassName) {
    /*
     * System.out.println(mg.getAccessFlags());
     * System.out.println(mg.getReturnType());
     * System.out.println(mg.getArgumentTypes());
     * System.out.println(mg.getName());
     * System.out.println(mg.getClassName());
     */

    InstructionFactory factory = new InstructionFactory(cg);

    InstructionList il = new InstructionList();
    il.append(call_initNotify(cg, cg.getConstantPool(), fullClassName,
                              factory));
    il.append(InstructionFactory.createReturn(Type.VOID)); // need to
    // return!

    MethodGen newMethGen = new MethodGen(8, Type.VOID, new Type[0],
                                         new String[0], "<clinit>", fullClassName, il, cg
                                         .getConstantPool());
    newMethGen.update();

    for (Attribute a : newMethGen.getCodeAttributes()) {
      if (is_local_variable_type_table(a)) {
        newMethGen.removeCodeAttribute(a);
      }
    }

    // MethodContext context = new MethodContext (cg, newMethGen);
    // InstructionFactory ifact = context.ifact;

    // il.append (ifact.createConstant (0));
    // newMethGen.setInstructionList(il);
    // newMethGen.update();

    // Update the max stack and Max Locals
    newMethGen.setMaxLocals();
    newMethGen.setMaxStack();
    newMethGen.update();

    return newMethGen.getMethod();
  }


  //created the InstructionList to insert for adding the <clinit> hookd
  private InstructionList call_initNotify(ClassGen cg, ConstantPoolGen cp, String fullClassName, InstructionFactory factory)
  {
    InstructionList invokeList = new InstructionList();

    invokeList.append(new PUSH(cp, fullClassName));
    invokeList.append(factory.createInvoke(runtime_classname, "initNotify",
                                           Type.VOID, new Type[] {Type.STRING}, Constants.INVOKESTATIC));

    //System.out.println(fullClassName + " --- " + invokeList.size());
    return invokeList;
  }

  /**
   * Changes each return statement to first place the value being returned into
   * a local and then return. This allows us to work around the JDI deficiency
   * of not being able to query return values.
   * @param fullClassName must be packageName.className
   */
  private ClassInfo save_ret_value (ClassGen cg, String fullClassName,
                                    ClassLoader loader) {

    ClassInfo class_info = new ClassInfo (cg.getClassName(), loader);
    List<MethodInfo> method_infos = new ArrayList<MethodInfo>();

    boolean shouldInclude = false;

    try {
      pgen = cg.getConstantPool();

      // Loop through each method in the class
      Method[] methods = cg.getMethods();
      for (int i = 0; i < methods.length; i++) {
        MethodGen mg = new MethodGen (methods[i], cg.getClassName(), pgen);
        MethodContext context = new MethodContext (cg, mg);

        if (debug)
          {
            out.format("  Method = %s%n", mg);
            Attribute[] attributes = mg.getCodeAttributes();
            for (Attribute a : attributes)
              {
                int con_index = a.getNameIndex();
                Constant c = pgen.getConstant(con_index);
                String att_name = ((ConstantUtf8) c).getBytes();
                out.format("attribute: %s [%s]%n", a, att_name);
              }
          }

        // check for the class init method
        if (mg.getName().equals("<clinit>"))
          {
            if (Chicory.checkStaticInit)
              {
                cg.replaceMethod(methods[i], addInvokeToClinit(cg, mg, fullClassName));
                cg.update();
              }
            continue;
          }

        // If method is synthetic...
        if ((Constants.ACC_SYNTHETIC & mg.getAccessFlags()) > 0)
          {
            continue;
          }

        // Get the instruction list and skip methods with no instructions
        InstructionList il = mg.getInstructionList();
        if (il == null)
          continue;

        if (debug)
          out.format ("Original code: %s%n", mg.getMethod().getCode());

        // Create a MethodInfo that describes this methods arguments
        // and exit line numbers (information not available via reflection)
        // and add it to the list for this class.
        MethodInfo mi = (create_method_info (class_info, mg));

        if (mi == null)  //method filtered out!
          continue;

        if (!shouldInclude && ChicoryPremain.debug)
          out.format ("Class %s included [%s]%n", cg.getClassName(),
                      mi);
        shouldInclude = true; //at least one method not filtered out

        method_infos.add (mi);

        cur_method_info_index = Runtime.methods.size();
        Runtime.methods.add (mi);

        // Add nonce local to matchup enter/exits
        String entry_ppt_name = DaikonWriter.methodEntryName(fullClassName,
                                                             getArgTypes(mg), mg.toString(), mg.getName());
        add_method_startup (il, context, !shouldFilter(fullClassName,
                                                       mg.getName(), entry_ppt_name));

        Iterator<Boolean> shouldIncIter = mi.is_included.iterator();
        Iterator<Integer> exitIter = mi.exit_locations.iterator();

        // Loop through each instruction
        for (InstructionHandle ih = il.getStart(); ih != null; ) {
          InstructionList new_il = null;
          Instruction inst = ih.getInstruction();

          // Get the translation for this instruction (if any)
          new_il = xform_inst (fullClassName, inst, context, shouldIncIter, exitIter);

          // Remember the next instruction to process
          InstructionHandle next_ih = ih.getNext();

          // If this instruction was modified, replace it with the new
          // instruction list. If this instruction was the target of any
          // jumps, replace it with the first instruction in the new list
          if (new_il != null) {
            if (true) {
              new_il.delete (new_il.getEnd());
              InstructionHandle new_start = il.insert (ih, new_il);
              //out.format ("old start = %s, new_start = %s%n", ih, new_start);
              il.redirectBranches (ih, new_start);

              // Fix up line numbers to point at the new code
              if (ih.hasTargeters()) {
                for (InstructionTargeter it : ih.getTargeters()) {
                  if (it instanceof LineNumberGen) {
                    it.updateTarget (ih, new_start);
                  }
                }
              }

              ih = next_ih;
              continue;
            }

            if (debug)
              out.format ("Replacing %s by %s%n", ih, new_il);

            il.append (ih, new_il);
            InstructionTargeter[] targeters = ih.getTargeters();
            if (targeters != null) {
              // out.format ("targeters length = %d%n", targeters.length);
              for (int j = 0; j < targeters.length; j++)
                targeters[j].updateTarget (ih, ih.getNext());
            }
            try {
              il.delete (ih);
            } catch (TargetLostException e) {
              throw new Error ("unexpected lost target exception", e);
            }
          }
          // Go on to the next instruction in the list
          ih = next_ih;
        }

        // Remove the Local variable type table attribute (if any).
        // Evidently, some changes we make require this to be updated, but
        // without BCEL support, that would be hard to do.  Just delete it
        // for now (since it is optional, and we are unlikely to be used by
        // a debugger)
        for (Attribute a : mg.getCodeAttributes()) {
          if (is_local_variable_type_table (a)) {
            mg.removeCodeAttribute (a);
          }
        }

        // Update the instruction list
        mg.setInstructionList (il);
        mg.update();

        // Update the max stack and Max Locals
        mg.setMaxLocals();
        mg.setMaxStack();
        mg.update();

        // Update the method in the class
        cg.replaceMethod (methods[i], mg.getMethod());
        if (debug)
          out.format ("Modified code: %s%n", mg.getMethod().getCode());

        // verify the new method
        // StackVer stackver = new StackVer();
        // VerificationResult vr = stackver.do_stack_ver (mg);
        // log ("vr for method %s = %s%n", mg.getName(), vr);
        // if (vr.getStatus() != VerificationResult.VERIFIED_OK) {
        //  System.out.printf ("Warning BCEL Verify failed for method %s: %s",
        //                     mg.getName(), vr);
        //  System.out.printf ("Code: %n%s%n", mg.getMethod().getCode());
        // System.exit(1);
        // }
      }

      cg.update();
    } catch (Exception e) {
      out.format ("Unexpected exception encountered: " + e);
      e.printStackTrace();
    }

    // Add the class and method information to runtime so it is available
    // as enter/exit ppts are processed.
    class_info.set_method_infos (method_infos);

    if (shouldInclude) {
      debug_transform.log ("Added trace info to class %s%n", class_info);
      synchronized (Runtime.new_classes) {
        Runtime.new_classes.add (class_info);
      }
      synchronized (Runtime.all_classes) {
        Runtime.all_classes.add (class_info);
      }
    } else { // not included
      debug_transform.log ("Trace info not added to class %s%n", class_info);
    }

    class_info.shouldInclude = shouldInclude;
    return class_info;
  }

  /**
   * Transforms return instructions to first assign the result to a local
   * variable (return__$trace2_val) and then do the return.  Also, calls
   * Runtime.exit() immediately before the return.
   */
  private InstructionList xform_inst (String fullClassName, Instruction inst, MethodContext c,
                                      Iterator<Boolean> shouldIncIter, Iterator<Integer> exitIter)
  {

    switch (inst.getOpcode()) {

    case Constants.ARETURN:
    case Constants.DRETURN:
    case Constants.FRETURN:
    case Constants.IRETURN:
    case Constants.LRETURN:
    case Constants.RETURN:
      break;

    default:
      return (null);
    }

    if (!shouldIncIter.hasNext())
      throw new RuntimeException("Not enough entries in shouldIncIter");

    boolean shouldInclude = shouldIncIter.next();

    if (!shouldInclude)
      return null;

    Type type = c.mgen.getReturnType();
    InstructionList il = new InstructionList();
    if (type != type.VOID) {
      LocalVariableGen return_loc = get_return_local (c.mgen, type);
      il.append (c.ifact.createDup (type.getSize()));
      il.append (c.ifact.createStore (type, return_loc.getIndex()));
    }

    if (!exitIter.hasNext())
      throw new RuntimeException("Not enough exit locations in the exitIter");

    il.append (call_enter_exit (c, "exit", exitIter.next()));
    il.append (inst);
    return (il);
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
   * Finds the nonce local variable.  Returns null if not present
   */
  LocalVariableGen get_nonce_local (MethodGen mgen) {

    // Find the local used for the nonce value
    for (LocalVariableGen lv : mgen.getLocalVariables()) {
      if (lv.getName().equals ("this_invocation_nonce")) {
        return (lv);
      }
    }

    return (null);
  }

  /**
   * Adds a local variable  (this_invocation_nonce) that is initialized
   * to Runtime.nonce++.  This provides a unique id on each method entry/exit
   * that allows them to be matched up from the dtrace file.  Also calls
   * Runtime.enter()
   */
  private void add_method_startup (InstructionList il, MethodContext c, boolean shouldCallEnter) {

    InstructionList nl = new InstructionList();

    // create the local variable
    LocalVariableGen nonce_lv= c.mgen.addLocalVariable("this_invocation_nonce",
                                                       Type.INT, null, null);

    //
    // The following implements this_invocation_nonce = Runtime.nonce++
    //

    // getstatic Runtime.nonce (push its current value on stack)
    nl.append (c.ifact.createGetStatic (runtime_classname, "nonce",
                                        Type.INT));

    // dup (make a second copy of runtime.nonce on the stack)
    nl.append (c.ifact.createDup (Type.INT.getSize()));

    // iconst_1 (push 1 on the stack)
    nl.append (c.ifact.createConstant (1));

    // iadd (add the top two items on the stack together)
    nl.append (c.ifact.createBinaryOperation ("+", Type.INT));

    // putstatic Runtime.nonce (pop result of add to Runtime.nonce)
    nl.append (c.ifact.createPutStatic (runtime_classname, "nonce",
                                        Type.INT));

    // istore <lv> (pop original value of nonce into this_invocation_nonce)
    nl.append (c.ifact.createStore (Type.INT, nonce_lv.getIndex()));


    if (shouldCallEnter)
      {
        // call Runtime.enter()
        nl.append (call_enter_exit (c, "enter", -1));
      }

    // Add the new instruction at the start and move any LineNumbers
    // and Local variables to point to them.  Other targeters
    // (branches, exceptions) should still point to the old start
    InstructionHandle old_start = il.getStart();
    InstructionHandle new_start = il.insert (nl);
    for (InstructionTargeter it : old_start.getTargeters()) {
      if ((it instanceof LineNumberGen) || (it instanceof LocalVariableGen))
        it.updateTarget (old_start, new_start);
    }

    /*
    // Add the new code to the front of the method
    InstructionHandle start = il.getStart();
    nl.append (start.getInstruction());
    start.setInstruction (nl.getStart().getInstruction());
    try {
    nl.delete (nl.getStart());
    } catch (TargetLostException e) {
    throw new Error ("unexpected lost target exception", e);
    }
    il.append (start, nl);

    // there shouldn't be jumps to the first opcode of the method
    InstructionTargeter[] targeters = start.getTargeters();
    if (targeters.length > 0) {
    // out.format ("%d targets point to %s%n", targeters.length, start);
    for (InstructionTargeter it : targeters) {
    // out.format ("    targeter: %s%n", it);
    assert !(it instanceof BranchInstruction) : "target " + it;
    if (it instanceof CodeExceptionGen) {

    }
    }
    */
  }

  /**
   * Pushes the object, nonce, parameters, and return value
   * on the stack and calls the specified Method (normally
   * enter or exit) in Runtime.  The parameters are passed
   * as an array of objects.  Any primitive values are wrapped
   * in the appropriate Runtime wrapper (IntWrap, FloatWrap, etc)
   */
  InstructionList call_enter_exit (MethodContext c, String method_name, int line) {

    InstructionList il = new InstructionList();
    InstructionFactory ifact = c.ifact;
    MethodGen mgen = c.mgen;
    Type[] arg_types = mgen.getArgumentTypes();

    // Push the object.  Null if this is a static method or a constructor
    if (mgen.isStatic() ||
        (method_name.equals ("enter") && is_constructor (mgen))) {
      il.append (new ACONST_NULL());
    } else { // must be an instance method
      il.append (ifact.createLoad (Type.OBJECT, 0));
    }

    // Determine the offset of the first parameter
    int param_offset = 1;
    if (c.mgen.isStatic())
      param_offset = 0;

    // Push the nonce
    LocalVariableGen nonce_lv = get_nonce_local (mgen);
    il.append (ifact.createLoad (Type.INT, nonce_lv.getIndex()));

    // Push the MethodInfo index
    il.append (ifact.createConstant (cur_method_info_index));

    // Create an array of objects with elements for each parameter
    il.append (ifact.createConstant (arg_types.length));
    Type object_arr_typ = new ArrayType ("java.lang.Object", 1);
    il.append (ifact.createNewArray (Type.OBJECT, (short) 1));

    // Put each argument into the array
    int param_index = param_offset;
    for (int ii = 0; ii < arg_types.length; ii++) {
      il.append (ifact.createDup (object_arr_typ.getSize()));
      il.append (ifact.createConstant (ii));
      Type at = arg_types[ii];
      if (at instanceof BasicType) {
        il.append (create_wrapper (c, at, param_index));
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
          il.append (create_wrapper (c, ret_type, return_local.getIndex()));
        } else {
          il.append (ifact.createLoad (Type.OBJECT, return_local.getIndex()));
        }
      }


      //push line number
      //System.out.println(c.mgen.getName() + " --> " + line);
      il.append (ifact.createConstant (line));
    }

    // Call the specified method
    Type[] method_args = null;
    if (method_name.equals ("exit"))
      method_args = new Type[] {Type.OBJECT, Type.INT, Type.INT,
                                object_arr_typ, Type.OBJECT, Type.INT};
    else
      method_args = new Type[] {Type.OBJECT, Type.INT, Type.INT,
                                object_arr_typ};
    il.append (c.ifact.createInvoke (runtime_classname, method_name,
                                     Type.VOID, method_args, Constants.INVOKESTATIC));


    return (il);
  }

  /**
   * Creates code to put the local var/param at the specified var_index
   * into a wrapper appropriate for prim_type.  prim_type should be one
   * of the basic types (eg, Type.INT, Type.FLOAT, etc).  The wrappers
   * are those defined in Runtime.
   *
   * The stack is left with a pointer to the newly created wrapper at the
   * top.
   */
  private InstructionList create_wrapper (MethodContext c, Type prim_type,
                                          int var_index) {

    String wrapper = null;
    switch (prim_type.getType()) {
    case Constants.T_BOOLEAN: wrapper = "BooleanWrap"; break;
    case Constants.T_BYTE:    wrapper = "ByteWrap"; break;
    case Constants.T_CHAR:    wrapper = "CharWrap"; break;
    case Constants.T_DOUBLE:  wrapper = "DoubleWrap"; break;
    case Constants.T_FLOAT:   wrapper = "FloatWrap"; break;
    case Constants.T_INT:     wrapper = "IntWrap"; break;
    case Constants.T_LONG:    wrapper = "LongWrap"; break;
    case Constants.T_SHORT:   wrapper = "ShortWrap"; break;
    default:
    assert false : "unexpected type " + prim_type;
    }

    InstructionList il = new InstructionList();
    String classname = runtime_classname + "$" + wrapper;
    il.append (c.ifact.createNew (classname));
    il.append (c.ifact.createDup (Type.OBJECT.getSize()));
    il.append (c.ifact.createLoad (prim_type, var_index));
    il.append (c.ifact.createInvoke (classname, "<init>", Type.VOID,
                                     new Type[] {prim_type}, Constants.INVOKESPECIAL));

    return (il);
  }

  /**
   * Returns true iff mgen is a constructor
   * @return true iff mgen is a constructor
   */
  private boolean is_constructor (MethodGen mgen) {

    if (mgen.getName().equals ("<init>") || mgen.getName().equals ("")) {
      // log ("method '%s' is a constructor%n", mgen.getName());
      return (true);
    } else
      return (false);
  }

  /**
   * Return an array of strings, each corresponding to mgen's argument types
   * @return an array of strings, each corresponding to mgen's argument types
   */
  String[] getArgTypes(MethodGen mgen)
  {
    Type[] arg_types = mgen.getArgumentTypes();
    String[] arg_type_strings = new String[arg_types.length];
    for (int ii = 0; ii < arg_types.length; ii++)
      {
        Type t = arg_types[ii];
        /*if (t instanceof ObjectType)
          arg_type_strings[ii] = ((ObjectType) t).getClassName();
          else
          arg_type_strings[ii] = t.getSignature().replace('/', '.');
        */
        arg_type_strings[ii] = t.toString();
      }

    return arg_type_strings;
  }

  //creates a MethodInfo struct corresponding to mgen
  @SuppressWarnings("unchecked")
    private MethodInfo create_method_info(ClassInfo class_info, MethodGen mgen)
  {
    // Get the argument names for this method
    String[] arg_names = mgen.getArgumentNames();
    LocalVariableGen[] lvs = mgen.getLocalVariables();
    int param_offset = 1;
    if (mgen.isStatic())
      param_offset = 0;
    if (lvs != null)
      {
        for (int ii = 0; ii < arg_names.length; ii++)
          {
            if ((ii + param_offset) < lvs.length)
              arg_names[ii] = lvs[ii + param_offset].getName();
          }
      }

    boolean shouldInclude = false;

    //see if we should filter the entry point
    if (!shouldFilter(class_info.class_name, mgen.getName(),
                      DaikonWriter.methodEntryName(class_info.class_name, getArgTypes(mgen), mgen.toString(), mgen.getName())))
      shouldInclude = true;

    // Get the argument types for this method
    Type[] arg_types = mgen.getArgumentTypes();
    String[] arg_type_strings = new String[arg_types.length];
    for (int ii = 0; ii < arg_types.length; ii++)
      {
        Type t = arg_types[ii];
        if (t instanceof ObjectType)
          arg_type_strings[ii] = ((ObjectType) t).getClassName();
        else
          arg_type_strings[ii] = t.getSignature().replace('/', '.');
      }

    // Loop through each instruction and find the line number for each
    // return opcode
    List<Integer> exit_locs = new ArrayList<Integer>();

    //tells whether each exit loc in the method is included or not (based on filters)
    List<Boolean> isIncluded = new ArrayList<Boolean>();

    // log ("Looking for exit points in %s%n", mgen.getName());
    InstructionList il = mgen.getInstructionList();
    int line_number = 0;
    int last_line_number = 0;
    boolean foundLine;

    for (Iterator<InstructionHandle> ii = (Iterator<InstructionHandle>) il.iterator(); ii.hasNext();) // unchecked cast: BCEL is non-generic
      {
        InstructionHandle ih = ii.next();

        foundLine = false;

        if (ih.hasTargeters())
          {
            for (InstructionTargeter it : ih.getTargeters())
              {
                if (it instanceof LineNumberGen)
                  {
                    LineNumberGen lng = (LineNumberGen) it;
                    // log ("  line number at %s: %d%n", ih, lng.getSourceLine());
                    //System.out.printf("  line number at %s: %d%n", ih, lng.getSourceLine());
                    line_number = lng.getSourceLine();
                    foundLine = true;
                  }
              }
          }

        switch (ih.getInstruction().getOpcode())
          {
          case Constants.ARETURN :
          case Constants.DRETURN :
          case Constants.FRETURN :
          case Constants.IRETURN :
          case Constants.LRETURN :
          case Constants.RETURN :
            // log ("Exit at line %d%n", line_number);

            //only do incremental lines if we don't have the line generator
            if (line_number == last_line_number && foundLine == false)
              {
                //System.out.printf("Could not find line... at %d%n", line_number);
                line_number++;
              }

            last_line_number = line_number;

            if (!shouldFilter(class_info.class_name, mgen.getName(),
                              DaikonWriter.methodExitName(class_info.class_name, getArgTypes(mgen), mgen.toString(), mgen.getName(), line_number)))
              {
                shouldInclude = true;
                exit_locs.add(new Integer(line_number));

                isIncluded.add(true);
              }
            else
              isIncluded.add(false);

            break;

          default :
            break;
          }
      }

    if (shouldInclude)
      return new MethodInfo(class_info, mgen.getName(), arg_names, arg_type_strings, exit_locs, isIncluded);
    else
      return null;
  }

  public boolean is_local_variable_type_table (Attribute a) {
    return (get_attribute_name (a).equals ("LocalVariableTypeTable"));
  }

  /**
   * Returns the attribute name for the specified attribute
   */
  public String get_attribute_name (Attribute a) {

    int con_index = a.getNameIndex();
    Constant c = pgen.getConstant (con_index);
    String att_name = ((ConstantUtf8) c).getBytes();
    return (att_name);
  }

  /**
   * Any information needed by InstTransform routines about the method
   * and class
   */
  private static class MethodContext {

    public ClassGen cg;
    public ConstantPoolGen cpg;
    public InstructionFactory ifact;
    public MethodGen mgen;

    public MethodContext (ClassGen cg) {
      this.cg = cg;
      ifact = new InstructionFactory (cg);
      cpg = cg.getConstantPool();
    }

    public MethodContext (ClassGen cg, MethodGen mgen) {
      this.cg = cg;
      ifact = new InstructionFactory (cg);
      cpg = cg.getConstantPool();
      this.mgen = mgen;
    }
  }

  /**
   * Returns whether or not the specified class is part of chicory
   * itself (and thus should not be instrumented).  Some daikon classes
   * that are used by Chicory are included here as well
   */
  private static boolean is_chicory (String classname) {
    if (classname.startsWith ("daikon/chicory")
        && !classname.equals ("daikon/chicory/Test"))
      return true;
    if (classname.equals ("daikon/PptTopLevel$PptType"))
      return true;
    return false;
  }

}
