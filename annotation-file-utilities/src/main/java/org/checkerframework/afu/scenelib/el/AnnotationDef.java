package org.checkerframework.afu.scenelib.el;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.StringTokenizer;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.AnnotationBuilder;
import org.checkerframework.afu.scenelib.Annotations;
import org.checkerframework.afu.scenelib.field.AnnotationFieldType;
import org.checkerframework.afu.scenelib.util.MethodRecorder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

/**
 * An annotation type definition, consisting of the annotation name, its meta-annotations, and its
 * field names and types. {@code AnnotationDef}s are immutable. An AnnotationDef with a non-null
 * retention policy is called a "top-level annotation definition".
 */
public final class AnnotationDef extends AElement {

  /**
   * The binary name of the annotation type, such as "foo.Bar$Baz" for inner class Baz in class Bar
   * in package foo.
   */
  public final @BinaryName String name;

  /**
   * A map of the names of this annotation type's fields to their types. Since {@link
   * AnnotationDef}s are immutable, clients should not modify this map, and doing so will result in
   * an exception.
   */
  public Map<String, AnnotationFieldType> fieldTypes;

  /** Where the annotation definition came from, such as a file name. */
  public String source;

  /**
   * Constructs an annotation definition with the given name. You MUST call setFieldTypes afterward,
   * even if with an empty map. (Yuck.)
   *
   * @param name the binary name of the annotation type
   * @param source where the annotation came from, such as a filename
   */
  public AnnotationDef(@BinaryName String name, String source) {
    super("annotation: " + name);
    assert name != null;
    assert source != null;
    this.name = name;
    this.source = source;
  }

  /**
   * Returns a list of method names for a class in the order in which they occur in the .class file.
   * Note that the JDK method Class.getDeclaredMethods() does not preserve this order.
   *
   * @param name the ifully qualified name of the class to be read
   * @return a list of methods for the class
   */
  public static List<String> getDeclaredMethods(String name) {
    List<String> methods;
    try {
      ClassReader classReader = new ClassReader(name);
      MethodRecorder methodRecorder = new MethodRecorder(Opcodes.ASM8);
      classReader.accept(methodRecorder, 0);
      methods = methodRecorder.getMethods();
    } catch (IOException e) {
      methods = null;
      e.printStackTrace();
    }
    return methods;
  }

  // Problem:  I am not sure how to handle circularities (annotations meta-annotated with
  // themselves)
  /**
   * Returns an AnnotationDef for the given annotation type. It might have been looked up in adefs,
   * or created new and inserted in adefs.
   *
   * @param annoType the type for which to create an AnnotationDef
   * @param adefs a cache of known AnnotationDef objects
   * @return an AnnotationDef for the given annotation type
   */
  public static AnnotationDef fromClass(
      Class<? extends java.lang.annotation.Annotation> annoType, Map<String, AnnotationDef> adefs) {
    @SuppressWarnings("signature:assignment") // not an array, so ClassGetName => BinaryName
    @BinaryName String name = annoType.getName();
    assert name != null;

    if (adefs.containsKey(name)) {
      return adefs.get(name);
    }

    Map<String, AnnotationFieldType> fieldTypes = new LinkedHashMap<>();
    getDeclaredMethods(name)
        .forEach(m -> fieldTypes.put(m, null)); // dummy initialization sets order for the map

    for (Method m : annoType.getDeclaredMethods()) {
      AnnotationFieldType aft = AnnotationFieldType.fromClass(m.getReturnType(), adefs);
      fieldTypes.put(m.getName(), aft);
    }

    AnnotationDef result =
        new AnnotationDef(name, Annotations.noAnnotations, fieldTypes, "class " + annoType);
    adefs.put(name, result);

    // An annotation can be meta-annotated with itself, so add
    // meta-annotations after putting the annotation in the map.
    java.lang.annotation.Annotation[] jannos;
    try {
      jannos = annoType.getDeclaredAnnotations();
    } catch (Exception e) {
      printClasspath();
      throw new Error("Exception in anno.getDeclaredAnnotations() for anno = " + annoType, e);
    }
    for (java.lang.annotation.Annotation ja : jannos) {
      result.tlAnnotationsHere.add(new Annotation(ja, adefs));
    }

    return result;
  }

  /**
   * Constructs an empty (so far) annotation definition.
   *
   * @param name the binary name of the annotation
   * @param tlAnnotationsHere the meta-annotations that are directly on the annotation definition
   * @param source where the annotation came from, such as a filename
   */
  public AnnotationDef(@BinaryName String name, Set<Annotation> tlAnnotationsHere, String source) {
    super("annotation: " + name);
    assert name != null;
    assert source != null;
    this.name = name;
    this.source = source;
    if (tlAnnotationsHere != null) {
      this.tlAnnotationsHere.addAll(tlAnnotationsHere);
    }
  }

