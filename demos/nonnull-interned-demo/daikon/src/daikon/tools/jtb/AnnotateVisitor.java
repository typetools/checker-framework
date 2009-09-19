// See Annotate for documentation of behavior.

package daikon.tools.jtb;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import jtb.syntaxtree.*;
import jtb.visitor.*;
import daikon.*;
import utilMDE.Assert;
import utilMDE.ArraysMDE;
import utilMDE.MultiReader;
import utilMDE.UtilMDE;
import daikon.inv.Invariant;
import daikon.inv.OutputFormat;
import daikon.inv.unary.sequence.EltNonZero;
import daikon.inv.unary.stringsequence.EltOneOfString;
import daikon.inv.unary.stringsequence.OneOfStringSequence;
import daikon.chicory.DaikonVariableInfo;


// For each class:  (UnmodifiedClassDeclaration)
//  * insert object invariants
//  * insert owner assertions for fields
// For each field:
//  * add spec_public
// For each method:
//  * add preconditions
//  * add postconditions (including exsures)
//  * add "modifies"
//  * all of these may be prefixed by "also_"
// For each field assignment:
//  * add owner annotations

// The overall strategy is to read the Java file into a list of strings
// (javaFileLines) and then to manipulate that list directly.  It doesn't
// insert the comments into the JTB tree.

// TODO:  Unlike InsertCommentFormatter, this does not keep track of column
// shifts.  (It does handle line shifts.)  This needs to be fixed.

public class AnnotateVisitor extends DepthFirstVisitor {

  private static final boolean debug = false;

  private static final String lineSep = System.getProperty("line.separator");

  public static final String JML_START_COMMENT = "/*@" + lineSep;
  public static final String JML_END_COMMENT = "@*/" + lineSep;

  public List<String> javaFileLines;

  public PptMap ppts;
  /** if true, use "//" comments; if false, use "/*" comments. **/
  public boolean slashslash;
  /** If true, insert annotations not supported by ESC. **/
  public boolean insert_inexpressible;
  /** If false, use full JML specs; if true, use lightweight ESC specs. **/
  public boolean lightweight;

  /**
   * Whether to use reflection when trying to figure out if a method
   * overrides/implements another method. If this variable is set to
   * false, then Annotate will not try to determine if a method
   * overrides/implements another method, which means that it will not
   * try to add "also" tags to its output.
   **/
  public boolean useReflection;

  /**
   * If the --max_invariants_pp option is given, this variable is set
   * to the maximum number of invariants output by annotate per program point.
   **/
  public int maxInvariantsPP;


  public Vector<NodeToken> addedComments = new Vector<NodeToken>();

  private Stack<ClassFieldInfo> cfis = new Stack<ClassFieldInfo>();

  private PptNameMatcher pptMatcher;

  public AnnotateVisitor(String javafilename,
                         Node root,
                         PptMap ppts,
                         boolean slashslash,
                         boolean insert_inexpressible,
                         boolean lightweight,
                         boolean useReflection,
                         int maxInvariantsPP) {

    this.pptMatcher = new PptNameMatcher(root);

    // Read the Java file into a list of Strings.
    this.javaFileLines = new ArrayList<String>();
    try {
      for (String line : new MultiReader(javafilename)) {
        this.javaFileLines.add(line);
      }
    } catch (IOException e) {
      throw new Error(e);
    }

    this.ppts = ppts;
    this.slashslash = slashslash;
    this.insert_inexpressible = insert_inexpressible;
    this.lightweight = lightweight;
    this.useReflection = useReflection;
    this.maxInvariantsPP = maxInvariantsPP;
  }


  // Like Ast.addComment, but also keeps a list of what comments were added.
  void addComment(Node n, String comment, boolean first) {
    NodeToken nt = new NodeToken(comment);
    Ast.findLineAndCol(n, nt, first);
    addedComments.add(nt);
    if (debug) {
      System.out.printf("addComment at %d:%d : %s%n",
                        nt.beginLine, nt.beginColumn, comment.trim());
    }
    int linenumber = nt.beginLine - 1; /* because in jtb lines start at 1 */
    String lineString = javaFileLines.get(linenumber);
    int column = getTabbedIndex(nt.beginColumn - 1, /* because in jtb cols start at 1 */
                                lineString);
    StringBuffer sb = new StringBuffer();
    sb.append(lineString.substring(0, column));
    sb.append(comment);
    if (comment.endsWith("\n")) {
      // Insert the whitespace preceding the line
      sb.append(precedingWhitespace(lineString.substring(0, column)));
    }
    sb.append(lineString.substring(column));
    javaFileLines.set(linenumber, sb.toString());
    if (debug) { System.out.printf("addComment result: <<<%s>>>%n", sb.toString()); }
  }

