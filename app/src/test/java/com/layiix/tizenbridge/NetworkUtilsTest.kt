package com.layiix.tizenbridge

import com.layiix.tizenbridge.net.NetworkUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkUtilsTest {

    @Test
    fun `ipv4 validi`() {
        assertTrue(NetworkUtils.isValidIpv4("192.168.1.100"))
        assertTrue(NetworkUtils.isValidIpv4("10.0.0.5"))
        assertTrue(NetworkUtils.isValidIpv4(" 192.168.1.1 "))
    }

    @Test
    fun `ipv4 non validi`() {
        assertFalse(NetworkUtils.isValidIpv4(""))
        assertFalse(NetworkUtils.isValidIpv4("999.1.1.1"))
        assertFalse(NetworkUtils.isValidIpv4("192.168.1"))
        assertFalse(NetworkUtils.isValidIpv4("192.168.01.1"))
        assertFalse(NetworkUtils.isValidIpv4("0.0.0.0"))
        assertFalse(NetworkUtils.isValidIpv4("255.255.255.255"))
        assertFalse(NetworkUtils.isValidIpv4("abc"))
        assertFalse(NetworkUtils.isValidIpv4("192.168.1.1:26101"))
    }

    @Test
    fun `porte valide`() {
        assertTrue(NetworkUtils.isValidPort("26101"))
        assertTrue(NetworkUtils.isValidPort("1"))
        assertTrue(NetworkUtils.isValidPort("65535"))
        assertFalse(NetworkUtils.isValidPort("0"))
        assertFalse(NetworkUtils.isValidPort("65536"))
        assertFalse(NetworkUtils.isValidPort("abc"))
        assertFalse(NetworkUtils.isValidPort(""))
    }

    @Test
    fun `subnet base`() {
        assertEquals("192.168.1", NetworkUtils.subnetBaseOf("192.168.1.100"))
        assertEquals(null, NetworkUtils.subnetBaseOf("non-ip"))
    }

    @Test
    fun `porta chiusa su localhost ritorna false in fretta`() {
        // Porta quasi certamente chiusa; timeout breve per non rallentare i test.
        assertFalse(NetworkUtils.isPortOpen("127.0.0.1", 9, 300))
    }
}
