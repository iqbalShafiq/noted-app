package id.usecase.noted.feature.note.domain

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

sealed interface NoteContentBlock {
    data class Text(val value: String) : NoteContentBlock

    data class Image(val uri: String) : NoteContentBlock

    data class Location(
        val latitude: Double,
        val longitude: Double,
        val label: String,
    ) : NoteContentBlock
}

object NoteContentCodec {
    private const val prefix = "noted:block:v1;"

    fun encode(blocks: List<NoteContentBlock>): String {
        val payload = blocks.joinToString(separator = ";") { block ->
            when (block) {
                is NoteContentBlock.Text -> "t:${encodeToken(block.value)}"
                is NoteContentBlock.Image -> "i:${encodeToken(block.uri)}"
                is NoteContentBlock.Location -> {
                    val payload = "${block.latitude};${block.longitude};${block.label}"
                    "l:${encodeToken(payload)}"
                }
            }
        }
        return "$prefix$payload"
    }

    fun decode(content: String): List<NoteContentBlock> {
        if (!content.startsWith(prefix)) {
            return listOf(NoteContentBlock.Text(content))
        }

        val payload = content.removePrefix(prefix)
        if (payload.isEmpty()) {
            return listOf(NoteContentBlock.Text(""))
        }

        val blocks = payload.split(';').mapNotNull { token ->
            val separatorIndex = token.indexOf(':')
            if (separatorIndex <= 0) {
                return listOf(NoteContentBlock.Text(content))
            }

            val type = token.substring(0, separatorIndex)
            val encodedValue = token.substring(separatorIndex + 1)
            val decodedValue = decodeToken(encodedValue) ?: return listOf(NoteContentBlock.Text(content))

            when (type) {
                "t" -> NoteContentBlock.Text(decodedValue)
                "i" -> NoteContentBlock.Image(decodedValue)
                "l" -> parseLocationPayload(decodedValue) ?: return listOf(NoteContentBlock.Text(content))
                else -> return listOf(NoteContentBlock.Text(content))
            }
        }

        return if (blocks.isEmpty()) listOf(NoteContentBlock.Text("")) else blocks
    }

    fun toListPreview(content: String): String {
        val tokens = decode(content).mapNotNull { block ->
            when (block) {
                is NoteContentBlock.Text -> block.value.trim().ifEmpty { null }
                is NoteContentBlock.Image -> "[Foto]"
                is NoteContentBlock.Location -> "[Lokasi]"
            }
        }

        return tokens.joinToString(separator = " ").trim()
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun encodeToken(value: String): String {
        return Base64.UrlSafe.encode(value.toByteArray(Charsets.UTF_8))
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeToken(value: String): String? {
        return runCatching {
            String(Base64.UrlSafe.decode(value), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun parseLocationPayload(payload: String): NoteContentBlock.Location? {
        val parts = payload.split(';', limit = 3)
        if (parts.size != 3) {
            return null
        }

        val latitude = parts[0].toDoubleOrNull() ?: return null
        val longitude = parts[1].toDoubleOrNull() ?: return null
        val label = parts[2]
        return NoteContentBlock.Location(
            latitude = latitude,
            longitude = longitude,
            label = label,
        )
    }
}
