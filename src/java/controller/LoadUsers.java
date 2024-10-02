/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import entity.User;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.HibernateUtil;
import model.Validate;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;

/**
 *
 * @author SINGER
 */
@WebServlet(name = "LoadUsers", urlPatterns = {"/LoadUsers"})
public class LoadUsers extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("ok", false);

        JsonObject reqObject = gson.fromJson(req.getReader(), JsonObject.class);
        String id = reqObject.get("id").getAsString();

        if (id.isEmpty()) {
            responseObject.addProperty("msg", "Something went wrong! Please sign in again.");
        } else if (!Validate.isInteger(id)) {
            responseObject.addProperty("msg", "Cloudn't process this request! \\nYou are a third-party person.");
        } else {
            Session session = HibernateUtil.getSessionFactory().openSession();

            try {
                Criteria userCriteria = session.createCriteria(User.class);
                userCriteria.addOrder(Order.asc("f_name"));
                List<User> userList = userCriteria.list();

                JsonArray userArray = new JsonArray();
                for (User user : userList) {
                    JsonObject userObject = new JsonObject();
                    userObject.addProperty("id", user.getId());
                    if (user.getId() == Integer.parseInt(id)) {
                        userObject.addProperty("name", user.getF_name() + " (You)");
                        userObject.addProperty("bio", "Message your self");
                    } else {
                        userObject.addProperty("name", user.getF_name() + " " + user.getL_name());
                        userObject.addProperty("bio", user.getBio());
                    }
                    userObject.addProperty("profile_img", "images//user//" + user.getId() + "//" + user.getId() + "avatar.png");

                    userArray.add(userObject);
                }
                responseObject.add("users", userArray);
                responseObject.addProperty("ok", true);
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
