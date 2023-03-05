package model.worker;

import com.shurablack.core.event.EventWorker;
import com.shurablack.core.util.ServerUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Subscription extends EventWorker {

    public static final Map<String, Integer> INTERACTIONS = new HashMap<>();

    @Override
    public void processPublicChannelEvent(Member member, MessageChannelUnion channel, String message, MessageReceivedEvent event) {
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            return;
        }

        if (!channel.getType().equals(ChannelType.TEXT)) {
            return;
        }

        if (!message.equals("!sub")) {
            return;
        }

        createMessage(channel.asTextChannel());
    }

    @Override
    public void processButtonEvent(Member member, MessageChannelUnion channel, String textID, ButtonInteractionEvent event) {
        if (INTERACTIONS.containsKey(member.getId()) && INTERACTIONS.get(member.getId()) >= 10) {
            return;
        }

        if (INTERACTIONS.containsKey(member.getId())) {
            INTERACTIONS.replace(member.getId(), INTERACTIONS.get(member.getId())+1);
        } else {
            INTERACTIONS.put(member.getId(),1);
        }

        Guild guild = channel.asTextChannel().getGuild();

        Role leaguerole = guild.getRoleById("804433890143895552");
        Role warframerole = guild.getRoleById("804433895202226197");
        Role moonrole = guild.getRoleById("804433899136221254");
        Role mcrole = guild.getRoleById("815012063013109790");
        Role satirole = guild.getRoleById("815011932934504449");
        Role casinorole = guild.getRoleById("845055075947905043");

        switch (textID) {
            case "sub league":
                assert leaguerole != null;
                guild.addRoleToMember(UserSnowflake.fromId(member.getId()),leaguerole).queue();
                sendFeedback(event, "Du hast erfolgreich den **League of Legends** Channel abonniert!", ServerUtil.GREEN,true);
                break;
            case "sub warframe":
                assert warframerole != null;
                guild.addRoleToMember(User.fromId(member.getId()),warframerole).queue();
                sendFeedback(event, "Du hast erfolgreich den **Warframe** Channel abonniert!",ServerUtil.GREEN,true);
                break;
            case "sub minecraft":
                assert mcrole != null;
                guild.addRoleToMember(User.fromId(member.getId()), mcrole).queue();
                sendFeedback(event, "Du hast erfolgreich den **Minecraft** Channel abonniert!",ServerUtil.GREEN,true);
                break;
            case "sub satisfactory":
                assert satirole != null;
                guild.addRoleToMember(User.fromId(member.getId()), satirole).queue();
                sendFeedback(event, "Du hast erfolgreich den **Satisfactory** Channel abonniert!",ServerUtil.GREEN,true);
                break;
            case "sub moon":
                assert moonrole != null;
                guild.addRoleToMember(User.fromId(member.getId()), moonrole).queue();
                sendFeedback(event, "Du hast erfolgreich den **Moonstruck** Channel abonniert!",ServerUtil.GREEN,true);
                break;
            case "sub casino":
                if (member.getRoles().stream().map(Role::getName).noneMatch(name -> name.equals("Member")
                        || name.equals("Veteran") || name.equals("Moderator"))) {
                    sendFeedback(event, "Du benötigst mindestens den **Member** Rang für diese Aktion!",ServerUtil.RED,true);
                    return;
                }
                assert casinorole != null;
                guild.addRoleToMember(User.fromId(member.getId()), casinorole).queue();
                sendFeedback(event, "Du hast erfolgreich die **Game (Casino)** Channels abonniert!",ServerUtil.GREEN,true);
                break;
            case "sub delete_news":
                List<Role> rNews = new ArrayList<>();
                rNews.add(leaguerole);
                rNews.add(warframerole);
                rNews.add(mcrole);
                rNews.add(satirole);

                guild.modifyMemberRoles(member, new ArrayList<>(), rNews).queue();
                sendFeedback(event, "Alle deine Channel Abonnements wurden entfernt!",ServerUtil.GREEN,true);
                break;
            case "sub delete_special":
                List<Role> rSpecials = new ArrayList<>();
                rSpecials.add(moonrole);
                rSpecials.add(casinorole);

                guild.modifyMemberRoles(member, new ArrayList<>(), rSpecials).queue();
                sendFeedback(event, "Alle deine Partner & Special Abonnements wurden entfernt!",ServerUtil.GREEN,true);
                break;
        }
    }

    private void sendFeedback(ButtonInteractionEvent event, String description, int color, boolean ephemeral) {
        Member m = event.getMember();
        assert m != null;
        if (INTERACTIONS.get(m.getId()) >= 10) {
            description += "\n\nSperre: Du hast die maximale Menge an Anfragen pro Tag überschritten!\n" +
                    "Versuch es morgen wieder";
            color = ServerUtil.RED;
            ephemeral = false;
        }
        EmbedBuilder accept = new EmbedBuilder()
                .setAuthor(m.getEffectiveName(), m.getEffectiveAvatarUrl(), m.getEffectiveAvatarUrl())
                .setColor(color)
                .setDescription(description)
                .setFooter("Anfragen - (" + INTERACTIONS.get(m.getId()) + "/10)");
        event.replyEmbeds(accept.build()).setEphemeral(ephemeral).queue();
    }

    private void createMessage (TextChannel channel) {
        final Emoji league = channel.getGuild().getEmojiById("804426711479484417");
        final Emoji warframe = channel.getGuild().getEmojiById("804427054015447062");
        final Emoji moon = channel.getGuild().getEmojiById("804427189969747999");
        final Emoji mc = channel.getGuild().getEmojiById("815238918496583710");
        final Emoji sati = channel.getGuild().getEmojiById("815238883620159548");
        final Emoji casino = channel.getGuild().getEmojiById("845057339145191434");
        final String ex = "✖️";

        assert league != null;
        assert warframe != null;
        assert mc != null;
        assert sati != null;
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("News Channel - Abonnieren")
                .setDescription("Über diese Nachricht kann der Nutzer bestimmen, welchen Subscription Channel er abonnieren/sehen möchte")
                .addField("","Verfügbare Channel:",false)
                .addField(league.getFormatted() + " League of Legends,","Updates, game changes usw. von dem eigenen League Discord Server",false)
                .addField(warframe.getFormatted() + " Warframe,","Updates, game changes usw. von dem eigenen Warframe Discord Server",false)
                .addField(mc.getFormatted() + " Minecraft,","Updates, game changes usw. von dem eigenen Minecraft Discord Server",false)
                .addField(sati.getFormatted() + " Satisfactory,","Updates, game changes usw. von dem eigenen Satisfactory Discord Server",false)
                .addField("❌ Entfernen,","Entfernt alle abonnierten Channel von dir",false);

        MessageCreateBuilder messageBuilder = new MessageCreateBuilder()
                .addEmbeds(eb.build())
                .setComponents(ActionRow.of(
                        Button.secondary("sub league", Emoji.fromUnicode("<:league:804426711479484417>")),
                        Button.secondary("sub warframe", Emoji.fromUnicode("<:warframe:804427054015447062>")),
                        Button.secondary("sub minecraft", Emoji.fromUnicode("<:mc:815238918496583710>")),
                        Button.secondary("sub satisfactory", Emoji.fromUnicode("<:sati:815238883620159548>")),
                        Button.danger("sub delete_news", Emoji.fromUnicode(ex))
                ));
        channel.sendMessage(messageBuilder.build()).queue();

        assert moon != null;
        assert casino != null;
        eb = new EmbedBuilder()
                .setTitle("Partner & Specials - Abonnieren")
                .addField(moon.getFormatted() + " Moonstruck,","Information über den Streamer, sowie Ankündigungen für den Stream",false)
                .addField(casino.getFormatted() + " Games,","Schaltet alle Server privaten Games Channel frei\n" +
                        "```diff\n- Du benötigst ebenfalls mindestens den Member Rang und bestätigst damit auch das du über 18 bist\n```",false)
                .addField("❌ Entfernen,","Entfernt alle abonnierten Partner und Specials von dir",false)
                .setFooter("Das Missbrauchen der Funktion führt zu einem Ausschluss (10 Anfrage pro Tag/Person)");

        messageBuilder.clear().addEmbeds(eb.build())
                .setComponents(ActionRow.of(
                        Button.secondary("sub moon", Emoji.fromUnicode("<:moon:804427189969747999>")),
                        Button.secondary("sub casino", Emoji.fromUnicode("<:casino:845057339145191434>")),
                        Button.danger("sub delete_special", Emoji.fromUnicode(ex))
                ));
        channel.sendMessage(messageBuilder.build()).queue();

    }
}
