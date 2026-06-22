package nota.android.crash.xp.migration

import android.content.Context
import android.content.SharedPreferences
import android.util.Xml
import kotlinx.coroutines.runBlocking
import nota.android.crash.root.RootAccessClient
import nota.android.crash.xp.PrefManager
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

/**
 * Reads the legacy `tiiehenry.xp.grapcrash` / `grapcrash.xml` prefs snapshot
 * via package context (if installed) or root file access through [RootAccessClient].
 */
class LegacyPrefSnapshotReader(private val rootAccessClient: RootAccessClient) {

    companion object {
        private const val LEGACY_PACKAGE = "tiiehenry.xp.grapcrash"
        private const val LEGACY_PREF_FILE = "grapcrash"

        internal val BOOL_KEYS = listOf(
            PrefManager.PREF_SCOPE_MODE,
            PrefManager.PREF_HANDLE_SYSTEM,
            PrefManager.PREF_SHOW_SYSTEM_UI,
        )

        internal fun snapshotFromPrefs(prefs: SharedPreferences): Snapshot {
            val booleans = mutableMapOf<String, Boolean>()
            val stringSets = mutableMapOf<String, Set<String>>()

            for (key in BOOL_KEYS) {
                if (prefs.contains(key)) {
                    booleans[key] = prefs.getBoolean(key, false)
                }
            }
            if (prefs.contains(PrefManager.PREF_PACKAGE_LIST)) {
                val set = prefs.getStringSet(PrefManager.PREF_PACKAGE_LIST, emptySet()) ?: emptySet()
                stringSets[PrefManager.PREF_PACKAGE_LIST] = set
            }

            return Snapshot(booleans, stringSets)
        }

        internal fun parsePrefsXml(xml: String): Snapshot? {
            return try {
                val parser = Xml.newPullParser()
                parser.setInput(StringReader(xml))
                val booleans = mutableMapOf<String, Boolean>()
                val stringSets = mutableMapOf<String, Set<String>>()

                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event != XmlPullParser.START_TAG) {
                        event = parser.next()
                        continue
                    }

                    when (parser.name) {
                        "boolean" -> {
                            val name = parser.getAttributeValue(null, "name")
                            val value = parser.getAttributeValue(null, "value")
                            if (name != null && value != null && BOOL_KEYS.contains(name)) {
                                booleans[name] = value == "true"
                            }
                        }
                        "set" -> {
                            val name = parser.getAttributeValue(null, "name")
                            if (name == PrefManager.PREF_PACKAGE_LIST) {
                                stringSets[name] = readStringSetChildren(parser)
                            }
                        }
                    }
                    event = parser.next()
                }

                Snapshot(booleans, stringSets)
            } catch (_: Exception) {
                null
            }
        }

        private fun readStringSetChildren(parser: XmlPullParser): Set<String> {
            val values = HashSet<String>()
            var event = parser.next()
            while (event != XmlPullParser.END_TAG || parser.name != "set") {
                if (event == XmlPullParser.START_TAG && parser.name == "string") {
                    values.add(parser.nextText())
                }
                event = parser.next()
            }
            return values
        }
    }

    data class Snapshot(
        val booleans: Map<String, Boolean> = emptyMap(),
        val stringSets: Map<String, Set<String>> = emptyMap(),
    ) {
        fun hasData(): Boolean = booleans.isNotEmpty() || stringSets.isNotEmpty()
    }

    fun read(context: Context): Snapshot? {
        readViaPackageContext(context)?.takeIf { it.hasData() }?.let { return it }
        readViaRoot(context)?.takeIf { it.hasData() }?.let { return it }
        return null
    }

    private fun readViaPackageContext(context: Context): Snapshot? {
        return try {
            val legacyContext = context.createPackageContext(
                LEGACY_PACKAGE,
                Context.CONTEXT_IGNORE_SECURITY,
            )
            val legacy = legacyContext.getSharedPreferences(LEGACY_PREF_FILE, Context.MODE_PRIVATE)
            snapshotFromPrefs(legacy)
        } catch (_: Exception) {
            null
        }
    }

    private fun readViaRoot(context: Context): Snapshot? {
        val path = "${context.filesDir.parentFile}/$LEGACY_PACKAGE/shared_prefs/$LEGACY_PREF_FILE.xml"
        val xml = runBlocking {
            try {
                rootAccessClient.readText(path)
            } catch (_: Exception) {
                null
            }
        }
        return xml?.takeIf { it.isNotBlank() }?.let { parsePrefsXml(it) }
    }
}
