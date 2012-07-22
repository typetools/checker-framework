package checkers.nonnull;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.annotation.processing.ProcessingEnvironment;

import checkers.quals.*;
import checkers.source.SourceVisitor;
import checkers.source.SupportedLintOptions;
import checkers.subtype.SubtypeChecker;
import checkers.types.AnnotatedTypeFactory;

import com.sun.source.tree.CompilationUnitTree;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * An annotation processor that checks a program's use of the {@code @NonNull}
 * type annotation. The {@code @NonNull} annotation indicates that a variable
 * should never have a null value. This plugin issues a warning whenever
 * a variable has been declared {@code @NonNull} and may become null.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({"checkers.nullness.quals.NonNull","checkers.quals.Nullable"})
@SupportedLintOptions({"flow", "cast", "cast:redundant"})
@DefaultQualifier("checkers.nullness.quals.NonNull")
public class NonnullChecker extends SubtypeChecker {

    /**
     * Creates a {@link NonnullChecker}.
     */
    public NonnullChecker() {
        // The @NonNull annotation is the annotation that the SubtypeChecker
        // implementation will "think" in terms of (i.e., @Nullable will be
        // converted to "not @NonNull".
        super(NonNull.class);
    }

    @Override
    protected Properties getMessages(String fileName) throws IOException {
        
        // Use a message file if one exists.
        if (new File(fileName).exists())
            return super.getMessages(fileName);
       
        // Sets message defaults.
        Properties msgDefaults = new Properties();
        msgDefaults.setProperty("assignment.invalid", "invalid assignment: expected %s, found %s");
        msgDefaults.setProperty("assignment.compound.invalid", 
                "invalid compound assignment type");
        msgDefaults.setProperty("argument.invalid", "invalid argument: expected %s, found %s");
        msgDefaults.setProperty("receiver.invalid", "invalid receiver: expected %s, found %s");
        msgDefaults.setProperty("return.invalid", "invalid return: expected %s, found %s");
        msgDefaults.setProperty("override.param.invalid", "invalid overriding parameter type");
        msgDefaults.setProperty("override.return.invalid", "invalid overriding return type");
        msgDefaults.setProperty("cast.annotated", "annotated cast");
        msgDefaults.setProperty("cast.redundant", "redundant cast");
        msgDefaults.setProperty("unary.invalid", "invalid unary expression type");
        msgDefaults.setProperty("deref.invalid", "invalid dereference: found %s");
        
        return msgDefaults;
    }

    @Override
    public AnnotatedTypeFactory getFactory(ProcessingEnvironment env, CompilationUnitTree root) {
        boolean useFlow = this.getLintOption("flow", true);
        return new NonnullAnnotatedTypeFactory(env, root, useFlow, true);
    }

    @Override
    public SourceVisitor getSourceVisitor(CompilationUnitTree root) {
        return new NonnullVisitor(this, root);
    }

    @Override
    public String getDefaultSkipPattern() {
        return "^java.*";
    }

    @Override
    public String getSuppressWarningsKey() {
        return "nonnull";
    }
}
