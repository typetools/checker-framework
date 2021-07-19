package org.checkerframework.framework.type.typeannotator;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Adds annotations to a type based on the use of a type. This class applies annotations specified
 * by {@link DefaultFor}; it is designed to be used in a {@link ListTypeAnnotator} constructed in
 * {@link GenericAnnotatedTypeFactory#createTypeAnnotator()} ()}
 *
 * <p>{@link DefaultForTypeAnnotator} traverses types deeply.
 *
 * <p>This class takes care of two of the attributes of {@link DefaultFor}; the others are handled
 * in {@link org.checkerframework.framework.util.defaults.QualifierDefaults}.
 *
 * @see ListTypeAnnotator
 */
public class DefaultForTypeAnnotator extends TypeAnnotator {

  /** Map from {@link TypeKind} to annotations. */
  private final Map<TypeKind, Set<AnnotationMirror>> typeKinds;
  /** Map from {@link AnnotatedTypeMirror} classes to annotations. */
  private final Map<Class<? extends AnnotatedTypeMirror>, Set<AnnotationMirror>> atmClasses;
  /** Map from fully qualified class name strings to annotations. */
  private final Map<String, Set<AnnotationMirror>> types;
  /**
   * A list where each element associates an annotation with name regexes and name exception
   * regexes.
   */
  private final ListOfNameRegexes listOfNameRegexes;

  /** {@link QualifierHierarchy} */
  private final QualifierHierarchy qualHierarchy;

  /**
   * Creates a {@link DefaultForTypeAnnotator} from the given checker, using that checker to
   * determine the annotations that are in the type hierarchy.
   */
  public DefaultForTypeAnnotator(AnnotatedTypeFactory typeFactory) {
    super(typeFactory);
    this.typeKinds = new EnumMap<>(TypeKind.class);
    this.atmClasses = new HashMap<>();
    this.types = new HashMap<>();
    this.listOfNameRegexes = new ListOfNameRegexes();

    this.qualHierarchy = typeFactory.getQualifierHierarchy();

    // Get type qualifiers from the checker.
    Set<Class<? extends Annotation>> quals = typeFactory.getSupportedTypeQualifiers();

    // For each qualifier, read the @DefaultFor annotation and put its types, kinds, and names
    // into maps.
    for (Class<? extends Annotation> qual : quals) {
      DefaultFor defaultFor = qual.getAnnotation(DefaultFor.class);
      if (defaultFor == null) {
        continue;
      }

      AnnotationMirror theQual = AnnotationBuilder.fromClass(typeFactory.getElementUtils(), qual);

      for (org.checkerframework.framework.qual.TypeKind typeKind : defaultFor.typeKinds()) {
        TypeKind mappedTk = mapTypeKinds(typeKind);
        addTypeKind(mappedTk, theQual);
      }

      for (Class<?> typeName : defaultFor.types()) {
        addTypes(typeName, theQual);
      }

      listOfNameRegexes.add(theQual, defaultFor);
    }
  }

  /**
   * Map between {@link org.checkerframework.framework.qual.TypeKind} and {@link
   * javax.lang.model.type.TypeKind}.
   *
   * @param typeKind the Checker Framework TypeKind
   * @return the javax TypeKind
   */
  private TypeKind mapTypeKinds(org.checkerframework.framework.qual.TypeKind typeKind) {
    return TypeKind.valueOf(typeKind.name());
  }

  /** Add default qualifier, {@code theQual}, for the given TypeKind. */
  public void addTypeKind(TypeKind typeKind, AnnotationMirror theQual) {
    boolean res = qualHierarchy.updateMappingToMutableSet(typeKinds, typeKind, theQual);
    if (!res) {
      throw new BugInCF(
          "TypeAnnotator: invalid update of typeKinds "
              + typeKinds
              + " at "
              + typeKind
              + " with "
              + theQual);
    }
  }

  /** Add default qualifier, {@code theQual}, for the given {@link AnnotatedTypeMirror} class. */
  public void addAtmClass(
      Class<? extends AnnotatedTypeMirror> typeClass, AnnotationMirror theQual) {
    boolean res = qualHierarchy.updateMappingToMutableSet(atmClasses, typeClass, theQual);
    if (!res) {
      throw new BugInCF(
          "TypeAnnotator: invalid update of atmClasses "
              + atmClasses
              + " at "
              + typeClass
              + " with "
              + theQual);
    }
  }

  /** Add default qualifier, {@code theQual}, for the given type. */
  public void addTypes(Class<?> clazz, AnnotationMirror theQual) {
    String typeNameString = clazz.getCanonicalName();
    boolean res = qualHierarchy.updateMappingToMutableSet(types, typeNameString, theQual);
    if (!res) {
      throw new BugInCF(
          "TypeAnnotator: invalid update of types " + types + " at " + clazz + " with " + theQual);
    }
  }

  @Override
  protected Void scan(AnnotatedTypeMirror type, Void p) {
    // If the type's fully-qualified name is in the appropriate map, annotate the type. Do this
    // before looking at kind or class, as this information is more specific.

    String qname;
    if (type.getKind() == TypeKind.DECLARED) {
      qname = TypesUtils.getQualifiedName((DeclaredType) type.getUnderlyingType());
    } else if (type.getKind().isPrimitive()) {
      qname = type.getUnderlyingType().toString();
    } else {
      qname = null;
    }

    if (qname != null) {
      Set<AnnotationMirror> fromQname = types.get(qname);
      if (fromQname != null) {
        type.addMissingAnnotations(fromQname);
      }
    }

    // If the type's kind or class is in the appropriate map, annotate the type.
    Set<AnnotationMirror> fromKind = typeKinds.get(type.getKind());
    if (fromKind != null) {
      type.addMissingAnnotations(fromKind);
    } else if (!atmClasses.isEmpty()) {
      Class<? extends AnnotatedTypeMirror> t = type.getClass();
      Set<AnnotationMirror> fromClass = atmClasses.get(t);
      if (fromClass != null) {
        type.addMissingAnnotations(fromClass);
      }
    }

    return super.scan(type, p);
  }

  /**
   * Adds standard rules. Currently, sets Void to bottom if no other qualifier is set for Void.
   * Also, see {@link LiteralTreeAnnotator#addStandardLiteralQualifiers()}.
   *
   * @return this
   */
  public DefaultForTypeAnnotator addStandardDefaults() {
    if (!types.containsKey(Void.class.getCanonicalName())) {
      for (AnnotationMirror bottom : qualHierarchy.getBottomAnnotations()) {
        addTypes(Void.class, bottom);
      }
    } else {
      Set<AnnotationMirror> annos = types.get(Void.class.getCanonicalName());
      for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
        if (qualHierarchy.findAnnotationInHierarchy(annos, top) == null) {
          addTypes(Void.class, qualHierarchy.getBottomAnnotation(top));
        }
      }
    }

    return this;
  }

  /**
   * Apply defaults based on a variable name to a type.
   *
   * @param type a type to apply defaults to
   * @param name the name of the variable that has type {@code type}, or the name of the method
   *     whose return type is {@code type}
   */
  public void defaultTypeFromName(AnnotatedTypeMirror type, String name) {
    // TODO: Check whether the annotation is applicable to this Java type?
    AnnotationMirror defaultAnno = listOfNameRegexes.getDefaultAnno(name);
    if (defaultAnno != null) {
      if (typeFactory
              .getQualifierHierarchy()
              .findAnnotationInHierarchy(type.getAnnotations(), defaultAnno)
          == null) {
        type.addAnnotation(defaultAnno);
      }
    }
  }

  @Override
  public Void visitExecutable(AnnotatedExecutableType type, Void aVoid) {
    ExecutableElement element = type.getElement();

    Iterator<AnnotatedTypeMirror> paramTypes = type.getParameterTypes().iterator();
    for (VariableElement paramElt : element.getParameters()) {
      String paramName = paramElt.getSimpleName().toString();
      AnnotatedTypeMirror paramType = paramTypes.next();
      defaultTypeFromName(paramType, paramName);
    }

    String methodName = element.getSimpleName().toString();
    AnnotatedTypeMirror returnType = type.getReturnType();
    defaultTypeFromName(returnType, methodName);

    return super.visitExecutable(type, aVoid);
  }

  /**
   * A list where each element associates an annotation with name regexes and name exception
   * regexes.
   */
  private static class ListOfNameRegexes extends ArrayList<NameRegexes> {

    static final long serialVersionUID = 20200218L;

    /**
     * Update this list from the {@code names} and {@code namesExceptions} fields of a @DefaultFor
     * annotation.
     *
     * @param theQual the qualifier that a @DefaultFor annotation is written on
     * @param defaultFor the @DefaultFor annotation written on {@code theQual}
     */
    void add(AnnotationMirror theQual, DefaultFor defaultFor) {
      if (defaultFor.names().length != 0) {
        NameRegexes thisName = new NameRegexes(theQual);
        for (String nameRegex : defaultFor.names()) {
          try {
            thisName.names.add(Pattern.compile(nameRegex));
          } catch (PatternSyntaxException e) {
            throw new TypeSystemError(
                "In annotation %s, names() value \"%s\" is not a regular expression",
                theQual, nameRegex);
          }
        }
        for (String namesExceptionsRegex : defaultFor.namesExceptions()) {
          try {
            thisName.namesExceptions.add(Pattern.compile(namesExceptionsRegex));
          } catch (PatternSyntaxException e) {
            throw new TypeSystemError(
                "In annotation %s, namesExceptions() value \"%s\" is not a regular expression",
                theQual, namesExceptionsRegex);
          }
        }
        add(thisName);
      } else if (defaultFor.namesExceptions().length != 0) {
        throw new TypeSystemError(
            "On annotation %s, %s has empty names() but nonempty namesExceptions()",
            theQual, defaultFor);
      }
    }

    /**
     * Returns the annotation that should be the default for a variable of the given name, or for
     * the return type of a method of the given name.
     *
     * @param name a variable name
     * @return the annotation that should be the default for a variable named {@code name}, or null
     *     if none
     */
    @Nullable AnnotationMirror getDefaultAnno(String name) {
      if (this.isEmpty()) {
        return null;
      }
      AnnotationMirror result = null;
      for (NameRegexes nameRegexes : this) {
        if (nameRegexes.matches(name)) {
          if (result == null) {
            result = nameRegexes.anno;
          } else {
            // This could combine the annotatations instead, but I think doing so
            // silently would confuse users.
            throw new TypeSystemError(
                "Multiple annotations are applicable to the name \"%s\"", name);
          }
        }
      }
      return result;
    }
  }

  /**
   * Associates an annotation with the variable names that cause the annotation to be chosen as a
   * default.
   */
  private static class NameRegexes {
    /** The annotation. */
    final AnnotationMirror anno;
    /** The name regexes. */
    final List<Pattern> names = new ArrayList<>(0);
    /** The name exception regexes. */
    final List<Pattern> namesExceptions = new ArrayList<>(0);

    /**
     * Constructs a NameRegexes from a @DefaultFor annotation.
     *
     * @param theQual the qualifier that {@code defaultFor} is written on
     */
    NameRegexes(AnnotationMirror theQual) {
      this.anno = theQual;
    }

    /**
     * Returns true if the regular expressions match the given name -- that is, if {@link #anno}
     * should be used as the default type for a variable named {@code name}, or for the return type
     * of a method named {@code name}.
     *
     * @param name a variable or method name
     * @return true if {@link #anno} should be used as the default for a variable named {@code name}
     */
    public boolean matches(String name) {
      return names.stream().anyMatch(p -> p.matcher(name).matches())
          && namesExceptions.stream().noneMatch(p -> p.matcher(name).matches());
    }
  }
}
