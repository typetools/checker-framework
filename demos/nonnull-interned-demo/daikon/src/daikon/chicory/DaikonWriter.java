package daikon.chicory;

import java.lang.reflect.*;
import daikon.Chicory;
import utilMDE.UtilMDE;
/**
 * DaikonWriter is the parent class of DeclWriter and DTraceWriter.
 *
 */
public abstract class DaikonWriter
{
    /**
     * Controls whether modifiers and the return type are included in
     * the decl output
     **/
    protected static final boolean no_modifiers_ppt = true;

    /** Platform dependent line separator.  Should be "\n" on Unix **/
    public static final String lineSep;

    static
    {
        lineSep = System.getProperty("line.separator");
        assert lineSep != null : "Line separator cannot be null";
    }

    protected DaikonWriter()
    {
    }

    /**
     * Determines if this field warrants an [ = val ] entry in decls file
     *
     * @param field requires field != null
     * @return true iff field warrants an [ = val ] entry in the decls files
     */
    protected static boolean isStaticConstField(Field field)
    {
        Class type = field.getType();
        int mod = field.getModifiers();

        if (DaikonVariableInfo.dkconfig_constant_infer) {
            return Modifier.isFinal(mod) && Modifier.isStatic(mod);
        }
        else {
        	return Modifier.isFinal(mod) && Modifier.isStatic(mod) && type.isPrimitive();
        }
    }

    /**
     * Given a method, returns the method entry program point name for Daikon
     * @param method non-null method
     * @return the decorated method entry name for Daikon
     */
    public static String methodEntryName(Member method) {
        return methodName (method, "ENTER");
    }

    /**
     * Given a method, returns the method entry program point name for Daikon
     * method entry name for Daikon.  Used when reflection information is
     * not available
     *
     * @param types Argument types
     * @return the decorated method entry name for Daikon
     */
    public static String methodEntryName(String fullClassName, String[] types,
                                         String name, String short_name)
    {
        return methodName(fullClassName, types, name, short_name, "ENTER");
    }
    /**
     * Given a method, returns the method exit program point name for Daikon
     * @param method require method != null
     * @param lineNum The line number of the exit point of the method
     * @return the decorated method exit name for Daikon
     */
    public static String methodExitName(Member method, int lineNum)
    {
        return methodName(method, "EXIT" + lineNum);
    }
    /**
     * Given a method, returns the method exit program point name for Daikon
     *
     * @param types Argument types
     * @param lineNum The line number of the exit point of the method
     * @return the decorated method entry name for Daikon
     */
    public static String methodExitName(String fullClassName, String[] types,
                              String name, String short_name, int lineNum)
    {
        return methodName(fullClassName, types, name, short_name, "EXIT" + lineNum);
    }

    /**
     * Constructs the program point name (which includes the point
     * string at the end)
     *
     * @param fullClassName packageName.className
     * @param types String representation of the declared types of the
     *          parameters.  for example: {"int", "java.lang.Object", "float"}
     * @param name The method with modifiers and parameters
     * @param short_name Just the method's name (except it is "<init>"
     * for constructors)
     *
     * So a corresponding name/short_name pair could be:
     *    name: public static void DataStructures.StackArTester.doNew(int size)
     *    short_name: doNew
     *
     * @param point Usually "EXIT" or "ENTER"
     * @return Same thing as methodName(Member, point)
     */
    private static String methodName(String fullClassName, String[] types,
                              String name, String short_name, String point) {

        //System.out.printf("fullclass: %s !!! name: %s !!! short_name: %s %n",
        //                  fullClassName, name, short_name);


        boolean isConstructor = name.equals("<init>") || name.equals("");

        if (isConstructor) {
          // replace <init>'s with the actual class name
          // so "public void <init>" becomes "public void StackAr" for example
          short_name = fullClassName.substring(fullClassName.lastIndexOf('.')
                                               + 1);
          name = name.replace("<init>", short_name);
        }

        // build up the string to go inside the parens
        StringBuilder paramTypes = new StringBuilder();
        paramTypes.append("(");
        for (int i = 0; i < types.length; i++)
        {
            paramTypes.append(types[i]);

            if (i != types.length - 1)
                paramTypes.append(",");
        }
        paramTypes.append(")");
        String pptname = fullClassName + "." + short_name + paramTypes
            + ":::" + point;

        if (Chicory.debug_ppt_names)
          System.out.printf ("methodName1 final ppt name = '%s'%n", pptname);

        //Throwable t = new Throwable("debug");
        //t.fillInStackTrace();
        // t.printStackTrace();
        // System.out.printf ("ppt name = %s%n", pptname);

        return (pptname);

        /*
        // Quote dollar signs, which replaceFirst would interpreted as a
        // group reference.
        String paramTypesString = paramTypes.toString().replace("$", "\\$");
        name = name.replaceFirst("\\(.*\\)", paramTypesString);
        // System.out.printf ("params = %s, newname = %s, short_name = %s%n",
        //                   paramTypesString, name, short_name);

        return methodName(name, short_name, isConstructor, point);
        */
    }

