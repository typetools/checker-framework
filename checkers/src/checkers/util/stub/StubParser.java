package checkers.util.stub;

import java.io.InputStream;
import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.*;
import checkers.util.AnnotationUtils;

import japa.parser.JavaParser;
import japa.parser.ast.*;
import japa.parser.ast.body.*;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.type.*;

// Main entry points are parse() and parse(Map<Element, AnnotatedTypeMirror>).

public class StubParser {
    /** file being parsed, to make error messages more informative */
    final String filename;
    final IndexUnit index;
    final AnnotatedTypeFactory atypeFactory;
    final AnnotationUtils annoUtils;
    final ProcessingEnvironment env;
    final Elements elements;

    final Map<String, AnnotationMirror> annotations;

    public StubParser(String filename, InputStream inputStream, AnnotatedTypeFactory factory, ProcessingEnvironment env) {
        this.filename = filename;
        try {
            this.index = JavaParser.parse(inputStream);
        } catch (Exception e) {
            throw new Error(e);
        }
        this.atypeFactory = factory;
        this.env = env;
        this.annoUtils = AnnotationUtils.getInstance(env);
        this.elements = env.getElementUtils();
        annotations = getSupportedAnnotations();
    }

    private Map<String, AnnotationMirror> annoWithinPackage(String packageName) {
        AnnotationUtils annoUtils = AnnotationUtils.getInstance(env);
        Map<String, AnnotationMirror> r = new HashMap<String, AnnotationMirror>();

        PackageElement pkg = this.elements.getPackageElement(packageName);
        if (pkg == null)
            return r;

        for (TypeElement typeElm : ElementFilter.typesIn(pkg.getEnclosedElements())) {
            if (typeElm.getKind() == ElementKind.ANNOTATION_TYPE) {
                AnnotationMirror anno = annoUtils.fromName(typeElm.getQualifiedName());
                r.put(typeElm.getSimpleName().toString(), anno);
            }
        }

        return r;
    }

    private Map<String, AnnotationMirror> getSupportedAnnotations() {
        assert !index.getCompilationUnits().isEmpty();
        CompilationUnit cu = index.getCompilationUnits().get(0);
        AnnotationUtils annoUtils = AnnotationUtils.getInstance(env);

        Map<String, AnnotationMirror> result = new HashMap<String, AnnotationMirror>();

        if (cu.getImports() == null)
            return result;

        for (ImportDeclaration importDecl : cu.getImports()) {
            String imported = importDecl.getName().toString();
            try {
                if (!importDecl.isAsterisk()) {
                    AnnotationMirror anno = annoUtils.fromName(imported);
                    if (anno != null ) {
                        Element annoElt = anno.getAnnotationType().asElement();
                        result.put(annoElt.getSimpleName().toString(), anno);
                    } else {
                        System.err.println("StubParser: Could not load import: " + imported);
                    }
                } else {
                    result.putAll(annoWithinPackage(imported));
                }
            } catch (AssertionError error) {
                System.err.println("StubParser: " + error);
            }
        }
        return result;
    }

    // One of the two main entry points
    public Map<Element, AnnotatedTypeMirror> parse() {
        Map<Element, AnnotatedTypeMirror> result = new HashMap<Element, AnnotatedTypeMirror>();
        parse(result);
        return result;
    }

    // One of the two main entry points
    public void parse(Map<Element, AnnotatedTypeMirror> result) {
        parse(this.index, result);
    }

    private void parse(IndexUnit index, Map<Element, AnnotatedTypeMirror> result) {
        for (CompilationUnit cu : index.getCompilationUnits())
            parse(cu, result);
    }

    private void parse(CompilationUnit cu, Map<Element, AnnotatedTypeMirror> result) {
        final String packageName;
        if (cu.getPackage() == null)
            packageName = null;
        else
            packageName = cu.getPackage().getName().toString();
        if (cu.getTypes() != null) {
            for (TypeDeclaration typeDecl : cu.getTypes())
                parse(typeDecl, packageName, result);
        }
    }

