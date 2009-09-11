package checkers.nonnull;

import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;

import checkers.flow.*;
import checkers.quals.*;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.*;
import checkers.util.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

/**
 * An {@link AnnotatedTypeFactory} that accounts for the properties of the
 * NonNull type system. This type factory will add the {@link NonNull}
 * annotation to a type if the input:
 *
 * <ul>
 * <li>is determined to be NonNull by flow-sensitive inference</li>
 * <li>is the class in a static member access (e.g., "System" in
 * "System.out")</li>
 * <li>is an array-creation expression (with new)</li>
 * <li>is an object-creation expression (with new)</li>
 * <li>is a string literal</li>
 * <li>is a package declaration</li>
 * </ul>
 *
 * TODO: some are missing from the above list
 *
 * <p>
 *
 * Additionally, the type factory will add the {@link Nullable} annotation to a
 * type if the input is the null literal.
 */
public class NonNullAnnotatedTypeFactory extends AnnotatedTypeFactory {

    private final Flow flow;
    private final TypePostAnnotator typePost;
    private final TreePreAnnotator treePre;
    protected final AnnotationMirror NONNULL;
    protected final AnnotationMirror NULLABLE;
    private final SourcePositions srcPos;
    private final AnnotationCompleter completer;
    
    public NonNullAnnotatedTypeFactory(ProcessingEnvironment env,
            CompilationUnitTree root) {
        super(env, root);
        
        typePost = new TypePostAnnotator();
        treePre = new TreePreAnnotator();
        
        srcPos = trees.getSourcePositions();
        
        NONNULL = this.annotations.fromName("checkers.nullness.quals.NonNull");
        NULLABLE = this.annotations.fromName("checkers.quals.Nullable");
        
        completer = new AnnotationCompleter(NULLABLE);
        
        flow = new NonNullFlow(env, root, NONNULL, this);
        //flow.debug = System.out;
        flow.scan(root, null);
    }

    private void checkRep(AnnotatedTypeMirror type, Object src) {
        String info = "";
        if (src instanceof Tree) {
            Tree t = (Tree)src;
            long pos = srcPos.getStartPosition(root, t);
            if (pos == -1)
                info = "(invalid position) " + (t != null ? t.getKind() : "null");
            else {
                LineMap lm = root.getLineMap();
                info = String.format("checkRep failure @ %s:%d:%d", 
                        root.getSourceFile().getName(),
                        lm.getLineNumber(pos),
                        lm.getColumnNumber(pos));
            }
        }
        assert !(type.hasAnnotation(NONNULL) && type.hasAnnotation(NULLABLE)) :
            type + ": " + info;
    }
    
    @Override
    protected void annotateImplicit(Element elt, AnnotatedTypeMirror type) {

        if (elt instanceof VariableElement)
            type.addAnnotations(annotateIfStatic(elt));
        
        typePost.visit(type);
        if (elt.getKind() != ElementKind.CLASS)
            applyDefaults(elt, type);
        checkRep(type, elt);
    }
    
