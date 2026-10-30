# 🐾 Pet Care REST API — Technical Documentation

## 1. Overview & Architecture

The **Pet Care REST API** is built on **PHP 8+** utilizing MySQL with **PDO** and parameterized prepared statements. It provides secure, token-based JSON RESTful endpoints for mobile client applications.

- **Base URL (Localhost / Apache XAMPP)**: `http://localhost/petcare/public/api/`
- **Base URL (Android Emulator via 10.0.2.2)**: `http://10.0.2.2/petcare/public/api/`
- **Base URL (Local Network / Physical Android Device)**: `http://172.23.176.206/petcare/public/api/`
- **Authentication**: Bearer Token via HTTP header `Authorization: Bearer <token>`
- **Response Format**: Standard JSON envelope:
  ```json
  {
    "success": true,
    "message": "Status description",
    "data": { ... }
  }
  ```

---

## 2. Authentication Endpoints

### 2.1 Register New User
- **Method**: `POST`
- **Endpoint**: `/api/auth/register`
- **Auth**: None (Public)
- **Request Body**:
  ```json
  {
    "name": "Jane Doe",
    "email": "jane@example.com",
    "password": "securepassword",
    "device_name": "Pixel 7"
  }
  ```
- **Response (201 Created)**:
  ```json
  {
    "success": true,
    "message": "Registration successful",
    "data": {
      "token": "7a8b9c0d1e2f...",
      "user": {
        "id": 2,
        "name": "Jane Doe",
        "email": "jane@example.com"
      }
    }
  }
  ```

### 2.2 User Login
- **Method**: `POST`
- **Endpoint**: `/api/auth/login`
- **Auth**: None (Public)
- **Request Body**:
  ```json
  {
    "email": "kavya@petcare.com",
    "password": "password123",
    "device_name": "Pixel 7"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Login successful",
    "data": {
      "token": "2201fc68b2172e1c5a497f69540329af5ef6855b26eebcb6dea9a82b0e8bdf72",
      "user": {
        "id": 1,
        "name": "Kavya",
        "email": "kavya@petcare.com",
        "avatar_url": null
      },
      "active_pet": {
        "id": 1,
        "name": "Clover",
        "species": "Rabbit",
        "breed": "Holland Lop",
        "age_years": "1.2 yrs",
        "weight_kg": "1.80",
        "gender": "Female",
        "microchip_id": "#985142091",
        "health_score": 95,
        "vaccination_status": "Up to Date"
      }
    }
  }
  ```

### 2.3 Forgot Password
- **Method**: `POST`
- **Endpoint**: `/api/auth/forgot-password`
- **Auth**: None (Public)
- **Request Body**:
  ```json
  {
    "email": "kavya@petcare.com"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "If an account exists for kavya@petcare.com, a password reset link has been dispatched.",
    "data": {
      "email": "kavya@petcare.com"
    }
  }
  ```

### 2.4 Get Current User Profile (`Me`)
- **Method**: `GET`
- **Endpoint**: `/api/auth/me`
- **Auth**: Bearer Token
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Success",
    "data": {
      "user": {
        "id": 1,
        "name": "Kavya",
        "email": "kavya@petcare.com"
      },
      "pets": [
        {
          "id": 1,
          "name": "Clover",
          "species": "Rabbit",
          "breed": "Holland Lop",
          "weight_kg": "1.80"
        }
      ]
    }
  }
  ```

### 2.5 Logout
- **Method**: `POST`
- **Endpoint**: `/api/auth/logout`
- **Auth**: Bearer Token
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Logged out successfully",
    "data": null
  }
  ```

---

## 3. Dashboard Endpoints

