/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import entity.Chat;
import entity.ChatStatus;
import entity.User;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author SINGER
 */
public class LoadHomeData {

    String LoggedInUser;

    public LoadHomeData(String LoggedInUser) {
        this.LoggedInUser = LoggedInUser;
    }

    public JsonArray loadChatList() throws HibernateException, NumberFormatException {
        Session session = HibernateUtil.getSessionFactory().openSession();

        User loggedInUser = (User) session.get(User.class, Integer.valueOf(LoggedInUser));

        Criteria notReadedStatusCriteria = session.createCriteria(ChatStatus.class);
        notReadedStatusCriteria.add(Restrictions.eq("name", "Delivered"));
        ChatStatus notReadedChatStatus = (ChatStatus) notReadedStatusCriteria.uniqueResult();

        Criteria chatUsersCriteria = session.createCriteria(Chat.class);
        chatUsersCriteria.add(
                Restrictions.or(
                        Restrictions.eq("from", loggedInUser),
                        Restrictions.eq("to", loggedInUser)
                )
        );
        chatUsersCriteria.addOrder(Order.desc("id"));

        List<Chat> chatList = chatUsersCriteria.list();
        Set<Integer> addedUserIds = new HashSet<>();
        JsonArray usersArray = new JsonArray();

        for (Chat chat : chatList) {
            if (chat == null) {
                continue;
            }

            User otherUser = (chat.getFrom().equals(loggedInUser)) ? chat.getTo() : chat.getFrom();

            if (otherUser == null || addedUserIds.contains(otherUser.getId())) {
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

            SimpleDateFormat timeFormat;
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

            if (format.format(chat.getDateTime()).equals(format.format(new Date()))) {
                timeFormat = new SimpleDateFormat("hh:mm a");
            } else {
                timeFormat = new SimpleDateFormat("yyyy/MM/dd");
            }

            if (chat.getMessage() != null) {
                jsonChatItem.addProperty("msg", chat.getMessage());
            } else if (chat.getImg() != null) {
                jsonChatItem.addProperty("msg", "Image");
            }

            jsonChatItem.addProperty("time", timeFormat.format(chat.getDateTime()));

            if (chat.getFrom().equals(loggedInUser)) {
                jsonChatItem.addProperty("view", true);
            } else {
                jsonChatItem.addProperty("view", false);
            }

            if (chat.getChatStatus() == null) {
                continue;
            }

            jsonChatItem.addProperty("status", chat.getChatStatus().getId());

            int count = 0;
            for (Chat unreadChat : chatList) {
                if (unreadChat.getTo().equals(loggedInUser)
                        && unreadChat.getFrom().equals(chat.getFrom())
                        && unreadChat.getChatStatus().equals(notReadedChatStatus)) {
                    count++;
                }
            }
            jsonChatItem.addProperty("count", count);

            usersArray.add(jsonChatItem);
            addedUserIds.add(otherUser.getId());
        }

        session.close();
        return usersArray;
    }

}
