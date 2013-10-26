package checkers.fenum;

import checkers.basetype.BaseTypeChecker;
import checkers.basetype.BaseTypeVisitor;
import checkers.subtyping.SubtypingChecker;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.SupportedOptions;

/**
 * The main checker class for the Fake Enum Checker.
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
 * </ol>
 *
 * @author wmdietl
 * @checker.framework.manual #fenum-checker Fake Enum Checker
 */
@SupportedOptions( { "quals" } )
public class FenumChecker extends BaseTypeChecker {

    /*
    @Override
    public void initChecker() {
        super.initChecker();
    }
    */

    /** Copied from SubtypingChecker; cannot reuse it, because SubtypingChecker is final.
     * @see SubtypingChecker#getSuppressWarningsKeys()
     */
    @Override
    public Collection<String> getSuppressWarningsKeys() {
        Set<String> swKeys = new HashSet<String>();
        Set<Class<? extends Annotation>> annos = ((BaseTypeVisitor<?>)visitor).getTypeFactory().getSupportedTypeQualifiers();
        if (annos.isEmpty())
            return super.getSuppressWarningsKeys();

        for (Class<? extends Annotation> anno : annos)
            swKeys.add(anno.getSimpleName().toLowerCase());

        return swKeys;
    }
}
