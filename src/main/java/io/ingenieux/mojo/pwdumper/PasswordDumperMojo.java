package io.ingenieux.mojo.pwdumper;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

@Mojo(name = "dump-password", defaultPhase = LifecyclePhase.NONE, requiresDirectInvocation=true)

public class PasswordDumperMojo extends AbstractMojo implements Contextualizable {
  /**
   * Application Name
   */
  @Parameter(property = "serverId", required=true)
  String serverId;

  /**
   * Maven Settings
   */
  @Parameter(defaultValue = "${settings}")
  private Settings settings;



  @Requirement
  private PlexusContainer container;

  private SettingsDecrypter decrypter;

  /**
   * {@inheritDoc}
   */
  public void contextualize(Context context) throws ContextException {
    container = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);

    try {
      decrypter = container.lookup(SettingsDecrypter.class);
    } catch (ComponentLookupException exc) {
      throw new ContextException("Decrypter", exc);
    }
  }

  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      executeInternal();
    } catch (Exception exc) {
      throw new MojoExecutionException("Failure", exc);
    }
  }

  protected void executeInternal() throws Exception {
    Server server = getServer(settings, serverId);

    if (null != server) {
      getLog().info("l/p: " + server.getUsername() + " / " + server.getPassword());
    } else {
      getLog().warn("Password not found");
    }
  }

  /**
   * Get server with given id
   *
   * @param settings
   * @param serverId must be non-null and non-empty
   * @return server or null if none matching
   */
  protected Server getServer(final Settings settings, final String serverId) {
    Server result = null;

    if (settings == null)
      return null;

    List<Server> servers = settings.getServers();

    if (servers == null || servers.isEmpty())
      return null;

    for (Server server : servers) {
      if (server.getId().equals(serverId)) {
        result = server;
        break;
      }
    }

    if (null != result) {
      SettingsDecryptionResult dResult = decrypter.decrypt(new DefaultSettingsDecryptionRequest(result));

      result = dResult.getServer();
    }

    return result;
  }
}
