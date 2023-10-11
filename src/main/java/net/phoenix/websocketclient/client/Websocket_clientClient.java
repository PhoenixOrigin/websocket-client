package net.phoenix.websocketclient.client;

import com.google.gson.JsonObject;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.phoenix.websocketclient.SimpleConfig;
import net.phoenix.websocketclient.WebsocketHandler;
import net.phoenix.websocketclient.commands.WebsocketCommand;

import java.net.URI;
import java.net.URISyntaxException;

public class Websocket_clientClient implements ClientModInitializer {

    public static SimpleConfig config = null;
    public static WebsocketHandler websocket = null;

    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralCommandNode<FabricClientCommandSource> node = dispatcher.register(new WebsocketCommand().build());
            dispatcher.register(ClientCommandManager.literal("ws").redirect(node));
        });

        config = SimpleConfig.of("websocket-client/settings").request();
        connectWebsocket();

        ClientReceiveMessageEvents.CHAT.register(((message, signedMessage, sender, params, receptionTimestamp) -> handleText(message)));
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            handleText(message);
        });
        ClientReceiveMessageEvents.GAME_CANCELED.register(((message, overlay) -> {
            if (overlay) return;
            handleText(message);
        }));
    }

    private void handleText(Text message) {
        if (websocket.isClosed()) return;

        String token = config.get("token");
        MinecraftClient client = MinecraftClient.getInstance();
        String username = client.player.getName().getString();
        String uuid = client.player.getUuidAsString();

        JsonObject obj = new JsonObject();
        obj.addProperty("wsToken", token);
        obj.addProperty("username", username);
        obj.addProperty("uuid", uuid);
        obj.addProperty("message", message.getString().replace("\n", "\\n"));

        websocket.send(obj.toString());
    }

    public static void connectWebsocket(){
        try {
            URI uri = new URI(config.get("ws-url"));
            websocket = new WebsocketHandler(uri);
            websocket.connect();
            websocket.setMessageHandler(new WebsocketHandler.MessageHandler() {
                @Override
                public void handleMessage(String message) {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (message.equals("$SERVERPING")) {
                        websocket.send("$CLIENTPONG");
                        return;
                    }
                    client.execute(() -> {
                        try {
                            client.player.sendMessage(Text.literal(message));
                        } catch (NullPointerException ignored) {
                        }
                    });
                }
            });
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}