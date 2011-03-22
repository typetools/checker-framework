package daikon.tools.jtb;

import daikon.*;
import daikon.inv.OutputFormat;
import utilMDE.*;
import gnu.getopt.*;
import java.util.logging.Logger;
import java.io.*;
import java.util.*;

import jtb.*;
import jtb.syntaxtree.*;

/**
 * Merge Daikon-generated invariants into Java source code as ESC/JML/DBC
 * annotations.  All original .java files are left unmodified; copies are
 * created.
 * <p>
 *
 * The first argument is a Daikon .inv file -- a serialized file of
 * Invariant objects.  All subsequent arguments are Foo.java files that are
 * rewritten into Foo.java-jmlannotated versions; alternately, use the -r
 * flag to process every .java file under the current directory.
 * <p>
 **/
public class Annotate {

  // ESC format: Invariants are inserted as follows:
  //  * invariants at method entry become "requires"
  //  * invariants at method exit become "ensures".
  //    ensures/exsures annotations should not mention orig(primitive arguments).
  //  * invariants at method exceptional exit become "exsures"
  //  * object invariants become object invariants, inserted at the beginning
  //    of the java class
  //  * "modifies" clauses are inserted for changed variables.
  //    Use "a[*]", not "a[]" or "a[i]" or "a[i..]" or "a[...].field", for arrays;
  //    consider adding a \forall to specify which indices are *not* changed.
  //    Modifies clauses should not contain "size()", ".getClass()", or "~".
  //    Modifies clauses should not contain final fields.
  //  * use "also_requires", "also_ensures", "also_modifies" if this method
  //    overrides another method/interface.

  // spec_public:
  // for each non-public field, add "/*@ spec_public */" to its declaration

  // owner annotations:
  // for each private field that is set via a call to new in the constructor
  // (in the short term, just do this for all array fields):
  //  * add object invariant "invariant field.owner == this"
  //  * whenever the field is set, "set field.owner = this"

  // JML format: The formatting of invariants is different (we use
  // Invariant.OutputFormat.JML instead of
  // Invariant.OutputFormat.ESC).  Other than that, the format is
  // almost the same as ESC (heavyweight) format, but "modifies"
  // clauses are omitted.

  // DBC format: same as ESC format, but "requires" becomes "@pre",
  // "ensures" becomes "@post", and we omit everything but invariants,
  // pre and postconditions.  See Appendix A below for Jtest's (not
  // very formal or detailed) specification of their DBC
  // language.  Note that we avoided using most DBC constructs -- we
  // found the DBC aspect of Jtest buggy, so we try to depend on their
  // constructs as little as possible.

  // Explanatory comments:
  // Handle "The invariant on the following line means:".

  // Optional behavior:
  //  * With -i flag, invariants not supported by ESC are inserted with "!"
  //    instead of "@"; by default these "inexpressible" invariants are
  //    simply omitted.
  //  * With -s flag, use // comments; by default, use /* comments.

  public static final Logger debug = Logger.getLogger("daikon.tools.jtb.Annotate");

  public static final String wrapXML_SWITCH = "wrap_xml";
  public static final String max_invariants_pp_SWITCH = "max_invariants_pp";
  public static final String no_reflection_SWITCH = "no_reflection";

  private static String usage =
    UtilMDE.joinLines(
      "Usage:  java daikon.tools.Annotate FILE.inv FILE.java ...",
      "  -h   Display this usage message",
      "  -i   Insert invariants not supported by ESC with \"!\" instead of \"@\";",
      "       by default these \"inexpressible\" invariants are simply omitted",
      "  -r   Use all .java files under the current directory as arguments",
      "  -s   Use // comments rather than /* comments",
      "  --format name  Insert specifications in the given format: DBC, ESC, JML, Java",
      "  --wrap_xml     Wrap each annotation and auxiliary information in XML tags",
      "  --max_invariants_pp N",
      "                 Annotate the sources with at most N invariants per program point",
      "                 (the annotated invariants will be an arbitrary subset of the",
      "                 total number of invariants for the program point).",
      "  --no_reflection",
      "                  (This is an experimental option.) Annotate uses reflection",
      "                  to determine whether a method overrides/implements another",
      "                  method. If this flag is given, Annotate will not use reflection",
      "                  to access information about an instrumented class. This means",
      "                  that in the JML and ESC formats, no \"also\" annotations",
      "                  will be inserted.",
      "  --dbg CATEGORY",
      "  --debug",
      "                  Enable one or all logger, analogously to the Daikon optin"
      );

