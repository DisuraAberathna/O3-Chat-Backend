package com.disuraaberathna.o3_chat.controller;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.disuraaberathna.o3_chat.entity.User;
import com.disuraaberathna.o3_chat.model.HibernateUtil;
import com.disuraaberathna.o3_chat.model.Mail;
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
import java.util.Date;

@WebServlet(name = "Register", urlPatterns = {"/register"})
public class Register extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("ok", false);

        Gson gson = new Gson();
        JsonObject reqObject = gson.fromJson(req.getReader(), JsonObject.class);
        System.out.println("DEBUG Register payload: " + reqObject.toString());

        String f_name = reqObject.has("f_name") ? reqObject.get("f_name").getAsString() : "";
        String l_name = reqObject.has("l_name") ? reqObject.get("l_name").getAsString() : "";
        String password = reqObject.has("password") ? reqObject.get("password").getAsString() : "";
        String mobile = reqObject.has("mobile") ? reqObject.get("mobile").getAsString() : "";
        String username = reqObject.has("username") ? reqObject.get("username").getAsString() : "";
        String email = reqObject.has("email") ? reqObject.get("email").getAsString() : "";
        String profile_url = reqObject.has("profile_img") ? reqObject.get("profile_img").getAsString() : "";

        if (f_name.isEmpty()) responseObject.addProperty("msg", "Please enter your first name!");
        else if (Validate.hasDigit(f_name)) responseObject.addProperty("msg", "First name cannot contain numbers!");
        else if (f_name.length() > 50) responseObject.addProperty("msg", "First name exceeds character limit!");
        else if (l_name.isEmpty()) responseObject.addProperty("msg", "Please enter your last name!");
        else if (Validate.hasDigit(l_name)) responseObject.addProperty("msg", "Last name cannot contain numbers!");
        else if (l_name.length() > 50) responseObject.addProperty("msg", "Last name exceeds character limit!");
        else if (password.isEmpty()) responseObject.addProperty("msg", "Please enter a password!");
        else if (password.length() < 5 || password.length() > 20)
            responseObject.addProperty("msg", "Password must be between 5 and 20 characters!");
        else if (Validate.isValidPassword(password))
            responseObject.addProperty("msg", "Password must contain uppercase, lowercase, number, and special character!");
        else if (profile_url == null || profile_url.isEmpty())
            responseObject.addProperty("msg", "Please select a profile picture!");
        else if (mobile.isEmpty()) responseObject.addProperty("msg", "Please enter your mobile number!");
        else if (!Validate.isValidMobile(mobile))
            responseObject.addProperty("msg", "Invalid mobile number! Format: 07********");
        else if (username.isEmpty()) responseObject.addProperty("msg", "Please enter your username!");
        else if (username.length() < 3 || username.length() > 20)
            responseObject.addProperty("msg", "Username must be between 3 and 20 characters!");
        else if (email.isEmpty()) responseObject.addProperty("msg", "Please enter your email!");
        else if (!Validate.isValidEmail(email)) responseObject.addProperty("msg", "Invalid email format!");
        else if (email.length() > 100) responseObject.addProperty("msg", "Email exceeds character limit!");
        else {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                Transaction tx = session.beginTransaction();
                CriteriaBuilder cb = session.getCriteriaBuilder();

                CriteriaQuery<User> mobileQuery = cb.createQuery(User.class);
                Root<User> mobileRoot = mobileQuery.from(User.class);
                mobileQuery.select(mobileRoot).where(cb.equal(mobileRoot.get("mobile"), mobile));
                
                CriteriaQuery<User> usernameQuery = cb.createQuery(User.class);
                Root<User> usernameRoot = usernameQuery.from(User.class);
                usernameQuery.select(usernameRoot).where(cb.equal(usernameRoot.get("username"), username));
                
                CriteriaQuery<User> emailQuery = cb.createQuery(User.class);
                Root<User> emailRoot = emailQuery.from(User.class);
                emailQuery.select(emailRoot).where(cb.equal(emailRoot.get("email"), email));

                if (!session.createQuery(mobileQuery).getResultList().isEmpty()) {
                    responseObject.addProperty("msg", "Mobile number already exists!");
                } else if (!session.createQuery(usernameQuery).getResultList().isEmpty()) {
                    responseObject.addProperty("msg", "Username already exists!");
                } else if (!session.createQuery(emailQuery).getResultList().isEmpty()) {
                    responseObject.addProperty("msg", "Email already exists!");
                } else {
                    int otp = (int) (Math.random() * 1000000);
                    String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
                    String hashedOTP = BCrypt.withDefaults().hashToString(12, String.valueOf(otp).toCharArray());

                    User user = new User();
                    user.setF_name(f_name);
                    user.setL_name(l_name);
                    user.setUsername(username);
                    user.setMobile(mobile);
                    user.setEmail(email);
                    user.setPassword(hashedPassword);
                    user.setRegistered_date(new Date());
                    user.setVerification(hashedOTP);
                    user.setStatus(1);
                    user.setBio("Hey there! I am using O3 Chat.");
                    user.setProfile_url(profile_url);

                    session.persist(user);
                    tx.commit();

                    Thread mailSender = new Thread(() -> {
                        String content = "<html><body><h1>Verification Code</h1>"
                                + "<p>Hello User,</p>"
                                + "<p>Your OTP is: <strong>" + otp + "</strong></p>"
                                + "<p>If you did not request this, ignore this email.</p>"
                                + "</body></html>";
                        Mail.sendMail(email, "Verify Your Account - O3 Chat", content);
                    });
                    mailSender.start();

                    responseObject.addProperty("ok", true);
                    responseObject.addProperty("user", user.getId());
                }

            } catch (HibernateException e) {
                e.printStackTrace();
                responseObject.addProperty("msg", "Cannot process this request!");
            }
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseObject));
    }
}
