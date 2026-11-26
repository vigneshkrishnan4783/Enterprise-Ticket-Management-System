package com.enterprise.ticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.Notification;
import com.enterprise.ticket.model.NotificationEventType;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.NotificationRepository;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.response.NotificationResponse;
import com.enterprise.ticket.response.UnreadCountResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

class NotificationServiceTest {

  private NotificationService notificationService;
  private StubNotificationRepository notificationRepository;
  private StubUserRepository userRepository;
  private User testUser;
  private Ticket testTicket;

  @BeforeEach
  void setUp() {
    notificationRepository = new StubNotificationRepository();
    userRepository = new StubUserRepository();
    notificationService = new NotificationService(notificationRepository, userRepository);

    testUser = User.builder().username("testuser").email("test@example.com").build();
    testUser.setId(100L);
    userRepository.users.add(testUser);

    testTicket = Ticket.builder().ticketNumber("TKT-10001").title("Sample Ticket").build();
    testTicket.setId(10L);
  }

  @Test
  @DisplayName("createNotification should save notification when recipient is present")
  void testCreateNotificationSuccess() {
    notificationService.createNotification(
        testUser, testTicket, NotificationEventType.TICKET_CREATED, "Ticket Created", "Your ticket has been created.");

    assertEquals(1, notificationRepository.notifications.size());
    Notification saved = notificationRepository.notifications.getFirst();
    assertEquals("Ticket Created", saved.getTitle());
    assertEquals(NotificationEventType.TICKET_CREATED, saved.getEventType());
    assertFalse(saved.getIsRead());
    assertEquals(testUser, saved.getRecipient());
  }

  @Test
  @DisplayName("createNotification should safely ignore null recipient")
  void testCreateNotificationNullRecipient() {
    notificationService.createNotification(
        null, testTicket, NotificationEventType.TICKET_CREATED, "Ticket Created", "Message");

    assertTrue(notificationRepository.notifications.isEmpty());
  }

  @Test
  @DisplayName("getUserNotifications should return list of notifications for user")
  void testGetUserNotifications() {
    Notification n1 =
        Notification.builder()
            .id(1L)
            .recipient(testUser)
            .ticket(testTicket)
            .eventType(NotificationEventType.TICKET_ASSIGNED)
            .title("Assigned")
            .message("Ticket assigned")
            .isRead(false)
            .build();
    n1.setCreatedAt(Instant.now());
    notificationRepository.notifications.add(n1);

    List<NotificationResponse> responses = notificationService.getUserNotifications("testuser", false);
    assertEquals(1, responses.size());
    assertEquals("Assigned", responses.getFirst().title());
    assertEquals("TKT-10001", responses.getFirst().ticketNumber());
  }

  @Test
  @DisplayName("getUnreadCount should return correct count")
  void testGetUnreadCount() {
    Notification n1 =
        Notification.builder()
            .id(1L)
            .recipient(testUser)
            .isRead(false)
            .title("Title")
            .message("Msg")
            .eventType(NotificationEventType.TICKET_CREATED)
            .build();
    notificationRepository.notifications.add(n1);

    UnreadCountResponse response = notificationService.getUnreadCount("testuser");
    assertEquals(1, response.unreadCount());
  }

  @Test
  @DisplayName("markAsRead should update isRead and readAt")
  void testMarkAsReadSuccess() {
    Notification n1 =
        Notification.builder()
            .id(1L)
            .recipient(testUser)
            .isRead(false)
            .title("Title")
            .message("Msg")
            .eventType(NotificationEventType.TICKET_CREATED)
            .build();
    notificationRepository.notifications.add(n1);

    NotificationResponse response = notificationService.markAsRead(1L, "testuser");
    assertTrue(response.isRead());
    assertNotNull(response.readAt());
  }

  @Test
  @DisplayName("markAsRead should throw forbidden when accessed by different user")
  void testMarkAsReadForbidden() {
    User otherUser = User.builder().username("otheruser").build();
    otherUser.setId(200L);
    userRepository.users.add(otherUser);

    Notification n1 =
        Notification.builder()
            .id(1L)
            .recipient(otherUser)
            .isRead(false)
            .title("Title")
            .message("Msg")
            .eventType(NotificationEventType.TICKET_CREATED)
            .build();
    notificationRepository.notifications.add(n1);

    assertThrows(RestException.class, () -> notificationService.markAsRead(1L, "testuser"));
  }

  @Test
  @DisplayName("markAllAsRead should invoke repository markAll")
  void testMarkAllAsRead() {
    notificationService.markAllAsRead("testuser");
    assertTrue(notificationRepository.markAllCalled);
  }

  private static class StubNotificationRepository implements NotificationRepository {
    List<Notification> notifications = new ArrayList<>();
    AtomicLong idGenerator = new AtomicLong(1);
    boolean markAllCalled = false;

    @Override
    public List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId) {
      return notifications.stream().filter(n -> n.getRecipient().getId().equals(recipientId)).toList();
    }

