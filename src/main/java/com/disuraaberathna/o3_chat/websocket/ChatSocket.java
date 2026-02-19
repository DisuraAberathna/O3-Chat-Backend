package com.disuraaberathna.o3_chat.websocket;

import com.disuraaberathna.o3_chat.entity.Chat;
import com.disuraaberathna.o3_chat.entity.ChatStatus;
import com.disuraaberathna.o3_chat.entity.User;
import com.disuraaberathna.o3_chat.model.HibernateUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/chat/{userId}")
public class ChatSocket {

    private static final Map<String, java.util.Set<jakarta.websocket.Session>> sessions = new ConcurrentHashMap<>();
    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    @OnOpen
    public void onOpen(jakarta.websocket.Session session, @PathParam("userId") String userId) {
        sessions.computeIfAbsent(userId, k -> java.util.Collections.newSetFromMap(new ConcurrentHashMap<>())).add(session);
    }

    @OnClose
    public void onClose(jakarta.websocket.Session session, @PathParam("userId") String userId) {
        java.util.Set<jakarta.websocket.Session> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.remove(session);
            if (userSessions.isEmpty()) {
                sessions.remove(userId);
            }
        }
    }

    @OnError
    public void onError(jakarta.websocket.Session session, Throwable throwable) {
    }

    @OnMessage
    public void onMessage(String message, jakarta.websocket.Session session, @PathParam("userId") String senderId) {
        try {
            JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
            String type = jsonMessage.has("type") ? jsonMessage.get("type").getAsString() : "";

            if ("send".equals(type)) {
                handleSendMessage(jsonMessage, senderId, session);
            } else if ("seen".equals(type)) {
                handleSeenMessage(jsonMessage, senderId);
            } else if ("load_chats".equals(type)) {
                handleLoadChatList(jsonMessage, senderId);
            } else if ("load_chat_history".equals(type)) {
                handleLoadChatHistory(jsonMessage, senderId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleLoadChatHistory(JsonObject jsonMessage, String userId) {
        String otherUserId = jsonMessage.has("other_id") && !jsonMessage.get("other_id").isJsonNull() ? jsonMessage.get("other_id").getAsString() : "";

        if (otherUserId == null || otherUserId.isEmpty()) return;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User loggedInUser = session.find(User.class, Integer.valueOf(userId));
            User otherUser = session.find(User.class, Integer.valueOf(otherUserId));

            if (loggedInUser != null && otherUser != null) {
                CriteriaBuilder cb = session.getCriteriaBuilder();
                jakarta.persistence.criteria.CriteriaQuery<Chat> chatQuery = cb.createQuery(Chat.class);
                jakarta.persistence.criteria.Root<Chat> chatRoot = chatQuery.from(Chat.class);

                chatQuery.select(chatRoot)
                        .where(
                                cb.or(
                                        cb.and(
                                                cb.equal(chatRoot.get("from"), loggedInUser),
                                                cb.equal(chatRoot.get("to"), otherUser)
                                        ),
                                        cb.and(
                                                cb.equal(chatRoot.get("to"), loggedInUser),
                                                cb.equal(chatRoot.get("from"), otherUser)
                                        )
                                )
                        )
                        .orderBy(cb.asc(chatRoot.get("id")));

                List<Chat> chatList = session.createQuery(chatQuery).getResultList();

                String folderName = "";
                if (!chatList.isEmpty()) {
                    Chat firstChat = chatList.get(0);
                    folderName = firstChat.getFrom().getId() + "-" + firstChat.getTo().getId();
                }

                ChatStatus readedStatus = getChatStatus(session, "Seen");
                if (readedStatus == null) readedStatus = getChatStatus(session, "Readed");
                if (readedStatus == null) readedStatus = getChatStatus(session, "Read");

                com.google.gson.JsonArray chats = new com.google.gson.JsonArray();
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");

                Transaction tx = null;
                boolean transactionActive = false;

                for (Chat chat : chatList) {
                    if (chat.getTo().getId() == loggedInUser.getId() && readedStatus != null) {
                        if (chat.getChatStatus() == null || chat.getChatStatus().getId() != readedStatus.getId()) {
                            if (!transactionActive) {
                                tx = session.beginTransaction();
                                transactionActive = true;
                            }
                            chat.setChatStatus(readedStatus);
                            session.merge(chat);

                            String senderId = String.valueOf(chat.getFrom().getId());
                            if (sessions.containsKey(senderId)) {
                                JsonObject payload = new JsonObject();
                                payload.addProperty("type", "seen");
                                payload.addProperty("chat_id", chat.getId());
                                payload.addProperty("seen_by", userId);
                                for (jakarta.websocket.Session s : sessions.get(senderId)) {
                                    if (s.isOpen()) s.getAsyncRemote().sendText(gson.toJson(payload));
                                }
                            }
                            
                            if (sessions.containsKey(userId)) {
                                JsonObject payload = new JsonObject();
                                payload.addProperty("type", "seen");
                                payload.addProperty("chat_id", chat.getId());
                                payload.addProperty("seen_by", userId);
                                for (jakarta.websocket.Session s : sessions.get(userId)) {
                                    if (s.isOpen()) s.getAsyncRemote().sendText(gson.toJson(payload));
                                }
                            }
                        }
                    }

                    JsonObject chatObject = new JsonObject();
                    chatObject.addProperty("id", chat.getId());
                    chatObject.addProperty("fromUser", chat.getFrom().getF_name() + " " + chat.getFrom().getL_name());
                    chatObject.addProperty("toUser", chat.getTo().getF_name() + " " + chat.getTo().getL_name());
                    chatObject.addProperty("from_id", chat.getFrom().getId());
                    chatObject.addProperty("to_id", chat.getTo().getId());

                    if (chat.getMessage() != null) {
                        chatObject.addProperty("msg", chat.getMessage());
                    }

                    if (chat.getImg() != null) {
                        if (chat.getImg().startsWith("http")) {
                            chatObject.addProperty("img", chat.getImg());
                        } else if (!folderName.isEmpty()) {
                            chatObject.addProperty("img", "images//chat//" + folderName + "//" + chat.getImg());
                        }
                    }

                    chatObject.addProperty("time", timeFormat.format(chat.getDateTime()));
                    if (chat.getChatStatus() != null) {
                        chatObject.addProperty("status", chat.getChatStatus().getId());
                    }

                    chatObject.addProperty("side", chat.getFrom().getId() == loggedInUser.getId() ? "right" : "left");

                    if (chat.getReply() != null) {
                        chatObject.addProperty("replyUser", chat.getReply().getFrom().getF_name() + " " + chat.getReply().getFrom().getL_name());
                        chatObject.addProperty("replyTime", timeFormat.format(chat.getReply().getDateTime()));
                        if (chat.getReply().getMessage() != null) {
                            chatObject.addProperty("replyMsg", chat.getReply().getMessage());
                        }
                        if (chat.getReply().getImg() != null) {
                            if (chat.getReply().getImg().startsWith("http")) {
                                chatObject.addProperty("replyImg", chat.getReply().getImg());
                            } else {
                                chatObject.addProperty("replyImg", "images//chat//" + folderName + "//" + chat.getReply().getImg());
                            }
                        }
                    }

                    chats.add(chatObject);
                }

                if (transactionActive && tx != null) {
                    tx.commit();
                }

                JsonObject response = new JsonObject();
                response.addProperty("type", "chat_history");
                response.add("chats", chats);

                if (sessions.containsKey(userId)) {
                    for (jakarta.websocket.Session s : sessions.get(userId)) {
                        if (s.isOpen()) s.getAsyncRemote().sendText(gson.toJson(response));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleLoadChatList(JsonObject jsonMessage, String userId) {
        String searchText = jsonMessage.has("searchText") ? jsonMessage.get("searchText").getAsString() : "";

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User loggedInUser = session.find(User.class, Integer.valueOf(userId));
            CriteriaBuilder cb = session.getCriteriaBuilder();

            List<User> searchResults = new java.util.ArrayList<>();
            if (searchText != null && !searchText.isEmpty()) {
                jakarta.persistence.criteria.CriteriaQuery<User> userSearchQuery = cb.createQuery(User.class);
                jakarta.persistence.criteria.Root<User> userRoot = userSearchQuery.from(User.class);
                userSearchQuery.select(userRoot)
                        .where(cb.like(userRoot.get("f_name"), "%" + searchText + "%")); 
                searchResults = session.createQuery(userSearchQuery).getResultList();
            }

            ChatStatus deliveredStatus = getChatStatus(session, "Delivered");
            if (deliveredStatus == null) deliveredStatus = getChatStatus(session, "Sent");

            jakarta.persistence.criteria.CriteriaQuery<Chat> chatQuery = cb.createQuery(Chat.class);
            jakarta.persistence.criteria.Root<Chat> chatRoot = chatQuery.from(Chat.class);
            chatQuery.select(chatRoot)
                    .where(cb.or(
                            cb.equal(chatRoot.get("from"), loggedInUser),
                            cb.equal(chatRoot.get("to"), loggedInUser)
                    ))
                    .orderBy(cb.desc(chatRoot.get("id")));

            java.util.List<Chat> chatList = session.createQuery(chatQuery).getResultList();

            java.util.Set<Integer> addedUserIds = new java.util.HashSet<>();
            com.google.gson.JsonArray usersArray = new com.google.gson.JsonArray();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat timeFormatToday = new SimpleDateFormat("hh:mm a");
            SimpleDateFormat timeFormatPast = new SimpleDateFormat("yyyy/MM/dd");

            ChatStatus finalDeliveredStatus = deliveredStatus;

            for (Chat chat : chatList) {
                if (chat == null) continue;

                User otherUser;
                if (chat.getFrom().getId() == chat.getTo().getId()) {
                    otherUser = chat.getFrom(); 
                } else {
                    otherUser = chat.getFrom().getId() == loggedInUser.getId() ? chat.getTo() : chat.getFrom();
                }

                if (otherUser == null || addedUserIds.contains(otherUser.getId())) continue;

                if (!searchText.isEmpty()) {
                    boolean found = false;
                    for (User u : searchResults) {
                        if (u.getId() == otherUser.getId()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) continue;
                }

                JsonObject jsonChatItem = new JsonObject();
                jsonChatItem.addProperty("id", otherUser.getId());

                if (otherUser.getId() == loggedInUser.getId()) { 
                    jsonChatItem.addProperty("name", loggedInUser.getF_name() + " (You)");
                    jsonChatItem.addProperty("bio", "Message yourself");
                } else {
                    jsonChatItem.addProperty("name", otherUser.getF_name() + " " + otherUser.getL_name());
                    jsonChatItem.addProperty("bio", otherUser.getBio());
                }

                jsonChatItem.addProperty("profile_img", otherUser.getProfile_url());

                String formattedTime;
                try {
                    formattedTime = dateFormat.format(chat.getDateTime()).equals(dateFormat.format(new Date()))
                            ? timeFormatToday.format(chat.getDateTime())
                            : timeFormatPast.format(chat.getDateTime());
                } catch (Exception e) {
                    formattedTime = "";
                }
                jsonChatItem.addProperty("time", formattedTime);

                if (chat.getMessage() != null && !chat.getMessage().trim().isEmpty()) {
                    jsonChatItem.addProperty("msg", chat.getMessage());
                } else if (chat.getImg() != null) {
                    jsonChatItem.addProperty("msg", "Image");
                }

                jsonChatItem.addProperty("view", chat.getFrom().getId() == loggedInUser.getId());

                if (chat.getChatStatus() != null) {
                    jsonChatItem.addProperty("status", chat.getChatStatus().getId());
                }

                long unreadCount = 0;
                if (finalDeliveredStatus != null) {
                    unreadCount = chatList.stream().filter(c ->
                            c.getTo().getId() == loggedInUser.getId() &&
                            c.getFrom().getId() == chat.getFrom().getId() &&
                            c.getChatStatus().getId() == finalDeliveredStatus.getId()
                    ).count();
                }
                jsonChatItem.addProperty("count", unreadCount);

                usersArray.add(jsonChatItem);
                addedUserIds.add(otherUser.getId());
            }

            JsonObject response = new JsonObject();
            response.addProperty("type", "chat_list");
            response.add("chatList", usersArray);

            if (sessions.containsKey(userId)) {
                for (jakarta.websocket.Session s : sessions.get(userId)) {
                    if (s.isOpen()) s.getAsyncRemote().sendText(gson.toJson(response));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSendMessage(JsonObject jsonMessage, String senderId, jakarta.websocket.Session wsSession) {
        String toId = jsonMessage.has("to_id") && !jsonMessage.get("to_id").isJsonNull() ? jsonMessage.get("to_id").getAsString() : null;
        String msgContent = jsonMessage.has("message") && !jsonMessage.get("message").isJsonNull() ? jsonMessage.get("message").getAsString() : null;

        if (msgContent != null && msgContent.trim().isEmpty()) {
            msgContent = null;
        }

        String replyId = jsonMessage.has("reply_id") && !jsonMessage.get("reply_id").isJsonNull() ? jsonMessage.get("reply_id").getAsString() : null;
        String img = jsonMessage.has("image") && !jsonMessage.get("image").isJsonNull() ? jsonMessage.get("image").getAsString() : null;

        if (toId != null && (msgContent != null || img != null)) {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                Transaction tx = session.beginTransaction();

                User fromUser = session.find(User.class, Integer.valueOf(senderId));
                User toUser = session.find(User.class, Integer.valueOf(toId));

                ChatStatus status = getChatStatus(session, "Delivered");
                if (status == null) status = getChatStatus(session, "Sent");
                if (status == null) status = getChatStatus(session, "Readed");
                if (status == null) status = getChatStatus(session, "Read");

                Chat chat = new Chat();
                chat.setFrom(fromUser);
                chat.setTo(toUser);
                chat.setMessage(msgContent);
                chat.setImg(img);
                chat.setDateTime(new Date());
                chat.setChatStatus(status);

                if (replyId != null && !replyId.isEmpty()) {
                    try {
                        Chat replyChat = session.find(Chat.class, Integer.valueOf(replyId));
                        if (replyChat != null) {
                            chat.setReply(replyChat);
                        }
                    } catch (NumberFormatException ignored) {}
                }

                session.persist(chat);
                tx.commit();

                JsonObject payload = new JsonObject();
                payload.addProperty("type", "message");
                payload.addProperty("id", chat.getId());
                payload.add("from", gson.toJsonTree(fromUser)); 
                
                payload.addProperty("from_id", fromUser.getId());
                payload.addProperty("to_id", toUser.getId());
                payload.addProperty("message", chat.getMessage());
                if (chat.getImg() != null) payload.addProperty("image", chat.getImg());
                payload.addProperty("date_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(chat.getDateTime()));
                payload.addProperty("status_id", chat.getChatStatus().getId());

                if (chat.getReply() != null) {
                    JsonObject replyObj = new JsonObject();
                    replyObj.addProperty("id", chat.getReply().getId());
                    replyObj.addProperty("message", chat.getReply().getMessage());
                    replyObj.addProperty("img", chat.getReply().getImg());
                    payload.add("reply", replyObj);
                }

                if (sessions.containsKey(toId)) {
                    for (jakarta.websocket.Session s : sessions.get(toId)) {
                        if (s.isOpen()) s.getAsyncRemote().sendText(gson.toJson(payload));
                    }
                }

                if (sessions.containsKey(senderId)) {
                    JsonObject ack = new JsonObject();
                    ack.addProperty("type", "ack");
                    ack.addProperty("status", "sent");
                    ack.addProperty("chat_id", chat.getId());
                    ack.addProperty("status_id", chat.getChatStatus().getId());
                    if (jsonMessage.has("temp_id") && !jsonMessage.get("temp_id").isJsonNull()) {
                        ack.addProperty("temp_id", jsonMessage.get("temp_id").getAsString());
                    }
                    for (jakarta.websocket.Session s : sessions.get(senderId)) {
                        if (s.isOpen()) s.getAsyncRemote().sendText(gson.toJson(ack));
                    }
                    
                    payload.addProperty("side", "right");
                    for (jakarta.websocket.Session s : sessions.get(senderId)) {
                        if (s.isOpen() && !s.getId().equals(wsSession.getId())) {
                            s.getAsyncRemote().sendText(gson.toJson(payload));
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleSeenMessage(JsonObject jsonMessage, String userId) {
        String chatId = jsonMessage.has("chat_id") ? jsonMessage.get("chat_id").getAsString() : null;

        if (chatId != null) {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                Transaction tx = session.beginTransaction();
                Chat chat = session.find(Chat.class, Integer.valueOf(chatId));

                if (chat != null) {
                    ChatStatus seenStatus = getChatStatus(session, "Seen");
                    if (seenStatus == null) seenStatus = getChatStatus(session, "Readed");
                    if (seenStatus == null) seenStatus = getChatStatus(session, "Read");

                    if (seenStatus != null) {
                        chat.setChatStatus(seenStatus);
                        session.merge(chat);
                        tx.commit();

                        String senderId = String.valueOf(chat.getFrom().getId());
                        if (sessions.containsKey(senderId)) {
                            JsonObject payload = new JsonObject();
                            payload.addProperty("type", "seen");
                            payload.addProperty("chat_id", chat.getId());
                            payload.addProperty("seen_by", userId);
                            for (jakarta.websocket.Session s : sessions.get(senderId)) {
                                if (s.isOpen()) s.getAsyncRemote().sendText(gson.toJson(payload));
                            }
                        }
                        
                        if (sessions.containsKey(userId)) {
                            JsonObject payload = new JsonObject();
                            payload.addProperty("type", "seen");
                            payload.addProperty("chat_id", chat.getId());
                            payload.addProperty("seen_by", userId);
                            for (jakarta.websocket.Session s : sessions.get(userId)) {
                                if (s.isOpen()) s.getAsyncRemote().sendText(gson.toJson(payload));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ChatStatus getChatStatus(Session session, String name) {
        try {
            Query<ChatStatus> query = session.createQuery("FROM ChatStatus WHERE name = :name", ChatStatus.class);
            query.setParameter("name", name);
            return query.uniqueResult();
        } catch (Exception e) {
            return null;
        }
    }
}
