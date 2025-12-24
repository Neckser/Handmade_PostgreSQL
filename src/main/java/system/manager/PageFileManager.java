package system.manager;

import system.page.Page;

import java.nio.file.Path;

public interface PageFileManager {
    void write(Page page, Path path);

    Page read(int pageId, Path path);
}
