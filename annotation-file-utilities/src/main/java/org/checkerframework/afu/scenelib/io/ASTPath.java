package org.checkerframework.afu.scenelib.io;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.checkerframework.afu.annotator.find.CaseUtils;
import org.plumelib.util.ArraysPlume;

/** A path through the AST. */
public class ASTPath extends ImmutableStack<ASTPath.ASTEntry>
    implements Comparable<ASTPath>, Iterable<ASTPath.ASTEntry> {
  private static final ASTPath EMPTY = new ASTPath();
  private static final String[] typeSelectors = {
    "bound",
    "identifier",
    "type",
    "typeAlternative",
    "typeArgument",
    "typeParameter",
    "underlyingType"
  };

  // Constants for the various child selectors.
  public static final String ANNOTATION = "annotation";
  public static final String ARGUMENT = "argument";
  public static final String BLOCK = "block";
  public static final String BODY = "body";
  public static final String BOUND = "bound";
  public static final String CASE = "case";
  public static final String CATCH = "catch";
  public static final String CLASS_BODY = "classBody";
  public static final String CONDITION = "condition";
  public static final String DETAIL = "detail";
  public static final String DIMENSION = "dimension";
  public static final String ELSE_STATEMENT = "elseStatement";
  public static final String ENCLOSING_EXPRESSION = "enclosingExpression";
  public static final String EXPRESSION = "expression";
  public static final String FALSE_EXPRESSION = "falseExpression";
  public static final String FINALLY_BLOCK = "finallyBlock";
  public static final String IDENTIFIER = "identifier";
  public static final String INDEX = "index";
  public static final String INITIALIZER = "initializer";
  public static final String LEFT_OPERAND = "leftOperand";
  public static final String METHOD_SELECT = "methodSelect";
  public static final String MODIFIERS = "modifiers";
  public static final String PARAMETER = "parameter";
  public static final String QUALIFIER_EXPRESSION = "qualifierExpression";
  public static final String RESOURCE = "resource";
  public static final String RIGHT_OPERAND = "rightOperand";
  public static final String STATEMENT = "statement";
  public static final String THEN_STATEMENT = "thenStatement";
  public static final String THROWS = "throws";
  public static final String TRUE_EXPRESSION = "trueExpression";
  public static final String TYPE = "type";
  public static final String TYPE_ALTERNATIVE = "typeAlternative";
  public static final String TYPE_ARGUMENT = "typeArgument";
  public static final String TYPE_PARAMETER = "typeParameter";
  public static final String UNDERLYING_TYPE = "underlyingType";
  public static final String UPDATE = "update";
  public static final String VARIABLE = "variable";

  /** A single entry in an AST path. */
  public static class ASTEntry implements Comparable<ASTEntry> {
    private Tree.Kind treeKind;
    private String childSelector;
    /* May be null. */
    private Integer argument;

    /**
     * Constructs a new AST entry. For example, in the entry:
     *
     * <pre>{@code
     * Block.statement 3
     * }</pre>
     *
     * the tree kind is "Block", the child selector is "statement", and the argument is "3".
     *
     * @param treeKind the kind of this AST entry
     * @param childSelector the child selector to this AST entry
     * @param argument the argument
     */
    public ASTEntry(Tree.Kind treeKind, String childSelector, Integer argument) {
      if (childSelector == null) {
        throw new NullPointerException();
      }
      this.treeKind = treeKind;
      this.childSelector = childSelector;
      this.argument = argument;
    }

    /**
     * Constructs a new AST entry, without a numeric argument.
     *
     * @param treeKind the kind of this AST entry
     * @param childSelector the child selector to this AST entry
     */
    public ASTEntry(Tree.Kind treeKind, String childSelector) {
      this(treeKind, childSelector, null);
    }

    /**
     * Gets the tree node equivalent kind of this AST entry. For example, in
     *
     * <pre>{@code
     * Block.statement 3
     * }</pre>
     *
     * "Block" is the tree kind.
     *
     * @return the tree kind
     */
    public Tree.Kind getTreeKind() {
      return treeKind;
    }

    /**
     * Gets the child selector of this AST entry. For example, in
     *
     * <pre>{@code
     * Block.statement 3
     * }</pre>
     *
     * "statement" is the child selector.
     *
     * @return the child selector
     */
    public String getChildSelector() {
      return childSelector;
    }

    /**
     * Determines if the given string is equal to this AST path entry's child selector.
     *
     * @param s the string to compare to
     * @return {@code true} if the string matches the child selector, {@code false} otherwise
     */
    public boolean childSelectorIs(String s) {
      return childSelector.equals(s);
    }

    /**
     * Gets the argument of this AST entry. For example, in
     *
     * <pre>{@code
     * Block.statement 3
     * }</pre>
     *
     * "3" is the argument.
     *
     * @return the argument
     * @throws IllegalStateException if this AST entry does not have an argument
     */
    public int getArgument() {
      if (argument >= (negativeAllowed() ? -1 : 0)) {
        return argument;
      }
      throw new IllegalStateException("Value not set.");
    }

    /**
     * Checks that this Entry has an argument.
     *
     * @return if this entry has an argument
     */
    public boolean hasArgument() {
      return argument == null ? false : argument >= 0 ? true : negativeAllowed();
    }

    // argument < 0 valid for two cases
    private boolean negativeAllowed() {
      switch (treeKind) {
        case CLASS:
          return childSelectorIs(ASTPath.BOUND);
        case METHOD:
          return childSelectorIs(ASTPath.PARAMETER);
        default:
          return false;
      }
    }

    @Override
    public int compareTo(ASTEntry o) {
      if (o == null) {
        return 1;
      }
      int c = treeKind.compareTo(o.treeKind);
      if (c != 0) {
        return c;
      }
      c = childSelector.compareTo(o.childSelector);
      if (c != 0) {
        return c;
      }
      return o.argument == null
          ? argument == null ? 0 : 1
          : argument == null ? -1 : argument.compareTo(o.argument);
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof ASTEntry && compareTo((ASTEntry) o) == 0;
    }

    @Override
    public int hashCode() {
      return Objects.hash(treeKind, childSelector, argument);
    }

    @Override
    public String toString() {
      StringBuilder b = new StringBuilder();
      b.append(treeKind.asInterface().getSimpleName().replace("Tree", ""));
      b.append(".").append(childSelector);
      if (argument != null) {
        b.append(" ").append(argument);
      }
      return b.toString();
    }
  }

  private static Comparator<ASTPath> comparator =
      new Comparator<ASTPath>() {
        @Override
        public int compare(ASTPath p1, ASTPath p2) {
          return p1 == null ? (p2 == null ? 0 : -1) : p1.compareTo(p2);
        }
      };

  ASTPath() {}

  public static ASTPath empty() {
    return EMPTY;
  }

  public static Comparator<ASTPath> getComparator() {
    return comparator;
  }

  // TODO: replace w/ skip list?
  @Override
  public Iterator<ASTEntry> iterator() {
    ImmutableStack<ASTEntry> s = this;
    int n = size();
    ASTEntry[] a = new ASTEntry[n];
    while (--n >= 0) {
      a[n] = s.peek();
      s = s.pop();
    }
    return Arrays.asList(a).iterator();
  }

  public ASTPath extendNewArray(int depth) {
    return extend(new ASTEntry(Tree.Kind.NEW_ARRAY, ASTPath.TYPE, depth));
  }

  public ASTPath newArrayLevel(int depth) {
    return add(new ASTEntry(Tree.Kind.NEW_ARRAY, ASTPath.TYPE, depth));
  }

  public ASTPath add(ASTEntry entry) {
    ASTPath path = EMPTY;
    for (ASTEntry e : this) {
      path = path.extend(e);
    }
    return path.extend(entry);
  }

  public ASTPath extend(ASTEntry entry) {
    return (ASTPath) push(entry);
  }

  public ASTPath getParentPath() {
    return (ASTPath) pop();
  }

  public ASTEntry getLast() {
    return peek();
  }

  private static ASTPath canonical(ASTPath astPath) {
    // TODO
    return astPath;
  }

  /**
   * Create a new {@code ASTPath} from a formatted string description.
   *
   * @param s formatted string as in JAIF {@code insert-\{cast,annotation\}}
   * @return the corresponding {@code ASTPath}
   * @throws ParseException if there is trouble parsing the string
   */
  public static ASTPath parse(final String s) throws ParseException {
    return new Parser(s).parseASTPath();
  }

  /** Determine whether this {@code ASTPath} matches a given {@code TreePath}. */
  public boolean matches(TreePath treePath) {
    CompilationUnitTree cut = treePath.getCompilationUnit();
    Tree leaf = treePath.getLeaf();
    ASTPath astPath = ASTIndex.indexOf(cut).get(leaf).astPath; // FIXME
    return this.equals(astPath);
  }

  static class Parser {
    // adapted from IndexFileParser
    // TODO: refactor IndexFileParser to use this class

    StreamTokenizer st;

    Parser(String s) {
      st = new StreamTokenizer(new StringReader(s));
    }

    private void getTok() {
      try {
        st.nextToken();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private boolean gotType(int t) {
      return st.ttype == t;
    }

    private int intVal() throws ParseException {
      if (gotType(StreamTokenizer.TT_NUMBER)) {
        int n = (int) st.nval;
        if (n == st.nval) {
          return n;
        }
      }
      throw new ParseException("expected integer, got " + st);
    }

    private String strVal() throws ParseException {
      if (gotType(StreamTokenizer.TT_WORD)) {
        return st.sval;
      }
      throw new ParseException("expected string, got " + st);
    }

    /**
     * Parses an AST path.
     *
     * @return the AST path
     */
    ASTPath parseASTPath() throws ParseException {
      ASTPath astPath = new ASTPath().extend(parseASTEntry());
      while (gotType(',')) {
        getTok();
        astPath = astPath.extend(parseASTEntry());
      }
      return astPath;
    }

    /**
     * Parses and returns the next AST entry.
     *
     * @return a new AST entry
     * @throws ParseException if the next entry type is invalid
     */
    ASTEntry parseASTEntry() throws ParseException {
      String s = strVal();

      if (s.equals("AnnotatedType")) {
        return newASTEntry(
            Tree.Kind.ANNOTATED_TYPE,
            new String[] {ASTPath.ANNOTATION, ASTPath.UNDERLYING_TYPE},
            new String[] {ASTPath.ANNOTATION});
      } else if (s.equals("ArrayAccess")) {
        return newASTEntry(
            Tree.Kind.ARRAY_ACCESS, new String[] {ASTPath.EXPRESSION, ASTPath.INDEX});
      } else if (s.equals("ArrayType")) {
        return newASTEntry(Tree.Kind.ARRAY_TYPE, new String[] {ASTPath.TYPE});
      } else if (s.equals("Assert")) {
        return newASTEntry(Tree.Kind.ASSERT, new String[] {ASTPath.CONDITION, ASTPath.DETAIL});
      } else if (s.equals("Assignment")) {
        return newASTEntry(
            Tree.Kind.ASSIGNMENT, new String[] {ASTPath.VARIABLE, ASTPath.EXPRESSION});
      } else if (s.equals("Binary")) {
        // Always use Tree.Kind.PLUS for Binary
        return newASTEntry(
            Tree.Kind.PLUS, new String[] {ASTPath.LEFT_OPERAND, ASTPath.RIGHT_OPERAND});
      } else if (s.equals("Block")) {
        return newASTEntry(
            Tree.Kind.BLOCK, new String[] {ASTPath.STATEMENT}, new String[] {ASTPath.STATEMENT});
      } else if (s.equals("Case")) {
        return newASTEntry(
            Tree.Kind.CASE,
            new String[] {ASTPath.EXPRESSION, ASTPath.STATEMENT},
            new String[] {ASTPath.STATEMENT});
      } else if (s.equals("Catch")) {
        return newASTEntry(Tree.Kind.CATCH, new String[] {ASTPath.PARAMETER, ASTPath.BLOCK});
      } else if (s.equals("Class")) {
        return newASTEntry(
            Tree.Kind.CLASS,
            new String[] {ASTPath.BOUND, ASTPath.INITIALIZER, ASTPath.TYPE_PARAMETER},
            new String[] {ASTPath.BOUND, ASTPath.INITIALIZER, ASTPath.TYPE_PARAMETER});
      } else if (s.equals("CompoundAssignment")) {
        // Always use Tree.Kind.PLUS_ASSIGNMENT for CompoundAssignment
        return newASTEntry(
            Tree.Kind.PLUS_ASSIGNMENT, new String[] {ASTPath.VARIABLE, ASTPath.EXPRESSION});
      } else if (s.equals("ConditionalExpression")) {
        return newASTEntry(
            Tree.Kind.CONDITIONAL_EXPRESSION,
            new String[] {ASTPath.CONDITION, ASTPath.TRUE_EXPRESSION, ASTPath.FALSE_EXPRESSION});
      } else if (s.equals("DoWhileLoop")) {
        return newASTEntry(
            Tree.Kind.DO_WHILE_LOOP, new String[] {ASTPath.CONDITION, ASTPath.STATEMENT});
      } else if (s.equals("EnhancedForLoop")) {
        return newASTEntry(
            Tree.Kind.ENHANCED_FOR_LOOP,
            new String[] {ASTPath.VARIABLE, ASTPath.EXPRESSION, ASTPath.STATEMENT});
      } else if (s.equals("ExpressionStatement")) {
        return newASTEntry(Tree.Kind.EXPRESSION_STATEMENT, new String[] {ASTPath.EXPRESSION});
      } else if (s.equals("ForLoop")) {
        return newASTEntry(
            Tree.Kind.FOR_LOOP,
            new String[] {
              ASTPath.INITIALIZER, ASTPath.CONDITION, ASTPath.UPDATE, ASTPath.STATEMENT
            },
            new String[] {ASTPath.INITIALIZER, ASTPath.UPDATE});
      } else if (s.equals("If")) {
        return newASTEntry(
            Tree.Kind.IF,
            new String[] {ASTPath.CONDITION, ASTPath.THEN_STATEMENT, ASTPath.ELSE_STATEMENT});
      } else if (s.equals("InstanceOf")) {
        return newASTEntry(Tree.Kind.INSTANCE_OF, new String[] {ASTPath.EXPRESSION, ASTPath.TYPE});
      } else if (s.equals("LabeledStatement")) {
        return newASTEntry(Tree.Kind.LABELED_STATEMENT, new String[] {ASTPath.STATEMENT});
      } else if (s.equals("LambdaExpression")) {
        return newASTEntry(
            Tree.Kind.LAMBDA_EXPRESSION,
            new String[] {ASTPath.PARAMETER, ASTPath.BODY},
            new String[] {ASTPath.PARAMETER});
      } else if (s.equals("MemberReference")) {
        return newASTEntry(
            Tree.Kind.MEMBER_REFERENCE,
            new String[] {ASTPath.QUALIFIER_EXPRESSION, ASTPath.TYPE_ARGUMENT},
            new String[] {ASTPath.TYPE_ARGUMENT});
      } else if (s.equals("MemberSelect")) {
        return newASTEntry(Tree.Kind.MEMBER_SELECT, new String[] {ASTPath.EXPRESSION});
      } else if (s.equals("Method")) {
        return newASTEntry(
            Tree.Kind.METHOD,
            new String[] {ASTPath.BODY, ASTPath.PARAMETER, ASTPath.TYPE, ASTPath.TYPE_PARAMETER},
            new String[] {ASTPath.PARAMETER, ASTPath.TYPE_PARAMETER});
      } else if (s.equals("MethodInvocation")) {
        return newASTEntry(
            Tree.Kind.METHOD_INVOCATION,
            new String[] {ASTPath.TYPE_ARGUMENT, ASTPath.METHOD_SELECT, ASTPath.ARGUMENT},
            new String[] {ASTPath.TYPE_ARGUMENT, ASTPath.ARGUMENT});
      } else if (s.equals("NewArray")) {
        return newASTEntry(
            Tree.Kind.NEW_ARRAY,
            new String[] {ASTPath.TYPE, ASTPath.DIMENSION, ASTPath.INITIALIZER},
            new String[] {ASTPath.TYPE, ASTPath.DIMENSION, ASTPath.INITIALIZER});
      } else if (s.equals("NewClass")) {
        return newASTEntry(
            Tree.Kind.NEW_CLASS,
            new String[] {
              ASTPath.ENCLOSING_EXPRESSION,
              ASTPath.TYPE_ARGUMENT,
              ASTPath.IDENTIFIER,
              ASTPath.ARGUMENT,
              ASTPath.CLASS_BODY
            },
            new String[] {ASTPath.TYPE_ARGUMENT, ASTPath.ARGUMENT});
      } else if (s.equals("ParameterizedType")) {
        return newASTEntry(
            Tree.Kind.PARAMETERIZED_TYPE,
            new String[] {ASTPath.TYPE, ASTPath.TYPE_ARGUMENT},
            new String[] {ASTPath.TYPE_ARGUMENT});
      } else if (s.equals("Parenthesized")) {
        return newASTEntry(Tree.Kind.PARENTHESIZED, new String[] {ASTPath.EXPRESSION});
      } else if (s.equals("Return")) {
        return newASTEntry(Tree.Kind.RETURN, new String[] {ASTPath.EXPRESSION});
      } else if (s.equals("Switch")) {
        return newASTEntry(
            Tree.Kind.SWITCH,
            new String[] {ASTPath.EXPRESSION, ASTPath.CASE},
            new String[] {ASTPath.CASE});
      } else if (s.equals("Synchronized")) {
        return newASTEntry(
            Tree.Kind.SYNCHRONIZED, new String[] {ASTPath.EXPRESSION, ASTPath.BLOCK});
      } else if (s.equals("Throw")) {
        return newASTEntry(Tree.Kind.THROW, new String[] {ASTPath.EXPRESSION});
      } else if (s.equals("Try")) {
        return newASTEntry(
            Tree.Kind.TRY,
            new String[] {ASTPath.BLOCK, ASTPath.CATCH, ASTPath.FINALLY_BLOCK},
            new String[] {ASTPath.CATCH});
      } else if (s.equals("TypeCast")) {
        return newASTEntry(Tree.Kind.TYPE_CAST, new String[] {ASTPath.TYPE, ASTPath.EXPRESSION});
      } else if (s.equals("Unary")) {
        // Always use Tree.Kind.UNARY_PLUS for Unary
        return newASTEntry(Tree.Kind.UNARY_PLUS, new String[] {ASTPath.EXPRESSION});
      } else if (s.equals("UnionType")) {
        return newASTEntry(
            Tree.Kind.UNION_TYPE,
            new String[] {ASTPath.TYPE_ALTERNATIVE},
            new String[] {ASTPath.TYPE_ALTERNATIVE});
      } else if (s.equals("Variable")) {
        return newASTEntry(Tree.Kind.VARIABLE, new String[] {ASTPath.TYPE, ASTPath.INITIALIZER});
      } else if (s.equals("WhileLoop")) {
        return newASTEntry(
            Tree.Kind.WHILE_LOOP, new String[] {ASTPath.CONDITION, ASTPath.STATEMENT});
      } else if (s.equals("Wildcard")) {
        // Always use Tree.Kind.UNBOUNDED_WILDCARD for Wildcard
        return newASTEntry(Tree.Kind.UNBOUNDED_WILDCARD, new String[] {ASTPath.BOUND});
      }

      throw new ParseException("Invalid AST path type: " + s);
    }

    /**
     * Parses and constructs a new AST entry, where none of the child selections require arguments.
     * For example, the call:
     *
     * <pre>{@code
     * newASTEntry(Tree.Kind.WHILE_LOOP, new String[] {"condition", "statement"});
     * }</pre>
     *
     * constructs a while loop AST entry, where the valid child selectors are "condition" or
     * "statement".
     *
     * @param kind the kind of this AST entry
     * @param legalChildSelectors a list of the legal child selectors for this AST entry
     * @return a new {@link ASTEntry}
     * @throws ParseException if an illegal argument is found
     */
    private ASTEntry newASTEntry(Tree.Kind kind, String[] legalChildSelectors)
        throws ParseException {
      return newASTEntry(kind, legalChildSelectors, null);
    }

    /**
     * Parses and constructs a new AST entry. For example, the call:
     *
     * <pre>{@code
     * newASTEntry(Tree.Kind.CASE, new String[] {"expression", "statement"}, new String[] {"statement"});
     * }</pre>
     *
     * constructs a case AST entry, where the valid child selectors are "expression" or "statement"
     * and the "statement" child selector requires an argument.
     *
     * @param kind the kind of this AST entry
     * @param legalChildSelectors a list of the legal child selectors for this AST entry
     * @param argumentChildSelectors a list of the child selectors that also require an argument.
     *     Entries here should also be in the legalChildSelectors list.
     * @return a new {@link ASTEntry}
     * @throws ParseException if an illegal argument is found
     */
    private ASTEntry newASTEntry(
        Tree.Kind kind, String[] legalChildSelectors, String[] argumentChildSelectors)
        throws ParseException {
      if (gotType('.')) {
        getTok();
      } else {
        throw new ParseException("expected '.', got " + st);
      }

      String s = strVal();
      for (String arg : legalChildSelectors) {
        if (s.equals(arg)) {
          if (argumentChildSelectors != null
              && ArraysPlume.indexOf(argumentChildSelectors, arg) >= 0) {
            getTok();
            return new ASTEntry(kind, arg, intVal());
          } else {
            return new ASTEntry(kind, arg);
          }
        }
      }

      throw new ParseException(
          "Invalid argument for "
              + kind
              + " (legal arguments - "
              + Arrays.toString(legalChildSelectors)
              + "): "
              + s);
    }
  }

  static class Matcher {
    // adapted from IndexFileParser.parseASTPath et al.
    // TODO: refactor switch statement into TreeVisitor?

    /** Debugging logger. */
    public static final DebugWriter dbug = new DebugWriter(false);

    /** The path associated with this. */
    private ASTPath astPath;

    Matcher(ASTPath astPath) {
      this.astPath = astPath;
    }

    private boolean nonDecl(TreePath path) {
      switch (path.getLeaf().getKind()) {
        case CLASS:
        case METHOD:
          return false;
        case VARIABLE:
          TreePath parentPath = path.getParentPath();
          return parentPath != null && parentPath.getLeaf().getKind() != Tree.Kind.CLASS;
        default:
          return true;
      }
    }

    public boolean matches(TreePath path) {
      return matches(path, -1);
    }

    public boolean matches(TreePath path, int depth) {
      if (path == null) {
        return false;
      }

      // actualPath stores the path through the source code AST to this
      // location (specified by the "path" parameter to this method). It is
      // computed by traversing from this location up the source code AST
      // until it reaches a method node (this gets only the part of the path
      // within a method) or class node (this gets only the part of the path
      // within a field).
      List<Tree> actualPath = new ArrayList<>();
      while (path != null && nonDecl(path)) {
        actualPath.add(0, path.getLeaf());
        path = path.getParentPath();
      }

      if (dbug.isEnabled()) {
        dbug.debug("AST [%s]%n", astPath);
        for (Tree t : actualPath) {
          dbug.debug("  %s: %s%n", t.getKind(), t.toString().replace('\n', ' '));
        }
      }

      if (astPath.isEmpty() || actualPath.isEmpty() || actualPath.size() != astPath.size() + 1) {
        return false;
      }

      for (int i = 0; i < astPath.size() && i < actualPath.size(); i++) {
        ASTPath.ASTEntry astNode = astPath.get(i);
        Tree actualNode = actualPath.get(i);

        // Based on the child selector and (optional) argument in "astNode",
        // "next" will get set to the next source node below "actualNode".
        // Then "next" will be compared with the node following "astNode"
        // in "actualPath". If it's not a match, this is not the correct
        // location. If it is a match, keep going.
        Tree next = null;
        dbug.debug("astNode: %s%n", astNode);
        dbug.debug("actualNode: %s%n", actualNode.getKind());
        if (!kindsMatch(astNode.getTreeKind(), actualNode.getKind())) {
          return false;
        }

        switch (actualNode.getKind()) {
          case ANNOTATED_TYPE:
            {
              AnnotatedTypeTree annotatedType = (AnnotatedTypeTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.ANNOTATION)) {
                int arg = astNode.getArgument();
                List<? extends AnnotationTree> annos = annotatedType.getAnnotations();
                if (arg >= annos.size()) {
                  return false;
                }
                next = annos.get(arg);
              } else {
                next = annotatedType.getUnderlyingType();
              }
              break;
            }
          case ARRAY_ACCESS:
            {
              ArrayAccessTree arrayAccess = (ArrayAccessTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.EXPRESSION)) {
                next = arrayAccess.getExpression();
              } else {
                next = arrayAccess.getIndex();
              }
              break;
            }
          case ARRAY_TYPE:
            {
              ArrayTypeTree arrayType = (ArrayTypeTree) actualNode;
              next = arrayType.getType();
              break;
            }
          case ASSERT:
            {
              AssertTree azzert = (AssertTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.CONDITION)) {
                next = azzert.getCondition();
              } else {
                next = azzert.getDetail();
              }
              break;
            }
          case ASSIGNMENT:
            {
              AssignmentTree assignment = (AssignmentTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.VARIABLE)) {
                next = assignment.getVariable();
              } else {
                next = assignment.getExpression();
              }
              break;
            }
          case BLOCK:
            {
              BlockTree block = (BlockTree) actualNode;
              int arg = astNode.getArgument();
              List<? extends StatementTree> statements = block.getStatements();
              if (arg >= block.getStatements().size()) {
                return false;
              }
              next = statements.get(arg);
              break;
            }
          case CASE:
            {
              CaseTree caze = (CaseTree) actualNode;
              int arg = astNode.getArgument();
              if (astNode.childSelectorIs(ASTPath.EXPRESSION)) {
                List<? extends ExpressionTree> expressions = CaseUtils.caseTreeGetExpressions(caze);
                // If expressions is empty, it means default case:
                if (!expressions.isEmpty() && arg >= expressions.size()) {
                  return false;
                }
                next = expressions.get(arg);
              } else {
                List<? extends StatementTree> statements = caze.getStatements();
                if (arg >= statements.size()) {
                  return false;
                }
                next = statements.get(arg);
              }
              break;
            }
          case CATCH:
            {
              CatchTree cach = (CatchTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.PARAMETER)) {
                next = cach.getParameter();
              } else {
                next = cach.getBlock();
              }
              break;
            }
          case CLASS:
            {
              ClassTree clazz = (ClassTree) actualNode;
              int arg = astNode.getArgument();
              if (astNode.childSelectorIs(ASTPath.BOUND)) {
                next = arg == -1 ? clazz.getExtendsClause() : clazz.getImplementsClause().get(arg);
              } else {
                next = clazz.getTypeParameters().get(arg);
              }
              break;
            }
          case CONDITIONAL_EXPRESSION:
            {
              ConditionalExpressionTree conditionalExpression =
                  (ConditionalExpressionTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.CONDITION)) {
                next = conditionalExpression.getCondition();
              } else if (astNode.childSelectorIs(ASTPath.TRUE_EXPRESSION)) {
                next = conditionalExpression.getTrueExpression();
              } else {
                next = conditionalExpression.getFalseExpression();
              }
              break;
            }
          case DO_WHILE_LOOP:
            {
              DoWhileLoopTree doWhileLoop = (DoWhileLoopTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.CONDITION)) {
                next = doWhileLoop.getCondition();
              } else {
                next = doWhileLoop.getStatement();
              }
              break;
            }
          case ENHANCED_FOR_LOOP:
            {
              EnhancedForLoopTree enhancedForLoop = (EnhancedForLoopTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.VARIABLE)) {
                next = enhancedForLoop.getVariable();
              } else if (astNode.childSelectorIs(ASTPath.EXPRESSION)) {
                next = enhancedForLoop.getExpression();
              } else {
                next = enhancedForLoop.getStatement();
              }
              break;
            }
          case EXPRESSION_STATEMENT:
            {
              ExpressionStatementTree expressionStatement = (ExpressionStatementTree) actualNode;
              next = expressionStatement.getExpression();
              break;
            }
          case FOR_LOOP:
            {
              ForLoopTree forLoop = (ForLoopTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.INITIALIZER)) {
                int arg = astNode.getArgument();
                List<? extends StatementTree> inits = forLoop.getInitializer();
                if (arg >= inits.size()) {
                  return false;
                }
                next = inits.get(arg);
              } else if (astNode.childSelectorIs(ASTPath.CONDITION)) {
                next = forLoop.getCondition();
              } else if (astNode.childSelectorIs(ASTPath.UPDATE)) {
                int arg = astNode.getArgument();
                List<? extends ExpressionStatementTree> updates = forLoop.getUpdate();
                if (arg >= updates.size()) {
                  return false;
                }
                next = updates.get(arg);
              } else {
                next = forLoop.getStatement();
              }
              break;
            }
          case IF:
            {
              IfTree iff = (IfTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.CONDITION)) {
                next = iff.getCondition();
              } else if (astNode.childSelectorIs(ASTPath.THEN_STATEMENT)) {
                next = iff.getThenStatement();
              } else {
                next = iff.getElseStatement();
              }
              break;
            }
          case INSTANCE_OF:
            {
              InstanceOfTree instanceOf = (InstanceOfTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.EXPRESSION)) {
                next = instanceOf.getExpression();
              } else {
                next = instanceOf.getType();
              }
              break;
            }
          case LABELED_STATEMENT:
            {
              LabeledStatementTree labeledStatement = (LabeledStatementTree) actualNode;
              next = labeledStatement.getStatement();
              break;
            }
          case LAMBDA_EXPRESSION:
            {
              LambdaExpressionTree lambdaExpression = (LambdaExpressionTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.PARAMETER)) {
                int arg = astNode.getArgument();
                List<? extends VariableTree> params = lambdaExpression.getParameters();
                if (arg >= params.size()) {
                  return false;
                }
                next = params.get(arg);
              } else {
                next = lambdaExpression.getBody();
              }
              break;
            }
          case MEMBER_REFERENCE:
            {
              MemberReferenceTree memberReference = (MemberReferenceTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.QUALIFIER_EXPRESSION)) {
                next = memberReference.getQualifierExpression();
              } else {
                int arg = astNode.getArgument();
                List<? extends ExpressionTree> typeArgs = memberReference.getTypeArguments();
                if (arg >= typeArgs.size()) {
                  return false;
                }
                next = typeArgs.get(arg);
              }
              break;
            }
          case MEMBER_SELECT:
            {
              MemberSelectTree memberSelect = (MemberSelectTree) actualNode;
              next = memberSelect.getExpression();
              break;
            }
          case METHOD:
            {
              MethodTree method = (MethodTree) actualNode;
              int arg = astNode.getArgument();
              if (astNode.childSelectorIs(ASTPath.TYPE)) {
                next = method.getReturnType();
              } else if (astNode.childSelectorIs(ASTPath.PARAMETER)) {
                next = arg == -1 ? method.getReceiverParameter() : method.getParameters().get(arg);
              } else if (astNode.childSelectorIs(ASTPath.TYPE_PARAMETER)) {
                next = method.getTypeParameters().get(arg);
              } else if (astNode.childSelectorIs(ASTPath.BODY)) {
                next = method.getBody();
              } else { // THROWS?
                return false;
              }
              break;
            }
          case METHOD_INVOCATION:
            {
              MethodInvocationTree methodInvocation = (MethodInvocationTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.TYPE_ARGUMENT)) {
                int arg = astNode.getArgument();
                List<? extends Tree> typeArgs = methodInvocation.getTypeArguments();
                if (arg >= typeArgs.size()) {
                  return false;
                }
                next = typeArgs.get(arg);
              } else if (astNode.childSelectorIs(ASTPath.METHOD_SELECT)) {
                next = methodInvocation.getMethodSelect();
              } else {
                int arg = astNode.getArgument();
                List<? extends ExpressionTree> args = methodInvocation.getArguments();
                if (arg >= args.size()) {
                  return false;
                }
                next = args.get(arg);
              }
              break;
            }
          case NEW_ARRAY:
            {
              NewArrayTree newArray = (NewArrayTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.TYPE)) {
                int arg = astNode.getArgument();
                if (arg < 0) {
                  next = newArray.getType();
                } else {
                  return arg == depth;
                }
              } else if (astNode.childSelectorIs(ASTPath.DIMENSION)) {
                int arg = astNode.getArgument();
                List<? extends ExpressionTree> dims = newArray.getDimensions();
                if (arg >= dims.size()) {
                  return false;
                }
                next = dims.get(arg);
              } else {
                int arg = astNode.getArgument();
                List<? extends ExpressionTree> inits = newArray.getInitializers();
                if (arg >= inits.size()) {
                  return false;
                }
                next = inits.get(arg);
              }
              break;
            }
          case NEW_CLASS:
            {
              NewClassTree newClass = (NewClassTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.ENCLOSING_EXPRESSION)) {
                next = newClass.getEnclosingExpression();
              } else if (astNode.childSelectorIs(ASTPath.TYPE_ARGUMENT)) {
                int arg = astNode.getArgument();
                List<? extends Tree> typeArgs = newClass.getTypeArguments();
                if (arg >= typeArgs.size()) {
                  return false;
                }
                next = typeArgs.get(arg);
              } else if (astNode.childSelectorIs(ASTPath.IDENTIFIER)) {
                next = newClass.getIdentifier();
              } else if (astNode.childSelectorIs(ASTPath.ARGUMENT)) {
                int arg = astNode.getArgument();
                List<? extends ExpressionTree> args = newClass.getArguments();
                if (arg >= args.size()) {
                  return false;
                }
                next = args.get(arg);
              } else {
                next = newClass.getClassBody();
              }
              break;
            }
          case PARAMETERIZED_TYPE:
            {
              ParameterizedTypeTree parameterizedType = (ParameterizedTypeTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.TYPE)) {
                next = parameterizedType.getType();
              } else {
                int arg = astNode.getArgument();
                List<? extends Tree> typeArgs = parameterizedType.getTypeArguments();
                if (arg >= typeArgs.size()) {
                  return false;
                }
                next = typeArgs.get(arg);
              }
              break;
            }
          case PARENTHESIZED:
            {
              ParenthesizedTree parenthesized = (ParenthesizedTree) actualNode;
              next = parenthesized.getExpression();
              break;
            }
          case RETURN:
            {
              ReturnTree returnn = (ReturnTree) actualNode;
              next = returnn.getExpression();
              break;
            }
          case SWITCH:
            {
              SwitchTree zwitch = (SwitchTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.EXPRESSION)) {
                next = zwitch.getExpression();
              } else {
                int arg = astNode.getArgument();
                List<? extends CaseTree> cases = zwitch.getCases();
                if (arg >= cases.size()) {
                  return false;
                }
                next = cases.get(arg);
              }
              break;
            }
          case SYNCHRONIZED:
            {
              SynchronizedTree synchronizzed = (SynchronizedTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.EXPRESSION)) {
                next = synchronizzed.getExpression();
              } else {
                next = synchronizzed.getBlock();
              }
              break;
            }
          case THROW:
            {
              ThrowTree throww = (ThrowTree) actualNode;
              next = throww.getExpression();
              break;
            }
          case TRY:
            {
              TryTree tryy = (TryTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.BLOCK)) {
                next = tryy.getBlock();
              } else if (astNode.childSelectorIs(ASTPath.CATCH)) {
                int arg = astNode.getArgument();
                List<? extends CatchTree> catches = tryy.getCatches();
                if (arg >= catches.size()) {
                  return false;
                }
                next = catches.get(arg);
              } else if (astNode.childSelectorIs(ASTPath.FINALLY_BLOCK)) {
                next = tryy.getFinallyBlock();
              } else {
                int arg = astNode.getArgument();
                List<? extends Tree> resources = tryy.getResources();
                if (arg >= resources.size()) {
                  return false;
                }
                next = resources.get(arg);
              }
              break;
            }
          case TYPE_CAST:
            {
              TypeCastTree typeCast = (TypeCastTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.TYPE)) {
                next = typeCast.getType();
              } else {
                next = typeCast.getExpression();
              }
              break;
            }
          case UNION_TYPE:
            {
              UnionTypeTree unionType = (UnionTypeTree) actualNode;
              int arg = astNode.getArgument();
              List<? extends Tree> typeAlts = unionType.getTypeAlternatives();
              if (arg >= typeAlts.size()) {
                return false;
              }
              next = typeAlts.get(arg);
              break;
            }
          case VARIABLE:
            {
              VariableTree var = (VariableTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.INITIALIZER)) {
                next = var.getInitializer();
              } else {
                next = var.getType();
              }
              break;
            }
          case WHILE_LOOP:
            {
              WhileLoopTree whileLoop = (WhileLoopTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.CONDITION)) {
                next = whileLoop.getCondition();
              } else {
                next = whileLoop.getStatement();
              }
              break;
            }
          default:
            {
              if (isBinaryOperator(actualNode.getKind())) {
                BinaryTree binary = (BinaryTree) actualNode;
                if (astNode.childSelectorIs(ASTPath.LEFT_OPERAND)) {
                  next = binary.getLeftOperand();
                } else {
                  next = binary.getRightOperand();
                }
              } else if (isCompoundAssignment(actualNode.getKind())) {
                CompoundAssignmentTree compoundAssignment = (CompoundAssignmentTree) actualNode;
                if (astNode.childSelectorIs(ASTPath.VARIABLE)) {
                  next = compoundAssignment.getVariable();
                } else {
                  next = compoundAssignment.getExpression();
                }
              } else if (isUnaryOperator(actualNode.getKind())) {
                UnaryTree unary = (UnaryTree) actualNode;
                next = unary.getExpression();
              } else if (isWildcard(actualNode.getKind())) {
                WildcardTree wildcard = (WildcardTree) actualNode;
                // The following check is necessary because Oracle has decided that
                //   x instanceof Class<? extends Object>
                // will remain illegal even though it means the same thing as
                //   x instanceof Class<?>.
                if (i > 0) { // TODO: refactor GenericArrayLoc to use same code?
                  Tree ancestor = actualPath.get(i - 1);
                  if (ancestor instanceof InstanceOfTree) {
                    System.err.println(
                        "WARNING: wildcard bounds not allowed"
                            + " in 'instanceof' expression; skipping insertion");
                    return false;
                  } else if (i > 1 && ancestor instanceof ParameterizedTypeTree) {
                    ancestor = actualPath.get(i - 2);
                    if (ancestor instanceof ArrayTypeTree) {
                      System.err.println(
                          "WARNING: wildcard bounds not allowed"
                              + " in generic array type; skipping insertion");
                      return false;
                    }
                  }
                }
                next = wildcard.getBound();
              } else {
                throw new IllegalArgumentException("Illegal kind: " + actualNode.getKind());
              }
              break;
            }
        }

        if (dbug.isEnabled()) {
          String nextString = next.toString();
          if (nextString.length() > 80) {
            nextString = nextString.substring(0, 80) + "...";
          }
          dbug.debug("next: %s%n", nextString);
        }
        if (next != actualPath.get(i + 1)) {
          dbug.debug("no next match%n");
          return false;
        }
      }
      return true;
    }

    /**
     * Determines if the given kinds match, false otherwise. Two kinds match if they're exactly the
     * same or if the two kinds are both compound assignments, unary operators, binary operators or
     * wildcards.
     *
     * <p>This is necessary because in the JAIF file these kinds are represented by their general
     * types (i.e. BinaryOperator, CompoundOperator, etc.) rather than their kind (i.e. PLUS, MINUS,
     * PLUS_ASSIGNMENT, XOR_ASSIGNMENT, etc.). Internally, a single kind is used to represent each
     * general type (i.e. PLUS is used for BinaryOperator, PLUS_ASSIGNMENT is used for
     * CompoundAssignment, etc.). Yet, the actual source nodes have the correct kind. So if an AST
     * path entry has a PLUS kind, that really means it could be any BinaryOperator, resulting in
     * PLUS matching any other BinaryOperator.
     *
     * @param kind1 the first kind to match
     * @param kind2 the second kind to match
     * @return {@code true} if the kinds match as described above, {@code false} otherwise
     */
    private static boolean kindsMatch(Tree.Kind kind1, Tree.Kind kind2) {
      return kind1 == kind2
          || (isCompoundAssignment(kind1) && isCompoundAssignment(kind2))
          || (isUnaryOperator(kind1) && isUnaryOperator(kind2))
          || (isBinaryOperator(kind1) && isBinaryOperator(kind2))
          || (isWildcard(kind1) && isWildcard(kind2));
    }
  }

  public static boolean isTypeSelector(String selector) {
    return Arrays.<String>binarySearch(typeSelectors, selector, Collator.getInstance()) >= 0;
  }

  public static boolean isClassEquiv(Tree.Kind kind) {
    return kind.asInterface().equals(ClassTree.class);
  }

  /**
   * Determines if the given kind is a compound assignment.
   *
   * @param kind the kind to test
   * @return true if the given kind is a compound assignment
   */
  public static boolean isCompoundAssignment(Tree.Kind kind) {
    return kind.asInterface().equals(CompoundAssignmentTree.class);
  }

  /**
   * Determines if the given kind is a unary operator.
   *
   * @param kind the kind to test
   * @return true if the given kind is a unary operator
   */
  public static boolean isUnaryOperator(Tree.Kind kind) {
    return kind.asInterface().equals(UnaryTree.class);
  }

  /**
   * Determines if the given kind is a binary operator.
   *
   * @param kind the kind to test
   * @return true if the given kind is a binary operator
   */
  public static boolean isBinaryOperator(Tree.Kind kind) {
    return kind.asInterface().equals(BinaryTree.class);
  }

  public static boolean isLiteral(Tree.Kind kind) {
    return kind.asInterface().equals(LiteralTree.class);
  }

  public static boolean isTypeKind(Tree.Kind kind) {
    switch (kind) {
      case ANNOTATED_TYPE:
      case ARRAY_TYPE:
      case IDENTIFIER:
      case INTERSECTION_TYPE:
      // case MEMBER_SELECT:
      case PARAMETERIZED_TYPE:
      case PRIMITIVE_TYPE:
      case UNION_TYPE:
        return true;
      default:
        return false;
    }
  }

  /**
   * Determines if the given kind is a wildcard.
   *
   * @param kind the kind to test
   * @return true if the given kind is a wildcard
   */
  public static boolean isWildcard(Tree.Kind kind) {
    return kind.asInterface().equals(WildcardTree.class);
  }

  /**
   * Determines if the given kind is a declaration.
   *
   * @param kind the kind to test
   * @return true if the given kind is a declaration
   */
  public static boolean isDeclaration(Tree.Kind kind) {
    return kind == Tree.Kind.ANNOTATION
        || kind == Tree.Kind.CLASS
        || kind == Tree.Kind.ENUM
        || kind == Tree.Kind.INTERFACE
        || kind == Tree.Kind.METHOD
        || kind == Tree.Kind.VARIABLE;
  }

  /**
   * Determines whether an {@code ASTPath} can identify nodes of the given kind.
   *
   * @param kind the kind to test
   * @return true if the given kind can be identified by an {@code ASTPath}
   */
  public static boolean isHandled(Tree.Kind kind) {
    switch (kind) {
      case BREAK:
      case COMPILATION_UNIT:
      case CONTINUE:
      case IMPORT:
      case MODIFIERS:
        return false;
      default:
        return !isDeclaration(kind);
    }
  } // TODO: need "isType"?

  @Override
  public int hashCode() {
    // hacky fix: remove {Method,Class}.body for comparison
    ImmutableStack<ASTEntry> s = canonical(this);
    int hash = 0;
    while (!s.isEmpty()) {
      hash = Integer.rotateRight(hash ^ s.peek().hashCode(), 1);
      s = s.pop();
    }
    return hash;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ASTPath && equals((ASTPath) o);
  }

  public boolean equals(ASTPath astPath) {
    return compareTo(astPath) == 0;
  }

  @SuppressWarnings("JdkObsolete") // TODO: don't use LinkedList
  @Override
  public int compareTo(ASTPath o) {
    // hacky fix: remove {Method,Class}.body for comparison
    ImmutableStack<ASTEntry> s0 = canonical(this);
    ImmutableStack<ASTEntry> s1 = canonical(o);
    Deque<ASTEntry> d0 = new LinkedList<ASTEntry>();
    Deque<ASTEntry> d1 = new LinkedList<ASTEntry>();
    while (!s0.isEmpty()) {
      d0.push(s0.peek());
      s0 = s0.pop();
    }
    while (!s1.isEmpty()) {
      d1.push(s1.peek());
      s1 = s1.pop();
    }
    int n0 = d0.size();
    int n1 = d1.size();
    int c = Integer.compare(n0, n1);
    if (c == 0) {
      Iterator<ASTEntry> i0 = d0.iterator();
      Iterator<ASTEntry> i1 = d1.iterator();
      while (i0.hasNext()) {
        c = i0.next().compareTo(i1.next());
        if (c != 0) {
          return c;
        }
      }
    }
    return c;
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return "";
    }
    Iterator<ASTEntry> iter = iterator();
    StringBuilder sb = new StringBuilder().append(iter.next());
    while (iter.hasNext()) {
      sb = sb.append(", ").append(iter.next());
    }
    return sb.toString();
  }
} // end of class ASTPath