    @Override
    public List<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId) {
      return notifications.stream()
          .filter(n -> n.getRecipient().getId().equals(recipientId) && !Boolean.TRUE.equals(n.getIsRead()))
          .toList();
    }

    @Override
    public long countByRecipientIdAndIsReadFalse(Long recipientId) {
      return notifications.stream()
          .filter(n -> n.getRecipient().getId().equals(recipientId) && !Boolean.TRUE.equals(n.getIsRead()))
          .count();
    }

    @Override
    public int markAllAsReadForRecipient(Long recipientId) {
      markAllCalled = true;
      int count = 0;
      for (Notification n : notifications) {
        if (n.getRecipient().getId().equals(recipientId) && !Boolean.TRUE.equals(n.getIsRead())) {
          n.setIsRead(true);
          n.setReadAt(Instant.now());
          count++;
        }
      }
      return count;
    }

    @Override
    public <S extends Notification> S save(S entity) {
      if (entity.getId() == null) {
        entity.setId(idGenerator.getAndIncrement());
      }
      notifications.removeIf(n -> n.getId().equals(entity.getId()));
      notifications.add(entity);
      return entity;
    }

    @Override
    public Optional<Notification> findById(Long id) {
      return notifications.stream().filter(n -> n.getId().equals(id)).findFirst();
    }

    @Override public void flush() {}
    @Override public <S extends Notification> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends Notification> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
    @Override public void deleteAllInBatch(Iterable<Notification> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
    @Override public void deleteAllInBatch() {}
    @Override public Notification getOne(Long id) { return null; }
    @Override public Notification getById(Long id) { return null; }
    @Override public Notification getReferenceById(Long id) { return null; }
    @Override public <S extends Notification> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends Notification> List<S> findAll(Example<S> example) { return List.of(); }
    @Override public <S extends Notification> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
    @Override public <S extends Notification> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends Notification> long count(Example<S> example) { return 0; }
    @Override public <S extends Notification> boolean exists(Example<S> example) { return false; }
    @Override public <S extends Notification, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends Notification> List<S> saveAll(Iterable<S> entities) { return List.of(); }
    @Override public boolean existsById(Long id) { return false; }
    @Override public List<Notification> findAll() { return notifications; }
    @Override public List<Notification> findAllById(Iterable<Long> ids) { return List.of(); }
    @Override public long count() { return notifications.size(); }
    @Override public void deleteById(Long id) {}
    @Override public void delete(Notification entity) {}
    @Override public void deleteAllById(Iterable<? extends Long> ids) {}
    @Override public void deleteAll(Iterable<? extends Notification> entities) {}
    @Override public void deleteAll() {}
    @Override public List<Notification> findAll(Sort sort) { return List.of(); }
    @Override public Page<Notification> findAll(Pageable pageable) { return Page.empty(); }
  }

  private static class StubUserRepository implements UserRepository {
    List<User> users = new ArrayList<>();

    @Override
    public Optional<User> findByUsername(String username) {
      return users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    @Override public Optional<User> findByEmail(String email) { return Optional.empty(); }
    @Override public boolean existsByUsername(String username) { return false; }
    @Override public boolean existsByEmail(String email) { return false; }
    @Override public List<User> findAll() { return users; }
    @Override public List<User> findAll(Sort sort) { return List.of(); }
    @Override public Page<User> findAll(Pageable pageable) { return Page.empty(); }
    @Override public <S extends User> S save(S entity) { return entity; }
    @Override public <S extends User> List<S> saveAll(Iterable<S> entities) { return List.of(); }
    @Override public Optional<User> findById(Long id) { return users.stream().filter(u -> u.getId().equals(id)).findFirst(); }
    @Override public boolean existsById(Long id) { return false; }
    @Override public List<User> findAllById(Iterable<Long> ids) { return List.of(); }
    @Override public long count() { return users.size(); }
    @Override public void deleteById(Long id) {}
    @Override public void delete(User entity) {}
    @Override public void deleteAllById(Iterable<? extends Long> ids) {}
    @Override public void deleteAll(Iterable<? extends User> entities) {}
    @Override public void deleteAll() {}
    @Override public void flush() {}
    @Override public <S extends User> S saveAndFlush(S entity) { return entity; }
    @Override public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
    @Override public void deleteAllInBatch(Iterable<User> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
    @Override public void deleteAllInBatch() {}
    @Override public User getOne(Long id) { return null; }
    @Override public User getById(Long id) { return null; }
    @Override public User getReferenceById(Long id) { return null; }
    @Override public <S extends User> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends User> List<S> findAll(Example<S> example) { return List.of(); }
    @Override public <S extends User> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
    @Override public <S extends User> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends User> long count(Example<S> example) { return 0; }
    @Override public <S extends User> boolean exists(Example<S> example) { return false; }
    @Override public <S extends User, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
  }
}