  // Like Ast.addComment, but also keeps a list of what comments were added.
  void addComment(Node n, String comment) {
    addComment(n, comment, false);
  }

  // Like Ast.addComment, but also keeps a list of what comments were added.
  void addCommentAfter(Node n, String comment) {
    addComment(Ast.nodeTokenAfter(n), comment, true);
  }

  private boolean isOwned(String fieldname) {
    for (ClassFieldInfo cfi : cfis) {
      if (cfi.ownedFieldNames.contains(fieldname)) {
        return true;
      }
    }
    return false;
  }

  private boolean isFinal(String fieldname) {
    for (ClassFieldInfo cfi : cfis) {
      if (cfi.finalFieldNames.contains(fieldname)) {
        return true;
      }
    }
    return false;
  }

  private boolean isNotContainsNull(String fieldname) {
    for (ClassFieldInfo cfi : cfis) {
      if (cfi.notContainsNullFieldNames.contains(fieldname)) {
        return true;
      }
    }
    return false;
  }

  private boolean isElementType(String fieldname) {
    for (ClassFieldInfo cfi : cfis) {
      if (cfi.elementTypeFieldNames.containsKey(fieldname)) {
        return true;
      }
    }
    return false;
  }

  private String elementType(String fieldname) {
    for (ClassFieldInfo cfi : cfis) {
      String result = cfi.elementTypeFieldNames.get(fieldname);
      if (result != null) {
        return result;
      }
    }
    assert false;
    return null;
  }

  // ClassDeclaration is a top-level (non-nested) construct.  Collect all
  // the fields in this and any nested class, so that we can recognize
  // assignments to them later on.
  /**
   * f0 -> ( "abstract" | "final" | "public" )*
   * f1 -> UnmodifiedClassDeclaration()
   */
//   public void visit(ClassOrInterfaceDeclaration n) {
//     super.visit(n);             // call "accept(this)" on each field
//   }

  /**
   * f0 -> ( "static" | "abstract" | "final" | "public" | "protected" | "private" )*
   * f1 -> UnmodifiedClassDeclaration()
   */
//   public void visit(NestedClassDeclaration n) {
//     super.visit(n);             // call "accept(this)" on each field
//   }

  class ClassFieldInfo {
    // List<FieldDeclaration> fieldDecls;
    List<String> allFieldNames;
    List<String> ownedFieldNames;
    List<String> finalFieldNames;
    List<String> notContainsNullFieldNames;
    Map<String,String> elementTypeFieldNames;
  }


  // Insert object invariants for this class.
  // Insert owner assertions for fields.
/**
 * Grammar production:
 * f0 -> ( "class" | "interface" )
 * f1 -> <IDENTIFIER>
 * f2 -> [ TypeParameters() ]
 * f3 -> [ ExtendsList(isInterface) ]
 * f4 -> [ ImplementsList(isInterface) ]
 * f5 -> ClassOrInterfaceBody(isInterface)
 */
  public void visit(ClassOrInterfaceDeclaration n) {
    String classname = Ast.getClassName(n);
    String pptname = classname + ":::OBJECT";
    PptTopLevel object_ppt = ppts.get(pptname);
    if (object_ppt == null) {
      pptname = classname + ":::CLASS";
      object_ppt = ppts.get(pptname); // might *still* be null; we'll check later
    }

    ClassFieldInfo cfi = new ClassFieldInfo();
    { // set fieldNames slots
      CollectFieldsVisitor cfv = new CollectFieldsVisitor(n, false);
      cfi.ownedFieldNames = cfv.ownedFieldNames();
      cfi.finalFieldNames = cfv.finalFieldNames();
      if (object_ppt == null) {
        cfi.notContainsNullFieldNames = new ArrayList<String>();
        cfi.elementTypeFieldNames = new HashMap<String,String>();
      } else {
        cfi.notContainsNullFieldNames = not_contains_null_fields(object_ppt, cfv.allFieldNames());
        cfi.elementTypeFieldNames = element_type_fields(object_ppt, cfv.allFieldNames());
      }
      cfis.push(cfi);
    }

    super.visit(n);             // call "accept(this)" on each field

    if (lightweight && (Daikon.output_format != OutputFormat.DBCJAVA)) {
      for (int i=cfi.ownedFieldNames.size()-1; i>=0; i--) {
        addComment(n.f5.f1, javaLineComment("@ invariant" + " " + cfi.ownedFieldNames.get(i) + ".owner == this;"), true);
      }
    }
    if (object_ppt == null) {
      if (debug) { System.out.println("No object program point found for " + classname); }
    } else {
      InvariantsAndModifiedVars obj_invs = invariants_for(object_ppt, ppts);
      String inv_tag = (Daikon.output_format == OutputFormat.DBCJAVA ? "@invariant" : "invariant");
      insertInvariants(n.f5.f1, inv_tag, obj_invs);
    }

    cfis.pop();
  }

