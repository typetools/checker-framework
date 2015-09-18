package org.checkerframework.checker.lock;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByBottom;
import org.checkerframework.checker.lock.qual.GuardedByInaccessible;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.Holding;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.checker.lock.qual.LockingFree;
import org.checkerframework.checker.lock.qual.ReleasesNoLocks;
import org.checkerframework.checker.lock.qual.MayReleaseLocks;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.cfg.node.ExplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;

/**
 * The LockVisitor enforces the subtyping rules of LockHeld and LockPossiblyHeld
 * (via BaseTypeVisitor). It also manually verifies that @Holding
 * annotations are properly used on overridden methods.
 * Finally, it ensures that we avoid doing any lock checking
 * when visiting initializers.
 */

public class LockVisitor extends BaseTypeVisitor<LockAnnotatedTypeFactory> {
    private final Class<? extends Annotation> checkerGuardedByClass = GuardedBy.class;
    private final Class<? extends Annotation> checkerGuardSatisfiedClass = GuardSatisfied.class;
    private final Class<? extends Annotation> checkerHoldingClass = Holding.class;
    //private final Class<? extends Annotation> checkerHoldingOnEntryClass = org.checkerframework.checker.lock.qual.HoldingOnEntry.class;
    private final Class<? extends Annotation> checkerLockHeldClass = LockHeld.class;
    private final Class<? extends Annotation> checkerLockingFreeClass = LockingFree.class;
    private final Class<? extends Annotation> checkerReleasesNoLocksClass = ReleasesNoLocks.class;
    private final Class<? extends Annotation> checkerMayReleaseLocksClass = MayReleaseLocks.class;
    private final Class<? extends Annotation> sideEffectFreeClass = SideEffectFree.class;
    private final Class<? extends Annotation> pureClass = Pure.class;

    // Note that Javax and JCIP @GuardedBy is used on both methods and objects. For methods they are
    // equivalent to the Checker Framework @Holding annotation.
    private final Class<? extends Annotation> javaxGuardedByClass = javax.annotation.concurrent.GuardedBy.class;
    private final Class<? extends Annotation> jcipGuardedByClass = net.jcip.annotations.GuardedBy.class;

    /** Annotation constants */
    protected final AnnotationMirror GUARDEDBY, GUARDEDBYINACCESSIBLE, GUARDEDBYBOTTOM;

    public LockVisitor(BaseTypeChecker checker) {
        super(checker);

        GUARDEDBYINACCESSIBLE = AnnotationUtils.fromClass(elements, GuardedByInaccessible.class);
        GUARDEDBY = AnnotationUtils.fromClass(elements, GuardedBy.class);
        GUARDEDBYBOTTOM = AnnotationUtils.fromClass(elements, GuardedByBottom.class);

        checkForAnnotatedJdk();
    }
    
    @Override
    public Void visitVariable(VariableTree node, Void p) {
    	//AnnotatedTypeMirror atm = varType.getAnnotationInHierarchy(GUARDEDBYINACCESSIBLE);
    	
    	if (node.getType().getKind() == Kind.PRIMITIVE_TYPE &&
    		node.toString().contains("Guard")) { // HACK!!! TODO
            checker.report(Result.failure("primitive.type.guardedby"), node);
    	}
    	return super.visitVariable(node, p);
    }

    @Override
    public LockAnnotatedTypeFactory createTypeFactory() {
        // We need to directly access useFlow from the checker, because this method gets called
        // by the superclass constructor and a field in this class would not be initialized
        // yet. Oh the pain.
        return new LockAnnotatedTypeFactory(checker, true); // TODO: Do we still need this?
    }
    
    @Override
    public Void visitMethod(MethodTree node, Void p) {

        SideEffectAnnotation sea = methodSideEffectAnnotation(TreeUtils.elementFromDeclaration(node));

    	if (sea == SideEffectAnnotation.MAYRELEASELOCKS) {
    		/* Skip checking whether the receiver is @GuardSatisfied because it may not be the right design.
    		 * 
    		 * if (AnnotationUtils.areSameByClass(atypeFactory.getAnnotatedType(node.getReceiverParameter()).getAnnotationInHierarchy(GUARDEDBYINACCESSIBLE),
    				checkerGuardSatisfiedClass)){
                checker.report(Result.failure("guardsatisfied.with.mayreleaselocks"), node);
    		}*/

    		for(VariableTree vt : node.getParameters()) {
        		if (AnnotationUtils.areSameByClass(atypeFactory.getAnnotatedType(vt).getAnnotationInHierarchy(GUARDEDBYINACCESSIBLE),
        				checkerGuardSatisfiedClass)){
                    checker.report(Result.failure("guardsatisfied.with.mayreleaselocks"), node);
        		}
    		}
    	}
    	
    	return super.visitMethod(node, p);
    }

