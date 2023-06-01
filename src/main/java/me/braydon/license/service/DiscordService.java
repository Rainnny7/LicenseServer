package me.braydon.license.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.braydon.license.common.TimeUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

/**
 * @author Braydon
 */
@Service
@Slf4j(topic = "Discord")
public final class DiscordService {
    /**
     * The version of this Springboot application.
     */
    @NonNull private final String applicationVersion;
    /**
     * The name of this Springboot application.
     */
    @Value("${spring.application.name}")
    @NonNull private String applicationName;
    /**
     * The token to the Discord bot.
     */
    @Value("${discord.token}")
    @NonNull private String token;
    
    /**
     * The channel ID to log to.
     */
    @Value("${discord.logs.channel}")
    private long logsChannel;
    
    /**
     * Should used licenses be logged?
     */
    @Value("${discord.logs.uses}") @Getter
    private boolean logUses;
    
    /**
     * Should we log if an expired license was used?
     */
    @Value("${discord.logs.expired}") @Getter
    private boolean logExpired;
    
    /**
     * Should IP limited licenses be logged when used?
     */
    @Value("${discord.logs.expired}") @Getter
    private boolean logIpLimitExceeded;
    
    /**
     * Should HWID limited licenses be logged when used?
     */
    @Value("${discord.logs.expired}") @Getter
    private boolean logHwidLimitExceeded;
    
    /**
     * The {@link JDA} instance of the bot.
     */
    private JDA jda;
    
    @Autowired
    public DiscordService(@NonNull BuildProperties buildProperties) {
        this.applicationVersion = buildProperties.getVersion();
    }
    
    @PostConstruct @SneakyThrows
    public void onInitialize() {
        // No token was provided
        if (token.trim().isEmpty()) {
            log.info("Not using Discord, no token provided");
            return;
        }
        // Initialize the bot
        long before = System.currentTimeMillis();
        log.info("Logging in..."); // Log that we're logging in
        jda = JDABuilder.createDefault(token)
                  .enableIntents(
                      GatewayIntent.GUILD_MEMBERS
                  ).setStatus(OnlineStatus.DO_NOT_DISTURB)
                  .setActivity(Activity.watching("your licenses"))
                  .build();
        jda.awaitReady(); // Await JDA to be ready
        
        // Log that we're logged in
        log.info("Logged into {} in {}ms",
            jda.getSelfUser().getAsTag(), System.currentTimeMillis() - before
        );
    }
    
    /**
     * Send a log to the logs channel
     * with the given embed.
     *
     * @param embed the embed to send
     * @see TextChannel for channel
     * @see EmbedBuilder for embed
     */
    public void sendLog(@NonNull EmbedBuilder embed) {
        // JDA must be ready to send logs
        if (!isReady()) {
            return;
        }
        // Not enabled
        if (logsChannel <= 0L) {
            return;
        }
        TextChannel textChannel = jda.getTextChannelById(logsChannel); // Get the logs channel
        if (textChannel == null) { // We must have a logs channel
            throw new IllegalArgumentException("Log channel %s wasn't found".formatted(logsChannel));
        }
        // Send the log
        textChannel.sendMessageEmbeds(embed.setFooter("%s v%s - %s".formatted(
            applicationName, applicationVersion, TimeUtils.dateTime()
        )).build()).queue();
    }
    
    /**
     * Check if the bot is ready.
     *
     * @return true if ready, otherwise false
     */
    public boolean isReady() {
        return jda != null && (jda.getStatus() == JDA.Status.CONNECTED);
    }
}