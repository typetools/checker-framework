package daikon.test.split;

import daikon.split.*;
import java.util.*;
import java.io.*;
import utilMDE.*;
import daikon.*;

/**
 * This class's main method can be used to update both the
 * target files of SplitterFactoryTest and the code of the
 * SplitterFactoryTest itself.
 *
 * To use this program to update SplitterFactoryTest
 * and the target files, run
 * <pre>    rm $inv/java/daikon/test/split/targets/*.java.goal
 *      rm $inv/java/daikon/test/split/SplitterFactoryTest.java</pre>
 * Then simply run the main method with out any arguments
 * in the $INV/java directory. After running the
 * main method one should re-compile the SplitterFactoryTest.
 *
 * To add additional tests to this test program, place the .spinfo
 * and decls files into the "targets" directory then add a call to
 * generateSplitters with the new files.  generateSplitters is
 * overloaded; therefore if there are only one .spinfo file and
 * only decls file then only the names of those two files need to be
 * used as arguments to generateSplitters.  However, if there are
 * multiple .spinfo files or multiple decls files, the file names
 * should be placed into Lists then passed to generateSplitters.
 * See generateSplitters for more information.
 */
public class SplitterFactoryTestUpdater {
  public static java.lang.Runtime commander = java.lang.Runtime.getRuntime();
  private static String targetDir = "daikon/test/split/targets/";
  private static String splitDir = "daikon/test/split/";

  private static ArrayList<ArrayList<File>> spinfoFileLists = new ArrayList<ArrayList<File>>();
  private static ArrayList<ArrayList<File>> declsFileLists = new ArrayList<ArrayList<File>>();
  private static ArrayList<String> classNames = new ArrayList<String>();

  private static File tempDir = null;

  private SplitterFactoryTestUpdater() {} //blocks public constructor

  /**
   * If one has changed the test cases used below, for best results run
   * "rm *.java.goal" while in the targets directory before running this
   * method. Creates new splitter java files, moves the new files into
   * target directory, rewrites the code of SplitterFactoryTest
   * to use the new files.  One should recompile SplitterFactoryTest
   * after running this method.
   * @param args are ignored.
   */
  public static void  main(String[] args) {
    generateSplitters("StreetNumberSet.spinfo", "StreetNumberSet.decls");
    generateSplitters("Fib.spinfo", "Fib.decls");
    generateSplitters("QueueAr.spinfo", "QueueAr.decls");
    generateSplitters("muldiv.spinfo", "BigFloat.decls");
    moveFiles();
    writeTestClass();
    // UtilMDE.deleteDir(tempDir); // file's delete requires a dir be empty
  }

  /**
   * This is a short-cut method if only one spinfo file and only
   * one decls files is to be used.  See generateSplitters(List, List).
   */
  private static void generateSplitters(String spinfoFile, String declsFile) {
    List<String> spinfo = new ArrayList<String>();
    spinfo.add(spinfoFile);
    List<String> decls = new ArrayList<String>();
    decls.add(declsFile);
    generateSplitters(spinfo, decls);
  }

