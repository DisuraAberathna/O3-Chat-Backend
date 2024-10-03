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
import model.Validate;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author SINGER
 */
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

        if (id.isEmpty()) {
            responseObject.addProperty("msg", "Something went wrong! Please sign in again.");
        } else if (!Validate.isInteger(id)) {
            responseObject.addProperty("msg", "Cloudn't process this request! \\nYou are a third-party person.");
        } else if (otp.isEmpty()) {
            responseObject.addProperty("msg", "Please enter your otp!");
        } else if (!Validate.isInteger(otp)) {
            responseObject.addProperty("msg", "Invlid otp! \\nPlease enter valid one.");
        } else {
            Session session = HibernateUtil.getSessionFactory().openSession();

            try {
                Criteria userCriteria = session.createCriteria(User.class);
                userCriteria.add(Restrictions.eq("id", Integer.valueOf(id)));
                User user = (User) userCriteria.uniqueResult();

                if (user.getVerification().equals(otp)) {
                    user.setVerification("Verified");

                    session.update(user);
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
                } else {
                    responseObject.addProperty("msg", "OTP mismatched!");
                }
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
