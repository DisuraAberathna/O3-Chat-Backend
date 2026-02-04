package com.disuraaberathna.o3_chat.controller;

import com.disuraaberathna.o3_chat.entity.User;
import com.disuraaberathna.o3_chat.model.HibernateUtil;
import com.disuraaberathna.o3_chat.model.Validate;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.hibernate.Session;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@MultipartConfig
@WebServlet(name = "SaveProfileImage", urlPatterns = {"/save-profile-image"})
public class SaveProfileImage extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("ok", false);

        String id = req.getParameter("id");
        Part image = req.getPart("image");

        if (id.isEmpty()) {
            responseObject.addProperty("msg", "Something went wrong! Please sign in again.");
        } else if (!Validate.isInteger(id)) {
            responseObject.addProperty("msg", "Cloudn't process this request! \\nYou are a third-party person.");
        } else if (image.getSubmittedFileName() == null) {
            responseObject.addProperty("msg", "Please select a profile picture!");
        } else {

            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                User user = session.find(User.class, Integer.valueOf(id));

                if (user != null) {
                    String applicationPath = req.getServletContext().getRealPath("");
                    String newApplicationPath = applicationPath.replace("build" + File.separator + "web", "web");
                    File folder = new File(newApplicationPath + "//images//user//" + id);
                    folder.mkdir();

                    File imageFile = new File(folder, id + "avatar.png");
                    InputStream inputStreamImage = image.getInputStream();
                    Files.copy(inputStreamImage, imageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    responseObject.addProperty("ok", true);
                } else {
                    responseObject.addProperty("msg", "User not found! Please sign in again.");
                }
            } catch (NumberFormatException e) {
                System.out.println(e.getMessage());
                responseObject.addProperty("msg", "Can not process this request!");
            }
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseObject));

    }

}
