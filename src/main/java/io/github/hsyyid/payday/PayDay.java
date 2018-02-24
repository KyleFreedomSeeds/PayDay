package io.github.hsyyid.payday;

import com.google.inject.Inject;
import io.github.hsyyid.payday.utils.Utils;
import io.github.nucleuspowered.nucleus.api.service.NucleusAFKService;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map.Entry;
import java.util.Optional;

@Plugin(id = "payday", name = "PayDay", version = "1.4.0", description = "Pay your players as they play.", dependencies = {@Dependency(
        id = "nucleus", optional = true)})
public class PayDay {

    public static ConfigurationNode config;
    public static ConfigurationLoader<CommentedConfigurationNode> configurationManager;
    public static EconomyService economyService;
    private static PayDay instance;
    private Task task;
    private boolean functional = false;
    private Optional<NucleusAFKService> afkService = Optional.empty();

    @Inject private Logger logger;

    @Inject private PluginContainer container;

    public Logger getLogger() {
        return logger;
    }

    @Inject @DefaultConfig(sharedRoot = true) private File dConfig;

    @Inject @DefaultConfig(sharedRoot = true) private ConfigurationLoader<CommentedConfigurationNode> confManager;

    @Listener
    public void onGameInit(GameInitializationEvent event) {
        instance = this;
        getLogger().info("PayDay loading...");

        try {
            if (!dConfig.exists()) {
                dConfig.createNewFile();
                config = confManager.load();
                confManager.save(config);
            }

            configurationManager = confManager;
            config = confManager.load();
        } catch (IOException exception) {
            getLogger().error("The default configuration could not be loaded or created!");
        }

        // Setup other config options
        Utils.getJoinPay();
        Utils.enableAfkPay();

        Task.Builder taskBuilder = Sponge.getScheduler().createTaskBuilder();

        task = taskBuilder.execute(task ->
        {
            for (Player player : Sponge.getServer().getOnlinePlayers()) {
                // Check if the player is afk
                if (!Utils.enableAfkPay() && afkService.isPresent() && afkService.get().isAFK(player)) {
                    continue;
                }
                for (Entry<String, BigDecimal> entry : Utils.getPaymentAmounts().entrySet()) {
                    if (entry.getKey().equals("*") || player.hasPermission(entry.getKey())) {
                        BigDecimal pay = entry.getValue();
                        player.sendMessage(Utils.getSalaryMessage(pay));
                        UniqueAccount uniqueAccount = economyService.getOrCreateAccount(player.getUniqueId()).get();
                        uniqueAccount.deposit(economyService.getDefaultCurrency(), pay, Cause.of(EventContext.empty(), container));
                    }
                }
            }
        }).interval(Utils.getTimeAmount(), Utils.getTimeUnit()).name("PayDay - Pay").submit(this);

        getLogger().info("-----------------------------");
        getLogger().info("PayDay was made by HassanS6000!");
        getLogger().info("Patched to APIv5 by Kostronor from the Minecolonies team!");
        getLogger().info("Further updated by Flibio!");
        getLogger().info("Please post all errors on the Sponge Thread or on GitHub!");
        getLogger().info("Have fun, and enjoy! :D");
        getLogger().info("-----------------------------");
        getLogger().info("PayDay loaded!");
    }

    @Listener
    public void onChangeServiceProvider(ChangeServiceProviderEvent event) {
        if (Sponge.getPluginManager().getPlugin("nucleus").isPresent()) {
            if (event.getService().equals(NucleusAFKService.class)) {
                Object raw = event.getNewProviderRegistration().getProvider();
                if (raw instanceof NucleusAFKService) {
                    afkService = Optional.of((NucleusAFKService) raw);
                }
            }
        }
    }

    @Listener
    public void onGamePostInit(GamePostInitializationEvent event) {
        Optional<EconomyService> econService = Sponge.getServiceManager().provide(EconomyService.class);

        if (econService.isPresent()) {
            economyService = econService.get();
            functional = true;

            // Setup messages
            getLogger().info("Initializing messages config!");
            Utils.getFirstJoinMessage(BigDecimal.ONE);
            Utils.getSalaryMessage(BigDecimal.ONE);
        } else {
            getLogger().error("Error! There is no Economy plugin found on this server, PayDay will not work correctly!");
            task.cancel();
            functional = false;
        }
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        if (!Utils.getJoinPay() || !functional) {
            return;
        }
        Player player = event.getTargetEntity();

        for (Entry<String, BigDecimal> entry : Utils.getPaymentAmounts().entrySet()) {
            if (entry.getKey().equals("*") || player.hasPermission(entry.getKey())) {
                BigDecimal pay = entry.getValue();
                player.sendMessage(Utils.getFirstJoinMessage(pay));
                UniqueAccount uniqueAccount = economyService.getOrCreateAccount(player.getUniqueId()).get();
                uniqueAccount.deposit(economyService.getDefaultCurrency(), pay, Cause.of(EventContext.empty(), container));
            }
        }
    }

    public static PayDay getInstance() {
        return instance;
    }

    public static ConfigurationLoader<CommentedConfigurationNode> getConfigManager() {
        return configurationManager;
    }
}
