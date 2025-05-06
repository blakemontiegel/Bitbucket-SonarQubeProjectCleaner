Bitbucket-SonarQube Project Cleaner Plugin
================================
## Overview
The **Bitbucket-SonarQube** Project Cleaner Plugin for Bitbucket is a tool designed to streamline the management of SonarQube projects by automatically deleting projects when branches are merged into key branches like 'master' or 'OD-Fixes'. This is useful for maintaining a clean and relevant SonarQube instance when using the community edition, which does not support branch recognition.

## Key Features
- Automatic Project Deletion: Deletes SonarQube projects based on branch merges into key branches.
- Branch Protection Configuration: Configurable list of protected branches to control when projects are deleted.
- Integration with Bitbucket: Retrieves repository details and pom.xml content from Bitbucket.
- Error Handling and Logging: Comprehensive logging and error handling for debugging and monitoring.
  
# SonarqubeRepoCleaner.java Overview
This overview provides a summary of each method in the class, helping the reader understand how this plugin functions.
### **public SonarqubeRepoCleaner()**

-   **Description**: Constructor that initializes the plugin by loading configuration settings from `config.properties`.
-   **Details**: Calls the `loadConfig` method to set up necessary tokens, domains, and protected branches.

### **public void postUpdate(PostRepositoryHookContext context, PullRequestMergeHookRequest hookRequest)**

-   **Description**: Handles post-update events triggered by repository hooks.
-   **Parameters**:
    -   `context`: The context of the repository hook.
    -   `hookRequest`: Contains details about the pull request and repository.
-   **Details**:
    -   Retrieves repository and pull request information.
    -   Checks if the target branch of the pull request is in the list of protected branches.
    -   Fetches the POM file from Bitbucket and constructs the SonarQube project title.
    -   Deletes the SonarQube project if the source branch is not protected.

### **private void loadConfig()**

-   **Description**: Loads configuration properties from the `config.properties` file.
-   **Details**: Reads tokens, domains, and protected branches from the configuration file and initializes the respective fields. Logs errors if the file is missing or an issue occurs during loading.

### **private String fetchPomXml(String bitbucketEndpoint, String bitbucketToken)**

-   **Description**: Fetches the content of the POM file from Bitbucket.
-   **Parameters**:
    -   `bitbucketEndpoint`: The URL endpoint to fetch the POM file.
    -   `bitbucketToken`: The authentication token for Bitbucket.
-   **Returns**: The content of the POM file as a string.
-   **Details**: Sends an HTTP GET request to Bitbucket, retrieves the POM file content, and handles possible errors.

### **private String buildSonarQubeProjectTitle(String pomText, String pullRequestDisplayId)**

-   **Description**: Constructs the SonarQube project title based on the POM file content and pull request display ID.
-   **Parameters**:
    -   `pomText`: The content of the POM file.
    -   `pullRequestDisplayId`: The display ID of the pull request.
-   **Returns**: The formatted SonarQube project title.
-   **Details**: Extracts `groupId` and `artifactId` from the POM file using regex. Formats the pull request display ID and returns the combined title.

### private static void deleteProjectInSonarqube(String sonarqubeEndpoint, String sonarqubeToken)

-   **Description**: Deletes the project in SonarQube using the provided endpoint.
-   **Parameters**:
    -   `sonarqubeEndpoint`: The URL endpoint for the SonarQube project deletion API.
    -   `sonarqubeToken`: The authentication token for SonarQube.
-   **Details**: Sends an HTTP POST request to SonarQube to delete the project. Logs the success or failure of the deletion.

  ## Execution Flow

The **SonarqubeRepoCleaner** class operates by integrating with Bitbucket's repository hooks to automate the management of SonarQube projects. When a pull request is merged into a branch, the postUpdate method is triggered. This method first verifies if the target branch of the pull request is protected according to the configured settings. If the branch is not protected, it proceeds to fetch the POM file from the Bitbucket repository to extract relevant project information. Using this information, it constructs the appropriate SonarQube project title and checks if the source branch of the pull request is also not protected before attempting to delete the corresponding project in SonarQube. The deletion request is sent via HTTP POST, and the success or failure of the operation is logged for debugging. The overall workflow ensures that only projects associated with non-protected branches are cleaned up, maintaining an organized SonarQube instance.

# Setting Up the Build Environment

To develop and deploy the SonarQube Repo Cleaner Plugin, you'll need to set up your build environment. Follow the steps below to install and configure the necessary tools and set up the plugin

## 1. Install Atlassian Plugin SDK

Important Note

