package checkers.fenum;


import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.SupportedOptions;

import checkers.fenum.quals.FenumTop;
import checkers.fenum.quals.Fenum;
import checkers.fenum.quals.FenumUnqualified;
import checkers.fenum.quals.FenumBottom;
import checkers.source.SupportedLintOptions;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.basetype.BaseTypeChecker;

/**
 * The main checker class for the fake enum checker.
 * 
 * There are two options to distinguish different enumerators:
 * 
 * <ol>
 * <li> {@code @Fenum("Name")}: introduces a fake enumerator with the name
 * "Name". Enumerators with different names are distinct. The default name is
 * empty, but you are encouraged to use a unique name for your purpose.
 * </li>
 * 
 * <li> Alternatively, you can specify the annotation to use with the
 * {@code -Aqual} command line argument. 
 * </li>
 * </ul>
 * 
 * TODO: document flowinference lint option. 
 * 
 * @author wmdietl
 */
@SupportedOptions( { "quals" } )
@SupportedLintOptions( { "flowinference" } )
public class FenumChecker extends BaseTypeChecker {
    /** Copied from BasicChecker.
     * Instead of returning an empty set if no "quals" option is given,
     * we return Fenum as the only qualifier.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        Set<Class<? extends Annotation>> qualSet =
            new HashSet<Class<? extends Annotation>>();

        String qualNames = env.getOptions().get("quals");
        if (qualNames == null) {
        	// maybe issue a warning?
        	qualSet.add(FenumTop.class);
        	qualSet.add(Fenum.class);
        	qualSet.add(FenumUnqualified.class);
        	qualSet.add(FenumBottom.class);
		} else {
			try {
				for (String qualName : qualNames.split(",")) {
					final Class<? extends Annotation> q =
						(Class<? extends Annotation>) Class.forName(qualName);
					qualSet.add(q);
				}
			} catch (ClassNotFoundException e) {
				throw new Error(e);
			}
			qualSet.add(FenumTop.class);
			qualSet.add(Fenum.class);
			qualSet.add(FenumUnqualified.class);
			qualSet.add(FenumBottom.class);
		}
        return Collections.unmodifiableSet(qualSet);
    }

    /** Copied from BasicChecker; cannot reuse it, because BasicChecker is final.
     */
    @Override
    public Collection<String> getSuppressWarningsKey() {
        Set<String> swKeys = new HashSet<String>();
        Set<Class<? extends Annotation>> annos = getSupportedTypeQualifiers();
        if (annos.isEmpty())
            return super.getSuppressWarningsKey();

        for (Class<? extends Annotation> anno : annos)
            swKeys.add(anno.getSimpleName().toLowerCase());

        return swKeys;
    }
    
    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType) {
		// The checker calls this method to compare the annotation used in a
		// type to the modifier it adds to the class declaration. As our default
		// modifier is Unqualified, this results in an error when a non-subtype
		// is used. Just ignore this check here and do them manually in the
		// visitor.
    	return true;
    }
    
    /** TODO: this is a copy of the super-method, just slightly modified to try
     * adding the FenumBottom to all qualifiers.
     */
    /*
    @Override
	protected QualifierHierarchy createQualifierHierarchy() {
    	AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);

		GraphQualifierHierarchy.Factory factory = new GraphQualifierHierarchy.Factory();
		AnnotationMirror bottom = annoFactory.fromClass(FenumBottom.class);
		
		for (Class<? extends Annotation> typeQualifier : getSupportedTypeQualifiers()) {
			if (typeQualifier.equals(Unqualified.class)) {
				factory.addQualifier(null);
				continue;
			}
			AnnotationMirror typeQualifierAnno = annoFactory
					.fromClass(typeQualifier);
			factory.addQualifier(typeQualifierAnno);
			if (typeQualifier.getAnnotation(SubtypeOf.class) == null) {
				// polymorphic qualifiers don't need to declared their
				// supertypes
				if (typeQualifier.getAnnotation(PolymorphicQualifier.class) != null)
					continue;
				throw new AssertionError(typeQualifier
						+ " does not specify its super qualifiers");
			}
			Class<? extends Annotation>[] superQualifiers = typeQualifier
					.getAnnotation(SubtypeOf.class).value();
			for (Class<? extends Annotation> superQualifier : superQualifiers) {
				AnnotationMirror superAnno = null;
				if (superQualifier != Unqualified.class)
					superAnno = annoFactory.fromClass(superQualifier);
				factory.addSubtype(typeQualifierAnno, superAnno);
			}	
			
			if( typeQualifier.getCanonicalName()!= "checkers.fenum.FenumBottom" ) {
				System.out.println("Adding subtype to : " + typeQualifier.getCanonicalName());
				// WMD TODO: get this working!
				// WMD add bottom to all qualifiers
				// factory.addSubtype(bottom, typeQualifierAnno);
			}
		}
		QualifierHierarchy hierarchy = factory.build();
		if (hierarchy.getTypeQualifiers().size() < 2) {
			throw new IllegalStateException(
					"Invalid qualifier hierarchy: hierarchy requires at least two annotations: "
							+ hierarchy.getTypeQualifiers());
		}
		return hierarchy;
	}
	*/
}
