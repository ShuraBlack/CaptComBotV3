package model.worker;

import com.shurablack.core.event.EventWorker;
import com.shurablack.core.util.AssetPool;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public class Help extends EventWorker {

    @Override
    public void processPublicChannelEvent(Member member, MessageChannelUnion channel, String message, MessageReceivedEvent event) {
        if(!member.hasPermission(channel.asGuildMessageChannel(), Permission.ADMINISTRATOR)) {
            return;
        }

        TextChannel guildChannel = channel.asTextChannel();

        if (message.equals("!help")) {
            guildChannel.createThreadChannel("Befehle",false).complete();
            guildChannel.createThreadChannel("Warframe Tracker", false).complete();

            List<ThreadChannel> subChannel = guildChannel.getThreadChannels();
            for (ThreadChannel tc : subChannel) {
                if (tc.getName().equals("Befehle")) {
                    tc.sendMessageEmbeds(createMessage().build()).complete();
                } else if (tc.getName().equals("Warframe Tracker")) {
                    EmbedBuilder wt  = new EmbedBuilder()
                            .setTitle("Warframe Tracker - Befehle")
                            .setDescription("**Prefix:** !wftracker\n" +
                                    "- cycle (Cycles of the Open-World-Maps)\n" +
                                    "- arbi (Arbitration info, like mission type, faction,...)\n" +
                                    "- construct (Construction Progress of Razorback and Fomorian)\n" +
                                    "- sortie (Sortie Missions & general)\n" +
                                    "- nightwave (Season info & current missions)\n" +
                                    "- voidtrader (When he will be available and what he got)");
                    tc.sendMessageEmbeds(wt.build()).complete();
                }
            }

            channel.sendMessageEmbeds(createMessage().build()).complete();
        }
    }

    public EmbedBuilder createMessage () {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setThumbnail(AssetPool.get("url_help"));
        eb.setTitle("Allgemeine Befehle: ");
        eb.setDescription("Hier kannst du sehen welche Befehle du als normaler Nutzer ausführen darfst");
        eb.addField("__Zufallsbefehle:__","\n" +
                "**!dice** ```cs\n" +
                "wirft einen 6 seitigen Würfel\n" +
                "```\n" +
                "**!dice [zahl]**```cs\n" +
                "wirft einen [\"zahl\"] seitigen Würfel\n" +
                "```\n" +
                "**!dice match** ```cs\n" +
                "startet ein Würfelspiel, beidem ein anderer Nutzer gegen dich spielen kann\n" +
                "# Andere Nutzer können über Reaction beitreten\n" +
                "```\n" +
                "**!dice invitematch [userID]**```cs\n" +
                "Läd einen Nutzer zu einem aktiven *Dice Match* von dir ein\n" +
                "# Der Nutzer erhält ein Link zum Match\n" +
                "```\n" +
                "**!coinflip**```cs\n" +
                "wirft eine Münze mit Kopf oder Zahl\n" +
                "```\n" +
                "**!mdice [zahl]**```cs\n" +
                "wirft [\"zahl\"] an 6 seitgen Würfeln\n" +
                "```\n" +
                "**!mdice [zahlA] [zahlB]**```cs\n" +
                "wirft [\"zahlA\"] an [\"zahlB\"] seitigen Würfeln\n" +
                "```",false);
        eb.addField("__Playlist:__","\n" +
                "**!playlist add [playlistname] [link]**```cs\n" +
                "Fügt [\"link\"] in deine [\"playlistname\"] ein\n" +
                "```\n" +
                "**!playlist remove [ID]**```cs\n" +
                "Entfernt link mit [\"ID\"]\n" +
                "Wenn als [\"ID\"] \"all\" eingetragen wird, werden alle deine Lieder gelöscht\n" +
                "```\n" +
                "**!playlist show**```cs\n" +
                "Zeigt alle deine Lieder (playlistübergreifend)\n" +
                "```\n" +
                "**!playlist show [playlistname]**```cs\n" +
                "Zeigt alle Lieder aus [\"playlistname\"]\n" +
                "```\n" +
                "**!playlist list**```cs\n" +
                "Zeigt all deine Playlists\n```",false);
        eb.addField("__Temporäre VoiceChannel:__","Wenn du```cs\n" +
                "\"Create T:Voice\" beitritts, wird automatisch ein neuer VoiceChannel erstellt," +
                " mit der gleichen Maximalanzahl und zusätzlichen Rechten für den Ersteller\n```",false);
        return eb;
    }
}