`The Atlassian Plugin SDK may suggest using Java 8 during installation. However, this plugin is written with Java 11 in mind. Ensure that you have Java 11 installed and configured as JAVA_HOME to avoid any compatibility issues`

The Atlassian Plugin SDK simplifies the build process with specific commands tailored for Atlassian products. Downloading and installing the SDK provides access to these commands, simplifying plugin development and packaging

-   **Windows:**
    -   [Download and install the Atlassian Plugin SDK](https://developer.atlassian.com/server/framework/atlassian-sdk/install-the-atlassian-sdk-on-a-windows-system/) by following the detailed steps provided for Windows systems
-   **Linux or macOS:**
    -   [Download and install the Atlassian Plugin SDK](https://developer.atlassian.com/server/framework/atlassian-sdk/install-the-atlassian-sdk-on-a-linux-or-mac-system/) by following the detailed steps provided for Linux or macOS systems

## 2. Alternative setup: Apache Maven and Java

If you prefer not to use the Atlassian Plugin SDK, you can set up your environment manually by installing Apache Maven and Java

-   **Apache Maven:**
    -   Download [Apache Maven 3.5.4](https://archive.apache.org/dist/maven/maven-3/3.5.4/binaries/)
    -   Ensure that MAVEN_HOME is set to the installation directory and \bin is set in Path
    -   **Note:** The Atlassian Plugin SDK includes a Maven wrapper (atlas-mvn), which simplifies plugin development
-   **Java 11:**

    -   Download and install [Java 11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) if you haven't already
    -   Ensure that JAVA_HOME is set to the JDK installation directory and \bin is set in Path

## 3. Building the Plugin

After setting up your build environment, you first need to obtain the project files

-   **Clone the Project:**

    1.  Clone the repository
-   **Using Atlassian Plugin SDK:**
    1.  Open a terminal or command prompt

    2.  Navigate to the directory of where the plugin was cloned

    3.  Configure **config.properties** before running package command
        1.  Setup **HTTP Read Token** for Bitbucket
        2.  Setup **HTTP User Token** for SonarQube
        3.  Ensure the domains are properly configured
            1.  URL should look something like https://domain.com
            2.  omit trailing '/' if included
        4.  Enter branches you don't want to be deleted when a merge occurs in **protectedBranches**
            1.  Typically will be 'master', 'OD-Fixes', and 'main' if named that
    4.  Run the following command to build the plugin:

        1.  `atlas-mvn clean package`

    5.  This command compiles the code, empties the target/ directory, and packages it into a JAR located in the target/ directory

-   **Using Apache Maven:**
    -   If you're using Maven directly, the command is similar:

        -   `mvn clean package`

## 4. Installing the Plugin in Bitbucket

Once the JAR file is created, you can install it into Bitbucket. (For Bitbucket version 8.9.10 and later uploading plugins is disabled by default so we must enable it if it hasn't been already)

-   **Enable Plugin Upload (Can be skipped for versions 8.9.9 and earlier):**
    1.  Open or create the `bitbucket.properties` file located in the servers file system at `/var/atlassian/application-data/bitbucket/shared/`
    2.  Add the following line to the file: **`upm.plugin.upload.enabled=true`**
    3.  Save the file (a restart of the Bitbucket instance may be required for the changes to take effect)
-   **Upload the JAR file:**
    1.  Navigate to **Administration > Manage apps**
    2.  Click on **Upload app**
    3.  Drag and drop the JAR file from the `target/` directory into the upload area and click **Upload**

## 5. Configuring Logging

To ensure that the plugin logs its activities properly, you need to enable debug logging in Bitbucket

-   **Enable Debug Logging:**
    1.  Navigate to **Administration > Logging and profiling**
    2.  Click **Enable debug logging**
-   **Access Logs:**
    -   Logs can be found in `atlassian-bitbucket.log` file located at `/var/atlassian/application-data/bitbucket/shared/log`

## 6. Enable Plugin

The plugin will be disabled by default on the first install

-   **Enable Plugin**

    1.  Navigate to desired repo
    2.  **Settings** > **Hooks** > under **Post Receive**
    3.  Enable **Sonarqube Repo Cleaner**
-   This can be done at the Project level too if wanted


Setting Up the Test Environment
===============================

> Important Note - Ensure you have the Build Environment set up so you can make changes to the plugin and test them using the Test Environment

To test the plugin, you can use Docker containers to run Bitbucket server and SonarQube instances.

## 1. Setting up Docker Containers

Follow these steps to set up Docker containers for Bitbucket Server and Sonarqube

- **Install Docker**
    - Click this to install [Docker](https://www.docker.com/) follow the instructions it gives 

-   **Pull Docker Images:**

    -   Bitbucket Server:
        -   The recommended version is **8.9.10**. Use the following command to pull the Docker image:
            -   `docker pull atlassian/bitbucket-server:8.9.10`
    -   SonarQube Community Edition:
        -   The recommended version is Community Edition **10**. Use the following command to pull the Docker image:
            -   `docker pull sonarqube:10-community`
-   **Create Docker Volumes:**

    -   Bitbucket Server:
        -   `docker volume create --name bitbucketVolume`
    -   SonarQube Community Edition:
        -   `docker volume create --name sonarqubeVolume`
    -   Replace with **preferred volume names**
-   **Run Docker Containers:**

    -   Bitbucket Server:
        -   `docker run -v bitbucketVolume:/var/atlassian/application-data/bitbucket --name="bitbucketX" -d -p 7990:7990 -p 7999:7999 atlassian/bitbucket-server:8.9.10`
    -   SonarQube Community Edition:
        -   `docker run -v sonarqubeVolume:/opt/sonarqube/data --name="sonarqubeX" -d -p 9000:9000 sonarqube:10-community`
    -   Replace name with **preferred container names** (avoid special characters as this will mess with URI generation in plugin)
    -   Adjust **port numbers** if necessary

## 2. Setting up Docker Container

Follow these steps to enable communication between the Bitbucket Server and SonarQube containers

-   **Create a Docker Network:**
    -   `docker network create bitbucket-sonarqube-network`
    -   Replace with desired name
-   **Connect the Containers to the Network:**
    -   Find the **container ID's** using
        -   `docker ps`
    -   Connect each container to the network
        -   `docker network connect bitbucket-sonarqube-network bitbucketID`
        -   `docker network connect bitbucket-sonarqube-network sonarqubeID`
    -   Replace **bitbucketID** and **sonarqubeID** with actual **container ID's**
-   **Verify Network**
    - Run this command and you should see both containers:
        - `docker network inspect bitbucket-sonarqube-network`
    - Replace **bitbucket-sonarqube-network** with your desired network name!!!!! 

## 3. Access and Configure the Applications

-   **Bitbucket Server:**
    -   Access at [http://localhost:7990](http://localhost:7990/)
    -   Follow the setup instructions on the website
    -   Set up any test projects or repositories you want to scan in SonarQube. 
    -   Make sure they have a pom.xml, with a** groupID** and **artifactID**. This is how the plugin generates SonarQube Project Keys.
-   **SonarQube:**
    -   Access at [http://localhost:9000](http://localhost:9000/)
    -   Follow the setup instructions on the website
    -   If prompted to log in, use the default credentials: Username: **admin**, Password: **admin**
-   Change the **port numbers** if necessary

## 4. Setting Up Bitbucket Branch Analysis with SonarQube

To enable Bitbucket Integration and analysis with SonarQube and configure local analysis, follow these steps in SonarQube:

-   **Create a New Configuration:**
    1.  Navigate to **Administration > DevOps Platform Integrations > Bitbucket**
    2.  Click **Create Configuration**
    3.  Enter a **Name** for the configuration
    4.  Set the **Server URL** to **http://bitbucketX:7990/**, replacing **bitbucketX **and **port number** with the name of your **container** and **port number**
    5.  Provide an **HTTP Token** from the Bitbucket instance with at least read permissions
    6.  Save
-   **Add a New Project**

    1.  Select **Import from Bitbucket Server** or **Import Locally**
    2.  Select any available repos you want to scan
    3.  For testing purposes we can manually set the project key to groupID:artifactID:branch_name
    4.  You could set up Maven analysis and include certain dependencies in the repos pom.xml which would create new projects in sonarqube anytime a new branch is created, but I wasn't familiar with how to do this
        1.  To work around this, anytime you create a new branch in Bitbucket create a project in Sonarqube with the project key **groupID:artifactID:branch_name** where **groupID** and **artifactID** are set in the **pom.xml** of the repo
        2.  In the future, we should set up step 4 to closely mimic what happens with the real Bitbucket Server

## 5. Install Plugin

Follow steps 3 and 4 in Build Environment to install the **Bitbucket-SonarQube Project Cleaner Plugin **onto the docker Bitbucket instance

-   You can access the Bitbucket server files using this command:
    -   `docker exec -it bitbucketID /bin/bash`
-   Whenever a change is made to the plugin re-run **package** command and install the plugin again
-   Ensure the **config.properties** is properly set up with tokens, domains, and branches
    -   domains will be **http://localhost:7990** and **http://sonarqube:9000** (change sonarqube container name or port numbers if needed)
