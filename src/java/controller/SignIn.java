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
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author SINGER
 */
@WebServlet(name = "SignIn", urlPatterns = {"/SignIn"})
public class SignIn extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("ok", false);

        JsonObject reqObject = gson.fromJson(req.getReader(), JsonObject.class);
        String username = reqObject.get("username").getAsString();
        String password = reqObject.get("password").getAsString();

        if (username.isEmpty()) {
            responseObject.addProperty("msg", "Please enter your username!");
        } else if (password.isEmpty()) {
            responseObject.addProperty("msg", "Please enter your password!");
        } else {
            Session session = HibernateUtil.getSessionFactory().openSession();

            try {
                Criteria userCriteria = session.createCriteria(User.class);
                userCriteria.add(Restrictions.eq("username", username));
                userCriteria.add(Restrictions.eq("password", password));

                if (!userCriteria.list().isEmpty()) {
                    User user = (User) userCriteria.uniqueResult();

                    if (user.getStatus() == 1) {
                        if (!user.getVerification().equals("Verified")) {
                            responseObject.addProperty("msg", "Not Verified");

                            responseObject.addProperty("user", user.getId());
                        } else {
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
                        }
                        responseObject.addProperty("ok", true);
                    } else {
                        responseObject.addProperty("msg", "Your account was suspended!");
                    }
                } else {
                    responseObject.addProperty("msg", "Invalid credentials!");
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
