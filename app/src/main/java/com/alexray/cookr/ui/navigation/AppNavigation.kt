package com.alexray.cookr.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alexray.cookr.data.Supabase
import com.alexray.cookr.data.repository.AuthRepository
import com.alexray.cookr.ui.auth.AuthScreen
import com.alexray.cookr.ui.recipes.RecipeListScreen
import io.github.jan.supabase.auth.status.SessionStatus

private object Routes {
    const val RECIPES = "recipes"
    const val IMPORT = "import"
    const val DETAIL = "recipe/{id}"
    fun detail(id: String) = "recipe/$id"
}

@Composable
fun AppNavigation() {
    if (!Supabase.configured) {
        SetupNeeded()
        return
    }

    val repo = remember { AuthRepository() }
    val status by repo.sessionStatus.collectAsState()

    when (status) {
        is SessionStatus.Authenticated -> SignedInNav()
        is SessionStatus.Initializing -> FullScreenLoader()
        else -> AuthScreen()
    }
}

@Composable
private fun SignedInNav() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.RECIPES) {
        composable(Routes.RECIPES) {
            RecipeListScreen(
                onAddByUrl = { nav.navigate(Routes.IMPORT) },
                onOpenRecipe = { id -> nav.navigate(Routes.detail(id)) },
            )
        }
        composable(Routes.IMPORT) {
            Placeholder(
                title = "Import from URL",
                body = "Next up: the Supabase edge function that parses a recipe URL.",
            )
        }
        composable(Routes.DETAIL) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            Placeholder(
                title = "Recipe detail",
                body = "Will render recipe $id once the detail screen is built.",
            )
        }
    }
}

@Composable
private fun FullScreenLoader() {
    Box(Modifier.fillMaxSize()) {
        CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}

@Composable
private fun Placeholder(title: String, body: String) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SetupNeeded() {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Supabase not configured", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties, " +
                    "then rebuild.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
