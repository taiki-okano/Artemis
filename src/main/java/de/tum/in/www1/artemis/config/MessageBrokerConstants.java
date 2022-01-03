package de.tum.in.www1.artemis.config;

/**
 * Message broker constants containing names of the used queues for communication between microservices.
 */
public final class MessageBrokerConstants {

    // Artemis <-> User Management Microservice queues
    public static final String USER_MANAGEMENT_QUEUE_SEND_ACTIVATION_MAIL = "user_management_queue.send_activation_mail";

    public static final String USER_MANAGEMENT_QUEUE_SEND_PASSWORD_RESET_MAIL = "user_management_queue.send_password_reset_mail";

    public static final String USER_MANAGEMENT_QUEUE_ARE_GROUPS_AVAILABLE = "user_management_queue.are_groups_available";

    public static final String USER_MANAGEMENT_QUEUE_ARE_GROUPS_AVAILABLE_RESP = "user_management_queue.are_groups_available_resp";

    public static final String USER_MANAGEMENT_QUEUE_UPDATE_USER_GROUPS = "user_management_queue.update_user_groups";

    public static final String USER_MANAGEMENT_QUEUE_CREATE_INTERNAL_USER = "user_management_queue.create_internal_user";

    public static final String USER_MANAGEMENT_QUEUE_CREATE_INTERNAL_USER_RESP = "user_management_queue.create_internal_user_resp";

    public static final String USER_MANAGEMENT_QUEUE_ACTIVATE_USER = "user_management_queue.activate_user";

    public static final String USER_MANAGEMENT_QUEUE_CREATE_USER = "user_management_queue.create_user";

    public static final String USER_MANAGEMENT_QUEUE_CREATE_USER_RESP = "user_management_queue.create_user_resp";

    public static final String USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET = "user_management_queue.request_password_reset";

    public static final String USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET_RESP = "user_management_queue.request_password_reset_resp";

    public static final String USER_MANAGEMENT_QUEUE_SAVE_USER = "user_management_queue.save_user";

    public static final String USER_MANAGEMENT_QUEUE_SAVE_USER_RESP = "user_management_queue.save_user_resp";

    private MessageBrokerConstants() {
    }
}
