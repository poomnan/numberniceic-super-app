package com.numberniceic.data.product

import com.google.gson.annotations.SerializedName

data class ProductCategory(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class Product(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("price") val price: Double,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("stock_quantity") val stockQuantity: Int = 0,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("shipping_status") val shippingStatus: String? = "none"
)
