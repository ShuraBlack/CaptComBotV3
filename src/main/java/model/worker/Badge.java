package model.worker;

import com.shurablack.core.event.EventWorker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.time.OffsetDateTime;

public class Badge extends EventWorker {

    @Override
    public void processGlobalSlashEvent(User user, PrivateChannel channel, String name, SlashCommandInteractionEvent event) {
        sendFeedBack(event);
    }

    @Override
    public void processGuildSlashEvent(Member member, MessageChannelUnion channel, String name, SlashCommandInteractionEvent event) {
        sendFeedBack(event);
    }

    private void sendFeedBack(SlashCommandInteractionEvent event) {
        OffsetDateTime now = OffsetDateTime.now();
        EmbedBuilder eb = new EmbedBuilder()
                .setDescription("Danke für das Nutzen des Badge Commands")
                .setFooter("Nächstes Datum")
                .setTimestamp(now.plusDays(29));
        event.replyEmbeds(eb.build()).queue();
    }
}
