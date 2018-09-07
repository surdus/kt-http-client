package com.github.surdus.http.content.multipart

import com.github.surdus.http.client.toContentBody
import com.github.surdus.http.content.Content
import org.apache.http.HttpEntity
import org.apache.http.entity.mime.MultipartEntityBuilder
import java.io.InputStream

class MultipartContent(fill: MultipartContent.() -> Unit): Content {
    val parts = mutableMapOf<String, MultipartContentPart>()

    init {
        this.fill()
    }

    fun part(name: String, part: MultipartContentPart) {
        parts[name] = part
    }

    override fun toHttpEntity(): HttpEntity {
        return MultipartEntityBuilder.create().also {
            parts.forEach { name, part ->
                it.addPart(name, toContentBody(part))
            }
        }.build()
    }
}

interface MultipartContentPart

class StringContentPart(val data: String): MultipartContentPart

class StreamContentPart(val inputStream: InputStream,
                        val fileName: String? = null,
                        val size: Long?): MultipartContentPart
