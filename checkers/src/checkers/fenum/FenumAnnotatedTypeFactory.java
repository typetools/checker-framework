package checkers.fenum;

import checkers.fenum.quals.FenumTop;
import checkers.fenum.quals.FenumUnqualified;
import checkers.quals.DefaultLocation;
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

    defaults.addAbsoluteDefault(annotations.fromClass(FenumTop.class), DefaultLocation.LOCALS);
    defaults.addAbsoluteDefault(annotations.fromClass(FenumUnqualified.class), DefaultLocation.OTHERWISE);

    this.postInit();
    // flow.setDebug(System.err);
  }

}
