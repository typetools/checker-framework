package utilMDE;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.text.*;
import static utilMDE.MultiReader.Entry;
import com.sun.javadoc.*;

/**
 * TaskManager extracts information about tasks from text files and
 * provides structured output.  For example, it can extract all of
 * the tasks associated with a specific milestone or person and total
 * the amount of work required.
 */
public class TaskManager {

  public enum OutputFormat {short_ascii, short_html, milestone_html};

  // Command line options
  @Option ("-r Include only those tasks assigned to the specified person")
  public static String responsible = null;

  @Option ("-m Include only those tasks required for the specified milestone")
  public static String milestone = null;

  @Option ("-c Include only completed tasks")
  public static boolean completed = false;

  @Option ("-o Include only open tasks")
  public static boolean open = false;

  @Option ("-v Print progress information")
  public static boolean verbose = false;

  @Option ("-f Specify output format (short_ascii, short_html, milestone_html)")
  public static OutputFormat format = OutputFormat.short_ascii;

  @Option ("Regex that matches an entire comment (not just a comment start)")
  public static String comment_re = "^%.*";

  @Option ("Regex that matches an include directive; group 1 is the file name")
  public static String include_re = "\\\\include\\{(.*)\\}";

  private static String usage_string
    = "TaskManger [options] <task-file> <task_file> ...";

  public static final String lineSep = System.getProperty("line.separator");

  /** Information about a single task **/
  public static class Task {

    String filename;
    long line_number;

    String task;
    String responsible;
    Date assigned_date;
    String milestone;
    Float duration;
    Float completed;
    String description;
    String notes;

    public Task (String body, String filename, long line_number)
      throws IOException {

      this.filename = filename;
      this.line_number = line_number;

      String[] lines = body.split (lineSep);
      for (int ii = 0; ii < lines.length; ii++) {
        String line = lines[ii];

        // Get the item/value out of the record.  One line items
        // are specifed as '{item}: {value}'.  Multiple line items
        // have a start line of '{item}>' and an end line of '<{item}'
        // with any number of value lines between.
        String item = null;
        String value = null;
        if (line.matches ("^[_a-zA-Z]+:.*")) {
          String[] sa = line.split (" *: *", 2);
          item = sa[0];
          value = sa[1];
          if (value.length() == 0)
            value = null;
        } else if (line.matches ("^[-a-zA-Z]+>.*")) {
          item = line.replaceFirst (" *>.*", "");
          value = "";
          for (ii++; ii < lines.length; ii++) {
            String nline = lines[ii];
            if (nline.equals ("<" + item))
              break;
            value += nline + lineSep;
          }
        } else {
          throw new IOException ("malformed line: " + line);
        }

        // parse the value based on the item and store it away
        if (item.equals ("task")) {
          task = value;
        } else if (item.equals ("responsible")) {
          if (value == null)
            responsible = "none";
          else
            responsible = value;
        } else if (item.equals ("assigned_date")) {
          if (value == null)
            assigned_date = null;
          else {
            DateFormat df = new SimpleDateFormat("yy-MM-dd");
            try {
              assigned_date = df.parse (value);
              assert assigned_date != null : value;
            } catch (Throwable t) {
              throw new RuntimeException (t);
            }
          }
        } else if (item.equals ("milestone")) {
          milestone = value;
        } else if (item.equals ("duration")) {
          if (value == null)
            duration = null;
          else
            duration = Float.parseFloat (value);
        } else if (item.equals ("completed")) {
          if (value == null)
            completed = null;
          else
            completed = Float.parseFloat (value);
        } else if (item.equals("description")) {
          description = value;
        } else if (item.equals("notes")) {
          notes = value;
        } else {
          throw new IOException ("unknown field " + item);
        }
      }
    }

    public static String short_str (float f) {
      if (((double)f) - Math.floor ((double)(f)) > 0.1)
        return String.format ("%.1f", f);
      else
        return String.format ("%d", Math.round (f));
    }

    public String toString_short_ascii() {

      String duration_str = String.format ("%s/%s", short_str (completed),
                                           short_str(duration));
      return String.format ("%-10s %-10s %-6s %s", responsible, milestone,
                            duration_str, task);
    }

    public String toString_short_html(double total) {
      String duration_str = String.format ("%s/%s", short_str (completed),
                                           short_str(duration));
      return String.format ("<tr> <td> %s </td><td> %s </td><td> "
                            + "%s </td><td> %f </td><td> %s </td></tr>",
                            responsible, milestone, duration_str, total, task);
    }

    public String toString_milestone_html(double total) {
      String duration_str = String.format ("%s/%s", short_str (completed),
                                           short_str(duration));
      String resp_str = responsible;
      if (resp_str.equals ("none"))
        resp_str = "<font color=red><b>" + resp_str + "</b></font>";
      return String.format ("<tr> <td> %s </td><td> %s </td><td> %.1f </td><td>"
                            + "<a href=%s?file=%s&line=%d> %s </a></td></tr>",
                            resp_str, duration_str, total,
                            "show_task_details.php",
                            filename, line_number, task);
    }


