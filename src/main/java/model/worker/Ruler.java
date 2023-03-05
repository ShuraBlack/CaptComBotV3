package model.worker;

import com.shurablack.core.event.EventWorker;
import com.shurablack.core.util.ServerUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Ruler extends EventWorker {

    public static final Map<String, Integer> INTERACTIONS = new HashMap<>();

    @Override
    public void processPublicChannelEvent(Member member, MessageChannelUnion channel, String message, MessageReceivedEvent event) {
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            return;
        }

        if (!message.equals("!ruler")) {
            return;
        }

        createMessage(channel.asTextChannel());
    }

    @Override
    public void processButtonEvent(Member member, MessageChannelUnion channel, String textID, ButtonInteractionEvent event) {

        if (INTERACTIONS.containsKey(member.getId()) && INTERACTIONS.get(member.getId()) >= 5) {
            return;
        }

        Guild guild = channel.asTextChannel().getGuild();
        Role guestRole = guild.getRoleById("384133791595102218");
        Role memberRole = guild.getRoleById("286631357772201994");

        if (INTERACTIONS.containsKey(member.getId())) {
            INTERACTIONS.replace(member.getId(), INTERACTIONS.get(member.getId())+1);
        } else {
            INTERACTIONS.put(member.getId(),1);
        }

        switch (textID) {
            case "ruler guest":
                if (hasHigherRank(member.getRoles())) {
                    sendFeedback(event,"Du hast bereits einen höheren Rang auf dem Server!",ServerUtil.RED,true);
                    return;
                }
                guild.addRoleToMember(UserSnowflake.fromId(member.getId()), guestRole).queue();
                guild.removeRoleFromMember(User.fromId(member.getId()), memberRole).queue();
                sendFeedback(event,"Du wurdest erfolgreich zum **Guest** ernannt!",ServerUtil.GREEN,true);
                break;
            case "ruler member":
                if (hasHigherRank(member.getRoles())) {
                    sendFeedback(event,"Du hast bereits einen höheren Rang auf dem Server!",ServerUtil.RED,true);
                    return;
                }
                guild.addRoleToMember(UserSnowflake.fromId(member.getId()), memberRole).queue();
                guild.removeRoleFromMember(User.fromId(member.getId()), guestRole).queue();
                sendFeedback(event,"Du wurdest erfolgreich zum **Member** ernannt!",ServerUtil.GREEN,true);
                break;
        }
    }

    private void createMessage(TextChannel channel) {
        EmbedBuilder eb = new EmbedBuilder()
            .setTitle("Rollenverteilung - Nutzerhilfe")
            .setDescription("Über diese Nachricht ist jeder Nutzer in der Lage, eine Standard-Rolle zu erhalten." +
                    "\nNutzer mit höherem Rang können sich nicht über diese Nachricht degradieren.")
            .addField("","**Auswahl:**",false)
            .addField("<:bronze:1007022490037518526> Guest,","Für jeden Nutzer der nur gelegentlich/einmalig auf dem" +
                " Discord Server ist. Keine Besonderen Rechte",true)
            .addField("<:silber:1007022487411896320> Member,","Für bekannte Mitspieler und Freunde die neuer sind",true)
                .addField("<:gold:1007022493955010631> Veteran / <:platin:1007022498296103024> Moderator",
                        "Benachrichtige die Servermoderation um diese Ränge zu beantragen",false)
                .addBlankField(false)
                .addField("Freunde einladen","Nutze diesen Link dafür:\nhttps://discord.gg/6yJm5kpfDp",false)
            .setFooter("Das Missbrauchen der Funktion führt zu einem Ausschluss (2 Anfrage pro Tag/Person)");

        MessageCreateBuilder messageBuilder = new MessageCreateBuilder()
                .addEmbeds(eb.build())
                .setComponents(ActionRow.of(
                        Button.secondary("ruler guest", Emoji.fromCustom("bronze",1007022490037518526L,false)),
                        Button.secondary("ruler member",Emoji.fromCustom("silber",1007022487411896320L, false))
                ));
        channel.sendMessage(messageBuilder.build()).complete();
    }

    private boolean hasHigherRank(List<Role> roles) {
        return roles.stream().anyMatch(role -> role.getId().equals("286631270258180117")
                || role.getId().equals("286631247315337219"));
    }

    private void sendFeedback(ButtonInteractionEvent event, String description, int color, boolean ephemeral) {
        Member m = event.getMember();
        if (INTERACTIONS.get(m.getId()) >= 2) {
            description += "\n\nSperre: Du hast die maximale Menge an Anfragen pro Tag überschritten!\n" +
                    "Versuch es morgen wieder";
            color = ServerUtil.RED;
            ephemeral = false;
        }
        EmbedBuilder accept = new EmbedBuilder()
                .setAuthor(m.getEffectiveName(), m.getEffectiveAvatarUrl(), m.getEffectiveAvatarUrl())
                .setColor(color)
                .setDescription(description)
                .setFooter("Anfragen - (" + INTERACTIONS.get(m.getId()) + "/2)");
        event.replyEmbeds(accept.build()).setEphemeral(ephemeral).queue();
    }
}
