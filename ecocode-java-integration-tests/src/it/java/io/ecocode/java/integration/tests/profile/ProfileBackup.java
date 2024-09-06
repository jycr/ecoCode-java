package io.ecocode.java.integration.tests.profile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Manage XML Backup file of profile based on JSON official profile.
 *
 * <p>Example, following JSON profile:</p>
 * <pre>
 * {
 *  "name": "ecoCode way",
 *  "language": "java",
 *  "ruleKeys": [
 * 	    "EC1",
 * 	    "EC2"
 *  ]
 * }
 * </pre>
 * <p>may produce following XML profile:</p>
 * <pre>
 * &lt;?xml version='1.0' encoding='UTF-8'?&gt;
 * &lt;profile&gt;
 * 	&lt;name&gt;ecoCode way&lt;/name&gt;
 * 	&lt;language&gt;java&lt;/language&gt;
 * 	&lt;rules&gt;
 * 		&lt;rule&gt;
 * 			&lt;repositoryKey&gt;ecocode-java&lt;/repositoryKey&gt;
 * 			&lt;key&gt;EC1&lt;/key&gt;
 * 			&lt;type&gt;CODE_SMELL&lt;/type&gt;
 * 			&lt;priority&gt;MINOR&lt;/priority&gt;
 * 			&lt;parameters /&gt;
 * 		&lt;/rule&gt;
 * 		&lt;rule&gt;
 * 			&lt;repositoryKey&gt;ecocode-java&lt;/repositoryKey&gt;
 * 			&lt;key&gt;EC2&lt;/key&gt;
 * 			&lt;type&gt;CODE_SMELL&lt;/type&gt;
 * 			&lt;priority&gt;MINOR&lt;/priority&gt;
 * 			&lt;parameters /&gt;
 * 		&lt;/rule&gt;
 * 	&lt;/rules&gt;
 * &lt;/profile&gt;
 * </pre>
 */
public class ProfileBackup {
	private static final MessageFormat TEMPLATE_PROFIL = new MessageFormat(
			"""
					<?xml version="1.0" encoding="UTF-8"?>
					<profile>
						<name>{0}</name>
						<language>{1}</language>
						<rules>
					{2}
						</rules>
					</profile>
					"""
	);
	private static final MessageFormat TEMPLATE_RULE = new MessageFormat(
			"""
							<rule>
								<repositoryKey>{0}</repositoryKey>
								<key>{1}</key>
								<type>{2}</type>
								<priority>{3}</priority>
								<parameters />
							</rule>
					"""
	);

	private final ObjectMapper mapper;
	private final URI jsonProfile;

	public ProfileBackup(URI jsonProfile) {
		this.mapper = new ObjectMapper();
		// Ignore unknown properties
		this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		this.jsonProfile = jsonProfile;
	}

	private transient ProfileMetadata _profileMetadata;

	private ProfileMetadata profileMetadata() throws IOException {
		if (_profileMetadata == null) {
			try (InputStream profilJsonFile = jsonProfile.toURL().openStream()) {
				_profileMetadata = mapper.readValue(profilJsonFile, ProfileMetadata.class);
			}
		}
		return _profileMetadata;
	}

	private RuleMetadata loadRule(String language, String ruleKey) {
		try (InputStream ruleMetadataJsonFile = ClassLoader.getSystemResourceAsStream("io/ecocode/rules/" + language + "/" + ruleKey + ".json")) {
			RuleMetadata result = mapper.readValue(ruleMetadataJsonFile, RuleMetadata.class);
			result.setKey(ruleKey);
			return result;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String xmlProfile() throws IOException {
		ProfileMetadata profileMetadata = profileMetadata();
		String language = profileMetadata.getLanguage();
		List<RuleMetadata> rules = profileMetadata.getRuleKeys().stream()
		                                          .map(ruleKey -> this.loadRule(language, ruleKey))
		                                          .toList();
		StringBuilder output = new StringBuilder();
		String repositoryKey = "ecocode-" + profileMetadata.getLanguage();
		rules.forEach(rule -> output.append(
				xmlRule(
						repositoryKey,
						rule.getKey(),
						rule.getType(),
						rule.getDefaultSeverity().toUpperCase()
				))
		);
		return TEMPLATE_PROFIL.format(new Object[]{
				profileMetadata.getName(),
				profileMetadata.getLanguage(),
				output.toString()
		});
	}

	private String xmlRule(String repositoryKey, String key, String type, String priority) {
		return TEMPLATE_RULE.format(new Object[]{
				repositoryKey,
				key,
				type,
				priority
		});
	}

	/**
	 * Get the content of XML Profil in datauri format.
	 */
	public URL profileDataUri() {
		try {
			String xmlProfileContent = xmlProfile();
			String xmlProfileBase64encoded = Base64.getEncoder().encodeToString(xmlProfileContent.getBytes());
			return new URL("data:text/xml;base64," + xmlProfileBase64encoded);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String language() throws IOException {
		return profileMetadata().getLanguage();
	}


	public String name() throws IOException {
		return profileMetadata().getName();
	}
}