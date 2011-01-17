package checkers.interned;

import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;

import checkers.flow.Flow;
import checkers.quals.Interned;
import checkers.types.*;
import static checkers.types.AnnotatedTypeMirror.*;

import checkers.util.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

/**
 * An {@link AnnotatedTypeFactory} that accounts for the properties of the
 * Interned type system. This type factory will add the {@link Interned}
 * annotation to a type if the input:
 * 
 * <ul>
 * <li>is a String literal
 * <li>is a class literal
 * <li>is an enum constant
 * <li>has a primitive type
 * <li>has the type java.lang.Class
 * <li>is a reference to a final field with an Interned initializer
 * <li>is a call to the method {@link String#intern()}
 * </ul>
 */
public class InternedAnnotatedTypeFactory extends AnnotatedTypeFactory {

    /** Adds annotations from tree context before type resolution. */
    private final TreePreAnnotator treePre;

    /** Adds annotations from the resulting type after type resolution. */
    private final TypePostAnnotator typePost;

    /** The {@link Interned} annotation. */
    private final AnnotationMirror INTERNED;

    /** Flow-sensitive qualifier inference. */
    private final Flow flow;

    /** Utility for getting line/column positions of trees. */
    private final SourcePositions srcPos;

    /**
     * Creates a new {@link InternedAnnotatedTypeFactory} that operates on a
     * particular AST.
     * 
     * @param env the current processing environment for this annotation
     *        processor
     * @param root the AST on which this type factory operates
     * @param useFlow whether or not flow-sensitive qualifier inference should
     *        be used
     */
    public InternedAnnotatedTypeFactory(ProcessingEnvironment env,
        CompilationUnitTree root, boolean useFlow) {
        super(env, root);
        this.INTERNED = annotations.fromName("checkers.quals.Interned");
        this.treePre = new TreePreAnnotator();
        this.typePost = new TypePostAnnotator();
        this.srcPos = trees.getSourcePositions();
        
        this.flow = new Flow(env, root, INTERNED, this);
        if (useFlow) flow.scan(root, null);
    }
    
    @Override
    protected void annotateImplicit(Element elt, AnnotatedTypeMirror type) {
        typePost.visit(type);
    }

