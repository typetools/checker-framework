package org.checkerframework.framework.stub;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.StubUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.WildcardType;
import java.io.InputStream;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.FromStubFile;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeMerger;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;

/**
 * Given a stub file, yields the annotated types in it and the declaration annotations in it. The
 * main entry point is {@link StubParser#parse(String, InputStream, AnnotatedTypeFactory,
 * ProcessingEnvironment, Map, Map)}, which side-effects its last two arguments.
 *
 * <p>The constructor acts in two parts. First, it calls the Stub Parser to parse a stub file. Then,
 * itis walks the Stub Parser's AST to create/collect types and declaration annotations.
 */
public class StubParser {

    /**
     * Whether to print warnings about types/members that were not found. The warning is about
     * whether a class/field in the stub file is not found on the user's real classpath. Since the
     * stub file may contain packages that are not on the classpath, this can be OK, so default to
     * false.
     */
    private final boolean warnIfNotFound;

    /**
     * Whether to ignore missing classes even when warnIfNotFound is set to true. This allows the
     * stubs to contain classes not in the classpath (even if another class in the classpath has the
     * same package), but still warn if members of the class (methods, fields) are missing. This
     * option does nothing unless warnIfNotFound is also set.
     */
    private final boolean warnIfNotFoundIgnoresClasses;

    /** Whether to print warnings about stub files that overwrite annotations from bytecode. */
    private final boolean warnIfStubOverwritesBytecode;

    /** Whether to print verbose debugging messages. */
    private final boolean debugStubParser;

    /** The name of the stub file being processed; used only for diagnostic messages. */
    private final String filename;

    /**
     * The AST of the parsed stub file that this class is processing. May be null if there was a
     * problem parsing the stub file. (TODO: Should the Checker Framework just halt in that case?)
     */
    // Not final in order to accommodate a default value.
    private StubUnit stubUnit;

    private final ProcessingEnvironment processingEnv;
    private final AnnotatedTypeFactory atypeFactory;
    private final Elements elements;

    /**
     * The set of annotations found in the stub file. Keys are simple (unqualified) names. (This may
     * be a problem in the unlikely occurrence that a type-checker supports two annotations with the
     * same simple name.)
     *
     * @see #getAllStubAnnotations
     */
    private Map<String, AnnotationMirror> allStubAnnotations;

    /**
     * A list of the fully-qualified names of enum constants and static fields with constant values
     * that have been imported.
     */
    private final List<String> importedConstants = new ArrayList<>();

    /** A map of imported fully-qualified type names to type elements. */
    private final Map<String, TypeElement> importedTypes = new HashMap<>();

    /** The annotation {@code @FromStubFile}. */
    private final AnnotationMirror fromStubFile;

    /**
     * List of AnnotatedTypeMirrors for class or method type parameters that are in scope of the
     * elements currently parsed.
     */
    private final List<AnnotatedTypeVariable> typeParameters = new ArrayList<>();

    // The following variables are stored in the StubParser because otherwise they would need to be
    // passed through everywhere, which would be verbose.

    /** The type that is currently being parsed. */
    FqName parseState;

    /** Output variable: .... */
    Map<Element, AnnotatedTypeMirror> atypes;

    /** Output variable: .... */
    Map<String, Set<AnnotationMirror>> declAnnos;

    /** The line separator. */
    private static final String LINE_SEPARATOR = System.getProperty("line.separator").intern();

    /**
     * Create a new StubParser object, which will parse and extract annotations from the given stub
     * file.
     *
     * @param filename name of stub file, used only for diagnostic messages
     * @param atypeFactory AnnotatedtypeFactory to use
     * @param processingEnv ProcessingEnviroment to use
     */
    public StubParser(
            String filename,
            AnnotatedTypeFactory atypeFactory,
            ProcessingEnvironment processingEnv,
            Map<Element, AnnotatedTypeMirror> atypes,
            Map<String, Set<AnnotationMirror>> declAnnos) {
        this.filename = filename;
        this.atypeFactory = atypeFactory;
        this.processingEnv = processingEnv;
        this.elements = processingEnv.getElementUtils();

        Map<String, String> options = processingEnv.getOptions();
        this.warnIfNotFound = options.containsKey("stubWarnIfNotFound");
        this.warnIfNotFoundIgnoresClasses = options.containsKey("stubWarnIfNotFoundIgnoresClasses");
        this.warnIfStubOverwritesBytecode = options.containsKey("stubWarnIfOverwritesBytecode");
        this.debugStubParser = options.containsKey("stubDebug");

        this.fromStubFile = AnnotationBuilder.fromClass(elements, FromStubFile.class);

        this.atypes = atypes;
        this.declAnnos = declAnnos;
    }

    /**
     * All annotations defined in the package (but not those nested within classes in the package).
     * Keys are simple names.
     */
    private Map<String, AnnotationMirror> annosInPackage(PackageElement packageElement) {
        return createImportedAnnotationsMap(
                ElementFilter.typesIn(packageElement.getEnclosedElements()));
    }

    /** All annotations declared (directly) within a class. Keys are simple names. */
    private Map<String, AnnotationMirror> annosInType(TypeElement typeElement) {
        return createImportedAnnotationsMap(
                ElementFilter.typesIn(typeElement.getEnclosedElements()));
    }

    private Map<String, AnnotationMirror> createImportedAnnotationsMap(
            List<TypeElement> typeElements) {
        Map<String, AnnotationMirror> result = new HashMap<>();
        for (TypeElement typeElm : typeElements) {
            if (typeElm.getKind() == ElementKind.ANNOTATION_TYPE) {
                AnnotationMirror anno =
                        AnnotationBuilder.fromName(elements, typeElm.getQualifiedName());
                putNoOverride(result, typeElm.getSimpleName().toString(), anno);
            }
        }
        return result;
    }

    /**
     * Get all members of a Type that are importable in a stub file. Currently these are values of
     * enums, or compile time constants.
     *
     * @return a list fully-qualified member names
     */
    private static List<String> getImportableMembers(TypeElement typeElement) {
        List<String> result = new ArrayList<>();
        List<VariableElement> memberElements =
                ElementFilter.fieldsIn(typeElement.getEnclosedElements());
        for (VariableElement varElement : memberElements) {
            if (varElement.getConstantValue() != null
                    || varElement.getKind() == ElementKind.ENUM_CONSTANT) {
                result.add(
                        String.format(
                                "%s.%s",
                                typeElement.getQualifiedName().toString(),
                                varElement.getSimpleName().toString()));
            }
        }
        return result;
    }

    /**
     * Returns all annotations found in the stub file, as a value for {@link #allStubAnnotations}.
     * Note that this also modifies {@link #importedConstants} and {@link #importedTypes}.
     *
     * @see #allStubAnnotations
     */
    private Map<String, AnnotationMirror> getAllStubAnnotations() {
        Map<String, AnnotationMirror> result = new HashMap<>();

        assert !stubUnit.getCompilationUnits().isEmpty();
        CompilationUnit cu = stubUnit.getCompilationUnits().get(0);

        if (cu.getImports() == null) {
            return result;
        }

        for (ImportDeclaration importDecl : cu.getImports()) {
            String imported = importDecl.getNameAsString();
            try {
                if (importDecl.isAsterisk()) {
                    if (importDecl.isStatic()) {
                        // Wildcard import of members of a type (class or interface)
                        TypeElement element = getTypeElement(imported, "Imported type not found");
                        if (element != null) {
                            // Find nested annotations
                            // Find compile time constant fields, or values of an enum
                            putAllNew(result, annosInType(element));
                            importedConstants.addAll(getImportableMembers(element));
                            addEnclosingTypesToImportedTypes(element);
                        }

                    } else {
                        // Wildcard import of members of a package
                        PackageElement element = findPackage(imported);
                        if (element != null) {
                            putAllNew(result, annosInPackage(element));
                            addEnclosingTypesToImportedTypes(element);
                        }
                    }
                } else {
                    // A single (non-wildcard) import

                    final TypeElement importType = elements.getTypeElement(imported);
                    if (importType == null && !importDecl.isStatic()) {
                        // Class or nested class (according to JSL), but we can't resolve

                        stubWarnNotFound("Imported type not found: " + imported);
                    } else if (importType == null) {
                        // Nested Field

                        Pair<String, String> typeParts = StubUtil.partitionQualifiedName(imported);
                        String type = typeParts.first;
                        String fieldName = typeParts.second;
                        TypeElement enclType =
                                getTypeElement(
                                        type,
                                        String.format(
                                                "Enclosing type of static field %s not found",
                                                fieldName));

                        if (enclType != null) {
                            if (findFieldElement(enclType, fieldName) != null) {
                                importedConstants.add(imported);
                            }
                        }

                    } else if (importType.getKind() == ElementKind.ANNOTATION_TYPE) {
                        // Single annotation or nested annotation

                        AnnotationMirror anno = AnnotationBuilder.fromName(elements, imported);
                        if (anno != null) {
                            Element annoElt = anno.getAnnotationType().asElement();
                            putNoOverride(result, annoElt.getSimpleName().toString(), anno);
                            importedTypes.put(
                                    annoElt.getSimpleName().toString(), (TypeElement) annoElt);
                        } else {
                            stubWarnNotFound("Could not load import: " + imported);
                        }
                    } else {
                        // Class or nested class
                        // TODO: Is this needed?
                        importedConstants.add(imported);
                        TypeElement element = getTypeElement(imported, "Imported type not found");
                        importedTypes.put(element.getSimpleName().toString(), element);
                    }
                }
            } catch (AssertionError error) {
                stubWarnNotFound("" + error);
            }
        }
        return result;
    }

