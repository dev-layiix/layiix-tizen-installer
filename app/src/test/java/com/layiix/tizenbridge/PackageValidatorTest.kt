package com.layiix.tizenbridge

import com.layiix.tizenbridge.pkg.PackageValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageValidatorTest {

    private val pk = byteArrayOf(0x50, 0x4B)

    private fun wgtEntries(signed: Boolean = true): List<String> {
        val base = mutableListOf("config.xml", "index.html", "icon.png")
        if (signed) base.add("author-signature.xml")
        return base
    }

    private val configXml =
        """<?xml version="1.0" encoding="UTF-8"?>""" +
            """<widget id="AbC1234567" version="1.0.0">""" +
            """<application id="AbC1234567.Jellyfin" package="AbC1234567"/>""" +
            """</widget>"""

    @Test
    fun `wgt firmato valido`() {
        val r = PackageValidator.validate("Jellyfin.wgt", 10_000_000, pk, wgtEntries(), configXml)
        assertTrue(r.ok)
        assertEquals(PackageValidator.Kind.WGT, r.kind)
        assertTrue(r.signed)
        assertEquals("AbC1234567.Jellyfin", r.appId)
        assertEquals("AbC1234567", r.packageId)
    }

    @Test
    fun `wgt non firmato valido con warning`() {
        val r = PackageValidator.validate("app.wgt", 1000, pk, wgtEntries(false), configXml)
        assertTrue(r.ok)
        assertFalse(r.signed)
        assertTrue(r.warnings.any { it.contains("-11") })
    }

    @Test
    fun `apk rifiutato esplicitamente`() {
        val r = PackageValidator.validate("app.apk", 1000, pk, listOf("AndroidManifest.xml"), null)
        assertFalse(r.ok)
        assertTrue(r.errors.any { it.contains("APK") })
    }

    @Test
    fun `estensione sconosciuta rifiutata`() {
        val r = PackageValidator.validate("app.zip", 1000, pk, emptyList(), null)
        assertFalse(r.ok)
    }

    @Test
    fun `firma zip mancante`() {
        val r = PackageValidator.validate("app.wgt", 1000, byteArrayOf(0x00, 0x00), wgtEntries(), configXml)
        assertFalse(r.ok)
        assertTrue(r.errors.any { it.contains("ZIP") })
    }

    @Test
    fun `file vuoto e troppo grande`() {
        assertFalse(PackageValidator.validate("a.wgt", 0, pk, wgtEntries(), configXml).ok)
        assertFalse(
            PackageValidator.validate("a.wgt", PackageValidator.MAX_PACKAGE_BYTES + 1, pk, wgtEntries(), configXml).ok,
        )
    }

    @Test
    fun `config mancante nel wgt`() {
        val r = PackageValidator.validate("a.wgt", 100, pk, listOf("index.html"), null)
        assertFalse(r.ok)
        assertTrue(r.errors.any { it.contains("config.xml") })
    }

    @Test
    fun `tpk richiede tizen-manifest`() {
        val bad = PackageValidator.validate("a.tpk", 100, pk, listOf("config.xml"), null)
        assertFalse(bad.ok)
        val good = PackageValidator.validate(
            "a.tpk", 100, pk,
            listOf("tizen-manifest.xml", "bin/app"),
            """<manifest package="PkgId"><ui-application appid="PkgId.App"/></manifest>""",
        )
        assertTrue(good.ok)
        assertEquals("PkgId.App", good.appId)
    }

    @Test
    fun `maiuscole accettate`() {
        val r = PackageValidator.validate("APP.WGT", 100, pk, wgtEntries(), configXml)
        assertTrue(r.ok)
    }

    @Test
    fun `wgt tizen application con namespace`() {
        val tizen = """<?xml version="1.0" encoding="UTF-8"?>""" +
            """<widget id="LayiixTv01" version="1.0.0">""" +
            """<tizen:application id="LayiixTv01.LayiixTV" package="LayiixTv01"/>""" +
            """</widget>"""
        val r = PackageValidator.validate("LayiixTV.wgt", 52485, pk, wgtEntries(), tizen)
        assertTrue(r.ok)
        assertEquals("LayiixTv01.LayiixTV", r.appId)
        assertEquals("LayiixTv01", r.packageId)
        assertTrue(r.warnings.none { it.contains("ID applicazione") })
    }

    @Test
    fun `formatSize`() {
        assertEquals("500 B", PackageValidator.formatSize(500))
        assertEquals("2.0 KB", PackageValidator.formatSize(2048))
        assertEquals("10.0 MB", PackageValidator.formatSize(10 * 1024 * 1024))
    }

    @Test
    fun `required_version letta dal manifest`() {
        val xml = """<widget id="X" version="1.0">""" +
            """<tizen:application id="X.App" package="X" required_version="6.0"/>""" +
            """</widget>"""
        assertEquals("6.0", PackageValidator.parseRequiredVersion(xml))
        val r = PackageValidator.validate("a.wgt", 100, pk, wgtEntries(), xml)
        assertEquals("6.0", r.requiredVersion)
        assertEquals(null, PackageValidator.parseRequiredVersion(configXml))
    }

    @Test
    fun `compatibilita versioni tizen`() {
        assertEquals(true, PackageValidator.isCompatible("5.5", "5.5"))
        assertEquals(true, PackageValidator.isCompatible("5.0", "5.5"))
        assertEquals(false, PackageValidator.isCompatible("6.0", "5.5"))
        assertEquals(false, PackageValidator.isCompatible("6.5", "6.0"))
        assertEquals(true, PackageValidator.isCompatible("6.0", "6.5"))
        assertEquals(null, PackageValidator.isCompatible(null, "5.5"))
        assertEquals(null, PackageValidator.isCompatible("6.0", ""))
    }
}
