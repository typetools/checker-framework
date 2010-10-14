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
 * @author wmdietl
 */
@SupportedOptions( { "quals" } )
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
}
