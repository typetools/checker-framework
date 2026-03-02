package org.checkerframework.afu.scenelib.el;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.util.coll.VivifyingMap;
import org.plumelib.util.MapsP;

/** An annotated class. */
public class AClass extends ADeclaration {
  /** The class's annotated type parameter bounds. */
  public final VivifyingMap<BoundLocation, ATypeElement> bounds =
      ATypeElement.<BoundLocation>newVivifyingLHMap_ATE();

  // -1 maps to superclass, non-negative integers map to implemented interfaces
  public final VivifyingMap<TypeIndexLocation, ATypeElement> extendsImplements =
      ATypeElement.<TypeIndexLocation>newVivifyingLHMap_ATE();

  /**
   * The class's annotated methods; a method's key consists of its name followed by its erased
   * signature in JVML format. For example, {@code foo()V} or {@code bar(B[I[[Ljava/lang/String;)I}.
   * The annotation scene library does not validate the keys, nor does it check that annotated
   * subelements of the {@link AMethod}s exist in the signature.
   */
  public final VivifyingMap<String, AMethod> methods = createMethodMap();

  public final VivifyingMap<Integer, ABlock> staticInits = createInitBlockMap();

  public final VivifyingMap<Integer, ABlock> instanceInits = createInitBlockMap();

  /** The class's annotated fields; map key is field name. */
  public final VivifyingMap<String, AField> fields = AField.<String>newVivifyingLHMap_AF();

  public final VivifyingMap<String, AExpression> fieldInits = createFieldInitMap();

  /**
   * The type element representing the class. Clients must call {@link #setTypeElement(TypeElement)}
   * before accessing this field.
   */
  private /*@MonotonicNonNull*/ TypeElement typeElement = null;

  /** The fully-qualified name of the annotated class. */
  public final String className;

  /** The simple class names any of this class's outer classes (or this class) that are enums. */
  private final HashSet<String> enums = new HashSet<>();

  /** The enum constants of this class, or null if this class is not an enum. */
  private /*@MonotonicNonNull*/ List<VariableElement> enumConstants = null;

  /**
   * The simple class names any of this class's outer classes (or this class) that are annotations.
   */
  private final HashSet<String> annotationTypes = new HashSet<>();

  /**
   * The simple class names any of this class's outer classes (or this class) that are interfaces.
   */
  private final HashSet<String> interfaces = new HashSet<>();

  /** The simple class names any of this class's outer classes (or this class) that are records. */
  private final HashSet<String> records = new HashSet<>();

  // debug fields to keep track of all classes created
  // private static List<AClass> debugAllClasses = new ArrayList<>();
  // private final List<AClass> allClasses;

  /**
   * Create a new AClass.
   *
   * @param className the fully-qualified name of the annotated class
   */
  AClass(String className) {
    super("class: " + className);
    this.className = className;
    // debugAllClasses.add(this);
    // allClasses = debugAllClasses;
  }

  /**
   * A copy constructor for AClass.
   *
   * @param clazz the AClass to copy
   */
  AClass(AClass clazz) {
    super(clazz);
    className = clazz.className;
    copyMapContents(clazz.bounds, bounds);
    copyMapContents(clazz.extendsImplements, extendsImplements);
    copyMapContents(clazz.fieldInits, fieldInits);
    copyMapContents(clazz.fields, fields);
    copyMapContents(clazz.instanceInits, instanceInits);
    copyMapContents(clazz.methods, methods);
    copyMapContents(clazz.staticInits, staticInits);
  }

  @Override
  public AClass clone() {
    return new AClass(this);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof AClass && ((AClass) o).equalsClass(this);
  }

  final boolean equalsClass(AClass o) {
    return super.equals(o)
        && className.equals(o.className)
        && bounds.equals(o.bounds)
        && methods.equals(o.methods)
        && fields.equals(o.fields)
        && extendsImplements.equals(o.extendsImplements);
  }

  @Override
  public int hashCode() {
    return super.hashCode()
        + bounds.hashCode()
        + methods.hashCode()
        + fields.hashCode()
        + staticInits.hashCode()
        + instanceInits.hashCode()
        + extendsImplements.hashCode();
  }

  @Override
  public boolean isEmpty() {
    return super.isEmpty()
        && bounds.isEmpty()
        && methods.isEmpty()
        && fields.isEmpty()
        && staticInits.isEmpty()
        && instanceInits.isEmpty()
        && extendsImplements.isEmpty();
  }

