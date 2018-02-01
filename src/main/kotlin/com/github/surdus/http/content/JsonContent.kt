package com.github.surdus.http.content

import com.google.gson.*
import com.google.gson.reflect.TypeToken.getParameterized
import java.io.InputStream
import java.io.Reader
import java.lang.IllegalStateException
import java.lang.reflect.Type
import java.nio.charset.Charset

val gson = GsonBuilder()
        .registerTypeHierarchyAdapter(Json::class.java, GsonConverter())
        .serializeNulls()
        .create()!!

class GsonConverter: JsonSerializer<Json>, JsonDeserializer<Json> {
    override fun serialize(json: Json, type: Type, context: JsonSerializationContext): JsonElement {
        return when (json) {
            is JsonObject -> context.serialize(json.data)
            is JsonNumber -> context.serialize(json.data)
            is JsonBoolean -> context.serialize(json.data)
            is JsonString -> context.serialize(json.data)
            is JsonArray -> context.serialize(json.data)
            is JsonNull -> com.google.gson.JsonNull.INSTANCE
            else -> throw IllegalArgumentException("Unsupported json type")
        }
    }

    override fun deserialize(jsonElement: JsonElement, type: Type, context: JsonDeserializationContext): Json {
        return when (jsonElement) {

            is com.google.gson.JsonObject -> JsonObject(context.deserialize(jsonElement,
                    getParameterized(Map::class.java, String::class.java, Json::class.java).type))

            is com.google.gson.JsonNull -> JsonNull()

            is com.google.gson.JsonArray -> JsonArray(context.deserialize(jsonElement,
                    getParameterized(MutableList::class.java, Json::class.java).type))

            is JsonPrimitive -> when {
                jsonElement.isBoolean -> JsonBoolean(jsonElement.asBoolean)
                jsonElement.isNumber -> JsonNumber(jsonElement.asNumber)
                jsonElement.isString -> JsonString(jsonElement.asString)
                else -> throw IllegalArgumentException("Unsupported gson primitive type")
            }

            else -> throw IllegalArgumentException("Unsupported gson type")
        }
    }
}

class JsonContent(fillJson: Json.() -> Unit): BaseContent {
    private val data = gson.toJson(JsonObject().apply(fillJson), Json::class.java).toByteArray(Charsets.UTF_8)

    override val contentType: String
        get() = "application/json"
    override val contentEncoding: String?
        get() = null
    override val contentLength: Long?
        get() = data.size.toLong()
    override val inputStream: InputStream
        get() = data.inputStream()
}

fun RawContent.toJson(): Json {
    return gson.fromJson<Json>(inputStream.bufferedReader(Charset.forName(contentEncoding)), Json::class.java)
}

fun Reader.toJson(): Json {
    return gson.fromJson(this, Json::class.java)
}

interface Json {
    operator fun get(key: String): Json {
        throw IllegalStateException("Can't get json value")
    }

    operator fun get(index: Int): Json {
        throw IllegalStateException("Can't get json value by index")
    }

    operator fun invoke(key: String): Json? {
        throw IllegalStateException("Can't get json value")
    }

    operator fun set(key: String, value: Int?) {
        throw IllegalStateException("Can't set json value")
    }

    operator fun set(key: String, value: String?) {
        throw IllegalStateException("Can't set json value")
    }

    operator fun set(key: String, value: Boolean?) {
        throw IllegalStateException("Can't set json value")
    }

    operator fun set(key: String, value: Json?) {
        throw IllegalStateException("Can't set json value")
    }

    operator fun set(key: String, fillJson: JsonObject.() -> Unit) {
        throw IllegalStateException("Can't set json value")
    }

    operator fun set(key: String, nothing: Nothing?) {
        throw IllegalStateException("Can't set json value")
    }

    operator fun set(key: String, values: List<Any>?) {
        throw IllegalStateException("Can't set json value")
    }

    val string: String?
        get() = throw IllegalStateException("Can't get string value")
    val int: Int?
        get() = throw IllegalStateException("Can't get int value")
    val bool: Boolean?
        get() = throw IllegalStateException("Can't get string value")
    val long: Long?
        get() = throw IllegalStateException("Can't get long value")
    val size: Int
        get() = throw IllegalStateException("Can't get size value")
    val array: List<Json>?
        get() = throw IllegalStateException("Can't get array value")
}

class JsonObject(val data: MutableMap<String, Json> = mutableMapOf()) : Json {

    override fun get(key: String): Json {
        return data[key] ?: if (data.containsKey(key))
            JsonNull()
        else
            throw IllegalStateException("No such key '$key' in json object")
    }

    override fun invoke(key: String): Json? {
        return data[key]
    }

    override fun set(key: String, value: Int?) {
        data[key] = JsonNumber(value)
    }

    override fun set(key: String, value: String?) {
        data[key] = JsonString(value)
    }

    override fun set(key: String, value: Boolean?) {
        data[key] = JsonBoolean(value)
    }

    override fun set(key: String, value: Json?) {
        data[key] = value ?: JsonNull()
    }

    override fun set(key: String, nothing: Nothing?) {
        data[key] = JsonNull()
    }

    override fun set(key: String, fillJson: JsonObject.() -> Unit) {
        data[key] = JsonObject().apply(fillJson)
    }

    override fun set(key: String, values: List<Any>?) {
        val jsonList = values?.map {
            when (it) {
                is String -> JsonString(it)
                is Number -> JsonNumber(it)
                is Boolean -> JsonBoolean(it)
                is Nothing -> JsonNull()
                is Json -> it
                else -> throw IllegalStateException("Cant get json value from ${it.javaClass}")
            }
        }?.toMutableList()
        data[key] = jsonList?.let { JsonArray(it) } ?: JsonNull()
    }
}

class JsonNumber(var data: Number? = null) : Json {
    override val int: Int?
        get() = data?.toInt()
    override val long: Long?
        get() = data?.toLong()
}

class JsonString(var data: String? = null) : Json {
    override val string: String?
        get() = data
}

class JsonBoolean(var data: Boolean? = null) : Json {
    override val bool: Boolean?
        get() = data
}

class JsonNull : Json {
    override val string: String?
        get() = null
    override val int: Int?
        get() = null
    override val bool: Boolean?
        get() = null
    override val long: Long?
        get() = null
}

class JsonArray(val data: MutableList<Json>): Json {

    override fun get(index: Int): Json {
        return data[index]
    }

    override val size: Int
        get() = data.size

    override val array: List<Json>
        get() = data
}
