package com.drupaltracker.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JsonApiProjectListResponse(
    @Json(name = "data") val data: List<JsonApiProjectNode> = emptyList(),
    @Json(name = "links") val links: JsonApiPaginationLinks? = null
)

@JsonClass(generateAdapter = true)
data class JsonApiProjectNode(
    @Json(name = "id") val id: String = "",
    @Json(name = "attributes") val attributes: JsonApiProjectAttributes = JsonApiProjectAttributes()
)

@JsonClass(generateAdapter = true)
data class JsonApiProjectAttributes(
    @Json(name = "drupal_internal__nid") val nid: Int = 0,
    @Json(name = "title") val title: String = "",
    @Json(name = "field_project_machine_name") val machineName: String? = null,
    @Json(name = "changed") val changed: String? = null
)

@JsonClass(generateAdapter = true)
data class JsonApiPaginationLinks(
    @Json(name = "next") val next: JsonApiLink? = null
)

@JsonClass(generateAdapter = true)
data class JsonApiLink(
    @Json(name = "href") val href: String = ""
)

fun JsonApiProjectNode.toProjectNodeApiModel() = ProjectNodeApiModel(
    nid = attributes.nid.toString(),
    title = attributes.title,
    machineName = attributes.machineName
)
