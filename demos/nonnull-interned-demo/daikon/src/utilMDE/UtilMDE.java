// If you edit this file, you must also edit its tests.
// For tests of this and the entire utilMDE package, see class TestUtilMDE.

package utilMDE;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.lang.reflect.*;
// import Assert;

// The class name "UtilMDE" is very close to the package name "utilMDE".
/** Utility functions that do not belong elsewhere in the utilMDE package. */
public final class UtilMDE {
  private UtilMDE() { throw new Error("do not instantiate"); }

  private static final String lineSep = System.getProperty("line.separator");

  ///////////////////////////////////////////////////////////////////////////
  /// Array
  ///

  // For arrays, see ArraysMDE.java.

  ///////////////////////////////////////////////////////////////////////////
  /// BitSet
  ///

  /**
   * Returns true if the cardinality of the intersection of the two
   * BitSets is at least the given value.
   **/
  public static boolean intersectionCardinalityAtLeast(BitSet a, BitSet b, int i) {
    // Here are three implementation strategies to determine the
    // cardinality of the intersection:
    // 1. a.clone().and(b).cardinality()
    // 2. do the above, but copy only a subset of the bits initially -- enough
    //    that it should exceed the given number -- and if that fails, do the
    //    whole thing.  Unfortunately, bits.get(int, int) isn't optimized
    //    for the case where the indices line up, so I'm not sure at what
    //    point this approach begins to dominate #1.
    // 3. iterate through both sets with nextSetBit()

    int size = Math.min(a.length(), b.length());
    if (size > 10*i) {
      // The size is more than 10 times the limit.  So first try processing
      // just a subset of the bits (4 times the limit).
      BitSet intersection = a.get(0, 4*i);
      intersection.and(b);
      if (intersection.cardinality() >= i) {
        return true;
      }
    }
    return (intersectionCardinality(a, b) >= i);
  }

  /**
   * Returns true if the cardinality of the intersection of the two
   * BitSets is at least the given value.
   **/
  public static boolean intersectionCardinalityAtLeast(BitSet a, BitSet b, BitSet c, int i) {
    // See comments in intersectionCardinalityAtLeast(BitSet, BitSet, int).
    // This is a copy of that.

    int size = Math.min(a.length(), b.length());
    size = Math.min(size, c.length());
    if (size > 10*i) {
      // The size is more than 10 times the limit.  So first try processing
      // just a subset of the bits (4 times the limit).
      BitSet intersection = a.get(0, 4*i);
      intersection.and(b);
      intersection.and(c);
      if (intersection.cardinality() >= i) {
        return true;
      }
    }
    return (intersectionCardinality(a, b, c) >= i);
  }

  /** Returns the cardinality of the intersection of the two BitSets. **/
  public static int intersectionCardinality(BitSet a, BitSet b) {
    BitSet intersection = (BitSet) a.clone();
    intersection.and(b);
    return intersection.cardinality();
  }

  /** Returns the cardinality of the intersection of the three BitSets. **/
  public static int intersectionCardinality(BitSet a, BitSet b, BitSet c) {
    BitSet intersection = (BitSet) a.clone();
    intersection.and(b);
    intersection.and(c);
    return intersection.cardinality();
  }


  ///////////////////////////////////////////////////////////////////////////
  /// BufferedFileReader
  ///

  // Convenience methods for creating BufferedReaders and LineNumberReaders.

  /**
   * Returns a BufferedReader for the file, accounting for the possibility
   * that the file is compressed.
   * <p>
   * Warning: The "gzip" program writes and reads files containing
   * concatenated gzip files.  As of Java 1.4, Java reads
   * just the first one:  it silently discards all characters (including
   * gzipped files) after the first gzipped file.
   **/
  public static BufferedReader bufferedFileReader(String filename) throws FileNotFoundException, IOException {
    return bufferedFileReader(new File(filename));
  }

  /**
   * Returns a BufferedReader for the file, accounting for the possibility
   * that the file is compressed.
   * <p>
   * Warning: The "gzip" program writes and reads files containing
   * concatenated gzip files.  As of Java 1.4, Java reads
   * just the first one:  it silently discards all characters (including
   * gzipped files) after the first gzipped file.
   **/
  public static BufferedReader bufferedFileReader(File file) throws FileNotFoundException, IOException {
    Reader file_reader;
    if (file.getName().endsWith(".gz")) {
      file_reader = new InputStreamReader(new GZIPInputStream(new FileInputStream(file)),
                                          "ISO-8859-1");
    } else {
      file_reader = new InputStreamReader(new FileInputStream(file),
                                          "ISO-8859-1");
    }
    return new BufferedReader(file_reader);
  }


  /**
   * Returns a LineNumberReader for the file, accounting for the possibility
   * that the file is compressed.
   * <p>
   * Warning: The "gzip" program writes and reads files containing
   * concatenated gzip files.  As of Java 1.4, Java reads
   * just the first one:  it silently discards all characters (including
   * gzipped files) after the first gzipped file.
   **/
  public static LineNumberReader lineNumberFileReader(String filename) throws FileNotFoundException, IOException {
    return lineNumberFileReader(new File(filename));
  }

  /**
   * Returns a LineNumberReader for the file, accounting for the possibility
   * that the file is compressed.
   * <p>
   * Warning: The "gzip" program writes and reads files containing
   * concatenated gzip files.  As of Java 1.4, Java reads
   * just the first one:  it silently discards all characters (including
   * gzipped files) after the first gzipped file.
   **/
  public static LineNumberReader lineNumberFileReader(File file) throws FileNotFoundException, IOException {
    Reader file_reader;
    if (file.getName().endsWith(".gz")) {
      file_reader = new InputStreamReader(new GZIPInputStream(new FileInputStream(file)),
                                          "ISO-8859-1");
    } else {
      file_reader = new InputStreamReader(new FileInputStream(file),
                                          "ISO-8859-1");
    }
    return new LineNumberReader(file_reader);
  }

  /**
   * Returns a BufferedWriter for the file, accounting for the possibility
   * that the file is compressed.
   * <p>
   * Warning: The "gzip" program writes and reads files containing
   * concatenated gzip files.  As of Java 1.4, Java reads
   * just the first one:  it silently discards all characters (including
   * gzipped files) after the first gzipped file.
   **/
  public static BufferedWriter bufferedFileWriter(String filename) throws IOException {
    return bufferedFileWriter (filename, false);
  }

