package java.lang;

import org.checkerframework.checker.lock.qual.GuardSatisfied;

@SuppressWarnings("try")
public interface AutoCloseable {
    void close(@GuardSatisfied AutoCloseable this) throws Exception;
}
