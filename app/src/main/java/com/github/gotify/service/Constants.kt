package com.github.gotify.service

/**
 * Command to the service to register a client, receiving callbacks
 * from the service.  The Message's replyTo field must be a Messenger of
 * the client where callbacks should be sent.
 */
const val TYPE_CLIENT_STARTED = 1
const val TYPE_REGISTER_CLIENT = 2
const val TYPE_REGISTERED_CLIENT = 3
const val TYPE_UNREGISTER_CLIENT = 4
const val TYPE_UNREGISTERED_CLIENT = 5
const val TYPE_MESSAGE = 6
const val TYPE_CHANGED_URL = 7