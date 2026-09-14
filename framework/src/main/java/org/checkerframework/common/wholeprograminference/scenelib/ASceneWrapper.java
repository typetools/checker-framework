package org.checkerframework.common.wholeprograminference.scenelib;

import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.el.AClass;
import org.checkerframework.afu.scenelib.el.AField;
import org.checkerframework.afu.scenelib.el.AMethod;
import org.checkerframework.afu.scenelib.el.AScene;
import org.checkerframework.afu.scenelib.el.ATypeElement;
import org.checkerframework.afu.scenelib.el.DefException;
import org.checkerframework.afu.scenelib.el.TypePathEntry;
import org.checkerframework.afu.scenelib.io.IndexFileWriter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.wholeprograminference.AnnotationConverter;
import org.checkerframework.common.wholeprograminference.SceneToStubWriter;
import org.checkerframework.common.wholeprograminference.WholeProgramInference.OutputFormat;
import org.checkerframework.common.wholeprograminference.WholeProgramInferenceScenesStorage.AnnotationsInContexts;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.UserError;
import org.plumelib.util.CollectionsP;

/**
 * scene-lib (from the Annotation File Utilities) doesn't provide enough information to usefully
 * print stub files: it lacks information about what is and is not an enum, about the base types of
 * variables, and about formal parameter names.
 *
 * <p>This class wraps AScene but provides access to that missing information. This allows us to
 * preserve the code that generates .jaif files, while allowing us to sanely and safely keep the
 * information we need to generate stubs.
 *
 * <p>This class would be better as a subclass of AScene.
 */
public class ASceneWrapper {

  /** The AScene being wrapped. */
  private final AScene theScene;

  /**
   * Constructor. Pass the AScene to wrap.
   *
   * @param theScene the scene to wrap
   */
  public ASceneWrapper(AScene theScene) {
    this.theScene = theScene;
  }

  /**
   * Removes the specified annotations from {@code copy}, which must be a clone of {@code original}.
   *
   * <p>Two scenes are needed because {@code annosToRemove} is keyed by the ATypeElements of {@code
   * original}, but the annotations must be removed from {@code copy}, so that {@code original} --
   * which whole-program inference continues to use -- is not side-effected. The two scenes have the
   * same structure, so this method traverses them in lockstep.
   *
   * <p>This method visits the type annotations on fields, method return types, method receivers,
   * formal parameters, and inferred preconditions and postconditions. It does not visit the
   * declaration annotations on a class, method, or field, because those are stored on the {@link
   * AClass}, {@link AMethod}, or {@link AField} itself rather than on an {@link ATypeElement}. It
   * does, however, visit the declaration annotations on a formal parameter, because {@code
   * WholeProgramInferenceScenesStorage.addDeclarationAnnotationToFormalParameter} stores them in
   * the parameter's {@link ATypeElement} (that is, in {@code param.type.tlAnnotationsHere}), which
   * is the same set that this method removes annotations from.
   *
   * <p>Visiting a formal parameter's declaration annotations is nonetheless harmless, because
   * {@code annosToRemove} never contains the name of a declaration annotation. The only writer of
   * {@code annosToRemove} is {@code
   * WholeProgramInferenceScenesStorage.addAnnotationsToATypeElement}, which records only primary
   * annotations of an {@code AnnotatedTypeMirror}; that is, only type qualifiers supported by the
   * checker. No declaration annotation that whole-program inference writes, such as {@code @Owning}
   * or {@code @MustCallAlias}, is a supported type qualifier, so the names never collide.
   *
   * @param original the scene that {@code annosToRemove} refers to
   * @param copy a clone of {@code original}, from which to remove annotations
   * @param annosToRemove annotations that should not be added to .jaif or stub files
   */
  private static void removeAnnosFromScene(
      AScene original, AScene copy, AnnotationsInContexts annosToRemove) {
    for (Map.Entry<String, AClass> classEntry : copy.classes.entrySet()) {
      AClass originalClass = correspondingElement(original.classes, classEntry.getKey());
      AClass copyClass = classEntry.getValue();
      for (Map.Entry<String, AField> fieldEntry : copyClass.fields.entrySet()) {
        AField originalField = correspondingElement(originalClass.fields, fieldEntry.getKey());
        removeAnnosFromATypeElement(originalField.type, fieldEntry.getValue().type, annosToRemove);
      }
      for (Map.Entry<String, AMethod> methodEntry : copyClass.methods.entrySet()) {
        AMethod originalMethod = correspondingElement(originalClass.methods, methodEntry.getKey());
        AMethod copyMethod = methodEntry.getValue();
        removeAnnosFromATypeElement(
            originalMethod.returnType, copyMethod.returnType, annosToRemove);
        removeAnnosFromATypeElement(
            originalMethod.receiver.type, copyMethod.receiver.type, annosToRemove);
        for (Map.Entry<Integer, AField> paramEntry : copyMethod.parameters.entrySet()) {
          AField originalParam =
              correspondingElement(originalMethod.parameters, paramEntry.getKey());
          removeAnnosFromATypeElement(
              originalParam.type, paramEntry.getValue().type, annosToRemove);
        }
        for (Map.Entry<String, AField> preEntry : copyMethod.preconditions.entrySet()) {
          AField originalPre =
              correspondingElement(originalMethod.preconditions, preEntry.getKey());
          removeAnnosFromATypeElement(originalPre.type, preEntry.getValue().type, annosToRemove);
        }
        for (Map.Entry<String, AField> postEntry : copyMethod.postconditions.entrySet()) {
          AField originalPost =
              correspondingElement(originalMethod.postconditions, postEntry.getKey());
          removeAnnosFromATypeElement(originalPost.type, postEntry.getValue().type, annosToRemove);
        }
      }
    }
  }

