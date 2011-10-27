package checkers.interned;

import checkers.quals.*;
import checkers.types.*;
import checkers.util.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

import java.lang.annotation.*;
import java.util.*;

public class InternedAnnotatedTypeFactory extends AnnotatedTypeFactory {

    private Types types;
    private Elements elements;
    private final Map<Element, Set<AnnotationData>> eltAnnotations;
    
    public InternedAnnotatedTypeFactory(ProcessingEnvironment env, CompilationUnitTree root) {
        super(env, root);
        this.types = env.getTypeUtils();
        this.elements = env.getElementUtils();
        this.eltAnnotations = new HashMap<Element, Set<AnnotationData>>();
    }

    public void annotateElement(Element elt, Class<? extends Annotation>
            annotation, AnnotationLocation location) {
        if (!eltAnnotations.containsKey(elt))
            eltAnnotations.put(elt, new HashSet<AnnotationData>());

        AnnotationData ad = annotations.createAnnotation(annotation.getName(), location);
        eltAnnotations.get(elt).add(ad);
    }

    /**
     * @see checkers.types.AnnotatedTypeFactory#getClass(com.sun.source.tree.Tree)
     */
    @Override
    public AnnotatedClassType getClass(Tree tree) {

        AnnotatedClassType cls = super.getClass(tree);

        // Include all annotations from the receiver on "this".
        if (tree.getKind() == Tree.Kind.IDENTIFIER) {
            Element idElt = InternalUtils.symbol(tree);
            if (idElt != null && "this".equals(idElt.getSimpleName().toString())) {
                TreePath path = trees.getPath(root, tree);
                MethodTree methodTree = TreeUtils.enclosingMethod(path);
                AnnotatedClassType receiver = getMethod(methodTree).getAnnotatedReceiverType();
                for (AnnotationData ad : receiver.getAnnotationData(true))
                    cls.include(ad); 
            }
        }

        // New classes/new arrays aren't @Interned.  This rule happens earlier
        // so as not to remove subsequently included annotations.
        if (!cls.hasAnnotationAt(Interned.class, AnnotationLocation.RAW) &&
                (tree.getKind() == Tree.Kind.NEW_CLASS ||
                tree.getKind() == Tree.Kind.NEW_ARRAY))
            cls.exclude(Interned.class);

        Element clsElt = cls.getElement();
        if (tree.getKind() == Tree.Kind.NEW_CLASS || 
                tree.getKind() == Tree.Kind.NEW_ARRAY) {
            TreePath path = trees.getPath(root, tree);
            TypeMirror type = trees.getTypeMirror(path);
            checkInterned(type, cls);
        } else if (clsElt != null) {
        
            Set<AnnotationData> thisEltAnnos = eltAnnotations.get(clsElt);
            if (thisEltAnnos != null) {
                for (AnnotationData ad : thisEltAnnos)
                    cls.annotate(ad);
            }
            
            // Enum constants are always @Interned.
            if (clsElt.getKind() == ElementKind.ENUM_CONSTANT)
                cls.include(Interned.class);

            // Anything with an enum type is always @Interned.
            TypeMirror clsType;
            if (clsElt instanceof ExecutableElement)
                clsType = ((ExecutableElement)clsElt).getReturnType();
            else
                clsType = clsElt.asType();

            if (clsType != null) {
                checkInterned(clsType, cls);
            }

            if (tree instanceof MethodInvocationTree
                    && clsType.getKind() == TypeKind.TYPEVAR) {

                MethodInvocationTree mi = (MethodInvocationTree)tree;
                if (mi.getMethodSelect().getKind() != Tree.Kind.IDENTIFIER) {

                    MemberSelectTree ms = (MemberSelectTree)mi.getMethodSelect();
                    Element e = InternalUtils.symbol(ms);

                    TreePath path = trees.getPath(root, ms.getExpression());
                    TypeMirror exprType = trees.getTypeMirror(path);

                    TypeMirror memberType;
                    if (exprType instanceof DeclaredType)
                        memberType = types.asMemberOf((DeclaredType)exprType, e);
                    else memberType = null;
                    
                    if (memberType instanceof ExecutableType) {
                        ExecutableType et = (ExecutableType)memberType;
                        checkInterned(et.getReturnType(), cls);
                    }
                }
            }
        
            if (!(tree instanceof ArrayAccessTree) // array access elt is the index!
                    && clsElt.asType().getKind().isPrimitive())
                cls.include(Interned.class);

            if (clsElt instanceof ExecutableElement &&
                    ((ExecutableElement)clsElt).getReturnType().getKind().isPrimitive())
                cls.include(Interned.class);
        }

        // FIXME: this is a quick fix for one-dimensional arrays only
        if (tree instanceof ArrayAccessTree) {
            Element arrayElt = InternalUtils.symbol(
                    ((ArrayAccessTree)tree).getExpression());
            if (arrayElt != null && arrayElt.asType().getKind() == TypeKind.ARRAY) {
                ArrayType at = (ArrayType)arrayElt.asType();
                if (at.getComponentType().getKind().isPrimitive())
                    cls.include(Interned.class);
            }
        }

        // FIXME: this is a quick fix that doesn't work on generics
        if (tree instanceof ConditionalExpressionTree) {
            ConditionalExpressionTree ceTree = (ConditionalExpressionTree)tree;
            AnnotatedClassType trueType = getClass(ceTree.getTrueExpression());
            AnnotatedClassType falseType = getClass(ceTree.getFalseExpression());
            if (trueType.hasAnnotationAt(Interned.class, AnnotationLocation.RAW)
                    && falseType.hasAnnotationAt(Interned.class, AnnotationLocation.RAW))
                cls.include(Interned.class);                
        }
            

        // Literals are always @Interned.
        if (tree instanceof LiteralTree)
            cls.include(Interned.class);

        // FIXME: except in cases of String operations?
        if (tree instanceof BinaryTree || tree instanceof UnaryTree 
                || tree instanceof InstanceOfTree)
            cls.include(Interned.class);
        
        return cls;
    }
    
