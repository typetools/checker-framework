package org.checkerframework.afu.scenelib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.afu.scenelib.el.AnnotationDef;
import org.checkerframework.afu.scenelib.field.AnnotationFieldType;
import org.checkerframework.afu.scenelib.field.ArrayAFT;
import org.checkerframework.afu.scenelib.field.ScalarAFT;
import org.checkerframework.checker.signature.qual.BinaryName;

/**
 * An {@link AnnotationBuilder} builds a single annotation object after the annotation's fields have
 * been supplied one by one.
 *
 * <p>It is not possible to specify the type name or the retention policy. Either the {@link
 * AnnotationBuilder} expects a certain definition (and may throw exceptions if the fields deviate
 * from it) or it determines the definition automatically from the supplied fields.
 *
 * <p>Each {@link AnnotationBuilder} is mutable and single-use; the purpose of an {@link
 * AnnotationFactory} is to produce as many {@link AnnotationBuilder}s as needed.
 */
public class AnnotationBuilder {

  /**
   * Sometimes, we build the AnnotationDef at the very end, and sometimes we have it before
   * starting.
   */
  AnnotationDef def;

  /** The name of the annotation being built. */
  private @BinaryName String typeName;

  /**
   * The top-level meta-annotations that appear directly on the annotation being built. "tl" stands
   * for "top-level".
   */
  Set<Annotation> tlAnnotationsHere;

  /** Where the annotation came from, such as a filename. */
  String source;

  boolean arrayInProgress = false;

  boolean active = true;

  // Generally, don't use this.  Use method fieldTypes() instead.
  private Map<String, AnnotationFieldType> fieldTypes = new LinkedHashMap<>();

  Map<String, Object> fieldValues = new LinkedHashMap<>();

  /**
   * Returns the name of the annotation.
   *
   * @return the name of the annotation
   */
  public @BinaryName String typeName() {
    if (def != null) {
      return def.name;
    } else {
      return typeName;
    }
  }

  public Map<String, AnnotationFieldType> fieldTypes() {
    if (def != null) {
      return def.fieldTypes;
    } else {
      return fieldTypes;
    }
  }

  class SimpleArrayBuilder implements ArrayBuilder {
    boolean abActive = true;

    String fieldName;
    AnnotationFieldType aft; // the type for the elements

    List<Object> arrayElements = new ArrayList<Object>();

    SimpleArrayBuilder(String fieldName, AnnotationFieldType aft) {
      assert aft != null;
      assert fieldName != null;
      this.fieldName = fieldName;
      this.aft = aft;
    }

    @Override
    public void appendElement(Object x) {
      if (!abActive) {
        throw new IllegalStateException("Array is finished");
      }
      if (!aft.isValidValue(x)) {
        throw new IllegalArgumentException(
            String.format(
                "Bad array element value%n  %s (%s)%nfor field %s%n  %s (%s)",
                x, x.getClass(), fieldName, aft, aft.getClass()));
      }
      arrayElements.add(x);
    }

    @Override
    public void finish() {
      if (!abActive) {
        throw new IllegalStateException("Array is finished");
      }
      fieldValues.put(fieldName, Collections.<Object>unmodifiableList(arrayElements));
      arrayInProgress = false;
      abActive = false;
    }
  }

  private void checkAddField(String fieldName) {
    if (!active) {
      throw new IllegalStateException("Already finished");
    }
    if (arrayInProgress) {
      throw new IllegalStateException("Array in progress");
    }
    if (fieldValues.containsKey(fieldName)) {
      throw new IllegalArgumentException("Duplicate field \'" + fieldName + "\' in " + fieldValues);
    }
  }

  /**
   * Supplies a scalar field of the given name, type, and value for inclusion in the annotation
   * returned by {@link #finish}. See the rules for values on {@link Annotation#getFieldValue}.
   *
   * <p>Each field may be supplied only once. This method may throw an exception if the {@link
   * AnnotationBuilder} expects a certain definition for the built annotation and the given field
   * does not exist in that definition or has the wrong type.
   *
   * @param fieldName the name of the annotation element to set
   * @param aft the element's type, which is a scalar type
   * @param x the element's value
   */
  public void addScalarField(String fieldName, ScalarAFT aft, Object x) {
    checkAddField(fieldName);
    if (x instanceof Annotation && !(x instanceof Annotation)) {
      throw new IllegalArgumentException("All subannotations must be Annotations");
    }
    if (def == null) {
      fieldTypes.put(fieldName, aft);
    }
    fieldValues.put(fieldName, x);
  }

