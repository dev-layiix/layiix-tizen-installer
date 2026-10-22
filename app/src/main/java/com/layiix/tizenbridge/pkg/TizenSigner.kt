package com.layiix.tizenbridge.pkg

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.apache.xml.security.c14n.Canonicalizer
import org.w3c.dom.Element
import org.xml.sax.InputSource

/**
 * Firma pacchetti .wgt in stile Tizen (author-signature.xml + signature1.xml),
 * replicando il formato di Tizen Studio: riferimenti SHA-512 ordinati,
 * RSA-SHA512 su SignedInfo in exclusive-C14N, blocco #prop in C14N 1.1.
 * Formato verificato byte-a-byte contro pacchetti firmati reali.
 */
object TizenSigner {

    data class P12(val key: PrivateKey, val chain: List<X509Certificate>)

    private const val DSIG = "http://www.w3.org/2000/09/xmldsig#"
    private const val EXC_C14N = "http://www.w3.org/2001/10/xml-exc-c14n#"
    private const val C14N11 = "http://www.w3.org/2006/12/xml-c14n11"
    private const val RSA_SHA512 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512"
    private const val SHA512 = "http://www.w3.org/2001/04/xmlenc#sha512"
    private const val DSP = "http://www.w3.org/2009/xmldsig-properties"
    private const val PROFILE = "http://www.w3.org/ns/widgets-digsig#profile"
    private const val ROLE_AUTHOR = "http://www.w3.org/ns/widgets-digsig#role-author"
    private const val ROLE_DIST = "http://www.w3.org/ns/widgets-digsig#role-distributor"

    private val SIG_FILES = setOf("author-signature.xml", "signature1.xml", "signature2.xml")

    init {
        // Su Android alcune registrazioni opzionali possono mancare: il path
        // DOM (Canonicalizer) funziona comunque; l'errore emergerebbe all'uso.
        runCatching { org.apache.xml.security.Init.init() }
    }

    /** Legge chiave privata RSA + catena dal .p12 (author.p12 / distributor.p12 di Tizen Studio). */
    fun loadP12(bytes: ByteArray, password: String): P12 {
        val ks = KeyStore.getInstance("PKCS12")
        ks.load(ByteArrayInputStream(bytes), password.toCharArray())
        val aliases = ks.aliases().asSequence().toList()
        for (a in aliases) {
            if (!ks.isKeyEntry(a)) continue
            val key = ks.getKey(a, password.toCharArray()) as? PrivateKey ?: continue
            require(key.algorithm == "RSA") { "Il certificato deve usare chiave RSA." }
            val chain = (ks.getCertificateChain(a)?.toList() ?: listOfNotNull(ks.getCertificate(a)))
                .filterIsInstance<X509Certificate>()
            require(chain.isNotEmpty()) { "Nessun certificato nel .p12." }
            return P12(key, chain)
        }
        error("Nessuna chiave privata trovata nel .p12.")
    }

