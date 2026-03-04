package org.checkerframework.afu.annotator;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.JCTree;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.afu.annotator.find.AnnotationInsertion;
import org.checkerframework.afu.annotator.find.CastInsertion;
import org.checkerframework.afu.annotator.find.ConstructorInsertion;
import org.checkerframework.afu.annotator.find.Criteria;
import org.checkerframework.afu.annotator.find.GenericArrayLocationCriterion;
import org.checkerframework.afu.annotator.find.Insertion;
import org.checkerframework.afu.annotator.find.Insertions;
import org.checkerframework.afu.annotator.find.NewInsertion;
import org.checkerframework.afu.annotator.find.ReceiverInsertion;
import org.checkerframework.afu.annotator.find.TreeFinder;
import org.checkerframework.afu.annotator.find.TypedInsertion;
import org.checkerframework.afu.annotator.scanner.LocalVariableScanner;
import org.checkerframework.afu.annotator.scanner.TreePathUtil;
import org.checkerframework.afu.annotator.specification.IndexFileSpecification;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.el.ABlock;
import org.checkerframework.afu.scenelib.el.AClass;
import org.checkerframework.afu.scenelib.el.ADeclaration;
import org.checkerframework.afu.scenelib.el.AElement;
import org.checkerframework.afu.scenelib.el.AExpression;
import org.checkerframework.afu.scenelib.el.AField;
import org.checkerframework.afu.scenelib.el.AMethod;
import org.checkerframework.afu.scenelib.el.AScene;
import org.checkerframework.afu.scenelib.el.ATypeElement;
import org.checkerframework.afu.scenelib.el.ATypeElementWithType;
import org.checkerframework.afu.scenelib.el.AnnotationDef;
import org.checkerframework.afu.scenelib.el.DefException;
import org.checkerframework.afu.scenelib.el.ElementVisitor;
import org.checkerframework.afu.scenelib.el.LocalLocation;
import org.checkerframework.afu.scenelib.el.TypePathEntry;
import org.checkerframework.afu.scenelib.io.ASTIndex;
import org.checkerframework.afu.scenelib.io.ASTPath;
import org.checkerframework.afu.scenelib.io.ASTRecord;
import org.checkerframework.afu.scenelib.io.DebugWriter;
import org.checkerframework.afu.scenelib.io.IndexFileParser;
import org.checkerframework.afu.scenelib.io.IndexFileWriter;
import org.checkerframework.afu.scenelib.io.classfile.ClassFileReader;
import org.checkerframework.afu.scenelib.type.DeclaredType;
import org.checkerframework.afu.scenelib.type.Type;
import org.checkerframework.afu.scenelib.util.CommandLineUtils;
import org.checkerframework.afu.scenelib.util.coll.VivifyingMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.TypePath;
import org.plumelib.options.Option;
import org.plumelib.options.OptionGroup;
import org.plumelib.options.Options;
import org.plumelib.reflection.ReflectionPlume;
import org.plumelib.util.FileIOException;
import org.plumelib.util.FilesPlume;
import org.plumelib.util.IPair;

/**
 * This is the main class for the annotator, which inserts annotations in Java source code. You can
 * call it as {@code java org.checkerframework.afu.annotator.Main} or by using the shell script
 * {@code insert-annotations-to-source}.
 *
 * <p>It takes as input
 *
 * <ul>
 *   <li>annotation (index) files, which indicate the annotations to insert
 *   <li>Java source files, into which the annotator inserts annotations
 * </ul>
 *
 * Annotations that are not for the specified Java files are ignored.
 *
 * <p>The <a id="command-line-options">command-line options</a> are as follows:
 * <!-- start options doc (DO NOT EDIT BY HAND) -->
 *
 * <ul>
 *   <li id="optiongroup:General-options">General options
 *       <ul>
 *         <li id="option:outdir"><b>-d</b> <b>--outdir=</b><i>directory</i>. Directory in which
 *             output files are written. [default: annotated/]
 *         <li id="option:in-place"><b>-i</b> <b>--in-place=</b><i>boolean</i>. If true, overwrite
 *             original source files (making a backup first). Furthermore, if the backup files
 *             already exist, they are used instead of the .java files. This behavior permits a user
 *             to tweak the {@code .jaif} file and re-run the annotator.
 *             <p>Note that if the user runs the annotator with --in-place, makes edits, and then
 *             re-runs the annotator with this --in-place option, those edits are lost. Similarly,
 *             if the user runs the annotator twice in a row with --in-place, only the last set of
 *             annotations will appear in the codebase at the end.
 *             <p>To preserve changes when using the --in-place option, first remove the backup
 *             files. Or, use the {@code -d .} option, which makes (and reads) no backup, instead of
 *             --in-place. [default: false]
 *         <li id="option:abbreviate"><b>-a</b> <b>--abbreviate=</b><i>boolean</i>. If true, insert
 *             {@code import} statements as necessary. [default: true]
 *         <li id="option:omit-annotation"><b>-o</b> <b>--omit-annotation=</b><i>string</i>. Omit
 *             given annotation
 *         <li id="option:nowarn"><b>--nowarn=</b><i>boolean</i>. Suppress warnings about disallowed
 *             insertions [default: false]
 *         <li id="option:convert-jaifs"><b>--convert-jaifs=</b><i>boolean</i>. Convert JAIFs to AST
 *             Path format, but do no insertion into source [default: false]
 *         <li id="option:help"><b>-h</b> <b>--help=</b><i>boolean</i>. Print usage information and
 *             exit [default: false]
 *       </ul>
 *   <li id="optiongroup:Debugging-options">Debugging options
 *       <ul>
 *         <li id="option:verbose"><b>-v</b> <b>--verbose=</b><i>boolean</i>. Verbose (print
 *             progress information) [default: false]
 *         <li id="option:debug"><b>--debug=</b><i>boolean</i>. Debug (print debug information)
 *             [default: false]
 *         <li id="option:print-error-stack"><b>--print-error-stack=</b><i>boolean</i>. Print error
 *             stack [default: false]
 *       </ul>
 * </ul>
 *
 * <!-- end options doc -->
 */
