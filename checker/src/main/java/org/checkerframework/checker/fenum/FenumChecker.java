package org.checkerframework.checker.fenum;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.SupportedOptions;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.subtyping.SubtypingChecker;
import org.checkerframework.framework.qual.StubFiles;

/**
 * The main checker class for the Fake Enum Checker.
 *
 * <p>There are two options to distinguish different enumerators:
 *
 * <ol>
 *   <li>{@code @Fenum("Name")}: introduces a fake enumerator with the name "Name". Enumerators with
 *       different names are distinct. The default name is empty, but you are encouraged to use a
 *       unique name for your purpose.
 *   <li>Alternatively, you can specify the annotation to use with the {@code -Aqual} command line
 *       argument.
 * </ol>
 *
 * @checker_framework.manual #fenum-checker Fake Enum Checker
 */
@StubFiles("jdnc.astub")
@SupportedOptions({"quals", "qualDirs"})
public class FenumChecker extends BaseTypeChecker {

    /*
    @Override
    public void initChecker() {
        super.initChecker();
    }
    */

    /**
     * Copied from SubtypingChecker; cannot reuse it, because SubtypingChecker is final.
     *
     * @see SubtypingChecker#getSuppressWarningsKeys()
     */
    @Override
    public Collection<String> getSuppressWarningsKeys() {
        Set<Class<? extends Annotation>> annos =
                ((BaseTypeVisitor<?>) visitor).getTypeFactory().getSupportedTypeQualifiers();
        if (annos.isEmpty()) {
            return super.getSuppressWarningsKeys();
        }

        Set<String> swKeys = new HashSet<>();
        swKeys.add(SUPPRESS_ALL_KEY);
        for (Class<? extends Annotation> anno : annos) {
            swKeys.add(anno.getSimpleName().toLowerCase());
        }

        return swKeys;
    }
}
