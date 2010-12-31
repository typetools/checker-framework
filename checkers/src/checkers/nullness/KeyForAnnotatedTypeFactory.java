package checkers.nullness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree.Kind;

import checkers.nullness.quals.AssertNonNullIfTrue;
import checkers.nullness.quals.KeyFor;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.AnnotatedTypeMirror.AnnotatedTypeVariable;
import checkers.types.AnnotatedTypeMirror.AnnotatedWildcardType;
import checkers.util.AnnotationUtils;
import checkers.util.AnnotationUtils.AnnotationBuilder;
import checkers.util.TreeUtils;

public class KeyForAnnotatedTypeFactory extends BasicAnnotatedTypeFactory<KeyForSubchecker> {

	public KeyForAnnotatedTypeFactory(KeyForSubchecker checker,
			CompilationUnitTree root) {
		super(checker, root);
	}


	/* TODO: we currently do not substitute field types.
	 * postAsMemberOf only gives us the type of the receiver expression ("owner"),
	 * but not the Tree. Therefore, we could not decide the substitution.
	 * I think it shouldn't happen frequently to have a field
	 * with annotation @KeyFor("this").
	 * However, one field being marked as the key for a different field might
	 * be necessary, so changing a @KeyFor("map") into @KeyFor("recv.map")
	 * might be necessary.
	@Override
	protected void postAsMemberOf(AnnotatedTypeMirror type,
			AnnotatedTypeMirror owner, Element element) {
	}
    */

	// TODO
	/* Once the method substitution is stable, create 
	 * substituteNewClass. Look whether they can share code somehow.
	@Override
	public AnnotatedExecutableType constructorFromUse(NewClassTree call) {
		assert call != null;

		AnnotatedExecutableType constructor = super.constructorFromUse(call);

		Map<AnnotatedTypeMirror, AnnotatedTypeMirror> mappings = new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>();

		// Get the result type
		AnnotatedTypeMirror resultType = getAnnotatedType(call);

		// Modify parameters
		for (AnnotatedTypeMirror parameterType : constructor.getParameterTypes()) {
			AnnotatedTypeMirror combinedType = substituteNewClass(call, parameterType);
			mappings.put(parameterType, combinedType);
		}

		// TODO: upper bounds, throws?
		
		constructor = constructor.substitute(mappings);

		return constructor;
	}
	*/
	
	// TODO: doc
	@Override
	public AnnotatedExecutableType methodFromUse(MethodInvocationTree call) {
		assert call != null;
		// System.out.println("looking at call: " + call);
		
		AnnotatedExecutableType method = super.methodFromUse(call);

		Map<AnnotatedTypeMirror, AnnotatedTypeMirror> mappings = new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>();

		// Modify parameters
		for (AnnotatedTypeMirror parameterType : method.getParameterTypes()) {
			AnnotatedTypeMirror subst = substituteCall(call, parameterType);
			mappings.put(parameterType, subst);
		}

		// Modify return type
		AnnotatedTypeMirror returnType = method.getReturnType();
		if (returnType.getKind() != TypeKind.VOID) {
			AnnotatedTypeMirror subst = substituteCall(call, returnType);
			mappings.put(returnType, subst);
		}

		// TODO: upper bounds, throws?

		method = method.substitute(mappings);

		// System.out.println("adapted method: " + method);

		return method;
	}
	

	/* TODO: doc
	 * This pattern and the logic how to use it is copied from NullnessFlow.
	 * NullnessFlow already contains four exact copies of the logic for handling this
	 * pattern and should really be refactored.
	 */
    private static final Pattern parameterPtn = Pattern.compile("#(\\d+)");

