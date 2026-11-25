package org.checkerframework.framework.testchecker.h1h2checker;

import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Bot;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Invalid;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Poly;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S2;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Top;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2Bot;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2Poly;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2S2;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2Top;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;

public class H1H2AnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  AnnotationMirror H1S2;

  /**
   * The qualifier upper bounds for a use of a class named {@code H1S1Bounded}: {@code @H1S1} and
   * {@code @H2Top}.
   */
  AnnotationMirrorSet h1s1BoundedBounds;

  /**
   * Creates a new H1H2AnnotatedTypeFactory.
   *
   * @param checker the checker
   */
  @SuppressWarnings("this-escape")
  public H1H2AnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.postInit();
    H1S2 = AnnotationBuilder.fromClass(elements, H1S2.class);
    h1s1BoundedBounds = new AnnotationMirrorSet();
    h1s1BoundedBounds.add(AnnotationBuilder.fromClass(elements, H1S1.class));
    h1s1BoundedBounds.add(AnnotationBuilder.fromClass(elements, H2Top.class));
  }

  /**
   * Returns the qualifier upper bounds for a use of the type. For a class named {@code
   * H1S1Bounded}, they are {@code @H1S1} and {@code @H2Top}, though no annotation on the class says
   * so.
   *
   * @param type a type whose upper bounds to obtain
   * @return the set of qualifiers that are the upper bounds for a use of the type
   */
  @Override
  public AnnotationMirrorSet getTypeDeclarationBounds(TypeMirror type) {
    if (type.getKind() == TypeKind.DECLARED
        && ((DeclaredType) type).asElement().getSimpleName().contentEquals("H1S1Bounded")) {
      return h1s1BoundedBounds;
    }
    return super.getTypeDeclarationBounds(type);
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return getBundledTypeQualifiers(
        H1Top.class,
        H1S1.class,
        H1S2.class,
        H1Bot.class,
        H2Top.class,
        H2S1.class,
        H2S2.class,
        H2Bot.class,
        H1Poly.class,
        H2Poly.class,
        H1Invalid.class);
  }

  @Override
  protected void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
    super.addComputedTypeAnnotations(tree, type, iUseFlow);
    if (tree.getKind() == Tree.Kind.VARIABLE
        && ((VariableTree) tree).getName().toString().contains("addH1S2")) {
      type.replaceAnnotation(H1S2);
    }
  }
}
