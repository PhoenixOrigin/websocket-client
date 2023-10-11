package net.phoenix.websocketclient.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.phoenix.websocketclient.client.Websocket_clientClient;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class WebsocketCommand {

    private int terminate(CommandContext<FabricClientCommandSource> ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if(client.player == null) return -1;
        if(Websocket_clientClient.websocket.isClosed()) {
            MinecraftClient.getInstance().player.sendMessage(Text.literal("You are already disconnected the server!"));
            return 0;
        }
        Websocket_clientClient.websocket.dc = true;
        Websocket_clientClient.websocket.close();
        MinecraftClient.getInstance().player.sendMessage(Text.literal("You have disconnected from the server!"));
        return 0;
    }

    private int connect(CommandContext<FabricClientCommandSource> ctx) {
        if(Websocket_clientClient.websocket.isOpen()) {
            MinecraftClient.getInstance().player.sendMessage(Text.literal("You are already connected to the server!"));
            return 0;
        }
        Websocket_clientClient.websocket.dc = false;
        Websocket_clientClient.connectWebsocket();
        return 0;
    }

    private int manualSend(CommandContext<FabricClientCommandSource> ctx) {
        return send(ctx, "manualSend");
    }

    private int command(CommandContext<FabricClientCommandSource> ctx) {
        return send(ctx, "command");
    }

    private int execute(CommandContext<FabricClientCommandSource> ctx) {
        return send(ctx, "chat");
    }

    public int send(CommandContext<FabricClientCommandSource> ctx, String type) {
        String token = Websocket_clientClient.config.get("token");
        MinecraftClient client = MinecraftClient.getInstance();
        String username = client.player.getName().getString();
        String uuid = client.player.getUuidAsString();

        JsonObject obj = new JsonObject();
        obj.addProperty("wsToken", token);
        obj.addProperty("username", username);
        obj.addProperty("uuid", uuid);
        obj.addProperty("message", ctx.getArgument("content", String.class).replace("\n", "\\n"));
        obj.addProperty("type", type);

        Websocket_clientClient.websocket.send(obj.toString());
        return 0;
    }



    public LiteralArgumentBuilder<FabricClientCommandSource> build(){
        return literal("websocket")
                .then(literal("terminate").executes(this::terminate))
                .then(literal("send")
                        .then(argument("content", StringArgumentType.greedyString()).executes(this::manualSend)))
                .then(literal("command")
                        .then(argument("content", StringArgumentType.greedyString()).executes(this::command)))
                .then(literal("connect").executes(this::connect))
                .executes(this::execute);
    }



}
