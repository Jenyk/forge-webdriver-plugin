package cz.bcp.forge.selenium.webdriver;

import java.util.List;
import javax.inject.Inject;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.ScopeType;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeIn;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.SetupCommand;
import cz.bcp.forge.selenium.webdriver.DependencyUtil;

/**
 *
 */
@Alias("webdriver")
public class WebDriverPlugin implements Plugin {
	
	public static final String JUNIT_VERSION_PROP_NAME = "version.junit";
	public static final String JUNIT_VERSION_PROP = "${" + JUNIT_VERSION_PROP_NAME + "}";
	
	public static final String SELENIUM_VERSION_PROP_NAME = "version.selenium";
	public static final String SELENIUM_VERSION_PROP = "${" + SELENIUM_VERSION_PROP_NAME + "}";
	
	@Inject
	private Shell shell;

	@Inject
	private Project project;

	private DependencyFacet dependencyFacet;

	@SetupCommand
	public void setupCommand() {
		dependencyFacet = project.getFacet(DependencyFacet.class);
		installJunitDependencies();
		installWebDriverDependencies();
	}
	
	private void installJunitDependencies() {
		DependencyBuilder junitDependency = createJunitDependency();
		if (!dependencyFacet.hasEffectiveDependency(junitDependency)) {
			List<Dependency> dependencies = dependencyFacet.resolveAvailableVersions(junitDependency);
			Dependency dependency = shell.promptChoiceTyped("Which version of JUnit do you want to install?", dependencies,
					DependencyUtil.getLatestNonSnapshotVersion(dependencies));

			dependencyFacet.setProperty(JUNIT_VERSION_PROP_NAME, dependency.getVersion());
			dependencyFacet.addDirectDependency(DependencyBuilder.create(dependency).setVersion(JUNIT_VERSION_PROP));
		}

	}
	
	private void installWebDriverDependencies() {
		DependencyBuilder webDriverDependency = createWebDriverDependency();
		if (!dependencyFacet.hasEffectiveDependency(webDriverDependency)) {
			List<Dependency> dependencies = dependencyFacet.resolveAvailableVersions(webDriverDependency);
			Dependency dependency = shell.promptChoiceTyped("Which version of Selenium do you want to install?", dependencies,
					DependencyUtil.getLatestNonSnapshotVersion(dependencies));

			dependencyFacet.setProperty(SELENIUM_VERSION_PROP_NAME, dependency.getVersion());
			dependencyFacet.addDirectDependency(DependencyBuilder.create(dependency).setVersion(SELENIUM_VERSION_PROP));
		}

	}

	private DependencyBuilder createJunitDependency() {
		return DependencyBuilder.create().setGroupId("junit").setArtifactId("junit").setScopeType(ScopeType.TEST);
	}
	
	private DependencyBuilder createWebDriverDependency() {
		return DependencyBuilder.create().setGroupId("org.seleniumhq.selenium").setArtifactId("selenium-java").setScopeType(ScopeType.TEST);
	}
}
