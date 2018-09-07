package com.github.surdus.http.content

import org.apache.http.HttpEntity
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.message.BasicNameValuePair
import java.nio.charset.Charset

class FormDataContent(private val charset: Charset = Charsets.UTF_8,
                      fill: FormDataContent.() -> Unit): Content {
    private val params = mutableMapOf<String, String>()

    init {
        this.fill()
    }

    fun set(name: String, param: String) {
        params[name] = param
    }

    override fun toHttpEntity(): HttpEntity {
        return UrlEncodedFormEntity(params.map { (k, v) -> BasicNameValuePair(k, v) }, charset)
    }
}
