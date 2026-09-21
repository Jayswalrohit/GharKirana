package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.Product

data class MarketImagePreset(
    val key: String,
    val label: String,
    val nepaliLabel: String,
    val drawableResId: Int,
    val suggestedCategory: String,
    val typicalPriceNpr: Double,
    val typicalUnit: String,
    val typicalBrand: String
)

object MarketPresets {
    val presets = listOf(
        MarketImagePreset(
            key = "img_product_rice",
            label = "Basmati Rice 5kg",
            nepaliLabel = "बासमती चामल",
            drawableResId = R.drawable.img_product_rice_1789873109171,
            suggestedCategory = "cat_rice_dal",
            typicalPriceNpr = 690.0,
            typicalUnit = "5 KG",
            typicalBrand = "Hulas"
        ),
        MarketImagePreset(
            key = "img_product_oil",
            label = "Mustard Oil 1L",
            nepaliLabel = "धारा तोरीको तेल",
            drawableResId = R.drawable.img_product_oil_1789873122422,
            suggestedCategory = "cat_oil_ghee",
            typicalPriceNpr = 285.0,
            typicalUnit = "1 Litre",
            typicalBrand = "Dhara"
        ),
        MarketImagePreset(
            key = "img_product_noodles",
            label = "Wai Wai Noodles",
            nepaliLabel = "वाइ वाइ चाउचाउ",
            drawableResId = R.drawable.img_product_noodles_1789873137821,
            suggestedCategory = "cat_instant",
            typicalPriceNpr = 25.0,
            typicalUnit = "75g Packet",
            typicalBrand = "Wai Wai"
        ),
        MarketImagePreset(
            key = "img_product_tea",
            label = "Tokla CTC Tea 500g",
            nepaliLabel = "टोक्ला चियापत्ती",
            drawableResId = R.drawable.img_product_tea_1789873156189,
            suggestedCategory = "cat_drinks",
            typicalPriceNpr = 295.0,
            typicalUnit = "500g Box",
            typicalBrand = "Tokla"
        ),
        MarketImagePreset(
            key = "img_product_lentils",
            label = "Red Musuro Dal 1kg",
            nepaliLabel = "रातो मुसुरो दाल",
            drawableResId = R.drawable.img_product_lentils_1789873169466,
            suggestedCategory = "cat_rice_dal",
            typicalPriceNpr = 160.0,
            typicalUnit = "1 KG",
            typicalBrand = "Janakpur Kirana"
        )
    )

    fun getDrawableForPresetKey(key: String?): Int? {
        return presets.find { it.key == key }?.drawableResId
    }
}

/**
 * Renders a product image using:
 * 1. An image URL or local photo picker content URI (Coil AsyncImage)
 * 2. Pre-packaged high-res market product photo (via drawable resource)
 * 3. Fallback to product emoji with clean background
 */
@Composable
fun ProductImageView(
    product: Product,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    emojiSize: TextUnit = 32.sp
) {
    val context = LocalContext.current
    val presetResId = MarketPresets.getDrawableForPresetKey(product.imageResName)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            // Case 1: External URL or local content URI from photo picker
            !product.imageUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(product.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Case 2: Pre-packaged photorealistic market product image
            presetResId != null -> {
                Image(
                    painter = painterResource(id = presetResId),
                    contentDescription = product.name,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Case 3: Fallback Emoji
            else -> {
                Text(
                    text = product.emoji,
                    fontSize = emojiSize
                )
            }
        }
    }
}
