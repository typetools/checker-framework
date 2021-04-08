package org.checkerframework.framework.util.element;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TargetType;
import java.util.List;
import javax.lang.model.element.TypeElement;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.element.ElementAnnotationUtil.UnexpectedAnnotationLocationException;

/**
 * When discovering supertypes of an AnnotatedTypeMirror we want to annotate each supertype with the
 * annotations of the subtypes class declaration. This class provides static methods to do this for
 * a list of supertypes. An instance of this class handles ONE supertype.
 */
public class SuperTypeApplier extends IndexedElementAnnotationApplier {

  /**
   * Annotates each supertype with annotations from subtypeElement's extends/implements clauses.
   *
   * @param supertypes supertypes to annotate
   * @param subtypeElement element that may have annotations to apply to supertypes
   */
  public static void annotateSupers(
      List<AnnotatedTypeMirror.AnnotatedDeclaredType> supertypes, TypeElement subtypeElement)
      throws UnexpectedAnnotationLocationException {
    for (int i = 0; i < supertypes.size(); i++) {
      final AnnotatedTypeMirror supertype = supertypes.get(i);
      // Offset i by -1 since typeIndex should start from -1.
      // -1 represents the (implicit) extends clause class.
      // 0 and greater represent the implements clause interfaces.
      // For details see the JSR 308 specification:
      // http://types.cs.washington.edu/jsr308/specification/java-annotation-design.html#class-file%3Aext%3Ari%3Aextends
      final int typeIndex = i - 1;
      new SuperTypeApplier(supertype, subtypeElement, typeIndex).extractAndApply();
    }
  }

  private final Symbol.ClassSymbol subclassSymbol;

  /**
   * The type_index of the supertype being annotated.
   *
   * <p>Note: Due to the semantics of TypeAnnotationPosition, type_index/index numbering works as
   * follows:
   *
   * <p>If subtypeElement represents a class and not an interface:
   *
   * <p>then the first member of supertypes represents the object and the relevant type_index = -1;
   * interface indices are offset by 1.
   *
   * <p>else all members of supertypes represent interfaces and their indices == their index in the
   * supertypes list
   */
  private final int index;

  /**
   * Note: This is not meant to be used in apply explicitly unlike all other AnnotationAppliers it
   * is intended to be used for annotate super types via the static annotateSuper method, hence the
   * private constructor.
   */
  SuperTypeApplier(
      final AnnotatedTypeMirror supertype, final TypeElement subclassElement, final int index) {
    super(supertype, subclassElement);
    this.subclassSymbol = (Symbol.ClassSymbol) subclassElement;
    this.index = index;
  }

  /**
   * Returns the type_index that should represent supertype.
   *
   * @return the type_index that should represent supertype
   */
  @Override
  public int getElementIndex() {
    return index;
  }

  /**
   * Returns the type_index of anno's TypeAnnotationPosition.
   *
   * @return the type_index of anno's TypeAnnotationPosition
   */
  @Override
  public int getTypeCompoundIndex(Attribute.TypeCompound anno) {
    int typeIndex = anno.getPosition().type_index;
    // TODO: this is a workaround of a bug in langtools
    // https://bugs.openjdk.java.net/browse/JDK-8164519
    // This bug is fixed in Java 9.
    return typeIndex == 0xffff ? -1 : typeIndex;
  }

  /**
   * Returns TargetType.CLASS_EXTENDS.
   *
   * @return TargetType.CLASS_EXTENDS
   */
  @Override
  protected TargetType[] annotatedTargets() {
    return new TargetType[] {TargetType.CLASS_EXTENDS};
  }

  /**
   * Returns TargetType.CLASS_TYPE_PARAMETER, TargetType.CLASS_TYPE_PARAMETER_BOUND.
   *
   * @return TargetType.CLASS_TYPE_PARAMETER, TargetType.CLASS_TYPE_PARAMETER_BOUND
   */
  @Override
  protected TargetType[] validTargets() {
    return new TargetType[] {
      TargetType.CLASS_TYPE_PARAMETER, TargetType.CLASS_TYPE_PARAMETER_BOUND
    };
  }

  /**
   * Returns the TypeCompounds (annotations) of the subclass.
   *
   * @return the TypeCompounds (annotations) of the subclass
   */
  @Override
  protected Iterable<Attribute.TypeCompound> getRawTypeAttributes() {
    return subclassSymbol.getRawTypeAttributes();
  }

  @Override
  protected void handleTargeted(List<TypeCompound> targeted)
      throws UnexpectedAnnotationLocationException {
    ElementAnnotationUtil.annotateViaTypeAnnoPosition(type, targeted);
  }

  @Override
  protected boolean isAccepted() {
    return true;
  }
}
