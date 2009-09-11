package daikon.test;

import utilMDE.*;
import daikon.*;

import daikon.inv.Invariant;
import daikon.inv.OutputFormat;

import daikon.inv.unary.UnaryInvariant;
import daikon.inv.binary.BinaryInvariant;
import daikon.inv.ternary.threeScalar.ThreeScalar;

import java.io.*;

import java.lang.reflect.*;

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import utilMDE.Assert;
import utilMDE.Intern;

/**
 * This class is used by InvariantFormatTester to store data
 * representing test cases and for formatting results related to that
 * data. This class is related to tests performed on one invariant.
 **/
class FormatTestCase {

  private static final String lineSep = Global.lineSep;

  /**
   * An inner class that represents a particular test on the invariant
   * represented by a FormatTestCase object. This is used so this code
   * can easily be extended to incorporating more than one test case
   * under an invariant heading, such as what is currently done in the
   * main code with having multiple goal outputs per invariant.
   **/
  static class SingleOutputTestCase {

    /**
     * A method that produces the formatting output when invoked.
     **/
    private Method outputProducer;

    /**
     * The arguments that must be passed to outputProducer to create the output.
     **/
    private Object [] outputProducerArgs;

    /**
     * The goal output for this particular test case.
     **/
    private String goalOutput;

    /**
     * The line number in the input file in which the goalOutput should occur in
     * the output file.
     **/
    private int goalLineNumber;

    /**
     * A cached copy of the result achieved by invoking the output method.
     **/
    private String resultCache;

    /**
     * A string containing the format that this particular test case represented.
     **/
    private String formatString;

    /**
     * This constructor initializes all of the fields without any
     * checking for legality (that should be done by the callers).
     *
     * @param outputProducer the output producing function (should return a String)
     * @param outputProducerArgs the arguments to be passed to the outputProducer
     *        method
     * @param goalOutput the desired output of the outputProducer function (that
     *        would mean the test was passed
     * @param goalLineNumber the line number in the input file in which the goal
     * should occur
     * @param formatString the format that this test case belongs to
     **/
    public SingleOutputTestCase(Method outputProducer,
                                Object[] outputProducerArgs,
                                String goalOutput,
                                int goalLineNumber,
                                String formatString) {
      this.outputProducer = outputProducer;
      this.outputProducerArgs = outputProducerArgs;
      this.goalOutput = goalOutput;
      this.goalLineNumber = goalLineNumber;
      resultCache = null;
      this.formatString = formatString;
    }

    /**
     * This function invokes the output producing function inside the object.
     *
     * @param inv the Invariant object on which to invoke the function
     * @return a String representing the output
     **/
    public String createTestOutput(Invariant inv) {
      try {
        if (resultCache == null)
          resultCache = (String)outputProducer.invoke(inv,outputProducerArgs);
        if (FileIO.new_decl_format)
          resultCache = VarInfo.old_var_names (resultCache);
        return resultCache;
      }
      catch (IllegalAccessException e) {
        throw new RuntimeException(e.toString());
      }
      catch (InvocationTargetException e) {
        System.out.println("***" + inv.getClass() + "***" + utilMDE.ArraysMDE.toString(outputProducerArgs));
        System.out.println("^^^" + e.toString());
        System.out.println("^^^" + e.getMessage());
        System.out.println("^^^" + e.getCause());
        e.printStackTrace();
        throw new RuntimeException ("unexpected exception", e);
        // throw new RuntimeException(e.toString() + e.getMessage()
        //                           + e.getCause());
      }
    }

    /**
     * This function tests whether this test is passed.
     *
     * @param inv the Invariant object on which to perform the test
     * @return true if the test is passed, false otherwise
     **/
    public boolean performTest(Invariant inv) {
      return createTestOutput(inv).equals(goalOutput);
    }

    /**
     * This function returns the line number that the goal should be listed on
     * in the commands file.
     *
     * @return the line number that the goal should be listed on
     *         in the commands file
     **/
    public int getGoalLineNumber() {
      return goalLineNumber;
    }

    /**
     * This function returns the format string of which the test case is a part.
     *
     * @return the format string of which the test case is a part
     **/
    public String getFormatString() {
      return formatString;
    }

    /**
     * This function creates a String representing the differences between the
     * goal output and the actual output; empty if there are no differences.
     *
     * @return a String as described above
     **/
    public String getDiffString() {
      if (resultCache != null && !resultCache.equals(goalOutput)) {
        return "Error on line " + goalLineNumber + ":" + lineSep +
          "Expected result:" + lineSep + goalOutput + lineSep +
          "Returned result:" + lineSep + resultCache;
      }
      return "";
    }
  }
  // End of SingleOutputTestCase

