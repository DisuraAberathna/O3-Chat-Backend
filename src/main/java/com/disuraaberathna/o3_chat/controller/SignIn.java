package com.disuraaberathna.o3_chat.controller;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.disuraaberathna.o3_chat.entity.User;
import com.disuraaberathna.o3_chat.model.HibernateUtil;
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

import java.io.IOException;
import java.util.List;

@WebServlet(name = "SignIn", urlPatterns = {"/sign-in"})
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
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                CriteriaBuilder cb = session.getCriteriaBuilder();
                CriteriaQuery<User> cq = cb.createQuery(User.class);
                Root<User> root = cq.from(User.class);

                cq.select(root).where(cb.equal(root.get("username"), username));

                List<User> users = session.createQuery(cq).getResultList();

                if (!users.isEmpty()) {
                    User user = users.get(0);

                    BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.getPassword());

                    if (result.verified) {
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
                                userObject.addProperty("profile_img", user.getProfile_url());
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
                } else {
                    responseObject.addProperty("msg", "Invalid credentials!");
                }

            } catch (HibernateException e) {
                System.out.println(e.getMessage());
                responseObject.addProperty("msg", "Can not process this request!");
            }
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseObject));

    }

}
