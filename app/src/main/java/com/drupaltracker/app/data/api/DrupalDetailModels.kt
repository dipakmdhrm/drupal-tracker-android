package com.drupaltracker.app.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

// GET /api-d7/node/{nid}.json
@JsonClass(generateAdapter = true)
data class NodeDetailResponse(
    @Json(name = "nid") val nid: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "body") val body: BodyValue? = null
)

// No @JsonClass here — we supply a custom adapter registered in RetrofitClient
data class BodyValue(val value: String? = null)

// Handles both {"value":"..."} and [] (Drupal returns [] for empty fields)
class BodyValueAdapter : JsonAdapter<BodyValue?>() {
    override fun fromJson(reader: JsonReader): BodyValue? = when (reader.peek()) {
        JsonReader.Token.BEGIN_OBJECT -> {
            reader.beginObject()
            var value: String? = null
            while (reader.hasNext()) {
                if (reader.nextName() == "value") {
                    value = if (reader.peek() == JsonReader.Token.NULL) {
                        reader.nextNull()
                    } else {
                        reader.nextString()
                    }
                } else {
                    reader.skipValue()
                }
            }
            reader.endObject()
            BodyValue(value)
        }
        JsonReader.Token.BEGIN_ARRAY -> {
            reader.beginArray()
            while (reader.hasNext()) reader.skipValue()
            reader.endArray()
            null
        }
        JsonReader.Token.NULL -> { reader.nextNull<Any>(); null }
        else -> { reader.skipValue(); null }
    }

    override fun toJson(writer: JsonWriter, value: BodyValue?) {
        if (value == null) { writer.nullValue(); return }
        writer.beginObject().name("value").value(value.value).endObject()
    }
}

// GET /api-d7/comment.json?node={nid}
@JsonClass(generateAdapter = true)
data class CommentListResponse(
    @Json(name = "list") val list: List<CommentApiModel> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CommentApiModel(
    @Json(name = "cid") val cid: String = "",
    @Json(name = "subject") val subject: String = "",
    @Json(name = "comment_body") val commentBody: BodyValue? = null
)