    // If a member is imported, then consider every containing class to also be imported.
    private void addEnclosingTypesToImportedTypes(Element element) {
        for (Element enclosedEle : element.getEnclosedElements()) {
            if (enclosedEle.getKind().isClass()) {
                importedTypes.put(
                        enclosedEle.getSimpleName().toString(), (TypeElement) enclosedEle);
            }
        }
    }

    /**
     * The main entry point. Parse a stub file and side-effects the last two arguments.
     *
     * @param filename name of stub file, used only for diagnostic messages
     * @param inputStream of stub file to parse
     * @param atypeFactory AnnotatedtypeFactory to use
     * @param processingEnv ProcessingEnviroment to use
     */
    public static void parse(
            String filename,
            InputStream inputStream,
            AnnotatedTypeFactory atypeFactory,
            ProcessingEnvironment processingEnv,
            Map<Element, AnnotatedTypeMirror> atypes,
            Map<String, Set<AnnotationMirror>> declAnnos) {
        StubParser sp = new StubParser(filename, atypeFactory, processingEnv, atypes, declAnnos);
        try {
            sp.parseStubUnit(inputStream);
            sp.process(atypes, declAnnos);
        } catch (ParseProblemException e) {
            StringBuilder message =
                    new StringBuilder(
                            "exception while parsing stub file "
                                    + filename
                                    + ". Encountered problems: ");
            // Manually build up the message, to get verbose location information.
            for (Problem p : e.getProblems()) {
                message.append(p.getVerboseMessage());
                message.append(LINE_SEPARATOR);
            }
            sp.stubWarn(message.toString());
        }
    }

    /**
     * Delegate to the Stub Parser to parse the stub file to an AST. Subsequently, all work uses the
     * AST.
     */
    private void parseStubUnit(InputStream inputStream) {
        if (debugStubParser) {
            stubDebug(String.format("parsing stub file %s", filename));
        }
        stubUnit = JavaParser.parseStubUnit(inputStream);

        // getAllStubAnnotations() also modifies importedConstants and importedTypes. This should
        // be refactored to be nicer.
        allStubAnnotations = getAllStubAnnotations();
        if (allStubAnnotations.isEmpty()) {
            stubWarnNotFound(
                    String.format(
                            "No supported annotations found! This likely means stub file %s doesn't import them correctly.",
                            filename));
        }
    }

    /** Process {@link #stubUnit}, which is the AST produced by {@link #parseStubUnit}. */
    private void process(
            Map<Element, AnnotatedTypeMirror> atypes,
            Map<String, Set<AnnotationMirror>> declAnnos) {
        processStubUnit(this.stubUnit);
    }

    /** Parse the given StubUnit. */
    private void processStubUnit(StubUnit index) {
        for (CompilationUnit cu : index.getCompilationUnits()) {
            processCompilationUnit(cu);
        }
    }

    private void processCompilationUnit(CompilationUnit cu) {
        final List<AnnotationExpr> packageAnnos;

        if (!cu.getPackageDeclaration().isPresent()) {
            packageAnnos = null;
            parseState = new FqName(null, null);
        } else {
            PackageDeclaration pDecl = cu.getPackageDeclaration().get();
            packageAnnos = pDecl.getAnnotations();
            processPackage(pDecl);
        }
        if (cu.getTypes() != null) {
            for (TypeDeclaration<?> typeDeclaration : cu.getTypes()) {
                processTypeDecl(typeDeclaration, null, packageAnnos);
            }
        }
    }

    private void processPackage(PackageDeclaration packDecl) {
        assert (packDecl != null);
        String packageName = packDecl.getNameAsString();
        parseState = new FqName(packageName, null);
        Element elem = elements.getPackageElement(packageName);
        // If the element lookup fails, it's because we have an annotation for a
        // package that isn't on the classpath, which is fine.
        if (elem != null) {
            annotateDecl(declAnnos, elem, packDecl.getAnnotations());
        }
        // TODO: Handle atypes???
    }

    /** @param outertypeName the name of the containing class, when processing a nested class */
    private void processTypeDecl(
            TypeDeclaration<?> typeDecl, String outertypeName, List<AnnotationExpr> packageAnnos) {
        assert parseState != null;
        String innerName =
                (outertypeName == null ? "" : outertypeName + ".") + typeDecl.getNameAsString();
        parseState = new FqName(parseState.packageName, innerName);
        String fqTypeName = parseState.toString();
        TypeElement typeElt = elements.getTypeElement(fqTypeName);
        if (typeElt == null) {
            if (debugStubParser
                    || (!hasNoStubParserWarning(typeDecl.getAnnotations())
                            && !hasNoStubParserWarning(packageAnnos)
                            && !warnIfNotFoundIgnoresClasses)) {
                stubWarnNotFound("Type not found: " + fqTypeName);
            }
            return;
        }

        if (typeElt.getKind() == ElementKind.ENUM) {
            typeParameters.addAll(processEnum((EnumDeclaration) typeDecl, typeElt));
        } else if (typeElt.getKind() == ElementKind.ANNOTATION_TYPE) {
            stubWarnNotFound("Skipping annotation type: " + fqTypeName);
        } else if (typeDecl instanceof ClassOrInterfaceDeclaration) {
            typeParameters.addAll(processType((ClassOrInterfaceDeclaration) typeDecl, typeElt));
        } // else it's an EmptyTypeDeclaration.  TODO:  An EmptyTypeDeclaration can have
        // annotations, right?

        Map<Element, BodyDeclaration<?>> elementsToDecl = getMembers(typeElt, typeDecl);
        for (Map.Entry<Element, BodyDeclaration<?>> entry : elementsToDecl.entrySet()) {
            final Element elt = entry.getKey();
            final BodyDeclaration<?> decl = entry.getValue();
            switch (elt.getKind()) {
                case FIELD:
                case ENUM_CONSTANT:
                    processField((FieldDeclaration) decl, (VariableElement) elt);
                    break;
                case CONSTRUCTOR:
                case METHOD:
                    processCallableDeclaration(
                            (CallableDeclaration<?>) decl, (ExecutableElement) elt);
                    break;
                case CLASS:
                case INTERFACE:
                    processTypeDecl((ClassOrInterfaceDeclaration) decl, innerName, packageAnnos);
                    break;
                case ENUM:
                    processTypeDecl((EnumDeclaration) decl, innerName, packageAnnos);
                    break;
                default:
                    /* do nothing */
                    stubWarnNotFound("StubParser ignoring: " + elt);
                    break;
            }
        }
        typeParameters.clear();
    }

