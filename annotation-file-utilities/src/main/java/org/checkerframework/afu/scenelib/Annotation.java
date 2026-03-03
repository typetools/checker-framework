package org.checkerframework.afu.scenelib;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.checkerframework.afu.scenelib.el.AnnotationDef;
import org.checkerframework.afu.scenelib.field.AnnotationFieldType;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A very simple annotation representation constructed with a map of field names to values. See the
 * rules for values on {@link Annotation#getFieldValue}; furthermore, subannotations must be {@link
 * Annotation}s. {@link Annotation}s are immutable.
 *
 * <p>{@link Annotation}s can be constructed directly or through {@link AnnotationFactory#saf}.
 * Either way works, but if you construct one directly, you must provide a matching {@link
 * AnnotationDef} yourself.
 */
public final class Annotation {

  /** The annotation definition. */
  public final AnnotationDef def;

  /** An unmodifiable copy of the passed map of field values. */
  public final Map<String, Object> fieldValues;

  /** Check the representation, throw assertion failure if it is violated. */
  public void checkRep() {
    assert fieldValues != null;
    assert fieldValues.keySet() != null;
    assert def != null;
    assert def.fieldTypes != null;
    assert def.fieldTypes.keySet() != null;
    if (!fieldValues.keySet().equals(def.fieldTypes.keySet())) {
      for (String s : fieldValues.keySet()) {
        assert def.fieldTypes.containsKey(s)
            : String.format(
                "Annotation contains field %s but AnnotationDef does not%n"
                    + "  annotation: %s%n"
                    + "  def: %s%n",
                s, this, this.def);
      }
      // TODO: Faulty assertions, fails when default value is used
      //            for (String s : def.fieldTypes.keySet()) {
      //                assert fieldValues.containsKey(s)
      //                    : String.format("AnnotationDef contains field %s but Annotation does
      // not", s);
      //            }
      //            assert false : "This can't happen.";
    }

    for (String fieldname : fieldValues.keySet()) {
      AnnotationFieldType aft = def.fieldTypes.get(fieldname);
      Object value = fieldValues.get(fieldname);
      String valueString;
      String classString = value.getClass().toString();
      if (value instanceof Object[]) {
        Object[] arr = (Object[]) value;
        valueString = Arrays.toString(arr);
        classString += " {";
        for (Object elt : arr) {
          classString += " " + elt.getClass();
        }
        classString += "}";
      } else if (value instanceof Collection) {
        Collection<?> coll = (Collection<?>) value;
        valueString = Arrays.toString(coll.toArray());
        classString += " {";
        for (Object elt : coll) {
          classString += " " + elt.getClass();
        }
        classString += " }";
      } else {
        valueString = value.toString();
        // No need to modify valueString.
      }
      assert aft.isValidValue(value)
          : String.format(
              "isValidValue returned false.  aft=%s [%s], value=%s = %s [%s] [%s]%n  def = %s",
              aft, aft.getClass(), value, valueString, value.getClass(), classString, def);
    }
  }

  // TODO make sure the field values are valid?
  /**
   * Constructs a {@link Annotation} with the given definition and field values. Make sure that the
   * field values obey the rules given on {@link Annotation#getFieldValue} and that subannotations
   * are also {@link Annotation}s; this constructor does not validate the values.
   *
   * @param def the definition for the constructed annotation
   * @param fields the fields for the constructed annotation
   */
  public Annotation(AnnotationDef def, Map<String, ? extends Object> fields) {
    this.def = def;
    this.fieldValues = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    checkRep();
  }

  /**
   * Construct an Annotation for the given java.lang.annotation.Annotation.
   *
   * @param ja the java.lang.annotation.Annotation to make an Annotation for
   * @param adefs a cache from which to look up (or insert into) AnnotationDefs
   */
  public Annotation(java.lang.annotation.Annotation ja, Map<String, AnnotationDef> adefs) {
    Class<? extends java.lang.annotation.Annotation> jaType = ja.annotationType();
    String name = jaType.getName();
    if (adefs.containsKey(name)) {
      def = adefs.get(name);
    } else {
      def = AnnotationDef.fromClass(jaType, adefs);
      adefs.put(name, def);
    }
    fieldValues = new LinkedHashMap<>();
    try {
      for (String fieldname : def.fieldTypes.keySet()) {
        AnnotationFieldType aft = def.fieldTypes.get(fieldname);
        Method m = jaType.getDeclaredMethod(fieldname);
        Object val = m.invoke(ja);
        if (!aft.isValidValue(val)) {
          if (val instanceof Class[]) {
            Class<?>[] vala = (Class[]) val;
            List<Class<?>> vall = new ArrayList<Class<?>>(vala.length);
            for (Class<?> elt : vala) {
              vall.add(elt);
            }
            val = vall;
          } else if (val instanceof Object[]) {
            Object[] vala = (Object[]) val;
            List<Object> vall = new ArrayList<>(vala.length);
            for (Object elt : vala) {
              vall.add(elt.toString());
            }
            val = vall;
          } else {
            val = val.toString();
          }
        }
        assert aft.isValidValue(val)
            : String.format(
                "invalid value \"%s\" for field \"%s\" of class \"%s\" and expected type \"%s\";"
                    + " ja=%s",
                val, val.getClass(), fieldname, aft, ja);
        fieldValues.put(fieldname, val);
      }
    } catch (NoSuchMethodException e) {
      throw new Error(
          String.format(
              "no such method (annotation field) in %s%n  from: %s %s", jaType, ja, adefs),
          e);
    } catch (InvocationTargetException e) {
      throw new Error(e);
    } catch (IllegalAccessException e) {
      throw new Error(e);
    }
    checkRep();
  }

  /**
   * Returns the value of the field whose name is given.
   *
   * <p>Everywhere in the annotation scene library, field values are to be represented as follows:
   *
   * <ul>
   *   <li>Primitive value: wrapper object, such as {@link Integer}.
   *   <li>{@link String}: {@link String}.
   *   <li>Class token: name of the type as a {@link String}, using the source code notation {@code
   *       int[]} for arrays.
   *   <li>Enumeration constant: name of the constant as a {@link String}.
   *   <li>Subannotation: {@code Annotation} object.
   *   <li>Array: {@link List} of elements in the formats defined here. If the element type is
   *       unknown (see {@link AnnotationBuilder#addEmptyArrayField}), the array must have zero
   *       elements.
   * </ul>
   *
   * @param fieldName the name of the field whose value to return
   * @return the value of the field named {@code fieldName}
   */
  public Object getFieldValue(String fieldName) {
    return fieldValues.get(fieldName);
  }

  /**
   * Returns the definition of the annotation type to which this annotation belongs.
   *
   * @return the definition of the annotation type to which this annotation belongs
   */
  public final AnnotationDef def() {
    return def;
  }

  /**
   * This {@link Annotation} equals {@code o} if and only if {@code o} is a nonnull {@link
   * Annotation} and {@code this} and {@code o} have recursively equal definitions and field values,
   * even if they were created by different {@link AnnotationFactory}s.
   */
  @Override
  public final boolean equals(Object o) {
    return o instanceof Annotation && equals((Annotation) o);
  }

  /**
   * Returns true if this annotation equals {@code o}; a slightly faster variant of {@link
   * #equals(Object)} for when the argument is statically known to be another nonnull {@link
   * Annotation}. Subclasses may wish to override this with a hard-coded "&amp;&amp;" of field
   * comparisons to improve performance.
   *
   * @param o the {@code Annotation} to compare to this
   * @return true if this equals {@code o}
   */
  public boolean equals(Annotation o) {
    return def.equals(o.def()) && fieldValues.equals(o.fieldValues);
  }

  /**
   * Returns the hash code of this annotation as defined on {@link Annotation#hashCode}. Subclasses
   * may wish to override this with a hard-coded XOR/addition of fields to improve performance.
   */
  @Override
  public int hashCode() {
    return def.hashCode() + fieldValues.hashCode();
  }

  /**
   * Returns a string representation of this annotation, using valid Java syntax.
   *
   * @return a string representation of this annotation, using valid Java syntax
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    toString(sb);
    return sb.toString();
  }

  /**
   * Formats this annotation, using valid Java syntax.
   *
   * @param sb where to format the annotation to
   */
  public void toString(StringBuilder sb) {
    // TODO: reduce duplication with
    // org.checkerframework.afu.annotator.specification.IndexFileSpecification.getElementAnnotations(AElement)

    // TODO: figure out how to consider abbreviated annotation names.
    // See org.checkerframework.afu.annotator.find.AnnotationInsertion.getText(boolean, boolean)
    sb.append("@");
    sb.append(def.name);
    if (fieldValues.size() == 1 && fieldValues.containsKey("value")) {
      @SuppressWarnings("nullness:assignment") // just checked containsKey
      @NonNull Object fieldValue = fieldValues.get("value");
      @SuppressWarnings("nullness:assignment") // same keyset
      @NonNull AnnotationFieldType fieldType = def.fieldTypes.get("value");
      sb.append('(');
      fieldType.format(sb, fieldValue);
      sb.append(')');
    } else if (!fieldValues.isEmpty()) {
      sb.append('(');
      boolean notfirst = false;
      for (Entry<String, Object> field : fieldValues.entrySet()) {
        // parameters of the annotation
        if (notfirst) {
          sb.append(", ");
        } else {
          notfirst = true;
        }
        sb.append(field.getKey() + "=");
        AnnotationFieldType fieldType = def.fieldTypes.get(field.getKey());
        fieldType.format(sb, field.getValue());
      }
      sb.append(')');
    }
  }
}

// package org.checkerframework.afu.annotations;
//
// import org.checkerframework.afu.scenelib.el.*;
// import Keyer;
//
// /**
//  * A top-level annotation containing an ordinary annotation plus a retention
//  * policy.  These are attached to {@link AElement}s.
//  */
// public final class Annotation {
//     public static final Keyer<String, Annotation> nameKeyer
//         = new Keyer<String, Annotation>() {
//         public String getKeyFor(
//                 Annotation v) {
//             return v.tldef.name;
//         }
//     };
//
//     /**
//      * The annotation definition.
//      */
//     public final AnnotationDef tldef;
//
//     /**
//      * The ordinary annotation, which contains the data and the ordinary
//      * definition.
//      */
//     public final Annotation ann;
//
//     /**
//      * Wraps the given annotation in a top-level annotation using the given
//      * top-level annotation definition, which provides a retention policy.
//      */
//     public Annotation(AnnotationDef tldef, Annotation ann) {
//         if (!ann.def().equals(tldef))
//             throw new IllegalArgumentException("Definitions mismatch");
//         this.tldef = tldef;
//         this.ann = ann;
//     }
//
//     /**
//      * Wraps the given annotation in a top-level annotation with the given
//      * retention policy, generating the top-level annotation definition
//      * automatically for convenience.
//      */
//     public Annotation(Annotation ann1,
//             RetentionPolicy retention) {
//         this(new AnnotationDef(ann1.def(), retention), ann1);
//     }
//
//     @Override
//     public int hashCode() {
//         return tldef.hashCode() + ann.hashCode();
//     }
//
//     @Override
//     public String toString() {
//       StringBuilder sb = new StringBuilder();
//       sb.append("tla: ");
//       sb.append(tldef.retention);
//       sb.append(":");
//       sb.append(ann.toString());
//       return sb.toString();
//     }
// }