  /**
   * Constructs an annotation definition with the given name and field types. Uses {@link
   * #setFieldTypes} to protect the immutability of the annotation definition.
   *
   * @param name the binary name of the annotation
   * @param tlAnnotationsHere the meta-annotations that are directly on the annotation definition
   * @param fieldTypes the annotation's element types
   * @param source where the annotation came from, such as a filename
   */
  public AnnotationDef(
      @BinaryName String name,
      Set<Annotation> tlAnnotationsHere,
      Map<String, ? extends AnnotationFieldType> fieldTypes,
      String source) {
    this(name, tlAnnotationsHere, source);
    setFieldTypes(fieldTypes);
  }

  // This ovverride is necessary because AnnotationDef extends AElement, which implements Cloneable.
  @Override
  public AnnotationDef clone() {
    throw new UnsupportedOperationException("Can't duplicate an AnnotationDef");
  }

  /**
   * Sets the field types of this annotation. The field type map is copied and then wrapped in an
   * {@linkplain Collections#unmodifiableMap unmodifiable map} to protect the immutability of the
   * annotation definition.
   *
   * @param fieldTypes the annotation's element types
   */
  public void setFieldTypes(Map<String, ? extends AnnotationFieldType> fieldTypes) {
    this.fieldTypes = Collections.unmodifiableMap(new LinkedHashMap<>(fieldTypes));
  }

  /**
   * The retention policy for annotations of this type. If non-null, this is called a "top-level"
   * annotation definition. It may be null for annotations that are used only as a field of other
   * annotations.
   *
   * @return the retention policy for annotations of this type
   */
  public @Nullable RetentionPolicy retention() {
    if (tlAnnotationsHere.contains(Annotations.aRetentionClass)) {
      return RetentionPolicy.CLASS;
    } else if (tlAnnotationsHere.contains(Annotations.aRetentionRuntime)) {
      return RetentionPolicy.RUNTIME;
    } else if (tlAnnotationsHere.contains(Annotations.aRetentionSource)) {
      return RetentionPolicy.SOURCE;
    } else {
      return null;
    }
  }

  /**
   * Returns the contents of the java.lang.annotation.Target meta-annotation, or null if there is
   * none.
   *
   * @return the contents of the @Target meta-annotation, or null
   */
  public List<String> targets() {
    Annotation target = target();
    if (target == null) {
      return null;
    }
    @SuppressWarnings("unchecked")
    List<String> fieldValue = (List<String>) target.getFieldValue("value");
    return fieldValue;
  }

  /**
   * Returns the java.lang.annotation.Target meta-annotation, or null if there is none.
   *
   * @return the @Target meta-annotation, or null
   */
  public Annotation target() {
    for (Annotation anno : tlAnnotationsHere) {
      if (anno.def().equals(Annotations.adTarget)) {
        return anno;
      }
    }
    return null;
  }

  /**
   * True if this is valid in type annotation locations. It was meta-annotated
   * with @Target({ElementType.TYPE_USE, ...}).
   *
   * <p>Returns true if this is valid in type annotation locations and (possibly) declaration
   * locations. To test whether this is valid only in type annotation locations and not in
   * declaration locations, use {@link #isOnlyTypeAnnotation}.
   *
   * @return true iff this is a type annotation
   */
  public boolean isTypeAnnotation() {
    List<String> targets = targets();
    return targets != null && targets.contains("TYPE_USE");
  }

  /**
   * True if this is a type annotation but not a declaration annotation. It was meta-annotated
   * with @Target(ElementType.TYPE_USE) or @Target({ElementType.TYPE_USE, ElementType.TYPE})
   * or @Target({ElementType.TYPE, ElementType.TYPE_USE}).
   *
   * @return true iff this is valid only in type annotation locations
   * @see #isTypeAnnotation
   */
  public boolean isOnlyTypeAnnotation() {
    boolean result = Annotations.onlyTypeAnnotationTargets.contains(target());
    return result;
  }

  /**
   * This {@link AnnotationDef} equals {@code o} if and only if {@code o} is another nonnull {@link
   * AnnotationDef} and {@code this} and {@code o} define annotation types of the same name with the
   * same field names and types.
   */
  @Override
  public boolean equals(Object o) {
    return o instanceof AnnotationDef && ((AnnotationDef) o).equals(this);
  }

