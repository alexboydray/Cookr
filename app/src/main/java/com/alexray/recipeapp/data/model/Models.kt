package com.alexray.recipeapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Row models. Property names match the Postgres columns via @SerialName so
 * the schema stays snake_case and the Kotlin stays camelCase.
 */

@Serializable
data class Profile(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class Recipe(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    val title: String,
    val description: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("source_name") val sourceName: String? = null,
    @SerialName("image_path") val imagePath: String? = null,
    val servings: Int? = null,
    @SerialName("prep_minutes") val prepMinutes: Int? = null,
    @SerialName("cook_minutes") val cookMinutes: Int? = null,
    @SerialName("total_minutes") val totalMinutes: Int? = null,
    val notes: String? = null,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
)

/**
 * Carries both the original line and a parsed split. The split is what makes
 * grocery aggregation possible; rawText is what gets displayed and the
 * fallback when a line won't parse.
 */
@Serializable
data class RecipeIngredient(
    val id: String? = null,
    @SerialName("recipe_id") val recipeId: String? = null,
    val position: Int,
    @SerialName("raw_text") val rawText: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val item: String? = null,
    val note: String? = null,
    val section: String? = null,
)

@Serializable
data class RecipeStep(
    val id: String? = null,
    @SerialName("recipe_id") val recipeId: String? = null,
    val position: Int,
    val instruction: String,
    val section: String? = null,
)

/** A recipe with its children, as the detail screen needs it. */
data class RecipeDetail(
    val recipe: Recipe,
    val ingredients: List<RecipeIngredient>,
    val steps: List<RecipeStep>,
)
