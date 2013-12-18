package daikon.split;

import daikon.*;

import utilMDE.*;
import utilMDE.FileCompiler;
import jtb.ParseException;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.*;

/**
 * This class contains static methods parse_spinfofile(spinfofile) and
 * load_splitters() which respectively creates Splitters from a .spinfo file
 * and load the splitters for a given Ppt.
 **/
public class SplitterFactory {
  private SplitterFactory() { throw new Error("do not instantiate"); }

  public static final Logger debug =
    Logger.getLogger("daikon.split.SplitterFactory");

  /** The directory in which the java files for the splitter will be made. */
  // This must not be set in a static block, which happens before the
  // Configuration object has had a chance to possibly set
  // dkconfig_delete_splitters_on_exit.
  private static String tempdir;

  /**
   * Boolean. Specifies whether or not the temporary Splitter files
   * should be deleted on exit.
   **/
  public static boolean dkconfig_delete_splitters_on_exit = true;

  /**
   * String.  Specifies which Java compiler is used to compile
   * Splitters.  This can be the full path name or whatever is used on
   * the commandline.
   **/
  public static String dkconfig_compiler = "javac";

  /**
   * Positive integer.  Specifies the Splitter compilation timeout, in
   * seconds, after which the compilation process is terminated and
   * retried, on the assumption that it has hung.
   **/
  public static int dkconfig_compile_timeout = 6;

  private static FileCompiler fileCompiler; // lazily initialized

  /**
   * guid is a counter that increments every time a file is written.  It is
   * used to ensure that every file written has a unique name.
   */
  private static int guid = 0;

  /// Methods

  /**
   * Parses the Splitter info.
   * @param infofile filename.spinfo
   * @return a SpinfoFileParser encapsulating the parsed splitter info file.
   */

  public static SpinfoFileParser parse_spinfofile (File infofile)
    throws IOException, FileNotFoundException {
    if (tempdir == null) {
      tempdir = createTempDir();
    }
    if (! dkconfig_delete_splitters_on_exit) {
      System.out.println("Splitters for this run created in " + tempdir);
    }
    return new SpinfoFileParser(infofile, tempdir);
  }

  /**
   * Finds the splitters that apply to a given Ppt and loads them.
   * @param ppt the Ppt
   * @param splitters a list of SpinfoFileParsers
   */

  public static void load_splitters (PptTopLevel ppt,
				     List<SpinfoFileParser> splitters)
  {
    for (SpinfoFileParser fileParser : splitters) {
      SplitterObject[][] splitterObjects = fileParser.getSplitterObjects();
      StatementReplacer statementReplacer = fileParser.getReplacer();
      for (int i = 0; i < splitterObjects.length; i++) {
	int numsplitters = splitterObjects[i].length;
	if (numsplitters != 0) {
	  String ppt_name = splitterObjects[i][0].getPptName();
	  if (matchPpt(ppt_name, ppt)) {
            // Writes, compiles, and loads the splitter .java files.
	    loadSplitters(splitterObjects[i], ppt, statementReplacer);
	    Vector<Splitter> sp = new Vector<Splitter>();
	    for (int k = 0; k < numsplitters; k++) {
	      if (splitterObjects[i][k].splitterExists()) {
		sp.addElement(splitterObjects[i][k].getSplitter());
	      } else if (! Daikon.dkconfig_suppressSplitterErrors) {
		System.out.println(splitterObjects[i][k].getError());
	      }
	    }
	    if (sp.size() >= 1) {
	      SplitterList.put (ppt_name, sp.toArray(new Splitter[0]));
	    }
	    // delete this entry in the splitter array to prevent it from
	    // matching any other Ppts, since the documented behavior is that
	    // it only matches one.
	    splitterObjects[i] = new SplitterObject[0];
	  }
	}
      }
    }
  }


  // Accessible for the purpose of testing.
  public static String getTempDir() {
    if (tempdir == null) {
      tempdir = createTempDir();
    }
    return tempdir;
  }

  private static void printAll(PptMap map) {
    System.out.println("start");
    Iterator<PptTopLevel> it = map.pptIterator();
    while (it.hasNext()) {
      PptTopLevel ppt = it.next();
      System.out.println("PPT: " + ppt.name());
    }
  }


