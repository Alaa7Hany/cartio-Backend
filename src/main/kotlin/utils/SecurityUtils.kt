package com.example.utils

import org.mindrot.jbcrypt.BCrypt

object SecurityUtils {

    fun hashPassword(password: String): String =
        BCrypt.hashpw(password, BCrypt.gensalt())

    fun checkPassword(password: String, hashed: String): Boolean =
        BCrypt.checkpw(password, hashed)
}
