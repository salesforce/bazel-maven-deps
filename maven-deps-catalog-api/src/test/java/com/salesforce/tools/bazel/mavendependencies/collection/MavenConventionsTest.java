package com.salesforce.tools.bazel.mavendependencies.collection;

import static com.salesforce.tools.bazel.mavendependencies.collection.MavenConventions.getFileGroup;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MavenConventionsTest {

    static void assertFileGroup(String expectedFileGroupd, String groupId) {
        assertEquals(expectedFileGroupd, getFileGroup(groupId));
    }

    @Test
    public void testGetFileGroup() {
        assertFileGroup("commons", "commons");

        assertFileGroup("commons_lang", "commons.lang");
        assertFileGroup("commons_lang", "commons.lang.third");
        assertFileGroup("commons_lang", "commons.lang.something.else");

        assertFileGroup("com_salesforce", "com.salesforce");

        assertFileGroup("com_salesforce_a", "com.salesforce.a");
        assertFileGroup("com_salesforce_a", "com.salesforce.a.b");
        assertFileGroup("com_salesforce_a", "com.salesforce.a.b.c");

        assertFileGroup("com_salesforce_b", "com.salesforce.b");
        assertFileGroup("com_salesforce_b", "com.salesforce.b.c");
        assertFileGroup("com_salesforce_b", "com.salesforce.b.c.d");
    }

}