    // typeDecl's name may be a binary name such as A$B
    // That is a hack because the StubParser does not handle nested classes.
    private void parse(TypeDeclaration typeDecl, String packageName, Map<Element, AnnotatedTypeMirror> result) {
        // System.out.printf("parse(%s.%s, ...)%n", packageName, typeDecl.getName());
        String typeName = (packageName == null ? "" : packageName + ".") + typeDecl.getName().replace('$', '.');
        TypeElement typeElt = elements.getTypeElement(typeName);
        // couldn't find type.  not in class path
        // TODO: Should throw exception?!
        if (typeElt == null
                || typeElt.getKind() == ElementKind.ENUM
                || typeElt.getKind() == ElementKind.ANNOTATION_TYPE) {
            // System.err.println("StubParser: Type not found: " + typeName);
            return;
        }

        if (typeDecl instanceof ClassOrInterfaceDeclaration) {
            parseType((ClassOrInterfaceDeclaration)typeDecl, typeElt, result);
        }

        Map<Element, BodyDeclaration> elementsToDecl = mapMembers(typeElt, typeDecl);
        for (Map.Entry<Element, BodyDeclaration> entry : elementsToDecl.entrySet()) {
            final Element elt = entry.getKey();
            final BodyDeclaration decl = entry.getValue();
            if (elt.getKind().isField())
                parseField((FieldDeclaration)decl, (VariableElement)elt, result);
            else if (elt.getKind() == ElementKind.CONSTRUCTOR)
                parseConstructor((ConstructorDeclaration)decl, (ExecutableElement)elt, result);
            else if (elt.getKind() == ElementKind.METHOD)
                parseMethod((MethodDeclaration)decl, (ExecutableElement)elt, result);
            else { /* do nothing */
                System.err.println("Ignoring: " + elt);
            }
        }
    }

    private void parseType(ClassOrInterfaceDeclaration decl, TypeElement elt, Map<Element, AnnotatedTypeMirror> result) {
        AnnotatedDeclaredType type = atypeFactory.fromElement(elt);
        annotate(type, decl.getAnnotations());
        annotateParameters(type.getTypeArguments(), decl.getTypeParameters());
        annotateSupertypes(decl, type);
        result.put(elt, type);
    }

    private void annotateSupertypes(ClassOrInterfaceDeclaration typeDecl, AnnotatedDeclaredType type) {
        if (typeDecl.getExtends() != null) {
            for (ClassOrInterfaceType superType : typeDecl.getExtends()) {
                AnnotatedDeclaredType foundType = findType(superType, type.directSuperTypes());
                assert foundType != null;
                if (foundType != null) annotate(foundType, superType);
            }
        }
        if (typeDecl.getImplements() != null) {
            for (ClassOrInterfaceType superType : typeDecl.getImplements()) {
                AnnotatedDeclaredType foundType = findType(superType, type.directSuperTypes());
                assert foundType != null;
                if (foundType != null) annotate(foundType, superType);
            }
        }
    }

    private void parseMethod(MethodDeclaration decl, ExecutableElement elt,
            Map<Element, AnnotatedTypeMirror> result) {
        AnnotatedExecutableType methodType = atypeFactory.fromElement(elt);
        annotateParameters(methodType.getTypeVariables(), decl.getTypeParameters());
        annotate(methodType.getReturnType(), decl.getType());

        for (int i = 0; i < methodType.getParameterTypes().size(); ++i) {
            AnnotatedTypeMirror paramType = methodType.getParameterTypes().get(i);
            Parameter param = decl.getParameters().get(i);
            if (param.isVarArgs()) {
                // workaround
                assert paramType.getKind() == TypeKind.ARRAY;
                annotate(((AnnotatedArrayType)paramType).getComponentType(), param.getType());
            } else {
                annotate(paramType, param.getType());
            }
        }

        annotate(methodType.getReceiverType(), decl.getReceiverAnnotations());

        result.put(elt, methodType);
    }

    private List<AnnotatedTypeMirror> arrayList(AnnotatedArrayType atype) {
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
        List<AnnotatedTypeMirror> arrayTypes = arrayList(atype);
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
            ExecutableElement elt, Map<Element, AnnotatedTypeMirror> result) {
        AnnotatedExecutableType methodType = atypeFactory.fromElement(elt);

        for (int i = 0; i < methodType.getParameterTypes().size(); ++i) {
            AnnotatedTypeMirror paramType = methodType.getParameterTypes().get(i);
            Parameter param = decl.getParameters().get(i);
            annotate(paramType, param.getType());
        }

        annotate(methodType.getReceiverType(), decl.getReceiverAnnotations());

        result.put(elt, methodType);
    }

