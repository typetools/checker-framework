package org.checkerframework.common.wholeprograminference;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.ReceiverParameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.wholeprograminference.WholeProgramInference.OutputFormat;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.framework.ajava.AnnotationMirrorToAnnotationExprConversion;
import org.checkerframework.framework.ajava.AnnotationTransferVisitor;
import org.checkerframework.framework.ajava.DefaultJointVisitor;
import org.checkerframework.framework.ajava.JointJavacJavaParserVisitor;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.JavaParserUtil;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import scenelib.annotations.util.JVMNames;

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
  public static final String AJAVA_FILES_PATH =
      "build" + File.separator + "whole-program-inference" + File.separator;

  /** The type factory associated with this. */
  protected final AnnotatedTypeFactory atypeFactory;

  /**
   * Maps from binary class name to the wrapper containing the class. Contains all classes in Java
   * source files containing an Element for which an annotation has been inferred.
   */
  private Map<@BinaryName String, ClassOrInterfaceAnnos> classToAnnos = new HashMap<>();

  /**
   * Files containing classes for which an annotation has been inferred since the last time files
   * were written to disk.
   */
  private Set<String> modifiedFiles = new HashSet<>();

  /** Mapping from source file to the wrapper for the compilation unit parsed from that file. */
  private Map<String, CompilationUnitAnnos> sourceToAnnos = new HashMap<>();

  /**
   * Constructs a new {@code WholeProgramInferenceJavaParser} that has not yet inferred any
   * annotations.
   *
   * @param atypeFactory the associated type factory
   */
  public WholeProgramInferenceJavaParserStorage(AnnotatedTypeFactory atypeFactory) {
    this.atypeFactory = atypeFactory;
  }

  @Override
  public String getFileForElement(Element elt) {
    return addClassesForElement(elt);
  }

  @Override
  public void setFileModified(String path) {
    modifiedFiles.add(path);
  }

  ///
  /// Reading stored annotations
  ///

  @Override
  public boolean hasStorageLocationForMethod(ExecutableElement methodElt) {
    return getMethodAnnos(methodElt) != null;
  }

  /**
   * Get the annotations for a method or constructor.
   *
   * @param methodElt the method or constructor
   * @return the annotations for a method or constructor
   */
  private CallableDeclarationAnnos getMethodAnnos(ExecutableElement methodElt) {
    String className = ElementUtils.getEnclosingClassName(methodElt);
    // Read in classes for the element.
    getFileForElement(methodElt);
    ClassOrInterfaceAnnos classAnnos = classToAnnos.get(className);
    CallableDeclarationAnnos methodAnnos =
        classAnnos.callableDeclarations.get(JVMNames.getJVMMethodSignature(methodElt));
    return methodAnnos;
  }

  @Override
  public AnnotatedTypeMirror getParameterAnnotations(
      ExecutableElement methodElt,
      int i,
      AnnotatedTypeMirror paramATM,
      VariableElement ve,
      AnnotatedTypeFactory atypeFactory) {
    CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElt);
    return methodAnnos.getParameterTypeInitialized(paramATM, i, atypeFactory);
  }

  @Override
  public AnnotatedTypeMirror getReceiverAnnotations(
      ExecutableElement methodElt,
      AnnotatedTypeMirror paramATM,
      AnnotatedTypeFactory atypeFactory) {
    CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElt);
    return methodAnnos.getReceiverType(paramATM, atypeFactory);
  }

  @Override
  public AnnotatedTypeMirror getReturnAnnotations(
      ExecutableElement methodElt, AnnotatedTypeMirror atm, AnnotatedTypeFactory atypeFactory) {
    CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElt);
    return methodAnnos.getReturnType(atm, atypeFactory);
  }

  @Override
  public AnnotatedTypeMirror getFieldAnnotations(
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
    // If it's an enum constant it won't appear as a field
    // and it won't have extra annotations, so just return the basic type:
    if (classAnnos.enumConstants.contains(fieldName)) {
      return lhsATM;
    } else {
      return classAnnos.fields.get(fieldName).getType(lhsATM, atypeFactory);
    }
  }

  @Override
  public AnnotatedTypeMirror getPreOrPostconditions(
      Analysis.BeforeOrAfter preOrPost,
      ExecutableElement methodElement,
      String expression,
      AnnotatedTypeMirror declaredType,
      AnnotatedTypeFactory atypeFactory) {
    switch (preOrPost) {
      case BEFORE:
        return getPreconditionsForExpression(methodElement, expression, declaredType, atypeFactory);
      case AFTER:
        return getPostconditionsForExpression(
            methodElement, expression, declaredType, atypeFactory);
      default:
        throw new BugInCF("Unexpected " + preOrPost);
    }
  }

  /**
   * Returns the precondition annotations for the given expression.
   *
   * @param methodElement the method
   * @param expression the expression
   * @param declaredType the declared type of the expression
   * @param atypeFactory the type factory
   * @return the precondition annotations for a field
   */
  private AnnotatedTypeMirror getPreconditionsForExpression(
      ExecutableElement methodElement,
      String expression,
      AnnotatedTypeMirror declaredType,
      AnnotatedTypeFactory atypeFactory) {
    CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElement);
    return methodAnnos.getPreconditionsForExpression(expression, declaredType, atypeFactory);
  }

  /**
   * Returns the postcondition annotations for an expression.
   *
   * @param methodElement the method
   * @param expression the expression
   * @param declaredType the declared type of the expression
   * @param atypeFactory the type factory
   * @return the postcondition annotations for a field
   */
  private AnnotatedTypeMirror getPostconditionsForExpression(
      ExecutableElement methodElement,
      String expression,
      AnnotatedTypeMirror declaredType,
      AnnotatedTypeFactory atypeFactory) {
    CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElement);
    return methodAnnos.getPostconditionsForExpression(expression, declaredType, atypeFactory);
  }

  @Override
  public boolean addMethodDeclarationAnnotation(
      ExecutableElement methodElt, AnnotationMirror anno) {

    CallableDeclarationAnnos methodAnnos = getMethodAnnos(methodElt);
    boolean isNewAnnotation = methodAnnos.addDeclarationAnnotation(anno);
    if (isNewAnnotation) {
      modifiedFiles.add(getFileForElement(methodElt));
    }
    return isNewAnnotation;
  }

  @Override
  public AnnotatedTypeMirror atmFromStorageLocation(
      TypeMirror typeMirror, AnnotatedTypeMirror storageLocation) {
    return storageLocation;
  }

  @Override
  public void updateStorageLocationFromAtm(
      AnnotatedTypeMirror newATM,
      AnnotatedTypeMirror curATM,
      AnnotatedTypeMirror typeToUpdate,
      TypeUseLocation defLoc,
      boolean ignoreIfAnnotated) {
    // Clears only the annotations that are supported by atypeFactory.
    // The others stay intact.
    Set<AnnotationMirror> annosToRemove = AnnotationUtils.createAnnotationSet();
    for (AnnotationMirror anno : typeToUpdate.getAnnotations()) {
      if (atypeFactory.isSupportedQualifier(anno)) {
        annosToRemove.add(anno);
      }
    }

    // This method may be called consecutive times to modify the same AnnotatedTypeMirror.
    // Each time it is called, the AnnotatedTypeMirror has a better type
    // estimate for the modified AnnotatedTypeMirror. Therefore, it is not a problem to remove
    // all annotations before inserting the new annotations.
    typeToUpdate.removeAnnotations(annosToRemove);

    // Only update the AnnotatedTypeMirror if there are no explicit annotations
    if (curATM.getExplicitAnnotations().isEmpty() || !ignoreIfAnnotated) {
      for (AnnotationMirror am : newATM.getAnnotations()) {
        typeToUpdate.addAnnotation(am);
      }
    } else if (curATM.getKind() == TypeKind.TYPEVAR) {
      // getExplicitAnnotations will be non-empty for type vars whose bounds are explicitly
      // annotated.  So instead, only insert the annotation if there is not primary annotation
      // of the same hierarchy.
      for (AnnotationMirror am : newATM.getAnnotations()) {
        if (curATM.getAnnotationInHierarchy(am) != null) {
          // Don't insert if the type is already has a primary annotation
          // in the same hierarchy.
          break;
        }

        typeToUpdate.addAnnotation(am);
      }
    }

    if (newATM.getKind() == TypeKind.ARRAY) {
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

  ///
  /// Reading in files
  ///

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
  }

  /**
   * The first two arugments are a javac tree and a JavaParser node representing the same class.
   * This method creates wrappers around all the classes, fields, and methods in that class, and
   * stores those wrappers in {@code sourceAnnos}.
   *
   * @param javacClass javac tree for class
   * @param javaParserClass JavaParser node corresponding to the same class as {@code javacClass}
   * @param sourceAnnos compilation unit wrapper to add new wrappers to
   */
  private void createWrappersForClass(
      ClassTree javacClass, TypeDeclaration<?> javaParserClass, CompilationUnitAnnos sourceAnnos) {
    JointJavacJavaParserVisitor visitor =
        new DefaultJointVisitor() {
          @Override
          public void processClass(
              ClassTree javacTree, ClassOrInterfaceDeclaration javaParserNode) {
            addClass(javacTree);
          }

          @Override
          public void processClass(ClassTree javacTree, EnumDeclaration javaParserNode) {
            addClass(javacTree);
          }

          @Override
          public void processClass(ClassTree javacTree, RecordDeclaration javaParserNode) {
            addClass(javacTree);
          }

          @Override
          public void processNewClass(NewClassTree javacTree, ObjectCreationExpr javaParserNode) {
            if (javacTree.getClassBody() != null) {
              addClass(javacTree.getClassBody());
            }
          }

          /**
           * Creates a wrapper around the class for {@code tree} and stores it in {@code
           * sourceAnnos}.
           *
           * @param tree tree to add
           */
          private void addClass(ClassTree tree) {
            TypeElement classElt = TreeUtils.elementFromDeclaration(tree);
            String className = ElementUtils.getBinaryName(classElt);
            ClassOrInterfaceAnnos typeWrapper = new ClassOrInterfaceAnnos();
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
           * @param javaParserNode JavaParser node for the same class as {@code javacTree}
           */
          private void addCallableDeclaration(
              MethodTree javacTree, CallableDeclaration<?> javaParserNode) {
            ExecutableElement elt = TreeUtils.elementFromDeclaration(javacTree);
            String className = ElementUtils.getEnclosingClassName(elt);
            ClassOrInterfaceAnnos enclosingClass = classToAnnos.get(className);
            String executableSignature = JVMNames.getJVMMethodSignature(javacTree);
            if (!enclosingClass.callableDeclarations.containsKey(executableSignature)) {
              enclosingClass.callableDeclarations.put(
                  executableSignature, new CallableDeclarationAnnos(javaParserNode));
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
          }

          @Override
          public void processVariable(VariableTree javacTree, VariableDeclarator javaParserNode) {
            // This seems to occur when javacTree is a local variable in the second
            // class located in a source file. If this check returns false, then the
            // below call to TreeUtils.elementFromDeclaration causes a crash.
            if (TreeUtils.elementFromTree(javacTree) == null) {
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
  public void prepareCompilationUnitForWriting(CompilationUnitAnnos compilationUnitAnnos) {
    for (ClassOrInterfaceAnnos type : compilationUnitAnnos.types) {
      prepareClassForWriting(type);
    }
  }

  /**
   * Side-effects the class annotations to make any desired changes before writing to a file.
   *
   * @param classAnnos the class annotations to modify
   */
  public void prepareClassForWriting(ClassOrInterfaceAnnos classAnnos) {
    for (Map.Entry<String, CallableDeclarationAnnos> methodEntry :
        classAnnos.callableDeclarations.entrySet()) {
      prepareMethodForWriting(methodEntry.getValue());
    }
  }

  /**
   * Side-effects the method or constructor annotations to make any desired changes before writing
   * to a file.
   *
   * @param methodAnnos the method or constructor annotations to modify
   */
  public void prepareMethodForWriting(CallableDeclarationAnnos methodAnnos) {
    atypeFactory.prepareMethodForWriting(methodAnnos);
  }

  @Override
  public void writeResultsToFile(OutputFormat outputFormat, BaseTypeChecker checker) {
    if (outputFormat != OutputFormat.AJAVA) {
      throw new BugInCF("WholeProgramInferenceJavaParser used with format " + outputFormat);
    }

    File outputDir = new File(AJAVA_FILES_PATH);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }

    for (String path : modifiedFiles) {
      CompilationUnitAnnos root = sourceToAnnos.get(path);
      prepareCompilationUnitForWriting(root);
      root.transferAnnotations(checker);
      String packageDir = AJAVA_FILES_PATH;
      if (root.compilationUnit.getPackageDeclaration().isPresent()) {
        packageDir +=
            File.separator
                + root.compilationUnit
                    .getPackageDeclaration()
                    .get()
                    .getNameAsString()
                    .replaceAll("\\.", File.separator);
      }

      File packageDirFile = new File(packageDir);
      if (!packageDirFile.exists()) {
        packageDirFile.mkdirs();
      }

      String name = new File(path).getName();
      if (name.endsWith(".java")) {
        name = name.substring(0, name.length() - ".java".length());
      }

      name += "-" + checker.getClass().getCanonicalName() + ".ajava";
      String outputPath = packageDir + File.separator + name;
      try {
        FileWriter writer = new FileWriter(outputPath);

        // JavaParser can output using lexical preserving printing, which writes the file such that
        // its formatting is close to the original source file it was parsed from as
        // possible. Currently, this feature is very buggy and crashes when adding annotations in
        // certain locations. This implementation could be used instead if it's fixed in JavaParser.
        // LexicalPreservingPrinter.print(root.declaration, writer);

        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        writer.write(prettyPrinter.print(root.compilationUnit));
        writer.close();
      } catch (IOException e) {
        throw new BugInCF("Error while writing ajava file " + outputPath, e);
      }
    }

    modifiedFiles.clear();
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

  ///
  /// Storing annotations
  ///

  /**
   * Stores the JavaParser node for a compilation unit and the list of wrappers for the classes and
   * interfaces in that compilation unit.
   */
  private static class CompilationUnitAnnos {
    /** Compilation unit being wrapped. */
    public CompilationUnit compilationUnit;
    /** Wrappers for classes and interfaces in {@code compilationUnit}. */
    public List<ClassOrInterfaceAnnos> types;

    /**
     * Constructs a wrapper around the given compilation unit.
     *
     * @param compilationUnit compilation unit to wrap
     */
    public CompilationUnitAnnos(CompilationUnit compilationUnit) {
      this.compilationUnit = compilationUnit;
      types = new ArrayList<>();
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
  }

  /**
   * Stores wrappers for the locations where annotations may be inferred in a class or interface.
   */
  private static class ClassOrInterfaceAnnos {
    /**
     * Mapping from JVM method signatures to the wrapper containing the corresponding executable.
     */
    public Map<String, CallableDeclarationAnnos> callableDeclarations = new HashMap<>();
    /** Mapping from field names to wrappers for those fields. */
    public Map<String, FieldAnnos> fields = new HashMap<>(2);
    /** Collection of declared enum constants (empty if not an enum). */
    public Set<String> enumConstants = new HashSet<>(2);

    /**
     * Transfers all annotations inferred by whole program inference for the methods and fields in
     * the wrapper class or interface to their corresponding JavaParser locations.
     */
    public void transferAnnotations() {
      for (CallableDeclarationAnnos callableAnnos : callableDeclarations.values()) {
        callableAnnos.transferAnnotations();
      }

      for (FieldAnnos field : fields.values()) {
        field.transferAnnotations();
      }
    }

    @Override
    public String toString() {
      return "ClassOrInterfaceAnnos [callableDeclarations="
          + callableDeclarations
          + ", fields="
          + fields
          + "]";
    }
  }

  /**
   * Stores the JavaParser node for a method or constructor and the annotations that have been
   * inferred about its parameters and return type.
   */
  public class CallableDeclarationAnnos {
    /** Wrapped method or constructor declaration. */
    public CallableDeclaration<?> declaration;
    /** Path to file containing the declaration. */
    public String file;
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
    /** Annotations on the callable declaration. */
    private @MonotonicNonNull Set<AnnotationMirror> declarationAnnotations = null;

    /**
     * Mapping from expression strings to pairs of (inferred precondition, declared type). The keys
     * are strings representing JavaExpressions, using the same format as a user would in an {@link
     * org.checkerframework.framework.qual.RequiresQualifier} annotation.
     */
    private @MonotonicNonNull Map<String, Pair<AnnotatedTypeMirror, AnnotatedTypeMirror>>
        preconditions = null;
    /**
     * Mapping from expression strings to pairs of (inferred postcondition, declared type). The
     * okeys are strings representing JavaExpressions, using the same format as a user would in an
     * {@link org.checkerframework.framework.qual.EnsuresQualifier} annotation.
     */
    private @MonotonicNonNull Map<String, Pair<AnnotatedTypeMirror, AnnotatedTypeMirror>>
        postconditions = null;

    /**
     * Creates a wrapper for the given method or constructor declaration.
     *
     * @param declaration method or constructor declaration to wrap
     */
    public CallableDeclarationAnnos(CallableDeclaration<?> declaration) {
      this.declaration = declaration;
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
     * @param index index of the parameter to return the inferred annotations of
     * @return an {@code AnnotatedTypeMirror} containing all annotations inferred for the parameter
     *     at the given index
     */
    public AnnotatedTypeMirror getParameterTypeInitialized(
        AnnotatedTypeMirror type, int index, AnnotatedTypeFactory atf) {
      if (parameterTypes == null) {
        parameterTypes =
            new ArrayList<>(Collections.nCopies(declaration.getParameters().size(), null));
      }

      if (parameterTypes.get(index) == null) {
        parameterTypes.set(
            index, AnnotatedTypeMirror.createType(type.getUnderlyingType(), atf, false));
      }

      return parameterTypes.get(index);
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
     * Returns the inferred declaration annotations on this executable, or null if there are no
     * annotations.
     *
     * @return the declaration annotations for this callable declaration
     */
    public Set<AnnotationMirror> getDeclarationAnnotations() {
      if (declarationAnnotations == null) {
        return Collections.emptySet();
      }

      return Collections.unmodifiableSet(declarationAnnotations);
    }

    /**
     * Adds a declaration annotation to this callable declaration and returns whether it was a new
     * annotation.
     *
     * @param annotation declaration annotation to add
     * @return true if {@code annotation} wasn't previously stored for this callable declaration
     */
    public boolean addDeclarationAnnotation(AnnotationMirror annotation) {
      if (declarationAnnotations == null) {
        declarationAnnotations = new HashSet<>();
      }

      return declarationAnnotations.add(annotation);
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
     * Returns the inferred preconditions for this callable declaration.
     *
     * @return a mapping from expression string to pairs of (inferred precondition, declared type).
     *     The keys of this map use the same string formatting as the {@link
     *     org.checkerframework.framework.qual.RequiresQualifier} annotation, e.g. "#1" for the
     *     first parameter.
     */
    public Map<String, Pair<AnnotatedTypeMirror, AnnotatedTypeMirror>> getPreconditions() {
      if (preconditions == null) {
        return Collections.emptyMap();
      }

      return Collections.unmodifiableMap(preconditions);
    }

    /**
     * Returns the inferred postconditions for this callable declaration.
     *
     * @return a mapping from expression string to pairs of (inferred postcondition, declared type).
     *     The keys of this map use the same string formatting as the {@link
     *     org.checkerframework.framework.qual.EnsuresQualifier} annotation, e.g. "#1" for the first
     *     parameter.
     */
    public Map<String, Pair<AnnotatedTypeMirror, AnnotatedTypeMirror>> getPostconditions() {
      if (postconditions == null) {
        return Collections.emptyMap();
      }

      return Collections.unmodifiableMap(postconditions);
    }

    /**
     * Returns an AnnotatedTypeMirror containing the preconditions for the given expression.
     *
     * @param expression a string representing a Java expression, in the same format as the argument
     *     to a {@link org.checkerframework.framework.qual.RequiresQualifier} annotation
     * @param declaredType the declared type of {@code expression}
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used
     * @return an {@code AnnotatedTypeMirror} containing the annotations for the inferred
     *     preconditions for the given expression
     */
    public AnnotatedTypeMirror getPreconditionsForExpression(
        String expression, AnnotatedTypeMirror declaredType, AnnotatedTypeFactory atf) {
      if (preconditions == null) {
        preconditions = new HashMap<>(1);
      }

      if (!preconditions.containsKey(expression)) {
        AnnotatedTypeMirror preconditionsType =
            AnnotatedTypeMirror.createType(declaredType.getUnderlyingType(), atf, false);
        preconditions.put(expression, Pair.of(preconditionsType, declaredType));
      }

      return preconditions.get(expression).first;
    }

    /**
     * Returns an AnnotatedTypeMirror containing the postconditions for the given expression.
     *
     * @param expression a string representing a Java expression, in the same format as the argument
     *     to a {@link org.checkerframework.framework.qual.EnsuresQualifier} annotation
     * @param declaredType the declared type of {@code expression}
     * @param atf the annotated type factory of a given type system, whose type hierarchy will be
     *     used
     * @return an {@code AnnotatedTypeMirror} containing the annotations for the inferred
     *     postconditions for the given expression
     */
    public AnnotatedTypeMirror getPostconditionsForExpression(
        String expression, AnnotatedTypeMirror declaredType, AnnotatedTypeFactory atf) {
      if (postconditions == null) {
        postconditions = new HashMap<>(1);
      }

      if (!postconditions.containsKey(expression)) {
        AnnotatedTypeMirror postconditionsType =
            AnnotatedTypeMirror.createType(declaredType.getUnderlyingType(), atf, false);
        postconditions.put(expression, Pair.of(postconditionsType, declaredType));
      }

      return postconditions.get(expression).first;
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

      if (declarationAnnotations != null) {
        for (AnnotationMirror annotation : declarationAnnotations) {
          declaration.addAnnotation(
              AnnotationMirrorToAnnotationExprConversion.annotationMirrorToAnnotationExpr(
                  annotation));
        }
      }

      if (returnType != null) {
        // If a return type exists, then the declaration must be a method, not a constructor.
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
        Parameter param = declaration.getParameter(i);
        Type javaParserType = param.getType();
        if (param.isVarArgs()) {
          NodeList<AnnotationExpr> varArgsAnnoExprs =
              AnnotationMirrorToAnnotationExprConversion.annotationMirrorSetToAnnotationExprList(
                  inferredType.getAnnotations());
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
      return "CallableDeclarationAnnos [declaration="
          + declaration
          + ", file="
          + file
          + ", parameterTypes="
          + parameterTypes
          + ", receiverType="
          + receiverType
          + ", returnType="
          + returnType
          + "]";
    }
  }

  /** Stores the JavaParser node for a field and the annotations that have been inferred for it. */
  private static class FieldAnnos {
    /** Wrapped field declaration. */
    public VariableDeclarator declaration;
    /** Inferred type for field, initialized the first time it's accessed. */
    private @MonotonicNonNull AnnotatedTypeMirror type = null;

    /**
     * Creates a wrapper for the given field declaration.
     *
     * @param declaration field declaration to wrap
     */
    public FieldAnnos(VariableDeclarator declaration) {
      this.declaration = declaration;
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
     * Transfers all annotations inferred by whole program inference on this field to the JavaParser
     * nodes for that field.
     */
    public void transferAnnotations() {
      if (type == null) {
        return;
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
}
