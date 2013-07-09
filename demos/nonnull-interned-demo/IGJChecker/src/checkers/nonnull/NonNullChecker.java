package checkers.nonnull;

import java.util.Collection;
import java.util.Properties;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.CompilationUnitTree;

import checkers.basetype.BaseTypeChecker;
import checkers.basetype.BaseTypeVisitor;
import checkers.metaquals.TypeQualifiers;
import checkers.nullness.quals.NonNull;
import checkers.quals.Nullable;
import checkers.source.*;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotationFactory;
import checkers.types.AnnotationRelations;
import checkers.util.*;

/**
 * A typechecker plug-in for the {@link NonNull} qualifier that finds (and
 * verifies the absence of) null-pointer errors.
 */
@SupportedAnnotationTypes({"checkers.nullness.quals.NonNull", "checkers.quals.Nullable"})
@SupportedLintOptions({"flow", "cast", "cast:redundant"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SuppressWarningsKey("nonnull")
@TypeQualifiers( { Nullable.class, NonNull.class } )
public class NonNullChecker extends BaseTypeChecker {

    @Override
    protected Properties getMessages() {
        Properties msgs = new Properties(super.getMessages());
        msgs.setProperty("dereference.of.nullable",
                "cannot dereference the possibly-null value %s");
        return msgs;
    }

    @Override
    protected BaseTypeVisitor<?, ?> getSourceVisitor(CompilationUnitTree root) {
        return new NonNullVisitor(this, root);
    }

    @Override
    public AnnotatedTypeFactory createFactory(ProcessingEnvironment env,
            CompilationUnitTree root) {
        return new NonNullAnnotatedTypeFactory(env, root); 
    }
}