  // Given that this method works not on FieldDeclaration, but its grandparent,
  // it should likely be moved to a method with a different formal parameter.
  public void visit(FieldDeclaration n) {
    super.visit(n);             // call "accept(this)" on each field

    // Nothing to do for Jtest DBCJAVA format
    if (Daikon.output_format == OutputFormat.DBCJAVA) { return; }

    if (! Ast.contains(n.getParent().getParent(), "public")) {

//       n.accept(new TreeFormatter());
//       String nString = Ast.format(n);
//       System.out.println("@@@");
//       Node n2 = n.getParent().getParent(); n2.accept(new TreeFormatter());
//       System.out.println("@@@" + Ast.format(n2));
      addComment(n.getParent().getParent(), "/*@ spec_public */ ");
    }
  }

  // Node n is a MethodDeclaration or a ConstructorDeclaration
  InvariantsAndModifiedVars[] get_requires_and_ensures(PptMap ppts, Node n) {
    InvariantsAndModifiedVars requires_invs = null;
    InvariantsAndModifiedVars ensures_invs = null;

    List<PptTopLevel> matching_ppts = null;
    if (n instanceof MethodDeclaration) {
      matching_ppts = pptMatcher.getMatches(ppts, (MethodDeclaration)n);
    } else if (n instanceof ConstructorDeclaration) {
      matching_ppts = pptMatcher.getMatches(ppts, (ConstructorDeclaration)n);
    } else {
      throw new Error("Node must be MethodDeclaration or ConstructorDeclaration");
    }

    for (PptTopLevel ppt : matching_ppts) {
      String prefix;
      if (ppt.ppt_name.isEnterPoint()) {
        requires_invs = invariants_for(ppt, ppts);
      } else if (ppt.ppt_name.isExitPoint()) {
        if (! ppt.ppt_name.isCombinedExitPoint()) {
          continue;
        }
        ensures_invs = invariants_for(ppt, ppts);
      }
    }

    return new InvariantsAndModifiedVars[] { requires_invs, ensures_invs };
  }


  public void insertAlso(Node n) {
    addComment(n, "@ also" + lineSep, true);
  }

  // n must be a MethodDeclaration or ConstructorDeclaration
  public void insertBehavior(Node n) {
    class InsertBehaviorVisitor extends DepthFirstVisitor {
      Node n;
      boolean behaviorInserted;

      public InsertBehaviorVisitor(Node n) {
        super();
        this.n = n;
        behaviorInserted = false;
      }

      private String getBehaviorString() {
        return "normal_behavior";
      }

      public void visit(NodeChoice nc) {
        //System.out.println("InsertBehavior visitor visiting a NodeChoice");
         String modifier = (nc != null && nc.choice != null ? nc.choice.toString() : "");
         //System.out.println("A node choice here: " + modifier);
//          StringWriter sb = new StringWriter();
//          TreeDumper d = new TreeDumper(sb);

//          if (n instanceof MethodDeclaration)
//            d.visit((MethodDeclaration)n);
//          else
//            d.visit((ConstructorDeclaration)n);

//          System.out.println("Tree dump of n: " + sb.toString());
        if (Ast.isAccessModifier(modifier)) {
          addComment(n.getParent().getParent(), "@ " + modifier + " " + getBehaviorString() +
                     (Daikon.output_format == OutputFormat.JML
                      ? " // Generated by Daikon"
                      : "") +
                     lineSep, true);
          behaviorInserted = true;
        }
      }

      public void visit(NodeOptional no) {
        //System.out.println("InsertBehavior visitor visiting a NodeOptional");
        visit((NodeChoice)no.node);
      }

      public void visit(NodeListOptional nlo) {
        //System.out.println("InsertBehavior visitor visiting a NodeListOptional");
        //System.out.println("With " + nlo.nodes.size() + " nodes");
        for (int i=0; i<nlo.nodes.size() && !behaviorInserted; i++) {
          //System.out.println("Visiting a NodeChoice");

          // The other way around (that is, ((NodeChoice)nlo...).accept(this))
          // does not work because of the way NodeChoice.accept is defined
          visit((NodeChoice)nlo.nodes.get(i));
        }
        if (!behaviorInserted)
          addComment(n.getParent().getParent(), "@ " + "private " + getBehaviorString() +
                     (Daikon.output_format == OutputFormat.JML
                      ? " // Generated by Daikon"
                      : "") +
                     lineSep, true);
      }

      public void visit(MethodDeclaration md) {
        // System.out.println("InsertBehavior visitor visiting a MethodDeclaration");
        md.f0.accept(this);
      }

      public void visit(ConstructorDeclaration cd) {
        // System.out.println("InsertBehavior visitor visiting a ConstructorDeclaration");
        cd.f0.accept(this);
      }
    }

    InsertBehaviorVisitor v = new InsertBehaviorVisitor(n);
    if (n instanceof MethodDeclaration)
      ((MethodDeclaration)n).getParent().getParent().accept(v);
    else
      ((ConstructorDeclaration)n).getParent().getParent().accept(v);
  }


