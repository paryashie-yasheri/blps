-- liquibase formatted sql

-- changeset huji:1772571554446-1 splitStatements:false
CREATE TABLE "achievements" ("id" VARCHAR(255) NOT NULL, "name" VARCHAR(255), "description" TEXT, "bonuspoints" INTEGER, "icon" VARCHAR(255), "criteria" TEXT, "category" VARCHAR(255), CONSTRAINT "achievements_pkey" PRIMARY KEY ("id"));

-- changeset huji:1772571554446-2 splitStatements:false
CREATE TABLE "bonus_transactions" ("id" VARCHAR(255) NOT NULL, "amount" INTEGER, "type" VARCHAR(255), "referenceid" VARCHAR(255), "createdat" TIMESTAMP WITHOUT TIME ZONE, "sourcetype" VARCHAR(255), "studentid" VARCHAR(255), CONSTRAINT "bonus_transactions_pkey" PRIMARY KEY ("id"));

-- changeset huji:1772571554446-3 splitStatements:false
CREATE TABLE "courses" ("id" VARCHAR(255) NOT NULL, "name" VARCHAR(255), "description" TEXT, "totallessons" INTEGER, "completionpoints" INTEGER, "createdat" TIMESTAMP WITHOUT TIME ZONE, CONSTRAINT "courses_pkey" PRIMARY KEY ("id"));

-- changeset huji:1772571554446-4 splitStatements:false
CREATE TABLE "lessons" ("id" VARCHAR(255) NOT NULL, "courseid" VARCHAR(255), "name" VARCHAR(255), "orderindex" INTEGER, "duration" INTEGER, CONSTRAINT "lessons_pkey" PRIMARY KEY ("id"));

-- changeset huji:1772571554446-5 splitStatements:false
CREATE TABLE "notifications" ("id" VARCHAR(255) NOT NULL, "studentid" VARCHAR(255), "type" VARCHAR(255), "message" TEXT, "isread" BOOLEAN, "createdat" TIMESTAMP WITHOUT TIME ZONE, CONSTRAINT "notifications_pkey" PRIMARY KEY ("id"));

-- changeset huji:1772571554446-6 splitStatements:false
CREATE TABLE "redemption_orders" ("id" VARCHAR(255) NOT NULL, "studentid" VARCHAR(255), "rewardid" VARCHAR(255), "status" VARCHAR(255), "pointsspent" INTEGER, "createdat" TIMESTAMP WITHOUT TIME ZONE, "fulfilledat" TIMESTAMP WITHOUT TIME ZONE, CONSTRAINT "redemption_orders_pkey" PRIMARY KEY ("id"));

-- changeset huji:1772571554446-7 splitStatements:false
CREATE TABLE "rewards" ("id" VARCHAR(255) NOT NULL, "name" VARCHAR(255), "description" TEXT, "pointscost" INTEGER, "imageurl" VARCHAR(255), "isavailable" BOOLEAN, CONSTRAINT "rewards_pkey" PRIMARY KEY ("id"));

-- changeset huji:1772571554446-8 splitStatements:false
CREATE TABLE "student_achievements" ("id" VARCHAR(255) NOT NULL, "achievementid" VARCHAR(255), "status" VARCHAR(255), "earnedat" TIMESTAMP WITHOUT TIME ZONE, "rejectionreason" TEXT, "studentid" VARCHAR(255), CONSTRAINT "student_achievements_pkey" PRIMARY KEY ("id"));

-- changeset huji:1772571554446-9 splitStatements:false
CREATE TABLE "student_courses" ("id" VARCHAR(255) NOT NULL, "studentid" VARCHAR(255), "progress" INTEGER, "status" VARCHAR(255), "completedat" TIMESTAMP WITHOUT TIME ZONE, "courseid" VARCHAR(255), CONSTRAINT "student_courses_pkey" PRIMARY KEY ("id"));

-- changeset huji:1772571554446-10 splitStatements:false
CREATE TABLE "student_lessons" ("id" VARCHAR(255) NOT NULL, "studentid" VARCHAR(255), "lessonid" VARCHAR(255), "status" VARCHAR(255), "completedat" TIMESTAMP WITHOUT TIME ZONE, CONSTRAINT "student_lessons_pkey" PRIMARY KEY ("id"));

-- changeset huji:1772571554446-11 splitStatements:false
CREATE TABLE "students" ("id" VARCHAR(255) NOT NULL, "email" VARCHAR(255), "displayname" VARCHAR(255), "bonusbalance" INTEGER, "createdat" TIMESTAMP WITHOUT TIME ZONE, CONSTRAINT "students_pkey" PRIMARY KEY ("id"));

-- changeset huji:1772571554446-12 splitStatements:false
ALTER TABLE "bonus_transactions" ADD CONSTRAINT "bonus_transactions_studentid_fkey" FOREIGN KEY ("studentid") REFERENCES "students" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset huji:1772571554446-13 splitStatements:false
ALTER TABLE "lessons" ADD CONSTRAINT "lessons_courseid_fkey" FOREIGN KEY ("courseid") REFERENCES "courses" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset huji:1772571554446-14 splitStatements:false
ALTER TABLE "notifications" ADD CONSTRAINT "notifications_studentid_fkey" FOREIGN KEY ("studentid") REFERENCES "students" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset huji:1772571554446-15 splitStatements:false
ALTER TABLE "redemption_orders" ADD CONSTRAINT "redemption_orders_rewardid_fkey" FOREIGN KEY ("rewardid") REFERENCES "rewards" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset huji:1772571554446-16 splitStatements:false
ALTER TABLE "redemption_orders" ADD CONSTRAINT "redemption_orders_studentid_fkey" FOREIGN KEY ("studentid") REFERENCES "students" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset huji:1772571554446-17 splitStatements:false
ALTER TABLE "student_achievements" ADD CONSTRAINT "student_achievements_achievementid_fkey" FOREIGN KEY ("achievementid") REFERENCES "achievements" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset huji:1772571554446-18 splitStatements:false
ALTER TABLE "student_achievements" ADD CONSTRAINT "student_achievements_studentid_fkey" FOREIGN KEY ("studentid") REFERENCES "students" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset huji:1772571554446-19 splitStatements:false
ALTER TABLE "student_courses" ADD CONSTRAINT "student_courses_courseid_fkey" FOREIGN KEY ("courseid") REFERENCES "courses" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset huji:1772571554446-20 splitStatements:false
ALTER TABLE "student_courses" ADD CONSTRAINT "student_courses_studentid_fkey" FOREIGN KEY ("studentid") REFERENCES "students" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset huji:1772571554446-21 splitStatements:false
ALTER TABLE "student_lessons" ADD CONSTRAINT "student_lessons_lessonid_fkey" FOREIGN KEY ("lessonid") REFERENCES "lessons" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset huji:1772571554446-22 splitStatements:false
ALTER TABLE "student_lessons" ADD CONSTRAINT "student_lessons_studentid_fkey" FOREIGN KEY ("studentid") REFERENCES "students" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

