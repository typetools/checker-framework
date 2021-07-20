package org.checkerframework.checker.fenum;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.subtyping.SubtypingChecker;
import org.checkerframework.framework.qual.StubFiles;

import java.util.SortedSet;

import javax.annotation.processing.SupportedOptions;

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

    @Override
    public SortedSet<String> getSuppressWarningsPrefixes() {
        return SubtypingChecker.getSuppressWarningsPrefixes(
                this.visitor, super.getSuppressWarningsPrefixes());
    }
}
