package net.vivin.gradle.versioning

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import spock.lang.Unroll

import static net.vivin.gradle.versioning.VersionComparatorSpecification.Constraint.*

@Title('Version Comparator Specification')
class VersionComparatorSpecification extends Specification {
    enum Constraint {
        IS_LESS_THAN('is less than', { it < 0 }),
        IS_EQUAL_TO('is equal to', { it == 0 }),
        IS_GREATER_THAN('is greater than', { it > 0 })

        private static Map<Constraint, Constraint> opposites = [
            (IS_LESS_THAN)   : IS_GREATER_THAN,
            (IS_GREATER_THAN): IS_LESS_THAN
        ]

        private String description
        private Closure<Boolean> constraint

        Constraint(String description, Closure<Boolean> constraint) {
            this.description = description
            this.constraint = constraint
        }

        Boolean check(it) {
            constraint it
        }

        Constraint getOpposite() {
            opposites[this] ?: this
        }

        @Override
        String toString() {
            return description
        }
    }

    @Subject
    private VersionComparator versionComparator = new VersionComparator();

    @Unroll
    def 'comparing #versionA #resultConstraint #versionB'() {
        expect:
        resultConstraint.check versionComparator.compare(versionA, versionB)

        and:
        resultConstraint.opposite.check versionComparator.compare(versionB, versionA)

        where:
        versionA                       || resultConstraint || versionB
        '0.0.1'                        || IS_EQUAL_TO      || '0.0.1'
        '0.0.1'                        || IS_LESS_THAN     || '0.0.2'
        '0.0.10'                       || IS_GREATER_THAN  || '0.0.2'
        '0.0.1'                        || IS_GREATER_THAN  || '0.0.1-alpha'
        '0.0.1-2'                      || IS_LESS_THAN     || '0.0.1-3'
        '0.0.1-alpha.2'                || IS_LESS_THAN     || '0.0.1-alpha.3'
        '0.0.1-x'                      || IS_LESS_THAN     || '0.0.1-y'
        '0.0.1-alpha.x'                || IS_LESS_THAN     || '0.0.1-alpha.y'
        '0.0.1-999'                    || IS_LESS_THAN     || '0.0.1-alpha'
        '0.0.1-alpha.999'              || IS_LESS_THAN     || '0.0.1-alpha.beta'
        '0.0.1-alpha.999.0'            || IS_LESS_THAN     || '0.0.1-alpha.beta.1'
        '3.0.1-alpha.beta.gamma'       || IS_LESS_THAN     || '3.0.1-alpha.beta.gamma.delta'
        '3.0.1-alpha.beta.gamma.delta' || IS_LESS_THAN     || '3.0.1-beta'
        '3.0.1-alpha.beta.gamma.delta' || IS_LESS_THAN     || '4.0.1'
        '3.0.1-alpha.beta.gamma.delta' || IS_LESS_THAN     || '3.1.1'
    }
}
