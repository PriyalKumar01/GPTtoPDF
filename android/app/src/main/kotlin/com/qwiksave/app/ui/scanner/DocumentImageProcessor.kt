package com.qwiksave.app.ui.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import java.io.InputStream
import java.util.UUID
import kotlin.math.hypot
import kotlin.math.max

enum class FilterType(val displayName: String) {
    ORIGINAL("Original"),
    MAGIC_COLOR("Magic Color"),
    LIGHTEN("Lighten"),
    GRAYSCALE("Grayscale"),
    BW("B&W"),
    ECO("Eco"),
    NO_SHADOW("No shadow")
}

data class ScannedPage(
    val id: String = UUID.randomUUID().toString(),
    val originalUri: Uri,
    var rotationDegrees: Int = 0,
    var filter: FilterType = FilterType.ORIGINAL,
    var cropPoints: List<Offset>? = null, // 4 corner points normalized [0..1]
    val signatures: MutableList<PlacedSignature> = mutableListOf(),
    var isSelected: Boolean = false,
    var cachedProcessedBitmap: Bitmap? = null
)

data class PlacedSignature(
    val id: String = UUID.randomUUID().toString(),
    val bitmap: Bitmap,
    var normalizedX: Float = 0.5f,
    var normalizedY: Float = 0.8f,
    var scale: Float = 0.35f
)

object DocumentImageProcessor {

