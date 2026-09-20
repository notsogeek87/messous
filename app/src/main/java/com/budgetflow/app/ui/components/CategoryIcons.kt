package com.budgetflow.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Category
import androidx.compose.ui.graphics.vector.ImageVector

/** Maps the small string key stored on a [com.budgetflow.app.domain.model.Category] to a Material icon. */
object CategoryIcons {
    private val map: Map<String, ImageVector> = mapOf(
        "home" to Icons.Filled.Home,
        "receipt" to Icons.Filled.Receipt,
        "bolt" to Icons.Filled.Bolt,
        "cart" to Icons.Filled.ShoppingCart,
        "train" to Icons.Filled.Train,
        "fuel" to Icons.Filled.LocalGasStation,
        "health" to Icons.Filled.FavoriteBorder,
        "restaurant" to Icons.Filled.Restaurant,
        "celebration" to Icons.Filled.Celebration,
        "games" to Icons.Filled.SportsEsports,
        "movie" to Icons.Filled.Movie,
        "child" to Icons.Filled.ChildCare,
        "school" to Icons.Filled.School,
        "activity" to Icons.Filled.DirectionsCar,
        "savings" to Icons.Filled.Savings,
        "trending_up" to Icons.Filled.TrendingUp,
        "bank" to Icons.Filled.AccountBalance
    )

    val availableKeys: List<String> = map.keys.toList()

    fun of(key: String?): ImageVector = map[key] ?: Icons.Outlined.Category
}
