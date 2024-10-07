package org.example

import org.bson.Document

data class Student(val id: String, val name: String, val borrowedBooks: MutableList<Book>)
data class Book(val title: String, val author: String, var status: String)

fun main() {
    val mongoDBManager = MongoDBManager()

    // Load initial data from MongoDB
    val students = loadStudents(mongoDBManager)
    val books = loadBooks(mongoDBManager)

    // Main menu loop
    while (true) {
        println("\nLibrary System:\n1. Add Book\n2. Add Student\n3. Borrow Book\n4. Return Book\n5. Display Books\n6. Display Students\n7. Exit")
        print("Choose an option: ")

        when (readLine()?.toIntOrNull()) {
            1 -> addBook(mongoDBManager, books)
            2 -> addStudent(mongoDBManager, students)
            3 -> borrowBook(mongoDBManager, books, students)
            4 -> returnBook(mongoDBManager, books, students)
            5 -> displayBooks(books)
            6 -> displayStudents(students)
            7 -> {
                println("Exiting the program.")
                return
            }
            else -> println("Invalid option. Try again.")
        }
    }
}

fun loadStudents(mongoDBManager: MongoDBManager): ArrayList<Student> {
    val studentList = ArrayList<Student>()
    mongoDBManager.studentCollection.find().forEach { doc ->
        val id = doc.getString("id")
        val name = doc.getString("name")
        val borrowedBooks = mutableListOf<Book>()

        // Load borrowed books from stored string representation
        val borrowedBooksList = doc.getList("borrowedBooks", String::class.java)
        borrowedBooksList.forEach { borrowedBook ->
            val (title, author) = borrowedBook.split(" by ")
            borrowedBooks.add(Book(title, author, "borrowed")) // Assuming the status is always "borrowed"
        }

        studentList.add(Student(id, name, borrowedBooks))
    }
    return studentList
}

fun loadBooks(mongoDBManager: MongoDBManager): ArrayList<Book> {
    val bookList = ArrayList<Book>()
    mongoDBManager.bookCollection.find().forEach { doc ->
        val title = doc.getString("title")
        val author = doc.getString("author")
        val status = doc.getString("status")
        bookList.add(Book(title, author, status))
    }
    return bookList
}

fun addStudent(mongoDBManager: MongoDBManager, students: ArrayList<Student>) {
    print("Enter student name: ")
    val name = readLine() ?: return
    val newId = (students.size + 1).toString() // Simple ID generation

    val studentDoc = Document("id", newId)
        .append("name", name)
        .append("borrowedBooks", ArrayList<String>()) // Initialize with empty list

    mongoDBManager.studentCollection.insertOne(studentDoc)
    students.add(Student(newId, name, mutableListOf()))
    println("Added student ID '$newId': $name")
}

fun addBook(mongoDBManager: MongoDBManager, books: ArrayList<Book>) {
    print("Enter book title: ")
    val title = readLine() ?: return
    print("Enter book author: ")
    val author = readLine() ?: return

    val bookDoc = Document("title", title)
        .append("author", author)
        .append("status", "available")

    mongoDBManager.bookCollection.insertOne(bookDoc)
    books.add(Book(title, author, "available"))
    println("Added book '$title' by '$author'")
}

fun borrowBook(mongoDBManager: MongoDBManager, books: ArrayList<Book>, students: ArrayList<Student>) {
    print("Enter student ID: ")
    val studentId = readLine() ?: return
    val student = students.find { it.id == studentId } ?: run {
        println("Student not found.")
        return
    }

    print("Enter book title: ")
    val bookTitle = readLine() ?: return
    print("Enter book author: ")
    val bookAuthor = readLine() ?: return

    val book = books.find { it.title == bookTitle && it.author == bookAuthor && it.status == "available" }
    if (book == null) {
        println("Book is not available.")
        return
    }

    student.borrowedBooks.add(book)
    book.status = "borrowed"

    // Update MongoDB
    mongoDBManager.studentCollection.updateOne(
        Document("id", studentId),
        Document("\$set", Document("borrowedBooks", student.borrowedBooks.map { "${it.title} by ${it.author}" }))
    )
    mongoDBManager.bookCollection.updateOne(
        Document("title", bookTitle).append("author", bookAuthor),
        Document("\$set", Document("status", "borrowed"))
    )

    println("Successfully borrowed book '$bookTitle' by '$bookAuthor'.")
}
fun returnBook(mongoDBManager: MongoDBManager, books: ArrayList<Book>, students: ArrayList<Student>) {
    print("Enter student ID: ")
    val studentId = readLine() ?: return
    val student = students.find { it.id == studentId } ?: run {
        println("Student not found.")
        return
    }

    println("Borrowed books: ${student.borrowedBooks.map { it.title }}")
    print("Enter the title of the book to return: ")
    val bookTitle = readLine() ?: return

    val bookToReturn = student.borrowedBooks.find { it.title == bookTitle }
    if (bookToReturn == null) {
        println("This student does not have this book borrowed.")
        return
    }

    // Remove the book from the student's borrowed list
    student.borrowedBooks.remove(bookToReturn)

    // Update the status of the book
    bookToReturn.status = "available"

    // Update MongoDB
    mongoDBManager.studentCollection.updateOne(
        Document("id", studentId),
        Document("\$set", Document("borrowedBooks", student.borrowedBooks.map { "${it.title} by ${it.author}" }))
    )

    // Update the book document in MongoDB
    mongoDBManager.bookCollection.updateOne(
        Document("title", bookToReturn.title).append("author", bookToReturn.author),
        Document("\$set", Document("status", "available"))
    )

    // Also update the book status in the local list
    val bookInList = books.find { it.title == bookToReturn.title && it.author == bookToReturn.author }
    if (bookInList != null) {
        bookInList.status = "available"
    }

    println("Successfully returned book '$bookTitle'.")
}

fun displayBooks(books: ArrayList<Book>) {
    println("Books in the library:")
    books.forEach { println("${it.title} by ${it.author} - Status: ${it.status}") }
}

fun displayStudents(students: ArrayList<Student>) {
    println("Students in the library:")
    students.forEach {
        println("ID: ${it.id}, Name: ${it.name}, Borrowed Books: ${it.borrowedBooks.size}")
    }
}