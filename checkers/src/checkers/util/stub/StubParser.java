package checkers.util.stub;

import java.io.InputStream;
import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.*;
import checkers.util.AnnotationUtils;
import checkers.util.ElementUtils;
import checkers.util.AnnotationUtils.AnnotationBuilder;
import checkers.util.TreeUtils;

import japa.parser.JavaParser;
import japa.parser.ast.*;
import japa.parser.ast.body.*;
import japa.parser.ast.expr.*;
import japa.parser.ast.type.*;

// Main entry point is:
// parse(Map<Element, AnnotatedTypeMirror>, Map<Element, Set<AnnotationMirror>>)

public class StubParser {

    /**
     * Whether to print warnings about types/members that were not found.
     * The warning is about whether a class/field in the stub file is not
     * found on the user's real classpath.  Since the stub file may contain
     * packages that are not on the classpath, this can be OK, so default to
     * false.
     */
    private static final boolean warnIfNotFound = false;

    private static final boolean debugStubParser = false;

    /** The file being parsed (makes error messages more informative). */
    private final String filename;

    private final IndexUnit index;
    private final ProcessingEnvironment env;
    private final AnnotatedTypeFactory atypeFactory;
    private final AnnotationUtils annoUtils;
    private final Elements elements;

    /**
     * The supported annotations. Keys are simple (unqualified) names.
     * (This may be a problem in the unlikely occurrence that a
     * type-checker supports two annotations with the same simple name.)
     */
    private final Map<String, AnnotationMirror> supportedAnnotations;

    public StubParser(String filename, InputStream inputStream, AnnotatedTypeFactory factory, ProcessingEnvironment env) {
        this.filename = filename;
        IndexUnit parsedindex;
        try {
            parsedindex = JavaParser.parse(inputStream);
        } catch (Exception e) {
            SourceChecker.errorAbort("StubParser: exception from JavaParser.parse", e);
            parsedindex = null; // dead code, but needed for def. assignment checks
        }
        this.index = parsedindex;
        this.atypeFactory = factory;
        this.env = env;
        this.annoUtils = AnnotationUtils.getInstance(env);
        this.elements = env.getElementUtils();
        supportedAnnotations = getSupportedAnnotations();
        if (supportedAnnotations.isEmpty()) {
            stubWarning("No supported annotations found! This likely means your stub file doesn't import them correctly.");
        }
    }

    /** All annotations defined in the package.  Keys are simple names. */
    private Map<String, AnnotationMirror> annosInPackage(String packageName) {
        Map<String, AnnotationMirror> r = new HashMap<String, AnnotationMirror>();

        PackageElement pkg = this.elements.getPackageElement(packageName);
        if (pkg == null)
            return r;

        for (TypeElement typeElm : ElementFilter.typesIn(pkg.getEnclosedElements())) {
            if (typeElm.getKind() == ElementKind.ANNOTATION_TYPE) {
                AnnotationMirror anno = annoUtils.fromName(typeElm.getQualifiedName());
                putNew(r, typeElm.getSimpleName().toString(), anno);
            }
        }

        return r;
    }

    /** @see #supportedAnnotations */
    private Map<String, AnnotationMirror> getSupportedAnnotations() {
        assert !index.getCompilationUnits().isEmpty();
        CompilationUnit cu = index.getCompilationUnits().get(0);

        Map<String, AnnotationMirror> result = new HashMap<String, AnnotationMirror>();

        if (cu.getImports() == null)
            return result;

        for (ImportDeclaration importDecl : cu.getImports()) {
            String imported = importDecl.getName().toString();
            try {
                if (importDecl.isAsterisk()) {
                    putAllNew(result, annosInPackage(imported));
                } else {
                    AnnotationMirror anno = annoUtils.fromName(imported);
                    if (anno != null ) {
                        Element annoElt = anno.getAnnotationType().asElement();
                        putNew(result, annoElt.getSimpleName().toString(), anno);
                    } else {
                        if (warnIfNotFound || debugStubParser)
                            stubWarning("Could not load import: " + imported);
                    }
                }
            } catch (AssertionError error) {
                stubWarning("" + error);
            }
        }
        return result;
    }

