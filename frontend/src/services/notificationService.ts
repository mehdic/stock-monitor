import { apiClient } from './api';
import { Notification } from '../types';

/**
 * Notification service for managing user notifications.
 */
class NotificationService {
  /**
   * Get all notifications for the current user.
   *
   * @returns List of notifications
   */
  async getNotifications(): Promise<Notification[]> {
    const response = await apiClient.get<Notification[]>('/notifications');
    return response.data;
  }

  /**
   * Get unread notifications for the current user.
   *
   * @returns List of unread notifications
   */
  async getUnreadNotifications(): Promise<Notification[]> {
    const response = await apiClient.get<Notification[]>('/notifications/unread');
    return response.data;
  }

  /**
   * Mark a notification as read.
   *
   * @param notificationId Notification ID
   * @returns Updated notification
   */
  async markAsRead(notificationId: string): Promise<Notification> {
    const response = await apiClient.put<Notification>(`/notifications/${notificationId}/read`);
    return response.data;
  }

  /**
   * Mark all notifications as read for the current user.
   *
   * @returns Number of notifications marked as read
   */
  async markAllAsRead(): Promise<{ count: number }> {
    const response = await apiClient.put<{ count: number }>('/notifications/read-all');
    return response.data;
  }

  /**
   * Delete a notification.
   *
   * @param notificationId Notification ID
   */
  async deleteNotification(notificationId: string): Promise<void> {
    await apiClient.delete(`/notifications/${notificationId}`);
  }

  /**
   * Get notification count (total and unread).
   *
   * @returns Notification counts
   */
  async getNotificationCount(): Promise<{ total: number; unread: number }> {
    const response = await apiClient.get<{ total: number; unread: number }>('/notifications/count');
    return response.data;
  }
}

export const notificationService = new NotificationService();