public class Main {

  // Options

  /** Directory in which output files are written. */
  @OptionGroup("General options")
  @Option("-d <directory> Directory in which output files are written")
  public static String outdir = "annotated/";

  /**
   * If true, overwrite original source files (making a backup first). Furthermore, if the backup
   * files already exist, they are used instead of the .java files. This behavior permits a user to
   * tweak the {@code .jaif} file and re-run the annotator.
   *
   * <p>Note that if the user runs the annotator with {@code --in-place}, makes edits, and then
   * re-runs the annotator with this {@code --in-place} option, those edits are lost. Similarly, if
   * the user runs the annotator twice in a row with {@code --in-place}, only the last set of
   * annotations will appear in the codebase at the end.
   *
   * <p>To preserve changes when using the {@code --in-place} option, first remove the backup files.
   * Or, use the {@code -d .} option, which makes (and reads) no backup, instead of {@code
   * --in-place}.
   */
  @Option("-i Overwrite original source files")
  public static boolean in_place = false;

  /** If true, insert {@code import} statements as necessary. */
  @Option("-a Abbreviate annotation names")
  public static boolean abbreviate = true;

  /** Don't insert the given annotation. */
  @Option("-o Omit given annotation")
  public static String omit_annotation;

  @Option("Suppress warnings about disallowed insertions")
  public static boolean nowarn;

  // Instead of doing insertions, create new JAIFs using AST paths
  //  extracted from existing JAIFs and source files they match
  @Option("Convert JAIFs to AST Path format, but do no insertion into source")
  public static boolean convert_jaifs = false;

  @Option("-h Print usage information and exit")
  public static boolean help = false;

  // Debugging options go below here.

  @OptionGroup("Debugging options")
  @Option("-v Verbose (print progress information)")
  public static boolean verbose = false;

  @Option("Debug (print debug information)")
  public static boolean debug = false;

  @Option("Print error stack")
  public static boolean print_error_stack = false;

  // TODO: remove this.
  public static boolean temporaryDebug = false;

  @SuppressWarnings("resourceleak:required.method.not.known") // Not relevant to resources
  private static ElementVisitor<Void, AElement> classFilter =
      new ElementVisitor<Void, AElement>() {
        <K, V extends AElement> Void filter(VivifyingMap<K, V> vm0, VivifyingMap<K, V> vm1) {
          for (Map.Entry<K, V> entry : vm0.entrySet()) {
            entry.getValue().accept(this, vm1.getVivify(entry.getKey()));
          }
          return null;
        }

        @Override
        public Void visitAnnotationDef(AnnotationDef def, AElement el) {
          // not used, since package declarations not handled here
          return null;
        }

        @Override
        public Void visitBlock(ABlock el0, AElement el) {
          ABlock el1 = (ABlock) el;
          filter(el0.locals, el1.locals);
          return visitExpression(el0, el);
        }

        @Override
        public Void visitClass(AClass el0, AElement el) {
          AClass el1 = (AClass) el;
          filter(el0.methods, el1.methods);
          filter(el0.fields, el1.fields);
          filter(el0.fieldInits, el1.fieldInits);
          filter(el0.staticInits, el1.staticInits);
          filter(el0.instanceInits, el1.instanceInits);
          return visitDeclaration(el0, el);
        }

        @Override
        public Void visitDeclaration(ADeclaration el0, AElement el) {
          ADeclaration el1 = (ADeclaration) el;
          VivifyingMap<ASTPath, ATypeElement> insertAnnotations = el1.insertAnnotations;
          VivifyingMap<ASTPath, ATypeElementWithType> insertTypecasts = el1.insertTypecasts;
          for (Map.Entry<ASTPath, ATypeElement> entry : el0.insertAnnotations.entrySet()) {
            ASTPath p = entry.getKey();
            ATypeElement e = entry.getValue();
            insertAnnotations.put(p, e);
            // visitTypeElement(e, insertAnnotations.getVivify(p));
          }
          for (Map.Entry<ASTPath, ATypeElementWithType> entry : el0.insertTypecasts.entrySet()) {
            ASTPath p = entry.getKey();
            ATypeElementWithType e = entry.getValue();
            Type type = e.getType();
            if (type instanceof DeclaredType && ((DeclaredType) type).getName().isEmpty()) {
              insertAnnotations.put(p, e);
              // visitTypeElement(e, insertAnnotations.getVivify(p));
            } else {
              insertTypecasts.put(p, e);
              // visitTypeElementWithType(e, insertTypecasts.getVivify(p));
            }
          }
          return null;
        }

        @Override
        public Void visitExpression(AExpression el0, AElement el) {
          AExpression el1 = (AExpression) el;
          filter(el0.typecasts, el1.typecasts);
          filter(el0.instanceofs, el1.instanceofs);
          filter(el0.news, el1.news);
          return null;
        }

        @Override
        public Void visitField(AField el0, AElement el) {
          return visitDeclaration(el0, el);
        }

        @Override
        public Void visitMethod(AMethod el0, AElement el) {
          AMethod el1 = (AMethod) el;
          filter(el0.bounds, el1.bounds);
          el0.returnType.accept(this, el1.returnType);
          el0.receiver.accept(this, el1.receiver);
          filter(el0.parameters, el1.parameters);
          filter(el0.throwsException, el1.throwsException);
          filter(el0.preconditions, el1.preconditions);
          filter(el0.postconditions, el1.postconditions);
          el0.body.accept(this, el1.body);
          return visitDeclaration(el0, el);
        }

        @Override
        public Void visitTypeElement(ATypeElement el0, AElement el) {
          ATypeElement el1 = (ATypeElement) el;
          filter(el0.innerTypes, el1.innerTypes);
          return null;
        }

        @Override
        public Void visitTypeElementWithType(ATypeElementWithType el0, AElement el) {
          ATypeElementWithType el1 = (ATypeElementWithType) el;
          el1.setType(el0.getType());
          return visitTypeElement(el0, el);
        }

        @Override
        public Void visitElement(AElement el, AElement arg) {
          return null;
        }
      };