  /**
   * Begins supplying an array field of the given name and type. The elements of the array must be
   * passed to the returned {@link ArrayBuilder} in order, and the {@link ArrayBuilder} must be
   * finished before any other methods on this {@link AnnotationBuilder} are called. <code>
   * aft.{@link ArrayAFT#elementType elementType}</code> must be known (not <code>null</code>).
   *
   * <p>Each field may be supplied only once. This method may throw an exception if the {@link
   * AnnotationBuilder} expects a certain definition for the built annotation and the given field
   * does not exist in that definition or has the wrong type.
   *
   * @param fieldName the name of the annotation element to set
   * @param aft the element's type, which is an array type
   */
  public ArrayBuilder beginArrayField(String fieldName, ArrayAFT aft) {
    checkAddField(fieldName);
    if (def == null) {
      fieldTypes.put(fieldName, aft);
    } else {
      aft = (ArrayAFT) fieldTypes().get(fieldName);
      if (aft == null) {
        throw new Error(
            String.format("Definition for %s lacks field %s:%n  %s", def.name, fieldName, def));
      }
      assert aft != null;
    }
    arrayInProgress = true;
    assert aft.elementType != null;
    return new SimpleArrayBuilder(fieldName, aft.elementType);
  }

  /**
   * Supplies an zero-element array field whose element type is unknown. The field type of this
   * array is represented by an {@link ArrayAFT} with {@link ArrayAFT#elementType elementType} ==
   * <code>null</code>.
   *
   * <p>This can sometimes happen due to a design flaw in the format of annotations in class files.
   * An array value does not specify an type itself; instead, each element carries a type. Thus, a
   * zero-length array carries no indication of its element type.
   *
   * @param fieldName the name of the annotation element to set to an empty array
   */
  public void addEmptyArrayField(String fieldName) {
    checkAddField(fieldName);
    if (def == null) {
      fieldTypes.put(fieldName, new ArrayAFT(null));
    }
    fieldValues.put(fieldName, Collections.emptyList());
  }

  /**
   * Returns the completed annotation. This method may throw an exception if the {@link
   * AnnotationBuilder} expects a certain definition for the built annotation and one or more fields
   * in that definition were not supplied. Once this method has been called, no more method calls
   * may be made on this {@link AnnotationBuilder}.
   *
   * @return the completed annotation corresponding to this builder
   */
  public Annotation finish() {
    if (!active) {
      throw new IllegalStateException("Already finished: " + this);
    }
    if (arrayInProgress) {
      throw new IllegalStateException("Array in progress: " + this);
    }
    active = false;
    if (def == null) {
      assert fieldTypes != null;
      def = new AnnotationDef(typeName, tlAnnotationsHere, fieldTypes, source);
    } else {
      assert typeName == null;
      assert fieldTypes.isEmpty();
    }
    return new Annotation(def, fieldValues);
  }

  AnnotationBuilder(AnnotationDef def, String source) {
    assert def != null;
    assert source != null;
    this.def = def;
    this.source = source;
  }

  /**
   * Create a new AnnotationBuilder.
   *
   * @param typeName the name of the annotation being built
   * @param source where the annotation came from, such as a filename
   */
  AnnotationBuilder(@BinaryName String typeName, String source) {
    assert typeName != null;
    assert source != null;
    this.typeName = typeName;
    this.source = source;
  }

  /**
   * Create a new AnnotationBuilder.
   *
   * @param typeName the name of the annotation being built
   * @param tlAnnotationsHere the top-level meta-annotations that appear directly on the annotation
   *     being built. "tl" stands for "top-level".
   * @param source where the annotation came from, such as a filename
   */
  AnnotationBuilder(@BinaryName String typeName, Set<Annotation> tlAnnotationsHere, String source) {
    assert typeName != null;
    assert source != null;
    this.typeName = typeName;
    this.tlAnnotationsHere = tlAnnotationsHere;
    this.source = source;
  }

  @Override
  public String toString() {
    if (def != null) {
      return String.format("AnnotationBuilder %s", def);
    } else {
      return String.format("(AnnotationBuilder %s : %s)", typeName, tlAnnotationsHere);
    }
  }
}
