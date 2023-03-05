package model.manager;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.shurablack.core.builder.CommandAction;
import com.shurablack.core.builder.Config;
import com.shurablack.core.builder.JDAUtil;
import com.shurablack.core.builder.UtilBuilder;
import com.shurablack.core.event.EventHandler;
import com.shurablack.core.option.Option;
import com.shurablack.core.option.OptionSet;
import com.shurablack.core.scheduling.TaskService;
import model.listener.*;
import model.service.LogService;
import model.web.Server;
import model.worker.*;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.http.client.config.RequestConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

import static com.shurablack.core.option.Type.*;
import static com.shurablack.core.util.LocalCache.getChannelID;
import static net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public class DiscordBot {

    private static final Logger LOGGER = LogManager.getLogger(DiscordBot.class);

    public static JDAUtil UTIL;

    public static boolean MUTE = false;
    public static final String GUILD = "286628427140825088";
    public static final String TMP_VOICE_CATEGORY = "820340114538102814";

    // AudioPlayer
    private static AudioPlayerManager PLAYER_MANAGER;
    private static MusicManager MUSIC_MANAGER;

    // Server
    private static Server server;

    public static void main(String[] args) {
        UtilBuilder.init();

        if (Config.getConfig("youtube_api_token") == null) {
            LOGGER.error("Missing youtube_api_token properties. This can lead to errors ...");
        }

        server = new Server();

        PLAYER_MANAGER = new DefaultAudioPlayerManager();
        PLAYER_MANAGER.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        PLAYER_MANAGER.registerSourceManager(new YoutubeAudioSourceManager());
        PLAYER_MANAGER.registerSourceManager(new TwitchStreamAudioSourceManager());
        PLAYER_MANAGER.setHttpRequestConfigurator((config) -> RequestConfig.copy(config).setConnectTimeout(5000).build());
        MUSIC_MANAGER = new MusicManager(PLAYER_MANAGER);

        new DiscordBot();
    }

    public DiscordBot() {
        UTIL = UtilBuilder
                .create(createJDA(), createHandler())
                .addDataBase()
                .addOnExit((input) -> server.getServer().stop(0))
                .setCommandLineAction(
                        new CommandAction(
                                "logs flush",
                                "Flushes the logs and saves them",
                                (input) -> {
                                    for (String file : LogService.getFiles()) {
                                        LOGGER.info("Flush " + file + ".log");
                                        LogService.writeFile(file);
                                    }
                                    LOGGER.info("Successfully flushed all logs");
                                }
                        ),
                        new CommandAction(
                                "upload slash",
                                "Upload slash commands",
                                (input) -> uploadSlashCommands()
                        ),
                        new CommandAction(
                                "mute",
                                "<on/off> Un/mute listeners",
                                (input) -> {
                                    if (input.equals("mute on")) {
                                        MUTE = true;
                                        LOGGER.info("Bot got muted");
                                    }

                                    if (input.equals("mute off")) {
                                        MUTE = false;
                                        LOGGER.info("Bot got unmuted");
                                    }
                                }
                        )
                )
                .build();
    }

    private EventHandler createHandler() {
        return new EventHandler()
                .create()
                .set(TaskService.getThreadPool(), "!", true)
                .registerEvent(
                        new OptionSet().create(
                                new Badge(),
                                Option.create(GLOBAL_SLASH,"badge").setGlobalCD(30),
                                Option.create(GUILD_SLASH,"badge")
                        ),
                        new OptionSet().create(
                                new Clear(),
                                Option.create(PUBLIC_CHANNEL, "clear")
                        ),
                        new OptionSet().create(
                                new CoinFlip(),
                                Option.create(GLOBAL_SLASH, "coinflip").setUserCD(30),
                                Option.create(GUILD_SLASH, "coinflip").setUserCD(10)
                                        .setChannelRestriction(List.of(getChannelID("request")))
                        ),
                        new OptionSet().create(
                                new Copy(),
                                Option.create(PUBLIC_CHANNEL, "copy")
                        ),
                        new OptionSet().create(
                                new Dice(),
                                Option.create(GLOBAL_SLASH, "dice").setUserCD(30),
                                Option.create(GUILD_SLASH, "dice").setUserCD(10)
                                        .setChannelRestriction(List.of(getChannelID("request")))
                        ),
                        new OptionSet().create(
                                new Help(),
                                Option.create(PUBLIC_CHANNEL, "help")
                                        .setChannelRestriction(List.of(getChannelID("request")))
                        ),
                        new OptionSet().create(
                                new Rules(),
                                Option.create(PUBLIC_CHANNEL, "rules")
                                        .setChannelRestriction(List.of(getChannelID("guidelines")))
                        ),
                        new OptionSet().create(
                                new WarframeTracker(),
                                Option.create(PUBLIC_CHANNEL, "wft").setUserCD(10)
                        ),
                        new OptionSet().create(
                                new Subscription(),
                                Option.create(PUBLIC_CHANNEL, "sub")
                                        .setChannelRestriction(List.of(getChannelID("subscription"))),
                                Option.create(BUTTON, "sub")
                                        .setChannelRestriction(List.of(getChannelID("subscription")))
                        ),
                        new OptionSet().create(
                                new Ruler(),
                                Option.create(PUBLIC_CHANNEL, "ruler")
                                        .setChannelRestriction(List.of(getChannelID("roles"))),
                                Option.create(BUTTON, "ruler")
                                        .setChannelRestriction(List.of(getChannelID("roles")))
                        ),
                        new OptionSet().create(
                                new Playlist(),
                                Option.create(PUBLIC_CHANNEL, "playlist")
                                        .setChannelRestriction(List.of(getChannelID("request")))
                        ),
                        new OptionSet().create(
                                new Player(),
                                Option.create(GUILD_SLASH, "player")
                                        .setChannelRestriction(List.of(getChannelID("music"))),
                                Option.create(PUBLIC_CHANNEL, "player")
                                        .setChannelRestriction(List.of(getChannelID("music"))),
                                Option.create(PUBLIC_REACTION, "player")
                                        .setChannelRestriction(List.of(getChannelID("music"))),
                                Option.create(BUTTON, "player")
                                        .setChannelRestriction(List.of(getChannelID("music"))),
                                Option.create(MODAL, "player")
                                        .setChannelRestriction(List.of(getChannelID("music"))),
                                Option.create(SELECTION, "player")
                                        .setChannelRestriction(List.of(getChannelID("music")))
                        ),
                        new OptionSet().create(
                                new Game(),
                                Option.create(PUBLIC_CHANNEL, "game")
                                        .setChannelRestriction(List.of(getChannelID("hub"), getChannelID("shop"))),
                                Option.create(BUTTON, "game")
                                        .setChannelRestriction(List.of(getChannelID("hub"), getChannelID("shop"))),
                                Option.create(MODAL, "game")
                                        .setChannelRestriction(List.of(getChannelID("hub"), getChannelID("shop")))
                        ),
                        new OptionSet().create(
                                new BlackJack(),
                                Option.create(PUBLIC_CHANNEL, "bj")
                                        .setChannelRestriction(List.of(getChannelID("blackjack"))),
                                Option.create(PUBLIC_REACTION, "bj")
                                        .setChannelRestriction(List.of(getChannelID("blackjack_broad"))),
                                Option.create(BUTTON, "bj")
                                        .setChannelRestriction(List.of(getChannelID("blackjack_broad"))),
                                Option.create(MODAL, "bj")
                                        .setChannelRestriction(List.of(getChannelID("blackjack_broad")))

                        )
                );
    }

    private JDABuilder createJDA() {
        return JDABuilder.createDefault(Config.getConfig("access_token"))
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.MESSAGE_CONTENT)
                .disableIntents(getDisabledIntents())
                .disableCache(getDisabledCacheFlags())
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setLargeThreshold(50)
                .addEventListeners(new DefaultListener())
                .addEventListeners(new MessageListener())
                .addEventListeners(new VoiceChannelListener())
                .addEventListeners(new SlashListener())
                .addEventListeners(new ActivListener())
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.watching("The CaptCom Server"));
    }

    private List<GatewayIntent> getDisabledIntents () {
        List<GatewayIntent> list = new LinkedList<>();
        list.add(GatewayIntent.DIRECT_MESSAGE_REACTIONS);
        list.add(GatewayIntent.DIRECT_MESSAGE_TYPING);
        list.add(GatewayIntent.GUILD_PRESENCES);
        return list;
    }

    private List<CacheFlag> getDisabledCacheFlags () {
        List<CacheFlag> list = new LinkedList<>();
        list.add(CacheFlag.ACTIVITY);
        list.add(CacheFlag.CLIENT_STATUS);
        return list;
    }

    public static AudioPlayerManager getAudioPlayer() {
        return PLAYER_MANAGER;
    }

    public static MusicManager getMusicManager() {
        JDAUtil.JDA.getGuildById(GUILD).getAudioManager().setSendingHandler(MUSIC_MANAGER.getSendHandler());
        return MUSIC_MANAGER;
    }

    private void uploadSlashCommands() {
        LOGGER.info("Try to upload new Slash command set ...");
        CommandListUpdateAction commands = JDAUtil.JDA.updateCommands();
        commands.addCommands().queue();

        JDAUtil.JDA.updateCommands().addCommands(
                Commands.slash("coinflip","Wirft eine Münze für dich"),
                Commands.slash("dice","Wirft Würfel für dich")
                        .addOptions(new OptionData(INTEGER,"dices","Anzahl an Würfel die geworfen werden").setRequired(false))
                        .addOptions(new OptionData(INTEGER,"eyes","Anzahl an Augen der Würfels").setRequired(false)),
                Commands.slash("badge", "Global Slash Interaction für Badge")
        ).queue();

        Guild guild = JDAUtil.JDA.getGuildById("286628427140825088");

        guild.updateCommands().addCommands(
                // Commands for MusicPlayer
                Commands.slash("player","Befehle für die Interaktion mit dem MusicPlayer").addSubcommands(
                        new SubcommandData("load","Lädt angegebene Playlist")
                                .addOptions(new OptionData(STRING, "playlist","Deine Bot Playlist").setRequired(true)),
                        new SubcommandData("save", "Speichert aktuellen Track in Playlist")
                                .addOptions(new OptionData(STRING,"playlist","In welche Playlist gespeichert wird").setRequired(true)),
                        new SubcommandData("volume", "Verändert die Lautstärke")
                                .addOptions(new OptionData(INTEGER,"value","Lautstärke zwischen 0-100").setRequired(true).setMinValue(0).setMaxValue(100)),
                        new SubcommandData("remove", "Entfernt Track mit der angegebenen Nummer")
                                .addOptions(new OptionData(INTEGER,"tracknumber","Die Nummer des Tracks").setRequired(true).setMinValue(1)),
                        new SubcommandData("start", "Startet Track mit der angegebenen Nummer")
                                .addOptions(new OptionData(INTEGER,"tracknumber","Die Nummer des Tracks").setRequired(true).setMinValue(1)),
                        new SubcommandData("pop","Nächster Track ist die angegebene Nummer")
                                .addOptions(new OptionData(INTEGER,"tracknumber","Die Nummer des Tracks").setRequired(true).setMinValue(1)),
                        new SubcommandData("position","Setzt Zeit des Tracks auf Position")
                                .addOptions(new OptionData(INTEGER,"hour","Stundenziffer").setRequired(true).setMinValue(0))
                                .addOptions(new OptionData(INTEGER,"minute","Minutenziffer (0-59)").setRequired(true).setMaxValue(59).setMinValue(0))
                                .addOptions(new OptionData(INTEGER,"second","Sekundenziffer (0-59)").setRequired(true).setMaxValue(59).setMinValue(0))
                )
        ).queue();
        LOGGER.info("Successfully uploaded Slash command set. The Commands will be activated within an hour");
    }
}
