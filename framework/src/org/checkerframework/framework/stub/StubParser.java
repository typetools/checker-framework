package org.checkerframework.framework.stub;

/*>>>
import org.checkerframework.checker.nullness.qual.*;
*/

import org.checkerframework.framework.qual.FromStubFile;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeMerger;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.stubparser.JavaParser;
import org.checkerframework.stubparser.ast.CompilationUnit;
import org.checkerframework.stubparser.ast.ImportDeclaration;
import org.checkerframework.stubparser.ast.IndexUnit;
import org.checkerframework.stubparser.ast.PackageDeclaration;
import org.checkerframework.stubparser.ast.TypeParameter;
import org.checkerframework.stubparser.ast.body.BodyDeclaration;
import org.checkerframework.stubparser.ast.body.ClassOrInterfaceDeclaration;
import org.checkerframework.stubparser.ast.body.ConstructorDeclaration;
import org.checkerframework.stubparser.ast.body.FieldDeclaration;
import org.checkerframework.stubparser.ast.body.MethodDeclaration;
import org.checkerframework.stubparser.ast.body.Parameter;
import org.checkerframework.stubparser.ast.body.TypeDeclaration;
import org.checkerframework.stubparser.ast.body.VariableDeclarator;
import org.checkerframework.stubparser.ast.expr.AnnotationExpr;
import org.checkerframework.stubparser.ast.expr.ArrayInitializerExpr;
import org.checkerframework.stubparser.ast.expr.BooleanLiteralExpr;
import org.checkerframework.stubparser.ast.expr.Expression;
import org.checkerframework.stubparser.ast.expr.FieldAccessExpr;
import org.checkerframework.stubparser.ast.expr.IntegerLiteralExpr;
import org.checkerframework.stubparser.ast.expr.MarkerAnnotationExpr;
import org.checkerframework.stubparser.ast.expr.MemberValuePair;
import org.checkerframework.stubparser.ast.expr.NameExpr;
import org.checkerframework.stubparser.ast.expr.NormalAnnotationExpr;
import org.checkerframework.stubparser.ast.expr.SingleMemberAnnotationExpr;
import org.checkerframework.stubparser.ast.expr.StringLiteralExpr;
import org.checkerframework.stubparser.ast.type.ClassOrInterfaceType;
import org.checkerframework.stubparser.ast.type.ReferenceType;
import org.checkerframework.stubparser.ast.type.Type;
import org.checkerframework.stubparser.ast.type.WildcardType;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

/**
 * Main entry point is:
 * {@link StubParser#parse(Map, Map)}
 */
// Full entry point signature:
// parse(Map<Element, AnnotatedTypeMirror>, Map<String, Set<AnnotationMirror>>)}
public class StubParser {

    /**
     * Whether to print warnings about types/members that were not found.
     * The warning is about whether a class/field in the stub file is not
     * found on the user's real classpath.  Since the stub file may contain
     * packages that are not on the classpath, this can be OK, so default to
     * false.
     */
    private final boolean warnIfNotFound;

    /**
     * Whether to print warnings about stub files that overwrite annotations
     * from bytecode.
     */
    private final boolean warnIfStubOverwritesBytecode;

    private final boolean debugStubParser;

    /** The file being parsed (makes error messages more informative). */
    private final String filename;

    private final IndexUnit index;
    private final ProcessingEnvironment processingEnv;
    private final AnnotatedTypeFactory atypeFactory;
    private final Elements elements;

    /**
     * The supported annotations. Keys are simple (unqualified) names.
     * (This may be a problem in the unlikely occurrence that a
     * type-checker supports two annotations with the same simple name.)
     */
    private final Map<String, AnnotationMirror> supportedAnnotations;

    /**
     * A list of imports that are not annotation types.
     * Used for importing enums.
     */
    private final List<String> imports;

    /**
     * Mapping of a field access expression that has already been encountered
     * to the resolved variable element.
     */
    private final Map<FieldAccessExpr, VariableElement> faexprcache;

    /**
     * Mapping of a name access expression that has already been encountered
     * to the resolved variable element.
     */
    private final Map<NameExpr, VariableElement> nexprcache;

    /**
     * Annotation to added to every method and constructor in the stub file.
     */
    private final AnnotationMirror fromStubFile;


    /**
     *
     * @param filename name of stub file
     * @param inputStream of stub file to parse
     * @param factory  AnnotatedtypeFactory to use
     * @param env ProcessingEnviroment to use
     */
    public StubParser(String filename, InputStream inputStream,
            AnnotatedTypeFactory factory, ProcessingEnvironment env) {
        this.filename = filename;
        IndexUnit parsedindex;
        try {
            parsedindex = JavaParser.parse(inputStream);
        } catch (Exception e) {
            ErrorReporter.errorAbort("StubParser: exception from JavaParser.parse for file " + filename, e);
            parsedindex = null; // dead code, but needed for def. assignment checks
        }
        this.index = parsedindex;
        this.atypeFactory = factory;
        this.processingEnv = env;
        this.elements = env.getElementUtils();
        imports = new ArrayList<String>();

        // getSupportedAnnotations uses these for warnings
        Map<String, String> options = env.getOptions();
        this.warnIfNotFound = options.containsKey("stubWarnIfNotFound");
        this.warnIfStubOverwritesBytecode= options.containsKey("stubWarnIfOverwritesBytecode");
        this.debugStubParser = options.containsKey("stubDebug");

        // getSupportedAnnotations also sets imports. This should be refactored to be nicer.
        supportedAnnotations = getSupportedAnnotations();
        if (supportedAnnotations.isEmpty()) {
            stubWarnIfNotFound("No supported annotations found! This likely means your stub file doesn't import them correctly.");
        }
        faexprcache = new HashMap<FieldAccessExpr, VariableElement>();
        nexprcache = new HashMap<NameExpr, VariableElement>();

        this.fromStubFile = AnnotationUtils.fromClass(elements, FromStubFile.class);
    }



    /** All annotations defined in the package.  Keys are simple names. */
    private Map<String, AnnotationMirror> annosInPackage(PackageElement packageElement) {
        return createImportedAnnotationsMap(ElementFilter.typesIn(packageElement.getEnclosedElements()));
    }

    /** All annotations declarations nested inside of a class. */
    private Map<String, AnnotationMirror> annosInType(TypeElement typeElement) {
        return createImportedAnnotationsMap(ElementFilter.typesIn(typeElement.getEnclosedElements()));
    }

