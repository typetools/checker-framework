package org.checkerframework.afu.scenelib.el;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.util.coll.VivifyingMap;

/**
 * An <code>AElement</code> represents a Java element and the annotations it carries. Some <code>
 * AElements</code> may contain others; for example, an {@link AClass} may contain {@link AMethod}s.
 * Every <code>AElement</code> usually belongs directly or indirectly to an {@link AScene}. Each
 * subclass of <code>AElement</code> represents one kind of annotatable element; its name should
 * make this clear.
 *
 * <p>The name {@code AElement} stands for "annotatable element" or "annotated element".
 */
public class AElement implements Cloneable {

  /**
   * The top-level annotations directly on this element. Annotations on subelements are in those
   * subelements' <code>tlAnnotationsHere</code> sets, not here.
   */
  public final Set<Annotation> tlAnnotationsHere;

  // TODO: What about methods; is `type` the return type?
  /**
   * The type of a field or a method parameter.
   *
   * <p>When this AElement object is a field or method parameter, its annotations are declaration
   * annotations, and the {@code type} field represents its type, whose annotations are type
   * annotations.
   *
   * <p>When this AElement object is NOT a field or method parameter, the {@code type} field is
   * null.
   */
  public final ATypeElement type; // initialized in constructor

  /**
   * A description of the element. Used for debugging and diagnostic messages. Almost always a
   * String, but in ATypeElement it is an ASTPath.
   */
  public Object description;

  /**
   * Create a new element with the given description.
   *
   * @param description the description of the element, used for debugging and diagnostic messages
   */
  AElement(Object description) {
    this(description, false);
  }

  AElement(Object description, boolean hasType) {
    this(description, hasType ? new ATypeElement("type of " + description) : null);
  }

  AElement(Object description, ATypeElement type) {
    tlAnnotationsHere = new LinkedHashSet<Annotation>();
    this.description = description;
    this.type = type;
  }

  AElement(AElement elem) {
    this(elem, elem.type);
  }

  AElement(AElement elem, ATypeElement type) {
    this(elem.description, type == null ? null : type.clone());
    tlAnnotationsHere.addAll(elem.tlAnnotationsHere);
  }

  AElement(AElement elem, Object description) {
    this(description, elem.type == null ? null : elem.type.clone());
    tlAnnotationsHere.addAll(elem.tlAnnotationsHere);
  }

  // Q: Are there any fields other than elements and maps that can't be shared?

  @Override
  public AElement clone() {
    return new AElement(this);
  }

  /**
   * Returns whether this {@link AElement} equals <code>o</code> (see warnings below). Generally
   * speaking, two {@link AElement}s are equal if they are of the same type, have the same {@link
   * #tlAnnotationsHere}, and have recursively equal, corresponding subelements. Two warnings:
   *
   * <ul>
   *   <li>While subelement collections usually remember the order in which subelements were added
   *       and regurgitate them in this order, two {@link AElement}s are equal even if order of
   *       subelements differs.
   *   <li>Two {@link AElement}s are unequal if one has a subelement that the other does not, even
   *       if the tree of elements rooted at the subelement contains no annotations. Thus, if you
   *       want to compare the <em>annotations</em>, you should {@link #prune} both {@link
   *       AElement}s first.
   * </ul>
   */
  @Override
  public boolean equals(Object o) {
    return o instanceof AElement && ((AElement) o).equals(this);
  }

  /**
   * Returns whether this {@link AElement} equals <code>o</code>. This is a slightly faster variant
   * of {@link #equals(Object)} for when the argument is statically known to be another nonnull
   * {@link AElement}.
   *
   * @param o the AElement to compare to
   * @return true if this is equal to {@code o}
   */
  // We need equals to be symmetric and operate correctly over the class
  // hierarchy.  Let x and y be objects of subclasses S and T, respectively,
  // of AElement.  x.equals((AElement) y) shall check that y is an S.  If so,
  // it shall call ((S) y).equalsS(x), which checks that x is a T and then
  // compares fields.
  public boolean equals(AElement o) {
    return o.equalsElement(this);
  }

  final boolean equalsElement(AElement o) {
    return o.tlAnnotationsHere.equals(tlAnnotationsHere)
        && (o.type == null ? type == null : o.type.equals(type));
  }

  @Override
  public int hashCode() {
    return getClass().getName().hashCode()
        + tlAnnotationsHere.hashCode()
        + (type == null ? 0 : type.hashCode());
  }

  /**
   * Returns whether this {@link AElement} is empty.
   *
   * @return true iff this is empty
   */
  public boolean isEmpty() {
    return tlAnnotationsHere.isEmpty() && (type == null || type.isEmpty());
  }

  /** Removes empty subelements of this {@link AElement} depth-first. */
  public void prune() {
    type.prune();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("AElement: ");
    sb.append(description);
    sb.append(" : ");
    tlAnnotationsHereFormatted(sb);
    if (type != null) {
      sb.append(' ');
      sb.append(type.toString());
    }
    // typeargs?
    return sb.toString();
  }

  /**
   * Return the top-level annotation on this that has the given name. Return null if no such
   * annotation exists.
   *
   * @param name the fully-qualified type name of the annotation to search for
   * @return the annotation on this with the given name, or null if none exists
   */
  public Annotation lookup(String name) {
    for (Annotation anno : tlAnnotationsHere) {
      if (anno.def.name.equals(name)) {
        return anno;
      }
    }
    return null;
  }

  public void tlAnnotationsHereFormatted(StringBuilder sb) {
    boolean first = true;
    for (Annotation aElement : tlAnnotationsHere) {
      if (!first) {
        sb.append(", ");
      }
      first = false;
      sb.append(aElement.toString());
    }
  }

  public <R, T> R accept(ElementVisitor<R, T> v, T t) {
    return v.visitElement(this, t);
  }

  // Static methods

  static <K extends Object> VivifyingMap<K, AElement> newVivifyingLHMap_AE() {
    return new VivifyingMap<K, AElement>(new LinkedHashMap<>()) {
      @Override
      public AElement createValueFor(K k) {
        return new AElement(k);
      }

      @Override
      public boolean isEmptyValue(AElement v) {
        return v.isEmpty();
      }
    };
  }

  // Different from the above in that the elements are guaranteed to
  // contain a non-null "type" field.
  static <K extends Object> VivifyingMap<K, AElement> newVivifyingLHMap_AET() {
    return new VivifyingMap<K, AElement>(new LinkedHashMap<>()) {
      @Override
      public AElement createValueFor(K k) {
        return new AElement(k, true);
      }

      @Override
      public boolean isEmptyValue(AElement v) {
        return v.isEmpty();
      }
    };
  }

  @SuppressWarnings("unchecked")
  static <K, V extends AElement> void copyMapContents(
      VivifyingMap<K, V> orig, VivifyingMap<K, V> copy) {
    for (K key : orig.keySet()) {
      V val = orig.get(key);
      copy.put(key, (V) val.clone());
    }
  }
}