  /**
   * Prefix to each goal line in the file for identitication.
   **/
  private static final String GOAL_PREFIX = "Goal";

  /**
   * A list of all of the test cases (which are SingleOutputTestCase
   * objects) that are to be performed on the contained Invariant.
   **/
  private List<SingleOutputTestCase> testCases;

  /**
   * The Invariant object to be tested.
   **/
  private Invariant invariantToTest;

  /**
   * This function constructs a FormatTestCase object directly from passed
   * in objects. It is to be called internally by instantiate to create an
   * instance of FormatTestCase
   *
   * @param testCases a List of SingleOutputTestCase objects to be performed
   *        on an Invariant
   * @param invariantToTest the Invariant on which the tests are to be
   *        performed
   **/
  private FormatTestCase(List<SingleOutputTestCase> testCases, Invariant invariantToTest) {
    this.testCases = testCases;
    this.invariantToTest = invariantToTest;
  }

  /**
   * This function generates the way a revised input file section for
   * a given invariant test should look upon being supplemented with
   * generated goals. The output represents an entire invariant up
   * until the last goal statement for any test of the invariant since
   * the test cases do not store what their samples look like in
   * String format.  Therefore, it is recommended that the only use of
   * this function be to get output from the invariant tests in the
   * order they appear in the file.  (Note: The output is done is this
   * particular way to maintain comments and white space between
   * Invariants)
   *
   * @param theInputFile a LineNumberReader object representing the input file
   * @throws IOException if reading operations from the input buffer fail
   * @return a String representing the goal output file object
   **/
  public String generateGoalOutput(LineNumberReader theInputFile) throws IOException {
    StringBuffer output = new StringBuffer();
    String currentLineOfText = null;
    int currentLine = theInputFile.getLineNumber();

    // System.out.println("Generating goal output");
    for (int i=0; i<testCases.size(); i++) {
      // System.out.println("Goal output gen: " + i);
      SingleOutputTestCase current = testCases.get(i);
      int currentGoalLineNumber = current.getGoalLineNumber();
      for (int j=currentLine; j<currentGoalLineNumber; j++) {
        currentLineOfText = theInputFile.readLine();
        if (parseGoal(currentLineOfText) == null)
          output.append(currentLineOfText + lineSep);
      }
      output.append(GOAL_PREFIX + " (" + current.getFormatString() + "): " +
                    current.createTestOutput(invariantToTest));
      if (i != testCases.size()-1)
        output.append(lineSep);
      currentLine = currentGoalLineNumber;
    }

    return output.toString();
  }

  /**
   * Checks to see whether all tests on this invariant are passed.
   *
   * @return true if all the tests on this invariant passed, false otherwise
   **/
  public boolean passes() {
    boolean passTest = true;
    boolean currentResult;

    // Changing the loop to to
    // for (SingleOutputTestCase sotc : testCases) {
    //   currentResult = sotc.performTest(invariantToTest);
    // yields an internal compiler exception (for JSR 308 compiler, 8/21/2007).
    for (int i=0; i<testCases.size(); i++) {
      currentResult = testCases.get(i).performTest(invariantToTest);
      passTest = passTest && currentResult;

    }
    return passTest;
  }

  /**
   * This function creates a String representing the difference between the test
   * result and the desired result.
   *
   * @return a String representing the difference between the test
   * result and the desired result
   **/
  public String getDiffString() {
    StringBuffer result = new StringBuffer();
    String currentDiffString;

    for (int i=0; i<testCases.size(); i++) {
      currentDiffString = testCases.get(i).getDiffString();
      result.append(currentDiffString);
      if (i != testCases.size() && currentDiffString != "") // "interned"
        result.append(lineSep + lineSep);
    }

    return result.toString();
  }

  /**
   * This function loads a class from file into the JVM given its
   * fully-qualified name.
   *
   * @param classInfo the fully-qualified class name
   * @return a Class object representing the class name if such a class is
   *         defined, otherwise null
   **/
  private static Class getClass(String classInfo) {
    try {
      return ClassLoader.getSystemClassLoader().loadClass(classInfo);
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException(e.toString());
    }
  }

  /**
   * This function takes in a String representing a goal statement
   * and returns the actual String to be returned by a test.
   *
   * @return the actual result String represented by the goal statement or
   *          null if the String isn't actually a goal statement
   **/
  static String parseGoal(String goalString) {
    if (goalString.startsWith(GOAL_PREFIX)) {
      return goalString.substring(GOAL_PREFIX.length(),goalString.length());
    }
    return null;
  }