  private static AScene filteredScene(final AScene scene) {
    final AScene filtered = new AScene();
    filtered.packages.putAll(scene.packages);
    filtered.imports.putAll(scene.imports);
    for (Map.Entry<String, AClass> entry : scene.classes.entrySet()) {
      String key = entry.getKey();
      AClass clazz0 = entry.getValue();
      AClass clazz1 = filtered.classes.getVivify(key);
      clazz0.accept(classFilter, clazz1);
    }
    filtered.prune();
    return filtered;
  }

  private static ATypeElement findInnerTypeElement(
      ASTRecord rec, ADeclaration decl, Insertion ins) {
    ASTPath astPath = rec.astPath;
    GenericArrayLocationCriterion galc = ins.getCriteria().getGenericArrayLocation();
    assert astPath != null && galc != null;
    List<TypePathEntry> tpes = galc.getLocation();
    ASTPath.ASTEntry entry;
    for (TypePathEntry tpe : tpes) {
      switch (tpe.step) {
        case TypePath.ARRAY_ELEMENT:
          if (!astPath.isEmpty()) {
            entry = astPath.getLast();
            if (entry.getTreeKind() == Tree.Kind.NEW_ARRAY && entry.childSelectorIs(ASTPath.TYPE)) {
              entry =
                  new ASTPath.ASTEntry(Tree.Kind.NEW_ARRAY, ASTPath.TYPE, entry.getArgument() + 1);
              break;
            }
          }
          entry = new ASTPath.ASTEntry(Tree.Kind.ARRAY_TYPE, ASTPath.TYPE);
          break;
        case TypePath.INNER_TYPE:
          entry = new ASTPath.ASTEntry(Tree.Kind.MEMBER_SELECT, ASTPath.EXPRESSION);
          break;
        case TypePath.TYPE_ARGUMENT:
          entry =
              new ASTPath.ASTEntry(
                  Tree.Kind.PARAMETERIZED_TYPE, ASTPath.TYPE_ARGUMENT, tpe.argument);
          break;
        case TypePath.WILDCARD_BOUND:
          entry = new ASTPath.ASTEntry(Tree.Kind.UNBOUNDED_WILDCARD, ASTPath.BOUND);
          break;
        default:
          throw new IllegalArgumentException("unknown type tag " + tpe.step);
      }
      astPath = astPath.extend(entry);
    }

    return decl.insertAnnotations.getVivify(astPath);
  }

  private static void convertInsertion(
      String pkg,
      JCTree.JCCompilationUnit tree,
      ASTRecord rec,
      Insertion ins,
      AScene scene,
      Multimap<Insertion, Annotation> insertionSources) {
    Collection<Annotation> annos = insertionSources.get(ins);
    if (rec == null) {
      if (ins.getCriteria().isOnPackage()) {
        for (Annotation anno : annos) {
          scene.packages.get(pkg).tlAnnotationsHere.add(anno);
        }
      }
    } else if (scene != null && rec.className != null) {
      AClass clazz = scene.classes.getVivify(rec.className);
      ADeclaration decl = null; // insertion target
      if (ins.getCriteria().onBoundZero()) {
        int n = rec.astPath.size();
        if (!rec.astPath.get(n - 1).childSelectorIs(ASTPath.BOUND)) {
          ASTPath astPath = ASTPath.empty();
          for (int i = 0; i < n; i++) {
            astPath = astPath.extend(rec.astPath.get(i));
          }
          astPath =
              astPath.extend(new ASTPath.ASTEntry(Tree.Kind.TYPE_PARAMETER, ASTPath.BOUND, 0));
          rec = rec.replacePath(astPath);
        }
      }
      if (rec.methodName == null) {
        decl = rec.varName == null ? clazz : clazz.fields.getVivify(rec.varName);
      } else {
        AMethod meth = clazz.methods.getVivify(rec.methodName);
        if (rec.varName == null) {
          decl = meth; // ?
        } else {
          try {
            int i = Integer.parseInt(rec.varName);
            decl = i < 0 ? meth.receiver : meth.parameters.getVivify(i);
          } catch (NumberFormatException e) {
            TreePath path = ASTIndex.getTreePath(tree, rec);
            JCTree.JCVariableDecl varTree = null;
            JCTree.JCMethodDecl methTree = null;
            loop:
            while (path != null) {
              Tree leaf = path.getLeaf();
              switch (leaf.getKind()) {
                case VARIABLE:
                  varTree = (JCTree.JCVariableDecl) leaf;
                  break;
                case METHOD:
                  methTree = (JCTree.JCMethodDecl) leaf;
                  break;
                case ANNOTATION:
                case CLASS:
                case ENUM:
                case INTERFACE:
                  break loop;
                default:
                  path = path.getParentPath();
              }
            }
            while (path != null) {
              Tree leaf = path.getLeaf();
              Tree.Kind kind = leaf.getKind();
              if (kind == Tree.Kind.METHOD) {
                methTree = (JCTree.JCMethodDecl) leaf;
                int i = LocalVariableScanner.indexOfVarTree(path, varTree, rec.varName);
                int m = methTree.getStartPosition();
                int a = varTree.getStartPosition();
                int b = TreePathUtil.getEndPosition(varTree, tree);
                LocalLocation loc = new LocalLocation(i, a - m, b - a);
                decl = meth.body.locals.getVivify(loc);
                break;
              }
              if (ASTPath.isClassEquiv(kind)) {
                // classTree = (JCTree.JCClassDecl) leaf;
                // ???
                break;
              }
              path = path.getParentPath();
            }
          }
        }
      }
      if (decl != null) {
        AElement el;
        if (rec.astPath.isEmpty()) {
          el = decl;
        } else if (ins.getKind() == Insertion.Kind.CAST) {
          ATypeElementWithType elem = decl.insertTypecasts.getVivify(rec.astPath);
          elem.setType(((CastInsertion) ins).getType());
          el = elem;
        } else {
          el = decl.insertAnnotations.getVivify(rec.astPath);
        }
        for (Annotation anno : annos) {
          el.tlAnnotationsHere.add(anno);
        }
        if (ins instanceof TypedInsertion) {
          TypedInsertion ti = (TypedInsertion) ins;
          if (!rec.astPath.isEmpty()) {
            // addInnerTypePaths(decl, rec, ti, insertionSources);
          }
          for (Insertion inner : ti.getInnerTypeInsertions()) {
            Tree t = ASTIndex.getNode(tree, rec);
            if (t != null) {
              ATypeElement elem = findInnerTypeElement(rec, decl, inner);
              for (Annotation a : insertionSources.get(inner)) {
                elem.tlAnnotationsHere.add(a);
              }
            }
          }
        }
      }
    }
  }

