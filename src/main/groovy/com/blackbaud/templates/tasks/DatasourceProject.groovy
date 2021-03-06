package com.blackbaud.templates.tasks

class DatasourceProject {

    private RestProject restProject
    private BasicProject basicProject

    DatasourceProject(RestProject restProject) {
        this.restProject = restProject
        this.basicProject = restProject.basicProject
    }

    String getServiceName() {
        restProject.serviceName
    }

    String getServiceId() {
        restProject.serviceId
    }

    String getServicePackage() {
        restProject.servicePackage
    }

    void initMybatis() {
        basicProject.applyPlugin("mybatis")

        basicProject.applyTemplate("src/main/resources") {
            "mybatis-generator-config.xml" template: "/templates/mybatis/mybatis-generator-config.xml.tmpl",
                    serviceId: serviceId, serviceName: serviceName.toLowerCase()
        }
    }

    void initPostgres() {
        basicProject.addDockerPlugin()
        basicProject.applyPlugin("postgres")

        applyEntityScan()
        applyPostgresCompileDependencies()
        applyApplicationProperties()
        applyTestCleanupSql()
        basicProject.appendServiceToAppDescriptor("postgres-shared")
    }

    private void applyTestCleanupSql() {
        basicProject.getProjectFile("src/componentTest/resources/db/test_clean.sql") << ""
        File componentTestFile = basicProject.findFile("ComponentTest.java")
        if (componentTestFile.exists()) {
            FileUtils.appendAfterLine(componentTestFile, /import.*WebAppConfiguration/,
                                      "import org.springframework.test.context.jdbc.Sql;"
            )
            FileUtils.appendAfterLine(componentTestFile, /.*@WebAppConfiguration/,
                                      "@Sql(scripts = \"classpath:/db/test_cleanup.sql\", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)"
            )
        }
    }

    private void applyApplicationProperties() {
        File componentTestPropertiesFile = basicProject.getProjectFile("src/componentTest/resources/application-componentTest.properties")
        componentTestPropertiesFile.append("""
spring.datasource.url=jdbc:postgresql://docker.localhost:5432/\${spring.application.name}-test
""")

        File applicationPropertiesFile = basicProject.getProjectFile("src/main/resources/application-local.properties")
        applicationPropertiesFile.append("""
spring.datasource.url=jdbc:postgresql://docker.localhost:5432/\${spring.application.name}
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.test-on-borrow=true
spring.datasource.validation-interval=30000
spring.datasource.validation-query=SELECT 1;
""")
    }

    private applyPostgresCompileDependencies() {
        FileUtils.appendAfterLine(basicProject.getProjectFile("build.gradle"), '$dependencies \\{.*',
                                  """\
    compile "com.blackbaud:common-spring-boot-persistence:\${springBootVersion}-2.+"
    compile "postgresql:postgresql:9.0-801.jdbc4"
    compile "org.liquibase:liquibase-core\""""
        )
    }

    private void applyEntityScan() {
        String entityScan = "@EntityScan({\"${servicePackage}\", \"com.blackbaud.boot.converters\"})"
        String entityScanImport = "import org.springframework.boot.autoconfigure.domain.EntityScan;"
        File applicationClassFile = basicProject.findFile("${serviceName}.java")

        FileUtils.appendAfterLine(applicationClassFile, "@SpringBootApplication", entityScan)
        FileUtils.appendAfterLine(applicationClassFile, "ManagementWebSecurityAutoConfiguration;", entityScanImport)
    }

    private createLiquibaseChangeLog() {
        getLiquibaseChangeLog().parentFile.mkdirs()
        getLiquibaseChangeLog().text = """databaseChangeLog:
"""
    }

    private File getLiquibaseChangeLog() {
        basicProject.getProjectFile("src/main/resources/db/changelog/db.changelog-master.yaml")
    }

    void addCreateTableScript(String tableName) {
        basicProject.applyTemplate("src/main/resources/db") {
            "create_${tableName}.sql" template: "/templates/liquibase/create-table.sql.tmpl",
                    tableName: tableName
        }

        File liquibaseChangelog = getLiquibaseChangeLog()
        if (liquibaseChangelog.exists() == false) {
            createLiquibaseChangeLog()
        }
        liquibaseChangelog.append("""
  - include:
      file: db/create_${tableName}.sql
""")
    }

}
