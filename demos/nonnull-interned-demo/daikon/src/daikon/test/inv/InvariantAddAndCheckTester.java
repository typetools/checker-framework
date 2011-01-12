package daikon.test.inv;

import daikon.*;

import daikon.config.Configuration;

import daikon.inv.*;
import daikon.inv.ternary.threeScalar.ThreeScalar;
import daikon.inv.binary.*;
import daikon.inv.unary.*;
import daikon.inv.unary.sequence.CommonSequence;

import java.io.*;
import java.lang.reflect.*;

import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import junit.framework.*;

import utilMDE.Assert;
import utilMDE.Fmt;

/**
 * This is a tester for the results of adding or checking an sample
 * to an invariant.  It can test practically
 * any invariant in the Daikon system given the appropriate commands.
 * The test are configured from the InvariantTest.commands file
 * and errors that occur are written to the InvariantTest.diffs
 * file. For conveince a partcailly complete file InvariantTest.input
 * can be used to generate a complete commands file.  To generate
 * InvariantTest.commands from InvariantTest.input run this class's
 * main method with option "--generate_goals".
 *
 * Each test case
 * starts with a line containing the full name of the invariant
 * then a second line containing the types of the arguments to
 * the invariant's check and add method (excluding the count
 * argument). After the test case's header any number of command
 * lines may be included in a command file.  Finally, after all
 * command lines, the key work "end" must appear on its own line.
 *
 * Each command line is starts with
 * a command: "add:" or "check:". Following the command
 * comes the arguments to be checked or added to the invariant.
 * These arguments should be in the same format as in a dtrace
 * file.  Next comes the the InvariantStatus that is expected to
 * be returned by the check or add command on checking or adding
 * the arguments.  Finally, the expected format of the Invariant
 * after checking or adding the arguments
 * is included.  (The format of the invariant is given by
 * "Invariant.format_using(OutputFormat.DAIKON)")
 *
 * A full example test case is as follows:
 *
 * daikon.inv.binary.twoSequence.PairwiseIntEqual
 * int_array int_array
 * add: [ 1 2 3 ]; [1 2 3 ]; no_change; a[] == b[] (elementwise)
 * add: [ 10 -1 6 ]; [10 -1 6 ]; no_change; a[] == b[] (elementwise)
 * add: [ -3 -3 -9 ]; [-3 6 -9 ]; falsified; a[] == b[] (elementwise)
 * end
 *
 *
 * Alternatively, this class can be used to generate a test case
 * when given command lines that do not include expected
 * InvariantStatus or invariant format.  The proper input to
 * generate the test above would be:
 *
 * daikon.inv.binary.twoSequence.PairwiseIntEqual
 * int_array int_array
 * add: [ 1 2 3 ]; [1 2 3 ]
 * add: [ 10 -1 6 ]; [10 -1 6 ]
 * add: [ -3 -3 -9 ]; [-3 6 -9 ]
 * end
 *
 *
 * To run a test case the method runTest should be used.  To generate
 * a test case the method generateTest should be used.
 *
 * Note: no test case should contain the character ';' in any way
 * other than to divide arguments with in a command line.
 **/
public class InvariantAddAndCheckTester extends TestCase {

  /**
   * Maximum file size that can currently be examined by the program.
   * It is arbitrary, but a length must be supplied to
   * LineNumberReader.mark().
   */
  private static final int MAX_FILE_SIZE = 262144;

  /**
   * Indicates a string that when it starts a line signifies that the
   * line is a comment.
   **/
  public static final String COMMENT_STARTER_STRING = "#";

  /**
   * A list containing all of the test formats.
   **/
  public static final List<String> TEST_FORMAT_LIST = getTestFormatList();

  /**
   * Allows for the configuring of Daikon options.
   **/
  static Configuration config = Configuration.getInstance();

  private static final String inputFileName = "daikon/test/inv/InvariantTest.input";
  private static final String commandsFileName = "daikon/test/inv/InvariantTest.commands";
  private static final String diffFileName = "daikon/test/inv/InvariantTest.diffs";

