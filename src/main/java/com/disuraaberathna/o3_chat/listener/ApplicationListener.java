package com.disuraaberathna.o3_chat.listener;

import com.disuraaberathna.o3_chat.entity.ChatStatus;
import com.disuraaberathna.o3_chat.model.HibernateUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.Arrays;
import java.util.List;

@WebListener
public class ApplicationListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Application started!");
        initializeChatStatuses();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Application stopped!");
        HibernateUtil.getSessionFactory().close();
    }

    private void initializeChatStatuses() {
        List<String> requiredStatuses = Arrays.asList("Sent", "Delivered", "Seen", "Readed");

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            for (String statusName : requiredStatuses) {
                Query<ChatStatus> query = session.createQuery("FROM ChatStatus WHERE name = :name", ChatStatus.class);
                query.setParameter("name", statusName);
                ChatStatus existingStatus = query.uniqueResult();

                if (existingStatus == null) {
                    ChatStatus newStatus = new ChatStatus();
                    newStatus.setName(statusName);
                    session.persist(newStatus);
                    System.out.println("Initialized ChatStatus: " + statusName);
                }
            }

            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            e.printStackTrace();
        }
    }
}
