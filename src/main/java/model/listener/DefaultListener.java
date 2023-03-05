package model.listener;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import com.shurablack.core.scheduling.TaskService;
import com.shurablack.core.util.LocalCache;
import com.shurablack.core.util.ServerUtil;
import model.game.event.EventManager;
import model.service.task.*;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.concurrent.ThreadLocalRandom;

public class DefaultListener extends ListenerAdapter {

    private static final Logger LOGGER = LogManager.getLogger(DefaultListener.class);

    @Override
    public void onGuildBan(@NotNull GuildBanEvent event) {
        User u = event.getUser();
        WebhookEmbedBuilder eb = new WebhookEmbedBuilder()
                .setAuthor(new WebhookEmbed.EmbedAuthor(u.getAsTag(), u.getEffectiveAvatarUrl(), null))
                .setDescription(u.getAsMention() + ", wurde des Servers verwiesen!")
                .setColor(ServerUtil.RED)
                .setTimestamp(OffsetDateTime.now());
        ServerUtil.sendWebHookMessage(LocalCache.getWebHookLink("lobby"),eb);
    }

    @Override
    public void onGuildUnban(@NotNull GuildUnbanEvent event) {
        User u = event.getUser();
        WebhookEmbedBuilder eb = new WebhookEmbedBuilder()
                .setAuthor(new WebhookEmbed.EmbedAuthor(u.getAsTag(), u.getEffectiveAvatarUrl(), null))
                .setDescription(u.getAsMention() + ", wurde eine zweite Chance ermöglicht!")
                .setColor(ServerUtil.BLUE)
                .setTimestamp(OffsetDateTime.now());
        ServerUtil.sendWebHookMessage(LocalCache.getWebHookLink("lobby"),eb);
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        User u = event.getUser();
        WebhookEmbedBuilder eb = new WebhookEmbedBuilder()
                .setAuthor(new WebhookEmbed.EmbedAuthor(u.getAsTag(), u.getEffectiveAvatarUrl(), null))
                .setDescription(joinMessage(u.getAsMention()))
                .setColor(ServerUtil.GREEN)
                .setTimestamp(OffsetDateTime.now());
        ServerUtil.sendWebHookMessage(LocalCache.getWebHookLink("lobby"),eb);
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        User u = event.getUser();
        WebhookEmbedBuilder eb = new WebhookEmbedBuilder()
                .setAuthor(new WebhookEmbed.EmbedAuthor(u.getAsTag(), u.getEffectiveAvatarUrl(), null))
                .setDescription(removeMessage(u.getAsMention()))
                .setColor(ServerUtil.RED)
                .setTimestamp(OffsetDateTime.now());
        ServerUtil.sendWebHookMessage(LocalCache.getWebHookLink("lobby"),eb);
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        LOGGER.debug("JDA announced to be ready");
        scheduleTasks();
        EventManager.load();
    }

    private void scheduleTasks() {
        TaskService.submitCronJob("0 1 * * 1-7","UpdateStatsTask" ,new UpdateStatsTask(LOGGER));
        TaskService.submitCronJob("0 1 * * 1-7","GameDailyTask" ,new GameDailyTask(LOGGER));
        TaskService.submitCronJob("0 2 * * 1-7", "RulerReenableUserTask", new RulerReenableUserTask(LOGGER));
        TaskService.submitCronJob("55 1 * * 1-7", "SubReenableUserTask", new SubReenableUserTask(LOGGER));
        TaskService.submitCronJob("00 13 * * Sun", "WeeklyGameEventTask", new WeeklyGameEventTask(LOGGER));
    }

    private String joinMessage(String mention) {
        String[] messages = {
                "{name} trat dem Server bei - glhf",
                "{name}, wir hoffen du hast Pizza dabei",
                "{name}, lass deine Waffen hier",
                "{name} erschien. Seems OP GGEZ - please nerf"
        };
        return messages[ThreadLocalRandom.current().nextInt(0, messages.length)].replace("{name}",mention);
    }

    private String removeMessage(String name) {
        String[] messages = {
                "{name}. Mission failed. We will get them next time",
                "{name}. Another one bits the dust",
                "{name} auf wiedersehen",
                "{name}. Got nerfed and removed"
        };
        return messages[ThreadLocalRandom.current().nextInt(0, messages.length)].replace("{name}",name);
    }
}
