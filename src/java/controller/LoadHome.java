/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 *
 * @author SINGER
 */
@ServerEndpoint("/LoadHome")
public class LoadHome {

    private static final Map<Session, JsonObject> clients = new HashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("WebSocket connection opened: " + session.getId());
        clients.put(session, new JsonObject());
    }

    @OnError
    public void onError(Throwable error, Session session) {
        System.out.println("error on -> " + session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        if (clients.containsKey(session)) {
            Home.updateOfline(clients.get(session));
            clients.remove(session);
            System.out.println("Update client offline");
        }
        System.out.println("WebSocket connection closed: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("Received message from -> " + session.getId() + "\n" + message);

        if (message.startsWith("user:")) {
            String jsonMessage = message.replace("user:", "");
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonMessage, JsonObject.class);
            clients.put(session, jsonObject);
            Home.updateOnline(jsonObject);
            System.out.println("Update client online");
            JsonObject resp = Home.loadData(jsonObject);
            try {
                session.getBasicRemote().sendText("chatList:" + resp);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            } 
        }
    }

}
