package com.disuraaberathna.o3_chat.controller;

import com.disuraaberathna.o3_chat.entity.Chat;
import com.disuraaberathna.o3_chat.entity.ChatStatus;
import com.disuraaberathna.o3_chat.entity.User;
import com.disuraaberathna.o3_chat.model.HibernateUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.hibernate.HibernateException;
import org.hibernate.Session;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;

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
            User fromUser = session.find(User.class, Integer.valueOf(fromId));
            User toUser = session.find(User.class, Integer.valueOf(toId));

            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<ChatStatus> cq = cb.createQuery(ChatStatus.class);
            Root<ChatStatus> root = cq.from(ChatStatus.class);

            String statusName = fromUser.equals(toUser) ? "Readed" : "Delivered";
            cq.select(root).where(cb.equal(root.get("name"), statusName));

            ChatStatus chatStatus = session.createQuery(cq).getSingleResult();

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
                String fileName;

                String condition1 = applicationPath + File.separator + "images" + File.separator + "chat" + File.separator
                        + fromUser.getId() + "-" + toUser.getId() + File.separator;
                String condition2 = applicationPath + File.separator + "images" + File.separator + "chat" + File.separator
                        + toUser.getId() + "-" + fromUser.getId() + File.separator;

                File folder;
                File condition1File = new File(condition1);
                File condition2File = new File(condition2);

                if (condition1File.exists()) {
                    folder = new File(newApplicationPath + "//images//chat//" + fromUser.getId() + "-" + toUser.getId());
                    fileName = fromUser.getId() + "-" + toUser.getId() + "message" + System.currentTimeMillis() + ".png";
                } else if (condition2File.exists()) {
                    folder = new File(newApplicationPath + "//images//chat//" + toUser.getId() + "-" + fromUser.getId());
                    fileName = toUser.getId() + "-" + fromUser.getId() + "message" + System.currentTimeMillis() + ".png";
                } else {
                    folder = new File(newApplicationPath + "//images//chat//" + fromUser.getId() + "-" + toUser.getId());
                    fileName = fromUser.getId() + "-" + toUser.getId() + "message" + System.currentTimeMillis() + ".png";
                }

                folder.mkdirs();

                File imageFile = new File(folder, fileName);
                try (InputStream inputStreamImage = image.getInputStream()) {
                    Files.copy(inputStreamImage, imageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                chat.setImg(fileName);
            }

            if (reply) {
                String msgId = req.getParameter("msgId");
                Chat repliedTo = session.find(Chat.class, Integer.valueOf(msgId));
                chat.setReply(repliedTo);
            }

            session.beginTransaction();
            session.persist(chat);
            session.getTransaction().commit();

        } catch (NumberFormatException | HibernateException e) {
            System.out.println(e.getMessage());
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
        } finally {
            session.close();
        }

        resp.setContentType("application/json");
    }

}
