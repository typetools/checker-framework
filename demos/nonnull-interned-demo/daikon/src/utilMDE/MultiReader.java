package utilMDE;

import java.io.*;
import java.util.*;
import java.util.regex.*;

// TODO:
// MultiReader has a public concept of "short entry", but I don't think that
// concept is logically part of MultiReader.  I think it would be better for
// Lookup to make this decision itself, for instance by checking whether there
// are any line separators in the entry that it gets back.
//
// Here are some useful features that MultiReader should have.
//  * It should extend LineNumberReader (but also provide a mechanism for
//    indicating the current file name in addition to the current line number).
//  * It should have constructors that take an InputStream or Reader
//    (in addition to the current BufferedReader, File, and String versions).
//  * It should have a close method.
//  * It should automatically close the underlying file/etc. when the
//    iterator gets to the end (or the end is otherwise reached).
// Some of these features are inspired by TextFile, which we should
// eliminate in favor of MultiReader.
//
// Lower priority:  consider changing the name of this class to LineReader,
// or EntryReader, or something else that is more descriptive.


/**
 * Class that reads "lines" from a file.  It supports:
 *   include files,
 *   comments, and
 *   multi-line entries (paragraphs).
 * The syntax of each of these is customizable.
 * @see #get_entry() and @see #set_entry_start_stop(String,String)
 */
public class MultiReader implements Iterable<String>, Iterator<String> {

  /** Information about the current reader **/
  private static class ReaderInfo {
    public BufferedReader reader;
    public String filename;
    public long line_number;

    public ReaderInfo (BufferedReader reader, String filename) {
      this.reader = reader;
      this.filename = filename;
    }

    public ReaderInfo (String filename) throws IOException {
      this.reader = UtilMDE.bufferedFileReader (filename);
      this.filename = filename;
    }
  }

  /** Stack of readers.  Used to support include files */
  private Stack<ReaderInfo> readers = new Stack<ReaderInfo>();

  /** Regular expression that specifies an include file. **/
  private Pattern include_re = null;

  /** Regular expression that matches a comment **/
  private Pattern comment_re = null;

  /**
   * Regular expression that starts a long entry (paragraph).
   * By default, paragraphs are separated by blank lines.
   */
  public Pattern entry_start_re = null;

  /**
   * Regular expression that terminates a long entry.  Long entries
   * can also be terminated by the start of a new long entry or the
   * end of the current file.
   */
  public Pattern entry_stop_re = null;

  /** Line that is pushed back to be reread **/
  String pushback_line = null;

  /** Platform-specific line separator **/
  private static final String lineSep = System.getProperty("line.separator");

  /** Descriptor for an entry (paragraph) **/
  public static class Entry {
    /** First line  of the entry */
    public String first_line;
    /** Complete body of the entry including the first line **/
    public String body;
    /** True if this is a short entry (blank line separated) **/
    boolean short_entry;
    /** Filename in which the entry was found **/
    String filename;
    /** Line number of first line of entry **/
    long line_number;

    /** Create an entry **/
    Entry (String first_line, String body, String filename, long line_number,
           boolean short_entry) {
      this.first_line = first_line;
      this.body = body;
      this.filename = filename;
      this.line_number = line_number;
      this.short_entry = short_entry;
    }

    /**
     * Return a description of the entry body that matches the specified
     * regular expression.  If no match is found, reeturns the first_line
     */
    String get_description (Pattern re) {

      if (re == null)
        return first_line;

      Matcher descr = re.matcher (body);
      if (descr.find()) {
        return descr.group();
      } else {
        return first_line;
      }
    }
  }


  /**
   * Create a MultiReader
   *
   *    @param reader Initial source
   *    @param comment_re Regular expression that matches comments.
   *                      Any text that matches comment_re is removed.
   *                      A line that is entirely a comment is ignored
   *    @param include_re Regular expression that matches include directives.
   *                      The expression should define one group that contains
   *                      the include file name
   */
  public MultiReader (BufferedReader reader, String comment_re,
                      String include_re) {

    readers.push (new ReaderInfo (reader, null));
    if (comment_re != null)
      this.comment_re = Pattern.compile (comment_re);
    if (include_re != null)
      this.include_re = Pattern.compile (include_re);
  }