  /**
   * Removes the specified annotations from {@code copy}, which must be a clone of {@code original}.
   *
   * @param original the type element that {@code annosToRemove} refers to
   * @param copy a clone of {@code original}, from which to remove annotations
   * @param annosToRemove annotations that should not be added to .jaif or stub files
   */
  private static void removeAnnosFromATypeElement(
      ATypeElement original, ATypeElement copy, AnnotationsInContexts annosToRemove) {
    Set<String> annosToRemoveHere = annosToRemove.get(original);
    if (annosToRemoveHere != null) {
      copy.tlAnnotationsHere.removeIf(anno -> annosToRemoveHere.contains(anno.def().toString()));
    }

    // Recursively remove annotations from inner types
    for (Map.Entry<List<TypePathEntry>, ATypeElement> innerEntry : copy.innerTypes.entrySet()) {
      ATypeElement originalInnerType =
          correspondingElement(original.innerTypes, innerEntry.getKey());
      removeAnnosFromATypeElement(originalInnerType, innerEntry.getValue(), annosToRemove);
    }
  }

  /**
   * Returns the value that {@code map} maps {@code key} to. Throws an exception if there is none;
   * {@code map} is a map of a scene of which the map being traversed is a clone, so it has the same
   * keys.
   *
   * @param <K> the type of the keys of {@code map}
   * @param <V> the type of the values of {@code map}
   * @param map a map of the original scene
   * @param key a key of the corresponding map of the clone of the original scene
   * @return the value that {@code map} maps {@code key} to
   */
  private static <K, V> V correspondingElement(Map<K, V> map, K key) {
    V result = map.get(key);
    if (result == null) {
      throw new BugInCF("Not in the scene that was cloned: " + key);
    }
    return result;
  }

  /**
   * Returns {@code jaifPath} with its ".jaif" extension replaced by {@code newExtension}. Only the
   * extension is replaced: an occurrence of ".jaif" elsewhere in the path, such as in a directory
   * name, is left alone.
   *
   * @param jaifPath a path ending in ".jaif"
   * @param newExtension the extension to use in place of ".jaif"
   * @return {@code jaifPath} with its ".jaif" extension replaced by {@code newExtension}
   */
  /*package-private*/ static String replaceJaifExtension(String jaifPath, String newExtension) {
    if (!jaifPath.endsWith(".jaif")) {
      throw new BugInCF("Expected a path ending in \".jaif\", but found: " + jaifPath);
    }
    return jaifPath.substring(0, jaifPath.length() - ".jaif".length()) + newExtension;
  }