// This class cannot be moved to (say) package org.checkerframework.afu.scenelib.util,
// probably because of the reflection cleverness.
/**
 * Immutable stack: operations create new stacks rather than mutate the receiver.
 *
 * @param <E> type of stack elements
 */
class ImmutableStack<E> {

  // The stack is implemented as a linked list:
  // each ImmutableStack consists of an element and the rest of the list.
  private E elem = null;
  private ImmutableStack<E> rest = null;
  private int size = 0;

  public ImmutableStack() {}

  private static <T, S extends ImmutableStack<T>> S extend(T el, S s0) {
    try {
      @SuppressWarnings("unchecked")
      S s1 = (S) s0.getClass().getDeclaredConstructor().newInstance();
      ImmutableStack<T> cs = (ImmutableStack<T>) s1;
      cs.size = 1 + s0.size();
      cs.elem = el;
      cs.rest = s0;
      return s1;
    } catch (InstantiationException
        | InvocationTargetException
        | IllegalAccessException
        | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public E peek() {
    if (isEmpty()) {
      throw new IllegalStateException("peek() on empty stack");
    }
    return elem;
  }

  public ImmutableStack<E> pop() {
    if (isEmpty()) {
      throw new IllegalStateException("pop() on empty stack");
    }
    return rest;
  }

  public ImmutableStack<E> push(E elem) {
    return extend(elem, this);
  }

  public int size() {
    return size;
  }

  /** Return the index-th element of this stack. */
  public E get(int index) {
    int n = size();

    if (!(0 <= index && index < n)) {
      throw new NoSuchElementException("Has " + n + " elements, asked for #" + index);
    }

    ImmutableStack<E> s = this;
    while (--n > index) {
      s = s.pop();
    }
    return s.peek();
  }

  @Override
  public String toString() {
    if (size > 0) {
      StringBuilder sb = new StringBuilder("]").insert(0, peek());
      for (ImmutableStack<E> stack = pop(); !stack.isEmpty(); stack = stack.pop()) {
        sb = sb.insert(0, ", ").insert(0, stack.peek());
      }
      return sb.insert(0, "[").toString();
    }
    return "[]";
  }
} // end of class ImmutableStack