    private Map<String, AnnotationMirror>  createImportedAnnotationsMap(List<TypeElement> typeElements) {
        Map<String, AnnotationMirror> r = new HashMap<String, AnnotationMirror>();
        for (TypeElement typeElm : typeElements) {
            if (typeElm.getKind() == ElementKind.ANNOTATION_TYPE) {
                AnnotationMirror anno = AnnotationUtils.fromName(elements, typeElm.getQualifiedName());
                putNew(r, typeElm.getSimpleName().toString(), anno);
            }
        }
        return r;
    }

    /**
     * Get all members of a Type that are useful in a stub file.
     * Currently these are values of enums, or compile time constants.
     *
     * @return a list fully qualified member names
     */
    private static List<String> getImportableMembers(TypeElement typeElement) {
        List<String> result = new ArrayList<String>();
        List<VariableElement> memberElements = ElementFilter.fieldsIn(typeElement.getEnclosedElements());
        for (VariableElement varElement : memberElements) {
            if (varElement.getConstantValue() != null
                    || varElement.getKind() == ElementKind.ENUM_CONSTANT) {

                result.add(String.format("%s.%s", typeElement.getQualifiedName().toString(),
                        varElement.getSimpleName().toString()));
            }
        }

        return result;
    }

    /** @see #supportedAnnotations */
    private Map<String, AnnotationMirror> getSupportedAnnotations() {
        assert !index.getCompilationUnits().isEmpty();
        CompilationUnit cu = index.getCompilationUnits().get(0);

        Map<String, AnnotationMirror> result = new HashMap<String, AnnotationMirror>();

        if (cu.getImports() == null) {
            return result;
        }

        for (ImportDeclaration importDecl : cu.getImports()) {
            String imported = importDecl.getName().toString();
            try {
                if (importDecl.isAsterisk()) {
                    // Static determines if we are importing members
                    // of a type (class or interface) or of a package
                    if (importDecl.isStatic()) {
                        // Members of a type (according to JLS)

                        TypeElement element = findType(imported, "Imported type not found");
                        if (element != null) {
                            // Find nested annotations
                            // Find compile time constant fields, or values of an enum
                            putAllNew(result, annosInType(element));
                            imports.addAll(getImportableMembers(element));
                        }
                    } else {
                        // Members of a package (according to JLS)

                        PackageElement element = findPackage(imported);
                        if (element != null) {
                            putAllNew(result, annosInPackage(element));
                        }
                    }
                } else {
                    // Process a single import

                    final TypeElement importType = elements.getTypeElement(imported);
                    if (importType == null && !importDecl.isStatic()) {
                        // Class or nested class (according to JSL), but we can't resolve

                        stubWarnIfNotFound("Imported type not found: " + imported);
                    } else if (importType == null) {
                        // Nested Field

                        Pair<String, String> typeParts = StubUtil.partitionQualifiedName(imported);
                        String type = typeParts.first;
                        String fieldName = typeParts.second;
                        TypeElement enclType = findType(type,
                                String.format("Enclosing type of static field %s not found", fieldName));

                        if (enclType != null) {
                            if (findFieldElement(enclType, fieldName) != null) {
                                imports.add(imported);
                            }
                        }

                    } else if (importType.getKind() == ElementKind.ANNOTATION_TYPE) {
                        // Single annotation or nested annotation

                        AnnotationMirror anno = AnnotationUtils.fromName(elements, imported);
                        if (anno != null) {
                            Element annoElt = anno.getAnnotationType().asElement();
                            putNew(result, annoElt.getSimpleName().toString(), anno);
                        } else {
                            stubWarnIfNotFound("Could not load import: " + imported);
                        }
                    } else {
                        // Class or nested class

                        imports.add(imported);
                    }
                }
            } catch (AssertionError error) {
                stubWarnIfNotFound("" + error);
            }
        }
        return result;
    }

    // The main entry point.  Side-effects the arguments.
    public void parse(Map<Element, AnnotatedTypeMirror> atypes, Map<String, Set<AnnotationMirror>> declAnnos) {
        parse(this.index, atypes, declAnnos);
    }

    private void parse(IndexUnit index,
            Map<Element, AnnotatedTypeMirror> atypes, Map<String, Set<AnnotationMirror>> declAnnos) {
        for (CompilationUnit cu : index.getCompilationUnits()) {
            parse(cu, atypes, declAnnos);
        }
    }

    private CompilationUnit theCompilationUnit;

    private void parse(CompilationUnit cu,
            Map<Element, AnnotatedTypeMirror> atypes, Map<String, Set<AnnotationMirror>> declAnnos) {
        theCompilationUnit = cu;
        final String packageName;
        final List<AnnotationExpr> packageAnnos;

        if (cu.getPackage() == null) {
            packageName = null;
            packageAnnos = null;
        } else {
            packageName = cu.getPackage().getName().toString();
            packageAnnos = cu.getPackage().getAnnotations();
            parsePackage(cu.getPackage(), atypes, declAnnos);
        }
        if (cu.getTypes() != null) {
            for (TypeDeclaration typeDecl : cu.getTypes()) {
                parse(typeDecl, packageName, packageAnnos, atypes, declAnnos);
            }
        }
    }

    private void parsePackage(PackageDeclaration packDecl,
            Map<Element, AnnotatedTypeMirror> atypes, Map<String, Set<AnnotationMirror>> declAnnos) {
        assert(packDecl != null);
        String packageName = packDecl.getName().toString();
        Element elem = elements.getPackageElement(packageName);
        // If the element lookup fails, it's because we have an annotation for a
        // package that isn't on the classpath, which is fine.
        if (elem != null) {
            annotateDecl(declAnnos, elem, packDecl.getAnnotations());
        }
        // TODO: Handle atypes???
    }

