package checkers.util.stub;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

public class StubParser {
    final IndexUnit index;
    final AnnotatedTypeFactory atypeFactory;
    final AnnotationUtils annoUtils;
    final ProcessingEnvironment env;
    final Elements elements;

    final Map<String, AnnotationMirror> annotations;

    public StubParser(InputStream inputStream, AnnotatedTypeFactory factory, ProcessingEnvironment env) {
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
                    Element annoElt = anno.getAnnotationType().asElement();
                    result.put(annoElt.getSimpleName().toString(), anno);
                } else {
                    result.putAll(annoWithinPackage(imported));
                }
            } catch (AssertionError error) {
                // do nothing
            }
        }
        return result;
    }

    public Map<Element, AnnotatedTypeMirror> parse() {
        Map<Element, AnnotatedTypeMirror> result = new HashMap<Element, AnnotatedTypeMirror>();
        parse(result);
        return result;
    }

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

    private void parse(TypeDeclaration type, String packageName, Map<Element, AnnotatedTypeMirror> result) {
        String typeName = (packageName == null ? "" : packageName + ".") + type.getName();
        TypeElement typeElt = elements.getTypeElement(typeName);
        // couldn't find type.  not in class path
        // TODO: Should throw exception?!
        if (typeElt == null
                || typeElt.getKind() == ElementKind.ENUM
                || typeElt.getKind() == ElementKind.ANNOTATION_TYPE)
            return;

        if (type instanceof ClassOrInterfaceDeclaration) {
            parseType((ClassOrInterfaceDeclaration)type, typeElt, result);
        }

        Map<Element, BodyDeclaration> elementsToDecl = mapMembers(typeElt, type);
        for (Map.Entry<Element, BodyDeclaration> entry : elementsToDecl.entrySet()) {
            final Element elt = entry.getKey();
            final BodyDeclaration decl = entry.getValue();
            if (elt.getKind().isField())
                parseField((FieldDeclaration)decl, (VariableElement)elt, result);
            else if (elt.getKind() == ElementKind.CONSTRUCTOR)
                parseConstructor((ConstructorDeclaration)decl, (ExecutableElement)elt, result);
            else if (elt.getKind() == ElementKind.METHOD)
                parseMethod((MethodDeclaration)decl, (ExecutableElement)elt, result);
            else { /* do nothing */ }
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
        annotateParameters(methodType.getParameterTypes(), decl.getTypeParameters());
        annotate(methodType.getReturnType(), decl.getType());

        for (int i = 0; i < methodType.getParameterTypes().size(); ++i) {
            AnnotatedTypeMirror paramType = methodType.getParameterTypes().get(i);
            Parameter param = decl.getParameters().get(i);
            annotate(paramType, param.getType());
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

    private void annotate(AnnotatedTypeMirror atype, Type typeDef) {
        if (atype.getKind() == TypeKind.ARRAY) {
            annotateAsArray((AnnotatedArrayType)atype, (ReferenceType)typeDef);
            return;
        }
        if (typeDef.getAnnotations() != null)
            annotate(atype, typeDef.getAnnotations());
        if (atype.getKind() == TypeKind.DECLARED
                && typeDef instanceof ClassOrInterfaceType) {
            AnnotatedDeclaredType adeclType = (AnnotatedDeclaredType)atype;
            ClassOrInterfaceType declType = (ClassOrInterfaceType)typeDef;
            if (declType.getTypeArgs() != null
                    && !declType.getTypeArgs().isEmpty()
                    && adeclType.isParameterized()) {
                assert declType.getTypeArgs().size() == adeclType.getTypeArguments().size();
                for (int i = 0; i < declType.getTypeArgs().size(); ++i) {
                    annotate(adeclType.getTypeArguments().get(i),
                            declType.getTypeArgs().get(i));
                }
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

    private void annotateParameters(List<AnnotatedTypeMirror> typeArguments,
            List<TypeParameter> typeParameters) {
        if (typeParameters == null)
            return;

        for (int i = 0; i < typeParameters.size(); ++i) {
            TypeParameter param = typeParameters.get(i);
            AnnotatedTypeVariable paramType = (AnnotatedTypeVariable)typeArguments.get(i);

            if (param.getTypeBound().size() == 1) {
                annotate(paramType.getUpperBound(), param.getTypeBound().get(0));
            }
        }
    }

    public Map<Element, BodyDeclaration> mapMembers(TypeElement typeElt, TypeDeclaration typeDecl) {
        assert typeElt.getSimpleName().contentEquals(typeDecl.getName());

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
        return null;
    }
    public ExecutableElement findElement(TypeElement typeElt, MethodDeclaration methodDecl) {
        final String wantedMethodName = methodDecl.getName();
        final int wantedMethodParams =
            (methodDecl.getParameters() == null) ? 0 :
                methodDecl.getParameters().size();
        final String wantedMethodString = StubUtil.toString(methodDecl);
        for (ExecutableElement method : ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
            // do hearustics first
            if (wantedMethodParams == method.getParameters().size()
                    && wantedMethodName.contentEquals(method.getSimpleName())
                    && StubUtil.toString(method).equals(wantedMethodString))
                return method;
        }
        return null;
    }

    public ExecutableElement findElement(TypeElement typeElt, ConstructorDeclaration methodDecl) {
        final int wantedMethodParams =
            (methodDecl.getParameters() == null) ? 0 :
                methodDecl.getParameters().size();
        final String wantedMethodString = StubUtil.toString(methodDecl);
        for (ExecutableElement method : ElementFilter.constructorsIn(typeElt.getEnclosedElements())) {
            // do hearustics first
            if (wantedMethodParams == method.getParameters().size()
                    && StubUtil.toString(method).equals(wantedMethodString))
                return method;
        }
        return null;
    }

    public VariableElement findElement(TypeElement typeElt, VariableDeclarator variable) {
        final String fieldName = variable.getId().getName();
        for (VariableElement field : ElementFilter.fieldsIn(typeElt.getEnclosedElements())) {
            if (fieldName.contains(field.getSimpleName()))
                return field;
        }
        return null;
    }
}
