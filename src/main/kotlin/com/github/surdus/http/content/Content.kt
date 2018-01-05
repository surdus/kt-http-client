package com.github.surdus.http.content

import java.io.InputStream

interface Content

interface BaseContent: Content {
    val contentType: String?
    val contentEncoding: String?
    val contentLength: Long?
    val inputStream: InputStream
}

class RawContent(override val inputStream: InputStream,
                 override val contentType: String?,
                 override val contentEncoding: String?,
                 override val contentLength: Long): BaseContent
