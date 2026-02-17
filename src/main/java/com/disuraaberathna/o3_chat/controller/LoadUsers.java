package com.disuraaberathna.o3_chat.controller;

import com.disuraaberathna.o3_chat.entity.User;
import com.disuraaberathna.o3_chat.model.HibernateUtil;
import com.disuraaberathna.o3_chat.model.Validate;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.HibernateException;
import org.hibernate.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "LoadUsers", urlPatterns = {"/load-users"})
public class LoadUsers extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("ok", false);

        JsonObject reqObject = gson.fromJson(req.getReader(), JsonObject.class);
        String id = reqObject.has("id") ? reqObject.get("id").getAsString() : "";
        String searchText = reqObject.has("searchText") ? reqObject.get("searchText").getAsString() : "";

        if (id.isEmpty()) {
            responseObject.addProperty("msg", "Something went wrong! Please sign in again.");
        } else if (!Validate.isInteger(id)) {
            responseObject.addProperty("msg", "Couldn't process this request! \\nYou are a third-party person.");
        } else {

            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                CriteriaBuilder cb = session.getCriteriaBuilder();
                CriteriaQuery<User> cq = cb.createQuery(User.class);
                Root<User> root = cq.from(User.class);

                List<Predicate> predicates = new ArrayList<>();

                predicates.add(cb.notEqual(root.get("id"), Integer.valueOf(id)));

                if (searchText != null && !searchText.isEmpty()) {
                    predicates.add(cb.like(root.get("f_name"), searchText + "%"));
                }

                cq.select(root)
                        .where(predicates.toArray(new Predicate[0]))
                        .orderBy(cb.asc(root.get("f_name")));

                List<User> userList = session.createQuery(cq).getResultList();

                User loggedInUser = session.find(User.class, Integer.valueOf(id));

                JsonArray users = new JsonArray();

                if (searchText == null || searchText.isEmpty()) {
                    JsonObject loggedInUserObject = new JsonObject();
                    loggedInUserObject.addProperty("id", loggedInUser.getId());
                    loggedInUserObject.addProperty("name", loggedInUser.getF_name() + " (You)");
                    loggedInUserObject.addProperty("bio", "Message yourself");
                    loggedInUserObject.addProperty("profile_img", loggedInUser.getProfile_url());
                    loggedInUserObject.addProperty("type", "self");
                    users.add(loggedInUserObject);
                }

                for (User user : userList) {
                    JsonObject userObject = new JsonObject();
                    userObject.addProperty("id", user.getId());
                    userObject.addProperty("name", user.getF_name() + " " + user.getL_name());
                    userObject.addProperty("bio", user.getBio());
                    userObject.addProperty("profile_img", user.getProfile_url());
                    userObject.addProperty("type", "other");
                    users.add(userObject);
                }

                responseObject.add("users", users);
                responseObject.addProperty("ok", true);

            } catch (HibernateException e) {
                System.out.println(e.getMessage());
                responseObject.addProperty("msg", "Cannot process this request!");
            }
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseObject));

    }

}
