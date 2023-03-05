package model.worker;

import com.shurablack.core.event.EventWorker;
import com.shurablack.core.util.ServerUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Clear extends EventWorker {

    @Override
    public void processPublicChannelEvent(Member member, MessageChannelUnion channel, String message, MessageReceivedEvent event) {
        if (!member.hasPermission(Permission.MANAGE_CHANNEL)) {
            return;
        }

        String[] args = message.split(" ");

        if (args.length == 1) {
            try {
                channel.purgeMessages(get(channel));
                channel.sendMessage("Alle verfügbaren Nachrichten wurden gelöscht!")
                        .complete().delete().queueAfter(3, TimeUnit.SECONDS);
            } catch (IllegalArgumentException e) {
                ServerUtil.GLOBAL_LOGGER.info(String.format("Couldnt delete requested Messages <%s,%s>", member.getEffectiveName(),channel.getId()));
            }
        } else if (args.length == 2 && args[1].equals("reset")) {
            if (channel.getType() != ChannelType.TEXT) {
                return;
            }

            TextChannel cpy = channel.asTextChannel();
            cpy.createCopy().setPosition(cpy.getPosition()).queue();
            channel.delete().queue();
        } else if (args.length == 2) {
            try {
                int amount = Integer.parseInt(args[1]);
                channel.purgeMessages(get(channel, amount));
                channel.sendMessage(String.format("%d Nachrichten wurden gelöscht!",amount))
                        .complete().delete().queueAfter(3, TimeUnit.SECONDS);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    /**
     * Create an List with the History of an channel
     * @param channel in which it got called
     * @return up to 100 entries in a List
     */
    private List<Message> get(MessageChannel channel) {
        List<Message> mes = new ArrayList<>();
        int i = 100;
        for (Message message : channel.getIterableHistory().cache(false)) {
            if(!message.isPinned()) {
                mes.add(message);
                if(--i <= 0) {
                    break;
                }
            }
        }
        return mes;
    }

    /**
     * Create an List with the History of an channel
     * @param channel in which it got called
     * @param amount of how many messages should be saved
     * @return List based on amount
     */
    private List<Message> get(MessageChannel channel, int amount) {
        List<Message> mes = new ArrayList<>();
        int i = amount;
        for (Message message : channel.getIterableHistory().cache(false)) {
            if(!message.isPinned()) {
                mes.add(message);
                if(--i <= 0) {
                    break;
                }
            }
        }
        return mes;
    }
}
