package checkers.igj;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

import checkers.basetype.BaseTypeChecker;
import checkers.igj.quals.Assignable;
import checkers.igj.quals.AssignsFields;
import checkers.igj.quals.I;
import checkers.igj.quals.Immutable;
import checkers.igj.quals.Mutable;
import checkers.igj.quals.ReadOnly;
import checkers.source.SuppressWarningsKey;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotationFactory;
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.AnnotatedTypes;
import checkers.util.AnnotationUtils;
import checkers.util.ElementUtils;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

/**
 * An annotation processor that checks a program use of IGJ mutability
 * type annotations, as specified by the FSE 2007 paper, ({@code @ReadOnly},
 * {@code @Mutable}, and {@code @Immutable}).
 * 
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes( { "checkers.igj.quals.*" })
@SuppressWarningsKey("igj")
public class IGJChecker extends BaseTypeChecker {

    /** Annotation factory to create annotation mirrors */
    protected AnnotationFactory annoFactory;
    
    /** Supported Annotations for IGJ. Used for subtyping rules **/
    protected AnnotationMirror READONLY, MUTABLE, IMMUTABLE, I, PLACE_HOLDER, ASSIGNS_FIELDS, ASSIGNABLE;
    
    protected IGJAnnotatedTypeFactory factory;
    
    protected static final String IMMUTABILITY_KEY = "value";
    
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        annoFactory = new AnnotationFactory(this.env);
        READONLY = annoFactory.fromName(ReadOnly.class.getCanonicalName());
        MUTABLE = annoFactory.fromName(Mutable.class.getCanonicalName());
        IMMUTABLE = annoFactory.fromName(Immutable.class.getCanonicalName());
        I = annoFactory.fromName(I.class.getCanonicalName());
        ASSIGNS_FIELDS = annoFactory.fromName(AssignsFields.class.getCanonicalName());
        ASSIGNABLE = annoFactory.fromName(Assignable.class.getCanonicalName());
        PLACE_HOLDER = 
            annoFactory.fromName(IGJPlaceHolder.class.getCanonicalName());
    }
    
    /**
     * Tests whether t1 is a subtype of t2, with respect to IGJ Subtyping rules
     */
    @Override
    public boolean isSubtype(AnnotatedTypeMirror sup, AnnotatedTypeMirror sub) {
        // TODO: Check that they are up to the same base
        // Err... cannot handle type variables quite yet
        if (sup.getKind() == TypeKind.TYPEVAR || sup.getKind() == TypeKind.WILDCARD ||
                sub.getKind() == TypeKind.TYPEVAR || sub.getKind() == TypeKind.WILDCARD)
            return true;
        
        AnnotatedTypes annoUtils = 
            new AnnotatedTypes(env, factory);
        
        AnnotatedTypeMirror valueBaseType = 
            annoUtils.asSuper(sub, sup);
        if (valueBaseType == null)
            // For now
            valueBaseType = sub;

        boolean isSubtype = isSubtypeOneLevel(sup, valueBaseType);
        boolean typeArgShouldBeSame = sup.hasAnnotation(MUTABLE);
        
        if ((sup.getKind() == TypeKind.DECLARED) 
                && (valueBaseType.getKind() == TypeKind.DECLARED)) {
            AnnotatedDeclaredType supDecl = (AnnotatedDeclaredType) sup;
            AnnotatedDeclaredType subDecl = (AnnotatedDeclaredType) valueBaseType;
            
//            if (supDecl.getTypeArguments().size() != subDecl.getTypeArguments().size())
//                System.out.println("Investigate this " + supDecl + " " + subDecl);

            for (int i = 0; i < supDecl.getTypeArguments().size()
                            && i < subDecl.getTypeArguments().size(); ++i) {
                AnnotatedTypeMirror supArg = supDecl.getTypeArguments().get(i);
                AnnotatedTypeMirror subArg = subDecl.getTypeArguments().get(i);
                if (typeArgShouldBeSame)
                    isSubtype &= isSameImmutability(supArg, subArg);
                else
                    isSubtype &= isSubtype(supArg, subArg);
            }
        } else if ((sup.getKind() == TypeKind.ARRAY)
                && (valueBaseType.getKind() == TypeKind.ARRAY)) {
            AnnotatedArrayType supArr = (AnnotatedArrayType) sup;
            AnnotatedArrayType subArr = (AnnotatedArrayType) valueBaseType;
            if (typeArgShouldBeSame)
                isSubtype &= isSameImmutability(supArr.getComponentType(), subArr.getComponentType());
            else
                isSubtype &= isSubtype(supArr.getComponentType(), subArr.getComponentType());
        }
            
        return isSubtype;
    }
    
    public boolean isSameImmutability(AnnotatedTypeMirror sup, AnnotatedTypeMirror sub) {
        // Err... cannot handle type variables quite yet
        if (sup.getKind() == TypeKind.TYPEVAR || sup.getKind() == TypeKind.WILDCARD ||
                sub.getKind() == TypeKind.TYPEVAR || sub.getKind() == TypeKind.WILDCARD)
            return true;
        
        if (sup.hasAnnotation(PLACE_HOLDER) || sub.hasAnnotation(PLACE_HOLDER))
            return true;
        else if (sup.hasAnnotation(READONLY))
            return sub.hasAnnotation(READONLY);
        else if (sup.hasAnnotation(MUTABLE))
            return sub.hasAnnotation(MUTABLE);
        else if (sup.hasAnnotation(IMMUTABLE))
            return sub.hasAnnotation(IMMUTABLE);
        
        return false;
    }
    
    public boolean isSubtypeOneLevel(AnnotatedTypeMirror sup, AnnotatedTypeMirror sub) {
        // Current implementation only need to deal with one level
        // Err... cannot handle type variables quite yet
        if (sup.getKind() == TypeKind.TYPEVAR || sup.getKind() == TypeKind.WILDCARD ||
                sub.getKind() == TypeKind.TYPEVAR || sub.getKind() == TypeKind.WILDCARD)
            return true;

        // TODO: Need to handle Generics and Arrays
        if (sup.hasAnnotation(READONLY) || sup.hasAnnotation(PLACE_HOLDER)
                || sub.hasAnnotation(PLACE_HOLDER))
            return true;
        else if (sup.hasAnnotation(I)) {
        	// t1 is a subtype of t2, if and only they have the same 
        	// immutability argument
        	if (!sub.hasAnnotation(I))
        		return false;
        	
        	AnnotationUtils annoUtils = new AnnotationUtils(this.env);
        	String t1Arg = annoUtils.parseStringValue(
        			sub.getAnnotation(I.class.getCanonicalName()), 
        			IMMUTABILITY_KEY);
        	String t2Arg = annoUtils.parseStringValue(
        			sup.getAnnotation(I.class.getCanonicalName()), 
        			IMMUTABILITY_KEY);
        	
        	return ((t1Arg != null) && (t2Arg != null) && t1Arg.equals(t2Arg));
        }
        
        return ((sup.hasAnnotation(IMMUTABLE) && sub.hasAnnotation(IMMUTABLE))
        		|| (sup.hasAnnotation(MUTABLE) && sub.hasAnnotation(MUTABLE))
        		|| (sup.hasAnnotation(ASSIGNS_FIELDS) && sub.hasAnnotation(ASSIGNS_FIELDS))
        		|| (sup.hasAnnotation(ASSIGNS_FIELDS) && sub.hasAnnotation(MUTABLE)));
    }

    @Override
    public boolean isValidUse(AnnotatedTypeMirror elemType, AnnotatedTypeMirror use) {
        if (elemType.hasAnnotation(I) || use.hasAnnotation(READONLY))
            return true;
        else
            return super.isValidUse(elemType, use);
    }

    @Override
    public boolean isAssignable(AnnotatedTypeMirror varType, Tree varTree) {
        if (varTree.getKind() == Tree.Kind.VARIABLE ||
                varType.hasAnnotation(ASSIGNABLE))
            return true;
        
        Element varElement = InternalUtils.symbol(varTree);
        if (varElement == null || !varElement.getKind().isField()
                || ElementUtils.isStatic(varElement))
            return true;
        
        ExpressionTree expTree = (ExpressionTree)varTree;
        AnnotatedTypeMirror receiver = factory.getReceiver(expTree);
        
        boolean isAssignable = 
            receiver.hasAnnotation(MUTABLE) ||
            (receiver.hasAnnotation(ASSIGNS_FIELDS) 
                    && TreeUtils.isSelfAccess(expTree));
        
        return isAssignable;
    }

    @Override
    public IGJAnnotatedTypeFactory getFactory(ProcessingEnvironment env, CompilationUnitTree root) {
        factory = new IGJAnnotatedTypeFactory(env, root);
        return factory;
    }
}