  /** Create a MultiReader that does not support comments or include directives.
   * @see #MultiReader(BufferedReader,String,String) **/
  public MultiReader (BufferedReader reader) {
    this (reader, null, null);
  }

  /**
   * Create a MultiReader
   *
   *    @param file       Initial file to read
   *    @param comment_re Regular expression that matches comments.
   *                      Any text that matches comment_re is removed.
   *                      A line that is entirely a comment is ignored
   *    @param include_re Regular expression that matches include directives.
   *                      The expression should define one group that contains
   *                      the include file name
   */
  public MultiReader (File file, String comment_re,
                      String include_re) throws IOException {
    this ((UtilMDE.bufferedFileReader (file)), comment_re, include_re);
    readers.peek().filename = file.toString();
  }

  /** Create a MultiReader that does not support comments or include directives.
   * @see #MultiReader(File,String,String) **/
  public MultiReader (File file) throws IOException {
    this (file, null, null);
  }

  /**
   * Create a new MultiReader starting with the specified file.
   * @see #MultiReader(File,String,String)
   */
  public MultiReader (String filename, String comment_re,
                      String include_re) throws IOException {
    this (new File(filename), comment_re, include_re);
  }

  /** Create a MultiReader that does not support comments or include directives.
   * @see #MultiReader(String,String,String) **/
  public MultiReader (String filename) throws IOException {
    this (filename, null, null);
  }

  /**
   * Read a line, ignoring comments and processing includes.  Note that
   * a line that is completely a comment is completely ignored (and
   * not returned as a blank line).  Returns null at end of file.
   */
  public String readLine() throws IOException {

    // System.out.printf ("Entering size = %d%n", readers.size());

    // If a line has been pushed back, return it instead
    if (pushback_line != null) {
      String line = pushback_line;
      pushback_line = null;
      return line;
    }

    String line = get_next_line();
    if (comment_re != null) {
      while (line != null) {
        Matcher cmatch = comment_re.matcher (line);
        if (cmatch.find()) {
          line = cmatch.replaceFirst ("");
          if (line.length() > 0)
            break;
        } else {
          break;
        }
        line = get_next_line();
      // System.out.printf ("get_next_line = %s%n", line);
      }
    }

    if (line == null)
      return null;

    // Handle include files.  Non-absolute pathnames are relative
    // to the including file (the current file)
    if (include_re != null) {
      Matcher m = include_re.matcher (line);
      if (m.matches()) {
        File filename = new File (UtilMDE.fix_filename(m.group (1)));
        // System.out.printf ("Trying to include filename %s%n", filename);
        if (!filename.isAbsolute()) {
          File current_filename = new File (readers.peek().filename);
          File current_parent = current_filename.getParentFile();
          filename = new File (current_parent, filename.toString());
          // System.out.printf ("absolute filename = %s %s %s%n",
          //                     current_filename, current_parent, filename);
        }
        readers.push (new ReaderInfo (filename.getAbsolutePath()));
        return readLine();
      }
    }

    // System.out.printf ("Returning [%d] '%s'%n", readers.size(), line);
    return (line);
  }

  /** Returns a line-by-line interator for this file. **/
  public Iterator<String> iterator() {
    return this;
  }

  /**
   * Returns whether or not there is another line to read.  Any IOExceptions
   * are turned into errors (because the definition of hasNext() in Iterator
   * doesn't throw any exceptions
   **/
  public boolean hasNext() {
    if (pushback_line != null)
      return true;

    String line = null;
    try {
      line = readLine();
    } catch (IOException e) {
      throw new Error ("unexpected IOException: ", e);
    }

    if (line == null)
      return false;

    putback (line);
    return true;
  }

  /** Returns the next line in the multi-file **/
  public String next() {
    try {
      return readLine();
    } catch (IOException e) {
      throw new Error ("unexpected IOException", e);
    }
  }

  /** remove() is not supported **/
  public void remove() {
    throw new UnsupportedOperationException ("can't remove lines from file");
  }