    // Hack: this is only for a paper deadline. Do it right. TODO
    @Override
    protected void checkMethodInvocability(AnnotatedExecutableType method,
            MethodInvocationTree node) {
        if (method.getReceiverType() == null) {
            // Static methods don't have a receiver.
            return;
        }
        if (method.getElement().getKind() == ElementKind.CONSTRUCTOR) {
            // TODO: Explicit "this()" calls of constructors have an implicit passed
            // from the enclosing constructor. We must not use the self type, but
            // instead should find a way to determine the receiver of the enclosing constructor.
            // rcv = ((AnnotatedExecutableType)atypeFactory.getAnnotatedType(atypeFactory.getEnclosingMethod(node))).getReceiverType();
            return;
        }

        AnnotatedTypeMirror methodReceiver = method.getReceiverType().getErased();
        AnnotatedTypeMirror treeReceiver = methodReceiver.shallowCopy(false);
        AnnotatedTypeMirror rcv = atypeFactory.getReceiverType(node);

        treeReceiver.addAnnotations(rcv.getEffectiveAnnotations());

        boolean receiverGuardSatisfied = false;

        Set<AnnotationMirror> annos = methodReceiver.getAnnotations();
        for(AnnotationMirror anno : annos) {
        	DeclaredType annotype = anno.getAnnotationType();
        	if (annotype.toString().equals("org.checkerframework.checker.lock.qual.GuardSatisfied")) { // TODO: hack!
        		receiverGuardSatisfied = true;
        	}
        }

        if (receiverGuardSatisfied) {
            Element invokedElement = TreeUtils.elementFromUse(node);

            if (invokedElement != null) {
    	        checkPreconditions(node,
    	                invokedElement,
    	                true,
    	                generatePreconditionsBasedOnGuards(rcv));
            }
        }
        else if (!atypeFactory.getTypeHierarchy().isSubtype(treeReceiver, methodReceiver)) {
            checker.report(Result.failure("method.invocation.invalid",
                TreeUtils.elementFromUse(node),
                treeReceiver.toString(), methodReceiver.toString()), node);
        }
    }

    // Hack: this is only for a paper deadline. Do it right. TODO
    @Override
    protected void checkArguments(List<? extends AnnotatedTypeMirror> requiredArgs,
            List<? extends ExpressionTree> passedArgs) {
        assert requiredArgs.size() == passedArgs.size() : "mismatch between required args (" + requiredArgs +
                ") and passed args (" + passedArgs + ")";

        Pair<Tree, AnnotatedTypeMirror> preAssCtxt = visitorState.getAssignmentContext();
        try {
            for (int i = 0; i < requiredArgs.size(); ++i) {
                visitorState.setAssignmentContext(Pair.<Tree, AnnotatedTypeMirror>of((Tree) null, (AnnotatedTypeMirror) requiredArgs.get(i)));

                AnnotatedTypeMirror reqArg = requiredArgs.get(i);
                        boolean formalParameterGuardSatisfied = false;


                        Set<AnnotationMirror> annos = reqArg.getAnnotations();
        for(AnnotationMirror anno : annos) {
        	DeclaredType annotype = anno.getAnnotationType();
        	if (annotype.toString().equals("org.checkerframework.checker.lock.qual.GuardSatisfied")) { // TODO: hack!
        		formalParameterGuardSatisfied = true;
        	}
        }

        // HACK: TODO: Conservatively check preconditions on every actual parameter, regardless of the type annotation on the formal parameter.

        ExpressionTree passedArg = passedArgs.get(i);

        AnnotatedTypeMirror passedArgType = atypeFactory.getAnnotatedType(passedArg);
        assert passedArgType != null : "null type for expression: " + passedArg;

        //System.out.println(passedArg);
        //System.out.println(passedArgType);
        
        if (formalParameterGuardSatisfied) {

        Element invokedElement = TreeUtils.elementFromUse(passedArg);

        if (invokedElement != null) {
	        checkPreconditions(passedArg,
	                invokedElement,
	                passedArg.getKind() == Tree.Kind.METHOD_INVOCATION,
	                generatePreconditionsBasedOnGuards(passedArgType));
        }
        }
        else {
                commonAssignmentCheck(reqArg, passedArg,
                        "argument.type.incompatible", false);
        }
                // Also descend into the argument within the correct assignment
                // context.
                scan(passedArgs.get(i), null);
            }
        } finally {
            visitorState.setAssignmentContext(preAssCtxt);
        }
    }
    
