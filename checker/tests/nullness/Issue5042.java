import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

public class Issue5042 {
    interface PromptViewModel {
        boolean isPending();

        @Nullable PromptButtonViewModel getConfirmationButton();
    }

    interface PromptButtonViewModel {
        @Nullable ConfirmationPopupViewModel getConfirmationPopup();
    }

    interface ConfirmationPopupViewModel {
        boolean isShowingConfirmation();
    }

    boolean f(PromptViewModel viewModel) {
        PromptButtonViewModel prompt = viewModel.getConfirmationButton();
        ConfirmationPopupViewModel popup = prompt != null ? prompt.getConfirmationPopup() : null;
        return viewModel.isPending() || (popup != null && popup.isShowingConfirmation());
    }

    static final Function<PromptViewModel, Boolean> IS_PENDING_OR_SHOWING_CONFIRMATION =
            (viewModel) -> {
                @Nullable PromptButtonViewModel promptLambda = viewModel.getConfirmationButton();
                @Nullable ConfirmationPopupViewModel popup =
                        promptLambda != null ? promptLambda.getConfirmationPopup() : null;
                return viewModel.isPending() || (popup != null && popup.isShowingConfirmation());
            };

    final Function<PromptViewModel, Boolean> IS_PENDING_OR_SHOWING_CONFIRMATION2 =
            (viewModel) -> {
                @Nullable PromptButtonViewModel prompt = viewModel.getConfirmationButton();
                @Nullable ConfirmationPopupViewModel popup =
                        prompt == null ? null : prompt.getConfirmationPopup();
                return viewModel.isPending() || (popup != null && popup.isShowingConfirmation());
            };

    @Nullable PromptButtonViewModel promptfield;
    Producer o =
            () -> {
                @Nullable ConfirmationPopupViewModel popup =
                        promptfield == null ? null : promptfield.getConfirmationPopup();
                return (popup != null && popup.isShowingConfirmation());
            };

    static @Nullable PromptButtonViewModel promptfield2;

    static Producer o2 =
            () -> {
                @Nullable ConfirmationPopupViewModel popup =
                        promptfield2 == null ? null : promptfield2.getConfirmationPopup();
                return (popup != null && popup.isShowingConfirmation());
            };

    interface Producer {
        Object apply();
    }

    Issue5042(int i, int i2) {}

    Issue5042(int i) {}

    Issue5042() {}
}
