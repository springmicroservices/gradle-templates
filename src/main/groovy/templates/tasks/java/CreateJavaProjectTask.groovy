/*
 * Copyright (c) 2011,2012 Eric Berry <elberry@tellurianring.com>
 * Copyright (c) 2013 Christopher J. Stehno <chris@stehno.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package templates.tasks.java
import org.gradle.api.tasks.TaskAction
import templates.ProjectTemplate
import templates.TemplatesPlugin

/**
 * Task that creates a new java project in a specified directory.
 */
class CreateJavaProjectTask extends AbstractJavaProjectTask {

    public static final String NEW_PROJECT_NAME = 'newProjectName'
    public static final String PROJECT_GROUP = 'projectGroup'
    public static final String PROJECT_VERSION = 'projectVersion'
    public static final String PROJECT_PARENT_DIR = 'projectParentDir'

    CreateJavaProjectTask(){
        name = 'createJavaProject'
        group = TemplatesPlugin.group
        description = 'Creates a new Gradle Java project in a new directory named after your project.'
    }

    @TaskAction def create(){
        def props = project.properties

        String projectName = props[NEW_PROJECT_NAME] ?: TemplatesPlugin.prompt('Project Name:')

        if (projectName) {
            String projectGroup = props[PROJECT_GROUP] ?: TemplatesPlugin.prompt('Group:', projectName.toLowerCase())
            String projectVersion = props[PROJECT_VERSION] ?: TemplatesPlugin.prompt('Version:', '0.1')

            String projectPath = props[PROJECT_PARENT_DIR] ? "${props[PROJECT_PARENT_DIR]}/$projectName" : projectName

            createBase projectPath

            ProjectTemplate.fromRoot(projectPath) {
                'build.gradle' template: '/templates/java/build.gradle.tmpl', projectGroup:projectGroup
                'gradle.properties' content: "version=$projectVersion", append:true
            }

        } else {
            // TODO: should be an error
            println 'No project name provided.'
        }
    }
}
