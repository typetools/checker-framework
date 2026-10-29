package org.checkerframework.checker.modifiability.iterator;

import java.util.List;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.modifiability.ModifiabilityBaseAnnotatedTypeFactory;
import org.checkerframework.checker.modifiability.qual.IteratorPolyMod;
import org.checkerframework.checker.modifiability.qual.MaybeIteratorPolyMod;
import org.checkerframework.checker.modifiability.qual.PolyIteratorPolyMod;
import org.checkerframework.common.basetype.BaseTypeChecker;

/** The annotated type factory for the {@link IteratorChecker}. */
public class IteratorAnnotatedTypeFactory extends ModifiabilityBaseAnnotatedTypeFactory {

  /**
   * Creates an IteratorAnnotatedTypeFactory.
   *
   * @param checker the associated type-checker
   */
  @SuppressWarnings("this-escape")
  public IteratorAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(
        checker,
        MaybeIteratorPolyMod.class,
        IteratorPolyMod.class,
        null,
        null,
        PolyIteratorPolyMod.class);
    postInit();
  }

  @Override
  protected boolean expandsModifiabilityAliases() {
    return false;
  }

  @Override
  protected List<TypeMirror> typesWithCapability() {
    // Never called, because the whole-modifiability aliases do not expand into this hierarchy.
    return List.of();
  }
}
