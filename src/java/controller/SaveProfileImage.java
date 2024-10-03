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
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import model.HibernateUtil;
import model.Validate;
import org.hibernate.Session;

/**
 *
 * @author SINGER
 */
@MultipartConfig
@WebServlet(name = "SaveProfileImage", urlPatterns = {"/SaveProfileImage"})
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
            Session session = HibernateUtil.getSessionFactory().openSession();

            try {
                User user = (User) session.get(User.class, Integer.valueOf(id));

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
            session.close();
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseObject));

    }

}
