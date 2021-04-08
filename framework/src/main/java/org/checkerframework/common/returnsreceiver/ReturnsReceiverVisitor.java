package org.checkerframework.common.returnsreceiver;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/** The visitor for the Returns Receiver Checker. */
public class ReturnsReceiverVisitor extends BaseTypeVisitor<ReturnsReceiverAnnotatedTypeFactory> {

  /**
   * Create a new {@code ReturnsReceiverVisitor}.
   *
   * @param checker the type-checker associated with this visitor
   */
  public ReturnsReceiverVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  public Void visitAnnotation(AnnotationTree node, Void p) {
    AnnotationMirror annot = TreeUtils.annotationFromAnnotationTree(node);
    // Warn if a @This annotation is in an illegal location.
    if (AnnotationUtils.areSame(annot, getTypeFactory().THIS_ANNOTATION)) {
      TreePath parentPath = getCurrentPath().getParentPath();
      Tree parent = parentPath.getLeaf();
      Tree grandparent = parentPath.getParentPath().getLeaf();
      Tree greatGrandparent = parentPath.getParentPath().getParentPath().getLeaf();
      boolean isReturnAnnot =
          grandparent instanceof MethodTree
              && (parent.equals(((MethodTree) grandparent).getReturnType())
                  || parent instanceof ModifiersTree);
      boolean isReceiverAnnot =
          greatGrandparent instanceof MethodTree
              && grandparent.equals(((MethodTree) greatGrandparent).getReceiverParameter())
              && parent.equals(((VariableTree) grandparent).getModifiers());
      boolean isCastAnnot =
          grandparent instanceof TypeCastTree
              && parent.equals(((TypeCastTree) grandparent).getType());
      if (!(isReturnAnnot || isReceiverAnnot || isCastAnnot)) {
        checker.reportError(node, "type.invalid.this.location");
      }
      if (isReturnAnnot
          && ElementUtils.isStatic(TreeUtils.elementFromDeclaration((MethodTree) grandparent))) {
        checker.reportError(node, "type.invalid.this.location");
      }
    }
    return super.visitAnnotation(node, p);
  }
}
