package me.maplef.mapbotv4.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.GroupNotAllowedException;
import me.maplef.mapbotv4.exceptions.InvalidSyntaxException;
import me.maplef.mapbotv4.exceptions.NoPermissionException;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import me.maplef.mapbotv4.utils.HttpUtils;
import net.kyori.adventure.text.Component;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.message.data.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class BindIDAndQQ implements MapbotPlugin {
    ConfigManager configManager = new ConfigManager();
    FileConfiguration config = configManager.getConfig();
    private final Long opGroup = config.getLong("op-group");
    private final Long playerGroup = config.getLong("player-group");

    Bot bot = BotOperator.getBot();

    public MessageChain bind(Long groupID, Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception{
        if(!Objects.equals(groupID, playerGroup)) throw new GroupNotAllowedException();
        if(args.length < 1) throw new InvalidSyntaxException();

        String pattern = "^\\w{3,20}$";
        if(!Pattern.matches(pattern, args[0].contentToString())) throw new Exception("非法的ID名");

        try{
            DatabaseOperator.queryPlayer(senderID);
            throw new Exception("你已经绑定id了，如需更改请联系管理组");
        } catch (PlayerNotFoundException ignored){}

        String fixedUUID, MCID;
        if(config.getBoolean("bind-id-and-qq.online-mode")){
            String mojangUrl = "https://api.mojang.com/users/profiles/minecraft/";
            String url = mojangUrl.concat(args[0].contentToString());
            String jsonString;
            try{
                jsonString = HttpUtils.doGet(url);
            } catch (Exception exception){
                throw new Exception("向mojang服务器请求认证用户名失败，请稍后重试");
            }
            
            JSONObject jsonRes = JSON.parseObject(jsonString);
            if(jsonRes == null) throw new Exception("向mojang服务器请求认证用户名失败，请稍后重试");
            if(jsonRes.containsKey("errorMessage")) throw new Exception("该ID非正版ID，请检查输入是否正确");

            StringBuilder fixedUUIDBuilder = new StringBuilder().append(jsonRes.getString("id"));
            fixedUUIDBuilder.insert(8, '-'); fixedUUIDBuilder.insert(13, '-');
            fixedUUIDBuilder.insert(18, '-'); fixedUUIDBuilder.insert(23, '-');

            MCID = jsonRes.getString("name");
            fixedUUID = fixedUUIDBuilder.toString();
        } else {
            MCID = args[0].contentToString(); fixedUUID = "";
        }

        String order = String.format("INSERT INTO PLAYER (NAME, QQ, UUID, MSGREC) VALUES ('%s', '%s', '%s', 1);", MCID, senderID, fixedUUID);
        new DatabaseOperator().executeCommand(order);

        if(config.getBoolean("bind-id-and-qq.whitelist")){
            String whitelistAddCommand = String.format("whitelist add %s", MCID);
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), whitelistAddCommand);
            });
        }

        if(config.getBoolean("bind-id-and-qq.modify-namecard")){
            if(Objects.requireNonNull(bot.getGroup(playerGroup)).getBotPermission().compareTo(MemberPermission.ADMINISTRATOR) >= 0)
                Objects.requireNonNull(Objects.requireNonNull(bot.getGroup(playerGroup)).get(senderID)).setNameCard(MCID);
            else
                BotOperator.sendGroupMessage(playerGroup, "修改群名片需要群管理员权限，请给bot设置管理员权限");
        }

        if(config.getBoolean("bind-id-and-qq.whitelist"))
            return MessageUtils.newChain(new At(senderID)).plus(new PlainText(" 绑定ID成功，白名单已添加"));
        else
            return MessageUtils.newChain(new At(senderID)).plus(new PlainText(" 绑定ID成功"));
    }

    public MessageChain update(Long groupID, Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws PlayerNotFoundException, SQLException {
        if(!config.getBoolean("bind-id-and-qq.online-mode")){
            return MessageUtils.newChain(new At(senderID), new PlainText(" 目前使用离线模式，无法与 mojang 服务器通信更新id，请联系管理员手动更新"));
        }

        String mojangURL = "https://sessionserver.mojang.com/session/minecraft/profile/";

        String UUID = (String) DatabaseOperator.queryPlayer(senderID).get("UUID");
        String jsonString = HttpUtils.doGet(mojangURL.concat(UUID));
        JSONObject jsonRes = JSON.parseObject(jsonString);
        String nowName = jsonRes.getString("name");
        new DatabaseOperator().executeCommand(String.format("UPDATE PLAYER SET NAME = '%s' WHERE UUID = '%s';", nowName, UUID));
        return new MessageChainBuilder().append(new At(senderID)).append(" 你的id已被更新为: ").append(nowName).build();
    }

    public MessageChain unbind(Long groupID, Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception{
        if(!Objects.requireNonNull(bot.getGroup(opGroup)).contains(senderID)) throw new NoPermissionException();
        if(args.length < 1) throw new InvalidSyntaxException();

        String name = args[0].contentToString();

        String fixedName = (String) DatabaseOperator.queryPlayer(name).get("NAME");
        long targetQQ = Long.parseLong(DatabaseOperator.queryPlayer(name).get("QQ").toString());

        if(!Objects.requireNonNull(config.getLongList("super-admin-account")).contains(senderID) &&
            Objects.requireNonNull(bot.getGroup(opGroup)).contains(targetQQ))
            throw new Exception("你没有权限解绑一位op的ID");

        new DatabaseOperator().executeCommand(String.format("DELETE FROM PLAYER WHERE NAME = '%s';", fixedName));
        String whitelistDelCommand = String.format("whitelist remove %s", fixedName);
        new BukkitRunnable(){
            @Override
            public void run(){
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), whitelistDelCommand);
            }
        }.runTask(Main.getPlugin(Main.class));

        try{
            Objects.requireNonNull(Bukkit.getServer().getPlayer(fixedName)).kick(Component.text("你的ID绑定已解除"), PlayerKickEvent.Cause.WHITELIST);
        } catch (NullPointerException ignored){}

        return new MessageChainBuilder().append(new At(senderID)).append(" 解除了 ").append(fixedName).append(" 的ID绑定，白名单已移除").build();
    }

    @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception {
        return null;
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("id", BindIDAndQQ.class.getMethod("bind", Long.class, Long.class, Message[].class, QuoteReply.class));
        commands.put("绑定", BindIDAndQQ.class.getMethod("bind", Long.class, Long.class, Message[].class, QuoteReply.class));
        commands.put("updateid", BindIDAndQQ.class.getMethod("update", Long.class, Long.class, Message[].class, QuoteReply.class));
        commands.put("更新id", BindIDAndQQ.class.getMethod("update", Long.class, Long.class, Message[].class, QuoteReply.class));
        commands.put("deleteid", BindIDAndQQ.class.getMethod("unbind", Long.class, Long.class, Message[].class, QuoteReply.class));
        commands.put("解绑", BindIDAndQQ.class.getMethod("unbind", Long.class, Long.class, Message[].class, QuoteReply.class));

        usages.put("id", "#id <游戏ID> - 绑定ID");
        usages.put("绑定", "#绑定 <游戏ID> - 绑定ID");
        usages.put("updateid", "#updateid - 更新游戏ID");
        usages.put("更新id", "#更新id - 更新游戏ID");
        usages.put("deleteid", "#deleteid <玩家ID> - 解除一位玩家的ID绑定，并将此ID移出白名单");
        usages.put("解绑", "#解绑 <玩家ID> - 解除一位玩家的ID绑定，并将此ID移出白名单");

        info.put("name", "BindIDAndQQ");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "进行绑定id的相关操作");
        info.put("version", "1.3");

        return info;
    }
}