  static String getFormat(String partialGoalString) {
    try {
      return partialGoalString.substring(partialGoalString.indexOf('(')+1,
                                         partialGoalString.indexOf(')'));
    }
    catch (IndexOutOfBoundsException e) {
    }

    return null;
  }

  static String getGoalOutput(String partialGoalString) {
    try {
      return partialGoalString.substring(partialGoalString.indexOf(':')+2,
                                         partialGoalString.length());
    }
    catch (IndexOutOfBoundsException e) {
    }

    return null;
  }

  /**
   * This function is an alias for the {@link #getNextRealLine(BufferedReader) getNextRealLine}
   * method.
   **/
  static String getNextRealLine(BufferedReader buffer) {
    return InvariantFormatTester.getNextRealLine(buffer);
  }

  /**
   * This function returns a FormatTestCase instance after parsing it
   * from file or null if the end of the file has been reached.
   *
   * @param commands a reader object representing the file to be parsed that
   *        contains data necessary to initialize the new object. The file
   *        must conform to the format described in
   *        InvariantFormatTester.Description
   * @param generateGoals true if goal generation is desired, false if goal testing
   *        is desired
   * @return a new FormatTestCase instance
   **/
  public static FormatTestCase instantiate(LineNumberReader commands, boolean generateGoals) {
    List<SingleOutputTestCase> testCases = new Vector<SingleOutputTestCase>();

    // The first line contains the class and its instantiate args
    // each token is separated by blanks.  Each argument to instantiate
    // consists of a type and its value.  For example
    // daikon.inv/binary.twoScalar.NumericInt$divides boolean true
    String line = getNextRealLine((BufferedReader)commands);
    if (line == null) return null;
    String[] tokens = line.split ("  *");
    String className = tokens[0];
    int arg_count = (tokens.length - 1) / 2;
    Class[] arg_types  = new Class[arg_count];
    Object[] arg_vals = new Object[arg_count];
    int arg_index = 0;
    for (int i = 1; i < tokens.length; i+=2) {
      String arg_type_name = tokens[i].intern();
      if (i+1 >= tokens.length)
        throw new RuntimeException ("No matching arg val for argument  type"
                                    + arg_type_name);
      String arg_val = tokens[i+1];
      Object val;
      Class val_type;
      if (arg_type_name == "boolean") { // interned
        val = Boolean.valueOf (arg_val);
        val_type = boolean.class;
      } else if (arg_type_name == "int") { // interned
        val = Integer.valueOf (arg_val);
        val_type = int.class;
      } else {
        throw new RuntimeException ("Unexpected type " + arg_type_name);
      }
      arg_types[arg_index] = val_type;
      arg_vals[arg_index] = val;
      arg_index++;
    }

    // System.out.println("On class " + className);

    // Load the class from file
    @SuppressWarnings("unchecked")
    Class<? extends Invariant> classToTest = (Class<? extends Invariant>) getClass(className);

    try {
      Field f = classToTest.getField("dkconfig_enabled");
      f.setBoolean (null, true);
      // InvariantFormatTester.config.apply(className + ".enabled", "true");
    }
    catch (Exception e) { // Otherwise do nothing
    }

    // Instantiate variables to be used as the names in the
    // invariants, variables are labelled a,b,c and so on as they
    // appear
    String typeString = getNextRealLine((BufferedReader)commands);

    ProglangType[] types = getTypes(typeString);
    VarInfo[] vars =
      getVarInfos(classToTest, types);
    PptSlice sl = createSlice(vars,
                            Common.makePptTopLevel("Test:::OBJECT", vars));
    Assert.assertTrue (sl != null);

    // Create an actual instance of the class
    Invariant invariantToTest = instantiateClass(classToTest, sl, arg_types,
                                                 arg_vals);
    Assert.assertTrue (invariantToTest != null, "class " + className);

    String goalOutput = "";
    String currentLine = null;

    int goalLineNumber = commands.getLineNumber();

    Method outputProducer = null;
    Object[] outputProducerArgs = null;
    String format = null;

    Iterator<String> formatStrings = null;


    // If not generating goals get the goal lines from the file
    // If generating goals get the formats from the list of formats
    if (!generateGoals) {
      goalOutput = parseGoal(getNextRealLine(commands));
      if (goalOutput == null)
        throw new RuntimeException("Bad format of goal data");
    } else {
      formatStrings = InvariantFormatTester.TEST_FORMAT_LIST.iterator();
    }

    while (goalOutput != null) {
      if (generateGoals) {
        if (!formatStrings.hasNext()) {
          goalOutput = null;
        } else {
          format = formatStrings.next();
          goalOutput = "init"; // Need something non-whitespace
        }
      } else {
        format = getFormat(goalOutput);
        goalOutput = getGoalOutput(goalOutput);

        if (format == null || goalOutput == null) {
          throw new RuntimeException("Goal string formatted incorrectly");
        }
        goalLineNumber++;
      }

      // System.out.println("Possibly add a test case:");
      // System.out.println("Goal output = " + goalOutput);

      // Get the method used to perform the formatting
      if (goalOutput != null && !InvariantFormatTester.isWhitespace(goalOutput)) {
        // System.out.println("Using format: " + format);


        // Update 2-27-2004: For some reason, this test used to
        // attempt to find a format_java() or format_esc(), etc inside
        // the invariant method and only if that method did not exist would
        // format_using (OutputFormat) be used.  I suspect it is due to
        // a different person writing the OutputFormat that simply decided
        // to add on to what was existing. In any event, there seems to be
        // no reason to rely on the old format_xxx(), we want to eliminate
        // those and only use the format_using (OutputFormat)

        //        try {
        //    outputProducer = classToTest.getMethod("format_" + format, null);
        //   outputProducerArgs = null;
        //   }
        //   catch (NoSuchMethodException e) {
           try {
            outputProducer =
              classToTest.getMethod("format_using", new Class [] {OutputFormat.class});
            OutputFormat output_format = OutputFormat.get(format);
            if (output_format == null) {
              throw new RuntimeException("bad output format " + format);
            }
            outputProducerArgs = new Object [] { output_format };
          }
          catch (NoSuchMethodException e2) {
            throw new RuntimeException("Could not find format_using method");
          }
           //     }

        // System.out.println("Adding a test case");

        // System.out.println("Method name: " + outputProducer.getName());
        // System.out.println("Goal output: " + goalOutput);
        // System.out.println("Goal line number: " + goalLineNumber);
        // System.out.println("Format string: " + format);

        // Add a test case for the invariant for the proper format
        testCases.add(new SingleOutputTestCase(outputProducer, outputProducerArgs, goalOutput, goalLineNumber, format));

        try {
          if (!generateGoals) {
            currentLine = commands.readLine();
            // System.out.println("In goal section, currentLine = " + currentLine);
            goalOutput = parseGoal(currentLine);
          }
        }
        catch (IOException e) {
          throw new RuntimeException("Error in reading command file");
        }
      }
    }

    List<Object[]> samples = new Vector<Object[]>();

    // Get samples if they are needed to determine invariant data
    // e.g. to determine the exact nature of a linear relationship
    // between variables x and y we need two data points
    if (currentLine == null || !InvariantFormatTester.isWhitespace(currentLine)) {
      // System.out.println("On file line " + commands.getLineNumber());
      // System.out.println("Right before getSamples, currentLine = " + currentLine);
      // System.out.println("Right before getSamples, goalOutput = " + goalOutput);

      // If generating goals, goalOutput will have the proper first line
      // Otherwise, currentLine will have the proper first sample line
      if (generateGoals)
        getSamples(types, (BufferedReader)commands, samples, generateGoals, goalOutput);
      else
        getSamples(types, (BufferedReader)commands, samples, generateGoals, currentLine);
    }

    // Use the add_modified function of the appropriate invariant to
    // add the data to the instance
    populateWithSamples(invariantToTest, samples);

    return new FormatTestCase(testCases, invariantToTest);
  }

