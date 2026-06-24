# Spring-Fundamentals-2026
# 📚 Library Management System (LMS)
<img src="images/demo.png" width="400"/>
A full-stack web application for managing library operations, including book inventory, user borrowing tracking, fine management, and book reviews.

## 🛠 Technical Specifications

### Core Stack
*   **Framework:** Spring Boot 3.4.0
*   **Language:** Java 17
*   **Build Tool:** Maven
*   **Database:** MySQL (via `mysql-connector-j`)
*   **ORM:** Spring Data JPA (Hibernate)
*   **Template Engine:** Thymeleaf
*   **Utility:** Lombok (to reduce boilerplate code)
*   **Monitoring:** Spring Boot Actuator

### Security & Validation
*   **Password Encryption:** `spring-security-crypto` (BCrypt)
*   **Data Integrity:** `spring-boot-starter-validation` (used for `@NotNull`, `@Size`, `@Email` constraints)

---

## 🗄 Database Architecture

The system uses a relational MySQL database with the following entity map:

### Entities & Relationships
- **`User`**: Manages membership and authentication. 
    - $\text{1} \rightarrow \text{Many}$ with `BorrowRecord` and `Review`.
- **`Book`**: The core inventory item.
    - $\text{Many} \rightarrow \text{1}$ with `Author` and `Category`.
    - $\text{1} \rightarrow \text{Many}$ with `BorrowRecord` and `Review`.
- **`Author` & `Category`**: Lookup tables for book organization.
- **`BorrowRecord`**: Tracks the lifecycle of a book loan (Borrow $\rightarrow$ Due $\rightarrow$ Return).
    - $\text{1} \rightarrow \text{1}$ relationship with `Fine`.
- **`Fine`**: Handles financial penalties for overdue returns.
- **`Review`**: Stores user-generated ratings (1-5) and comments.

---

## ⚙️ Application Features

### 1. Catalog Management
- View all books with search and filter functionality.
- Admin ability to add/edit/delete books, authors, and categories.
- Real-time tracking of `copies_available`.

### 2. Borrowing Logic
- **Checkout:** Validates book availability $\rightarrow$ creates record $\rightarrow$ reduces stock.
- **Return:** Updates return date $\rightarrow$ increases stock $\rightarrow$ triggers fine check.
- **Fine Calculation:** Automatic generation of a `Fine` entity if `return_date > due_date`.

### 3. User Experience
- **Dashboard:** Users can see their active borrows and unpaid fines.
- **Reviews:** Users can rate books they have borrowed.
- **Authentication:** Secure password storage using BCrypt hashing.

---

## 📂 Suggested Project Structure

To keep the code clean, the following package structure is recommended:

```text
com.library
├── config            # Security and App configurations
├── controller        # Web Request Handlers (Spring MVC)
├── model             # JPA Entities (User, Book, etc.)
├── repository        # Spring Data JPA Repositories
├── service           # Business Logic (Borrowing, Fine calculations)
├── dto               # Data Transfer Objects (for Form submissions)
└── validation       # Custom validators (if any)
