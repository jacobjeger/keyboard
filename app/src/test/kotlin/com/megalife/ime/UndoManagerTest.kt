package com.megalife.ime

import com.megalife.ime.core.UndoManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class UndoManagerTest {

    private lateinit var undoManager: UndoManager

    @Before
    fun setup() {
        undoManager = UndoManager()
    }

    @Test
    fun `undo returns the word that was just committed`() {
        undoManager.record("hello", 5, hasAutoSpace = true)
        undoManager.record("world", 5, hasAutoSpace = false)

        val state = undoManager.undo()
        assertNotNull(state)
        assertEquals("world", state!!.text)
        assertFalse(state.hasAutoSpace)
    }

    @Test
    fun `undo with no history returns null`() {
        assertNull(undoManager.undo())
    }

    @Test
    fun `undo single item returns it`() {
        undoManager.record("hello", 5)
        val state = undoManager.undo()
        assertNotNull(state)
        assertEquals("hello", state!!.text)
    }

    @Test
    fun `redo returns undone state`() {
        undoManager.record("hello", 5, hasAutoSpace = true)
        undoManager.record("world", 5, hasAutoSpace = false)
        undoManager.undo()

        val state = undoManager.redo()
        assertNotNull(state)
        assertEquals("world", state!!.text)
        assertFalse(state.hasAutoSpace)
    }

    @Test
    fun `hasAutoSpace false for enter commit`() {
        undoManager.record("hello", 5, hasAutoSpace = false)

        val state = undoManager.undo()
        assertNotNull(state)
        assertFalse(state!!.hasAutoSpace)
    }

    @Test
    fun `hasAutoSpace true for space commit`() {
        undoManager.record("hello", 5, hasAutoSpace = true)

        val state = undoManager.undo()
        assertNotNull(state)
        assertTrue(state!!.hasAutoSpace)
    }

    @Test
    fun `max size evicts oldest`() {
        val manager = UndoManager(maxSize = 3)
        manager.record("a", 1)
        manager.record("b", 1)
        manager.record("c", 1)
        manager.record("d", 1)  // "a" should be evicted

        // Can undo 3 times (d, c, b)
        assertEquals("d", manager.undo()!!.text)
        assertEquals("c", manager.undo()!!.text)
        assertEquals("b", manager.undo()!!.text)
        assertNull(manager.undo())
    }

    @Test
    fun `clear resets stacks`() {
        undoManager.record("hello", 5)
        undoManager.record("world", 5)
        undoManager.clear()

        assertFalse(undoManager.canUndo)
        assertFalse(undoManager.canRedo)
    }

    @Test
    fun `undo then redo preserves state`() {
        undoManager.record("hello", 5, hasAutoSpace = true)
        val undone = undoManager.undo()
        assertEquals("hello", undone!!.text)

        val redone = undoManager.redo()
        assertEquals("hello", redone!!.text)
        assertTrue(redone.hasAutoSpace)
    }
}
