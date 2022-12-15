package com.victorhvs.tfc.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.victorhvs.tfc.presentation.screens.auth.AuthViewModel
import com.victorhvs.tfc.presentation.screens.home.HomeScreen

@Composable
fun RootNavigationGraph(
    navController: NavHostController,
    vm: AuthViewModel = hiltViewModel()
) {

    NavHost(
        navController = navController,
        route = Graph.ROOT,
        startDestination = Graph.AUTHENTICATION
    ) {
        authNavGraph(navController = navController)
        composable(route = Graph.HOME) {
            HomeScreen()
        }
    }

    if(vm.isUserAuthenticated) {
        navController.popBackStack()
        navController.navigate(Graph.HOME)
    }
}

object Graph {
    const val ROOT = "root_graph"
    const val AUTHENTICATION = "auth_graph"
    const val HOME = "home_graph"
    const val DETAILS = "details_graph"
}