package me.braydon.license.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.braydon.license.common.MiscUtils;
import me.braydon.license.common.TimeUtils;
import me.braydon.license.model.License;
import me.braydon.license.repository.LicenseRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Braydon
 */
@Service
@Slf4j(topic = "Discord")
public final class DiscordService {
    private static final String CLEAR_IPS_BUTTON_ID = "clearIps";
    private static final String CLEAR_HWIDS_BUTTON_ID = "clearHwids";
    
    /**
     * The {@link LicenseRepository} to use.
     */
    @Nonnull private final LicenseRepository licenseRepository;
    
    /**
     * The version of this Springboot application.
     */
    @NonNull private final String applicationVersion;
    
    /**
     * The salt to use for hashing license keys.
     */
    @Value("${salts.licenses}")
    @NonNull private String licensesSalt;
    
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
     * The {@link JDA} instance of the bot.
     */
    private JDA jda;
    
    /**
     * Cached licenses for messages.
     * <p>
     * When a license is looked up by it's owner, the
     * response message is cached (key is the message snowflake)
     * for 5 minutes. This is so we're able to get the message
     * an action was performed on, as well as action timeouts.
     * </p>
     */
    private final Cache<Long, License> cachedLicenses = CacheBuilder.newBuilder()
                                                            .expireAfterWrite(5L, TimeUnit.MINUTES)
                                                            .build();
    