    // The main entry point.  Side-effects the arguments.
    public void parse(Map<Element, AnnotatedTypeMirror> atypes, Map<String, Set<AnnotationMirror>> declAnnos) {
        parse(this.index, atypes, declAnnos);
    }

    private void parse(IndexUnit index, Map<Element, AnnotatedTypeMirror> atypes, Map<String, Set<AnnotationMirror>> declAnnos) {
        for (CompilationUnit cu : index.getCompilationUnits())
            parse(cu, atypes, declAnnos);
    }

    private CompilationUnit theCompilationUnit;

    private void parse(CompilationUnit cu, Map<Element, AnnotatedTypeMirror> atypes, Map<String, Set<AnnotationMirror>> declAnnos) {
        theCompilationUnit = cu;
        final String packageName;
        if (cu.getPackage() == null) {
            packageName = null;
        } else {
            packageName = cu.getPackage().getName().toString();
            parsePackage(cu.getPackage(), atypes, declAnnos);
        }
        if (cu.getTypes() != null) {
            for (TypeDeclaration typeDecl : cu.getTypes())
                parse(typeDecl, packageName, atypes, declAnnos);
        }
    }

    private void parsePackage(PackageDeclaration packDecl, Map<Element, AnnotatedTypeMirror> atypes, Map<String, Set<AnnotationMirror>> declAnnos) {
        assert(packDecl != null);
        String packageName = packDecl.getName().toString();
        Element elem = elements.getPackageElement(packageName);
        // If the element lookup fails, it's because we have an annotation for a package that isn't on the classpath, which is fine.
        if (elem != null) {
            annotateDecl(declAnnos, elem, packDecl.getAnnotations());
        }
        // TODO: Handle atypes???
    }

    // typeDecl's name may be a binary name such as "A$B".
    // That is a hack because the StubParser does not handle nested classes.
    private void parse(TypeDeclaration typeDecl, String packageName, Map<Element, AnnotatedTypeMirror> atypes, Map<String, Set<AnnotationMirror>> declAnnos) {
        // Fully-qualified name of the type being parsed
        String typeName = (packageName == null ? "" : packageName + ".") + typeDecl.getName().replace('$', '.');
        TypeElement typeElt = elements.getTypeElement(typeName);
        // couldn't find type.  not in class path
        // TODO: Should throw exception?!
        if (typeElt == null) {
            if (warnIfNotFound || debugStubParser)
                stubWarning("Type not found: " + typeName);
            return;
        }

        if (typeElt.getKind() == ElementKind.ENUM) {
            if (warnIfNotFound || debugStubParser)
                stubWarning("Skipping enum type: " + typeName);
        } else if (typeElt.getKind() == ElementKind.ANNOTATION_TYPE) {
            if (warnIfNotFound || debugStubParser)
                stubWarning("Skipping annotation type: " + typeName);
        } else if (typeDecl instanceof ClassOrInterfaceDeclaration) {
            parseType((ClassOrInterfaceDeclaration)typeDecl, typeElt, atypes, declAnnos);
        } // else it's an EmptyTypeDeclaration.  TODO:  An EmptyTypeDeclaration can have annotations, right?

        Map<Element, BodyDeclaration> elementsToDecl = getMembers(typeElt, typeDecl);
        for (Map.Entry<Element, BodyDeclaration> entry : elementsToDecl.entrySet()) {
            final Element elt = entry.getKey();
            final BodyDeclaration decl = entry.getValue();
            if (elt.getKind().isField())
                parseField((FieldDeclaration)decl, (VariableElement)elt, atypes, declAnnos);
            else if (elt.getKind() == ElementKind.CONSTRUCTOR)
                parseConstructor((ConstructorDeclaration)decl, (ExecutableElement)elt, atypes, declAnnos);
            else if (elt.getKind() == ElementKind.METHOD)
                parseMethod((MethodDeclaration)decl, (ExecutableElement)elt, atypes, declAnnos);
            else { /* do nothing */
                System.err.println("StubParser ignoring: " + elt);
            }
        }
    }

