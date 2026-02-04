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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

@WebServlet(name = "LoadChats", urlPatterns = {"/load-chats"})
public class LoadChats extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("ok", false);

        JsonObject reqObject = gson.fromJson(req.getReader(), JsonObject.class);
        String loggedInId = reqObject.get("loggedInId").getAsString();
        String otherId = reqObject.get("otherId").getAsString();

        if (loggedInId.isEmpty()) {
            responseObject.addProperty("msg", "Something went wrong! Please sign in again.");
        } else if (!Validate.isInteger(loggedInId)) {
            responseObject.addProperty("msg", "Cloudn't process this request! \\nYou are a third-party person.");
        } else if (otherId.isEmpty()) {
            responseObject.addProperty("msg", "Something went wrong! Please try again later.");
        } else if (!Validate.isInteger(otherId)) {
            responseObject.addProperty("msg", "Cloudn't process this request! \\nYou are a third-party person.");
        } else {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                User loggedInUser = session.find(User.class, Integer.valueOf(loggedInId));
                User otherUser = session.find(User.class, Integer.valueOf(otherId));

                CriteriaBuilder cb = session.getCriteriaBuilder();

                CriteriaQuery<Chat> chatQuery = cb.createQuery(Chat.class);
                Root<Chat> chatRoot = chatQuery.from(Chat.class);

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

                CriteriaQuery<ChatStatus> statusQuery = cb.createQuery(ChatStatus.class);
                Root<ChatStatus> statusRoot = statusQuery.from(ChatStatus.class);
                statusQuery.select(statusRoot)
                        .where(cb.equal(statusRoot.get("name"), "Readed"));

                ChatStatus readedChatStatus = session.createQuery(statusQuery)
                        .uniqueResult();

                JsonArray chats = new JsonArray();

                for (Chat chat : chatList) {
                    JsonObject chatObject = new JsonObject();
                    chatObject.addProperty("id", chat.getId());
                    chatObject.addProperty("fromUser", chat.getFrom().getF_name() + " " + chat.getFrom().getL_name());
                    chatObject.addProperty("toUser", chat.getTo().getF_name() + " " + chat.getTo().getL_name());

                    if (chat.getMessage() != null) {
                        chatObject.addProperty("msg", chat.getMessage());
                    }

                    if (chat.getImg() != null) {
                        String serverPath = req.getServletContext().getRealPath("");

                        String condition1 = serverPath + File.separator + "images" + File.separator + "chat"
                                + File.separator + loggedInUser.getId() + "-" + otherUser.getId() + File.separator + chat.getImg();
                        File condition1File = new File(condition1);

                        if (condition1File.exists()) {
                            chatObject.addProperty("img", "images//chat//" + loggedInUser.getId() + "-" + otherUser.getId() + "//" + chat.getImg());
                        }

                        String condition2 = serverPath + File.separator + "images" + File.separator + "chat"
                                + File.separator + otherUser.getId() + "-" + loggedInUser.getId() + File.separator + chat.getImg();
                        File condition2File = new File(condition2);

                        if (condition2File.exists()) {
                            chatObject.addProperty("img", "images//chat//" + otherUser.getId() + "-" + loggedInUser.getId() + "//" + chat.getImg());
                        }
                    }

                    chatObject.addProperty("time", new SimpleDateFormat("hh:mm a").format(chat.getDateTime()));
                    chatObject.addProperty("status", chat.getChatStatus().getId());

                    chatObject.addProperty("side", chat.getFrom().equals(loggedInUser) ? "right" : "left");

                    if (chat.getReply() != null) {
                        chatObject.addProperty("replyUser", chat.getReply().getFrom().getF_name() + " " + chat.getReply().getFrom().getL_name());
                        chatObject.addProperty("replyTime", new SimpleDateFormat("hh:mm a").format(chat.getReply().getDateTime()));

                        if (chat.getReply().getMessage() != null) {
                            chatObject.addProperty("replyMsg", chat.getReply().getMessage());
                        }

                        if (chat.getReply().getImg() != null) {
                            chatObject.addProperty("replyImg", "images//chat//" + loggedInUser.getId() + "-" + otherUser.getId() + "//" + chat.getReply().getImg());
                        }
                    }

                    chats.add(chatObject);

                    if (chat.getTo().equals(loggedInUser)) {
                        Transaction tx = session.beginTransaction();
                        chat.setChatStatus(readedChatStatus);
                        session.merge(chat);
                        tx.commit();
                    }
                }

                responseObject.add("chats", chats);
                responseObject.addProperty("ok", true);

                session.close();
            } catch (NumberFormatException | HibernateException e) {
                System.out.println(e.getMessage());
                responseObject.addProperty("msg", "Cannot process this request!");
            }
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseObject));

    }

}
