package model.listener;

import com.shurablack.core.mapping.MultiKeyMap;
import com.shurablack.core.util.LocalCache;
import model.manager.DiscordBot;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class MessageListener extends ListenerAdapter {

    private final MultiKeyMap<String,String> mapper = new MultiKeyMap<>();

    public MessageListener() {
        mapper.put(LocalCache.getChannelID("music") + "/" + LocalCache.getMessageID("player_main"), "player");
        mapper.put(LocalCache.getChannelID("blackjack") + "/" + LocalCache.getMessageID("blackjack_board"), "bj");
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (DiscordBot.MUTE) {
            return;
        }

        String message = event.getMessage().getContentDisplay();
        String command = message.split(" ")[0];
        if (event.isFromGuild()) {
            DiscordBot.UTIL.getHandler().onPublicChannelEvent(command, event);
        } else {
            DiscordBot.UTIL.getHandler().onPrivateChannelEvent(command, event);
        }
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (DiscordBot.MUTE) {
            return;
        }

        if (event.isFromGuild()) {
            DiscordBot.UTIL.getHandler().onPublicReactionEvent(mapper.get(event.getChannel().getId() + "/" + event.getMessageId()), event);
        } else {
            DiscordBot.UTIL.getHandler().onPrivateReactionEvent(mapper.get(event.getChannel().getId() + "/" + event.getMessageId()), event);
        }
    }
}
