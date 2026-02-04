package com.disuraaberathna.o3_chat.controller;

import com.disuraaberathna.o3_chat.entity.Chat;
import com.disuraaberathna.o3_chat.entity.ChatStatus;
import com.disuraaberathna.o3_chat.entity.User;
import com.disuraaberathna.o3_chat.model.HibernateUtil;
import com.disuraaberathna.o3_chat.model.Validate;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@WebServlet(name = "LoadChatList", urlPatterns = {"/load-chat-list"})
public class LoadChatList extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("ok", false);

        JsonObject reqObject = gson.fromJson(req.getReader(), JsonObject.class);
        String id = reqObject.get("id").getAsString();
        String searchText = reqObject.get("searchText").getAsString();

        if (id.isEmpty()) {
            responseObject.addProperty("msg", "Something went wrong! Please sign in again.");
        } else if (!Validate.isInteger(id)) {
            responseObject.addProperty("msg", "Couldn't process this request! You are a third-party person.");
        } else {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {

                Transaction tx = session.beginTransaction();

                User loggedInUser = session.find(User.class, Integer.valueOf(id));

                CriteriaBuilder cb = session.getCriteriaBuilder();

                List<User> searchResults = new ArrayList<>();
                if (searchText != null && !searchText.isEmpty()) {
                    CriteriaQuery<User> userSearchQuery = cb.createQuery(User.class);
                    Root<User> userRoot = userSearchQuery.from(User.class);
                    userSearchQuery.select(userRoot)
                            .where(cb.like(userRoot.get("f_name"), "%" + searchText));
                    searchResults = session.createQuery(userSearchQuery).getResultList();
                }

                CriteriaQuery<ChatStatus> statusQuery = cb.createQuery(ChatStatus.class);
                Root<ChatStatus> statusRoot = statusQuery.from(ChatStatus.class);
                statusQuery.select(statusRoot)
                        .where(cb.equal(statusRoot.get("name"), "Delivered"));
                ChatStatus notReadedChatStatus = session.createQuery(statusQuery)
                        .uniqueResultOptional().orElse(null);

                CriteriaQuery<Chat> chatQuery = cb.createQuery(Chat.class);
                Root<Chat> chatRoot = chatQuery.from(Chat.class);
                chatQuery.select(chatRoot)
                        .where(cb.or(
                                cb.equal(chatRoot.get("from"), loggedInUser),
                                cb.equal(chatRoot.get("to"), loggedInUser)
                        ))
                        .orderBy(cb.desc(chatRoot.get("id")));
                List<Chat> chatList = session.createQuery(chatQuery).getResultList();

                Set<Integer> addedUserIds = new HashSet<>();
                JsonArray usersArray = new JsonArray();

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat timeFormatToday = new SimpleDateFormat("hh:mm a");
                SimpleDateFormat timeFormatPast = new SimpleDateFormat("yyyy/MM/dd");

                for (Chat chat : chatList) {
                    if (chat == null) {
                        continue;
                    }

                    User otherUser = chat.getFrom().equals(loggedInUser) ? chat.getTo() : chat.getFrom();
                    if (otherUser == null || addedUserIds.contains(otherUser.getId())) {
                        continue;
                    }

                    if (!searchResults.isEmpty() && !searchResults.contains(otherUser)) {
                        continue;
                    }

                    JsonObject jsonChatItem = new JsonObject();
                    jsonChatItem.addProperty("id", otherUser.getId());

                    if (otherUser.equals(loggedInUser)) {
                        jsonChatItem.addProperty("name", loggedInUser.getF_name() + " (You)");
                        jsonChatItem.addProperty("bio", "Message yourself");
                    } else {
                        jsonChatItem.addProperty("name", otherUser.getF_name() + " " + otherUser.getL_name());
                        jsonChatItem.addProperty("bio", otherUser.getBio());
                    }

                    jsonChatItem.addProperty("profile_img", "images//user//" + otherUser.getId() + "//" + otherUser.getId() + "avatar.png");

                    String formattedTime = dateFormat.format(chat.getDateTime()).equals(dateFormat.format(new Date()))
                            ? timeFormatToday.format(chat.getDateTime())
                            : timeFormatPast.format(chat.getDateTime());
                    jsonChatItem.addProperty("time", formattedTime);

                    if (chat.getMessage() != null) {
                        jsonChatItem.addProperty("msg", chat.getMessage());
                    } else if (chat.getImg() != null) {
                        jsonChatItem.addProperty("msg", "Image");
                    }

                    jsonChatItem.addProperty("view", chat.getFrom().equals(loggedInUser));

                    if (chat.getChatStatus() == null) {
                        continue;
                    }
                    jsonChatItem.addProperty("status", chat.getChatStatus().getId());

                    int unreadCount = (int) chatList.stream()
                            .filter(unreadChat
                                    -> unreadChat.getTo().equals(loggedInUser)
                            && unreadChat.getFrom().equals(chat.getFrom())
                            && unreadChat.getChatStatus().equals(notReadedChatStatus))
                            .count();

                    jsonChatItem.addProperty("count", unreadCount);

                    usersArray.add(jsonChatItem);
                    addedUserIds.add(otherUser.getId());
                }

                responseObject.add("chatList", usersArray);
                responseObject.addProperty("ok", true);

                tx.commit();

                session.close();
            } catch (NumberFormatException | HibernateException e) {
                responseObject.addProperty("msg", "Cannot process this request!");
            }

        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseObject));
    }
}
