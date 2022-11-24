package a.b;

import a.b.mixin.AccessorServer;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class B implements ModInitializer {
    public int doStuff(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var player = UuidArgumentType.getUuid(context, "player");
        var world = context.getSource().getWorld();
        var server = world.getServer();
        server.getSaveProperties().getPlayerData();
        var worldSaveHandler = ((AccessorServer) server).invSee$saveHandler();
        var sp = new ServerPlayerEntity(server, world, new GameProfile(player, "bop"), null);
        var data = worldSaveHandler.loadPlayerData(sp);
        if (data == null) {
            context.getSource().sendError(Text.literal("Could not find player with uuid " + player));
            return 0;
        }
        var type = InvViewArgType.getInvViewType(context, "type");
        var viewer = context.getSource().getPlayerOrThrow();
        viewer.openHandledScreen(new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Text.literal("Corpse Looter 9000");
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
                var inventory = switch (type) {
                    case ENDERCHEST -> sp.getEnderChestInventory();
                    case INVENTORY -> sp.getInventory();
                };
                SimpleInventory simpleInventory = new SimpleInventory(54);
                for (int i = 0; i < 54 && i < inventory.size(); i++) {
                    simpleInventory.setStack(i, inventory.getStack(i));
                }
                return GenericContainerScreenHandler.createGeneric9x6(syncId, inv, simpleInventory);
            }
        });
        return 1;
    }

    @Override
    public void onInitialize() {
        ArgumentTypeRegistry.registerArgumentType(Identifier.of("invview", "invviewtpye"),
                InvViewArgType.class,
                ConstantArgumentSerializer.of(InvViewArgType::argType));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("invsee")
                    .then(argument("player", UuidArgumentType.uuid())
                            .then(argument("type", InvViewArgType.argType())
                                    .executes(context -> {
                                        try {
                                            return doStuff(context);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            return 0;
                                        }
                                    })))
            );
        });
    }

    public enum InvViewType implements StringIdentifiable {
        ENDERCHEST, INVENTORY;

        @Override
        public String asString() {
            return name();
        }
    }

    public static class InvViewArgType extends EnumArgumentType<InvViewType> {
        protected InvViewArgType() {
            super(StringIdentifiable.createCodec(InvViewType::values), InvViewType::values);
        }

        public static InvViewArgType argType() {
            return new InvViewArgType();
        }

        public static InvViewType getInvViewType(CommandContext<ServerCommandSource> context, String id) {
            return context.getArgument(id, InvViewType.class);
        }
    }
}
