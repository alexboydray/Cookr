package com.alexray.cookr.data.repository

import com.alexray.cookr.data.Supabase
import com.alexray.cookr.data.model.Recipe
import com.alexray.cookr.data.model.RecipeDetail
import com.alexray.cookr.data.model.RecipeIngredient
import com.alexray.cookr.data.model.RecipeStep
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

/**
 * No owner_id filters anywhere in here on purpose: Row Level Security on the
 * Postgres side already restricts every row to the signed-in user. Filtering
 * again in the client would be duplicated policy that can drift.
 */
class RecipeRepository {

    private val db get() = Supabase.client

    suspend fun list(): List<Recipe> =
        db.from("recipes")
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<Recipe>()

    suspend fun detail(recipeId: String): RecipeDetail {
        val recipe = db.from("recipes")
            .select { filter { eq("id", recipeId) } }
            .decodeSingle<Recipe>()

        val ingredients = db.from("recipe_ingredients")
            .select {
                filter { eq("recipe_id", recipeId) }
                order("position", Order.ASCENDING)
            }
            .decodeList<RecipeIngredient>()

        val steps = db.from("recipe_steps")
            .select {
                filter { eq("recipe_id", recipeId) }
                order("position", Order.ASCENDING)
            }
            .decodeList<RecipeStep>()

        return RecipeDetail(recipe, ingredients, steps)
    }

    suspend fun insert(recipe: Recipe): Recipe =
        db.from("recipes")
            .insert(recipe) { select() }
            .decodeSingle<Recipe>()

    suspend fun delete(recipeId: String) {
        db.from("recipes").delete { filter { eq("id", recipeId) } }
    }
}