    // typeDecl's name may be a binary name such as "A$B".
    // That is a hack because the StubParser does not handle nested classes.
    private void parse(TypeDeclaration typeDecl,
            String packageName, List<AnnotationExpr> packageAnnos,
            Map<Element, AnnotatedTypeMirror> atypes,
            Map<String, Set<AnnotationMirror>> declAnnos) {
        // Fully-qualified name of the type being parsed
        String typeName = (packageName == null ? "" : packageName + ".") + typeDecl.getName().replace('$', '.');
        TypeElement typeElt = elements.getTypeElement(typeName);
        // couldn't find type.  not in class path
        if (typeElt == null) {
            boolean warn = true;
            if (typeDecl.getAnnotations() != null) {
                for (AnnotationExpr anno : typeDecl.getAnnotations()) {
                    if (anno.getName().getName().contentEquals("NoStubParserWarning")) {
                        warn = false;
                    }
                }
            }
            if (packageAnnos != null) {
                for (AnnotationExpr anno : packageAnnos) {
                    if (anno.getName().getName().contentEquals("NoStubParserWarning")) {
                        warn = false;
                    }
                }
            }
            warn = warn || debugStubParser;
            if (warn) {
                stubWarnIfNotFound("Type not found: " + typeName);
            }
            return;
        }

        if (typeElt.getKind() == ElementKind.ENUM) {
            stubWarnIfNotFound("Skipping enum type: " + typeName);
        } else if (typeElt.getKind() == ElementKind.ANNOTATION_TYPE) {
            stubWarnIfNotFound("Skipping annotation type: " + typeName);
        } else if (typeDecl instanceof ClassOrInterfaceDeclaration) {
            parseType((ClassOrInterfaceDeclaration)typeDecl, typeElt, atypes, declAnnos);
        } // else it's an EmptyTypeDeclaration.  TODO:  An EmptyTypeDeclaration can have annotations, right?

        Map<Element, BodyDeclaration> elementsToDecl = getMembers(typeElt, typeDecl);
        for (Map.Entry<Element, BodyDeclaration> entry : elementsToDecl.entrySet()) {
            final Element elt = entry.getKey();
            final BodyDeclaration decl = entry.getValue();
            if (elt.getKind().isField()) {
                parseField((FieldDeclaration)decl, (VariableElement)elt, atypes, declAnnos);
            } else if (elt.getKind() == ElementKind.CONSTRUCTOR) {
                parseConstructor((ConstructorDeclaration)decl, (ExecutableElement)elt, atypes, declAnnos);
            } else if (elt.getKind() == ElementKind.METHOD) {
                parseMethod((MethodDeclaration)decl, (ExecutableElement)elt, atypes, declAnnos);
            } else {
                /* do nothing */
                stubWarnIfNotFound("StubParser ignoring: " + elt);
            }
        }
    }

    private void parseType(ClassOrInterfaceDeclaration decl,
            TypeElement elt, Map<Element, AnnotatedTypeMirror> atypes,
            Map<String, Set<AnnotationMirror>> declAnnos) {
        annotateDecl(declAnnos, elt, decl.getAnnotations());
        AnnotatedDeclaredType type = atypeFactory.fromElement(elt);
        annotate(type, decl.getAnnotations());

        final List<? extends AnnotatedTypeMirror> typeArguments = type.getTypeArguments();
        final List<TypeParameter> typeParameters = decl.getTypeParameters();

        // It can be the case that args=[] and params=null.
        // if ((typeParameters == null) != (typeArguments == null)) {
        //     throw new Error(String.format("parseType (%s, %s): inconsistent nullness for args and params%n  args = %s%n  params = %s%n", decl, elt, typeArguments, typeParameters));
        // }

        if ((typeParameters == null) && (typeArguments.size() != 0)) {
            // TODO: Class EventListenerProxy in Java 6 does not have type parameters, but in Java 7 does.
            // To handle both with one specification, we currently ignore the problem.
            // Investigate what a cleaner solution is, e.g. having a separate Java 7 specification that overrides
            // the Java 6 specification.
            // System.out.printf("Dying.  theCompilationUnit=%s%n", theCompilationUnit);
            if (debugStubParser) {
                stubDebug(String.format("parseType:  mismatched sizes for params and args%n  decl=%s%n  typeParameters=%s%n  elt=%s (%s)%n  type=%s (%s)%n  typeArguments (size %d)=%s%n  theCompilationUnit=%s%nEnd of message for parseType:  mismatched sizes for params and args%n",
                        decl, typeParameters,
                        elt, elt.getClass(), type, type.getClass(), typeArguments.size(), typeArguments,
                        theCompilationUnit));
            }
            /*
                throw new Error(String.format("parseType:  mismatched sizes for params and args%n  decl=%s%n  typeParameters=%s%n  elt=%s (%s)%n  type=%s (%s)%n  typeArguments (size %d)=%s%n",
                                          decl, typeParameters,
                                          elt, elt.getClass(), type, type.getClass(), typeArguments.size(), typeArguments));
             */
        }

        if ((typeParameters != null) && (typeParameters.size() != typeArguments.size())) {
            // TODO: decide how severe this problem really is; see comment above.
            // System.out.printf("Dying.  theCompilationUnit=%s%n", theCompilationUnit);
            if (debugStubParser) {
                stubDebug(String.format("parseType:  mismatched sizes for params and args%n  decl=%s%n  typeParameters (size %d)=%s%n  elt=%s (%s)%n  type=%s (%s)%n  typeArguments (size %d)=%s%n  theCompilationUnit=%s%nEnd of message for parseType:  mismatched sizes for params and args%n",
                        decl, typeParameters.size(), typeParameters,
                        elt, elt.getClass(), type, type.getClass(), typeArguments.size(), typeArguments,
                        theCompilationUnit));
            }
            /*
                throw new Error(String.format("parseType:  mismatched sizes for params and args%n  decl=%s%n  typeParameters (size %d)=%s%n  elt=%s (%s)%n  type=%s (%s)%n  typeArguments (size %d)=%s%n",
                                          decl, typeParameters.size(), typeParameters,
                                          elt, elt.getClass(), type, type.getClass(), typeArguments.size(), typeArguments));
             */
        }

        annotateParameters(typeArguments, typeParameters);
        annotateSupertypes(decl, type);
        putNew(atypes, elt, type);
    }

    private void annotateSupertypes(ClassOrInterfaceDeclaration typeDecl, AnnotatedDeclaredType type) {
        if (typeDecl.getExtends() != null) {
            for (ClassOrInterfaceType superType : typeDecl.getExtends()) {
                AnnotatedDeclaredType foundType = findType(superType, type.directSuperTypes());
                assert foundType != null : "StubParser: could not find superclass " + superType + " from type " + type;
                if (foundType != null) {
                    annotate(foundType, superType);
                }
            }
        }
        if (typeDecl.getImplements() != null) {
            for (ClassOrInterfaceType superType : typeDecl.getImplements()) {
                AnnotatedDeclaredType foundType = findType(superType, type.directSuperTypes());
                // TODO: Java 7 added a few AutoCloseable superinterfaces to classes.
                // We specify those as superinterfaces in the jdk.astub file. Let's ignore
                // this addition to be compatible with Java 6.
                assert foundType != null || (superType.toString().equals("AutoCloseable") || superType.toString().equals("java.io.Closeable") || superType.toString().equals("Closeable")) :
                    "StubParser: could not find superinterface " + superType + " from type " + type;
                if (foundType != null) {
                    annotate(foundType, superType);
                }
            }
        }
    }

