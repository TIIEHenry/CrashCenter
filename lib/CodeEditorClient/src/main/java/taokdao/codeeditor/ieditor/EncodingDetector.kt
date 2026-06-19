package taokdao.codeeditor.ieditor

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import kotlin.text.Charsets.ISO_8859_1
import kotlin.text.Charsets.US_ASCII
import kotlin.text.Charsets.UTF_16
import kotlin.text.Charsets.UTF_32
import kotlin.text.Charsets.UTF_8

@Suppress("unused")
object EncodingDetector {

    fun detectEncoding(bytes: ByteArray): Collection<Charset> = charsets.filter { detectCharset(bytes = bytes, charset = it) }

    private fun detectCharset(bytes: ByteArray, charset: Charset) =
            try {
                charset.newDecoder().reset().decode(ByteBuffer.wrap(bytes))
                true
            } catch (e: CharacterCodingException) {
                false
            }

    fun stringWithDetectedEncoding(bytes: ByteArray): String =
            try {
                String(bytes, detectEncoding(bytes).first())
            } catch (e: NoSuchElementException) {
                throw CharacterCodingException()
            }

    private val charsets = arrayOf(
            UTF_8, ISO_8859_1, US_ASCII, UTF_16, UTF_32,
            Charset.forName("GBK"),
            Charset.forName("GB2312"),
            Charset.forName("GB18030")
    )
}