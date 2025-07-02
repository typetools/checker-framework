package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.*;
import java.util.regex.*;
import javax.lang.model.element.Name;
import org.checkerframework.afu.annotator.scanner.AnonymousClassScanner;
import org.checkerframework.afu.annotator.scanner.LocalClassScanner;
import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.checker.signature.qual.ClassGetName;

// If there are dollar signs in a name, then there are two
// possibilities regarding how the dollar sign got there.
//  1. Inserted by the compiler, for inner classes.
//  2. Written by the programmer (or by a tool that creates .class files).
// We need to account for both possibilities (and all combinations of them).

// Example names
//   org.checkerframework.afu.annotator.tests.FullClassName
//   org.checkerframework.afu.annotator.tests.FullClassName$InnerClass
//   org.checkerframework.afu.annotator.tests.FullClassName$0

/** Represents the criterion that a program element is in a class with a particular name. */
public final class InClassCriterion implements Criterion {

  /** If true, print diagnostic information. */
  static boolean debug = false;

  /** The class name. */
  public final @ClassGetName String className;

  /** If true, require an exact match. */
  private final boolean exactMatch;

  /**
   * Creates a new InClassCriterion.
   *
   * @param className the class name
   * @param exactMatch if true, require an exact match
   */
  public InClassCriterion(@ClassGetName String className, boolean exactMatch) {
    this.className = className;
    this.exactMatch = exactMatch;
  }

  @Override
  public Kind getKind() {
    return Kind.IN_CLASS;
  }

  @Override
  public boolean isSatisfiedBy(TreePath path, Tree leaf) {
    if (path == null) {
      return false;
    }
    assert path.getLeaf() == leaf;
    return isSatisfiedBy(path);
  }

  @Override
  public boolean isSatisfiedBy(TreePath path) {
    return InClassCriterion.isSatisfiedBy(path, className, exactMatch);
  }

  static Pattern anonclassPattern;
  static Pattern localClassPattern;

  static {
    // for JDK 7: anonclassPattern = Pattern.compile("^(?<num>[0-9]+)(\\$(?<remaining>.*))?$");
    anonclassPattern = Pattern.compile("^([0-9]+)(\\$(.*))?$");
    localClassPattern = Pattern.compile("^([0-9]+)([^$]+)(\\$(.*))?$");
  }

