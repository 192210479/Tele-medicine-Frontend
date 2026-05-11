package com.simats.Tmapp

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

object AvatarUtils {

    private val COLORS = listOf(
        "#1976D2", "#388E3C", "#7B1FA2", "#F57C00",
        "#0288D1", "#D32F2F", "#0097A7", "#5D4037"
    )

    fun getInitialsBitmap(name: String, sizePx: Int = 128): Bitmap {
        val initials = name.trim()
            .split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "?" }

        val colorHex = COLORS[Math.abs(name.hashCode()) % COLORS.size]
        val color = Color.parseColor(colorHex)

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            textSize = sizePx * 0.38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val yOffset = (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(initials, sizePx / 2f, sizePx / 2f - yOffset, textPaint)

        return bitmap
    }

    /**
     * Load profile image with initials fallback.
     * imageUrl: full URL to /api/profile/image/...
     * name: display name for initials generation
     */
    fun loadAvatar(imageView: ImageView, imageUrl: String?, name: String) {
        val context = imageView.context
        val fallback = BitmapDrawable(context.resources, getInitialsBitmap(name, 128))

        // If no URL at all, directly show initials
        if (imageUrl.isNullOrBlank()) {
            imageView.setImageDrawable(fallback)
            return
        }

        Glide.with(context)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .skipMemoryCache(false)
            .dontAnimate()
            .transform(CircleCrop())
            .error(fallback)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    imageView.setImageDrawable(fallback)
                    return true
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }
            })
            .into(imageView)
    }
}
