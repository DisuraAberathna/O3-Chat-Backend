package com.disuraaberathna.o3_chat.controller;

import at.favre.lib.crypto.bcrypt.BCrypt;
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
import org.hibernate.Transaction;

import java.io.IOException;

@WebServlet(name = "UpdatePassword", urlPatterns = {"/update-password"})
public class UpdatePassword extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("ok", false);

        JsonObject reqObject = gson.fromJson(req.getReader(), JsonObject.class);
        String userId = reqObject.has("userId") ? reqObject.get("userId").getAsString() : "";
        String currentPassword = reqObject.has("currentPassword") ? reqObject.get("currentPassword").getAsString() : "";
        String newPassword = reqObject.has("newPassword") ? reqObject.get("newPassword").getAsString() : "";
        String confirmPassword = reqObject.has("confirmPassword") ? reqObject.get("confirmPassword").getAsString() : "";

        if (userId.isEmpty()) {
            responseObject.addProperty("msg", "User ID is required!");
        } else if (currentPassword.isEmpty()) {
            responseObject.addProperty("msg", "Please enter your current password!");
        } else if (newPassword.isEmpty()) {
            responseObject.addProperty("msg", "Please enter your new password!");
        } else if (newPassword.length() < 5 || newPassword.length() > 20) {
            responseObject.addProperty("msg", "New password must be between 5 and 20 characters!");
        } else if (Validate.isValidPassword(newPassword)) { 
            responseObject.addProperty("msg", "New password must contain at least one uppercase letter, one lowercase letter, one number and one special character!");
        } else if (!newPassword.equals(confirmPassword)) {
            responseObject.addProperty("msg", "New passwords do not match!");
        } else {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                User user = session.find(User.class, Integer.valueOf(userId));

                if (user != null) {
                    BCrypt.Result result = BCrypt.verifyer().verify(currentPassword.toCharArray(), user.getPassword());
                    
                    if (result.verified) {
                        if (currentPassword.equals(newPassword)) {
                            responseObject.addProperty("msg", "New password cannot be the same as your current password!");
                        } else {
                            String hashedPassword = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());

                            Transaction tx = session.beginTransaction();
                            user.setPassword(hashedPassword);
                            session.merge(user);
                            tx.commit();

                            responseObject.addProperty("ok", true);
                            responseObject.addProperty("msg", "Password updated successfully!");
                        }
                    } else {
                        responseObject.addProperty("msg", "Current password is incorrect!");
                    }
                } else {
                    responseObject.addProperty("msg", "User not found!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                responseObject.addProperty("msg", "Cannot process this request!");
            }
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseObject));
    }

}
