package org.checkerframework.checker.nonempty;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nonempty.qual.NonEmpty;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;

/** The type factory for the {@link NonEmptyChecker}. */
public class NonEmptyAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The @{@link NonEmpty} annotation. */
  public final AnnotationMirror NON_EMPTY = AnnotationBuilder.fromClass(elements, NonEmpty.class);

  /**
   * Creates a new {@link NonEmptyAnnotatedTypeFactory} that operates on a particular AST.
   *
   * @param checker the checker to use
   */
  @SuppressWarnings("this-escape")
  public NonEmptyAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.sideEffectsUnrefineAliases = true;
    this.postInit();
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(super.createTreeAnnotator(), new NonEmptyTreeAnnotator(this));
  }

  /** The tree annotator for the Non-Empty Checker. */
  private class NonEmptyTreeAnnotator extends TreeAnnotator {

    /**
     * Creates a new {@link NonEmptyTreeAnnotator}.
     *
     * @param aTypeFactory the type factory for this tree annotator
     */
    public NonEmptyTreeAnnotator(AnnotatedTypeFactory aTypeFactory) {
      super(aTypeFactory);
    }

    @Override
    public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {
      if (!type.hasEffectiveAnnotation(NON_EMPTY)) {
        List<? extends ExpressionTree> initializers = tree.getInitializers();
        if (initializers != null && !initializers.isEmpty()) {
          type.replaceAnnotation(NON_EMPTY);
        }
      }
      return super.visitNewArray(tree, type);
    }
  }
}
