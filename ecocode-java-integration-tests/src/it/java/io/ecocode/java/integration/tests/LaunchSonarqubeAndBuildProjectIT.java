package io.ecocode.java.integration.tests;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Scanner;

import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.junit5.OrchestratorExtension;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.locator.URLLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sonar.java.test.classpath.TestClasspathUtils;

import static fr.greencodeinitiative.java.JavaEcoCodeProfile.PROFILE_PATH;
import static fr.greencodeinitiative.java.JavaRulesDefinition.LANGUAGE;
import static java.lang.System.Logger.Level.INFO;
import static org.assertj.core.api.Assertions.assertThat;

class LaunchSonarqubeAndBuildProjectIT {
	private static final System.Logger LOGGER = System.getLogger(LaunchSonarqubeAndBuildProjectIT.class.getName());

	private static final String PROJECT_KEY = "ecocode-java-checks-test-sources";
	private static final String PROJECT_NAME = "ecoCode Java Checks Test Sources";

	private static final ProfileBackup PROFILE_BACKUP = new ProfileBackup(() -> ClassLoader.getSystemResourceAsStream(PROFILE_PATH));
	private static OrchestratorExtension ORCHESTRATOR;

	@BeforeAll
	static void setup() {
		MavenLocation javaPlugin = MavenLocation.of(
				"org.sonarsource.java",
				"sonar-java-plugin",
				getMavenProperty("sonarjava.version")
		);

		URLLocation profilePath = URLLocation.create(PROFILE_BACKUP.profileDataUri());

		ORCHESTRATOR = OrchestratorExtension
				.builderEnv()
				.useDefaultAdminCredentialsForBuilds(true)
				.setOrchestratorProperty("orchestrator.artifactory.url", getMavenProperty("orchestrator.artifactory.url"))
				.setSonarVersion(getMavenProperty("sonarqube.version"))
				.setServerProperty("sonar.forceAuthentication", "false")
				.addPlugin(FileLocation.of(getEcoCodePluginPath().toFile()))
				.addPlugin(javaPlugin)
				.restoreProfileAtStartup(profilePath)
				.setServerProperty("sonar.web.javaOpts", "-Xmx1G")
				.build();
		ORCHESTRATOR.start();
		LOGGER.log(INFO, () -> MessageFormat.format("SonarQube server available on: {0}", ORCHESTRATOR.getServer().getUrl()));
	}

	@AfterAll
	static void tearDown() {
		if ("true".equalsIgnoreCase(System.getProperty("sonar.keepRunning"))) {
			try (Scanner in = new Scanner(System.in)) {
				LOGGER.log(INFO, () ->
						MessageFormat.format(
								"\n" +
										"\n====================================================================================================" +
										"\nSonarQube available at: {0} (to login: admin/admin)" +
										"\n====================================================================================================" +
										"\n",
								ORCHESTRATOR.getServer().getUrl()
						)
				);
				do {
					LOGGER.log(INFO, "✍ Please press ENTER to stop");
				}
				while (!in.nextLine().isEmpty());
			}
		}
		if (ORCHESTRATOR != null) {
			ORCHESTRATOR.stop();
		}
	}

	/**
	 * Path of the plugin project containing the implementation of the rule we are working on
	 */
	private static Path getEcoCodePluginPath() {
		Path ecoCodePluginPath = TestClasspathUtils.findModuleJarPath("../ecocode-" + LANGUAGE + "-plugin").toAbsolutePath();
		assertThat(ecoCodePluginPath).isRegularFile();
		assertThat(ecoCodePluginPath).hasExtension("jar");
		return ecoCodePluginPath;
	}

	/**
	 * Path of the "test project"
	 */
	private static Path getTestProjectDir() {
		Path testProjectDir = Path.of(".").resolve("../ecocode-" + LANGUAGE + "-test-project").toAbsolutePath();
		assertThat(testProjectDir).isDirectory();
		return testProjectDir;
	}

	/**
	 * Path to the project `pom.xml` file defining the necessary properties.
	 */
	private static Path getProjectPom() {
		Path pom = Path.of(".").resolve("../pom.xml").toAbsolutePath();
		assertThat(pom).isRegularFile();
		return pom;
	}

	@Test
	void myTest() throws IOException {
		// run SonarQube Scanner
		ORCHESTRATOR.getServer().provisionProject(PROJECT_KEY, PROJECT_NAME);
		ORCHESTRATOR.getServer().associateProjectToQualityProfile(PROJECT_KEY, PROFILE_BACKUP.language(), PROFILE_BACKUP.name());

		MavenBuild build = MavenBuild.create(getTestProjectDir().resolve("pom.xml").toFile())
		                             .setCleanPackageSonarGoals()
//		                             .setDebugLogs(true)
                                     .setProperty("sonar.projectKey", PROJECT_KEY)
                                     .setProperty("sonar.projectName", PROJECT_NAME)
                                     .setProperty("sonar.scm.disabled", "true");

		ORCHESTRATOR.executeBuild(build);
	}

	private static String getMavenProperty(String propertyName) {
		MavenXpp3Reader mavenReader = new MavenXpp3Reader();
		try (Reader fileReader = Files.newBufferedReader(getProjectPom())) {
			Model model = mavenReader.read(fileReader);
			return Objects.requireNonNull(
					model.getProperties().getProperty(propertyName),
					() -> "Property `" + propertyName + "` must be defined in " + getProjectPom().toAbsolutePath()
			);
		} catch (XmlPullParserException | IOException e) {
			throw new RuntimeException(e);
		}
	}
}