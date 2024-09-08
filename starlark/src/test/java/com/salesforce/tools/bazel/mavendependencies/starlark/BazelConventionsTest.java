package com.salesforce.tools.bazel.mavendependencies.starlark;

import static com.salesforce.tools.bazel.mavendependencies.starlark.BazelConventions.toTargetName;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class BazelConventionsTest {

	@Test
	public void getShortenedTargetName_test() throws Exception {
		assertEquals("com_google_guava_guava", toTargetName("com.google.guava", "guava", null, null));
		assertEquals("commons_logging_commons_logging", toTargetName("commons-logging", "commons-logging", null, null));
		assertEquals("junit_junit", toTargetName("junit", "junit", null, null));
		assertEquals("org_apache_commons_logging_commons_logging", toTargetName("org.apache.commons.logging", "commons-logging", null, null));
		assertEquals("org_apache_commons_logging_commons_logging_api", toTargetName("org.apache.commons.logging", "commons-logging-api", null, null));
		assertEquals("org_slf4j_slf4j_api", toTargetName("org.slf4j", "slf4j-api", null, null));
		assertEquals("axis_axis_ant", toTargetName("axis", "axis-ant", null, null));

		// the following used to cause conflicts with shorting, hence we went to full
		assertEquals("org_apache_poi_ooxml_schemas", toTargetName("org.apache.poi", "ooxml-schemas", null, null));
		assertEquals("org_apache_poi_poi_ooxml_schemas", toTargetName("org.apache.poi", "poi-ooxml-schemas", null, null));

		// test variations of extension
		assertEquals("com_google_guava_guava", toTargetName("com.google.guava", "guava", null, null));
		assertEquals("com_google_guava_guava", toTargetName("com.google.guava", "guava", "", null));
		assertEquals("com_google_guava_guava", toTargetName("com.google.guava", "guava", "jar", null));
		assertEquals("com_google_guava_guava_pom", toTargetName("com.google.guava", "guava", "pom", null));
		assertEquals("com_google_guava_guava_zip", toTargetName("com.google.guava", "guava", "zip", null));

		// test variations of classifier
		assertEquals("com_google_guava_guava", toTargetName("com.google.guava", "guava", null, null));
		assertEquals("com_google_guava_guava", toTargetName("com.google.guava", "guava", null, ""));
		assertEquals("com_google_guava_guava", toTargetName("com.google.guava", "guava", null, "jar"));
		assertEquals("com_google_guava_guava_sources", toTargetName("com.google.guava", "guava", null, "sources"));

		// test combinations of extension and classifier (and document known clash)
		assertEquals("com_google_guava_guava_war", toTargetName("com.google.guava", "guava", "war", null));
		assertEquals("com_google_guava_guava_war", toTargetName("com.google.guava", "guava", null, "war"));
		assertEquals("com_google_guava_guava_war_war", toTargetName("com.google.guava", "guava", "war", "war"));
	}

	@Test
	public void toTargetName_test() throws Exception {
		assertEquals("my_artifact_id", toTargetName("my-artifact-id"));
		assertEquals("my_artifact_id", toTargetName("my_artifact_id"));
	}

}
