package org.checkerframework.afu.annotator.specification;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.sun.source.tree.Tree;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.checkerframework.afu.annotator.Main;
import org.checkerframework.afu.annotator.find.AnnotationInsertion;
import org.checkerframework.afu.annotator.find.CastInsertion;
import org.checkerframework.afu.annotator.find.CloseParenthesisInsertion;
import org.checkerframework.afu.annotator.find.ConstructorInsertion;
import org.checkerframework.afu.annotator.find.Criteria;
import org.checkerframework.afu.annotator.find.GenericArrayLocationCriterion;
import org.checkerframework.afu.annotator.find.Insertion;
import org.checkerframework.afu.annotator.find.IntersectionTypeLocationCriterion;
import org.checkerframework.afu.annotator.find.NewInsertion;
import org.checkerframework.afu.annotator.find.ReceiverInsertion;
import org.checkerframework.afu.annotator.scanner.MethodOffsetClassVisitor;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.el.ABlock;
import org.checkerframework.afu.scenelib.el.AClass;
import org.checkerframework.afu.scenelib.el.AElement;
import org.checkerframework.afu.scenelib.el.AExpression;
import org.checkerframework.afu.scenelib.el.AField;
import org.checkerframework.afu.scenelib.el.AMethod;
import org.checkerframework.afu.scenelib.el.AScene;
import org.checkerframework.afu.scenelib.el.ATypeElement;
import org.checkerframework.afu.scenelib.el.ATypeElementWithType;
import org.checkerframework.afu.scenelib.el.AnnotationDef;
import org.checkerframework.afu.scenelib.el.BoundLocation;
import org.checkerframework.afu.scenelib.el.LocalLocation;
import org.checkerframework.afu.scenelib.el.RelativeLocation;
import org.checkerframework.afu.scenelib.el.TypeIndexLocation;
import org.checkerframework.afu.scenelib.el.TypePathEntry;
import org.checkerframework.afu.scenelib.field.AnnotationFieldType;
import org.checkerframework.afu.scenelib.io.ASTPath;
import org.checkerframework.afu.scenelib.io.IndexFileParser;
import org.checkerframework.afu.scenelib.type.DeclaredType;
import org.checkerframework.afu.scenelib.type.Type;
import org.checkerframework.afu.scenelib.util.coll.VivifyingMap;
import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.plumelib.reflection.ReflectionPlume;
import org.plumelib.util.FileIOException;
import org.plumelib.util.IPair;

/** Represents the annotations in an index file (a .jaif file). */
public class IndexFileSpecification {
  /** Insertion sources. */
  private final Multimap<Insertion, Annotation> insertionSources =
      LinkedHashMultimap.<Insertion, Annotation>create();

  /** The state of this object. */
  private final List<Insertion> insertions = new ArrayList<>();

  /** An insertion for the default constructor, or null. Is a member of insertions if non-null. */
  private ConstructorInsertion constructorInsertion = null;

  private final AScene scene;
  private final String indexFileName;

  // If set, do not attempt to read class files with Asm.
  // Mostly for debugging and workarounds.
  public static boolean noAsm = false;

  private static boolean debug = false;

  public IndexFileSpecification(String indexFileName) {
    this.indexFileName = indexFileName;
    scene = new AScene();
  }

  public List<Insertion> parse() throws FileIOException {
    try {
      Map<String, AnnotationDef> annotationDefs = IndexFileParser.parseFile(indexFileName, scene);
      Set<String> defKeys = annotationDefs.keySet();
      Set<String> ambiguous = new LinkedHashSet<>();
      // If a qualified name's unqualified counterpart maps to null in
      // defKeys, it means that the unqualified name is ambiguous and
      // thus should always be qualified.
      for (String key : defKeys) {
        int ix = Math.max(key.lastIndexOf('.'), key.lastIndexOf('$'));
        if (ix >= 0) {
          String name = key.substring(ix + 1);
          // containsKey() would give wrong result here
          if (annotationDefs.get(name) == null) {
            ambiguous.add(name);
          }
        }
      }
      Insertion.setAlwaysQualify(ambiguous);
    } catch (FileIOException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Exception while parsing index file", e);
    }

    if (debug) {
      System.out.printf("Scene parsed from %s:%n", indexFileName);
      System.out.println(scene.unparse());
    }

    parseScene();
    //    debug("---------------------------------------------------------");
    return this.insertions;
  }