  public void visit(MethodDeclaration n) {

/**
 * Grammar production for ClassOrInterfaceBodyDeclaration:
 * f0 -> Initializer()
 *       | Modifiers() ( ClassOrInterfaceDeclaration(modifiers) | EnumDeclaration(modifiers) | ConstructorDeclaration() | FieldDeclaration(modifiers) | MethodDeclaration(modifiers) )
 *       | ";"
 */


    super.visit(n);             // call "accept(this)" on each field


    InvariantsAndModifiedVars[] requires_and_ensures = get_requires_and_ensures(ppts, n);

    InvariantsAndModifiedVars requires_invs = requires_and_ensures[0];
    InvariantsAndModifiedVars ensures_invs = requires_and_ensures[1];

    String ensures_tag = Daikon.output_format.ensures_tag();
    String requires_tag = Daikon.output_format.requires_tag();

    boolean isOverride = false;
    boolean isImplementation = false;
    if (useReflection) {
      isOverride = Ast.isOverride(n); // of a superclass
      isImplementation = Ast.isImplementation(n); // of an interface
    }

    if (lightweight) {
      if (isImplementation) {
        requires_tag = "also_" + requires_tag;
        ensures_tag = "also_" + ensures_tag;
      }
      if (isOverride) {
        requires_invs = null; // no requires permitted on overridden methods
        ensures_tag = "also_" + ensures_tag;
      }
    }

    if (!lightweight)
      addComment(n.getParent().getParent() /* see  ClassOrInterfaceBodyDeclaration */, JML_END_COMMENT, true);

    boolean invariantInserted =
      insertInvariants(n.getParent().getParent() /* see  ClassOrInterfaceBodyDeclaration */, ensures_tag, ensures_invs, lightweight);

          if (ensures_invs != null) {
            insertModifies(n.getParent().getParent() /* see  ClassOrInterfaceBodyDeclaration */, ensures_invs.modifiedVars, ensures_tag, lightweight);
          }

    invariantInserted =
      insertInvariants(n.getParent().getParent() /* see  ClassOrInterfaceBodyDeclaration */, requires_tag, requires_invs, lightweight) ||
      invariantInserted;

    if (!lightweight) {
      if (!invariantInserted) {
        insertJMLWorkaround(n.getParent().getParent() /* see  ClassOrInterfaceBodyDeclaration */);
      }
      insertBehavior(n);
      if (isImplementation || isOverride
          // temporary fix: not processed correctly by Ast.java
          || n.f2.f0.toString().equals("clone")) {
        insertAlso(n.getParent().getParent());
      }
      addComment(n.getParent().getParent() /* see  ClassOrInterfaceBodyDeclaration */, JML_START_COMMENT, true);
    }
  }

/**
 * Grammar production:
 * f0 -> [ TypeParameters() ]
 * f1 -> <IDENTIFIER>
 * f2 -> FormalParameters()
 * f3 -> [ "throws" NameList() ]
 * f4 -> "{"
 * f5 -> [ ExplicitConstructorInvocation() ]
 * f6 -> ( BlockStatement() )*
 * f7 -> "}"
 */
  public void visit(ConstructorDeclaration n) {
    // System.out.println("ConstructorDeclaration: " + n.f1);

    super.visit(n);             // call "accept(this)" on each field

    InvariantsAndModifiedVars[] requires_and_ensures = get_requires_and_ensures(ppts, n);
    InvariantsAndModifiedVars requires_invs = requires_and_ensures[0];
    InvariantsAndModifiedVars ensures_invs = requires_and_ensures[1];

    String ensures_tag = Daikon.output_format.ensures_tag();
    String requires_tag = Daikon.output_format.requires_tag();

    if (!lightweight) {
      addComment(n.getParent().getParent() /* see  ClassOrInterfaceBodyDeclaration */, JML_END_COMMENT, true);
    }

    boolean invariantInserted =
      insertInvariants(n.getParent().getParent() /* see  ClassOrInterfaceBodyDeclaration */,
                       ensures_tag, ensures_invs, lightweight);

    if (ensures_invs != null) {
      insertModifies(n.getParent().getParent() /* see  ClassOrInterfaceBodyDeclaration */,
                     ensures_invs.modifiedVars, ensures_tag, lightweight);
    }

    invariantInserted =
      insertInvariants(n.getParent().getParent() /* see  ClassOrInterfaceBodyDeclaration */,
                       requires_tag, requires_invs, lightweight) ||
      invariantInserted;

    if (!lightweight) {
      if (!invariantInserted) {
        insertJMLWorkaround(n.getParent().getParent() /* see  ClassOrInterfaceBodyDeclaration */);
      }
      insertBehavior(n);
      addComment(n.getParent().getParent() /* see  ClassOrInterfaceBodyDeclaration */, JML_START_COMMENT, true);
    }

  }

