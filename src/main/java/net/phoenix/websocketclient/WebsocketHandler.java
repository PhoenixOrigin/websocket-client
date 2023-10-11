package net.phoenix.websocketclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Map;

public class WebsocketHandler extends WebSocketClient {

    public boolean dc = false;
    private MessageHandler messageHandler = null;

    public WebsocketHandler(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public WebsocketHandler(URI serverURI) {
        super(serverURI);
    }

    public WebsocketHandler(URI serverUri, Map<String, String> httpHeaders) {
        super(serverUri, httpHeaders);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Connected to websocket"));
            }
        });
    }

    @Override
    public void onMessage(String message) {
        messageHandler.handleMessage(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Disconnected from websocket"));
            }
        });
        if (dc) return;
        client.execute(super::reconnect);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    public abstract static class MessageHandler {
        public abstract void handleMessage(String message);
    }

}