  /**
   * Returns the next entry (paragraph) in the file.  Entries are separated
   * by blank lines unless the entry started with entry_start_re (@see
   * set_entry_start_stop).  If no more entries are available returns null.
   */
  public Entry get_entry() throws IOException {

    // Skip any preceeding blank lines
    String line = readLine();
    while ((line != null) && (line.trim().length() == 0))
      line = readLine();
    if (line == null)
      return (null);

    StringBuilder body = new StringBuilder(10000);
    Entry entry = null;
    String filename = get_filename();
    long line_number = get_line_number();

    // If this is a long entry
    Matcher entry_match = null;
    if (entry_start_re != null)
      entry_match = entry_start_re.matcher (line);
    if ((entry_match != null) && entry_match.find()) {

      // Remove entry match from the line
      if (entry_match.groupCount() > 0) {
        line = entry_match.replaceFirst (entry_match.group(1));
      }

      // Description is the first line
      String description = line;

      // Read until we find the termination of the entry
      Matcher end_entry_match = entry_stop_re.matcher(line);
      while ((line != null) && !entry_match.find() &&
             !end_entry_match.find() && filename.equals (get_filename())) {
        body.append (line);
        body.append (lineSep);
        line = readLine();
        entry_match = entry_start_re.matcher(line);
        end_entry_match = entry_stop_re.matcher(line);
      }

      // If this entry was terminated by the start of the next one,
      // put that line back
      if ((line != null) && (entry_match.find(0)
                             || !filename.equals (get_filename())))
        putback (line);

      entry = new Entry (description, body.toString(), filename,
                                     line_number, false);

    } else { // blank separated entry

      String description = line;

      // Read until we find another blank line
      while ((line != null) && (line.trim().length() != 0)
             && filename.equals (get_filename())) {
        body.append (line);
        body.append (lineSep);
        line = readLine();
      }

      // If this entry was terminated by the start of a new input file
      // put that line back
      if ((line != null) && !filename.equals (get_filename()))
        putback (line);

      entry = new Entry (description, body.toString(), filename, line_number,
                         true);
    }

    return (entry);

  }



  /**
   * Reads the next line from the current reader.  If EOF is encountered
   * pop out to the next reader.  Returns null if there is no more input
   */
  private String get_next_line() throws IOException {

    if (readers.size() == 0)
      return (null);

    ReaderInfo ri = readers.peek();
    String line = ri.reader.readLine();
    ri.line_number++;
    while (line == null) {
      readers.pop();
      if (readers.empty())
        return (null);
      ri = readers.peek();
      line = ri.reader.readLine();
      ri.line_number++;
    }
    return (line);
  }

  /** Returns the current filename **/
  public String get_filename() {
    return readers.peek().filename;
  }

  /** Returns the current line number in the current file **/
  public long get_line_number() {
    return readers.peek().line_number;
  }

  /**
   * Set the regular expressions for the start and stop of long
   * entries (multiple lins that are read as a group by get_entry())
   */
  public void set_entry_start_stop (String entry_start_re,
                                    String entry_stop_re) {
    this.entry_start_re = Pattern.compile (entry_start_re);
    this.entry_stop_re = Pattern.compile (entry_stop_re);
  }

  /**
   * Set the regular expressions for the start and stop of long
   * entries (multiple lins that are read as a group by get_entry())
   */
  public void set_entry_start_stop (Pattern entry_start_re,
                                    Pattern entry_stop_re) {
    this.entry_start_re = entry_start_re;
    this.entry_stop_re = entry_stop_re;
  }

  /**
   * Puts the specified line back in the input.  Only one line can be
   * put back
   */
  public void putback (String line) {
    assert pushback_line == null : "push back '" + line + "' when '"
      + pushback_line + "' already back";
    pushback_line = line;
  }

  /** Simple example **/
  public static void main (String[] args) throws IOException {

    MultiReader reader = new MultiReader (args[0], args[1], args[2]);

    String line = reader.readLine();
    while (line != null) {
      System.out.printf ("%s: %d: %s%n", reader.get_filename(),
                         reader.get_line_number(), line);
      line = reader.readLine();
    }
  }
}