    /** True if the argument contains {@code @NoStubParserWarning}. */
    private boolean hasNoStubParserWarning(Iterable<AnnotationExpr> aexprs) {
        if (aexprs == null) {
            return false;
        }
        for (AnnotationExpr anno : aexprs) {
            if (anno.getNameAsString().contentEquals("NoStubParserWarning")) {
                return true;
            }
        }
        return false;
    }

    /** @return list of AnnotatedTypeVariable of the type's type parameter declarations */
    private List<AnnotatedTypeVariable> processType(
            ClassOrInterfaceDeclaration decl, TypeElement elt) {
        annotateDecl(declAnnos, elt, decl.getAnnotations());
        AnnotatedDeclaredType type = atypeFactory.fromElement(elt);
        annotate(type, decl.getAnnotations());

        final List<? extends AnnotatedTypeMirror> typeArguments = type.getTypeArguments();
        final List<TypeParameter> typeParameters = decl.getTypeParameters();

        // It can be the case that args=[] and params=null, so don't crash in that case.
        // if ((typeParameters == null) != (typeArguments == null)) {
        //     throw new Error(String.format("parseType (%s, %s): inconsistent nullness for args and
        // params%n  args = %s%n  params = %s%n", decl, elt, typeArguments, typeParameters));
        // }

        if (debugStubParser) {
            int numParams = (typeParameters == null ? 0 : typeParameters.size());
            int numArgs = (typeArguments == null ? 0 : typeArguments.size());
            if (numParams != numArgs) {
                stubDebug(
                        String.format(
                                "parseType:  mismatched sizes for typeParameters=%s (size %d) and typeArguments=%s (size %d); decl=%s; elt=%s (%s); type=%s (%s); parseState=%s",
                                typeParameters,
                                numParams,
                                typeArguments,
                                numArgs,
                                decl.toString().replace(LINE_SEPARATOR, " "),
                                elt.toString().replace(LINE_SEPARATOR, " "),
                                elt.getClass(),
                                type,
                                type.getClass(),
                                parseState));
                stubDebug("Proceeding despite mismatched sizes");
            }
        }

        annotateTypeParameters(decl, elt, atypes, typeArguments, typeParameters);
        annotateSupertypes(decl, type);
        putNew(atypes, elt, type);
        List<AnnotatedTypeVariable> typeVariables = new ArrayList<>();
        for (AnnotatedTypeMirror typeV : type.getTypeArguments()) {
            if (typeV.getKind() != TypeKind.TYPEVAR) {
                stubWarn(
                        "expected an AnnotatedTypeVariable but found type kind "
                                + typeV.getKind()
                                + ": "
                                + typeV);
            } else {
                typeVariables.add((AnnotatedTypeVariable) typeV);
            }
        }
        return typeVariables;
    }

    /**
     * Gathers and returns a list of AnnotatedTypeVariable of the enum's type parameter
     * declarations.
     *
     * @param decl actual enum declaration
     * @param elt element representing enum
     * @param atypes map of annotated types
     * @param declAnnos map of declarations annotations
     * @return list of AnnotatedTypeVariable of the enum's type parameter declarations
     */
    private List<AnnotatedTypeVariable> processEnum(EnumDeclaration decl, TypeElement elt) {

        annotateDecl(declAnnos, elt, decl.getAnnotations());
        AnnotatedDeclaredType type = atypeFactory.fromElement(elt);
        annotate(type, decl.getAnnotations());

        putNew(atypes, elt, type);
        List<AnnotatedTypeVariable> typeVariables = new ArrayList<>();
        for (AnnotatedTypeMirror typeV : type.getTypeArguments()) {
            if (typeV.getKind() != TypeKind.TYPEVAR) {
                stubWarn(
                        "expected an AnnotatedTypeVariable but found type kind "
                                + typeV.getKind()
                                + ": "
                                + typeV);
            } else {
                typeVariables.add((AnnotatedTypeVariable) typeV);
            }
        }
        return typeVariables;
    }

    private void annotateSupertypes(
            ClassOrInterfaceDeclaration typeDecl, AnnotatedDeclaredType type) {
        if (typeDecl.getExtendedTypes() != null) {
            for (ClassOrInterfaceType superType : typeDecl.getExtendedTypes()) {
                AnnotatedDeclaredType foundType = findType(superType, type.directSuperTypes());
                if (foundType == null) {
                    throw new BugInCF(
                            "StubParser: could not find superclass "
                                    + superType
                                    + " from type "
                                    + type
                                    + LINE_SEPARATOR
                                    + "Stub file does not match bytecode");
                }
                annotate(foundType, superType, null);
            }
        }
        if (typeDecl.getImplementedTypes() != null) {
            for (ClassOrInterfaceType superType : typeDecl.getImplementedTypes()) {
                AnnotatedDeclaredType foundType = findType(superType, type.directSuperTypes());
                if (foundType == null) {
                    throw new BugInCF(
                            "StubParser: could not find superinterface "
                                    + superType
                                    + " from type "
                                    + type
                                    + LINE_SEPARATOR
                                    + "Stub file does not match bytecode");
                }
                annotate(foundType, superType, null);
            }
        }
    }

    /** Adds type and declaration annotations from {@code decl}. */
    private void processCallableDeclaration(CallableDeclaration<?> decl, ExecutableElement elt) {

        // Declaration annotations
        annotateDecl(declAnnos, elt, decl.getAnnotations());
        if (decl.isMethodDeclaration()) {
            // StubParser parses all annotations in type annotation position as type annotations
            annotateDecl(declAnnos, elt, ((MethodDeclaration) decl).getType().getAnnotations());
        }
        addDeclAnnotations(declAnnos, elt);

        AnnotatedExecutableType methodType = atypeFactory.fromElement(elt);
        // Type Parameters
        annotateTypeParameters(
                decl, elt, atypes, methodType.getTypeVariables(), decl.getTypeParameters());
        typeParameters.addAll(methodType.getTypeVariables());

        // Type annotations
        if (decl.isMethodDeclaration()) {
            annotate(
                    methodType.getReturnType(),
                    ((MethodDeclaration) decl).getType(),
                    decl.getAnnotations());
        } else {
            annotate(methodType.getReturnType(), decl.getAnnotations());
        }

        // Parameters
        processParameters(decl, elt, declAnnos, methodType);

        // Receiver
        if (decl.getReceiverParameter().isPresent()
                && !decl.getReceiverParameter().get().getAnnotations().isEmpty()) {
            if (methodType.getReceiverType() == null) {
                if (decl.isConstructorDeclaration()) {
                    stubWarn(
                            "parseParameter: constructor of a top-level class cannot have receiver annotations%n"
                                    + "Constructor: %s%n"
                                    + "Receiver annotations: %s",
                            methodType, decl.getReceiverParameter().get().getAnnotations());
                } else {
                    stubWarn(
                            "parseParameter: static methods cannot have receiver annotations%n"
                                    + "Method: %s%n"
                                    + "Receiver annotations: %s",
                            methodType, decl.getReceiverParameter().get().getAnnotations());
                }
            } else {
                annotate(
                        methodType.getReceiverType(),
                        decl.getReceiverParameter().get().getAnnotations());
            }
        }

        // Store the type.
        putNew(atypes, elt, methodType);
        typeParameters.removeAll(methodType.getTypeVariables());
    }

    /**
     * Adds declaration and type annotations to the parameters of {@code methodType}, which is
     * either a method or constructor.
     *
     * @param method Method or Constructor declaration
     * @param elt ExecutableElement of {@code method}
     * @param declAnnos map of declaration elements strings to annotations
     * @param methodType annotated type of {@code method}
     */
    private void processParameters(
            CallableDeclaration<?> method,
            ExecutableElement elt,
            Map<String, Set<AnnotationMirror>> declAnnos,
            AnnotatedExecutableType methodType) {
        List<Parameter> params = method.getParameters();
        List<? extends VariableElement> paramElts = elt.getParameters();
        List<? extends AnnotatedTypeMirror> paramTypes = methodType.getParameterTypes();

        for (int i = 0; i < methodType.getParameterTypes().size(); ++i) {
            VariableElement paramElt = paramElts.get(i);
            AnnotatedTypeMirror paramType = paramTypes.get(i);
            Parameter param = params.get(i);

            annotateDecl(declAnnos, paramElt, param.getAnnotations());
            annotateDecl(declAnnos, paramElt, param.getType().getAnnotations());

            if (param.isVarArgs()) {
                assert paramType.getKind() == TypeKind.ARRAY;
                // The "type" of param is actually the component type of the vararg.
                // For example, "Object..." the type would be "Object".
                annotate(
                        ((AnnotatedArrayType) paramType).getComponentType(),
                        param.getType(),
                        param.getAnnotations());
                // The "VarArgsAnnotations" are those just before "...".
                annotate(paramType, param.getVarArgsAnnotations());
            } else {
                annotate(paramType, param.getType(), param.getAnnotations());
            }
        }
    }