    @Override
    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type) {
        
        if (type == null) throw new IllegalArgumentException("null type / " + tree);
        
        Boolean flowResult = null;

        Element elt = InternalUtils.symbol(tree);
        if (elt != null && (tree instanceof IdentifierTree || tree instanceof MemberSelectTree)) {
            long pos = srcPos.getStartPosition(root, tree);
            flowResult = flow.test(pos);
        }
        
        List<AnnotationMirror> pre 
            = treePre.visit(tree, new LinkedList<AnnotationMirror>());
        assert pre != null;
        type.addAnnotations(pre);
        
        if (flowResult == Boolean.TRUE)
            type.addAnnotation(INTERNED);
        
        typePost.visit(type);
    }
    
    /**
     * A class for adding annotations to a type based on its tree context.
     */
    protected class TreePreAnnotator extends
        SimpleTreeVisitor<List<AnnotationMirror>, List<AnnotationMirror>> {
        
        @Override
        public List<AnnotationMirror> defaultAction(Tree node,
            List<AnnotationMirror> lst) {
            // By default, return whatever annotations were passed in.
            return lst;
        }

        @Override
        public List<AnnotationMirror> visitLiteral(LiteralTree node,
            List<AnnotationMirror> lst) {

            // Literals: add an @Interned annotation.
            lst.add(INTERNED);
            
            return super.visitLiteral(node, lst);
        }

        /**
         * Adds annotations to references to final fields that have been
         * initialized to an {@link Interned} value.
         * 
         * @param elt the element corresponding to the referenced field
         * @see {@link AnnotatedTypeFactory#elementFromUse}
         */
        private List<AnnotationMirror> annotateIfInternedFinal(Element elt) {
            
            assert elt != null;
            
            // Return immediately if it's not a final member.
            if (!ElementUtils.isFinal(elt))
                return Collections.emptyList();
             
            Tree decl = declarationFromElement(elt);

            // If it's variable with an initializer, check the type of the
            // initializer.
            if (decl != null &&
                    decl.getKind() == Tree.Kind.VARIABLE &&
                    ((VariableTree)decl).getInitializer() != null) {
                    
                AnnotatedTypeMirror type =
                    getAnnotatedType(((VariableTree)decl).getInitializer());
                
                if (type.hasAnnotation(INTERNED))
                    return Collections.singletonList(INTERNED);
            }
            
            return Collections.emptyList();
        }

        @Override
        public List<AnnotationMirror> visitMemberSelect(MemberSelectTree node,
                List<AnnotationMirror> lst) {

            // Class literals are Interned automatically.
            if (node.getIdentifier().contentEquals("class"))
                lst.add(INTERNED);

            Element elt = elementFromUse(node);
            lst.addAll(annotateIfInternedFinal(elt));

            return super.visitMemberSelect(node, lst);
        }

        @Override
        public List<AnnotationMirror> visitIdentifier(IdentifierTree node,
                List<AnnotationMirror> lst) {

            // Annotate "this".
            if (node.getName().contentEquals("this")) {
                
                // Get the type of the enclosing method.
                TreePath path = trees.getPath(root, node);
                MethodTree encl = TreeUtils.enclosingMethod(path);
		if (encl != null) {
		    Element elt = InternalUtils.symbol(encl);
		    AnnotatedExecutableType ex = (AnnotatedExecutableType)getAnnotatedType(elt);
                
		    // Add the annotations on the receiver.
		    lst.addAll(ex.getReceiverType().getAnnotations());
                
		    // Add annotations on the class declaration.
		    ClassTree enclClass = TreeUtils.enclosingClass(path);
		    Element cls = InternalUtils.symbol(enclClass);
		    AnnotatedTypeMirror clsType = getAnnotatedType(cls);
		    lst.addAll(clsType.getAnnotations());
		}
            }

            Element elt = elementFromUse(node);
            assert elt != null : node;
            lst.addAll(annotateIfInternedFinal(elt));

            return super.visitIdentifier(node, lst);

        }
    }

    /** 
     * A class for adding annotations to a type after initial type resolution.
     */
    protected class TypePostAnnotator extends AnnotatedTypeScanner<Void, Void> {

        @Override
        public Void visitDeclared(AnnotatedDeclaredType t, Void p) {
            
            // Enum types and constants: add an @Interned annotation.
            Element elt = types.asElement(t.getUnderlyingType());
            if (elt != null) {

                if (elt.getKind() == ElementKind.ENUM 
                        || elt.getKind() == ElementKind.ENUM_CONSTANT)
                    t.addAnnotation(INTERNED);
                
                // TODO: check that other calls to annotateJSR175 aren't missing
                //annotateJSR175(t, elt);
            }

            TypeElement classElt = elements.getTypeElement("java.lang.Class");
            assert classElt != null;
            
            // Annotate class types.
            if (types.isSameType(types.erasure(t.getUnderlyingType()),
                    types.erasure(classElt.asType())))
                t.addAnnotation(INTERNED);

            return super.visitDeclared(t, p);
        }
        
        @Override
        public Void visitPrimitive(AnnotatedPrimitiveType t, Void p) {
            
            // All primitive types are implicitly interned.
            t.addAnnotation(INTERNED);
            
            return super.visitPrimitive(t, p);
        }

        @Override
        public Void visitExecutable(AnnotatedExecutableType t, Void p) {

            // Annotate the java.lang.String.intern() method.
            if (t.getElement() != null) {
                ExecutableElement method = t.getElement();
                if (method.getSimpleName().contentEquals("intern")) {
                    Name clsName = ElementUtils.getQualifiedClassName(method);
                    if (clsName != null && clsName.contentEquals("java.lang.String"))
                        t.getReturnType().addAnnotation(INTERNED);
                }
            }

            // skip the receiver
            scan(t.getReturnType(), p);
            scanAndReduce(t.getParameterTypes(), p, null);
            scanAndReduce(t.getThrownTypes(), p, null);
            scanAndReduce(t.getTypeVariables(), p, null);
            return null;
        }

        @Override
        public Void visitNull(AnnotatedNullType type, Void p) {
            
            // TODO temporary, until there's better intersection for ( ? : )            
            type.addAnnotation(INTERNED);
            
            return super.visitNull(type, p);
        }

        @Override
        public Void visitTypeVariable(AnnotatedTypeVariable type, Void p) {
            
            if (type.getUpperBound() != null)
                type.addAnnotations(type.getUpperBound().getAnnotations());

            return super.visitTypeVariable(type, p);
        }
    }
    
    @Override
    public AnnotatedPrimitiveType getUnboxedType(AnnotatedDeclaredType type) {
        AnnotatedPrimitiveType primitive = super.getUnboxedType(type);
        primitive.addAnnotation(INTERNED);
        return primitive;
    }
}