  /**
   * Returns the map of annotation imports.
   *
   * @return a map of annotation imports
   */
  public Map<String, Set<String>> annotationImports() {
    return scene.imports;
  }

  public Multimap<Insertion, Annotation> insertionSources() {
    return insertionSources;
  }

  private void addInsertionSource(Insertion ins, Annotation anno) {
    insertionSources.put(ins, anno);
  }

  private static void debug(String s) {
    if (debug) {
      System.out.println(s);
    }
  }

  /**
   * Prints to standard out, if the {@code debug} flag is set.
   *
   * @param format a format string
   * @param args the format string arguments
   */
  @FormatMethod
  private static void debug(String format, Object... args) {
    if (debug) {
      System.out.printf(format, args);
    }
  }

  /**
   * Returns the current scene.
   *
   * @return the current scene
   */
  public AScene getScene() {
    return scene;
  }

  /** Fill in this.insertions with insertion pairs. */
  private void parseScene() {
    debug("parseScene()");

    // Empty criterion to work from.
    CriterionList clist = new CriterionList();

    VivifyingMap<String, AElement> packages = scene.packages;
    for (Map.Entry<String, AElement> entry : packages.entrySet()) {
      parsePackage(clist, entry.getKey(), entry.getValue());
    }

    @SuppressWarnings("signature:assignment") // scene-lib is not fully annotated
    VivifyingMap<@ClassGetName String, AClass> classes = scene.classes;
    for (Map.Entry<@ClassGetName String, AClass> entry : classes.entrySet()) {
      String key = entry.getKey();
      AClass clazz = entry.getValue();
      if (key.endsWith(".package-info")) {
        // strip off suffix to get package name
        parsePackage(clist, key.substring(0, key.length() - 13), clazz);
      } else {
        parseClass(clist, key, clazz);
      }
    }
  }

  // There is no .class file corresponding to the package-info.java file.
  private void parsePackage(CriterionList clist, String packageName, AElement element) {
    // There is no Tree.Kind.PACKAGE, only Tree.Kind.COMPILATION_UNIT.
    // CompilationUnitTree has getPackageName and getPackageAnnotations
    CriterionList packageClist = clist.add(Criteria.packageDecl(packageName));
    parseElement(packageClist, element);
  }

