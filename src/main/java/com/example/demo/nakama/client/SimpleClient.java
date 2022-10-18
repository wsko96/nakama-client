package com.example.demo.nakama.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.heroiclabs.nakama.AbstractSocketListener;
import com.heroiclabs.nakama.Channel;
import com.heroiclabs.nakama.ChannelMessageAck;
import com.heroiclabs.nakama.ChannelType;
import com.heroiclabs.nakama.Client;
import com.heroiclabs.nakama.DefaultClient;
import com.heroiclabs.nakama.Session;
import com.heroiclabs.nakama.SocketClient;
import com.heroiclabs.nakama.SocketListener;
import com.heroiclabs.nakama.api.ChannelMessage;

import nakama.com.google.gson.Gson;

public class SimpleClient {

    private static Logger log = LoggerFactory.getLogger(SimpleClient.class);

    public static void main(String[] args) throws Exception {
        final String deviceId = System.getProperty("client.device");

        if (StringUtils.isBlank(deviceId)) {
            System.out.println(
                    "Provide the unique device ID by the system property, 'client.device'. e.g. '-Dclient.device=gildong000'");
            System.exit(1);
        }

        log.info("Nakama Client Demo ({})...", deviceId);

        final Client client = new DefaultClient("defaultkey", "127.0.0.1", 7349, false);

        log.debug("Creating a socket client...");
        final SocketClient socket = client.createSocket();
        log.debug("A socket client created...");

        final SocketListener listener = new AbstractSocketListener() {
            @Override
            public void onDisconnect(final Throwable th) {
                log.info("!!! Socket disconnected !!!");
            }

            @Override
            public void onChannelMessage(final ChannelMessage message) {
                System.out.println("[Channel message] (from " + message.getSenderId() + ") " + message.getContent());
                System.out.print("[CHAT_MESSAGE] > ");
            }
        };

        log.debug("Authenticating a session by device ID: {} ...", deviceId);
        final Session session = client.authenticateDevice(deviceId).get();
        log.info("Session authToken: {}", session.getAuthToken());

        log.debug("Connecting a session through the socket...");
        socket.connect(session, listener).get();
        log.info("Socket connected...");

        // Joining a group chat
        final String roomName = "studyroom001";
        final boolean persistence = false;
        final boolean hidden = false;
        final Channel channel = socket.joinChat(roomName, ChannelType.ROOM, persistence, hidden)
                .get();
        log.info("Connected to dynamic room channel: {}", channel.getId());

        System.out.println("");
        System.out.println("[Session ready to chat. Session ID: " + session.getUserId());
        System.out.println("    Type 'quit' to exit.");
        System.out.println("");

        // Sending messages
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        System.out.print("[CHAT_MESSAGE] > ");
        String line = br.readLine();

        while (line != null) {
            line = line.trim();

            if ("quit".equals(line)) {
                break;
            }

            if (!line.isEmpty()) {
                final Map<String, String> messageContent = new HashMap<>();
                messageContent.put("message", line);
                ChannelMessageAck messageSendAck = socket.writeChatMessage(channel.getId(),
                        new Gson().toJson(messageContent, messageContent.getClass())).get();
            }

            System.out.print("[CHAT_MESSAGE] > ");
            line = br.readLine();
        }

        System.out.println("Bye!");
        socket.disconnect();
        client.disconnect();

        System.exit(0);
    }
}
