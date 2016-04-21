// Test case for issue #665:
// https://github.com/typetools/checker-framework/issues/665

import java.util.Collection;
import java.util.Spliterator;
import java.util.Spliterators;

import org.checkerframework.checker.igj.qual.I;
import org.checkerframework.framework.qual.AnnotatedFor;


//@below-java8-jdk-skip-test

@I
@AnnotatedFor({"igj"})
public abstract class Issue665<E> implements @I Collection<E> {
    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this,
            Spliterator.DISTINCT | Spliterator.ORDERED);
    }
}
