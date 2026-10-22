package com.layiix.tizenbridge

import com.layiix.tizenbridge.sdb.TvErrorMapper
import com.layiix.tizenbridge.sdb.TvRestApi
import com.layiix.tizenbridge.prefs.LangPrefs
import com.layiix.tizenbridge.prefs.TvProfile
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TvErrorMapperTest {

    @Test
    fun `firma -11`() {
        val m = TvErrorMapper.map("install failed[-11] invalid signature")
        assertEquals(TvErrorMapper.Category.SIGNATURE_11, m.category)
        assertTrue(m.fixSteps.isNotEmpty())
    }

    @Test
    fun `certificato -12`() {
        val m = TvErrorMapper.map("failed[-12] author certificate mismatch")
        assertEquals(TvErrorMapper.Category.CERT_12, m.category)
    }

    @Test
    fun `privilegi -14`() {
        val m = TvErrorMapper.map("failed[-14] privilege level")
        assertEquals(TvErrorMapper.Category.PRIVILEGE_14, m.category)
    }

    @Test
    fun `rete rifiutata`() {
        val m = TvErrorMapper.map("Connection refused (errno 111)")
        assertEquals(TvErrorMapper.Category.DEVMODE_OR_AUTH, m.category)
        assertTrue(m.fixSteps.any { it.contains("1 2 3 4 5") })
    }

    @Test
    fun `null o vuoto non e errore`() {
        assertEquals(TvErrorMapper.Category.NONE, TvErrorMapper.map("").category)
        assertEquals(TvErrorMapper.Category.NONE, TvErrorMapper.map(null).category)
    }

    @Test
    fun `certificato 118 meno 12 non firmato`() {
        val raw = "app_id[Iperf3TvAp.iperf3tv] install failed[118, -12], reason: Check certificate"
        val m = TvErrorMapper.map(raw)
        assertEquals(TvErrorMapper.Category.CERT_12, m.category)
        assertTrue(m.fixSteps.any { it.contains("Tizen Studio") })
    }

    @Test
    fun `spazio esaurito 116`() {
        val raw = "install httpjellyfin.orgJellyfin app_id[httpjellyfin.orgJellyfin] " +
            "download failed[116] spend time for wascmd is [54]ms"
        val m = TvErrorMapper.map(raw)
        assertEquals(TvErrorMapper.Category.PKG_REJECTED_116, m.category)
        assertTrue(m.fixSteps.isNotEmpty())
    }
}

class LangPrefsTest {

    @Test
    fun `mapping tag locale`() {
        assertEquals(Locale.ITALIAN, LangPrefs.localeFor("it"))
        assertEquals(Locale.ENGLISH, LangPrefs.localeFor("en"))
        assertEquals("es", LangPrefs.localeFor("es")?.language)
        assertEquals("pt", LangPrefs.localeFor("pt")?.language)
        assertEquals("ru", LangPrefs.localeFor("ru")?.language)
        assertNull(LangPrefs.localeFor(""))
        assertNull(LangPrefs.localeFor("xx"))
    }

    @Test
    fun `lingue supportate`() {
        assertTrue(LangPrefs.SUPPORTED.containsAll(listOf("", "it", "en", "es", "pt", "ru")))
    }
}

class TvProfileTest {

    @Test
    fun `mapping anni tizen da specifiche Samsung`() {
        assertEquals("9.0", TvProfile.tizenForYear("2025"))
        assertEquals("8.0", TvProfile.tizenForYear("2024"))
        assertEquals("7.0", TvProfile.tizenForYear("2023"))
        assertEquals("6.5", TvProfile.tizenForYear("2022"))
        assertEquals("6.0", TvProfile.tizenForYear("2021"))
        assertEquals("5.5", TvProfile.tizenForYear("2020"))
        assertEquals("5.0", TvProfile.tizenForYear("2019"))
        assertEquals("4.0", TvProfile.tizenForYear("2018"))
        assertEquals("3.0", TvProfile.tizenForYear("2017"))
        assertEquals("2.4", TvProfile.tizenForYear("2016"))
        assertEquals("2.3", TvProfile.tizenForYear("2015"))
        assertNull(TvProfile.tizenForYear("legacy"))
        assertNull(TvProfile.tizenForYear(""))
    }

