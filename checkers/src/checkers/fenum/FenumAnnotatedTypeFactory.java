package checkers.fenum;

import checkers.types.BasicAnnotatedTypeFactory;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

public class FenumAnnotatedTypeFactory extends
        BasicAnnotatedTypeFactory<FenumChecker> {

  public FenumAnnotatedTypeFactory(FenumChecker checker,
                                   CompilationUnitTree root) {

    super(checker, root);

    // Reuse the framework Bottom annotation and make it the default for the
    // null literal.
    treeAnnotator.addTreeKind(Tree.Kind.NULL_LITERAL, checker.BOTTOM);
    typeAnnotator.addTypeName(java.lang.Void.class, checker.BOTTOM);

    this.postInit();
    // flow.setDebug(System.err);
  }

}
