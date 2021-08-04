package com.github.gotify;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import androidx.annotation.NonNull;
import com.squareup.picasso.Picasso;
import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonSpansFactory;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.core.CoreProps;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TableAwareMovementMethod;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.image.picasso.PicassoImagesPlugin;
import io.noties.markwon.movement.MovementMethodPlugin;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.Code;
import org.commonmark.node.Emphasis;
import org.commonmark.node.Heading;
import org.commonmark.node.Link;
import org.commonmark.node.ListItem;
import org.commonmark.node.StrongEmphasis;

public class MarkwonFactory {
    public static Markwon create(Context context, Picasso picasso) {
        return createBuilderBase(context, picasso).build();
    }

    public static Markwon createForNotification(Context context, Picasso picasso) {
        final float[] headingSizes = {
            2.F, 1.5F, 1.17F, 1.F, .83F, .67F,
        };

        final int bulletGapWidth =
                (int) (8 * context.getResources().getDisplayMetrics().density + 0.5F);

        return createBuilderBase(context, picasso)
                .usePlugin(
                        new AbstractMarkwonPlugin() {
                            @Override
                            public void configureSpansFactory(
                                    @NonNull MarkwonSpansFactory.Builder builder) {
                                builder.setFactory(
                                                Heading.class,
                                                (configuration, props) ->
                                                        new Object[] {
                                                            new RelativeSizeSpan(
                                                                    headingSizes[
                                                                            CoreProps.HEADING_LEVEL
                                                                                            .require(
                                                                                                    props)
                                                                                    - 1]),
                                                            new StyleSpan(Typeface.BOLD)
                                                        })
                                        .setFactory(
                                                Emphasis.class,
                                                (configuration, props) ->
                                                        new StyleSpan(Typeface.ITALIC))
                                        .setFactory(
                                                StrongEmphasis.class,
                                                (configuration, props) ->
                                                        new StyleSpan(Typeface.BOLD))
                                        .setFactory(
                                                BlockQuote.class,
                                                (configuration, props) -> new QuoteSpan())
                                        .setFactory(
                                                Code.class,
                                                (configuration, props) ->
                                                        new Object[] {
                                                            new BackgroundColorSpan(Color.LTGRAY),
                                                            new TypefaceSpan("monospace")
                                                        })
                                        .setFactory(
                                                ListItem.class,
                                                (configuration, props) ->
                                                        new BulletSpan(bulletGapWidth))
                                        .setFactory(Link.class, ((configuration, props) -> null));
                            }
                        })
                .build();
    }

    private static Markwon.Builder createBuilderBase(Context context, Picasso picasso) {
        return Markwon.builder(context)
                .usePlugin(CorePlugin.create())
                .usePlugin(MovementMethodPlugin.create(TableAwareMovementMethod.create()))
                .usePlugin(PicassoImagesPlugin.create(picasso))
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(context));
    }
}
