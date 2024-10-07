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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.HibernateUtil;
import model.Validate;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author SINGER
 */
@WebServlet(name = "LoadChatList", urlPatterns = {"/LoadChatList"})
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
            responseObject.addProperty("msg", "Cloudn't process this request! \\nYou are a third-party person.");
        } else {
            Session session = HibernateUtil.getSessionFactory().openSession();

            try {
                User loggedInUser = (User) session.get(User.class, Integer.valueOf(id));

                Criteria otherUserCriteria = session.createCriteria(User.class);
                if (!searchText.isEmpty()) {
                    otherUserCriteria.add(Restrictions.like("f_name", searchText + "%"));
                }
                List<User> otherUserList = otherUserCriteria.list();

                Criteria notReadedStatusCriteria = session.createCriteria(ChatStatus.class);
                notReadedStatusCriteria.add(Restrictions.eq("name", "Delivered"));
                ChatStatus notReadedChatStatus = (ChatStatus) notReadedStatusCriteria.uniqueResult();

                Criteria deletedStatusCriteria = session.createCriteria(ChatStatus.class);
                deletedStatusCriteria.add(Restrictions.eq("name", "Deleted"));
                ChatStatus deletedChatStatus = (ChatStatus) deletedStatusCriteria.uniqueResult();

                JsonArray usersArray = new JsonArray();

                for (User otherUser : otherUserList) {

                    Criteria chatUsersCriteria = session.createCriteria(Chat.class);
                    chatUsersCriteria.add(Restrictions.ne("chatStatus", deletedChatStatus));
                    chatUsersCriteria.add(
                            Restrictions.or(
                                    Restrictions.and(
                                            Restrictions.eq("from", loggedInUser),
                                            Restrictions.eq("to", otherUser)
                                    ),
                                    Restrictions.and(
                                            Restrictions.eq("from", otherUser),
                                            Restrictions.eq("to", loggedInUser)
                                    )
                            )
                    );

                    chatUsersCriteria.addOrder(Order.desc("id"));
                    List<Chat> dbChatCountList = chatUsersCriteria.list();
                    chatUsersCriteria.setMaxResults(1);

                    List<Chat> dbChatList = chatUsersCriteria.list();

                    if (!dbChatList.isEmpty()) {
                        JsonObject jsonChatItem = new JsonObject();
                        jsonChatItem.addProperty("id", otherUser.getId());
                        if (otherUser.equals(loggedInUser)) {
                            jsonChatItem.addProperty("name", loggedInUser.getF_name() + " (You)");
                            jsonChatItem.addProperty("bio", "Message your self");
                        } else {
                            jsonChatItem.addProperty("name", otherUser.getF_name() + " " + otherUser.getL_name());
                            jsonChatItem.addProperty("bio", otherUser.getBio());
                        }
                        jsonChatItem.addProperty("profile_img", "images//user//" + otherUser.getId() + "//" + otherUser.getId() + "avatar.png");

                        SimpleDateFormat timeFormat;
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

                        if (format.format(dbChatList.get(0).getDateTime()).equals(format.format(new Date()))) {
                            timeFormat = new SimpleDateFormat("hh:mm a");
                        } else {
                            timeFormat = new SimpleDateFormat("yyyy/MM/dd");
                        }

                        if (dbChatList.get(0).getMessage() != null) {
                            jsonChatItem.addProperty("msg", dbChatList.get(0).getMessage());
                        }
                        if (dbChatList.get(0).getImg()!= null) {
                            jsonChatItem.addProperty("msg", "Image");
                        }
                        jsonChatItem.addProperty("time", timeFormat.format(dbChatList.get(0).getDateTime()));
                        if (dbChatList.get(0).getFrom().equals(loggedInUser)) {
                            jsonChatItem.addProperty("view", true);
                        } else {
                            jsonChatItem.addProperty("view", false);
                        }
                        jsonChatItem.addProperty("status", dbChatList.get(0).getChatStatus().getId());

                        int count = 0;
                        for (Chat chat : dbChatCountList) {
                            if (chat.getTo().equals(loggedInUser) && chat.getFrom().equals(dbChatList.get(0).getFrom()) && chat.getChatStatus().equals(notReadedChatStatus)) {
                                count++;
                            }
                        }

                        jsonChatItem.addProperty("count", count);

                        usersArray.add(jsonChatItem);
                    }

                }

                responseObject.add("chatList", usersArray);
                responseObject.addProperty("ok", true);
            } catch (NumberFormatException | HibernateException e) {
                System.out.println(e.getMessage());
                responseObject.addProperty("msg", "Can not process this request!");
            }
            session.close();
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseObject));
    }

}
