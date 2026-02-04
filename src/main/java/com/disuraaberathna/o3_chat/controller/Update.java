package com.disuraaberathna.o3_chat.controller;

import com.disuraaberathna.o3_chat.entity.User;
import com.disuraaberathna.o3_chat.model.HibernateUtil;
import com.disuraaberathna.o3_chat.model.Validate;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;

import java.io.IOException;

@WebServlet(name = "Update", urlPatterns = {"/update"})
public class Update extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("ok", false);

        JsonObject reqObject = gson.fromJson(req.getReader(), JsonObject.class);
        String id = reqObject.get("user").getAsString();
        String username = reqObject.get("username").getAsString();
        String f_name = reqObject.get("f_name").getAsString();
        String l_name = reqObject.get("l_name").getAsString();
        String bio = reqObject.get("bio").getAsString();

        if (id.isEmpty()) {
            responseObject.addProperty("msg", "Something went wrong! Please sign in again.");
        } else if (!Validate.isInteger(id)) {
            responseObject.addProperty("msg", "Couldn't process this request! \\nYou are a third-party person.");
        } else {

            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                User user = session.find(User.class, Integer.valueOf(id));

                if (user != null) {
                    if (username != null) {
                        if (!username.isEmpty() && !user.getUsername().equals(username)) {
                            user.setUsername(username);
                        }
                    }

                    if (f_name != null) {
                        if (!f_name.isEmpty() && !user.getF_name().equals(f_name)) {
                            user.setF_name(f_name);
                        }
                    }

                    if (l_name != null) {
                        if (!l_name.isEmpty() && !user.getL_name().equals(l_name)) {
                            user.setL_name(l_name);
                        }
                    }

                    if (bio != null) {
                        if (!bio.isEmpty() && !user.getBio().equals(bio)) {
                            user.setBio(bio);
                        }
                    }

                    session.merge(user);
                    session.beginTransaction().commit();

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
                    responseObject.addProperty("msg", "Invalid user!");
                }
            } catch (NumberFormatException e) {
                System.out.println(e.getMessage());
                responseObject.addProperty("msg", "Can not process this request!");
            }

            resp.setContentType("application/json");
            resp.getWriter().write(gson.toJson(responseObject));
        }
    }

}
