package system.execution.executors;

import system.ast.AConst;
import system.ast.Expr;
import system.catalog.model.TableDefinition;
import system.memory.buffer.BufferPoolManager;
import system.memory.manager.PageFileManager;
import system.memory.page.HeapPage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.List;

public class InsertExecutor implements Executor {

    private final TableDefinition tableDefinition;
    private final List<Expr> values;

    // ✅ минимум зависимостей для записи HeapPage
    private final PageFileManager pageFileManager;
    private final BufferPoolManager bufferPool;

    private boolean done = false;

    public InsertExecutor(PageFileManager pageFileManager,
                          BufferPoolManager bufferPool,
                          TableDefinition tableDefinition,
                          List<Expr> values) {
        this.pageFileManager = pageFileManager;
        this.bufferPool = bufferPool;
        this.tableDefinition = tableDefinition;
        this.values = values;
    }

    @Override
    public void open() { }

    @Override
    public Object next() {
        if (done) return null;
        done = true;

        List<Object> rowValues = values.stream()
                .map(expr -> ((AConst) expr).value)
                .toList();

        byte[] tuple = serializeRow(rowValues);

        Path file = Path.of(tableDefinition.getFileNode()).toAbsolutePath();
        int pageId = 0;

        HeapPage page;
        try {
            page = (HeapPage) pageFileManager.read(pageId, file);
        } catch (Exception e) {

            page = new HeapPage(pageId);
        }

        page.write(tuple);

        pageFileManager.write(page, file);


        return null;
    }

    @Override
    public void close() { }

    private byte[] serializeRow(List<Object> values) {
        ByteBuffer buf = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);

        for (Object v : values) {
            if (v == null) {
                buf.put((byte) 0); // null tag
            } else if (v instanceof Integer i) {
                buf.put((byte) 1);
                buf.putInt(i);
            } else if (v instanceof Long l) {
                buf.put((byte) 2);
                buf.putLong(l);
            } else if (v instanceof Boolean b) {
                buf.put((byte) 3);
                buf.put((byte) (b ? 1 : 0));
            } else if (v instanceof String s) {
                byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                buf.put((byte) 4);
                buf.putShort((short) bytes.length);
                buf.put(bytes);
            } else if (v instanceof Number n) {
                buf.put((byte) 2);
                buf.putLong(n.longValue());
            } else {
                throw new IllegalArgumentException("Unsupported value type: " + v.getClass());
            }
        }

        byte[] out = new byte[buf.position()];
        System.arraycopy(buf.array(), 0, out, 0, buf.position());
        return out;
    }
}
