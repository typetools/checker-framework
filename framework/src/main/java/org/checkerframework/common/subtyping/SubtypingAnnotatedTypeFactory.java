package org.checkerframework.common.subtyping;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.subtyping.qual.Unqualified;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotationClassLoader;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.UserError;
import org.plumelib.reflection.Signatures;

/** Defines {@link #createSupportedTypeQualifiers}. */
public class SubtypingAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  @SuppressWarnings("this-escape")
  public SubtypingAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    postInit();
  }

  @Override
  protected AnnotationClassLoader createAnnotationClassLoader() {
    return new SubtypingAnnotationClassLoader(checker);
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    // Subtyping Checker doesn't have a qual directory, so we instantiate the loader here to
    // load externally declared annotations
    loader = createAnnotationClassLoader();

    Set<Class<? extends Annotation>> qualSet = new LinkedHashSet<>();

    // load individually named qualifiers
    for (String qualName : checker.getStringsOption("quals", ',')) {
      if (!Signatures.isBinaryName(qualName)) {
        throw new UserError("Malformed qualifier \"%s\" in -Aquals", qualName);
      }
      Class<? extends Annotation> anno = loader.loadExternalAnnotationClass(qualName);
      if (anno == null) {
        throw new UserError("Qualifier specified in -Aquals not found: " + qualName);
      }
      qualSet.add(anno);
    }

    // load directories of qualifiers
    for (String dirName : checker.getStringsOption("qualDirs", ':')) {
      if (!new File(dirName).exists()) {
        throw new UserError("Directory specified in -AqualsDir does not exist: %s", dirName);
      }
      Set<Class<? extends Annotation>> annos =
          loader.loadExternalAnnotationClassesFromDirectory(dirName);
      if (annos.isEmpty()) {
        throw new UserError("Directory specified in -AqualsDir contains no qualifiers: " + dirName);
      }
      qualSet.addAll(annos);
    }

    if (qualSet.isEmpty()) {
      throw new UserError("SubtypingChecker: no qualifiers specified via -Aquals or -AqualDirs");
    }

    // check for subtype meta-annotation
    for (Class<? extends Annotation> qual : qualSet) {
      Annotation subtypeOfAnnotation = qual.getAnnotation(SubtypeOf.class);
      if (subtypeOfAnnotation != null) {
        for (Class<? extends Annotation> superqual : qual.getAnnotation(SubtypeOf.class).value()) {
          if (!qualSet.contains(superqual)) {
            throw new UserError(
                "SubtypingChecker: qualifier "
                    + qual
                    + " was specified via -Aquals but its super-qualifier "
                    + superqual
                    + " was not");
          }
        }
      }
    }

    return qualSet;
  }

  /**
   * If necessary, make Unqualified the default qualifier. Keep most logic in sync with super.
   *
   * @see
   *     org.checkerframework.framework.type.GenericAnnotatedTypeFactory#addCheckedCodeDefaults(org.checkerframework.framework.util.defaults.QualifierDefaults)
   */
  @Override
  protected void addCheckedCodeDefaults(QualifierDefaults defs) {
    boolean foundOtherwise = false;
    // Add defaults from @DefaultFor and @DefaultQualifierInHierarchy
    for (Class<? extends Annotation> qual : getSupportedTypeQualifiers()) {
      DefaultFor defaultFor = qual.getAnnotation(DefaultFor.class);
      if (defaultFor != null) {
        TypeUseLocation[] locations = defaultFor.value();
        defs.addCheckedCodeDefaults(AnnotationBuilder.fromClass(elements, qual), locations);
        foundOtherwise =
            foundOtherwise || Arrays.asList(locations).contains(TypeUseLocation.OTHERWISE);
      }

      if (qual.getAnnotation(DefaultQualifierInHierarchy.class) != null) {
        defs.addCheckedCodeDefault(
            AnnotationBuilder.fromClass(elements, qual), TypeUseLocation.OTHERWISE);
        foundOtherwise = true;
      }
    }
    // If Unqualified is a supported qualifier, make it the default.
    AnnotationMirror unqualified = AnnotationBuilder.fromClass(elements, Unqualified.class);
    if (!foundOtherwise && this.isSupportedQualifier(unqualified)) {
      defs.addCheckedCodeDefault(unqualified, TypeUseLocation.OTHERWISE);
    }
  }
}
