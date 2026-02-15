package id.usecase.noted.di

import org.junit.Assert.assertEquals
import org.junit.Test

class BackendBaseUrlTest {
    @Test
    fun blankConfiguredUrlFallsBackToDefault() {
        val resolved = resolveBackendBaseUrl("   ")

        assertEquals("http://10.0.2.2:8080", resolved)
    }

    @Test
    fun configuredUrlIsTrimmedAndUsed() {
        val resolved = resolveBackendBaseUrl("  https://api.noted.dev  ")

        assertEquals("https://api.noted.dev", resolved)
    }
}
