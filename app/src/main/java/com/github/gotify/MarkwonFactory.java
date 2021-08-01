package com.github.gotify;

import android.content.Context;

import com.squareup.picasso.Picasso;

import io.noties.markwon.Markwon;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.ext.tables.TableAwareMovementMethod;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.image.picasso.PicassoImagesPlugin;
import io.noties.markwon.movement.MovementMethodPlugin;

public class MarkwonFactory {
    public static Markwon create(Context context, Picasso picasso) {
        return Markwon.builder(context)
                .usePlugin(CorePlugin.create())
                .usePlugin(MovementMethodPlugin.create(TableAwareMovementMethod.create()))
                .usePlugin(PicassoImagesPlugin.create(picasso))
                .usePlugin(TablePlugin.create(context))
                .build();
    }
}
