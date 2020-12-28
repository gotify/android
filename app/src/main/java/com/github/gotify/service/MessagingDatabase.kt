package com.github.gotify.service

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DB_NAME = "gotify_service"
private const val DB_VERSION = 1

class MessagingDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION){
    private val TABLE_APPS = "apps"
    private val FIELD_PACKAGE_NAME = "package_name"
    private val FIELD_APP_ID = "app_id"
    private val FIELD_CONNECTOR_TOKEN = "connector_token"
    private val FIELD_GOTIFY_TOKEN = "gotify_token"
    private val CREATE_TABLE_APPS = "CREATE TABLE $TABLE_APPS (" +
            "$FIELD_PACKAGE_NAME TEXT," +
            "$FIELD_APP_ID INT," +
            "$FIELD_GOTIFY_TOKEN TEXT," +
            "$FIELD_CONNECTOR_TOKEN TEXT," +
            "PRIMARY KEY ($FIELD_PACKAGE_NAME));"

    override fun onCreate(db: SQLiteDatabase){
        db.execSQL(CREATE_TABLE_APPS)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        throw IllegalStateException("Upgrades not supported")
    }

    fun registerApp(packageName: String, appId :Long, gotify_token: String, connector_token: String){
        val db = writableDatabase
        val values = ContentValues().apply {
            put(FIELD_PACKAGE_NAME, packageName)
            put(FIELD_APP_ID,appId.toString())
            put(FIELD_GOTIFY_TOKEN,gotify_token)
            put(FIELD_CONNECTOR_TOKEN,connector_token)
        }
        db.insert(TABLE_APPS,null,values)
    }

    fun unregisterApp(packageName: String, connector_token: String){
        val db = writableDatabase
        val selection = "$FIELD_PACKAGE_NAME = ? AND $FIELD_CONNECTOR_TOKEN = ?"
        val selectionArgs = arrayOf(packageName,connector_token)
        db.delete(TABLE_APPS,selection,selectionArgs)
    }

    fun forceUnregisterApp(packageName: String){
        val db = writableDatabase
        val selection = "$FIELD_PACKAGE_NAME = ?"
        val selectionArgs = arrayOf(packageName)
        db.delete(TABLE_APPS,selection,selectionArgs)
    }

    fun isRegistered(packageName: String): Boolean {
        val db = readableDatabase
        val selection = "$FIELD_PACKAGE_NAME = ?"
        val selectionArgs = arrayOf(packageName)
        return db.query(
                TABLE_APPS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
        ).use { cursor ->
            (cursor != null && cursor.count > 0)
        }
    }

    fun strictIsRegistered(packageName: String, connector_token: String): Boolean {
        val db = readableDatabase
        val selection = "$FIELD_PACKAGE_NAME = ? AND $FIELD_CONNECTOR_TOKEN = ?"
        val selectionArgs = arrayOf(packageName,connector_token)
        return db.query(
                TABLE_APPS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
        ).use { cursor ->
            (cursor != null && cursor.count > 0)
        }
    }

    fun getAppFromId(appId: Long): String{
        val db = readableDatabase
        val projection = arrayOf(FIELD_PACKAGE_NAME)
        val selection = "$FIELD_APP_ID = ?"
        val selectionArgs = arrayOf(appId.toString())
        return db.query(
                TABLE_APPS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndex(FIELD_PACKAGE_NAME)) else ""
        }
    }

    fun getAppId(packageName: String): Long{
        val db = readableDatabase
        val projection = arrayOf(FIELD_APP_ID)
        val selection = "$FIELD_PACKAGE_NAME = ?"
        val selectionArgs = arrayOf(packageName)
        return db.query(
                TABLE_APPS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(cursor.getColumnIndex(FIELD_APP_ID)) else -1
        }
    }

    fun getGotifyToken(packageName: String, remove: Boolean): String{
        val db = readableDatabase
        val projection = arrayOf(FIELD_GOTIFY_TOKEN)
        val selection = "$FIELD_PACKAGE_NAME = ?"
        val selectionArgs = arrayOf(packageName)
        val token = db.query(
                TABLE_APPS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndex(FIELD_GOTIFY_TOKEN)) else ""
        }
        if (remove)
            removeGotifyToken(token)
        return token
    }

    private fun removeGotifyToken(packageName: String){
        val db = writableDatabase
        val values = ContentValues().apply {
            put(FIELD_GOTIFY_TOKEN,"null")
        }
        val selection = "$FIELD_PACKAGE_NAME = ?"
        val selectionArgs = arrayOf(packageName)
        db.update(TABLE_APPS,values,selection,selectionArgs)
    }

    fun getConnectorToken(packageName: String): String{
        val db = readableDatabase
        val projection = arrayOf(FIELD_CONNECTOR_TOKEN)
        val selection = "$FIELD_PACKAGE_NAME = ?"
        val selectionArgs = arrayOf(packageName)
        val token = db.query(
                TABLE_APPS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndex(FIELD_CONNECTOR_TOKEN)) else ""
        }
        return token
    }

    fun listApps(): List<String>{
        val db = readableDatabase
        val projection = arrayOf(FIELD_PACKAGE_NAME)
        return db.query(
                TABLE_APPS,
                projection,
                null,
                null,
                null,
                null,
                null
        ).use{ cursor ->
            generateSequence { if (cursor.moveToNext()) cursor else null }
                    .map{ it.getString(it.getColumnIndex(FIELD_PACKAGE_NAME)) }
                    .toList()
        }
    }
}