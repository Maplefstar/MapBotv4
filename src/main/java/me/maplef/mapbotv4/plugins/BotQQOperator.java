package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.InvalidSyntaxException;
import me.maplef.mapbotv4.exceptions.NoPermissionException;
import me.maplef.mapbotv4.listeners.CheckInGroupListeners;
import me.maplef.mapbotv4.listeners.PlayerGroupListeners;
import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.utils.BotOperator;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.QuoteReply;
import net.mamoe.mirai.network.LoginFailedException;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class BotQQOperator implements MapbotPlugin {
    static final FileConfiguration config = new ConfigManager().getConfig();
    static final FileConfiguration messageConfig = new ConfigManager().getMessageConfig();
    static final Logger logger = Main.getInstance().getLogger();

    public static final Long opGroup = config.getLong("op-group");
    public static final Long playerGroup = config.getLong("player-group");
    public static final String wsHost = config.getString("ws-host");
    public static final String wsToken = config.getString("ws-token");

    public static void login(){
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            Bukkit.getServer().getLogger().info(String.format("[%s] Mapbot正在登陆，请耐心等待...", Main.getInstance().getPluginMeta().getName()));
            try{
                BotOperator.login(wsHost, wsToken);
                BotOperator.getBot().getEventChannel().registerListenerHost(new PlayerGroupListeners());
                BotOperator.getBot().getEventChannel().registerListenerHost(new CheckInGroupListeners());
                if(!messageConfig.getString("enable-message.op-group", "").isEmpty())
                    BotOperator.sendGroupMessage(opGroup, messageConfig.getString("enable-message.op-group"));
                if(!messageConfig.getString("enable-message.player-group", "").isEmpty())
                    BotOperator.sendGroupMessage(playerGroup, messageConfig.getString("enable-message.player-group"));
                logger.info(String.format("QQ账号 %d 登陆成功", BotOperator.getBot().getId()));
            } catch (LoginFailedException e){
                logger.severe(String.format("QQ账号 %d 登陆失败：%s", BotOperator.getBot().getId(), e.getMessage()));
            }
        });
    }

    public static void logout(){
        if(!messageConfig.getString("disable-message.op-group", "").isEmpty())
            Objects.requireNonNull(BotOperator.getBot().getGroup(opGroup)).sendMessage(Objects.requireNonNull(messageConfig.getString("disable-message.op-group")));
        if(!messageConfig.getString("disable-message.player-group", "").isEmpty())
            Objects.requireNonNull(BotOperator.getBot().getGroup(playerGroup)).sendMessage(Objects.requireNonNull(messageConfig.getString("disable-message.player-group")));

        BotOperator.logout();
        logger.info(String.format("QQ账号 %d 已退出登陆", BotOperator.getBot().getId()));
    }

    public static void reLogin() {
        logout();
        login();
    }

    @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception {
        if(args.length == 0)
            throw new InvalidSyntaxException();
        if(!Objects.requireNonNull(BotOperator.getBot().getGroup(opGroup)).contains(senderID)) throw new NoPermissionException();

        if(args[0].contentToString().equals("login")) login();
        else if(args[0].contentToString().equals("logout")) logout();
        else if(args[0].contentToString().equals("relogin")) reLogin();
        else throw new IllegalArgumentException("未知的参数: " + args[0]);

        return null;
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException {
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("bot", BotQQOperator.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));

        usages.put("bot", "#bot - 操作bot账号");

        info.put("name", "BotQQOperator");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "操作bot账号");
        info.put("version", "1.1");

        return info;
    }
}