  // Implementation details:
  //  1. The annotator partially compiles source
  //     files using the compiler API (JSR-199), obtaining an AST.
  //  2. The annotator reads the specification file, producing a set of
  //     org.checkerframework.afu.annotator.find.Insertions.  Insertions completely specify what to
  //     write (as a String, which is ultimately translated according to the
  //     keyword file) and how to write it (as org.checkerframework.afu.annotator.find.Criteria).
  //  3. It then traverses the tree, looking for nodes that satisfy the
  //     Insertion Criteria, translating the Insertion text against the
  //     keyword file, and inserting the annotations into the source file.

  /**
   * Runs the annotator, parsing the source and spec files and applying the annotations.
   *
   * @param args .jaif files and/or .java files and/or @arg-files, in any order
   */
  @SuppressWarnings({
    "ReferenceEquality", // interned operand
    "EmptyCatch", // TODO
  })
  public static void main(String[] args) throws IOException {

    if (verbose) {
      System.out.printf("insert-annotations-to-source (%s)%n", ClassFileReader.INDEX_UTILS_VERSION);
    }

    Options options =
        new Options(
            "java org.checkerframework.afu.annotator.Main [options] { jaif-file | java-file |"
                + " @arg-file } ..."
                + System.lineSeparator()
                + "(Contents of argfiles are expanded into the argument list.)",
            Main.class);
    String[] cl_args;
    String[] file_args;
    try {
      cl_args = CommandLineUtils.parseCommandLine(args);
      file_args = options.parse(true, cl_args);
    } catch (Exception ex) {
      System.err.println(ex);
      System.err.println("(For non-argfile beginning with \"@\", use \"@@\" for initial \"@\".");
      System.err.println("Alternative for filenames: indicate directory, e.g. as './@file'.");
      System.err.println("Alternative for flags: use '=', as in '-o=@Deprecated'.)");
      System.exit(1);
      throw new Error("Unreachable");
    }

    DebugWriter dbug = new DebugWriter(debug);
    DebugWriter verb = new DebugWriter(verbose);
    TreeFinder.warn.setEnabled(!nowarn);
    TreeFinder.dbug.setEnabled(debug);
    Criteria.dbug.setEnabled(debug);

    if (help) {
      options.printUsage();
      System.exit(0);
    }

    @SuppressWarnings("interning:not.interned") // reference equality check
    boolean outdir_and_in_place = in_place && outdir != "annotated/"; // interned initial value
    if (outdir_and_in_place) {
      System.out.println("The --outdir and --in-place options are mutually exclusive.");
      options.printUsage();
      System.exit(1);
    }

    if (file_args.length < 2) {
      System.out.printf("Supplied %d arguments, at least 2 needed%n", file_args.length);
      System.out.printf("Supplied arguments: %s%n", Arrays.toString(args));
      System.out.printf(
          "  (After javac parsing, remaining arguments = %s)%n", Arrays.toString(cl_args));
      System.out.printf("  (File arguments = %s)%n", Arrays.toString(file_args));
      options.printUsage();
      System.exit(1);
    }

    // The names of annotation files (.jaif files).
    List<String> jaifFiles = new ArrayList<>();
    // The Java files into which to insert.
    List<String> javafiles = new ArrayList<>();

    for (String arg : file_args) {
      if (arg.endsWith(".java")) {
        javafiles.add(arg);
      } else if (arg.endsWith(".jaif") || arg.endsWith(".jann")) {
        jaifFiles.add(arg);
      } else {
        System.out.println("Unrecognized file extension: " + arg);
        System.exit(1);
        throw new Error("unreachable");
      }
    }

    computeConstructors(javafiles);

    // The insertions specified by the annotation files.
    Insertions insertions = new Insertions();

    // Indices to maintain insertion source traces.
    Map<String, Multimap<Insertion, Annotation>> insertionIndex = new HashMap<>();
    Map<Insertion, String> insertionOrigins = new HashMap<>();
    Map<String, AScene> scenes = new HashMap<>();

    // maintain imports info for annotations field
    // Key: fully-qualified annotation name. e.g. "com.foo.Bar" for annotation @com.foo.Bar(x).
    // Value: names of packages this annotation needs.
    Map<String, Set<String>> annotationImports = new HashMap<>();

    IndexFileParser.setAbbreviate(abbreviate);
    for (String jaifFile : jaifFiles) {
      IndexFileSpecification spec = new IndexFileSpecification(jaifFile);
      try {
        List<Insertion> parsedSpec = spec.parse();
        if (temporaryDebug) {
          System.out.printf("parsedSpec (size %d):%n", parsedSpec.size());
          for (Insertion insertion : parsedSpec) {
            System.out.printf("  %s, isInserted=%s%n", insertion, insertion.isInserted());
          }
        }
        AScene scene = spec.getScene();
        Collections.sort(
            parsedSpec,
            new Comparator<Insertion>() {
              @Override
              public int compare(Insertion i1, Insertion i2) {
                ASTPath p1 = i1.getCriteria().getASTPath();
                ASTPath p2 = i2.getCriteria().getASTPath();
                return p1 == null ? p2 == null ? 0 : -1 : p2 == null ? 1 : p1.compareTo(p2);
              }
            });
        if (convert_jaifs) {
          scenes.put(jaifFile, filteredScene(scene));
          for (Insertion ins : parsedSpec) {
            insertionOrigins.put(ins, jaifFile);
          }
          if (!insertionIndex.containsKey(jaifFile)) {
            insertionIndex.put(jaifFile, LinkedHashMultimap.<Insertion, Annotation>create());
          }
          insertionIndex.get(jaifFile).putAll(spec.insertionSources());
        }
        verb.debug("Read %d annotations from %s%n", parsedSpec.size(), jaifFile);
        if (omit_annotation != null) {
          List<Insertion> filtered = new ArrayList<Insertion>(parsedSpec.size());
          for (Insertion insertion : parsedSpec) {
            // TODO: this won't omit annotations if the insertion is more than
            // just the annotation (such as if the insertion is a cast
            // insertion or a 'this' parameter in a method declaration).
            if (!omit_annotation.equals(insertion.getText())) {
              filtered.add(insertion);
            }
          }
          parsedSpec = filtered;
          verb.debug("After filtering: %d annotations from %s%n", parsedSpec.size(), jaifFile);
        }
        // if (dbug.isEnabled()) {
        //   dbug.debug("parsedSpec:%n");
        //   for (Insertion insertion : parsedSpec) {
        //     dbug.debug("  %s, isInserted=%s%n", insertion, insertion.isInserted());
        //   }
        // }
        insertions.addAll(parsedSpec);
        annotationImports.putAll(spec.annotationImports());
      } catch (RuntimeException e) {
        if (e.getCause() != null && e.getCause() instanceof FileNotFoundException) {
          System.err.println("File not found: " + jaifFile);
          System.exit(1);
        } else {
          throw e;
        }
      } catch (FileIOException e) {
        // Add 1 to the line number since line numbers in text editors are usually one-based.
        System.err.println(
            "Error while parsing annotation file "
                + jaifFile
                + " at line "
                + (e.lineNumber + 1)
                + ":");
        if (e.getMessage() != null) {
          System.err.println("  " + e.getMessage());
        }
        if (e.getCause() != null && e.getCause().getMessage() != null) {
          String causeMessage = e.getCause().getMessage();
          System.err.println("  " + causeMessage);
          if (causeMessage.startsWith("Could not load class: ")) {
            System.err.println(
                "To fix the problem, add class "
                    + causeMessage.substring(22)
                    + " to the classpath.");
            System.err.println("The classpath is:");
            System.err.println(ReflectionPlume.classpathToString());
          }
        }
        if (print_error_stack) {
          e.printStackTrace();
        }
        System.exit(1);
      }
    }

    if (dbug.isEnabled()) {
      dbug.debug("In org.checkerframework.afu.annotator.Main:%n");
      dbug.debug("%d insertions, %d .java files%n", insertions.size(), javafiles.size());
      dbug.debug("Insertions:%n");
      for (Insertion insertion : insertions) {
        dbug.debug("  %s, isInserted=%s%n", insertion, insertion.isInserted());
      }
    }

    for (String javafilename : javafiles) {
      verb.debug("Processing %s%n", javafilename);

      File javafile = new File(javafilename);
      File unannotated = new File(javafilename + ".unannotated");
      if (in_place) {
        // It doesn't make sense to check timestamps;
        // if the .java.unannotated file exists, then just use it.
        // A user can rename that file back to just .java to cause the
        // .java file to be read.
        if (unannotated.exists()) {
          verb.debug("Renaming %s to %s%n", unannotated, javafile);
          boolean success = unannotated.renameTo(javafile);
          if (!success) {
            throw new Error(String.format("Failed renaming %s to %s", unannotated, javafile));
          }
        }
      }

      Source src = fileToSource(javafilename);
      if (src == null) {
        return;
      } else {
        verb.debug("Parsed %s%n", javafilename);
      }
      String fileLineSep;
      try {
        // fileLineSep is set here so that exceptions can be caught
        fileLineSep = FilesPlume.inferLineSeparator(javafilename);
      } catch (IOException e) {
        throw new Error("Cannot read " + javafilename, e);
      }

      // Imports required to resolve annotations (when abbreviate==true).
      LinkedHashSet<String> imports = new LinkedHashSet<>();
      int num_insertions = 0;
      String pkg = "";

      for (CompilationUnitTree cut : src.parse()) {
        JCTree.JCCompilationUnit tree = (JCTree.JCCompilationUnit) cut;
        ExpressionTree pkgExp = cut.getPackageName();
        pkg = pkgExp == null ? "" : pkgExp.toString();

        // Create a finder, and use it to get positions.
        TreeFinder finder = new TreeFinder(tree);
        SetMultimap<IPair<Integer, ASTPath>, Insertion> positions =
            finder.getPositions(tree, insertions);
        if (dbug.isEnabled()) {
          dbug.debug("In org.checkerframework.afu.annotator.Main:%n");
          dbug.debug("positions (for %d insertions) = %s%n", insertions.size(), positions);
        }

        if (convert_jaifs) {
          // With --convert-jaifs command-line option, the program is used only for JAIF conversion.
          // Execute the following block and then skip the remainder of the loop.
          Multimap<ASTRecord, Insertion> astInsertions = finder.getPaths();
          for (Map.Entry<ASTRecord, Collection<Insertion>> entry :
              astInsertions.asMap().entrySet()) {
            ASTRecord rec = entry.getKey();
            for (Insertion ins : entry.getValue()) {
              if (ins.getCriteria().getASTPath() != null) {
                continue;
              }
              String arg = insertionOrigins.get(ins);
              AScene scene = scenes.get(arg);
              Multimap<Insertion, Annotation> insertionSources = insertionIndex.get(arg);
              // String text =
              //  ins.getText(abbreviate, false, 0, '\0');

              // TODO: adjust for missing end of path (?)

              if (insertionSources.containsKey(ins)) {
                convertInsertion(pkg, tree, rec, ins, scene, insertionSources);
              }
            }
          }
          continue;
        }

        // Apply the positions to the source file.
        verb.debug(
            "getPositions returned %d positions in tree for %s%n", positions.size(), javafilename);

        Set<IPair<Integer, ASTPath>> positionKeysUnsorted = positions.keySet();
        Set<IPair<Integer, ASTPath>> positionKeysSorted =
            new TreeSet<IPair<Integer, ASTPath>>(
                new Comparator<IPair<Integer, ASTPath>>() {
                  @Override
                  public int compare(IPair<Integer, ASTPath> p1, IPair<Integer, ASTPath> p2) {
                    int c = Integer.compare(p2.first, p1.first);
                    if (c != 0) {
                      return c;
                    }
                    return p2.second == null
                        ? (p1.second == null ? 0 : -1)
                        : (p1.second == null ? 1 : p2.second.compareTo(p1.second));
                  }
                });
        positionKeysSorted.addAll(positionKeysUnsorted);
        for (IPair<Integer, ASTPath> pair : positionKeysSorted) {
          boolean receiverInserted = false;
          boolean newInserted = false;
          boolean constructorInserted = false;
          Set<String> seen = new TreeSet<>();
          List<Insertion> toInsertList = new ArrayList<>(positions.get(pair));
          // The Multimap interface doesn't seem to have a way to specify the order of elements in
          // the collection, so sort them here.
          toInsertList.sort(insertionSorter);
          dbug.debug("insertion pos: %d%n", pair.first);
          dbug.debug("insertions sorted: %s%n", toInsertList);
          assert pair.first >= 0
              : "pos is negative: " + pair.first + " " + toInsertList.get(0) + " " + javafilename;
          for (Insertion iToInsert : toInsertList) {
            // Possibly add whitespace after the insertion
            String trailingWhitespace = "";
            boolean gotSeparateLine = false;
            int pos = pair.first; // reset each iteration in case of dyn adjustment
            if (iToInsert.isSeparateLine()) {
              // System.out.printf("isSeparateLine=true for insertion at pos %d: %s%n", pos,
              // iToInsert);

              // If an annotation should have its own line, first check that the insertion location
              // is the first non-whitespace on its line. If so, then the insertion content should
              // be the annotation, followed, by a line break, followed by a copy of the indentation
              // of the line being inserted onto. This puts the annotation on its own line aligned
              // with the contents of the next line.

              // Number of whitespace characters preceeding the insertion position on the same line
              // (tabs count as one).
              int indentation = 0;
              while ((pos - indentation != 0)
                  // horizontal whitespace
                  && (src.charAt(pos - indentation - 1) == ' '
                      || src.charAt(pos - indentation - 1) == '\t')) {
                // System.out.printf("src.charAt(pos-indentation-1 == %d-%d-1)='%s'%n",
                //                   pos, indentation, src.charAt(pos-indentation-1));
                indentation++;
              }
              // Checks that insertion position is the first non-whitespace on the line it occurs
              // on.
              if ((pos - indentation == 0)
                  || (src.charAt(pos - indentation - 1) == '\f'
                      || src.charAt(pos - indentation - 1) == '\n'
                      || src.charAt(pos - indentation - 1) == '\r')) {
                trailingWhitespace = fileLineSep + src.substring(pos - indentation, pos);
                gotSeparateLine = true;
              }
            }

            char precedingChar;
            if (pos != 0) {
              precedingChar = src.charAt(pos - 1);
            } else {
              precedingChar = '\0';
            }

            if (iToInsert.getKind() == Insertion.Kind.ANNOTATION) {
              AnnotationInsertion ai = (AnnotationInsertion) iToInsert;
              if (ai.isGenerateBound()) { // avoid multiple ampersands
                try {
                  String s = src.substring(pos, pos + 9);
                  if ("Object & ".equals(s)) {
                    ai.setGenerateBound(false);
                    precedingChar = '.'; // suppress leading space
                  }
                } catch (StringIndexOutOfBoundsException e) {
                }
              }
              if (ai.isGenerateExtends()) { // avoid multiple "extends"
                try {
                  String s = src.substring(pos, pos + 9);
                  if (" extends ".equals(s)) {
                    ai.setGenerateExtends(false);
                    pos += 8;
                  }
                } catch (StringIndexOutOfBoundsException e) {
                }
              }
            } else if (iToInsert.getKind() == Insertion.Kind.CAST) {
              ((CastInsertion) iToInsert).setOnArrayLiteral(src.charAt(pos) == '{');
            } else if (iToInsert.getKind() == Insertion.Kind.RECEIVER) {
              ReceiverInsertion ri = (ReceiverInsertion) iToInsert;
              ri.setAnnotationsOnly(receiverInserted);
              receiverInserted = true;
            } else if (iToInsert.getKind() == Insertion.Kind.NEW) {
              NewInsertion ni = (NewInsertion) iToInsert;
              ni.setAnnotationsOnly(newInserted);
              newInserted = true;
            } else if (iToInsert.getKind() == Insertion.Kind.CONSTRUCTOR) {
              ConstructorInsertion ci = (ConstructorInsertion) iToInsert;
              if (constructorInserted) {
                ci.setAnnotationsOnly(true);
              }
              constructorInserted = true;
            }

            String toInsert =
                iToInsert.getText(abbreviate, gotSeparateLine, pos, precedingChar)
                    + trailingWhitespace;
            // eliminate duplicates
            if (seen.contains(toInsert)) {
              continue;
            }
            seen.add(toInsert);

            // If it's an annotation and already there, don't re-insert.  This is a hack!
            // Also, I think this is already checked when constructing the
            // insertions.
            if (toInsert.startsWith("@")) {
              int precedingTextPos = pos - toInsert.length() - 1;
              if (precedingTextPos >= 0) {
                String precedingTextPlusChar = src.getString().substring(precedingTextPos, pos);
                if (toInsert.equals(precedingTextPlusChar.substring(0, toInsert.length()))
                    || toInsert.equals(precedingTextPlusChar.substring(1))) {
                  dbug.debug(
                      "Inserting '%s' at %d in code of length %d with preceding text '%s'%n",
                      toInsert, pos, src.getString().length(), precedingTextPlusChar);
                  dbug.debug("Already present, skipping%n");
                  continue;
                }
              }
              int followingTextEndPos = pos + toInsert.length();
              if (followingTextEndPos < src.getString().length()) {
                String followingText = src.getString().substring(pos, followingTextEndPos);
                dbug.debug("followingText=\"%s\"%n", followingText);
                dbug.debug("toInsert=\"%s\"%n", toInsert);
                // toInsertNoWs does not contain the trailing whitespace.
                String toInsertNoWs = toInsert.substring(0, toInsert.length() - 1);
                if (followingText.equals(toInsert)
                    || (followingText.substring(0, followingText.length() - 1).equals(toInsertNoWs)
                        // Untested.  Is there an off-by-one error here?
                        && Character.isWhitespace(src.getString().charAt(followingTextEndPos)))) {
                  dbug.debug("Already present, skipping %s%n", toInsertNoWs);
                  continue;
                }
              }
            }

            // TODO: Neither the above hack nor this check should be
            // necessary.  Find out why re-insertions still occur and
            // fix properly.
            if (iToInsert.isInserted()) {
              continue;
            }
            src.insert(pos, toInsert);
            if (verbose && !debug) {
              System.out.print(".");
              num_insertions++;
              if ((num_insertions % 50) == 0) {
                System.out.println(); // terminate the line that contains dots
              }
            }
            dbug.debug("Post-insertion source: %s%n", src.getString());

            Collection<String> packageNames = nonJavaLangClasses(iToInsert.getPackageNames());
            if (!packageNames.isEmpty()) {
              dbug.debug("Need import %s%n  due to insertion %s%n", packageNames, toInsert);
              imports.addAll(packageNames);
            }
            if (iToInsert instanceof AnnotationInsertion) {
              AnnotationInsertion annoToInsert = (AnnotationInsertion) iToInsert;
              Set<String> annoImports =
                  annotationImports.get(annoToInsert.getAnnotationFullyQualifiedName());
              if (annoImports != null) {
                imports.addAll(annoImports);
              }
            }
          }
        }
      }

      if (convert_jaifs) {
        for (Map.Entry<String, AScene> entry : scenes.entrySet()) {
          String filename = entry.getKey();
          AScene scene = entry.getValue();
          try {
            IndexFileWriter.write(scene, filename + ".converted");
          } catch (DefException e) {
            System.err.println(filename + ": " + " format error in conversion");
            if (print_error_stack) {
              e.printStackTrace();
            }
          }
        }
        return; // done with conversion
      }

      if (dbug.isEnabled()) {
        dbug.debug("%d imports to insert%n", imports.size());
        for (String classname : imports) {
          dbug.debug("  %s%n", classname);
        }
      }

      // insert import statements
      {
        Pattern importPattern = Pattern.compile("(?m)^import\\b");
        Pattern packagePattern = Pattern.compile("(?m)^package\\b.*;(\\n|\\r\\n?)");
        int importIndex = 0; // default: beginning of file
        String srcString = src.getString();
        Matcher m = importPattern.matcher(srcString);
        Set<String> inSource = new TreeSet<>();
        if (m.find()) {
          importIndex = m.start();
          do {
            int i = m.start();
            int j = srcString.indexOf(System.lineSeparator(), i) + 1;
            if (j <= 0) {
              j = srcString.length();
            }
            String s = srcString.substring(i, j);
            inSource.add(s);
          } while (m.find());
        } else {
          // Debug.info("Didn't find import in " + srcString);
          m = packagePattern.matcher(srcString);
          if (m.find()) {
            importIndex = m.end();
          }
        }
        for (String classname : imports) {
          String toInsert = "import " + classname + ";" + fileLineSep;
          if (!inSource.contains(toInsert)) {
            inSource.add(toInsert);
            src.insert(importIndex, toInsert);
            importIndex += toInsert.length();
          }
        }
      }

      // Write the source file.
      File outfile = null;
      try {
        if (in_place) {
          outfile = javafile;
          if (verbose) {
            System.out.printf("Renaming %s to %s%n", javafile, unannotated);
          }
          boolean success = javafile.renameTo(unannotated);
          if (!success) {
            throw new Error(String.format("Failed renaming %s to %s", javafile, unannotated));
          }
        } else {
          if (pkg.isEmpty()) {
            outfile = new File(outdir, javafile.getName());
          } else {
            @SuppressWarnings("StringSplitter") // false positive because pkg is non-empty
            String[] pkgPath = pkg.split("\\.");
            StringBuilder sb = new StringBuilder(outdir);
            for (int i = 0; i < pkgPath.length; i++) {
              sb.append(File.separator).append(pkgPath[i]);
            }
            outfile = new File(sb.toString(), javafile.getName());
          }
          outfile.getParentFile().mkdirs();
        }
        try (OutputStream output = new FileOutputStream(outfile)) {
          if (verbose) {
            System.out.printf("Writing %s%n", outfile);
          }
          src.write(output);
        }
      } catch (IOException e) {
        System.err.println("Problem while writing file " + outfile);
        e.printStackTrace();
        System.exit(1);
      }
    }
  }

