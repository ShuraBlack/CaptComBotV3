package model.worker;

import com.shurablack.core.event.EventWorker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.time.OffsetDateTime;

public class Rules extends EventWorker {

    @Override
    public void processPublicChannelEvent(Member member, MessageChannelUnion channel, String message, MessageReceivedEvent event) {
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            return;
        }

        if (!message.equals("!rules")) {
            return;
        }

        EmbedBuilder rules = new EmbedBuilder()
                .setTitle("Server - Richtlinien")
                .setDescription("**§1** Beleidigt, diskriminiert oder stört keine anderen Member\n\n" +
                        "**§2** Die Servermoderation muss zu jeder aktiven Zeit mit dir Kommunizieren können\n\n" +
                        "**§3** Provoziere keine Member\n\n" +
                        "**§4** Nicknames werden, wenn sie *§1* verletzen, auf dem Server verändert\n\n" +
                        "**§5** Sende keine Links mit unangemessenem Inhalt (z.B. Pornografische oder Gewaltätige Darstellung, ...)\n\n" +
                        "**§6** Keine Eigenwerbung jeglicher Art (z.B. Discord Server, Youtube Kanal, Facebook- und Twitter Fanpages, sowie Klans/Teams)\n\n" +
                        "**§7** Hochgeladene Daten dürfen *§1,3,5,6* nicht verletzen\n\n" +
                        "**§8** Das Spamming in Voice- oder TextChannel\n\n" +
                        "**§9** Ungekennzeichnete Bots werden entfernt (Unter Absprache möglich)\n\n" +
                        "**§10** Betrüge niemanden\n\n" +
                        "**§11** Halte dich an Discord´s [AGBs](https://discord.com/terms)\n\n" +
                        "_Missachten der Server-Richtlinien führt zum Ausschluss!_")
                .setFooter("Gültig ab dem")
                .setTimestamp(OffsetDateTime.now());
        EmbedBuilder ebbottom = new EmbedBuilder();
        ebbottom.setColor(Color.RED);
        ebbottom.addField("Meldet Probleme oder Beschwerden der Server Moderation",
                "Wie auch beim Gesetz: Unwissenheit schützt nicht vor Strafe und deswegen " +
                        "empfehlen wir die Regeln __gut__ durchzulesen und zu kennen",false);

        EmbedBuilder redirect = new EmbedBuilder();
        redirect.addField("Zur Rollenverteilung:"
                ,"[Roles Channel](https://discord.com/channels/286628427140825088/799449909090713631/1015080368036126832)",false);

        channel.sendMessageEmbeds(rules.build(), ebbottom.build(), redirect.build()).queue();
    }
}
