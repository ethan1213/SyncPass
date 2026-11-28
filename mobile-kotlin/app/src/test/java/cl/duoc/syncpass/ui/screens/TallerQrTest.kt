package cl.duoc.syncpass.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TallerQrTest {
    @Test fun reconoceQrDeTallerDeWebYAndroid() {
        assertEquals(42L, tallerIdDelQr("SPT:42"))
        assertEquals(42L, tallerIdDelQr(" spt:42\n"))
    }

    @Test fun rechazaQrPersonalYCodigosInvalidos() {
        assertNull(tallerIdDelQr("SP1:11111111-2222-4333-8444-555555555555"))
        assertNull(tallerIdDelQr("SPT:0"))
        assertNull(tallerIdDelQr("SPT:99999999999999999999999"))
    }
}
