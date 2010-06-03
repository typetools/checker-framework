package checkers.igj;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;

import com.sun.source.tree.*;
import com.sun.source.util.SimpleTreeVisitor;

import checkers.igj.quals.*;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.*;
import checkers.util.AnnotationUtils;
import checkers.util.ElementUtils;
import checkers.util.TypesUtils;

/**
 * Generates {@link AnnotatedTypeMirror} instances accounting for the properties
 * of the IGJ type system. For example, when the
 * {@link IGJAnnotatedTypeFactory#getAnnotatedType(Tree)} method is invoked
 * on a primitive int, it returns the type "@Immutable int".
 */
public class IGJAnnotatedTypeFactory extends AnnotatedTypeFactory{
    /** Adds annotations from tree context before type resolution. */
    private final TreePreAnnotator treePre;

    /** Adds annotations from the resulting type after type resolution. */
    private final TypePostAnnotator typePost;

    /** The various IGJ annotation. */
    private final AnnotationMirror READONLY, MUTABLE, IMMUTABLE, I,
            PLACE_HOLDER, ASSIGNS_FIELDS;
    
    private final AnnotationUtils annoUtils;

    /**
     * Constructor for IGJAnnotatedTypeFactory object.
     * 
     * @param env   the processing environment for the current compiler round
     * @param root  the compilation unit the annotation processor is processing currently
     */
    public IGJAnnotatedTypeFactory(ProcessingEnvironment env,
            CompilationUnitTree root) {
        super(env, root);
        treePre = new TreePreAnnotator();
        typePost = new TypePostAnnotator();
        AnnotationFactory annoFactory = new AnnotationFactory(env);
        READONLY = annoFactory.fromName(ReadOnly.class.getCanonicalName());
        MUTABLE = annoFactory.fromName(Mutable.class.getCanonicalName());
        IMMUTABLE = annoFactory.fromName(Immutable.class.getCanonicalName());
        I = annoFactory.fromName(I.class.getCanonicalName());
        PLACE_HOLDER =
            annoFactory.fromName(IGJPlaceHolder.class.getCanonicalName());
        ASSIGNS_FIELDS =
            annoFactory.fromName(AssignsFields.class.getCanonicalName());
        annoUtils = new AnnotationUtils(env);
    }

    /**
     * Returns the annotation specifying the immutability type of {@code type}.
     */
    private AnnotationMirror getImmutabilityAnnotation(AnnotatedTypeMirror type) {
        if (type.hasAnnotation(I))
            return type.getAnnotation(I.class.getCanonicalName());
        if (type.hasAnnotation(READONLY))
            return READONLY;
        else if (type.hasAnnotation(MUTABLE))
            return MUTABLE;
        else if (type.hasAnnotation(IMMUTABLE))
            return IMMUTABLE;
        else if (type.hasAnnotation(PLACE_HOLDER))
            return PLACE_HOLDER;
        else if (type.hasAnnotation(ASSIGNS_FIELDS))
            return ASSIGNS_FIELDS;
        else
            return null;
    }

    /**
     * @param type  an annotated type mirror
     * @return  true iff the type is specified an immutability type, 
     *          false otherwise
     */
    public boolean hasImmutabilityAnnotation(AnnotatedTypeMirror type) {
        return getImmutabilityAnnotation(type) != null;
    }

