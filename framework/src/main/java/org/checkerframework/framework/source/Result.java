package org.checkerframework.framework.source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * Represents the outcome of a type-checking operation (success, warning, or failure, plus a list of
 * explanatory messages). {@link Result}s created during type-checking can be reported using {@link
 * SourceChecker#report}, which ultimately delivers an error or warning message via the JSR 199
 * compiler interface.
 *
 * @see SourceChecker#report
 */
public final class Result {

    private static enum Type {
        SUCCESS,
        FAILURE,
        WARNING;

        /** @return whichever of the given types is most serious */
        public static final Type merge(Type a, Type b) {
            if (a == FAILURE || b == FAILURE) {
                return FAILURE;
            } else if (a == WARNING || b == WARNING) {
                return WARNING;
            } else {
                return SUCCESS;
            }
        }
    }

    /** The type of result (success, warning, failure). */
    private final Type type;

    /** The messages for the results. */
    private final List<DiagMessage> messages;

    /** The success result. */
    public static final Result SUCCESS = new Result(Type.SUCCESS, null);

    /**
     * Creates a new failure result with the given message key.
     *
     * @param messageKey the key representing the reason for failure
     * @param args optional arguments to be included in the message
     * @return the failure result
     */
    public static Result failure(@CompilerMessageKey String messageKey, @Nullable Object... args) {
        return new Result(Type.FAILURE, Collections.singleton(new DiagMessage(messageKey, args)));
    }

    /**
     * Creates a new warning result with the given message.
     *
     * @param messageKey the key for the warning message
     * @param args optional arguments to be included in the message
     * @return the warning result
     */
    public static Result warning(@CompilerMessageKey String messageKey, @Nullable Object... args) {
        return new Result(Type.WARNING, Collections.singleton(new DiagMessage(messageKey, args)));
    }

    private Result(Type type, Collection<DiagMessage> messagePairs) {
        this.type = type;
        this.messages = new ArrayList<>();
        if (messagePairs != null) {
            for (DiagMessage msg : messagePairs) {
                String message = msg.getMessageKey();
                @Nullable Object[] args = msg.getArgs();
                if (args != null) {
                    args = Arrays.copyOf(msg.getArgs(), args.length);
                }
                this.messages.add(new DiagMessage(message, args));
            }
        }
    }

    /**
     * Merges two results into one.
     *
     * @param r the result to merge with this result
     * @return a result that is the success result if both this and {@code r} are success results,
     *     or a result that has the more significant type (failure &gt; warning &gt; success) and
     *     the message keys of both this result and {@code r}
     */
    public Result merge(Result r) {
        if (r == null) {
            return this;
        }

        if (r.isSuccess() && this.isSuccess()) {
            return SUCCESS;
        }

        List<DiagMessage> messages = new ArrayList<>(this.messages.size() + r.messages.size());
        messages.addAll(this.messages);
        messages.addAll(r.messages);
        return new Result(Type.merge(r.type, this.type), messages);
    }

    /** @return true if the result is success (not a failure or warning) */
    public boolean isSuccess() {
        return type == Type.SUCCESS;
    }

    /** @return true if the result is a failure */
    public boolean isFailure() {
        return type == Type.FAILURE;
    }

    /** @return true if the result is a warning */
    public boolean isWarning() {
        return type == Type.WARNING;
    }

    /** @return the message keys associated with the result */
    public List<String> getMessageKeys() {
        List<String> msgKeys = new ArrayList<>(getDiagMessages().size());
        for (DiagMessage msg : getDiagMessages()) {
            msgKeys.add(msg.getMessageKey());
        }

        return Collections.unmodifiableList(msgKeys);
    }

    /** @return an unmodifiable list of the message pairs */
    public List<DiagMessage> getDiagMessages() {
        return Collections.unmodifiableList(messages);
    }

    @SideEffectFree
    @Override
    public String toString() {
        switch (type) {
            case FAILURE:
                return "FAILURE: " + messages;
            case WARNING:
                return "WARNING: " + messages;
            case SUCCESS:
            default:
                return "SUCCESS";
        }
    }

    /**
     * A class that represents diagnostic messages.
     *
     * <p>{@code DiagMessage} encapsulates the message key which would identify the relevant
     * standard error message according to the user locale.
     *
     * <p>The optional arguments are possible custom strings for the error message.
     */
    public static class DiagMessage {
        private final @CompilerMessageKey String message;
        private Object[] args;

        protected DiagMessage(@CompilerMessageKey String message, Object[] args) {
            this.message = message;
            if (args == null) {
                this.args = new Object[0]; /*null->nn*/
            } else {
                this.args = Arrays.copyOf(args, args.length);
            }
        }

        /** @return the message key of this DiagMessage */
        public @CompilerMessageKey String getMessageKey() {
            return this.message;
        }

        /** @return the customized optional arguments for the message */
        public Object[] getArgs() {
            return this.args;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DiagMessage)) {
                return false;
            }

            DiagMessage other = (DiagMessage) obj;

            return (message.equals(other.message) && Arrays.equals(args, other.args));
        }

        @Pure
        @Override
        public int hashCode() {
            return Objects.hash(this.message, Arrays.hashCode(this.args));
        }

        @SideEffectFree
        @Override
        public String toString() {
            if (args.length == 0) {
                return message;
            }

            return message + " : " + Arrays.toString(args);
        }
    }
}