    @Test
    fun `blocco solo non tizen`() {
        assertTrue(TvProfile.isBlocked(TvProfile.LG, ""))
        assertTrue(TvProfile.isBlocked(TvProfile.ANDROID, ""))
        assertTrue(TvProfile.isBlocked(TvProfile.OTHER, ""))
        assertTrue(TvProfile.isBlocked(TvProfile.SAMSUNG, TvProfile.LEGACY))
        assertFalse(TvProfile.isBlocked(TvProfile.SAMSUNG, "2020"))
        assertFalse(TvProfile.isBlocked(TvProfile.SAMSUNG, ""))
        assertFalse(TvProfile.isBlocked("", ""))
    }

    @Test
    fun `anno dedotto dal tizen rilevato`() {
        assertEquals("2020", TvProfile.yearForTizen("5.5"))
        assertEquals("2021", TvProfile.yearForTizen("Tizen 6.0"))
        assertEquals("2022", TvProfile.yearForTizen("6.5.1"))
        assertNull(TvProfile.yearForTizen(""))
        assertNull(TvProfile.yearForTizen(null))
        assertNull(TvProfile.yearForTizen("Orsay"))
    }
}

class TvRestApiTest {

    @Test
    fun `parse risposta samsung`() {
        val json = """{"device":{"modelName":"UE55AU7170","name":"TV Samsung","duid":"uuid:abc-123","developerMode":"1","developerIP":"192.168.1.5","resolution":"3840x2160"}}"""
        val info = TvRestApi.parse(json)
        assertEquals("UE55AU7170", info.model)
        assertEquals("abc-123", info.duid)
        assertEquals(true, info.isDevMode)
        assertEquals("192.168.1.5", info.developerIp)
    }

    @Test
    fun `parse con valori numerici`() {
        val info = TvRestApi.parse("""{"device":{"developerMode":0}}""")
        assertEquals("0", info.developerMode)
        assertEquals(false, info.isDevMode)
    }

    @Test
    fun `parse json vuoto`() {
        val info = TvRestApi.parse("{}")
        assertEquals("", info.model)
        assertEquals(false, info.isDevMode)
    }
}

class SamsungCertTest {

    @Test
    fun `csr auto-firmato verificabile`() {
        java.security.Security.addProvider(org.bouncycastle.jce.provider.BouncyCastleProvider())
        val samsung = com.layiix.tizenbridge.samsung.SamsungCert
        val kp = samsung.genRsa()
        val (pem, _) = samsung.buildCsr(
            kp, "Layiix",
            listOf("URN:tizen:packageid=", "URN:tizen:deviceid=2.0#ABC123"),
        )
        assertTrue(pem.contains("BEGIN CERTIFICATE REQUEST"))
        val holder = org.bouncycastle.pkcs.PKCS10CertificationRequest(pemBytes(pem))
        assertTrue(holder.isSignatureValid(
            org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder()
                .setProvider("BC").build(kp.public),
        ))
        val sans = holder.requestedExtensions
            .getExtension(org.bouncycastle.asn1.x509.Extension.subjectAlternativeName)
            .let { org.bouncycastle.asn1.x509.GeneralNames.getInstance(it.parsedValue).names }
            .map { (it.name as org.bouncycastle.asn1.DERIA5String).string }
            .joinToString("|")
        assertTrue(sans.contains("URN:tizen:deviceid=2.0#ABC123"))
    }

    private fun pemBytes(pem: String): ByteArray {
        val b64 = pem.lines().filter { !it.startsWith("-----") && it.isNotBlank() }.joinToString("")
        return java.util.Base64.getMimeDecoder().decode(b64)
    }

    @Test
    fun `duid normalizzato`() {
        val samsung = com.layiix.tizenbridge.samsung.SamsungCert
        assertEquals("2.0#ABC123", samsung.normalizeDuid(" 2.0#abc123 "))
        assertNull(samsung.normalizeDuid(""))
        assertNull(samsung.normalizeDuid("a b"))
        assertNull(samsung.normalizeDuid(null))
    }