  /**
   * This function creates an array of VarInfo objects that can
   * represent a set of program language types provided by the
   * caller. Their names carry no meaning except for the type.
   *
   * @param classToTest the invariant class for which the VarInfos
   *        must be determined
   * @param types the types that the VarInfos must have
   * @return an array of VarInfo objects that have the types corresponding
   *         to those in types
   **/
  private static VarInfo [] getVarInfos(Class<? extends Invariant> classToTest, ProglangType[] types) {
    int numInfos = getArity(classToTest);

    if (numInfos == -1)
      throw new RuntimeException("Class arity cannot be determined.");

    VarInfo[] result = new VarInfo [numInfos];

    for (int i=0; i<numInfos; i++) {
      result[i] = getVarInfo(types[i], i);
    }

    return result;
  }

  /**
   * This function determines the arity of a given invariant given its class.
   *
   * @param classToTest the invariant type in question
   * @return the arity of the invariant if it can be determined, -1 otherwise
   **/
  private static int getArity(Class classToTest) {
    if (UnaryInvariant.class.isAssignableFrom(classToTest))
      return 1;
    else if (BinaryInvariant.class.isAssignableFrom(classToTest))
      return 2;
    if (ThreeScalar.class.isAssignableFrom(classToTest))
      return 3;

    return -1;
  }

