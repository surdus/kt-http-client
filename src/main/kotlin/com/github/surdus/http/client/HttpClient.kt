package com.github.surdus.http.client

import com.github.surdus.http.content.BaseContent
import com.github.surdus.http.content.Content
import com.github.surdus.http.content.RawContent
import com.github.surdus.http.content.multipart.MultipartContent
import com.github.surdus.http.content.multipart.MultipartContentPart
import com.github.surdus.http.content.multipart.StreamContentPart
import com.github.surdus.http.content.multipart.StringContentPart
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse as ApacheHttpResponse
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.ContentType
import org.apache.http.entity.InputStreamEntity
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.ContentBody
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.impl.nio.client.HttpAsyncClients
import java.io.Closeable
import java.net.URI
import java.util.concurrent.Future

class HttpClient : Closeable {

    private val innerClient = HttpAsyncClients.createMinimal().apply { start() }

    fun exec(method: String, uri: String, buildRequest: HttpRequest.() -> Unit): HttpResponse {
        return execAsync(method, uri, buildRequest).get()
    }

    fun execAsync(method: String, uri: String, buildRequest: HttpRequest.() -> Unit): Future<HttpResponse> {
        val request = HttpRequest().apply(buildRequest)

        val uriBuilder = URIBuilder(uri)
        request.queryParams.forEach { name, value -> uriBuilder.setParameter(name, value) }

        val apacheHttpRequest = ApacheHttpRequest(method, uriBuilder.build())
        request.headers.forEach { name, value -> apacheHttpRequest.addHeader(name, value) }

        val requestContent = request.content
        if (requestContent != null)
            apacheHttpRequest.entity = toHttpEntity(requestContent)

        val res = innerClient.execute(apacheHttpRequest, null)

        return res.map { it.toHttpResponse() }
    }

    override fun close() {
        innerClient.close()
    }
}

class HttpRequest {
    var content: Content? = null
    val headers = mutableMapOf<String, String>()
    val queryParams = mutableMapOf<String, String>()

    fun queryParam(name: String, value: String) {
        queryParams[name] = value
    }

    fun header(name: String, value: String) {
        headers[name] = value
    }
}

data class HttpResponse(
        val statusCode: Int,
        val content: RawContent?)

class ApacheHttpRequest(private val method: String, uri: URI) : HttpEntityEnclosingRequestBase() {

    init {
        setURI(uri)
    }

    override fun getMethod(): String {
        return method
    }
}

fun toHttpEntity(content: Content): HttpEntity {
    return when (content) {
        is MultipartContent -> toHttpEntity(content)
        is BaseContent -> toHttpEntity(content)
        else -> throw IllegalArgumentException("Unsupported content type")
    }
}

fun toHttpEntity(content: BaseContent): HttpEntity {
    return InputStreamEntity(content.inputStream, content.contentLength ?: -1).also {
        it.setContentType(content.contentType)
        it.setContentEncoding(content.contentEncoding)
    }
}

fun toHttpEntity(content: MultipartContent): HttpEntity {
    return MultipartEntityBuilder.create().also {
        content.parts.forEach { name, part ->
            it.addPart(name, toContentBody(part))
        }
    }.build()
}

fun toContentBody(part: MultipartContentPart): ContentBody {
    return when (part) {

        is StringContentPart -> StringBody(part.data, ContentType.TEXT_PLAIN)

        is StreamContentPart -> object : InputStreamBody(part.inputStream, ContentType.DEFAULT_BINARY, part.fileName) {
            override fun getContentLength(): Long {
                return part.size ?: -1
            }
        }

        else -> throw IllegalArgumentException("Unsupported MultipartContentPart type")
    }
}

fun ApacheHttpResponse.toHttpResponse(): HttpResponse {
    val contentType = if (entity.contentType != null) ContentType.parse(entity.contentType.value) else null
    return HttpResponse(
            statusCode = statusLine.statusCode,
            content = RawContent(
                    inputStream = entity.content,
                    contentType = contentType?.mimeType,
                    contentEncoding = contentType?.charset?.name(),
                    contentLength = entity.contentLength))
}