    @Test
    fun `regola password`() {
        val samsung = com.layiix.tizenbridge.samsung.SamsungCert
        assertTrue(samsung.validPassword("Abcdef12"))
        assertFalse(samsung.validPassword("short1A"))
        assertFalse(samsung.validPassword("alllowercase1"))
        assertFalse(samsung.validPassword("ALLUPPER1"))
        assertFalse(samsung.validPassword("NoDigitsHere"))
    }
    @Test
    fun `doh parse estrae primo record A`() {
        val samsung = com.layiix.tizenbridge.samsung.SamsungCert
        val json = """{"Status":0,"Answer":[{"name":"x","type":1,"TTL":300,"data":"3.146.69.208"},{"name":"x","type":1,"TTL":300,"data":"3.129.78.174"}]}"""
        assertEquals("3.146.69.208", samsung.parseDoh(json))
        assertNull(samsung.parseDoh("""{"Status":0}"""))
        assertNull(samsung.parseDoh("non json"))
    }

    @Test
    fun `callback code da body e query`() {
        val samsung = com.layiix.tizenbridge.samsung.SamsungCert
        val code = "%7B%22access_token%22%3A%22abc%22%7D"
        assertEquals(
            """{"access_token":"abc"}""",
            samsung.parseCallbackCode("code=$code&x=1", "/signin/callback"),
        )
        assertEquals(
            """{"access_token":"abc"}""",
            samsung.parseCallbackCode("", "/signin/callback?code=$code"),
        )
        assertNull(samsung.parseCallbackCode("", "/signin/callback"))
    }

    @Test
    fun `login url ben formato`() {
        val url = com.layiix.tizenbridge.samsung.SamsungCert.loginUrl(4794)
        assertTrue(url.startsWith("https://account.samsung.com/mobile/account/check.do"))
        assertTrue(url.contains("serviceID=v285zxnl3h"))
        assertTrue(url.contains("StartOAuth2"))
        assertTrue(url.contains("localhost%3A4794") || url.contains("localhost:4794"))
    }

    private fun assetBytes(name: String): ByteArray {
        val cands = listOf(
            java.io.File("src/main/assets/certs/$name"),
            java.io.File("app/src/main/assets/certs/$name"),
        )
        val f = cands.firstOrNull { it.exists() }
            ?: throw AssertionError("asset mancante: $name (wd=${java.io.File(".").absolutePath})")
        return f.readBytes()
    }

