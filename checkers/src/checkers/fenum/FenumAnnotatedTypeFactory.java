package checkers.fenum;

import java.util.Collections;

import javax.lang.model.element.AnnotationMirror;

import checkers.fenum.quals.FenumTop;
import checkers.fenum.quals.FenumUnqualified;
import checkers.quals.DefaultLocation;
import checkers.types.BasicAnnotatedTypeFactory;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

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
