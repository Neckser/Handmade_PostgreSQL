package system.memory.manager;

import system.memory.page.Page;

import java.nio.file.Path;

public interface PageFileManager {
    void write(Page page, Path path);

    Page read(int pageId, Path path);
}
