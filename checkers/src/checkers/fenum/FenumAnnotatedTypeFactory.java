package checkers.fenum;

import java.util.Collections;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

import checkers.basetype.BaseTypeChecker;
import checkers.fenum.quals.FenumTop;
import checkers.fenum.quals.FenumUnqualified;
import checkers.quals.DefaultLocation;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;


public class FenumAnnotatedTypeFactory extends
        BasicAnnotatedTypeFactory<FenumChecker> {

  public FenumAnnotatedTypeFactory(FenumChecker checker,
                                   CompilationUnitTree root) {

    super(checker, root);

    // Reuse the framework Bottom annotation and make it the default for the
    // null literal.
    treeAnnotator.addTreeKind(Tree.Kind.NULL_LITERAL, checker.BOTTOM);

    defaults.addAbsoluteDefault( this.annotations.fromClass(FenumUnqualified.class),
                                 Collections.singleton(DefaultLocation.ALL_EXCEPT_LOCALS));
    defaults.setLocalVariableDefault(Collections.singleton(annotations.fromClass(FenumTop.class)));

    this.postInit();
    // flow.setDebug(System.err);
  }

  @Override
  protected TreeAnnotator createTreeAnnotator(FenumChecker checker) {
    return new FenumTreeAnnotator(checker);
  }

  /**
   * A class for adding annotations based on tree
   */
  private class FenumTreeAnnotator  extends TreeAnnotator {

    FenumTreeAnnotator(BaseTypeChecker checker) {
      super(checker, FenumAnnotatedTypeFactory.this);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
      /*
       * For some reason we did not get a default qualifier applied to the tree.
       * The default qualifier would be the wrong qualifier in some cases, but better than nothing.
       * The implementation below looks for the least upper bound of both sides and returns it.
       */
      ExpressionTree var = node.getVariable();
      ExpressionTree expr = node.getExpression();
      AnnotatedTypeMirror varType = getAnnotatedType(var);
      AnnotatedTypeMirror exprType = getAnnotatedType(expr);
      Set<AnnotationMirror> lub = qualHierarchy.leastUpperBounds(varType.getAnnotations(), exprType.getAnnotations());
      type.clearAnnotations();
      type.addAnnotations(lub);
      return super.visitCompoundAssignment(node, type);
    }
  }
}
