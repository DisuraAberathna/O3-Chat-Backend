package com.disuraaberathna.o3_chat.controller;

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

import java.io.IOException;
import java.util.List;

@WebServlet(name = "UpdateUsername", urlPatterns = {"/UpdateUsername"})
public class UpdateUsername extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("ok", false);

        JsonObject reqObject = gson.fromJson(req.getReader(), JsonObject.class);
        String id = reqObject.get("id").getAsString();
        String username = reqObject.get("username").getAsString();

        if (id.isEmpty()) {
            responseObject.addProperty("msg", "Something went wrong! Please sign in again.");
        } else if (!Validate.isInteger(id)) {
            responseObject.addProperty("msg", "Couldn't process this request! \\nYou are a third-party person.");
        } else if (username.isEmpty()) {
            responseObject.addProperty("msg", "Please enter your username!");
        } else {

            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                int userId = Integer.parseInt(id);
                User user = session.find(User.class, userId);

                if (user != null) {
                    CriteriaBuilder builder = session.getCriteriaBuilder();
                    CriteriaQuery<User> query = builder.createQuery(User.class);
                    Root<User> root = query.from(User.class);

                    query.select(root)
                            .where(builder.notEqual(root.get("id"), user.getId()));

                    List<User> userList = session.createQuery(query).getResultList();

                    boolean canChange = true;
                    for (User otherUser : userList) {
                        if (otherUser.getUsername().equalsIgnoreCase(username)) {
                            canChange = false;
                            break;
                        }
                    }

                    if (canChange) {
                        Transaction tx = session.beginTransaction();

                        user.setUsername(username);
                        session.merge(user);

                        tx.commit();

                        JsonObject userObject = new JsonObject();
                        userObject.addProperty("id", user.getId());
                        userObject.addProperty("f_name", user.getF_name());
                        userObject.addProperty("l_name", user.getL_name());
                        userObject.addProperty("username", user.getUsername());
                        userObject.addProperty("mobile", user.getMobile());
                        userObject.addProperty("email", user.getEmail());
                        userObject.addProperty("profile_img", "images//user//" + user.getId() + "//" + user.getId() + "avatar.png");
                        userObject.addProperty("bio", user.getBio());

                        responseObject.add("user", userObject);
                        responseObject.addProperty("ok", true);
                    } else {
                        responseObject.addProperty("msg", "Username already exists!");
                    }
                } else {
                    responseObject.addProperty("msg", "Invalid user!");
                }

            } catch (NumberFormatException e) {
                System.out.println(e.getMessage());
                responseObject.addProperty("msg", "Cannot process this request!");
            } catch (HibernateException e) {
                System.out.println("Hibernate error: " + e.getMessage());
                responseObject.addProperty("msg", "Database error occurred!");
            }

        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseObject));
    }

}
