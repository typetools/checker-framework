package org.checkerframework.afu.scenelib.el;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.util.coll.VivifyingMap;

/**
 * An {@link AElement} that represents a type might have annotations on inner types ("generic/array"
 * annotations in the design document). For example, "@A Map. @B Entry" has top-level annotation @A,
 * and @B is an annotation on an inner type.
 *
 * <p>Any element that can have an inner type postpended to it extends {@link ATypeElement} so that
 * their annotated inner types can be accessed in a uniform fashion. In the example above, "Map" is
 * an ATypeElement rather than just an AElement. (I think!)
 *
 * <p>An {@link AElement} holds the annotations on one inner type; {@link #innerTypes} maps
 * locations to inner types.
 */
public class ATypeElement extends AElement {

  /** The annotated inner types; map key is the inner type location. */
  public final VivifyingMap<List<TypePathEntry>, ATypeElement> innerTypes =
      ATypeElement.newVivifyingLHMap_ATE();

  /**
   * Construct a new ATypeElement from its description.
   *
   * @param description the description of the type element
   */
  ATypeElement(Object description) {
    super(description);
  }

  /**
   * Construct a new ATypeElement from another.
   *
   * @param elem the ATypeElement to copy
   */
  ATypeElement(ATypeElement elem) {
    super(elem);
    copyMapContents(elem.innerTypes, innerTypes);
  }

  @Override
  public ATypeElement clone() {
    return new ATypeElement(this);
  }

  void checkRep() {
    assert type == null;
  }

  @Override
  public boolean equals(AElement o) {
    return o instanceof ATypeElement && ((ATypeElement) o).equalsTypeElement(this);
  }

  // note:  does not call super.equals, so does not check name
  final boolean equalsTypeElement(ATypeElement o) {
    return equalsElement(o)
        && o.innerTypes.equals(innerTypes)
        && (type == null ? o.type == null : o.type.equals(type));
  }

  @Override
  public int hashCode() {
    checkRep();
    return tlAnnotationsHere.hashCode() + innerTypes.hashCode();
  }

  @Override
  public boolean isEmpty() {
    checkRep();
    return super.isEmpty() && innerTypes.isEmpty();
  }

  @Override
  public void prune() {
    super.prune();
    innerTypes.prune();
  }

  private static final String lineSep = System.getProperty("line.separator");

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ATypeElement: ");
    sb.append(description);
    sb.append(" : ");
    for (Annotation a : tlAnnotationsHere) {
      sb.append(a.toString());
      sb.append(" ");
    }
    sb.append("{");
    String linePrefix = "  ";
    for (Map.Entry<List<TypePathEntry>, ATypeElement> entry : innerTypes.entrySet()) {
      sb.append(linePrefix);
      sb.append(entry.getKey().toString());
      sb.append(" => ");
      sb.append(entry.getValue().toString());
      sb.append(lineSep);
    }
    sb.append("}");
    return sb.toString();
  }

  @Override
  public <R, T> R accept(ElementVisitor<R, T> v, T t) {
    return v.visitTypeElement(this, t);
  }

  static <K extends Object> VivifyingMap<K, ATypeElement> newVivifyingLHMap_ATE() {
    return new VivifyingMap<K, ATypeElement>(new LinkedHashMap<>()) {
      @Override
      public ATypeElement createValueFor(K k) {
        return new ATypeElement("" + k);
      }

      @Override
      public boolean isEmptyValue(ATypeElement v) {
        return v.isEmpty();
      }
    };
  }
}