  /**
   * Generates the splitter java files in the tempDir.
   * @param spinfos the spinfo files that should be used in generating
   *  the splitter java files.
   * @param decls the decls files that should be used in generating the
   *  splitter java files.
   */
  private static void generateSplitters(List<String> spinfos,
                                        List<String> decls) {
    HashSet<File> declsFileSet = new HashSet<File>();
    HashSet<File> spinfoFiles = new HashSet<File>();
    for (String spinfoFile : spinfos) {
      spinfoFile = targetDir + spinfoFile;
      spinfoFiles.add(new File(spinfoFile));
    }
    spinfoFileLists.add(new ArrayList<File>(spinfoFiles));
    for (String declsFile : decls) {
      declsFile = targetDir + declsFile;
      declsFileSet.add(new File(declsFile));
    }
    declsFileLists.add(new ArrayList<File>(declsFileSet));
    try {
      Daikon.dkconfig_suppressSplitterErrors = true;
      Daikon.create_splitters(spinfoFiles);
      PptMap allPpts = FileIO.read_declaration_files(declsFileSet);
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Moves the generated splitter files from the tempDir to the target Dir.
   */
  private static void moveFiles() {
    tempDir = new File(SplitterFactory.getTempDir());
    String[] fileNames = tempDir.list();
    for (int i = 0; i < fileNames.length; i++) {
      if (fileNames[i].endsWith(".java")) {
        String fileName = fileNames[i];
	String fromName = tempDir.getPath() + File.separator + fileName;
	String toName = targetDir + fileName + ".goal";
	boolean moveSuccess = moveFile(fromName, toName);
	if (! moveSuccess) {
          // This is consistently failing for me; not sure why.  -MDE 7/8/2005
          System.out.printf("Failed to move %s to %s%n", fromName, toName);
	}
        String javaFileName = new File(fileName).getName();
        String className =
          javaFileName.substring(0, javaFileName.length()-".java".length());
        classNames.add(className);
      }
    }
  }

  private static boolean moveFile(String fromName, String toName) {
    File from = new File(fromName);
    File to = new File(toName);
    if (! from.canRead()) {
      throw new Error("Cannot read " + fromName);
    }
    if (! to.canWrite()) {
      throw new Error("Cannot write " + toName);
    }
    // if (to.exists()) { to.delete(); }
    return from.renameTo(to);
  }

  /**
   * Writes the new code for "SplitterFactoryTest.java".
   */
  private static void writeTestClass() {
    String code = getTestClassText();
    try {
      // Delete the file, in case it is unwriteable (in which case deleting
      // works, but overwriting does not).
      new File(splitDir + "SplitterFactoryTest.java").delete();
      BufferedWriter writer = UtilMDE.bufferedFileWriter(splitDir + "SplitterFactoryTest.java");
      writer.write(code);
      writer.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a String of the new text for the SplitterFactoryTest class.
   */
  private static String getTestClassText() {
    OutputStream code = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(code);

    ps.println("// ***** This file is automatically generated by SplitterFactoryTestUpdater.java");
    ps.println();
    ps.println("package daikon.test.split;");
    ps.println();
    ps.println("import junit.framework.*;");
    ps.println("import daikon.split.*;"  );
    ps.println("import daikon.*;");
    ps.println("import java.util.*;");
    ps.println("import java.io.*;");
    ps.println("import utilMDE.*;");
    ps.println("import gnu.getopt.*;");
    ps.println();
    ps.println("/**");
    ps.println(" * THIS CLASS WAS GENERATED BY SplitterFactoryTestUpdater.");
    ps.println(" * Therefore, it is a bad idea to directly edit this class's");
    ps.println(" * code for all but temporary reasons.  Any permanent changes");
    ps.println(" * should be made through SplitterFactoryUpdater.");
    ps.println(" *");
    ps.println(" * This class contains regression tests for the SplitterFactory class.");
    ps.println(" * The tests directly test the java files produced by the");
    ps.println(" * load_splitters method by comparing them against goal files.");
    ps.println(" * Note that it is normal for some classes not to compile during this test.");
    ps.println(" *");
    ps.println(" * These tests assume that the goal files are contained in the directory:");
    ps.println(" * \"" + targetDir + "\"");
    ps.println(" * These tests ignore extra white spaces.");
    ps.println(" */");
    ps.println("public class SplitterFactoryTest extends TestCase {");
    ps.println("  // Because the SplitterFactory sequentially numbers the");
    ps.println("  // java files it produces, changing the order that the setUpTests");
    ps.println("  // commands are run will cause the tests to fail.");
    ps.println();
    ps.println("  private static String targetDir = \"" + targetDir + "\";");
    ps.println();
    ps.println("  private static String tempDir = null;");
    ps.println();
    ps.println("  private static boolean saveFiles = false;");
    ps.println();
    ps.println("    private static String usage =");
    ps.println("      UtilMDE.joinLines(");
    ps.println("        \"Usage:  java daikon.tools.CreateSpinfo FILE.java ...\",");
    ps.println("        \"  -s       Save (do not delete) the splitter java files in the temp directory\",");
    ps.println("        \"  -h       Display this usage message\"");
    ps.println("      );");
    ps.println();
    ps.println("  public static void main(String[] args) {");
    ps.println("    Getopt g =");
    ps.println("      new Getopt(\"daikon.test.split.SplitterFactoryTest\", args, \"hs\");");
    ps.println("    int c;");
    ps.println("    while ((c = g.getopt()) != -1) {");
    ps.println("      switch(c) {");
    ps.println("      case 's':");
    ps.println("        saveFiles = true;");
    ps.println("        break;");
    ps.println("      case 'h':");
    ps.println("        System.out.println(usage);");
    ps.println("        System.exit(1);");
    ps.println("        break;");
    ps.println("      case '?':");
    ps.println("        break;");
    ps.println("      default:");
    ps.println("        System.out.println(\"getopt() returned \" + c);");
    ps.println("        break;");
    ps.println("      }");
    ps.println("    }");
    ps.println("    junit.textui.TestRunner.run(suite());");
    ps.println("  }");
    ps.println();

    appendSetUpTest(ps);

    ps.println();
    ps.println("  public SplitterFactoryTest(String name) {");
    ps.println("    super(name);");
    ps.println("  }");
    ps.println();
    ps.println("  /**");
    ps.println("   * Sets up the test by generating the needed splitter java files.");
    ps.println("   */");
    ps.println("  private static void createSplitterFiles(String spinfo, String decl) {");
    ps.println("    List<String> spinfoFiles = new ArrayList<String>();");
    ps.println("    spinfoFiles.add(spinfo);");
    ps.println("    List<String> declsFiles = new ArrayList<String>();");
    ps.println("    declsFiles.add(decl);");
    ps.println("    createSplitterFiles(spinfoFiles, declsFiles);");
    ps.println("  }");
    ps.println();
    ps.println("  /**");
    ps.println("   * Sets up the test by generating the needed splitter java files.");
    ps.println("   */");
    ps.println("  private static void createSplitterFiles(List<String> spinfos, List<String> decls) {");
    ps.println("    List<File> declsFiles = new ArrayList<File>();");
    ps.println("    for (String decl : decls) {");
    ps.println("      declsFiles.add(new File(decl));");
    ps.println("    }");
    ps.println("    Set<File> spFiles = new HashSet<File>();");
    ps.println("    for (String spinfo : spinfos) {");
    ps.println("      spFiles.add(new File(spinfo));");
    ps.println("    }");
    ps.println("    try {");
    ps.println("      if (saveFiles) {");
    ps.println("        SplitterFactory.dkconfig_delete_splitters_on_exit = false;");
    ps.println("      }");
    ps.println("      Daikon.dkconfig_suppressSplitterErrors = true;");
    ps.println("      Daikon.create_splitters(spFiles);");
    ps.println("      FileIO.read_declaration_files(declsFiles); // invoked for side effect");
    ps.println("      tempDir = SplitterFactory.getTempDir();");
    ps.println("    } catch(IOException e) {");
    ps.println("        throw new RuntimeException(e);");
    ps.println("    }");
    ps.println("   }");
    ps.println();

    appendTests(ps);

    ps.println("  public static Test suite() {");
    ps.println("    setUpTests();");
    ps.println("    TestSuite suite = new TestSuite();");

    appendSuite(ps);

    ps.println("    return suite;");
    ps.println("  }");
    ps.println();
    ps.println("}");

    ps.close();
    return code.toString();
  }

  /**
   * Appends the code to write the static block of code to code.
   * This code is used by the SplitterFactoryTest to set up the
   * needed files to run the tests on.
   */
  public static void appendSetUpTest(PrintStream ps) {
    ps.println("  private static void setUpTests() {");
    ps.println("    List<String> spinfoFiles;");
    ps.println("    List<String> declsFiles;");
    for (int i = 0; i < spinfoFileLists.size(); i++) {
      ps.println("    createSplitterFiles(\"" 
                 + UtilMDE.java_source(spinfoFileLists.get(i).get(0))
                 + "\", \"" 
                 + UtilMDE.java_source(declsFileLists.get(i).get(0)) + "\");");
    }
    ps.println("  }");
  }

  /**
   * Appends the code that executes the test in SplitterFactoryTest
   * to code.
   */
  private static void appendTests(PrintStream ps) {
    ps.println("  /**");
    ps.println("   * Returns true iff files are the same (ignoring extra white space).");
    ps.println("   */");
    ps.println();
    ps.println("  public static void assertEqualFiles(String f1, String f2) {");
    ps.println("    assertTrue(\"Files \" + f1 + \" and \" + f2 + \" differ.\",");
    ps.println("               UtilMDE.equalFiles(f1, f2));");
    ps.println("  }");
    ps.println();
    ps.println("  public static void assertEqualFiles(String f1) {");
    ps.println("    assertEqualFiles(tempDir + f1,");
    ps.println("                     targetDir + f1 + \".goal\");");
    ps.println("  }");
    ps.println();
    for (String className : classNames) {
      ps.println("  public static void test" + className + "() {");
      ps.println("    assertEqualFiles(\"" + className + ".java\");");
      ps.println("  }");
      ps.println();
    }
  }

  /**
   * Appends the code that generates the test suite in SplitterFactoryTest
   * to code.
   */
  private static void appendSuite(PrintStream ps) {
    for (String className : classNames) {
      ps.println("    suite.addTest(new SplitterFactoryTest(\"test" + className + "\"));");
    }
  }

}
