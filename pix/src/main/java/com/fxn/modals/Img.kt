package com.fxn.modals

import java.io.Serializable

/**
 * Created by akshay on 17/03/18.
 */
class Img(
    var headerDate: String, var contentUrl: String, var url: String, var scrollerDate: String,
    type: Int
) : Serializable {
    var selected = false
    var mediaType = 1
    var position = 0

    init {
        mediaType = type
    }
}