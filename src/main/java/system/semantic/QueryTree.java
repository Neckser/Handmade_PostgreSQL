package system.semantic;

import system.catalog.model.ColumnDefinition;

import java.util.List;
import java.util.Map;

public class QueryTree {
    private final String queryType;
    private final List<ResolvedColumn> targetColumns;
    private final List<String> fromTables;
    private final Filter filter;

    private final String tableName;
    private final List<ColumnDefinition> createColumns;
    private final Map<ResolvedColumn, Object> setValues;

    public QueryTree(String queryType, String tableName, List<ColumnDefinition> cols) {
        this.queryType = queryType;
        this.tableName = tableName;
        this.createColumns = cols;
        this.targetColumns = null;
        this.fromTables = null;
        this.filter = null;
        this.setValues = null;
    }

    public QueryTree(String queryType, String tableName, Map<ResolvedColumn, Object> setValues, Filter filter) {
        this.queryType = queryType;
        this.tableName = tableName;
        this.setValues = setValues;
        this.filter = filter;
        this.targetColumns = null;
        this.fromTables = null;
        this.createColumns = null;
    }


    public QueryTree(List<ResolvedColumn> targetColumns, List<String> fromTables, Filter filter) {
        this.queryType = "SELECT";
        this.targetColumns = targetColumns;
        this.fromTables = fromTables;
        this.filter = filter;
        this.tableName = null;
        this.createColumns = null;
        this.setValues = null;
    }

    public List<ResolvedColumn> getTargetList() { return targetColumns; }
    public List<String> getFromTables() { return fromTables; }
    public Filter getFilter() { return filter; }
    public String getQueryType() { return queryType; }

    public String getTableName(){ return tableName; }
    public List<ColumnDefinition> getCreateColumns() { return createColumns; }
    public Map<ResolvedColumn, Object> getSetValues() { return setValues; }


@Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("QueryTree(")
                .append("type=").append(queryType);

        switch (queryType) {
            case "SELECT" -> {
                sb.append(", targets=").append(targetColumns);
                sb.append(", from=").append(fromTables);
                if (filter != null) sb.append(", where=").append(filter);
            }

            case "CREATE" -> {
                sb.append(", table=").append(tableName);
                sb.append(", columns=").append(createColumns);
            }

            case "UPDATE" -> {
                sb.append(", table=").append(tableName);
                sb.append(", set=").append(setValues);
                if (filter != null) sb.append(", where=").append(filter);
            }

            default -> sb.append(", unknown query type");
        }

        sb.append(")");
        return sb.toString();
    }


}