    // TODO: doc
	private AnnotatedTypeMirror substituteCall(MethodInvocationTree call, AnnotatedTypeMirror inType) {
		// System.out.println("input type: " + inType);
		AnnotatedTypeMirror outType = inType.getCopy(true);

		if (inType.getAnnotation(KeyFor.class) != null) {
            AnnotationMirror anno = inType.getAnnotation(KeyFor.class);
            
            List<String> inMaps = AnnotationUtils.parseStringArrayValue(anno, "value");
            List<String> outMaps = new ArrayList<String>();
            
            // receiver method is in NullnessFlow
            // String receiver = receiver(call);
            /*
    		// Set the receiver type?
    		AnnotatedTypeMirror receiverType = null;
    		ExpressionTree exprTree = call.getMethodSelect();
    		if (exprTree.getKind() == Kind.MEMBER_SELECT) {
    			MemberSelectTree memberSelectTree = (MemberSelectTree) exprTree;
    			receiverType = getAnnotatedType(memberSelectTree.getExpression());
    		} else {
    			receiverType = getSelfType(call);
    			// is the following needed? remove!
    			// receiverType.clearAnnotations();
    			// receiverType.addAnnotation(GUTChecker.SELF);
    		}
    		assert receiverType != null;
    		System.out.println("Receiver: " + receiverType);
    		*/
            
            for (String inMapName : inMaps) {
            	// TODO: substitute "this" by receiver expression.
            	// What else should be supported?
            	
                if (parameterPtn.matcher(inMapName).matches()) {
                    int param = Integer.valueOf(inMapName.substring(1));
                    if (param < call.getArguments().size()) {
                    	String res = call.getArguments().get(param).toString();
                    	outMaps.add(res);
                    }
                } else {
                	// TODO: look at the code below, copied from NullnessFlow
                	System.out.println("KeyFor argument unhandled: " + inMapName);
                	// just copy name for now, better than doing nothing
                	outMaps.add(inMapName);
                }
                	/* if (parameterPtn.matcher(inMapName).find()) {
                	// TODO
                	/*
                    Matcher matcher = parameterPtn.matcher(inMapName);
                    matcher.find();
                    int param = Integer.valueOf(matcher.group(1));
                    if (param < call.getArguments().size()) {
                        String rep = call.getArguments().get(param).toString();

                        String val = matcher.replaceAll(rep);
                        System.out.println("What should be done with: " + val);
                        // asserts.add(receiver + val);
                        outMaps.add(val);
                    }
                    
                } else {
                	// TODO
                	// System.out.println("The last option is: " + inMapName);
                    // asserts.add(receiver + s);
                } */
            }
            AnnotationBuilder builder = new AnnotationBuilder(env, KeyFor.class);
            builder.setValue("value", outMaps);
            AnnotationMirror newAnno =  builder.build();
            
            outType.removeAnnotation(KeyFor.class);
            outType.addAnnotation(newAnno);
		} 
        
        if (outType.getKind() == TypeKind.DECLARED) {
            AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) outType;
            Map<AnnotatedTypeMirror, AnnotatedTypeMirror> mapping = new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>();

            // Get the substituted type arguments
            for (AnnotatedTypeMirror typeArgument : declaredType.getTypeArguments()) {
                AnnotatedTypeMirror substTypeArgument = substituteCall(call, typeArgument);
                mapping.put(typeArgument, substTypeArgument);
            }

            outType = declaredType.substitute(mapping);
        } else if (outType.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType  arrayType = (AnnotatedArrayType) outType;

            // Get the substituted component type
            AnnotatedTypeMirror elemType = arrayType.getComponentType();
   			AnnotatedTypeMirror substElemType = substituteCall(call, elemType);
	
   			arrayType.setComponentType(substElemType);
   			// outType aliases arrayType
        } else if(outType.getKind().isPrimitive() ||
        		outType.getKind() == TypeKind.WILDCARD ||
        		outType.getKind() == TypeKind.TYPEVAR) {
        	// TODO: for which of these should we also recursively substitute?
        	// System.out.println("KeyForATF: Intentionally unhandled Kind: " + outType.getKind());
        } else {
        	// System.err.println("KeyForATF: Unknown getKind(): " + outType.getKind());
            // assert false;
        }
		
        // System.out.println("result type: " + outType);
		return outType;
	}
}