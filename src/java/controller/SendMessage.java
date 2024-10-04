/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;

import entity.Chat;
import entity.ChatStatus;
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
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author SINGER
 */
@MultipartConfig
@WebServlet(name = "SendMessage", urlPatterns = {"/SendMessage"})
public class SendMessage extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        Part image = req.getPart("image");
        String msg = req.getParameter("message");
        String toId = req.getParameter("toUser");
        String fromId = req.getParameter("fromUser");
        boolean reply = Boolean.parseBoolean(req.getParameter("reply"));

        Session session = HibernateUtil.getSessionFactory().openSession();

        try {
            User fromUser = (User) session.get(User.class, Integer.valueOf(fromId));
            User toUser = (User) session.get(User.class, Integer.valueOf(toId));

            Criteria statusCriteria = session.createCriteria(ChatStatus.class);
            statusCriteria.add(Restrictions.eq("name", "Delivered"));
            ChatStatus chatStatus = (ChatStatus) statusCriteria.list().get(0);

            Chat chat = new Chat();
            chat.setFrom(fromUser);
            chat.setTo(toUser);
            chat.setChatStatus(chatStatus);
            chat.setDateTime(new Date());

            if (msg != null) {
                chat.setMessage(msg);
            }
            if (image != null) {
                String applicationPath = req.getServletContext().getRealPath("");
                String newApplicationPath = applicationPath.replace("build" + File.separator + "web", "web");
                String fileName = fromUser.getId() + "-" + toUser.getId() + "message" + System.currentTimeMillis() + ".png";

                File folder = new File(newApplicationPath + "//images//chat//" + fromUser.getId() + "-" + toUser.getId());
                folder.mkdir();

                File imageFile = new File(folder, fileName);
                InputStream inputStreamImage = image.getInputStream();
                Files.copy(inputStreamImage, imageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                chat.setImg(fileName);
            }
            if (reply) {
                String msgId = req.getParameter("msgId");

                Chat replyedTo = (Chat) session.get(Chat.class, Integer.valueOf(msgId));
                chat.setReply(replyedTo);
            }

            session.save(chat);
            session.beginTransaction().commit();
        } catch (NumberFormatException | HibernateException e) {
            System.out.println(e.getMessage());
        }
        session.close();

        resp.setContentType("application/json");
    }

}
