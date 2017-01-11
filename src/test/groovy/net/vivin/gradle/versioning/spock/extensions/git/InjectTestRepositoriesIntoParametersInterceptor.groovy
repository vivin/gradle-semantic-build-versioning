package net.vivin.gradle.versioning.spock.extensions.git

import net.vivin.gradle.versioning.TestRepository
import org.eclipse.jgit.junit.RepositoryTestCase
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation

import java.lang.reflect.Parameter

class InjectTestRepositoriesIntoParametersInterceptor implements IMethodInterceptor {
    @Override
    public void intercept(IMethodInvocation invocation) {
        // create a map of all TestRepository parameters with their parameter index
        Map<Parameter, Integer> parameters = [:]
        invocation.method.reflection.parameters.eachWithIndex { parameter, i -> parameters << [(parameter): i] }
        parameters = parameters.findAll { TestRepository.equals it.key.type }

        // enlarge arguments array if necessary
        def lastTestRepositoryParameterIndex = parameters*.value.max()
        lastTestRepositoryParameterIndex = lastTestRepositoryParameterIndex == null ? 0 : lastTestRepositoryParameterIndex + 1
        if(invocation.arguments.length < lastTestRepositoryParameterIndex) {
            def newArguments = new Object[lastTestRepositoryParameterIndex]
            System.arraycopy invocation.arguments, 0, newArguments, 0, invocation.arguments.length
            invocation.arguments = newArguments
        }

        // find all parameters to fill
        def parametersToFill = parameters.findAll { !invocation.arguments[it.value] }

        if(!parametersToFill) {
            invocation.proceed()
            return
        }

        def repositoryTestCase = new RepositoryTestCase() {
            def getDb() { super.db }

            def createRepository(bare) { bare ? createBareRepository() : createWorkRepository() }
        }

        try {
            repositoryTestCase.setUp()
            def nonBareParameterEntry = parametersToFill.find { !it.key.isAnnotationPresent(Bare) }
            if(nonBareParameterEntry) {
                invocation.arguments[nonBareParameterEntry.value] = new TestRepository(repositoryTestCase.db)
            }
            parametersToFill.findAll { it != nonBareParameterEntry }.each { parameter, i ->
                invocation.arguments[i] = new TestRepository(repositoryTestCase.createRepository(parameter.isAnnotationPresent(Bare)))
            }

            invocation.proceed()
        } finally {
            repositoryTestCase.tearDown()
        }
    }
}
