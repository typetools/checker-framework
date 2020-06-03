package org.checkerframework.framework.source;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.MANDATORY_WARNING;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.javacutil.BugInCF;

/**
 * Represents the outcome of a type-checking operation (success, warning, or failure, plus a list of
 * explanatory messages). {@link Result}s created during type-checking can be reported using {@link
 * SourceChecker#report}, which ultimately delivers an error or warning message via the JSR 199
 * compiler interface.
 *
 * @see SourceChecker#report
 * @deprecated use {@link DiagMessage} or {@code List<DiagMessage>} instead
 */
@Deprecated // use {@link DiagMessage} or {@code List<DiagMessage>} instead
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
     * @deprecated use a {@link DiagMessage} instead, or call {@code reportError} or {@code
     *     reportWarning} directly
     */
    @Deprecated // use a {@link DiagMessage} instead, or call {@code reportError} or {@code
    // reportWarning} directly
    public static Result failure(@CompilerMessageKey String messageKey, @Nullable Object... args) {
        return new Result(
                Type.FAILURE, Collections.singleton(new DiagMessage(ERROR, messageKey, args)));
    }

    /**
     * Creates a new warning result with the given message key.
     *
     * @param messageKey the key for the warning message
     * @param args optional arguments to be included in the message
     * @return the warning result
     * @deprecated use a {@link org.checkerframework.framework.source.DiagMessage} instead
     */
    @Deprecated // use a {@link org.checkerframework.framework.source.DiagMessage} instead
    public static Result warning(@CompilerMessageKey String messageKey, @Nullable Object... args) {
        return new Result(
                Type.WARNING,
                Collections.singleton(new DiagMessage(MANDATORY_WARNING, messageKey, args)));
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
                javax.tools.Diagnostic.Kind kind;
                switch (type) {
                    case FAILURE:
                        kind = ERROR;
                        break;
                    case WARNING:
                        kind = MANDATORY_WARNING;
                        break;
                    case SUCCESS:
                    default:
                        throw new BugInCF(
                                "type=%s, messagePairs=%s", type, messagePairs.toString());
                }
                this.messages.add(new DiagMessage(kind, message, args));
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

    /**
     * Returns true if the result is success (not a failure or warning).
     *
     * @return true if the result is success (not a failure or warning)
     */
    public boolean isSuccess() {
        return type == Type.SUCCESS;
    }

    /**
     * Returns true if the result is a failure.
     *
     * @return true if the result is a failure
     */
    public boolean isFailure() {
        return type == Type.FAILURE;
    }

    /**
     * Returns true if the result is a warning.
     *
     * @return true if the result is a warning
     */
    public boolean isWarning() {
        return type == Type.WARNING;
    }

    /**
     * Returns the message keys associated with the result.
     *
     * @return the message keys associated with the result
     */
    public List<String> getMessageKeys() {
        List<String> msgKeys = new ArrayList<>(getDiagMessages().size());
        for (DiagMessage msg : getDiagMessages()) {
            msgKeys.add(msg.getMessageKey());
        }

        return Collections.unmodifiableList(msgKeys);
    }

    /**
     * Returns an unmodifiable list of the message pairs.
     *
     * @return an unmodifiable list of the message pairs
     */
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
}
