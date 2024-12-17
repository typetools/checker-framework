package org.checkerframework.common.wholeprograminference;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.ReceiverParameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.DefaultPrettyPrinterVisitor;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.afu.scenelib.util.JVMNames;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.wholeprograminference.WholeProgramInference.OutputFormat;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.ajava.AnnotationMirrorToAnnotationExprConversion;
import org.checkerframework.framework.ajava.AnnotationTransferVisitor;
import org.checkerframework.framework.ajava.DefaultJointVisitor;
import org.checkerframework.framework.ajava.JointJavacJavaParserVisitor;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.JavaParserUtil;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.plumelib.util.ArraySet;
import org.plumelib.util.CollectionsPlume;
import org.plumelib.util.DeepCopyable;
import org.plumelib.util.IPair;
import org.plumelib.util.UtilPlume;

/**
 * This is an implementation of {@link WholeProgramInferenceStorage} that stores annotations
 * directly with the JavaParser node corresponding to the annotation's location. It outputs ajava
 * files.
 */
public class WholeProgramInferenceJavaParserStorage
    implements WholeProgramInferenceStorage<AnnotatedTypeMirror> {

  /**
   * Directory where .ajava files will be written to and read from. This directory is relative to
   * where the javac command is executed.
   */
  public static final File AJAVA_FILES_PATH = new File("build", "whole-program-inference");

  /** The type factory associated with this. */
  protected final AnnotatedTypeFactory atypeFactory;

  /** The element utilities for {@code atypeFactory}. */
  protected final Elements elements;

  /**
   * Maps from binary class name to the wrapper containing the class. Contains all classes in Java
   * source files containing an Element for which an annotation has been inferred.
   */
  private Map<@BinaryName String, ClassOrInterfaceAnnos> classToAnnos = new HashMap<>();

  /** Maps from binary class name to binary names of all supertypes. */
  private Map<@BinaryName String, Set<@BinaryName String>> supertypesMap = new HashMap<>();

  /** Maps from binary class name to binary names of all known subtypes. */
  private Map<@BinaryName String, Set<@BinaryName String>> subtypesMap = new HashMap<>();

  /**
   * Files containing classes for which an annotation has been inferred since the last time files
   * were written to disk.
   */
  private Set<String> modifiedFiles = new HashSet<>();

  /** Mapping from source file to the wrapper for the compilation unit parsed from that file. */
  private Map<String, CompilationUnitAnnos> sourceToAnnos = new HashMap<>();

  /** Maps from binary class name to the source file that contains it. */
  private Map<String, String> classToSource = new HashMap<>();

  /** Whether the {@code -AinferOutputOriginal} option was supplied to the checker. */
  private final boolean inferOutputOriginal;

  /**
   * Returns the names of all qualifiers that are marked with {@link InvisibleQualifier}, and that
   * are supported by the given type factory.
   *
   * @param atypeFactory a type factory
   * @return the names of every invisible qualifier supported by {@code atypeFactory}
   */
  public static Set<String> getInvisibleQualifierNames(AnnotatedTypeFactory atypeFactory) {
    return atypeFactory.getSupportedTypeQualifiers().stream()
        .filter(WholeProgramInferenceJavaParserStorage::isInvisible)
        .map(Class::getCanonicalName)
        .collect(Collectors.toSet());
  }

  /**
   * Is the definition of the given annotation class annotated with {@link InvisibleQualifier}?
   *
   * @param qual an annotation class
   * @return true iff {@code qual} is meta-annotated with {@link InvisibleQualifier}
   */
  @Pure
  public static boolean isInvisible(Class<? extends Annotation> qual) {
    return Arrays.stream(qual.getAnnotations())
        .anyMatch(anno -> anno.annotationType() == InvisibleQualifier.class);
  }

  /**
   * Constructs a new {@code WholeProgramInferenceJavaParser} that has not yet inferred any
   * annotations.
   *
   * @param atypeFactory the associated type factory
   * @param inferOutputOriginal whether the -AinferOutputOriginal option was supplied to the checker
   */
  public WholeProgramInferenceJavaParserStorage(
      AnnotatedTypeFactory atypeFactory, boolean inferOutputOriginal) {
    this.atypeFactory = atypeFactory;
    this.elements = atypeFactory.getElementUtils();
    this.inferOutputOriginal = inferOutputOriginal;
  }

  @Override
  public String getFileForElement(Element elt) {
    return addClassesForElement(elt);
  }

  @Override
  public void setFileModified(String path) {
    modifiedFiles.add(path);
  }

  /**
   * Set the source file as modified, for the given class.
   *
   * @param className the binary name of a class that should be written to disk
   */
  private void setClassModified(@Nullable @BinaryName String className) {
    if (className == null) {
      return;
    }
    String path = classToSource.get(className);
    if (path != null) {
      setFileModified(path);
    }
  }

  /**
   * Set the source files as modified, for all the given classes.
   *
   * @param classNames the binary names of classes that should be written to disk
   */
  private void setClassesModified(@Nullable Collection<@BinaryName String> classNames) {
    if (classNames == null) {
      return;
    }
    for (String className : classNames) {
      setClassModified(className);
    }
  }

  /**
   * For every modified file, consider its subclasses and superclasses modified, too. The reason is
   * that an annotation change in a class might require annotations in its superclasses and
   * supclasses to be modified, in order to preserve behavioral subtyping. Setting it modified will
   * cause it to be written out, and while writing out, the annotations will be made consistent
   * across the class hierarchy by {@link #wpiPrepareCompilationUnitForWriting}.
   */
  public void setSupertypesAndSubtypesModified() {
    // Copy into a list to avoid a ConcurrentModificationException.
    for (String path : new ArrayList<>(modifiedFiles)) {
      CompilationUnitAnnos cuAnnos = sourceToAnnos.get(path);
      for (ClassOrInterfaceAnnos classAnnos : cuAnnos.types) {
        String className = classAnnos.className;
        setClassesModified(supertypesMap.get(className));
        setClassesModified(subtypesMap.get(className));
      }
    }
  }

  //
  // Reading stored annotations
  //

  @Override
  public boolean hasStorageLocationForMethod(ExecutableElement methodElt) {
    return getMethodAnnos(methodElt) != null;
  }

  @Override
  public AnnotationMirrorSet getMethodDeclarationAnnotations(ExecutableElement methodElt) {
    String className = ElementUtils.getEnclosingClassName(methodElt);
    // Read in classes for the element.
    getFileForElement(methodElt);
    ClassOrInterfaceAnnos classAnnos = classToAnnos.get(className);
    if (classAnnos == null) {
      return AnnotationMirrorSet.emptySet();
    }
    CallableDeclarationAnnos methodAnnos =
        classAnnos.callableDeclarations.get(JVMNames.getJVMMethodSignature(methodElt));
    if (methodAnnos == null) {
      return AnnotationMirrorSet.emptySet();
    }
    return methodAnnos.getDeclarationAnnotations();
  }

  /**
   * Get the annotations for a method or constructor.
   *
   * @param methodElt the method or constructor
   * @return the annotations for a method or constructor
   */
  private @Nullable CallableDeclarationAnnos getMethodAnnos(ExecutableElement methodElt) {
    String className = ElementUtils.getEnclosingClassName(methodElt);
    // Read in classes for the element.
    getFileForElement(methodElt);
    ClassOrInterfaceAnnos classAnnos = classToAnnos.get(className);
    if (classAnnos == null) {
      return null;
    }
    CallableDeclarationAnnos methodAnnos =
        classAnnos.callableDeclarations.get(JVMNames.getJVMMethodSignature(methodElt));
    return methodAnnos;
  }

  /**
   * Get the annotations for a field.
   *
   * @param fieldElt a field
   * @return the annotations for a field
   */
  private @Nullable FieldAnnos getFieldAnnos(VariableElement fieldElt) {
    String className = ElementUtils.getEnclosingClassName(fieldElt);
    // Read in classes for the element.
    getFileForElement(fieldElt);
    ClassOrInterfaceAnnos classAnnos = classToAnnos.get(className);
    if (classAnnos == null) {
      return null;
    }
    FieldAnnos fieldAnnos = classAnnos.fields.get(fieldElt.getSimpleName().toString());
    return fieldAnnos;
  }

  @Override
  public AnnotatedTypeMirror getParameterAnnotations(
      ExecutableElement methodElt,
      @Positive int index_1based,
      AnnotatedTypeMirror paramATM,
      VariableElement ve,
      AnnotatedTypeFactory atypeFactory) {
    if (index_1based == 0) {
      throw new TypeSystemError(
          "0 is illegal as index argument to addDeclarationAnnotationToFormalParameter");
    }
    CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElt);
    if (methodAnnos == null) {
      // When processing anonymous inner classes outside their compilation units,
      // it might not have been possible to create an appropriate CallableDeclarationAnnos:
      // no element would have been available, causing the computed method signature to
      // be incorrect. In this case, abort looking up annotations -- inference will fail,
      // because even if WPI inferred something, it couldn't be printed.
      return paramATM;
    }
    return methodAnnos.getParameterTypeInitialized(paramATM, index_1based, atypeFactory);
  }

  @Override
  public AnnotatedTypeMirror getReceiverAnnotations(
      ExecutableElement methodElt,
      AnnotatedTypeMirror paramATM,
      AnnotatedTypeFactory atypeFactory) {
    CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElt);
    if (methodAnnos == null) {
      // See the comment on the similar exception in #getParameterAnnotations, above.
      return paramATM;
    }
    return methodAnnos.getReceiverType(paramATM, atypeFactory);
  }

  @Override
  public AnnotatedTypeMirror getReturnAnnotations(
      ExecutableElement methodElt, AnnotatedTypeMirror atm, AnnotatedTypeFactory atypeFactory) {
    CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElt);
    if (methodAnnos == null) {
      // See the comment on the similar exception in #getParameterAnnotations, above.
      return atm;
    }
    return methodAnnos.getReturnType(atm, atypeFactory);
  }

  @Override
  public @Nullable AnnotatedTypeMirror getFieldAnnotations(
      Element element,
      String fieldName,
      AnnotatedTypeMirror lhsATM,
      AnnotatedTypeFactory atypeFactory) {
    ClassSymbol enclosingClass = ((VarSymbol) element).enclClass();
    // Read in classes for the element.
    getFileForElement(element);
    @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
    @BinaryName String className = enclosingClass.flatname.toString();
    ClassOrInterfaceAnnos classAnnos = classToAnnos.get(className);
    if (classAnnos == null) {
      return null;
    }
    // If it's an enum constant it won't appear as a field
    // and it won't have extra annotations, so just return the basic type:
    if (classAnnos.enumConstants.contains(fieldName)) {
      return lhsATM;
    } else if (classAnnos.fields.get(fieldName) == null) {
      // There might not be a corresponding entry for the field name
      // in an anonymous class, if the field and class were defined in
      // another compilation unit (for the same reason that a method
      // might not have an entry, as in #getParameterAnnotations, above).
      return null;
    } else {
      return classAnnos.fields.get(fieldName).getType(lhsATM, atypeFactory);
    }
  }

  @Override
  public AnnotatedTypeMirror getPreOrPostconditions(
      String className,
      Analysis.BeforeOrAfter preOrPost,
      ExecutableElement methodElement,
      String expression,
      AnnotatedTypeMirror declaredType,
      AnnotatedTypeFactory atypeFactory) {
    switch (preOrPost) {
      case BEFORE:
        return getPreconditionsForExpression(
            className, methodElement, expression, declaredType, atypeFactory);
      case AFTER:
        return getPostconditionsForExpression(
            className, methodElement, expression, declaredType, atypeFactory);
      default:
        throw new BugInCF("Unexpected " + preOrPost);
    }
  }

  /**
   * Returns the precondition annotations for the given expression.
   *
   * @param className the class that contains the method, for diagnostics only
   * @param methodElement the method
   * @param expression the expression
   * @param declaredType the declared type of the expression
   * @param atypeFactory the type factory
   * @return the precondition annotations for a field
   */
  private AnnotatedTypeMirror getPreconditionsForExpression(
      String className,
      ExecutableElement methodElement,
      String expression,
      AnnotatedTypeMirror declaredType,
      AnnotatedTypeFactory atypeFactory) {
    CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElement);
    if (methodAnnos == null) {
      // See the comment on the similar exception in #getParameterAnnotations, above.
      return declaredType;
    }
    return methodAnnos.getPreconditionsForExpression(
        className,
        methodElement.getSimpleName().toString(),
        expression,
        declaredType,
        atypeFactory);
  }

  /**
   * Returns the postcondition annotations for an expression.
   *
   * @param className the class that contains the method, for diagnostics only
   * @param methodElement the method
   * @param expression the expression
   * @param declaredType the declared type of the expression
   * @param atypeFactory the type factory
   * @return the postcondition annotations for a field
   */
  private AnnotatedTypeMirror getPostconditionsForExpression(
      String className,
      ExecutableElement methodElement,
      String expression,
      AnnotatedTypeMirror declaredType,
      AnnotatedTypeFactory atypeFactory) {
    CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElement);
    if (methodAnnos == null) {
      // See the comment on the similar exception in #getParameterAnnotations, above.
      return declaredType;
    }
    return methodAnnos.getPostconditionsForExpression(
        className,
        methodElement.getSimpleName().toString(),
        expression,
        declaredType,
        atypeFactory);
  }

  @Override
  public boolean addMethodDeclarationAnnotation(
      ExecutableElement methodElt, AnnotationMirror anno) {

    CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElt);
    if (methodAnnos == null) {
      // See the comment on the similar exception in #getParameterAnnotations, above.
      return false;
    }
    boolean isNewAnnotation = methodAnnos.addDeclarationAnnotation(anno);
    if (isNewAnnotation) {
      modifiedFiles.add(getFileForElement(methodElt));
    }
    return isNewAnnotation;
  }

  @Override
  public boolean removeMethodDeclarationAnnotation(ExecutableElement elt, AnnotationMirror anno) {
    CallableDeclarationAnnos methodAnnos = getMethodAnnos(elt);
    if (methodAnnos == null) {
      // See the comment on the similar exception in #getParameterAnnotations, above.
      return false;
    }
    return methodAnnos.removeDeclarationAnnotation(anno);
  }

  @Override
  public boolean addFieldDeclarationAnnotation(VariableElement field, AnnotationMirror anno) {
    FieldAnnos fieldAnnos = getFieldAnnos(field);
    if (fieldAnnos == null) {
      // See the comment on the similar exception in #getParameterAnnotations, above.
      return false;
    }
    boolean isNewAnnotation = fieldAnnos != null && fieldAnnos.addDeclarationAnnotation(anno);
    if (isNewAnnotation) {
      modifiedFiles.add(getFileForElement(field));
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
    CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElt);
    if (methodAnnos == null) {
      // See the comment on the similar exception in #getParameterAnnotations, above.
      return false;
    }
    boolean isNewAnnotation =
        methodAnnos.addDeclarationAnnotationToFormalParameter(anno, index_1based);
    if (isNewAnnotation) {
      modifiedFiles.add(getFileForElement(methodElt));
    }
    return isNewAnnotation;
  }

  @Override
  public boolean addClassDeclarationAnnotation(TypeElement classElt, AnnotationMirror anno) {
    String className = ElementUtils.getBinaryName(classElt);
    ClassOrInterfaceAnnos classAnnos = classToAnnos.get(className);
    if (classAnnos == null) {
      // See the comment on the similar exception in #getParameterAnnotations, above.
      return false;
    }
    boolean isNewAnnotation = classAnnos.addAnnotationToClassDeclaration(anno);
    if (isNewAnnotation) {
      modifiedFiles.add(getFileForElement(classElt));
    }
    return isNewAnnotation;
  }

  @Override
  public AnnotatedTypeMirror atmFromStorageLocation(
      TypeMirror typeMirror, AnnotatedTypeMirror storageLocation) {
    if (typeMirror.getKind() == TypeKind.TYPEVAR) {
      // Only copy the primary annotation, because we don't currently have
      // support for inferring type bounds. This avoids accidentally substituting the
      // use of the type variable for its declaration when inferring annotations on
      // fields with a type variable as their type.
      AnnotatedTypeMirror asExpectedType =
          AnnotatedTypeMirror.createType(typeMirror, atypeFactory, false);
      asExpectedType.replaceAnnotations(storageLocation.getPrimaryAnnotations());
      return asExpectedType;
    } else {
      return storageLocation;
    }
  }

  @Override
  public void updateStorageLocationFromAtm(
      AnnotatedTypeMirror newATM,
      AnnotatedTypeMirror curATM,
      AnnotatedTypeMirror typeToUpdate,
      TypeUseLocation defLoc,
      boolean ignoreIfAnnotated) {
    // Only update the AnnotatedTypeMirror if there are no explicit annotations
    if (curATM.getExplicitAnnotations().isEmpty() || !ignoreIfAnnotated) {
      for (AnnotationMirror am : newATM.getPrimaryAnnotations()) {
        typeToUpdate.replaceAnnotation(am);
      }
    } else if (curATM.getKind() == TypeKind.TYPEVAR) {
      // getExplicitAnnotations will be non-empty for type vars whose bounds are explicitly
      // annotated.  So instead, only insert the annotation if there is not primary annotation
      // of the same hierarchy.
      for (AnnotationMirror am : newATM.getPrimaryAnnotations()) {
        if (curATM.getPrimaryAnnotationInHierarchy(am) != null) {
          // Don't insert if the type is already has a primary annotation
          // in the same hierarchy.
          break;
        }
        typeToUpdate.replaceAnnotation(am);
      }
    }

    // Need to check both newATM and curATM, because one might be a declared type
    // even if the other is an array: it is permitted to assign e.g., a String[]
    // to a location with static type Object **and vice-versa** (if a cast is used).
    if (newATM.getKind() == TypeKind.ARRAY && curATM.getKind() == TypeKind.ARRAY) {
      AnnotatedArrayType newAAT = (AnnotatedArrayType) newATM;
      AnnotatedArrayType oldAAT = (AnnotatedArrayType) curATM;
      AnnotatedArrayType aatToUpdate = (AnnotatedArrayType) typeToUpdate;
      updateStorageLocationFromAtm(
          newAAT.getComponentType(),
          oldAAT.getComponentType(),
          aatToUpdate.getComponentType(),
          defLoc,
          ignoreIfAnnotated);
    }
  }

  //
  // Reading in files
  //

  @Override
  public void preprocessClassTree(ClassTree classTree) {
    addClassTree(classTree);
  }

  /**
   * Reads in the source file containing {@code tree} and creates wrappers around all classes in the
   * file. Stores the wrapper for the compilation unit in {@link #sourceToAnnos} and stores the
   * wrappers of all classes in the file in {@link #classToAnnos}.
   *
   * @param tree tree for class to add
   */
  private void addClassTree(ClassTree tree) {
    TypeElement element = TreeUtils.elementFromDeclaration(tree);
    if (element == null) {
      // TODO: There should be an element here, or there is nowhere to store inferences about
      // `tree`.
      return;
    }
    String className = ElementUtils.getBinaryName(element);
    if (classToAnnos.containsKey(className)) {
      return;
    }

    TypeElement toplevelClass = ElementUtils.toplevelEnclosingTypeElement(element);
    String path = ElementUtils.getSourceFilePath(toplevelClass);
    addSourceFile(path);
    CompilationUnitAnnos sourceAnnos = sourceToAnnos.get(path);
    TypeDeclaration<?> javaParserNode =
        sourceAnnos.getClassOrInterfaceDeclarationByName(toplevelClass.getSimpleName().toString());
    ClassTree toplevelClassTree = atypeFactory.getTreeUtils().getTree(toplevelClass);
    createWrappersForClass(toplevelClassTree, javaParserNode, sourceAnnos);
  }

  /**
   * Reads in the file at {@code path} and creates a wrapper around its compilation unit. Stores the
   * wrapper in {@link #sourceToAnnos}, but doesn't create wrappers around any classes in the file.
   *
   * @param path path to source file to read
   */
  private void addSourceFile(String path) {
    if (sourceToAnnos.containsKey(path)) {
      return;
    }

    CompilationUnit root;
    try {
      root = JavaParserUtil.parseCompilationUnit(new File(path));
    } catch (FileNotFoundException e) {
      throw new BugInCF("Failed to read Java file " + path, e);
    }
    JavaParserUtil.concatenateAddedStringLiterals(root);
    CompilationUnitAnnos sourceAnnos = new CompilationUnitAnnos(root);
    sourceToAnnos.put(path, sourceAnnos);

    Optional<PackageDeclaration> oPackageDecl = root.getPackageDeclaration();
    String prefix = oPackageDecl.isPresent() ? oPackageDecl.get().getName().asString() + "." : "";
    List<@BinaryName String> typeNames = new ArrayList<>();
    for (TypeDeclaration<?> type : root.getTypes()) {
      addDeclaredTypes(type, prefix, typeNames);
    }
    for (String typeName : typeNames) {
      classToSource.put(typeName, path);
    }
  }

  /**
   * Computes the binary names of a type and all nested types.
   *
   * @param td a type declaration
   * @param prefix the package, or package+outerclass, prefix in a binary name
   * @param result a list to which to add the binary names of all classes defined in the compilation
   *     unit
   */
  private static void addDeclaredTypes(
      TypeDeclaration<?> td, String prefix, List<@BinaryName String> result) {
    @SuppressWarnings("signature:assignment") // string concatenation
    @BinaryName String typeName = prefix + td.getName().asString();
    result.add(typeName);
    for (BodyDeclaration<?> member : td.getMembers()) {
      if (member.isTypeDeclaration()) {
        addDeclaredTypes(member.asTypeDeclaration(), typeName + "$", result);
      }
    }
  }

  /**
   * The first two arguments are a javac tree and a JavaParser node representing the same class.
   * This method creates wrappers around all the classes, fields, and methods in that class, and
   * stores those wrappers in {@code sourceAnnos}.
   *
   * @param javacClass javac tree for class
   * @param javaParserClass a JavaParser node corresponding to the same class as {@code javacClass}
   * @param sourceAnnos compilation unit wrapper to add new wrappers to
   */
  private void createWrappersForClass(
      ClassTree javacClass, TypeDeclaration<?> javaParserClass, CompilationUnitAnnos sourceAnnos) {
    JointJavacJavaParserVisitor visitor =
        new DefaultJointVisitor() {

          /**
           * The number of inner classes encountered, for use in computing their names as keys to
           * various maps. This is an estimate only: an error might lead to inaccurate annotations
           * being emitted, but that is ok: WPI should never be run without running the checker
           * again afterwards to check the results. This field is only used when no element for the
           * inner class is available, such as when it comes from another compilation unit.
           */
          private int innerClassCount = 0;

          @Override
          public void processClass(
              ClassTree javacTree, ClassOrInterfaceDeclaration javaParserNode) {
            addClass(javacTree, javaParserNode);
          }

          @Override
          public void processClass(ClassTree javacTree, EnumDeclaration javaParserNode) {
            addClass(javacTree, javaParserNode);
          }

          @Override
          public void processClass(ClassTree javacTree, RecordDeclaration javaParserNode) {
            addClass(javacTree, javaParserNode);
          }

          @Override
          public void processClass(ClassTree javacTree, AnnotationDeclaration javaParserNode) {
            // TODO: consider supporting inferring annotations on annotation
            // declarations.
            // addClass(javacTree, javaParserNode);
          }

          @Override
          public void processNewClass(NewClassTree javacTree, ObjectCreationExpr javaParserNode) {
            ClassTree body = javacTree.getClassBody();
            if (body != null) {
              addClass(body, null);
            }
          }

          /**
           * Creates a wrapper around the class for {@code tree} and stores it in {@code
           * sourceAnnos}.
           *
           * <p>This method computes the name of the class when the element corresponding to tree is
           * null and uses it as the key for {@code classToAnnos}
           *
           * @param tree tree to add. Its corresponding name is used as the key for {@code
           *     classToAnnos}.
           * @param javaParserNode the node corresponding to the declaration, which is used to place
           *     annotations on the class itself. Can be null, e.g. for an anonymous class.
           */
          private void addClass(ClassTree tree, @Nullable TypeDeclaration<?> javaParserNode) {
            String className;
            TypeElement classElt = TreeUtils.elementFromDeclaration(tree);
            if (classElt == null) {
              // If such an element does not exist, compute the name of the class
              // instead. This method of computing the name is not 100% guaranteed to
              // be reliable, but it should be sufficient for WPI's purposes here: if
              // the wrong name is computed, the worst outcome is a false positive
              // because WPI inferred an untrue annotation.
              Optional<String> ofqn = javaParserClass.getFullyQualifiedName();
              if (!ofqn.isPresent()) {
                throw new BugInCF("Missing getFullyQualifiedName() for " + javaParserClass);
              }
              if ("".contentEquals(tree.getSimpleName())) {
                @SuppressWarnings("signature:assignment" // computed from string concatenation
                )
                @BinaryName String computedName = ofqn.get() + "$" + ++innerClassCount;
                className = computedName;
              } else {
                @SuppressWarnings("signature:assignment" // computed from string concatenation
                )
                @BinaryName String computedName = ofqn.get() + "$" + tree.getSimpleName().toString();
                className = computedName;
              }
            } else {
              className = ElementUtils.getBinaryName(classElt);
              for (TypeElement supertypeElement : ElementUtils.getSuperTypes(classElt, elements)) {
                String supertypeName = ElementUtils.getBinaryName(supertypeElement);
                Set<@BinaryName String> supertypeSet =
                    supertypesMap.computeIfAbsent(className, k -> new TreeSet<>());
                supertypeSet.add(supertypeName);
                Set<@BinaryName String> subtypeSet =
                    subtypesMap.computeIfAbsent(supertypeName, k -> new TreeSet<>());
                subtypeSet.add(className);
              }
            }

            ClassOrInterfaceAnnos typeWrapper =
                new ClassOrInterfaceAnnos(className, javaParserNode);
            if (!classToAnnos.containsKey(className)) {
              classToAnnos.put(className, typeWrapper);
            }

            sourceAnnos.types.add(typeWrapper);
          }

          @Override
          public void processMethod(MethodTree javacTree, MethodDeclaration javaParserNode) {
            addCallableDeclaration(javacTree, javaParserNode);
          }

          @Override
          public void processMethod(MethodTree javacTree, ConstructorDeclaration javaParserNode) {
            addCallableDeclaration(javacTree, javaParserNode);
          }

          /**
           * Creates a wrapper around {@code javacTree} with the corresponding declaration {@code
           * javaParserNode} and stores it in {@code sourceAnnos}.
           *
           * @param javacTree javac tree for declaration to add
           * @param javaParserNode a JavaParser node for the same class as {@code javacTree}
           */
          private void addCallableDeclaration(
              MethodTree javacTree, CallableDeclaration<?> javaParserNode) {
            ExecutableElement element = TreeUtils.elementFromDeclaration(javacTree);
            if (element == null) {
              // element can be null if there is no element corresponding to the
              // method, which happens for certain kinds of anonymous classes,
              // such as Ordering$1 in PolyCollectorTypeVar.java in the
              // all-systems test suite.
              return;
            }
            String className = ElementUtils.getEnclosingClassName(element);
            ClassOrInterfaceAnnos enclosingClass = classToAnnos.get(className);
            String executableSignature = JVMNames.getJVMMethodSignature(javacTree);
            if (!enclosingClass.callableDeclarations.containsKey(executableSignature)) {
              enclosingClass.callableDeclarations.put(
                  executableSignature,
                  new CallableDeclarationAnnos(
                      javacClass.getSimpleName().toString(), javaParserNode));
            }
          }

          @Override
          public void processVariable(
              VariableTree javacTree, EnumConstantDeclaration javaParserNode) {
            VariableElement elt = TreeUtils.elementFromDeclaration(javacTree);
            if (!elt.getKind().isField()) {
              throw new BugInCF(elt + " is not a field but a " + elt.getKind());
            }

            String enclosingClassName = ElementUtils.getEnclosingClassName(elt);
            ClassOrInterfaceAnnos enclosingClass = classToAnnos.get(enclosingClassName);
            String fieldName = javacTree.getName().toString();
            enclosingClass.enumConstants.add(fieldName);

            // Ensure that if an enum constant defines a class, that class gets
            // registered properly.  See
            // e.g.
            // https://docs.oracle.com/javase/specs/jls/se17/html/jls-8.html#jls-8.9.1
            // for the specification of an enum constant, which does permit it to
            // define an anonymous class.
            NewClassTree constructor = (NewClassTree) javacTree.getInitializer();
            ClassTree constructorClassBody = constructor.getClassBody();
            if (constructorClassBody != null) {
              // addClass assumes there is an element for its argument, but that is
              // not always true!
              if (TreeUtils.elementFromDeclaration(constructorClassBody) != null) {
                addClass(constructorClassBody, null);
              }
            }
          }

          @Override
          public void processVariable(VariableTree javacTree, VariableDeclarator javaParserNode) {
            // This seems to occur when javacTree is a local variable in the second
            // class located in a source file. If this check returns false, then the
            // below call to TreeUtils.elementFromDeclaration causes a crash.
            if (TreeUtils.elementFromDeclaration(javacTree) == null) {
              return;
            }

            VariableElement elt = TreeUtils.elementFromDeclaration(javacTree);
            if (!elt.getKind().isField()) {
              return;
            }

            String enclosingClassName = ElementUtils.getEnclosingClassName(elt);
            ClassOrInterfaceAnnos enclosingClass = classToAnnos.get(enclosingClassName);
            String fieldName = javacTree.getName().toString();
            if (!enclosingClass.fields.containsKey(fieldName)) {
              enclosingClass.fields.put(fieldName, new FieldAnnos(javaParserNode));
            }
          }
        };
    visitor.visitClass(javacClass, javaParserClass);
  }

  /**
   * Calls {@link #addSourceFile(String)} for the file containing the given element.
   *
   * @param element the element for the source file to add
   * @return path of the file containing {@code element}
   */
  private String addClassesForElement(Element element) {
    if (!ElementUtils.isElementFromSourceCode(element)) {
      throw new BugInCF("Called addClassesForElement for non-source element: " + element);
    }

    TypeElement toplevelClass = ElementUtils.toplevelEnclosingTypeElement(element);
    String path = ElementUtils.getSourceFilePath(toplevelClass);
    if (toplevelClass.getKind() == ElementKind.ANNOTATION_TYPE) {
      // Inferring annotations on elements of annotation declarations is not supported.
      // One issue with supporting inference on annotation declaration elements is that
      // AnnotatedTypeFactory#declarationFromElement returns null for annotation declarations
      // quite commonly (because Trees#getTree, which it delegates to, does as well).
      // In this case, we return path here without actually attempting to create the wrappers
      // for the annotation declaration. The rest of WholeProgramInferenceJavaParserStorage
      // already needs to handle classes without entries in the various tables (because of the
      // possibility of classes outside the current compilation unit), so this is safe.
      return path;
    }
    if (classToAnnos.containsKey(ElementUtils.getBinaryName(toplevelClass))) {
      return path;
    }

    addSourceFile(path);
    CompilationUnitAnnos sourceAnnos = sourceToAnnos.get(path);
    ClassTree toplevelClassTree = (ClassTree) atypeFactory.declarationFromElement(toplevelClass);
    TypeDeclaration<?> javaParserNode =
        sourceAnnos.getClassOrInterfaceDeclarationByName(toplevelClass.getSimpleName().toString());
    createWrappersForClass(toplevelClassTree, javaParserNode, sourceAnnos);
    return path;
  }

  //
  // Writing to a file
  //

  // The prepare*ForWriting hooks are needed in addition to the postProcessClassTree hook because
  // a scene may be modifed and written at any time, including before or after
  // postProcessClassTree is called.

  /**
   * Side-effects the compilation unit annotations to make any desired changes before writing to a
   * file.
   *
   * @param compilationUnitAnnos the compilation unit annotations to modify
   */
  public void wpiPrepareCompilationUnitForWriting(CompilationUnitAnnos compilationUnitAnnos) {
    for (ClassOrInterfaceAnnos type : compilationUnitAnnos.types) {
      wpiPrepareClassForWriting(
          type, supertypesMap.get(type.className), subtypesMap.get(type.className));
    }
  }

  /**
   * Side-effects the class annotations to make any desired changes before writing to a file.
   *
   * <p>Because of the side effect, clients may want to pass a copy into this method.
   *
   * @param classAnnos the class annotations to modify
   * @param supertypes the binary names of all supertypes; not side-effected
   * @param subtypes the binary names of all subtypes; not side-effected
   */
  public void wpiPrepareClassForWriting(
      ClassOrInterfaceAnnos classAnnos,
      Collection<@BinaryName String> supertypes,
      Collection<@BinaryName String> subtypes) {
    if (classAnnos.callableDeclarations.isEmpty()) {
      return;
    }

    for (Map.Entry<String, CallableDeclarationAnnos> methodEntry :
        classAnnos.callableDeclarations.entrySet()) {
      String jvmSignature = methodEntry.getKey();
      List<CallableDeclarationAnnos> inSupertypes =
          findOverrides(jvmSignature, supertypesMap.get(classAnnos.className));
      List<CallableDeclarationAnnos> inSubtypes =
          findOverrides(jvmSignature, subtypesMap.get(classAnnos.className));

      wpiPrepareMethodForWriting(methodEntry.getValue(), inSupertypes, inSubtypes);
    }
  }

  /**
   * Return all the CallableDeclarationAnnos for the given signature.
   *
   * @param jvmSignature the JVM signature
   * @param typeNames a collection of type names
   * @return the CallableDeclarationAnnos for the given signature, in all of the types
   */
  private List<CallableDeclarationAnnos> findOverrides(
      String jvmSignature, @Nullable Collection<@BinaryName String> typeNames) {
    if (typeNames == null) {
      return Collections.emptyList();
    }
    List<CallableDeclarationAnnos> result = new ArrayList<>();
    for (String typeName : typeNames) {
      ClassOrInterfaceAnnos classAnnos = classToAnnos.get(typeName);
      if (classAnnos != null) {
        CallableDeclarationAnnos callableAnnos = classAnnos.callableDeclarations.get(jvmSignature);
        if (callableAnnos != null) {
          result.add(callableAnnos);
        }
      }
    }
    return result;
  }

  /**
   * Side-effects the method or constructor annotations to make any desired changes before writing
   * to a file. For example, this method may make inferred annotations consistent with one another
   * between superclasses and subclasses.
   *
   * @param methodAnnos the method or constructor annotations to modify
   * @param inSupertypes the method or constructor annotations for all overridden methods; not
   *     side-effected
   * @param inSubtypes the method or constructor annotations for all overriding methods; not
   *     side-effected
   */
  // TODO:  Inferred annotations must be consistent both with one another and with
  // programmer-written annotations.  The latter are stored in elements and, with the given formal
  // parameter list, are not accessible to this method.  In the future, the annotations stored in
  // elements should also be passed to this method (or maybe they are already available to the
  // type factory?).  I'm leaving that enhancement until later.
  public void wpiPrepareMethodForWriting(
      CallableDeclarationAnnos methodAnnos,
      Collection<CallableDeclarationAnnos> inSupertypes,
      Collection<CallableDeclarationAnnos> inSubtypes) {
    atypeFactory.wpiPrepareMethodForWriting(methodAnnos, inSupertypes, inSubtypes);
  }

  @Override
  public void writeResultsToFile(OutputFormat outputFormat, BaseTypeChecker checker) {
    if (outputFormat != OutputFormat.AJAVA) {
      throw new BugInCF("WholeProgramInferenceJavaParser used with output format " + outputFormat);
    }

    File outputDir = AJAVA_FILES_PATH;
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }

    setSupertypesAndSubtypesModified();

    for (String path : modifiedFiles) {
      // This calls deepCopy() because wpiPrepareCompilationUnitForWriting performs side
      // effects that we don't want to be persistent.
      CompilationUnitAnnos root = sourceToAnnos.get(path).deepCopy();
      wpiPrepareCompilationUnitForWriting(root);
      File packageDir;
      if (!root.compilationUnit.getPackageDeclaration().isPresent()) {
        packageDir = AJAVA_FILES_PATH;
      } else {
        packageDir =
            new File(
                AJAVA_FILES_PATH,
                root.compilationUnit
                    .getPackageDeclaration()
                    .get()
                    .getNameAsString()
                    .replaceAll("\\.", File.separator));
      }

      if (!packageDir.exists()) {
        packageDir.mkdirs();
      }

      String name = new File(path).getName();
      if (name.endsWith(".java")) {
        name = name.substring(0, name.length() - ".java".length());
      }

      String nameWithChecker = name + "-" + checker.getClass().getCanonicalName() + ".ajava";
      File outputPath = new File(packageDir, nameWithChecker);
      if (this.inferOutputOriginal) {
        File outputPathNoCheckerName = new File(packageDir, name + ".ajava");
        // Avoid re-writing this file for each checker that was run.
        if (Files.notExists(outputPathNoCheckerName.toPath())) {
          writeAjavaFile(outputPathNoCheckerName, root);
        }
      }
      root.transferAnnotations(checker);
      writeAjavaFile(outputPath, root);
    }

    modifiedFiles.clear();
  }

  /**
   * Write an ajava file to disk.
   *
   * @param outputPath the path to which the ajava file should be written
   * @param root the compilation unit to be written
   */
  private void writeAjavaFile(File outputPath, CompilationUnitAnnos root) {
    try (Writer writer = Files.newBufferedWriter(outputPath.toPath(), StandardCharsets.UTF_8)) {

      // This commented implementation uses JavaParser's lexical preserving printing, which
      // writes the file such that its formatting is close to the original source file it was
      // parsed from as possible. It is commented out because this feature is very buggy and
      // crashes when adding annotations in certain locations.
      // LexicalPreservingPrinter.print(root.declaration, writer);

      // Do not print invisible qualifiers, to avoid cluttering the output.
      Set<String> invisibleQualifierNames = getInvisibleQualifierNames(this.atypeFactory);
      DefaultPrettyPrinter prettyPrinter =
          new DefaultPrettyPrinter() {
            @Override
            public String print(Node node) {
              VoidVisitor<Void> visitor =
                  new DefaultPrettyPrinterVisitor(getConfiguration()) {
                    @Override
                    public void visit(MarkerAnnotationExpr n, Void arg) {
                      if (invisibleQualifierNames.contains(n.getName().toString())) {
                        return;
                      }
                      super.visit(n, arg);
                    }

                    @Override
                    public void visit(SingleMemberAnnotationExpr n, Void arg) {
                      if (invisibleQualifierNames.contains(n.getName().toString())) {
                        return;
                      }
                      super.visit(n, arg);
                    }

                    @Override
                    public void visit(NormalAnnotationExpr n, Void arg) {
                      if (invisibleQualifierNames.contains(n.getName().toString())) {
                        return;
                      }
                      super.visit(n, arg);
                    }

                    // visit(CharLiteralExpr) and visit(StringLiteralExpr) work around bugs in
                    // JavaParser, with respect to handling lonely surrogate characters.

                    @Override
                    public void visit(final CharLiteralExpr n, final Void arg) {
                      String value = n.getValue();
                      if (value.length() == 1) {
                        char c = value.charAt(0);
                        if (Character.isSurrogate(c)) {
                          n.setValue(String.format("\\u%04X", (int) c));
                        }
                      }
                      super.visit(n, arg);
                    }

                    @Override
                    public void visit(final StringLiteralExpr n, final Void arg) {
                      n.setValue(escapeLonelySurrogates(n.getValue()));

                      super.visit(n, arg);
                    }
                  };
              node.accept(visitor, null);
              return visitor.toString();
            }
          };

      writer.write(prettyPrinter.print(root.compilationUnit));
    } catch (IOException e) {
      throw new BugInCF("Error while writing ajava file " + outputPath, e);
    }
  }

  // TODO: Move these two routines to StringUtils.

  /**
   * Returns the index of a lonely surrogate character in its argument, or -1 if there is none.
   *
   * @param s a string
   * @return the index of a lonely surrogate character in its argument, or -1 if there is none
   */
  private int indexOfLonelySurrogateCharacter(String s) {
    int limit = s.length();
    for (int i = 0; i < limit; i++) {
      if (Character.isSurrogate(s.charAt(i))) {
        if (i == limit - 1) {
          return i;
        } else if (Character.isSurrogatePair(s.charAt(i), s.charAt(i + 1))) {
          i++;
        } else {
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Replace lonely surrogate characters by their unicode escape.
   *
   * @param s a string
   * @return the string, with lonely surrogate characters replaced by their unicode escape
   */
  private String escapeLonelySurrogates(String s) {
    int idx = indexOfLonelySurrogateCharacter(s);
    if (idx != -1) {
      // This recursion is less efficient than a loop with StringBuilder would be,
      // but there should rarely be lonely surrogate characters.
      s =
          s.substring(0, idx)
              + "\\u"
              + String.format("%04X", (int) s.charAt(idx))
              + escapeLonelySurrogates(s.substring(idx + 1));
    }
    return s;
  }

  /**
   * Adds an explicit receiver type to a JavaParser method declaration.
   *
   * @param methodDeclaration declaration to add a receiver to
   */
  private static void addExplicitReceiver(MethodDeclaration methodDeclaration) {
    if (methodDeclaration.getReceiverParameter().isPresent()) {
      return;
    }

    com.github.javaparser.ast.Node parent = methodDeclaration.getParentNode().get();
    if (!(parent instanceof TypeDeclaration)) {
      return;
    }

    TypeDeclaration<?> parentDecl = (TypeDeclaration<?>) parent;
    ClassOrInterfaceType receiver = new ClassOrInterfaceType();
    receiver.setName(parentDecl.getName());
    if (parentDecl.isClassOrInterfaceDeclaration()) {
      ClassOrInterfaceDeclaration parentClassDecl = parentDecl.asClassOrInterfaceDeclaration();
      if (!parentClassDecl.getTypeParameters().isEmpty()) {
        NodeList<Type> typeArgs = new NodeList<>();
        for (TypeParameter typeParam : parentClassDecl.getTypeParameters()) {
          ClassOrInterfaceType typeArg = new ClassOrInterfaceType();
          typeArg.setName(typeParam.getNameAsString());
          typeArgs.add(typeArg);
        }

        receiver.setTypeArguments(typeArgs);
      }
    }

    methodDeclaration.setReceiverParameter(new ReceiverParameter(receiver, "this"));
  }

  /**
   * Transfers all annotations for {@code annotatedType} and its nested types to {@code target},
   * which is the JavaParser node representing the same type. Does nothing if {@code annotatedType}
   * is null (this may occur if there are no inferred annotations for the type).
   *
   * @param annotatedType type to transfer annotations from
   * @param target the JavaParser type to transfer annotation to; must represent the same type as
   *     {@code annotatedType}
   */
  private static void transferAnnotations(
      @Nullable AnnotatedTypeMirror annotatedType, Type target) {
    if (annotatedType == null) {
      return;
    }

    target.accept(new AnnotationTransferVisitor(), annotatedType);
  }

  //
  // Storing annotations
  //

  /**
   * Stores the JavaParser node for a compilation unit and the list of wrappers for the classes and
   * interfaces in that compilation unit.
   */
  private static class CompilationUnitAnnos implements DeepCopyable<CompilationUnitAnnos> {
    /** Compilation unit being wrapped. */
    public final CompilationUnit compilationUnit;

    /** Wrappers for classes and interfaces in {@code compilationUnit}. */
    public final List<ClassOrInterfaceAnnos> types;

    /**
     * Constructs a wrapper around the given compilation unit.
     *
     * @param compilationUnit compilation unit to wrap
     */
    public CompilationUnitAnnos(CompilationUnit compilationUnit) {
      this.compilationUnit = compilationUnit;
      this.types = new ArrayList<>();
    }

    /**
     * Private constructor for use by deepCopy().
     *
     * @param compilationUnit compilation unit to wrap
     * @param types wrappers for classes and interfaces in {@code compilationUnit}
     */
    private CompilationUnitAnnos(
        CompilationUnit compilationUnit, List<ClassOrInterfaceAnnos> types) {
      this.compilationUnit = compilationUnit;
      this.types = types;
    }

    @Override
    public CompilationUnitAnnos deepCopy() {
      return new CompilationUnitAnnos(compilationUnit, CollectionsPlume.deepCopy(types));
    }

    /**
     * Transfers all annotations inferred by whole program inference for the wrapped compilation
     * unit to their corresponding JavaParser locations.
     *
     * @param checker the checker who's name to include in the @AnnotatedFor annotation
     */
    public void transferAnnotations(BaseTypeChecker checker) {
      JavaParserUtil.clearAnnotations(compilationUnit);
      for (TypeDeclaration<?> typeDecl : compilationUnit.getTypes()) {
        typeDecl.addSingleMemberAnnotation(
            "org.checkerframework.framework.qual.AnnotatedFor",
            "\"" + checker.getClass().getCanonicalName() + "\"");
      }

      for (ClassOrInterfaceAnnos typeAnnos : types) {
        typeAnnos.transferAnnotations();
      }
    }

    /**
     * Returns the top-level type declaration named {@code name} in the compilation unit.
     *
     * @param name name of type declaration
     * @return the type declaration named {@code name} in the wrapped compilation unit
     */
    public TypeDeclaration<?> getClassOrInterfaceDeclarationByName(String name) {
      return JavaParserUtil.getTypeDeclarationByName(compilationUnit, name);
    }

    /**
     * Returns a verbose printed representation of this.
     *
     * @return a verbose printed representation of this
     */
    @SuppressWarnings("UnusedMethod")
    public String toStringVerbose() {
      StringJoiner sb = new StringJoiner(System.lineSeparator());
      sb.add("CompilationUnitAnnos:");
      for (ClassOrInterfaceAnnos type : types) {
        sb.add(type.toStringVerbose());
      }
      return sb.toString();
    }
  }

  /**
   * Stores wrappers for the locations where annotations may be inferred in a class or interface.
   */
  private static class ClassOrInterfaceAnnos implements DeepCopyable<ClassOrInterfaceAnnos> {
    /**
     * Mapping from JVM method signatures to the wrapper containing the corresponding executable.
     */
    public Map<String, CallableDeclarationAnnos> callableDeclarations = new HashMap<>();

    /** Mapping from field names to wrappers for those fields. */
    public Map<String, FieldAnnos> fields = new HashMap<>(2);

    /** Collection of declared enum constants (empty if not an enum). */
    public Set<String> enumConstants = new HashSet<>(2);

    /**
     * Annotations on the declaration of the class (note that despite the name, these can also be
     * type annotations).
     */
    private @MonotonicNonNull AnnotationMirrorSet classAnnotations = null;

    /**
     * The JavaParser TypeDeclaration representing the class's declaration. Used for placing
     * annotations inferred on the class declaration itself.
     */
    private @MonotonicNonNull TypeDeclaration<?> classDeclaration;

    /** The binary name of the class. */
    private @BinaryName String className;

    /**
     * Create a new ClassOrInterfaceAnnos.
     *
     * @param className the binary name of the class
     * @param javaParserNode the JavaParser node corresponding to the class declaration, which is
     *     used for placing annotations on the class declaration
     */
    public ClassOrInterfaceAnnos(
        @BinaryName String className, @Nullable TypeDeclaration<?> javaParserNode) {
      this.classDeclaration = javaParserNode;
      this.className = className;
    }

    @Override
    public ClassOrInterfaceAnnos deepCopy() {
      ClassOrInterfaceAnnos result = new ClassOrInterfaceAnnos(className, classDeclaration);
      result.callableDeclarations = CollectionsPlume.deepCopyValues(callableDeclarations);
      result.fields = CollectionsPlume.deepCopyValues(fields);
      result.enumConstants = UtilPlume.clone(enumConstants); // no deep copy: elements are strings
      if (classAnnotations != null) {
        result.classAnnotations = classAnnotations.deepCopy();
      }
      // no need to change classDeclaration
      return result;
    }

    /**
     * Adds {@code annotation} to the set of annotations on the declaration of this class.
     *
     * @param annotation an annotation (can be declaration or type)
     * @return true if this is a new annotation for this class
     */
    public boolean addAnnotationToClassDeclaration(AnnotationMirror annotation) {
      if (classAnnotations == null) {
        classAnnotations = new AnnotationMirrorSet();
      }

      return classAnnotations.add(annotation);
    }

    /**
     * Transfers all annotations inferred by whole program inference for the methods and fields in
     * the wrapper class or interface to their corresponding JavaParser locations.
     */
    public void transferAnnotations() {
      for (CallableDeclarationAnnos callableAnnos : callableDeclarations.values()) {
        callableAnnos.transferAnnotations();
      }

      if (classAnnotations != null && classDeclaration != null) {
        for (AnnotationMirror annotation : classAnnotations) {
          classDeclaration.addAnnotation(
              AnnotationMirrorToAnnotationExprConversion.annotationMirrorToAnnotationExpr(
                  annotation));
        }
      }

      for (FieldAnnos field : fields.values()) {
        field.transferAnnotations();
      }
    }

    @Override
    public String toString() {
      String fieldsString = fields.toString();
      if (fieldsString.length() > 100) {
        // The quoting increases the likelihood that all delimiters are balanced in the
        // result.  That makes it easier to manipulate the result (such as skipping over it)
        // in an editor.  The quoting also makes clear that the value is truncated.
        fieldsString = "\"" + fieldsString.substring(0, 95) + "...\"";
      }

      return "ClassOrInterfaceAnnos ["
          + (classDeclaration == null ? "unnamed" : classDeclaration.getName())
          + ": callableDeclarations="
          // For deterministic output
          + new TreeMap<>(callableDeclarations)
          + ", fields="
          + fieldsString
          + "]";
    }

    /**
     * Returns a verbose printed representation of this.
     *
     * @return a verbose printed representation of this
     */
    public String toStringVerbose() {
      return toString();
    }
  }

  /**
   * Stores the JavaParser node for a method or constructor and the annotations that have been
   * inferred about its parameters and return type.
   */
  public class CallableDeclarationAnnos implements DeepCopyable<CallableDeclarationAnnos> {
    /** The class that contains the method. */
    public final String className;

    /** Wrapped method or constructor declaration. */
    public final CallableDeclaration<?> declaration;

    /**
     * Inferred annotations for the return type, if the declaration represents a method. Initialized
     * on first usage.
     */
    private @MonotonicNonNull AnnotatedTypeMirror returnType = null;

    /**
     * Inferred annotations for the receiver type, if the declaration represents a method.
     * Initialized on first usage.
     */
    private @MonotonicNonNull AnnotatedTypeMirror receiverType = null;

    /**
     * Inferred annotations for parameter types. The list is initialized the first time any
     * parameter is accessed, and each parameter is initialized the first time it's accessed.
     */
    private @MonotonicNonNull List<@Nullable AnnotatedTypeMirror> parameterTypes = null;

    /** Declaration annotations on the parameters. */
    private @MonotonicNonNull Set<IPair<Integer, AnnotationMirror>> paramsDeclAnnos = null;

    /**
     * Annotations on the callable declaration. This does not include preconditions and
     * postconditions.
     */
    private @MonotonicNonNull AnnotationMirrorSet declarationAnnotations = null;

    /**
     * Mapping from expression strings to pairs of (inferred precondition, declared type). The keys
     * are strings representing JavaExpressions, using the same format as a user would in an {@link
     * org.checkerframework.framework.qual.RequiresQualifier} annotation.
     */
    private @MonotonicNonNull Map<String, InferredDeclared> preconditions = null;

    /**
     * Mapping from expression strings to pairs of (inferred postcondition, declared type). The
     * okeys are strings representing JavaExpressions, using the same format as a user would in an
     * {@link org.checkerframework.framework.qual.EnsuresQualifier} annotation.
     */
    private @MonotonicNonNull Map<String, InferredDeclared> postconditions = null;

    /**
     * Creates a wrapper for the given method or constructor declaration.
     *
     * @param className the class that contains the method, for diagnostics only
     * @param declaration method or constructor declaration to wrap
     */
    public CallableDeclarationAnnos(String className, CallableDeclaration<?> declaration) {
      this.className = className;
      this.declaration = declaration;
    }

    @Override
    public CallableDeclarationAnnos deepCopy() {
      CallableDeclarationAnnos result = new CallableDeclarationAnnos(className, declaration);
      result.returnType = DeepCopyable.deepCopyOrNull(this.returnType);
      result.receiverType = DeepCopyable.deepCopyOrNull(this.receiverType);
      if (parameterTypes != null) {
        result.parameterTypes = CollectionsPlume.deepCopy(this.parameterTypes);
      }
      result.declarationAnnotations = DeepCopyable.deepCopyOrNull(this.declarationAnnotations);

      if (this.paramsDeclAnnos != null) {
        result.paramsDeclAnnos = new ArraySet<>(this.paramsDeclAnnos);
      }
      result.preconditions = deepCopyMapOfStringToPair(this.preconditions);
      result.postconditions = deepCopyMapOfStringToPair(this.postconditions);
      return result;
    }

    /**
     * Returns the inferred type for the parameter at the given index. If necessary, initializes the
     * {@code AnnotatedTypeMirror} for that location using {@code type} and {@code atf} to a wrapper
     * around the base type for the parameter.
     *
     * @param type type for the parameter at {@code index}, used for initializing the returned
     *     {@code AnnotatedTypeMirror} the first time it's accessed
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used
     * @param index_1based index of the parameter to return the inferred annotations of (1-based)
     * @return an {@code AnnotatedTypeMirror} containing all annotations inferred for the parameter
     *     at the given index
     */
    public AnnotatedTypeMirror getParameterTypeInitialized(
        AnnotatedTypeMirror type, @Positive int index_1based, AnnotatedTypeFactory atf) {
      // 0-based index
      int i = index_1based - 1;

      if (parameterTypes == null) {
        parameterTypes =
            new ArrayList<>(Collections.nCopies(declaration.getParameters().size(), null));
      }

      if (parameterTypes.get(i) == null) {
        parameterTypes.set(i, AnnotatedTypeMirror.createType(type.getUnderlyingType(), atf, false));
      }

      return parameterTypes.get(i);
    }

    /**
     * Returns the inferred type for the parameter at the given index, or null if there's no
     * parameter at the given index or there's no inferred type for that parameter.
     *
     * @param index index of the parameter to return the inferred annotations of
     * @return an {@code AnnotatedTypeMirror} containing all annotations inferred for the parameter
     *     at the given index, or null if there's no parameter at {@code index} or if there's not
     *     inferred annotations for that parameter
     */
    public @Nullable AnnotatedTypeMirror getParameterType(int index) {
      if (parameterTypes == null || index < 0 || index >= parameterTypes.size()) {
        return null;
      }

      return parameterTypes.get(index);
    }

    /**
     * Adds a declaration annotation to this parameter and returns whether it was a new annotation.
     *
     * @param annotation the declaration annotation to add
     * @param index_1based index of the parameter (1-indexed)
     * @return true if {@code annotation} wasn't previously stored for this parameter
     */
    public boolean addDeclarationAnnotationToFormalParameter(
        AnnotationMirror annotation, @Positive int index_1based) {
      if (index_1based == 0) {
        throw new TypeSystemError(
            "0 is illegal as index argument to addDeclarationAnnotationToFormalParameter");
      }
      if (paramsDeclAnnos == null) {
        // There are usually few formal parameters.
        paramsDeclAnnos = new ArraySet<>(4);
      }

      return paramsDeclAnnos.add(IPair.of(index_1based, annotation));
    }

    /**
     * If this wrapper holds a method, returns the inferred type of the receiver. If necessary,
     * initializes the {@code AnnotatedTypeMirror} for that location using {@code type} and {@code
     * atf} to a wrapper around the base type for the receiver type.
     *
     * @param type base type for the receiver type, used for initializing the returned {@code
     *     AnnotatedTypeMirror} the first time it's accessed
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used
     * @return an {@code AnnotatedTypeMirror} containing all annotations inferred for the receiver
     *     type
     */
    public AnnotatedTypeMirror getReceiverType(AnnotatedTypeMirror type, AnnotatedTypeFactory atf) {
      if (receiverType == null) {
        receiverType = AnnotatedTypeMirror.createType(type.getUnderlyingType(), atf, false);
      }

      return receiverType;
    }

    /**
     * If this wrapper holds a method, returns the inferred type of the return type. If necessary,
     * initializes the {@code AnnotatedTypeMirror} for that location using {@code type} and {@code
     * atf} to a wrapper around the base type for the return type.
     *
     * @param type base type for the return type, used for initializing the returned {@code
     *     AnnotatedTypeMirror} the first time it's accessed
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used
     * @return an {@code AnnotatedTypeMirror} containing all annotations inferred for the return
     *     type
     */
    public AnnotatedTypeMirror getReturnType(AnnotatedTypeMirror type, AnnotatedTypeFactory atf) {
      if (returnType == null) {
        returnType = AnnotatedTypeMirror.createType(type.getUnderlyingType(), atf, false);
      }

      return returnType;
    }

    /**
     * Returns the inferred declaration annotations on this executable. Returns an empty set if
     * there are no annotations.
     *
     * @return the declaration annotations for this callable declaration
     */
    public AnnotationMirrorSet getDeclarationAnnotations() {
      if (declarationAnnotations == null) {
        return AnnotationMirrorSet.emptySet();
      }

      return AnnotationMirrorSet.unmodifiableSet(declarationAnnotations);
    }

    /**
     * Adds a declaration annotation to this callable declaration and returns whether it was a new
     * annotation.
     *
     * @param annotation the declaration annotation to add
     * @return true if {@code annotation} wasn't previously stored for this callable declaration
     */
    public boolean addDeclarationAnnotation(AnnotationMirror annotation) {
      if (declarationAnnotations == null) {
        declarationAnnotations = new AnnotationMirrorSet();
      }

      return declarationAnnotations.add(annotation);
    }

    /**
     * Attempts to remove the given declaration annotation from this callable declaration and
     * returns whether an annotation was successfully removed.
     *
     * @param anno an annotation
     * @return true if {@code anno} was removed; false if it was not present or otherwise couldn't
     *     be removed
     */
    /*package-private*/ boolean removeDeclarationAnnotation(AnnotationMirror anno) {
      if (declarationAnnotations == null) {
        return false;
      }
      return declarationAnnotations.remove(anno);
    }

    /**
     * Returns the inferred preconditions for this callable declaration. The keys of the returned
     * map use the same string formatting as the {@link
     * org.checkerframework.framework.qual.RequiresQualifier} annotation, e.g. "#1" for the first
     * parameter.
     *
     * <p>Although the map is immutable, the AnnotatedTypeMirrors within it can be modified, and
     * such changes will be reflected in the receiver CallableDeclarationAnnos object.
     *
     * @return a mapping from Java expression string to pairs of (inferred precondition for the
     *     expression, declared type of the expression)
     * @see #getPreconditionsForExpression
     */
    public Map<String, InferredDeclared> getPreconditions() {
      if (preconditions == null) {
        return Collections.emptyMap();
      } else {
        return Collections.unmodifiableMap(preconditions);
      }
    }

    /**
     * Returns the inferred postconditions for this callable declaration. The keys of the returned
     * map use the same string formatting as the {@link
     * org.checkerframework.framework.qual.EnsuresQualifier} annotation, e.g. "#1" for the first
     * parameter.
     *
     * <p>Although the map is immutable, the AnnotatedTypeMirrors within it can be modified, and
     * such changes will be reflected in the receiver CallableDeclarationAnnos object.
     *
     * @return a mapping from Java expression string to pairs of (inferred postcondition for the
     *     expression, declared type of the expression)
     * @see #getPostconditionsForExpression
     */
    public Map<String, InferredDeclared> getPostconditions() {
      if (postconditions == null) {
        return Collections.emptyMap();
      }

      return Collections.unmodifiableMap(postconditions);
    }

    /**
     * Returns an AnnotatedTypeMirror containing the preconditions for the given expression. Changes
     * to the returned AnnotatedTypeMirror are reflected in this CallableDeclarationAnnos.
     *
     * @param className the class that contains the method, for diagnostics only
     * @param methodName the method name, for diagnostics only
     * @param expression a string representing a Java expression, in the same format as the argument
     *     to a {@link org.checkerframework.framework.qual.RequiresQualifier} annotation
     * @param declaredType the declared type of {@code expression}
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used
     * @return an {@code AnnotatedTypeMirror} containing the annotations for the inferred
     *     preconditions for the given expression
     */
    public AnnotatedTypeMirror getPreconditionsForExpression(
        String className,
        String methodName,
        String expression,
        AnnotatedTypeMirror declaredType,
        AnnotatedTypeFactory atf) {
      if (preconditions == null) {
        preconditions = new HashMap<>(1);
      }

      if (!preconditions.containsKey(expression)) {
        AnnotatedTypeMirror preconditionsType =
            AnnotatedTypeMirror.createType(declaredType.getUnderlyingType(), atf, false);
        preconditions.put(expression, new InferredDeclared(preconditionsType, declaredType));
      }

      return preconditions.get(expression).inferred;
    }

    /**
     * Returns an AnnotatedTypeMirror containing the postconditions for the given expression.
     * Changes to the returned AnnotatedTypeMirror are reflected in this CallableDeclarationAnnos.
     *
     * @param className the class that contains the method, for diagnostics only
     * @param methodName the method name, for diagnostics only
     * @param expression a string representing a Java expression, in the same format as the argument
     *     to a {@link org.checkerframework.framework.qual.EnsuresQualifier} annotation
     * @param declaredType the declared type of {@code expression}
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used
     * @return an {@code AnnotatedTypeMirror} containing the annotations for the inferred
     *     postconditions for the given expression
     */
    public AnnotatedTypeMirror getPostconditionsForExpression(
        String className,
        String methodName,
        String expression,
        AnnotatedTypeMirror declaredType,
        AnnotatedTypeFactory atf) {
      if (postconditions == null) {
        postconditions = new HashMap<>(1);
      }

      if (!postconditions.containsKey(expression)) {
        AnnotatedTypeMirror postconditionsType =
            AnnotatedTypeMirror.createType(declaredType.getUnderlyingType(), atf, false);
        postconditions.put(expression, new InferredDeclared(postconditionsType, declaredType));
      }

      InferredDeclared postAndDecl = postconditions.get(expression);
      AnnotatedTypeMirror result = postAndDecl.inferred;
      return result;
    }

    /**
     * Transfers all annotations inferred by whole program inference for the return type, receiver
     * type, and parameter types for the wrapped declaration to their corresponding JavaParser
     * locations.
     */
    public void transferAnnotations() {
      if (atypeFactory instanceof GenericAnnotatedTypeFactory<?, ?, ?, ?>) {
        GenericAnnotatedTypeFactory<?, ?, ?, ?> genericAtf =
            (GenericAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory;
        for (AnnotationMirror contractAnno : genericAtf.getContractAnnotations(this)) {
          declaration.addAnnotation(
              AnnotationMirrorToAnnotationExprConversion.annotationMirrorToAnnotationExpr(
                  contractAnno));
        }
      }

      if (declarationAnnotations != null && declaration != null) {
        for (AnnotationMirror annotation : declarationAnnotations) {
          declaration.addAnnotation(
              AnnotationMirrorToAnnotationExprConversion.annotationMirrorToAnnotationExpr(
                  annotation));
        }
      }

      if (paramsDeclAnnos != null) {
        for (IPair<Integer, AnnotationMirror> pair : paramsDeclAnnos) {
          Parameter param = declaration.getParameter(pair.first - 1);
          param.addAnnotation(
              AnnotationMirrorToAnnotationExprConversion.annotationMirrorToAnnotationExpr(
                  pair.second));
        }
      }

      if (returnType != null) {
        // If a return type exists, then the declaration must be a method, not a
        // constructor.
        WholeProgramInferenceJavaParserStorage.transferAnnotations(
            returnType, declaration.asMethodDeclaration().getType());
      }

      if (receiverType != null) {
        addExplicitReceiver(declaration.asMethodDeclaration());
        // The receiver won't be present for an anonymous class.
        if (declaration.getReceiverParameter().isPresent()) {
          WholeProgramInferenceJavaParserStorage.transferAnnotations(
              receiverType, declaration.getReceiverParameter().get().getType());
        }
      }

      if (parameterTypes == null) {
        return;
      }

      for (int i = 0; i < parameterTypes.size(); i++) {
        AnnotatedTypeMirror inferredType = parameterTypes.get(i);
        if (inferredType == null) {
          // Can occur if the only places that this method was called were
          // outside the compilation unit.
          continue;
        }
        Parameter param = declaration.getParameter(i);
        Type javaParserType = param.getType();
        if (param.isVarArgs()) {
          NodeList<AnnotationExpr> varArgsAnnoExprs =
              AnnotationMirrorToAnnotationExprConversion.annotationMirrorSetToAnnotationExprList(
                  inferredType.getPrimaryAnnotations());
          param.setVarArgsAnnotations(varArgsAnnoExprs);

          AnnotatedTypeMirror inferredComponentType =
              ((AnnotatedArrayType) inferredType).getComponentType();
          WholeProgramInferenceJavaParserStorage.transferAnnotations(
              inferredComponentType, javaParserType);
        } else {
          WholeProgramInferenceJavaParserStorage.transferAnnotations(inferredType, javaParserType);
        }
      }
    }

    @Override
    public String toString() {
      StringJoiner sj =
          new StringJoiner(
              "," + System.lineSeparator() + "  ",
              "CallableDeclarationAnnos{",
              System.lineSeparator() + "}");
      sj.add("returnType = " + returnType);
      sj.add("receiverType = " + receiverType);
      sj.add("parameterTypes = " + parameterTypes);
      sj.add("paramsDeclAnnos = " + paramsDeclAnnos);
      sj.add("declarationAnnotations = " + declarationAnnotations);
      sj.add("preconditions = " + preconditions);
      return sj.toString();
    }
  }

  /**
   * Deep copy (according to the {@code DeepCopyable} interface) a pre- or post-condition map.
   *
   * @param orig the map to copy
   * @return a deep copy of the map
   */
  private static @Nullable Map<String, InferredDeclared> deepCopyMapOfStringToPair(
      @Nullable Map<String, InferredDeclared> orig) {
    if (orig == null) {
      return null;
    }
    Map<String, InferredDeclared> result = new HashMap<>(CollectionsPlume.mapCapacity(orig.size()));
    result.clear();
    for (Map.Entry<String, InferredDeclared> entry : orig.entrySet()) {
      String javaExpression = entry.getKey();
      InferredDeclared atms = entry.getValue();
      result.put(
          javaExpression, new InferredDeclared(atms.inferred.deepCopy(), atms.declared.deepCopy()));
    }
    return result;
  }

  /** Stores the JavaParser node for a field and the annotations that have been inferred for it. */
  private static class FieldAnnos implements DeepCopyable<FieldAnnos> {
    /** Wrapped field declaration. */
    public final VariableDeclarator declaration;

    /** Inferred type for field, initialized the first time it's accessed. */
    private @MonotonicNonNull AnnotatedTypeMirror type = null;

    /** Annotations on the field declaration. */
    private @MonotonicNonNull AnnotationMirrorSet declarationAnnotations = null;

    /**
     * Creates a wrapper for the given field declaration.
     *
     * @param declaration field declaration to wrap
     */
    public FieldAnnos(VariableDeclarator declaration) {
      this.declaration = declaration;
    }

    @Override
    public FieldAnnos deepCopy() {
      FieldAnnos result = new FieldAnnos(declaration);
      result.type = DeepCopyable.deepCopyOrNull(this.type);
      result.declarationAnnotations = DeepCopyable.deepCopyOrNull(this.declarationAnnotations);
      return result;
    }

    /**
     * Returns the inferred type of the field. If necessary, initializes the {@code
     * AnnotatedTypeMirror} for that location using {@code type} and {@code atf} to a wrapper around
     * the base type for the field.
     *
     * @param type base type for the field, used for initializing the returned {@code
     *     AnnotatedTypeMirror} the first time it's accessed
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used
     * @return an {@code AnnotatedTypeMirror} containing all annotations inferred for the field
     */
    public AnnotatedTypeMirror getType(AnnotatedTypeMirror type, AnnotatedTypeFactory atf) {
      if (this.type == null) {
        this.type = AnnotatedTypeMirror.createType(type.getUnderlyingType(), atf, false);
      }

      return this.type;
    }

    /**
     * Returns the inferred declaration annotations on this field, or an empty set if there are no
     * annotations.
     *
     * @return the declaration annotations for this field declaration
     */
    @SuppressWarnings("UnusedMethod")
    public AnnotationMirrorSet getDeclarationAnnotations() {
      if (declarationAnnotations == null) {
        return AnnotationMirrorSet.emptySet();
      }

      return AnnotationMirrorSet.unmodifiableSet(declarationAnnotations);
    }

    /**
     * Adds a declaration annotation to this field declaration and returns whether it was a new
     * annotation.
     *
     * @param annotation declaration annotation to add
     * @return true if {@code annotation} wasn't previously stored for this field declaration
     */
    public boolean addDeclarationAnnotation(AnnotationMirror annotation) {
      if (declarationAnnotations == null) {
        declarationAnnotations = new AnnotationMirrorSet();
      }

      return declarationAnnotations.add(annotation);
    }

    /**
     * Transfers all annotations inferred by whole program inference on this field to the JavaParser
     * nodes for that field.
     */
    public void transferAnnotations() {
      if (declarationAnnotations != null) {
        // Don't add directly to the type of the variable declarator,
        // because declaration annotations need to be attached to the FieldDeclaration
        // node instead.
        Node declParent = declaration.getParentNode().orElse(null);
        if (declParent instanceof FieldDeclaration) {
          FieldDeclaration decl = (FieldDeclaration) declParent;
          for (AnnotationMirror annotation : declarationAnnotations) {
            decl.addAnnotation(
                AnnotationMirrorToAnnotationExprConversion.annotationMirrorToAnnotationExpr(
                    annotation));
          }
        }
      }

      // Don't transfer type annotations to variable declarators with sibling
      // variable declarators, because they're printed incorrectly (as "???").
      // (A variable declarator can have siblings if it's part of a declaration
      // like "int x, y, z;", which is bad style but legal Java.)
      // In any event, WPI doesn't consider the LUB of the types of the siblings,
      // so any inferred type is likely to be wrong.
      // TODO: avoid inferring these types at all, or take the LUB of all assignments
      // to the siblings. Unfortunately, VariableElements don't track whether they have
      // siblings, and there's no other information about the declaration for
      // WholeProgramInferenceImplementation to use: to determine that there are siblings,
      // a parse tree is needed.
      boolean foundVariableDeclarator = false;
      for (Node child : this.declaration.getParentNode().get().getChildNodes()) {
        if (child instanceof VariableDeclarator) {
          if (foundVariableDeclarator) {
            // This is the second VariableDeclarator that was found.
            return;
          }
          foundVariableDeclarator = true;
        }
      }
      Type newType = (Type) declaration.getType().accept(new CloneVisitor(), null);
      WholeProgramInferenceJavaParserStorage.transferAnnotations(type, newType);
      declaration.setType(newType);
    }

    @Override
    public String toString() {
      return "FieldAnnos [declaration=" + declaration + ", type=" + type + "]";
    }
  }

  /** A pair of two annotated types: an inferred type and a declared type. */
  public static class InferredDeclared {
    /** The inferred type. */
    public final AnnotatedTypeMirror inferred;

    /** The declared type. */
    public final AnnotatedTypeMirror declared;

    /**
     * Creates an InferredDeclared.
     *
     * @param inferred the inferred type
     * @param declared the declared type
     */
    public InferredDeclared(AnnotatedTypeMirror inferred, AnnotatedTypeMirror declared) {
      this.inferred = inferred;
      this.declared = declared;
    }

    @Override
    public String toString() {
      return "InferredDeclared(" + inferred + ", " + declared + ")";
    }
  }
}