    private enum SideEffectAnnotation {
        MAYRELEASELOCKS,
        RELEASESNOLOCKS,
        LOCKINGFREE,
        SIDEEFFECTFREE,
        PURE
    }

    protected SideEffectAnnotation methodSideEffectAnnotation(Element element) {
    	if (element == null)
    		return SideEffectAnnotation.MAYRELEASELOCKS;
    	
    	// If more than one annotation is present, this method issues a warning and returns
    	// the most annotation providing the weakest guarantee.
    	
    	// If no annotation is present, return RELEASESNOLOCKS as the default. TODO: conservative library default
    	
    	boolean[] sideEffectAnnotationPresent = new boolean[5];
    	
    	sideEffectAnnotationPresent[0] = atypeFactory.getDeclAnnotation(element, checkerMayReleaseLocksClass) != null;
    	sideEffectAnnotationPresent[1] = atypeFactory.getDeclAnnotation(element, checkerReleasesNoLocksClass) != null;
    	sideEffectAnnotationPresent[2] = atypeFactory.getDeclAnnotation(element, checkerLockingFreeClass) != null;
    	sideEffectAnnotationPresent[3] = atypeFactory.getDeclAnnotation(element, sideEffectFreeClass) != null;
    	sideEffectAnnotationPresent[4] = atypeFactory.getDeclAnnotation(element, pureClass) != null;
        
        int count = 0;
        
        for(int i = 0; i < 5; i++) {
        	if (sideEffectAnnotationPresent[i])
        		count++;
        }
        
        if (count == 0) {
        	return SideEffectAnnotation.RELEASESNOLOCKS;
        }
        
        if (count > 1) {
            // checker.report(Result.failure("", ), overriderTree);
        	// TODO: Implement warning
        	// TODO: Make sure ton of warnings are not issued for each method call of a method that has 2 annos.
        	// A warning should be issued once per method.
        }
        
        for(int i = 0; i < 5; i++) {
        	if (sideEffectAnnotationPresent[i])
        		return SideEffectAnnotation.values()[i];
        }
        
        return SideEffectAnnotation.MAYRELEASELOCKS;
    }

