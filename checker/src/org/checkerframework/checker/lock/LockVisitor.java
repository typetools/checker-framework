package org.checkerframework.checker.lock;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.cfg.node.ExplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

/**
 * The visitor for the lock type-system.
 */

public class LockVisitor extends BaseTypeVisitor<LockAnnotatedTypeFactory> {

    public LockVisitor(BaseTypeChecker checker) {
        super(checker);

        elements.getTypeElement("java.lang.String").asType();

        ProcessingEnvironment env = checker.getProcessingEnvironment();
        TreeUtils.getMethod("java.util.Collection",
                "size", 0, env);
        TreeUtils.getMethod("java.util.Collection",
                "toArray", 1, env);

        checkForAnnotatedJdk();
    }

    @Override
    public LockAnnotatedTypeFactory createTypeFactory() {
        // We need to directly access useFlow from the checker, because this method gets called
        // by the superclass constructor and a field in this class would not be initialized
        // yet. Oh the pain.
        return new LockAnnotatedTypeFactory(checker, true);
    }

    @Override
    protected void commonAssignmentCheck(AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType, Tree valueTree, /*@CompilerMessageKey*/ String errorKey,
            boolean isLocalVariableAssignement) {
        
    	if (valueType instanceof AnnotatedNullType) {
    		// Avoid issuing warnings about 'null' not matching the type of the variable.
            return;
    	}
    	
        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, isLocalVariableAssignement);
    }
        
    @Override
    protected boolean checkOverride(MethodTree overriderTree,
            AnnotatedDeclaredType enclosingType,
            AnnotatedExecutableType overridden,
            AnnotatedDeclaredType overriddenType,
            Void p) {

        List<String> overriderLocks = methodHolding(TreeUtils.elementFromDeclaration(overriderTree));
        List<String> overriddenLocks = methodHolding(overridden.getElement());

        boolean isValid = overriddenLocks.containsAll(overriderLocks);

        if (!isValid) {
            checker.report(Result.failure("override.holding.invalid",
                    TreeUtils.elementFromDeclaration(overriderTree),
                    overridden.getElement(),
                    overriderLocks, overriddenLocks), overriderTree);
        }

        return super.checkOverride(overriderTree, enclosingType, overridden, overriddenType, p) && isValid;
    }    
    
    protected List<String> methodHolding(ExecutableElement element) {
        AnnotationMirror holding = atypeFactory.getDeclAnnotation(element, org.checkerframework.checker.lock.qual.Holding.class);
        AnnotationMirror guardedBy
            = atypeFactory.getDeclAnnotation(element, net.jcip.annotations.GuardedBy.class);
        AnnotationMirror guardedByJavax
            = atypeFactory.getDeclAnnotation(element, javax.annotation.concurrent.GuardedBy.class);
        if (holding == null && guardedBy == null && guardedByJavax == null)
            return Collections.emptyList();

        List<String> locks = new ArrayList<String>();

        if (holding != null) {
            List<String> holdingValue = AnnotationUtils.getElementValueArray(holding, "value", String.class, false);
            locks.addAll(holdingValue);
        }
        if (guardedBy != null) {
            String guardedByValue = AnnotationUtils.getElementValue(guardedBy, "value", String.class, false);
            locks.add(guardedByValue);
        }
        if (guardedByJavax != null) {
            String guardedByValue = AnnotationUtils.getElementValue(guardedByJavax, "value", String.class, false);
            locks.add(guardedByValue);
        }

        return locks;
    }    

   	/**
     * Checks all the preconditions of the method invocation {@code tree} with
     * element {@code invokedMethodElement}.
     */
    protected void checkPreconditions(Tree tree,
            Element invokedElement, boolean methodCall, Set<Pair<String, String>> additionalPreconditions) {
    	
    	if (additionalPreconditions == null) {
    		additionalPreconditions = new HashSet<>();
    	}
    		
		// Retrieve the @GuardedBy annotation on the receiver of the enclosing method
        // and add it to the preconditions set if the receiver on the enclosing method is
        // the same as the receiver on the field/method we are checking preconditions for.
        MethodTree enclosingMethod = TreeUtils.enclosingMethod(atypeFactory.getPath(tree));
        if (enclosingMethod != null) {
		    ExecutableElement methodElement = TreeUtils.elementFromDeclaration(enclosingMethod);
		    
		    if (methodElement != null) {
				TypeMirror rt = methodElement.getReceiverType();
				
				if (rt != null) {
				    List<? extends AnnotationMirror> amList = rt.getAnnotationMirrors();
				    
				    for (AnnotationMirror annotationMirror : amList) {
				        String typeString = annotationMirror.getAnnotationType().toString();
				        
				        if (typeString.equals("org.checkerframework.checker.lock.qual.GuardedBy") ||
				            typeString.equals("javax.annotation.concurrent.GuardedBy") ||
				            typeString.equals("net.jcip.annotations.GuardedBy")) {
				            List<String> guardedByValue = AnnotationUtils.getElementValueArray(annotationMirror, "value", String.class, false);
				            
                            // A GuardedBy annotation on the receiver of the enclosing method was found.
                            // Now check if the receiver on the enclosing method is the same as the receiver
                            // on the field/method we are checking preconditions for.

				            if (guardedByValue != null) {
				                Node nodeNode = atypeFactory.getNodeForTree(tree);
				                Node receiverNode = null;
				                
				                if (nodeNode instanceof FieldAccessNode) {
				                    receiverNode = ((FieldAccessNode) nodeNode).getReceiver();
				                }
				                else if (nodeNode instanceof MethodInvocationNode)
				                {
				                    MethodAccessNode man = ((MethodInvocationNode) nodeNode).getTarget();

				                    if (!man.getMethod().getModifiers().contains(Modifier.STATIC)) {
				                        receiverNode = man.getReceiver();
				                    }
				                }
				                
				                if (receiverNode instanceof ExplicitThisLiteralNode ||
				                    receiverNode instanceof ImplicitThisLiteralNode) {

                                    // The receivers match. Add to the preconditions set.
				                    for(String lockExpression : guardedByValue) {
				                    	additionalPreconditions.add(Pair.of(lockExpression, "org.checkerframework.checker.lock.qual.LockHeld"));
				                    }
				                }
				            }
				        }
				    }
				}
		    }
        }
    	
    	super.checkPreconditions(tree, invokedElement, methodCall, additionalPreconditions);
    }    
    
}