    @Autowired
    public DiscordService(@NonNull LicenseRepository licenseRepository, @NonNull BuildProperties buildProperties) {
        this.licenseRepository = licenseRepository;
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
                  .addEventListeners(new EventHandler())
                  .build();
        jda.awaitReady(); // Await JDA to be ready
        
        // Log that we're logged in
        log.info("Logged into {} in {}ms",
            jda.getSelfUser().getEffectiveName(), System.currentTimeMillis() - before
        );
        
        // Registering slash commands
        jda.updateCommands().addCommands(
            Commands.slash("license", "Manage one of your licenses")
                .addOption(OptionType.STRING, "key", "The license key", true)
                .addOption(OptionType.STRING, "product", "The product the license is for", true)
        ).queue();
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
    
    /**
     * Send an embed to the owner
     * of the given license.
     *
     * @param license the license
     * @param embed   the embed to send
     * @see License for license
     * @see EmbedBuilder for embed
     */
    public void sendOwnerLog(@NonNull License license, @NonNull EmbedBuilder embed) {
        // JDA must be ready to send logs
        if (!isReady()) {
            return;
        }
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
    
    /**
     * The event handler for the bot.
     */
    public class EventHandler extends ListenerAdapter {
        @Override
        public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event) {
            User user = event.getUser(); // The command executor
            
            // Handle the license command
            if (event.getName().equals("license")) {
                String key = Objects.requireNonNull(event.getOption("key")).getAsString();
                String product = Objects.requireNonNull(event.getOption("product")).getAsString();
                event.deferReply(true).queue(); // Send thinking...
                
                // License lookup
                try {
                    Optional<License> optionalLicense = licenseRepository.getLicense(BCrypt.hashpw(key, licensesSalt), product);
                    if (optionalLicense.isEmpty() // License not found or owned by someone else
                            || (!optionalLicense.get().isOwner(user.getIdLong()))) {
                        event.getHook().sendMessageEmbeds(buildEmbed(new EmbedBuilder()
                                                                         .setColor(Color.RED)
                                                                         .setTitle("License not found")
                                                                         .setDescription("Could not locate the license you were looking for")
                        )).queue(); // Send the error message
                        return;
                    }
                    License license = optionalLicense.get(); // The found license
                    String obfuscateKey = MiscUtils.obfuscateKey(key); // Obfuscate the key
                    long expires = license.isPermanent() ? -1L : license.getExpires().getTime() / 1000L;
                    long lastUsed = license.getLastUsed() == null ? -1L : license.getExpires().getTime() / 1000L;
                    event.getHook().sendMessageEmbeds(buildEmbed(new EmbedBuilder()
                                                                     .setColor(Color.BLUE)
                                                                     .setTitle("Your License")
                                                                     .addField("License", "`" + obfuscateKey + "`", true)
                                                                     .addField("Product", license.getProduct(), true)
                                                                     .addField("Description", license.getDescription(), true)
                                                                     .addField("Expiration",
                                                                         expires == -1L ? "Never" : "<t:" + expires + ":R>",
                                                                         true
                                                                     )
                                                                     .addField("Uses", String.valueOf(license.getUses()), true)
                                                                     .addField("Last Used",
                                                                         lastUsed == -1L ? "Never" : "<t:" + lastUsed + ":R>",
                                                                         true
                                                                     )
                                                                     .addField("IPs",
                                                                         license.getIps().size() + "/" + license.getIpLimit(),
                                                                         true
                                                                     )
                                                                     .addField("HWIDs",
                                                                         license.getHwids().size() + "/" + license.getHwidLimit(),
                                                                         true
                                                                     )
                                                                     .addField("Created",
                                                                         "<t:" + (license.getCreated().getTime() / 1000L) + ":R>",
                                                                         true
                                                                     )
                    )).addActionRow( // Buttons
                        Button.danger(CLEAR_IPS_BUTTON_ID, "Clear IPs")
                            .withEmoji(Emoji.fromUnicode("🗑️")),
                        Button.danger(CLEAR_HWIDS_BUTTON_ID, "Clear HWIDs")
                            .withEmoji(Emoji.fromUnicode("🗑️"))
                    ).queue(message -> cachedLicenses.put(message.getIdLong(), license)); // Cache the license for the message
                } catch (Exception ex) {
                    event.getHook().sendMessageEmbeds(buildEmbed(new EmbedBuilder()
                                                                     .setColor(Color.RED)
                                                                     .setTitle("Lookup Failed")
                                                                     .setDescription("More information has been logged")
                    )).queue(); // Send the error message
                    ex.printStackTrace();
                }
            }
        }
        
        @Override
        public void onButtonInteraction(@NonNull ButtonInteractionEvent event) {
            User user = event.getUser(); // The user who clicked the button
            String componentId = event.getComponentId(); // The button id
            
            // License Actions
            boolean clearIps = componentId.equals(CLEAR_IPS_BUTTON_ID);
            boolean clearHwids = componentId.equals(CLEAR_HWIDS_BUTTON_ID);
            if (clearIps || clearHwids) {
                event.deferReply(true).queue(); // Send thinking...
                License license = cachedLicenses.getIfPresent(event.getMessageIdLong()); // Get the cached license
                if (license == null || (!license.isOwner(user.getIdLong()))) { // License not found or owned by someone else
                    event.getHook().sendMessageEmbeds(buildEmbed(new EmbedBuilder()
                                                                     .setColor(Color.RED)
                                                                     .setTitle("License Action Failed")
                                                                     .setDescription("The license couldn't be found or the action timed out")
                    )).queue(); // Send the error message
                    return;
                }
                try {
                    // Clear IPs
                    if (clearIps) {
                        license.setIps(new HashSet<>());
                    }
                    // Clear HWIDs
                    if (clearHwids) {
                        license.setHwids(new HashSet<>());
                    }
                    licenseRepository.save(license); // Save the license
                    event.getHook().sendMessageEmbeds(buildEmbed(new EmbedBuilder()
                                                                     .setColor(Color.GREEN)
                                                                     .setTitle("Cleared " + (clearIps ? "IP" : "HWID") + "s")
                    )).queue(); // Inform action success
                } catch (Exception ex) {
                    event.getHook().sendMessageEmbeds(buildEmbed(new EmbedBuilder()
                                                                     .setColor(Color.RED)
                                                                     .setTitle("License Action Failed")
                                                                     .setDescription("More information has been logged")
                    )).queue(); // Send the error message
                    ex.printStackTrace();
                }
            }
        }
    }
}