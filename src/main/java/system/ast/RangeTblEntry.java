package system.ast;


public class RangeTblEntry {
    public String tableName;
    public String alias;
    public int index;

    public RangeTblEntry(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("tableName must not be null or empty");
        }
        this.tableName = tableName;
        this.alias = null;
        this.index = 0;
    }

    @Override
    public String toString() {
        return "RangeTblEntry{" +
                "tableName='" + tableName + '\'' +
                ", alias='" + alias + '\'' +
                ", index=" + index +
                '}';
    }
}
