package com.example.bookmarks.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private const val ROOT_ROUTE = "root"
private const val FOLDER_ROUTE = "folder/{id}"

@Composable
fun BookmarksApp(sharedLink: SharedLink?, onSharedLinkHandled: (saved: Boolean) -> Unit) {
    val navController = rememberNavController()
    val openFolder: (Long) -> Unit = { navController.navigate("folder/$it") }

    NavHost(navController = navController, startDestination = ROOT_ROUTE) {
        composable(ROOT_ROUTE) {
            FolderScreen(
                folderId = null,
                onOpenFolder = openFolder,
                onBack = null,
                sharedLink = sharedLink,
                onSharedLinkHandled = onSharedLinkHandled,
            )
        }
        composable(FOLDER_ROUTE, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            FolderScreen(
                folderId = entry.arguments?.getLong("id"),
                onOpenFolder = openFolder,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
