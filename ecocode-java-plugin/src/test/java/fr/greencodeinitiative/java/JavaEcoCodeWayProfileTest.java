/*
 * ecoCode - Java language - Provides rules to reduce the environmental footprint of your Java programs
 * Copyright © 2023 Green Code Initiative (https://www.ecocode.io)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.greencodeinitiative.java;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.check.Rule;

import static fr.greencodeinitiative.java.JavaCheckRegistrarTest.getDefinedRules;
import static fr.greencodeinitiative.java.JavaEcoCodeWayProfile.PROFILE_NAME;
import static fr.greencodeinitiative.java.JavaEcoCodeWayProfile.PROFILE_PATH;
import static fr.greencodeinitiative.java.JavaRulesDefinition.LANGUAGE;
import static org.assertj.core.api.Assertions.assertThat;

class JavaEcoCodeWayProfileTest {
	@Test
	void should_create_ecocode_profile() {
		BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();

		JavaEcoCodeWayProfile definition = new JavaEcoCodeWayProfile();
		definition.define(context);

		BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context.profile(LANGUAGE, PROFILE_NAME);

		assertThat(profile.language()).isEqualTo(LANGUAGE);
		assertThat(profile.name()).isEqualTo(PROFILE_NAME);
		List<String> definedRuleIds = getDefinedRules().stream().map(c -> c.getAnnotation(Rule.class).key()).collect(Collectors.toList());
		assertThat(profile.rules())
				.describedAs("All implemented rules must be declared in '%s' profile file: %s", PROFILE_NAME, PROFILE_PATH)
				.map(BuiltInQualityProfilesDefinition.BuiltInActiveRule::ruleKey)
				.containsExactlyInAnyOrderElementsOf(definedRuleIds);
	}
}
