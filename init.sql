-- Drop tables in reverse order of dependencies
DROP TABLE IF EXISTS student_lessons CASCADE;
DROP TABLE IF EXISTS lessons CASCADE;
DROP TABLE IF EXISTS student_courses CASCADE;
DROP TABLE IF EXISTS courses CASCADE;
DROP TABLE IF EXISTS redemption_orders CASCADE;
DROP TABLE IF EXISTS rewards CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS bonus_transactions CASCADE;
DROP TABLE IF EXISTS student_achievements CASCADE;
DROP TABLE IF EXISTS achievements CASCADE;
DROP TABLE IF EXISTS students CASCADE;

-- Create Students table first as it is referenced by many other tables
CREATE TABLE students (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255),
    displayName VARCHAR(255),
    bonusBalance INT,
    createdAt TIMESTAMP
);

-- Create Achievements table
CREATE TABLE achievements (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    bonusPoints INT,
    icon VARCHAR(255),
    criteria TEXT,
    category VARCHAR(255)
);

-- Create Student_Achievements junction table
CREATE TABLE student_achievements (
    id VARCHAR(255) PRIMARY KEY,
    achievementId VARCHAR(255),
    status VARCHAR(255),
    earnedAt TIMESTAMP,
    rejectionReason TEXT,
    studentId VARCHAR(255),
    FOREIGN KEY (achievementId) REFERENCES achievements(id),
    FOREIGN KEY (studentId) REFERENCES students(id)
);

-- Create Bonus_Transactions table
CREATE TABLE bonus_transactions (
    id VARCHAR(255) PRIMARY KEY,
    amount INT,
    type VARCHAR(255),
    referenceId VARCHAR(255),
    createdAt TIMESTAMP,
    sourceType VARCHAR(255),
    studentId VARCHAR(255),
    FOREIGN KEY (studentId) REFERENCES students(id)
);

-- Create Notifications table
CREATE TABLE notifications (
    id VARCHAR(255) PRIMARY KEY,
    studentId VARCHAR(255),
    type VARCHAR(255),
    message TEXT,
    isRead BOOLEAN,
    createdAt TIMESTAMP,
    FOREIGN KEY (studentId) REFERENCES students(id)
);

-- Create Rewards table
CREATE TABLE rewards (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    pointsCost INT,
    imageUrl VARCHAR(255),
    isAvailable BOOLEAN
);

-- Create Redemption_Orders table
CREATE TABLE redemption_orders (
    id VARCHAR(255) PRIMARY KEY,
    studentId VARCHAR(255),
    rewardId VARCHAR(255),
    status VARCHAR(255),
    pointsSpent INT,
    createdAt TIMESTAMP,
    fulfilledAt TIMESTAMP,
    FOREIGN KEY (studentId) REFERENCES students(id),
    FOREIGN KEY (rewardId) REFERENCES rewards(id)
);

-- Create Courses table
CREATE TABLE courses (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    totalLessons INT,
    completionPoints INT,
    createdAt TIMESTAMP
);

-- Create Student_Courses table
CREATE TABLE student_courses (
    id VARCHAR(255) PRIMARY KEY,
    studentId VARCHAR(255),
    progress INT,
    status VARCHAR(255),
    completedAt TIMESTAMP,
    courseId VARCHAR(255),
    FOREIGN KEY (studentId) REFERENCES students(id),
    FOREIGN KEY (courseId) REFERENCES courses(id)
);

-- Create Lessons table
CREATE TABLE lessons (
    id VARCHAR(255) PRIMARY KEY,
    courseId VARCHAR(255),
    name VARCHAR(255),
    orderIndex INT,
    duration INT,
    FOREIGN KEY (courseId) REFERENCES courses(id)
);

-- Create Student_Lessons table
CREATE TABLE student_lessons (
    id VARCHAR(255) PRIMARY KEY,
    studentId VARCHAR(255),
    lessonId VARCHAR(255),
    status VARCHAR(255),
    completedAt TIMESTAMP,
    FOREIGN KEY (studentId) REFERENCES students(id),
    FOREIGN KEY (lessonId) REFERENCES lessons(id)
);