### 3.1 Get Home Dashboard Overview
- **Method**: `GET`
- **Endpoint**: `/api/dashboard`
- **Auth**: Bearer Token
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Success",
    "data": {
      "user": { "id": 1, "name": "Kavya", "email": "kavya@petcare.com" },
      "active_pet": {
        "id": 1,
        "name": "Clover",
        "species": "Rabbit",
        "breed": "Holland Lop",
        "age_years": "1.2 yrs",
        "weight_kg": "1.80",
        "health_score": 95,
        "vaccination_status": "Up to Date"
      },
      "today_schedule": [
        {
          "id": "1",
          "title": "Morning Timothy Hay & Greens",
          "time": "8:00 AM",
          "type": "reminder",
          "is_completed": false
        },
        {
          "id": "2",
          "title": "Fur Brushing & Nail Check",
          "time": "5:30 PM",
          "type": "reminder",
          "is_completed": false
        }
      ],
      "diet_summary": {
        "consumed_calories": 275,
        "target_calories": 800,
        "progress_ratio": 0.34
      },
      "reminders_count": 7,
      "meals_count": 3
    }
  }
  ```

---

## 4. Pets Management Endpoints

### 4.1 List Pets
- **Method**: `GET`
- **Endpoint**: `/api/pets`
- **Auth**: Bearer Token

### 4.2 Create Pet Profile
- **Method**: `POST`
- **Endpoint**: `/api/pets`
- **Auth**: Bearer Token
- **Request Body**:
  ```json
  {
    "name": "Bella",
    "species": "Dog",
    "breed": "Golden Retriever",
    "age_years": "2.0 yrs",
    "weight_kg": 25.5,
    "gender": "Female"
  }
  ```

### 4.3 Get Pet Weight History
- **Method**: `GET`
- **Endpoint**: `/api/pets/{id}/weights`
- **Auth**: Bearer Token
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": {
      "current_weight": 1.80,
      "target_weight": 1.70,
      "status": "Optimal",
      "history": [
        { "id": 1, "weight_kg": 1.60, "recorded_date": "Jan 1" },
        { "id": 2, "weight_kg": 1.70, "recorded_date": "Feb 1" },
        { "id": 3, "weight_kg": 1.80, "recorded_date": "Mar 1" }
      ]
    }
  }
  ```

### 4.4 Record New Weight Entry
- **Method**: `POST`
- **Endpoint**: `/api/pets/{id}/weights`
- **Auth**: Bearer Token
- **Request Body**:
  ```json
  {
    "weight_kg": 1.85,
    "recorded_date": "Oct 29"
  }
  ```

---

## 5. Diet & Nutrition Endpoints

### 5.1 Calorie Summary
- **Method**: `GET`
- **Endpoint**: `/api/diet/summary`
- **Auth**: Bearer Token

### 5.2 Meal Schedule
- **Method**: `GET`
- **Endpoint**: `/api/diet/schedule`
- **Auth**: Bearer Token

### 5.3 Log Meal
- **Method**: `POST`
- **Endpoint**: `/api/diet/meals`
- **Auth**: Bearer Token
- **Request Body**:
  ```json
  {
    "food_item": "Fresh Timothy Hay & Greens",
    "portion_size": "1.5 Cups",
    "calories": 200,
    "meal_time": "12:30 PM"
  }
  ```

### 5.4 Safe vs. Toxic Foods Library
- **Method**: `GET`
- **Endpoint**: `/api/diet/foods/safe-toxic`
- **Auth**: None (Public)

---

## 6. Reminders & Grooming Endpoints

### 6.1 List Care Reminders
- **Method**: `GET`
- **Endpoint**: `/api/reminders`
- **Auth**: Bearer Token

### 6.2 Create Reminder
- **Method**: `POST`
- **Endpoint**: `/api/reminders`
- **Auth**: Bearer Token
- **Request Body**:
  ```json
  {
    "title": "Bath & Shampoo Session",
    "reminder_time": "Nov 2, 10:00 AM",
    "category": "Grooming",
    "notify_push": 1
  }
  ```

### 6.3 Toggle Reminder Complete
- **Method**: `PUT`
- **Endpoint**: `/api/reminders/{id}/toggle`
- **Auth**: Bearer Token

### 6.4 Grooming Calendar Tasks
- **Method**: `GET`
- **Endpoint**: `/api/reminders/calendar`
- **Auth**: Bearer Token

### 6.5 Grooming Tutorials & Guides
- **Method**: `GET`
- **Endpoint**: `/api/grooming/guides`
- **Auth**: None (Public)

---

## 7. Health & Diagnostics Endpoints

### 7.1 Symptom Checklist Options
- **Method**: `GET`
- **Endpoint**: `/api/health/symptoms`
- **Auth**: None (Public)

### 7.2 Run AI Health Scan
- **Method**: `POST`
- **Endpoint**: `/api/health/scan`
- **Auth**: Bearer Token
- **Request Body**:
  ```json
  {
    "scan_type": "symptoms",
    "symptoms": [
      "Itching / Excessive Scratching",
      "Skin Redness or Bumps"
    ]
  }
  ```

### 7.3 Get Condition Guide Detail
- **Method**: `GET`
- **Endpoint**: `/api/health/conditions/{id}`
- **Auth**: None (Public)

### 7.4 Save Health Report
- **Method**: `POST`
- **Endpoint**: `/api/health/reports/save`
- **Auth**: Bearer Token
- **Request Body**:
  ```json
  {
    "report_title": "Condition Report: Mite-related irritation"
  }
  ```