    private void parseMethod(MethodDeclaration decl,
            ExecutableElement elt, Map<Element, AnnotatedTypeMirror> atypes,
            Map<String, Set<AnnotationMirror>> declAnnos) {
        annotateDecl(declAnnos, elt, decl.getAnnotations());
        // StubParser parses all annotations in type annotation position as type annotations
        annotateDecl(declAnnos, elt, decl.getType().getAnnotations());
        addDeclAnnotations(declAnnos, elt);

        AnnotatedExecutableType methodType = atypeFactory.fromElement(elt);
        annotateParameters(methodType.getTypeVariables(), decl.getTypeParameters());
        annotate(methodType.getReturnType(), decl.getType());

        List<Parameter> params = decl.getParameters();
        List<? extends VariableElement> paramElts = elt.getParameters();
        List<? extends AnnotatedTypeMirror> paramTypes = methodType.getParameterTypes();

        for (int i = 0; i < methodType.getParameterTypes().size(); ++i) {
            VariableElement paramElt = paramElts.get(i);
            AnnotatedTypeMirror paramType = paramTypes.get(i);
            Parameter param = params.get(i);

            annotateDecl(declAnnos, paramElt, param.getAnnotations());
            annotateDecl(declAnnos, paramElt, param.getType().getAnnotations());

            if (param.isVarArgs()) {
                // workaround
                assert paramType.getKind() == TypeKind.ARRAY;
                annotate(((AnnotatedArrayType)paramType).getComponentType(), param.getType());
            } else {
                annotate(paramType, param.getType());
            }
        }

        if (methodType.getReceiverType() == null &&
                decl.getReceiverAnnotations() != null &&
                !decl.getReceiverAnnotations().isEmpty()) {
            stubAlwaysWarn(String.format("parseMethod: static methods cannot have receiver annotations%n" +
                "Method: %s%n" +
                "Receiver annotations: %s", methodType, decl.getReceiverAnnotations()));
        } else {
            annotate(methodType.getReceiverType(), decl.getReceiverAnnotations());
        }

        putNew(atypes, elt, methodType);
    }

    /**
     * Handle existing annotations on the type. Stub files should override the existing
     * annotations on a type. Using {@code replaceAnnotation} is usually good enough
     * to achieve this; however, for annotations on type variables, the stub file sometimes
     * needs to be able to remove an existing annotation, leaving no annotation on
     * the type variable. This method achieves this by calling {@code clearAnnotations}.
     *
     * @param atype The type to modify.
     * @param typeDef The type from the stub file, for warnings.
     */
    private void handleExistingAnnotations(AnnotatedTypeMirror atype, Type typeDef) {
        Set<AnnotationMirror> annos = atype.getAnnotations();
        if (annos != null &&
                !annos.isEmpty() &&
                !"flow.astub".equals(filename)) {
            // TODO: instead of comparison against flow.astub, this should
            // check whether the stub file is @AnnotatedFor the current type system.
            // flow.astub isn't annotated for any particular type system, so let's
            // not warn for now, as @AnnotatedFor isn't integrated in stub files yet.
            stubWarnIfOverwritesBytecode(
                    String.format("in file %s at line %s ignored existing annotations on type: %s%n",
                            filename.substring(filename.lastIndexOf('/') + 1), typeDef.getBeginLine(),
                            atype.toString(true)));
            // TODO: filename is the simple "jdk.astub" and "flow.astub" for those pre-defined files,
            // without complete path, but the full path in other situations.
            // All invocations should provide the short path or the full path.
            // For testing it is easier if only the file name is used.

            // Clear existing annotations, which only makes a difference for
            // type variables, but doesn't hurt in other cases.
            atype.clearAnnotations();
        }
    }

    /**
     * Adds a declAnnotation to every method in the stub file.
     */
    private void addDeclAnnotations(
            Map<String, Set<AnnotationMirror>> declAnnos, Element elt) {
        if (fromStubFile != null) {
            Set<AnnotationMirror> annos = declAnnos.get(ElementUtils
                    .getVerboseName(elt));
            if (annos == null) {
                annos = AnnotationUtils.createAnnotationSet();
                putOrAddToMap(declAnnos, ElementUtils.getVerboseName(elt), annos);
            }
            annos.add(fromStubFile);
        }
    }

    /**
     * List of all array component types.
     * Example input: int[][]
     * Example output: int, int[], int[][]
     */
    private List<AnnotatedTypeMirror> arrayAllComponents(AnnotatedArrayType atype) {
        LinkedList<AnnotatedTypeMirror> arrays = new LinkedList<AnnotatedTypeMirror>();

        AnnotatedTypeMirror type = atype;
        while (type.getKind() == TypeKind.ARRAY) {
            arrays.addFirst(type);

            type = ((AnnotatedArrayType)type).getComponentType();
        }

        arrays.add(type);
        return arrays;
    }

    private void annotateAsArray(AnnotatedArrayType atype, ReferenceType typeDef) {
        List<AnnotatedTypeMirror> arrayTypes = arrayAllComponents(atype);
        assert typeDef.getArrayCount() == arrayTypes.size() - 1 ||
                // We want to allow simply using "Object" as return type of a
                // method, regardless of what the real type is.
                typeDef.getArrayCount() == 0 :
            "Mismatched array lengths; typeDef: " + typeDef.getArrayCount() +
            " vs. arrayTypes: " + (arrayTypes.size() - 1) +
                    "\n  typedef: " + typeDef + "\n  arraytypes: " + arrayTypes;
        /* Separate TODO: the check for zero above ensures that "Object" can be
         * used as return type, even when the real method uses something else.
         * However, why was this needed for the RequiredPermissions declaration annotation?
         * It looks like the StubParser ignored the target for annotations.
         */
        for (int i = 0; i < typeDef.getArrayCount(); ++i) {
            List<AnnotationExpr> annotations = typeDef.getAnnotationsAtLevel(i);
            handleExistingAnnotations(arrayTypes.get(i), typeDef);

            if (annotations != null) {
                annotate(arrayTypes.get(i), annotations);
            }
        }

        // handle generic type on base
        handleExistingAnnotations(arrayTypes.get(arrayTypes.size() - 1), typeDef);
        annotate(arrayTypes.get(arrayTypes.size() - 1), typeDef.getAnnotations());
    }

