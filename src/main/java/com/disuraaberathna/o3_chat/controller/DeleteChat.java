package com.disuraaberathna.o3_chat.controller;

import com.disuraaberathna.o3_chat.entity.Chat;
import com.disuraaberathna.o3_chat.entity.User;
import com.disuraaberathna.o3_chat.model.HibernateUtil;
import com.disuraaberathna.o3_chat.model.Validate;
import com.google.gson.Gson;
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
import java.util.List;

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
                int loggedInUserId = Integer.parseInt(loggedInId);
                int otherUserId = Integer.parseInt(otherId);

                User loggedInUser = session.find(User.class, loggedInUserId);
                User otherUser = session.find(User.class, otherUserId);

                if (loggedInUser == null || otherUser == null) {
                    responseObject.addProperty("msg", "Invalid user(s)!");
                    return;
                }

                CriteriaBuilder builder = session.getCriteriaBuilder();
                CriteriaQuery<Chat> query = builder.createQuery(Chat.class);
                Root<Chat> root = query.from(Chat.class);

                query.select(root)
                        .where(builder.or(
                                builder.and(
                                        builder.equal(root.get("from"), loggedInUser),
                                        builder.equal(root.get("to"), otherUser)
                                ),
                                builder.and(
                                        builder.equal(root.get("to"), loggedInUser),
                                        builder.equal(root.get("from"), otherUser)
                                )
                        ));

                List<Chat> chatList = session.createQuery(query).getResultList();

                String applicationPath = req.getServletContext().getRealPath("");
                String newApplicationPath = applicationPath.replace("build" + File.separator + "web", "web");

                File folder = null;
                File folder1 = new File(applicationPath + File.separator + "images" + File.separator + "chat" + File.separator + loggedInUserId + "-" + otherUserId);
                File folder2 = new File(applicationPath + File.separator + "images" + File.separator + "chat" + File.separator + otherUserId + "-" + loggedInUserId);

                if (folder1.exists()) {
                    folder = new File(newApplicationPath + "//images//chat//" + loggedInUserId + "-" + otherUserId);
                } else if (folder2.exists()) {
                    folder = new File(newApplicationPath + "//images//chat//" + otherUserId + "-" + loggedInUserId);
                }

                if (folder != null && folder.exists()) {
                    Transaction tx = session.beginTransaction();

                    for (Chat chat : chatList) {
                        if (chat.getImg() != null) {
                            File imageFile = new File(folder, chat.getImg());
                            if (imageFile.exists()) {
                                imageFile.delete();
                            }
                        }
                        session.remove(chat);
                    }

                    tx.commit();

                    folder.delete();
                }

                responseObject.addProperty("ok", true);

            } catch (NumberFormatException | HibernateException | NullPointerException e) {
                System.out.println(e.getMessage());
                responseObject.addProperty("msg", "Cannot process this request!");
            } finally {
                session.close();
            }
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseObject));

    }

}
