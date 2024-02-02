package lk.eternal.ai.plugin;


import lk.eternal.ai.dto.req.Parameters;
import org.springframework.stereotype.Component;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//@Component
public class DbPlugin implements Plugin {

    public DbPlugin() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String name() {
        return "db";
    }

    @Override
    public String prompt() {
        return "A tool for executing database operations. This tool is already connected to a specified Postgresql database and can execute corresponding database operations by receiving SQL statements from users. With this tool, you can easily complete various database query and operation tasks.";
    }

    @Override
    public String description() {
        return "4.Postgresql数据库";
    }

    @Override
    public List<Prop> properties() {
        return List.of(new Prop("url", "url")
                ,new Prop("username", "username")
                ,new Prop("password", "password"));
    }

    @Override
    public Parameters parameters() {
        return Parameters.singleton("sql", "string", "sql语句");
    }

    @Override
    public String execute(Map<String, Object> args) {
        String exp = Optional.ofNullable(args.get("sql"))
                .orElseGet(() -> args.get("value"))
                .toString();
        try (final var connection = DriverManager.getConnection(args.get("url").toString(), args.get("username").toString(), args.get("password").toString());
             final var statement = connection.prepareStatement(exp)) {
            boolean isResultSet = statement.execute();  // 执行 SQL 语句

            String resp;
            if (isResultSet) {
                // 处理查询结果
                StringBuilder sb = new StringBuilder();
                ResultSet resultSet = statement.getResultSet();
                final var next = resultSet.next();
                if (!next) {
                    return "无数据";
                }
                // 添加表头
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    sb.append(metaData.getColumnName(i));
                    if (i < columnCount) {
                        sb.append(",");
                    }
                }
                sb.append("\n");

                do {
                    for (int i = 1; i <= columnCount; i++) {
                        sb.append(resultSet.getString(i));
                        if (i < columnCount) {
                            sb.append(",");
                        }
                    }
                    sb.append("\n");
                } while (resultSet.next());

                resp = sb.toString();
                resultSet.close();
            } else {
                // 处理更新结果
                int updateCount = statement.getUpdateCount();
                resp = "更新行数：" + updateCount;
            }
            return resp;
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