    @Override
    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type) {
        List<AnnotationMirror> preAnnotations = treePre.visit(tree, null);
        
        if (preAnnotations != null)
            type.addAnnotations(preAnnotations);
        
        if (tree.getKind() == Tree.Kind.IDENTIFIER)
//                (tree.getKind() == Tree.Kind.METHOD_INVOCATION))
//                        ((MethodInvocationTree)tree).getMethodSelect().getKind() == Tree.Kind.IDENTIFIER))
            resolveImmutabilityTypeVar(type, (ExpressionTree) tree);
        
        // Add annotations based on type properties.
        typePost.visit(type, tree.getKind() == Tree.Kind.CLASS);
    }
    
    @Override
    protected void annotateImplicit(Element element, AnnotatedTypeMirror type) {
        // Add annotations based on type properties.
        typePost.visit(type, element.getKind().isClass() || element.getKind().isInterface());
    }
    
    private boolean resolveImmutabilityTypeVar(AnnotatedTypeMirror type, 
            AnnotatedTypeMirror ...provided) {
        ImmutabilityTemplateCollector collector =
            new ImmutabilityTemplateCollector();
        
        Map<String, AnnotationMirror> templateMapping = 
            collector.visit(provided[0]);
        
        for (int i = 1; i < provided.length; ++i) {
            templateMapping = collector.reduce(templateMapping, 
                    collector.visit(provided[i]));
        }
        
        final Map<String, AnnotationMirror> mapping = templateMapping;
        
        // There is nothing to resolve
        if (mapping == null || mapping.isEmpty()) 
            return false;
        
        new ImmutabilityResolver().visit(type, mapping);
        
        return true;
    }
    
    /**
     * Tries to resolve {@code @I} IGJ immutability parameter based on receiver
     * type, and updates the type with the resolved immutability type qualifier.
     * 
     * The method does not change the underlying type of the annotated type, nor
     * does it resolve type variables.
     * 
     * @param type  the type potentially having a immutability type variable
     * @param tree  the expression tree whose type is the provided type
     * @return true if any immutability parameter exists and/or resolved
     */
    private boolean resolveImmutabilityTypeVar(AnnotatedTypeMirror type,
            ExpressionTree tree) {
        // TODO: Clear up this
        if (tree.getKind() == Tree.Kind.IDENTIFIER &&
                ((IdentifierTree)tree).getName().contentEquals("this"))
            return false;
        if (tree.getKind() == Tree.Kind.MEMBER_SELECT &&
                ((MemberSelectTree)tree).getIdentifier().contentEquals("this"))
            return false;

        List<AnnotatedTypeMirror> provided = new ArrayList<AnnotatedTypeMirror>();
        provided.add(getReceiver(tree));
        resolveImmutabilityTypeVar(type, provided.toArray(new AnnotatedTypeMirror[0]));

        return true;
    }
    
    @Override
    protected void postDirectSuperTypes(AnnotatedTypeMirror type,
            List<? extends AnnotatedTypeMirror> supertypes) {

        // Try to resolve @I
        ImmutabilityTemplateCollector collector = new ImmutabilityTemplateCollector();
        
        Map<String, AnnotationMirror> templateMapping = 
            collector.visit(type);
        ImmutabilityResolver resolver = new ImmutabilityResolver();
        
        for (AnnotatedTypeMirror t : supertypes) {
            if (templateMapping != null && !templateMapping.isEmpty())
                resolver.visit(t, templateMapping);
            this.typePost.visit(t, false);
        }
    }

    @Override
    public void postAsMemberOf(AnnotatedTypeMirror type, AnnotatedTypeMirror owner, Element element) {
        resolveImmutabilityTypeVar(type, owner);
    }
    
    @Override
    protected void annotateInheritedFromClass(@Mutable AnnotatedTypeMirror type) {
        // Do nothing
    }

    /**
     * Returns a type of the class and the field {@code this}.
     */
    @Override
    public AnnotatedDeclaredType getSelfType(Tree tree) {
        AnnotatedDeclaredType act = this.getCurrentClassType(tree);
        AnnotatedDeclaredType methodReceiver = this.getCurrentMethodReceiver(tree);
        
        if (methodReceiver == null)
            return act;
        // Are we in a mutable or Immutable scope
        if (methodReceiver.hasAnnotation(MUTABLE) ||
                methodReceiver.hasAnnotation(IMMUTABLE)) {
            return methodReceiver;
        } else if (act.hasAnnotation(I)) {
            if (methodReceiver.hasAnnotation(ASSIGNS_FIELDS))
                act.addAnnotation(ASSIGNS_FIELDS);
            return act;
        } else if (act.hasAnnotation(IMMUTABLE)) {
            if (methodReceiver.hasAnnotation(ASSIGNS_FIELDS))
                act.addAnnotation(ASSIGNS_FIELDS);
            return act;
        } else // TODO: Figure out when
            return methodReceiver;
    }

    @Override
    public Collection<AnnotationMirror> unify(Collection<AnnotationMirror> c1,
            Collection<AnnotationMirror> c2) {
        Map<String, AnnotationMirror> first = 
            new HashMap<String, AnnotationMirror>();
        for (AnnotationMirror anno : c1)
            first.put(AnnotationUtils.annotationName(anno), anno);
        Map<String, AnnotationMirror> second = new HashMap<String, AnnotationMirror>();
        for (AnnotationMirror anno : c2)
            second.put(AnnotationUtils.annotationName(anno), anno);

        if (first.containsKey(IGJPlaceHolder.class.getCanonicalName()))
            first = second;
        else if (!second.containsKey(IGJPlaceHolder.class.getCanonicalName()))
            first.keySet().retainAll(second.keySet());

        // We don't have any igj type add ReadOnly
        if (!first.containsKey(I.class.getCanonicalName()) &&
                !first.containsKey(Mutable.class.getCanonicalName()) &&
                !first.containsKey(Immutable.class.getCanonicalName()) &&
                !first.containsKey(IGJPlaceHolder.class.getCanonicalName()))
            first.put(ReadOnly.class.getCanonicalName(), READONLY);
        return first.values();
    }

    /**
     * A helper class that resolves the immutability on a types based on a
     * provided mapping.
     * 
     * It returns a set of the annotations that were inserted. This is important
     * to recognize which immutability type variables were resolved and which
     * are to be made into place holder.
     */
    private class ImmutabilityResolver extends
    AnnotatedTypeScanner<Set<AnnotationMirror>, Map<String, AnnotationMirror>> {
        
        @Override
        public Set<AnnotationMirror> reduce(Set<AnnotationMirror> r1,
                Set<AnnotationMirror> r2) {
            Set<AnnotationMirror> result = new HashSet<AnnotationMirror>();
            if (r1 != null)
                result.addAll(r1);
            if (r2 != null)
                result.addAll(r2);
            return result;
        }
        
        @Override
        public Set<AnnotationMirror> visitDeclared(AnnotatedDeclaredType type, 
                Map<String, AnnotationMirror> p) {
            Set<AnnotationMirror> result = new HashSet<AnnotationMirror>();
    
            if (type.hasAnnotation(I)) {
                String immutableString = 
                    annoUtils.parseStringValue(getImmutabilityAnnotation(type), 
                            IGJChecker.IMMUTABILITY_KEY);
                if (p.containsKey(immutableString)) {
                    type.removeAnnotation(I);
                    type.addAnnotation(p.get(immutableString));
                    result.add(p.get(immutableString));
                }
            }
            
            return reduce(result, super.visitDeclared(type, p));
        }
    }

    /**
     * Helper class to annotate trees.
     * 
     * Currently it only adds an PLACE_HOLDER for new classes and new arrays,
     * when an annotation is not specified
     */
    private class TreePreAnnotator
    extends SimpleTreeVisitor<List<AnnotationMirror>, Void> {
        @Override
        public List<AnnotationMirror> visitNewClass(NewClassTree node,
                Void p) {
            // If no immutability is specified, use a place holder
            AnnotatedDeclaredType type = (AnnotatedDeclaredType)fromExpression(node);
            AnnotatedTypeMirror ct = fromElement(type.getUnderlyingType().asElement());
            
            if (!hasImmutabilityAnnotation(type) && 
                    (!hasImmutabilityAnnotation(ct) || ct.hasAnnotation(I)))
                return Collections.singletonList(PLACE_HOLDER);
    
            return Collections.emptyList();
        }
    }

    /**
     * Helper class for annotating unannotated types.
     */
    private class TypePostAnnotator extends AnnotatedTypeScanner<Void, Boolean> {
        /** All primitives are immutable **/
        @Override
        public Void visitPrimitive(AnnotatedPrimitiveType type, Boolean p) {
            if (!hasImmutabilityAnnotation(type))
                type.addAnnotation(PLACE_HOLDER);
            return super.visitPrimitive(type, p);
        }
        
        /**
         * For Declared types:
         *  Classes are mutable
         *  Interface declaration are placeholders
         *  Enum and annotations    are immutable
         */
        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Boolean p) {
            if (!hasImmutabilityAnnotation(type)) {
                // Actual element
                TypeElement element = (TypeElement)type.getUnderlyingType().asElement();
                AnnotatedDeclaredType elementType = (AnnotatedDeclaredType)fromElement(element);
    
                if (TypesUtils.isBoxedPrimitive(type.getUnderlyingType()))
                    type.addAnnotation(PLACE_HOLDER);
                else if (ElementUtils.isObject(element))
                    type.addAnnotation(PLACE_HOLDER);
                else if (elementType.hasAnnotation(IMMUTABLE))
                    type.addAnnotation(IMMUTABLE);
                else if (hasImmutabilityAnnotation(elementType)) // not immutable
                    type.addAnnotation(MUTABLE);
                else if (element.getKind() == ElementKind.CLASS)
                    type.addAnnotation(MUTABLE);
                else if (element.getKind() == ElementKind.INTERFACE)
                    type.addAnnotation(p ? PLACE_HOLDER : MUTABLE);
                else if (element.getKind() == ElementKind.ENUM ||
                        element.getKind() == ElementKind.ANNOTATION_TYPE)
                    type.addAnnotation(IMMUTABLE);
                else
                    assert false : "shouldn't be here!";
            }
            return super.visitDeclared(type, p);
        }
        
        /** null are subtypes of everything **/
        @Override
        public Void visitNull(AnnotatedNullType type, Boolean p) {
            type.addAnnotation(PLACE_HOLDER);
            return super.visitNull(type, p);
        }
        
        @Override
        public Void visitExecutable(AnnotatedExecutableType type, Boolean p) {
            if (hasImmutabilityAnnotation(type.getReceiverType()))
                return super.visitExecutable(type, p);
            
            AnnotatedDeclaredType receiver = type.getReceiverType();
            TypeElement ownerElement = ElementUtils.enclosingClass(type.getElement());
            AnnotatedDeclaredType ownerType = 
                (AnnotatedDeclaredType) getAnnotatedType(ownerElement);
    
            if (ElementUtils.isObject(ownerElement)) {
                receiver.addAnnotation(PLACE_HOLDER);
            } else if (type.getElement().getKind() == ElementKind.CONSTRUCTOR) {
                receiver.addAnnotation(
                        ownerType.hasAnnotation(MUTABLE) ? MUTABLE : ASSIGNS_FIELDS);
            } else if (ownerType.hasAnnotation(IMMUTABLE)) {
                receiver.addAnnotation(READONLY);
            } else {
                receiver.addAnnotation(MUTABLE);
            }
            
            return super.visitExecutable(type, p);
        }
        
        @Override
        public Void visitArray(AnnotatedArrayType type, Boolean p) {
            if (!hasImmutabilityAnnotation(type))
                type.addAnnotation(MUTABLE);
            return super.visitArray(type, p);
        }
    }

    /**
     * A Helper class that tries to resolve the immutability type variable,
     * as the type variable is assigned to the most restricted immutability
     */
    private class ImmutabilityTemplateCollector
    extends SimpleAnnotatedTypeVisitor<Map<String, AnnotationMirror>, AnnotatedTypeMirror> {
        
        public Map<String, AnnotationMirror> reduce(Map<String, AnnotationMirror> r1,
                Map<String, AnnotationMirror> r2) {
            Map<String, AnnotationMirror> result = 
                new HashMap<String, AnnotationMirror>();
            
            if (r1 != null)
                result.putAll(r1);
                    
            if (r2 != null) {
                // Need to be careful about overlap
                for (String key : r2.keySet()) {
                    if (!result.containsKey(key))
                        result.put(key, r2.get(key));
                    else if (!result.get(key).equals(r2.get(key)))
                        result.put(key, READONLY);
                }
            }
            return result;
        }
        
        public Map<String, AnnotationMirror> visit(Iterable<? extends AnnotatedTypeMirror> types,
                Iterable<? extends AnnotatedTypeMirror> actualTypes) {
            Map<String, AnnotationMirror> result = new HashMap<String, AnnotationMirror>();
            
            Iterator<? extends AnnotatedTypeMirror> itert = types.iterator();
            Iterator<? extends AnnotatedTypeMirror> itera = actualTypes.iterator();
            
            while (itert.hasNext() && itera.hasNext()) {
                AnnotatedTypeMirror type = itert.next();
                AnnotatedTypeMirror actualType = itera.next();
                result = reduce(result, visit(type, actualType));
            }
            return result;
        }
        
        @Override
        public Map<String, AnnotationMirror> visitDeclared(
                AnnotatedDeclaredType type, AnnotatedTypeMirror actualType) {
            
            if (actualType == null) {
                TypeElement elem = (TypeElement)type.getUnderlyingType().asElement();
                actualType = getAnnotatedType(elem);
            }
            if (actualType.getKind() != type.getKind())
                return Collections.emptyMap();
            
            assert actualType.getKind() == type.getKind();
            type = (AnnotatedDeclaredType)atypes.asSuper(type, actualType);
            AnnotatedDeclaredType dcType = (AnnotatedDeclaredType)actualType;
                
            Map<String, AnnotationMirror> result =
                new HashMap<String, AnnotationMirror>();
                
            if (dcType.hasAnnotation(I)) {
                String immutableString = 
                    annoUtils.parseStringValue(getImmutabilityAnnotation(dcType), 
                            IGJChecker.IMMUTABILITY_KEY);
                AnnotationMirror immutability = getImmutabilityAnnotation(type);
                // Assertion failes some times
                assert immutability != null;
                if ((immutability != null) && (!immutability.equals(ASSIGNS_FIELDS)))
                    result.put(immutableString, immutability);
            }

            result = reduce(result, visit(type.getTypeArguments(), dcType.getTypeArguments()));
            return result;
        }
        
        @Override
        public Map<String, AnnotationMirror> visitArray(
                AnnotatedArrayType type, AnnotatedTypeMirror actualType) {
            if (actualType == null)
                return visit(type.getComponentType(), null);
            
            if (type.getKind() != actualType.getKind())
                return visit(type, null);
            assert type.getKind() == actualType.getKind();
            AnnotatedArrayType arType = (AnnotatedArrayType)actualType;
                
            Map<String, AnnotationMirror> result =
                new HashMap<String, AnnotationMirror>();
                
            if (arType.hasAnnotation(I)) {
                String immutableString = 
                    annoUtils.parseStringValue(getImmutabilityAnnotation(arType), 
                            IGJChecker.IMMUTABILITY_KEY);
                AnnotationMirror immutability = getImmutabilityAnnotation(type);
                // Assertion failes some times
                assert immutability != null;
                if ((immutability != null) && (!type.hasAnnotation(ASSIGNS_FIELDS)))
                    result.put(immutableString, immutability);
            }

            result = reduce(result, visit(type.getComponentType(), arType.getComponentType()));
            return result;
        }
    }
        
    @Override
    public AnnotatedExecutableType methodFromUse(MethodInvocationTree tree) {
        AnnotatedExecutableType type = super.methodFromUse(tree);
        List<AnnotatedTypeMirror> arguments = atypes.getAnnotatedTypes(tree.getArguments());
        List<AnnotatedTypeMirror> requiredArgs = atypes.getMethodParameters(type, tree.getArguments());
        ImmutabilityTemplateCollector collector = new ImmutabilityTemplateCollector();
        Map<String, AnnotationMirror> matchingMapping = collector.visit(arguments, requiredArgs);
        if (!matchingMapping.isEmpty())
            new ImmutabilityResolver().visit(type, matchingMapping);

        Map<String, AnnotationMirror> fromReceiver = collector.visit(getReceiver(tree));
        final Map<String, AnnotationMirror> mapping = 
            collector.reduce(matchingMapping, fromReceiver);
        new AnnotatedTypeScanner<Void, Void>() {
            @Override
            public Void visitDeclared(AnnotatedDeclaredType type, Void p) {
                if (type.hasAnnotation(I)) {
                    AnnotationMirror anno = 
                        type.getAnnotation(I.class.getCanonicalName());
                    if (!mapping.containsValue(anno)) {
                        type.removeAnnotation(I);
                        type.addAnnotation(PLACE_HOLDER);
                    }
                }
                return super.visitDeclared(type, p);
            }
        }.visit(type);
        return type;
    }
}