    public String all_vals() {
      StringBuilder out = new StringBuilder();
      out.append ("task:            " + task + lineSep);
      out.append ("responsible:     " + responsible + lineSep);
      out.append ("assigned_date:   " + assigned_date + lineSep);
      out.append ("milestone:       " + milestone + lineSep);
      out.append ("duration:        " + duration + lineSep);
      out.append ("completed:       " + completed + lineSep);
      out.append ("description:     " + description + lineSep);
      out.append ("notes:           " + notes + lineSep);
      return out.toString();
    }
  }

  /** List of all of the tasks **/
  public List<Task> tasks = new ArrayList<Task>();

  /** empty TaskManger **/
  public TaskManager() {
  }

  /** initializes a task manager with all of the tasks in filenames **/
  public TaskManager (String[] filenames) throws IOException {

    // Read in each specified task file
    for (String filename : filenames) {
      filename = UtilMDE.fix_filename (filename);
      MultiReader reader = new MultiReader (filename, comment_re, include_re);
      while (true) {
        MultiReader.Entry entry = reader.get_entry();
        if (entry == null) break;
        try {
          tasks.add (new Task (entry.body, entry.filename, entry.line_number));
        } catch (IOException e) {
          throw new Error ("Error parsing " + entry.filename + " at line "
                           + entry.line_number, e);
        }
      }
    }
  }

  public static void main (String args[]) throws IOException {

    Options options = new Options (usage_string, TaskManager.class);
    String[] filenames = options.parse_and_usage (args);

    if (verbose)
      System.out.printf ("Option settings: %s%n", options.settings());

    // Make sure at least one file was specified
    if (filenames.length == 0) {
      options.print_usage ("Error: No task files specified");
      System.exit (254);
    }

    TaskManager tm = new TaskManager(filenames);

    // Dump out the tasks
    if (verbose) {
      System.out.printf ("All tasks:%n");
      for (Task task : tm.tasks) {
        System.out.printf ("%s\n\n", task.all_vals());
      }
    }

    // Print specified tasks
    TaskManager matches = tm.responsible_match (responsible);
    matches = matches.milestone_match (milestone);
    if (open)
      matches = matches.open_only();
    if (completed)
      matches = matches.completed_only();
    switch (format) {
    case short_ascii:
      System.out.println (matches.toString_short_ascii());
      break;
    case short_html:
      System.out.println (matches.toString_short_html());
      break;
    case milestone_html:
      System.out.println (matches.toString_milestone_html());
      break;
    }
  }

  public String toString_short_ascii() {
    StringBuilder out = new StringBuilder();
    for (Task task : tasks) {
      out.append (task.toString_short_ascii() + lineSep);
    }
    return (out.toString());
  }

  public String toString_short_html() {
    StringBuilder out = new StringBuilder();
    double total = 0.0;
    String responsible = null;
    out.append ("<table>" + lineSep);
    for (Task task : tasks) {
      if (!task.responsible.equals (responsible)) {
        responsible = task.responsible;
        total = 0.0;
      }
      total += (task.duration.floatValue() - task.completed.floatValue());
      out.append (task.toString_short_html(total) + lineSep);
    }
    out.append ("</table>" + lineSep);
    return (out.toString());
  }

  public String toString_milestone_html() {
    StringBuilder out = new StringBuilder();
    out.append ("<table border cellspacing=0 cellpadding=2>" + lineSep);
    out.append ("<tr> <th> Responsible <th> C/D <th> Total <th> Task </tr>"
                + lineSep);
    double total = 0.0;
    String responsible = null;
    for (Task task : tasks) {
      if (!task.responsible.equals (responsible)) {
        if (responsible != null)
          out.append ("<tr bgcolor=grey><td colspan=4></td></tr>" + lineSep);
        responsible = task.responsible;
        total = 0.0;
      }
      total += (task.duration.floatValue() - task.completed.floatValue());
      out.append (task.toString_milestone_html(total) + lineSep);
    }
    out.append ("</table>" + lineSep);
    return (out.toString());
  }

  /** Adds the specified task to the end of the task list **/
  public void add (Task task) {
    tasks.add (task);
  }

  /**
   * Create a new TaskManger with only those tasks assigned to responsible.
   * All tasks match a responsible value of null
   **/
  public TaskManager responsible_match (String responsible) {

    TaskManager tm = new TaskManager();

    for (Task task : tasks) {
      if ((responsible == null)
          || responsible.equalsIgnoreCase (task.responsible))
        tm.add (task);
    }

    return tm;
  }

  /** Create a new TaskManger with only those tasks in milestone **/
  public TaskManager milestone_match (String milestone) {

    TaskManager tm = new TaskManager();
    if (milestone == null)
      return tm;

    for (Task task : tasks) {
      if (milestone.equalsIgnoreCase (task.milestone))
        tm.add (task);
    }

    return tm;
  }

  /**
   * Create a new TaskManger with only completed tasks.
   **/
  public TaskManager completed_only () {

    TaskManager tm = new TaskManager();

    for (Task task : tasks) {
      if (task.duration <= task.completed)
        tm.add (task);
    }

    return tm;
  }

  /**
   * Create a new TaskManger with only open tasks.
   **/
  public TaskManager open_only () {

    TaskManager tm = new TaskManager();

    for (Task task : tasks) {
      if (task.duration > task.completed)
        tm.add (task);
    }

    return tm;
  }



}