  private static final String lineSep = Global.lineSep;


  /**
   * This function allows this test to be run from the command line
   * instead of its usual method, which is through the Daikon
   * MasterTester.
   *
   * @param args arguments to the main function, which control options
   *        to the program. As of now there is only one option,
   *        "--generate_goals", which will generate goal information for
   *        the selected tests assuming the output that the tests provide
   *        is the correct output
   **/
  public static void main(String[] args) {
    daikon.LogHelper.setupLogs (daikon.LogHelper.INFO);
    if (args.length == 1 && args[0].equalsIgnoreCase("--generate_goals")) {
      writeCommandFile();
    } else if (args.length > 0) {
      throw new Daikon.TerminationMessage("Usage: java daikon.test.InvariantAddAndCheckTester" +
                                   " [--generate_goals]");
    } else {
      junit.textui.TestRunner.run(new TestSuite(InvariantAddAndCheckTester.class));
    }
  }


  /**
   * This constructor allows the test to be created from the
   * MasterTester class.
   *
   * @param name the desired name of the test case
   **/
  public InvariantAddAndCheckTester(String name) {
    super(name);
  }

  /**
   * This function produces the format list for intialization of the
   * static format list variable.
   **/
  static List<String> getTestFormatList() {
    List<String> result = new Vector<String>();

    // Add test formats - hard coded in
    result.add("daikon");
    result.add("java");
    result.add("esc");
    result.add("ioa");
    result.add("jml");
    result.add("dbc");
    result.add("simplify");

    return result;
  }

  /**
   * This function is the actual function performed when this class is
   * run through JUnit.
   **/
  public static void testFormats() {

    // Don't care about comparability info because we are only
    // creating variables for the purpose of being compared (thus they
    // should all be comparable)
    Daikon.ignore_comparability = true;

    // run the actual test

    if (!execute()) {
      fail("At least one test failed." +
           " Inspect java/daikon/test/InvariantAddAndCheckTest.diffs for error report.");
    }
  }

  /**
   * Returns the next non-comment, non-whitespace line of the input buffer.
   *
   * @param input the input buffer
   * @return the next non-comment, non-whitespace line of the input buffer or
   *         null if the end of the buffer is reached before such a line can be found
   **/
  static String getNextRealLine(BufferedReader input) {
    String currentLine = "";

    try {
      while (currentLine != null) {
        currentLine = input.readLine();
        if (currentLine != null && !isComment(currentLine) && !isWhitespace(currentLine))
          return currentLine;
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e.toString());
    }
    return null;
  }

  /**
   * This function performs the testing for a particular format
   * indicated by the format string. It subsequently sets up
   * appropriate input and output streams for the format test,
   * performs the test, and the compares the test results to the
   * goals.  If the goals differ from the actual results the test
   * fails.
   * @return false if any tests fail.
   **/
  private static boolean execute() {
    LineNumberReader commandReader = getCommands();
    String output = performTest(commandReader);

    if (output == null) { // no errors
      return true;
    } else {
      FileWriter diffsOutput = getDiffsOutputWriter();
      try {
        diffsOutput.write(output, 0, output.length());
        diffsOutput.close();
      } catch (IOException e) {
        throw new RuntimeException("Could not output generated diffs");
      }
      return false;
    }
  }

  private static void writeCommandFile() {
    LineNumberReader inputReader = getInputReader();

    String output = generateCommands(inputReader);
    FileWriter commandOutput = getCommandWriter();
    try {
      commandOutput.write(output, 0, output.length());
      commandOutput.close();
    } catch(IOException e) {
      throw new RuntimeException("Could not output generated commands");
    }
    System.out.println("Goals generated");
  }

