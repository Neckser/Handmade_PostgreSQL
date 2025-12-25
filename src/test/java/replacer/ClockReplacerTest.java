package replacer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import system.model.BufferSlot;
import system.page.HeapPage;
import system.replacer.ClockReplacer;

import static org.junit.jupiter.api.Assertions.*;

class ClockReplacerTest {

    private ClockReplacer replacer;
    @BeforeEach
    void setUp() {
        replacer = new ClockReplacer();
    }
    private BufferSlot createBufferSlot(int pageId, boolean pinned, int usageCount) {
        HeapPage page = new HeapPage(pageId);
        BufferSlot slot = new BufferSlot(pageId, page);
        slot.setPinned(pinned);
        slot.setUsageCount(usageCount);
        return slot;
    }
    @Test
    void pickVictim_emptyBuffer_returnsNull() {
        assertNull(replacer.pickVictim());
    }

    @Test
    void push_unpinnedSlot_addsToReplacer() {
        BufferSlot slot = createBufferSlot(1, false, 0);
        replacer.push(slot);
        BufferSlot victim = replacer.pickVictim();
        assertNotNull(victim);
        assertEquals(1, victim.getPageId());
    }

    @Test
    void push_pinnedSlot_doesNotAddToReplacer() {
        BufferSlot slot = createBufferSlot(1, true, 0);
        replacer.push(slot);
        BufferSlot victim = replacer.pickVictim();
        assertNull(victim);
    }

    @Test
    void push_duplicateSlot_doesNotAddDuplicate() {
        BufferSlot slot = createBufferSlot(1, false, 0);
        replacer.push(slot);
        replacer.push(slot); // Duplicate push
        BufferSlot victim = replacer.pickVictim();
        assertNotNull(victim);
        assertEquals(1, victim.getPageId());
        assertNull(replacer.pickVictim());
    }

    @Test
    void delete_existingSlot_removesFromReplacer() {
        BufferSlot slot1 = createBufferSlot(1, false, 0);
        BufferSlot slot2 = createBufferSlot(2, false, 0);
        replacer.push(slot1);
        replacer.push(slot2);
        replacer.delete(1);
        BufferSlot victim = replacer.pickVictim();
        assertNotNull(victim);
        assertEquals(2, victim.getPageId());
        assertNull(replacer.pickVictim());
    }

    @Test
    void delete_nonExistingSlot_doesNothing() {
        BufferSlot slot = createBufferSlot(1, false, 0);
        replacer.push(slot);
        replacer.delete(999);
        BufferSlot victim = replacer.pickVictim();
        assertNotNull(victim);
        assertEquals(1, victim.getPageId());
    }

    @Test
    void pickVictim_usageCountZero_selectsImmediately() {
        BufferSlot slot1 = createBufferSlot(1, false, 0);
        BufferSlot slot2 = createBufferSlot(2, false, 1);
        replacer.push(slot1);
        replacer.push(slot2);
        BufferSlot victim = replacer.pickVictim();
        assertNotNull(victim);
        assertEquals(1, victim.getPageId());
    }

    @Test
    void mixedPinnedAndUnpinnedWithUsage() {
        BufferSlot pinned = createBufferSlot(1, true, 0);
        BufferSlot unpinned1 = createBufferSlot(2, false, 1);
        BufferSlot unpinned2 = createBufferSlot(3, false, 0);
        replacer.push(pinned);
        replacer.push(unpinned1);
        replacer.push(unpinned2);
        BufferSlot victim1 = replacer.pickVictim();
        assertNotNull(victim1);
        assertEquals(3, victim1.getPageId());
        BufferSlot victim2 = replacer.pickVictim();
        assertNotNull(victim2);
        assertEquals(2, victim2.getPageId());
    }

    @Test
    void allSlotsPinned_returnsNull() {
        BufferSlot slot1 = createBufferSlot(1, true, 0);
        BufferSlot slot2 = createBufferSlot(2, true, 0);
        replacer.push(slot1);
        replacer.push(slot2);
        assertNull(replacer.pickVictim());
    }

    @Test
    void slotBecomesUnpinned_canBeSelected() {
        BufferSlot slot = createBufferSlot(1, true, 0);
        replacer.push(slot);
        assertNull(replacer.pickVictim());
        slot.setPinned(false);
        replacer.push(slot);
        BufferSlot victim = replacer.pickVictim();
        assertNotNull(victim);
        assertEquals(1, victim.getPageId());
    }
}