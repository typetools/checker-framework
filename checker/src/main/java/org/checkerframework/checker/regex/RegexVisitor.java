package org.checkerframework.checker.regex;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A type-checking visitor for the Regex type system.
 *
 * <p>This visitor does the following:
 *
 * <ol>
 *   <li value="1">Allows any String to be passed to Pattern.compile if the Pattern.LITERAL flag is
 *       passed.
 *   <li value="2">Checks compound String concatenation to ensure correct usage of Regex Strings.
 *   <li value="3">Checks calls to {@code MatchResult.start}, {@code MatchResult.end} and {@code
 *       MatchResult.group} to ensure that a valid group number is passed.
 * </ol>
 *
 * @see RegexChecker
 */
public class RegexVisitor extends BaseTypeVisitor<RegexAnnotatedTypeFactory> {

  /** The method java.util.regex.MatchResult.end(int). */
  private final ExecutableElement matchResultEndInt;

  /** The method java.util.regex.MatchResult.group(int). */
  private final ExecutableElement matchResultGroupInt;

  /** The method java.util.regex.MatchResult.start(int). */
  private final ExecutableElement matchResultStartInt;

  /** The method java.util.regex.Pattern.compile. */
  private final ExecutableElement patternCompile;

  /** The field java.util.regex.Pattern.LITERAL. */
  private final VariableElement patternLiteral;

  /**
   * Create a RegexVisitor.
   *
   * @param checker the associated RegexChecker
   */
  public RegexVisitor(BaseTypeChecker checker) {
    super(checker);
    ProcessingEnvironment env = checker.getProcessingEnvironment();
    this.matchResultEndInt = TreeUtils.getMethod("java.util.regex.MatchResult", "end", env, "int");
    this.matchResultGroupInt =
        TreeUtils.getMethod("java.util.regex.MatchResult", "group", env, "int");
    this.matchResultStartInt =
        TreeUtils.getMethod("java.util.regex.MatchResult", "start", env, "int");
    this.patternCompile = TreeUtils.getMethod("java.util.regex.Pattern", "compile", 2, env);
    this.patternLiteral = TreeUtils.getField("java.util.regex.Pattern", "LITERAL", env);
  }

  /**
   * Case 1: Don't require a Regex annotation on the String argument to Pattern.compile if the
   * Pattern.LITERAL flag is passed.
   */
  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
    ProcessingEnvironment env = checker.getProcessingEnvironment();
    if (TreeUtils.isMethodInvocation(tree, patternCompile, env)) {
      ExpressionTree flagParam = tree.getArguments().get(1);
      if (flagParam instanceof MemberSelectTree) {
        MemberSelectTree memSelect = (MemberSelectTree) flagParam;
        if (TreeUtils.isSpecificFieldAccess(memSelect, patternLiteral)) {
          // This is a call to Pattern.compile with the Pattern.LITERAL flag so the first
          // parameter doesn't need to be a @Regex String. Don't call the super method to
          // skip checking if the first parameter is a @Regex String, but make sure to
          // still recurse on all of the different parts of the method call.
          Void r = scan(tree.getTypeArguments(), p);
          r = reduce(scan(tree.getMethodSelect(), p), r);
          r = reduce(scan(tree.getArguments(), p), r);
          return r;
        }
      }
    } else if (TreeUtils.isMethodInvocation(tree, matchResultEndInt, env)
        || TreeUtils.isMethodInvocation(tree, matchResultGroupInt, env)
        || TreeUtils.isMethodInvocation(tree, matchResultStartInt, env)) {
      // Case 3: Checks calls to {@code MatchResult.start}, {@code MatchResult.end} and {@code
      // MatchResult.group} to ensure that a valid group number is passed.
      ExpressionTree group = tree.getArguments().get(0);
      if (group.getKind() == Tree.Kind.INT_LITERAL) {
        LiteralTree literal = (LiteralTree) group;
        int paramGroups = (Integer) literal.getValue();
        ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
        if (receiver == null) {
          // When checking implementations of java.util.regex.MatcherResult, calls to
          // group (and other methods) don't have a receiver tree.  So, just do the
          // regular checking.
          // Verifying an implementation of a subclass of MatcherResult is out of the
          // scope of this checker.
          return super.visitMethodInvocation(tree, p);
        }
        int annoGroups = 0;
        AnnotatedTypeMirror receiverType = atypeFactory.getAnnotatedType(receiver);

        if (receiverType != null && receiverType.hasPrimaryAnnotation(Regex.class)) {
          annoGroups = atypeFactory.getGroupCount(receiverType.getPrimaryAnnotation(Regex.class));
        }
        if (paramGroups > annoGroups) {
          checker.reportError(group, "group.count", paramGroups, annoGroups, receiver);
        }
      } else {
        checker.reportWarning(group, "group.count.unknown");
      }
    }
    return super.visitMethodInvocation(tree, p);
  }

  // Case 2: Check String compound concatenation for valid Regex use.
  // TODO: Remove this. This should be handled by flow.
  /*
  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
      // Default behavior from superclass
  }
  */

}
