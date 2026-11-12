package com.moudonut.admin.util

import java.text.DecimalFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val bangkok = ZoneId.of("Asia/Bangkok")
private val moneyFormat = DecimalFormat("#,##0.##")
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale("th", "TH"))
private val dateTimeFormat = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale("th", "TH"))

fun formatMoney(value: Double): String = "${moneyFormat.format(value)} บาท"

fun shortOrderId(orderId: String): String = orderId.take(8).uppercase(Locale.ROOT)

fun formatTime(iso: String?): String =
    iso?.let {
        runCatching {
            OffsetDateTime.parse(it).atZoneSameInstant(bangkok).format(timeFormat)
        }.getOrDefault("-")
    } ?: "-"

fun formatDateTime(iso: String?): String =
    iso?.let {
        runCatching {
            OffsetDateTime.parse(it).atZoneSameInstant(bangkok).format(dateTimeFormat)
        }.getOrDefault("-")
    } ?: "-"

fun todayBangkok(): String = LocalDate.now(bangkok).toString()

fun shiftDate(date: String, days: Long): String =
    runCatching { LocalDate.parse(date).plusDays(days).toString() }.getOrDefault(date)
