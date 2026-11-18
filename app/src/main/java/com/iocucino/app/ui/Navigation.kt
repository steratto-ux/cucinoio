package com.iocucino.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import com.iocucino.app.data.Meal
import com.iocucino.app.data.dayFull
import com.iocucino.app.ui.components.LinenBackground
import com.iocucino.app.ui.components.NoInsets
import com.iocucino.app.ui.theme.Pal
import com.iocucino.app.ui.screens.PantryScreen
import com.iocucino.app.ui.screens.PlannerScreen
import com.iocucino.app.ui.screens.RecipeDetailScreen
import com.iocucino.app.ui.screens.RecipePickerScreen
import com.iocucino.app.ui.screens.RecipesScreen
import com.iocucino.app.ui.screens.SettingsScreen
import com.iocucino.app.ui.screens.ShoppingScreen
import java.time.LocalDate

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("recipes", "Ricette", Icons.Outlined.Restaurant),
    Tab("planner", "Menù", Icons.Outlined.CalendarMonth),
    Tab("shopping", "Spesa", Icons.Outlined.ShoppingCart),
    Tab("pantry", "Dispensa", Icons.Outlined.Kitchen)
)

private fun NavController.goToTab(route: String) {
    navigate(route) {
        popUpTo("recipes") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: MainViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = tabs.any { it.route == route }

    LinenBackground(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = NoInsets,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = Pal.Cream, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.goToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Pal.OrangeDark,
                                indicatorColor = Pal.Orange,
                                unselectedIconColor = Pal.InkSoft,
                                unselectedTextColor = Pal.InkSoft
                            )
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = "recipes",
            modifier = Modifier.padding(pad)
        ) {
            composable("recipes") {
                RecipesScreen(
                    vm = vm,
                    onOpen = { id -> nav.navigate("recipe/$id") },
                    onSettings = { nav.navigate("settings") }
                )
            }
            composable("planner") {
                PlannerScreen(
                    vm = vm,
                    onOpenRecipe = { id -> nav.navigate("recipe/$id") },
                    onPick = { date, meal -> nav.navigate("pick/$date/${meal.name}") },
                    onGoShopping = { nav.goToTab("shopping") }
                )
            }
            composable("shopping") { ShoppingScreen(vm) }
            composable("pantry") {
                PantryScreen(
                    vm = vm,
                    onCookWithPantry = {
                        vm.pantryMode.value = true
                        nav.goToTab("recipes")
                    }
                )
            }
            composable(
                route = "recipe/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                RecipeDetailScreen(
                    vm = vm,
                    recipeId = entry.arguments?.getString("id") ?: "",
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                route = "pick/{date}/{meal}",
                arguments = listOf(
                    navArgument("date") { type = NavType.StringType },
                    navArgument("meal") { type = NavType.StringType }
                )
            ) { entry ->
                val date = LocalDate.parse(entry.arguments?.getString("date") ?: LocalDate.now().toString())
                val meal = Meal.from(entry.arguments?.getString("meal"))
                RecipePickerScreen(
                    vm = vm,
                    title = "${dayFull(date)} · ${meal.label}",
                    onPick = { id ->
                        vm.repo.addPlan(date, meal, id, vm.repo.settings.value.servings)
                        nav.popBackStack()
                    },
                    onBack = { nav.popBackStack() }
                )
            }
            composable("settings") { SettingsScreen(vm, onBack = { nav.popBackStack() }) }
        }
    }
    }
}
