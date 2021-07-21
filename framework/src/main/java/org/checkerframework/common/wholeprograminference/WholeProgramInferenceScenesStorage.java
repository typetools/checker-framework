package org.checkerframework.common.wholeprograminference;

import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.wholeprograminference.WholeProgramInference.OutputFormat;
import org.checkerframework.common.wholeprograminference.scenelib.ASceneWrapper;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.UserError;
import scenelib.annotations.Annotation;
import scenelib.annotations.el.AClass;
import scenelib.annotations.el.AField;
import scenelib.annotations.el.AMethod;
import scenelib.annotations.el.AScene;
import scenelib.annotations.el.ATypeElement;
import scenelib.annotations.el.TypePathEntry;
import scenelib.annotations.io.IndexFileParser;
import scenelib.annotations.util.JVMNames;

/**
 * This class stores annotations using scenelib objects.
 *
 * <p>The set of annotations inferred for a certain class is stored in an {@link
 * scenelib.annotations.el.AScene}, which {@code writeScenes()} can write into a file. For example,
 * a class {@code my.pakkage.MyClass} will have its members' inferred types stored in a Scene, and
 * later written into a file named {@code my.pakkage.MyClass.jaif} if using {@link
 * OutputFormat#JAIF}, or {@code my.pakkage.MyClass.astub} if using {@link OutputFormat#STUB}.
 *
 * <p>This class populates the initial Scenes by reading existing .jaif files on the {@link
 * #JAIF_FILES_PATH} directory (regardless of output format). Having more information in those
 * initial .jaif files means that the precision achieved by the whole-program inference analysis
 * will be better. {@link #writeScenes} rewrites the initial .jaif files and may create new ones.
 */