    /** Firma tutti i file del pacchetto; ritorna il .wgt completo di firme. */
    fun signWgt(unsigned: ByteArray, author: P12, dist: P12): ByteArray {
        val entries = readEntries(unsigned)
        require(entries.isNotEmpty()) { "Pacchetto vuoto." }
        val files = entries.filter { (name, _) -> name !in SIG_FILES }
        require(files.isNotEmpty()) { "Nessun file da firmare." }

        val authorXml = buildSignature("AuthorSignature", ROLE_AUTHOR, files, author).toByteArray(Charsets.UTF_8)
        val withAuthor = files + ("author-signature.xml" to authorXml)
        val distXml = buildSignature("DistributorSignature", ROLE_DIST, withAuthor, dist).toByteArray(Charsets.UTF_8)

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, data) in files) {
                zip.putNextEntry(ZipEntry(name).apply { method = ZipEntry.DEFLATED })
                zip.write(data)
                zip.closeEntry()
            }
            for ((name, data) in listOf("author-signature.xml" to authorXml, "signature1.xml" to distXml)) {
                zip.putNextEntry(ZipEntry(name).apply { method = ZipEntry.DEFLATED })
                zip.write(data)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun readEntries(wgt: ByteArray): List<Pair<String, ByteArray>> {
        val list = mutableListOf<Pair<String, ByteArray>>()
        ZipInputStream(ByteArrayInputStream(wgt)).use { zin ->
            var e = zin.nextEntry
            while (e != null) {
                if (!e.isDirectory) list.add(e.name to zin.readBytes())
                zin.closeEntry()
                e = zin.nextEntry
            }
        }
        return list
    }

    private fun sha512(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-512").digest(data)

    private val b64enc: Base64.Encoder = Base64.getMimeEncoder(64, byteArrayOf('\n'.code.toByte()))

    private fun b64(data: ByteArray): String = b64enc.encodeToString(data)

    /** Percent-encoding stile Tizen: invariati solo [A-Za-z0-9-_.~], resto %XX maiuscolo. */
    fun pct(name: String): String {
        val sb = StringBuilder()
        for (ch in name) {
            if (ch.isAsciiAlphanumeric() || ch in "-_.~") {
                sb.append(ch)
            } else {
                for (b in ch.toString().toByteArray(Charsets.UTF_8)) {
                    sb.append('%').append(String.format("%02X", (b.toInt() and 0xFF)))
                }
            }
        }
        return sb.toString()
    }

    private fun Char.isAsciiAlphanumeric(): Boolean =
        this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9'

    private fun parseXml(xml: String): Element {
        val dbf = DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        val doc = dbf.newDocumentBuilder().parse(InputSource(xml.reader()))
        return doc.documentElement
    }

    private fun c14n(el: Element, algo: String): ByteArray {
        val out = ByteArrayOutputStream()
        Canonicalizer.getInstance(algo).canonicalizeSubtree(el, out)
        return out.toByteArray()
    }

    private fun rsaSign(key: PrivateKey, data: ByteArray): ByteArray {
        val s = Signature.getInstance("SHA512withRSA")
        s.initSign(key)
        s.update(data)
        return s.sign()
    }

    private fun propObject(sigId: String, role: String): String {
        val dsp = "xmlns:dsp=\"$DSP\""
        // xmlns esplicito: il fragment, parsato da solo, deve canonicalizzare
        // come nel documento (dove eredita il default ns da Signature).
        return "<Object xmlns=\"$DSIG\" Id=\"prop\">" +
            "<SignatureProperties $dsp>" +
            "<SignatureProperty Id=\"profile\" Target=\"#$sigId\">" +
            "<dsp:Profile URI=\"$PROFILE\"></dsp:Profile></SignatureProperty>" +
            "<SignatureProperty Id=\"role\" Target=\"#$sigId\">" +
            "<dsp:Role URI=\"$role\"></dsp:Role></SignatureProperty>" +
            "<SignatureProperty Id=\"identifier\" Target=\"#$sigId\">" +
            "<dsp:Identifier></dsp:Identifier></SignatureProperty>" +
            "</SignatureProperties></Object>"
    }

    private fun buildSignature(
        sigId: String,
        role: String,
        files: List<Pair<String, ByteArray>>,
        p12: P12,
    ): String {
        val sorted = files.sortedBy { it.first }
        val refsXml = StringBuilder()
        for ((name, data) in sorted) {
            refsXml.append("<Reference URI=\"${pct(name)}\">\n")
            refsXml.append("<DigestMethod Algorithm=\"$SHA512\"></DigestMethod>\n")
            refsXml.append("<DigestValue>${b64(sha512(data))}</DigestValue>\n")
            refsXml.append("</Reference>\n")
        }
        val prop = propObject(sigId, role)
        val propDigest = b64(sha512(c14n(parseXml(prop), C14N11)))
        refsXml.append("<Reference URI=\"#prop\">\n")
        refsXml.append("<Transforms>\n")
        refsXml.append("<Transform Algorithm=\"$C14N11\"></Transform>\n")
        refsXml.append("</Transforms>\n")
        refsXml.append("<DigestMethod Algorithm=\"$SHA512\"></DigestMethod>\n")
        refsXml.append("<DigestValue>$propDigest</DigestValue>\n")
        refsXml.append("</Reference>\n")

        val signedInfo =
            "<SignedInfo xmlns=\"$DSIG\">\n" +
            "<CanonicalizationMethod Algorithm=\"$EXC_C14N\"></CanonicalizationMethod>\n" +
            "<SignatureMethod Algorithm=\"$RSA_SHA512\"></SignatureMethod>\n" +
            refsXml.toString() +
            "</SignedInfo>"
        val signedInfoBytes = c14n(parseXml(signedInfo), EXC_C14N)
        val sigValue = b64(rsaSign(p12.key, signedInfoBytes))

        val certs = p12.chain.take(2).joinToString("") { c ->
            "<X509Certificate>${b64(c.encoded)}</X509Certificate>\n"
        }
        // Serializzazione stile Tizen: niente dichiarazione XML, un elemento per riga.
        return "<Signature xmlns=\"$DSIG\" Id=\"$sigId\">\n" +
            signedInfo + "\n" +
            "<SignatureValue>\n$sigValue\n</SignatureValue>\n" +
            "<KeyInfo>\n<X509Data>\n$certs</X509Data>\n</KeyInfo>\n" +
            "$prop\n" +
            "</Signature>"
    }
}
