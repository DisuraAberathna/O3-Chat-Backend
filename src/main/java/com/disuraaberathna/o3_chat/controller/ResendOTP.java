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

@WebServlet(name = "ResendOTP", urlPatterns = {"/resend-otp"})
public class ResendOTP extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("ok", false);

        JsonObject reqObject = gson.fromJson(req.getReader(), JsonObject.class);
        String userId = reqObject.has("userId") ? reqObject.get("userId").getAsString() : "";
        String username = reqObject.has("username") ? reqObject.get("username").getAsString() : "";
        String email = reqObject.has("email") ? reqObject.get("email").getAsString() : "";

        if (userId.isEmpty() && username.isEmpty() && email.isEmpty()) {
            responseObject.addProperty("msg", "Please provide userId, username, or email!");
        } else {

            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                CriteriaBuilder builder = session.getCriteriaBuilder();
                CriteriaQuery<User> query = builder.createQuery(User.class);
                Root<User> root = query.from(User.class);

                final User user;

                if (!userId.isEmpty() && Validate.isInteger(userId)) {
                    query.select(root).where(builder.equal(root.get("id"), Integer.valueOf(userId)));
                    user = session.createQuery(query).uniqueResult();
                } else if (!username.isEmpty()) {
                    query.select(root).where(builder.equal(root.get("username"), username));
                    user = session.createQuery(query).uniqueResult();
                } else if (!email.isEmpty()) {
                    query.select(root).where(builder.equal(root.get("email"), email));
                    user = session.createQuery(query).uniqueResult();
                } else {
                    user = null;
                }

                if (user != null) {
                    int otp = (int) (Math.random() * 1000000);
                    String hashedOTP = BCrypt.withDefaults().hashToString(12, String.valueOf(otp).toCharArray());

                    Thread mailSender = new Thread(() -> {
                        String content = "<head>\n"
                                + "  <style>\n"
                                + "    body { font-family: Arial, sans-serif; background-color: #e7e7e7; margin: 0; padding: 0; }\n"
                                + "    .email-container { max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n"
                                + "    .email-header { text-align: center; padding: 20px 0; background-color: #0c4eac; color: white; border-radius: 8px 8px 0 0; }\n"
                                + "    .email-body { padding: 20px; border-left: 2px solid #0c4eac; border-right: 2px solid #0c4eac; text-align: center; }\n"
                                + "    .email-body p { font-size: 16px; color: #333333; line-height: 1.5; margin: 0 0 20px; }\n"
                                + "    .verification-code { display: inline-block; font-size: 24px; color: #0c4eac; background-color: #f4f4f4; padding: 10px 20px; border-radius: 5px; letter-spacing: 2px; margin: 20px 0; }\n"
                                + "    .email-footer { text-align: center; font-size: 14px; color: #777777; padding: 20px 0; border-top: 1px solid #eeeeee; border-left: 2px solid #0c4eac; border-right: 2px solid #0c4eac; border-bottom: 2px solid #0c4eac; border-bottom-left-radius: 10px; border-bottom-right-radius: 10px; }\n"
                                + "    .email-footer a { color: #0c4eac; text-decoration: none; }\n"
                                + "  </style>\n"
                                + "</head>\n"
                                + "<body>\n"
                                + "  <div class=\"email-container\">\n"
                                + "    <div class=\"email-header\">\n"
                                + "      <h1>Verification Code</h1>\n"
                                + "    </div>\n"
                                + "    <div class=\"email-body\">\n"
                                + "      <p>Hello " + user.getUsername() + ",</p>\n"
                                + "      <p>Thank you for using O3 Chat. Please use the following verification code:</p>\n"
                                + "      <div class=\"verification-code\">" + otp + "</div>\n"
                                + "      <p>If you did not request this, please ignore this email.</p>\n"
                                + "    </div>\n"
                                + "    <div class=\"email-footer\">\n"
                                + "      <p>Best regards,</p>\n"
                                + "      <p>The O3 Chat Team</p>\n"
                                + "      <p><a href=\"#\">Contact Support</a></p>\n"
                                + "    </div>\n"
                                + "  </div>\n"
                                + "</body>";

                        Mail.sendMail(user.getEmail(), "Resend OTP - O3 Chat", content);
                    });
                    mailSender.start();

                    Transaction tx = session.beginTransaction();
                    user.setVerification(hashedOTP);
                    session.merge(user);
                    tx.commit();

                    responseObject.addProperty("ok", true);
                } else {
                    responseObject.addProperty("msg", "User not found!");
                }

            } catch (NumberFormatException | HibernateException e) {
                System.out.println(e.getMessage());
                responseObject.addProperty("msg", "Cannot process this request!");
            }
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseObject));

    }

}