    /**
     * Clear (remove) existing annotations on the type.
     *
     * <p>Stub files override annotations read from .class files. Using {@code replaceAnnotation}
     * usually achieves this; however, for annotations on type variables, it is sometimes necessary
     * to remove an existing annotation, leaving no annotation on the type variable. This method
     * does so.
     *
     * @param atype the type to modify
     * @param typeDef the type from the stub file, used only for diagnostic messages
     */
    @SuppressWarnings("unused") // for disabled warning message
    private void clearAnnotations(AnnotatedTypeMirror atype, Type typeDef) {
        Set<AnnotationMirror> annos = atype.getAnnotations();
        // TODO: This should check whether the stub file is @AnnotatedFor the current type system.
        // @AnnotatedFor isn't integrated in stub files yet.
        if (annos != null && !annos.isEmpty()) {
            // TODO: only produce output if the removed annotation
            // isn't the top and default annotation in the type hierarchy.
            if (false) {
                stubWarnOverwritesBytecode(
                        String.format(
                                "in file %s at line %s removed existing annotations on type: %s",
                                filename.substring(filename.lastIndexOf('/') + 1),
                                typeDef.getBegin().get().line,
                                atype.toString(true)));
            }
            // Clear existing annotations, which only makes a difference for
            // type variables, but doesn't hurt in other cases.
            atype.clearAnnotations();
        }
    }

    /** Adds a declAnnotation to every method in the stub file. */
    private void addDeclAnnotations(Map<String, Set<AnnotationMirror>> declAnnos, Element elt) {
        Set<AnnotationMirror> annos = declAnnos.get(ElementUtils.getVerboseName(elt));
        if (annos == null) {
            annos = AnnotationUtils.createAnnotationSet();
            putOrAddToMap(declAnnos, ElementUtils.getVerboseName(elt), annos);
        }
        annos.add(fromStubFile);
    }

    /**
     * Add the annotations from {@code type} to {@code atype}. Type annotations that parsed as
     * declaration annotations (ie those in {@code declAnnos} are applied to the innermost component
     * type.
     *
     * @param atype annotated type to which to add annotations
     * @param type parsed type
     * @param declAnnos annotations stored on the declaration of the variable with this type or null
     */
    private void annotateAsArray(
            AnnotatedArrayType atype, ReferenceType type, NodeList<AnnotationExpr> declAnnos) {
        annotateInnermostComponentType(atype, declAnnos);
        Type typeDef = type;
        AnnotatedTypeMirror currentAtype = atype;
        while (typeDef.isArrayType() && currentAtype.getKind() == TypeKind.ARRAY) {
            // handle generic type
            clearAnnotations(currentAtype, typeDef);

            List<AnnotationExpr> annotations = typeDef.getAnnotations();
            if (annotations != null) {
                annotate(currentAtype, annotations);
            }
            typeDef = ((com.github.javaparser.ast.type.ArrayType) typeDef).getComponentType();
            currentAtype = ((AnnotatedArrayType) currentAtype).getComponentType();
            if (typeDef.isArrayType() ^ currentAtype.getKind() == TypeKind.ARRAY) {
                stubWarn("Mismatched array lengths; atype: " + atype + "%n  type: " + type);
            }
        }
    }

    private ClassOrInterfaceType unwrapDeclaredType(Type type) {
        if (type instanceof ClassOrInterfaceType) {
            return (ClassOrInterfaceType) type;
        } else if (type instanceof ReferenceType && type.getArrayLevel() == 0) {
            return unwrapDeclaredType(type.getElementType());
        } else {
            return null;
        }
    }

    /**
     * Add to {@code atype}:
     *
     * <ol>
     *   <li>the annotations from {@code typeDef}, and
     *   <li>any type annotations that parsed as declaration annotations (ie those in {@code
     *       declAnnos}).
     * </ol>
     *
     * @param atype annotated type to which to add annotations
     * @param typeDef parsed type
     * @param declAnnos annotations stored on the declaration of the variable with this type, or
     *     null
     */
    private void annotate(
            AnnotatedTypeMirror atype, Type typeDef, NodeList<AnnotationExpr> declAnnos) {
        if (atype.getKind() == TypeKind.ARRAY) {
            annotateAsArray((AnnotatedArrayType) atype, (ReferenceType) typeDef, declAnnos);
            return;
        }

        clearAnnotations(atype, typeDef);

        // Primary annotations for the type of a variable declaration are not stored in typeDef, but
        // rather as declaration annotations (passed as declAnnos to this method).  But, if typeDef
        // is not the type of a variable, then the primary annotations are stored in typeDef.
        NodeList<AnnotationExpr> primaryAnnotations;
        if (typeDef.getAnnotations().isEmpty() && declAnnos != null) {
            primaryAnnotations = declAnnos;
        } else {
            primaryAnnotations = typeDef.getAnnotations();
        }

        if (atype.getKind() != TypeKind.WILDCARD) {
            // The primary annotation on a wildcard applies to the super or extends bound and
            // are added below.
            annotate(atype, primaryAnnotations);
        }
        switch (atype.getKind()) {
            case DECLARED:
                ClassOrInterfaceType declType = unwrapDeclaredType(typeDef);
                if (declType == null) {
                    break;
                }
                AnnotatedDeclaredType adeclType = (AnnotatedDeclaredType) atype;
                if (declType.getTypeArguments().isPresent()
                        && !declType.getTypeArguments().get().isEmpty()
                        && !adeclType.getTypeArguments().isEmpty()) {
                    assert declType.getTypeArguments().get().size()
                                    == adeclType.getTypeArguments().size()
                            : String.format(
                                    "Mismatch in type argument size between %s (%d) and %s (%d)",
                                    declType,
                                    declType.getTypeArguments().get().size(),
                                    adeclType,
                                    adeclType.getTypeArguments().size());
                    for (int i = 0; i < declType.getTypeArguments().get().size(); ++i) {
                        annotate(
                                adeclType.getTypeArguments().get(i),
                                declType.getTypeArguments().get().get(i),
                                null);
                    }
                }
                break;
            case WILDCARD:
                AnnotatedWildcardType wildcardType = (AnnotatedWildcardType) atype;
                // Ensure that the stub also has a wildcard type, report an error otherwise
                if (!typeDef.isWildcardType()) {
                    // We throw an error here, as otherwise we are just getting a generic cast error
                    // on the very next line.
                    throw new Error(
                            "StubParser: Wildcard type <"
                                    + atype.toString()
                                    + "> doesn't match type in stubs file: <"
                                    + typeDef.toString()
                                    + ">"
                                    + LINE_SEPARATOR
                                    + "In file "
                                    + filename
                                    + LINE_SEPARATOR
                                    + "While parsing "
                                    + parseState.toString());
                }
                WildcardType wildcardDef = (WildcardType) typeDef;
                if (wildcardDef.getExtendedType().isPresent()) {
                    annotate(
                            wildcardType.getExtendsBound(),
                            wildcardDef.getExtendedType().get(),
                            null);
                    annotate(wildcardType.getSuperBound(), primaryAnnotations);
                } else if (wildcardDef.getSuperType().isPresent()) {
                    annotate(wildcardType.getSuperBound(), wildcardDef.getSuperType().get(), null);
                    annotate(wildcardType.getExtendsBound(), primaryAnnotations);
                } else {
                    annotate(atype, primaryAnnotations);
                }
                break;
            case TYPEVAR:
                // Add annotations from the declaration of the TypeVariable
                AnnotatedTypeVariable typeVarUse = (AnnotatedTypeVariable) atype;
                for (AnnotatedTypeVariable typePar : typeParameters) {
                    if (typePar.getUnderlyingType() == atype.getUnderlyingType()) {
                        AnnotatedTypeMerger.merge(
                                typePar.getUpperBound(), typeVarUse.getUpperBound());
                        AnnotatedTypeMerger.merge(
                                typePar.getLowerBound(), typeVarUse.getLowerBound());
                    }
                }
                break;
            default:
                // No additional annotations to add.
        }
    }

