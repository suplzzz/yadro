package com.suplz.yadro.algorithm

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class Employee(val name: String, val currentDays: Int)

fun main() {
    val employees = listOf(
        Employee("Иван Иванов", 993),
        Employee("Пётр Петров", 994),
        Employee("Алексей Сидоров", 995),
        Employee("Дмитрий Смирнов", 999),
        Employee("Сергей Кузнецов", 1000),
        Employee("Никита Попов", 1001),
        Employee("Андрей Васильев", 1993),
        Employee("Михаил Новиков", 1994),
        Employee("Владимир Фёдоров", 1995),
        Employee("Егор Морозов", 1996),
        Employee("Максим Волков", 2000),
        Employee("Роман Алексеев", 2001),
        Employee("Артём Лебедев", 2002),
        Employee("Кирилл Семёнов", 2005),
        Employee("Олег Егоров", 2006),
        Employee("Иван Петров", 2007),
        Employee("Пётр Иванов", 2008)
    )

    val today = LocalDate.now()
    val monday = today.with(DayOfWeek.MONDAY)
    val formatter = DateTimeFormatter.ofPattern("dd.MM")

    val result: Array<Array<String>> = (0L..6L)
        .map { daysToAdd -> monday.plusDays(daysToAdd) }
        .map { targetDate ->
            val offset = ChronoUnit.DAYS.between(today, targetDate).toInt()

            val jubileesText = employees
                .filter { (it.currentDays + offset) > 0 }
                .filter { (it.currentDays + offset) % 1000 == 0 }
                .joinToString("\n") { "${it.name} - ${it.currentDays + offset} дней" }

            arrayOf(targetDate.format(formatter), jubileesText)
        }
        .toTypedArray()

    println("\"Date\",\"Text\"")

    result.forEach { row ->
        val date = row[0]
        val text = row[1]

        val formattedText = listOf(text)
            .filter { it.isNotEmpty() }
            .joinToString(prefix = "\"", postfix = "\"")

        println("\"$date\",$formattedText")
    }
}