    private ClassOrInterfaceType unwrapDeclaredType(Type type) {
        if (type instanceof ClassOrInterfaceType) {
            return (ClassOrInterfaceType)type;
        } else if (type instanceof ReferenceType
                && ((ReferenceType)type).getArrayCount() == 0) {
            return unwrapDeclaredType(((ReferenceType)type).getType());
        } else {
            return null;
        }
    }

    private void annotate(AnnotatedTypeMirror atype, Type typeDef) {
        if (atype.getKind() == TypeKind.ARRAY) {
            annotateAsArray((AnnotatedArrayType)atype, (ReferenceType)typeDef);
            return;
        }

        handleExistingAnnotations(atype, typeDef);

        if (typeDef.getAnnotations() != null) {
            annotate(atype, typeDef.getAnnotations());
        }
        ClassOrInterfaceType declType = unwrapDeclaredType(typeDef);
        if (atype.getKind() == TypeKind.DECLARED
                && declType != null) {
            AnnotatedDeclaredType adeclType = (AnnotatedDeclaredType)atype;
            if (declType.getTypeArgs() != null
                    && !declType.getTypeArgs().isEmpty()
                    && !adeclType.getTypeArguments().isEmpty()) {
                assert declType.getTypeArgs().size() == adeclType.getTypeArguments().size() : String.format(
                        "Mismatch in type argument size between %s (%d) and %s (%d)", declType,
                        declType.getTypeArgs().size(), adeclType, adeclType.getTypeArguments().size());
                for (int i = 0; i < declType.getTypeArgs().size(); ++i) {
                    annotate(adeclType.getTypeArguments().get(i),
                            declType.getTypeArgs().get(i));
                }
            }
        } else if (atype.getKind() == TypeKind.WILDCARD) {
            AnnotatedWildcardType wildcardType = (AnnotatedWildcardType)atype;
            WildcardType wildcardDef = (WildcardType)typeDef;
            if (wildcardDef.getExtends() != null) {
                annotate(wildcardType.getExtendsBound(), wildcardDef.getExtends());
            } else if (wildcardDef.getSuper() != null) {
                annotate(wildcardType.getSuperBound(), wildcardDef.getSuper());
            }
        }
    }

    private void parseConstructor(ConstructorDeclaration decl,
            ExecutableElement elt, Map<Element, AnnotatedTypeMirror> atypes,
            Map<String, Set<AnnotationMirror>> declAnnos) {
        annotateDecl(declAnnos, elt, decl.getAnnotations());
        AnnotatedExecutableType methodType = atypeFactory.fromElement(elt);
        addDeclAnnotations(declAnnos, elt);
        annotate(methodType.getReturnType(), decl.getAnnotations());

        for (int i = 0; i < methodType.getParameterTypes().size(); ++i) {
            AnnotatedTypeMirror paramType = methodType.getParameterTypes().get(i);
            Parameter param = decl.getParameters().get(i);
            annotate(paramType, param.getType());
        }

        if (methodType.getReceiverType() == null &&
                decl.getReceiverAnnotations() != null &&
                !decl.getReceiverAnnotations().isEmpty()) {
            stubAlwaysWarn(String.format("parseConstructor: constructor of a top-level class cannot have receiver annotations%n" +
                "Constructor: %s%n" +
                "Receiver annotations: %s", methodType, decl.getReceiverAnnotations()));
        } else {
            annotate(methodType.getReceiverType(), decl.getReceiverAnnotations());
        }

        putNew(atypes, elt, methodType);
    }

    private void parseField(FieldDeclaration decl,
            VariableElement elt, Map<Element, AnnotatedTypeMirror> atypes,
            Map<String, Set<AnnotationMirror>> declAnnos) {
        addDeclAnnotations(declAnnos, elt);
        annotateDecl(declAnnos, elt, decl.getAnnotations());
        // StubParser parses all annotations in type annotation position as type annotations
        annotateDecl(declAnnos, elt, decl.getType().getAnnotations());
        AnnotatedTypeMirror fieldType = atypeFactory.fromElement(elt);
        annotate(fieldType, decl.getType());
        putNew(atypes, elt, fieldType);
    }

    private void annotate(AnnotatedTypeMirror type, List<AnnotationExpr> annotations) {
        if (annotations == null) {
            return;
        }
        for (AnnotationExpr annotation : annotations) {
            AnnotationMirror annoMirror = getAnnotation(annotation, supportedAnnotations);
            if (annoMirror != null) {
                type.replaceAnnotation(annoMirror);
            }
        }
    }

    private void annotateDecl(Map<String, Set<AnnotationMirror>> declAnnos, Element elt,
            List<AnnotationExpr> annotations) {
        if (annotations == null) {
            return;
        }
        Set<AnnotationMirror> annos = AnnotationUtils.createAnnotationSet();
        for (AnnotationExpr annotation : annotations) {
            AnnotationMirror annoMirror = getAnnotation(annotation, supportedAnnotations);
            if (annoMirror != null) {
                annos.add(annoMirror);
            }
        }
        String key = ElementUtils.getVerboseName(elt);
        putOrAddToMap(declAnnos, key, annos);
    }

    private void annotateParameters(List<? extends AnnotatedTypeMirror> typeArguments,
            List<TypeParameter> typeParameters) {
        if (typeParameters == null) {
            return;
        }

        if (typeParameters.size() != typeArguments.size()) {
            stubAlwaysWarn(String.format("annotateParameters: mismatched sizes%n  typeParameters (size %d)=%s%n  typeArguments (size %d)=%s%n  For more details, run with -AstubDebug%n",
                            typeParameters.size(), typeParameters,
                            typeArguments.size(), typeArguments));
        }
        for (int i = 0; i < typeParameters.size(); ++i) {
            TypeParameter param = typeParameters.get(i);
            AnnotatedTypeVariable paramType = (AnnotatedTypeVariable)typeArguments.get(i);

            annotate(paramType, param.getAnnotations());
            if (param.getTypeBound() != null && param.getTypeBound().size() == 1) {
                annotate(paramType.getUpperBound(), param.getTypeBound().get(0));
            }
        }
    }

