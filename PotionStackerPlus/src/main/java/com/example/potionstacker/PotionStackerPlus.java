package com.example.potionstacker;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;


import java.util.*;
import java.util.stream.Collectors;

public class PotionStackerPlus extends JavaPlugin implements TabExecutor, Listener {

    private int maxStackSize;
    private List<String> enabledPotionTypes;
    private boolean useCustomEffects;
    private List<String> allowedEffects;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("potionstacker").setExecutor(this);
        getCommand("potionstacker").setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);
        loadConfigAndApply();
        getLogger().info("PotionStacker enabled!");
    }

    public void loadConfigAndApply() {
        FileConfiguration config = getConfig();
        this.maxStackSize = config.getInt("stack-size", 16);
        this.enabledPotionTypes = config.getStringList("enabled-potions");
        this.useCustomEffects = config.getBoolean("use-custom-effects", false);
        this.allowedEffects = config.getStringList("allowed-effects").stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());

        getLogger().info("Configured max stack size: " + maxStackSize);
        getLogger().info("Enabled potion types: " + enabledPotionTypes);
        getLogger().info("Use custom effects filtering: " + useCustomEffects);
        getLogger().info("Allowed effects: " + allowedEffects);
    }

    private boolean isPotionTypeEnabled(Material material) {
        return enabledPotionTypes.contains(material.toString());
    }

    private boolean hasAllowedEffects(ItemStack item) {
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return false;

        for (PotionEffect effect : meta.getCustomEffects()) {
            if (!allowedEffects.contains(effect.getType().getName().toUpperCase())) {
                return false;
            }
        }
        return true;
    }



    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (current == null || cursor == null) return;
        if (!isPotionTypeEnabled(current.getType()) || !isPotionTypeEnabled(cursor.getType())) return;
        if (!current.isSimilar(cursor)) return;

        if (useCustomEffects && (!hasAllowedEffects(current) || !hasAllowedEffects(cursor))) {
            event.setCancelled(true);
            return;
        }

        int maxStack = maxStackSize;
        int currentAmount = current.getAmount();
        int cursorAmount = cursor.getAmount();
        int combined = currentAmount + cursorAmount;

        if (combined <= maxStack) {
            // Merge fully
            ItemStack result = cursor.clone();
            result.setAmount(combined);
            event.setCurrentItem(result);
            event.setCursor(null);
        } else {
            // Merge partially
            int spaceLeft = maxStack - currentAmount;
            if (spaceLeft <= 0) return; // No room to merge

            ItemStack result = current.clone();
            result.setAmount(maxStack);
            ItemStack remaining = cursor.clone();
            remaining.setAmount(cursorAmount - spaceLeft);

            event.setCurrentItem(result);
            event.setCursor(remaining);
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerPickupPotion(EntityPickupItemEvent event) {
        // Only handle players picking up items
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack pickedUp = event.getItem().getItemStack();
        if (pickedUp == null) return;
        if (!isPotionTypeEnabled(pickedUp.getType())) return;
        if (useCustomEffects && !hasAllowedEffects(pickedUp)) return;

        PlayerInventory inv = player.getInventory();

        // Try to find existing potion stacks to merge into
        int amountToAdd = pickedUp.getAmount();

        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null) continue;
            if (!stack.isSimilar(pickedUp)) continue;

            int currentAmount = stack.getAmount();
            if (currentAmount < maxStackSize) {
                int spaceLeft = maxStackSize - currentAmount;
                int add = Math.min(spaceLeft, amountToAdd);

                stack.setAmount(currentAmount + add);
                amountToAdd -= add;

                inv.setItem(slot, stack);

                if (amountToAdd <= 0) {
                    // All merged, cancel the pickup event to prevent duplicate item
                    event.setCancelled(true);
                    event.getItem().remove();
                    return;
                }
            }
        }

        // If we still have leftover potions after merging into existing stacks,
        // they will be picked up normally (added as a new stack)
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("potionstacker")) return false;

        if (args.length == 0) {
            sender.sendMessage("Usage: /potionstacker <reload|setstack|addeffect|removeeffect>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                reloadConfig();
                loadConfigAndApply();
                sender.sendMessage("PotionStacker config reloaded.");
                return true;
            }
            case "setstack" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /potionstacker setstack <size>");
                    return true;
                }
                try {
                    int size = Integer.parseInt(args[1]);
                    getConfig().set("stack-size", size);
                    saveConfig();
                    loadConfigAndApply();
                    sender.sendMessage("Stack size set to " + size);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid number: " + args[1]);
                }
                return true;
            }
            case "addeffect" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /potionstacker addeffect <effect>");
                    return true;
                }
                String effect = args[1].toUpperCase();
                List<String> effects = getConfig().getStringList("allowed-effects");
                if (!effects.contains(effect)) {
                    effects.add(effect);
                    getConfig().set("allowed-effects", effects);
                    saveConfig();
                    loadConfigAndApply();
                    sender.sendMessage("Added allowed effect: " + effect);
                } else {
                    sender.sendMessage(effect + " is already allowed.");
                }
                return true;
            }
            case "removeeffect" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /potionstacker removeeffect <effect>");
                    return true;
                }
                String effect = args[1].toUpperCase();
                List<String> effects = getConfig().getStringList("allowed-effects");
                if (effects.remove(effect)) {
                    getConfig().set("allowed-effects", effects);
                    saveConfig();
                    loadConfigAndApply();
                    sender.sendMessage("Removed allowed effect: " + effect);
                } else {
                    sender.sendMessage(effect + " was not in the allowed list.");
                }
                return true;
            }
            default -> {
                sender.sendMessage("Unknown subcommand. Use reload, setstack, addeffect, or removeeffect.");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "setstack", "addeffect", "removeeffect");
        }
        return Collections.emptyList();
    }
}