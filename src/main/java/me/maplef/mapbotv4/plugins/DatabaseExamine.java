package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.InvalidSyntaxException;
import me.maplef.mapbotv4.exceptions.NoPermissionException;
import me.maplef.mapbotv4.managers.ConfigManager;
import net.mamoe.mirai.message.data.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class DatabaseExamine implements MapbotPlugin {

    ConfigManager configManager = new ConfigManager();
    FileConfiguration config = configManager.getConfig();

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";

    final String MYSQL_HOST = config.getString("ex-host");
    final String PORT = config.getString("ex-port");
    final String DB_NAME = config.getString("ex-database");
    final String DB_URL = "jdbc:mysql://" + MYSQL_HOST + ":" + PORT + "/" + DB_NAME;

    final String USERNAME = config.getString("ex-username");
    final String PASSWORD = config.getString("ex-password");

    private final Connection c = connect();

    private Connection connect() {
        Connection conn = null;
        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

    @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception {
        ConfigManager configManager = new ConfigManager();
        FileConfiguration config = configManager.getConfig();
        if (!config.getLongList("super-admin-account").contains(senderID)) throw new NoPermissionException();

        if (args[0].contentToString().equals("get")) {
            // 获取指定qq的examine表中的数据
            long qq = Long.parseLong(args[1].contentToString());
            Map<String, Object> queryRes = new HashMap<>();

            try (PreparedStatement pstmt = c.prepareStatement("SELECT * FROM examine WHERE qq = ?")) {
                pstmt.setLong(1, qq);
                try (ResultSet rs = pstmt.executeQuery()) {
                    boolean hasData = rs.next();
                    if (!hasData) {
                        return MessageUtils.newChain(new At(senderID)).plus(" " + "没有查到相关数据");
                    }

                    boolean flag = false;
                    do {
                        ResultSetMetaData data = rs.getMetaData();
                        for (int i = 1; i <= data.getColumnCount(); ++i) {
                            queryRes.put(data.getColumnName(i), rs.getObject(data.getColumnName(i)));
                        }

                        if ((boolean) queryRes.get("passed")) {
                            flag = true;
                            break;
                        }
                        queryRes.clear();
                    } while (rs.next());

                    if (flag) {
                        // 通过，则查询invitation_code表中的信息
                        String uuid = String.valueOf(queryRes.get("uuid"));
                        try (Statement stmt2 = c.createStatement(); ResultSet rs2 = stmt2.executeQuery("SELECT * FROM invitation_code WHERE uuid = '%s';".formatted(uuid))) {
                            Map<String, Object> queryRes2 = new HashMap<>();
                            if (rs2.next()) {
                                ResultSetMetaData data = rs2.getMetaData();
                                for (int i = 1; i <= data.getColumnCount(); ++i) {
                                    queryRes2.put(data.getColumnName(i), rs2.getObject(data.getColumnName(i)));
                                }
                            } else {
                                return MessageUtils.newChain(new At(senderID)).plus(" " + "该玩家已通过审核，但没有在invitation_code表中查到相关数据");
                            }

                            // 获取examine表中的数据
                            String gid = String.valueOf(queryRes.get("gid"));
                            String email = String.valueOf(queryRes.get("email"));

                            // 获取invitation_code表中的数据
                            String code = String.valueOf(queryRes2.get("invitation_code"));
                            boolean used = (Boolean) queryRes2.get("used");
                            String used_time = String.valueOf(queryRes2.get("used_time"));

                            return MessageUtils.newChain(new At(senderID)).plus(" " + "QQ为%d的用户信息：\n邀请码：%s\n邮箱：%s\n游戏id：%s\n邀请码是否使用：%s\n邀请码使用时间：%s\n数据uuid：%s".formatted(qq, code, email, gid, used, used_time, uuid));
                        }
                    } else {
                        // 不通过
                        // 获取examine表中的数据
                        String gid = String.valueOf(queryRes.get("gid"));
                        String email = String.valueOf(queryRes.get("email"));
                        String refuse_reason = String.valueOf(queryRes.get("refuse_reason"));
                        String uuid = String.valueOf(queryRes.get("uuid"));

                        return MessageUtils.newChain(new At(senderID)).plus(" " + "QQ为%d的用户信息：\n该用户审核未通过，原因为：\n%s\n游戏id：%s\n邮箱：%s\n数据uuid：%s".formatted(qq, refuse_reason, gid, email, uuid));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return MessageUtils.newChain(new At(senderID)).plus(" " + "出现异常，请查看控制台报错信息");
            }
        } else {
            throw new InvalidSyntaxException();
        }
    }


    @Override
    public Map<String, Object> register() throws NoSuchMethodException {
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("database", DatabaseExamine.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));
        commands.put("数据库", DatabaseExamine.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));

        usages.put("database", "#database <get> <QQ号> - 查询指定玩家的审核数据库信息");
        usages.put("数据库", "#数据库 <get> <QQ号> - 查询指定玩家的审核数据库信息");

        info.put("name", "Database");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "LQ_Snow");
        info.put("description", "查询指定玩家的审核数据库信息");
        info.put("version", "1.0");

        return info;
    }
}