    /**
     * @param clsType the type to check for @Interned-ness
     * @param cls the class type to include @Interned on if clsType is @Interned
     */
    protected void checkInterned(TypeMirror clsType, AnnotatedClassType cls) {

        // TODO: is there a better way to check if something is unboxable?
        // (It would be great if unboxedType returned null instead. -MP)
        try {
            if (types.unboxedType(clsType) != null) {
                cls.include(annotations.createAnnotation(Interned.class.getName(),
                            AnnotationLocation.RAW));
                return;
            }
        } catch (IllegalArgumentException e) {
            // Do nothing.
        }

        
        AnnotationLocation includeLoc = AnnotationLocation.RAW;
        if (clsType.getKind() == TypeKind.ARRAY) {
            clsType = ((ArrayType)clsType).getComponentType();
            includeLoc = AnnotationLocation.fromArray(new int[] { 0 });
        } 
        
        AnnotationData includeAnno = annotations.createAnnotation(Interned.class.getName(), includeLoc);
        
        Element te = types.asElement(clsType);
       
        if (te != null) {
            if (te.getKind() == ElementKind.ENUM)
                cls.include(includeAnno);
            else {
                for (AnnotationMirror am : te.getAnnotationMirrors()) {
                    InternalAnnotation ad = annotations.createAnnotation(am);
                    String name = AnnotationUtils.annotationName(ad);
                    if (!ad.isExtended() &&
                            "checkers.quals.Interned".equals(name))
                        cls.include(includeAnno);
                }
            }
        }
    }

    @Override
    protected AnnotatedClassType returnType(ExecutableElement method) {
        AnnotatedClassType result = super.returnType(method);

        checkInterned(method.getReturnType(), result);
        
        // String.intern() should return an @Interned String.
        Element elt = method.getEnclosingElement();
        if (elt instanceof TypeElement) {
            TypeElement classElt = (TypeElement)elt;
            if ("java.lang.String".equals(classElt.getQualifiedName().toString()) 
                    && "intern".equals(method.getSimpleName().toString()))
                result.include(Interned.class);    
        }
        
        return result;
    }

}
