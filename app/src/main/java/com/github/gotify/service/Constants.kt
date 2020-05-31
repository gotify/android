package com.github.gotify.service

/**
 * Command to the service to register a client, receiving callbacks
 * from the service.  The Message's replyTo field must be a Messenger of
 * the client where callbacks should be sent.
 */
const val MSG_REGISTER_CLIENT = 1
const val MSG_UNREGISTER_CLIENT = 2
const val MSG_START = 3
const val MSG_GET_INFO = 4
const val MSG_NEW_URL = 5
const val MSG_NOTIFICATION = 6
const val MSG_IS_REGISTERED = 7
/**
 * ERRORS
 */
const val ERROR_NOT_CONNECTED = 1
const val ERROR_NULL_OR_BLANK = 2
const val ERROR_ALREADY_REGISTERED = 3