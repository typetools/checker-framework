package org.checkerframework.common.subtyping;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.type.AnnotationClassLoader;

public class SubtypingAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public SubtypingAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
    }

    @Override
    protected AnnotationClassLoader createAnnotationClassLoader() {
        return new SubtypingAnnotationClassLoader(checker);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        // Subtyping Checker doesn't have a qual directory, so we instantiate the loader here to
        // load externally declared annotations
        loader = createAnnotationClassLoader();

        Set<Class<? extends Annotation>> qualSet = new LinkedHashSet<>();

        String qualNames = checker.getOption("quals");
        String qualDirectories = checker.getOption("qualDirs");

        if (qualNames == null && qualDirectories == null) {
            checker.userErrorAbort(
                    "SubtypingChecker: missing required option. Use -Aquals or -AqualDirs");
            throw new Error("This can't happen"); // dead code
        }

        // load individually named qualifiers
        if (qualNames != null) {
            for (String qualName : qualNames.split(",")) {
                qualSet.add(loader.loadExternalAnnotationClass(qualName));
            }
        }

        // load directories of qualifiers
        if (qualDirectories != null) {
            for (String dirName : qualDirectories.split(":")) {
                qualSet.addAll(loader.loadExternalAnnotationClassesFromDirectory(dirName));
            }
        }

        // check for subtype meta-annotation
        for (Class<? extends Annotation> qual : qualSet) {
            Annotation subtypeOfAnnotation = qual.getAnnotation(SubtypeOf.class);
            if (subtypeOfAnnotation != null) {
                for (Class<? extends Annotation> superqual :
                        qual.getAnnotation(SubtypeOf.class).value()) {
                    if (!qualSet.contains(superqual)) {
                        checker.userErrorAbort(
                                "SubtypingChecker: qualifier "
                                        + qual
                                        + " was specified via -Aquals but its super-qualifier "
                                        + superqual
                                        + " was not");
                    }
                }
            }
        }

        return qualSet;
    }
}
