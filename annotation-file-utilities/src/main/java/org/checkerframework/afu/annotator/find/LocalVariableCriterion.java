package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Pair;
import java.util.List;
import java.util.Objects;
import org.checkerframework.afu.annotator.scanner.LocalVariableScanner;
import org.checkerframework.afu.scenelib.el.LocalLocation;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Criterion for being a specific local variable.
 *
 * <p>This matches the variable itself (in the local variable definition), and also anywhere in its
 * type, but should not match in the initializer.
 */
public class LocalVariableCriterion implements Criterion {

  private final String fullMethodName;
  private final LocalLocation loc;

  public LocalVariableCriterion(String methodName, LocalLocation loc) {
    this.fullMethodName = methodName.substring(0, methodName.indexOf(")") + 1);
    this.loc = loc;
  }

  @Override
  public boolean isSatisfiedBy(@Nullable TreePath path, @FindDistinct Tree leaf) {
    if (path == null) {
      return false;
    }
    assert path.getLeaf() == leaf;
    return isSatisfiedBy(path);
  }

  @Override
  public boolean isSatisfiedBy(@Nullable TreePath path) {
    if (path == null) {
      return false;
    }

    TreePath parentPath = path.getParentPath();
    if (parentPath == null) {
      return false;
    }

    Tree parent = parentPath.getLeaf();
    Tree leaf = path.getLeaf();
    if (parent instanceof VariableTree) {
      // parent is a variable declaration

      if (parentPath.getParentPath().getLeaf() instanceof MethodTree) {
        // formal parameter, not local variable
        return false;
      }

      VariableTree vtt = (VariableTree) parent;
      if (leaf.equals(vtt.getInitializer())) {
        // don't match in initializer
        return false;
      }

      String varName = vtt.getName().toString();

      if (Objects.equals(loc.variableName, varName)) {
        int varIndex = LocalVariableScanner.indexOfVarTree(path, vtt, varName);
        return (loc.getVarIndex() == varIndex);
      }

      if (loc.scopeStartDefined()) {
        Pair<String, Pair<Integer, Integer>> key =
            Pair.of(fullMethodName, Pair.of(loc.getVarIndex(), loc.getScopeStart()));
        String potentialVarName = LocalVariableScanner.getFromMethodNameIndexMap(key);
        if (potentialVarName != null) {
          if (varName.equals(potentialVarName)) {
            // now use methodNameCounter to ensure that if this is the
            // i'th variable of this name, its offset is the i'th offset
            // of all variables with this name
            List<Integer> allOffsetsWithThisName =
                LocalVariableScanner.getFromMethodNameCounter(fullMethodName, potentialVarName);
            //      methodNameCounter.get(fullMethodName).get(potentialVarName);
            Integer thisVariablesOffset = allOffsetsWithThisName.indexOf(loc.getScopeStart());

            // now you need to make sure that this is the
            // thisVariablesOffset'th variable tree in the entire source
            int i = LocalVariableScanner.indexOfVarTree(path, parent, potentialVarName);

            if (i == thisVariablesOffset) {
              return true;
            }
          }
        }
      }
      return false;
    }
    // isSatisfiedBy should return true not just for the local variable itself, but for its type.
    // So, if this is part of a type, try its parent.
    // For example, return true for the tree for "Integer"
    // within the local variable "List<Integer> foo;"
    //
    // But, stop the search once it gets to certain types, such as MethodTree.
    // Is it OK to stop at ExpressionTree too?
    else if (parent instanceof MethodTree) {
      return false;
    } else {
      return this.isSatisfiedBy(parentPath);
    }
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return false;
  }

  @Override
  public Kind getKind() {
    return Kind.LOCAL_VARIABLE;
  }

  @Override
  public String toString() {
    return "LocalVariableCriterion: in: " + fullMethodName + " loc: " + loc;
  }
}
