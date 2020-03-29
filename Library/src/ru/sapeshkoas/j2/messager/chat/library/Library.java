package ru.sapeshkoas.j2.messager.chat.library;

public class Library {
     /*
/auth_request±login±password
/auth_accept±nickname
/auth_error
/broadcast±msg
/msg_format_error±msg
* */

     public static final String DELIMITER = "#";
     public static final String AUTH_REQUEST = "/auth_request";
     public static final String AUTH_ACCEPT = "/auth_accept";
     public static final String AUTH_DENIED = "/auth_denied";
     public static final String MSG_FORMAT_ERROR = "/msg_format_error";
     public static final String TYPE_BROADCAST = "/bcast";

     public static String getAuthRequest(String login, String password) {
         return AUTH_REQUEST + DELIMITER + login + DELIMITER + password;
     }

     public static String getAuthAccept(String nickName) {
         return AUTH_ACCEPT + DELIMITER + nickName;
     }

     public static String getAuthDenied() {
         return AUTH_DENIED;
     }

     public static String getMsgFormatError(String msg) {
         return MSG_FORMAT_ERROR + DELIMITER + msg;
     }

     public static String getBroadcast(String src, String message) {
         return TYPE_BROADCAST + DELIMITER + System.currentTimeMillis() + DELIMITER + src + DELIMITER + message;
     }
}