    private void processField(FieldDeclaration decl, VariableElement elt) {
        addDeclAnnotations(declAnnos, elt);
        annotateDecl(declAnnos, elt, decl.getAnnotations());
        // StubParser parses all annotations in type annotation position as type annotations
        annotateDecl(declAnnos, elt, decl.getElementType().getAnnotations());
        AnnotatedTypeMirror fieldType = atypeFactory.fromElement(elt);

        VariableDeclarator fieldVarDecl = null;
        String eltName = elt.getSimpleName().toString();
        for (VariableDeclarator var : decl.getVariables()) {
            if (var.getName().toString().equals(eltName)) {
                fieldVarDecl = var;
                break;
            }
        }
        assert fieldVarDecl != null;
        annotate(fieldType, fieldVarDecl.getType(), decl.getAnnotations());
        putNew(atypes, elt, fieldType);
    }

    /**
     * Returns the innermost component type of {@code type}.
     *
     * @param type array type
     */
    private AnnotatedTypeMirror innermostComponentType(AnnotatedArrayType type) {
        AnnotatedTypeMirror componentType = type;
        while (componentType.getKind() == TypeKind.ARRAY) {
            componentType = ((AnnotatedArrayType) componentType).getComponentType();
        }
        return componentType;
    }

    /**
     * Adds {@code annotations} to the innermost component type of {@code type}.
     *
     * @param type array type
     * @param annotations annotations to add
     */
    private void annotateInnermostComponentType(
            AnnotatedArrayType type, List<AnnotationExpr> annotations) {
        annotate(innermostComponentType(type), annotations);
    }

    private void annotate(AnnotatedTypeMirror type, List<AnnotationExpr> annotations) {
        if (annotations == null) {
            return;
        }
        for (AnnotationExpr annotation : annotations) {
            AnnotationMirror annoMirror = getAnnotation(annotation, allStubAnnotations);
            if (annoMirror != null) {
                type.replaceAnnotation(annoMirror);
            } else {
                stubWarnNotFound("Unknown annotation: " + annotation);
            }
        }
    }

    private void annotateDecl(
            Map<String, Set<AnnotationMirror>> declAnnos,
            Element elt,
            List<AnnotationExpr> annotations) {
        if (annotations == null) {
            return;
        }
        Set<AnnotationMirror> annos = AnnotationUtils.createAnnotationSet();
        for (AnnotationExpr annotation : annotations) {
            AnnotationMirror annoMirror = getAnnotation(annotation, allStubAnnotations);
            if (annoMirror != null) {
                Target target =
                        annoMirror.getAnnotationType().asElement().getAnnotation(Target.class);
                // Only add the declaration annotation if the annotation applies to the element.
                if (AnnotationUtils.getElementKindsForTarget(target).contains(elt.getKind())) {
                    annos.add(annoMirror);
                }
            }
        }
        String key = ElementUtils.getVerboseName(elt);
        putOrAddToMap(declAnnos, key, annos);
    }

    private void annotateTypeParameters(
            BodyDeclaration<?> decl, // for debugging
            Object elt, // for debugging; TypeElement or ExecutableElement
            Map<Element, AnnotatedTypeMirror> atypes,
            List<? extends AnnotatedTypeMirror> typeArguments,
            List<TypeParameter> typeParameters) {
        if (typeParameters == null) {
            return;
        }

        if (typeParameters.size() != typeArguments.size()) {
            String msg =
                    String.format(
                            "annotateTypeParameters: mismatched sizes:  typeParameters (size %d)=%s;  typeArguments (size %d)=%s;  decl=%s;  elt=%s (%s).",
                            typeParameters.size(),
                            typeParameters,
                            typeArguments.size(),
                            typeArguments,
                            decl.toString().replace(LINE_SEPARATOR, " "),
                            elt.toString().replace(LINE_SEPARATOR, " "),
                            elt.getClass());
            if (!debugStubParser) {
                msg = msg + "%n  For more details, run with -AstubDebug";
            }
            stubWarn(msg);
        }
        for (int i = 0; i < typeParameters.size(); ++i) {
            TypeParameter param = typeParameters.get(i);
            AnnotatedTypeVariable paramType = (AnnotatedTypeVariable) typeArguments.get(i);

            if (param.getTypeBound() == null || param.getTypeBound().isEmpty()) {
                // No bound so annotations are both lower and upper bounds
                annotate(paramType, param.getAnnotations());
            } else if (param.getTypeBound() != null && param.getTypeBound().size() > 0) {
                annotate(paramType.getLowerBound(), param.getAnnotations());
                annotate(paramType.getUpperBound(), param.getTypeBound().get(0), null);
                if (param.getTypeBound().size() > 1) {
                    // TODO: add support for intersection types
                    stubWarnNotFound("Annotations on intersection types are not yet supported");
                }
            }
            putNew(atypes, paramType.getUnderlyingType().asElement(), paramType);
        }
    }

    private Map<Element, BodyDeclaration<?>> getMembers(
            TypeElement typeElt, TypeDeclaration<?> typeDecl) {
        assert (typeElt.getSimpleName().contentEquals(typeDecl.getNameAsString())
                        || typeDecl.getNameAsString()
                                .endsWith("$" + typeElt.getSimpleName().toString()))
                : String.format("%s  %s", typeElt.getSimpleName(), typeDecl.getName());

        Map<Element, BodyDeclaration<?>> result = new LinkedHashMap<>();
        for (BodyDeclaration<?> member : typeDecl.getMembers()) {
            putNewElement(typeElt, result, member, typeDecl.getNameAsString());
        }
        return result;
    }

    private void putNewElement(
            TypeElement typeElt,
            Map<Element, BodyDeclaration<?>> result,
            BodyDeclaration<?> member,
            String typeDeclName) {
        if (member instanceof MethodDeclaration) {
            Element elt = findElement(typeElt, (MethodDeclaration) member);
            if (elt != null) {
                putNoOverride(result, elt, member);
            }
        } else if (member instanceof ConstructorDeclaration) {
            Element elt = findElement(typeElt, (ConstructorDeclaration) member);
            if (elt != null) {
                putNoOverride(result, elt, member);
            }
        } else if (member instanceof FieldDeclaration) {
            FieldDeclaration fieldDecl = (FieldDeclaration) member;
            for (VariableDeclarator var : fieldDecl.getVariables()) {
                Element varelt = findElement(typeElt, var);
                if (varelt != null) {
                    putNoOverride(result, varelt, fieldDecl);
                }
            }
        } else if (member instanceof ClassOrInterfaceDeclaration) {
            Element elt = findElement(typeElt, (ClassOrInterfaceDeclaration) member);
            if (elt != null) {
                putNoOverride(result, elt, member);
            }
        } else if (member instanceof EnumDeclaration) {
            Element elt = findElement(typeElt, (EnumDeclaration) member);
            if (elt != null) {
                putNoOverride(result, elt, member);
            }
        } else {
            stubWarnNotFound(
                    String.format(
                            "Ignoring element of type %s in %s", member.getClass(), typeDeclName));
        }
    }

    /** Return the element of {@code types} whose name matches {@code type}. */
    private AnnotatedDeclaredType findType(
            ClassOrInterfaceType type, List<AnnotatedDeclaredType> types) {
        String typeString = type.getNameAsString();
        for (AnnotatedDeclaredType superType : types) {
            if (superType
                    .getUnderlyingType()
                    .asElement()
                    .getSimpleName()
                    .contentEquals(typeString)) {
                return superType;
            }
        }
        stubWarnNotFound("Supertype " + typeString + " not found");
        if (debugStubParser) {
            stubDebug("Supertypes that were searched:");
            for (AnnotatedDeclaredType superType : types) {
                stubDebug(String.format("  %s", superType));
            }
        }
        return null;
    }

