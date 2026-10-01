package com.cy.ktmain.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 屏幕宽度尺寸类别，常量与谷歌 Jetpack WindowManager 的
 * androidx.window.core.layout.WindowWidthSizeClass 对齐：
 * COMPACT(<600dp) / MEDIUM(600-839dp) / EXPANDED(>=840dp)。
 * 若项目引入 androidx.window 依赖，可直接替换为官方枚举。
 */
enum class WindowWidthSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED;

    companion object {
        const val MEDIUM_MIN_WIDTH_DP = 600
        const val EXPANDED_MIN_WIDTH_DP = 840

        fun fromWidthDp(widthDp: Int): WindowWidthSizeClass = when {
            widthDp >= EXPANDED_MIN_WIDTH_DP -> EXPANDED
            widthDp >= MEDIUM_MIN_WIDTH_DP -> MEDIUM
            else -> COMPACT
        }
    }
}

/** 按宽度尺寸类别计算网格列数，各档列数可由调用方覆盖。 */
fun WindowWidthSizeClass.gridSpanCount(
    compact: Int = 1,
    medium: Int = 2,
    expanded: Int = 3,
): Int = when (this) {
    WindowWidthSizeClass.COMPACT -> compact
    WindowWidthSizeClass.MEDIUM -> medium
    WindowWidthSizeClass.EXPANDED -> expanded
}

/** 通用网格间距：列间距均匀分配，行间距固定，首行不加顶距。 */
class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return

        val column = position % spanCount
        val row = position / spanCount

        outRect.left = spacing * column / spanCount
        outRect.right = spacing * (spanCount - 1 - column) / spanCount
        outRect.top = if (row == 0) 0 else spacing
        outRect.bottom = 0
    }
}