    private static final Set<String> nestedClassWarnings = new HashSet<String>();

    private Map<Element, BodyDeclaration> getMembers(TypeElement typeElt, TypeDeclaration typeDecl) {
        assert (typeElt.getSimpleName().contentEquals(typeDecl.getName())
                || typeDecl.getName().endsWith("$" + typeElt.getSimpleName().toString()))
            : String.format("%s  %s", typeElt.getSimpleName(), typeDecl.getName());

        Map<Element, BodyDeclaration> result = new HashMap<>();

        for (BodyDeclaration member : typeDecl.getMembers()) {
            if (member instanceof MethodDeclaration) {
                Element elt = findElement(typeElt, (MethodDeclaration)member);
                if (elt != null) {
                    putNew(result, elt, member);
                }
            } else if (member instanceof ConstructorDeclaration) {
                Element elt = findElement(typeElt, (ConstructorDeclaration)member);
                if (elt != null) {
                    putNew(result, elt, member);
                }
            } else if (member instanceof FieldDeclaration) {
                FieldDeclaration fieldDecl = (FieldDeclaration)member;
                for (VariableDeclarator var : fieldDecl.getVariables()) {
                    Element varelt = findElement(typeElt, var);
                    if (varelt != null) {
                        putNew(result, varelt, fieldDecl);
                    }
                }
            } else if (member instanceof ClassOrInterfaceDeclaration) {
                // TODO: handle nested classes
                ClassOrInterfaceDeclaration ciDecl = (ClassOrInterfaceDeclaration) member;
                String nestedClass = typeDecl.getName() + "." + ciDecl.getName();
                if (nestedClassWarnings.add(nestedClass)) { // avoid duplicate warnings
                    stubAlwaysWarn(
                            String.format("Warning: ignoring nested class in %s at line %d:%n    class %s { class %s { ... } }%n",
                                    filename, ciDecl.getBeginLine(),
                                    typeDecl.getName(), ciDecl.getName())
                            + "\n"
                            + String.format(
                                    "  Instead, write the nested class as a top-level class:%n    class %s { ... }%n    class %s$%s { ... }%n",
                                    typeDecl.getName(), typeDecl.getName(),
                                    ciDecl.getName()));
                }
            } else {
                stubWarnIfNotFound(String.format("Ignoring element of type %s in getMembers", member.getClass()));
            }
        }
        // // remove null keys, which can result from findElement returning null
        // result.remove(null);
        return result;
    }

    private AnnotatedDeclaredType findType(ClassOrInterfaceType type, List<AnnotatedDeclaredType> types) {
        String typeString = type.getName();
        for (AnnotatedDeclaredType superType : types) {
            if (superType.getUnderlyingType().asElement().getSimpleName().contentEquals(typeString)) {
                return superType;
            }
        }
        stubWarnIfNotFound("Type " + typeString + " not found");
        if (debugStubParser) {
            for (AnnotatedDeclaredType superType : types) {
                stubDebug(String.format("  %s%n", superType));
            }
        }
        return null;
    }

    private ExecutableElement findElement(TypeElement typeElt, MethodDeclaration methodDecl) {
        final String wantedMethodName = methodDecl.getName();
        final int wantedMethodParams =
            (methodDecl.getParameters() == null) ? 0 :
                methodDecl.getParameters().size();
        final String wantedMethodString = StubUtil.toString(methodDecl);
        for (ExecutableElement method : ElementUtils.getAllMethodsIn(elements, typeElt)) {
            // do heuristics first
            if (wantedMethodParams == method.getParameters().size()
                && wantedMethodName.contentEquals(method.getSimpleName())
                && StubUtil.toString(method).equals(wantedMethodString)) {
                return method;
            }
        }
        stubWarnIfNotFound("Method " + wantedMethodString + " not found in type " + typeElt);
        if (debugStubParser) {
            for (ExecutableElement method : ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
                stubDebug(String.format("  %s%n", method));
            }
        }
        return null;
    }

    private ExecutableElement findElement(TypeElement typeElt, ConstructorDeclaration methodDecl) {
        final int wantedMethodParams =
            (methodDecl.getParameters() == null) ? 0 :
                methodDecl.getParameters().size();
        final String wantedMethodString = StubUtil.toString(methodDecl);
        for (ExecutableElement method : ElementFilter.constructorsIn(typeElt.getEnclosedElements())) {
            // do heuristics first
            if (wantedMethodParams == method.getParameters().size() &&
                    StubUtil.toString(method).equals(wantedMethodString)) {
                return method;
            }
        }

        stubWarnIfNotFound("Constructor " + wantedMethodString + " not found in type " + typeElt);
        if (debugStubParser) {
            for (ExecutableElement method : ElementFilter.constructorsIn(typeElt.getEnclosedElements())) {
                stubDebug(String.format("  %s%n", method));
            }
        }
        return null;
    }

    private VariableElement findElement(TypeElement typeElt, VariableDeclarator variable) {
        final String fieldName = variable.getId().getName();
        return findFieldElement(typeElt, fieldName);
    }

    private VariableElement findFieldElement(TypeElement typeElt, String fieldName) {
        for (VariableElement field : ElementUtils.getAllFieldsIn(elements, typeElt)) {
            // field.getSimpleName() is a CharSequence, not a String
            if (fieldName.equals(field.getSimpleName().toString())) {
                return field;
            }
        }

        stubWarnIfNotFound("Field " + fieldName + " not found in type " + typeElt);
        if (debugStubParser) {
            for (VariableElement field : ElementFilter.fieldsIn(typeElt.getEnclosedElements())) {
                stubDebug(String.format("  %s%n", field));
            }
        }
        return null;
    }

    private TypeElement findType(String typeName, String... msg) {
        TypeElement classElement = elements.getTypeElement(typeName);
        if (classElement == null) {
            if (msg.length == 0) {
                stubWarnIfNotFound("Type not found: " + typeName);
            } else {
                stubWarnIfNotFound(msg[0] + ": " + typeName);
            }
        }
        return classElement;
    }

    private PackageElement findPackage(String packageName) {
        PackageElement packageElement = elements.getPackageElement(packageName);
        if (packageElement == null) {
            stubWarnIfNotFound("Imported package not found: " + packageName);
        }
        return packageElement;
    }

    /** The line separator */
    private final static String LINE_SEPARATOR = System.getProperty("line.separator").intern();