  /**
   * Returns a BufferedWriter for the file, accounting for the possibility
   * that the file is compressed.  The parameter 'append' if true returns
   * a file writer that appends to the end of the file instead of the
   * beginning.
   * <p>
   * Warning: The "gzip" program writes and reads files containing
   * concatenated gzip files.  As of Java 1.4, Java reads
   * just the first one:  it silently discards all characters (including
   * gzipped files) after the first gzipped file.
   **/
  public static BufferedWriter bufferedFileWriter(String filename, boolean append) throws IOException {
    Writer file_writer;
    if (filename.endsWith(".gz")) {
      file_writer = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(filename, append)));
    } else {
      file_writer = new FileWriter(filename, append);
    }
    return new BufferedWriter(file_writer);
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Class
  ///

  private static HashMap<String,Class> primitiveClasses = new HashMap<String,Class>(8);
  static {
    primitiveClasses.put("boolean", Boolean.TYPE);
    primitiveClasses.put("byte", Byte.TYPE);
    primitiveClasses.put("char", Character.TYPE);
    primitiveClasses.put("double", Double.TYPE);
    primitiveClasses.put("float", Float.TYPE);
    primitiveClasses.put("int", Integer.TYPE);
    primitiveClasses.put("long", Long.TYPE);
    primitiveClasses.put("short", Short.TYPE);
  }

  /**
   * Like @link{Class.forName(String)}, but works when the string
   * represents a primitive type, too.
   **/
  public static Class classForName(String className) throws ClassNotFoundException {
    Class result = primitiveClasses.get(className);
    if (result != null)
      return result;
    else
      return Class.forName(className);
  }

  private static HashMap<String,String> primitiveClassesJvm = new HashMap<String,String>(8);
  static {
    primitiveClassesJvm.put("boolean", "Z");
    primitiveClassesJvm.put("byte", "B");
    primitiveClassesJvm.put("char", "C");
    primitiveClassesJvm.put("double", "D");
    primitiveClassesJvm.put("float", "F");
    primitiveClassesJvm.put("int", "I");
    primitiveClassesJvm.put("long", "J");
    primitiveClassesJvm.put("short", "S");
  }

  /**
   * Convert a fully-qualified classname from Java format to JVML format.
   * For example, convert "java.lang.Object[]" to "[Ljava/lang/Object;".
   **/
  public static String classnameToJvm(String classname) {
    int dims = 0;
    while (classname.endsWith("[]")) {
      dims++;
      classname = classname.substring(0, classname.length()-2);
    }
    String result = primitiveClassesJvm.get(classname);
    if (result == null) {
      result = "L" + classname + ";";
    }
    for (int i=0; i<dims; i++) {
      result = "[" + result;
    }
    return result.replace('.', '/');
  }

  /**
   * Convert a primitive java type name (eg, int, double, etc) to
   * the single character JVM name (eg, I, D, etc).  Returns null
   * primitive_name is not a valid name
   */
  public static String primitive_name_to_jvm (String primitive_name) {
    return primitiveClassesJvm.get (primitive_name);
  }

  /**
   * Convert a fully-qualified argument list from Java format to JVML format.
   * For example, convert "(java.lang.Integer[], int, java.lang.Integer[][])"
   * to "([Ljava/lang/Integer;I[[Ljava/lang/Integer;)".
   **/
  public static String arglistToJvm(String arglist) {
    if (! (arglist.startsWith("(") && arglist.endsWith(")"))) {
      throw new Error("Malformed arglist: " + arglist);
    }
    String result = "(";
    String comma_sep_args = arglist.substring(1, arglist.length()-1);
    StringTokenizer args_tokenizer
      = new StringTokenizer(comma_sep_args, ",", false);
    for ( ; args_tokenizer.hasMoreTokens(); ) {
      String arg = args_tokenizer.nextToken().trim();
      result += classnameToJvm(arg);
    }
    result += ")";
    // System.out.println("arglistToJvm: " + arglist + " => " + result);
    return result;
  }

  private static HashMap<String,String> primitiveClassesFromJvm = new HashMap<String,String>(8);
  static {
    primitiveClassesFromJvm.put("Z", "boolean");
    primitiveClassesFromJvm.put("B", "byte");
    primitiveClassesFromJvm.put("C", "char");
    primitiveClassesFromJvm.put("D", "double");
    primitiveClassesFromJvm.put("F", "float");
    primitiveClassesFromJvm.put("I", "int");
    primitiveClassesFromJvm.put("J", "long");
    primitiveClassesFromJvm.put("S", "short");
  }

  /**
   * Convert a classname from JVML format to Java format.
   * For example, convert "[Ljava/lang/Object;" to "java.lang.Object[]".
   **/
  public static String classnameFromJvm(String classname) {
    int dims = 0;
    while (classname.startsWith("[")) {
      dims++;
      classname = classname.substring(1);
    }
    String result;
    if (classname.startsWith("L") && classname.endsWith(";")) {
      result = classname.substring(1, classname.length() - 1);
    } else {
      result = primitiveClassesFromJvm.get(classname);
      if (result == null) {
        throw new Error("Malformed base class: " + classname);
      }
    }
    for (int i=0; i<dims; i++) {
      result += "[]";
    }
    return result.replace('/', '.');
  }

  /**
   * Convert an argument list from JVML format to Java format.
   * For example, convert "([Ljava/lang/Integer;I[[Ljava/lang/Integer;)"
   * to "(java.lang.Integer[], int, java.lang.Integer[][])".
   **/
  public static String arglistFromJvm(String arglist) {
    if (! (arglist.startsWith("(") && arglist.endsWith(")"))) {
      throw new Error("Malformed arglist: " + arglist);
    }
    String result = "(";
    int pos = 1;
    while (pos < arglist.length()-1) {
      if (pos > 1)
        result += ", ";
      int nonarray_pos = pos;
      while (arglist.charAt(nonarray_pos) == '[') {
        nonarray_pos++;
      }
      char c = arglist.charAt(nonarray_pos);
      if (c == 'L') {
        int semi_pos = arglist.indexOf(";", nonarray_pos);
        result += classnameFromJvm(arglist.substring(pos, semi_pos+1));
        pos = semi_pos + 1;
      } else {
        String maybe = classnameFromJvm(arglist.substring(pos, nonarray_pos+1));
        if (maybe == null) {
          return null;
        }
        result += maybe;
        pos = nonarray_pos+1;
      }
    }
    return result + ")";
  }


  ///////////////////////////////////////////////////////////////////////////
  /// ClassLoader
  ///

  /**
   * This class has no purpose but to define loadClassFromFile.
   * ClassLoader.defineClass is protected, so I subclass ClassLoader in
   * order to call defineClass.
   **/
  private static class PromiscuousLoader extends ClassLoader {
    public Class loadClassFromFile(String className, String pathname) throws FileNotFoundException, IOException {
      FileInputStream fi = new FileInputStream(pathname);
      int numbytes = fi.available();
      byte[] classBytes = new byte[numbytes];
      fi.read(classBytes);
      fi.close();
      Class return_class = defineClass(className, classBytes, 0, numbytes);
      resolveClass(return_class);
      return return_class;
    }
  }

  private static PromiscuousLoader thePromiscuousLoader = new PromiscuousLoader();

  /**
   * @param pathname the pathname of a .class file
   * @return a Java Object corresponding to the Class defined in the .class file
   **/
  public static Class loadClassFromFile(String className, String pathname) throws FileNotFoundException, IOException {
    return thePromiscuousLoader.loadClassFromFile(className, pathname);
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Classpath
  ///

  // Perhaps abstract out the simpler addToPath from this
  /** Add the directory to the system classpath. */
  public static void addToClasspath(String dir) {
    // If the dir isn't on CLASSPATH, add it.
    String pathSep = System.getProperty("path.separator");
    // what is the point of the "replace()" call?
    String cp = System.getProperty("java.class.path",".").replace('\\', '/');
    StringTokenizer tokenizer = new StringTokenizer(cp, pathSep, false);
    boolean found = false;
    while (tokenizer.hasMoreTokens() && !found) {
      found = tokenizer.nextToken().equals(dir);
    }
    if (!found) {
      System.setProperty("java.class.path", dir + pathSep + cp);
    }
  }


  ///////////////////////////////////////////////////////////////////////////
  /// File
  ///


  /** Count the number of lines in the specified file **/
  public static long count_lines(String filename) throws IOException {
    LineNumberReader reader = UtilMDE.lineNumberFileReader(filename);
    long count = 0;
    while (reader.readLine() != null)
      count++;
    return count;
  }

  /**
   * Returns true iff files have the same contents.
   */
  public static boolean equalFiles(String file1, String file2) {
    return equalFiles(file1, file2, false);
  }

  /**
   * Returns true iff files have the same contents.
   * @param trimLines if true, call String.trim on each line before comparing
   */
  public static boolean equalFiles(String file1, String file2, boolean trimLines) {
    try {
      LineNumberReader reader1 = UtilMDE.lineNumberFileReader(file1);
      LineNumberReader reader2 = UtilMDE.lineNumberFileReader(file2);
      String line1 = reader1.readLine();
      String line2 = reader2.readLine();
      while (line1 != null && line2 != null) {
        if (trimLines) {
          line1 = line1.trim();
          line2 = line2.trim();
        }
        if (! (line1.equals(line2))) {
          return false;
        }
        line1 = reader1.readLine();
        line2 = reader2.readLine();
      }
      if (line1 == null && line2 == null) {
        return true;
      }
      return false;
    } catch (IOException e) {
        throw new RuntimeException(e);
      }
  }


  /**
   * Returns true
   *  if the file exists and is writable, or
   *  if the file can be created.
   **/
  public static boolean canCreateAndWrite(File file) {
    if (file.exists()) {
      return file.canWrite();
    } else {
      File directory = file.getParentFile();
      if (directory == null) {
        directory = new File(".");
      }
      // Does this test need "directory.canRead()" also?
      return directory.canWrite();
    }

    /// Old implementation; is this equivalent to the new one, above??
    // try {
    //   if (file.exists()) {
    //     return file.canWrite();
    //   } else {
    //     file.createNewFile();
    //     file.delete();
    //     return true;
    //   }
    // } catch (IOException e) {
    //   return false;
    // }
  }


  ///
  /// Directories
  ///

  /**
   * Creates an empty directory in the default temporary-file directory,
   * using the given prefix and suffix to generate its name. For example
   * calling createTempDir("myPrefix", "mySuffix") will create the following
   * directory: temporaryFileDirectory/myUserName/myPrefix_someString_suffix.
   * someString is internally generated to ensure no temporary files of the
   * same name are generated.
   * @param prefix The prefix string to be used in generating the file's
   *  name; must be at least three characters long
   * @param suffix The suffix string to be used in generating the file's
   *  name; may be null, in which case the suffix ".tmp" will be used Returns:
   *  An abstract pathname denoting a newly-created empty file
   * @throws IllegalArgumentException If the prefix argument contains fewer
   *  than three characters
   * @throws IOException If a file could not be created
   * @throws SecurityException If a security manager exists and its
   *  SecurityManager.checkWrite(java.lang.String) method does not allow a
   *  file to be created
   * @see java.io.File#createTempFile(String, String, File)
   **/
  public static File createTempDir(String prefix, String suffix)
    throws IOException {
    String fs = File.separator;
    String path = System.getProperty("java.io.tmpdir") + fs +
      System.getProperty("user.name") + fs;
    File pathFile =  new File(path);
    pathFile.mkdirs();
    File tmpfile = File.createTempFile(prefix + "_", "_", pathFile);
    String tmpDirPath = tmpfile.getPath() + suffix;
    tmpfile.delete();
    File tmpDir = new File(tmpDirPath);
    tmpDir.mkdirs();
    return tmpDir;
  }


  /**
   * Deletes the directory at dirName and all its files.
   * Fails if dirName has any subdirectories.
   */
  public static void deleteDir(String dirName) {
    deleteDir(new File(dirName));
  }

  /**
   * Deletes the directory at dirName and all its files.
   * Fails if dirName has any subdirectories.
   */
  public static void deleteDir(File dir) {
    File[] files = dir.listFiles();
    for (int i = 0; i < files.length; i++) {
      files[i].delete();
    }
    dir.delete();
  }


  ///
  /// File names (aka filenames)
  ///

  // Someone must have already written this.  Right?

  // Deals with exactly one "*" in name.
  public static final class WildcardFilter implements FilenameFilter {
    String prefix;
    String suffix;
    public WildcardFilter(String filename) {
      int astloc = filename.indexOf("*");
      if (astloc == -1)
        throw new Error("No asterisk in wildcard argument: " + filename);
      prefix = filename.substring(0, astloc);
      suffix = filename.substring(astloc+1);
      if (filename.indexOf("*") != -1)
        throw new Error("Multiple asterisks in wildcard argument: " + filename);
    }
    public boolean accept(File dir, String name) {
      return name.startsWith(prefix) && name.endsWith(suffix);
    }
  }

  // A better name would be "expandFilename"; "fix"is too vague. -MDE
  /**
   * Fixes a file name to do tilde expansion (to the user's home directory).
   * There maybe other logical things to do as well.
   */
  public static File fix_filename (File name) {
    String path = name.getPath();
    String newname = fix_filename (path);
    if (newname == path)
      return (name);
    else
      return new File (newname);
  }

  // A better name would be "expandFilename"; "fix"is too vague. -MDE
  /**
   * Fixes a file name to do tilde expansion (to the users home directory)
   * There maybe other logical things to do as well
   */
  public static String fix_filename (String name) {
    if (name.contains ("~"))
      return (name.replace ("~", System.getProperty ("user.home")));
    else
      return name;
  }


  /**
   * Returns a string version of the name that can be used in java source.
   * On Windows, the file will return a backslash separated string.  Since
   * backslash is an escape character, it must be quoted itself inside
   * the string.
   *
   * The current implementation presumes that backslashes don't appear
   * in filenames except as windows path separators.  That seems like a
   * reasonable assumption
   */
  public static String java_source (File name) {

    return name.getPath().replace ("\\", "\\\\");
  }

  ///
  /// Reading and writing
  ///

  /**
   * Writes an Object to a File.
   **/
  public static void writeObject(Object o, File file) throws IOException {
    // 8192 is the buffer size in BufferedReader
    OutputStream bytes =
      new BufferedOutputStream(new FileOutputStream(file), 8192);
    if (file.getName().endsWith(".gz")) {
      bytes = new GZIPOutputStream(bytes);
    }
    ObjectOutputStream objs = new ObjectOutputStream(bytes);
    objs.writeObject(o);
    objs.close();
  }


  /**
   * Reads an Object from a File.
   **/
  public static Object readObject(File file) throws
  IOException, ClassNotFoundException {
    // 8192 is the buffer size in BufferedReader
    InputStream istream =
      new BufferedInputStream(new FileInputStream(file), 8192);
    if (file.getName().endsWith(".gz")) {
      istream = new GZIPInputStream(istream);
    }
    ObjectInputStream objs = new ObjectInputStream(istream);
    return objs.readObject();
  }

  /**
   * Reads the entire contents of the specified file and returns it
   * as a string.  Any IOException encountered will be turned into an Error.
   */
  public static String readFile (File file) {

    try {
      BufferedReader reader = UtilMDE.bufferedFileReader (file);
      StringBuilder contents = new StringBuilder();
      String line = reader.readLine();
      while (line != null) {
        contents.append (line);
        contents.append (lineSep);
        line = reader.readLine();
      }
      reader.close();
      return contents.toString();
    } catch (Exception e) {
      throw new Error ("Unexpected error in writeFile", e);
    }
  }

  /**
   * Creates a file with the given name and writes the specified string
   * to it.  If the file currently exists (and is writable) it is overwritten
   * Any IOException encountered will be turned into an Error.
   */
  public static void writeFile (File file, String contents) {

    try {
      FileWriter writer = new FileWriter (file);
      writer.write (contents, 0, contents.length());
      writer.close();
    } catch (Exception e) {
      throw new Error ("Unexpected error in writeFile", e);
    }
  }


  ///////////////////////////////////////////////////////////////////////////
  /// HashMap
  ///

  // In Python, inlining this gave a 10x speed improvement.
  // Will the same be true for Java?
  /**
   * Increment the Integer which is indexed by key in the HashMap.
   * If the key isn't in the HashMap, it is added.
   * Throws an error if the key is in the HashMap but maps to a non-Integer.
   **/
  public static <T> Integer incrementHashMap(HashMap<T,Integer> hm, T key, int count) {
    Integer old = hm.get(key);
    int new_total;
    if (old == null) {
      new_total = count;
    } else {
      new_total = old.intValue() + count;
    }
    return hm.put(key, new Integer(new_total));
  }


  // In hashing, there are two separate issues.  First, one must convert
  // the input datum into an integer.  Then, one must transform the
  // resulting integer in a pseudorandom way so as to result in a number
  // that is far separated from other values that may have been near it to
  // begin with.  Often these two steps are combined, particularly if
  // one wishes to avoid creating too large an integer (losing information
  // off the top bits).

  // http://burtleburtle.net/bob/hash/hashfaq.html says (of combined methods):
  //  * for (h=0, i=0; i<len; ++i) { h += key[i]; h += (h<<10); h ^= (h>>6); }
  //    h += (h<<3); h ^= (h>>11); h += (h<<15);
  //    is good.
  //  * for (h=0, i=0; i<len; ++i) h = tab[(h^key[i])&0xff]; may be good.
  //  * for (h=0, i=0; i<len; ++i) h = (h>>8)^tab[(key[i]+h)&0xff]; may be good.

  // In this part of the file, perhaps I will eventually write good hash
  // functions.  For now, write cheesy ones that primarily deal with the
  // first issue, transforming a data structure into a single number.  This
  // is also known as fingerprinting.

  // Note that this differs from the result of Double.hashCode (which see).
  public static final int hash(double x) {
    return hash(Double.doubleToLongBits(x));
  }

  public static final int hash(double a, double b) {
    double result = 17;
    result = result * 37 + a;
    result = result * 37 + b;
    return hash(result);
  }

  public static final int hash(double a, double b, double c) {
    double result = 17;
    result = result * 37 + a;
    result = result * 37 + b;
    result = result * 37 + c;
    return hash(result);
  }

  public static final int hash(double[] a) {
    double result = 17;
    if (a != null) {
      result = result * 37 + a.length;
      for (int i = 0; i < a.length; i++) {
        result = result * 37 + a[i];
      }
    }
    return hash(result);
  }

  public static final int hash(double[] a, double[] b) {
    return hash(hash(a), hash(b));
  }


  // Don't define hash with int args; use the long versions instead.

  // Note that this differs from the result of Long.hashCode (which see)
  // But it doesn't map -1 and 0 to the same value.
  public static final int hash(long l) {
    // If possible, use the value itself.
    if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
      return (int) l;
    }

    int result = 17;
    int hibits = (int) (l >> 32);
    int lobits = (int) l;
    result = result * 37 + hibits;
    result = result * 37 + lobits;
    return result;
  }

  public static final int hash(long a, long b) {
    long result = 17;
    result = result * 37 + a;
    result = result * 37 + b;
    return hash(result);
  }

  public static final int hash(long a, long b, long c) {
    long result = 17;
    result = result * 37 + a;
    result = result * 37 + b;
    result = result * 37 + c;
    return hash(result);
  }

  public static final int hash(long[] a) {
    long result = 17;
    if (a != null) {
      result = result * 37 + a.length;
      for (int i = 0; i < a.length; i++) {
        result = result * 37 + a[i];
      }
    }
    return hash(result);
  }

  public static final int hash(long[] a, long[] b) {
    return hash(hash(a), hash(b));
  }

  public static final int hash(String a) {
    return (a == null) ? 0 : a.hashCode();
  }

  public static final int hash(String a, String b) {
    long result = 17;
    result = result * 37 + hash(a);
    result = result * 37 + hash(b);
    return hash(result);
  }

  public static final int hash(String a, String b, String c) {
    long result = 17;
    result = result * 37 + hash(a);
    result = result * 37 + hash(b);
    result = result * 37 + hash(c);
    return hash(result);
  }

  public static final int hash(String[] a) {
    long result = 17;
    if (a != null) {
      result = result * 37 + a.length;
      for (int i = 0; i < a.length; i++) {
        result = result * 37 + hash(a[i]);
      }
    }
    return hash(result);
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Iterator
  ///

  // Making these classes into functions didn't work because I couldn't get
  // their arguments into a scope that Java was happy with.

  /** Converts an Enumeration into an Iterator. */
  public static final class EnumerationIterator<T> implements Iterator<T> {
    Enumeration<T> e;
    public EnumerationIterator(Enumeration<T> e) { this.e = e; }
    public boolean hasNext() { return e.hasMoreElements(); }
    public T next() { return e.nextElement(); }
    public void remove() { throw new UnsupportedOperationException(); }
  }

  /** Converts an Iterator into an Enumeration. */
  public static final class IteratorEnumeration<T> implements Enumeration<T> {
    Iterator<T> itor;
    public IteratorEnumeration(Iterator<T> itor) { this.itor = itor; }
    public boolean hasMoreElements() { return itor.hasNext(); }
    public T nextElement() { return itor.next(); }
  }

  // This must already be implemented someplace else.  Right??
  /**
   * An Iterator that returns first the elements returned by its first
   * argument, then the elements returned by its second argument.
   * Like MergedIterator, but specialized for the case of two arguments.
   **/
  public static final class MergedIterator2<T> implements Iterator<T> {
    Iterator<T> itor1, itor2;
    public MergedIterator2(Iterator<T> itor1_, Iterator<T> itor2_) {
      this.itor1 = itor1_; this.itor2 = itor2_;
    }
    public boolean hasNext() {
      return (itor1.hasNext() || itor2.hasNext());
    }
    public T next() {
      if (itor1.hasNext())
        return itor1.next();
      else if (itor2.hasNext())
        return itor2.next();
      else
        throw new NoSuchElementException();
    }
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  // This must already be implemented someplace else.  Right??
  /**
   * An Iterator that returns the elements in each of its argument
   * Iterators, in turn.  The argument is an Iterator of Iterators.
   * Like MergedIterator2, but generlaized to arbitrary number of iterators.
   **/
  public static final class MergedIterator<T> implements Iterator<T> {
    Iterator<Iterator<T>> itorOfItors;
    public MergedIterator(Iterator<Iterator<T>> itorOfItors) { this.itorOfItors = itorOfItors; }

    // an empty iterator to prime the pump
    Iterator<T> current = new ArrayList<T>().iterator();

    public boolean hasNext() {
      while ((!current.hasNext()) && (itorOfItors.hasNext())) {
        current = itorOfItors.next();
      }
      return current.hasNext();
    }

    public T next() {
      hasNext();                // for side effect
      return current.next();
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  public static final class FilteredIterator<T> implements Iterator<T> {
    Iterator<T> itor;
    Filter<T> filter;

    public FilteredIterator(Iterator<T> itor, Filter<T> filter) {
      this.itor = itor; this.filter = filter;
    }

    T current;
    boolean current_valid = false;

    public boolean hasNext() {
      while ((!current_valid) && itor.hasNext()) {
        current = itor.next();
        current_valid = filter.accept(current);
      }
      return current_valid;
    }

    public T next() {
      if (hasNext()) {
        current_valid = false;
        return current;
      } else {
        throw new NoSuchElementException();
      }
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Returns an iterator just like its argument, except that the first and
   * last elements are removed.  They can be accessed via the getFirst and
   * getLast methods.
   **/
  public static final class RemoveFirstAndLastIterator<T> implements Iterator<T> {
    Iterator<T> itor;
    T nothing = null;           // was Object nothing = new Object();
    T first = nothing;
    T current = nothing;

    public RemoveFirstAndLastIterator(Iterator<T> itor) {
      this.itor = itor;
      if (itor.hasNext()) {
        first = itor.next();
      }
      if (itor.hasNext()) {
        current = itor.next();
      }
    }

    public boolean hasNext() {
      return itor.hasNext();
    }

    public T next() {
      if (! itor.hasNext()) {
        throw new NoSuchElementException();
      }
      T tmp = current;
      current = itor.next();
      return tmp;
    }

    public T getFirst() {
      if (first == nothing) {
        throw new NoSuchElementException();
      }
      return first;
    }

    // Throws an error unless the RemoveFirstAndLastIterator has already
    // been iterated all the way to its end (so the delegate is pointing to
    // the last element).  Also, this is buggy when the delegate is empty.
    public T getLast() {
      if (itor.hasNext()) {
        throw new Error();
      }
      return current;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }


  /**
   * Return an List containing num_elts randomly chosen
   * elements from the iterator, or all the elements of the iterator if
   * there are fewer.  It examines every element of the iterator, but does
   * not keep them all in memory.
   **/
  public static <T> List<T> randomElements(Iterator<T> itor, int num_elts) {
    return randomElements(itor, num_elts, r);
  }
  private static Random r = new Random();

  /**
   * Return an List containing num_elts randomly chosen
   * elements from the iterator, or all the elements of the iterator if
   * there are fewer.  It examines every element of the iterator, but does
   * not keep them all in memory.
   **/
  public static <T> List<T> randomElements(Iterator<T> itor, int num_elts, Random random) {
    // The elements are chosen with the following probabilities,
    // where n == num_elts:
    //   n n/2 n/3 n/4 n/5 ...

    RandomSelector<T> rs = new RandomSelector<T> (num_elts, random);

    while (itor.hasNext()) {
      rs.accept (itor.next());
    }
    return rs.getValues();


    /*
    ArrayList<T> result = new ArrayList<T>(num_elts);
    int i=1;
    for (int n=0; n<num_elts && itor.hasNext(); n++, i++) {
      result.add(itor.next());
    }
    for (; itor.hasNext(); i++) {
      T o = itor.next();
      // test random < num_elts/i
      if (random.nextDouble() * i < num_elts) {
        // This element will replace one of the existing elements.
        result.set(random.nextInt(num_elts), o);
      }
    }
    return result;
    */
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Method
  ///

  // maps from a string of arg names to an array of Class objects.
  static HashMap<String,Class[]> args_seen = new HashMap<String,Class[]>();

  public static Method methodForName(String method)
    throws ClassNotFoundException, NoSuchMethodException, SecurityException {

    int oparenpos = method.indexOf('(');
    int dotpos = method.lastIndexOf('.', oparenpos);
    int cparenpos = method.indexOf(')', oparenpos);
    if ((dotpos == -1) || (oparenpos == -1) || (cparenpos == -1)) {
      throw new Error("malformed method name should contain a period, open paren, and close paren: " + method + " <<" + dotpos + "," + oparenpos + "," + cparenpos + ">>");
    }
    for (int i=cparenpos+1; i<method.length(); i++) {
      if (! Character.isWhitespace(method.charAt(i))) {
        throw new Error("malformed method name should contain only whitespace following close paren");
      }
    }

    String classname = method.substring(0,dotpos);
    String methodname = method.substring(dotpos+1, oparenpos);
    String all_argnames = method.substring(oparenpos+1, cparenpos).trim();
    Class[] argclasses = args_seen.get(all_argnames);
    if (argclasses == null) {
      String[] argnames;
      if (all_argnames.equals("")) {
        argnames = new String[0];
      } else {
        argnames = split(all_argnames, ',');
      }

      argclasses = new Class[argnames.length];
      for (int i=0; i<argnames.length; i++) {
        String argname = argnames[i].trim();
        int numbrackets = 0;
        while (argname.endsWith("[]")) {
          argname = argname.substring(0, argname.length()-2);
          numbrackets++;
        }
        if (numbrackets > 0) {
          argname = "L" + argname + ";";
          while (numbrackets>0) {
            argname = "[" + argname;
            numbrackets--;
          }
        }
        // System.out.println("argname " + i + " = " + argname + " for method " + method);
        argclasses[i] = classForName(argname);
      }
      args_seen.put(all_argnames, argclasses);
    }
    return methodForName(classname, methodname, argclasses);
  }

  public static Method methodForName(String classname, String methodname, Class[] params)
    throws ClassNotFoundException, NoSuchMethodException, SecurityException {

    Class<?> c = Class.forName(classname);
    Method m = c.getDeclaredMethod(methodname, params);
    return m;
  }



  ///////////////////////////////////////////////////////////////////////////
  /// Properties
  ///

  /**
   * Determines whether a property has value "true", "yes", or "1".
   * @see Properties#getProperty
   **/
  public static boolean propertyIsTrue(Properties p, String key) {
    String pvalue = p.getProperty(key);
    if (pvalue == null) {
      return false;
    }
    pvalue = pvalue.toLowerCase();
    return (pvalue.equals("true") || pvalue.equals("yes") || pvalue.equals("1"));
  }

  /**
   * Set the property to its previous value concatenated to the given value.
   * @see Properties#getProperty
   * @see Properties#setProperty
   **/
  public static String appendProperty(Properties p, String key, String value) {
    return (String)p.setProperty(key, p.getProperty(key, "") + value);
  }

  /**
   * Set the property only if it was not previously set.
   * @see Properties#getProperty
   * @see Properties#setProperty
   **/
  public static String setDefault(Properties p, String key, String value) {
    String currentValue = p.getProperty(key);
    if (currentValue == null) {
      p.setProperty(key, value);
    }
    return currentValue;
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Regexp (regular expression)
  ///

    // Stolen from JDK 1.5.  Intended for use (and viewing) only by JRL
    // licensees (if you are not one, proceed no further).  To be removed
    // as soon as we migrate to Java 1.5.
    /**
     * Returns a literal pattern <code>String</code> for the specified
     * <code>String</code>.
     *
     * <p>This method produces a <code>String</code> that can be used to
     * create a <code>Pattern</code> that would match the string
     * <code>s</code> as if it were a literal pattern.</p> Metacharacters
     * or escape sequences in the input sequence will be given no special
     * meaning.
     *
     * @param  s The string to be literalized
     * @return  A literal string replacement
     * @since 1.5
     */
    public static String patternQuote(String s) {
        int slashEIndex = s.indexOf("\\E");
        if (slashEIndex == -1)
            return "\\Q" + s + "\\E";

        StringBuffer sb = new StringBuffer(s.length() * 2);
        sb.append("\\Q");
        slashEIndex = 0;
        int current = 0;
        while ((slashEIndex = s.indexOf("\\E", current)) != -1) {
            sb.append(s.substring(current, slashEIndex));
            current = slashEIndex + 2;
            sb.append("\\E\\\\E\\Q");
        }
        sb.append(s.substring(current, s.length()));
        sb.append("\\E");
        return sb.toString();
    }


  ///////////////////////////////////////////////////////////////////////////
  /// Set
  ///

  /**
   * Returns the object in this set that is equal to key.
   * The Set abstraction doesn't provide this; it only provides "contains".
   **/
  public static Object getFromSet(Set set, Object key) {
    if (key == null) {
      return null;
    }
    for (Object elt : set) {
      if (key.equals(elt)) {
        return elt;
      }
    }
    return null;
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Stream
  ///

  /** Copy the contents of the input stream to the output stream. */
  public static void streamCopy(java.io.InputStream from, java.io.OutputStream to) {
    byte[] buffer = new byte[1024];
    int bytes;
    try {
      while (true) {
        bytes = from.read(buffer);
        if (bytes == -1) {
          return;
        }
        to.write(buffer, 0, bytes);
      }
    } catch (java.io.IOException e) {
      e.printStackTrace();
      throw new Error(e);
    }
  }

  /** Return a String containing all the characters from the input stream. **/
  public static String streamString(java.io.InputStream is) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    streamCopy(is, baos);
    return baos.toString();
  }


  ///////////////////////////////////////////////////////////////////////////
  /// String
  ///

  /**
   * Return a new string which is the text of target with all instances of
   * oldStr replaced by newStr.
   **/
  public static String replaceString(String target, String oldStr, String newStr) {
    if (oldStr.equals("")) throw new IllegalArgumentException();

    StringBuffer result = new StringBuffer();
    int lastend = 0;
    int pos;
    while ((pos = target.indexOf(oldStr, lastend)) != -1) {
      result.append(target.substring(lastend, pos));
      result.append(newStr);
      lastend = pos + oldStr.length();
    }
    result.append(target.substring(lastend));
    return result.toString();
  }

  /**
   * Return an array of Strings representing the characters between
   * successive instances of the delimiter character.
   * Always returns an array of length at least 1 (it might contain only the
   * empty string).
   * @see #split(String s, String delim)
   **/
  public static String[] split(String s, char delim) {
    Vector<String> result = new Vector<String>();
    for (int delimpos = s.indexOf(delim); delimpos != -1; delimpos = s.indexOf(delim)) {
      result.add(s.substring(0, delimpos));
      s = s.substring(delimpos+1);
    }
    result.add(s);
    String[] result_array = new String[result.size()];
    result.copyInto(result_array);
    return result_array;
  }

  /**
   * Return an array of Strings representing the characters between
   * successive instances of the delimiter String.
   * Always returns an array of length at least 1 (it might contain only the
   * empty string).
   * @see #split(String s, char delim)
   **/
  public static String[] split(String s, String delim) {
    int delimlen = delim.length();
    if (delimlen == 0) {
      throw new Error("Second argument to split was empty.");
    }
    Vector<String> result = new Vector<String>();
    for (int delimpos = s.indexOf(delim); delimpos != -1; delimpos = s.indexOf(delim)) {
      result.add(s.substring(0, delimpos));
      s = s.substring(delimpos+delimlen);
    }
    result.add(s);
    String[] result_array = new String[result.size()];
    result.copyInto(result_array);
    return result_array;
  }

  /**
   * Return an array of Strings, one for each line in the argument.
   * Always returns an array of length at least 1 (it might contain only the
   * empty string).  All common line separators (cr, lf, cr-lf, or lf-cr)
   * are supported.  Note that a string that ends with a line separator
   * will return an empty string as the last element of the array.
   * @see #split(String s, char delim)
   **/
  public static String[] splitLines(String s) {
    return s.split ("\r\n?|\n\r?", -1);
  }

  /**
   * Concatenate the string representations of the objects, placing the
   * delimiter between them.
   * @see ArraysMDE#toString(int[])
   **/
  public static String join(Object[] a, String delim) {
    if (a.length == 0) return "";
    if (a.length == 1) return String.valueOf(a[0]);
    StringBuffer sb = new StringBuffer(String.valueOf(a[0]));
    for (int i=1; i<a.length; i++)
      sb.append(delim).append(a[i]);
    return sb.toString();
  }

  /**
   * Concatenate the string representations of the objects, placing the
   * system-specific line separator between them.
   * @see ArraysMDE#toString(int[])
   **/
  public static String joinLines(Object... a) {
    return join(a, lineSep);
  }

  /**
   * Concatenate the string representations of the objects, placing the
   * delimiter between them.
   * @see java.util.AbstractCollection#toString()
   **/
  public static String join(List<?> v, String delim) {
    if (v.size() == 0) return "";
    if (v.size() == 1) return v.get(0).toString();
    // This should perhaps use an iterator rather than get().
    StringBuffer sb = new StringBuffer(v.get(0).toString());
    for (int i=1; i<v.size(); i++)
      sb.append(delim).append(v.get(i));
    return sb.toString();
  }

  /**
   * Concatenate the string representations of the objects, placing the
   * system-specific line separator between them.
   * @see java.util.AbstractCollection#toString()
   **/
  public static String joinLines(List<String> v, String delim) {
    return join(v, lineSep);
  }

  // Inspired by the 'quote' function in Ajax (but independent code).
  /**
   * Escape \, ", newline, and carriage-return characters in the
   * target as \\, \\", \n, and \r; return a new string if any
   * modifications were necessary. The intent is that by surrounding
   * the return value with double quote marks, the result will be a
   * Java string literal denoting the original string. Previously
   * known as quote().
   **/
  public static String escapeNonJava(String orig) {
    StringBuffer sb = new StringBuffer();
    // The previous escape character was seen right before this position.
    int post_esc = 0;
    int orig_len = orig.length();
    for (int i=0; i<orig_len; i++) {
      char c = orig.charAt(i);
      switch (c) {
      case '\"':
      case '\\':
        if (post_esc < i) {
          sb.append(orig.substring(post_esc, i));
        }
        sb.append('\\');
        post_esc = i;
        break;
      case '\n':                // not lineSep
        if (post_esc < i) {
          sb.append(orig.substring(post_esc, i));
        }
        sb.append("\\n");       // not lineSep
        post_esc = i+1;
        break;
      case '\r':
        if (post_esc < i) {
          sb.append(orig.substring(post_esc, i));
        }
        sb.append("\\r");
        post_esc = i+1;
        break;
      default:
        // Nothing to do: i gets incremented
      }
    }
    if (sb.length() == 0)
      return orig;
    sb.append(orig.substring(post_esc));
    return sb.toString();
  }

  // The overhead of this is too high to call in escapeNonJava(String)
  public static String escapeNonJava(Character ch) {
    char c = ch.charValue();
    switch (c) {
    case '\"':
      return "\\\"";
    case '\\':
      return "\\\\";
    case '\n':                  // not lineSep
      return "\\n";             // not lineSep
    case '\r':
      return "\\r";
    default:
      return new String(new char[] { c });
    }
  }

  /**
   * Escape unprintable characters in the target, following the usual
   * Java backslash conventions, so that the result is sure to be
   * printable ASCII.  Returns a new string.
   **/
  public static String escapeNonASCII(String orig) {
    StringBuffer sb = new StringBuffer();
    int orig_len = orig.length();
    for (int i=0; i<orig_len; i++) {
      char c = orig.charAt(i);
      sb.append(escapeNonASCII(c));
    }
    return sb.toString();
  }

  /**
   * Like escapeNonJava(), but quote more characters so that the
   * result is sure to be printable ASCII. Not particularly optimized.
   **/
  private static String escapeNonASCII(char c) {
    if (c == '"') {
      return "\\\"";
    } else if (c == '\\') {
      return "\\\\";
    } else if (c == '\n') {     // not lineSep
      return "\\n";             // not lineSep
    } else if (c == '\r') {
      return "\\r";
    } else if (c == '\t') {
      return "\\t";
    } else if (c >= ' ' && c <= '~') {
      return new String(new char[] { c });
    } else if (c < 256) {
      String octal = Integer.toOctalString(c);
      while (octal.length() < 3)
        octal = '0' + octal;
      return "\\" + octal;
    } else {
      String hex = Integer.toHexString(c);
      while (hex.length() < 4)
        hex = "0" + hex;
      return "\\u" + hex;
    }
  }

  /**
   * Replace "\\", "\"", "\n", and "\r" sequences by their
   * one-character equivalents.  All other backslashes are removed
   * (for instance, octal/hex escape sequences are not turned into
   * their respective characters). This is the inverse operation of
   * escapeNonJava(). Previously known as unquote().
   **/
  public static String unescapeNonJava(String orig) {
    StringBuffer sb = new StringBuffer();
    // The previous escape character was seen just before this position.
    int post_esc = 0;
    int this_esc = orig.indexOf('\\');
    while (this_esc != -1) {
      if (this_esc == orig.length()-1) {
        sb.append(orig.substring(post_esc, this_esc+1));
        post_esc = this_esc+1;
        break;
      }
      switch (orig.charAt(this_esc+1)) {
      case 'n':
        sb.append(orig.substring(post_esc, this_esc));
        sb.append('\n');        // not lineSep
        post_esc = this_esc+2;
        break;
      case 'r':
        sb.append(orig.substring(post_esc, this_esc));
        sb.append('\r');
        post_esc = this_esc+2;
        break;
      case '\\':
        // This is not in the default case because the search would find
        // the quoted backslash.  Here we incluce the first backslash in
        // the output, but not the first.
        sb.append(orig.substring(post_esc, this_esc+1));
        post_esc = this_esc+2;
        break;

      case '0': case '1': case '2': case '3': case '4':
      case '5': case '6': case '7': case '8': case '9':
        sb.append(orig.substring(post_esc, this_esc));
        char octal_char = 0;
        int ii = this_esc+1;
        while (ii < orig.length()) {
          char ch = orig.charAt(ii++);
          if ((ch < '0') || (ch > '8'))
            break;
          octal_char = (char) ((octal_char * 8)+ Character.digit (ch, 8));
        }
        sb.append (octal_char);
        post_esc = ii-1;
        break;

      default:
        // In the default case, retain the character following the backslash,
        // but discard the backslash itself.  "\*" is just a one-character string.
        sb.append(orig.substring(post_esc, this_esc));
        post_esc = this_esc+1;
        break;
      }
      this_esc = orig.indexOf('\\', post_esc);
    }
    if (post_esc == 0)
      return orig;
    sb.append(orig.substring(post_esc));
    return sb.toString();
  }

  // Use the built-in String.trim()!
  // /** Return the string with all leading and trailing whitespace stripped. */
  // public static String trimWhitespace(String s) {
  //   int len = s.length();
  //   if (len == 0)
  //     return s;
  //   int first_non_ws = 0;
  //   int last_non_ws = len-1;
  //   while ((first_non_ws < len) && Character.isWhitespace(s.charAt(first_non_ws)))
  //     first_non_ws++;
  //   if (first_non_ws == len)
  //     return "";
  //   while (Character.isWhitespace(s.charAt(last_non_ws)))
  //     last_non_ws--;
  //   if ((first_non_ws == 0) && (last_non_ws == len))
  //     return s;
  //   else
  //     return s.substring(first_non_ws, last_non_ws+1);
  // }
  // // // Testing:
  // // assert(UtilMDE.trimWhitespace("foo").equals("foo"));
  // // assert(UtilMDE.trimWhitespace(" foo").equals("foo"));
  // // assert(UtilMDE.trimWhitespace("    foo").equals("foo"));
  // // assert(UtilMDE.trimWhitespace("foo ").equals("foo"));
  // // assert(UtilMDE.trimWhitespace("foo    ").equals("foo"));
  // // assert(UtilMDE.trimWhitespace("  foo   ").equals("foo"));
  // // assert(UtilMDE.trimWhitespace("  foo  bar   ").equals("foo  bar"));
  // // assert(UtilMDE.trimWhitespace("").equals(""));
  // // assert(UtilMDE.trimWhitespace("   ").equals(""));


  /** Remove all whitespace before or after instances of delimiter. **/
  public static String removeWhitespaceAround(String arg, String delimiter) {
    arg = removeWhitespaceBefore(arg, delimiter);
    arg = removeWhitespaceAfter(arg, delimiter);
    return arg;
  }

  /** Remove all whitespace after instances of delimiter. **/
  public static String removeWhitespaceAfter(String arg, String delimiter) {
    // String orig = arg;
    int delim_len = delimiter.length();
    int delim_index = arg.indexOf(delimiter);
    while (delim_index > -1) {
      int non_ws_index = delim_index+delim_len;
      while ((non_ws_index < arg.length())
             && (Character.isWhitespace(arg.charAt(non_ws_index)))) {
        non_ws_index++;
      }
      // if (non_ws_index == arg.length()) {
      //   System.out.println("No nonspace character at end of: " + arg);
      // } else {
      //   System.out.println("'" + arg.charAt(non_ws_index) + "' not a space character at " + non_ws_index + " in: " + arg);
      // }
      if (non_ws_index != delim_index+delim_len) {
        arg = arg.substring(0, delim_index + delim_len) + arg.substring(non_ws_index);
      }
      delim_index = arg.indexOf(delimiter, delim_index+1);
    }
    return arg;
  }

  /** Remove all whitespace before instances of delimiter. **/
  public static String removeWhitespaceBefore(String arg, String delimiter) {
    // System.out.println("removeWhitespaceBefore(\"" + arg + "\", \"" + delimiter + "\")");
    // String orig = arg;
    int delim_len = delimiter.length();
    int delim_index = arg.indexOf(delimiter);
    while (delim_index > -1) {
      int non_ws_index = delim_index-1;
      while ((non_ws_index >= 0)
             && (Character.isWhitespace(arg.charAt(non_ws_index)))) {
        non_ws_index--;
      }
      // if (non_ws_index == -1) {
      //   System.out.println("No nonspace character at front of: " + arg);
      // } else {
      //   System.out.println("'" + arg.charAt(non_ws_index) + "' not a space character at " + non_ws_index + " in: " + arg);
      // }
      if (non_ws_index != delim_index-1) {
        arg = arg.substring(0, non_ws_index + 1) + arg.substring(delim_index);
      }
      delim_index = arg.indexOf(delimiter, non_ws_index+2);
    }
    return arg;
  }


  // @return either "n noun" or "n nouns" depending on n
  public static String nplural(int n, String noun) {
    if (n == 1)
      return n + " " + noun;
    else if (noun.endsWith("s") || noun.endsWith("x") ||
             noun.endsWith("ch") || noun.endsWith("sh"))
      return n + " " + noun + "es";
    else
      return n + " " + noun + "s";
  }


  // Returns a string of the specified length, truncated if necessary,
  // and padded with spaces to the right if necessary.
  public static String rpad(String s, int length) {
    if (s.length() < length) {
      StringBuffer buf = new StringBuffer(s);
      for (int i = s.length(); i < length; i++) {
        buf.append(' ');
      }
      return buf.toString();
    } else {
      return s.substring(0, length);
    }
  }

  // Converts the int to a String, then formats it using rpad
  public static String rpad(int num, int length) {
    return rpad(String.valueOf(num), length);
  }

  // Converts the double to a String, then formats it using rpad
  public static String rpad(double num, int length) {
    return rpad(String.valueOf(num), length);
  }

  // Same as built-in String comparison, but accept null arguments,
  // and place them at the beginning.
  public static class NullableStringComparator
    implements Comparator<String>
  {
    public int compare(String s1, String s2) {
      if (s1 == null && s2 == null) return 0;
      if (s1 == null && s2 != null) return 1;
      if (s1 != null && s2 == null) return -1;
      return s1.compareTo(s2);
    }
  }

  /** Return the number of times the character appears in the string. **/
  public static int count(String s, int ch) {
    int result = 0;
    int pos = s.indexOf(ch);
    while (pos > -1) {
      result++;
      pos = s.indexOf(ch, pos+1);
    }
    return result;
  }

  /** Return the number of times the second string appears in the first. **/
  public static int count(String s, String sub) {
    int result = 0;
    int pos = s.indexOf(sub);
    while (pos > -1) {
      result++;
      pos = s.indexOf(sub, pos+1);
    }
    return result;
  }


  ///////////////////////////////////////////////////////////////////////////
  /// StringTokenizer
  ///

  /**
   * Return a Vector of the Strings returned by
   * {@link java.util.StringTokenizer#StringTokenizer(String,String,boolean)} with the given arguments.
   **/
  public static Vector tokens(String str, String delim, boolean returnTokens) {
    return makeVector(new StringTokenizer(str, delim, returnTokens));
  }

  /**
   * Return a Vector of the Strings returned by
   * {@link java.util.StringTokenizer#StringTokenizer(String,String)} with the given arguments.
   **/
  public static Vector tokens(String str, String delim) {
    return makeVector(new StringTokenizer(str, delim));
  }

  /**
   * Return a Vector of the Strings returned by
   * {@link java.util.StringTokenizer#StringTokenizer(String)} with the given arguments.
   **/
  public static Vector tokens(String str) {
    return makeVector(new StringTokenizer(str));
  }



  ///////////////////////////////////////////////////////////////////////////
  /// Throwable
  ///

  /** For the current backtrace, do "backtrace(new Throwable())". **/
  public static String backTrace(Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    pw.close();
    String result = sw.toString();
    return result;
  }

  // Deprecated as of 2/1/2004.
  /**
   * @deprecated use "backtrace(new Throwable())" instead, to avoid
   * spurious "at utilMDE.UtilMDE.backTrace(UtilMDE.java:1491)" in output.
   * @see #backTrace(Throwable)
   **/
  @Deprecated
  public static String backTrace() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    new Throwable().printStackTrace(pw);
    pw.close();
    String result = sw.toString();
    // TODO: should remove "at utilMDE.UtilMDE.backTrace(UtilMDE.java:1491)"
    return result;
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Collections
  ///

  /**
   * Returns the sorted version of the list.  Does not alter the list.
   * Simply calls Collections.sort(List<T>, Comparator<? super T>).
   **/
  public static <T> List<T> sortList (List<T> l, Comparator<? super T> c) {
    List<T> result = new ArrayList<T>(l);
    Collections.sort(result, c);
    return result;
  }


  /**
   * Returns a copy of the list with duplicates removed.
   * Retains the original order.
   **/
  public static <T> List<T> removeDuplicates(List<T> l) {
    // There are shorter solutions that do not maintain order.
    HashSet<T> hs = new HashSet<T>(l.size());
    List<T> result = new ArrayList<T>();
    for (T t : l) {
      if (hs.add(t)) {
        result.add(t);
      }
    }
    return result;
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Vector
  ///

  /** Returns a vector containing the elements of the enumeration. */
  public static <T> Vector makeVector(Enumeration<T> e) {
    Vector<T> result = new Vector<T>();
    while (e.hasMoreElements()) {
      result.addElement(e.nextElement());
    }
    return result;
  }

  // Rather than writing something like VectorToStringArray, use
  //   v.toArray(new String[0])


  /**
   * Returns a list of lists of each combination (with repetition, but
   * not permutations) of the specified objects starting at index
   * start over dims dimensions, for dims &gt; 0.
   *
   * For example, create_combinations (1, 0, {a, b, c}) returns:
   *    {a}, {b}, {c}
   *
   * And create_combinations (2, 0, {a, b, c}) returns:
   *
   *    {a, a}, {a, b}, {a, c}
   *    {b, b}, {b, c},
   *    {c, c}
   */
  public static <T> List<List<T>> create_combinations (int dims, int start, List<T> objs) {

    if (dims < 1) throw new IllegalArgumentException();

    List<List<T>> results = new ArrayList<List<T>>();

    for (int i = start; i < objs.size(); i++) {
      if (dims == 1) {
        List<T> simple = new ArrayList<T>();
        simple.add (objs.get(i));
        results.add (simple);
      } else {
        List<List<T>> combos = create_combinations (dims-1, i, objs);
        for (Iterator<List<T>> j = combos.iterator(); j.hasNext(); ) {
          List<T> simple = new ArrayList<T>();
          simple.add (objs.get(i));
          simple.addAll (j.next());
          results.add (simple);
        }
      }
    }

    return (results);
  }

  /**
   * Returns a list of lists of each combination (with repetition, but
   * not permutations) of integers from start to cnt (inclusive) over
   * arity dimensions.
   *
   * For example, create_combinations (1, 0, 2) returns:
   *    {0}, {1}, {2}
   *
   * And create_combinations (2, 0, 2) returns:
   *
   *    {0, 0}, {0, 1}, {0, 2}
   *    {1, 1}  {1, 2},
   *    {2, 2}
   */
  public static ArrayList<ArrayList<Integer>> create_combinations (int arity, int start, int cnt) {

    ArrayList<ArrayList<Integer>> results = new ArrayList<ArrayList<Integer>>();

    // Return a list with one zero length element if arity is zero
    if (arity == 0) {
      results.add (new ArrayList<Integer>());
      return (results);
    }

    for (int i = start; i <= cnt; i++) {
      ArrayList<ArrayList<Integer>> combos = create_combinations (arity-1, i, cnt);
      for (Iterator<ArrayList<Integer>> j = combos.iterator(); j.hasNext(); ) {
        ArrayList<Integer> simple = new ArrayList<Integer>();
        simple.add (new Integer(i));
        simple.addAll (j.next());
        results.add (simple);
      }
    }

    return (results);

  }

  /**
   * Returns the simple unqualified class name that corresponds to the
   * specified fully qualified name.  For example if qualified name
   * is java.lang.String, String will be returned.
   **/
  public static String unqualified_name (String qualified_name) {

    int offset = qualified_name.lastIndexOf ('.');
    if (offset == -1)
      return (qualified_name);
    return (qualified_name.substring (offset+1));
  }

  /**
   * Returns the simple unqualified class name that corresponds to the
   * specified class.  For example if qualified name of the class
   * is java.lang.String, String will be returned.
   **/
  public static String unqualified_name (Class cls) {

    return (unqualified_name (cls.getName()));
  }

  public static String human_readable (long val) {

    double dval = (double) val;
    String mag = "";

    if (val < 1000)
      ;
    else if (val < 1000000) {
      dval = val / 1000.0;
      mag = "K";
    } else if (val < 1000000000) {
      dval = val / 1000000.0;
      mag = "M";
    } else {
      dval = val / 1000000000.0;
      mag = "G";
    }

    String precision = "0";
    if (dval < 10)
      precision = "2";
    else if (dval < 100)
      precision = "1";

    return String.format ("%,1." + precision + "f" + mag, dval);

  }

}