  public static boolean isSatisfiedBy(TreePath path, String className, boolean exactMatch) {
    if (path == null) {
      return false;
    }

    // However much of the class name remains to match.
    String cname = className;

    // It is wrong to work from the leaf up to the root of the tree, which
    // would fail if the criterion is a.second.c and the actual is a.second.c.c.
    List<Tree> trees = new ArrayList<>();
    for (Tree tree : path) {
      trees.add(tree);
    }
    Collections.reverse(trees);

    boolean insideMatch = false;
    for (int i = 0; i < trees.size(); i++) {
      Tree tree = trees.get(i);
      boolean checkAnon = false;
      boolean checkLocal = false;

      switch (tree.getKind()) {
        case COMPILATION_UNIT:
          debug("InClassCriterion.isSatisfiedBy:%n  cname=%s%n  tree=%s%n", cname, tree);
          ExpressionTree packageTree = ((CompilationUnitTree) tree).getPackageName();
          if (packageTree == null) {
            // compilation unit is in default package; nothing to do
          } else {
            String declaredPackage = packageTree.toString();
            if (cname.startsWith(declaredPackage + ".")) {
              cname = cname.substring(declaredPackage.length() + 1);
            } else {
              debug(
                  "false[COMPILATION_UNIT; bad declaredPackage = %s]"
                      + " InClassCriterion.isSatisfiedBy:%n  cname=%s%n  tree=%s%n",
                  declaredPackage, cname, tree);
              return false;
            }
          }
          break;
        case CLASS:
        case INTERFACE:
        case ENUM:
        case ANNOTATION_TYPE:
          if (i > 0 && trees.get(i - 1) instanceof NewClassTree) {
            // For an anonymous class, the CLASS tree is always directly inside of
            // a NEW_CLASS tree. If that's the case here then skip this iteration
            // since we've already looked at the new class tree in the previous
            // iteration.
            break;
          }
          debug("InClassCriterion.isSatisfiedBy:%n  cname=%s%n  tree=%s%n", cname, tree);

          if (i > 0 && trees.get(i - 1) instanceof BlockTree) {
            // Section 14.3 of the JLS says "every local class declaration
            // statement is immediately contained by a block".
            checkLocal = true;
            debug(
                "found local class: InClassCriterion.isSatisfiedBy:%n  cname=%s%n  tree=%s%n",
                cname, tree);
            break;
          }

          // all four Kinds are represented by ClassTree
          ClassTree c = (ClassTree) tree;
          Name csn = c.getSimpleName();

          if (csn == null || csn.length() == 0) {
            debug(
                "empty getSimpleName: InClassCriterion.isSatisfiedBy:%n  cname=%s%n  tree=%s%n",
                cname, tree);
            checkAnon = true;
            break;
          }
          String treeClassName = csn.toString();
          if (cname.equals(treeClassName)) {
            if (exactMatch) {
              cname = "";
            } else {
              debug("true InClassCriterion.isSatisfiedBy:%n  cname=%s%n  tree=%s%n", cname, tree);
              return true;
            }
          } else if (cname.startsWith(treeClassName + "$")
              || cname.startsWith(treeClassName + ".")) {
            cname = cname.substring(treeClassName.length() + 1);
          } else if (!treeClassName.isEmpty()) {
            // treeClassName is empty for anonymous inner class
            // System.out.println("cname else: " + cname);
            debug("false InClassCriterion.isSatisfiedBy:%n  cname=%s%n  tree=%s%n", cname, tree);
            return false;
          }
          break;
        case NEW_CLASS:
          // When matching the "new Class() { ... }" expression itself, we
          // should not use the anonymous class name.  But when matching
          // within the braces, we should.
          debug("InClassCriterion.isSatisfiedBy:%n  cname=%s%n  tree=%s%n", cname, tree);
          if (cname.equals("")) {
            insideMatch = true;
          } else {
            NewClassTree nc = (NewClassTree) tree;
            checkAnon = nc.getClassBody() != null;
          }
          break;
        case METHOD:
        case VARIABLE:
          // Avoid searching inside inner classes of the matching class,
          // lest a homographic inner class lead to a spurious match.
          if (insideMatch) {
            debug("false InClassCriterion.isSatisfiedBy:%n  cname=%s%n  tree=%s%n", cname, tree);
            return false;
          }
          break;
        default:
          // nothing to do
          break;
      }

      if (checkAnon) {
        // If block is anonymous class, and cname starts with an
        // anonymous class index, see if they match.

        Matcher anonclassMatcher = anonclassPattern.matcher(cname);
        if (!anonclassMatcher.matches()) {
          debug(
              "false[anonclassMatcher] InClassCriterion.isSatisfiedBy:%n  cname=%s%n  tree=%s%n",
              cname, tree);
          return false;
        }
        // for JDK 7: String anonclassNumString = anonclassMatcher.group("num");
        // for JDK 7: cname = anonclassMatcher.group("remaining");
        String anonclassNumString = anonclassMatcher.group(1);
        cname = anonclassMatcher.group(3);
        if (cname == null) {
          cname = "";
        }
        int anonclassNum;
        try {
          anonclassNum = Integer.parseInt(anonclassNumString);
        } catch (NumberFormatException e) {
          throw new Error("This can't happen: " + cname + "$" + anonclassNumString, e);
        }

        int actualIndexInSource = AnonymousClassScanner.indexOfClassTree(path, tree);

        if (anonclassNum != actualIndexInSource) {
          debug(
              "false[anonclassNum %d %d] InClassCriterion.isSatisfiedBy:%n  cname=%s%n  tree=%s%n",
              anonclassNum, actualIndexInSource, cname, tree);
          return false;
        }
      } else if (checkLocal) {
        ClassTree c = (ClassTree) tree;
        String treeClassName = c.getSimpleName().toString();

        Matcher localClassMatcher = localClassPattern.matcher(cname);
        if (!localClassMatcher.matches()) {
          debug(
              "false[localClassMatcher] InClassCriterion.isSatisfiedBy:%n  cname=%s%n  tree=%s%n",
              cname, tree);
          return false;
        }
        String localClassNumString = localClassMatcher.group(1);
        String localClassName = localClassMatcher.group(2);
        int localClassNum = Integer.parseInt(localClassNumString);

        int actualIndexInSource = LocalClassScanner.indexOfClassTree(path, c);

        if (actualIndexInSource == localClassNum && treeClassName.startsWith(localClassName)) {
          cname = localClassMatcher.group(4);
          if (cname == null) {
            cname = "";
          }
        } else {
          debug(
              "false[localClassNum %d %d] InClassCriterion.isSatisfiedBy:%n  cname=%s%n  tree=%s%n",
              localClassNum, actualIndexInSource, cname, tree);
          return false;
        }
      }
    }

    debug(
        "%s InClassCriterion.isSatisfiedBy:%n  cname=%s%n  tree=%s%n",
        cname.equals(""), cname, path.getLeaf());
    return cname.equals("");
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return false;
  }

  @Override
  public String toString() {
    return "In class '" + className + "'" + (exactMatch ? " (exactly)" : "");
  }

  /**
   * Print debugging output to System.out.
   *
   * @param message a format string
   * @param args arguments to the format string
   */
  @FormatMethod
  private static void debug(String message, Object... args) {
    if (debug) {
      System.out.printf(message, args);
    }
  }
}
