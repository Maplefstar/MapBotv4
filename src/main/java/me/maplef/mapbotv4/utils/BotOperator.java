package me.maplef.mapbotv4.utils;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.managers.ConfigManager;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.network.LoginFailedException;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import top.mrxiaom.overflow.BotBuilder;


import java.util.Objects;

public class BotOperator {
    private static Bot bot;

    public static void login(String host, String token) throws LoginFailedException {
        ConfigManager configManager = new ConfigManager();
        FileConfiguration config = configManager.getConfig();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(GlobalEventChannel.class.getClassLoader());

        bot = BotBuilder.positive(host).token(token).connect();

        Thread.currentThread().setContextClassLoader(loader);
    }

    public static void sendGroupMessage(Long groupID, MessageChain message){
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try{
                Objects.requireNonNull(bot.getGroup(groupID)).sendMessage(message);
            } catch (NullPointerException e){
                Bukkit.getLogger().info(String.format("[%s] QQ账户正在登陆中，登陆期间的消息将不会转发", Main.getInstance().getDescription().getName()));
            } catch (IllegalStateException e){
                Bukkit.getServer().getLogger().severe(String.format("[%s] 发送消息失败，QQ账户可能被风控，请及时处理", Main.getInstance().getDescription().getName()));
            }
        });
    }

    public static void sendGroupMessage(Long groupID, String message){
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try{
                Objects.requireNonNull(bot.getGroup(groupID)).sendMessage(message);
            } catch (NullPointerException e){
                Bukkit.getLogger().info(String.format("[%s] QQ账户正在登陆中，登陆期间的消息将不会转发", Main.getInstance().getDescription().getName()));
            } catch (IllegalStateException e){
                Bukkit.getServer().getLogger().severe(String.format("[%s] 发送消息失败，QQ账户可能被风控，请及时处理", Main.getInstance().getDescription().getName()));
            }
        });
    }

    public static Bot getBot() {
        return bot;
    }
}

