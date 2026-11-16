package org.checkerframework.common.wholeprograminference;

import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.el.AClass;
import org.checkerframework.afu.scenelib.el.AField;
import org.checkerframework.afu.scenelib.el.AMethod;
import org.checkerframework.afu.scenelib.el.AScene;
import org.checkerframework.afu.scenelib.el.ATypeElement;
import org.checkerframework.afu.scenelib.el.TypePathEntry;
import org.checkerframework.afu.scenelib.io.IndexFileParser;
import org.checkerframework.afu.scenelib.util.JVMNames;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.wholeprograminference.WholeProgramInference.OutputFormat;
import org.checkerframework.common.wholeprograminference.scenelib.ASceneWrapper;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.UserError;
import org.plumelib.util.MapsP;

/**
 * This class stores annotations using scenelib objects.
 *
 * <p>The set of annotations inferred for a certain class is stored in an {@link AScene}, which
 * {@code writeScenes()} can write into a file. For example, a class {@code my.pakkage.MyClass} will
 * have its members' inferred types stored in a Scene, and later written into a file named {@code
 * my.pakkage.MyClass.jaif} if using {@link OutputFormat#JAIF}, or {@code my.pakkage.MyClass.astub}
 * if using {@link OutputFormat#STUB}.
 *
 * <p>This class populates the initial Scenes by reading existing .jaif files on the {@link
 * #inferOutputDirectory} directory (regardless of output format). Having more information in those
 * initial .jaif files means that the precision achieved by the whole-program inference analysis
 * will be better. {@link #writeScenes} rewrites the initial .jaif files and may create new ones.
 */
