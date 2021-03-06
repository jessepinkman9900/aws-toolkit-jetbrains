// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.core.credentials

import com.intellij.openapi.components.ServiceManager
import org.junit.rules.ExternalResource
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.aws.toolkits.core.credentials.CredentialIdentifier
import software.aws.toolkits.core.credentials.CredentialIdentifierBase
import software.aws.toolkits.core.credentials.CredentialProviderFactory
import software.aws.toolkits.core.credentials.CredentialsChangeListener
import software.aws.toolkits.core.credentials.ToolkitCredentialsProvider
import software.aws.toolkits.core.region.AwsRegion
import software.aws.toolkits.core.utils.test.aString
import software.aws.toolkits.jetbrains.core.region.MockRegionProvider

@Deprecated("Use MockCredentialManagerRule")
class MockCredentialsManager : CredentialManager() {
    init {
        reset()
    }

    fun reset() {
        getCredentialIdentifiers().filterNot { it.id == DUMMY_PROVIDER_IDENTIFIER.id }.forEach { removeProvider(it) }

        addProvider(DUMMY_PROVIDER_IDENTIFIER)
    }

    fun addCredentials(
        id: String,
        credentials: AwsCredentials = AwsBasicCredentials.create("Access", "Secret"),
        regionId: String? = null
    ): CredentialIdentifier = addCredentials(id, StaticCredentialsProvider.create(credentials), regionId)

    fun addCredentials(
        id: String,
        credentials: AwsCredentialsProvider,
        regionId: String? = null
    ): MockCredentialIdentifier = MockCredentialIdentifier(id, credentials, regionId).also {
        addProvider(it)
    }

    fun createCredentialProvider(
        id: String = aString(),
        credentials: AwsCredentials = AwsBasicCredentials.create("Access", "Secret"),
        region: AwsRegion = MockRegionProvider.getInstance().defaultRegion()
    ): ToolkitCredentialsProvider {
        val credentialIdentifier = MockCredentialIdentifier(id, StaticCredentialsProvider.create(credentials), null)

        addProvider(credentialIdentifier)

        return getAwsCredentialProvider(credentialIdentifier, region)
    }

    fun removeCredentials(credentialIdentifier: CredentialIdentifier) {
        removeProvider(credentialIdentifier)
    }

    override fun factoryMapping(): Map<String, CredentialProviderFactory> = mapOf(MockCredentialProviderFactory.id to MockCredentialProviderFactory)

    companion object {
        fun getInstance(): MockCredentialsManager = ServiceManager.getService(CredentialManager::class.java) as MockCredentialsManager

        val DUMMY_PROVIDER_IDENTIFIER: CredentialIdentifier = MockCredentialIdentifier(
            "DUMMY_CREDENTIALS",
            StaticCredentialsProvider.create(AwsBasicCredentials.create("DummyAccess", "DummySecret")),
            null
        )
    }

    class MockCredentialIdentifier(override val displayName: String, val credentials: AwsCredentialsProvider, override val defaultRegionId: String?) :
        CredentialIdentifierBase(null) {
        override val id: String = displayName
        override val factoryId: String = "mockCredentialProviderFactory"
    }

    private object MockCredentialProviderFactory : CredentialProviderFactory {
        override val id: String = "mockCredentialProviderFactory"

        override fun setUp(credentialLoadCallback: CredentialsChangeListener) {}

        override fun createAwsCredentialProvider(
            providerId: CredentialIdentifier,
            region: AwsRegion,
            sdkHttpClientSupplier: () -> SdkHttpClient
        ): ToolkitCredentialsProvider = ToolkitCredentialsProvider(providerId, (providerId as MockCredentialIdentifier).credentials)
    }
}

@Suppress("DEPRECATION")
class MockCredentialManagerRule : ExternalResource() {
    private lateinit var credentialManager: MockCredentialsManager

    override fun before() {
        credentialManager = MockCredentialsManager.getInstance()
    }

    fun addCredentials(
        id: String,
        credentials: AwsCredentials = AwsBasicCredentials.create("Access", "Secret"),
        region: AwsRegion? = null
    ): CredentialIdentifier = credentialManager.addCredentials(id, credentials, region?.id)

    fun addCredentials(
        id: String,
        credentials: AwsCredentialsProvider,
        region: AwsRegion? = null
    ): CredentialIdentifier = credentialManager.addCredentials(id, credentials, region?.id)

    fun createCredentialProvider(
        id: String = aString(),
        credentials: AwsCredentials = AwsBasicCredentials.create("Access", "Secret"),
        region: AwsRegion = MockRegionProvider.getInstance().defaultRegion()
    ): ToolkitCredentialsProvider = credentialManager.createCredentialProvider(id, credentials, region)

    fun removeCredentials(credentialIdentifier: CredentialIdentifier) = credentialManager.removeCredentials(credentialIdentifier)

    override fun after() {
        reset()
    }

    fun reset() {
        @Suppress("DEPRECATION")
        credentialManager.reset()
    }
}
