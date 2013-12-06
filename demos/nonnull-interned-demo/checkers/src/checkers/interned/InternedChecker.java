package checkers.interned;

import checkers.source.*;
import checkers.subtype.*;
import checkers.quals.Interned;
import checkers.types.AnnotatedTypeFactory;

import com.sun.source.tree.CompilationUnitTree;

import java.io.IOException;
import java.util.Properties;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;

/**
 * An annotation processor that checks a program's use of the {@code @Interned}
 * type annotation. The {@code @Interned} annotation indicates that a variable
 * refers to the canonical instance of an object, meaning that it is safe to
 * compare that object using the "==" operator. This plugin suggests using "=="
 * instead of ".equals" where possible, and warns whenever "==" is used in
 * cases where one or both operands are not {@code @Interned}[
 */
@SupportedAnnotationTypes({"*"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class InternedChecker extends SubtypeChecker {

    /**
     * Creates a {@link InternedChecker}.
     */ 
    public InternedChecker() {
        super(Interned.class);
    }

    @Override
    protected String getDefaultSkipPattern() {
        return "";
    }

    @Override
    protected Properties getMessages(String fileName) throws IOException {

        Properties msgs = new Properties();
        return msgs;
    }

    @Override
    public AnnotatedTypeFactory getFactory(ProcessingEnvironment env, CompilationUnitTree root) {
        return new InternedAnnotatedTypeFactory(env, root);
    }

    @Override
    public SourceVisitor getSourceVisitor(CompilationUnitTree root) {
        return new InternedVisitor(this, root);
    }

    @Override
    public boolean shouldSkip(String className) {
        return false;
    }

    @Override
    protected String getSuppressWarningsKey() {
        return "interned";
    }
}
