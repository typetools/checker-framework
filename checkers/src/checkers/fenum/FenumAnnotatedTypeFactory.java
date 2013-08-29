package checkers.fenum;

import checkers.fenum.quals.FenumTop;
import checkers.fenum.quals.FenumUnqualified;
import checkers.quals.DefaultLocation;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.util.AnnotationUtils;

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

    defaults.addAbsoluteDefault(AnnotationUtils.fromClass(elements, FenumTop.class), DefaultLocation.LOCALS);
    defaults.addAbsoluteDefault(AnnotationUtils.fromClass(elements, FenumUnqualified.class), DefaultLocation.OTHERWISE);

    this.postInit();
    // flow.setDebug(System.err);
  }

}
