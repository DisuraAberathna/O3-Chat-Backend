/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import entity.Chat;
import entity.User;
import java.io.File;
import java.io.IOException;
import java.util.List;
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
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author SINGER
 */
@WebServlet(name = "DeleteChat", urlPatterns = {"/DeleteChat"})
public class DeleteChat extends HttpServlet {

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
                List<Chat> chatList = chatCriteria.list();

                String applicationPath = req.getServletContext().getRealPath("");
                String newApplicationPath = applicationPath.replace("build" + File.separator + "web", "web");

                String condition1 = applicationPath + File.separator + "images" + File.separator + "chat" + File.separator + loggedInUser.getId() + "-" + otherUser.getId() + File.separator;
                File condition1File = new File(condition1);

                String condition2 = applicationPath + File.separator + "images" + File.separator + "chat" + File.separator + otherUser.getId() + "-" + loggedInUser.getId() + File.separator;
                File condition2File = new File(condition2);

                File folder = null;
                if (condition1File.exists()) {
                    folder = new File(newApplicationPath + "//images//chat//" + loggedInUser.getId() + "-" + otherUser.getId());
                } else if (condition2File.exists()) {
                    folder = new File(newApplicationPath + "//images//chat//" + otherUser.getId() + "-" + loggedInUser.getId());
                }

                for (Chat chat : chatList) {
                    if (chat.getImg() != null) {
                        File image = new File(folder, chat.getImg());
                        image.delete();
                    }

                    session.delete(chat);
                    session.beginTransaction().commit();
                }

                folder.delete();
                responseObject.addProperty("ok", true);

            } catch (NumberFormatException | HibernateException | NullPointerException e) {
                System.out.println(e.getMessage());
                responseObject.addProperty("msg", "Can not process this request!");
            }

            session.close();
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseObject));

    }

}
