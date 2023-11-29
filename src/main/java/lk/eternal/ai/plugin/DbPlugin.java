package lk.eternal.ai.plugin;


import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class DbPlugin implements Plugin {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public DbPlugin() throws ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        dbUrl = System.getProperty("db.url");
        dbUser = System.getProperty("db.username");
        dbPassword = System.getProperty("db.password");
    }

    @Override
    public String name() {
        return "db";
    }

    @Override
    public String description() {
        return "执行数据库的工具(默认已经连接上数据库),参数是需要执行的sql语句,数据库类型是Postgresql";
    }

    public String execute(String sql) {
        try (final var connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             final var statement = connection.prepareStatement(sql)) {
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