  // This is a hack that indicates whether an "assignable \everything"
  // clause would be legal for the given method.
  public boolean pureInJML(Node n) {
    String name = null;
    if (n instanceof MethodDeclaration) {
      name = ((MethodDeclaration)n).f2.f0.toString();
      if (name.equals("clone") ||
          name.equals("equals") ||
          name.equals("hashCode") ||
          name.equals("hasMoreElements") ||
          name.equals("hasNext")) {
        return true;
      }
    }
    return false;
  }

  // This seems to get inserted when nothing else does.  I guess it works
  // around some problem with JML output.
  public void insertJMLWorkaround(Node n) {
    addComment(n, "@ requires true;" + lineSep, true);
  }

  public boolean insertInvariants(Node n, String prefix, InvariantsAndModifiedVars invs) {
    return insertInvariants(n, prefix, invs, true);
  }

  public boolean insertModifies(Node n, String modifiesString, String prefix, boolean useJavaComment) {

    modifiesString = modifiesString.trim();

    if (modifiesString.equals("modifies") || modifiesString.equals("assignable")) {
      // No variables found.
      return false;
    }

    if (!(modifiesString.startsWith("modifies") || modifiesString.startsWith("assignable"))) {
      // Doesn't look ilke a modifies clause.
      return false;
    }

    String inv = null;
    boolean doInsert = true;

    if (Daikon.output_format == OutputFormat.JML) {
      // Currently, AnnotateVisitor currently does not produce sensible
      // "assignable" clauses for JML.
      //  * One possible default is "assignable \everything".  However, that
      //    is incorrect when the method is pure (that is, it overrides a
      //    method that is annotated as pure, such as hashCode).
      //  * Another possible default is to omit the assignable clause
      //    entirely.  Since "assignable \everything" is the JML default, this
      //    omission strategy works both when the method is pure and when it
      //    is not.  Thus, we use this strategy.  Thanks to Christoph
      //    Csnallner for this idea.
      // In the future, AnnotateVisitor should produce "assignable" clauses.
      doInsert = false;
    } else if (Daikon.output_format == OutputFormat.DBCJAVA) {
      // Modifies/assignable has no translation in Jtest DBC
      doInsert = false;
    } else if (lightweight && prefix.startsWith("also_")) {
      inv = "also_" + modifiesString;
    } else {
      inv = modifiesString;
    }

    if (!doInsert) {
      return false;
    }

    String commentContents = "@ " + inv + ";";
    if (useJavaComment) {
      commentContents = javaLineComment(commentContents);
    } else {
      commentContents += lineSep;
    }
    addComment(n, commentContents, true);
    return true;
  }

  // The "invs" argument may be null, in which case no work is done.
  public boolean insertInvariants(Node n, String prefix, InvariantsAndModifiedVars invs,
                                  boolean useJavaComment) {
    if (invs == null) {
      return false;
    }

    boolean invariantInserted = false;
    boolean assignableInserted = false;

    int maxIndex = invs.invariants.size();
    if (maxInvariantsPP > 0) {
      maxIndex = Math.min(maxIndex, maxInvariantsPP);
    }

    for (int i = maxIndex-1 ; i >= 0 ; i--) {
      Invariant inv = invs.invariants.get(i);
      if (! inv.isValidExpression(Daikon.output_format)) {
        // inexpressible invariant
        if (insert_inexpressible) {
          addComment(n, javaLineComment("! " + inv + ";"), true);
        }
        continue;
      } else {
        String commentContents = (Daikon.output_format == OutputFormat.DBCJAVA ? "  " : "@ ")
          + prefix + " " + inv.format_using(Daikon.output_format)
          + (Daikon.output_format == OutputFormat.DBCJAVA ? "  " : ";");
        if (useJavaComment)
          commentContents = javaLineComment(commentContents);
        else
          commentContents += lineSep;

        addComment(n, commentContents, true);
        invariantInserted = true;
      }
    }

    return invariantInserted;
  }

