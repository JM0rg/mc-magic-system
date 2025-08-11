package com.magicsystem.commands;

import com.magicsystem.MagicSystemMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class CastCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("cast")
            .requires(source -> source.hasPermissionLevel(0)) // All players can use
            .then(CommandManager.argument("spell", StringArgumentType.word())
                .executes(context -> {
                    String spellId = StringArgumentType.getString(context, "spell");
                    ServerCommandSource source = context.getSource();
                    
                    if (source.getPlayer() == null) {
                        source.sendMessage(Text.literal("§cThis command can only be used by players!"));
                        return 0;
                    }
                    
                    boolean success = MagicSystemMod.getSpellManager().castSpell(source.getPlayer(), spellId);
                    
                    if (!success) {
                        source.sendMessage(Text.literal("§cFailed to cast spell: " + spellId));
                    }
                    
                    return success ? 1 : 0;
                })
            )
            .executes(context -> {
                context.getSource().sendMessage(Text.literal("§cUsage: /cast <spell>"));
                return 0;
            })
        );
    }
}
