package com.jesus.iot01.data

//  Sesión del usuario actual — singleton en memoria
object UserSession {
    var userId: String = "user_test_001"  // valor por defecto temporal
    var email: String = ""
    var accessToken: String = ""
    var idToken: String = ""
    var refreshToken: String = ""
    var isLoggedIn: Boolean = false

    fun setUser(user: CognitoUser) {
        userId = user.userId
        email = user.email
        accessToken = user.accessToken
        idToken = user.idToken
        refreshToken = user.refreshToken
        isLoggedIn = true
    }

    fun clear() {
        userId = "user_test_001"
        email = ""
        accessToken = ""
        idToken = ""
        refreshToken = ""
        isLoggedIn = false
    }
}