package model.worker;

import com.shurablack.core.event.EventWorker;
import model.database.models.PlaylistModel;
import model.database.models.SongIDModel;
import model.manager.DiscordBot;
import com.shurablack.sql.SQLRequest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static model.database.Statement.*;

public class Playlist extends EventWorker {

    @Override
    public void processPublicChannelEvent(Member member, MessageChannelUnion channel, String message, MessageReceivedEvent event) {
        String[] args = message.split(" ");

        TextChannel textChannel = channel.asTextChannel();

        if (args.length == 4 && args[1].equals("add")) {
            SQLRequest.run(DiscordBot.UTIL.getConnectionPool(),INSERT_SONG(member.getId(),args[2],args[3]));
            UserFeedback(textChannel, member, String.format("Der Link %s wurde zur Playlist %s hinzugefügt", args[3], args[2]), Color.GREEN);
        } else if (args.length == 3 && args[1].equals("remove") && args[2].equals("all")) {
            SQLRequest.run(DiscordBot.UTIL.getConnectionPool(),DELETE_ALL_SONGS(member.getId()));
            UserFeedback(textChannel, member,"Alle deine Links wurden entfernt", Color.RED);
        } else if (args.length == 3 && args[1].equals("remove")) {
            SQLRequest.run(DiscordBot.UTIL.getConnectionPool(),DELETE_SONG(member.getId(),args[2]));
            UserFeedback(textChannel, member,String.format("Der Link %s wurde entfernt", args[2]), Color.RED);
        } else if (args.length == 2 && args[1].equals("show")) {

            List<PlaylistModel> playlistModels =
                    SQLRequest.runList(DiscordBot.UTIL.getConnectionPool(),SELECT_ALL_SONGS(member.getId()),PlaylistModel.class);
            Map<String, Integer> songs = new TreeMap<>();
            for (PlaylistModel pm : playlistModels) {
                songs.put(pm.getSong(), pm.getId());
            }

            List<Integer> count =
                    SQLRequest.runList(DiscordBot.UTIL.getConnectionPool(),SELECT_COUNT_PLAYLIST(member.getId()), Integer.class);
            channel.sendMessageEmbeds(createSongMessage(songs, member, count.get(0)).build())
                    .complete().delete().queueAfter(3, TimeUnit.MINUTES);
        } else if (args.length == 3 && args[1].equals("show")) {
            List<SongIDModel> list =
                    SQLRequest.runList(DiscordBot.UTIL.getConnectionPool(),SELECT_SONG_PLAYLIST(member.getId(),args[2]), SongIDModel.class);
            Map<String, List<SongIDModel>> songs = new TreeMap<>();

            if (!list.isEmpty()) {
                List<SongIDModel> tmp = new ArrayList<>();
                for (SongIDModel sidm : list) {
                    if (songs.containsKey(sidm.getSonglist())) {
                        songs.get(sidm.getSonglist()).add(sidm);
                    }
                    tmp.add(sidm);
                    songs.put(sidm.getSonglist(),tmp);
                }
            }
            channel.sendMessageEmbeds(createPlaylistSongMessage(songs, member).build())
                    .complete().delete().queueAfter(3, TimeUnit.MINUTES);
        } else if (args.length == 2 && args[1].equals("list")) {
            List<String> playlists =
                    SQLRequest.runList(DiscordBot.UTIL.getConnectionPool(),SELECT_PLAYLISTS(member.getId()), String.class);
            channel.sendMessageEmbeds(createPlaylistsMessage(playlists, member).build())
                    .complete().delete().queueAfter(1, TimeUnit.MINUTES);
        }
    }

    public EmbedBuilder createPlaylistsMessage (List<String> playlists, Member m) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.WHITE);
        eb.setTitle(m.getEffectiveName() + "\nID: " + m.getId());
        eb.setThumbnail(m.getUser().getAvatarUrl());
        eb.setDescription("Eine Liste von all deinen gespeicherten Playlist/s");
        if (playlists.isEmpty()) {
            eb.addField("","Du hast noch keine Playlist/s eingetragen",false);
        } else {
            StringBuilder s = new StringBuilder();
            for (String playlist : playlists) {
                s.append(playlist).append("\n");
            }
            eb.addField("Playlist/s: " + playlists.size(), s.toString(),false);
            eb.setFooter("Diese Nachricht wird automatisch nach 1 Minuten gelöscht");
        }
        return eb;
    }

    public EmbedBuilder createSongMessage (Map<String, Integer> songs, Member m, int playlistcount) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.WHITE);
        eb.setTitle(m.getEffectiveName() + "\nID: " + m.getId());
        eb.setThumbnail(m.getUser().getAvatarUrl());
        eb.setDescription("Eine Liste von all deinen gespeicherten Liedern");
        if (songs.isEmpty()) {
            eb.addField("","Du hast noch keine Lieder eingetragen",false);
        } else {
            StringBuilder s = new StringBuilder();
            for (Map.Entry<String, Integer> song : songs.entrySet()) {
                s.append(song.getKey()).append(" **[").append(song.getValue()).append("]**").append("\n");
            }
            eb.addField("Anzahl: " + songs.size() + " - Playlist/s: " + playlistcount, s.toString(),false);
        }
        eb.setFooter("Diese Nachricht wird automatisch nach 3 Minuten gelöscht");
        return eb;
    }

    public EmbedBuilder createPlaylistSongMessage (Map<String, List<SongIDModel>> songs, Member m) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.WHITE);
        eb.setTitle(m.getEffectiveName() + "\nID: " + m.getId());
        eb.setThumbnail(m.getUser().getAvatarUrl());
        eb.setDescription("Eine Liste von allen Liedern in einer bestimmten Playlist");
        if (songs.isEmpty()) {
            eb.addField("","Du hast noch keine Lieder eingetragen",false);
        } else {
            for (Map.Entry<String, List<SongIDModel>> entry : songs.entrySet()) {
                StringBuilder s = new StringBuilder();
                for (SongIDModel song : entry.getValue()) {
                    s.append(song.getSong()).append(" **[").append(song.getId()).append("]**").append("\n");
                }
                eb.addField(entry.getKey() + " - Anzahl: " + entry.getValue().size(), s.toString(), false);
            }
        }
        eb.setFooter("Diese Nachricht wird automatisch nach 3 Minuten gelöscht");
        return eb;
    }

    public void UserFeedback (TextChannel channel, Member m, String message, Color color) {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(color)
                .setDescription(m.getAsMention() + " : " + message)
                .setFooter("CaptCommunity Playlist");
        channel.sendMessageEmbeds(eb.build()).queue((mes) -> mes.delete().queueAfter(5, TimeUnit.SECONDS));
    }
}
