package net.vivin.gradle.versioning.spock.extensions.gradle

import java.lang.annotation.Retention
import java.lang.annotation.Target

import static java.lang.annotation.ElementType.FIELD
import static java.lang.annotation.ElementType.PARAMETER
import static java.lang.annotation.RetentionPolicy.RUNTIME

/**
 * This annotation can be used to set the project directory of a {@link org.gradle.testkit.runner.GradleRunner} field or
 * parameter that gets filled automatically.
 * <p/>
 * This annotation needs one argument, which is a closure that has to return what should be used as project directory
 * for the Gradle runner. The valid return types for the given closure are defined by the availability of a {@code get}
 * method for the type or one of its supertypes or interfaces in the class {@link ProjectDirProviderCategory}. These
 * currently are {@link File}, {@link java.nio.file.Path} and {@link net.vivin.gradle.versioning.TestRepository}, but
 * in the latter case of course only non-bare repositories are supported.
 * <p/>
 * The closure must not return {@code null}.
 * <p/>
 * This annotation makes only sense on unassigned {@code GradleRunner} fields and parameters. On any other field or
 * parameter this annotation will be ignored.<br/>
 * <b>Example:</b>
 * <pre>
 * TestRepository testRepository
 *
 * def test(@ProjectDirProvider({ testRepository }) GradleRunner gradleRunner) {
 *     expect:
 *     gradleRunner.build()
 * }
 * </pre>
 */
@Retention(RUNTIME)
@Target([FIELD, PARAMETER])
@interface ProjectDirProvider {
    /**
     * A closure that has to return what should be used as project directory for the annotated Gradle runner. The valid
     * return types for the given closure are defined by the availability of a {@code get} method for the type or one of
     * its supertypes or interfaces in the class {@link ProjectDirProviderCategory}. These currently are {@link File},
     * {@link java.nio.file.Path} and {@link net.vivin.gradle.versioning.TestRepository}, but in the latter case of
     * course only non-bare repositories are supported.
     * <p/>
     * The closure must not return {@code null}.
     */
    Class<Closure<?>> value()
}
