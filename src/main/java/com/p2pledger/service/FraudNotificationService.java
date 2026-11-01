package com.p2pledger.service;

import com.p2pledger.model.Notification;
import com.p2pledger.model.NotificationType;
import com.p2pledger.model.Role;
import com.p2pledger.model.User;
import com.p2pledger.repository.NotificationRepository;
import com.p2pledger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FraudNotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyAdminsAboutDailyLimitViolation(Long senderId, String senderName, 
                                                      BigDecimal attemptedAmount, 
                                                      BigDecimal todaySpending, 
                                                      BigDecimal dailyLimit) {
        // Get all admin users
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .collect(Collectors.toList());

        // Calculate how much over the limit
        BigDecimal totalWouldBe = todaySpending.add(attemptedAmount);
        BigDecimal overLimit = totalWouldBe.subtract(dailyLimit);

        // Send notification to each admin
        for (User admin : admins) {
            Notification notification = Notification.builder()
                    .userId(admin.getId())
                    .type(NotificationType.FRAUD_ALERT)
                    .message("Daily limit violation: User #" + senderId + " (" + senderName +
                            ") attempted ₹" + attemptedAmount + " transfer. Today's spending: ₹" + todaySpending +
                            " (limit: ₹" + dailyLimit + ", over by ₹" + overLimit + ")")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyAdminsAboutDuplicateTransfer(Long senderId, String senderName,
                                                    Long receiverId, String receiverName,
                                                    BigDecimal amount) {
        // Get all admin users
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .collect(Collectors.toList());

        // Send notification to each admin
        for (User admin : admins) {
            Notification notification = Notification.builder()
                    .userId(admin.getId())
                    .type(NotificationType.FRAUD_ALERT)
                    .message("Duplicate transaction detected: User #" + senderId + " (" + senderName +
                            ") attempted duplicate transfer of ₹" + amount + " to User #" + receiverId +
                            " (" + receiverName + ") within 2 minutes")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
        }
    }
}
