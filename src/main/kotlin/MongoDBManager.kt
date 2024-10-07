package org.example

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document

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