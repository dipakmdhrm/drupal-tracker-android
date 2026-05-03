package com.drupaltracker.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the Drupal status/priority/category code → label helpers.
 * These are displayed directly to users; wrong mappings are silent UI bugs.
 */
class IssueModelTest {

    // --- toStatusLabel ---

    @Test
    fun statusActive() = assertEquals("Active", "1".toStatusLabel())

    @Test
    fun statusFixed() = assertEquals("Fixed", "2".toStatusLabel())

    @Test
    fun statusNeedsReview() = assertEquals("Needs review", "8".toStatusLabel())

    @Test
    fun statusNeedsWork() = assertEquals("Needs work", "13".toStatusLabel())

    @Test
    fun statusRtbc() = assertEquals("RTBC", "14".toStatusLabel())

    @Test
    fun statusPostponedNeedsInfo() = assertEquals("Postponed (needs info)", "16".toStatusLabel())

    @Test
    fun statusUnknownCodeFallsBack() = assertEquals("Unknown", "999".toStatusLabel())

    @Test
    fun statusEmptyStringFallsBack() = assertEquals("Unknown", "".toStatusLabel())

    // --- toPriorityLabel ---

    @Test
    fun priorityCritical() = assertEquals("Critical", "400".toPriorityLabel())

    @Test
    fun priorityMajor() = assertEquals("Major", "300".toPriorityLabel())

    @Test
    fun priorityNormal() = assertEquals("Normal", "200".toPriorityLabel())

    @Test
    fun priorityMinor() = assertEquals("Minor", "100".toPriorityLabel())

    @Test
    fun priorityUnknownCodeFallsBack() = assertEquals("Unknown", "0".toPriorityLabel())

    // --- toCategoryLabel ---

    @Test
    fun categoryBugReport() = assertEquals("Bug report", "1".toCategoryLabel())

    @Test
    fun categoryTask() = assertEquals("Task", "2".toCategoryLabel())

    @Test
    fun categoryFeatureRequest() = assertEquals("Feature request", "3".toCategoryLabel())

    @Test
    fun categorySupportRequest() = assertEquals("Support request", "4".toCategoryLabel())

    @Test
    fun categoryPlan() = assertEquals("Plan", "5".toCategoryLabel())

    @Test
    fun categoryUnknownCodeFallsBack() = assertEquals("Other", "99".toCategoryLabel())
}
