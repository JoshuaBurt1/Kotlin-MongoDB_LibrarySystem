package org.example

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document

//Local MongoDB Connection - only you have access to data, it is saved after each session
//New Project -> Kotlin; Build System -> Gradle

//Download MongoDB Compass
//Next to 'Connections'; click + (Add new connection)
//Enter the connection string i.e. mongodb://localhost:27017

class MongoDBManager {
    private val client = MongoClients.create("mongodb://localhost:27017") // Adjust connection string if necessary
    private val database: MongoDatabase = client.getDatabase("librarySystem")

    init {
        // Check the connection
        try {
            // Attempt to ping the database
            client.getDatabase("admin").runCommand(Document("ping", 1))
            println("MongoDB connected successfully!")
        } catch (e: Exception) {
            println("Failed to connect to MongoDB: ${e.message}")
        }
    }

    val studentCollection: MongoCollection<Document> = database.getCollection("students")
    val bookCollection: MongoCollection<Document> = database.getCollection("books")
}