package model.listener;

import com.shurablack.sql.SQLRequest;
import model.database.Statement;
import model.database.models.UserTimeModel;
import model.manager.DiscordBot;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ActivListener extends ListenerAdapter {

    private final Map<String, LocalDateTime> joined = new HashMap<>();

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        if (event.getChannelJoined() != null && event.getChannelLeft() == null) {
            if (this.joined.containsKey(event.getMember().getId())) {
                return;
            }
            this.joined.put(event.getMember().getId(), LocalDateTime.now());
        } else if (event.getChannelJoined() == null && event.getChannelLeft() != null) {
            if (!this.joined.containsKey(event.getMember().getId())) {
                return;
            }
            LocalDateTime start = this.joined.remove(event.getMember().getId());
            LocalDateTime now = LocalDateTime.now();

            Duration timeInChanel = Duration.between(start,now);

            SQLRequest.Result<UserTimeModel> entry = SQLRequest.runSingle(DiscordBot.UTIL.getConnectionPool()
                    , Statement.SELECT_USER_TIME(event.getMember().getId()), UserTimeModel.class);

            if (entry.isPresent()) {
                SQLRequest.run(DiscordBot.UTIL.getConnectionPool()
                        , Statement.UPDATE_USER_TIME(event.getMember().getId(),timeInChanel.toSeconds()));
            } else {
                SQLRequest.run(DiscordBot.UTIL.getConnectionPool()
                        , Statement.INSERT_USER_TIME(event.getMember().getId(),timeInChanel.toSeconds()));
            }
        }

        super.onGuildVoiceUpdate(event);
    }
}
