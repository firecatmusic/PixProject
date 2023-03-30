package com.fxn.utility

/**
 * Created by sangcomz on 09/04/2017.
 */
class RegexUtil {
    fun checkGif(path: String): Boolean {
        return path.matches(Regex(GIF_PATTERN))
    }

    companion object {
        private const val GIF_PATTERN = "(.+?)\\.gif$"
    }
}