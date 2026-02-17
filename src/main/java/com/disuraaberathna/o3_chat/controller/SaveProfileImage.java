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

@WebServlet(name = "SaveProfileImage", urlPatterns = {"/save-profile-image"})
public class SaveProfileImage extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("ok", false);

        JsonObject reqObject = gson.fromJson(req.getReader(), JsonObject.class);
        String id = reqObject.get("id").getAsString();
        String profile_url = reqObject.get("profile_url").getAsString();

        if (id.isEmpty()) {
            responseObject.addProperty("msg", "Something went wrong! Please sign in again.");
        } else if (!Validate.isInteger(id)) {
            responseObject.addProperty("msg", "Cloudn't process this request! \\nYou are a third-party person.");
        } else if (profile_url.isEmpty()) {
            responseObject.addProperty("msg", "Please select a profile picture!");
        } else {

            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                User user = session.find(User.class, Integer.valueOf(id));

                if (user != null) {
                    user.setProfile_url(profile_url);
                    session.beginTransaction();
                    session.merge(user);
                    session.getTransaction().commit();

                    responseObject.addProperty("ok", true);
                } else {
                    responseObject.addProperty("msg", "User not found! Please sign in again.");
                }
            } catch (NumberFormatException e) {
                System.out.println(e.getMessage());
                responseObject.addProperty("msg", "Can not process this request!");
            }
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseObject));

    }

}
