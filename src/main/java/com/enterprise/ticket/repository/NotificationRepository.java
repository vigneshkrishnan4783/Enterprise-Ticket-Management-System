package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

  List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

  List<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId);

  long countByRecipientIdAndIsReadFalse(Long recipientId);

  @Modifying
  @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.recipient.id = :recipientId AND n.isRead = false")
  int markAllAsReadForRecipient(@Param("recipientId") Long recipientId);
}