    @Test
    fun `certificati condivisi integrati firmano davvero`() {
        val signer = com.layiix.tizenbridge.pkg.TizenSigner
        val author = signer.loadP12(assetBytes("author.p12"), "layiix-community-01")
        val dist = signer.loadP12(assetBytes("distributor.p12"), "tizenpkcs12passfordsigner")
        assertEquals("RSA", author.key.algorithm)
        assertEquals("RSA", dist.key.algorithm)
        // Firma un pacchetto e verifica che le firme siano presenti e coerenti.
        val bos = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(bos).use { z ->
            z.putNextEntry(java.util.zip.ZipEntry("config.xml"))
            z.write("""<widget id="X" version="1.0"><tizen:application id="X.A" package="X"/></widget>""".toByteArray())
            z.closeEntry()
            z.putNextEntry(java.util.zip.ZipEntry("index.html"))
            z.write("<html></html>".toByteArray())
            z.closeEntry()
        }
        val signed = signer.signWgt(bos.toByteArray(), author, dist)
        val names = mutableListOf<String>()
        java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(signed)).use { zin ->
            var e = zin.nextEntry
            while (e != null) {
                names.add(e.name)
                zin.closeEntry()
                e = zin.nextEntry
            }
        }
        assertTrue(names.contains("author-signature.xml"))
        assertTrue(names.contains("signature1.xml"))
        assertTrue(signed.size > bos.size())
    }

    @Test
    fun `duid pronto per san con prefisso versione`() {
        val samsung = com.layiix.tizenbridge.samsung.SamsungCert
        assertEquals("2.0#MTCEYX5ACON5S", samsung.duidForSan("mtceyx5acon5s"))
        assertEquals("2.0#ABC", samsung.duidForSan("2.0#abc"))
        assertEquals("-DEV1", samsung.duidForSan("-dev1"))
        assertNull(samsung.duidForSan(""))
        assertNull(samsung.duidForSan(null))
    }

    @Test
    fun `extensionrequest identico al riferimento bouncycastle`() {
        java.security.Security.addProvider(org.bouncycastle.jce.provider.BouncyCastleProvider())
        val samsung = com.layiix.tizenbridge.samsung.SamsungCert
        val kp = samsung.genRsa()
        val sans = listOf("URN:tizen:packageid=", "URN:tizen:deviceid=2.0#ABC")
        val (pem, _) = samsung.buildCsr(kp, "Layiix", sans)

        val bcBuilder = org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder(
            org.bouncycastle.asn1.x500.X500Name("CN=Layiix"), kp.public,
        )
        val gns = org.bouncycastle.asn1.x509.GeneralNames(
            sans.map {
                org.bouncycastle.asn1.x509.GeneralName(
                    org.bouncycastle.asn1.x509.GeneralName.uniformResourceIdentifier, it,
                )
            }.toTypedArray(),
        )
        val exts = org.bouncycastle.asn1.x509.Extensions(
            org.bouncycastle.asn1.x509.Extension(
                org.bouncycastle.asn1.x509.Extension.subjectAlternativeName, false, gns.encoded,
            ),
        )
        bcBuilder.addAttribute(
            org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, exts,
        )
        val signer = org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA512withRSA")
            .setProvider("BC").build(kp.private)
        val bcDer = bcBuilder.build(signer).encoded

        fun attrBytes(csrDer: ByteArray): ByteArray {
            val holder = org.bouncycastle.pkcs.PKCS10CertificationRequest(csrDer)
            val attr = holder.attributes.find {
                it.attrType == org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pkcs_9_at_extensionRequest
            } ?: throw AssertionError("extensionRequest assente")
            return attr.attrValues.encoded
        }

        val myDer = pemBytes(pem)
        assertTrue(attrBytes(myDer).contentEquals(attrBytes(bcDer)))
    }
}

class TizenSignerTest {