  /**
   * This function returns a VarInfo of the given type. The name is
   * the ith letter of the alphabet. (Produces variables such that i=0
   * -> name=a, i=1 -> name=b, ...)
   *
   * @param type the desired type that the VarInfo will represent
   * @param i a unique identifier that determines the name to be used
   * @return a VarInfo object that described the type
   **/
  private static VarInfo getVarInfo(ProglangType type, int i) {
    Assert.assertTrue(type != null,"Unexpected null variable type passed to getVarInfo");

    String arrayModifier = "";

    if (type == ProglangType.INT_ARRAY ||
        type == ProglangType.DOUBLE_ARRAY ||
        type == ProglangType.STRING_ARRAY) { // Is it an array ?
      arrayModifier = "[]";
    }

    // Create the new VarInfoName dependent on a couple factors:
    // - If it is an array, attach [] to the name to make parse return
    // the correct thing
    // - The base part of the name will be "a" for the first var in an
    // invariant, "b" for the second, and so on
    // - The ProglangType will be specified in the parameters
    // - The comparability will be none
    VarInfo result;
    String base_name = new String(new char [] {(char)('a' + i)});
    String name = base_name + arrayModifier;
    if (FileIO.new_decl_format) {
      if (arrayModifier != "") { // interned
        FileIO.VarDefinition vardef
          = new FileIO.VarDefinition (base_name, VarInfo.VarKind.VARIABLE,
                                      type.elementType());
        VarInfo hashcode = new VarInfo (vardef);
        vardef = new FileIO.VarDefinition (base_name + "[..]",
                                           VarInfo.VarKind.ARRAY, type);
        vardef.arr_dims = 1;
        result = new VarInfo (vardef);
        result.enclosing_var = hashcode;
        assert result.enclosing_var.enclosing_var == null;
        // System.out.printf ("Created %s [%s]%n", result, hashcode);
      } else {
        FileIO.VarDefinition vardef
          = new FileIO.VarDefinition (name, VarInfo.VarKind.VARIABLE, type);
        result = new VarInfo (vardef);
        assert result.enclosing_var == null;
        // System.out.printf ("Created %s%n", result);
      }
    } else {
      result = new VarInfo (name, type, type,
                            VarComparabilityNone.it, VarInfoAux.getDefault());
    }
    return result;
  }

  /**
   * This function parses a format string -- a space separated list of
   * types -- and determines the types of objects to be collected.
   *
   * @param typeNames the type string for an invariant
   * @return an array of ProglangTypes representing the data in typeNames
   **/
  private static ProglangType [] getTypes(String typeNames) {
    StringTokenizer stok = new StringTokenizer(typeNames);
    ProglangType[] result = new ProglangType [stok.countTokens()];

    for (int i=0; i<result.length; i++) {
      String typeName = stok.nextToken();

      // A way of doing the same thing as below in fewer lines of code
      // Doesn't seem to work...
      // result[i] = ProglangType.parse(typeName);

      if (typeName.equalsIgnoreCase("int"))
        result[i] = ProglangType.INT;
      else if (typeName.equalsIgnoreCase("double"))
        result[i] = ProglangType.DOUBLE;
      else if (typeName.equalsIgnoreCase("string"))
        result[i] = ProglangType.STRING;
      else if (typeName.equalsIgnoreCase("int_array"))
        result[i] = ProglangType.INT_ARRAY;
      else if (typeName.equalsIgnoreCase("double_array"))
        result[i] = ProglangType.DOUBLE_ARRAY;
      else if (typeName.equalsIgnoreCase("string_array"))
        result[i] = ProglangType.STRING_ARRAY;
      else
        return null;

      Assert.assertTrue(result[i] != null,"ProglangType unexpectedly parsed to null in getTypes(String)");
    }

    return result;
  }