  @Override
  public void prune() {
    super.prune();
    bounds.prune();
    methods.prune();
    fields.prune();
    staticInits.prune();
    instanceInits.prune();
    extendsImplements.prune();
  }

  @Override
  public String toString() {
    return "AClass: " + className;
  }

  public String unparse() {
    return unparse("");
  }

  public String unparse(String linePrefix) {
    StringBuilder sb = new StringBuilder();
    sb.append(linePrefix);
    sb.append(toString());
    sb.append(System.lineSeparator());
    sb.append(linePrefix);
    sb.append("Annotations:" + System.lineSeparator());
    for (Annotation a : tlAnnotationsHere) {
      sb.append(linePrefix);
      sb.append("  " + a + System.lineSeparator());
    }
    sb.append(linePrefix);
    sb.append("Bounds:" + System.lineSeparator());
    MapsP.mapToStringMultiLine(sb, bounds, linePrefix + "  ");
    sb.append(linePrefix);
    sb.append("Extends/implements:" + System.lineSeparator());
    MapsP.mapToStringMultiLine(sb, extendsImplements, linePrefix + "  ");
    sb.append(linePrefix);
    sb.append("Fields:" + System.lineSeparator());
    MapsP.mapToStringMultiLine(sb, fields, linePrefix + "  ");
    sb.append(linePrefix);
    sb.append("Field Initializers:" + System.lineSeparator());
    MapsP.mapToStringMultiLine(sb, fieldInits, linePrefix + "  ");
    sb.append(linePrefix);
    sb.append("Static Initializers:" + System.lineSeparator());
    MapsP.mapToStringMultiLine(sb, staticInits, linePrefix + "  ");
    sb.append(linePrefix);
    sb.append("Instance Initializers:" + System.lineSeparator());
    MapsP.mapToStringMultiLine(sb, instanceInits, linePrefix + "  ");
    sb.append(linePrefix);
    sb.append("AST Typecasts:" + System.lineSeparator());
    MapsP.mapToStringMultiLine(sb, insertTypecasts, linePrefix + "  ");
    sb.append(linePrefix);
    sb.append("AST Annotations:" + System.lineSeparator());
    MapsP.mapToStringMultiLine(sb, insertAnnotations, linePrefix + "  ");
    sb.append(linePrefix);
    sb.append("Methods:" + System.lineSeparator());
    MapsP.mapToStringMultiLine(sb, methods, linePrefix + "  ");
    return sb.toString();
  }

  @Override
  public <R, T> R accept(ElementVisitor<R, T> v, T t) {
    return v.visitClass(this, t);
  }

  // Static methods

  private static VivifyingMap<String, AMethod> createMethodMap() {
    return new VivifyingMap<String, AMethod>(new LinkedHashMap<>()) {
      @Override
      public AMethod createValueFor(String k) {
        return new AMethod(k);
      }

      @Override
      public boolean isEmptyValue(AMethod v) {
        return v.isEmpty();
      }
    };
  }

  private static VivifyingMap<Integer, ABlock> createInitBlockMap() {
    return new VivifyingMap<Integer, ABlock>(new LinkedHashMap<>()) {
      @Override
      public ABlock createValueFor(Integer k) {
        return new ABlock("" + k);
      }

      @Override
      public boolean isEmptyValue(ABlock v) {
        return v.isEmpty();
      }
    };
  }

  private static VivifyingMap<String, AExpression> createFieldInitMap() {
    return new VivifyingMap<String, AExpression>(new LinkedHashMap<>()) {
      @Override
      public AExpression createValueFor(String k) {
        return new AExpression(k);
      }

      @Override
      public boolean isEmptyValue(AExpression v) {
        return v.isEmpty();
      }
    };
  }

  /**
   * Returns true if the given class is an enum.
   *
   * @param className the simple class name of this class or one of its outer classes
   * @return true if the given class is an enum
   */
  public boolean isEnum(String className) {
    return enums.contains(className);
  }

  /**
   * Returns true if this class is an enum.
   *
   * @return true if this class is an enum
   */
  public boolean isEnum() {
    return enums.contains(this.className);
  }

