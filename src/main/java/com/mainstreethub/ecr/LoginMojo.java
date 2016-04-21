package com.mainstreethub.ecr;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ecr.AmazonECR;
import com.amazonaws.services.ecr.AmazonECRClient;
import com.amazonaws.services.ecr.model.AuthorizationData;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenRequest;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenResult;
import com.google.common.base.Splitter;
import com.google.common.io.BaseEncoding;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "login")
public class LoginMojo extends AbstractMojo {
  private static final Splitter TOKEN_SPLITTER = Splitter.on(":").limit(3).trimResults();

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    final AmazonECR ecrClient = new AmazonECRClient();

    final List<String> splitTokenList;
    try {
      getLog().info("Fetching ECR authorization...");
      final GetAuthorizationTokenRequest authRequest = new GetAuthorizationTokenRequest();
      final GetAuthorizationTokenResult tokenResult
          = ecrClient.getAuthorizationToken(authRequest);

      final List<AuthorizationData> authorizationDataList = tokenResult.getAuthorizationData();
      if (authorizationDataList.isEmpty()) {
        getLog().error("Authorization data not available.");
        return;
      }

      final AuthorizationData authorizationData = authorizationDataList.get(0);

      final String decodedToken = new String(BaseEncoding.base64()
          .decode(authorizationData.getAuthorizationToken()));

      splitTokenList = TOKEN_SPLITTER.splitToList(decodedToken);
      if (splitTokenList.size() != 2) {
        getLog().error("Authorization data in unsupported format.");
        return;
      }
    } catch (AmazonServiceException ex) {
      throw new MojoExecutionException("Error while retrieving authorization data", ex);
    }

    getLog().info("Successfully retrieved ECR docker credentials, exporting variables.");

    project.getProperties().setProperty("aws.ecr.login.user", splitTokenList.get(0));
    project.getProperties().setProperty("aws.ecr.login.password", splitTokenList.get(1));
  }
}
