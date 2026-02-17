package com.disuraaberathna.o3_chat.controller;

import at.favre.lib.crypto.bcrypt.BCrypt;
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
import org.hibernate.Session;

import java.io.IOException;

@WebServlet(name = "Verify", urlPatterns = {"/verify"})
public class Verify extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("ok", false);

        JsonObject reqObject = gson.fromJson(req.getReader(), JsonObject.class);
        String id = reqObject.has("id") ? reqObject.get("id").getAsString() : "";
        String username = reqObject.has("username") ? reqObject.get("username").getAsString() : "";
        String email = reqObject.has("email") ? reqObject.get("email").getAsString() : "";
        String otp = reqObject.has("otp") ? reqObject.get("otp").getAsString() : "";
        String serverOTP = reqObject.has("serverOTP") ? reqObject.get("serverOTP").getAsString() : "";
        String password = reqObject.has("password") ? reqObject.get("password").getAsString() : "";

        if (id.isEmpty() && username.isEmpty() && email.isEmpty()) {
            responseObject.addProperty("msg", "Please provide id, username, or email!");
        } else if (otp.isEmpty()) {
            responseObject.addProperty("msg", "Please enter your otp!");
        } else if (!Validate.isInteger(otp)) {
            responseObject.addProperty("msg", "Invalid otp! \\nPlease enter valid one.");
        } else {

            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                final User user;

                if (!id.isEmpty() && Validate.isInteger(id)) {
                    user = session.find(User.class, Integer.valueOf(id));
                } else if (!username.isEmpty()) {
                    CriteriaBuilder builder = session.getCriteriaBuilder();
                    CriteriaQuery<User> query = builder.createQuery(User.class);
                    Root<User> root = query.from(User.class);
                    query.select(root).where(builder.equal(root.get("username"), username));
                    user = session.createQuery(query).uniqueResult();
                } else if (!email.isEmpty()) {
                    CriteriaBuilder builder = session.getCriteriaBuilder();
                    CriteriaQuery<User> query = builder.createQuery(User.class);
                    Root<User> root = query.from(User.class);
                    query.select(root).where(builder.equal(root.get("email"), email));
                    user = session.createQuery(query).uniqueResult();
                } else {
                    user = null;
                }

                if (user == null) {
                    responseObject.addProperty("msg", "User not found!");
                } else if (user.getVerification().equals("Verified")) {
                    if (!serverOTP.isEmpty() && !password.isEmpty()) {
                        BCrypt.Result otpResult = BCrypt.verifyer().verify(otp.toCharArray(), serverOTP);
                        
                        if (otpResult.verified) {
                            String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
                            user.setPassword(hashedPassword);

                            session.merge(user);
                            session.beginTransaction().commit();

                            responseObject.addProperty("ok", true);
                        } else {
                            responseObject.addProperty("msg", "OTP mismatched!");
                        }
                    } else {
                        responseObject.addProperty("msg", "Account already verified!");
                    }
                } else {
                    BCrypt.Result otpResult = BCrypt.verifyer().verify(otp.toCharArray(), user.getVerification());
                    
                    if (otpResult.verified) {
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
                        userObject.addProperty("profile_img", user.getProfile_url());
                        userObject.addProperty("bio", user.getBio());

                        responseObject.addProperty("ok", true);
                        responseObject.add("user", userObject);
                    } else {
                        responseObject.addProperty("msg", "OTP mismatched!");
                    }
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