  private static void getSamples(ProglangType[] types, BufferedReader commands, List<Object[]> samples, boolean generateGoals, String firstLine) {
    String currentLine = (firstLine == null ? InvariantFormatTester.COMMENT_STARTER_STRING :
                                       firstLine);

    // System.out.println("firstLine in getSamples: " + firstLine);
    // System.out.println("currentLine line in getSamples: " + currentLine);

    try {
      // Read until end of file or separator (whitespace line) encountered
      while (currentLine != null && !InvariantFormatTester.isWhitespace(currentLine)) {
        // Skip over goal lines and comments
        while (InvariantFormatTester.isComment(currentLine) ||
               (generateGoals && parseGoal(currentLine) != null)) {
          currentLine = commands.readLine();
          // System.out.println("In getSamples early part, currentLine = " + currentLine);
        }
        // System.out.println("Current line: " + currentLine);

        // if (!InvariantFormatTester.isComment(currentLine) &&
        //    !InvariantFormatTester.isWhitespace(currentLine)) {

        // if current line is not whitespace then we have a valid line (that is, end hasn't been reached
        if (!InvariantFormatTester.isWhitespace(currentLine)) {
          // System.out.println(InvariantFormatTester.isComment(currentLine));
          Object[] sample = new Object [types.length];
          for (int i=0; i<types.length; i++) {
            // Parse each line according to a type in the paramTypes array
            // System.out.println("in getSamples right before parse, currentLine = \"" + currentLine + "\"");
            sample[i] = types[i].parse_value(currentLine);
            currentLine = commands.readLine();
          }
          samples.add(sample);
          // System.out.println("Debug: current sample: sample[" + i + "] == " + sample[i]);
        }
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e.toString());
    }
  }

  //    /**
  //     * This function parses a string or a section of a string into an
  //     * appropriate data type as indicated by the type class. The result
  //     * is stored into sample[sampleIndex]
  //     *
  //     * @param type the type of the object to create
  //     * @param sampleIndex the index in the sample array in which the result is to be stored
  //     * @param sample the array in which the result is to be stored
  //     * @param toBeParsed the String to be parsed for the result
  //     */
  //    public static void parse(Class type, int sampleIndex, Object[] sample, String toBeParsed) {
  //      Method parser;
  //      String typeName = type.getName();
  //      String arrayFunctionName;
  //      Method arrayFunction;

  //      // System.out.println("Parse called");

  //      // System.out.println(type.getName());
  //      // System.out.println(sampleIndex);
  //      // System.out.println(sample.length);
  //      // System.out.println(toBeParsed);

  //      try {
  //        if (type.isPrimitive()) { // Primitive types
  //          Class wrapper = getWrapperClass(type);

  //          if (wrapper != null) {
  //            // Use the valueOf function in the wrapper of the primitive
  //            // type to create the result
  //            parser = wrapper.getMethod("valueOf",new Class [] {String.class});

  //            // Use the Array.set to set the appropriate spot in the
  //            // array to the result of the parser method

  //            arrayFunction =
  //              Array.class.getMethod("set", new Class [] {Object.class, int.class, Object.class});

  //            Object arrayFunctionParams[] = new Object [3];
  //            arrayFunctionParams[0] = (Object)sample;
  //            arrayFunctionParams[1] = new Integer(sampleIndex);

  //            // Debug code

  //            // System.out.println("**********" + lineSep + toBeParsed + lineSep +
  //            // arrayFunctionParams[1] + lineSep + "**********" + lineSep);

  //            try {
  //              // Get the result
  //              arrayFunctionParams[2] = parser.invoke(null, new Object [] {toBeParsed});
  //            }
  //            catch (Exception e) {
  //              throw new RuntimeException("Error in invoking parser for primitive object");
  //            }

  //            try {
  //              // Put the result into the array
  //              arrayFunction.invoke(null,arrayFunctionParams);
  //            }
  //            catch (Exception e) {
  //              throw new RuntimeException("Error in invoking arrayFunction to put result in sample");
  //            }
  //          }
  //          else
  //            throw new RuntimeException("Could not find wrapper class for primitive");
  //        } else if (type.isArray()) { // Array type - only parses single dimensional now
  //          Class subType = type.getComponentType();

  //          StringTokenizer stok = new StringTokenizer(toBeParsed);
  //          int arrayLength = stok.countTokens();
  //          Object temp[] = new Object [arrayLength];

  //          // Recursively call parse on the substrings to get the array
  //          // entries
  //          for (int i=0; i<arrayLength; i++) {
  //            parse(subType, i, temp, stok.nextToken());
  //          }

  //          Object result = Array.newInstance(subType, arrayLength);

  //          // If primitive subtype, must use appropriate set function to
  //          // get entries into the result array
  //          if (subType.isPrimitive()) {
  //            String subTypeName = subType.getName();
  //            // Capitalize the first letter pf subTypeName
  //            String capsSubTypeName =
  //              new String(new char [] {Character.toUpperCase(subTypeName.charAt(0))}) +
  //              subTypeName.substring(1, subTypeName.length());

  //            arrayFunctionName = "set" + capsSubTypeName;
  //            arrayFunction = Array.class.getMethod(arrayFunctionName, new Class [] {Object.class, int.class, subType});

