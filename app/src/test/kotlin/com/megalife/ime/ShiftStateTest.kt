package com.megalife.ime

import com.megalife.ime.input.ShiftState
import org.junit.Assert.*
import org.junit.Test

class ShiftStateTest {

    @Test
    fun `initial state is LOWER`() {
        val state = ShiftState.LOWER
        assertFalse(state.isUpperCase)
        assertFalse(state.isCapsLock)
    }

    @Test
    fun `tap from LOWER goes to UPPER_NEXT`() {
        val state = ShiftState.LOWER.onShiftTap()
        assertEquals(ShiftState.UPPER_NEXT, state)
        assertTrue(state.isUpperCase)
    }

    @Test
    fun `tap from UPPER_NEXT goes to LOWER`() {
        val state = ShiftState.UPPER_NEXT.onShiftTap()
        assertEquals(ShiftState.LOWER, state)
    }

    @Test
    fun `tap from CAPS_LOCK goes to LOWER`() {
        val state = ShiftState.CAPS_LOCK.onShiftTap()
        assertEquals(ShiftState.LOWER, state)
    }

    @Test
    fun `double tap goes to CAPS_LOCK`() {
        val state = ShiftState.LOWER.onShiftDoubleTap()
        assertEquals(ShiftState.CAPS_LOCK, state)
        assertTrue(state.isCapsLock)
    }

    @Test
    fun `afterCharTyped from UPPER_NEXT returns LOWER`() {
        val state = ShiftState.UPPER_NEXT.afterCharTyped()
        assertEquals(ShiftState.LOWER, state)
    }

    @Test
    fun `afterCharTyped from CAPS_LOCK stays CAPS_LOCK`() {
        val state = ShiftState.CAPS_LOCK.afterCharTyped()
        assertEquals(ShiftState.CAPS_LOCK, state)
    }

    @Test
    fun `applyToChar uppercases in UPPER_NEXT`() {
        assertEquals('A', ShiftState.UPPER_NEXT.applyToChar('a'))
        assertEquals('Z', ShiftState.UPPER_NEXT.applyToChar('z'))
    }

    @Test
    fun `applyToChar uppercases in CAPS_LOCK`() {
        assertEquals('A', ShiftState.CAPS_LOCK.applyToChar('a'))
    }

    @Test
    fun `applyToChar keeps lowercase in LOWER`() {
        assertEquals('a', ShiftState.LOWER.applyToChar('a'))
    }
}