    /**
     * Looks for the nested type element in the typeElt and returns it if the element has the same
     * name as provided class or interface declaration. In case nested element is not found it
     * returns null.
     *
     * @param typeElt an element where nested type element should be looked for
     * @param ciDecl class or interface declaration which name should be found among nested elements
     *     of the typeElt
     * @return nested in typeElt element with the name of the class or interface or null if nested
     *     element is not found
     */
    private Element findElement(TypeElement typeElt, ClassOrInterfaceDeclaration ciDecl) {
        final String wantedClassOrInterfaceName = ciDecl.getNameAsString();
        for (TypeElement typeElement : ElementUtils.getAllTypeElementsIn(typeElt)) {
            if (wantedClassOrInterfaceName.equals(typeElement.getSimpleName().toString())) {
                return typeElement;
            }
        }

        stubWarnNotFound(
                "Class/interface " + wantedClassOrInterfaceName + " not found in type " + typeElt);
        if (debugStubParser) {
            for (ExecutableElement method :
                    ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
                stubDebug(String.format("  Here are the type declarations of %s:", typeElt));
                stubDebug(String.format("  %s", method));
            }
        }
        return null;
    }

    /**
     * Looks for the nested enum element in the typeElt and returns it if the element has the same
     * name as provided enum declaration. In case nested element is not found it returns null.
     *
     * @param typeElt an element where nested enum element should be looked for
     * @param enumDecl enum declaration which name should be found among nested elements of the
     *     typeElt
     * @return nested in typeElt enum element with the name of the provided enum or null if nested
     *     element is not found
     */
    private Element findElement(TypeElement typeElt, EnumDeclaration enumDecl) {
        final String wantedEnumName = enumDecl.getNameAsString();
        for (TypeElement typeElement : ElementUtils.getAllTypeElementsIn(typeElt)) {
            if (wantedEnumName.equals(typeElement.getSimpleName().toString())) {
                return typeElement;
            }
        }

        stubWarnNotFound("Enum " + wantedEnumName + " not found in type " + typeElt);
        if (debugStubParser) {
            for (ExecutableElement method :
                    ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
                stubDebug(String.format("  Here are the type declarations of %s:", typeElt));
                stubDebug(String.format("  %s", method));
            }
        }
        return null;
    }

    /**
     * Looks for method element in the typeElt and returns it if the element has the same signature
     * as provided method declaration. In case method element is not found it returns null.
     *
     * @param typeElt type element where method element should be looked for
     * @param methodDecl method declaration with signature that should be found among methods in the
     *     typeElt
     * @return method element in typeElt with the same signature as the provided method declaration
     *     or null if method element is not found
     */
    private ExecutableElement findElement(TypeElement typeElt, MethodDeclaration methodDecl) {
        final String wantedMethodName = methodDecl.getNameAsString();
        final int wantedMethodParams =
                (methodDecl.getParameters() == null) ? 0 : methodDecl.getParameters().size();
        final String wantedMethodString = StubUtil.toString(methodDecl);
        for (ExecutableElement method : ElementUtils.getAllMethodsIn(typeElt, elements)) {
            // do heuristics first
            if (wantedMethodParams == method.getParameters().size()
                    && wantedMethodName.contentEquals(method.getSimpleName().toString())
                    && StubUtil.toString(method).equals(wantedMethodString)) {
                return method;
            }
        }
        stubWarnNotFound("Method " + wantedMethodString + " not found in type " + typeElt);
        if (debugStubParser) {
            for (ExecutableElement method :
                    ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
                stubDebug(String.format("  Here are the methods of %s:", typeElt));
                stubDebug(String.format("  %s", method));
            }
        }
        return null;
    }

    /**
     * Looks for a constructor element in the typeElt and returns it if the element has the same
     * signature as provided constructor declaration. In case constructor element is not found it
     * returns null.
     *
     * @param typeElt type element where constructor element should be looked for
     * @param constructorDecl constructor declaration with signature that should be found among
     *     constructors in the typeElt
     * @return constructor element in typeElt with the same signature as the provided constructor
     *     declaration or null if constructor element is not found
     */
    private ExecutableElement findElement(
            TypeElement typeElt, ConstructorDeclaration constructorDecl) {
        final int wantedMethodParams =
                (constructorDecl.getParameters() == null)
                        ? 0
                        : constructorDecl.getParameters().size();
        final String wantedMethodString = StubUtil.toString(constructorDecl);
        for (ExecutableElement method :
                ElementFilter.constructorsIn(typeElt.getEnclosedElements())) {
            // do heuristics first
            if (wantedMethodParams == method.getParameters().size()
                    && StubUtil.toString(method).equals(wantedMethodString)) {
                return method;
            }
        }

        stubWarnNotFound("Constructor " + wantedMethodString + " not found in type " + typeElt);
        if (debugStubParser) {
            for (ExecutableElement method :
                    ElementFilter.constructorsIn(typeElt.getEnclosedElements())) {
                stubDebug(String.format("  %s", method));
            }
        }
        return null;
    }

    private VariableElement findElement(TypeElement typeElt, VariableDeclarator variable) {
        final String fieldName = variable.getNameAsString();
        return findFieldElement(typeElt, fieldName);
    }

    /**
     * Looks for a field element in the typeElt and returns it if the element has the same name as
     * provided. In case field element is not found it returns null.
     *
     * @param typeElt type element where field element should be looked for
     * @param fieldName field name that should be found
     * @return field element in typeElt with the provided name or null if field element is not found
     */
    private VariableElement findFieldElement(TypeElement typeElt, String fieldName) {
        for (VariableElement field : ElementUtils.getAllFieldsIn(typeElt, elements)) {
            // field.getSimpleName() is a CharSequence, not a String
            if (fieldName.equals(field.getSimpleName().toString())) {
                return field;
            }
        }

        stubWarnNotFound("Field " + fieldName + " not found in type " + typeElt);
        if (debugStubParser) {
            for (VariableElement field : ElementFilter.fieldsIn(typeElt.getEnclosedElements())) {
                stubDebug(String.format("  %s", field));
            }
        }
        return null;
    }

    /**
     * Given a fully-qualified type name, return a TypeElement for it, or null if none exists. Also
     * cache in importedTypes.
     */
    private TypeElement getTypeElementOrNull(String name) {
        TypeElement typeElement = elements.getTypeElement(name);
        if (typeElement != null) {
            importedTypes.put(name, typeElement);
        }
        // for debugging: stubWarn("getTypeElementOrNull(%s) => %s%n", name, typeElement);
        return typeElement;
    }

    /**
     * Get the type element for the given fully-qualified type name, or issue a warning if none is
     * found.
     */
    private TypeElement getTypeElement(String typeName, String... msg) {
        TypeElement classElement = elements.getTypeElement(typeName);
        if (classElement == null) {
            if (msg.length == 0) {
                stubWarnNotFound("Type not found: " + typeName);
            } else {
                stubWarnNotFound(msg[0] + ": " + typeName);
            }
        }
        return classElement;
    }

    private PackageElement findPackage(String packageName) {
        PackageElement packageElement = elements.getPackageElement(packageName);
        if (packageElement == null) {
            stubWarnNotFound("Imported package not found: " + packageName);
        }
        return packageElement;
    }

