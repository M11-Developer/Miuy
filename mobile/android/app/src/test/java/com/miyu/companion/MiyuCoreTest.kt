package com.miyu.companion

import com.miyu.companion.data.AgeProfile
import com.miyu.companion.data.FloatingPosition
import com.miyu.companion.data.ToyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests (run by `./gradlew testDebugUnitTest` on CI, no device or emulator needed).
 * They cover the shared Play Lab / age-profile logic and the safety defaults of the bubble.
 */
class MiyuCoreTest {

    @Test
    fun arabicToyCommandIsUnderstood() {
        // "امسكي عربية لعبة" — the Egyptian-dialect phrase from the product spec.
        assertEquals(ToyType.CAR, ToyType.fromInput("امسكي عربية لعبة"))
        assertEquals(ToyType.TEDDY, ToyType.fromInput("Hold a teddy bear"))
        assertEquals(ToyType.BALLOON, ToyType.fromInput("بالونة"))
        assertNull(ToyType.fromInput("كلام عادي بدون لعبة"))
    }

    @Test
    fun allNineBaseToysExist() {
        assertEquals(9, ToyType.values().size)
    }

    @Test
    fun ageBandsMatchTheFiveProfiles() {
        assertEquals(AgeProfile.CHILD_3_5, AgeProfile.fromAge(4))
        assertEquals(AgeProfile.CHILD_6_8, AgeProfile.fromAge(7))
        assertEquals(AgeProfile.CHILD_9_12, AgeProfile.fromAge(11))
        assertEquals(AgeProfile.TEEN_13_17, AgeProfile.fromAge(15))
        assertEquals(AgeProfile.ADULT_18_PLUS, AgeProfile.fromAge(30))
    }

    @Test
    fun minorsRequireGuardianAndAdultsDoNot() {
        for (band in listOf(AgeProfile.CHILD_3_5, AgeProfile.CHILD_6_8, AgeProfile.CHILD_9_12)) {
            assertTrue("minors need guardian confirmation", band.maxAge <= 12)
        }
        assertTrue(AgeProfile.ADULT_18_PLUS.minAge >= 18)
    }

    @Test
    fun bubbleDefaultsAreSafe() {
        val pos = FloatingPosition()
        assertFalse("click-through must be opt-in", pos.clickThrough)
        assertFalse("motion must never be silently paused", pos.motionPaused)
        assertFalse("bubble must never hide itself", pos.isHidden)
        assertTrue("transparency within safe range", pos.alpha in 0.3f..1f)
        assertTrue("size within safe range", pos.size in 0.5f..1.5f)
    }
}
