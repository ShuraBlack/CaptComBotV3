package model.listener;

import com.shurablack.core.util.LocalCache;
import model.manager.DiscordBot;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class VoiceChannelListener extends ListenerAdapter {

    List<String> tmpChannel = new LinkedList<>();


    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        if (event.getMember().getUser().isBot()) {
            return;
        }

        if (event.getChannelJoined() != null && event.getChannelLeft() != null) {
            String channelid = event.getChannelJoined().getId();
            if (channelid.equals(LocalCache.getChannelID("tmp_voice"))) {

                String newChannelID = ((VoiceChannel)event.getChannelJoined()).createCopy()
                        .setName("T: " + event.getMember().getUser().getAsTag())
                        .setPosition(0)
                        .setParent(event.getChannelJoined().getGuild().getCategoryById(DiscordBot.TMP_VOICE_CATEGORY))
                        .addMemberPermissionOverride(Long.parseLong(event.getMember().getId()), getPermissions(),new LinkedList<>())
                        .complete()
                        .getId();
                VoiceChannel vc = event.getGuild().getVoiceChannelById(newChannelID);
                event.getMember().getGuild()
                        .moveVoiceMember(event.getMember(),vc).queue();
                this.tmpChannel.add(newChannelID);
            }

            String channelIDLeft = event.getChannelLeft().getId();
            if (this.tmpChannel.contains(channelIDLeft)) {
                this.tmpChannel.remove(channelIDLeft);
                event.getChannelLeft().delete().queue();
            }
        } else if (event.getChannelJoined() != null) {
            String channelid = event.getChannelJoined().getId();
            if (channelid.equals(LocalCache.getChannelID("tmp_voice"))) {
                String newChannelID = ((VoiceChannel)event.getChannelJoined()).createCopy()
                        .setName("T: " + event.getMember().getUser().getAsTag())
                        .setPosition(0)
                        .setParent(event.getChannelJoined().getGuild().getCategoryById(DiscordBot.TMP_VOICE_CATEGORY))
                        .addMemberPermissionOverride(Long.parseLong(event.getMember().getId()), getPermissions(),new LinkedList<>())
                        .complete()
                        .getId();
                VoiceChannel vc = event.getGuild().getVoiceChannelById(newChannelID);
                assert vc != null;
                event.getMember().getGuild()
                        .moveVoiceMember(event.getMember(),vc).queue();
                tmpChannel.add(newChannelID);
            }
        } else if (event.getChannelLeft() != null) {
            if (event.getChannelLeft().getMembers().isEmpty()
                    && this.tmpChannel.contains(event.getChannelLeft().getId())) {
                event.getChannelLeft().delete().queue();
            }
        }

        super.onGuildVoiceUpdate(event);
    }

    private List<Permission> getPermissions() {
        List<Permission> list = new LinkedList<>();
        list.add(Permission.MANAGE_CHANNEL);
        list.add(Permission.KICK_MEMBERS);
        list.add(Permission.VOICE_MOVE_OTHERS);
        list.add(Permission.VOICE_MUTE_OTHERS);
        return list;
    }

}
