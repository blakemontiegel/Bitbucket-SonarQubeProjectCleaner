package sonarqube.repo.delete;

import com.atlassian.bitbucket.hook.repository.*;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.repository.Repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

/**
 * SonarQube Repo Cleaner plugin for Bitbucket.
 * 
 * This plugin deletes SonarQube projects when a pull request is merged into the 'master' branch.
 */
public class SonarqubeRepoCleaner implements PostRepositoryHook<PullRequestMergeHookRequest>
{
    private static final Logger log = LoggerFactory.getLogger(SonarqubeRepoCleaner.class);

    private String bitbucketToken;
    private String sonarqubeToken;
    private String bitbucketDomain;
    private String sonarqubeDomain;
    private Set<String> protectedBranches;

    /**
     * Constructor that loads the configuration settings.
     */
    public SonarqubeRepoCleaner() {
        loadConfig();
    }

    /**
     * Handles post-update events for repository hooks.
     * 
     * @param context The context of the repository hook.
     * @param hookRequest The request containing pull request and repository details.
     */
    @Override
    public void postUpdate(PostRepositoryHookContext context, PullRequestMergeHookRequest hookRequest) {
        Repository repository = hookRequest.getRepository();
        PullRequest pullRequest = hookRequest.getPullRequest();
        
        String projectName = repository.getProject().getName();
        String repoName = repository.getName();

        // Check if target branch is "master"
        String targetBranch = pullRequest.getToRef().getDisplayId().toLowerCase();

        // Check if branch is in protectBranches which can be configured in config.properties
        if (protectedBranches.contains(targetBranch)) {
            // Construct the Bitbucket endpoint to retrieve the POM file
            String projectId = repository.getProject().getKey();
            String repoId = repository.getSlug();
            String bitbucketEndpoint = bitbucketDomain + "/rest/api/1.0/projects/" + projectId + "/repos/" + repoId + "/browse/pom.xml";

            try {
                String sonarqubeURL = sonarqubeDomain + "/api/projects/delete?project=";

                // Fetch the POM file content from Bitbucket
                String pomContent = fetchPomXml(bitbucketEndpoint, bitbucketToken);

                // Build the SonarQube project title from POM content and pull request ID
                String sourceBranch = pullRequest.getFromRef().getDisplayId().toLowerCase();
                String sonarQubeProjectTitle = buildSonarQubeProjectTitle(pomContent, sourceBranch);

                // Construct the endpoint for SonarQube project deletion
                String sonarqubeEndpoint = sonarqubeURL + sonarQubeProjectTitle;

                // Check if the source branch is not protected before deletion
                if(!protectedBranches.contains(sourceBranch)) {
                    // Delete the project in SonarQube
                    log.info("Attempting to delete branch '{}' for the repo '{}/{}' from Sonarqube.", sourceBranch, projectName, repoName);
                    deleteProjectInSonarqube(sonarqubeEndpoint, sonarqubeToken);
                } else {
                    log.info("Source branch '{}' for the repo '{}/{}' is protected. No deletion will be performed.", sourceBranch, projectName, repoName);
                }
            } catch (Exception e) {
                log.error("Error occurred during postUpdate processing: ", e);
            }
        } else {
            log.info("Pull request target branch '{}' for the repo '{}/{}' is not protected. No action taken.", targetBranch, projectName, repoName);
        }
    }

     /**
     * Loads configuration properties from the config.properties file.
     */
    private void loadConfig() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                log.error("Unable to find config.properties");
                return;
            }
            properties.load(input);
            bitbucketToken = properties.getProperty("bitbucket.token");
            sonarqubeToken = properties.getProperty("sonarqube.token");
            bitbucketDomain = properties.getProperty("bitbucket.domain");
            sonarqubeDomain = properties.getProperty("sonarqube.domain");

            String branches = properties.getProperty("protectedBranches", "");
            protectedBranches = new HashSet<>(Arrays.asList(branches.toLowerCase().split(",")));
        } catch (Exception e) {
            log.error("An error occurred while loading configuration: ", e);
        }
    }

    /**
     * Fetches the content of the POM file from Bitbucket.
     * 
     * @param bitbucketEndpoint The Bitbucket endpoint for fetching the POM file.
     * @param bitbucketToken The authentication token for Bitbucket.
     * @return The content of the POM file as a string.
     */
    private String fetchPomXml(String bitbucketEndpoint, String bitbucketToken){
        HttpClient client = HttpClient.newHttpClient();
        try {
            URI uri = new URI(bitbucketEndpoint);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + bitbucketToken)
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Check response code
            int responseCode = response.statusCode();
            String responseBody = response.body();

            if (response.statusCode() == HttpURLConnection.HTTP_OK) { // 200 OK
                return response.body();
            } else {
                log.error("Failed to fetch pom.xml. HTTP response code: " + responseCode + "HTTP response body: " + responseBody);
                return "";
            }
        }catch (Exception e) {
            log.error("An error occurred while trying to fetch pom file: ", e);
            return "";
        }
    }

    /**
     * Builds the SonarQube project title based on the POM file content and pull request display ID.
     * 
     * @param pomText The content of the POM file.
     * @param pullRequestDisplayId The display ID of the pull request.
     * @return The formatted SonarQube project title.
     */
    private String buildSonarQubeProjectTitle(String pomText, String pullRequestDisplayId) {
        String groupIdPattern = "<groupId>(.*?)<\\/groupId>";
        String artifactIdPattern = "<artifactId>(.*?)<\\/artifactId>";

        // Create Pattern and Matcher for groupId
        Pattern groupIdRegex = Pattern.compile(groupIdPattern);
        Matcher groupIdMatcher = groupIdRegex.matcher(pomText);
        String groupId = "";
        if (groupIdMatcher.find()) {
            groupId = groupIdMatcher.group(1);
        }

        // Create Pattern and Matcher for artifactId
        Pattern artifactIdRegex = Pattern.compile(artifactIdPattern);
        Matcher artifactIdMatcher = artifactIdRegex.matcher(pomText);
        String artifactId = "";
        if (artifactIdMatcher.find()) {
            artifactId = artifactIdMatcher.group(1);
        }

        // Format pull request displayId by replacing / and spaces with -
        String formattedDisplayId = pullRequestDisplayId.replaceAll("[/\\s]", "-");

        return groupId + ":" + artifactId + ":" + formattedDisplayId;
    }

    /**
     * Deletes the project in SonarQube using the provided endpoint.
     * 
     * @param sonarqubeEndpoint The endpoint for the SonarQube project deletion API.
     * @param sonarqubeToken The authentication token for SonarQube.
     */
    private static void deleteProjectInSonarqube(String sonarqubeEndpoint, String sonarqubeToken) {
        HttpClient client = HttpClient.newHttpClient();
        try {

            URI uri = new URI(sonarqubeEndpoint);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + sonarqubeToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());    
    
            // Get response code
            int responseCode = response.statusCode();
            String responseBody = response.body();

            // Log successful project deletion (check for the appropriate success code for your API)
            if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) { // 204 No Content
                log.info("Project deleted successfully.");
            } else {
                log.error("Failed to delete project. HTTP response code: " + responseCode + " HTTP Response: " + responseBody);
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to delete the project: ", e);
        }
    }    
}