  /**
   * Writes, compiles, and loads the splitter .java files for each
   * splitterObject in splitterObjects.
   * @param splitterObjects are the splitterObjects for ppt
   * @param ppt the Ppt for these splitterObjects
   * @param statementReplacer a StatementReplacer for the replace statements
   *  to be used in these splitterObjects.
   */
  private static void loadSplitters(SplitterObject[] splitterObjects,
                                    PptTopLevel ppt,
                                    StatementReplacer statementReplacer)
  {
    // System.out.println("loadSplitters for " + ppt.name);
    for (int i = 0; i < splitterObjects.length; i++) {
      SplitterObject splitObj = splitterObjects[i];
      String fileName = getFileName(splitObj.getPptName());
      StringBuffer file;
      try {
        SplitterJavaSource splitterWriter =
          new SplitterJavaSource(splitObj,
                                 splitObj.getPptName(),
                                 fileName,
                                 ppt.var_infos,
                                 statementReplacer);
        file = splitterWriter.getFileText();
      } catch (ParseException e) {
        System.out.println("Error in SplitterFactory while writing splitter java file for: ");
        System.out.println(splitObj.condition() + " cannot be parsed.");
        continue;
      }
      String fileAddress = tempdir + fileName;
      splitObj.setClassName(fileName);
      try {
        BufferedWriter writer = UtilMDE.bufferedFileWriter(fileAddress + ".java");
        if (dkconfig_delete_splitters_on_exit) {
          (new File (fileAddress + ".java")).deleteOnExit();
          (new File (fileAddress + ".class")).deleteOnExit();
          }
        writer.write(file.toString());
        writer.flush();
      } catch (IOException ioe) {
        System.out.println("Error while writing Splitter file: " +
                           fileAddress);
        debugPrintln(ioe.toString());
      }
    }
    List<String> fileNames = new ArrayList<String>();
    for (int i = 0; i < splitterObjects.length; i++) {
      fileNames.add(splitterObjects[i].getFullSourcePath());
    }
    compileFiles(fileNames);
    SplitterLoader loader = new SplitterLoader();
    for (int i = 0; i < splitterObjects.length; i++) {
      splitterObjects[i].load(loader);
    }
  }

  private static void compileFiles(List<String> fileNames) {
    // We delay setting fileCompiler until now because we want to permit
    // the user to set the dkconfig_compiler variable.  Note that our
    // timeout is specified in seconds, but the parameter to FileCompiler
    // is specified in milliseconds
    if (fileCompiler == null) {
      fileCompiler = new FileCompiler(dkconfig_compiler,
                                      dkconfig_compile_timeout * 1000);
    }
    fileCompiler.compileFiles(fileNames);
  }


  /**
   * Determine whether a Ppt's name matches the given pattern.
   */

  private static boolean matchPpt(String ppt_name, PptTopLevel ppt) {
    if (ppt.name.equals(ppt_name))
      return true;
    if (ppt_name.endsWith(":::EXIT")) {
      String regex = UtilMDE.patternQuote(ppt_name) + "[0-9]+";
      if (matchPptRegex(regex, ppt))
	return true;
    }

    // look for corresponding EXIT ppt. This is because the exit ppt usually has
    // more relevant variables in scope (eg. return, hashcodes) than the enter.
    String regex;
    int index = ppt_name.indexOf("OBJECT");
    if (index == -1) {
      // Didn't find "OBJECT" suffix; add ".*EXIT".
      regex = UtilMDE.patternQuote(ppt_name) + ".*EXIT";
    } else {
      // Found "OBJECT" suffix.
      if (ppt_name.length() > 6) {
        regex = UtilMDE.patternQuote(ppt_name.substring(0, index-1)) + ":::OBJECT";
      } else {
        regex = UtilMDE.patternQuote(ppt_name);
      }
    }
    return matchPptRegex(regex, ppt);
  }

  private static boolean matchPptRegex(String ppt_regex, PptTopLevel ppt) {
    // System.out.println("matchPptRegex: " + ppt_regex);
    Pattern pattern = Pattern.compile(ppt_regex);
    String name = ppt.name;
    Matcher matcher = pattern.matcher(name);
    // System.out.println("  considering " + name);
    return matcher.find();
  }


  /**
   * Returns a file name for a splitter file to be used with a Ppt
   * with the name, ppt_name.  The file name is ppt_name with all
   * characters which are invalid for use in a java file name (such
   * as ".") replaced with "_".  Then "_guid" is append to the end.
   * For example if ppt_name is "myPackage.myClass.someMethod" and
   * guid = 12, then the following would be returned:
   * "myPackage_myClass_someMethod_12".
   * @param ppt_name the name of the Ppt for which the splitter
   *  java file is going to be used with.
   */
  private static String getFileName(String ppt_name) {
    String splitterName = clean(ppt_name);
    splitterName = splitterName + "_" + guid;
    guid++;
    return splitterName;
  }

  /**
   * Cleans str by replacing all characters that are not
   * valid java indentifier parts with "_".
   * @param str the string to be cleaned.
   * @return str with all non java indentifier parts replaced
   *  with "_".
   */
  private static String clean(String str) {
    char[] cleaned = str.toCharArray();
    for (int i=0; i < cleaned.length; i++) {
      char c = cleaned[i];
      if (! Character.isJavaIdentifierPart(c)) {
        cleaned[i] = '_';
      }
    }
    return new String(cleaned);
  }


  /**
   * Creates the temporary directory in which splitter files will
   * be stored.
   * @return the name of the temporary directory. This is where
   *  the Splitters are created.
   **/
  private static String createTempDir() {
    try {
      File tmpDir = UtilMDE.createTempDir("daikon", "split");
      if (dkconfig_delete_splitters_on_exit) {
        tmpDir.deleteOnExit();
      }
      return tmpDir.getPath() + File.separator;
    } catch (IOException e) {
      debugPrintln(e.toString());
    }
    return ""; // Use current directory
  }

  /**
   * Print out a message if the debugPptSplit variable is set to "true".
   **/
  private static void debugPrintln(String s) {
    Global.debugSplit.fine(s);
  }


}
