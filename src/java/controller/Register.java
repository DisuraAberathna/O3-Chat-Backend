/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import entity.User;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import model.HibernateUtil;
import model.Mail;
import model.Validate;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author SINGER
 */
@MultipartConfig
@WebServlet(name = "Register", urlPatterns = {"/Register"})
public class Register extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("ok", false);

        String f_name = req.getParameter("f_name");
        String l_name = req.getParameter("l_name");
        String password = req.getParameter("password");
        String mobile = req.getParameter("mobile");
        String username = req.getParameter("username");
        String email = req.getParameter("email");
        Part image = req.getPart("image");

        if (f_name.isEmpty()) {
            responseObject.addProperty("msg", "Please enter your first name!");
        } else if (Validate.hasDigit(f_name)) {
            responseObject.addProperty("msg", "Your first name can not has numbers!");
        } else if (f_name.length() > 50) {
            responseObject.addProperty("msg", "Your first name has exceeded the maximum character limit!");
        } else if (l_name.isEmpty()) {
            responseObject.addProperty("msg", "Please enter your last name!");
        } else if (Validate.hasDigit(l_name)) {
            responseObject.addProperty("msg", "Your last name can not has numbers!");
        } else if (l_name.length() > 50) {
            responseObject.addProperty("msg", "Your last name has exceeded the maximum character limit!");
        } else if (password.isEmpty()) {
            responseObject.addProperty("msg", "Please enter your password!");
        } else if (password.length() < 5 || password.length() > 20) {
            responseObject.addProperty("msg", "Your password must be between 5 and 20 characters!");
        } else if (!Validate.isValidPassword(password)) {
            responseObject.addProperty("msg", "Your password must contain at least one uppercase letter, one lowercase letter, one number and one special character!");
        } else if (image.getSubmittedFileName() == null) {
            responseObject.addProperty("msg", "Please select a profile picture!");
        } else if (mobile.isEmpty()) {
            responseObject.addProperty("msg", "Please enter your mobile number!");
        } else if (!Validate.isValidMobile(mobile)) {
            responseObject.addProperty("msg", "Please enter valid mobile number! \\n07********");
        } else if (username.isEmpty()) {
            responseObject.addProperty("msg", "Please enter your username!");
        } else if (username.length() < 3 || username.length() > 20) {
            responseObject.addProperty("msg", "Your username must be between 3 and 20 characters!");
        } else if (email.isEmpty()) {
            responseObject.addProperty("msg", "Please enter your email!");
        } else if (!Validate.isValidEmail(email)) {
            responseObject.addProperty("msg", "Please enter valid email!");
        } else if (email.length() > 100) {
            responseObject.addProperty("msg", "Your email has exceeded the maximum character limit!");
        } else {
            Session session = HibernateUtil.getSessionFactory().openSession();

            try {
                Criteria checkMobileCriteria = session.createCriteria(User.class);
                checkMobileCriteria.add(Restrictions.eq("mobile", mobile));

                if (checkMobileCriteria.list().isEmpty()) {
                    Criteria checkUserNameCriteria = session.createCriteria(User.class);
                    checkUserNameCriteria.add(Restrictions.eq("username", username));

                    if (checkUserNameCriteria.list().isEmpty()) {
                        Criteria checkEmailCriteria = session.createCriteria(User.class);
                        checkEmailCriteria.add(Restrictions.eq("email", email));

                        if (checkEmailCriteria.list().isEmpty()) {
                            int otp = (int) (Math.random() * 1000000);

                            User user = new User();
                            user.setF_name(f_name);
                            user.setL_name(l_name);
                            user.setUsername(username);
                            user.setMobile(mobile);
                            user.setEmail(email);
                            user.setPassword(password);
                            user.setRegistered_date(new Date());
                            user.setVerification(String.valueOf(otp));
                            user.setStatus(1);

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
                                    Mail.sendMail(email, "Verify Your Account - O3 Chat", content);
                                }
                            };
                            mailSender.start();

                            int userId = (int) session.save(user);
                            session.beginTransaction().commit();

                            String applicationPath = req.getServletContext().getRealPath("");
                            String newApplicationPath = applicationPath.replace("build" + File.separator + "web", "web");
                            File folder = new File(newApplicationPath + "//images//user//" + userId);
                            folder.mkdir();

                            File imageFile = new File(folder, userId + "avatar.png");
                            InputStream inputStreamImage = image.getInputStream();
                            Files.copy(inputStreamImage, imageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                            responseObject.addProperty("ok", true);
                            responseObject.addProperty("user", userId);
                        } else {
                            responseObject.addProperty("msg", "Email already exists!");
                        }
                    } else {
                        responseObject.addProperty("msg", "Username already exists!");
                    }
                } else {
                    responseObject.addProperty("msg", "Mobile number already exists!");
                }
            } catch (HibernateException e) {
                System.out.println(e.getMessage());
                responseObject.addProperty("msg", "Can not process this request!");
            }
            session.close();
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseObject));

    }

}
