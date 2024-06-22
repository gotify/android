package com.github.gotify

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.style.BackgroundColorSpan
import android.text.style.BulletSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.RenderProps
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.core.CoreProps
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TableAwareMovementMethod
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.movement.MovementMethodPlugin
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.BlockQuote
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.Heading
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.StrongEmphasis
import org.commonmark.parser.Parser
import org.tinylog.kotlin.Logger

internal object MarkwonFactory {
    fun createForMessage(context: Context, imageLoader: ImageLoader): Markwon {
        return Markwon.builder(context)
            .usePlugin(CorePlugin.create())
            .usePlugin(MovementMethodPlugin.create(TableAwareMovementMethod.create()))
            .usePlugin(
                CoilImagesPlugin.create(
                    object : CoilImagesPlugin.CoilStore {
                        override fun load(drawable: AsyncDrawable): ImageRequest {
                            return ImageRequest.Builder(context)
                                .data(drawable.destination)
                                .placeholder(R.drawable.ic_placeholder)
                                .listener(onError = { _, err ->
                                    Logger.error(err.throwable) {
                                        "Could not load markdown image: ${Utils.redactPassword(
                                            drawable.destination
                                        )}"
                                    }
                                })
                                .build()
                        }

                        override fun cancel(disposable: Disposable) {
                            disposable.dispose()
                        }
                    },
                    imageLoader
                )
            )
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder.linkColor(ContextCompat.getColor(context, R.color.hyperLink))
                        .isLinkUnderlined(true)
                }
            })
            .build()
    }

    fun createForNotification(context: Context, imageLoader: ImageLoader): Markwon {
        val headingSizes = floatArrayOf(2f, 1.5f, 1.17f, 1f, .83f, .67f)
        val bulletGapWidth = (8 * context.resources.displayMetrics.density + 0.5f).toInt()

        return Markwon.builder(context)
            .usePlugin(CorePlugin.create())
            .usePlugin(CoilImagesPlugin.create(context, imageLoader))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                    builder.setFactory(Heading::class.java) { _, props: RenderProps? ->
                        arrayOf<Any>(
                            RelativeSizeSpan(
                                headingSizes[CoreProps.HEADING_LEVEL.require(props!!) - 1]
                            ),
                            StyleSpan(Typeface.BOLD)
                        )
                    }
                        .setFactory(Emphasis::class.java) { _, _ -> StyleSpan(Typeface.ITALIC) }
                        .setFactory(StrongEmphasis::class.java) { _, _ -> StyleSpan(Typeface.BOLD) }
                        .setFactory(BlockQuote::class.java) { _, _ -> QuoteSpan() }
                        .setFactory(Code::class.java) { _, _ ->
                            arrayOf<Any>(
                                BackgroundColorSpan(Color.LTGRAY),
                                TypefaceSpan("monospace")
                            )
                        }
                        .setFactory(ListItem::class.java) { _, _ -> BulletSpan(bulletGapWidth) }
                        .setFactory(Link::class.java) { _, _ -> null }
                }

                override fun configureParser(builder: Parser.Builder) {
                    builder.extensions(setOf(TablesExtension.create()))
                }

                override fun configureVisitor(builder: MarkwonVisitor.Builder) {
                    builder.on(TableCell::class.java) { visitor: MarkwonVisitor, node: TableCell? ->
                        visitor.visitChildren(node!!)
                        visitor.builder().append(' ')
                    }
                }
            })
            .build()
    }
}
