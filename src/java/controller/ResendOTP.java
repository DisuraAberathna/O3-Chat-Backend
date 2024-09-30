/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import entity.User;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.HibernateUtil;
import model.Mail;
import model.Validate;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author SINGER
 */
@WebServlet(name = "ResendOTP", urlPatterns = {"/ResendOTP"})
public class ResendOTP extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("ok", false);

        JsonObject reqObject = gson.fromJson(req.getReader(), JsonObject.class);
        String id = reqObject.get("userId").getAsString();

        if (id.isEmpty()) {
            responseObject.addProperty("msg", "Something went wrong! Please sign in again.");
        } else if (!Validate.isInteger(id)) {
            responseObject.addProperty("msg", "Cloudn't process this request! \\nYou are a third-party person.");
        } else {
            Session session = HibernateUtil.getSessionFactory().openSession();

            try {
                Criteria userCriteria = session.createCriteria(User.class);
                userCriteria.add(Restrictions.eq("id", Integer.valueOf(id)));
                User user = (User) userCriteria.uniqueResult();

                int otp = (int) (Math.random() * 1000000);

                Thread mailSender = new Thread() {
                    @Override
                    public void run() {
                        String content = "<head>\n"
                                + "  <style>\n"
                                + "    body {\n"
                                + "      font-family: Arial, sans-serif;\n"
                                + "      background-color: #e7e7e7;\n"
                                + "      margin: 0;\n"
                                + "      padding: 0;\n"
                                + "      -webkit-font-smoothing: antialiased;\n"
                                + "      -moz-osx-font-smoothing: grayscale;\n"
                                + "    }\n"
                                + "    .email-container {\n"
                                + "      max-width: 600px;\n"
                                + "      margin: 0 auto;\n"
                                + "      background-color: #ffffff;\n"
                                + "      padding: 20px;\n"
                                + "      border-radius: 8px;\n"
                                + "      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);\n"
                                + "    }\n"
                                + "    .email-header {\n"
                                + "      text-align: center;\n"
                                + "      padding: 20px 0;\n"
                                + "      background-color: #0c4eac;\n"
                                + "      color: white;\n"
                                + "      border-radius: 8px 8px 0 0;\n"
                                + "    }\n"
                                + "    .email-body {\n"
                                + "      padding: 20px;\n"
                                + "      border-left: 2px solid #0c4eac;\n"
                                + "      border-right: 2px solid #0c4eac;\n"
                                + "      text-align: center;\n"
                                + "    }\n"
                                + "    .email-body p {\n"
                                + "      font-size: 16px;\n"
                                + "      color: #333333;\n"
                                + "      line-height: 1.5;\n"
                                + "      margin: 0 0 20px;\n"
                                + "    }\n"
                                + "    .verification-code {\n"
                                + "      display: inline-block;\n"
                                + "      font-size: 24px;\n"
                                + "      color: #0c4eac;\n"
                                + "      background-color: #f4f4f4;\n"
                                + "      padding: 10px 20px;\n"
                                + "      border-radius: 5px;\n"
                                + "      letter-spacing: 2px;\n"
                                + "      margin: 20px 0;\n"
                                + "    }\n"
                                + "    .email-footer {\n"
                                + "      text-align: center;\n"
                                + "      font-size: 14px;\n"
                                + "      color: #777777;\n"
                                + "      padding: 20px 0;\n"
                                + "      border-top: 1px solid #eeeeee;\n"
                                + "      border-left: 2px solid #0c4eac;\n"
                                + "      border-right: 2px solid #0c4eac;\n"
                                + "      border-bottom: 2px solid #0c4eac;\n"
                                + "      border-bottom-left-radius: 10px;\n"
                                + "      border-bottom-right-radius: 10px;\n"
                                + "    }\n"
                                + "    .email-footer a {\n"
                                + "      color: #0c4eac;\n"
                                + "      text-decoration: none;\n"
                                + "    }\n"
                                + "  </style>\n"
                                + "</head>\n"
                                + "<body>\n"
                                + "  <div class=\"email-container\">\n"
                                + "    <div class=\"email-header\">\n"
                                + "      <h1>Verification Code</h1>\n"
                                + "    </div>\n"
                                + "    <div class=\"email-body\">\n"
                                + "      <p>Hello User,</p>\n"
                                + "      <p>\n"
                                + "        Thank you for registering with us. Please use the following verification\n"
                                + "        code to complete your registration process:\n"
                                + "      </p>\n"
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
                    }
                };
                mailSender.start();

                user.setVerification(String.valueOf(otp));

                session.update(user);
                session.beginTransaction().commit();

                responseObject.addProperty("ok", true);
            } catch (NumberFormatException e) {
                System.out.println(e.getMessage());
                responseObject.addProperty("msg", "Can not process this request!");
            }
            session.close();
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseObject));

    }

}