    /** Just like Map.put, but errs if the key is already in the map. */
    private static <K,V> void putNew(Map<K,V> m, K key, V value) {
        if (key == null) {
            ErrorReporter.errorAbort("StubParser: key is null!");
            return;
        }
        if (m.containsKey(key) && !m.get(key).equals(value)) {
            ErrorReporter.errorAbort("StubParser: key is already in map: " + LINE_SEPARATOR
                            + "  " + key + " => " + m.get(key) + LINE_SEPARATOR
                            + "while adding: " + LINE_SEPARATOR
                            + "  " + key + " => " + value);
        }
        m.put(key, value);
    }

    /**
     * If the key is already in the map, then add the annos to the list.
     * Otherwise put the key and the annos in the map
     */
    private static void putOrAddToMap(Map<String, Set<AnnotationMirror>> map,
            String key, Set<AnnotationMirror> annos) {
        if (map.containsKey(key)) {
            map.get(key).addAll(annos);
        } else {
            map.put(key, annos);
        }
    }

    /** Just like Map.put, but does not throw an error if the key with the same value is already in the map. */
    private static void putNew(Map<Element, AnnotatedTypeMirror> m, Element key, AnnotatedTypeMirror value) {
        if (key == null) {
            ErrorReporter.errorAbort("StubParser: key is null!");
            return;
        }
        if (m.containsKey(key)) {
            AnnotatedTypeMirror value2 = m.get(key);
            AnnotatedTypeMerger.merge(value, value2);
            m.put(key, value2);
        } else {
            m.put(key, value);
        }
    }


    /** Just like Map.putAll, but errs if any key is already in the map. */
    private static <K,V> void putAllNew(Map<K,V> m, Map<K,V> m2) {
        for (Map.Entry<K,V> e2 : m2.entrySet()) {
            putNew(m, e2.getKey(), e2.getValue());
        }
    }

    private static Set<String> warnings = new HashSet<String>();

    /**
     * Issues the given warning about missing elements, only if it has not
     * been previously issued.
     */
    private void stubWarnIfNotFound(String warning) {
        if (warnings.add(warning) && (warnIfNotFound || debugStubParser)) {
            processingEnv.getMessager().printMessage(
                    javax.tools.Diagnostic.Kind.WARNING,
                    "StubParser: " + warning);
        }
    }

    /**
     * Issues the given warning about overwriting bytecode, only if it has not
     * been previously issued.
     */
    private void stubWarnIfOverwritesBytecode(String warning) {
        if (warnings.add(warning) && (warnIfStubOverwritesBytecode || debugStubParser)) {
            processingEnv.getMessager().printMessage(
                    javax.tools.Diagnostic.Kind.WARNING,
                    "StubParser: " + warning);
        }
    }

    /**
     * Issues a warning even if -AstubWarnIfNotFound or -AstubDebugs options are not passed.
     */
    private void stubAlwaysWarn(String warning) {
        if (warnings.add(warning)) {
            processingEnv.getMessager().printMessage(
                    javax.tools.Diagnostic.Kind.WARNING,
                    "StubParser: " + warning);
        }
    }


    private void stubDebug(String warning) {
        if (warnings.add(warning) && debugStubParser) {
            processingEnv.getMessager().printMessage(
                    javax.tools.Diagnostic.Kind.NOTE,
                    "StubParser: " + warning);
        }
    }

