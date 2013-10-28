package cz.bcp.forge.selenium.webdriver;

import java.io.FileNotFoundException;
import java.io.StringWriter;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.ScopeType;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.MetadataFacet;
import org.jboss.forge.project.facets.WebResourceFacet;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.events.PickupResource;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.SetupCommand;

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

    @Inject
    private Event<PickupResource> pickup;

    private DependencyFacet dependencyFacet;

    @SetupCommand
    public void setupCommand() {
        dependencyFacet = project.getFacet(DependencyFacet.class);
        installJunitDependencies();
        installWebDriverDependencies();
    }

    static {
        Properties properties = new Properties();
        properties.setProperty("resource.loader", "class");
        properties.setProperty("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

        Velocity.init(properties);
    }

    @Command(value = "create-test", help = "Create a new test class")
    public void createTest(@Option(name = "class", required = true, type = PromptType.JAVA_CLASS) JavaResource classUnderTest,
                           final PipeOut out)
            throws FileNotFoundException {
        JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
        WebResourceFacet web = project.getFacet(WebResourceFacet.class);
        MetadataFacet metaData = project.getFacet(MetadataFacet.class);
        JavaSource<?> javaSource = classUnderTest.getJavaSource();

        List members = javaSource.getMembers();
        List fields = new ArrayList<Field>();
        for (Member<?, ?> member : javaSource.getMembers()) {
            if (member instanceof Field) {
                if (((Field) member).getType().equals("String"))
                fields.add(member);
            }
        }

        VelocityContext context = new VelocityContext();
        context.put("package", javaSource.getPackage());
        context.put("ClassToTest", javaSource.getName());
        context.put("classToTest", javaSource.getName().toLowerCase());
        context.put("packageImport", javaSource.getPackage());
        context.put("fields", fields);
        context.put("projectName", metaData.getProjectName());


        StringWriter writer = new StringWriter();
        Velocity.mergeTemplate("TestTemplate.vtl", "UTF-8", context, writer);

        JavaClass testClass = JavaParser.parse(JavaClass.class, writer.toString());
        java.saveTestJavaSource(testClass);

        pickup.fire(new PickupResource(java.getTestJavaResource(testClass)));
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