  /**
   * This function performs an individual formatting test after the
   * input and output streams have been created.
   *
   * @param commands the input that decides which tests to perform
   * @return a String holding the error messages for any failed tests
   *  or null if no tests are failed.
   **/
  private static String performTest(LineNumberReader commands) {
    StringBuffer output = new StringBuffer();
    //  List invariantTestCases = new Vector();
    boolean noTestFailed = true;

    while (true) {
      // Create a new test case
      //  FormatTestCase currentCase = FormatTestCase.instantiate(commands, generateGoals);

      //if (currentCase == null)
      //  break;
      //  else {
      //  invariantTestCases.add(currentCase);
      String results = AddAndCheckTestCase.runTest(commands);
      if (results == null) break;
      if (! (results.length() == 0)) {
        //  output.print(currentCase.getDiffString());
          output.append(results);
          noTestFailed = false;
      }
    }
    if (noTestFailed) {
      return null;
    } else {
      return output.toString();
    }
  }

  private static String generateCommands(LineNumberReader input) {
    StringBuffer output = new StringBuffer();

    while (true) {
      String commands = AddAndCheckTestCase.generateTest(input);
      if (commands == null) break;
      output.append(commands);
    }
    return output.toString();
  }

  private static LineNumberReader getInputReader() {

  // Calculate input file locations
//     URL inputFileLocation =
//       ClassLoader.getSystemClassLoader().getSystemResource("InvariantTest.commands");
//     if (inputFileLocation == null)
//       fail("Input file for invariant format tests missing." +
//            " (Should be in InvariantTest.commands" +
//            " and it must be within the classpath)");

    //  String inputFile = inputFileLocation.getFile();
    LineNumberReader input = null;
    try {
      input = new LineNumberReader
        (new InputStreamReader
         (new FileInputStream(inputFileName)));
    }
    catch (FileNotFoundException e) {
      fail("Unexpected FileNotFoundException (very strange since the URL" +
           " of the file was found earlier)");
    }
    return input;
  }

  private static FileWriter getCommandWriter() {

    try {
      return new FileWriter(commandsFileName);
    }
    catch (IOException e) {
      throw new RuntimeException("Cannot write output into " + commandsFileName);
    }
  }

  private static LineNumberReader getCommands() {
    // Calculate input file locations
    //   URL inputFileLocation =
    //      ClassLoader.getSystemClassLoader().getSystemResource("InvariantTest.commands");

//     if (inputFileLocation == null)
//       fail("Input file for invariant format tests missing." +
//            " (Should be in InvariantTest.commands" +
//            " and it must be within the classpath)");

    //  String inputFile = inputFileLocation.getFile();
    //    System.out.println(System.getProperty("user.dir"));
    LineNumberReader commands = null;
    try {
      commands =  new LineNumberReader
      (new InputStreamReader
       (new FileInputStream(commandsFileName)));
    }
    catch (FileNotFoundException e) {
      fail("Unexpected FileNotFoundException (very strange since the URL" +
           " of the file was found earlier)");
    }
    return commands;
  }

  private static FileWriter getDiffsOutputWriter() {
    try {
        return new FileWriter(new File(diffFileName));
      }
      catch (IOException e) {
        throw new RuntimeException("Cannot write output into " + diffFileName);
      }
  }

  /**
   * Determines whether a line is a comment or not.
   *
   * @param line the line in question
   * @return true if the line is a comment (that is, not to be interpretted as a command)
   *         false otherwise
   **/
  static boolean isComment(String line) {
    return line.startsWith(COMMENT_STARTER_STRING);
  }

  /**
   * Determines whether a given line is made only of whitespcae.
   *
   * @param line the line in question
   * @return true if the line is made up only of whitespace, false otherwise
   **/
  static boolean isWhitespace(String line) {
    for (int x=0; x<line.length(); x++) {
      if (!Character.isWhitespace(line.charAt(x)))
        return false;
    }
    return true;
  }

  private static class AddAndCheckTestCase {

    /**
     * The Invariant object to be tested.
     **/
    private static Invariant invariantToTest;

    /**
     * The types of the arguments to invariantToTest's
     * check and add commands (not including the count argument).
     */
    private static ProglangType[] types;

    /**
     * invariantToTest's addModified method.
     */
    private static Method addModified;

    /**
     * invariantToTest's checkModified method.
     */
    private static Method checkModified;

    /**
     * invariantToTest's format_using method.
     */
    private static Method outputProducer;