    private fun selfSigned(cn: String): Triple<java.security.PrivateKey, java.security.cert.X509Certificate, ByteArray> {
        java.security.Security.addProvider(org.bouncycastle.jce.provider.BouncyCastleProvider())
        val kpg = java.security.KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp = kpg.generateKeyPair()
        val name = org.bouncycastle.asn1.x500.X500Name("CN=$cn")
        val now = java.util.Date()
        val exp = java.util.Date(now.time + 365L * 24 * 3600 * 1000)
        val builder = org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
            name, java.math.BigInteger.valueOf(now.time), now, exp, name, kp.public,
        )
        val signer = org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA256withRSA").build(kp.private)
        val cert = org.bouncycastle.cert.jcajce.JcaX509CertificateConverter()
            .setProvider("BC").getCertificate(builder.build(signer)) as java.security.cert.X509Certificate
        val ks = java.security.KeyStore.getInstance("PKCS12")
        ks.load(null, null)
        ks.setKeyEntry("a", kp.private, "pwd".toCharArray(), arrayOf(cert))
        val bos = java.io.ByteArrayOutputStream()
        ks.store(bos, "pwd".toCharArray())
        return Triple(kp.private, cert, bos.toByteArray())
    }

    private fun minWgt(): ByteArray {
        val bos = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(bos).use { z ->
            z.putNextEntry(java.util.zip.ZipEntry("config.xml"))
            z.write("""<widget id="X" version="1.0"><tizen:application id="X.A" package="X"/></widget>""".toByteArray())
            z.closeEntry()
            z.putNextEntry(java.util.zip.ZipEntry("index.html"))
            z.write("<html></html>".toByteArray())
            z.closeEntry()
        }
        return bos.toByteArray()
    }

    private fun parseXml(s: String): org.w3c.dom.Element {
        val dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        return dbf.newDocumentBuilder().parse(org.xml.sax.InputSource(s.reader())).documentElement
    }

    private fun sha512(b: ByteArray): ByteArray =
        java.security.MessageDigest.getInstance("SHA-512").digest(b)

    private fun c14nExc(el: org.w3c.dom.Element): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        org.apache.xml.security.c14n.Canonicalizer
            .getInstance("http://www.w3.org/2001/10/xml-exc-c14n#").canonicalizeSubtree(el, out)
        return out.toByteArray()
    }

    @Test
    fun `loadP12 legge chiave e catena`() {
        val (key, cert, p12) = selfSigned("author")
        val loaded = com.layiix.tizenbridge.pkg.TizenSigner.loadP12(p12, "pwd")
        assertEquals(key, loaded.key)
        assertEquals(cert, loaded.chain.first())
    }

    @Test
    fun `password errata rifiutata`() {
        val (_, _, p12) = selfSigned("author")
        try {
            com.layiix.tizenbridge.pkg.TizenSigner.loadP12(p12, "sbagliata")
            assertTrue(false)
        } catch (_: Exception) {
        }
    }

    @Test
    fun `firma verificabile in modo indipendente`() {
        org.apache.xml.security.Init.init()
        val (_, authorCert, authorP12) = selfSigned("author")
        val (_, _, distP12) = selfSigned("distributor")
        val author = com.layiix.tizenbridge.pkg.TizenSigner.loadP12(authorP12, "pwd")
        val dist = com.layiix.tizenbridge.pkg.TizenSigner.loadP12(distP12, "pwd")
        val signed = com.layiix.tizenbridge.pkg.TizenSigner.signWgt(minWgt(), author, dist)

        val entries = mutableMapOf<String, ByteArray>()
        java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(signed)).use { zin ->
            var e = zin.nextEntry
            while (e != null) {
                if (!e.isDirectory) entries[e.name] = zin.readBytes()
                zin.closeEntry()
                e = zin.nextEntry
            }
        }
        assertTrue(entries.containsKey("author-signature.xml"))
        assertTrue(entries.containsKey("signature1.xml"))

        // signature1 deve coprire author-signature.xml + tutti i file.
        val sig1 = parseXml(entries["signature1.xml"]!!.toString(Charsets.UTF_8))
        val refs = sig1.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Reference")
        val uris = (0 until refs.length).map { (refs.item(it) as org.w3c.dom.Element).getAttribute("URI") }
        assertTrue(uris.contains("author-signature.xml"))
        assertTrue(uris.contains("config.xml"))
        assertTrue(uris.contains("index.html"))
        assertEquals("#prop", uris.last())
        assertEquals(uris.dropLast(1).sorted(), uris.dropLast(1))

        // Ogni digest file ricombacia (SHA-512 indipendente).
        val dig = { el: org.w3c.dom.Element ->
            val dv = el.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "DigestValue")
                .item(0).textContent.replace("\\s".toRegex(), "")
            java.util.Base64.getDecoder().decode(dv)
        }
        for (i in 0 until refs.length) {
            val el = refs.item(i) as org.w3c.dom.Element
            val uri = el.getAttribute("URI")
            if (uri == "#prop") continue
            val name = java.net.URLDecoder.decode(uri, "UTF-8")
            val expected = entries[name] ?: throw AssertionError("entry mancante: $name")
            assertTrue("digest $uri", sha512(expected).contentEquals(dig(el)))
        }

        // Firma RSA verificabile con la chiave pubblica del certificato autore.
        // (signature1 è firmata dal distributore; qui si verifica il meccanismo
        // su author-signature.xml, firmata dall'autore.)
        val sigA = parseXml(entries["author-signature.xml"]!!.toString(Charsets.UTF_8))
        val siA = sigA.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "SignedInfo").item(0)
            as org.w3c.dom.Element
        val canonA = c14nExc(siA)
        val sigBytesA = java.util.Base64.getMimeDecoder().decode(
            sigA.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "SignatureValue")
                .item(0).textContent,
        )
        val vA = java.security.Signature.getInstance("SHA512withRSA")
        vA.initVerify(authorCert.publicKey)
        vA.update(canonA)
        assertTrue(vA.verify(sigBytesA))

        // KeyInfo contiene il certificato foglia.
        val ki = sigA.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "X509Certificate")
            .item(0).textContent.replace("\\s".toRegex(), "")
        assertEquals(
            java.util.Base64.getEncoder().encodeToString(authorCert.encoded),
            ki,
        )
    }
}
