package com.cy.ktmain.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Compose 版轮播指示器控件，支持小圆点随滑动进度实时拉伸与颜色渐变。
 */
@Composable
fun CarouselIndicator(
    count: Int,
    currentIndex: Int,
    nextIndex: Int,
    progress: Float,
    modifier: Modifier = Modifier,
    normalWidthDp: Dp = 8.dp,
    activeWidthDp: Dp = 22.dp,
    dotHeightDp: Dp = 8.dp,
    dotMarginDp: Dp = 4.dp,
    normalColor: Color = Color(0xFFB0BEC5),
    activeColor: Color = Color(0xFF087F6A),
) {
    if (count <= 1) return

    val density = LocalDensity.current
    val nWidth = with(density) { normalWidthDp.toPx() }
    val aWidth = with(density) { activeWidthDp.toPx() }
    val h = with(density) { dotHeightDp.toPx() }
    val margin = with(density) { dotMarginDp.toPx() }

    val totalContentWidthPx = aWidth + (count - 1) * nWidth + count * (margin * 2)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .width(with(density) { totalContentWidthPx.toDp() })
                .height(dotHeightDp),
        ) {
            val radius = h / 2f
            var startX = (size.width - totalContentWidthPx) / 2f

            for (i in 0 until count) {
                val dotWidth: Float
                val dotColor: Color

                val p = progress.coerceIn(0f, 1f)

                when (i) {
                    currentIndex -> {
                        dotWidth = aWidth - (aWidth - nWidth) * p
                        dotColor = lerp(activeColor, normalColor, p)
                    }

                    nextIndex -> {
                        dotWidth = nWidth + (aWidth - nWidth) * p
                        dotColor = lerp(normalColor, activeColor, p)
                    }

                    else -> {
                        dotWidth = nWidth
                        dotColor = normalColor
                    }
                }

                drawRoundRect(
                    color = dotColor,
                    topLeft = Offset(startX + margin, (size.height - h) / 2f),
                    size = Size(dotWidth, h),
                    cornerRadius = CornerRadius(radius, radius)
                )

                startX += dotWidth + margin * 2
            }
        }
    }
}
