package vn.codegym.lunchbot_be.service;

import vn.codegym.lunchbot_be.model.Notification;

public interface NotificationService {

    void sendPrivateNotification(String username, Notification notification);

}