    private AnnotationMirror getAnnotation(AnnotationExpr annotation,
            Map<String, AnnotationMirror> supportedAnnotations) {
        AnnotationMirror annoMirror;
        if (annotation instanceof MarkerAnnotationExpr) {
            String annoName = ((MarkerAnnotationExpr)annotation).getName().getName();
            annoMirror = supportedAnnotations.get(annoName);
        } else if (annotation instanceof NormalAnnotationExpr) {
            NormalAnnotationExpr nrmanno = (NormalAnnotationExpr)annotation;
            String annoName = nrmanno.getName().getName();
            annoMirror = supportedAnnotations.get(annoName);
            if (annoMirror == null) {
                // Not a supported qualifier -> ignore
                return null;
            }
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv, annoMirror);
            List<MemberValuePair> pairs = nrmanno.getPairs();
            if (pairs != null) {
                for (MemberValuePair mvp : pairs) {
                    String meth = mvp.getName();
                    Expression exp = mvp.getValue();
                    handleExpr(builder, meth, exp);
                }
            }
            return builder.build();
        } else if (annotation instanceof SingleMemberAnnotationExpr) {
            SingleMemberAnnotationExpr sglanno = (SingleMemberAnnotationExpr)annotation;
            String annoName = sglanno.getName().getName();
            annoMirror = supportedAnnotations.get(annoName);
            if (annoMirror == null) {
                // Not a supported qualifier -> ignore
                return null;
            }
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv, annoMirror);
            Expression valexpr = sglanno.getMemberValue();
            handleExpr(builder, "value", valexpr);
            return builder.build();
        } else {
            ErrorReporter.errorAbort("StubParser: unknown annotation type: " + annotation);
            annoMirror = null; // dead code
        }
        return annoMirror;
    }

    /*
     * Handles expressions in annotations.
     * Supports String, int, and boolean literals, but not other literals
     * as documented in the stub file limitation section of the manual.
     */
    private void handleExpr(AnnotationBuilder builder, String name,
            Expression expr) {
        if (expr instanceof FieldAccessExpr || expr instanceof NameExpr) {
            VariableElement elem;
            if (expr instanceof FieldAccessExpr) {
                elem = findVariableElement((FieldAccessExpr)expr);
            } else {
                elem = findVariableElement((NameExpr)expr);
            }

            if (elem == null) {
                // A warning was already issued by findVariableElement;
                return;
            }

            ExecutableElement var = builder.findElement(name);
            TypeMirror expected = var.getReturnType();
            if (expected.getKind() == TypeKind.DECLARED) {
                if (elem.getConstantValue() != null) {
                    builder.setValue(name, (String) elem.getConstantValue());
                } else {
                    builder.setValue(name, elem);
                }
            } else if (expected.getKind() == TypeKind.ARRAY) {
                if (elem.getConstantValue() != null) {
                    String[] arr = { (String) elem.getConstantValue() };
                    builder.setValue(name, arr);
                } else {
                    VariableElement[] arr = { elem };
                    builder.setValue(name, arr);
                }
            } else {
                ErrorReporter.errorAbort("StubParser: unhandled annotation attribute type: " + expr +
                        " and expected: " + expected);
            }
        } else if (expr instanceof IntegerLiteralExpr) {
            IntegerLiteralExpr ilexpr = (IntegerLiteralExpr) expr;
            ExecutableElement var = builder.findElement(name);
            TypeMirror expected = var.getReturnType();
            if (expected.getKind() == TypeKind.DECLARED) {
                builder.setValue(name, Integer.valueOf(ilexpr.getValue()));
            } else if (expected.getKind() == TypeKind.ARRAY) {
                Integer[] arr = { Integer.valueOf(ilexpr.getValue()) };
                builder.setValue(name, arr);
            } else {
                ErrorReporter.errorAbort("StubParser: unhandled annotation attribute type: " + ilexpr +
                        " and expected: " + expected);
            }
        } else if (expr instanceof StringLiteralExpr) {
            StringLiteralExpr slexpr = (StringLiteralExpr) expr;
            ExecutableElement var = builder.findElement(name);
            TypeMirror expected = var.getReturnType();
            if (expected.getKind() == TypeKind.DECLARED) {
                builder.setValue(name, slexpr.getValue());
            } else if (expected.getKind() == TypeKind.ARRAY) {
                String[] arr = { slexpr.getValue() };
                builder.setValue(name, arr);
            } else {
                ErrorReporter.errorAbort("StubParser: unhandled annotation attribute type: " + slexpr +
                        " and expected: " + expected);
            }
        } else if (expr instanceof ArrayInitializerExpr) {
            ExecutableElement var = builder.findElement(name);
            TypeMirror expected = var.getReturnType();
            if (expected.getKind() != TypeKind.ARRAY) {
                ErrorReporter.errorAbort("StubParser: unhandled annotation attribute type: " + expr +
                        " and expected: " + expected);
            }

            ArrayInitializerExpr aiexpr = (ArrayInitializerExpr) expr;
            List<Expression> aiexprvals = aiexpr.getValues();

            Object[] elemarr = new Object[aiexprvals.size()];

            Expression anaiexpr;
            for (int i = 0; i < aiexprvals.size(); ++i) {
                anaiexpr = aiexprvals.get(i);
                if (anaiexpr instanceof FieldAccessExpr
                        || anaiexpr instanceof NameExpr) {

                    if (anaiexpr instanceof FieldAccessExpr) {
                        elemarr[i] = findVariableElement((FieldAccessExpr) anaiexpr);
                    } else {
                        elemarr[i] = findVariableElement((NameExpr) anaiexpr);
                    }

                    if (elemarr[i] == null) {
                        // A warning was already issued by findVariableElement;
                        return;
                    }
                    String constval = (String) ((VariableElement)elemarr[i]).getConstantValue();
                    if (constval != null) {
                        elemarr[i] = constval;
                    }
                } else if (anaiexpr instanceof IntegerLiteralExpr) {
                    elemarr[i] = Integer.valueOf(((IntegerLiteralExpr) anaiexpr).getValue());
                } else if (anaiexpr instanceof StringLiteralExpr) {
                    elemarr[i] = ((StringLiteralExpr) anaiexpr).getValue();
                } else {
                    ErrorReporter.errorAbort("StubParser: unhandled annotation attribute type: " + anaiexpr);
                }
            }

            builder.setValue(name, elemarr);
        } else if (expr instanceof BooleanLiteralExpr) {
            BooleanLiteralExpr blexpr = (BooleanLiteralExpr) expr;
            ExecutableElement var = builder.findElement(name);
            TypeMirror expected = var.getReturnType();
            if (expected.getKind() == TypeKind.BOOLEAN) {
                builder.setValue(name, blexpr.getValue());
            } else if (expected.getKind() == TypeKind.ARRAY) {
                Boolean[] arr = { blexpr.getValue() };
                builder.setValue(name, arr);
            } else {
                ErrorReporter.errorAbort("StubParser: unhandled annotation attribute type: " + blexpr +
                        " and expected: " + expected);
            }
        } else {
            ErrorReporter.errorAbort("StubParser: unhandled annotation attribute type: " + expr +
                    " class: " + expr.getClass());
        }
    }

    private /*@Nullable*/ VariableElement findVariableElement(NameExpr nexpr) {
        if (nexprcache.containsKey(nexpr)) {
            return nexprcache.get(nexpr);
        }

        VariableElement res = null;
        boolean importFound = false;
        for (String imp: imports) {
            Pair<String, String> partitionedName = StubUtil.partitionQualifiedName(imp);
            String typeName = partitionedName.first;
            String fieldName = partitionedName.second;
            if (fieldName.equals(nexpr.getName())) {
                TypeElement enclType = findType(typeName,
                        String.format("Enclosing type of static import %s not found", fieldName));

                if (enclType == null) {
                    return null;
                } else {
                    importFound = true;
                    res = findFieldElement(enclType, fieldName);
                    break;
                }
            }
        }

        // Imported but invalid types or fields will have warnings from above,
        // only warn on fields missing an import
        if (res == null && !importFound) {
            stubWarnIfNotFound("Static field " + nexpr.getName() + " is not imported");
        }

        nexprcache.put(nexpr, res);
        return res;
    }

    private /*@Nullable*/ VariableElement findVariableElement(FieldAccessExpr faexpr) {
        if (faexprcache.containsKey(faexpr)) {
            return faexprcache.get(faexpr);
        }
        TypeElement rcvElt = elements.getTypeElement(faexpr.getScope().toString());
        if (rcvElt == null) {
            // Search imports for full annotation name.
            for (String imp: imports) {
                String[] import_delimited = imp.split("\\.");
                if (import_delimited[import_delimited.length - 1].equals(faexpr.getScope().toString())) {
                    StringBuilder full_annotation = new StringBuilder();
                    for (int i = 0; i < import_delimited.length - 1; i++) {
                        full_annotation.append(import_delimited[i]);
                        full_annotation.append('.');
                    }
                    full_annotation.append(faexpr.getScope().toString());
                    rcvElt = elements.getTypeElement(full_annotation);
                    break;
                }
            }

            if (rcvElt == null) {
                stubWarnIfNotFound("Type " + faexpr.getScope().toString() + " not found");
                return null;
            }
        }

        VariableElement res = findFieldElement(rcvElt, faexpr.getField());
        faexprcache.put(faexpr, res);
        return res;
    }
}