    /**
     * Contains error messages if any test commands fail.
     */
    private static StringBuffer results;

    /**
     * The token that divides the different arguments to a
     * test command.
     */
    private static final String argDivider = ";";

    /**
     * @return String containing error messages for any
     *  failed cases.  In the case that there are no failed
     *  cases, the empty string is returned.  In the case where
     *  commands is empty (there are no more test cases and the
     *  end of the file has been reached), null is returned.
     */
    public static String runTest(LineNumberReader commands) {
      boolean endOfFile = initFields(commands, false);
      if (endOfFile) {
        return null;
      }
      while (true) {
        String commandLine = getNextRealLine(commands);
        int lineNumber = commands.getLineNumber();
        if (InvariantAddAndCheckTester.isComment(commandLine)) {
          continue;
        } else if (isTestTerminator(commandLine)) {
          break;
        } else if (isAddCommand(commandLine) ||
                   isCheckCommand(commandLine)) {
          exicuteCheckOrAddCommand(commandLine, lineNumber);
        } else if (isCompareCommand(commandLine)) {
        } else {
          throw new RuntimeException("unrecognized command");
        }
      }
      return results.toString();
    }

    /**
     * @return a String containing the proper add and check
     *  commands for this input lines of this test case.
     */
    public static String generateTest(LineNumberReader commands) {
      boolean endOfFile = initFields(commands, true);
      if (endOfFile) return null;
      while (true) {
        String commandLine = getNextLine(commands).trim();
        int lineNumber = commands.getLineNumber();
        if (InvariantAddAndCheckTester.isComment(commandLine)) {
          results.append(commandLine + lineSep);
        } else if (isTestTerminator(commandLine)) {
          results.append(commandLine + lineSep + lineSep);
          break;
        } else if (isAddCommand(commandLine) ||
                   isCheckCommand(commandLine)) {
          generateCheckOrAddCommand(commandLine, lineNumber);
        } else if (isCompareCommand(commandLine)) {
          // generateCompareCommand(commandLine);
        } else {
          throw new RuntimeException("unrecognized command");
        }
      }
      return results.toString();
    }

    /**
     * Initializes the fields of this class based on the first two
     * lines of a case which include the class name and parameter
     * types.
     * @return true is end of file is reached.
     */
    private static boolean initFields(LineNumberReader commands, boolean generatingCommands) {

      results = new StringBuffer();

      String className = getNextRealLine(commands);

      // End of file reached
      if (className == null) return true;

      // Load the class from file
      @SuppressWarnings("unchecked")
      Class<? extends Invariant> classToTest = (Class<? extends Invariant>) getClass(className);

      try {
        classToTest.getField("dkconfig_enabled"); // Enable if needs to be done
        InvariantAddAndCheckTester.config.apply(className + ".enabled", "true");
      }
      catch (NoSuchFieldException e) { // Otherwise do nothing
      }

      if (generatingCommands) {
        results.append(className + lineSep);
      }

      // Instantiate variables to be used as the names in the
      // invariants, variables are labeled a,b,c and so on as they
      // appear
      String typeString = getNextRealLine( commands);

      types = getTypes(typeString);

      VarInfo[] vars = getVarInfos(classToTest, types);
      PptSlice sl = createSlice(vars, daikon.test.Common.makePptTopLevel("Test:::OBJECT", vars));


      // Create an actual instance of the class
      invariantToTest = instantiateClass(classToTest, sl);

      addModified = getAddModified(invariantToTest.getClass());
      checkModified = getCheckModified(invariantToTest.getClass());
      outputProducer = getOutputProducer(invariantToTest.getClass());

      Assert.assertTrue(getArity(invariantToTest.getClass()) == types.length);

      if (generatingCommands) {
        results.append(typeString + lineSep);
      }
      return false;
    }


