package system.execution.executors;

import system.catalog.model.TableDefinition;
import system.execution.tuple.TupleCodec;
import system.memory.buffer.BufferPoolManager;
import system.memory.page.HeapPage;

public class SeqScanExecutor implements Executor {
    private final BufferPoolManager bufferPool;
    private final String tableName;
    private int currentPageId;
    private int currentRowIndex;
    private boolean isOpen;

    public SeqScanExecutor(BufferPoolManager bufferPool, TableDefinition tableDefinition) {
        this.bufferPool = bufferPool;
        this.tableName = tableDefinition.getName();
    }

    @Override
    public void open() {
        currentPageId = 0;
        currentRowIndex = 0;
        isOpen = true;
    }

    @Override
    public Object next() {
        if (!isOpen) return null;

        while (true) {
            var bufferSlot = (system.memory.model.BufferSlot) null;
            try {
                bufferSlot = bufferPool.getPage(currentPageId);
            } catch (Exception e) {
                return null;
            }

            if (bufferSlot == null) return null;

            HeapPage page = (HeapPage) bufferSlot.getPage();


            if (currentRowIndex < page.size()) {
                byte[] rowData = page.read(currentRowIndex++);
                return TupleCodec.decodeTagged(rowData);

            } else {
                currentPageId++;
                currentRowIndex = 0;
            }
        }
    }

    @Override
    public void close() {
        isOpen = false;
        currentPageId = 0;
        currentRowIndex = 0;
    }
}