package me.braydon.license.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.braydon.license.common.TimeUtils;
import me.braydon.license.model.License;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
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
     * Should new IPs be sent to the license owner?
     */
    @Value("${discord.owner-logs.newIp}") @Getter
    private boolean logNewIpsToOwner;
    
    /**
     * Should new HWIDs be sent to the license owner?
     */
    @Value("${discord.owner-logs.newHwid}") @Getter
    private boolean logNewHwidsToOwner;
    
    /**
     * Should the license owner be notified when their license is about to expire?
     */
    @Value("${discord.owner-logs.expiringSoon}") @Getter
    private boolean logExpiringSoonToOwner;
    
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
        textChannel.sendMessageEmbeds(buildEmbed(embed)).queue();
    }
    
    public void sendOwnerLog(@NonNull License license, @NonNull EmbedBuilder embed) {
        // We need an owner for the license
        if (license.getOwnerSnowflake() <= 0L) {
            return;
        }
        // Lookup the owner of the license
        jda.retrieveUserById(license.getOwnerSnowflake()).queue(owner -> {
            if (owner == null) { // Couldn't locate the owner of the license
                return;
            }
            owner.openPrivateChannel().queue(channel -> {
                channel.sendMessageEmbeds(buildEmbed(embed)).queue(null, ex -> {
                    // Ignore the ex if the owner has priv msgs turned off, we don't care
                    if (((ErrorResponseException) ex).getErrorResponse() != ErrorResponse.CANNOT_SEND_TO_USER) {
                        ex.printStackTrace();
                    }
                });
            });
        }, ex -> {
            // Ignore the ex if the owner isn't found, we don't care
            if (((ErrorResponseException) ex).getErrorResponse() != ErrorResponse.UNKNOWN_USER) {
                ex.printStackTrace();
            }
        });
    }
    
    /**
     * Check if the bot is ready.
     *
     * @return true if ready, otherwise false
     */
    public boolean isReady() {
        return jda != null && (jda.getStatus() == JDA.Status.CONNECTED);
    }
    
    /**
     * Build the given embed.
     *
     * @param embedBuilder the embed builder
     * @return the built embed
     */
    @NonNull
    private MessageEmbed buildEmbed(@NonNull EmbedBuilder embedBuilder) {
        return embedBuilder.setFooter("%s v%s - %s".formatted(
            applicationName, applicationVersion, TimeUtils.dateTime()
        )).build();
    }
}