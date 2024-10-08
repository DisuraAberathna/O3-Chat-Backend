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
import org.hibernate.Session;

/**
 *
 * @author SINGER
 */
@WebServlet(name = "UpdateBio", urlPatterns = {"/UpdateBio"})
public class UpdateBio extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("ok", false);

        JsonObject reqObject = gson.fromJson(req.getReader(), JsonObject.class);
        String id = reqObject.get("id").getAsString();
        String bio = reqObject.get("bio").getAsString();

        if (id.isEmpty()) {
            responseObject.addProperty("msg", "Something went wrong! Please sign in again.");
        } else if (!Validate.isInteger(id)) {
            responseObject.addProperty("msg", "Cloudn't process this request! \\nYou are a third-party person.");
        } else if (bio.isEmpty()) {
            responseObject.addProperty("msg", "Please enter your bio.");
        } else {
            Session session = HibernateUtil.getSessionFactory().openSession();

            try {
                User user = (User) session.get(User.class, Integer.valueOf(id));

                if (user != null) {
                    user.setBio(bio);

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

                    responseObject.add("user", userObject);
                    responseObject.addProperty("ok", true);
                } else {
                    responseObject.addProperty("msg", "Invalid user!");
                }
            } catch (NumberFormatException e) {
                System.out.println(e.getMessage());
                responseObject.addProperty("msg", "Can not process this request!");
            }
            session.close();

            resp.setContentType("application/json");
            resp.getWriter().write(gson.toJson(responseObject));
        }
    }

}
