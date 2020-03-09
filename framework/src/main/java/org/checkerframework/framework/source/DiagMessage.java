package org.checkerframework.framework.source;

import java.util.Arrays;
import java.util.Objects;
import javax.tools.Diagnostic.Kind;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * A {@code DiagMessage} is a kind, a message key, and arguments. The message key will be expanded
 * according to the user locale. Any arguments will then be interpolated into the localized message.
 *
 * <p>By contrast, {@code javax.tools.Diagnostic} has just a string message.
 */
public class DiagMessage {
    /** The kind of message. */
    private final Kind kind;
    /** The message key. */
    private final @CompilerMessageKey String messageKey;
    /** The arguments that will be interpolated into the localized message. */
    private final Object[] args;

    /**
     * Create a DiagMessage.
     *
     * @param kind the kind of message
     * @param messageKey the message key
     * @param args the arguments that will be interpolated into the localized message
     */
    public DiagMessage(Kind kind, @CompilerMessageKey String messageKey, Object... args) {
        this.kind = kind;
        this.messageKey = messageKey;
        if (args == null) {
            this.args = new Object[0]; /*null->nn*/
        } else {
            this.args = Arrays.copyOf(args, args.length);
        }
    }

    /** @return the kind of this DiagMessage */
    public Kind getKind() {
        return this.kind;
    }

    /** @return the message key of this DiagMessage */
    public @CompilerMessageKey String getMessageKey() {
        return this.messageKey;
    }

    /** @return the customized optional arguments for the message */
    public Object[] getArgs() {
        return this.args;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof DiagMessage)) {
            return false;
        }

        DiagMessage other = (DiagMessage) obj;

        return (kind == other.kind
                && messageKey.equals(other.messageKey)
                && Arrays.equals(args, other.args));
    }

    @Pure
    @Override
    public int hashCode() {
        return Objects.hash(kind, messageKey, Arrays.hashCode(args));
    }

    @SideEffectFree
    @Override
    public String toString() {
        if (args.length == 0) {
            return messageKey;
        }

        return kind + messageKey + " : " + Arrays.toString(args);
    }
}
