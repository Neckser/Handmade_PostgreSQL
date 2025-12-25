package buffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import system.manager.HeapPageFileManager;
import system.model.BufferSlot;
import system.page.HeapPage;
import system.buffer.DefaultBufferPoolManager;
import system.replacer.LRUReplacer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultBufferPoolManagerTest {

    @TempDir
    Path tempDir;

    private DefaultBufferPoolManager bufferPool;
    private Path testPath;

    @BeforeEach
    void setUp() {
        testPath = tempDir.resolve("test.heap");
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        LRUReplacer replacer = new LRUReplacer(new java.util.LinkedList<>(), new HashMap<>());
        bufferPool = new DefaultBufferPoolManager(3, pageFileManager, replacer);
    }

    @Test
    void getPage_returnsBufferSlot() throws IOException {
        BufferSlot result = bufferPool.getPage(1);
        assertNotNull(result);
        assertEquals(1, result.getPageId());
    }

    @Test
    void updatePage_marksPageAsDirty() throws IOException {
        bufferPool.updatePage(1, new HeapPage(1));
        List<BufferSlot> dirtyPages = bufferPool.getDirtyPages();
        assertEquals(1, dirtyPages.size());
        assertTrue(dirtyPages.get(0).isDirty());
    }

    @Test
    void pinPage_setsPinnedFlag() throws IOException {
        bufferPool.updatePage(1, new HeapPage(1));
        bufferPool.pinPage(1);
        assertDoesNotThrow(() -> bufferPool.getPage(1));
    }

    @Test
    void pinPage_nonExistentPage_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> bufferPool.pinPage(999));
    }

    @Test
    void unpinPage_clearsPinnedFlag() throws IOException {
        bufferPool.updatePage(1, new HeapPage(1));
        bufferPool.pinPage(1);
        bufferPool.unpinPage(1);
        assertDoesNotThrow(() -> bufferPool.getPage(1));
    }

    @Test
    void unpinPage_nonExistentPage_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> bufferPool.unpinPage(999));
    }

    @Test
    void flushPage_clearsDirtyFlag() throws IOException {
        bufferPool.updatePage(1, new HeapPage(1));
        bufferPool.flushPage(1);
        List<BufferSlot> dirtyPages = bufferPool.getDirtyPages();
        assertTrue(dirtyPages.isEmpty());
    }

    @Test
    void flushPage_nonExistentPage_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> bufferPool.flushPage(999));
    }

    @Test
    void flushAllPages_clearsAllDirtyFlags() throws IOException {
        bufferPool.updatePage(1, new HeapPage(1));
        bufferPool.updatePage(2, new HeapPage(2));
        bufferPool.flushAllPages();
        List<BufferSlot> dirtyPages = bufferPool.getDirtyPages();
        assertTrue(dirtyPages.isEmpty());
    }

    @Test
    void getDirtyPages_returnsOnlyDirtyPages() throws IOException {
        bufferPool.updatePage(1, new HeapPage(1));
        bufferPool.updatePage(2, new HeapPage(2));
        bufferPool.flushPage(1);
        List<BufferSlot> dirtyPages = bufferPool.getDirtyPages();
        assertEquals(1, dirtyPages.size());
        assertEquals(2, dirtyPages.get(0).getPageId());
    }

}