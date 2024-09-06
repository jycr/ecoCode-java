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
package fr.greencodeinitiative.java.checks;

import java.util.Arrays;
import java.util.List;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import static org.sonar.plugins.java.api.semantic.MethodMatchers.CONSTRUCTOR;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@Rule(key = "EC72")
@DeprecatedRuleKey(repositoryKey = "greencodeinitiative-java", ruleKey = "S72")
public class AvoidSQLRequestInLoop extends IssuableSubscriptionVisitor {

    protected static final String MESSAGERULE = "Avoid SQL request in loop";
    private static final String JAVA_SQL_STATEMENT = "java.sql.Statement";
    private static final String JAVA_SQL_CONNECTION = "java.sql.Connection";
    private static final String SPRING_JDBC_OPERATIONS = "org.springframework.jdbc.core.JdbcOperations";

    private static final MethodMatchers SQL_METHOD = MethodMatchers.or(
            MethodMatchers.create().ofSubTypes("org.hibernate.Session").names("createQuery", "createSQLQuery")
                    .withAnyParameters().build(),
            MethodMatchers.create().ofSubTypes(JAVA_SQL_STATEMENT)
                    .names("executeQuery", "execute", "executeUpdate", "executeLargeUpdate") // addBatch is recommended
                    .withAnyParameters().build(),
            MethodMatchers.create().ofSubTypes(JAVA_SQL_CONNECTION)
                    .names("prepareStatement", "prepareCall", "nativeSQL")
                    .withAnyParameters().build(),
            MethodMatchers.create().ofTypes("javax.persistence.EntityManager")
                    .names("createNativeQuery", "createQuery")
                    .withAnyParameters().build(),
            MethodMatchers.create().ofSubTypes(SPRING_JDBC_OPERATIONS)
                    .names("batchUpdate", "execute", "query", "queryForList", "queryForMap", "queryForObject",
                            "queryForRowSet", "queryForInt", "queryForLong", "update")
                    .withAnyParameters().build(),
            MethodMatchers.create().ofTypes("org.springframework.jdbc.core.PreparedStatementCreatorFactory")
                    .names(CONSTRUCTOR, "newPreparedStatementCreator")
                    .withAnyParameters().build(),
            MethodMatchers.create().ofSubTypes("javax.jdo.PersistenceManager").names("newQuery")
                    .withAnyParameters().build(),
            MethodMatchers.create().ofSubTypes("javax.jdo.Query").names("setFilter", "setGrouping")
                    .withAnyParameters().build());

    private final AvoidSQLRequestInLoopVisitor visitorInFile = new AvoidSQLRequestInLoopVisitor();

    @Override
    public List<Kind> nodesToVisit() {
        return Arrays.asList(
                Tree.Kind.FOR_EACH_STATEMENT, Tree.Kind.FOR_STATEMENT,
                Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT);
    }

    @Override
    public void visitNode(Tree tree) {
        tree.accept(visitorInFile);
    }

    private class AvoidSQLRequestInLoopVisitor extends BaseTreeVisitor {

        @Override
        public void visitMethodInvocation(MethodInvocationTree tree) {
            if (SQL_METHOD.matches(tree)) {
                reportIssue(tree, MESSAGERULE);
            } else {
                super.visitMethodInvocation(tree);
            }
        }
    }
}