    /**
     * Given a line from a command file, generates executes the
     * appropriate check or add command and checks the results against
     * the goal.  If the results and goal do not match, a message is
     * added to the results string buffer.
     */
    private static void exicuteCheckOrAddCommand(String command, int lineNumber) {

      // remove the command
      String args = command.substring(command.indexOf(":") + 1);

      StringTokenizer tokens = new StringTokenizer(args, argDivider);
      if (tokens.countTokens() != types.length + 2) {
        throw new RuntimeException("Number of arguments to add command on line "
                                   + lineNumber + " is: "
                                   + tokens.countTokens() +
                                   " but should be: " + (types.length + 2));
      }
      Object[] params = getParams(tokens);
      InvariantStatus goalStatus = parseStatus(tokens.nextToken().trim());
      tokens.nextToken();       // executed for side effect
      Assert.assertTrue(! tokens.hasMoreTokens());
      InvariantStatus resultStatus = null;
      if (isCheckCommand(command)) {
        resultStatus = getCheckStatus(params);
      } else {
        resultStatus = getAddStatus(params);
      }
      if (resultStatus != goalStatus) {
        results.append("Error on line " + lineNumber + ":" + lineSep +
                       "Expected  InvariantStatus: " + goalStatus + lineSep +
                       "Found InvariantStatus: " + resultStatus + lineSep);
      }
    }

    /**
     * Given a line from an input file, generates appropriate
     * check or add command.
     */
    private static void generateCheckOrAddCommand(String command, int lineNumber) {
      // remove the command
      String args = command.substring(command.indexOf(":") + 1);

      StringTokenizer tokens = new StringTokenizer(args, argDivider);
      if (tokens.countTokens() != types.length) {
        throw new RuntimeException("Number of arguments to generate an add command on line: "
                                   + lineNumber + " is: "
                                   + tokens.countTokens() +
                                   " but should be: " + types.length);
      }
      Object[] params = getParams(tokens);
      Assert.assertTrue(! tokens.hasMoreTokens());
      InvariantStatus goalStatus = null;
      if (isCheckCommand(command)) {
        goalStatus = getCheckStatus(params);
      } else {
        goalStatus = getAddStatus(params);
      }
      String invariantFormat = getInvariantFormat();
      results.append(command + argDivider + " " + goalStatus.toString() + argDivider + " " + invariantFormat + lineSep);
    }

    /**
     * @return an array of the arguments to be passed into
     *  check_modified or add_modified produced from tokens.
     */
    private static Object[] getParams(StringTokenizer tokens) {
      // add one for the "count" argument
      Object[] params = new Object[types.length + 1];
      for (int i = 0; i < types.length; i++) {
        params[i] = types[i].parse_value(tokens.nextToken().trim());
      }
      params[params.length - 1] = new Integer(1); // the "count" argument
      return params;
    }

    /**
     * @return the InvariantStatus produced by invoking invariantToTest's
     *  add_modified method on the arguments represented by params.
     */
    private static InvariantStatus getAddStatus(Object[] params) {
      try {
        return (InvariantStatus) addModified.invoke(invariantToTest, params);
      } catch (Exception e) {
        throw new RuntimeException (" error in " + invariantToTest.getClass()
                                    + ": " + e);
      }
    }

    /**
     * @return the InvariantStatus produced by invoking invariantToTest's
     *  check_modified method on the arguments represented by params.
     */
    private static InvariantStatus getCheckStatus(Object[] params) {
      try {
        return (InvariantStatus) checkModified.invoke(invariantToTest, params);
      } catch (Exception e) {
        throw new RuntimeException (" error in " + invariantToTest.getClass()
                                    + ": " + e);
      }
    }

    /**
     * @return a String representation of the invariantToTest.
     *  This String is produced by invoking the invariant's
     *  format_using with method with the argument
     *  OutputFormat.Daikon.
     */
    private static String getInvariantFormat() {
      try {
        return (String) outputProducer.invoke(invariantToTest,
                                              new Object[]{ OutputFormat.DAIKON});
      } catch (Exception e) {
        throw new RuntimeException(invariantToTest + " " + outputProducer);
      }
    }

