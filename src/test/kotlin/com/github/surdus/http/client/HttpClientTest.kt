package com.github.surdus.http.client

import com.github.surdus.http.content.FormDataContent
import com.github.surdus.http.content.JsonContent
import com.github.surdus.http.content.multipart.MultipartContent
import com.github.surdus.http.content.multipart.StreamContentPart
import com.github.surdus.http.content.multipart.StringContentPart
import com.github.surdus.http.content.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class HttpClientTest {

    private val httpClient = HttpClient()

    @Test
    fun testGet() {

        val response = httpClient.exec("GET", "https://postman-echo.com/get") {
            queryParam("testArg1", "test1")
            queryParam("testArg2", "test2")
            header("SomeTestHeader", "SomeTestHeaderValue")
        }

        val json = response.content!!.toJson()

        assertEquals("test1", json["args"]["testArg1"].string)
        assertEquals("test2", json["args"]["testArg2"].string)
    }

    @Test
    fun testPostJson() {

        val response = httpClient.exec("POST", "https://postman-echo.com/post") {
            header("SomeTestHeader", "SomeTestHeaderValue")
            content = JsonContent {
                set("intKey", 12)
                set("stringKey", "string")
                set("objectKey") {
                    set("int", 11)
                    set("bool", true)
                    set("empty", null)
                    set("array", listOf("val1", "val2"))
                }
            }
        }

        val json = response.content!!.toJson()

        assertEquals(12, json["data"]["intKey"].int)
        assertEquals("string", json["data"]["stringKey"].string)
        assertEquals(11L, json["data"]["objectKey"]["int"].long)
        assertEquals(true, json["data"]["objectKey"]["bool"].bool)
        assertEquals(null, json["data"]["objectKey"]["empty"].string)
        assertEquals(2, json["data"]["objectKey"]["array"].array!!.size)
        assertEquals("val1", json["data"]["objectKey"]["array"][0].string)
        assertEquals("val2", json["data"]["objectKey"]["array"][1].string)
        assertEquals(null, json["data"]["objectKey"]("absent"))
        assertEquals(setOf("intKey", "stringKey", "objectKey"), json["data"].map!!.keys)
    }

    @Test
    fun testPostMultipart() {
        val response = httpClient.exec("POST", "https://postman-echo.com/post") {
            content = MultipartContent {
                part("part1", StringContentPart("test string"))
                part("part2", StreamContentPart("test".byteInputStream(), "testFile", 4))
            }
        }

        val json = response.content!!.toJson()

        assertEquals("test string", json["form"]["part1"].string)
        assertNotNull(json["files"]["testFile"])
    }

    @Test
    fun testPostFormData() {
        val response = httpClient.exec("POST", "https://postman-echo.com/post") {
            content = FormDataContent {
                set("key1", "value1")
                set("key2!@#", "value2!@#")
            }
        }

        val json = response.content!!.toJson()

        assertEquals("value1", json["form"]["key1"].string!!)
        assertEquals("value2!@#", json["form"]["key2!@#"].string!!)
    }

    @Test
    fun testGetStream() {
        val response = httpClient.exec("GET", "https://postman-echo.com/stream/5") {}

        val str = response.content!!.inputStream.bufferedReader(Charsets.UTF_8).readText().replace("}{", "},{")
        val json = "[$str]".byteInputStream().reader().toJson()

        assertEquals(5, json.size)
    }
}