    /**
     * Convert {@code annotation} into an AnnotationMirror. Returns null if the annotation isn't
     * supported by the checker or if some error occurred while converting it.
     */
    private AnnotationMirror getAnnotation(
            AnnotationExpr annotation, Map<String, AnnotationMirror> allStubAnnotations) {
        AnnotationMirror annoMirror;
        if (annotation instanceof MarkerAnnotationExpr) {
            String annoName = ((MarkerAnnotationExpr) annotation).getNameAsString();
            annoMirror = allStubAnnotations.get(annoName);
        } else if (annotation instanceof NormalAnnotationExpr) {
            NormalAnnotationExpr nrmanno = (NormalAnnotationExpr) annotation;
            String annoName = nrmanno.getNameAsString();
            annoMirror = allStubAnnotations.get(annoName);
            if (annoMirror == null) {
                // Not a supported qualifier -> ignore
                return null;
            }
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv, annoMirror);
            List<MemberValuePair> pairs = nrmanno.getPairs();
            if (pairs != null) {
                for (MemberValuePair mvp : pairs) {
                    String member = mvp.getNameAsString();
                    Expression exp = mvp.getValue();
                    boolean success = handleExpr(builder, member, exp);
                    if (!success) {
                        stubWarn(
                                "Annotation expression, %s, could not be processed for annotation: %s. ",
                                exp, annotation);
                        return null;
                    }
                }
            }
            return builder.build();
        } else if (annotation instanceof SingleMemberAnnotationExpr) {
            SingleMemberAnnotationExpr sglanno = (SingleMemberAnnotationExpr) annotation;
            String annoName = sglanno.getNameAsString();
            annoMirror = allStubAnnotations.get(annoName);
            if (annoMirror == null) {
                // Not a supported qualifier -> ignore
                return null;
            }
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv, annoMirror);
            Expression valexpr = sglanno.getMemberValue();
            boolean success = handleExpr(builder, "value", valexpr);
            if (!success) {
                stubWarn(
                        "Annotation expression, %s, could not be processed for annotation: %s. ",
                        valexpr, annotation);
                return null;
            }
            return builder.build();
        } else {
            throw new BugInCF("StubParser: unknown annotation type: " + annotation);
        }
        return annoMirror;
    }

    /** Returns the value of {@code expr}, or null if some problem occurred getting the value. */
    private Object getValueOfExpressionInAnnotation(
            String name, Expression expr, TypeKind valueKind) {
        if (expr instanceof FieldAccessExpr || expr instanceof NameExpr) {
            VariableElement elem;
            if (expr instanceof NameExpr) {
                elem = findVariableElement((NameExpr) expr);
            } else {
                elem = findVariableElement((FieldAccessExpr) expr);
            }
            if (elem == null) {
                stubWarn("Field not found: " + expr);
                return null;
            }
            Object value = elem.getConstantValue() != null ? elem.getConstantValue() : elem;
            if (value instanceof Number) {
                return convert((Number) value, valueKind);
            } else {
                return value;
            }
        } else if (expr instanceof StringLiteralExpr) {
            return ((StringLiteralExpr) expr).asString();
        } else if (expr instanceof BooleanLiteralExpr) {
            return ((BooleanLiteralExpr) expr).getValue();
        } else if (expr instanceof CharLiteralExpr) {
            return convert((int) ((CharLiteralExpr) expr).asChar(), valueKind);
        } else if (expr instanceof DoubleLiteralExpr) {
            // No conversion needed if the expression is a double, the annotation value must be a
            // double, too.
            return ((DoubleLiteralExpr) expr).asDouble();
        } else if (expr instanceof IntegerLiteralExpr) {
            return convert(((IntegerLiteralExpr) expr).asInt(), valueKind);
        } else if (expr instanceof LongLiteralExpr) {
            return convert(((LongLiteralExpr) expr).asLong(), valueKind);
        } else if (expr instanceof UnaryExpr) {
            if (((UnaryExpr) expr).getOperator() == UnaryExpr.Operator.MINUS) {
                Object value =
                        getValueOfExpressionInAnnotation(
                                name, ((UnaryExpr) expr).getExpression(), valueKind);
                if (value instanceof Number) {
                    return convert((Number) value, valueKind, true);
                }
            }
            stubWarn("Unexpected Unary annotation expression: " + expr);
            return null;
        } else if (expr instanceof ClassExpr) {
            ClassExpr classExpr = (ClassExpr) expr;
            String className = classExpr.getType().toString();
            if (importedTypes.containsKey(className)) {
                return importedTypes.get(className).asType();
            }
            TypeElement typeElement = findTypeOfName(className);
            if (typeElement == null) {
                stubWarn("StubParser: unknown class name " + className);
                return null;
            }

            return typeElement.asType();
        } else if (expr instanceof NullLiteralExpr) {
            stubWarn("Null found as value for %s. Null isn't allowed as an annotation value", name);
            return null;
        } else {
            stubWarn("Unexpected annotation expression: " + expr);
            return null;
        }
    }

    /**
     * Returns the TypeElement with the name {@code name}, if one exists. Otherwise, checks the
     * class and package of {@code parseState} for a class named {@code name}.
     *
     * @param name classname (simple, or Outer.Inner, or fully-qualified)
     * @return the TypeElement for {@code name}, or null if not found
     */
    private @Nullable TypeElement findTypeOfName(String name) {
        String packageName = parseState.packageName;
        String packagePrefix = (packageName == null) ? "" : packageName + ".";

        // stubWarn("findTypeOfName(%s), parseState %s %s%n", name, packageName, enclosingClass);

        // As soon as typeElement is set to a non-null value, it will be returned.
        TypeElement typeElement = getTypeElementOrNull(name);
        if (typeElement == null && packageName != null) {
            typeElement = getTypeElementOrNull(packagePrefix + name);
        }
        String enclosingClass = parseState.className;
        while (typeElement == null && enclosingClass != null) {
            typeElement = getTypeElementOrNull(packagePrefix + enclosingClass + "." + name);
            int lastDot = enclosingClass.lastIndexOf('.');
            if (lastDot == -1) {
                break;
            } else {
                enclosingClass = enclosingClass.substring(0, lastDot);
            }
        }
        if (typeElement == null && !"java.lang".equals(packageName)) {
            typeElement = getTypeElementOrNull("java.lang." + name);
        }
        return typeElement;
    }

    /**
     * Converts {@code number} to {@code expectedKind}.
     * <p>
     * {@code @interface Anno { long value();})
     * {@code @Anno(1)}
     *
     * To properly build @Anno, the IntegerLiteralExpr "1" must be converted from an int to a long.
     * */
    private Object convert(Number number, TypeKind expectedKind) {
        return convert(number, expectedKind, false);
    }

    /**
     * Converts {@code number} to {@code expectedKind}. The value converted is multiplied by -1 if
     * {@code negate} is true
     *
     * @param number Number value to be converted
     * @param expectedKind one of type {byte, short, int, long, char, float, double}
     * @param negate whether to negate the value of the Number Object while converting
     * @return the converted Object
     */
    private Object convert(Number number, TypeKind expectedKind, boolean negate) {
        byte scalefactor = (byte) (negate ? -1 : 1);
        switch (expectedKind) {
            case BYTE:
                return number.byteValue() * scalefactor;
            case SHORT:
                return number.shortValue() * scalefactor;
            case INT:
                return number.intValue() * scalefactor;
            case LONG:
                return number.longValue() * scalefactor;
            case CHAR:
                /* char is not multiplied by the scale factor since it's not possible for `number` to be negative
                when `expectedkind` is a CHAR and casting a negative value to char is illegal */
                return (char) number.intValue();
            case FLOAT:
                return number.floatValue() * scalefactor;
            case DOUBLE:
                return number.doubleValue() * scalefactor;
            default:
                throw new BugInCF("Unexpected expectedKind: " + expectedKind);
        }
    }

    /**
     * Adds an annotation element (argument) to {@code builder}. The element is a Java expression.
     *
     * @return true if the expression was parsed and added to {@code builder}, false otherwise
     */
    private boolean handleExpr(AnnotationBuilder builder, String name, Expression expr) {
        ExecutableElement var = builder.findElement(name);
        TypeMirror expected = var.getReturnType();
        TypeKind valueKind;
        if (expected.getKind() == TypeKind.ARRAY) {
            valueKind = ((ArrayType) expected).getComponentType().getKind();
        } else {
            valueKind = expected.getKind();
        }
        if (expr instanceof ArrayInitializerExpr) {
            if (expected.getKind() != TypeKind.ARRAY) {
                stubWarn(
                        "unhandled annotation attribute type: "
                                + expr
                                + " and expected: "
                                + expected);
                return false;
            }

            List<Expression> arrayExpressions = ((ArrayInitializerExpr) expr).getValues();
            Object[] values = new Object[arrayExpressions.size()];

            for (int i = 0; i < arrayExpressions.size(); ++i) {
                values[i] =
                        getValueOfExpressionInAnnotation(name, arrayExpressions.get(i), valueKind);
                if (values[i] == null) {
                    return false;
                }
            }
            builder.setValue(name, values);
        } else {
            Object value = getValueOfExpressionInAnnotation(name, expr, valueKind);
            if (value == null) {
                return false;
            }
            if (expected.getKind() == TypeKind.ARRAY) {
                Object[] valueArray = {value};
                builder.setValue(name, valueArray);
            } else {
                builderSetValue(builder, name, value);
            }
        }
        return true;
    }

    /**
     * Cast to non-array values so that correct the correct AnnotationBuilder#setValue method is
     * called. (Different types of values are handled differently.)
     */
    private void builderSetValue(AnnotationBuilder builder, String name, Object value) {
        if (value instanceof Boolean) {
            builder.setValue(name, (Boolean) value);
        } else if (value instanceof Character) {
            builder.setValue(name, (Character) value);
        } else if (value instanceof Class<?>) {
            builder.setValue(name, (Class<?>) value);
        } else if (value instanceof Double) {
            builder.setValue(name, (Double) value);
        } else if (value instanceof Enum<?>) {
            builder.setValue(name, (Enum<?>) value);
        } else if (value instanceof Float) {
            builder.setValue(name, (Float) value);
        } else if (value instanceof Integer) {
            builder.setValue(name, (Integer) value);
        } else if (value instanceof Long) {
            builder.setValue(name, (Long) value);
        } else if (value instanceof Short) {
            builder.setValue(name, (Short) value);
        } else if (value instanceof String) {
            builder.setValue(name, (String) value);
        } else if (value instanceof TypeMirror) {
            builder.setValue(name, (TypeMirror) value);
        } else if (value instanceof VariableElement) {
            builder.setValue(name, (VariableElement) value);
        } else {
            throw new BugInCF("Unexpected builder value: %s", value);
        }
    }

    /**
     * Mapping of a name access expression that has already been encountered to the resolved
     * variable element.
     */
    private final Map<NameExpr, VariableElement> findVariableElementNameCache = new HashMap<>();

    private @Nullable VariableElement findVariableElement(NameExpr nexpr) {
        if (findVariableElementNameCache.containsKey(nexpr)) {
            return findVariableElementNameCache.get(nexpr);
        }

        VariableElement res = null;
        boolean importFound = false;
        for (String imp : importedConstants) {
            Pair<String, String> partitionedName = StubUtil.partitionQualifiedName(imp);
            String typeName = partitionedName.first;
            String fieldName = partitionedName.second;
            if (fieldName.equals(nexpr.getNameAsString())) {
                TypeElement enclType =
                        getTypeElement(
                                typeName,
                                String.format(
                                        "Enclosing type of static import %s not found", fieldName));

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
            stubWarnNotFound("Static field " + nexpr.getName() + " is not imported");
        }

        findVariableElementNameCache.put(nexpr, res);
        return res;
    }

    /**
     * Mapping of a field access expression that has already been encountered to the resolved
     * variable element.
     */
    private final Map<FieldAccessExpr, VariableElement> findVariableElementFieldCache =
            new HashMap<>();

    private @Nullable VariableElement findVariableElement(FieldAccessExpr faexpr) {
        if (findVariableElementFieldCache.containsKey(faexpr)) {
            return findVariableElementFieldCache.get(faexpr);
        }
        TypeElement rcvElt = elements.getTypeElement(faexpr.getScope().toString());
        if (rcvElt == null) {
            // Search importedConstants for full annotation name.
            for (String imp : importedConstants) {
                // TODO: should this use StubUtil.partitionQualifiedName?
                String[] importDelimited = imp.split("\\.");
                if (importDelimited[importDelimited.length - 1].equals(
                        faexpr.getScope().toString())) {
                    StringBuilder fullAnnotation = new StringBuilder();
                    for (int i = 0; i < importDelimited.length - 1; i++) {
                        fullAnnotation.append(importDelimited[i]);
                        fullAnnotation.append('.');
                    }
                    fullAnnotation.append(faexpr.getScope().toString());
                    rcvElt = elements.getTypeElement(fullAnnotation);
                    break;
                }
            }

            if (rcvElt == null) {
                stubWarnNotFound("Type " + faexpr.getScope().toString() + " not found");
                return null;
            }
        }

        VariableElement res = findFieldElement(rcvElt, faexpr.getNameAsString());
        findVariableElementFieldCache.put(faexpr, res);
        return res;
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Map utilities
    ///

    /** Just like Map.put, but does not override any existing value in the map. */
    private static <K, V> void putNoOverride(Map<K, V> m, K key, V value) {
        if (key == null) {
            throw new BugInCF("StubParser: key is null");
        }
        if (!m.containsKey(key)) {
            m.put(key, value);
        }
    }

    /**
     * If the key is already in the map, then add the annos to the list. Otherwise put the key and
     * the annos in the map
     */
    private static void putOrAddToMap(
            Map<String, Set<AnnotationMirror>> map, String key, Set<AnnotationMirror> annos) {
        if (map.containsKey(key)) {
            map.get(key).addAll(annos);
        } else {
            map.put(key, annos);
        }
    }

    /**
     * Just like Map.put, but merges with any existing value for the given key, instead ef replacing
     * it.
     */
    private static void putNew(
            Map<Element, AnnotatedTypeMirror> m, Element key, AnnotatedTypeMirror value) {
        if (key == null) {
            throw new BugInCF("StubParser: key is null");
        }
        if (m.containsKey(key)) {
            AnnotatedTypeMirror value2 = m.get(key);
            AnnotatedTypeMerger.merge(value, value2);
            m.put(key, value2);
        } else {
            m.put(key, value);
        }
    }

    /** Just like Map.putAll, but merges with existing values using {@link #putNew}. */
    private static <K, V> void putAllNew(Map<K, V> m, Map<K, V> m2) {
        for (Map.Entry<K, V> e2 : m2.entrySet()) {
            putNoOverride(m, e2.getKey(), e2.getValue());
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Issue warnings
    ///

    // The warnings that have been issued so far.
    private static final Set<String> warnings = new HashSet<>();

    /**
     * Issues the given warning about missing elements, only if it has not been previously issued
     * and the -AstubWarnIfNotFound command-line argument was passed.
     */
    private void stubWarnNotFound(String warning) {
        if (warnings.add(warning) && (warnIfNotFound || debugStubParser)) {
            processingEnv
                    .getMessager()
                    .printMessage(javax.tools.Diagnostic.Kind.WARNING, "StubParser: " + warning);
        }
    }

    /**
     * Issues the given warning about overwriting bytecode, only if it has not been previously
     * issued and the -AstubWarnIfOverwritesBytecode command-line argument was passed.
     */
    private void stubWarnOverwritesBytecode(String warning) {
        if (warnings.add(warning) && (warnIfStubOverwritesBytecode || debugStubParser)) {
            processingEnv
                    .getMessager()
                    .printMessage(javax.tools.Diagnostic.Kind.WARNING, "StubParser: " + warning);
        }
    }

    /**
     * Issues a warning, only if it has not been previously issued.
     *
     * @param warning a format string
     * @param args the arguments for {@code warning}
     */
    private void stubWarn(String warning, Object... args) {
        warning = String.format(warning, args);
        if (warnings.add(warning)) {
            processingEnv
                    .getMessager()
                    .printMessage(javax.tools.Diagnostic.Kind.WARNING, "StubParser: " + warning);
        }
    }

    private void stubDebug(String warning) {
        if (warnings.add(warning) && debugStubParser) {
            processingEnv
                    .getMessager()
                    .printMessage(javax.tools.Diagnostic.Kind.NOTE, "StubParser: " + warning);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Parse state
    ///

    /** Represents a class: its package name and simple name. */
    private static class FqName {
        /** Name of the package being parsed, or null. */
        public String packageName;

        /** Name of the type being parsed. Includes outer class names if any. */
        public String className;

        public FqName(String packageName, String className) {
            this.packageName = packageName;
            this.className = className;
        }

        /** Fully-qualified name of the class. */
        @Override
        public String toString() {
            if (packageName == null) {
                return className;
            } else {
                return packageName + "." + className;
            }
        }
    }
}