  /**
   * Fill in this.insertions with insertion pairs.
   *
   * @param clist where to store the Criteria
   * @param className fully qualified name of class to be parsed
   * @param clazz where to store the class information
   */
  private void parseClass(CriterionList clist, @ClassGetName String className, AClass clazz) {
    constructorInsertion = null; // 0 or 1 per class
    if (!noAsm) {
      //  load extra info using asm
      debug("parseClass(%s)", className);
      try {
        ClassReader classReader = new ClassReader(className);
        ClassWriter classWriter = new ClassWriter(classReader, 0);
        MethodOffsetClassVisitor cv =
            new MethodOffsetClassVisitor(Opcodes.ASM8, classReader, classWriter);
        classReader.accept(cv, 0);
        debug("Done reading %s.class", className);
      } catch (IOException e) {
        // If .class file not found, still proceed, in case
        // user only wants method signature annotations.
        // (TODO: It would be better to store which classes could not be
        // found, then issue a warning only if an attempt is made to use
        // the (missing) information.  See
        // https://github.com/typetools/annotation-tools/issues/34 .)
        System.out.println(
            "Warning: IndexFileSpecification did not find classfile for: " + className);
        System.out.println("The classpath is:");
        System.out.println(ReflectionPlume.classpathToString());
        // org.plumelib.util.SystemPlume.sleep(100);
        // throw new RuntimeException("IndexFileSpecification.parseClass", e);
      } catch (RuntimeException e) {
        System.err.println("IndexFileSpecification had a problem reading class: " + className);
        throw e;
      } catch (Error e) {
        System.err.println("IndexFileSpecification had a problem reading class: " + className);
        throw e;
      }
    }

    CriterionList clistSansClass = clist;

    clist = clist.add(Criteria.inClass(className, true));
    CriterionList classClist = clistSansClass.add(Criteria.is(Tree.Kind.CLASS, className));
    parseElement(classClist, clazz);

    VivifyingMap<BoundLocation, ATypeElement> bounds = clazz.bounds;
    for (Entry<BoundLocation, ATypeElement> entry : bounds.entrySet()) {
      BoundLocation boundLoc = entry.getKey();
      ATypeElement bound = entry.getValue();
      CriterionList boundList = clist.add(Criteria.classBound(className, boundLoc));
      for (Entry<List<TypePathEntry>, ATypeElement> innerEntry : bound.innerTypes.entrySet()) {
        List<TypePathEntry> innerLoc = innerEntry.getKey();
        AElement ae = innerEntry.getValue();
        CriterionList innerBoundList =
            boundList.add(Criteria.atLocation(TypePathEntry.listToTypePath(innerLoc)));
        parseElement(innerBoundList, ae);
      }
      CriterionList outerClist = boundList.add(Criteria.atLocation());
      parseElement(outerClist, bound);
    }

    clist = clist.add(Criteria.inClass(className, /* exactMatch= */ false));

    VivifyingMap<TypeIndexLocation, ATypeElement> extimpl = clazz.extendsImplements;
    for (Entry<TypeIndexLocation, ATypeElement> entry : extimpl.entrySet()) {
      TypeIndexLocation eiLoc = entry.getKey();
      ATypeElement ei = entry.getValue();
      CriterionList eiList = clist.add(Criteria.atExtImplsLocation(className, eiLoc));

      for (Entry<List<TypePathEntry>, ATypeElement> innerEntry : ei.innerTypes.entrySet()) {
        List<TypePathEntry> innerLoc = innerEntry.getKey();
        AElement ae = innerEntry.getValue();
        CriterionList innerBoundList =
            eiList.add(Criteria.atLocation(TypePathEntry.listToTypePath(innerLoc)));
        parseElement(innerBoundList, ae);
      }
      parseElement(eiList, ei);
    }

    parseASTInsertions(clist, clazz.insertAnnotations, clazz.insertTypecasts);

    for (Map.Entry<String, AField> entry : clazz.fields.entrySet()) {
      //      clist = clist.add(Criteria.notInMethod()); // TODO: necessary? what is in class but
      // not in method?
      parseField(clist, entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, AMethod> entry : clazz.methods.entrySet()) {
      parseMethod(clist, className, entry.getKey(), entry.getValue());
    }
    for (Map.Entry<Integer, ABlock> entry : clazz.staticInits.entrySet()) {
      parseStaticInit(clist, className, entry.getKey(), entry.getValue());
    }
    for (Map.Entry<Integer, ABlock> entry : clazz.instanceInits.entrySet()) {
      parseInstanceInit(clist, className, entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, AExpression> entry : clazz.fieldInits.entrySet()) {
      parseFieldInit(clist, className, entry.getKey(), entry.getValue());
    }

    debug("parseClass(%s):  done%n", className);
  }

  /** Fill in this.insertions with insertion pairs. */
  private void parseField(CriterionList clist, String fieldName, AField field) {
    // parse declaration annotations
    parseElement(clist.add(Criteria.field(fieldName, true)), field);

    // parse type annotations
    clist = clist.add(Criteria.field(fieldName, false));
    parseInnerAndOuterElements(clist, field.type);
    parseASTInsertions(clist, field.insertAnnotations, field.insertTypecasts);
  }

  private void parseStaticInit(CriterionList clist, String className, int blockID, ABlock block) {
    clist = clist.add(Criteria.inStaticInit(blockID));
    // the method name argument is not used for static initializers, which are only used
    // in source specifications. Same for instance and field initializers.
    // the empty () are there to prevent the whole string to be removed in later parsing.
    parseBlock(clist, className, "static init number " + blockID + "()", block);
  }

  private void parseInstanceInit(CriterionList clist, String className, int blockID, ABlock block) {
    clist = clist.add(Criteria.inInstanceInit(blockID));
    parseBlock(clist, className, "instance init number " + blockID + "()", block);
  }

  // keep the descriptive strings for field initializers and static inits consistent
  // with text used in NewCriterion.

  private void parseFieldInit(
      CriterionList clist, String className, String fieldName, AExpression exp) {
    clist = clist.add(Criteria.inFieldInit(fieldName));
    parseExpression(clist, className, "init for field " + fieldName + "()", exp);
  }

  /**
   * Fills in this.insertions with insertion pairs, and returns the ones that are
   * AnnotationInsertions.
   *
   * @param clist the criteria specifying the location of the insertions
   * @param element holds the annotations to be inserted
   * @return a list of the {@link AnnotationInsertion}s that are created
   */
  private List<Insertion> parseElement(CriterionList clist, AElement element) {
    return parseElement(clist, element, new ArrayList<Insertion>(), false);
  }

  /**
   * Fills in this.insertions with insertion pairs, and returns the ones that are
   * AnnotationInsertions.
   *
   * @param clist the criteria specifying the location of the insertions
   * @param element holds the annotations to be inserted
   * @param isCastInsertion {@code true} if this for a cast insertion, {@code false} otherwise
   * @return a list of the {@link AnnotationInsertion}s that are created
   */
  private List<Insertion> parseElement(
      CriterionList clist, AElement element, boolean isCastInsertion) {
    return parseElement(clist, element, new ArrayList<Insertion>(), isCastInsertion);
  }

  /**
   * Fills in this.insertions with insertion pairs, and returns the ones that are
   * AnnotationInsertions.
   *
   * @param clist the criteria specifying the location of the insertions
   * @param element holds the annotations to be inserted
   * @return a list of the {@link AnnotationInsertion}s that are created
   */
  private List<Insertion> parseElement(
      CriterionList clist, AElement element, List<Insertion> innerTypeInsertions) {
    return parseElement(clist, element, innerTypeInsertions, false);
  }

  /**
   * Fills in this.insertions with insertion pairs, and returns the ones that are
   * AnnotationInsertions.
   *
   * @param clist the criteria specifying the location of the insertions
   * @param element holds the annotations to be inserted
   * @param innerTypeInsertions the insertions on the inner type of this element. This is only used
   *     for receiver and "new" insertions. See {@link ReceiverInsertion} for more details.
   * @param isCastInsertion {@code true} if this for a cast insertion, {@code false} otherwise
   * @return a list of the {@link AnnotationInsertion}s that are created
   */
  private List<Insertion> parseElement(
      CriterionList clist,
      AElement element,
      List<Insertion> innerTypeInsertions,
      boolean isCastInsertion) {
    // if (debug) {
    //   String elementToString = element.toString();
    //   if (elementToString.length() > 23) {
    //     elementToString = elementToString.substring(0, 20) + "...";
    //   }
    //   debug(
    //       "entering parseElement(..., \"%s\", %s, %s)%n",
    //       elementToString, innerTypeInsertions, isCastInsertion);
    // }

    // Use at most one receiver and one cast insertion and add all of the
    // annotations to the one insertion.
    ReceiverInsertion receiver = null;
    NewInsertion newI = null;
    CastInsertion cast = null;
    CloseParenthesisInsertion closeParen = null;
    List<Insertion> annotationInsertions = new ArrayList<>();
    Set<IPair<String, Annotation>> elementAnnotations = getElementAnnotations(element);
    if (elementAnnotations.isEmpty()) {
      Criteria criteria = clist.criteria();
      if (element instanceof ATypeElementWithType) {
        // Still insert even if it's a cast insertion with no outer
        // annotations to just insert a cast, or insert a cast with
        // annotations on the compound types.
        IPair<CastInsertion, CloseParenthesisInsertion> pair =
            createCastInsertion(
                ((ATypeElementWithType) element).getType(), null, innerTypeInsertions, criteria);
        cast = pair.first;
        closeParen = pair.second;
      } else if (!innerTypeInsertions.isEmpty()) {
        if (isOnReceiver(criteria)) {
          receiver = new ReceiverInsertion(new DeclaredType(), criteria, innerTypeInsertions);
        } else if (isOnNew(criteria)) {
          newI = new NewInsertion(new DeclaredType(), criteria, innerTypeInsertions);
        }
      }
    }

    for (IPair<String, Annotation> p : elementAnnotations) {
      List<Insertion> elementInsertions = new ArrayList<>();
      String annotationString = p.first;
      Annotation annotation = p.second;
      Criteria criteria = clist.criteria();
      // If the annotation is only a type annotation, it will be inserted on the same line as the
      // following type.  If the annotation is also a declaration annotation, always insert it on
      // its own line.
      boolean isTypeAnnotationOnly =
          annotation.def.isOnlyTypeAnnotation() || criteria.isOnlyTypeAnnotationCriterion();

      if (noTypePath(criteria) && isOnReceiver(criteria)) {
        if (receiver == null) {
          DeclaredType type = new DeclaredType();
          type.addAnnotation(annotationString);
          receiver = new ReceiverInsertion(type, criteria, innerTypeInsertions);
          elementInsertions.add(receiver);
        } else {
          receiver.getType().addAnnotation(annotationString);
        }
        addInsertionSource(receiver, annotation);
      } else if (noTypePath(criteria) && isOnNew(criteria)) {
        if (newI == null) {
          DeclaredType type = new DeclaredType();
          type.addAnnotation(annotationString);
          newI = new NewInsertion(type, criteria, innerTypeInsertions);
          elementInsertions.add(newI);
        } else {
          newI.getType().addAnnotation(annotationString);
        }
        addInsertionSource(newI, annotation);
      } else if (element instanceof ATypeElementWithType) {
        if (cast == null) {
          IPair<CastInsertion, CloseParenthesisInsertion> insertions =
              createCastInsertion(
                  ((ATypeElementWithType) element).getType(),
                  annotationString,
                  innerTypeInsertions,
                  criteria);
          cast = insertions.first;
          closeParen = insertions.second;
          elementInsertions.add(cast);
          elementInsertions.add(closeParen);
          // no addInsertionSource, as closeParen is not explicit in scene
        } else {
          cast.getType().addAnnotation(annotationString);
        }
        addInsertionSource(cast, annotation);
      } else {
        RelativeLocation loc = criteria.getCastRelativeLocation();
        if (loc != null && loc.type_index > 0) {
          criteria.add(new IntersectionTypeLocationCriterion(loc));
        }
        Insertion ins =
            new AnnotationInsertion(annotationString, criteria, !isTypeAnnotationOnly, annotation);
        debug("parsed: %s%n", ins);
        if (!isCastInsertion) {
          // Annotations on compound types of a cast insertion will be
          // inserted directly on the cast insertion.
          // this.insertions.add(ins);
          elementInsertions.add(ins);
        }
        annotationInsertions.add(ins);
        addInsertionSource(ins, annotation);
      }
      // if (debug) {
      //   if (!(annotationInsertions.isEmpty() && elementInsertions.isEmpty())) {
      //     debug(
      //         "About to call this.insertions.addAll(elementInsertions):%n"
      //             + "  elementInsertions = %s%n"
      //             + "  annotationInsertions = %s%n",
      //         elementInsertions, annotationInsertions);
      //   }
      // }

      // If an insertion I is on the implicit default constructor, then the no-argument constructor
      // will itself be inserted into the source code, the insertion I will be added to it, and the
      // insertion I will be removed from the list.
      this.insertions.addAll(elementInsertions);

      if (debug) {
        if (!this.insertions.isEmpty()) {
          debug(
              "now this.insertions (size %d) = %s%n",
              insertions.size(), Insertion.collectionToString(insertions));
        }
      }

      // Handle elementInsertions.

      if (noTypePath(criteria) && isOnImplicitDefaultConstructor(criteria)) {

        if (constructorInsertion == null) {
          DeclaredType type = new DeclaredType(criteria.getClassName());
          constructorInsertion =
              new ConstructorInsertion(type, criteria, new ArrayList<Insertion>());
          this.insertions.add(constructorInsertion);
          // debug(
          //     "Added constructorInsertion to this.insertions, which is now %s%n",
          //     Insertion.collectionToString(this.insertions));
        } else {
          if (Main.temporaryDebug) {
            System.out.printf(
                "Ignoring criteria=%s because constructorInsertion=%s%n",
                criteria, constructorInsertion);
          }
        }
        // no addInsertionSource, as constructorInsertion is not explicit in scene
        for (Insertion i : elementInsertions) {
          if (i.getKind() == Insertion.Kind.RECEIVER) {
            constructorInsertion.addReceiverInsertion((ReceiverInsertion) i);
            this.insertions.remove(i);
          } else if (criteria.isOnReturnType()) {
            ((DeclaredType) constructorInsertion.getType()).addAnnotation(annotationString);
          } else if (!isTypeAnnotationOnly) {
            constructorInsertion.addDeclarationInsertion(i);
            this.insertions.remove(i);
          } else {
            annotationInsertions.add(i);
            // debug("Added to annotationInsertions: %s%n", i);
          }
        }
      }
      elementInsertions.clear();
    }
    if (receiver != null) {
      this.insertions.add(receiver);
    }
    if (newI != null) {
      this.insertions.add(newI);
    }
    if (cast != null) {
      this.insertions.add(cast);
      this.insertions.add(closeParen);
    }
    if (constructorInsertion != null) {
      constructorInsertion.setInserted(false);
    }

    // debug("Final this.insertions: %s%n", Insertion.collectionToString(this.insertions));
    // debug("parseElement(...) => %s%n", Insertion.collectionToString(annotationInsertions));

    return annotationInsertions;
  }

  private boolean noTypePath(Criteria criteria) {
    GenericArrayLocationCriterion galc = criteria.getGenericArrayLocation();
    return galc == null || galc.getLocation().isEmpty();
  }

  public static boolean isOnReceiver(Criteria criteria) {
    ASTPath astPath = criteria.getASTPath();
    if (astPath == null) {
      return criteria.isOnReceiver();
    }
    if (astPath.isEmpty()) {
      return false;
    }
    ASTPath.ASTEntry entry = astPath.getLast();
    return entry.childSelectorIs(ASTPath.PARAMETER) && entry.getArgument() < 0;
  }

  public static boolean isOnNew(Criteria criteria) {
    ASTPath astPath = criteria.getASTPath();
    if (astPath == null || astPath.isEmpty()) {
      return criteria.isOnNew();
    }
    ASTPath.ASTEntry entry = astPath.getLast();
    Tree.Kind kind = entry.getTreeKind();
    return (kind == Tree.Kind.NEW_ARRAY
            && entry.childSelectorIs(ASTPath.TYPE)
            && entry.getArgument() == 0)
        || (kind == Tree.Kind.NEW_CLASS && entry.childSelectorIs(ASTPath.IDENTIFIER));
  }

  /**
   * Returns true if the Criteria is on an implicit default constructor. Such a constructor was not
   * defined in source code. It is not considered synthetic, however; the isSynthetic() method
   * returns false.
   *
   * @param criteria the Criteria
   * @return true if the Criteria is on a synthethic constructor
   */
  private static boolean isOnImplicitDefaultConstructor(Criteria criteria) {
    if (!criteria.isOnMethod("<init>()V")) {
      return false;
    }
    String className = criteria.getClassName();
    Boolean classHasExplicitConstructor = Main.hasExplicitConstructor.get(className);
    return Boolean.FALSE.equals(classHasExplicitConstructor);
  }

  /**
   * Creates the {@link CastInsertion} and {@link CloseParenthesisInsertion} for a cast insertion.
   *
   * @param type the cast type to insert
   * @param annotationString the annotation on the outermost type, or {@code null} if none. With no
   *     outermost annotation this cast insertion will either be a cast without any annotations or a
   *     cast with annotations only on the compound types.
   * @param innerTypeInsertions the annotations on the inner types
   * @param criteria the criteria for the location of this insertion
   * @return the {@link CastInsertion} and {@link CloseParenthesisInsertion}
   */
  private IPair<CastInsertion, CloseParenthesisInsertion> createCastInsertion(
      Type type, String annotationString, List<Insertion> innerTypeInsertions, Criteria criteria) {
    if (annotationString != null) {
      type.addAnnotation(annotationString);
    }
    Insertion.decorateType(innerTypeInsertions, type, criteria.getASTPath());
    CastInsertion cast = new CastInsertion(criteria, type);
    CloseParenthesisInsertion closeParen =
        new CloseParenthesisInsertion(criteria, cast.isSeparateLine());
    return IPair.of(cast, closeParen);
  }

  /**
   * Fill in this.insertions with insertion pairs for the outer and inner types.
   *
   * @param clist the criteria specifying the location of the insertions
   * @param typeElement holds the annotations to be inserted
   */
  private void parseInnerAndOuterElements(CriterionList clist, ATypeElement typeElement) {
    parseInnerAndOuterElements(clist, typeElement, false);
  }

  /**
   * Fill in this.insertions with insertion pairs for the outer and inner types.
   *
   * @param clist the criteria specifying the location of the insertions
   * @param typeElement holds the annotations to be inserted
   * @param isCastInsertion {@code true} if this for a cast insertion, {@code false} otherwise
   */
  private void parseInnerAndOuterElements(
      CriterionList clist, ATypeElement typeElement, boolean isCastInsertion) {
    List<Insertion> innerInsertions = new ArrayList<>();
    for (Entry<List<TypePathEntry>, ATypeElement> innerEntry : typeElement.innerTypes.entrySet()) {
      List<TypePathEntry> innerLoc = innerEntry.getKey();
      AElement innerElement = innerEntry.getValue();
      CriterionList innerClist =
          clist.add(Criteria.atLocation(TypePathEntry.listToTypePath(innerLoc)));
      innerInsertions.addAll(parseElement(innerClist, innerElement, isCastInsertion));
    }
    // if (!innerInsertions.isEmpty()) {
    //   debug("parseInnerAndOuterElements: innerInsertions = %s%n", innerInsertions);
    // }
    CriterionList outerClist = clist;
    if (!isCastInsertion) {
      // Cast insertion is never on an existing type.
      outerClist = clist.add(Criteria.atLocation());
    }
    parseElement(outerClist, typeElement, innerInsertions);
  }

  // Returns a string representation of the annotations at the element.
  private Set<IPair<String, Annotation>> getElementAnnotations(AElement element) {
    Set<IPair<String, Annotation>> result =
        new LinkedHashSet<IPair<String, Annotation>>(element.tlAnnotationsHere.size());
    for (Annotation a : element.tlAnnotationsHere) {
      StringBuilder sb = new StringBuilder();
      sb.append("@");
      sb.append(a.def.name);

      if (a.fieldValues.isEmpty()) {
        // nothing to do
      } else {
        sb.append("(");
        if (a.fieldValues.size() == 1 && a.fieldValues.containsKey("value")) {
          formatFieldValue(sb, a, "value");
        } else {
          boolean first = true;
          for (String field : a.fieldValues.keySet()) {
            // parameters of the annotation
            if (!first) {
              sb.append(", ");
            } else {
              first = false;
            }
            sb.append(field);
            sb.append("=");
            formatFieldValue(sb, a, field);
          }
        }
        sb.append(")");
      }

      result.add(IPair.of(sb.toString(), a));
    }
    return result;
  }

  /**
   * Format an annotation field value.
   *
   * @param sb where to format the value to
   * @param a the annotation whose field to format
   * @param field which field to format
   */
  private void formatFieldValue(StringBuilder sb, Annotation a, String field) {
    AnnotationFieldType fieldType = a.def.fieldTypes.get(field);
    assert fieldType != null;
    fieldType.format(sb, a.fieldValues.get(field));
  }

  /**
   * Parses a method and fills in this.insertions.
   *
   * @param clist a criterion for the class containing the method being parsed
   * @param className the name of the containing class
   * @param methodName the name of the method
   * @param method the method's annotations (in scene-lib format)
   */
  private void parseMethod(
      CriterionList clist, @ClassGetName String className, String methodName, AMethod method) {
    // Being "in" a method refers to being somewhere in the
    // method's tree, which includes return types, parameters, receiver, and
    // elements inside the method body.
    clist = clist.add(Criteria.inMethod(methodName));

    // parse declaration annotations, fill in this.insertions
    parseElement(clist, method);

    // parse receiver
    CriterionList receiverClist = clist.add(Criteria.receiver(methodName));
    parseInnerAndOuterElements(receiverClist, method.receiver.type);

    // parse return type
    CriterionList returnClist = clist.add(Criteria.returnType(className, methodName));
    parseInnerAndOuterElements(returnClist, method.returnType);

    // parse bounds of method
    for (Entry<BoundLocation, ATypeElement> entry : method.bounds.entrySet()) {
      BoundLocation boundLoc = entry.getKey();
      ATypeElement bound = entry.getValue();
      CriterionList boundClist = clist.add(Criteria.methodBound(methodName, boundLoc));
      parseInnerAndOuterElements(boundClist, bound);
    }

    // parse parameters of method
    for (Entry<Integer, AField> entry : method.parameters.entrySet()) {
      Integer index = entry.getKey();
      AField param = entry.getValue();
      CriterionList paramClist = clist.add(Criteria.param(methodName, index));
      // parse declaration annotations
      // parseField(paramClist, index.toString(), param);
      parseInnerAndOuterElements(paramClist, param.type);
    }

    // parse insert annotations/typecasts of method
    parseASTInsertions(clist, method.insertAnnotations, method.insertTypecasts);

    // parse body of method
    parseBlock(clist, className, methodName, method.body);
  }

  private void parseBlock(CriterionList clist, String className, String methodName, ABlock block) {
    // parse locals of method
    for (Entry<LocalLocation, AField> entry : block.locals.entrySet()) {
      LocalLocation loc = entry.getKey();
      AElement var = entry.getValue();
      CriterionList varClist = clist.add(Criteria.local(methodName, loc));
      // parse declaration annotations
      parseElement(varClist, var);
      parseInnerAndOuterElements(varClist, var.type);
    }

    parseExpression(clist, className, methodName, block);
  }

  private void parseExpression(
      CriterionList clist, String className, String methodName, AExpression exp) {
    // parse typecasts of method
    for (Entry<RelativeLocation, ATypeElement> entry : exp.typecasts.entrySet()) {
      RelativeLocation loc = entry.getKey();
      ATypeElement cast = entry.getValue();
      CriterionList castClist = clist.add(Criteria.cast(methodName, loc));
      parseInnerAndOuterElements(castClist, cast);
    }

    // parse news (object creation) of method
    for (Entry<RelativeLocation, ATypeElement> entry : exp.news.entrySet()) {
      RelativeLocation loc = entry.getKey();
      ATypeElement newObject = entry.getValue();
      CriterionList newClist = clist.add(Criteria.newObject(methodName, loc));
      parseInnerAndOuterElements(newClist, newObject);
    }

    // parse instanceofs of method
    for (Entry<RelativeLocation, ATypeElement> entry : exp.instanceofs.entrySet()) {
      RelativeLocation loc = entry.getKey();
      ATypeElement instanceOf = entry.getValue();
      CriterionList instanceOfClist = clist.add(Criteria.instanceOf(methodName, loc));
      parseInnerAndOuterElements(instanceOfClist, instanceOf);
    }

    // parse member references of method
    for (Entry<RelativeLocation, ATypeElement> entry : exp.refs.entrySet()) {
      RelativeLocation loc = entry.getKey();
      ATypeElement ref = entry.getValue();
      CriterionList instanceOfClist = clist.add(Criteria.memberReference(methodName, loc));
      parseInnerAndOuterElements(instanceOfClist, ref);
    }

    // parse method invocations of method
    for (Entry<RelativeLocation, ATypeElement> entry : exp.calls.entrySet()) {
      RelativeLocation loc = entry.getKey();
      ATypeElement call = entry.getValue();
      CriterionList instanceOfClist = clist.add(Criteria.methodCall(methodName, loc));
      parseInnerAndOuterElements(instanceOfClist, call);
    }

    // parse lambda expressions of method
    for (Entry<RelativeLocation, AMethod> entry : exp.funs.entrySet()) {
      RelativeLocation loc = entry.getKey();
      AMethod lambda = entry.getValue();
      CriterionList lambdaClist = clist.add(Criteria.lambda(methodName, loc));
      parseLambdaExpression(className, methodName, lambda, lambdaClist);
    }
  }

  private void parseLambdaExpression(
      String className, String methodName, AMethod lambda, CriterionList clist) {
    for (Entry<Integer, AField> entry : lambda.parameters.entrySet()) {
      Integer index = entry.getKey();
      AField param = entry.getValue();
      CriterionList paramClist = clist.add(Criteria.param("(anonymous)", index));
      parseInnerAndOuterElements(paramClist, param.type);
      parseASTInsertions(paramClist, param.insertAnnotations, param.insertTypecasts);
    }
    parseBlock(clist, className, methodName, lambda.body);
  }

  private void parseASTInsertions(
      CriterionList clist,
      VivifyingMap<ASTPath, ATypeElement> insertAnnotations,
      VivifyingMap<ASTPath, ATypeElementWithType> insertTypecasts) {
    for (Entry<ASTPath, ATypeElement> entry : insertAnnotations.entrySet()) {
      ASTPath astPath = entry.getKey();
      ATypeElement insertAnnotation = entry.getValue();
      CriterionList insertAnnotationClist = clist.add(Criteria.astPath(astPath));
      parseInnerAndOuterElements(insertAnnotationClist, insertAnnotation, true);
    }
    for (Entry<ASTPath, ATypeElementWithType> entry : insertTypecasts.entrySet()) {
      ASTPath astPath = entry.getKey();
      ATypeElementWithType insertTypecast = entry.getValue();
      CriterionList insertTypecastClist = clist.add(Criteria.astPath(astPath));
      parseInnerAndOuterElements(insertTypecastClist, insertTypecast, true);
    }
  }
}