public class WholeProgramInferenceScenesStorage
    implements WholeProgramInferenceStorage<ATypeElement> {

  /** The type factory associated with this WholeProgramInferenceScenesStorage. */
  protected final AnnotatedTypeFactory atypeFactory;

  /** Determines which inferred qualifiers are redundant. */
  private final WholeProgramInferenceDefaults wpiDefaults;

  /** Annotations that should not be output to a .jaif or stub file. */
  private final AnnotationsInContexts annosToIgnore = new AnnotationsInContexts();

  /**
   * Maps each type element of a scene that represents the type of a declaration to the type that
   * the declaration's type has when no annotation is written on it, or to null if that type is not
   * known. A type element that is not a key, such as one for a pre- or postcondition, has no such
   * type.
   *
   * <p>Like {@link AnnotationsInContexts}, this map compares type elements by identity.
   */
  private final IdentityHashMap<ATypeElement, @Nullable AnnotatedTypeMirror> defaultedTypes =
      new IdentityHashMap<>();

  /**
   * Type elements none of whose annotations may be treated as redundant, because whole-program
   * inference also inferred, for the same program element, a declaration annotation that is an
   * alias for a type qualifier. Reading the output file gives such an element the aliased type
   * qualifier, so an annotation that is redundant in the input program -- because it is the default
   * or is already effective in the source code -- is not redundant in the output file.
   *
   * <p>Like {@link AnnotationsInContexts}, this set compares type elements by identity.
   */
  private final Set<ATypeElement> neverIgnore = Collections.newSetFromMap(new IdentityHashMap<>());

  /**
   * The binary names of the type qualifiers supported by {@link #atypeFactory}. It is lazily
   * initialized by {@link #getSupportedAnnoNames}, rather than in the constructor, because this
   * object is created while the type factory is still being constructed.
   */
  private @MonotonicNonNull Set<@BinaryName String> supportedAnnoNames = null;

  /** Maps .jaif file paths (Strings) to Scenes. Relative to inferOutputDirectory. */
  private final Map<String, ASceneWrapper> scenes = new HashMap<>();

  /**
   * Scenes that were modified since the last time all Scenes were written into .jaif files. Each
   * String element of this set is a path (relative to inferOutputDirectory) to the .jaif file of
   * the corresponding Scene in the set. It is obtained by passing a class name as argument to the
   * {@link #getJaifPath} method.
   *
   * <p>Modifying a Scene means adding (or changing) a type annotation for a field, method return
   * type, or method parameter type in the Scene. (Scenes are modified by the method {@link
   * #updateStorageLocationFromAtm}.)
   *
   * <p>Every element of this set is a key in {@link #scenes}; {@link #setFileModified} maintains
   * that invariant.
   */
  private final Set<String> modifiedScenes = new HashSet<>();

  /**
   * This map relates inferred preconditions to the declared types of the expressions to which the
   * precondition applies. It is necessary to keep this map here because the AFU does not have a
   * dependency on the CF itself, where AnnotatedTypeMirror exists.
   *
   * <p>The keys are the concatenation of the string representation of the method signature as
   * stored by {@link AMethod} to which the precondition applies and the expression to which the
   * precondition applies.
   */
  private final Map<String, AnnotatedTypeMirror> preconditionsToDeclaredTypes = new HashMap<>();

  /**
   * This map relates inferred postconditions to the declared types of the expressions to which the
   * postcondition applies. It is necessary to keep this map here because the AFU does not have a
   * dependency on the CF itself, where AnnotatedTypeMirror exists.
   *
   * <p>The keys are the concatenation of the string representation of the method signature as
   * stored by {@link AMethod} to which the postcondition applies and the expression to which the
   * postcondition applies.
   */
  private final Map<String, AnnotatedTypeMirror> postconditionsToDeclaredTypes = new HashMap<>();

  /** The directory for reading and writing .jaif files. */
  private final Path inferOutputDirectory;

  /**
   * Default constructor.
   *
   * @param atypeFactory the type factory associated with this WholeProgramInferenceScenesStorage
   * @param inferOutputDirectory the directory into which to write whole program inference files
   */
  public WholeProgramInferenceScenesStorage(
      AnnotatedTypeFactory atypeFactory, String inferOutputDirectory) {
    this.atypeFactory = atypeFactory;
    this.wpiDefaults = new WholeProgramInferenceDefaults(atypeFactory);
    try {
      this.inferOutputDirectory = Path.of(inferOutputDirectory);
    } catch (InvalidPathException e) {
      throw new UserError("Invalid -AinferOutputDirectory path: " + inferOutputDirectory, e);
    }
  }

  @Override
  public String getFileForElement(Element elt) {
    String className = getEnclosingClassName(elt);
    String file = getJaifPath(className);
    return file;
  }

  /**
   * Returns the binary name of the class that contains {@code elt}, or of {@code elt} itself if
   * {@code elt} is a class.
   *
   * @param elt an element
   * @return the binary name of the class that contains {@code elt}
   */
  /*package-private*/ static @BinaryName String getEnclosingClassName(Element elt) {
    return switch (elt.getKind()) {
      case CONSTRUCTOR, METHOD -> ElementUtils.getEnclosingClassName((ExecutableElement) elt);
      case FIELD, ENUM_CONSTANT -> {
        ClassSymbol enclosingClass = ((VarSymbol) elt).enclClass();
        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
        @BinaryName String className = enclosingClass.flatname.toString();
        yield className;
      }
      case CLASS -> ElementUtils.getBinaryName((TypeElement) elt);
      case LOCAL_VARIABLE, PARAMETER -> ElementUtils.getEnclosingClassName((VariableElement) elt);
      default -> throw new BugInCF("What element? %s %s", elt.getKind(), elt);
    };
  }

  /**
   * Returns the annotations for a class.
   *
   * @param className the name of the class, in binary form
   * @param file the path to the file that represents the class
   * @param classSymbol optionally, the ClassSymbol representing the class
   * @return the annotations for the class
   */
  private AClass getClassAnnos(
      @BinaryName String className, String file, @Nullable ClassSymbol classSymbol) {
    // Possibly reads .jaif file to obtain a Scene.
    ASceneWrapper scene = getScene(file);
    AClass aClass = scene.getAScene().classes.getVivify(className);
    scene.updateSymbolInformation(aClass, classSymbol);
    return aClass;
  }

  /**
   * Returns the annotations for a method or constructor.
   *
   * @param methodElt the method or constructor
   * @return the annotations for a method or constructor
   */
  private AMethod getMethodAnnos(ExecutableElement methodElt) {
    String className = ElementUtils.getEnclosingClassName(methodElt);
    String file = getFileForElement(methodElt);
    AClass classAnnos = getClassAnnos(className, file, ((MethodSymbol) methodElt).enclClass());
    AMethod methodAnnos = classAnnos.methods.getVivify(JVMNames.getJVMMethodSignature(methodElt));
    methodAnnos.setFieldsFromMethodElement(methodElt);
    return methodAnnos;
  }

  /**
   * Returns the annotations for a field.
   *
   * @param fieldElt the field
   * @return the annotations for a field
   */
  private AField getFieldAnnos(VariableElement fieldElt) {
    String className = ElementUtils.getEnclosingClassName(fieldElt);
    String file = getFileForElement(fieldElt);
    AClass classAnnos = getClassAnnos(className, file, ((VarSymbol) fieldElt).enclClass());
    AField fieldAnnos = classAnnos.fields.getVivify(fieldElt.getSimpleName().toString());
    return fieldAnnos;
  }

  @Override
  public boolean hasStorageLocationForMethod(ExecutableElement methodElt) {
    // The only location that the scenes implementation cannot store an annotation is on
    // a member of an annotation type, which it cannot distinguish from a normal interface's
    // method. Without this, the scenes implementation will attempt to annotate annotation
    // elements, which is an error.
    Element enclosingType = ElementUtils.enclosingTypeElement(methodElt);
    return enclosingType == null || enclosingType.getKind() != ElementKind.ANNOTATION_TYPE;
  }

  @Override
  public ATypeElement getParameterAnnotations(
      ExecutableElement methodElt,
      @Positive int index_1based,
      AnnotatedTypeMirror paramATM,
      VariableElement ve,
      AnnotatedTypeFactory atypeFactory) {
    if (index_1based == 0) {
      throw new TypeSystemError("0 is illegal as index argument to getParameterAnnotations");
    }
    AMethod methodAnnos = getMethodAnnos(methodElt);
    AField param =
        methodAnnos.vivifyAndAddTypeMirrorToParameter(
            index_1based - 1, paramATM.getUnderlyingType(), ve.getSimpleName());
    defaultedTypes.put(param.type, wpiDefaults.defaultedType(ve));
    return param.type;
  }

  @Override
  public ATypeElement getReceiverAnnotations(
      ExecutableElement methodElt,
      AnnotatedTypeMirror paramATM,
      AnnotatedTypeFactory atypeFactory) {
    AMethod methodAnnos = getMethodAnnos(methodElt);
    defaultedTypes.put(methodAnnos.receiver.type, wpiDefaults.defaultedReceiverType(methodElt));
    return methodAnnos.receiver.type;
  }

  @Override
  public AnnotationMirrorSet getMethodDeclarationAnnotations(ExecutableElement elt) {
    AMethod methodAnnos = getMethodAnnos(elt);
    Set<Annotation> annos = methodAnnos.tlAnnotationsHere;
    AnnotationMirrorSet result = new AnnotationMirrorSet();
    for (Annotation anno : annos) {
      result.add(
          AnnotationConverter.annotationToAnnotationMirror(anno, atypeFactory.getProcessingEnv()));
    }
    return result;
  }

  @Override
  public boolean removeMethodDeclarationAnnotation(
      ExecutableElement methodElt, AnnotationMirror anno) {
    AMethod methodAnnos = getMethodAnnos(methodElt);
    boolean wasRemoved =
        methodAnnos.tlAnnotationsHere.remove(
            AnnotationConverter.annotationMirrorToAnnotation(anno));
    if (wasRemoved) {
      setFileModified(getFileForElement(methodElt));
    }
    return wasRemoved;
  }

  @Override
  public ATypeElement getReturnAnnotations(
      ExecutableElement methodElt, AnnotatedTypeMirror atm, AnnotatedTypeFactory atypeFactory) {
    AMethod methodAnnos = getMethodAnnos(methodElt);
    defaultedTypes.put(methodAnnos.returnType, wpiDefaults.defaultedReturnType(methodElt));
    return methodAnnos.returnType;
  }

  @Override
  public ATypeElement getFieldAnnotations(
      Element element,
      String fieldName,
      AnnotatedTypeMirror lhsATM,
      AnnotatedTypeFactory atypeFactory) {
    ClassSymbol enclosingClass = ((VarSymbol) element).enclClass();
    String file = getFileForElement(element);
    @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
    @BinaryName String className = enclosingClass.flatname.toString();
    AClass classAnnos = getClassAnnos(className, file, enclosingClass);
    AField field = classAnnos.fields.getVivify(fieldName);
    field.setTypeMirror(lhsATM.getUnderlyingType());
    defaultedTypes.put(field.type, wpiDefaults.defaultedType(element));
    return field.type;
  }

  @Override
  public ATypeElement getPreOrPostconditions(
      String className,
      Analysis.BeforeOrAfter preOrPost,
      ExecutableElement methodElement,
      String expression,
      AnnotatedTypeMirror declaredType,
      AnnotatedTypeFactory atypeFactory) {
    AMethod methodAnnos = getMethodAnnos(methodElement);
    String key = methodAnnos.methodSignature + expression;
    return switch (preOrPost) {
      case BEFORE -> {
        preconditionsToDeclaredTypes.put(key, declaredType);
        yield methodAnnos.vivifyAndAddTypeMirrorToPrecondition(
                expression, declaredType.getUnderlyingType())
            .type;
      }
      case AFTER -> {
        postconditionsToDeclaredTypes.put(key, declaredType);
        yield methodAnnos.vivifyAndAddTypeMirrorToPostcondition(
                expression, declaredType.getUnderlyingType())
            .type;
      }
      default -> throw new BugInCF("Unexpected " + preOrPost);
    };
  }

  /**
   * Fetches the declared type of an expression for which a precondition was inferred, for the given
   * AMethod.
   *
   * @param m a method
   * @param expression the expression
   * @return the declared type
   */
  public AnnotatedTypeMirror getPreconditionDeclaredType(AMethod m, String expression) {
    String key = m.methodSignature + expression;
    if (!preconditionsToDeclaredTypes.containsKey(key)) {
      throw new BugInCF(
          "attempted to retrieve the declared type of a precondition expression for which"
              + " nothing was inferred: "
              + key);
    }
    return preconditionsToDeclaredTypes.get(key);
  }

  /**
   * Fetches the declared type of an expression for which a postcondition was inferred, for the
   * given AMethod.
   *
   * @param m a method
   * @param expression the expression
   * @return the declared type
   */
  public AnnotatedTypeMirror getPostconditionDeclaredType(AMethod m, String expression) {
    String key = m.methodSignature + expression;
    if (!postconditionsToDeclaredTypes.containsKey(key)) {
      throw new BugInCF(
          "attempted to retrieve the declared type of a postcondition expression for which"
              + " nothing was inferred: "
              + key);
    }
    return postconditionsToDeclaredTypes.get(key);
  }

  @Override
  public boolean addMethodDeclarationAnnotation(
      ExecutableElement methodElt, AnnotationMirror anno) {

    // Do not infer types for library code, only for type-checked source code.
    if (!ElementUtils.isElementFromSourceCode(methodElt)) {
      return false;
    }

    AMethod methodAnnos = getMethodAnnos(methodElt);

    Annotation sceneAnno = AnnotationConverter.annotationMirrorToAnnotation(anno);
    boolean isNewAnnotation = methodAnnos.tlAnnotationsHere.add(sceneAnno);
    if (wpiDefaults.isAliasForTypeQualifier(anno)) {
      // A declaration annotation on a method that is an alias for a type qualifier applies to
      // the method's return type.
      neverIgnoreAnnotationsOn(methodAnnos.returnType);
    }
    return isNewAnnotation;
  }

  @Override
  public boolean addFieldDeclarationAnnotation(VariableElement field, AnnotationMirror anno) {
    if (!ElementUtils.isElementFromSourceCode(field)) {
      return false;
    }

    AField fieldAnnos = getFieldAnnos(field);

    Annotation sceneAnno = AnnotationConverter.annotationMirrorToAnnotation(anno);

    boolean isNewAnnotation = fieldAnnos.tlAnnotationsHere.add(sceneAnno);
    if (wpiDefaults.isAliasForTypeQualifier(anno)) {
      neverIgnoreAnnotationsOn(fieldAnnos.type);
    }
    return isNewAnnotation;
  }

  @Override
  public boolean addDeclarationAnnotationToFormalParameter(
      ExecutableElement methodElt, @Positive int index_1based, AnnotationMirror anno) {
    if (index_1based == 0) {
      throw new TypeSystemError(
          "0 is illegal as index argument to addDeclarationAnnotationToFormalParameter");
    }
    if (!ElementUtils.isElementFromSourceCode(methodElt)) {
      return false;
    }

    VariableElement paramElt = methodElt.getParameters().get(index_1based - 1);
    AnnotatedTypeMirror paramAType = atypeFactory.getAnnotatedType(paramElt);
    ATypeElement paramAnnos =
        getParameterAnnotations(methodElt, index_1based, paramAType, paramElt, atypeFactory);
    Annotation sceneAnno = AnnotationConverter.annotationMirrorToAnnotation(anno);

    boolean isNewAnnotation = paramAnnos.tlAnnotationsHere.add(sceneAnno);
    if (wpiDefaults.isAliasForTypeQualifier(anno)) {
      neverIgnoreAnnotationsOn(paramAnnos);
    }
    return isNewAnnotation;
  }

  @Override
  public boolean addClassDeclarationAnnotation(TypeElement classElt, AnnotationMirror anno) {
    if (!ElementUtils.isElementFromSourceCode(classElt)) {
      return false;
    }

    AClass classAnnos =
        getClassAnnos(
            ElementUtils.getBinaryName(classElt),
            getFileForElement(classElt),
            (ClassSymbol) classElt);

    Annotation sceneAnno = AnnotationConverter.annotationMirrorToAnnotation(anno);

    boolean isNewAnnotation = classAnnos.tlAnnotationsHere.add(sceneAnno);
    return isNewAnnotation;
  }

  /**
   * Records that no annotation on {@code typeElt} may be omitted from the output file as redundant.
   *
   * @param typeElt a type element of a scene; see {@link #neverIgnore}
   */
  private void neverIgnoreAnnotationsOn(ATypeElement typeElt) {
    neverIgnore.add(typeElt);
    // The type annotations may have been inferred, and recorded as ignorable, before the
    // declaration annotation was inferred.
    annosToIgnore.remove(typeElt);
  }

  /**
   * Write all modified scenes into files. (Scenes are modified by the method {@link
   * #updateStorageLocationFromAtm}.)
   *
   * @param outputFormat the output format to use when writing files
   * @param checker the checker from which this method is called, for naming stub files
   */
  public void writeScenes(OutputFormat outputFormat, BaseTypeChecker checker) {
    // Create WPI directory if it doesn't exist already.
    try {
      Files.createDirectories(inferOutputDirectory);
    } catch (IOException e) {
      throw new UserError("Cannot create " + inferOutputDirectory.toAbsolutePath(), e);
    }
    // Write scenes into files.
    for (String jaifPath : modifiedScenes) {
      scenes.get(jaifPath).writeToFile(jaifPath, annosToIgnore, outputFormat, checker);
    }
    modifiedScenes.clear();
  }

  /**
   * Returns the String representing the .jaif path of a class given its name.
   *
   * @param className the simple name of a class
   * @return the path to the .jaif file
   */
  protected String getJaifPath(String className) {
    String jaifPath = inferOutputDirectory.resolve(className + ".jaif").toString();
    return jaifPath;
  }

  /**
   * Reads a Scene from the given .jaif file, or returns an empty Scene if the file does not exist.
   *
   * @param jaifPath the .jaif file
   * @return the Scene read from the file, or an empty Scene if the file does not exist
   */
  private ASceneWrapper getScene(String jaifPath) {
    AScene scene;
    if (!scenes.containsKey(jaifPath)) {
      File jaifFile = new File(jaifPath);
      scene = new AScene();
      if (jaifFile.exists()) {
        try {
          IndexFileParser.parseFile(jaifPath, scene);
        } catch (IOException e) {
          throw new UserError(e, "Problem while reading %s", jaifPath);
        }
      }
      ASceneWrapper wrapper = new ASceneWrapper(scene);
      scenes.put(jaifPath, wrapper);
      return wrapper;
    } else {
      return scenes.get(jaifPath);
    }
  }

  /**
   * Returns true if {@code am} should not be inserted in source code, for example {@link
   * org.checkerframework.common.value.qual.BottomVal}. This happens when {@code am} cannot be
   * inserted in source code or is redundant, because the location has that qualifier even when no
   * annotation is written there.
   *
   * <p>Invisible qualifiers, which are annotations that contain the {@link
   * org.checkerframework.framework.qual.InvisibleQualifier} meta-annotation, also return true.
   *
   * @param am an annotation to test for whether it should be inserted into source code
   * @param atm the inferred type that {@code am} is a primary annotation of
   * @param defaulted the type that the location has when no annotation is written there, or null if
   *     that type is not known
   * @return true if am should not be inserted into source code, or if am is invisible
   */
  private boolean shouldIgnore(
      AnnotationMirror am, AnnotatedTypeMirror atm, @Nullable AnnotatedTypeMirror defaulted) {
    Element elt = am.getAnnotationType().asElement();
    // Checks if am is an implementation detail (a type qualifier used
    // internally by the type system and not meant to be seen by the user).
    Target target = elt.getAnnotation(Target.class);
    if (target != null && target.value().length == 0) {
      return true;
    }
    if (elt.getAnnotation(InvisibleQualifier.class) != null) {
      return true;
    }
    return wpiDefaults.isRedundant(am, atm, defaulted);
  }

  /**
   * Returns the binary names of the type qualifiers supported by the type factory associated with
   * this.
   *
   * @return the binary names of the type qualifiers supported by this object's AnnotatedTypeFactory
   */
  private Set<@BinaryName String> getSupportedAnnoNames() {
    if (supportedAnnoNames == null) {
      Set<Class<? extends java.lang.annotation.Annotation>> supportedAnnos =
          atypeFactory.getSupportedTypeQualifiers();
      Set<@BinaryName String> result = new HashSet<>(MapsP.mapCapacity(supportedAnnos.size()));
      for (Class<? extends java.lang.annotation.Annotation> clazz : supportedAnnos) {
        @SuppressWarnings("signature:assignment") // an annotation is not an array or primitive
        @BinaryName String annoName = clazz.getName();
        result.add(annoName);
      }
      supportedAnnoNames = result;
    }
    return supportedAnnoNames;
  }

  /**
   * Returns a subset of annosSet, consisting of the annotations supported by the type factory
   * associated with this. These are not necessarily legal annotations: they have the right name,
   * but they may lack elements (fields).
   *
   * <p>The result is unmodifiable and is not aliased to {@code annosSet}.
   *
   * @param annosSet a set of annotations
   * @return the annotations supported by this object's AnnotatedTypeFactory
   */
  private Set<Annotation> getSupportedAnnosInSet(Set<Annotation> annosSet) {
    // TODO: Remove comparison by name.
    Set<@BinaryName String> supportedNames = getSupportedAnnoNames();
    // Is lazily initialized, to avoid allocation when annosSet contains no supported
    // annotation, which includes the common case that annosSet is empty.
    Set<Annotation> output = null;
    for (Annotation anno : annosSet) {
      if (supportedNames.contains(anno.def.name)) {
        if (output == null) {
          output = new HashSet<>(MapsP.mapCapacity(annosSet.size()));
        }
        output.add(anno);
      }
    }
    if (output == null) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(output);
  }

  @Override
  public AnnotatedTypeMirror atmFromStorageLocation(
      TypeMirror typeMirror, ATypeElement storageLocation) {
    AnnotatedTypeMirror result = AnnotatedTypeMirror.createType(typeMirror, atypeFactory, false);
    updateAtmFromATypeElement(result, storageLocation);
    return result;
  }

  /**
   * Updates an {@link org.checkerframework.framework.type.AnnotatedTypeMirror} to contain the
   * {@link Annotation}s of an {@link ATypeElement}.
   *
   * @param result the AnnotatedTypeMirror to be modified
   * @param storageLocation the {@link ATypeElement} used
   */
  private void updateAtmFromATypeElement(AnnotatedTypeMirror result, ATypeElement storageLocation) {
    Set<Annotation> annos = getSupportedAnnosInSet(storageLocation.tlAnnotationsHere);
    for (Annotation anno : annos) {
      AnnotationMirror am =
          AnnotationConverter.annotationToAnnotationMirror(anno, atypeFactory.getProcessingEnv());
      result.addAnnotation(am);
    }
    if (result.getKind() == TypeKind.ARRAY) {
      AnnotatedArrayType aat = (AnnotatedArrayType) result;
      for (ATypeElement innerType : storageLocation.innerTypes.values()) {
        updateAtmFromATypeElement(aat.getComponentType(), innerType);
      }
    }
    if (result.getKind() == TypeKind.TYPEVAR) {
      AnnotatedTypeVariable atv = (AnnotatedTypeVariable) result;
      for (ATypeElement innerType : storageLocation.innerTypes.values()) {
        updateAtmFromATypeElement(atv.getUpperBound(), innerType);
      }
    }
  }

  @Override
  public void updateStorageLocationFromAtm(
      AnnotatedTypeMirror newATM,
      AnnotatedTypeMirror curATM,
      ATypeElement typeToUpdate,
      TypeUseLocation defLoc,
      boolean ignoreIfAnnotated) {
    updateTypeElementFromATM(
        typeToUpdate, newATM, curATM, ignoreIfAnnotated, defaultedTypes.get(typeToUpdate));
  }

  //
  // Writing to a file
  //

  // The prepare*ForWriting hooks are needed in addition to the postProcessClassTree hook because
  // a scene may be modified and written at any time, including before or after
  // postProcessClassTree is called.

  /**
   * Side-effects the compilation unit annotations to make any desired changes before writing to a
   * file.
   *
   * @param compilationUnitAnnos the compilation unit annotations to modify
   */
  public void prepareSceneForWriting(AScene compilationUnitAnnos) {
    for (Map.Entry<String, AClass> classEntry : compilationUnitAnnos.classes.entrySet()) {
      wpiPrepareClassForWriting(classEntry.getValue());
    }
  }

  /**
   * Side-effects the class annotations to make any desired changes before writing to a file.
   *
   * @param classAnnos the class annotations to modify
   */
  public void wpiPrepareClassForWriting(AClass classAnnos) {
    for (Map.Entry<String, AMethod> methodEntry : classAnnos.methods.entrySet()) {
      wpiPrepareMethodForWriting(classAnnos.className, methodEntry.getValue());
    }
  }

  /**
   * Side-effects the method or constructor annotations to make any desired changes before writing
   * to a file.
   *
   * @param className the class that contains the method, for diagnostics only
   * @param methodAnnos the method or constructor annotations to modify
   */
  public void wpiPrepareMethodForWriting(String className, AMethod methodAnnos) {
    atypeFactory.wpiPrepareMethodForWriting(className, methodAnnos);
  }

  @Override
  public void writeResultsToFile(
      WholeProgramInference.OutputFormat outputFormat, BaseTypeChecker checker) {
    if (outputFormat == OutputFormat.AJAVA) {
      throw new BugInCF("WholeProgramInferenceScenes used with format " + outputFormat);
    }

    for (String file : modifiedScenes) {
      ASceneWrapper scene = scenes.get(file);
      prepareSceneForWriting(scene.getAScene());
    }

    writeScenes(outputFormat, checker);
  }

  @Override
  public void setFileModified(String path) {
    if (!scenes.containsKey(path)) {
      // No Scene was created for this file, so there is nothing to write out for it.
      return;
    }
    modifiedScenes.add(path);
  }

  @Override
  public void preprocessClassTree(ClassTree classTree) {
    // This implementation does nothing.
  }

  /**
   * Updates an {@link ATypeElement} to have the annotations of an {@link
   * org.checkerframework.framework.type.AnnotatedTypeMirror} passed as argument. Annotations in the
   * original set that should be ignored (see {@link #shouldIgnore}) are not added to the resulting
   * set. This method also checks if the AnnotatedTypeMirror has explicit annotations in source
   * code, and if that is the case no annotations are added for that location.
   *
   * <p>This method removes from the ATypeElement all annotations supported by this object's
   * AnnotatedTypeFactory before inserting new ones. It is assumed that every time this method is
   * called, the AnnotatedTypeMirror has a better type estimate for the ATypeElement. Therefore, it
   * is not a problem to remove all annotations before inserting the new annotations.
   *
   * @param typeToUpdate the ATypeElement that will be updated
   * @param newATM the AnnotatedTypeMirror whose annotations will be added to the ATypeElement
   * @param curATM used to check if the element which will be updated has explicit annotations in
   *     source code
   * @param ignoreIfAnnotated if true, don't update any type that is explicitly annotated in the
   *     source code
   * @param defaulted the type that the location of {@code typeToUpdate} has when no annotation is
   *     written there, or null if that type is not known
   */
  private void updateTypeElementFromATM(
      ATypeElement typeToUpdate,
      AnnotatedTypeMirror newATM,
      AnnotatedTypeMirror curATM,
      boolean ignoreIfAnnotated,
      @Nullable AnnotatedTypeMirror defaulted) {
    // Clears only the annotations that are supported by the relevant AnnotatedTypeFactory.
    // The others stay intact.
    Set<Annotation> annosToRemove = getSupportedAnnosInSet(typeToUpdate.tlAnnotationsHere);
    // This method may be called consecutive times for the same ATypeElement.  Each time it is
    // called, the AnnotatedTypeMirror has a better type estimate for the ATypeElement.
    // Therefore, it is not a problem to remove all annotations before inserting the new
    // annotations.
    typeToUpdate.tlAnnotationsHere.removeAll(annosToRemove);

    // Only update the ATypeElement if there are no explicit annotations.
    if (curATM.getExplicitAnnotations().isEmpty() || !ignoreIfAnnotated) {
      for (AnnotationMirror am : newATM.getPrimaryAnnotations()) {
        addAnnotationsToATypeElement(newATM, typeToUpdate, am, curATM.hasAnnotation(am), defaulted);
      }
    } else if (curATM.getKind() == TypeKind.TYPEVAR) {
      // getExplicitAnnotations will be non-empty for type vars whose bounds are explicitly
      // annotated.  So instead, only insert the annotation if there is not primary annotation
      // of the same hierarchy.
      for (AnnotationMirror am : newATM.getPrimaryAnnotations()) {
        if (curATM.getPrimaryAnnotationInHierarchy(am) != null) {
          // Don't insert if the type already has a primary annotation in the same hierarchy.
          // Other hierarchies are still handled, so this is `continue` rather than `break`.
          continue;
        }
        addAnnotationsToATypeElement(newATM, typeToUpdate, am, curATM.hasAnnotation(am), defaulted);
      }
    }

    // Recursively update the component type of an array.  Both newATM and curATM must be
    // arrays, because one might be a declared type even if the other is an array: it is
    // permitted to assign, e.g., a String[] to a location whose static type is Object, and
    // vice versa (if a cast is used).
    //
    // Type variables are not treated analogously: this method never recurs into the bounds
    // of a type variable, so it never writes a type-variable bound into innerTypes.
    // TODO: The read path is asymmetric with this one: updateAtmFromATypeElement reads
    // innerTypes into the upper bound of an AnnotatedTypeVariable, and updateAtmWithLub
    // takes the LUB of both bounds.  Those two behaviors apply only to type-variable bounds
    // that came from a pre-existing .jaif file (which getScene parses), never to bounds that
    // WPI itself wrote.  Decide whether this method should write type-variable bounds, or
    // whether those two methods should stop reading them.  Note that the ajava
    // implementation deliberately does not recur into type-variable bounds, to avoid
    // accidentally substituting the use of a type variable for its declaration.
    if (newATM.getKind() == TypeKind.ARRAY && curATM.getKind() == TypeKind.ARRAY) {
      AnnotatedArrayType newAAT = (AnnotatedArrayType) newATM;
      AnnotatedArrayType oldAAT = (AnnotatedArrayType) curATM;
      updateTypeElementFromATM(
          typeToUpdate.innerTypes.getVivify(
              TypePathEntry.getTypePathEntryListFromBinary(Collections.nCopies(2, 0))),
          newAAT.getComponentType(),
          oldAAT.getComponentType(),
          ignoreIfAnnotated,
          defaulted instanceof AnnotatedArrayType defaultedArray
              ? defaultedArray.getComponentType()
              : null);
    }
  }

  /**
   * Adds annotation {@code am} to {@code typeToUpdate}. If {@code am} should not be written into
   * the source code -- either because the source code already has that annotation, or because
   * {@link #shouldIgnore} returns true for it -- then this method also records {@code am} in {@link
   * #annosToIgnore}, which prevents it from being written to the .jaif or stub file. It makes no
   * such record for a type element in {@link #neverIgnore}.
   *
   * @param newATM the AnnotatedTypeMirror that {@code am} is a primary annotation of; used only to
   *     determine whether {@code am} should be ignored
   * @param typeToUpdate the ATypeElement that will be updated
   * @param am the annotation to add to {@code typeToUpdate}
   * @param isEffectiveAnnotation true if {@code am} is already an effective annotation of the type
   *     in the source code, in which case writing it into the source code would be redundant
   * @param defaulted the type that the location of {@code typeToUpdate} has when no annotation is
   *     written there, or null if that type is not known
   */
  private void addAnnotationsToATypeElement(
      AnnotatedTypeMirror newATM,
      ATypeElement typeToUpdate,
      AnnotationMirror am,
      boolean isEffectiveAnnotation,
      @Nullable AnnotatedTypeMirror defaulted) {
    Annotation anno = AnnotationConverter.annotationMirrorToAnnotation(am);
    typeToUpdate.tlAnnotationsHere.add(anno);
    if ((isEffectiveAnnotation || shouldIgnore(am, newATM, defaulted))
        && !neverIgnore.contains(typeToUpdate)) {
      Set<String> annosIgnored =
          annosToIgnore.computeIfAbsent(typeToUpdate, k -> new HashSet<>(MapsP.mapCapacity(1)));
      annosIgnored.add(anno.def().toString());
    }
  }

  /**
   * Maps an ATypeElement of a scene to the names of the annotation definitions of the annotations
   * that should not be written out for it.
   *
   * <p>The keys are compared by identity, not by {@code equals()}. Two distinct ATypeElements can
   * be {@code equals()} to one another (and their {@code hashCode()} changes as annotations are
   * inferred for them), so a hash map keyed by ATypeElements would conflate elements that need to
   * be distinguished.
   */
  public static class AnnotationsInContexts extends IdentityHashMap<ATypeElement, Set<String>> {
    /** UID for serialization. */
    private static final long serialVersionUID = 20200321L;

    /** Creates a new, empty AnnotationsInContexts. */
    public AnnotationsInContexts() {}
  }
}
