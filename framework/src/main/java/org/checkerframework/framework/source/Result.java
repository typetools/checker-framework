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
import org.checkerframework.javacutil.BugInCF;

/**
 * Represents the outcome of a type-checking operation (success, warning, or failure, plus a list of
 * explanatory messages). {@link Result}s created during type-checking can be reported using {@link
 * SourceChecker#report}, which ultimately delivers an error or warning message via the JSR 199
 * compiler interface.
 *
 * @see SourceChecker#report
 */
public final class Result {

    /** The kinds of results: SUCCESS, WARNING, or FAILURE. */
    private static enum Type {
        /** A successful result. */
        SUCCESS,
        /** A result containing a warning but no failures. */
        WARNING,
        /** A result containing a failure. */
        FAILURE;

        /**
         * Return whichever of the given types is most severe.
         *
         * @param a the first result kind to compare
         * @param b the second result kind to compare
         * @return whichever of the given types is most severe
         */
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
     * Creates a new warning result with the given message key.
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
     * <p>If both this and {@code r} are success results, returns the success result.
     *
     * <p>Otherwise, returns a result with the more severe type (failure &gt; warning &gt; success)
     * and the union of the messages.
     *
     * @param r the result to merge with this result
     * @return the merge of the two results
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
                return "SUCCESS";
            default:
                throw new BugInCF("Unhandled Result type %s in %s", type, this);
        }
    }

    /**
     * A {@code DiagMessage} is a message key plus arguments. The message key will be expanded
     * according to the user locale. Any arguments will then be interpolated into the localized
     * message.
     */
    public static class DiagMessage {
        /** The message key. */
        private final @CompilerMessageKey String messageKey;
        /** The arguments that will be interpolated into the localized message. */
        private Object[] args;

        /**
         * Create a DiagMessage.
         *
         * @param messageKey the message key.
         * @param args the arguments that will be interpolated into the localized message.
         */
        protected DiagMessage(@CompilerMessageKey String messageKey, Object[] args) {
            this.messageKey = messageKey;
            if (args == null) {
                this.args = new Object[0]; /*null->nn*/
            } else {
                this.args = Arrays.copyOf(args, args.length);
            }
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

            return (messageKey.equals(other.messageKey) && Arrays.equals(args, other.args));
        }

        @Pure
        @Override
        public int hashCode() {
            return Objects.hash(this.messageKey, Arrays.hashCode(this.args));
        }

        @SideEffectFree
        @Override
        public String toString() {
            if (args.length == 0) {
                return messageKey;
            }

            return messageKey + " : " + Arrays.toString(args);
        }
    }
}