    /*@Override
    protected void checkAccess(IdentifierTree node, Void p) {
        // This method is called by visitIdentifier (and only visitIdentifier).

        // Unless the identifier is a primitive or syntactic sugar for another expression, do not check preconditions.
        // Preconditions in the Lock Checker for reference types must not be
        // checked by visitIdentifier, since we want only dereferences of variables
        // to have their Preconditions enforced, but not every instance of the variable
        // (due to by-value instead of by-variable semantics for the Lock Checker).
        // The exception to this will be visitSynchronized, but that will be handled separately.
        // See the Lock Checker manual chapter definitions of dereferencing a value/variable
        // for more information.

        Node nodeNode = atypeFactory.getNodeForTree(node);

        // TODO: A check such as the following should determine whether the identifier
        // evaluates to a primitive type even when it looks like a reference type (e.g.
        // unboxing of a boxed type).
        // (nodeNode != null && nodeNode.getInSource() == false)
        // This doesn't work as expected, however, because the correct inSource information
        // is stored in ControlFlowGraph.convertedTreeLookup, whereas at this point
        // only ControlFlowGraph.treeLookup is available (via atypeFactory.getNodeForTree).
        // The precise point in the code where this information is lost (i.e. a reference
        // to convertedTreeLookup is not copied to the analysis result) is in the following
        // two lines in GenericAnnotatedTypeFactory.analyze :

        // analyses.getFirst().performAnalysis(cfg);
        // AnalysisResult<Value, Store> result = analyses.getFirst().getResult();

        // convertedTreeLookup is available in cfg, but a reference to it is not copied
        // over in getResult(). This should be fixed in a future release. At the present
        // time, such a change would unduly introduce risk to the release.

        // As a temporary workaround, boxed types are conservatively always treated
        // as if they are being converted to a primitive, even when they are being used
        // as a reference (since we can't tell which one is the case). This will conservatively
        // result in more errors visible to the user.

        boolean doCheckPreconditions = (nodeNode != null && TypesUtils.isBoxedPrimitive(nodeNode.getType())) ||
            (node instanceof JCTree && TypesUtils.isPrimitive(((JCTree) node).type));

        super.checkAccess(node, p, doCheckPreconditions);
    }

    @Override
    protected void commonAssignmentCheck(Tree varTree, ExpressionTree valueExp,
            String errorKey) {
        // If the RHS is known for sure to be a primitive type, skip the check.
        // Dereferences of primitives require the appropriate locks to be held,
        // but it does not require the annotations in the types involved in the
        // operation to match.
        // For example, given:
        // @GuardedBy("foo") int a;
        // @GuardedBy("bar") int b;
        // @GuardedBy({}) int c;
        // The expressions a = b, a = c, and a = b + c are legal from a
        // type-checking perspective, whereas none of them would be legal
        // if a, b and c were not primitives.

        if (!(valueExp instanceof JCTree && ((JCTree) valueExp).type.getKind().isPrimitive())) {
            super.commonAssignmentCheck(varTree, valueExp, errorKey);
        }
    }*/

    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        Set<? extends AnnotationMirror> tops = atypeFactory.getQualifierHierarchy().getTopAnnotations();
        Set<AnnotationMirror> annotationSet = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror anno : tops) {
            if (anno.equals(GUARDEDBYINACCESSIBLE)) {
                annotationSet.add(GUARDEDBY);
            }
            else {
                annotationSet.add(anno);
            }
        }
        return annotationSet;
    }

    private Set<Pair<String, String>> generatePreconditionsBasedOnGuards(AnnotatedTypeMirror atm) {
        return generatePreconditionsBasedOnGuards(atm.getAnnotations());
    }

    // Given a set of AnnotationMirrors, returns the list of lock expression preconditions
    // specified in all the @GuardedBy annotations in the set.
    // Returns an empty set if no such expressions are found.
    private Set<Pair<String, String>> generatePreconditionsBasedOnGuards(Set<AnnotationMirror> amList) {
        Set<Pair<String, String>> preconditions = new HashSet<>();

        if (amList != null) {
            for (AnnotationMirror annotationMirror : amList) {

                if (AnnotationUtils.areSameByClass( annotationMirror, checkerGuardedByClass) ||
                    AnnotationUtils.areSameByClass( annotationMirror, javaxGuardedByClass) ||
                    AnnotationUtils.areSameByClass( annotationMirror, jcipGuardedByClass)) {
                    if (AnnotationUtils.hasElementValue(annotationMirror, "value")) {
                        List<String> guardedByValue = AnnotationUtils.getElementValueArray(annotationMirror, "value", String.class, false);

                        for(String lockExpression : guardedByValue) {
                            preconditions.add(Pair.of(lockExpression, checkerLockHeldClass.toString().substring(10 /* "interface " */)));
                        }
                    }
                }
            }
        }

        return preconditions;
    }

    @Override
    protected void commonAssignmentCheck(AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType, Tree valueTree, /*@CompilerMessageKey*/ String errorKey,
            boolean isLocalVariableAssignement) {

        if (valueType.getKind() == TypeKind.NULL) {
            // Avoid issuing warnings about 'null' not matching the type of the variable.
            return;
        }

        Kind valueTreeKind = valueTree.getKind();

        switch(valueTreeKind) {
            case NEW_CLASS:
            case NEW_ARRAY:
                // Avoid issuing warnings when: @GuardedBy("this") Object guardedThis = new Object();
                // TODO: This is too broad and should be fixed to work the way it will work in the hacks repo.
            	// Do NOT do this if the LHS is @GuardedByBottom.
            	if (!AnnotationUtils.areSameIgnoringValues(varType.getAnnotationInHierarchy(GUARDEDBYINACCESSIBLE), GUARDEDBYBOTTOM))
                    return;
            	break;
            case INT_LITERAL:
            case LONG_LITERAL:
            case FLOAT_LITERAL:
            case DOUBLE_LITERAL:
            case BOOLEAN_LITERAL:
            case CHAR_LITERAL:
            case STRING_LITERAL:
            //case NULL_LITERAL: // Don't return in the case of NULL_LITERAL since the check must be made that the LHS is @GuardedByBottom.
                // Avoid issuing warnings when: guardedThis = "m";
                // TODO: This is too broad and should be fixed to work the way it will work in the hacks repo.
                return;
            default:
        }

        // Assigning a value with a @GuardedBy annotation to a variable with a @GuardedByInaccessible annotation is always
        // legal. However as a precaution we verify that the locks specified in the @GuardedBy annotation are held,
        // since this is our last chance to check anything before the @GuardedBy information is lost in the
        // assignment to the variable annotated with @GuardedByInaccessible. See the Lock Checker manual chapter discussion
        // on the @GuardedByInaccessible annotation for more details.
        if (AnnotationUtils.areSameIgnoringValues(varType.getAnnotationInHierarchy(GUARDEDBYINACCESSIBLE), GUARDEDBYINACCESSIBLE)) {
            if (AnnotationUtils.areSameIgnoringValues(valueType.getAnnotationInHierarchy(GUARDEDBYINACCESSIBLE), GUARDEDBY)) {
                ExpressionTree tree = (ExpressionTree) valueTree;

                checkPreconditions(tree,
                        TreeUtils.elementFromUse(tree),
                        tree.getKind() == Tree.Kind.METHOD_INVOCATION,
                      	generatePreconditionsBasedOnGuards(valueType));
            }
        }

        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, isLocalVariableAssignement);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void p) {
        // Just check the precondition on the expression of the member select tree.
        // It doesn't matter if the identifier is a method or field.
        // Here, we are checking that the lock must be held on the
        // expression.

        // Keep in mind, the expression itself may or may not be a
        // method call. Simple examples of expression.identifier :
        // myObject.field
        // myMethod().field
        // myObject.method()
        // myMethod().method()

        // by-value semantics require preconditions to be checked
        // on all value dereferences, including dereferences of method
        // return values.

        checkAccessOfExpression(node);

        return super.visitMemberSelect(node, p);
    }

    private void reportFailure(/*@CompilerMessageKey*/ String messageKey,
            MethodTree overriderTree,
            AnnotatedDeclaredType enclosingType,
            AnnotatedExecutableType overridden,
            AnnotatedDeclaredType overriddenType,
            List<String> overriderLocks,
            List<String> overriddenLocks
            ) {
        // Get the type of the overriding method.
        AnnotatedExecutableType overrider =
            atypeFactory.getAnnotatedType(overriderTree);

        if (overrider.getTypeVariables().isEmpty()
                && !overridden.getTypeVariables().isEmpty()) {
            overridden = overridden.getErased();
        }
        String overriderMeth = overrider.toString();
        String overriderTyp = enclosingType.getUnderlyingType().asElement().toString();
        String overriddenMeth = overridden.toString();
        String overriddenTyp = overriddenType.getUnderlyingType().asElement().toString();

        if (overriderLocks == null || overriddenLocks == null) {
            checker.report(Result.failure(messageKey,
                    overriderMeth, overriderTyp,
                    overriddenMeth, overriddenTyp), overriderTree);
        }
        else {
            checker.report(Result.failure(messageKey,
                    overriderMeth, overriderTyp,
                    overriddenMeth, overriddenTyp,
                    overriderLocks, overriddenLocks), overriderTree);
        }
    }


    // Ensures that subclass methods require a subset of the locks that a parent class method requires
    // Additionally ensures that subclass methods use @HoldingOnEntry (and not @Holding) if the parent
    // class method uses @HoldingOnEntry.
    @Override
    protected boolean checkOverride(MethodTree overriderTree,
            AnnotatedDeclaredType enclosingType,
            AnnotatedExecutableType overridden,
            AnnotatedDeclaredType overriddenType,
            Void p) {

        List<String> overriderLocks = methodHolding(TreeUtils.elementFromDeclaration(overriderTree));
        List<String> overriddenLocks = methodHolding(overridden.getElement());

        //List<String> overriderHoldingOnEntryLocks = methodHoldingOnEntry(TreeUtils.elementFromDeclaration(overriderTree));
        //List<String> overriddenHoldingOnEntryLocks = methodHoldingOnEntry(overridden.getElement());

        /*
         *  @Holding is a stronger requirement than @HoldingOnEntry, since it has both pre- and postconditions. Therefore:
         *
         *  If the overridden method uses @HoldingOnEntry, the overrider method can only use @HoldingOnEntry.
         *  If the overridden method uses @Holding, the overrider method can use either @Holding or @HoldingOnEntry.
         *
         */

        boolean isValid = true;

        /*if (!overriddenHoldingOnEntryLocks.isEmpty()) {
            if (!overriderLocks.isEmpty()) {
                isValid = false;
                reportFailure("override.holding.invalid.holdingonentry", overriderTree, enclosingType, overridden, overriddenType, null, null);
            } else if (!overriddenHoldingOnEntryLocks.containsAll(overriderHoldingOnEntryLocks)) {
                isValid = false;
                reportFailure("override.holding.invalid", overriderTree, enclosingType, overridden, overriddenType, overriderHoldingOnEntryLocks, overriddenHoldingOnEntryLocks);
            }
        } else {*/
            if (!overriderLocks.isEmpty()) {
                if (!overriddenLocks.containsAll(overriderLocks)) {
                    isValid = false;
                    reportFailure("override.holding.invalid", overriderTree, enclosingType, overridden, overriddenType, overriderLocks, overriddenLocks);
                }
            }/* else if (!overriddenLocks.containsAll(overriderHoldingOnEntryLocks)) {
                isValid = false;
                reportFailure("override.holding.invalid", overriderTree, enclosingType, overridden, overriddenType, overriderHoldingOnEntryLocks, overriddenLocks);
            }*/
        //}

        SideEffectAnnotation seaOfOverriderMethod = methodSideEffectAnnotation(TreeUtils.elementFromDeclaration(overriderTree));
        SideEffectAnnotation seaOfOverridenMethod = methodSideEffectAnnotation(overridden.getElement());

    	if (seaOfOverriderMethod.ordinal() < seaOfOverridenMethod.ordinal()) {
            isValid = false;
            reportFailure("override.sideeffect.invalid", overriderTree, enclosingType, overridden, overriddenType, null, null);
    	}

        return super.checkOverride(overriderTree, enclosingType, overridden, overriddenType, p) && isValid;
    }

    protected List<String> methodHolding(ExecutableElement element) {
        AnnotationMirror holding = atypeFactory.getDeclAnnotation(element, checkerHoldingClass);
        AnnotationMirror guardedBy
            = atypeFactory.getDeclAnnotation(element, jcipGuardedByClass);
        AnnotationMirror guardedByJavax
            = atypeFactory.getDeclAnnotation(element, javaxGuardedByClass);

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

    /*protected List<String> methodHoldingOnEntry(ExecutableElement element) {
        AnnotationMirror holdingOnEntry = atypeFactory.getDeclAnnotation(element, checkerHoldingOnEntryClass);

        if (holdingOnEntry == null)
            return Collections.emptyList();

        List<String> locks = new ArrayList<String>();

        List<String> holdingOnEntryValue = AnnotationUtils.getElementValueArray(holdingOnEntry, "value", String.class, false);
        locks.addAll(holdingOnEntryValue);

        return locks;
    }*/

    /**
     * Checks all the preconditions of the method invocation or variable access {@code tree} with
     * element {@code invokedElement}.
     */
    @Override
    protected void checkPreconditions(Tree tree,
            Element invokedElement, boolean methodCall, Set<Pair<String, String>> additionalPreconditions) {

        if (additionalPreconditions == null) {
            additionalPreconditions = new HashSet<>();
        }

        // Retrieve the @GuardedBy annotation on the receiver of the enclosing method
        // and add it to the preconditions set if the receiver on the enclosing method is
        // the same as the receiver on the field/method we are checking preconditions for.
        MethodTree enclosingMethod = TreeUtils.enclosingMethod(atypeFactory.getPath(tree));

        if (false && enclosingMethod != null) {
            ExecutableElement methodElement = TreeUtils.elementFromDeclaration(enclosingMethod);

            if (methodElement != null) {
                TypeMirror rt = methodElement.getReceiverType();

                if (rt != null) {
                    List<? extends AnnotationMirror> amList = rt.getAnnotationMirrors();

                    for (AnnotationMirror annotationMirror : amList) {

                        if (AnnotationUtils.areSameByClass( annotationMirror, checkerGuardedByClass) ||
                            AnnotationUtils.areSameByClass( annotationMirror, javaxGuardedByClass) ||
                            AnnotationUtils.areSameByClass( annotationMirror, jcipGuardedByClass)) {
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
                                    for (String lockExpression : guardedByValue) {

                                        if (lockExpression.equals("itself")) {
                                            // This is critical. That's because right now we know that, since
                                            // we are dealing with the receiver of the method, "itself" corresponds
                                            // to "this". However once super.checkPreconditions is called, that
                                            // knowledge is lost and it will think that "itself" is referring to
                                            // the variable the precondition we are about to add is attached to.
                                            lockExpression = "this";
                                        }

                                        additionalPreconditions.add(Pair.of(lockExpression, checkerLockHeldClass.toString().substring(10 /* "interface " */)));
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

    // We check the access of the expression of an ArrayAccessTree or
    // a MemberSelectTree, both of which happen to implement ExpressionTree.
    // The 'Expression' in checkAccessOfExpression is not the same as that in
    // 'Expression'Tree - the naming is a coincidence.
    protected void checkAccessOfExpression(ExpressionTree tree) {
        boolean isMethodCall = false;

    	Kind treeKind = tree.getKind();
        assert(treeKind == Kind.ARRAY_ACCESS ||
               treeKind == Kind.MEMBER_SELECT);

        if (treeKind == Kind.MEMBER_SELECT) {
            Element treeElement = TreeUtils.elementFromUse(tree);

        	if (treeElement != null && treeElement.getKind() == ElementKind.METHOD) { // Method calls are not dereferences.
        		isMethodCall = true;
        	}
        }

        if (!isMethodCall) {
	        ExpressionTree expr = treeKind == Kind.ARRAY_ACCESS ?
	            ((ArrayAccessTree) tree).getExpression() :
	            ((MemberSelectTree) tree).getExpression();
	
	        Element invokedElement = TreeUtils.elementFromUse(expr);
	
	        AnnotatedTypeMirror receiverAtm = atypeFactory.getReceiverType(tree);
	
	        if (expr != null && invokedElement != null && receiverAtm != null) {
	        	boolean skipCheckPreconditions = false;
	            AnnotationMirror gb = receiverAtm.getAnnotationInHierarchy(GUARDEDBYINACCESSIBLE);
	        	if (gb != null) {
	                if (AnnotationUtils.areSameByClass( gb, checkerGuardedByClass) ||
	                    AnnotationUtils.areSameByClass( gb, javaxGuardedByClass) ||
	                    AnnotationUtils.areSameByClass( gb, jcipGuardedByClass)) {
	                	// Do nothing.
	        	    } else if (AnnotationUtils.areSameByClass(gb, checkerGuardSatisfiedClass)){
	        	    	skipCheckPreconditions = true; // Can always dereference if type is @GuardSatisfied        	    	
	        	    } else {
	        	    	// Can never dereference for any other types in the @GuardedBy hierarchy
	                    String annotationName = gb.toString();
	                    annotationName = annotationName.substring(annotationName.lastIndexOf('.') + 1 /* +1 to skip the last . as well */);
	        	    	checker.report(Result.failure(
	                            "cannot.dereference",
	                            tree.toString(),
	                            annotationName), tree);
	                    return;
	        	    }
	        	}
	
	        	if (skipCheckPreconditions == false) {
	        		checkPreconditions(expr, invokedElement, expr.getKind() == Tree.Kind.METHOD_INVOCATION, generatePreconditionsBasedOnGuards(receiverAtm));
	        	}
	        }
        }
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree node, Void p) {
        checkAccessOfExpression(node);

        return super.visitArrayAccess(node, p);
    }

    /**
     * Whether to skip a contract check based on whether the @GuardedBy
     * expression {@code expr} is valid for the tree {@code tree}
     * under the context {@code flowExprContext}
     * if the current path is within the expression
     * of a synchronized block (e.g. bar in
     * synchronized(bar) { ... }
     *
     *  @param tree The tree that is @GuardedBy.
     *  @param expr The expression of the @GuardedBy annotation.
     *  @param flowExprContext The current context.
     *
     *  @return Whether to skip the contract check.
     */
    @Override
    protected boolean skipContractCheck(Tree tree, FlowExpressions.Receiver expr, FlowExpressionContext flowExprContext) {
        String fieldName = null;

        try {

            Node nodeNode = atypeFactory.getNodeForTree(tree);

            if (nodeNode instanceof FieldAccessNode) {

                fieldName = ((FieldAccessNode) nodeNode).getFieldName();

                if (fieldName != null) {
                    FlowExpressions.Receiver fieldExpr = FlowExpressionParseUtil.parse(fieldName,
                            flowExprContext, getCurrentPath());

                    // TODO: Is this still needed? Everything may just work without this:
                    if (fieldExpr.equals(expr)) {
                        // Avoid issuing warnings when accessing the field that is guarding the receiver.
                        // e.g. avoid issuing a warning when accessing bar below:
                        // void foo(@GuardedBy("bar") myClass this) { synchronized(bar) { ... }}

                        // Also avoid issuing a warning in this scenario:
                        // @GuardedBy("bar") Object bar;
                        // ...
                        // synchronized(bar) { ... }

                        // Cover only the most common case: synchronized(variableName).
                        // If the expression in the synchronized statement is more complex,
                        // we do want a warning to be issued so the user can take a closer look
                        // and see if the variable is safe to be used this way.

                        TreePath path = getCurrentPath().getParentPath();

                        if (path != null) {
                            path = path.getParentPath();

                            if (path != null && path.getLeaf().getKind() == Tree.Kind.SYNCHRONIZED) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (FlowExpressionParseException e) {
            checker.report(e.getResult(), tree);
        }

        return false;
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType, Tree tree) {
        declarationType.replaceAnnotation(GUARDEDBY);
        useType.replaceAnnotation(GUARDEDBY);

        return super.isValidUse(declarationType, useType, tree);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {

    	SideEffectAnnotation seaOfInvokedMethod = methodSideEffectAnnotation(TreeUtils.elementFromUse(node));

        MethodTree enclosingMethod = TreeUtils.enclosingMethod(atypeFactory.getPath(node));

        ExecutableElement methodElement = null;
        if (enclosingMethod != null) {
        	methodElement = TreeUtils.elementFromDeclaration(enclosingMethod);
        }
    	
    	SideEffectAnnotation seaOfContainingMethod = methodSideEffectAnnotation(methodElement);
    	// TODO: Think about methods enclosing other methods
    	
    	if (seaOfInvokedMethod.ordinal() < seaOfContainingMethod.ordinal()) {
	    	checker.report(Result.failure(
                    "method.guarantee.violated",
                    methodElement.toString(),
                    TreeUtils.elementFromUse(node).toString()), node);
    	}

    	return super.visitMethodInvocation(node, p);
    }
    
    @Override
    public Void visitSynchronized(SynchronizedTree node, Void p) {
    	LockAnalysis analysis = ((LockChecker)checker).getAnalysis();
    	
        javax.lang.model.util.Types types = analysis.getTypes();

        TypeMirror lockInterfaceTypeMirror = TypesUtils.typeFromClass(types, analysis.getEnv().getElementUtils(), Lock.class);

        TypeMirror expressionType = types.erasure(atypeFactory.getAnnotatedType(node.getExpression()).getUnderlyingType());

        if (types.isSubtype(expressionType, lockInterfaceTypeMirror)) {
	    	checker.report(Result.failure(
                    "explicit.lock.synchronized"), node);
        }

    	return super.visitSynchronized(node, p);
    }
}