  // Set .owner and/or .containsnull for ++, --, etc expressions within a
  // statement.
  /**
   * f0 -> PreIncrementExpression()
   *       | PreDecrementExpression()
   *       | PrimaryExpression() [ "++" | "--" | AssignmentOperator() Expression() ]
   */
  public void visit(StatementExpression n) {
    super.visit(n);             // call "accept(this)" on each field

    // System.out.println("Found a statement expression: " + n.f0.choice);

    // Nothing to do for Jtest DBC format
    if (Daikon.output_format == OutputFormat.DBCJAVA) { return; }

    if (n.f0.choice instanceof NodeSequence) {
      NodeSequence ns = (NodeSequence) n.f0.choice;
      PrimaryExpression pe = (PrimaryExpression) ns.elementAt(0);
      // for (int i=0; i<ns.size(); i++) {
      //   System.out.println("ns #" + i + ": " + ns.elementAt(i));
      // }
      if (ns.size() == 2) {
        NodeOptional no = (NodeOptional) ns.elementAt(1);
        NodeChoice nc = (NodeChoice) no.node;
        if ((nc != null) && (nc.choice instanceof NodeSequence)) {
          // It's an assignment.

          // Don't take action unless the PrimaryExpression is a simple
          // Name (that's effectively checked below) and has no
          // PrimarySuffix or else its prefix is "this" (check that here).
          String fieldname = null;
          if (pe.f1.size() == 0) {
            fieldname = Ast.fieldName(pe);
          } else if (pe.f1.size() == 1) {
            if (pe.f0.f0.which == 1) { // prefix is "this"
              PrimarySuffix ps = (PrimarySuffix) pe.f1.elementAt(0);
              if (ps.f0.which == 3) { // suffix is an identifier
                NodeSequence ns2 = (NodeSequence) ps.f0.choice;
                fieldname = ((NodeToken) ns2.elementAt(1)).tokenImage;
              }
            }
          }

          // System.out.printf("In statement, fieldname = %s", fieldname);
          if ((fieldname != null)
              && (isOwned(fieldname)
                  || isNotContainsNull(fieldname)
                  || isElementType(fieldname))) {
            ConstructorDeclaration cd
              = (ConstructorDeclaration) Ast.getParent(ConstructorDeclaration.class, n);
            MethodDeclaration md
              = (MethodDeclaration) Ast.getParent(MethodDeclaration.class, n);
            if ((cd != null)
                || ((md != null) && (! Ast.contains(md.f0, "static")))) {
              Node parent = Ast.getParent(Statement.class, n);
              // If parent isn't in a block (eg, if parent
              // is sole element in then or else clause), then this is wrong.
              // It's safe, however.  But does it cause syntax errors if an
              // else clause follows a then clause without braces?
              if (isOwned(fieldname)) {
                if (lightweight)
                  addCommentAfter(parent, javaLineComment("@ set " + fieldname + ".owner = this;"));
              }
              if (isNotContainsNull(fieldname)) {
                addCommentAfter(parent, javaLineComment("@ set " + fieldname + ".containsNull = false;"));
              }
              if (isElementType(fieldname)) {
                addCommentAfter(parent, javaLineComment("@ set " + fieldname + ".elementType = " + elementType(fieldname) + ";"));
              }
            }
          }
        }
      }
    }
  }

  // This is an assignment exactly if field f1 is present.
  /**
   * f0 -> ConditionalExpression()
   * f1 -> [ AssignmentOperator() Expression() ]
   */
  public void visit(Expression n) {
    super.visit(n);             // call "accept(this)" on each field

    if (n.f1.present()) {
      // it's an assignment
      // System.out.println("found an assignment expression: " + n);
      PrimaryExpression pe = Ast.assignment2primaryexpression(n);
      String fieldname = Ast.fieldName(pe);
      // System.out.println("In expression, fieldname = " + fieldname);
      Node stmt = Ast.getParent(Statement.class, n);
      if ((fieldname != null) && isOwned(fieldname)) {
        if (lightweight)
          addCommentAfter(stmt, javaLineComment("@ set " + fieldname + ".owner = this;"));
      }

    }

  }


