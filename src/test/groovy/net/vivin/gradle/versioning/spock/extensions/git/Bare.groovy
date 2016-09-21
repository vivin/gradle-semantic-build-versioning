package net.vivin.gradle.versioning.spock.extensions.git

import java.lang.annotation.Retention
import java.lang.annotation.Target

import static java.lang.annotation.ElementType.FIELD
import static java.lang.annotation.ElementType.PARAMETER
import static java.lang.annotation.RetentionPolicy.RUNTIME

/**
 * This annotation is a marker for {@link net.vivin.gradle.versioning.TestRepository} fields and parameters that get
 * filled automatically to get filled with a bare repository instead of a work repository which is the default.
 * <p/>
 * This annotation makes only sense on unassigned {@link net.vivin.gradle.versioning.TestRepository} fields and
 * parameters. On any other field or parameter this annotation will be ignored.
 */
@Retention(RUNTIME)
@Target([FIELD, PARAMETER])
@interface Bare {
}