    /**
     * @return an InvariantStatus that the string status parses to.
     */
    private static InvariantStatus parseStatus(String status) {
      status = status.trim();
      if (status.equals("no_change")) {
        return InvariantStatus.NO_CHANGE;
      } else  if (status.equals("falsified")) {
        return InvariantStatus.FALSIFIED;
      }  if (status.equals("weakened")) {
        return InvariantStatus.WEAKENED;
      } else {
        throw new RuntimeException("Unrecognized InvariantStatus: " + status);
      }
    }

    /**
     * This function returns the add_modified method from the class type provided.
     *
     * @param theClass the class in which to find the add_modified method
     * @return the add_modified method if it exists, null otherwise
     * @throws RuntimeException if check_modified does not exist.
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
      throw new RuntimeException("Cannot find add_modified method");
    }

    /**
     * This function returns the check_modified method from the class type provided.
     *
     * @param theClass the class in which to find the check_modified method
     * @return the check_modified method if it exists
     * @throws RuntimeException if check_modified does not exist.
     */
    private static Method getCheckModified(Class<? extends Invariant> theClass) {
      Method[] methods = theClass.getMethods();

      Method currentMethod;
      for (int i=0; i<methods.length; i++) {
        currentMethod = methods[i];
        if (currentMethod.getName().lastIndexOf("check_modified") != -1) { // Method should be called check_modified
          return currentMethod;
        }
      }
      throw new RuntimeException("Cannot find check_modified method");
    }

    /**
     * @return the method of invariant named by theClass
     *  that produces a String representation of the invariant.
     */
    private static Method getOutputProducer(Class<? extends Invariant> theClass) {
      Method[] methods = theClass.getMethods();

      Method currentMethod;
      for (int i=0; i<methods.length; i++) {
        currentMethod = methods[i];

        // Method should be called format_using
        if (currentMethod.getName().lastIndexOf("format_using") != -1) {
          return currentMethod;
        }
      }
      throw new RuntimeException("Cannot find format_using method");
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
     * This function is an alias for the {@link #getNextRealLine(BufferedReader) getNextRealLine}
     * method.
     **/
    static String getNextRealLine(BufferedReader buffer) {
      return InvariantAddAndCheckTester.getNextRealLine(buffer);
    }

    private static boolean isTestTerminator(String command) {
      String commandTrimmed = command.trim();
      return commandTrimmed.startsWith("end");
    }

    private static boolean isAddCommand(String command) {
      String commandTrimmed = command.trim();
      return commandTrimmed.startsWith("add");
    }

    private static boolean isCheckCommand(String command) {
      String commandTrimmed = command.trim();
      return commandTrimmed.startsWith("check");
    }

    private static boolean isCompareCommand(String command) {
      String commandTrimmed = command.trim();
      return commandTrimmed.startsWith("compare");
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
    private static VarInfo [] getVarInfos(Class classToTest, ProglangType[] types) {
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
      VarInfo result = new VarInfo (new String(new char [] {(char)('a' + i)}) +
                                    arrayModifier, type, type,
                                    VarComparabilityNone.it, VarInfoAux.getDefault());
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
      if (vars.length == 1) {
        Assert.assertTrue(vars[0] != null);
        return new PptSlice1(ppt, vars);
      } else if (vars.length == 2) {
        Assert.assertTrue(vars[0] != null);
        Assert.assertTrue(vars[1] != null);
        return new PptSlice2(ppt, vars);
      } else if (vars.length == 3) {
        Assert.assertTrue(vars[0] != null);
        Assert.assertTrue(vars[1] != null);
        Assert.assertTrue(vars[2] != null);
        return new PptSlice3(ppt, vars);
      } else {
        throw new RuntimeException("Improper vars passed to createSlice (length = "
                                   + vars.length + ")");
      }
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
        return (inv);
      }
      catch (Exception e) {
        e.printStackTrace(System.out);
        throw new RuntimeException("Error while instantiating invariant "
                                   + theClass.getName() + ": " + e.toString());
      }
    }

    private static String getNextLine(LineNumberReader input) {
      try {
        return input.readLine();
      } catch (IOException e) {
        throw new RuntimeException("Exception reading next line");
      }
    }

  }

}