public class WholeProgramInferenceScenesStorage
    implements WholeProgramInferenceStorage<ATypeElement> {

  /**
   * Directory where .jaif files will be written to and read from. This directory is relative to
   * where the CF's javac command is executed.
   */
  public static final String JAIF_FILES_PATH =
      "build" + File.separator + "whole-program-inference" + File.separator;

  /** The type factory associated with this WholeProgramInferenceScenesStorage. */
  protected final AnnotatedTypeFactory atypeFactory;

  /** Annotations that should not be output to a .jaif or stub file. */
  private final AnnotationsInContexts annosToIgnore = new AnnotationsInContexts();

  /**
   * If true, assignments where the rhs is null are be ignored.
   *
   * <p>If all assignments to a variable are null (because inference is being done with respect to a
   * limited set of uses) then the variable is inferred to have bottom type. That inference is
   * unlikely to be correct. To avoid that inference, set this variable to true. When the variable
   * is true, if all assignments are null, then none are recorded, no inference is done, and the
   * variable remains at its default type.
   */
  private final boolean ignoreNullAssignments;

  /** Maps .jaif file paths (Strings) to Scenes. Relative to JAIF_FILES_PATH. */
  public final Map<String, ASceneWrapper> scenes = new HashMap<>();

  /**
   * Scenes that were modified since the last time all Scenes were written into .jaif files. Each
   * String element of this set is a path (relative to JAIF_FILES_PATH) to the .jaif file of the
   * corresponding Scene in the set. It is obtained by passing a class name as argument to the
   * {@link #getJaifPath} method.
   *
   * <p>Modifying a Scene means adding (or changing) a type annotation for a field, method return
   * type, or method parameter type in the Scene. (Scenes are modified by the method {@link
   * #updateAnnotationSetInScene}.)
   */
  public final Set<String> modifiedScenes = new HashSet<>();

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

  /**
   * Default constructor.
   *
   * @param atypeFactory the type factory associated with this WholeProgramInferenceScenesStorage
   */
  public WholeProgramInferenceScenesStorage(AnnotatedTypeFactory atypeFactory) {
    this.atypeFactory = atypeFactory;
    boolean isNullness =
        atypeFactory.getClass().getSimpleName().equals("NullnessAnnotatedTypeFactory");
    this.ignoreNullAssignments = !isNullness;
  }

  @Override
  public String getFileForElement(Element elt) {
    String className;
    switch (elt.getKind()) {
      case CONSTRUCTOR:
      case METHOD:
        className = ElementUtils.getEnclosingClassName((ExecutableElement) elt);
        break;
      case LOCAL_VARIABLE:
        className = getEnclosingClassName((LocalVariableNode) elt);
        break;
      case FIELD:
        ClassSymbol enclosingClass = ((VarSymbol) elt).enclClass();
        className = enclosingClass.flatname.toString();
        break;
      default:
        throw new BugInCF("What element? %s %s", elt.getKind(), elt);
    }
    String file = getJaifPath(className);
    return file;
  }

  /**
   * Get the annotations for a class.
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
   * Get the annotations for a method or constructor.
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

  @Override
  public boolean hasStorageLocationForMethod(ExecutableElement methodElt) {
    // The scenes implementation can always add annotations to a method.
    return true;
  }

  @Override
  public ATypeElement getParameterAnnotations(
      ExecutableElement methodElt,
      int i,
      AnnotatedTypeMirror paramATM,
      VariableElement ve,
      AnnotatedTypeFactory atypeFactory) {
    AMethod methodAnnos = getMethodAnnos(methodElt);
    AField param =
        methodAnnos.vivifyAndAddTypeMirrorToParameter(
            i, paramATM.getUnderlyingType(), ve.getSimpleName());
    return param.type;
  }

  @Override
  public ATypeElement getReceiverAnnotations(
      ExecutableElement methodElt,
      AnnotatedTypeMirror paramATM,
      AnnotatedTypeFactory atypeFactory) {
    AMethod methodAnnos = getMethodAnnos(methodElt);
    return methodAnnos.receiver.type;
  }

  @Override
  public ATypeElement getReturnAnnotations(
      ExecutableElement methodElt, AnnotatedTypeMirror atm, AnnotatedTypeFactory atypeFactory) {
    AMethod methodAnnos = getMethodAnnos(methodElt);
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
    return field.type;
  }

  @Override
  public ATypeElement getPreOrPostconditions(
      Analysis.BeforeOrAfter preOrPost,
      ExecutableElement methodElement,
      String expression,
      AnnotatedTypeMirror declaredType,
      AnnotatedTypeFactory atypeFactory) {
    switch (preOrPost) {
      case BEFORE:
        return getPreconditionsForExpression(methodElement, expression, declaredType);
      case AFTER:
        return getPostconditionsForExpression(methodElement, expression, declaredType);
      default:
        throw new BugInCF("Unexpected " + preOrPost);
    }
  }

  /**
   * Returns the precondition annotations for a Java expression.
   *
   * @param methodElement the method
   * @param expression the expression
   * @param declaredType the declared type of the expression
   * @return the precondition annotations for a field
   */
  private ATypeElement getPreconditionsForExpression(
      ExecutableElement methodElement, String expression, AnnotatedTypeMirror declaredType) {
    AMethod methodAnnos = getMethodAnnos(methodElement);
    preconditionsToDeclaredTypes.put(methodAnnos.methodSignature + expression, declaredType);
    return methodAnnos.vivifyAndAddTypeMirrorToPrecondition(
            expression, declaredType.getUnderlyingType())
        .type;
  }

  /**
   * Returns the postcondition annotations for a field.
   *
   * @param methodElement the method
   * @param expression the expression
   * @param declaredType the declared type of the expression
   * @return the postcondition annotations for a field
   */
  private ATypeElement getPostconditionsForExpression(
      ExecutableElement methodElement, String expression, AnnotatedTypeMirror declaredType) {
    AMethod methodAnnos = getMethodAnnos(methodElement);
    postconditionsToDeclaredTypes.put(methodAnnos.methodSignature + expression, declaredType);
    return methodAnnos.vivifyAndAddTypeMirrorToPostcondition(
            expression, declaredType.getUnderlyingType())
        .type;
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
              + "nothing was inferred: "
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
              + "nothing was inferred: "
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

    scenelib.annotations.Annotation sceneAnno =
        AnnotationConverter.annotationMirrorToAnnotation(anno);
    boolean isNewAnnotation = methodAnnos.tlAnnotationsHere.add(sceneAnno);
    return isNewAnnotation;
  }

  /**
   * Write all modified scenes into files. (Scenes are modified by the method {@link
   * #updateAnnotationSetInScene}.)
   *
   * @param outputFormat the output format to use when writing files
   * @param checker the checker from which this method is called, for naming stub files
   */
  public void writeScenes(OutputFormat outputFormat, BaseTypeChecker checker) {
    // Create WPI directory if it doesn't exist already.
    File jaifDir = new File(JAIF_FILES_PATH);
    if (!jaifDir.exists()) {
      jaifDir.mkdirs();
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
    String jaifPath = JAIF_FILES_PATH + className + ".jaif";
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
          throw new UserError("Problem while reading %s: %s", jaifPath, e.getMessage());
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
   * Returns the scene-lib representation of the given className in the scene identified by the
   * given jaifPath.
   *
   * @param className the name of the class to get, in binary form
   * @param jaifPath the path to the jaif file that would represent that class (must end in ".jaif")
   * @param classSymbol optionally, the ClassSymbol representing the class. Used to set the symbol
   *     information stored on an AClass.
   * @return a version of the scene-lib representation of the class, augmented with symbol
   *     information if {@code classSymbol} was non-null
   */
  protected AClass getAClass(
      @BinaryName String className, String jaifPath, @Nullable ClassSymbol classSymbol) {
    // Possibly reads .jaif file to obtain a Scene.
    ASceneWrapper scene = getScene(jaifPath);
    AClass aClass = scene.getAScene().classes.getVivify(className);
    scene.updateSymbolInformation(aClass, classSymbol);
    return aClass;
  }

  /**
   * Returns the scene-lib representation of the given className in the scene identified by the
   * given jaifPath.
   *
   * @param className the name of the class to get, in binary form
   * @param jaifPath the path to the jaif file that would represent that class (must end in ".jaif")
   * @return the scene-lib representation of the class, possibly augmented with symbol information
   *     if {@link #getAClass(String, String, com.sun.tools.javac.code.Symbol.ClassSymbol)} has
   *     already been called with a non-null third argument
   */
  protected AClass getAClass(@BinaryName String className, String jaifPath) {
    return getAClass(className, jaifPath, null);
  }

  /**
   * Updates the set of annotations in a location of a Scene, as the result of a pseudo-assignment.
   *
   * <ul>
   *   <li>If there was no previous annotation for that location, then the updated set will be the
   *       annotations in rhsATM.
   *   <li>If there was a previous annotation, the updated set will be the LUB between the previous
   *       annotation and rhsATM.
   * </ul>
   *
   * @param type ATypeElement of the Scene which will be modified
   * @param jaifPath path to a .jaif file for a Scene; used for marking the scene as modified
   *     (needing to be written to disk)
   * @param rhsATM the RHS of the annotated type on the source code
   * @param lhsATM the LHS of the annotated type on the source code
   * @param defLoc the location where the annotation will be added
   * @param ignoreIfAnnotated if true, don't update any type that is explicitly annotated in the
   *     source code
   */
  protected void updateAnnotationSetInScene(
      ATypeElement type,
      TypeUseLocation defLoc,
      AnnotatedTypeMirror rhsATM,
      AnnotatedTypeMirror lhsATM,
      String jaifPath,
      boolean ignoreIfAnnotated) {
    if (rhsATM instanceof AnnotatedNullType && ignoreNullAssignments) {
      return;
    }
    AnnotatedTypeMirror atmFromScene = atmFromStorageLocation(rhsATM.getUnderlyingType(), type);
    updateAtmWithLub(rhsATM, atmFromScene);
    if (lhsATM instanceof AnnotatedTypeVariable) {
      Set<AnnotationMirror> upperAnnos =
          ((AnnotatedTypeVariable) lhsATM).getUpperBound().getEffectiveAnnotations();
      // If the inferred type is a subtype of the upper bounds of the
      // current type on the source code, halt.
      if (upperAnnos.size() == rhsATM.getAnnotations().size()
          && atypeFactory.getQualifierHierarchy().isSubtype(rhsATM.getAnnotations(), upperAnnos)) {
        return;
      }
    }
    updateTypeElementFromATM(type, 1, defLoc, rhsATM, lhsATM, ignoreIfAnnotated);
    modifiedScenes.add(jaifPath);
  }

  /**
   * Updates sourceCodeATM to contain the LUB between sourceCodeATM and jaifATM, ignoring missing
   * AnnotationMirrors from jaifATM -- it considers the LUB between an AnnotationMirror am and a
   * missing AnnotationMirror to be am. The results are stored in sourceCodeATM.
   *
   * @param sourceCodeATM the annotated type on the source code
   * @param jaifATM the annotated type on the .jaif file
   */
  private void updateAtmWithLub(AnnotatedTypeMirror sourceCodeATM, AnnotatedTypeMirror jaifATM) {

    switch (sourceCodeATM.getKind()) {
      case TYPEVAR:
        updateAtmWithLub(
            ((AnnotatedTypeVariable) sourceCodeATM).getLowerBound(),
            ((AnnotatedTypeVariable) jaifATM).getLowerBound());
        updateAtmWithLub(
            ((AnnotatedTypeVariable) sourceCodeATM).getUpperBound(),
            ((AnnotatedTypeVariable) jaifATM).getUpperBound());
        break;
        //        case WILDCARD:
        // Because inferring type arguments is not supported, wildcards won't be encoutered
        //            updateAtmWithLub(((AnnotatedWildcardType)
        // sourceCodeATM).getExtendsBound(),
        //                              ((AnnotatedWildcardType)
        // jaifATM).getExtendsBound());
        //            updateAtmWithLub(((AnnotatedWildcardType)
        // sourceCodeATM).getSuperBound(),
        //                              ((AnnotatedWildcardType) jaifATM).getSuperBound());
        //            break;
      case ARRAY:
        updateAtmWithLub(
            ((AnnotatedArrayType) sourceCodeATM).getComponentType(),
            ((AnnotatedArrayType) jaifATM).getComponentType());
        break;
        // case DECLARED:
        // inferring annotations on type arguments is not supported, so no need to recur on
        // generic types. If this was every implemented, this method would need VisitHistory
        // object to prevent infinite recursion on types such as T extends List<T>.
      default:
        // ATM only has primary annotations
        break;
    }

    // LUB primary annotations
    Set<AnnotationMirror> annosToReplace = new HashSet<>(sourceCodeATM.getAnnotations().size());
    for (AnnotationMirror amSource : sourceCodeATM.getAnnotations()) {
      AnnotationMirror amJaif = jaifATM.getAnnotationInHierarchy(amSource);
      // amJaif only contains  annotations from the jaif, so it might be missing
      // an annotation in the hierarchy
      if (amJaif != null) {
        amSource = atypeFactory.getQualifierHierarchy().leastUpperBound(amSource, amJaif);
      }
      annosToReplace.add(amSource);
    }
    sourceCodeATM.replaceAnnotations(annosToReplace);
  }

  /**
   * Returns true if {@code am} should not be inserted in source code, for example {@link
   * org.checkerframework.common.value.qual.BottomVal}. This happens when {@code am} cannot be
   * inserted in source code or is the default for the location passed as argument.
   *
   * <p>Invisible qualifiers, which are annotations that contain the {@link
   * org.checkerframework.framework.qual.InvisibleQualifier} meta-annotation, also return true.
   *
   * <p>TODO: Merge functionality somewhere else with {@link
   * org.checkerframework.framework.util.defaults.QualifierDefaults}. Look into the
   * createQualifierDefaults method in {@link GenericAnnotatedTypeFactory} (which uses the
   * QualifierDefaults class linked above) before changing anything here. See
   * https://github.com/typetools/checker-framework/issues/683 .
   *
   * @param am an annotation to test for whether it should be inserted into source code
   * @param location where the location would be inserted; used to determine if {@code am} is the
   *     default for that location
   * @param atm its kind is used to determine if {@code am} is the default for that kind
   * @return true if am should not be inserted into source code, or if am is invisible
   */
  private boolean shouldIgnore(
      AnnotationMirror am, TypeUseLocation location, AnnotatedTypeMirror atm) {
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

    // Checks if am is default
    if (elt.getAnnotation(DefaultQualifierInHierarchy.class) != null) {
      return true;
    }
    DefaultQualifier defaultQual = elt.getAnnotation(DefaultQualifier.class);
    if (defaultQual != null) {
      for (TypeUseLocation loc : defaultQual.locations()) {
        if (loc == TypeUseLocation.ALL || loc == location) {
          return true;
        }
      }
    }
    DefaultFor defaultQualForLocation = elt.getAnnotation(DefaultFor.class);
    if (defaultQualForLocation != null) {
      for (TypeUseLocation loc : defaultQualForLocation.value()) {
        if (loc == TypeUseLocation.ALL || loc == location) {
          return true;
        }
      }
    }

    // Checks if am is a default annotation.
    // This case checks if it is meta-annotated with @DefaultFor.
    // TODO: Handle cases of annotations added via an
    // org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator.
    DefaultFor defaultFor = elt.getAnnotation(DefaultFor.class);
    if (defaultFor != null) {
      org.checkerframework.framework.qual.TypeKind[] types = defaultFor.typeKinds();
      TypeKind atmKind = atm.getUnderlyingType().getKind();
      if (hasMatchingTypeKind(atmKind, types)) {
        return true;
      }
    }

    return false;
  }

  /** Returns true, iff a matching TypeKind is found. */
  private boolean hasMatchingTypeKind(
      TypeKind atmKind, org.checkerframework.framework.qual.TypeKind[] types) {
    for (org.checkerframework.framework.qual.TypeKind tk : types) {
      if (tk.name().equals(atmKind.name())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a subset of annosSet, consisting of the annotations supported by the type factory
   * associated with this. These are not necessarily legal annotations: they have the right name,
   * but they may lack elements (fields).
   *
   * @param annosSet a set of annotations
   * @return the annoattions supported by this object's AnnotatedTypeFactory
   */
  private Set<Annotation> getSupportedAnnosInSet(Set<Annotation> annosSet) {
    Set<Annotation> output = new HashSet<>(1);
    Set<Class<? extends java.lang.annotation.Annotation>> supportedAnnos =
        atypeFactory.getSupportedTypeQualifiers();
    for (Annotation anno : annosSet) {
      for (Class<? extends java.lang.annotation.Annotation> clazz : supportedAnnos) {
        // TODO: Remove comparison by name, and make this routine more efficient.
        if (clazz.getName().equals(anno.def.name)) {
          output.add(anno);
        }
      }
    }
    return output;
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
   * {@link scenelib.annotations.Annotation}s of an {@link scenelib.annotations.el.ATypeElement}.
   *
   * @param result the AnnotatedTypeMirror to be modified
   * @param storageLocation the {@link scenelib.annotations.el.ATypeElement} used
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
    updateTypeElementFromATM(typeToUpdate, 1, defLoc, newATM, curATM, ignoreIfAnnotated);
  }

  ///
  /// Writing to a file
  ///

  // The prepare*ForWriting hooks are needed in addition to the postProcessClassTree hook because
  // a scene may be modifed and written at any time, including before or after
  // postProcessClassTree is called.

  /**
   * Side-effects the compilation unit annotations to make any desired changes before writing to a
   * file.
   *
   * @param compilationUnitAnnos the compilation unit annotations to modify
   */
  public void prepareSceneForWriting(AScene compilationUnitAnnos) {
    for (Map.Entry<String, AClass> classEntry : compilationUnitAnnos.classes.entrySet()) {
      prepareClassForWriting(classEntry.getValue());
    }
  }

  /**
   * Side-effects the class annotations to make any desired changes before writing to a file.
   *
   * @param classAnnos the class annotations to modify
   */
  public void prepareClassForWriting(AClass classAnnos) {
    for (Map.Entry<String, AMethod> methodEntry : classAnnos.methods.entrySet()) {
      prepareMethodForWriting(methodEntry.getValue());
    }
  }

  /**
   * Side-effects the method or constructor annotations to make any desired changes before writing
   * to a file.
   *
   * @param methodAnnos the method or constructor annotations to modify
   */
  public void prepareMethodForWriting(AMethod methodAnnos) {
    atypeFactory.prepareMethodForWriting(methodAnnos);
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
    modifiedScenes.add(path);
  }

  @Override
  public void preprocessClassTree(ClassTree classTree) {
    // This implementation does nothing.
  }

  /**
   * Updates an {@link scenelib.annotations.el.ATypeElement} to have the annotations of an {@link
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
   * @param idx used to write annotations on compound types of an ATypeElement
   * @param defLoc the location where the annotation will be added
   * @param newATM the AnnotatedTypeMirror whose annotations will be added to the ATypeElement
   * @param curATM used to check if the element which will be updated has explicit annotations in
   *     source code
   * @param ignoreIfAnnotated if true, don't update any type that is explicitly annotated in the
   *     source code
   */
  private void updateTypeElementFromATM(
      ATypeElement typeToUpdate,
      int idx,
      TypeUseLocation defLoc,
      AnnotatedTypeMirror newATM,
      AnnotatedTypeMirror curATM,
      boolean ignoreIfAnnotated) {
    // Clears only the annotations that are supported by the relevant AnnotatedTypeFactory.
    // The others stay intact.
    if (idx == 1) {
      // This if avoids clearing the annotations multiple times in cases
      // of type variables and compound types.
      Set<Annotation> annosToRemove = getSupportedAnnosInSet(typeToUpdate.tlAnnotationsHere);
      // This method may be called consecutive times for the same ATypeElement.  Each time it is
      // called, the AnnotatedTypeMirror has a better type estimate for the ATypeElement. Therefore,
      // it is not a problem to remove all annotations before inserting the new annotations.
      typeToUpdate.tlAnnotationsHere.removeAll(annosToRemove);
    }

    // Only update the ATypeElement if there are no explicit annotations.
    if (curATM.getExplicitAnnotations().isEmpty() || !ignoreIfAnnotated) {
      for (AnnotationMirror am : newATM.getAnnotations()) {
        addAnnotationsToATypeElement(
            newATM, typeToUpdate, defLoc, am, curATM.hasEffectiveAnnotation(am));
      }
    } else if (curATM.getKind() == TypeKind.TYPEVAR) {
      // getExplicitAnnotations will be non-empty for type vars whose bounds are explicitly
      // annotated.  So instead, only insert the annotation if there is not primary annotation
      // of the same hierarchy.  #shouldIgnore prevent annotations that are subtypes of type
      // vars upper bound from being inserted.
      for (AnnotationMirror am : newATM.getAnnotations()) {
        if (curATM.getAnnotationInHierarchy(am) != null) {
          // Don't insert if the type is already has a primary annotation
          // in the same hierarchy.
          break;
        }
        addAnnotationsToATypeElement(
            newATM, typeToUpdate, defLoc, am, curATM.hasEffectiveAnnotation(am));
      }
    }

    // Recursively update compound type and type variable type if they exist.
    if (newATM.getKind() == TypeKind.ARRAY && curATM.getKind() == TypeKind.ARRAY) {
      AnnotatedArrayType newAAT = (AnnotatedArrayType) newATM;
      AnnotatedArrayType oldAAT = (AnnotatedArrayType) curATM;
      updateTypeElementFromATM(
          typeToUpdate.innerTypes.getVivify(
              TypePathEntry.getTypePathEntryListFromBinary(Collections.nCopies(2 * idx, 0))),
          idx + 1,
          defLoc,
          newAAT.getComponentType(),
          oldAAT.getComponentType(),
          ignoreIfAnnotated);
    }
  }

  private void addAnnotationsToATypeElement(
      AnnotatedTypeMirror newATM,
      ATypeElement typeToUpdate,
      TypeUseLocation defLoc,
      AnnotationMirror am,
      boolean isEffectiveAnnotation) {
    Annotation anno = AnnotationConverter.annotationMirrorToAnnotation(am);
    typeToUpdate.tlAnnotationsHere.add(anno);
    if (isEffectiveAnnotation || shouldIgnore(am, defLoc, newATM)) {
      // firstKey works as a unique identifier for each annotation
      // that should not be inserted in source code
      String firstKey = aTypeElementToString(typeToUpdate);
      Pair<String, TypeUseLocation> key = Pair.of(firstKey, defLoc);
      Set<String> annosIgnored = annosToIgnore.get(key);
      if (annosIgnored == null) {
        annosIgnored = new HashSet<>();
        annosToIgnore.put(key, annosIgnored);
      }
      annosIgnored.add(anno.def().toString());
    }
  }

  /**
   * Returns a string representation of an ATypeElement, for use as part of a key in {@link
   * AnnotationsInContexts}.
   *
   * @param aType an ATypeElement to convert to a string representation
   * @return a string representation of the argument
   */
  public static String aTypeElementToString(ATypeElement aType) {
    // return aType.description.toString() + aType.tlAnnotationsHere;
    return aType.description.toString();
  }

  /**
   * Maps the {@link #aTypeElementToString} representation of an ATypeElement and its
   * TypeUseLocation to a set of names of annotations.
   */
  public static class AnnotationsInContexts
      extends HashMap<Pair<String, TypeUseLocation>, Set<String>> {
    private static final long serialVersionUID = 20200321L;
  }

  /**
   * Returns the "flatname" of the class enclosing {@code localVariableNode}.
   *
   * @param localVariableNode the {@link LocalVariableNode}
   * @return the "flatname" of the class enclosing {@code localVariableNode}
   */
  private static @BinaryName String getEnclosingClassName(LocalVariableNode localVariableNode) {
    return ElementUtils.getBinaryName(
        ElementUtils.enclosingTypeElement(localVariableNode.getElement()));
  }
}
