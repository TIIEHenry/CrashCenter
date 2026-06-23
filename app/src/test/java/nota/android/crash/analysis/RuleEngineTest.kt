package nota.android.crash.analysis

import nota.android.crash.common.data.CrashAnalysis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RuleEngineTest {

    // Minimal rules JSON covering every exception type in rules_v1.json
    private val rulesJson = """
    {
      "version": 1,
      "rules": [
        {
          "id": "anr",
          "exceptionClassPatterns": [],
          "stackTracePatterns": ["ANR in"],
          "category": "Application Not Responding",
          "rootCauseTags": ["Main thread blocked"],
          "suggestion": "Avoid long operations on the main thread.",
          "devSuggestion": "Use coroutines with Dispatchers.IO."
        },
        {
          "id": "npe",
          "exceptionClassPatterns": ["java.lang.NullPointerException"],
          "stackTracePatterns": [],
          "category": "Null Reference",
          "rootCauseTags": ["Null dereference"],
          "suggestion": "Check for null references before accessing objects.",
          "devSuggestion": "Enable Kotlin null-safety."
        },
        {
          "id": "oom",
          "exceptionClassPatterns": ["java.lang.OutOfMemoryError"],
          "stackTracePatterns": [],
          "category": "Memory Exhaustion",
          "rootCauseTags": ["Heap overflow"],
          "suggestion": "Reduce memory usage or image sizes.",
          "devSuggestion": "Use BitmapFactory.Options.inSampleSize."
        },
        {
          "id": "stack_overflow",
          "exceptionClassPatterns": ["java.lang.StackOverflowError"],
          "stackTracePatterns": [],
          "category": "Stack Overflow",
          "rootCauseTags": ["Infinite recursion"],
          "suggestion": "Check for infinite recursion in method calls.",
          "devSuggestion": "Verify recursive methods have a valid base case."
        },
        {
          "id": "security",
          "exceptionClassPatterns": ["java.lang.SecurityException"],
          "stackTracePatterns": [],
          "category": "Permission Denied",
          "rootCauseTags": ["Missing permission"],
          "suggestion": "Check that all required permissions are granted.",
          "devSuggestion": "Request runtime permissions before accessing protected APIs."
        },
        {
          "id": "class_cast",
          "exceptionClassPatterns": ["java.lang.ClassCastException"],
          "stackTracePatterns": [],
          "category": "Type Mismatch",
          "rootCauseTags": ["Wrong cast"],
          "suggestion": "Check type compatibility before casting.",
          "devSuggestion": "Use safe casts (as?) in Kotlin."
        },
        {
          "id": "illegal_state",
          "exceptionClassPatterns": ["java.lang.IllegalStateException"],
          "stackTracePatterns": [],
          "category": "Invalid State",
          "rootCauseTags": ["Lifecycle violation"],
          "suggestion": "Check component lifecycle state before performing operations.",
          "devSuggestion": "Ensure Fragment is in correct lifecycle state."
        },
        {
          "id": "network_on_main",
          "exceptionClassPatterns": ["android.os.NetworkOnMainThreadException"],
          "stackTracePatterns": [],
          "category": "Network on Main Thread",
          "rootCauseTags": ["Blocking UI thread"],
          "suggestion": "Move network calls to a background thread.",
          "devSuggestion": "Use coroutines with Dispatchers.IO."
        },
        {
          "id": "sql",
          "exceptionClassPatterns": ["android.database.sqlite.SQLiteException"],
          "stackTracePatterns": [],
          "category": "Database Error",
          "rootCauseTags": ["Schema mismatch"],
          "suggestion": "Check database schema and queries.",
          "devSuggestion": "Verify table schema matches your queries."
        },
        {
          "id": "io",
          "exceptionClassPatterns": ["java.io.IOException", "java.io.FileNotFoundException"],
          "stackTracePatterns": [],
          "category": "I/O Error",
          "rootCauseTags": ["File access"],
          "suggestion": "Check file/network permissions and connectivity.",
          "devSuggestion": "Use try-with-resources (use{} in Kotlin) to close streams."
        }
      ]
    }
    """.trimIndent()

    private val engine = RuleEngine.fromJson(rulesJson)

    // ------------------------------------------------------------------ NPE
    @Test
    fun `NullPointerException matches Null Reference category`() {
        val result = engine.match(
            "java.lang.NullPointerException",
            "java.lang.NullPointerException: Attempt to invoke virtual method\n\tat com.example.app.MainActivity.onCreate(MainActivity.java:42)"
        )
        assertNotNull(result)
        assertEquals("Null Reference", result!!.category)
        assertEquals("Check for null references before accessing objects.", result.suggestion)
        assertEquals(listOf("Null dereference"), result.rootCauseTags)
    }

    @Test
    fun `NPE subclass also matches`() {
        val result = engine.match("java.lang.NullPointerException\$Inner", "stack trace")
        assertNotNull(result)
        assertEquals("Null Reference", result!!.category)
    }

    // ------------------------------------------------------------------ IOException
    @Test
    fun `IOException matches I O Error category`() {
        val result = engine.match(
            "java.io.IOException",
            "java.io.IOException: No such file or directory\n\tat java.io.FileInputStream.open"
        )
        assertNotNull(result)
        assertEquals("I/O Error", result!!.category)
        assertEquals("Check file/network permissions and connectivity.", result.suggestion)
    }

    @Test
    fun `FileNotFoundException subclass matches I O Error`() {
        val result = engine.match("java.io.FileNotFoundException", "stack trace")
        assertNotNull(result)
        assertEquals("I/O Error", result!!.category)
    }

    // -------------------------------------------------- NetworkOnMainThreadException
    @Test
    fun `NetworkOnMainThreadException matches`() {
        val result = engine.match(
            "android.os.NetworkOnMainThreadException",
            "android.os.NetworkOnMainThreadException\n\tat com.example.app.ApiClient.fetch(ApiClient.java:20)"
        )
        assertNotNull(result)
        assertEquals("Network on Main Thread", result!!.category)
        assertEquals("Move network calls to a background thread.", result.suggestion)
    }

    // -------------------------------------------------- SQLiteException
    @Test
    fun `SQLiteException matches Database Error category`() {
        val result = engine.match(
            "android.database.sqlite.SQLiteException",
            "android.database.sqlite.SQLiteException: no such table: users"
        )
        assertNotNull(result)
        assertEquals("Database Error", result!!.category)
        assertEquals("Check database schema and queries.", result.suggestion)
    }

    // -------------------------------------------------- SecurityException
    @Test
    fun `SecurityException matches Permission Denied category`() {
        val result = engine.match(
            "java.lang.SecurityException",
            "java.lang.SecurityException: Permission denied: android.permission.CAMERA"
        )
        assertNotNull(result)
        assertEquals("Permission Denied", result!!.category)
    }

    // -------------------------------------------------- OutOfMemoryError
    @Test
    fun `OutOfMemoryError matches Memory Exhaustion category`() {
        val result = engine.match(
            "java.lang.OutOfMemoryError",
            "java.lang.OutOfMemoryError: Failed to allocate a 4194304 byte allocation"
        )
        assertNotNull(result)
        assertEquals("Memory Exhaustion", result!!.category)
        assertEquals("Reduce memory usage or image sizes.", result.suggestion)
    }

    // -------------------------------------------------- ClassCastException
    @Test
    fun `ClassCastException matches Type Mismatch category`() {
        val result = engine.match(
            "java.lang.ClassCastException",
            "java.lang.ClassCastException: java.lang.String cannot be cast to java.lang.Integer"
        )
        assertNotNull(result)
        assertEquals("Type Mismatch", result!!.category)
    }

    // -------------------------------------------------- IllegalStateException
    @Test
    fun `IllegalStateException matches Invalid State category`() {
        val result = engine.match(
            "java.lang.IllegalStateException",
            "java.lang.IllegalStateException: Fragment not attached to Activity"
        )
        assertNotNull(result)
        assertEquals("Invalid State", result!!.category)
    }

    // -------------------------------------------------- StackOverflowError
    @Test
    fun `StackOverflowError matches Stack Overflow category`() {
        val result = engine.match(
            "java.lang.StackOverflowError",
            "java.lang.StackOverflowError: stack size 8MB\n\tat com.example.Recursive.method(Recursive.java:10)"
        )
        assertNotNull(result)
        assertEquals("Stack Overflow", result!!.category)
    }

    // -------------------------------------------------- ANR (stack trace pattern)
    @Test
    fun `ANR pattern matches from stack trace even with non-matching exception class`() {
        val result = engine.match(
            "java.lang.Exception",
            "ANR in com.example.app (com.example.app/.MainActivity)\nPID: 1234"
        )
        assertNotNull(result)
        assertEquals("Application Not Responding", result!!.category)
        assertEquals("Main thread blocked", result.rootCauseTags.single())
    }

    @Test
    fun `ANR pattern matches case insensitively`() {
        val result = engine.match(
            "java.lang.Exception",
            "anr in com.example.app"
        )
        assertNotNull(result)
        assertEquals("Application Not Responding", result!!.category)
    }

    // -------------------------------------------------- Negative: no match
    @Test
    fun `unknown exception returns null`() {
        val result = engine.match("com.example.UnknownException", "some stack trace")
        assertNull(result)
    }

    @Test
    fun `empty exception class returns null`() {
        val result = engine.match("", "some stack trace")
        assertNull(result)
    }

    @Test
    fun `empty rules list returns null`() {
        val empty = RuleEngine(emptyList())
        assertNull(empty.match("java.lang.NullPointerException", "stack"))
    }

    // -------------------------------------------------- ANR rule must not match when stack trace lacks pattern
    @Test
    fun `ANR rule does not match when stack trace lacks ANR pattern`() {
        // ANR rule has empty exceptionClassPatterns so it only fires on stack trace pattern.
        // With a NullPointerException exception class and no "ANR in" in trace, the NPE rule matches instead.
        val result = engine.match("java.lang.NullPointerException", "NullPointerException at line 1")
        assertNotNull(result)
        assertEquals("Null Reference", result!!.category)
    }

    // -------------------------------------------------- Real-world NPE stack trace
    @Test
    fun `real world NPE stack trace matches`() {
        val trace = """
            java.lang.NullPointerException: Attempt to invoke virtual method 'int java.lang.String.length()' on a null object reference
                at com.example.app.ui.HomeFragment.onViewCreated(HomeFragment.kt:68)
                at androidx.fragment.app.Fragment.performViewCreated(Fragment.java:2987)
                at androidx.fragment.app.FragmentStateManager.createView(FragmentStateManager.java:546)
                at androidx.fragment.app.FragmentStateManager.moveToExpectedState(FragmentStateManager.java:282)
                at androidx.fragment.app.FragmentStore.moveToExpectedState(FragmentStore.java:112)
                at androidx.fragment.app.FragmentManager.moveToState(FragmentManager.java:1647)
                at androidx.fragment.app.FragmentManager.dispatchStateChange(FragmentManager.java:3126)
                at androidx.fragment.app.FragmentManager.dispatchActivityCreated(FragmentManager.java:3070)
                at androidx.fragment.app.FragmentController.dispatchActivityCreated(FragmentController.java:251)
                at androidx.fragment.app.FragmentActivity.onStart(FragmentActivity.java:508)
                at androidx.appcompat.app.AppCompatActivity.onStart(AppCompatActivity.java:248)
                at android.app.Instrumentation.callActivityOnStart(Instrumentation.java:1435)
                at android.app.Activity.performStart(Activity.java:8231)
                at android.app.ActivityThread.handleStartActivity(ActivityThread.java:3827)
        """.trimIndent()

        val result = engine.match("java.lang.NullPointerException", trace)
        assertNotNull(result)
        assertEquals("Null Reference", result!!.category)
    }
}
