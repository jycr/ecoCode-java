package io.ecocode.java.integration.tests;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.junit5.OrchestratorExtension;
import com.sonar.orchestrator.junit5.OrchestratorExtensionBuilder;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.locator.URLLocation;
import io.ecocode.java.integration.tests.profile.ProfileBackup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static java.lang.System.Logger.Level.INFO;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

class LaunchSonarqubeAndBuildProjectIT {
	private static final System.Logger LOGGER = System.getLogger(LaunchSonarqubeAndBuildProjectIT.class.getName());

	private static final String PROJECT_KEY = "ecocode-test-project";
	private static final String PROJECT_NAME = "ecoCode test project";

	private static OrchestratorExtension orchestrator;

	private static void launchSonarqube() {
		String orchestratorArtifactoryUrl = getProperty("test-it.orchestrator.artifactory.url");
		String sonarqubeVersion = getProperty("test-it.sonarqube.version");

		OrchestratorExtensionBuilder orchestratorExtensionBuilder = OrchestratorExtension
				.builderEnv()
				.useDefaultAdminCredentialsForBuilds(true)
				.setOrchestratorProperty("orchestrator.artifactory.url", orchestratorArtifactoryUrl)
				.setSonarVersion(sonarqubeVersion)
				.setServerProperty("sonar.forceAuthentication", "false")
				.setServerProperty("sonar.web.javaOpts", "-Xmx1G");

		additionalPluginsToInstall().forEach(orchestratorExtensionBuilder::addPlugin);
		additionalProfiles().forEach(orchestratorExtensionBuilder::restoreProfileAtStartup);

		orchestrator = orchestratorExtensionBuilder.build();
		orchestrator.start();
		LOGGER.log(INFO, () -> MessageFormat.format("SonarQube server available on: {0}", orchestrator.getServer().getUrl()));
	}

	private static void buildAndAnalyzeTestProfile() {
		// run SonarQube Scanner
		Server server = orchestrator.getServer();

		server.provisionProject(PROJECT_KEY, PROJECT_NAME);
		testProjectProfileByLanguage().forEach((language, profileName) -> {
			server.associateProjectToQualityProfile(PROJECT_KEY, language, profileName);
		});

		MavenBuild build = MavenBuild.create(getTestProjectDir().resolve("pom.xml").toFile())
		                             .setCleanPackageSonarGoals()
		                             .setProperty("sonar.projectKey", PROJECT_KEY)
		                             .setProperty("sonar.projectName", PROJECT_NAME)
		                             .setProperty("sonar.scm.disabled", "true");

		orchestrator.executeBuild(build);
	}

	@BeforeAll
	static void setup() {
		launchSonarqube();
		buildAndAnalyzeTestProfile();
	}

	@Test
	void test() {
		System.out.println("TEST");
	}

	@AfterAll
	static void tearDown() {
		if ("true".equalsIgnoreCase(System.getProperty("test-it.sonar.keepRunning"))) {
			try (Scanner in = new Scanner(System.in)) {
				LOGGER.log(INFO, () ->
						MessageFormat.format(
								"""

										====================================================================================================
										SonarQube available at: {0} (to login: admin/admin)
										====================================================================================================

										""",
								orchestrator.getServer().getUrl()
						)
				);
				do {
					LOGGER.log(INFO, "✍ Please press CTRL+C to stop");
				}
				while (!in.nextLine().isEmpty());
			}
		}
		if (orchestrator != null) {
			orchestrator.stop();
		}
	}

	/**
	 * Path of the "test project"
	 */
	private static Path getTestProjectDir() {
		Path testProjectDir = Path.of(URI.create(getProperty("test-it.test-project")));
		assertThat(testProjectDir).isDirectory();
		return testProjectDir;
	}

	private static Stream<String> splitAndTrimNullableString(String nullableString, String regexSeparator) {
		return Optional.ofNullable(nullableString)
		               .map(str -> str.split(regexSeparator))
		               .stream()
		               .flatMap(Arrays::stream)
		               .map(String::trim)
		               .filter(not(String::isEmpty));
	}

	private static Stream<String> systemPropertyCommaSeparatedValues(String propertyName) {
		return splitAndTrimNullableString(System.getProperty(propertyName), "\\s*,\\s*");
	}

	private static Set<Location> additionalPluginsToInstall() {
		return systemPropertyCommaSeparatedValues("test-it.plugins")
				.map(LaunchSonarqubeAndBuildProjectIT::toPluginLocation)
				.collect(Collectors.toSet());
	}

	private static Set<URLLocation> additionalProfiles() {
		return systemPropertyCommaSeparatedValues("test-it.additional-profiles")
				.map(URI::create)
				.map(ProfileBackup::new)
				.map(ProfileBackup::profileDataUri)
				.map(URLLocation::create)
				.collect(Collectors.toSet());
	}

	private static Map<String, String> testProjectProfileByLanguage() {
		return systemPropertyCommaSeparatedValues("test-it.test-project-profile-by-language")
				.map(languageAndProfileStr -> splitAndTrimNullableString(languageAndProfileStr, "\\s*:\\s*")
						.toList())
				.filter(languageAndProfile -> languageAndProfile.size() == 2)
				.collect(toMap(
						// Language
						languageAndProfile -> languageAndProfile.get(0),
						// Profile name
						languageAndProfile -> languageAndProfile.get(1)
				));
	}

	private static Location toPluginLocation(String location) {
		if (location.startsWith("file://")) {
			try {
				return FileLocation.of(URI.create(location).toURL());
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException(e);
			}
		}
		String[] pluginGAVvalues = location.split(":");
		if (pluginGAVvalues.length != 3) {
			throw new IllegalArgumentException("Invalid plugin GAV definition (`groupId:artifactId:version`): " + location);
		}
		return MavenLocation.of(
				// groupId
				pluginGAVvalues[0],
				// artifactId
				pluginGAVvalues[1],
				// version
				pluginGAVvalues[2]
		);
	}

	private static String getProperty(String propertyName) {
		return ofNullable(System.getProperty(propertyName)).orElseThrow(() -> new IllegalStateException("System property `" + propertyName + "` must be defined"));
	}
}