  /**
   * Returns true if this {@link AnnotationDef} equals {@code o}; a slightly faster variant of
   * {@link #equals(Object)} for when the argument is statically known to be another nonnull {@link
   * AnnotationDef}.
   *
   * @param o another AnnotationDef to compare this to
   * @return true if this is equal to the given value
   */
  public boolean equals(AnnotationDef o) {
    boolean sameName = name.equals(o.name);
    boolean sameMetaAnnotations = equalsElement(o);
    boolean sameFieldTypes = fieldTypes.equals(o.fieldTypes);
    // Can be useful for debugging
    if (false) {
      if (sameName && !(sameMetaAnnotations && sameFieldTypes)) {
        String message =
            String.format(
                "Warning: incompatible definitions of annotation %s%n  %s%n  %s%n", name, this, o);
        new Exception(message).printStackTrace(System.out);
      }
    }
    return sameName && sameMetaAnnotations && sameFieldTypes;
  }

  @Override
  public int hashCode() {
    return name.hashCode()
        // Omit tlAnnotationsHere, becase it should be unique and, more
        // importantly, including it causes an infinite loop.
        // + tlAnnotationsHere.hashCode()
        + fieldTypes.hashCode();
  }

  /**
   * Returns an {@code AnnotationDef} containing all the information from both arguments, or {@code
   * null} if the two arguments contradict each other. Currently this just {@linkplain
   * AnnotationFieldType#unify unifies the field types} to handle arrays of unknown element type,
   * which can arise via {@link AnnotationBuilder#addEmptyArrayField}.
   *
   * <p>As a special case, if one annotation has no elements, the other one's elements are used.
   *
   * @param def1 the first AnnotationDef to unify
   * @param def2 the second AnnotationDef to unify
   * @return an AnnotationDef that contains all the fields of either argument
   */
  public static AnnotationDef unify(AnnotationDef def1, AnnotationDef def2) {
    // System.out.printf("unify(%s, %s)%n", def1, def2);
    if (def1.equals(def2)) {
      return def1;
    } else if (def1.name.equals(def2.name) && def1.equalsElement(def2)) {
      Set<String> ks1 = def1.fieldTypes.keySet();
      Set<String> ks2 = def2.fieldTypes.keySet();
      if (ks1.isEmpty() || ks2.isEmpty() || ks1.equals(ks2)) {
        Map<String, AnnotationFieldType> newFieldTypes = new LinkedHashMap<>();
        for (String fieldName : def1.fieldTypes.keySet()) {
          AnnotationFieldType aft1 = def1.fieldTypes.get(fieldName);
          AnnotationFieldType aft2 = def2.fieldTypes.get(fieldName);
          AnnotationFieldType uaft =
              aft1 == null ? aft2 : aft2 == null ? aft1 : AnnotationFieldType.unify(aft1, aft2);
          if (uaft == null) {
            return null;
          } else {
            newFieldTypes.put(fieldName, uaft);
          }
        }
        return new AnnotationDef(
            def1.name,
            def1.tlAnnotationsHere,
            newFieldTypes,
            String.format("unify(%s, %s)", def1.source, def2.source));
      }
    }
    return null;
  }

  /** The printed representation is: "[meta-annos...] @name(args...)". */
  @Override
  public String toString() {

    String metaAnnos;
    if (tlAnnotationsHere.isEmpty()) {
      metaAnnos = "";
    } else {
      StringJoiner metaAnnosJoiner = new StringJoiner(" ", "[", "]");
      for (Annotation a : tlAnnotationsHere) {
        metaAnnosJoiner.add(a.toString());
      }
      metaAnnos = metaAnnosJoiner.toString() + " ";
    }

    StringJoiner args = new StringJoiner(",", "(", ")");
    for (Map.Entry<String, AnnotationFieldType> entry : fieldTypes.entrySet()) {
      args.add(entry.getValue().toString() + " " + entry.getKey());
    }

    return metaAnnos.toString() + "@" + name + args.toString();
  }

  /**
   * Returns a string representation of this object, useful for debugging.
   *
   * @return a string representation of this object, useful for debugging
   */
  public String toStringDebug() {
    return toString()
        + String.format("; source=%s, tlAnnotationsHere=%s", source, tlAnnotationsHere);
  }

  /** Prints the classpath. */
  public static void printClasspath() {
    System.out.println("Classpath:");
    StringTokenizer tokenizer =
        new StringTokenizer(System.getProperty("java.class.path"), File.pathSeparator);
    while (tokenizer.hasMoreTokens()) {
      String cpelt = tokenizer.nextToken();
      boolean exists = new File(cpelt).exists();
      if (!exists) {
        System.out.print(" non-existent:");
      }
      System.out.println("  " + cpelt);
    }
  }
}