  public static void main(String[] args) throws Exception {
    try {
      mainHelper(args);
    } catch (Daikon.TerminationMessage e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
    // Any exception other than Daikon.TerminationMessage gets propagated.
    // This simplifies debugging by showing the stack trace.
  }

  /**
   * This does the work of main, but it never calls System.exit, so it
   * is appropriate to be called progrmmatically.
   * Termination of the program with a message to the user is indicated by
   * throwing Daikon.TerminationMessage.
   * @see #main(String[])
   * @see daikon.Daikon.TerminationMessage
   **/
  public static void mainHelper(final String[] args) throws Exception {
    boolean slashslash = false;
    boolean insert_inexpressible = false;
    boolean setLightweight = true;
    boolean useReflection = true;
    int maxInvariantsPP = -1;

    Daikon.output_format = OutputFormat.ESCJAVA;
    daikon.LogHelper.setupLogs (daikon.LogHelper.INFO);
    LongOpt[] longopts = new LongOpt[] {
      new LongOpt(Daikon.help_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.debugAll_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.debug_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(Daikon.format_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(wrapXML_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(max_invariants_pp_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(no_reflection_SWITCH, LongOpt.NO_ARGUMENT, null, 0)
    };
    Getopt g = new Getopt("daikon.tools.jtb.Annotate", args, "hs", longopts);
    int c;
    while ((c = g.getopt()) != -1) {
      switch(c) {
      case 0:
        // got a long option
        String option_name = longopts[g.getLongind()].getName();

        if (Daikon.help_SWITCH.equals(option_name)) {
          System.out.println(usage);
          throw new Daikon.TerminationMessage();
        } else if (no_reflection_SWITCH.equals(option_name)) {
          useReflection = false;
        } else if (max_invariants_pp_SWITCH.equals(option_name)) {
          try {
            maxInvariantsPP = Integer.parseInt(g.getOptarg());
          } catch (NumberFormatException e) {
            System.err.println("Annotate: found the --max_invariants_pp option " +
                               "followed by an invalid numeric argument. Annotate " +
                               "will run without the option.");
            maxInvariantsPP = -1;
          }
        } else if (wrapXML_SWITCH.equals(option_name)) {
          PrintInvariants.wrap_xml = true;
        } else if (Daikon.debugAll_SWITCH.equals(option_name)) {
          Global.debugAll = true;
        } else if (Daikon.debug_SWITCH.equals(option_name)) {
          LogHelper.setLevel (g.getOptarg(), LogHelper.FINE);
        } else if (Daikon.format_SWITCH.equals(option_name)) {
          String format_name = g.getOptarg();
          Daikon.output_format = OutputFormat.get(format_name);
          if (Daikon.output_format == null) {
            throw new Daikon.TerminationMessage("Bad argument:  --format "
                                         + format_name);
          }
          if (Daikon.output_format == OutputFormat.JML) {
            setLightweight = false;
          } else {
            setLightweight = true;
          }
        } else {
          throw new Daikon.TerminationMessage("Unknown long option received: " +
                                       option_name);
        }
        break;
      case 'h':
        System.out.println(usage);
        throw new Daikon.TerminationMessage();
      case 'i':
        insert_inexpressible = true;
        break;
      // case 'r':
      //   // Should do this witout calling out to the system.  (There must be
      //   // an easy way to do this in Java.)
      //   Process p = System.exec("find . -type f -name '*.java' -print");
      //   p.waitFor();
      //   StringBufferInputStream sbis
      //   break;
      case 's':
        slashslash = true;
        break;
      case '?':
        break; // getopt() already printed an error
      default:
        System.out.println("getopt() returned " + c);
        break;
      }
    }

    // The index of the first non-option argument -- the name of the .inv file
    int argindex = g.getOptind();

    if (argindex >= args.length) {
      throw new Daikon.TerminationMessage("Error: No .inv file or .java file arguments supplied.", usage);
    }
    String invfile = args[argindex];
    argindex++;
    if (argindex >= args.length) {
      throw new Daikon.TerminationMessage("Error: No .java file arguments supplied.", usage);
    }
    PptMap ppts = FileIO.read_serialized_pptmap(new File(invfile),
                                                /*use saved config=*/true
                                                );

    Daikon.suppress_implied_controlled_invariants = true;
    Daikon.suppress_implied_postcondition_over_prestate_invariants = true;
    Daikon.suppress_redundant_invariants_with_simplify = true;

    for ( ; argindex < args.length; argindex++) {
      String javafilename = args[argindex];
      if (! (javafilename.endsWith(".java") || javafilename.endsWith(".java-random-tabs"))) {
        throw new Daikon.TerminationMessage("File does not end in .java: " + javafilename);
      }
      File outputFile = null;
      if (Daikon.output_format == OutputFormat.ESCJAVA) {
        outputFile = new File(javafilename + "-escannotated");
      } else if (Daikon.output_format == OutputFormat.JML) {
        outputFile = new File(javafilename + "-jmlannotated");
      } else if (Daikon.output_format == OutputFormat.JAVA) {
        outputFile = new File(javafilename + "-javaannotated");
      } else if (Daikon.output_format == OutputFormat.DBCJAVA) {
        outputFile = new File(javafilename + "-dbcannotated");
      }
      // outputFile.getParentFile().mkdirs();
      Writer output = new FileWriter(outputFile);

      debug.fine ("Processing file " + javafilename);

      // Annotate the file
      Reader input = null;
      try {
        input = new FileReader(javafilename);
      } catch (FileNotFoundException e) {
        throw new Error(e);
      }

      JavaParser parser = new JavaParser(input);
      Node root = null;
      try {
        root = parser.CompilationUnit();
      }
      catch (ParseException e) {
        e.printStackTrace();
        throw new Daikon.TerminationMessage("ParseException in applyVisitorInsertComments");
      }

      try {
        Ast.applyVisitorInsertComments(javafilename, root, output,
                   new AnnotateVisitor(javafilename, root, ppts, slashslash, insert_inexpressible, setLightweight, useReflection,
                                       maxInvariantsPP));
      } catch (Error e) {
        if (e.getMessage() != null && e.getMessage().startsWith("Didn't find class ")) {
          throw new Daikon.TerminationMessage(e.getMessage() + ".",
            "Be sure to compile Java classes before calling Annotate.");
        }
        throw e;
      }
    }
  }

}

// Appendix A: Jtest DBC format (taken from documentation provided by
// Jtest with their tool, version 1.x)

//                     Design by Contract for Java
//
//
//    This document describes the syntax and semantics for the "Design by
// Contract" (DbC) specification supported by Jcontract 1.X.
//
//    The "Design by Contract" contracts are expressed with Java code
// embedded in Javadoc comments in the .java source file.
//
//
// 1. Javadoc Tags for Design by Contract.
// --------------------------------------
//
//    The reserved javadoc tags for DbC are:
//
// - @invariant: specifies class invariant condition.
// - @pre: specifies method pre-condition.
// - @post: specifies method post-condition.
// - @concurrency: specifies the method concurrency.
//
// Jcontract and Jtest also support the following tags:
//
// - @throws/@exception: to document exceptions.
// - @assert: to put assertions in the method bodies.
// - @verbose: to add verbose statements to the method bodies.
//
//    Please see "appendix10.txt" for information about those tags.
//
//
// 2. Contract Syntax.
// ------------------
//
//    The general syntax for a contract is:
//
//         DbcContract:
//               DbcTag DbcCode
//             | @concurrency { concurrent | guarded | sequential }
//
// where
//
//         DbcTag:
//               @invariant
//             | @pre
//             | @post
//
//         DbcCode:
//               BooleanExpression
//             | '(' BooleanExpression ')'
//             | '(' BooleanExpression ',' MessageExpression ')'
//             | CodeBlock
//             | $none
//
//         MessageExpression:
//               Expression
//
//    Any Java code can be used in the DbcCode with the following restrictions:
//
// - The code should not have side effects, i.e. it should not have assignments
//   or invocation of methods with side-effects.
//
//    Also the following extensions to Java (DbC keywords) are allowed in the
// contract code:
//
// - $result: used in a @post contract, evaluates to the return value of the
//   method.
//
// - $pre: used in a @post contract to refer to the value of an expression at
//   @pre-time. The syntax to use it is: $pre (ExpressionType, Expression).
//   Note: the full "$pre (...)" expression should not extend over multiple lines.
//
// - $assert: can be used in DbcCode CodeBlocks to specify the contract conditions.
//   The syntax to use it is:
//     $assert (BooleanExpression) or
//     $assert (BooleanExpression , MessageExpression)
//
// - $none: to specify there is no contract.
//
//
//    Notes:
//
// - The @pre, @post and @concurrent tags apply to the method that follows in
//   the source file.
//
// - The MessageExpression is optional and will be used to identify the contract
//   in the error messages or contract violation exceptions thrown.
//   The MessageExpression can be of any type. If it is a reference type it will
//   be converted to a String using the "toString ()" method. If it is of
//   primitive type it will first be wrapped into an object.
//
// - There can be multiple conditions of the same kind for a given method.
//   If there are multiple conditions, all conditions are checked. The
//   conditions are ANDed together into one virtual condition.
//   For example it is equivalent (and encouraged for clarity) to have multiple
//   @pre conditions instead of a single big @pre condition.
//
//
// Examples:
//
//
//     /**
//       * @pre {
//       *     for (int i = 0; i < array.length; i++)
//       *         $assert (array [i] != null, "array elements are non-null");
//       * }
//       */
//
//     public void set (int[] array) {...}
//
//
//     /** @post $result == ($pre (int, arg) + 1) */
//
//     public int inc (arg) {...}
//
//
//     /** @invariant size () >= 0 */
//
//     class Stack {...}
//
//
//     /**
//      * @concurrency sequential
//      * @pre (value > 0, "value positive:" + value)
//      */
//
//     void update (int value) {...}
//
//
// 3. Contract Semantics.
// ---------------------
//
//    The contracts are specified in comments and will not have any effect if
// compiling or executing in a non DbC enhanced environment.
//
//    In a DbC enhanced environment the contracts are executed/checked when
// methods of a class with DbC contracts are invoked.  Contracts placed inside
// a method are treated like normal statements and can thus alter the flow of
// poorly structured conditional statements.  See section 6: Coding Conventions
// for details.
//
//    A contract fails if any of these conditions occur:
//
// a) The "BooleanExpression" evaluates to "false"
//
// b) An "$assert (BooleanExpression)" is called in a "CodeBlock" with an argument
//    that evaluates to "false".
//
// c) The method is called in a way that violates its @concurrency contract.
//
//    If a contract fails the Runtime Handler for the class is notified of
// the contract violation. Jcontract provides several Runtime Handlers, the
// default one uses a GUI Monitor that shows the program progress and what
// contracts are violated. The user can also write its own RuntimeHandlers,
// for more information see {isntalldir}\docs\runtime.txt.
//
//    With the Moninor Runtime Handlers provided by Jcontract program execution
// continues as if nothing had happened when a contract is violated, i.e. even
// if a @pre contract is violated, the method will still be executed.
//    This option makes the DbC enabled and non DbC enabled versions of the
// program to work in exactly the same way. The only difference is that in the
// DbC enabled version the contract violations are reported to the current
// Jcontract Monitor.
//
//
//    Note that contract evaluation is not nested, when a contract calls
// another methods, the contracts in the other method are not executed.
//
//
// 4. DbC Contracts.
// ----------------
//
//    This section gives details about each specific DbC tag.
//
//
// 4.1 @pre.
// - - - - -
//
//    Pre-conditions check that the client calls the method correctly.
//
// - Point of execution: right before calling the method.
//
// - Scope: can access anything accessible from the method scope except local
//   variables. I.e. can access method arguments, and methods/fields of the
//   class.
//
//
// 4.2 @post.
// - - - - -
//
//    Post-conditions check that the method doesn't work incorrectly.
//    Sometimes when a post-condition fails it means that the method was not
// actually supposed to accept the arguments that were passed to it. The fix in
// this case is to strengthen the @pre-condition.
//
// - Point of execution: right after the method returns successfully. Note that
//   if the method throws an exception the @post contract is not executed.
//
// - Scope: same as @pre plus can access "$result" and "$pre (type, expression)".
//
//
// 4.3 @invariant.
// - - - - - - - -
//
//    Class invariants are contracts that the objects of the class should
// always satisfy.
//
// - Point of execution: same as @pre/@post: invariant checked before checking
//   the pre-condition and after checking the post-condition.
//   Done for every non-static, non-private method entry and exit and for
//   every non-private constructor exit.
//   - Note that if a constructor throws an exception its @post contract is not
//     executed.
//   - Not done for "finalize ()".
//   - When inner class methods are executed, the invariants of the outer classes
//     are not checked.
//
// - Scope: class scope, can access anything a method in the class can access,
//   except local variables.
//
//
// 4.4 @concurrency.
// - - - - - - - - -
//
//    The @concurrency tag specifies how the method can be called by multiple
// threads. Its possible values are:
//
// a) concurrent: the method can be called simultaneously by different threads.
//    I.e. the method is multi-thread safe. Note that this is the default
//    mode for Java methods.
//
// b) guarded: the method can be called simultaneously by different threads,
//    but only one will execute it in turn, while the other threads will
//    wait for the executing one to finish.
//    I.e. it specifies that the method is synchronized.
//    Jcontract will just give a compile-time error if a method is declared
//    as "guarded" but is not declared as "synchronized".
//
// c) sequential: the method can only by executed by one thread at once and it
//    is not declared synchronized. It is thus the responsibility of the callers
//    to ensure that no simultaneous calls to that method occur. For methods with
//    this concurrency contract Jcontract will generate code to check if they are
//    being executed by more than one thread at once. An error will be reported
//    at runtime if the contract is violated.
//
// - Point of execution: right before calling the method.
//
//
// 5. Inheritance.
// --------------
//
//    Contracts are inherited. If the derived class or overriding method doesn't
// define a contract, it inherits that of the super class or interface.
//    Note that a contract of $none implies that the super contract is applied.
//
//    If an overriding method does define a contract then it can only:
//
// - Weaken the precondition: because it should at least accept the same input as
//   the parent, but it can also accept more.
//
// - Strengthen the postcondition: because it should at least do as much as the
//   parent one, but it can also do more.
//
//    To enforce this:
//
// - When checking the @pre condition, the pre-condition contract is assumed to
//   succeed if any of the @pre conditions of the chain of overridded methods
//   succeeds. I.e. the preconditions are ORed.
//
// - When checking the @post condition, the post-condition contract is assumed
//   to succeed if all the @post conditions of the chain of overridded methods
//   succeed. I.e. the postconditions are ANDed.
//
//    Note that if there are multiple @pre conditions for a given method, the
// preconditions are ANDed together into one virtual @pre condition and then
// ORed with the virtual @pre conditions for the other methods in the chain
// of overridden methods.
//
//    For @invariant conditions the same logic as for @post applies.
//
//    @concurrency contracts are also inherited. If the overriding method doesn't
// have a @concurrency contract it inherits that of the parent. If it has an
// inheritance contract it can only weaken it, like for @pre conditions. For
// example if the parent has a "sequential" @concurrency, the overriding method
// can have a "guarded" or "concurrent" @concurrency.
//
// 6. Special Keyword and Functions
//
// 6.1 Keywords
//
// $implies
//
//     You can use the '$implies' keyword to compare two boolean expressions,
//     ensuring that when the first expression is true, the second one also
//     is true.
//
//     Example: a $implies b
//     This is equivalent to '!a || b'.
//
// 6.2 Collection functions
//
// Class and method contracts can contain a few special functions that are
// internally mapped to equivalent Java code.
//
// These are:
//
// public boolean Collection.$forall(Type t; <boolean expression>)
//
//     <boolean expression> is an expression that will be evaluated for all
//     elements in the collection, with each element of type "Type", using
//     an element named "t". The value of $forall is true when for all elements
//     the expression evaluates to true.
//
//     Think of this as a modified 'for' statement.
//
//     Example:
//     /** @pre names.$forall (String s; s.length() != 0) */
//     void method (List names) { }
//
//     Since $forall generates a boolean value, you can use it with assert
//     in a block precondition:
//
//     /** @pre {
//             $assert (names != null);
//             $assert (names.$forall (String e; e.length() > 1));
//         }
//      */
//
// public boolean Collection.$exists(Type t; <boolean expression>)
//
//     This is almost identical in structure to $forall, but it succeeds if any
//     of the elements in the collection cause the expression to evaluate to true.
//
//     Example:
//     /** @pre names.$exists (String s; s != null && "seth".equals (s)) */
//     void method (List names) { }
//
// 7. Coding Conventions.
// ---------------------
//
//    When using Design by Contract in Java, the following coding conventions
// are recommended:
//
// 1) Place all the @invariant conditions in the class javadoc comment, the
//    javadoc comment appearing immediately before the class definition.
//
// 2) It is recommended that the Javadoc comments with the @invariant tag appear
//    before the class definition.
//
// 3) All public and protected methods should have a contract. It is recommended
//    that all package-private and private methods also have a contract.
//
// 4) If a method has a DbC tag it should have a complete contract. This means
//    that is should have both a precondition and a postcondition.
//    One should use "DbcTag $none" to specify that a method doesn't have any
//    condition for that tag.
//
// 5) No public class field should participate in an @invariant clause. Since any
//    client can modify such a field arbitrarily, there is no way for the class to
//    ensure any invariant on it.
//
// 6) The code contracts should only access members visible from the interface.
//    For example the code in the @pre condition of a method should only access
//    members that are accessible from any client that could use the method. I.e.
//    the contract of a public method should only use public members from the
//    method's class.
//
// 7) Contracts that appear in the body of a method should be thought of as
//    normal statements.  Thus the following example would have undesireable
//    behavior:
//
//        if (a) /** @assert (a) */
//            System.out.println("poor use of contract code");
//
//    Because the assert is a normal statement when your code is compiled by
//    dbc_javac, this example is equivalent to the following pseudocode:
//
//        if (a)
//            assert (a);
//        System.out.println("poor use of contract code");
//
//    which makes it clear that the assertion is the body of the "if".
//
//    The current version of Jcontract doesn't enforce these coding conventions.
