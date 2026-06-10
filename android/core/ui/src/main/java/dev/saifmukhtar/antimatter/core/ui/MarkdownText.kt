package dev.saifmukhtar.antimatter.core.ui

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j
import androidx.compose.foundation.isSystemInDarkTheme
import dev.saifmukhtar.antimatter.core.ui.utils.GrammarLocatorDef

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Int = Color.WHITE,
    onLinkClicked: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val markwon = remember(context, isDarkTheme) {
        val prism4j = Prism4j(GrammarLocatorDef())
        val theme = if (isDarkTheme) Prism4jThemeDarkula.create() else Prism4jThemeDefault.create()
        Markwon.builder(context)
            .usePlugin(SyntaxHighlightPlugin.create(prism4j, theme))
            .usePlugin(TablePlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(object : io.noties.markwon.AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: io.noties.markwon.MarkwonConfiguration.Builder) {
                    builder.linkResolver(object : io.noties.markwon.LinkResolverDef() {
                        override fun resolve(view: android.view.View, link: String) {
                            if (link.startsWith("file://") && onLinkClicked != null) {
                                onLinkClicked(link)
                            } else {
                                super.resolve(view, link)
                            }
                        }
                    })
                }
            })
            .build()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setTextColor(textColor)
                textSize = 16f
                setLineSpacing(0f, 1.2f)
                movementMethod = android.text.method.LinkMovementMethod.getInstance()
                setTextIsSelectable(true)
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, markdown)
        }
    )
}