    private void parseType(ClassOrInterfaceDeclaration decl, TypeElement elt, Map<Element, AnnotatedTypeMirror> atypes, Map<String, Set<AnnotationMirror>> declAnnos) {
        annotateDecl(declAnnos, elt, decl.getAnnotations());
        AnnotatedDeclaredType type = atypeFactory.fromElement(elt);
        annotate(type, decl.getAnnotations());
        {
            List<? extends AnnotatedTypeMirror> typeArguments = type.getTypeArguments();
            List<TypeParameter> typeParameters = decl.getTypeParameters();
            /// It can be the case that args=[] and params=null.
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
                    System.out.printf(String.format("parseType:  mismatched sizes for params and args%n  decl=%s%n  typeParameters=%s%n  elt=%s (%s)%n  type=%s (%s)%n  typeArguments (size %d)=%s%n  theCompilationUnit=%s%nEnd of Message%n",
                                              decl, typeParameters,
                                              elt, elt.getClass(), type, type.getClass(), typeArguments.size(), typeArguments,
                                              theCompilationUnit));
                    System.out.flush();
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
                    System.out.printf(String.format("parseType:  mismatched sizes for params and args%n  decl=%s%n  typeParameters (size %d)=%s%n  elt=%s (%s)%n  type=%s (%s)%n  typeArguments (size %d)=%s%n  theCompilationUnit=%s%nEnd of Message%n",
                                              decl, typeParameters.size(), typeParameters,
                                              elt, elt.getClass(), type, type.getClass(), typeArguments.size(), typeArguments,
                                              theCompilationUnit));
                    System.out.flush();
                }
                /*
                throw new Error(String.format("parseType:  mismatched sizes for params and args%n  decl=%s%n  typeParameters (size %d)=%s%n  elt=%s (%s)%n  type=%s (%s)%n  typeArguments (size %d)=%s%n",
                                              decl, typeParameters.size(), typeParameters,
                                              elt, elt.getClass(), type, type.getClass(), typeArguments.size(), typeArguments));
                */
            }
        }
        annotateParameters(type.getTypeArguments(), decl.getTypeParameters());
        annotateSupertypes(decl, type);
        putNew(atypes, elt, type);
    }

    private void annotateSupertypes(ClassOrInterfaceDeclaration typeDecl, AnnotatedDeclaredType type) {
        if (typeDecl.getExtends() != null) {
            for (ClassOrInterfaceType superType : typeDecl.getExtends()) {
                AnnotatedDeclaredType foundType = findType(superType, type.directSuperTypes());
                assert foundType != null : "StubParser: could not find superclass " + superType + " from type " + type;
                if (foundType != null) annotate(foundType, superType);
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
                if (foundType != null) annotate(foundType, superType);
            }
        }
    }

    private void parseMethod(MethodDeclaration decl, ExecutableElement elt,
            Map<Element, AnnotatedTypeMirror> atypes, Map<String, Set<AnnotationMirror>> declAnnos) {
        annotateDecl(declAnnos, elt, decl.getAnnotations());
        // StubParser parses all annotations in type annotation position as type annotations
        annotateDecl(declAnnos, elt, decl.getType().getAnnotations());
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

        annotate(methodType.getReceiverType(), decl.getReceiverAnnotations());

        putNew(atypes, elt, methodType);
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
        assert typeDef.getArrayCount() == arrayTypes.size() - 1;
        for (int i = 0; i < typeDef.getArrayCount(); ++i) {
            List<AnnotationExpr> annotations = typeDef.getAnnotationsAtLevel(i);
            if (annotations != null) {
                annotate(arrayTypes.get(i), annotations);
            }
        }

        // handle generic type on base
        annotate(arrayTypes.get(arrayTypes.size() - 1), typeDef.getAnnotations());
    }

    private ClassOrInterfaceType unwrapDeclaredType(Type type) {
        if (type instanceof ClassOrInterfaceType)
            return (ClassOrInterfaceType)type;
        else if (type instanceof ReferenceType
                && ((ReferenceType)type).getArrayCount() == 0)
            return unwrapDeclaredType(((ReferenceType)type).getType());
        else
            return null;
    }

    private void annotate(AnnotatedTypeMirror atype, Type typeDef) {
        if (atype.getKind() == TypeKind.ARRAY) {
            annotateAsArray((AnnotatedArrayType)atype, (ReferenceType)typeDef);
            return;
        }
        if (typeDef.getAnnotations() != null)
            annotate(atype, typeDef.getAnnotations());
        ClassOrInterfaceType declType = unwrapDeclaredType(typeDef);
        if (atype.getKind() == TypeKind.DECLARED
                && declType != null) {
            AnnotatedDeclaredType adeclType = (AnnotatedDeclaredType)atype;
            if (declType.getTypeArgs() != null
                    && !declType.getTypeArgs().isEmpty()
                    && adeclType.isParameterized()) {
                assert declType.getTypeArgs().size() == adeclType.getTypeArguments().size();
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
            ExecutableElement elt, Map<Element, AnnotatedTypeMirror> atypes, Map<String, Set<AnnotationMirror>> declAnnos) {
        annotateDecl(declAnnos, elt, decl.getAnnotations());
        AnnotatedExecutableType methodType = atypeFactory.fromElement(elt);

        for (int i = 0; i < methodType.getParameterTypes().size(); ++i) {
            AnnotatedTypeMirror paramType = methodType.getParameterTypes().get(i);
            Parameter param = decl.getParameters().get(i);
            annotate(paramType, param.getType());
        }

        annotate(methodType.getReceiverType(), decl.getReceiverAnnotations());

        putNew(atypes, elt, methodType);
    }

    private void parseField(FieldDeclaration decl,
            VariableElement elt, Map<Element, AnnotatedTypeMirror> atypes, Map<String, Set<AnnotationMirror>> declAnnos) {
        annotateDecl(declAnnos, elt, decl.getAnnotations());
        // StubParser parses all annotations in type annotation position as type annotations
        annotateDecl(declAnnos, elt, decl.getType().getAnnotations());
        AnnotatedTypeMirror fieldType = atypeFactory.fromElement(elt);
        annotate(fieldType, decl.getType());
        putNew(atypes, elt, fieldType);
    }

    private void annotate(AnnotatedTypeMirror type, List<AnnotationExpr> annotations) {
        if (annotations == null)
            return;
        for (AnnotationExpr annotation : annotations) {
            AnnotationMirror annoMirror = getAnnotation(annotation, supportedAnnotations, env);
            if (annoMirror != null)
                type.addAnnotation(annoMirror);
        }
    }

    private void annotateDecl(Map<String, Set<AnnotationMirror>> declAnnos, Element elt, List<AnnotationExpr> annotations) {
        if (annotations == null)
            return;
        Set<AnnotationMirror> annos = AnnotationUtils.createAnnotationSet();
        for (AnnotationExpr annotation : annotations) {
            AnnotationMirror annoMirror = getAnnotation(annotation, supportedAnnotations, env);
            if (annoMirror != null)
                annos.add(annoMirror);
        }
        String key = ElementUtils.getVerboseName(elt);
        declAnnos.put(key, annos);
    }

    private void annotateParameters(List<? extends AnnotatedTypeMirror> typeArguments,
            List<TypeParameter> typeParameters) {
        if (typeParameters == null)
            return;

        if (typeParameters.size() != typeArguments.size()) {
            System.out.printf("annotateParameters: mismatched sizes%n  typeParameters (size %d)=%s%n  typeArguments (size %d)=%s%n", typeParameters.size(), typeParameters, typeArguments.size(), typeArguments);
        }
        for (int i = 0; i < typeParameters.size(); ++i) {
            TypeParameter param = typeParameters.get(i);
            AnnotatedTypeVariable paramType = (AnnotatedTypeVariable)typeArguments.get(i);

            if (param.getTypeBound() != null && param.getTypeBound().size() == 1) {
                annotate(paramType.getUpperBound(), param.getTypeBound().get(0));
            }
        }
    }

    private final static Set<String> nestedClassWarnings = new HashSet<String>();

    private Map<Element, BodyDeclaration> getMembers(TypeElement typeElt, TypeDeclaration typeDecl) {
        assert (typeElt.getSimpleName().contentEquals(typeDecl.getName())
                || typeDecl.getName().endsWith("$" + typeElt.getSimpleName().toString()))
            : String.format("%s  %s", typeElt.getSimpleName(), typeDecl.getName());

        Map<Element, BodyDeclaration> result = new HashMap<Element, BodyDeclaration>();

        for (BodyDeclaration member : typeDecl.getMembers()) {
            if (member instanceof MethodDeclaration) {
                Element elt = findElement(typeElt, (MethodDeclaration)member);
                putNew(result, elt, member);
            } else if (member instanceof ConstructorDeclaration) {
                Element elt = findElement(typeElt, (ConstructorDeclaration)member);
                putNew(result, elt, member);
            } else if (member instanceof FieldDeclaration) {
                FieldDeclaration fieldDecl = (FieldDeclaration)member;
                for (VariableDeclarator var : fieldDecl.getVariables()) {
                    putNew(result, findElement(typeElt, var), fieldDecl);
                }
            } else if (member instanceof ClassOrInterfaceDeclaration) {
                // TODO: handle nested classes
                ClassOrInterfaceDeclaration ciDecl = (ClassOrInterfaceDeclaration) member;
                String nestedClass = typeDecl.getName() + "." + ciDecl.getName();
                if (nestedClassWarnings.add(nestedClass)) { // avoid duplicate warnings
                    System.err.printf("Warning: ignoring nested class in %s at line %d:%n    class %s { class %s { ... } }%n", filename, ciDecl.getBeginLine(), typeDecl.getName(), ciDecl.getName());
                    System.err.printf("  Instead, write the nested class as a top-level class:%n    class %s { ... }%n    class %s$%s { ... }%n", typeDecl.getName(), typeDecl.getName(), ciDecl.getName());
                }
            } else {
                if (warnIfNotFound || debugStubParser)
                    System.out.printf("StubParser: Ignoring element of type %s in getMembers", member.getClass());
            }
        }
        // // remove null keys, which can result from findElement returning null
        // result.remove(null);
        return result;
    }

    private AnnotatedDeclaredType findType(ClassOrInterfaceType type, List<AnnotatedDeclaredType> types) {
        String typeString = type.getName();
        for (AnnotatedDeclaredType superType : types) {
            if (superType.getUnderlyingType().asElement().getSimpleName().contentEquals(typeString))
                return superType;
        }
        if (warnIfNotFound || debugStubParser)
            stubWarning("Type " + typeString + " not found");
        if (debugStubParser)
            for (AnnotatedDeclaredType superType : types)
                System.err.printf("  %s%n", superType);
        return null;
    }

    public ExecutableElement findElement(TypeElement typeElt, MethodDeclaration methodDecl) {
        final String wantedMethodName = methodDecl.getName();
        final int wantedMethodParams =
            (methodDecl.getParameters() == null) ? 0 :
                methodDecl.getParameters().size();
        final String wantedMethodString = StubUtil.toString(methodDecl);
        for (ExecutableElement method : ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
            // do heuristics first
            if (wantedMethodParams == method.getParameters().size()
                && wantedMethodName.contentEquals(method.getSimpleName())
                && StubUtil.toString(method).equals(wantedMethodString)) {
                return method;
            }
        }
        if (warnIfNotFound || debugStubParser)
            stubWarning("Method " + wantedMethodString + " not found in type " + typeElt);
        if (debugStubParser)
            for (ExecutableElement method : ElementFilter.methodsIn(typeElt.getEnclosedElements()))
                System.err.printf("  %s%n", method);
        return null;
    }

    public ExecutableElement findElement(TypeElement typeElt, ConstructorDeclaration methodDecl) {
        final int wantedMethodParams =
            (methodDecl.getParameters() == null) ? 0 :
                methodDecl.getParameters().size();
        final String wantedMethodString = StubUtil.toString(methodDecl);
        for (ExecutableElement method : ElementFilter.constructorsIn(typeElt.getEnclosedElements())) {
            // do heuristics first
            if (wantedMethodParams == method.getParameters().size()
                    && StubUtil.toString(method).equals(wantedMethodString))
                return method;
        }
        if (warnIfNotFound || debugStubParser)
            stubWarning("Constructor " + wantedMethodString + " not found in type " + typeElt);
        if (debugStubParser)
            for (ExecutableElement method : ElementFilter.constructorsIn(typeElt.getEnclosedElements()))
                System.err.printf("  %s%n", method);
        return null;
    }

    public VariableElement findElement(TypeElement typeElt, VariableDeclarator variable) {
        final String fieldName = variable.getId().getName();
        for (VariableElement field : ElementFilter.fieldsIn(typeElt.getEnclosedElements())) {
            // field.getSimpleName() is a CharSequence, not a String
            if (fieldName.equals(field.getSimpleName().toString())) {
                return field;
            }
        }
        if (warnIfNotFound || debugStubParser)
            stubWarning("Field " + fieldName + " not found in type " + typeElt);
        if (debugStubParser)
            for (VariableElement field : ElementFilter.fieldsIn(typeElt.getEnclosedElements()))
                System.err.printf("  %s%n", field);
        return null;
    }

    /** The line separator */
    private final static String LINE_SEPARATOR = System.getProperty("line.separator").intern();

    /** Just like Map.put, but errs if the key is already in the map. */
    private static <K,V> void putNew(Map<K,V> m, K key, V value) {
        if (key == null)
            return;
        if (m.containsKey(key)) {
            // TODO: instead of failing, can we try merging the information from
            // multiple stub files?
            SourceChecker.errorAbort("StubParser: key is already in map: " + LINE_SEPARATOR
                            + "  " + key + " => " + m.get(key) + LINE_SEPARATOR
                            + "while adding: " + LINE_SEPARATOR
                            + "  " + key + " => " + value);
        }
        m.put(key, value);
    }

    /** Just like Map.putAll, but errs if any key is already in the map. */
    private static <K,V> void putAllNew(Map<K,V> m, Map<K,V> m2) {
        for (Map.Entry<K,V> e2 : m2.entrySet()) {
            putNew(m, e2.getKey(), e2.getValue());
        }
    }

    private static Set<String> warnings = new HashSet<String>();

    /** Issues the given warning, only if it has not been previously issued. */
    private static void stubWarning(String warning) {
        if (warnings.add(warning)) {
            System.err.println("StubParser: " + warning);
        }
    }

    private AnnotationMirror getAnnotation(AnnotationExpr annotation,
            Map<String, AnnotationMirror> supportedAnnotations,
            ProcessingEnvironment env) {
        AnnotationMirror annoMirror;
        if (annotation instanceof MarkerAnnotationExpr) {
            String annoName = ((MarkerAnnotationExpr)annotation).getName().getName();
            annoMirror = supportedAnnotations.get(annoName);
        } else if (annotation instanceof NormalAnnotationExpr) {
            // TODO: support @A(a=b, c=d) annotations
            // NormalAnnotationExpr nrmanno = (NormalAnnotationExpr)annotation;
            // String annoName = nrmanno.getName().getName();
            // annoMirror = supportedAnnotations.get(annoName);
            SourceChecker.errorAbort("StubParser: unhandled annotation type: " + annotation);
            annoMirror = null; // dead code
        } else if (annotation instanceof SingleMemberAnnotationExpr) {
            SingleMemberAnnotationExpr sglanno = (SingleMemberAnnotationExpr)annotation;
            String annoName = sglanno.getName().getName();
            annoMirror = supportedAnnotations.get(annoName);
            if (annoMirror == null) {
                // Not a supported qualifier -> ignore
                return null;
            }
            AnnotationUtils.AnnotationBuilder builder =
                    new AnnotationUtils.AnnotationBuilder(env, annoMirror);
            Expression valexpr = sglanno.getMemberValue();
            handleExpr(builder, "value", valexpr);
            return builder.build();
        } else {
            SourceChecker.errorAbort("StubParser: unknown annotation type: " + annotation);
            annoMirror = null; // dead code
        }
        return annoMirror;
    }

    private void handleExpr(AnnotationBuilder builder, String name,
            Expression expr) {
        if (expr instanceof FieldAccessExpr) {
            FieldAccessExpr faexpr = (FieldAccessExpr) expr;
            VariableElement elem = findVariableElement(faexpr);

            ExecutableElement var = builder.findElement(name);
            TypeMirror expected = var.getReturnType();
            if (expected.getKind() == TypeKind.DECLARED) {
                builder.setValue(name, elem);
            } else if (expected.getKind() == TypeKind.ARRAY) {
                VariableElement[] arr = { elem };
                builder.setValue(name, arr);
            } else {
                SourceChecker.errorAbort("StubParser: unhandled annotation attribute type: " + faexpr + " and expected: " + expected);
            }
        } else if (expr instanceof ArrayInitializerExpr) {
            ExecutableElement var = builder.findElement(name);
            TypeMirror expected = var.getReturnType();
            if (expected.getKind() != TypeKind.ARRAY) {
                SourceChecker.errorAbort("StubParser: unhandled annotation attribute type: " + expr + " and expected: " + expected);
            }

            ArrayInitializerExpr aiexpr = (ArrayInitializerExpr) expr;
            List<Expression> aiexprvals = aiexpr.getValues();

            VariableElement[] varelemarr = new VariableElement[aiexprvals.size()];

            Expression anaiexpr;
            for (int i = 0; i < aiexprvals.size(); ++i) {
                anaiexpr = aiexprvals.get(i);
                if (!(anaiexpr instanceof FieldAccessExpr)) {
                    SourceChecker.errorAbort("StubParser: unhandled annotation attribute type: " + expr);
                }
                varelemarr[i] = findVariableElement((FieldAccessExpr) anaiexpr);
            }

            builder.setValue(name, varelemarr);
        } else {
            SourceChecker.errorAbort("StubParser: unhandled annotation attribute type: " + expr);
        }
    }

    private VariableElement findVariableElement(FieldAccessExpr faexpr) {
        TypeElement rcvElt = elements.getTypeElement(faexpr.getScope().toString());
        if (rcvElt==null) {
            SourceChecker.errorAbort("StubParser: unknown annotation attribute receiver: " + faexpr);
        }
        VariableElement elem = TreeUtils.getField(faexpr.getScope().toString(), faexpr.getField().toString() , env);

        if (elem==null) {
            SourceChecker.errorAbort("StubParser: unknown annotation attribute field access: " + faexpr);
        }
        return elem;
    }
}
