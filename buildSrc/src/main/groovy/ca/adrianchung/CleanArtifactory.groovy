package ca.adrianchung

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.apache.commons.lang.time.DateUtils

import java.text.SimpleDateFormat

class CleanArtifactory implements Plugin<Project> {
    void apply(Project target) {
        target.task('cleanArtifactory', type: CleanArtifactoryTask)
    }
}

/**
 * Task to delete old artifactory artifacts.
 */
class CleanArtifactoryTask extends DefaultTask {
    def artifactoryRoot = "http://localhost:8081/artifactory"
    def repository = 'libs-snapshot-local'
    def retainDays = 30
    def formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    def dryRun = true

    /**
     * Checks to see if a date is older than numberOfDays. Expected string is in the form 2014-03-21T14:47:10.812Z.
     * @param iso8601Date
     * @param numberOfDays
     * @return
     */
    def isOlderThanDays(iso8601Date, numberOfDays) {
        def date = formatter.parse(iso8601Date)
        def now = new Date()
        return (DateUtils.addDays(date, numberOfDays).getTime() < now.getTime())
    }

    def deleteItem(path) {
        if (!dryRun) {
            def uri = artifactoryRoot + '/' + repository + path
            def http = new HTTPBuilder(artifactoryRoot)
            // Need to preemptively set authorization header
            // http://stackoverflow.com/questions/6588256/using-groovy-http-builder-in-preemptive-mode
            http.setHeaders(['Authorization': 'Basic ' + 'admin:password'.bytes.encodeBase64().toString()])
            http.request(uri, Method.DELETE, 'application/json', {})
        }
    }

    def traverse(path) {
        def http = new HTTPBuilder(artifactoryRoot)
        def uri = artifactoryRoot + '/api/storage/' + repository + path
        http.request(uri, Method.GET, 'application/json', {
            response.success = { resp, json ->
                if (resp.success) {
                    if (json.children.size == 0) {
                        println json.path + ' (delete, no children)'
                        deleteItem(json.path)
                    }
                    else {
                        def folderContainsAllFiles = true
                        for (def child in json.children) {
                            if (child.folder) {
                                traverse(path + child.uri)
                                folderContainsAllFiles = false
                            }
                        }
                        if (folderContainsAllFiles && !json.path.contains('release')) {
                            if (isOlderThanDays(json.lastUpdated, retainDays)) {
                                println json.path + ' (delete, inactivity)'
                                deleteItem(json.path)
                            }
                            else {
                                println json.path + ' (keep)'
                            }
                        }
                        else {
                            println json.path + ' contains both files and folders (keep)'
                        }
                    }
                }
            }
        })
    }

    @TaskAction
    def cleanArtifactory() {
        traverse('')
    }
}