    @Override
    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type) {
        
        // Flow-sensitive inference
        Boolean flowResult = null;
        Element elt = InternalUtils.symbol(tree);
        if (elt != null && (tree instanceof IdentifierTree || tree instanceof MemberSelectTree)) {
            long pos = srcPos.getStartPosition(root, tree);
            flowResult = flow.test(pos);
        }
        
        List<AnnotationMirror> preAnnotations = treePre.visit(tree,
                new LinkedList<AnnotationMirror>());
        type.addAnnotations(preAnnotations);
        
        if (flowResult == Boolean.TRUE) {
            type.addAnnotation(NONNULL);
            type.removeAnnotation(NULLABLE);
        }
        
        typePost.visit(type);
        annotateDefaults(tree, type);
        checkRep(type, tree);
    }

    @Override
    public AnnotatedDeclaredType getSelfType(Tree tree) {
        AnnotatedDeclaredType selfType = super.getSelfType(tree);
        selfType.removeAnnotation(NULLABLE);
        selfType.addAnnotation(NONNULL);
        return selfType;
    }
    
    private Element nearestEnclosing(Tree tree) {
        
        TreePath path = trees.getPath(root, tree);
        assert path != null;
        
        for (Tree t : path) {
            switch (t.getKind()) {
            case VARIABLE:
                return elementFromDeclaration((VariableTree)t);
            case METHOD:
                return elementFromDeclaration((MethodTree)t);
            case CLASS:
                return elementFromDeclaration((ClassTree)t);
            default: // Do nothing.
            }
        }
        
        return null;
    }

    private void annotateDefaults(Tree tree, AnnotatedTypeMirror type) {

        // TODO handling for method invocation type arguments
        
        Element elt;
        switch (tree.getKind()) {
            case MEMBER_SELECT:
                elt = elementFromUse((MemberSelectTree)tree);
                applyDefaults(elt, type);
                break;
            
            case IDENTIFIER:
                elt = elementFromUse((IdentifierTree)tree);
                applyDefaults(elt, type);
                break;
                
            case METHOD_INVOCATION:
                elt = elementFromUse((MethodInvocationTree)tree);
                applyDefaults(elt, type);
                break;
 
            default:
                Element nearest = nearestEnclosing(tree);
                applyDefaults(nearest, type);
        }        
    }
    
    private void applyDefaults(final Element elt, final AnnotatedTypeMirror type) {
        assert elt != null : "null element";
        Element e = elt;
        while (e != null) {
            Default d = e.getAnnotation(Default.class);
            if (d != null && d.value().equals(NonNull.class.getName())) {

                // Get the default locations from the Default annotation.
                final Set<DefaultLocation> locations = 
                    new HashSet<DefaultLocation>(Arrays.asList(d.types()));

                new AnnotatedTypeScanner<Void, AnnotationMirror>() {

                    @Override
                    public Void scan(AnnotatedTypeMirror t,
                            AnnotationMirror p) {

                        // Skip type variables.
                        if (t != null && t.getKind() == TypeKind.TYPEVAR)
                            return super.scan(t, p);

                        // Skip annotating this type if:
                        // - the default is "all except (the raw types of) locals"
                        // - we are applying defaults to a local
                        // - and the type is a raw type
                        if (elt.getKind() == ElementKind.LOCAL_VARIABLE
                                && locations.contains(DefaultLocation.ALL_EXCEPT_LOCALS)
                                && t == type)
                            return super.scan(t, p);
                         
                        if (t != null && !t.hasAnnotation(NULLABLE))
                            t.addAnnotation(p);
                        return super.scan(t, p);
                    }

                    // Skip method receivers.
                    @Override
                    public Void visitExecutable(AnnotatedExecutableType t, 
                            AnnotationMirror p) {
                        scan(t.getReturnType(), p);
                        scanAndReduce(t.getParameterTypes(), p, null);
                        scanAndReduce(t.getThrownTypes(), p, null);
                        scanAndReduce(t.getTypeVariables(), p, null);
                        return null;
                    }

                }.scan(type, NONNULL);
            }
	    e = e.getEnclosingElement();
        }
        
        // Add Nullable to anything that hasn't yet been annotated.
        completer.visit(type);
    }
    
    // TODO move to a utils class?
    class AnnotationCompleter extends AnnotatedTypeScanner<Void, Void> {
        
        private final AnnotationMirror annotation;

        AnnotationCompleter(AnnotationMirror annotation) {
            this.annotation = annotation;
        }
        
        @Override 
        protected Void scan(AnnotatedTypeMirror type, Void p) {
            // TODO update to use @TypeQualifiers
            if (type != null 
                    && !type.hasAnnotation(NONNULL) 
                    && !type.hasAnnotation(NULLABLE))
                type.addAnnotation(annotation);
            return super.scan(type, p);
        }
    }
    
    protected class TypePostAnnotator extends AnnotatedTypeScanner<Void, Void> {

        @Override
        public Void visitPrimitive(AnnotatedPrimitiveType type, Void p) {
            type.addAnnotation(NONNULL);
            return super.visitPrimitive(type, p);
        }

        @Override
        public Void visitNoType(AnnotatedNoType type, Void p) {
            if (type.getKind() == TypeKind.PACKAGE)
                type.addAnnotation(NONNULL);
            return super.visitNoType(type, p);
        }
    }
    
    private Collection<? extends AnnotationMirror> annotateIfStatic(
            Element elt) {
        
        if (elt == null)
            return Collections.emptyList();
        
        if (elt.getKind().isClass() || elt.getKind().isInterface())
            return Collections.singletonList(NONNULL);
        
        // Workaround for System.{out,in,err} issue: assume all static
        // fields in java.lang.System are nonnull.
        if (elt.getKind().isField()) {
            Name name = ElementUtils.getQualifiedClassName(elt);
            if (name != null && name.contentEquals("java.lang.System") &&
                    ElementUtils.isStatic(elt))
                return Collections.singletonList(NONNULL);
        }
        
        return Collections.emptyList();
    }
    
    protected class TreePreAnnotator extends SimpleTreeVisitor<List<AnnotationMirror>, List<AnnotationMirror>> {
        
        @Override
        protected List<AnnotationMirror> defaultAction(Tree node,
                List<AnnotationMirror> p) {
            return p;
        }
        
        
        @Override
        public List<AnnotationMirror> visitMemberSelect(MemberSelectTree node,
                List<AnnotationMirror> p) {
            
            Element elt = elementFromUse(node);
            assert elt != null;
            
            p.addAll(annotateIfStatic(elt));
            return super.visitMemberSelect(node, p);
        }


        @Override
        public List<AnnotationMirror> visitIdentifier(IdentifierTree node,
                List<AnnotationMirror> p) {
            
            Element elt = elementFromUse(node);
            assert elt != null;
            
            if (elt.getKind() == ElementKind.PARAMETER) {
                TreePath path = trees.getPath(root, declarationFromElement(elt));
                if (path.getParentPath().getLeaf().getKind() == Tree.Kind.CATCH)
                    p.add(NONNULL);                
            }
            
            p.addAll(annotateIfStatic(elt));
            return super.visitIdentifier(node, p);
        }
        
        @Override
        public List<AnnotationMirror> visitNewArray(NewArrayTree node,
                List<AnnotationMirror> p) {
            p.add(NONNULL);
            return super.visitNewArray(node, p);
        }

        @Override
        public List<AnnotationMirror> visitNewClass(NewClassTree node,
                List<AnnotationMirror> p) {
            p.add(NONNULL);
            return super.visitNewClass(node, p);
        }

        @Override
        public List<AnnotationMirror> visitLiteral(LiteralTree node,
                List<AnnotationMirror> p) {
            if (node.getKind() == Tree.Kind.NULL_LITERAL)
                p.add(NULLABLE);
            else p.add(NONNULL);
            return super.visitLiteral(node, p);
        }
        
    }    
}