  /**
   * Given a Java file name, creates a Source, or returns null.
   *
   * @param javaFileName a Java file name
   * @return a Source for the Java file, or null
   */
  private static Source fileToSource(String javaFileName) {
    Source src;
    // Get the source file, and use it to obtain parse trees.
    try {
      src = new Source(javaFileName);
      return src;
    } catch (Source.CompilerException e) {
      e.printStackTrace();
      return null;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Primary sort criterion: put declaration annotations (which go on a separate line) last, so that
   * they *precede* type annotations when inserted.
   *
   * <p>Secondary sort criterion: for determinism, put annotations in reverse alphabetic order (so
   * that they are alphabetized when inserted).
   */
  private static Comparator<Insertion> insertionSorter =
      new Comparator<Insertion>() {
        @Override
        public int compare(Insertion i1, Insertion i2) {
          boolean separateLine1 = i1.isSeparateLine();
          boolean separateLine2 = i2.isSeparateLine();
          if (separateLine1 && !separateLine2) {
            return 1;
          } else if (separateLine2 && !separateLine1) {
            return -1;
          } else {
            return -i1.getText().compareTo(i2.getText());
          }
        }
      };

  /** Maps from binary class name to whether the class has any explicit constructor. */
  public static Map<String, Boolean> hasExplicitConstructor = new HashMap<>();

  /**
   * Fills in the {@link hasExplicitConstructor} map.
   *
   * @param javaFiles the Java files that were passed on the command line
   */
  static void computeConstructors(List<String> javaFiles) {
    for (String javaFile : javaFiles) {
      Source src = fileToSource(javaFile);
      if (src == null) {
        continue;
      }
      for (CompilationUnitTree cut : src.parse()) {
        TreePathScanner<Void, Void> constructorsScanner =
            new TreePathScanner<Void, Void>() {
              @Override
              public Void visitClass(ClassTree ct, Void p) {
                String className = TreePathUtil.getBinaryName(getCurrentPath());
                hasExplicitConstructor.put(className, TreePathUtil.hasConstructor(ct));
                return super.visitClass(ct, null);
              }
            };
        constructorsScanner.scan(cut, null);
      }
    }
  }

  /** A regular expression for classes in the java.lang package. */
  private static Pattern javaLangClassPattern = Pattern.compile("^java\\.lang\\.[A-Za-z0-9_]+$");

  /**
   * Returns true iff the class is a top-level class in the java.lang package.
   *
   * @param classname the class to test
   * @return true iff the class is a top-level class in the java.lang package
   */
  private static boolean isJavaLangClass(String classname) {
    Matcher m = javaLangClassPattern.matcher(classname);
    return m.matches();
  }

  /**
   * Filters out classes in the java.lang package from the given collection.
   *
   * @param classnames a collection of class names
   * @return the class names that are not in the java.lang package
   */
  private static Collection<String> nonJavaLangClasses(Collection<String> classnames) {
    // Don't side-effect the argument
    List<String> result = new ArrayList<>();
    for (String classname : classnames) {
      if (!isJavaLangClass(classname)) {
        result.add(classname);
      }
    }
    return result;
  }

  /**
   * Returns the representation of the leaf of the path.
   *
   * @param path a path whose leaf to format
   * @return the representation of the leaf of the path
   */
  public static String leafString(TreePath path) {
    if (path == null) {
      return "null path";
    }
    return treeToString(path.getLeaf());
  }

  /**
   * Returns the first 80 characters of the tree's printed representation, on one line.
   *
   * @param node a tree to format with truncation
   * @return the first 80 characters of the tree's printed representation, on one line
   */
  public static String treeToString(Tree node) {
    String asString = node.toString();
    String oneLine = first80(asString);
    if (oneLine.endsWith(" ")) {
      oneLine = oneLine.substring(0, oneLine.length() - 1);
    }
    // return "\"" + oneLine + "\"";
    return oneLine;
  }

  /**
   * Returns the first non-empty line of the string, adding an ellipsis (...) if the string was
   * truncated.
   *
   * @param s a string to truncate
   * @return the first non-empty line of the argument
   */
  public static String firstLine(String s) {
    while (s.startsWith("\n")) {
      s = s.substring(1);
    }
    int newlineIndex = s.indexOf('\n');
    if (newlineIndex == -1) {
      return s;
    } else {
      return s.substring(0, newlineIndex) + "...";
    }
  }

  /**
   * Returns the first 80 characters of the string, adding an ellipsis (...) if the string was
   * truncated.
   *
   * @param s a string to truncate
   * @return the first 80 characters of the string
   */
  public static String first80(String s) {
    StringBuilder sb = new StringBuilder();
    int i = 0;
    while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
      i++;
    }
    while (i < s.length() && sb.length() < 80) {
      if (s.charAt(i) == '\n') {
        i++;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
          i++;
        }
        sb.append(' ');
      }
      if (i < s.length()) {
        sb.append(s.charAt(i));
      }
      i++;
    }
    if (i < s.length()) {
      sb.append("...");
    }
    return sb.toString();
  }

  /**
   * Separates the annotation class from its arguments.
   *
   * @param s the string representation of an annotation
   * @return given {@code @foo(bar)} it returns the pair <code>{ @foo, (bar) }</code>
   */
  public static IPair<String, @Nullable String> removeArgs(String s) {
    int pidx = s.indexOf('(');
    return (pidx == -1)
        ? IPair.of(s, (String) null)
        : IPair.of(s.substring(0, pidx), s.substring(pidx));
  }
}