  ///////////////////////////////////////////////////////////////////////////
  /// Subroutines
  ///

  /** The argument should already contain "@" or any other leading characters. */
  String javaLineComment(String comment) {
    if (slashslash) {
      return "//" + comment + lineSep;
    } else {
      return "/*" + comment + " */" + lineSep;
    }
  }

  /** The argument should already contain "@" or any other leading characters. */
  String javaComment(String comment) {
    return "/*" + comment + "*/";
  }


  /**
   * Find a Daikon variable for the given field.  The variable is either
   * "this.field" (for instance variables) or "ClassName.field" (for static
   * variables).
   **/
  private VarInfo findVar(String field, PptTopLevel ppt) {
    String varname = "this." + field;
    VarInfo vi = ppt.find_var_by_name (varname);
    if (vi == null) {
      // No "this.field" variable, so try (static) "ClassName.field".
      varname = ppt.ppt_name.getFullClassName() + "." + field;
      vi = ppt.find_var_by_name (varname);
    }
    if (vi == null) {
      // We found a variable in the source code that is not computed by
      // Daikon.  (This can happen if Daikon was run omitting the
      // variable, for instance due to --std-visibility.  But I want to
      // throw an error instead, since I expect bugs to be more common
      // than such problems.)
      debug_field_problem(field, ppt);
      throw new Error("Warning: Annotate: Daikon knows nothing about variable " + varname + " at " + ppt);
    }
    return vi;
  }


  private void debug_field_problem(String field, PptTopLevel ppt) {
    System.out.println("Warning: Annotate: Daikon knows nothing about field " + field + " at " + ppt);
    System.out.println("  ppt.var_infos:");
    for (VarInfo pptvi : ppt.var_infos) {
      System.out.println("    " + pptvi.name());
    }
  }


  // Returns a list of fields with ".containsNull == false" invariants.
  // ppt is an :::OBJECT or :::CLASS program point.
  List<String> not_contains_null_fields(PptTopLevel ppt, List<String> allFieldNames) {
    // System.out.println("not_contains_null_fields(" + ppt + ")");
    List<String> result = new ArrayList<String>();
    for (String field : allFieldNames) {
      // System.out.println("field: " + field);
      VarInfo vi = findVar(field, ppt);
      PptSlice1 slice = ppt.findSlice(vi);
      if (slice == null) {
        // There might be no slice if there are no invariants over this var.
        continue;
      }
      EltNonZero enz = EltNonZero.find(slice);
      if (enz != null) {
        String enz_format = format((Invariant)enz);
        if (enz_format.endsWith(".containsNull == false")) {
          result.add(field);
        }
      }
    }
    return result;
  }

  // Returns a HashMap for fields with ".elementType == \type(...)" invariants,
  // mapping the field to the type.
  // ppt is an :::OBJECT or :::CLASS program point.
  HashMap<String,String> element_type_fields(PptTopLevel ppt, List<String> allFieldNames) {
    // System.out.println("element_type_fields(" + ppt + ")");
    HashMap<String,String> result = new HashMap<String,String>();
    // FieldDeclaration[] fdecls = cfv.fieldDeclarations();
    // System.out.println("fields: " + ArraysMDE.toString(fields));
    for (String field : allFieldNames) {
      // System.out.printf("element_type_fields (%s) field: %s%n", ppt, field);
      VarInfo vi = findVar(field, ppt);
      if (! (vi.type.isArray() || vi.type.isPseudoArray())) {
        continue;
      }
      if (vi.type.elementType().isPrimitive()) {
        continue;
      }
      String varname = vi.name();
      String elt_varname = varname + "[]";
      VarInfo elt_vi = ppt.find_var_by_name (elt_varname);
      if (elt_vi == null) {
        debug_field_problem(elt_varname, ppt);
      }
      assert ppt.find_var_by_name (elt_varname) != null
        : "Annotate: Daikon knows nothing about variable " + elt_varname + " at " + ppt;
      // et_varname variable represents the types of the elements.
      String et_varname = elt_varname + DaikonVariableInfo.class_suffix;
      // System.out.printf("Found %s, seeking %s%n", elt_varname, et_varname);
      // We found variable "a[]".  Now find "a[].getClass()".
      // (Why might it not be present?)
      // It might not be present if DaikonVariableInfo.shouldAddRuntimeClass would return false.
      assert elt_vi.rep_type.isArray();
      if (elt_vi.rep_type.dimensions() == 1 && elt_vi.rep_type.baseIsPrimitive()) {
        continue;
      }
      VarInfo et_vi = ppt.find_var_by_name (et_varname);
      if (et_vi == null) {
        debug_field_problem(et_varname, ppt);
      }
      assert et_vi != null
        : "Annotate: Daikon knows nothing about variable " + et_varname + " at " + ppt + "\n  with allFieldNames = " + allFieldNames;

      // et_vi != null
      PptSlice1 slice = ppt.findSlice(et_vi);
      if (slice == null) {
        // There might be no slice if there are no invariants over this var.
        continue;
      }
      // System.out.println("Found slice for " + et_vi.name.name());
      {
        EltOneOfString eoos = EltOneOfString.find(slice);
        // System.out.println("eoos: " + (eoos == null ? "null" : format((Invariant)eoos)));
        if ((eoos != null) && (eoos.num_elts() == 1)) {
          String eoos_format = format((Invariant)eoos);
          int et_pos = eoos_format.indexOf(".elementType == \\type(");
          if (et_pos != -1) {
            String type = eoos_format.substring(et_pos + ".elementType == ".length());
            result.put(field, type);
          }
        }
      }
      {
        OneOfStringSequence eooss = OneOfStringSequence.find(slice);
        // System.out.println("eooss: " + (eooss == null ? "null" : format((Invariant)eooss)));
        if (eooss != null) {
          String eooss_format = format((Invariant)eooss);
          int et_pos = eooss_format.indexOf(".elementType == \\type(");
          if (et_pos != -1) {
            String type = eooss_format.substring(et_pos + ".elementType == ".length());
            result.put(field, type);
          }
        }
      }
    }
    return result;
  }

