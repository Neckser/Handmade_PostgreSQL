package replacer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import system.model.BufferSlot;
import system.page.HeapPage;
import system.replacer.LRUReplacer;

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;

class LRUReplacerTest {

    private Map<Integer, BufferSlot> buffer;
    private LRUReplacer replacer;
    private LinkedList<BufferSlot> lruList;

    @BeforeEach
    void setUp() {
        buffer = new HashMap<>();
        lruList = new LinkedList<>();
        replacer = new LRUReplacer(lruList, buffer);
    }

    private BufferSlot createBufferSlot(int pageId, boolean pinned) {
        HeapPage page = new HeapPage(pageId);
        BufferSlot slot = new BufferSlot(pageId, page);
        slot.setPinned(pinned);
        return slot;
    }

    @Test
    void pickVictim_emptyBuffer_returnsNull() {
        assertNull(replacer.pickVictim());
    }

    @Test
    void push_unpinnedSlot_addsToReplacer() {
        BufferSlot slot = createBufferSlot(1, false);
        buffer.put(1, slot);
        replacer.push(slot);
        BufferSlot victim = replacer.pickVictim();
        assertNotNull(victim);
        assertEquals(1, victim.getPageId());
    }

    @Test
    void push_pinnedSlot_doesNotAddToReplacer() {
        BufferSlot slot = createBufferSlot(1, true);
        buffer.put(1, slot);
        replacer.push(slot);
        BufferSlot victim = replacer.pickVictim();
        assertNull(victim);
    }

    @Test
    void push_existingSlot_movesToEnd() {
        BufferSlot slot1 = createBufferSlot(1, false);
        BufferSlot slot2 = createBufferSlot(2, false);
        buffer.put(1, slot1);
        buffer.put(2, slot2);
        replacer.push(slot1);
        replacer.push(slot2);
        replacer.push(slot1);
        BufferSlot victim1 = replacer.pickVictim();
        assertNotNull(victim1);
        assertEquals(2, victim1.getPageId());
        BufferSlot victim2 = replacer.pickVictim();
        assertNotNull(victim2);
        assertEquals(1, victim2.getPageId());
    }

    @Test
    void delete_existingSlot_removesFromReplacer() {
        BufferSlot slot1 = createBufferSlot(1, false);
        BufferSlot slot2 = createBufferSlot(2, false);
        buffer.put(1, slot1);
        buffer.put(2, slot2);
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
        BufferSlot slot = createBufferSlot(1, false);
        buffer.put(1, slot);
        replacer.push(slot);
        replacer.delete(999);
        BufferSlot victim = replacer.pickVictim();
        assertNotNull(victim);
        assertEquals(1, victim.getPageId());
    }

    @Test
    void integration_multipleOperations_correctOrder() {
        BufferSlot[] slots = new BufferSlot[5];
        for (int i = 1; i <= 5; i++) {
            slots[i-1] = createBufferSlot(i, false);
            buffer.put(i, slots[i-1]);
        }
        for (BufferSlot slot : slots) {
            replacer.push(slot);
        }
        replacer.push(slots[2]);
        replacer.push(slots[0]);
        assertEquals(2, replacer.pickVictim().getPageId());
        assertEquals(4, replacer.pickVictim().getPageId());
        assertEquals(5, replacer.pickVictim().getPageId());
        assertEquals(3, replacer.pickVictim().getPageId());
        assertEquals(1, replacer.pickVictim().getPageId());
        assertNull(replacer.pickVictim());
    }

    @Test
    void mixedPinnedAndUnpinnedSlots() {
        BufferSlot pinned1 = createBufferSlot(1, true);
        BufferSlot unpinned1 = createBufferSlot(2, false);
        BufferSlot pinned2 = createBufferSlot(3, true);
        BufferSlot unpinned2 = createBufferSlot(4, false);
        buffer.put(1, pinned1);
        buffer.put(2, unpinned1);
        buffer.put(3, pinned2);
        buffer.put(4, unpinned2);
        replacer.push(pinned1);
        replacer.push(unpinned1);
        replacer.push(pinned2);
        replacer.push(unpinned2);
        BufferSlot victim1 = replacer.pickVictim();
        assertNotNull(victim1);
        assertEquals(2, victim1.getPageId());
        BufferSlot victim2 = replacer.pickVictim();
        assertNotNull(victim2);
        assertEquals(4, victim2.getPageId());
        assertNull(replacer.pickVictim());
    }
}