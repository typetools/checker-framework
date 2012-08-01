package checkers.fenum;

import java.util.Collections;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import checkers.fenum.quals.FenumTop;
import checkers.fenum.quals.FenumUnqualified;
import checkers.quals.DefaultLocation;
import checkers.types.BasicAnnotatedTypeFactory;


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

}