  /**
   * Marks the given simple class name as an enum. This method is used to mark outer classes of this
   * class that have not been vivified, meaning that only their names are available.
   *
   * <p>Note that this code will misbehave if a class has the same name as its inner enum, or
   * vice-versa, because this uses simple names.
   *
   * @param className the simple class name of this class or one of its outer classes
   */
  public void markAsEnum(String className) {
    enums.add(className);
  }

  /**
   * Returns the set of enum constants for this class, or null if this is not an enum.
   *
   * @return the enum constants, or null if this is not an enum
   */
  public /*@Nullable*/ List<VariableElement> getEnumConstants() {
    if (enumConstants == null) {
      return null;
    }
    return ImmutableList.copyOf(enumConstants);
  }

  /**
   * Marks this class as an enum.
   *
   * @param enumConstants the list of enum constants for the class
   */
  public void setEnumConstants(List<VariableElement> enumConstants) {
    if (this.enumConstants != null) {
      throw new Error(
          String.format(
              "setEnumConstants was called multiple times with arguments %s and %s",
              this.enumConstants, enumConstants));
    }
    this.enumConstants = new ArrayList<>(enumConstants);
  }

  /**
   * Returns true if the given class is an annotation.
   *
   * @param className the simple class name of this class or one of its outer classes
   * @return true if the given class is an annotation
   */
  public boolean isAnnotation(String className) {
    return annotationTypes.contains(className);
  }

  /**
   * Returns true if this class is an annotation.
   *
   * @return true if this class is an annotation
   */
  public boolean isAnnotation() {
    return annotationTypes.contains(this.className);
  }

  /**
   * Marks the given simple name as an annotation.
   *
   * @param className the simple class name of this class or one of its outer classes
   * @see #markAsEnum
   */
  public void markAsAnnotation(String className) {
    annotationTypes.add(className);
  }

  /**
   * Returns true if the given class is an interface.
   *
   * @param className the simple class name of this class or one of its outer classes
   * @return true if the given class is an interface
   */
  public boolean isInterface(String className) {
    return interfaces.contains(className);
  }

  /**
   * Returns true if this class is an interface.
   *
   * @return true if this class is an interface
   */
  public boolean isInterface() {
    return interfaces.contains(this.className);
  }

  /**
   * Marks the given simple name as an interface.
   *
   * @param className the simple class name of this class or one of its outer classes
   * @see #markAsEnum
   */
  public void markAsInterface(String className) {
    interfaces.add(className);
  }

  /**
   * Returns true if the given class is an record.
   *
   * @param className the simple class name of this class or one of its outer classes
   * @return true if the given class is an record
   */
  public boolean isRecord(String className) {
    return records.contains(className);
  }

  /**
   * Returns true if this class is an record.
   *
   * @return true if this class is an record
   */
  public boolean isRecord() {
    return records.contains(this.className);
  }

  /**
   * Marks the given simple name as a record.
   *
   * @param className the simple class name of this class or one of its outer classes
   * @see #markAsEnum
   */
  public void markAsRecord(String className) {
    records.add(className);
  }

  /**
   * Returns the type of the class, or null if it is unknown. Callers should ensure that either:
   *
   * <ul>
   *   <li>{@link #setTypeElement(TypeElement)} has been called, or
   *   <li>the return value is checked against null.
   * </ul>
   *
   * @return a type element representing this class
   */
  public /*@Nullable*/ TypeElement getTypeElement() {
    return typeElement;
  }

  /**
   * Set the type element representing the class.
   *
   * @param typeElement the type element representing the class
   */
  public void setTypeElement(TypeElement typeElement) {
    if (this.typeElement == null) {
      this.typeElement = typeElement;
    } else if (!this.typeElement.equals(typeElement)) {
      throw new Error(
          String.format("setTypeElement(%s): type is already %s", typeElement, this.typeElement));
    }
  }

  /**
   * Returns all the methods that have been vivified on a class.
   *
   * @return a map from method signature (in JVM format) to the object representing the method
   */
  public Map<String, AMethod> getMethods() {
    return ImmutableMap.copyOf(methods);
  }

  /**
   * Returns all the fields that have been vivified on a class.
   *
   * @return a map from field name to the object representing the field
   */
  public Map<String, AField> getFields() {
    return ImmutableMap.copyOf(fields);
  }

  /**
   * Returns the annotations on the class.
   *
   * @return the annotations, directly from scenelib
   */
  public Collection<? extends Annotation> getAnnotations() {
    return tlAnnotationsHere;
  }
}
