package model.worker;

import com.shurablack.core.event.EventWorker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;

public class Dice extends EventWorker {

    @Override
    public void processGuildSlashEvent(Member member, MessageChannelUnion channel, String name, SlashCommandInteractionEvent event) {
        sendFeedBack(member.getUser(), event);
    }

    @Override
    public void processGlobalSlashEvent(User user, PrivateChannel channel, String name, SlashCommandInteractionEvent event) {
        sendFeedBack(user, event);
    }

    private void sendFeedBack(User user, SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.WHITE);
        eb.setAuthor(user.getName(), user.getEffectiveAvatarUrl(), user.getEffectiveAvatarUrl());
        int dices = 1;
        int eyes = 6;
        for (OptionMapping o : event.getOptions()) {
            if (o.getName().equals("dices")) {
                dices = o.getAsInt();
                if (dices < 1) {
                    dices = 1;
                }
            }
            if (o.getName().equals("eyes")) {
                eyes = o.getAsInt();
                if (eyes < 2) {
                    eyes = 2;
                }
            }
        }
        if (dices == 1) {
            eb.setTitle("Würfel fällt auf:");
        } else {
            eb.setTitle("Würfel fallen auf:");
        }
        eb.setDescription(dice(dices,eyes));
        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }

    private String dice(int dices, int eyes) {
        StringBuilder s = new StringBuilder();

        for (int i = 0 ; i < dices ; i++) {
            s.append(ThreadLocalRandom.current().nextInt(1,eyes+1));
            if (i < dices-1) {
                s.append(", ");
            }
        }

        return s.toString();
    }

    private boolean isNonNumeric(String string) {
        try {
            Integer.parseInt(string);
            return false;
        } catch (NumberFormatException e) {
            return true;
        }
    }
}