  //            for (int i=0; i<arrayLength; i++) {
  //              arrayFunction.invoke(null, new Object [] {result, new Integer(i), temp[i]});
  //            }
  //          } else {
  //            for (int i=0; i<arrayLength; i++) {
  //              Array.set(result, i, temp[i]);
  //            }
  //          }

  //          sample[sampleIndex] = result;

  //        } else { // Non-array (non-primitive) objects
  //          try {
  //            if (type != String.class) {
  //              parser = type.getMethod("valueOf", new Class [] {String.class});
  //              sample[sampleIndex] = parser.invoke(null, new Object [] {toBeParsed});
  //            }
  //            else
  //              sample[sampleIndex] = toBeParsed;
  //          }
  //          catch (Exception e) {
  //            throw new RuntimeException("Error in invoking parser on complex type" +
  //                                       " - no way to create one from a String");
  //          }
  //        }
  //      }
  //      catch (IllegalAccessException e) {
  //        throw new RuntimeException(e.toString());
  //      }
  //      catch (InvocationTargetException e) {
  //        throw new RuntimeException(e.toString());
  //      }
  //      catch (NoSuchMethodException e) {
  //        throw new RuntimeException(e.toString());
  //      }
  //      catch (SecurityException e) {
  //        throw new RuntimeException("SecurityException generated that indicates" +
  //                                   " reflection has been disallowed");
  //      }
  //    }

  //    /**
  //     * This function generates the wrapper class for a primitive class
  //     *
  //     * @param type the type for which the wrapper class is to be generated
  //     * @return the corresponding wrapper class if type is a primitive type,
  //     *         null otherwise
  //     */
  //    private static Class getWrapperClass(Class type) {
  //      if (type.equals(int.class))
  //        return Integer.class;
  //      else if (type.equals(long.class))
  //        return Long.class;
  //      else if (type.equals(double.class))
  //        return Double.class;
  //      else if (type.equals(float.class))
  //        return Float.class;
  //      else if (type.equals(boolean.class))
  //        return Boolean.class;
  //      else if (type.equals(byte.class))
  //        return Byte.class;
  //      else if (type.equals(char.class))
  //        return Character.class;
  //      else if (type.equals(short.class))
  //        return Short.class;
  //      return null;
  //    }

  /**
   * This function adds the samples in the samples list to the passed
   * in invariant by use of the appropriate add_modified function
   * (determined by reflection).
   *
   * @param inv an invariant to which samples are added
   * @param samples a list of samples (each entry of type Object []) that
   *        can be added to the variables involved
   **/
  private static void populateWithSamples(Invariant inv, List<Object[]> samples) {
    if (samples == null || samples.size() == 0) return;

    Assert.assertTrue (inv != null);

    // System.out.println(inv.getClass().getName());
    // System.out.println(samples.size());

    Method addModified = getAddModified(inv.getClass());
    int sampleSize = samples.get(0).length;
    Class currentClass;

    // System.out.println(sampleSize);

    for (int i=0; i<samples.size(); i++) {

      // Last slot is for "count" parameter
      Object[] params = new Object [sampleSize+1];
      Object[] currentSample = samples.get(i);

      for (int j=0; j<sampleSize; j++) {
        currentClass = currentSample[j].getClass();

        // Intern all objects that can be interned because some equality
        // functions will not work correctly unless the objects are
        // interned
        if (currentClass.equals(String.class)) {
          // Intern strings
          currentSample[j] = Intern.intern(currentSample[j]);
        } else if (currentClass.isArray()) {
          // Intern arrays
          if (currentClass.getComponentType().equals(String.class)) {
            for (int k=0; k<((String [])(currentSample[j])).length; k++) {
              // Intern Strings that are inside arrays
              ((String [])currentSample[j])[k] = Intern.intern(((String [])currentSample[j])[k]);
            }
          }
          currentSample[j] = Intern.intern(currentSample[j]);
        }

        params[j] = currentSample[j];
      }

      // Set count to 1
      params[sampleSize] = new Integer(1);

      // Debug code

      //        System.out.println("Sample #" + (i+1) + " of " + samples.size());
      //        System.out.println("P0: " + params[0] + lineSep + "P1: " + params[1]);
      //        System.out.println("P0 is array: " + params[0].getClass().isArray() + " type: " + params[0].getClass().getComponentType());
      //        System.out.println("P1 is array: " + params[1].getClass().isArray() + " type: " + params[1].getClass().getComponentType());

      //        for (int y=0; y<sampleSize; y++) {
      //          try {
      //            if (params[y].getClass().isArray()) {
      //              System.out.print("P" + y + " array representation: ");
      //              for (int x=0; ; x++) {
      //                System.out.print(Array.get(params[y],x) + " ");
      //              }
      //            }
      //          }
      //          catch (ArrayIndexOutOfBoundsException e) {
      //            System.out.println();
      //          }
      //        }

      try {
        addModified.invoke(inv,params);
      }
      //        catch (Exception e) {
      //        throw new RuntimeException(e.toString());
      //        }
      catch (InvocationTargetException e) {
        StringWriter target_backtrace = new StringWriter();
        e.getTargetException().printStackTrace(new PrintWriter(target_backtrace));
        throw new RuntimeException("Error in populating invariant with add_modified (" + addModified.toString() + "applied to " + utilMDE.ArraysMDE.toString(params) + "):" + lineSep + "START TARGETEXCEPTION=" + lineSep + target_backtrace.toString() + lineSep + "END TARGETEXCEPTION" + lineSep + e.toString());
      } catch (Exception e) {
        throw new RuntimeException("Error in populating invariant with add_modified (" + addModified.toString() + "applied to " + utilMDE.ArraysMDE.toString(params) + "): " + e.toString());
      }
    }
  }