    /**
     * Constructs the program point name (which includes the point
     * string at the end)
     *
     * @param method Reflection object for the method/constructor
     * @param point Usually "ENTER" or "EXIT"
     */
    private static String methodName(Member member, String point) {

      String ppt_name = null;

      String fullname;
      Class args[];
      Class declaring_class = member.getDeclaringClass();
      if (member instanceof Method) {
        Method method = (Method) member;
        fullname = declaring_class.getName() + "." + method.getName();
        args = method.getParameterTypes();
      } else {
        Constructor constructor = (Constructor) member;
        fullname = declaring_class.getName() + "."
          + UtilMDE.unqualified_name (declaring_class);
        args = constructor.getParameterTypes();
      }
      String arg_str = "";
      for (Class arg : args) {
        if (arg_str.length() > 0)
          arg_str += ", ";
        if (arg.isArray())
          arg_str += UtilMDE.classnameFromJvm (arg.getName());
        else
          arg_str += arg.getName();
      }
      ppt_name = String.format ("%s(%s):::%s", fullname, arg_str, point);
      return ppt_name;
    }

    /**
     * Constructs the program point name (which includes the point
     * string at the end)
     *
     * @param method Reflection object for the method/constructor
     * @param point Usually "ENTER" or "EXIT"
     */
    private static String old_methodName(Member method, String point)
    {
        String name = method.toString();
        if (Chicory.debug_ppt_names)
          System.out.printf ("methodName2: '%s' '%s'%n", name, point);

        if (method instanceof Constructor) {
            name = fixDuplicateConstructorName(name, method.getName());

            if (Chicory.debug_ppt_names)
              System.out.printf ("  constructor '%s'%n", name);
        }


        // Remove the modifiers and the type
        if (no_modifiers_ppt) {
            // At this point, name might look something like:
            // public boolean DataStructures.StackAr.push(java.lang.Object) \
            //          throws Exception
            // Get ride of throws and everything after it
            // (and the space right before it)
            int index = name.indexOf (" throws");
            if (index > 0)
                name = name.substring (0, index);
            if (Chicory.debug_ppt_names)
              System.out.printf ("  throws '%s'%n", name);

            // Get rid of modifiers before the method name
            // (public boolean in above example)
            index = name.lastIndexOf (' ');
            if (index > 0)
                name = name.substring(index+1);
            if (Chicory.debug_ppt_names)
              System.out.printf ("  modifiers '%s'%n", name);
        }
        name = name.replace (",", ", ");
        if (Chicory.debug_ppt_names)
          System.out.printf ("  spaces '%s'%n", name);

        // System.out.printf ("'%s' to '%s'%n", method.toString(), name);
        return (name + ":::" + point);
    }

    /**
     *
     * Dfej repeats the name of the class for
     * constructors (eg, DataStructures.StackAr() becomes
     * DataStructures.StackAr.StackAr().  This makes it clear
     * that SomePackage.ClassName.ClassName and SomePackage.ClassName.OtherMethod
     * are in the same class. Mimic that behavior.
     *
     */
    private static String fixDuplicateConstructorName(String name,
            String short_name)
    {
        // assert short_name.lastIndexOf(".") == -1 : "short_name: " + short_name
        //       + " should not contain a period ('.') character. ";

        int lastPeriod = short_name.lastIndexOf(".");
      if (Chicory.debug_ppt_names)
        System.out.printf ("  fixdup; '%s' '%s' [%d]%n", name, short_name,
                           lastPeriod);

        if (lastPeriod == -1)
        {
          if (Chicory.debug_ppt_names)
            System.out.printf ("  -1: replace '%s' with '%s'%n",
                               short_name + "(",
                               short_name + "." + short_name + "(");
            return name.replace(short_name + "(", short_name + "." + short_name
                    + "(");
        }
        else
        {
            // This case could occur for constructor names given as PackageName.ClassName


            short_name = short_name.substring(lastPeriod + 1);

          if (Chicory.debug_ppt_names)
            System.out.printf ("  >0: replace '%s' with '%s'%n",
                               "." + short_name + "(",
                               "." + short_name + "." + short_name + "(");
            String result = name.replace("." + short_name + "(", "."
                                        + short_name + "." + short_name + "(");
            if (Chicory.debug_ppt_names) {
              System.out.printf ("  result '%s'%n", result);
              String search = "." + short_name + "(";
              String replace = "." + short_name + "." + short_name + "(";
              System.out.printf ("  alt '%s' '%s' '%s' '%s'%n", search,
                                 name, replace, name.replace (search, replace));
            }
            return result;
        }
    }

    /**
     * Determines if the given method should be instrumented
     */
    protected boolean shouldInstrumentMethod(Member method)
    {
        int modifiers = method.getModifiers();
        if (Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers) || method.getName().equals("<clinit>"))
            return false;
        return true;
    }

    /**
     * Returns the class name of the specified class in 'java' format
     * (i.e., as the class would have been declared in java source code)
     */
    public static String stdClassName (Class type)
    {
        return Runtime.classnameFromJvm (type.getName());
    }

  /**
   * Escapes blanks and backslashes in names written to the decl/dtrace files
   */
  public String escape (String str) {

    // If there is nothing to escape, return the original string
    if ((str.indexOf ('\\') == -1) && (str.indexOf (' ') == -1))
      return (str);

    str = str.replace ("\\", "\\\\");
    str = str.replace (" ", "\\_");
    return (str);
  }
}