  /**
   * Write the scene wrapped by this object to a file at the given path.
   *
   * @param jaifPath the path of the file to be written, but ending in ".jaif". If {@code
   *     outputformat} is not {@code JAIF}, the path will be modified to match.
   * @param annosToIgnore which annotations should be ignored in which contexts
   * @param outputFormat the output format to use
   * @param checker the checker from which this method is called, for naming stub files
   */
  public void writeToFile(
      String jaifPath,
      AnnotationsInContexts annosToIgnore,
      OutputFormat outputFormat,
      BaseTypeChecker checker) {
    assert jaifPath.endsWith(".jaif");
    // Work on a clone, so that removing annotations does not side-effect the wrapped scene:
    // whole-program inference may continue to use the wrapped scene after this method returns.
    AScene scene = theScene.clone();
    removeAnnosFromScene(theScene, scene, annosToIgnore);
    scene.prune();
    String filepath =
        switch (outputFormat) {
          case JAIF -> jaifPath;
          case STUB -> {
            String astubWithChecker = "-" + checker.getClass().getCanonicalName() + ".astub";
            yield replaceJaifExtension(jaifPath, astubWithChecker);
          }
          default -> throw new BugInCF("Unhandled outputFormat " + outputFormat);
        };
    // Delete the file, so that a stale file does not remain if this method writes nothing.  That
    // happens if the scene is empty, and also for stub output, which writes no file if no class in
    // the scene is printable.  In the other cases, writing truncates the file, so there is no need
    // to delete it first -- and deleting it would be worse, because deletion requires write
    // permission on the containing directory, whereas truncation does not.
    if (scene.isEmpty() || outputFormat == OutputFormat.STUB) {
      try {
        Files.deleteIfExists(Paths.get(filepath));
      } catch (IOException e) {
        // Use e, not e.getMessage(), because the message of a FileSystemException is just the
        // file name, without any indication of what went wrong.
        throw new UserError("Problem while deleting %s: %s", filepath, e);
      }
    }
    // Only write non-empty scenes into files.
    if (!scene.isEmpty()) {
      try {
        switch (outputFormat) {
          case STUB ->
              // Write out a wrapper for the cleaned-up clone, not `this`, so that the
              // annotations that were removed above do not appear in the stub file.
              // Pass in the checker to compute contracts on the fly; precomputing yields
              // incorrect annotations, most likely due to nested classes.
              SceneToStubWriter.write(new ASceneWrapper(scene), filepath, checker);
          case JAIF -> {
            // For .jaif files, precompute contracts because the Annotation File
            // Utilities knows nothing about (and cannot depend on) the Checker
            // Framework.
            for (Map.Entry<String, AClass> classEntry : scene.classes.entrySet()) {
              AClass aClass = classEntry.getValue();
              for (Map.Entry<String, AMethod> methodEntry : aClass.getMethods().entrySet()) {
                AMethod aMethod = methodEntry.getValue();
                List<AnnotationMirror> contractAnnotationMirrors =
                    checker.getTypeFactory().getContractAnnotations(aMethod);
                List<Annotation> contractAnnotations =
                    CollectionsP.mapList(
                        AnnotationConverter::annotationMirrorToAnnotation,
                        contractAnnotationMirrors);
                aMethod.contracts = contractAnnotations;
              }
            }
            try (Writer fw = Files.newBufferedWriter(Paths.get(filepath), StandardCharsets.UTF_8)) {
              IndexFileWriter.write(scene, fw);
            }
          }
          default -> throw new BugInCF("Unhandled outputFormat " + outputFormat);
        }
      } catch (IOException e) {
        throw new UserError(e, "Problem while writing %s", filepath);
      } catch (DefException e) {
        throw new BugInCF(e);
      }
    }
  }

  /**
   * Updates the symbol information stored in AClass for the given class. May be called multiple
   * times (and needs to be if the second parameter was null the first time it was called; only some
   * calls provide the symbol information).
   *
   * @param aClass the class representation in which the symbol information is to be updated
   * @param classSymbol the source of the symbol information; may be null, in which case this method
   *     does nothing
   */
  public void updateSymbolInformation(AClass aClass, @Nullable ClassSymbol classSymbol) {
    if (classSymbol == null) {
      return;
    }
    if (classSymbol.isEnum()) {
      List<VariableElement> enumConstants = ElementUtils.getEnumConstants(classSymbol);
      if (!aClass.isEnum(classSymbol.getSimpleName().toString())) {
        aClass.setEnumConstants(enumConstants);
      } else {
        // Verify that the existing value is consistent.
        List<VariableElement> existingEnumConstants = aClass.getEnumConstants();
        if (!existingEnumConstants.equals(enumConstants)) {
          throw new BugInCF(
              "inconsistent enum constants in WPI for class %s: existing %s, new %s",
              classSymbol.getQualifiedName(), existingEnumConstants, enumConstants);
        }
      }
    }

    // Mark this class and each of its enclosing classes, because a stub file must print the
    // declaration of every enclosing class, using the right keyword for each one.
    ClassSymbol outerClass = classSymbol;
    while (outerClass != null) {
      if (outerClass.getKind() == ElementKind.ANNOTATION_TYPE) {
        aClass.markAsAnnotation(outerClass.getSimpleName().toString());
      } else if (outerClass.isEnum()) {
        aClass.markAsEnum(outerClass.getSimpleName().toString());
      } else if (outerClass.isInterface()) {
        aClass.markAsInterface(outerClass.getSimpleName().toString());
        // } else if (outerClass.isRecord()) {
        //   aClass.markAsRecord(outerClass.getSimpleName().toString());
      }
      Element element = outerClass.getEnclosingElement();
      if (element == null || element.getKind() == ElementKind.PACKAGE) {
        break;
      }
      outerClass = (ClassSymbol) ElementUtils.enclosingTypeElement(element);
    }

    aClass.setTypeElement(classSymbol);
  }

  /**
   * Avoid using this if possible; use the other methods of this class unless you absolutely need an
   * AScene.
   *
   * @return the AScene representation of this
   */
  public AScene getAScene() {
    return theScene;
  }
}
