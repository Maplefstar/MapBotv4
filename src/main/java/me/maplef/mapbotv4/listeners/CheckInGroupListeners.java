package me.maplef.mapbotv4.listeners;

import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.utils.BotOperator;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.MemberJoinEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.bukkit.configuration.file.FileConfiguration;

public class CheckInGroupListeners extends SimpleListenerHost {
    ConfigManager configManager = new ConfigManager();

//    @EventHandler
//    public void onJoinGroupRequest(MemberJoinRequestEvent e){
//        FileConfiguration config = configManager.getConfig();
//
//        long checkInGroup = config.getLong("check-in-group");
//
//        if(!config.getBoolean("check-in-group-auto-manage.enable")) return;
//        if(e.getGroupId() != checkInGroup) return;
//
//        if(config.getInt("check-in-group-auto-manage.minimum-QQ-level") > 0 ){
//            if(Mirai.getInstance().queryProfile(e.getBot(), e.getFromId()).getQLevel() < config.getInt("check-in-group-auto-manage.minimum-QQ-level"))
//                e.reject(false, "因QQ等级过低判定为小号，请使用大号入群");
//            else
//                e.accept();
//        } else {
//            e.accept();
//        }
//    }

    @EventHandler
    public void onNewCome(MemberJoinEvent e){
        FileConfiguration config = configManager.getConfig();
        FileConfiguration messages = configManager.getMessageConfig();

        long checkInGroup = config.getLong("check-in-group");

        if(!config.getBoolean("check-in-group-auto-manage.enable")) return;
        if(e.getGroupId() != checkInGroup) return;

        MessageChainBuilder newComeMsg = new MessageChainBuilder();
        newComeMsg.append(new At(e.getMember().getId())).append(" ").append(messages.getString("welcome-new-message.check-in-group.group"));

        BotOperator.sendGroupMessage(checkInGroup, newComeMsg.build());
    }
}
