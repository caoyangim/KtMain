package com.cy.ktmain.carousel

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.cy.ktmain.R
import com.cy.ktmain.utils.setupEdgeToEdgeInsets
import com.google.android.material.appbar.MaterialToolbar
import kotlin.random.Random

class FoldableVPActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_foldable_vp)

        val rootView = findViewById<View>(R.id.rootView)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setupEdgeToEdgeInsets(rootView, toolbar)

        toolbar.setNavigationOnClickListener { finish() }

        val viewPager = findViewById<ViewPager>(R.id.viewPager)
        val isExpanded = resources.configuration.screenWidthDp >= 600
        viewPager.adapter = VPAdapter(isExpanded)
    }

    private class VPAdapter(private val isExpanded: Boolean) : PagerAdapter() {
        private val items = (1..20).toList()

        override fun getCount(): Int = items.size

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val tv = TextView(container.context).apply {
                text = "Item ${items[position]}"
                textSize = 32f
                gravity = Gravity.CENTER
                
                val rnd = Random(position)
                val color = Color.argb(255, 200 + rnd.nextInt(55), 200 + rnd.nextInt(55), 200 + rnd.nextInt(55))
                setBackgroundColor(color)
            }
            container.addView(tv)
            return tv
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun getPageWidth(position: Int): Float {
            // Foldable expanded state shows 2 items (0.5f width each), folded shows 1 item (1.0f width)
            return if (isExpanded) 0.5f else 1.0f
        }
    }
}
