package com.github.surdus.http.content

import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.InputStreamEntity
import java.io.InputStream

interface Content {
    fun toHttpEntity(): HttpEntity
}

interface BaseContent: Content {
    val contentType: String?
    val contentEncoding: String?
    val contentLength: Long?
    val inputStream: InputStream

    override fun toHttpEntity(): HttpEntity {
        return InputStreamEntity(inputStream, contentLength ?: -1, ContentType.create(contentType, contentEncoding))
    }
}

class RawContent(override val inputStream: InputStream,
                 override val contentType: String?,
                 override val contentEncoding: String?,
                 override val contentLength: Long): BaseContent