  /**
   * This function returns the add_modified method from the class type provided.
   *
   * @param theClass the class in which to find the add_modified method
   * @return the add_modified method if it exists, null otherwise
   */
  private static Method getAddModified(Class<? extends Invariant> theClass) {
    Method[] methods = theClass.getMethods();

    Method currentMethod;
    for (int i=0; i<methods.length; i++) {
      currentMethod = methods[i];
      if (currentMethod.getName().lastIndexOf("add_modified") != -1) { // Method should be called add_modified
        return currentMethod;
      }
    }
    return null;
  }

  /**
   * This function creates an appropriate PptSlice for a given set of
   * VarInfos and a PptTopLevel.
   *
   * @param vars an array of VarInfo objects for which the slice is
   *        to be created
   * @param ppt the PptTopLevel object representing the program point
   * @return a new PptSlice object if the creation of one is possible,
   *         else throws a RuntimeException
   */
  private static PptSlice createSlice(VarInfo[] vars, PptTopLevel ppt) {
    if (vars.length == 1)
      return new PptSlice1(ppt, vars);
    else if (vars.length == 2)
      return new PptSlice2(ppt, vars);
    else if (vars.length == 3)
      return new PptSlice3(ppt, vars);
    else
      throw new RuntimeException("Improper vars passed to createSlice (length = " + vars.length + ")");
  }

  /**
   * This function instantiates an invariant class by using the
   * <type>(PptSlice) constructor.
   *
   * @param theClass the invariant class to be instantiated
   * @param sl the PptSlice representing the variables about
   *        which an invariant is determined
   * @return an instance of the class in theClass if one can be constructed,
   *         else throw a RuntimeException
   */
  private static Invariant instantiateClass(Class<? extends Invariant> theClass, PptSlice sl) {
    try {
      Method get_proto = theClass.getMethod ("get_proto", new Class[] {});
      Invariant proto = (Invariant) get_proto.invoke (null, new Object[] {});
      Invariant inv = proto.instantiate (sl);

      if (inv == null)
        throw new RuntimeException ("null inv for " + theClass.getName());
      return (inv);
    }
    catch (Exception e) {
      e.printStackTrace(System.out);
      throw new RuntimeException("Error while instantiating invariant "
                                 + theClass.getName() + ": " + e.toString());
    }
  }

  /**
   * This function instantiates an invariant class by using the
   * static instantiate method with the specified arguments.
   *
   * @param theClass  - the invariant class to be instantiated
   * @param arg_types - the types of each argument
   * @param arg_vals  - the value of each argument
   *
   * @return an instance of the class in theClass if one can be constructed,
   *         else throw a RuntimeException
   */
  private static Invariant instantiateClass(Class<? extends Invariant> theClass, PptSlice slice,
                                         Class[] arg_types, Object[] arg_vals) {
    try {
      // Fmt.pf ("creating " + theClass);
      // for (int i = 0; i < arg_types.length; i++)
      //   Fmt.pf ("  arg %s = %s", arg_types[i], arg_vals[i]);
      Method get_proto = theClass.getMethod ("get_proto", arg_types);
      Invariant proto = (Invariant) get_proto.invoke (null, arg_vals);

      return (proto.instantiate (slice));
    }
    catch (Exception e) {
      e.printStackTrace(System.out);
      throw new RuntimeException("Error while instantiating invariant "
                                 + theClass.getName() + ": " + e.toString());
    }
  }

}
