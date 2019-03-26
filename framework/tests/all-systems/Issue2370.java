import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("")
public class Issue2370 {
    private Stream<Action2370> getAction2370s(final State2370 state) {
        return Stream.of(
                        toStream(state.getOnExit()).flatMap(t -> t.getAction2370s().stream()),
                        toStream(state.getOnSignal()).flatMap(t -> t.getAction2370s().stream()),
                        toStream(state.getOnEnter()).flatMap(t -> t.getAction2370s().stream()))
                .flatMap(actionStream -> actionStream);
    }

    private <T> Stream<T> toStream(final Collection<T> obj) {
        return Optional.ofNullable(obj)
                .map(Stream::of)
                .orElseGet(Stream::empty)
                .flatMap(Collection::stream);
    }
}

interface Action2370 {
    public Collection<Action2370> getAction2370s();
}

interface State2370 {
    public Collection<Action2370> getOnExit();

    public Collection<Action2370> getOnSignal();

    public Collection<Action2370> getOnEnter();
}
