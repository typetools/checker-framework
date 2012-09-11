package javax.annotation.processing;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import javax.lang.model.element.*;
import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;

import checkers.javari.quals.*;

public abstract class AbstractProcessor implements Processor {
    protected ProcessingEnvironment processingEnv;
    protected AbstractProcessor() { throw new RuntimeException("skeleton method"); }
    public @PolyRead Set<String> getSupportedOptions(@PolyRead AbstractProcessor this) { throw new RuntimeException("skeleton method"); }
    public @PolyRead Set<String> getSupportedAnnotationTypes(@PolyRead AbstractProcessor this) { throw new RuntimeException("skeleton method"); }
    public @PolyRead SourceVersion getSupportedSourceVersion(@PolyRead AbstractProcessor this) { throw new RuntimeException("skeleton method"); }
    public synchronized void init(ProcessingEnvironment processingEnv) { throw new RuntimeException("skeleton method"); }
    public abstract boolean process(Set<? extends TypeElement> annotations,
                    RoundEnvironment roundEnv);
    public @PolyRead Iterable<? extends Completion> getCompletions(@PolyRead AbstractProcessor this, Element element,
                             AnnotationMirror annotation,
                             ExecutableElement member,
                             String userText) { throw new RuntimeException("skeleton method"); }
    protected synchronized boolean isInitialized(@ReadOnly AbstractProcessor this) { throw new RuntimeException("skeleton method"); }
}