    private void parseField(FieldDeclaration decl,
            VariableElement elt, Map<Element, AnnotatedTypeMirror> result) {
        AnnotatedTypeMirror fieldType = atypeFactory.fromElement(elt);
        annotate(fieldType, decl.getType());
        result.put(elt, fieldType);
    }

    private void annotate(AnnotatedTypeMirror type, List<AnnotationExpr> annotations) {
        if (annotations == null)
            return;
        for (AnnotationExpr annotation : annotations) {
            String annoName = StubUtil.getAnnotationName(annotation);
            AnnotationMirror annoMirror = this.annotations.get(annoName);
            if (annoMirror != null)
                type.addAnnotation(annoMirror);
        }
    }

    private void annotateParameters(List<? extends AnnotatedTypeMirror> typeArguments,
            List<TypeParameter> typeParameters) {
        if (typeParameters == null)
            return;

        for (int i = 0; i < typeParameters.size(); ++i) {
            TypeParameter param = typeParameters.get(i);
            AnnotatedTypeVariable paramType = (AnnotatedTypeVariable)typeArguments.get(i);

            if (param.getTypeBound() != null && param.getTypeBound().size() == 1) {
                annotate(paramType.getUpperBound(), param.getTypeBound().get(0));
            }
        }
    }

    static Set<String> nestedClassWarnings = new HashSet<String>();

    private Map<Element, BodyDeclaration> mapMembers(TypeElement typeElt, TypeDeclaration typeDecl) {
        assert (typeElt.getSimpleName().contentEquals(typeDecl.getName())
                || typeDecl.getName().endsWith("$" + typeElt.getSimpleName().toString()))
            : String.format("%s  %s", typeElt.getSimpleName(), typeDecl.getName());

        Map<Element, BodyDeclaration> result = new HashMap<Element, BodyDeclaration>();

        for (BodyDeclaration member : typeDecl.getMembers()) {
            if (member instanceof MethodDeclaration) {
                Element elt = findElement(typeElt, (MethodDeclaration)member);
                result.put(elt, member);
            } else if (member instanceof ConstructorDeclaration) {
                Element elt = findElement(typeElt, (ConstructorDeclaration)member);
                result.put(elt, member);
            } else if (member instanceof FieldDeclaration) {
                FieldDeclaration fieldDecl = (FieldDeclaration)member;
                for (VariableDeclarator var : fieldDecl.getVariables())
                    result.put(findElement(typeElt, var), fieldDecl);
            } else if (member instanceof ClassOrInterfaceDeclaration) {
                // TODO: handle nested classes
                ClassOrInterfaceDeclaration ciDecl = (ClassOrInterfaceDeclaration) member;
                String nestedClass = typeDecl.getName() + "." + ciDecl.getName();
                if (nestedClassWarnings.add(nestedClass)) { // avoid duplicate warnings
                    System.err.printf("Warning: ignoring nested class in %s at line %d:%n    class %s { class %s { ... } }%n", filename, ciDecl.getBeginLine(), typeDecl.getName(), ciDecl.getName());
                    System.err.printf("  Instead, write the nested class as a top-level class:%n    class %s { ... }%n    class %s$%s { ... }%n", typeDecl.getName(), typeDecl.getName(), ciDecl.getName());
                }
            } else {
                System.out.printf("StubParser: Ignoring element of type %s in mapMembers", member.getClass());
            }
        }
        result.remove(null);
        return result;
    }

    private AnnotatedDeclaredType findType(ClassOrInterfaceType type, List<AnnotatedDeclaredType> types) {
        String typeString = type.getName();
        for (AnnotatedDeclaredType superType : types) {
            if (superType.getUnderlyingType().asElement().getSimpleName().contentEquals(typeString))
                return superType;
        }
        // System.err.println("StubParser: Type " + typeString + " not found");
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
                    && StubUtil.toString(method).equals(wantedMethodString))
                return method;
        }
        // System.err.println("StubParser: Method " + wantedMethodString + " not found in type " + typeElt);
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
        // System.err.println("StubParser: Constructor " + wantedMethodString + " not found in type " + typeElt);
        return null;
    }

    public VariableElement findElement(TypeElement typeElt, VariableDeclarator variable) {
        final String fieldName = variable.getId().getName();
        for (VariableElement field : ElementFilter.fieldsIn(typeElt.getEnclosedElements())) {
            if (fieldName.contains(field.getSimpleName()))
                return field;
        }
        // System.err.println("StubParser: Field " + fieldName + " not found in type " + typeElt);
        return null;
    }
}
