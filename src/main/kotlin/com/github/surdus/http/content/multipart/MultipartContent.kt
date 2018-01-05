package com.github.surdus.http.content.multipart

import com.github.surdus.http.content.Content
import java.io.InputStream

class MultipartContent(fill: MultipartContent.() -> Unit): Content {
    val parts = mutableMapOf<String, MultipartContentPart>()

    init {
        this.fill()
    }

    fun part(name: String, part: MultipartContentPart) {
        parts[name] = part
    }
}

interface MultipartContentPart

class StringContentPart(val data: String): MultipartContentPart

class StreamContentPart(val inputStream: InputStream,
                        val fileName: String? = null,
                        val size: Long?): MultipartContentPart