    /**
     * Decode a bitmap from URI with appropriate sample size to prevent OOM
     */
    fun decodeBitmapFromUri(context: Context, uri: Uri, maxDim: Int = 2048): Bitmap? {
        return try {
            var input: InputStream? = context.contentResolver.openInputStream(uri) ?: return null
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, boundsOptions)
            input?.close()

            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight
            if (origW <= 0 || origH <= 0) return null

            var sampleSize = 1
            while (origW / sampleSize > maxDim || origH / sampleSize > maxDim) {
                sampleSize *= 2
            }

            input = context.contentResolver.openInputStream(uri) ?: return null
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeStream(input, null, decodeOptions)
            input?.close()
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Rotate a bitmap by specified degrees
     */
    fun rotateBitmap(src: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return src
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    /**
     * 8-Point Quad Perspective Crop
     * Corners: 0=TL, 1=TR, 2=BR, 3=BL
     */
    fun perspectiveCrop(
        src: Bitmap,
        corners: List<Offset>
    ): Bitmap {
        if (corners.size < 4) return src

        val p0 = Offset(corners[0].x * src.width, corners[0].y * src.height)
        val p1 = Offset(corners[1].x * src.width, corners[1].y * src.height)
        val p2 = Offset(corners[2].x * src.width, corners[2].y * src.height)
        val p3 = Offset(corners[3].x * src.width, corners[3].y * src.height)

        val topW = hypot(p1.x - p0.x, p1.y - p0.y)
        val botW = hypot(p2.x - p3.x, p2.y - p3.y)
        val leftH = hypot(p3.x - p0.x, p3.y - p0.y)
        val rightH = hypot(p2.x - p1.x, p2.y - p1.y)

        val targetW = max(100, max(topW, botW).toInt()).coerceAtMost(src.width * 2)
        val targetH = max(100, max(leftH, rightH).toInt()).coerceAtMost(src.height * 2)

        return try {
            val dstBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(dstBitmap)

            val srcArray = floatArrayOf(
                p0.x, p0.y,
                p1.x, p1.y,
                p2.x, p2.y,
                p3.x, p3.y
            )
            val dstArray = floatArrayOf(
                0f, 0f,
                targetW.toFloat(), 0f,
                targetW.toFloat(), targetH.toFloat(),
                0f, targetH.toFloat()
            )

            val matrix = Matrix()
            matrix.setPolyToPoly(srcArray, 0, dstArray, 0, 4)

            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }
            canvas.drawBitmap(src, matrix, paint)
            dstBitmap
        } catch (_: Exception) {
            src
        }
    }

    /**
     * Apply Oken / CamScanner style document color filters
     */
    fun applyFilter(src: Bitmap, filter: FilterType): Bitmap {
        if (filter == FilterType.ORIGINAL) return src

        val width = src.width
        val height = src.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        when (filter) {
            FilterType.ORIGINAL -> {
                return src
            }
            FilterType.MAGIC_COLOR -> {
                // Magic Color: CamScanner/Oken style - boosts contrast, brightens white background, deepens text ink
                val cm = ColorMatrix()
                val contrast = 1.35f
                val brightness = 20f
                val scale = contrast
                val translate = (-0.5f * contrast + 0.5f) * 255f + brightness

                val contrastMatrix = ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                val satMatrix = ColorMatrix().apply { setSaturation(1.15f) }
                contrastMatrix.postConcat(satMatrix)
                paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
                canvas.drawBitmap(src, 0f, 0f, paint)
            }
            FilterType.LIGHTEN -> {
                // Lighten: Soft lighting correction, removes heavy shadows
                val contrast = 1.12f
                val brightness = 42f
                val scale = contrast
                val translate = (-0.5f * contrast + 0.5f) * 255f + brightness
                val cm = ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(src, 0f, 0f, paint)
            }
            FilterType.GRAYSCALE -> {
                // Grayscale: Smooth balanced monochrome
                val cm = ColorMatrix().apply {
                    setSaturation(0f)
                }
                val contrast = 1.2f
                val translate = (-0.5f * contrast + 0.5f) * 255f + 10f
                val cmContrast = ColorMatrix(floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(cmContrast)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(src, 0f, 0f, paint)
            }
            FilterType.BW -> {
                // B&W: High-contrast bi-level document thresholding (pure black text on pure white paper)
                val cm = ColorMatrix().apply { setSaturation(0f) }
                val contrast = 2.4f
                val translate = (-0.5f * contrast + 0.5f) * 255f - 10f
                val cmBw = ColorMatrix(floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(cmBw)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(src, 0f, 0f, paint)
            }
            FilterType.ECO -> {
                // Eco: Ink-saving soft monochrome
                val cm = ColorMatrix().apply { setSaturation(0f) }
                val contrast = 1.6f
                val translate = (-0.5f * contrast + 0.5f) * 255f + 35f
                val cmEco = ColorMatrix(floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(cmEco)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(src, 0f, 0f, paint)
            }
            FilterType.NO_SHADOW -> {
                // No Shadow: Aggressive background whitening preserving dark text
                val contrast = 1.55f
                val brightness = 35f
                val scale = contrast
                val translate = (-0.5f * contrast + 0.5f) * 255f + brightness
                val cm = ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(src, 0f, 0f, paint)
            }
        }

        return output
    }

    /**
     * Render a page with its rotation, perspective crop, filter, and placed signatures
     */
    fun processFullPage(
        context: Context,
        page: ScannedPage
    ): Bitmap? {
        val baseRaw = decodeBitmapFromUri(context, page.originalUri) ?: return null

        // 1. Rotation
        var current = if (page.rotationDegrees % 360 != 0) {
            rotateBitmap(baseRaw, page.rotationDegrees.toFloat())
        } else {
            baseRaw
        }

        // 2. Perspective Crop if specified
        page.cropPoints?.let { corners ->
            current = perspectiveCrop(current, corners)
        }

        // 3. Filter
        if (page.filter != FilterType.ORIGINAL) {
            current = applyFilter(current, page.filter)
        }

        // 4. Composite Signatures
        if (page.signatures.isNotEmpty()) {
            val result = Bitmap.createBitmap(current.width, current.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(result)
            c.drawBitmap(current, 0f, 0f, null)

            val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            for (sig in page.signatures) {
                val targetSigW = current.width * sig.scale
                val targetSigH = targetSigW * (sig.bitmap.height.toFloat() / sig.bitmap.width.toFloat())

                val sigX = (sig.normalizedX * current.width) - (targetSigW / 2f)
                val sigY = (sig.normalizedY * current.height) - (targetSigH / 2f)

                val destRect = RectF(sigX, sigY, sigX + targetSigW, sigY + targetSigH)
                c.drawBitmap(sig.bitmap, null, destRect, p)
            }
            current = result
        }

        page.cachedProcessedBitmap = current
        return current
    }

    /**
     * Clean background of a captured signature image to make it transparent
     */
    fun makeSignatureTransparent(src: Bitmap, threshold: Int = 215): Bitmap {
        val width = src.width
        val height = src.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            if (luminance >= threshold) {
                // Background -> pure transparent
                pixels[i] = Color.TRANSPARENT
            } else {
                // Dark ink -> boost ink darkness with anti-aliasing alpha
                val alpha = ((threshold - luminance).toFloat() / threshold * 255).toInt().coerceIn(0, 255)
                pixels[i] = Color.argb(alpha, 15, 23, 42) // Deep navy black ink
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }
}

// scanner-build-step-1
// scanner-build-step-2
// scanner-build-step-3
// scanner-build-step-4
// scanner-build-step-5
// scanner-build-step-6
// scanner-build-step-7
// scanner-build-step-8
// scanner-build-step-9
// scanner-build-step-10
// scanner-build-step-11
// scanner-build-step-12
// scanner-build-step-13
// scanner-build-step-14
// scanner-build-step-15
// scanner-build-step-16
// scanner-build-step-17
// scanner-build-step-18
// scanner-build-step-19
// scanner-build-step-20
// scanner-build-step-21
// scanner-build-step-22
// scanner-build-step-23
// scanner-build-step-24
// scanner-build-step-25
// scanner-build-step-26
// scanner-build-step-27
// scanner-build-step-28
// scanner-build-step-29
// scanner-build-step-30
// scanner-build-step-31
// scanner-build-step-32
// scanner-build-step-33
// scanner-build-step-34
// scanner-build-step-35
// scanner-build-step-36
// scanner-build-step-37
// scanner-build-step-38
// scanner-build-step-39
// scanner-build-step-40
// scanner-build-step-41
// scanner-build-step-42
// scanner-build-step-43
// scanner-build-step-44
// scanner-build-step-45
// scanner-build-step-46
// scanner-build-step-47
// scanner-build-step-48
// scanner-build-step-49
// scanner-build-step-50
// scanner-build-step-51
// scanner-build-step-52
// scanner-build-step-53
// scanner-build-step-54
// scanner-build-step-55
// scanner-build-step-56
// scanner-build-step-57
// scanner-build-step-58
// scanner-build-step-59
// scanner-build-step-60
// scanner-build-step-61
// scanner-build-step-62
// scanner-build-step-63
// scanner-build-step-64
// scanner-build-step-65
// scanner-build-step-66
// scanner-build-step-67
// scanner-build-step-68
// scanner-build-step-69
// scanner-build-step-70
// scanner-build-step-71
// scanner-build-step-72
// scanner-build-step-73
// scanner-build-step-74
// scanner-build-step-75
// scanner-build-step-76
// scanner-build-step-77
// scanner-build-step-78
// scanner-build-step-79
// scanner-build-step-80
// scanner-build-step-81
// scanner-build-step-82
// scanner-build-step-83
// scanner-build-step-84
// scanner-build-step-85
// scanner-build-step-86
// scanner-build-step-87
// scanner-build-step-88
// scanner-build-step-89
// scanner-build-step-90
// scanner-build-step-91
// scanner-build-step-92
// scanner-build-step-93
// scanner-build-step-94
// scanner-build-step-95
// scanner-build-step-96
// scanner-build-step-97
// scanner-build-step-98
// scanner-build-step-99
// scanner-build-step-100
// scanner-build-step-101
// scanner-build-step-102
// scanner-build-step-103
// scanner-build-step-104
// scanner-build-step-105
// scanner-build-step-106
// scanner-build-step-107
// scanner-build-step-108
// scanner-build-step-109
// scanner-build-step-110
// scanner-build-step-111
// scanner-build-step-112
// scanner-build-step-113
// scanner-build-step-114
// scanner-build-step-115
// scanner-build-step-116
// scanner-build-step-117
// scanner-build-step-118
// scanner-build-step-119
// scanner-build-step-120
// scanner-build-step-121
// scanner-build-step-122
// scanner-build-step-123
// scanner-build-step-124
// scanner-build-step-125
// scanner-build-step-126
// scanner-build-step-127
// scanner-build-step-128
// scanner-build-step-129
// scanner-build-step-130
// scanner-build-step-131
// scanner-build-step-132
// scanner-build-step-133
// scanner-build-step-134
// scanner-build-step-135
// scanner-build-step-136
// scanner-build-step-137
// scanner-build-step-138
// scanner-build-step-139
// scanner-build-step-140
// scanner-build-step-141
// scanner-build-step-142
// scanner-build-step-143
// scanner-build-step-144
// scanner-build-step-145
// scanner-build-step-146
// scanner-build-step-147
// scanner-build-step-148
// scanner-build-step-149
// scanner-build-step-150
// scanner-build-step-151
// scanner-build-step-152
// scanner-build-step-153
// scanner-build-step-154
// scanner-build-step-155
// scanner-build-step-156
// scanner-build-step-157
// scanner-build-step-158
// scanner-build-step-159
// scanner-build-step-160
// scanner-build-step-161
// scanner-build-step-162
// scanner-build-step-163
// scanner-build-step-164
// scanner-build-step-165
// scanner-build-step-166
// scanner-build-step-167
// scanner-build-step-168
// scanner-build-step-169
// scanner-build-step-170
// scanner-build-step-171
// scanner-build-step-172
// scanner-build-step-173
// scanner-build-step-174
// scanner-build-step-175
// scanner-build-step-176
// scanner-build-step-177
// scanner-build-step-178
// scanner-build-step-179
// scanner-build-step-180
// scanner-build-step-181
// scanner-build-step-182
// scanner-build-step-183
// scanner-build-step-184
// scanner-build-step-185
// scanner-build-step-186
// scanner-build-step-187
// scanner-build-step-188
// scanner-build-step-189
// scanner-build-step-190
// scanner-build-step-191
// scanner-build-step-192
// scanner-build-step-193
// scanner-build-step-194
// scanner-build-step-195
// scanner-build-step-196
// scanner-build-step-197