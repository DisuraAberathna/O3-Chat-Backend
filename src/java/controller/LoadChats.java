/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import entity.Chat;
import entity.ChatStatus;
import entity.User;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.HibernateUtil;
import model.Validate;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author SINGER
 */
@WebServlet(name = "LoadChats", urlPatterns = {"/LoadChats"})
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
            Session session = HibernateUtil.getSessionFactory().openSession();

            try {
                User loggedInUser = (User) session.get(User.class, Integer.valueOf(loggedInId));
                User otherUser = (User) session.get(User.class, Integer.valueOf(otherId));

                Criteria chatCriteria = session.createCriteria(Chat.class);
                chatCriteria.add(Restrictions.or(
                        Restrictions.and(
                                Restrictions.eq("from", loggedInUser),
                                Restrictions.eq("to", otherUser)
                        ),
                        Restrictions.and(
                                Restrictions.eq("to", loggedInUser),
                                Restrictions.eq("from", otherUser)
                        )));
                chatCriteria.addOrder(Order.asc("id"));
                List<Chat> chatList = chatCriteria.list();

                Criteria readedStatusCriteria = session.createCriteria(ChatStatus.class);
                readedStatusCriteria.add(Restrictions.eq("name", "Readed"));
                ChatStatus readedChatStatus = (ChatStatus) readedStatusCriteria.uniqueResult();

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

                        String condition1 = serverPath + File.separator + "images" + File.separator + "chat" + File.separator + loggedInUser.getId() + "-" + otherUser.getId() + File.separator + chat.getImg();
                        File condition1File = new File(condition1);

                        if (condition1File.exists()) {
                            chatObject.addProperty("img", "images//chat//" + loggedInUser.getId() + "-" + otherUser.getId() + "//" + chat.getImg());
                        }

                        String condition2 = serverPath + File.separator + "images" + File.separator + "chat" + File.separator + otherUser.getId() + "-" + loggedInUser.getId() + File.separator + chat.getImg();
                        File condition2File = new File(condition2);

                        if (condition2File.exists()) {
                            chatObject.addProperty("img", "images//chat//" + otherUser.getId() + "-" + loggedInUser.getId() + "//" + chat.getImg());
                        }
                    }
                    chatObject.addProperty("time", new SimpleDateFormat("hh:mm a").format(chat.getDateTime()));
                    chatObject.addProperty("status", chat.getChatStatus().getId());
                    if (chat.getFrom().equals(loggedInUser)) {
                        chatObject.addProperty("side", "right");
                    } else {
                        chatObject.addProperty("side", "left");
                    }
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
                        chat.setChatStatus(readedChatStatus);
                        session.update(chat);
                        session.beginTransaction().commit();
                    }
                }

                responseObject.add("chats", chats);
                responseObject.addProperty("ok", true);
            } catch (NumberFormatException e) {
                System.out.println(e.getMessage());
                responseObject.addProperty("msg", "Can not process this request!");
            }
            session.close();
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseObject));

    }

}
