package com.github.gotify.messages;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import java.util.Objects;

public class MessagesModelFactory implements ViewModelProvider.Factory {

    Activity modelParameterActivity;

    public MessagesModelFactory(Activity activity) {
        modelParameterActivity = activity;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass == MessagesModel.class) {
            return Objects.requireNonNull(
                    modelClass.cast(new MessagesModel(modelParameterActivity)));
        }
        throw new IllegalArgumentException(
                String.format(
                        "modelClass parameter must be of type %s", MessagesModel.class.getName()));
    }
}
