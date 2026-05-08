package com.jesus.iot01.navigation

sealed class AppScreens(val route: String) {
    object Login : AppScreens("login_screen")
    object Register : AppScreens("register_screen")
    object Credentials : AppScreens("credentials_screen")
    object Variables : AppScreens("variables_screen")

    object ScadaPrompt : AppScreens("scada_prompt_screen")

    object ConnectionCheck : AppScreens("connection_check_screen")

    object ScadaView : AppScreens("scada_view_screen")

    object ScadaList : AppScreens("scada_list_screen")

    object ScadaEditor : AppScreens("scada_editor_screen")



}