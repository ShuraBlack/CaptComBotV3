package model.worker;

import com.shurablack.core.event.EventWorker;
import com.shurablack.core.util.AssetPool;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.util.Random;

public class CoinFlip extends EventWorker {

    @Override
    public void processGuildSlashEvent(Member member, MessageChannelUnion channel, String name, SlashCommandInteractionEvent event) {
        sendFeedBack(member.getUser(), event);
    }

    @Override
    public void processGlobalSlashEvent(User user, PrivateChannel channel, String name, SlashCommandInteractionEvent event) {
        sendFeedBack(user, event);
    }

    private void sendFeedBack(User user, SlashCommandInteractionEvent event) {
        Random rand = new Random();
        int rdmNum = rand.nextInt(2);

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.WHITE);
        eb.setTitle("Münze fällt für");
        eb.setDescription(String.format("%s auf:", user.getAsMention()));

        if(rdmNum == 1) {
            // HEAD
            eb.setThumbnail(AssetPool.get("url_coin_head"));
        } else {
            // TAIL
            eb.setThumbnail(AssetPool.get("url_coin_tail"));
        }
        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }

}
