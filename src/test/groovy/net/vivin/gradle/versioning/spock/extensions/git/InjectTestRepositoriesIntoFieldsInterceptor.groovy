package net.vivin.gradle.versioning.spock.extensions.git

import net.vivin.gradle.versioning.TestRepository
import org.eclipse.jgit.junit.RepositoryTestCase
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import spock.lang.Specification

class InjectTestRepositoriesIntoFieldsInterceptor implements IMethodInterceptor {
    private shared

    InjectTestRepositoriesIntoFieldsInterceptor(shared = true) {
        this.shared = shared
    }

    @Override
    public void intercept(IMethodInvocation invocation) {
        def instance = (shared ? invocation.sharedInstance : invocation.instance) as Specification

        def fieldsToFill = instance
            .specificationContext
            .currentSpec
            .fields
            .findAll { TestRepository.equals it.type }
            .findAll { it.shared == shared }
            .findAll { !it.readValue(instance) }

        if(!fieldsToFill) {
            invocation.proceed()
            return
        }

        def repositoryTestCase = new RepositoryTestCase() {
            def getDb() { super.db }

            def createRepository(bare) { bare ? createBareRepository() : createWorkRepository() }
        }

        try {
            repositoryTestCase.setUp()
            def nonBareField = fieldsToFill.find { !it.isAnnotationPresent(Bare) }
            if(nonBareField) {
                nonBareField.writeValue instance, new TestRepository(repositoryTestCase.db)
            }
            fieldsToFill.findAll { it != nonBareField }.each {
                it.writeValue instance, new TestRepository(repositoryTestCase.createRepository(it.isAnnotationPresent(Bare)))
            }

            invocation.proceed()
        } finally {
            fieldsToFill.each {
                it.writeValue instance, null
            }
            repositoryTestCase.tearDown()
        }
    }
}
