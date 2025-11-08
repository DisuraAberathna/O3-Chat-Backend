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

@WebServlet(name = "Verify", urlPatterns = {"/Verify"})
public class Verify extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("ok", false);

        JsonObject reqObject = gson.fromJson(req.getReader(), JsonObject.class);
        String id = reqObject.get("id").getAsString();
        String otp = reqObject.get("otp").getAsString();
        String serverOTP = reqObject.get("serverOTP").getAsString();
        String password = reqObject.get("password").getAsString();

        if (id.isEmpty()) {
            responseObject.addProperty("msg", "Something went wrong! Please sign in again.");
        } else if (!Validate.isInteger(id)) {
            responseObject.addProperty("msg", "Cloudn't process this request! \\nYou are a third-party person.");
        } else if (otp.isEmpty()) {
            responseObject.addProperty("msg", "Please enter your otp!");
        } else if (!Validate.isInteger(otp)) {
            responseObject.addProperty("msg", "Invlid otp! \\nPlease enter valid one.");
        } else {

            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                User user = session.find(User.class, Integer.valueOf(id));

                if (user.getVerification().equals(otp)) {
                    user.setVerification("Verified");

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

                    responseObject.addProperty("ok", true);
                    responseObject.add("user", userObject);
                } else if (user.getVerification().equals("Verified") && !serverOTP.isEmpty() && !password.isEmpty()) {
                    user.setPassword(password);

                    session.merge(user);
                    session.beginTransaction().commit();

                    responseObject.addProperty("ok", true);
                } else {
                    responseObject.addProperty("msg", "OTP mismatched!");
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