  public String format(Invariant inv) {
    String inv_string;
    if (lightweight) {
      inv_string = inv.format_using(OutputFormat.ESCJAVA);
    } else {
      inv_string = inv.format_using(OutputFormat.JML);
    }
    // Debugging
    // if (true) {
    //   inv_string = inv_string + "  REPR: " + inv.repr();
    // }
    return inv_string;
  }

  public static InvariantsAndModifiedVars invariants_for(PptTopLevel ppt,
                                                         PptMap pptmap) {

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    PrintInvariants.print_modified_vars(ppt, pw);

    InvariantsAndModifiedVars retval = new InvariantsAndModifiedVars();
    retval.invariants = Ast.getInvariants(ppt, pptmap);
    retval.modifiedVars = sw.toString();

    // PrintInvariants.print_modified_vars(ppt, pw) returns possibly
    // several lines. In such a case, we're only interested in the second
    // one, which contains the "modified" or "assignable" clause.
    String[] splitModVars = utilMDE.UtilMDE.splitLines(retval.modifiedVars);
    if (splitModVars.length > 1) {
      for (int i = 0 ; i < splitModVars.length ; i++) {
        if (splitModVars[i].startsWith("modifies ") ||
            splitModVars[i].startsWith("assignable ")) {
          retval.modifiedVars =  splitModVars[i];
          break;
        }
      }
    }

    return retval;
  }

  private static class InvariantsAndModifiedVars {
    public List<Invariant> invariants;
    public String modifiedVars;
  }

  // Consider a line L1 of text that contains some tabs.
  //
  // untabbedIndex represents an index into a line L2, where L2 is L1
  // with all tabs substituted by their corresponding number of blank
  // spaces, where a tab's width is 8 characters.
  //
  // This method returns the index into L1 that corresponds to the same
  // position in L2.
  //
  // I assume that untabbedIndex will not be an index into one of the
  // blank spaces that replaced some tab.
  public static int getTabbedIndex(int untabbedIndex, String L1) {
    if (untabbedIndex == 0) {
      return 0;
    }
    int expanded = 0;
    int index = 0;
    for (int i = 0 ; i < L1.length() ; i++) {

      if (expanded > untabbedIndex) {
        throw new RuntimeException("expanded:" + expanded
                                   + "untabbedIndex:" + untabbedIndex
                                   + "\nL1:" + L1);
      } else if (expanded == untabbedIndex) {
        index = i;
        break;
      }

      char c = L1.charAt(i);
      if (c == '\t') {
        expanded += 8 - (expanded % 8);
      } else {
        expanded += 1;
      }

    }

    assert expanded == untabbedIndex :
                      "\nexpanded:" + expanded
                      + "\nuntabbedIndex:" + untabbedIndex
                      + "\nL1: " + L1;
    return index;

  }

  /** Return the whitespace at the front of the string. **/
  public static String precedingWhitespace(String s) {
    for (int i = 0 ; i < s.length() ; i++) {
      if (!Character.isWhitespace(s.charAt(i))) {
        return s.substring(0, i);
      }
    }
    return s;
  }

}
