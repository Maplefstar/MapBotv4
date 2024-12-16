package me.maplef.mapbotv4.utils;

import me.maplef.mapbotv4.Main;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.network.LoginFailedException;
import org.bukkit.Bukkit;
import top.mrxiaom.overflow.BotBuilder;


import java.util.Objects;
import java.util.logging.Logger;

public class BotOperator {
    public static final Logger logger = Main.getInstance().getLogger();

    private static Bot bot;

    public synchronized static void login(String host, String token) throws LoginFailedException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(GlobalEventChannel.class.getClassLoader());

        bot = BotBuilder.positive(host).token(token).connect();

        Thread.currentThread().setContextClassLoader(loader);
    }

    public synchronized static void logout() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(GlobalEventChannel.class.getClassLoader());

        bot.close();

        Thread.currentThread().setContextClassLoader(loader);
    }

    public static void sendGroupMessage(Long groupID, MessageChain message){
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try{
                Objects.requireNonNull(bot.getGroup(groupID)).sendMessage(message);
            } catch (NullPointerException e){
                logger.info("QQ账户正在登陆中，登陆期间的消息将不会转发");
            } catch (IllegalStateException e){
                logger.severe("发送消息失败，QQ账户可能被风控，请及时处理");
            }
        });
    }

    public static void sendGroupMessage(Long groupID, String message){
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try{
                Objects.requireNonNull(bot.getGroup(groupID)).sendMessage(message);
            } catch (NullPointerException e){
                logger.info("QQ账户正在登陆中，登陆期间的消息将不会转发");
            } catch (IllegalStateException e){
                logger.severe("发送消息失败，QQ账户可能被风控，请及时处理");
            }
        });
    }

    public static Bot getBot() {
        return bot;
    }
}

